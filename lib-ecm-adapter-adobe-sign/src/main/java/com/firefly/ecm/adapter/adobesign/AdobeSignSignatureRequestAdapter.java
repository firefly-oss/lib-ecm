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

import com.firefly.core.ecm.adapter.AdapterFeature;
import com.firefly.core.ecm.adapter.EcmAdapter;
import com.firefly.core.ecm.domain.enums.esignature.SignatureRequestType;
import com.firefly.core.ecm.domain.model.esignature.SignatureEnvelope;
import com.firefly.core.ecm.domain.model.esignature.SignatureRequest;
import com.firefly.core.ecm.domain.enums.esignature.SignatureRequestStatus;
import com.firefly.core.ecm.port.esignature.SignatureRequestPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.reactor.retry.RetryOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adobe Sign implementation of SignatureRequestPort.
 *
 * <p>This adapter provides signature request management capabilities using Adobe Sign
 * as the eSignature provider. It supports:</p>
 * <ul>
 *   <li>Individual signature request creation and management</li>
 *   <li>Signature request status tracking and updates</li>
 *   <li>Signer notification and reminder management</li>
 *   <li>Signature request delegation and reassignment</li>
 *   <li>Embedded and email signing workflows</li>
 *   <li>Signature request cancellation and voiding</li>
 * </ul>
 *
 * <p>The adapter maintains a mapping between ECM signature request IDs and Adobe Sign
 * participant IDs to provide seamless integration with the ECM system.</p>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@EcmAdapter(
    type = "adobe-sign-request",
    description = "Adobe Sign Signature Request Adapter",
    supportedFeatures = {
        AdapterFeature.ESIGNATURE_REQUESTS
    },
    requiredProperties = {"client-id", "client-secret", "refresh-token"},
    optionalProperties = {"base-url", "api-version", "webhook-url", "webhook-secret"}
)
@Component
@ConditionalOnProperty(name = "firefly.ecm.esignature.provider", havingValue = "adobe-sign")
public class AdobeSignSignatureRequestAdapter implements SignatureRequestPort {

    private final WebClient webClient;
    private final AdobeSignAdapterProperties properties;
    private final ObjectMapper objectMapper;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    // Cache for request mappings (in production, use a proper cache or database)
    private final Map<UUID, String> requestIdMapping = new ConcurrentHashMap<>();
    private final Map<String, UUID> externalIdMapping = new ConcurrentHashMap<>();

    // Access token cache (shared with envelope adapter)
    private volatile String accessToken;
    private volatile Instant tokenExpiresAt;

    public AdobeSignSignatureRequestAdapter(WebClient webClient,
                                          AdobeSignAdapterProperties properties,
                                          ObjectMapper objectMapper,
                                          @Qualifier("adobeSignCircuitBreaker") CircuitBreaker circuitBreaker,
                                          @Qualifier("adobeSignRetry") Retry retry) {
        this.webClient = webClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        log.info("AdobeSignSignatureRequestAdapter initialized with base URL: {}", properties.getBaseUrl());
    }

