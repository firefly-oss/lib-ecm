/*
 * Copyright (c) 2024 Firefly Software Solutions Inc.
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

package com.firefly.ecm.adapter.azureblob;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Spring Boot auto-configuration for Azure Blob Storage ECM adapters.
 *
 * <p>This configuration class automatically sets up the necessary beans for Azure Blob Storage
 * integration when the appropriate conditions are met:</p>
 * <ul>
 *   <li>Azure Blob Storage SDK is on the classpath</li>
 *   <li>The adapter type is configured as "azure-blob"</li>
 *   <li>Required configuration properties are provided</li>
 * </ul>
 *
 * <p>The configuration supports multiple authentication methods:</p>
 * <ul>
 *   <li>Account key authentication</li>
 *   <li>Connection string authentication</li>
 *   <li>SAS token authentication</li>
 *   <li>Azure Managed Identity authentication</li>
 * </ul>
 *
 * <p>Resilience patterns are automatically configured:</p>
 * <ul>
 *   <li>Circuit breaker for fault tolerance</li>
 *   <li>Retry mechanism for transient failures</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({BlobServiceClient.class, BlobContainerClient.class})
@ConditionalOnProperty(name = "firefly.ecm.adapter-type", havingValue = "azure-blob")
@EnableConfigurationProperties(AzureBlobAdapterProperties.class)
public class AzureBlobAutoConfiguration {

    /**
     * Creates the Azure Blob Service Client based on the configured authentication method.
     *
     * @param properties the Azure Blob adapter properties
     * @return configured BlobServiceClient
     */
    @Bean
    @ConditionalOnMissingBean
    public BlobServiceClient blobServiceClient(AzureBlobAdapterProperties properties) {
        BlobServiceClientBuilder builder = new BlobServiceClientBuilder();

        // Set endpoint
        if (properties.getEndpoint() != null) {
            builder.endpoint(properties.getEndpoint());
        } else {
            builder.endpoint(String.format("https://%s.blob.core.windows.net", properties.getAccountName()));
        }

        // Configure authentication
        if (properties.getConnectionString() != null) {
            log.info("Configuring Azure Blob Storage with connection string authentication");
            builder.connectionString(properties.getConnectionString());
        } else if (properties.getAccountKey() != null) {
            log.info("Configuring Azure Blob Storage with account key authentication");
            StorageSharedKeyCredential credential = new StorageSharedKeyCredential(
                properties.getAccountName(), properties.getAccountKey());
            builder.credential(credential);
        } else if (properties.getSasToken() != null) {
            log.info("Configuring Azure Blob Storage with SAS token authentication");
            builder.sasToken(properties.getSasToken());
        } else if (properties.getManagedIdentity()) {
            log.info("Configuring Azure Blob Storage with managed identity authentication");
            TokenCredential credential = new DefaultAzureCredentialBuilder().build();
            builder.credential(credential);
        } else {
            throw new IllegalArgumentException(
                "Azure Blob Storage authentication not configured. Please provide one of: " +
                "connection-string, account-key, sas-token, or enable managed-identity");
        }

        // Configure retry policy (using RequestRetryOptions)
        com.azure.storage.common.policy.RequestRetryOptions retryOptions =
            new com.azure.storage.common.policy.RequestRetryOptions();
        // Note: Azure SDK API may vary, using basic configuration
        builder.retryOptions(retryOptions);

        BlobServiceClient client = builder.buildClient();
        log.info("Azure Blob Service Client configured successfully for account: {}", 
                properties.getAccountName());
        
        return client;
    }

    /**
     * Creates the Azure Blob Container Client for the configured container.
     *
     * @param blobServiceClient the blob service client
     * @param properties the Azure Blob adapter properties
     * @return configured BlobContainerClient
     */
    @Bean
    @ConditionalOnMissingBean
    public BlobContainerClient blobContainerClient(BlobServiceClient blobServiceClient,
                                                   AzureBlobAdapterProperties properties) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(properties.getContainerName());
        
        // Create container if it doesn't exist
        if (!containerClient.exists()) {
            log.info("Creating Azure Blob container: {}", properties.getContainerName());
            containerClient.create();
        }
        
        log.info("Azure Blob Container Client configured for container: {}", properties.getContainerName());
        return containerClient;
    }

    /**
     * Creates the ObjectMapper for JSON serialization/deserialization.
     *
     * @return configured ObjectMapper
     */
    @Bean
    @ConditionalOnMissingBean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    /**
     * Creates the circuit breaker for Azure Blob Storage operations.
     *
     * @return configured CircuitBreaker
     */
    @Bean("azureBlobCircuitBreaker")
    @ConditionalOnMissingBean(name = "azureBlobCircuitBreaker")
    public CircuitBreaker azureBlobCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .permittedNumberOfCallsInHalfOpenState(3)
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("azureBlobStorage", config);
        log.info("Azure Blob Storage circuit breaker configured");
        return circuitBreaker;
    }

    /**
     * Creates the retry mechanism for Azure Blob Storage operations.
     *
     * @param properties the Azure Blob adapter properties
     * @return configured Retry
     */
    @Bean("azureBlobRetry")
    @ConditionalOnMissingBean(name = "azureBlobRetry")
    public Retry azureBlobRetry(AzureBlobAdapterProperties properties) {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(properties.getMaxRetries())
            .waitDuration(Duration.ofSeconds(2))
            .retryOnException(throwable -> {
                // Retry on specific Azure Storage exceptions
                String exceptionName = throwable.getClass().getSimpleName();
                return exceptionName.contains("BlobStorageException") ||
                       exceptionName.contains("TimeoutException") ||
                       exceptionName.contains("ConnectException");
            })
            .build();

        Retry retry = Retry.of("azureBlobStorage", config);
        log.info("Azure Blob Storage retry mechanism configured with {} max attempts", 
                properties.getMaxRetries());
        return retry;
    }

    /**
     * Creates the Azure Blob Document Adapter.
     *
     * @param containerClient the blob container client
     * @param properties the Azure Blob adapter properties
     * @param objectMapper the object mapper
     * @param circuitBreaker the circuit breaker
     * @param retry the retry mechanism
     * @return configured AzureBlobDocumentAdapter
     */
    @Bean
    @ConditionalOnMissingBean
    public AzureBlobDocumentAdapter azureBlobDocumentAdapter(BlobContainerClient containerClient,
                                                            AzureBlobAdapterProperties properties,
                                                            ObjectMapper objectMapper,
                                                            CircuitBreaker circuitBreaker,
                                                            Retry retry) {
        log.info("Creating Azure Blob Document Adapter");
        return new AzureBlobDocumentAdapter(containerClient, properties, objectMapper, circuitBreaker, retry);
    }

    /**
     * Creates the Azure Blob Document Content Adapter.
     *
     * @param containerClient the blob container client
     * @param properties the Azure Blob adapter properties
     * @param circuitBreaker the circuit breaker
     * @param retry the retry mechanism
     * @return configured AzureBlobDocumentContentAdapter
     */
    @Bean
    @ConditionalOnMissingBean
    public AzureBlobDocumentContentAdapter azureBlobDocumentContentAdapter(BlobContainerClient containerClient,
                                                                          AzureBlobAdapterProperties properties,
                                                                          CircuitBreaker circuitBreaker,
                                                                          Retry retry) {
        log.info("Creating Azure Blob Document Content Adapter");
        return new AzureBlobDocumentContentAdapter(containerClient, properties, circuitBreaker, retry);
    }
}
