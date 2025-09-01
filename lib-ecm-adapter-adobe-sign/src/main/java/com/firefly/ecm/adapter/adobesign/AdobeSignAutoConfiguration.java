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

package com.firefly.ecm.adapter.adobesign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firefly.core.ecm.port.document.DocumentContentPort;
import com.firefly.core.ecm.port.document.DocumentPort;
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
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Spring Boot auto-configuration for Adobe Sign ECM adapters.
 *
 * <p>This configuration class automatically sets up the necessary beans for Adobe Sign
 * integration when the appropriate conditions are met:</p>
 * <ul>
 *   <li>WebClient is on the classpath</li>
 *   <li>The eSignature provider is configured as "adobe-sign"</li>
 *   <li>Required configuration properties are provided</li>
 * </ul>
 *
 * <p>The configuration supports OAuth 2.0 authentication with refresh tokens
 * for secure and reliable API access to Adobe Sign services.</p>
 *
 * <p>Resilience patterns are automatically configured:</p>
 * <ul>
 *   <li>Circuit breaker for fault tolerance</li>
 *   <li>Retry mechanism for transient failures</li>
 *   <li>Connection and read timeouts</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(WebClient.class)
@ConditionalOnProperty(name = "firefly.ecm.esignature.provider", havingValue = "adobe-sign")
@EnableConfigurationProperties(AdobeSignAdapterProperties.class)
public class AdobeSignAutoConfiguration {

    /**
     * Creates the WebClient for Adobe Sign API communication.
     *
     * @param properties the Adobe Sign adapter properties
     * @return configured WebClient
     */
    @Bean("adobeSignWebClient")
    @ConditionalOnMissingBean(name = "adobeSignWebClient")
    public WebClient adobeSignWebClient(AdobeSignAdapterProperties properties) {
        return WebClient.builder()
            .baseUrl(properties.getBaseUrl())
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
            .build();
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
     * Creates the circuit breaker for Adobe Sign operations.
     *
     * @return configured CircuitBreaker
     */
    @Bean("adobeSignCircuitBreaker")
    @ConditionalOnMissingBean(name = "adobeSignCircuitBreaker")
    public CircuitBreaker adobeSignCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .permittedNumberOfCallsInHalfOpenState(3)
            .recordExceptions(
                RuntimeException.class,
                org.springframework.web.reactive.function.client.WebClientException.class
            )
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("adobeSign", config);
        log.info("Adobe Sign circuit breaker configured");
        return circuitBreaker;
    }

    /**
     * Creates the retry mechanism for Adobe Sign operations.
     *
     * @param properties the Adobe Sign adapter properties
     * @return configured Retry
     */
    @Bean("adobeSignRetry")
    @ConditionalOnMissingBean(name = "adobeSignRetry")
    public Retry adobeSignRetry(AdobeSignAdapterProperties properties) {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(properties.getMaxRetries())
            .waitDuration(Duration.ofSeconds(2))
            .retryOnException(throwable -> {
                // Retry on specific exceptions
                String exceptionName = throwable.getClass().getSimpleName();
                return exceptionName.contains("WebClientException") ||
                       exceptionName.contains("TimeoutException") ||
                       exceptionName.contains("ConnectException") ||
                       (throwable instanceof RuntimeException && 
                        throwable.getMessage() != null &&
                        (throwable.getMessage().contains("timeout") ||
                         throwable.getMessage().contains("connection")));
            })
            .build();

        Retry retry = Retry.of("adobeSign", config);
        log.info("Adobe Sign retry mechanism configured with {} max attempts", 
                properties.getMaxRetries());
        return retry;
    }

    /**
     * Creates the Adobe Sign Signature Envelope Adapter.
     *
     * @param adobeSignWebClient the Adobe Sign web client
     * @param properties the Adobe Sign adapter properties
     * @param objectMapper the object mapper
     * @param documentContentPort the document content port
     * @param documentPort the document port
     * @param adobeSignCircuitBreaker the circuit breaker
     * @param adobeSignRetry the retry mechanism
     * @return configured AdobeSignSignatureEnvelopeAdapter
     */
    @Bean
    @ConditionalOnMissingBean
    public AdobeSignSignatureEnvelopeAdapter adobeSignSignatureEnvelopeAdapter(
            WebClient adobeSignWebClient,
            AdobeSignAdapterProperties properties,
            ObjectMapper objectMapper,
            DocumentContentPort documentContentPort,
            DocumentPort documentPort,
            CircuitBreaker adobeSignCircuitBreaker,
            Retry adobeSignRetry) {
        log.info("Creating Adobe Sign Signature Envelope Adapter");
        return new AdobeSignSignatureEnvelopeAdapter(
            adobeSignWebClient, 
            properties, 
            objectMapper, 
            documentContentPort, 
            documentPort, 
            adobeSignCircuitBreaker, 
            adobeSignRetry
        );
    }

    /**
     * Creates the Adobe Sign Signature Request Adapter.
     *
     * @param adobeSignWebClient the Adobe Sign web client
     * @param properties the Adobe Sign adapter properties
     * @param objectMapper the object mapper
     * @param adobeSignCircuitBreaker the circuit breaker
     * @param adobeSignRetry the retry mechanism
     * @return configured AdobeSignSignatureRequestAdapter
     */
    @Bean
    @ConditionalOnMissingBean
    public AdobeSignSignatureRequestAdapter adobeSignSignatureRequestAdapter(
            WebClient adobeSignWebClient,
            AdobeSignAdapterProperties properties,
            ObjectMapper objectMapper,
            CircuitBreaker adobeSignCircuitBreaker,
            Retry adobeSignRetry) {
        log.info("Creating Adobe Sign Signature Request Adapter");
        return new AdobeSignSignatureRequestAdapter(
            adobeSignWebClient, 
            properties, 
            objectMapper, 
            adobeSignCircuitBreaker, 
            adobeSignRetry
        );
    }

    /**
     * Creates the Adobe Sign Signature Validation Adapter.
     *
     * @param adobeSignWebClient the Adobe Sign web client
     * @param properties the Adobe Sign adapter properties
     * @param objectMapper the object mapper
     * @param adobeSignCircuitBreaker the circuit breaker
     * @param adobeSignRetry the retry mechanism
     * @return configured AdobeSignSignatureValidationAdapter
     */
    @Bean
    @ConditionalOnMissingBean
    public AdobeSignSignatureValidationAdapter adobeSignSignatureValidationAdapter(
            WebClient adobeSignWebClient,
            AdobeSignAdapterProperties properties,
            ObjectMapper objectMapper,
            CircuitBreaker adobeSignCircuitBreaker,
            Retry adobeSignRetry) {
        log.info("Creating Adobe Sign Signature Validation Adapter");
        return new AdobeSignSignatureValidationAdapter(
            adobeSignWebClient,
            properties,
            objectMapper,
            adobeSignCircuitBreaker,
            adobeSignRetry
        );
    }
}