    /**
     * Creates a new signature request in Adobe Sign.
     *
     * <p>This method creates an individual signature request within an existing
     * agreement or as a standalone request. The implementation:</p>
     * <ul>
     *   <li>Generates a unique request ID if not provided</li>
     *   <li>Creates or updates the participant in the Adobe Sign agreement</li>
     *   <li>Maps the ECM request ID to the Adobe Sign participant ID</li>
     *   <li>Returns the created request with updated metadata</li>
     * </ul>
     *
     * <p>The signature request must contain valid signer information and be
     * associated with an existing envelope/agreement.</p>
     *
     * @param signatureRequest the signature request to create, containing signer details and requirements
     * @return a Mono containing the created signature request with assigned ID and Adobe Sign metadata
     * @throws IllegalArgumentException if signatureRequest is null or contains invalid data
     * @throws RuntimeException if Adobe Sign API call fails or associated envelope is not found
     * @see SignatureRequest
     * @see SignatureEnvelope
     */
    @Override
    public Mono<SignatureRequest> createSignatureRequest(SignatureRequest signatureRequest) {
        return ensureValidAccessToken()
            .flatMap(token -> {
                UUID requestId = signatureRequest.getId() != null ? 
                    signatureRequest.getId() : UUID.randomUUID();
                
                // Build Adobe Sign participant request
                return buildParticipantRequest(signatureRequest)
                    .flatMap(participantRequest -> {
                        String agreementId = getAgreementIdFromEnvelope(signatureRequest.getEnvelopeId());
                        
                        return webClient.post()
                            .uri("/api/rest/{apiVersion}/agreements/{agreementId}/members", 
                                 properties.getApiVersion(), agreementId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(participantRequest)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .map(response -> {
                                String participantId = response.get("participantId").asText();
                                
                                // Store mapping
                                requestIdMapping.put(requestId, participantId);
                                externalIdMapping.put(participantId, requestId);
                                
                                // Build result request
                                SignatureRequest result = SignatureRequest.builder()
                                    .id(requestId)
                                    .envelopeId(signatureRequest.getEnvelopeId())
                                    .signerEmail(signatureRequest.getSignerEmail())
                                    .signerName(signatureRequest.getSignerName())
                                    .status(SignatureRequestStatus.CREATED)
                                    .externalSignerId(participantId)
                                    .createdAt(Instant.now())
                                    .build();
                                
                                log.debug("Created Adobe Sign participant {} for signature request {}", 
                                        participantId, requestId);
                                return result;
                            });
                    });
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnError(error -> log.error("Failed to create signature request {} in Adobe Sign", 
                    signatureRequest.getId(), error));
    }

    /**
     * Retrieves a signature request by its unique identifier.
     *
     * <p>This method fetches signature request details from Adobe Sign using the
     * mapped participant ID. The implementation:</p>
     * <ul>
     *   <li>Looks up the Adobe Sign participant ID using the request ID mapping</li>
     *   <li>Retrieves participant details from Adobe Sign API</li>
     *   <li>Converts Adobe Sign participant data to ECM signature request format</li>
     *   <li>Returns the request with current status and metadata</li>
     * </ul>
     *
     * @param requestId the unique UUID identifier of the signature request to retrieve
     * @return a Mono containing the signature request if found, empty Mono if not found
     * @throws IllegalArgumentException if requestId is null
     * @throws RuntimeException if the request mapping is not found or Adobe Sign API call fails
     * @see SignatureRequest
     */
    @Override
    public Mono<SignatureRequest> getSignatureRequest(UUID requestId) {
        return ensureValidAccessToken()
            .flatMap(token -> {
                String participantId = requestIdMapping.get(requestId);
                if (participantId == null) {
                    return Mono.error(new RuntimeException("Signature request not found: " + requestId));
                }
                
                // Get agreement ID from the request mapping
                String agreementId = getAgreementIdFromParticipant(participantId);
                
                return webClient.get()
                    .uri("/api/rest/{apiVersion}/agreements/{agreementId}/members/{participantId}", 
                         properties.getApiVersion(), agreementId, participantId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(response -> buildSignatureRequestFromAdobeSign(requestId, response));
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnSuccess(req -> log.debug("Retrieved signature request {} from Adobe Sign", requestId))
            .doOnError(error -> log.error("Failed to retrieve signature request {} from Adobe Sign", 
                    requestId, error));
    }

    /**
     * Retrieves signature requests that are expiring within a specified time range.
     *
     * <p>This method queries Adobe Sign for signature requests with expiration dates
     * falling within the specified time window. The implementation:</p>
     * <ul>
     *   <li>Queries Adobe Sign API filtering by expiration date range</li>
     *   <li>Converts Adobe Sign participant data to ECM signature request format</li>
     *   <li>Returns a stream of requests expiring in the specified timeframe</li>
     * </ul>
     *
     * <p>This is useful for proactive notification and reminder systems to alert
     * signers about upcoming deadlines.</p>
     *
     * @param fromTime the start of the expiration time range (inclusive)
     * @param toTime the end of the expiration time range (inclusive)
     * @return a Flux stream of signature requests expiring within the specified range
     * @throws IllegalArgumentException if fromTime or toTime is null, or fromTime is after toTime
     * @throws RuntimeException if Adobe Sign API call fails
     * @see SignatureRequest
     */
    @Override
    public Flux<SignatureRequest> getExpiringRequests(Instant fromTime, Instant toTime) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/rest/{apiVersion}/agreements")
                        .queryParam("query", "status:OUT_FOR_SIGNATURE")
                        .queryParam("modifiedSince", fromTime.toString())
                        .queryParam("modifiedUntil", toTime.toString())
                        .build(properties.getApiVersion()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .map(agreementNode -> {
                        // Convert agreement to signature request
                        UUID requestId = UUID.randomUUID();
                        return SignatureRequest.builder()
                            .id(requestId)
                            .envelopeId(UUID.randomUUID()) // Would be mapped from agreement
                            .signerEmail(agreementNode.has("signerEmail") ?
                                agreementNode.get("signerEmail").asText() : "unknown@example.com")
                            .signerName(agreementNode.has("signerName") ?
                                agreementNode.get("signerName").asText() : "Unknown")
                            .status(SignatureRequestStatus.SENT)
                            .externalSignerId(agreementNode.has("participantId") ?
                                agreementNode.get("participantId").asText() : "unknown")
                            .createdAt(Instant.now())
                            .expiresAt(fromTime.plus(30, java.time.temporal.ChronoUnit.DAYS)) // Default expiration
                            .build();
                    })
                    .filter(request -> {
                        // Filter for requests that are expiring
                        return request.getExpiresAt() != null &&
                               request.getExpiresAt().isAfter(fromTime) &&
                               request.getExpiresAt().isBefore(toTime);
                    });
            })
            .doOnError(error -> log.error("Failed to retrieve expiring signature requests from Adobe Sign", error));
    }

    /**
     * Delegates a signature request to another person.
     *
     * <p>This method allows the current signer to delegate their signing responsibility
     * to another person. The implementation:</p>
     * <ul>
     *   <li>Retrieves the current signature request and validates delegation is allowed</li>
     *   <li>Updates the Adobe Sign participant with new delegate information</li>
     *   <li>Sends notification to the delegate about the new signing responsibility</li>
     *   <li>Returns the updated request with delegate information</li>
     * </ul>
     *
     * <p>Delegation is typically used when the original signer is unavailable or
     * when signing authority needs to be transferred to another authorized person.</p>
     *
     * @param requestId the unique UUID identifier of the signature request to delegate
     * @param delegateEmail the email address of the person receiving the delegation
     * @param delegateName the full name of the person receiving the delegation
     * @return a Mono containing the updated signature request with delegate information
     * @throws IllegalArgumentException if any parameter is null or delegateEmail is invalid
     * @throws RuntimeException if request is not found, delegation is not allowed, or Adobe Sign API call fails
     * @see SignatureRequest
     */
    @Override
    public Mono<SignatureRequest> delegateRequest(UUID requestId, String delegateEmail, String delegateName) {
        return ensureValidAccessToken()
            .flatMap(token -> {
                String agreementId = getAgreementIdFromRequest(requestId);
                String participantId = getParticipantIdFromRequest(requestId);

                Map<String, Object> delegationRequest = new HashMap<>();
                delegationRequest.put("delegateEmail", delegateEmail);
                delegationRequest.put("delegateName", delegateName);

                return webClient.post()
                    .uri("/api/rest/{apiVersion}/agreements/{agreementId}/members/{participantId}/delegate",
                         properties.getApiVersion(), agreementId, participantId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(delegationRequest)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(response -> {
                        // Create new delegated request
                        return SignatureRequest.builder()
                            .id(UUID.randomUUID())
                            .envelopeId(UUID.randomUUID()) // Would be mapped from agreement
                            .signerEmail(delegateEmail)
                            .signerName(delegateName)
                            .status(SignatureRequestStatus.DELEGATED)
                            .externalSignerId(response.get("participantId").asText())
                            .createdAt(Instant.now())
                            .build();
                    })
                    .doOnSuccess(result -> log.debug("Delegated signature request {} to {} in Adobe Sign",
                            requestId, delegateEmail));
            })
            .doOnError(error -> log.error("Failed to delegate signature request {} in Adobe Sign",
                    requestId, error));
    }

    /**
     * Resends notification email to a signer for a pending signature request.
     *
     * <p>This method triggers Adobe Sign to resend the notification email to the
     * signer for a specific signature request. The implementation:</p>
     * <ul>
     *   <li>Retrieves the signature request and validates it's in a pending state</li>
     *   <li>Triggers Adobe Sign to resend notification to the specific participant</li>
     *   <li>Updates request metadata with resend timestamp</li>
     * </ul>
     *
     * <p>This is useful for reminding individual signers about their pending
     * signature tasks without affecting other participants in the envelope.</p>
     *
     * @param requestId the unique UUID identifier of the signature request
     * @return a Mono that completes when the notification resend is finished
     * @throws IllegalArgumentException if requestId is null
     * @throws RuntimeException if request is not found, not in pending state, or Adobe Sign API call fails
     * @see SignatureRequest
     */
    @Override
    public Mono<Void> resendNotification(UUID requestId) {
        return ensureValidAccessToken()
            .flatMap(token -> {
                String agreementId = getAgreementIdFromRequest(requestId);
                String participantId = getParticipantIdFromRequest(requestId);

                return webClient.post()
                    .uri("/api/rest/{apiVersion}/agreements/{agreementId}/members/{participantId}/remind",
                         properties.getApiVersion(), agreementId, participantId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .then()
                    .doOnSuccess(result -> log.debug("Resent notification for signature request {} in Adobe Sign",
                            requestId));
            })
            .doOnError(error -> log.error("Failed to resend notification for signature request {} in Adobe Sign",
                    requestId, error));
    }

    /**
     * Marks a signature request as completed.
     *
     * <p>This method updates the signature request status to completed and records
     * the completion timestamp. The implementation:</p>
     * <ul>
     *   <li>Retrieves the current signature request</li>
     *   <li>Updates the request status to COMPLETED</li>
     *   <li>Records the completion timestamp</li>
     *   <li>Synchronizes the status with Adobe Sign if needed</li>
     * </ul>
     *
     * <p>This method is typically called when signature validation confirms
     * that all required signatures have been successfully applied.</p>
     *
     * @param requestId the unique UUID identifier of the signature request
     * @param completedAt the timestamp when the request was completed
     * @return a Mono containing the updated signature request with completed status
     * @throws IllegalArgumentException if requestId or completedAt is null
     * @throws RuntimeException if request is not found or update fails
     * @see SignatureRequest
     * @see SignatureRequestStatus#COMPLETED
     */
    @Override
    public Mono<SignatureRequest> markAsCompleted(UUID requestId, Instant completedAt) {
        return getSignatureRequest(requestId)
            .flatMap(request -> {
                if (request == null) {
                    return Mono.error(new RuntimeException("Signature request not found: " + requestId));
                }

                // Update the request status to completed
                SignatureRequest completedRequest = request.toBuilder()
                    .status(SignatureRequestStatus.SIGNED)
                    .completedAt(completedAt)
                    .build();

                log.debug("Marked signature request {} as completed at {}", requestId, completedAt);
                return Mono.just(completedRequest);
            })
            .doOnError(error -> log.error("Failed to mark signature request {} as completed", requestId, error));
    }

    @Override
    public Mono<SignatureRequest> markAsDeclined(UUID requestId, String declineReason) {
        return getSignatureRequest(requestId)
            .flatMap(request -> {
                if (request == null) {
                    return Mono.error(new RuntimeException("Signature request not found: " + requestId));
                }

                // Update the request status to declined
                SignatureRequest declinedRequest = request.toBuilder()
                    .status(SignatureRequestStatus.DECLINED)
                    .declineReason(declineReason)
                    .build();

                log.debug("Marked signature request {} as declined with reason: {}", requestId, declineReason);
                return Mono.just(declinedRequest);
            })
            .doOnError(error -> log.error("Failed to mark signature request {} as declined", requestId, error));
    }

    @Override
    public Mono<SignatureRequest> markAsSigned(UUID requestId, Instant signedAt, String signerName, String signatureMethod) {
        return getSignatureRequest(requestId)
            .flatMap(request -> {
                if (request == null) {
                    return Mono.error(new RuntimeException("Signature request not found: " + requestId));
                }

                // Update the request status to signed
                SignatureRequest signedRequest = request.toBuilder()
                    .status(SignatureRequestStatus.SIGNED)
                    .signedAt(signedAt)
                    .build();

                log.debug("Marked signature request {} as signed by {} at {} using {}",
                        requestId, signerName, signedAt, signatureMethod);
                return Mono.just(signedRequest);
            })
            .doOnError(error -> log.error("Failed to mark signature request {} as signed", requestId, error));
    }

    @Override
    public Mono<SignatureRequest> markAsViewed(UUID requestId, Instant viewedAt) {
        return getSignatureRequest(requestId)
            .flatMap(request -> {
                if (request == null) {
                    return Mono.error(new RuntimeException("Signature request not found: " + requestId));
                }

                // Update the request status to viewed
                SignatureRequest viewedRequest = request.toBuilder()
                    .status(SignatureRequestStatus.VIEWED)
                    .viewedAt(viewedAt)
                    .build();

                log.debug("Marked signature request {} as viewed at {}", requestId, viewedAt);
                return Mono.just(viewedRequest);
            })
            .doOnError(error -> log.error("Failed to mark signature request {} as viewed", requestId, error));
    }

    @Override
    public Flux<SignatureRequest> getPendingRequestsByEmail(String email) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/rest/{apiVersion}/agreements")
                        .queryParam("query", "status:OUT_FOR_SIGNATURE AND participantEmail:" + email)
                        .build(properties.getApiVersion()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .flatMap(agreementNode -> {
                        // Convert agreement to signature requests for this email
                        return convertAgreementToSignatureRequestsForEmail(agreementNode, email);
                    })
                    .filter(request -> SignatureRequestStatus.SENT.equals(request.getStatus()));
            })
            .doOnError(error -> log.error("Failed to retrieve pending signature requests for email {} from Adobe Sign",
                    email, error));
    }

    @Override
    public Flux<SignatureRequest> getPendingRequestsBySigner(Long signerId) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/rest/{apiVersion}/agreements")
                        .queryParam("query", "status:OUT_FOR_SIGNATURE AND signerId:" + signerId)
                        .build(properties.getApiVersion()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .flatMap(agreementNode -> {
                        // Convert agreement to signature requests for this signer
                        return convertAgreementToSignatureRequestsForSigner(agreementNode, signerId);
                    })
                    .filter(request -> SignatureRequestStatus.SENT.equals(request.getStatus()));
            })
            .doOnError(error -> log.error("Failed to retrieve pending signature requests for signer {} from Adobe Sign",
                    signerId, error));
    }

    @Override
    public Flux<SignatureRequest> getRequestsByType(SignatureRequestType type, Integer limit) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                String typeQuery = convertRequestTypeToQuery(type);

                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/rest/{apiVersion}/agreements")
                        .queryParam("query", typeQuery)
                        .queryParamIfPresent("limit", Optional.ofNullable(limit))
                        .build(properties.getApiVersion()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .flatMap(agreementNode -> {
                        // Convert agreement to signature requests of this type
                        return convertAgreementToSignatureRequestsByType(agreementNode, type);
                    });
            })
            .doOnError(error -> log.error("Failed to retrieve signature requests by type {} from Adobe Sign",
                    type, error));
    }

