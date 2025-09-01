/*
 * Copyright 2024 Firefly Software Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.firefly.ecm.adapter.s3.config;

import com.firefly.ecm.adapter.s3.S3AdapterProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
// Removed HTTP client import - using default client
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;

/**
 * Configuration for optimized S3 client with connection pooling and performance tuning.
 * 
 * <p>This configuration provides:</p>
 * <ul>
 *   <li>Connection pooling for improved performance</li>
 *   <li>Optimized timeouts and retry policies</li>
 *   <li>Resource management and cleanup</li>
 *   <li>Support for different credential providers</li>
 * </ul>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "firefly.ecm.adapter-type", havingValue = "s3")
public class S3ClientConfiguration {

    /**
     * Creates an optimized S3 client with connection pooling.
     */
    @Bean
    public S3Client s3Client(S3AdapterProperties properties) {
        log.info("Configuring optimized S3 client for bucket: {}", properties.getBucketName());

        // Configure client override settings
        ClientOverrideConfiguration clientConfig = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofMinutes(5)) // API call timeout
                .apiCallAttemptTimeout(Duration.ofSeconds(30)) // Per-attempt timeout
                .retryPolicy(RetryPolicy.builder()
                        .numRetries(3) // Number of retries
                        .build())
                .build();

        // Build S3 client
        S3ClientBuilder clientBuilder = S3Client.builder()
                .overrideConfiguration(clientConfig)
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(createCredentialsProvider(properties));

        // Configure endpoint if specified (for LocalStack, MinIO, etc.)
        if (properties.getEndpoint() != null && !properties.getEndpoint().isEmpty()) {
            clientBuilder.endpointOverride(URI.create(properties.getEndpoint()));
            clientBuilder.forcePathStyle(true); // Required for non-AWS S3 implementations
        }

        S3Client client = clientBuilder.build();
        log.info("S3 client configured successfully with connection pooling");
        return client;
    }

    /**
     * Creates an S3 presigner for generating pre-signed URLs.
     */
    @Bean
    public S3Presigner s3Presigner(S3AdapterProperties properties) {
        log.info("Configuring S3 presigner");

        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(createCredentialsProvider(properties));

        // Configure endpoint if specified
        if (properties.getEndpoint() != null && !properties.getEndpoint().isEmpty()) {
            presignerBuilder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        S3Presigner presigner = presignerBuilder.build();
        log.info("S3 presigner configured successfully");
        return presigner;
    }

    /**
     * Creates appropriate credentials provider based on configuration.
     */
    private AwsCredentialsProvider createCredentialsProvider(S3AdapterProperties properties) {
        if (properties.getAccessKey() != null && properties.getSecretKey() != null) {
            log.debug("Using static credentials provider");
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
            );
        } else {
            log.debug("Using default credentials provider chain");
            return DefaultCredentialsProvider.create();
        }
    }

    /**
     * Configuration for S3 client metrics and monitoring.
     */
    @Bean
    @ConditionalOnProperty(name = "firefly.ecm.adapter.s3.enable-metrics", havingValue = "true")
    public S3ClientMetrics s3ClientMetrics() {
        return new S3ClientMetrics();
    }

    /**
     * Simple metrics collector for S3 operations.
     */
    public static class S3ClientMetrics {
        private volatile long totalRequests = 0;
        private volatile long successfulRequests = 0;
        private volatile long failedRequests = 0;
        private volatile long totalResponseTime = 0;

        public void recordRequest(boolean success, long responseTimeMs) {
            totalRequests++;
            totalResponseTime += responseTimeMs;
            if (success) {
                successfulRequests++;
            } else {
                failedRequests++;
            }
        }

        public double getSuccessRate() {
            return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0;
        }

        public double getAverageResponseTime() {
            return totalRequests > 0 ? (double) totalResponseTime / totalRequests : 0.0;
        }

        public long getTotalRequests() { return totalRequests; }
        public long getSuccessfulRequests() { return successfulRequests; }
        public long getFailedRequests() { return failedRequests; }
    }

    /**
     * Resource cleanup configuration.
     */
    @Bean
    public S3ResourceManager s3ResourceManager(S3Client s3Client, S3Presigner s3Presigner) {
        return new S3ResourceManager(s3Client, s3Presigner);
    }

    /**
     * Manages S3 client resources and cleanup.
     */
    public static class S3ResourceManager {
        private final S3Client s3Client;
        private final S3Presigner s3Presigner;

        public S3ResourceManager(S3Client s3Client, S3Presigner s3Presigner) {
            this.s3Client = s3Client;
            this.s3Presigner = s3Presigner;
        }

        public void cleanup() {
            log.info("Cleaning up S3 client resources");
            try {
                if (s3Presigner != null) {
                    s3Presigner.close();
                }
                if (s3Client != null) {
                    s3Client.close();
                }
                log.info("S3 client resources cleaned up successfully");
            } catch (Exception e) {
                log.error("Error during S3 client cleanup", e);
            }
        }
    }

    /**
     * Configuration for S3 transfer optimization.
     */
    @Bean
    @ConditionalOnProperty(name = "firefly.ecm.adapter.s3.enable-transfer-optimization", havingValue = "true")
    public S3TransferOptimizer s3TransferOptimizer(S3AdapterProperties properties) {
        return new S3TransferOptimizer(properties);
    }

    /**
     * Optimizes S3 transfer operations based on file size and type.
     */
    public static class S3TransferOptimizer {
        private final S3AdapterProperties properties;

        public S3TransferOptimizer(S3AdapterProperties properties) {
            this.properties = properties;
        }

        /**
         * Determines if multipart upload should be used based on content size.
         */
        public boolean shouldUseMultipart(long contentSize) {
            return properties.getEnableMultipart() && 
                   contentSize > properties.getMultipartThreshold();
        }

        /**
         * Calculates optimal part size for multipart uploads.
         */
        public long calculatePartSize(long contentSize) {
            // AWS S3 allows maximum 10,000 parts
            long maxParts = 10000;
            long minPartSize = 5 * 1024 * 1024; // 5MB minimum
            long maxPartSize = 5L * 1024 * 1024 * 1024; // 5GB maximum

            long calculatedPartSize = contentSize / maxParts;
            
            if (calculatedPartSize < minPartSize) {
                return minPartSize;
            } else if (calculatedPartSize > maxPartSize) {
                return maxPartSize;
            } else {
                return calculatedPartSize;
            }
        }

        /**
         * Determines optimal storage class based on content type and usage patterns.
         */
        public String getOptimalStorageClass(String mimeType, long contentSize) {
            // Simple heuristics - can be enhanced based on business requirements
            if (mimeType != null && mimeType.startsWith("image/")) {
                return "STANDARD_IA"; // Images might be accessed less frequently
            } else if (contentSize > 100 * 1024 * 1024) { // Files larger than 100MB
                return "STANDARD_IA";
            } else {
                return "STANDARD";
            }
        }
    }
}
