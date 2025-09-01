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
package com.firefly.ecm.adapter.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;
import java.time.Duration;

/**
 * Auto-configuration for the Amazon S3 ECM adapter.
 *
 * <p>This configuration class automatically sets up the necessary beans for the S3 adapter
 * when the adapter type is set to "s3". It configures:</p>
 * <ul>
 *   <li>S3 client with proper authentication and region settings</li>
 *   <li>S3 adapter properties from configuration</li>
 *   <li>Retry policies and timeouts</li>
 *   <li>Custom endpoint support for S3-compatible services</li>
 * </ul>
 *
 * <p>The configuration supports multiple authentication methods:</p>
 * <ul>
 *   <li>IAM roles (recommended for AWS environments)</li>
 *   <li>Access key and secret key (for development/testing)</li>
 *   <li>Default credential provider chain</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(S3Client.class)
@ConditionalOnProperty(name = "firefly.ecm.adapter-type", havingValue = "s3")
@EnableConfigurationProperties(S3AdapterProperties.class)
public class S3AdapterAutoConfiguration {

    /**
     * Creates and configures the AWS credentials provider based on the adapter properties.
     *
     * <p>If access key and secret key are provided in the configuration, it creates a
     * static credentials provider. Otherwise, it uses the default AWS credentials
     * provider chain which includes:</p>
     * <ul>
     *   <li>Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)</li>
     *   <li>Java system properties (aws.accessKeyId, aws.secretKey)</li>
     *   <li>Credential profiles file (~/.aws/credentials)</li>
     *   <li>IAM instance profile credentials</li>
     * </ul>
     *
     * @param properties the S3 adapter properties
     * @return configured AWS credentials provider
     */
    @Bean
    @ConditionalOnMissingBean
    public AwsCredentialsProvider awsCredentialsProvider(S3AdapterProperties properties) {
        if (properties.getAccessKey() != null && properties.getSecretKey() != null) {
            log.info("Using static credentials for S3 adapter");
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                    properties.getAccessKey(), 
                    properties.getSecretKey()
            );
            return StaticCredentialsProvider.create(credentials);
        } else {
            log.info("Using default credentials provider chain for S3 adapter");
            return DefaultCredentialsProvider.create();
        }
    }

    /**
     * Creates and configures the S3 client with all necessary settings.
     *
     * <p>The client is configured with:</p>
     * <ul>
     *   <li>Region from properties</li>
     *   <li>Credentials provider</li>
     *   <li>Custom endpoint (if specified)</li>
     *   <li>Path-style access (if enabled)</li>
     *   <li>Retry policy and timeouts</li>
     * </ul>
     *
     * @param properties the S3 adapter properties
     * @param credentialsProvider the AWS credentials provider
     * @return configured S3 client
     */
    @Bean
    @ConditionalOnMissingBean
    public S3Client s3Client(S3AdapterProperties properties, AwsCredentialsProvider credentialsProvider) {
        log.info("Configuring S3 client for region: {}, bucket: {}", 
                properties.getRegion(), properties.getBucketName());

        S3ClientBuilder clientBuilder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider);

        // Configure custom endpoint if provided (for S3-compatible services)
        if (properties.getEndpoint() != null && !properties.getEndpoint().trim().isEmpty()) {
            log.info("Using custom S3 endpoint: {}", properties.getEndpoint());
            clientBuilder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        // Configure path-style access if enabled
        if (properties.getPathStyleAccess()) {
            log.info("Enabling path-style access for S3 client");
            clientBuilder.forcePathStyle(true);
        }

        // Configure client overrides (timeouts, retries)
        ClientOverrideConfiguration.Builder overrideBuilder = ClientOverrideConfiguration.builder();
        
        // Set timeouts
        if (properties.getConnectionTimeout() != null) {
            overrideBuilder.apiCallTimeout(properties.getConnectionTimeout());
        }
        if (properties.getSocketTimeout() != null) {
            overrideBuilder.apiCallAttemptTimeout(properties.getSocketTimeout());
        }

        // Set retry policy
        if (properties.getMaxRetries() != null) {
            RetryPolicy retryPolicy = RetryPolicy.builder()
                    .numRetries(properties.getMaxRetries())
                    .build();
            overrideBuilder.retryPolicy(retryPolicy);
        }

        clientBuilder.overrideConfiguration(overrideBuilder.build());

        S3Client s3Client = clientBuilder.build();
        
        // Validate bucket access
        validateBucketAccess(s3Client, properties);
        
        return s3Client;
    }

    /**
     * Validates that the S3 bucket is accessible with the configured credentials.
     *
     * <p>This method performs a simple head bucket operation to verify that:</p>
     * <ul>
     *   <li>The bucket exists</li>
     *   <li>The credentials have access to the bucket</li>
     *   <li>The region configuration is correct</li>
     * </ul>
     *
     * @param s3Client the configured S3 client
     * @param properties the S3 adapter properties
     * @throws RuntimeException if bucket validation fails
     */
    private void validateBucketAccess(S3Client s3Client, S3AdapterProperties properties) {
        try {
            log.info("Validating access to S3 bucket: {}", properties.getBucketName());
            s3Client.headBucket(builder -> builder.bucket(properties.getBucketName()));
            log.info("Successfully validated access to S3 bucket: {}", properties.getBucketName());
        } catch (Exception e) {
            log.error("Failed to validate access to S3 bucket: {}. Please check bucket name, region, and credentials.", 
                    properties.getBucketName(), e);
            throw new RuntimeException("S3 bucket validation failed", e);
        }
    }
}