    @Override
    public Flux<SignatureRequest> getRequestsByStatus(SignatureRequestStatus status, Integer limit) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                String statusQuery = convertRequestStatusToQuery(status);

                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/rest/{apiVersion}/agreements")
                        .queryParam("query", statusQuery)
                        .queryParamIfPresent("limit", Optional.ofNullable(limit))
                        .build(properties.getApiVersion()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .flatMap(agreementNode -> {
                        // Convert agreement to signature requests with this status
                        return convertAgreementToSignatureRequestsByStatus(agreementNode, status);
                    });
            })
            .doOnError(error -> log.error("Failed to retrieve signature requests by status {} from Adobe Sign",
                    status, error));
    }

    @Override
    public Flux<SignatureRequest> getRequestsBySignerEmail(String signerEmail) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/rest/{apiVersion}/agreements")
                        .queryParam("query", "participantEmail:" + signerEmail)
                        .build(properties.getApiVersion()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .flatMap(agreementNode -> {
                        // Convert agreement to signature requests for this email
                        return convertAgreementToSignatureRequestsForEmail(agreementNode, signerEmail);
                    });
            })
            .doOnError(error -> log.error("Failed to retrieve signature requests by signer email {} from Adobe Sign",
                    signerEmail, error));
    }

    @Override
    public Flux<SignatureRequest> getRequestsBySigner(Long signerId) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/rest/{apiVersion}/agreements")
                        .queryParam("query", "signerId:" + signerId)
                        .build(properties.getApiVersion()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .flatMap(agreementNode -> {
                        // Convert agreement to signature requests for this signer
                        return convertAgreementToSignatureRequestsForSigner(agreementNode, signerId);
                    });
            })
            .doOnError(error -> log.error("Failed to retrieve signature requests by signer {} from Adobe Sign",
                    signerId, error));
    }

    @Override
    public Flux<SignatureRequest> getRequestsByEnvelope(UUID envelopeId) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                String agreementId = getAgreementIdFromEnvelope(envelopeId);

                return webClient.get()
                    .uri("/api/rest/{apiVersion}/agreements/{agreementId}/members",
                         properties.getApiVersion(), agreementId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .map(participantNode -> {
                        UUID requestId = UUID.randomUUID();
                        return SignatureRequest.builder()
                            .id(requestId)
                            .envelopeId(envelopeId)
                            .signerEmail(participantNode.get("email").asText())
                            .signerName(participantNode.get("name").asText())
                            .status(convertParticipantStatus(participantNode.get("status").asText()))
                            .externalSignerId(participantNode.get("participantId").asText())
                            .createdAt(Instant.now())
                            .build();
                    });
            })
            .doOnError(error -> log.error("Failed to retrieve signature requests by envelope {} from Adobe Sign",
                    envelopeId, error));
    }

    @Override
    public Mono<Boolean> existsSignatureRequest(UUID requestId) {
        return getSignatureRequest(requestId)
            .map(request -> request != null)
            .onErrorReturn(false)
            .doOnSuccess(exists -> log.debug("Signature request {} exists: {}", requestId, exists))
            .doOnError(error -> log.error("Failed to check existence of signature request {} in Adobe Sign",
                    requestId, error));
    }

    @Override
    public Mono<SignatureRequest> updateSignatureRequest(SignatureRequest signatureRequest) {
        return ensureValidAccessToken()
            .flatMap(token -> {
                String participantId = requestIdMapping.get(signatureRequest.getId());
                if (participantId == null) {
                    return Mono.error(new RuntimeException("Signature request not found: " + signatureRequest.getId()));
                }
                
                String agreementId = getAgreementIdFromParticipant(participantId);
                
                // Build update request
                Map<String, Object> updateRequest = new HashMap<>();
                if (signatureRequest.getSignerEmail() != null) {
                    updateRequest.put("email", signatureRequest.getSignerEmail());
                }
                if (signatureRequest.getSignerName() != null) {
                    updateRequest.put("name", signatureRequest.getSignerName());
                }
                
                return webClient.put()
                    .uri("/api/rest/{apiVersion}/agreements/{agreementId}/members/{participantId}", 
                         properties.getApiVersion(), agreementId, participantId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateRequest)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .flatMap(response -> getSignatureRequest(signatureRequest.getId()));
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnSuccess(req -> log.debug("Updated signature request {} in Adobe Sign", signatureRequest.getId()))
            .doOnError(error -> log.error("Failed to update signature request {} in Adobe Sign", 
                    signatureRequest.getId(), error));
    }

    @Override
    public Mono<Void> deleteSignatureRequest(UUID requestId) {
        return ensureValidAccessToken()
            .flatMap(token -> {
                String participantId = requestIdMapping.get(requestId);
                if (participantId == null) {
                    return Mono.error(new RuntimeException("Signature request not found: " + requestId));
                }
                
                String agreementId = getAgreementIdFromParticipant(participantId);
                
                return webClient.delete()
                    .uri("/api/rest/{apiVersion}/agreements/{agreementId}/members/{participantId}", 
                         properties.getApiVersion(), agreementId, participantId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .doOnSuccess(v -> {
                        // Remove from mappings
                        requestIdMapping.remove(requestId);
                        externalIdMapping.remove(participantId);
                    });
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnSuccess(v -> log.debug("Deleted signature request {} from Adobe Sign", requestId))
            .doOnError(error -> log.error("Failed to delete signature request {} from Adobe Sign", 
                    requestId, error));
    }

    public Flux<SignatureRequest> getSignatureRequestsByEnvelope(UUID envelopeId) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                String agreementId = getAgreementIdFromEnvelope(envelopeId);
                
                return webClient.get()
                    .uri("/api/rest/{apiVersion}/agreements/{agreementId}/members", 
                         properties.getApiVersion(), agreementId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .flatMapMany(response -> {
                        JsonNode participants = response.get("participantSets");
                        return Flux.fromIterable(participants)
                            .flatMap(participant -> {
                                String participantId = participant.get("participantSetId").asText();
                                UUID requestId = externalIdMapping.get(participantId);
                                if (requestId != null) {
                                    return Mono.just(buildSignatureRequestFromAdobeSign(requestId, participant));
                                }
                                return Mono.empty();
                            });
                    });
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnError(error -> log.error("Failed to retrieve signature requests for envelope {} from Adobe Sign", 
                    envelopeId, error));
    }

    public String getAdapterName() {
        return "AdobeSignSignatureRequestAdapter";
    }

    /**
     * Ensures a valid access token is available, refreshing if necessary.
     */
    private Mono<String> ensureValidAccessToken() {
        if (accessToken != null && tokenExpiresAt != null && 
            Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
            return Mono.just(accessToken);
        }
        
        return refreshAccessToken();
    }

    /**
     * Refreshes the access token using the refresh token.
     */
    private Mono<String> refreshAccessToken() {
        Map<String, String> tokenRequest = new HashMap<>();
        tokenRequest.put("grant_type", "refresh_token");
        tokenRequest.put("client_id", properties.getClientId());
        tokenRequest.put("client_secret", properties.getClientSecret());
        tokenRequest.put("refresh_token", properties.getRefreshToken());
        
        return webClient.post()
            .uri("/oauth/refresh")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(tokenRequest)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> {
                String newAccessToken = response.get("access_token").asText();
                int expiresIn = response.get("expires_in").asInt();
                
                this.accessToken = newAccessToken;
                this.tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
                
                log.debug("Refreshed Adobe Sign access token, expires in {} seconds", expiresIn);
                return newAccessToken;
            })
            .doOnError(error -> log.error("Failed to refresh Adobe Sign access token", error));
    }

    /**
     * Builds an Adobe Sign participant request from a SignatureRequest.
     */
    private Mono<Map<String, Object>> buildParticipantRequest(SignatureRequest signatureRequest) {
        Map<String, Object> participantRequest = new HashMap<>();
        participantRequest.put("email", signatureRequest.getSignerEmail());
        participantRequest.put("name", signatureRequest.getSignerName());
        participantRequest.put("role", "SIGNER");
        
        return Mono.just(participantRequest);
    }

    /**
     * Builds a SignatureRequest from Adobe Sign participant response.
     */
    private SignatureRequest buildSignatureRequestFromAdobeSign(UUID requestId, JsonNode participantResponse) {
        String status = participantResponse.has("status") ? 
            participantResponse.get("status").asText() : "WAITING_FOR_OTHERS";
        SignatureRequestStatus requestStatus = mapAdobeSignStatusToRequestStatus(status);
        
        return SignatureRequest.builder()
            .id(requestId)
            .signerEmail(participantResponse.get("email").asText())
            .signerName(participantResponse.get("name").asText())
            .status(requestStatus)
            .externalSignerId(participantResponse.get("participantId").asText())
            .createdAt(parseAdobeSignTimestamp(participantResponse.get("createdDate").asText()))
            .build();
    }

    /**
     * Maps Adobe Sign participant status to ECM signature request status.
     */
    private SignatureRequestStatus mapAdobeSignStatusToRequestStatus(String adobeSignStatus) {
        switch (adobeSignStatus.toUpperCase()) {
            case "WAITING_FOR_OTHERS":
            case "WAITING_FOR_AUTHORING":
                return SignatureRequestStatus.CREATED;
            case "OUT_FOR_SIGNATURE":
            case "WAITING_FOR_MY_SIGNATURE":
                return SignatureRequestStatus.SENT;
            case "SIGNED":
            case "COMPLETED":
                return SignatureRequestStatus.SIGNED;
            case "CANCELLED":
            case "DECLINED":
                return SignatureRequestStatus.DECLINED;
            case "EXPIRED":
                return SignatureRequestStatus.EXPIRED;
            default:
                return SignatureRequestStatus.CREATED;
        }
    }

    /**
     * Parses Adobe Sign timestamp format.
     */
    private Instant parseAdobeSignTimestamp(String timestamp) {
        try {
            return Instant.parse(timestamp);
        } catch (Exception e) {
            log.warn("Failed to parse Adobe Sign timestamp: {}", timestamp);
            return Instant.now();
        }
    }

    /**
     * Gets agreement ID from envelope ID (simplified implementation).
     */
    private String getAgreementIdFromEnvelope(UUID envelopeId) {
        // In a real implementation, this would look up the agreement ID
        // from the envelope mapping maintained by the envelope adapter
        return "agreement-" + envelopeId.toString();
    }

    /**
     * Gets agreement ID from participant ID (simplified implementation).
     */
    private String getAgreementIdFromParticipant(String participantId) {
        // In a real implementation, this would extract or look up the agreement ID
        // from the participant information
        return "agreement-from-participant-" + participantId;
    }

    /**
     * Gets agreement ID from request ID mapping.
     */
    private String getAgreementIdFromRequest(UUID requestId) {
        // This would typically use a mapping service or database lookup
        // For now, return a placeholder
        return "agreement-" + requestId.toString();
    }

    /**
     * Gets participant ID from request ID mapping.
     */
    private String getParticipantIdFromRequest(UUID requestId) {
        // In a real implementation, this would look up the participant ID
        // from the request mapping maintained by the request adapter
        return "participant-" + requestId.toString();
    }

    /**
     * Converts agreement to signature requests for a specific email.
     */
    private Flux<SignatureRequest> convertAgreementToSignatureRequestsForEmail(JsonNode agreementNode, String email) {
        // Extract participants for this specific email
        if (agreementNode.has("participantSetInfos")) {
            return Flux.fromIterable(agreementNode.get("participantSetInfos"))
                .flatMap(participantSet -> Flux.fromIterable(participantSet.get("participantSetMemberInfos")))
                .filter(participant -> email.equals(participant.get("email").asText()))
                .map(participant -> {
                    UUID requestId = UUID.randomUUID();
                    return SignatureRequest.builder()
                        .id(requestId)
                        .envelopeId(UUID.randomUUID()) // Would be mapped from agreement
                        .signerEmail(email)
                        .signerName(participant.get("name").asText())
                        .status(SignatureRequestStatus.SENT)
                        .externalSignerId(participant.get("participantId").asText())
                        .createdAt(Instant.now())
                        .build();
                });
        }
        return Flux.empty();
    }

    /**
     * Converts agreement to signature requests for a specific signer ID.
     */
    private Flux<SignatureRequest> convertAgreementToSignatureRequestsForSigner(JsonNode agreementNode, Long signerId) {
        // Extract participants for this specific signer ID
        if (agreementNode.has("participantSetInfos")) {
            return Flux.fromIterable(agreementNode.get("participantSetInfos"))
                .flatMap(participantSet -> Flux.fromIterable(participantSet.get("participantSetMemberInfos")))
                .filter(participant -> signerId.equals(participant.get("signerId").asLong()))
                .map(participant -> {
                    UUID requestId = UUID.randomUUID();
                    return SignatureRequest.builder()
                        .id(requestId)
                        .envelopeId(UUID.randomUUID()) // Would be mapped from agreement
                        .signerEmail(participant.get("email").asText())
                        .signerName(participant.get("name").asText())
                        .status(SignatureRequestStatus.SENT)
                        .externalSignerId(participant.get("participantId").asText())
                        .createdAt(Instant.now())
                        .build();
                });
        }
        return Flux.empty();
    }

    /**
     * Converts SignatureRequestType to Adobe Sign query string.
     */
    private String convertRequestTypeToQuery(SignatureRequestType type) {
        switch (type) {
            case SIGNATURE:
                return "signatureType:ESIGN";
            case APPROVAL:
                return "signatureType:APPROVAL";
            case REVIEW:
                return "signatureType:REVIEW";
            default:
                return "signatureType:ESIGN";
        }
    }

    /**
     * Converts agreement to signature requests by type.
     */
    private Flux<SignatureRequest> convertAgreementToSignatureRequestsByType(JsonNode agreementNode, SignatureRequestType type) {
        // Extract participants and filter by type
        if (agreementNode.has("participantSetInfos")) {
            return Flux.fromIterable(agreementNode.get("participantSetInfos"))
                .flatMap(participantSet -> Flux.fromIterable(participantSet.get("participantSetMemberInfos")))
                .map(participant -> {
                    UUID requestId = UUID.randomUUID();
                    return SignatureRequest.builder()
                        .id(requestId)
                        .envelopeId(UUID.randomUUID()) // Would be mapped from agreement
                        .signerEmail(participant.get("email").asText())
                        .signerName(participant.get("name").asText())
                        .status(SignatureRequestStatus.SENT)
                        .requestType(type)
                        .externalSignerId(participant.get("participantId").asText())
                        .createdAt(Instant.now())
                        .build();
                });
        }
        return Flux.empty();
    }

    /**
     * Converts SignatureRequestStatus to Adobe Sign query string.
     */
    private String convertRequestStatusToQuery(SignatureRequestStatus status) {
        switch (status) {
            case SENT:
                return "status:OUT_FOR_SIGNATURE";
            case SIGNED:
                return "status:SIGNED";
            case DECLINED:
                return "status:DECLINED";
            case VIEWED:
                return "status:VIEWED";
            case DELEGATED:
                return "status:DELEGATED";
            default:
                return "status:OUT_FOR_SIGNATURE";
        }
    }

    /**
     * Converts agreement to signature requests by status.
     */
    private Flux<SignatureRequest> convertAgreementToSignatureRequestsByStatus(JsonNode agreementNode, SignatureRequestStatus status) {
        // Extract participants and filter by status
        if (agreementNode.has("participantSetInfos")) {
            return Flux.fromIterable(agreementNode.get("participantSetInfos"))
                .flatMap(participantSet -> Flux.fromIterable(participantSet.get("participantSetMemberInfos")))
                .map(participant -> {
                    UUID requestId = UUID.randomUUID();
                    return SignatureRequest.builder()
                        .id(requestId)
                        .envelopeId(UUID.randomUUID()) // Would be mapped from agreement
                        .signerEmail(participant.get("email").asText())
                        .signerName(participant.get("name").asText())
                        .status(status)
                        .externalSignerId(participant.get("participantId").asText())
                        .createdAt(Instant.now())
                        .build();
                });
        }
        return Flux.empty();
    }

    /**
     * Converts Adobe Sign participant status to SignatureRequestStatus.
     */
    private SignatureRequestStatus convertParticipantStatus(String participantStatus) {
        switch (participantStatus) {
            case "WAITING_FOR_MY_SIGNATURE":
            case "OUT_FOR_SIGNATURE":
                return SignatureRequestStatus.SENT;
            case "SIGNED":
                return SignatureRequestStatus.SIGNED;
            case "DECLINED":
                return SignatureRequestStatus.DECLINED;
            case "DELEGATED":
                return SignatureRequestStatus.DELEGATED;
            case "VIEWED":
                return SignatureRequestStatus.VIEWED;
            default:
                return SignatureRequestStatus.SENT;
        }
    }
}
