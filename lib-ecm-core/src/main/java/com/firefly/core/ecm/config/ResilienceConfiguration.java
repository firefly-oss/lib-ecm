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
package com.firefly.core.ecm.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Predicate;

/**
 * Configuration for resilience patterns in the ECM system.
 * 
 * <p>This configuration provides circuit breakers, retry mechanisms, and time limiters
 * for external service calls to improve system reliability and fault tolerance.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Circuit breaker for external service protection</li>
 *   <li>Exponential backoff retry with jitter</li>
 *   <li>Configurable timeouts for operations</li>
 *   <li>Reactive support for Mono and Flux operations</li>
 * </ul>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "firefly.ecm.resilience.enabled", havingValue = "true", matchIfMissing = true)
public class ResilienceConfiguration {

    /**
     * Circuit breaker for S3 operations.
     */
    @Bean
    public CircuitBreaker s3CircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit if 50% of calls fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before trying again
                .slidingWindowSize(10) // Consider last 10 calls
                .minimumNumberOfCalls(5) // Need at least 5 calls to calculate failure rate
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 calls in half-open state
                .slowCallRateThreshold(50) // Consider slow calls as failures
                .slowCallDurationThreshold(Duration.ofSeconds(10)) // Calls taking >10s are slow
                .recordExceptions(
                    RuntimeException.class,
                    java.net.ConnectException.class,
                    java.net.SocketTimeoutException.class
                )
                .ignoreExceptions(
                    IllegalArgumentException.class
                )
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("s3-operations", config);
        
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.info("S3 Circuit breaker state transition: {} -> {}", 
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState()))
                .onCallNotPermitted(event -> 
                    log.warn("S3 Circuit breaker call not permitted"))
                .onError(event -> 
                    log.error("S3 Circuit breaker recorded error: {}", 
                            event.getThrowable().getMessage()));

        return circuitBreaker;
    }

    /**
     * Circuit breaker for DocuSign operations.
     */
    @Bean
    public CircuitBreaker docuSignCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(60) // DocuSign might be less reliable
                .waitDurationInOpenState(Duration.ofMinutes(1)) // Wait 1 minute
                .slidingWindowSize(15)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(2)
                .slowCallRateThreshold(40)
                .slowCallDurationThreshold(Duration.ofSeconds(15))
                .recordExceptions(
                    RuntimeException.class,
                    java.net.ConnectException.class,
                    java.net.SocketTimeoutException.class,
                    javax.net.ssl.SSLException.class
                )
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("docusign-operations", config);
        
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.info("DocuSign Circuit breaker state transition: {} -> {}", 
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState()))
                .onCallNotPermitted(event -> 
                    log.warn("DocuSign Circuit breaker call not permitted"))
                .onError(event -> 
                    log.error("DocuSign Circuit breaker recorded error: {}", 
                            event.getThrowable().getMessage()));

        return circuitBreaker;
    }

    /**
     * Retry configuration for S3 operations.
     */
    @Bean
    public Retry s3Retry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .waitDuration(Duration.ofSeconds(2)) // Fixed wait duration for simplicity
                .retryOnException(throwable ->
                    throwable instanceof RuntimeException ||
                    throwable instanceof java.net.ConnectException ||
                    throwable instanceof java.net.SocketTimeoutException)
                .ignoreExceptions(
                    IllegalArgumentException.class
                )
                .build();

        Retry retry = Retry.of("s3-operations", config);
        
        retry.getEventPublisher()
                .onRetry(event -> 
                    log.warn("S3 operation retry attempt {} due to: {}", 
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable().getMessage()))
                .onSuccess(event -> 
                    log.debug("S3 operation succeeded after {} attempts", 
                            event.getNumberOfRetryAttempts()));

        return retry;
    }

    /**
     * Retry configuration for DocuSign operations.
     */
    @Bean
    public Retry docuSignRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(4) // More attempts for DocuSign due to potential rate limiting
                .waitDuration(Duration.ofSeconds(2))
                .waitDuration(Duration.ofSeconds(3)) // Fixed wait duration for simplicity
                .retryOnException(throwable ->
                    throwable instanceof RuntimeException ||
                    throwable instanceof java.net.ConnectException ||
                    throwable instanceof java.net.SocketTimeoutException ||
                    (throwable instanceof RuntimeException &&
                     throwable.getMessage() != null && throwable.getMessage().contains("rate limit")))
                .ignoreExceptions(
                    IllegalArgumentException.class
                )
                .build();

        Retry retry = Retry.of("docusign-operations", config);
        
        retry.getEventPublisher()
                .onRetry(event -> 
                    log.warn("DocuSign operation retry attempt {} due to: {}", 
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable().getMessage()))
                .onSuccess(event -> 
                    log.debug("DocuSign operation succeeded after {} attempts", 
                            event.getNumberOfRetryAttempts()));

        return retry;
    }

    /**
     * Time limiter for long-running operations.
     */
    @Bean
    public TimeLimiter operationTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMinutes(5)) // 5 minute timeout
                .cancelRunningFuture(true)
                .build();

        TimeLimiter timeLimiter = TimeLimiter.of("ecm-operations", config);
        
        timeLimiter.getEventPublisher()
                .onTimeout(event ->
                    log.error("Operation timed out"));

        return timeLimiter;
    }

    /**
     * Utility class for applying resilience patterns to reactive operations.
     */
    public static class ReactiveResilience {

        /**
         * Applies circuit breaker and retry to a Mono operation.
         */
        public static <T> Mono<T> withResilience(Mono<T> mono, CircuitBreaker circuitBreaker, Retry retry) {
            return mono
                    .transformDeferred(io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator.of(circuitBreaker))
                    .transformDeferred(io.github.resilience4j.reactor.retry.RetryOperator.of(retry))
                    .doOnError(throwable -> 
                        log.error("Operation failed after all resilience attempts: {}", throwable.getMessage()));
        }

        /**
         * Applies circuit breaker and retry to a Flux operation.
         */
        public static <T> Flux<T> withResilience(Flux<T> flux, CircuitBreaker circuitBreaker, Retry retry) {
            return flux
                    .transformDeferred(io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator.of(circuitBreaker))
                    .transformDeferred(io.github.resilience4j.reactor.retry.RetryOperator.of(retry))
                    .doOnError(throwable -> 
                        log.error("Operation failed after all resilience attempts: {}", throwable.getMessage()));
        }

        /**
         * Applies timeout to a Mono operation.
         */
        public static <T> Mono<T> withTimeout(Mono<T> mono, Duration timeout) {
            return mono
                    .timeout(timeout)
                    .doOnError(java.util.concurrent.TimeoutException.class, 
                        throwable -> log.error("Operation timed out after {}", timeout));
        }

        /**
         * Applies timeout to a Flux operation.
         */
        public static <T> Flux<T> withTimeout(Flux<T> flux, Duration timeout) {
            return flux
                    .timeout(timeout)
                    .doOnError(java.util.concurrent.TimeoutException.class, 
                        throwable -> log.error("Operation timed out after {}", timeout));
        }
    }
}
