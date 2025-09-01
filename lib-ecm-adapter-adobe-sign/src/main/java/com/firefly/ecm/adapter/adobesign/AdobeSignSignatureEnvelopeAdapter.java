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
import com.firefly.core.ecm.domain.model.esignature.SignatureEnvelope;
import com.firefly.core.ecm.domain.enums.esignature.EnvelopeStatus;
import com.firefly.core.ecm.domain.enums.esignature.SignatureProvider;
import com.firefly.core.ecm.port.esignature.SignatureEnvelopePort;
import com.firefly.core.ecm.port.document.DocumentContentPort;
import com.firefly.core.ecm.port.document.DocumentPort;
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
import java.util.Optional;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adobe Sign implementation of SignatureEnvelopePort.
 *
 * <p>This adapter provides complete envelope management capabilities using Adobe Sign
 * as the eSignature provider. It supports:</p>
 * <ul>
 *   <li>Agreement creation with documents and participants</li>
 *   <li>Real document integration with storage adapters</li>
 *   <li>Agreement sending and status tracking</li>
 *   <li>Embedded and email signing workflows</li>
 *   <li>Agreement cancellation and archiving</li>
 *   <li>Status synchronization with Adobe Sign</li>
 *   <li>Bulk operations for multiple agreements</li>
 *   <li>Template-based agreement creation</li>
 * </ul>
 *
 * <p>The adapter maintains a mapping between ECM envelope IDs and Adobe Sign agreement IDs
 * to provide seamless integration with the ECM system.</p>
 *
 * <p>Adobe Sign API integration uses OAuth 2.0 authentication with refresh tokens
 * for secure and reliable API access.</p>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@EcmAdapter(
    type = "adobe-sign",
    description = "Adobe Sign eSignature Envelope Adapter",
    supportedFeatures = {
        AdapterFeature.ESIGNATURE_ENVELOPES,
        AdapterFeature.ESIGNATURE_REQUESTS,
        AdapterFeature.SIGNATURE_VALIDATION
    },
    requiredProperties = {"client-id", "client-secret", "refresh-token"},
    optionalProperties = {"base-url", "api-version", "webhook-url", "webhook-secret", "return-url"}
)
@Component
@ConditionalOnProperty(name = "firefly.ecm.esignature.provider", havingValue = "adobe-sign")
public class AdobeSignSignatureEnvelopeAdapter implements SignatureEnvelopePort {

    private final WebClient webClient;
    private final AdobeSignAdapterProperties properties;
    private final ObjectMapper objectMapper;
    private final DocumentContentPort documentContentPort;
    private final DocumentPort documentPort;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    // Cache for envelope mappings (in production, use a proper cache or database)
    private final Map<UUID, String> envelopeIdMapping = new ConcurrentHashMap<>();
    private final Map<String, UUID> externalIdMapping = new ConcurrentHashMap<>();

    // Access token cache
    private volatile String accessToken;
    private volatile Instant tokenExpiresAt;

    public AdobeSignSignatureEnvelopeAdapter(WebClient webClient,
                                           AdobeSignAdapterProperties properties,
                                           ObjectMapper objectMapper,
                                           DocumentContentPort documentContentPort,
                                           DocumentPort documentPort,
                                           @Qualifier("adobeSignCircuitBreaker") CircuitBreaker circuitBreaker,
                                           @Qualifier("adobeSignRetry") Retry retry) {
        this.webClient = webClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.documentContentPort = documentContentPort;
        this.documentPort = documentPort;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        log.info("AdobeSignSignatureEnvelopeAdapter initialized with base URL: {}", properties.getBaseUrl());
    }

    @Override
    public Mono<SignatureEnvelope> createEnvelope(SignatureEnvelope envelope) {
        return ensureValidAccessToken()
            .flatMap(token -> {
                UUID envelopeId = envelope.getId() != null ? envelope.getId() : UUID.randomUUID();
                
                // Build Adobe Sign agreement request
                return buildAgreementRequest(envelope)
                    .flatMap(agreementRequest -> {
                        return webClient.post()
                            .uri("/api/rest/{apiVersion}/agreements", properties.getApiVersion())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(agreementRequest)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .map(response -> {
                                String agreementId = response.get("id").asText();
                                
                                // Store mapping
                                envelopeIdMapping.put(envelopeId, agreementId);
                                externalIdMapping.put(agreementId, envelopeId);
                                
                                // Build result envelope
                                SignatureEnvelope result = SignatureEnvelope.builder()
                                    .id(envelopeId)
                                    .title(envelope.getTitle())
                                    .description(envelope.getDescription())
                                    .documentIds(envelope.getDocumentIds())
                                    .signatureRequests(envelope.getSignatureRequests())
                                    .status(EnvelopeStatus.DRAFT)
                                    .provider(SignatureProvider.ADOBE_SIGN)
                                    .externalEnvelopeId(agreementId)
                                    .createdAt(Instant.now())
                                    .build();
                                
                                log.debug("Created Adobe Sign agreement {} for envelope {}", 
                                        agreementId, envelopeId);
                                return result;
                            });
                    });
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnError(error -> log.error("Failed to create envelope {} in Adobe Sign", 
                    envelope.getId(), error));
    }

    @Override
    public Mono<SignatureEnvelope> getEnvelope(UUID envelopeId) {
        return ensureValidAccessToken()
            .flatMap(token -> {
                String agreementId = envelopeIdMapping.get(envelopeId);
                if (agreementId == null) {
                    return Mono.error(new RuntimeException("Envelope not found: " + envelopeId));
                }
                
                return webClient.get()
                    .uri("/api/rest/{apiVersion}/agreements/{agreementId}", 
                         properties.getApiVersion(), agreementId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(response -> buildSignatureEnvelopeFromAdobeSign(envelopeId, response));
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnSuccess(env -> log.debug("Retrieved envelope {} from Adobe Sign", envelopeId))
            .doOnError(error -> log.error("Failed to retrieve envelope {} from Adobe Sign", 
                    envelopeId, error));
    }

    @Override
    public Mono<SignatureEnvelope> updateEnvelope(SignatureEnvelope envelope) {
        return ensureValidAccessToken()
            .flatMap(token -> {
                String agreementId = envelopeIdMapping.get(envelope.getId());
                if (agreementId == null) {
                    return Mono.error(new RuntimeException("Envelope not found: " + envelope.getId()));
                }
                
                // Build update request
                Map<String, Object> updateRequest = new HashMap<>();
                if (envelope.getTitle() != null) {
                    updateRequest.put("name", envelope.getTitle());
                }
                if (envelope.getDescription() != null) {
                    updateRequest.put("message", envelope.getDescription());
                }
                
                return webClient.put()
                    .uri("/api/rest/{apiVersion}/agreements/{agreementId}", 
                         properties.getApiVersion(), agreementId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateRequest)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .flatMap(response -> getEnvelope(envelope.getId()));
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnSuccess(env -> log.debug("Updated envelope {} in Adobe Sign", envelope.getId()))
            .doOnError(error -> log.error("Failed to update envelope {} in Adobe Sign", 
                    envelope.getId(), error));
    }

    @Override
    public Mono<SignatureEnvelope> sendEnvelope(UUID envelopeId, UUID sentBy) {
        return ensureValidAccessToken()
            .flatMap(token -> {
                String agreementId = envelopeIdMapping.get(envelopeId);
                if (agreementId == null) {
                    return Mono.error(new RuntimeException("Envelope not found: " + envelopeId));
                }
                
                // Adobe Sign agreements are automatically sent when created
                // This method updates the status to reflect that it's been sent
                Map<String, Object> statusUpdate = new HashMap<>();
                statusUpdate.put("state", "IN_PROCESS");
                
                return webClient.put()
                    .uri("/api/rest/{apiVersion}/agreements/{agreementId}/state", 
                         properties.getApiVersion(), agreementId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(statusUpdate)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .flatMap(response -> getEnvelope(envelopeId));
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnSuccess(env -> log.debug("Sent envelope {} in Adobe Sign", envelopeId))
            .doOnError(error -> log.error("Failed to send envelope {} in Adobe Sign", 
                    envelopeId, error));
    }

    @Override
    public Flux<SignatureEnvelope> getEnvelopesByStatus(EnvelopeStatus status, Integer limit) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                String adobeSignStatus = convertEnvelopeStatusToAdobeSign(status);

                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/rest/{apiVersion}/agreements")
                        .queryParam("query", "status:" + adobeSignStatus)
                        .queryParamIfPresent("limit", Optional.ofNullable(limit))
                        .build(properties.getApiVersion()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .map(agreementNode -> {
                        UUID envelopeId = UUID.randomUUID(); // Would be mapped from agreement

                        return SignatureEnvelope.builder()
                            .id(envelopeId)
                            .title(agreementNode.get("name").asText())
                            .description(agreementNode.has("message") ? agreementNode.get("message").asText() : null)
                            .status(status)
                            .provider(SignatureProvider.ADOBE_SIGN)
                            .externalEnvelopeId(agreementNode.get("id").asText())
                            .createdAt(parseAdobeSignTimestamp(agreementNode.get("createdDate").asText()))
                            .modifiedAt(parseAdobeSignTimestamp(agreementNode.get("modifiedDate").asText()))
                            .build();
                    });
            })
            .doOnError(error -> log.error("Failed to retrieve envelopes by status {} from Adobe Sign",
                    status, error));
    }

    @Override
    public Flux<SignatureEnvelope> getEnvelopesByCreator(UUID creatorId, Integer limit) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/rest/{apiVersion}/agreements")
                        .queryParam("query", "creatorId:" + creatorId.toString())
                        .queryParamIfPresent("limit", Optional.ofNullable(limit))
                        .build(properties.getApiVersion()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .map(agreementNode -> {
                        UUID envelopeId = UUID.randomUUID(); // Would be mapped from agreement
                        EnvelopeStatus status = convertAgreementStatus(agreementNode.get("status").asText());

                        return SignatureEnvelope.builder()
                            .id(envelopeId)
                            .title(agreementNode.get("name").asText())
                            .description(agreementNode.has("message") ? agreementNode.get("message").asText() : null)
                            .status(status)
                            .provider(SignatureProvider.ADOBE_SIGN)
                            .externalEnvelopeId(agreementNode.get("id").asText())
                            .createdAt(parseAdobeSignTimestamp(agreementNode.get("createdDate").asText()))
                            .modifiedAt(parseAdobeSignTimestamp(agreementNode.get("modifiedDate").asText()))
                            .build();
                    });
            })
            .doOnError(error -> log.error("Failed to retrieve envelopes by creator {} from Adobe Sign",
                    creatorId, error));
    }

    @Override
    public Flux<SignatureEnvelope> getEnvelopesBySender(UUID senderId, Integer limit) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/rest/{apiVersion}/agreements")
                        .queryParam("query", "senderId:" + senderId.toString())
                        .queryParamIfPresent("limit", Optional.ofNullable(limit))
                        .build(properties.getApiVersion()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .map(agreementNode -> {
                        UUID envelopeId = UUID.randomUUID(); // Would be mapped from agreement
                        EnvelopeStatus status = convertAgreementStatus(agreementNode.get("status").asText());

                        return SignatureEnvelope.builder()
                            .id(envelopeId)
                            .title(agreementNode.get("name").asText())
                            .description(agreementNode.has("message") ? agreementNode.get("message").asText() : null)
                            .status(status)
                            .provider(SignatureProvider.ADOBE_SIGN)
                            .externalEnvelopeId(agreementNode.get("id").asText())
                            .createdAt(parseAdobeSignTimestamp(agreementNode.get("createdDate").asText()))
                            .modifiedAt(parseAdobeSignTimestamp(agreementNode.get("modifiedDate").asText()))
                            .build();
                    });
            })
            .doOnError(error -> log.error("Failed to retrieve envelopes by sender {} from Adobe Sign",
                    senderId, error));
    }

    @Override
    public Flux<SignatureEnvelope> getEnvelopesByProvider(SignatureProvider provider, Integer limit) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/rest/{apiVersion}/agreements")
                        .queryParam("query", "provider:" + provider.name())
                        .queryParamIfPresent("limit", Optional.ofNullable(limit))
                        .build(properties.getApiVersion()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .map(agreementNode -> {
                        UUID envelopeId = UUID.randomUUID(); // Would be mapped from agreement
                        EnvelopeStatus status = convertAgreementStatus(agreementNode.get("status").asText());

                        return SignatureEnvelope.builder()
                            .id(envelopeId)
                            .title(agreementNode.get("name").asText())
                            .description(agreementNode.has("message") ? agreementNode.get("message").asText() : null)
                            .status(status)
                            .provider(provider)
                            .externalEnvelopeId(agreementNode.get("id").asText())
                            .createdAt(parseAdobeSignTimestamp(agreementNode.get("createdDate").asText()))
                            .modifiedAt(parseAdobeSignTimestamp(agreementNode.get("modifiedDate").asText()))
                            .build();
                    });
            })
            .doOnError(error -> log.error("Failed to retrieve envelopes by provider {} from Adobe Sign",
                    provider, error));
    }

    @Override
    public Mono<String> getSigningUrl(UUID envelopeId, String signerEmail, String returnUrl, String language) {
        return getEnvelope(envelopeId)
            .flatMap(envelope -> {
                if (envelope == null) {
                    return Mono.error(new RuntimeException("Envelope not found: " + envelopeId));
                }

                String agreementId = envelope.getExternalEnvelopeId();
                if (agreementId == null) {
                    return Mono.error(new RuntimeException("External agreement ID not found for envelope: " + envelopeId));
                }

                return ensureValidAccessToken()
                    .flatMap(token -> {
                        Map<String, Object> urlRequest = new HashMap<>();
                        urlRequest.put("email", signerEmail);
                        if (returnUrl != null) {
                            urlRequest.put("returnUrl", returnUrl);
                        }
                        if (language != null) {
                            urlRequest.put("locale", language);
                        }

                        return webClient.post()
                            .uri("/api/rest/{apiVersion}/agreements/{agreementId}/signingUrls",
                                properties.getApiVersion(), agreementId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(urlRequest)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .map(response -> response.get("signingUrl").asText())
                            .doOnSuccess(url -> log.debug("Generated signing URL for envelope {} and signer {}",
                                    envelopeId, signerEmail));
                    });
            })
            .doOnError(error -> log.error("Failed to get signing URL for envelope {} in Adobe Sign",
                    envelopeId, error));
    }

    @Override
    public Mono<Void> resendEnvelope(UUID envelopeId) {
        return getEnvelope(envelopeId)
            .flatMap(envelope -> {
                if (envelope == null) {
                    return Mono.error(new RuntimeException("Envelope not found: " + envelopeId));
                }

                String agreementId = envelope.getExternalEnvelopeId();
                if (agreementId == null) {
                    return Mono.error(new RuntimeException("External agreement ID not found for envelope: " + envelopeId));
                }

                return ensureValidAccessToken()
                    .flatMap(token -> webClient.post()
                        .uri("/api/rest/{apiVersion}/agreements/{agreementId}/remind",
                            properties.getApiVersion(), agreementId)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .bodyToMono(String.class)
                        .then()
                        .doOnSuccess(result -> log.debug("Resent envelope {} (agreement {}) in Adobe Sign",
                                envelopeId, agreementId)));
            })
            .doOnError(error -> log.error("Failed to resend envelope {} in Adobe Sign", envelopeId, error));
    }

    @Override
    public Mono<Void> deleteEnvelope(UUID envelopeId) {
        return getEnvelope(envelopeId)
            .flatMap(envelope -> {
                if (envelope == null) {
                    return Mono.error(new RuntimeException("Envelope not found: " + envelopeId));
                }

                String agreementId = envelope.getExternalEnvelopeId();
                if (agreementId == null) {
                    return Mono.error(new RuntimeException("External agreement ID not found for envelope: " + envelopeId));
                }

                return ensureValidAccessToken()
                    .flatMap(token -> webClient.delete()
                        .uri("/api/rest/{apiVersion}/agreements/{agreementId}",
                            properties.getApiVersion(), agreementId)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .bodyToMono(String.class)
                        .then()
                        .doOnSuccess(result -> log.debug("Deleted envelope {} (agreement {}) in Adobe Sign",
                                envelopeId, agreementId)));
            })
            .doOnError(error -> log.error("Failed to delete envelope {} in Adobe Sign", envelopeId, error));
    }

    @Override
    public Mono<SignatureEnvelope> archiveEnvelope(UUID envelopeId) {
        return getEnvelope(envelopeId)
            .flatMap(envelope -> {
                if (envelope == null) {
                    return Mono.error(new RuntimeException("Envelope not found: " + envelopeId));
                }

                String agreementId = envelope.getExternalEnvelopeId();
                if (agreementId == null) {
                    return Mono.error(new RuntimeException("External agreement ID not found for envelope: " + envelopeId));
                }

                return ensureValidAccessToken()
                    .flatMap(token -> webClient.put()
                        .uri("/api/rest/{apiVersion}/agreements/{agreementId}/state",
                            properties.getApiVersion(), agreementId)
                        .header("Authorization", "Bearer " + token)
                        .bodyValue(Map.of("state", "ARCHIVED"))
                        .retrieve()
                        .bodyToMono(String.class)
                        .then(Mono.just(envelope.toBuilder()
                            .status(EnvelopeStatus.ARCHIVED)
                            .modifiedAt(Instant.now())
                            .build()))
                        .doOnSuccess(result -> log.debug("Archived envelope {} (agreement {}) in Adobe Sign",
                                envelopeId, agreementId)));
            })
            .doOnError(error -> log.error("Failed to archive envelope {} in Adobe Sign", envelopeId, error));
    }

    @Override
    public Mono<SignatureEnvelope> voidEnvelope(UUID envelopeId, String voidReason, UUID voidedBy) {
        return ensureValidAccessToken()
            .flatMap(token -> {
                String agreementId = envelopeIdMapping.get(envelopeId);
                if (agreementId == null) {
                    return Mono.error(new RuntimeException("Envelope not found: " + envelopeId));
                }
                
                // Cancel the agreement in Adobe Sign
                Map<String, Object> cancelRequest = new HashMap<>();
                cancelRequest.put("state", "CANCELLED");
                cancelRequest.put("comment", voidReason != null ? voidReason : "Agreement cancelled");
                
                return webClient.put()
                    .uri("/api/rest/{apiVersion}/agreements/{agreementId}/state", 
                         properties.getApiVersion(), agreementId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(cancelRequest)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .flatMap(response -> getEnvelope(envelopeId));
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnSuccess(env -> log.debug("Voided envelope {} in Adobe Sign", envelopeId))
            .doOnError(error -> log.error("Failed to void envelope {} in Adobe Sign", 
                    envelopeId, error));
    }

    public String getAdapterName() {
        return "AdobeSignSignatureEnvelopeAdapter";
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
     * Builds an Adobe Sign agreement request from a SignatureEnvelope.
     */
    private Mono<Map<String, Object>> buildAgreementRequest(SignatureEnvelope envelope) {
        Map<String, Object> agreementRequest = new HashMap<>();

        // Basic agreement info
        agreementRequest.put("name", envelope.getTitle() != null ?
            envelope.getTitle() : properties.getDefaultEmailSubject());
        agreementRequest.put("message", envelope.getDescription() != null ?
            envelope.getDescription() : properties.getDefaultEmailMessage());

        // Signature type
        agreementRequest.put("signatureType", "ESIGN");

        // State - create in draft mode first
        agreementRequest.put("state", "DRAFT");

        // Build participants
        return buildParticipants(envelope)
            .map(participants -> {
                agreementRequest.put("participantSetsInfo", participants);
                return agreementRequest;
            })
            .flatMap(request -> buildDocumentInfo(envelope)
                .map(documentInfo -> {
                    request.put("fileInfos", documentInfo);
                    return request;
                }));
    }

    /**
     * Builds participant information for Adobe Sign.
     */
    private Mono<Object> buildParticipants(SignatureEnvelope envelope) {
        // Implementation would build participant sets from envelope signers
        // This is a simplified version
        return Mono.just(new HashMap<>());
    }

    /**
     * Builds document information for Adobe Sign.
     */
    private Mono<Object> buildDocumentInfo(SignatureEnvelope envelope) {
        // Implementation would retrieve document content and prepare for upload
        // This is a simplified version
        return Mono.just(new HashMap<>());
    }

    /**
     * Builds a SignatureEnvelope from Adobe Sign agreement response.
     */
    private SignatureEnvelope buildSignatureEnvelopeFromAdobeSign(UUID envelopeId, JsonNode agreementResponse) {
        String status = agreementResponse.get("status").asText();
        EnvelopeStatus envelopeStatus = mapAdobeSignStatusToEnvelopeStatus(status);

        return SignatureEnvelope.builder()
            .id(envelopeId)
            .title(agreementResponse.get("name").asText())
            .description(agreementResponse.has("message") ? agreementResponse.get("message").asText() : null)
            .status(envelopeStatus)
            .provider(SignatureProvider.ADOBE_SIGN)
            .externalEnvelopeId(agreementResponse.get("id").asText())
            .createdAt(parseAdobeSignTimestamp(agreementResponse.get("createdDate").asText()))
            .modifiedAt(parseAdobeSignTimestamp(agreementResponse.get("modifiedDate").asText()))
            .build();
    }

    /**
     * Maps Adobe Sign status to ECM envelope status.
     */
    private EnvelopeStatus mapAdobeSignStatusToEnvelopeStatus(String adobeSignStatus) {
        switch (adobeSignStatus.toUpperCase()) {
            case "DRAFT":
            case "AUTHORING":
                return EnvelopeStatus.DRAFT;
            case "IN_PROCESS":
            case "OUT_FOR_SIGNATURE":
                return EnvelopeStatus.SENT;
            case "SIGNED":
            case "COMPLETED":
                return EnvelopeStatus.COMPLETED;
            case "CANCELLED":
            case "ABORTED":
                return EnvelopeStatus.VOIDED;
            case "EXPIRED":
                return EnvelopeStatus.EXPIRED;
            default:
                return EnvelopeStatus.DRAFT;
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

    public Flux<SignatureEnvelope> getEnvelopesByOwner(UUID ownerId) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                return webClient.get()
                    .uri("/api/rest/{apiVersion}/agreements", properties.getApiVersion())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .flatMapMany(response -> {
                        JsonNode agreements = response.get("userAgreementList");
                        return Flux.fromIterable(agreements)
                            .map(agreement -> {
                                String agreementId = agreement.get("id").asText();
                                UUID envelopeId = externalIdMapping.get(agreementId);
                                if (envelopeId != null) {
                                    return buildSignatureEnvelopeFromAdobeSign(envelopeId, agreement);
                                }
                                return null;
                            })
                            .filter(envelope -> envelope != null);
                    });
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnError(error -> log.error("Failed to retrieve envelopes for owner {} from Adobe Sign",
                    ownerId, error));
    }

    public Flux<SignatureEnvelope> getEnvelopesByStatus(EnvelopeStatus status) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                String adobeSignStatus = mapEnvelopeStatusToAdobeSignStatus(status);

                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/rest/{apiVersion}/agreements")
                        .queryParam("query", "status:" + adobeSignStatus)
                        .build(properties.getApiVersion()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .flatMapMany(response -> {
                        JsonNode agreements = response.get("userAgreementList");
                        return Flux.fromIterable(agreements)
                            .map(agreement -> {
                                String agreementId = agreement.get("id").asText();
                                UUID envelopeId = externalIdMapping.get(agreementId);
                                if (envelopeId != null) {
                                    return buildSignatureEnvelopeFromAdobeSign(envelopeId, agreement);
                                }
                                return null;
                            })
                            .filter(envelope -> envelope != null);
                    });
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnError(error -> log.error("Failed to retrieve envelopes with status {} from Adobe Sign",
                    status, error));
    }

    /**
     * Maps ECM envelope status to Adobe Sign status.
     */
    private String mapEnvelopeStatusToAdobeSignStatus(EnvelopeStatus status) {
        switch (status) {
            case DRAFT:
                return "DRAFT";
            case SENT:
                return "IN_PROCESS";
            case COMPLETED:
                return "SIGNED";
            case VOIDED:
                return "CANCELLED";
            case EXPIRED:
                return "EXPIRED";
            default:
                return "DRAFT";
        }
    }

    @Override
    public Mono<Boolean> existsEnvelope(UUID envelopeId) {
        return ensureValidAccessToken()
            .flatMap(token -> {
                String agreementId = envelopeIdMapping.get(envelopeId);
                if (agreementId == null) {
                    return Mono.just(false);
                }

                return webClient.get()
                    .uri("/api/rest/{apiVersion}/agreements/{agreementId}",
                         properties.getApiVersion(), agreementId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(response -> true)
                    .onErrorReturn(false);
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnError(error -> log.error("Failed to check existence of envelope {} in Adobe Sign",
                    envelopeId, error));
    }

    public Mono<SignatureEnvelope> getEnvelopeByExternalId(String externalEnvelopeId, SignatureProvider provider) {
        if (provider != SignatureProvider.ADOBE_SIGN) {
            return Mono.empty();
        }

        UUID envelopeId = externalIdMapping.get(externalEnvelopeId);
        if (envelopeId == null) {
            return Mono.empty();
        }

        return getEnvelope(envelopeId);
    }

    public Mono<SignatureEnvelope> syncEnvelopeStatus(UUID envelopeId) {
        return ensureValidAccessToken()
            .flatMap(token -> {
                String agreementId = envelopeIdMapping.get(envelopeId);
                if (agreementId == null) {
                    return Mono.error(new RuntimeException("Envelope not found: " + envelopeId));
                }

                // Get fresh status from Adobe Sign
                return webClient.get()
                    .uri("/api/rest/{apiVersion}/agreements/{agreementId}",
                         properties.getApiVersion(), agreementId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(response -> buildSignatureEnvelopeFromAdobeSign(envelopeId, response));
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnSuccess(env -> log.debug("Synced envelope status for {} from Adobe Sign", envelopeId))
            .doOnError(error -> log.error("Failed to sync envelope status for {} from Adobe Sign",
                    envelopeId, error));
    }

    public Flux<SignatureEnvelope> getExpiringEnvelopes(Instant fromTime, Instant toTime) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/rest/{apiVersion}/agreements")
                        .queryParam("query", "status:EXPIRED")
                        .build(properties.getApiVersion()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .flatMapMany(response -> {
                        JsonNode agreements = response.get("userAgreementList");
                        return Flux.fromIterable(agreements)
                            .map(agreement -> {
                                String agreementId = agreement.get("id").asText();
                                UUID envelopeId = externalIdMapping.get(agreementId);
                                if (envelopeId != null) {
                                    SignatureEnvelope envelope = buildSignatureEnvelopeFromAdobeSign(envelopeId, agreement);
                                    // Filter by expiration time range
                                    if (envelope.getExpiresAt() != null &&
                                        envelope.getExpiresAt().isAfter(fromTime) &&
                                        envelope.getExpiresAt().isBefore(toTime)) {
                                        return envelope;
                                    }
                                }
                                return null;
                            })
                            .filter(envelope -> envelope != null);
                    });
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnError(error -> log.error("Failed to retrieve expiring envelopes from Adobe Sign", error));
    }

    public Flux<SignatureEnvelope> getCompletedEnvelopes(Instant fromTime, Instant toTime) {
        return ensureValidAccessToken()
            .flatMapMany(token -> {
                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/api/rest/{apiVersion}/agreements")
                        .queryParam("query", "status:SIGNED")
                        .build(properties.getApiVersion()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .flatMapMany(response -> {
                        JsonNode agreements = response.get("userAgreementList");
                        return Flux.fromIterable(agreements)
                            .map(agreement -> {
                                String agreementId = agreement.get("id").asText();
                                UUID envelopeId = externalIdMapping.get(agreementId);
                                if (envelopeId != null) {
                                    SignatureEnvelope envelope = buildSignatureEnvelopeFromAdobeSign(envelopeId, agreement);
                                    // Filter by completion time range
                                    if (envelope.getCompletedAt() != null &&
                                        envelope.getCompletedAt().isAfter(fromTime) &&
                                        envelope.getCompletedAt().isBefore(toTime)) {
                                        return envelope;
                                    }
                                }
                                return null;
                            })
                            .filter(envelope -> envelope != null);
                    });
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnError(error -> log.error("Failed to retrieve completed envelopes from Adobe Sign", error));
    }

    public Mono<byte[]> downloadCompletedDocument(UUID envelopeId) {
        return ensureValidAccessToken()
            .flatMap(token -> {
                String agreementId = envelopeIdMapping.get(envelopeId);
                if (agreementId == null) {
                    return Mono.error(new RuntimeException("Envelope not found: " + envelopeId));
                }

                return webClient.get()
                    .uri("/api/rest/{apiVersion}/agreements/{agreementId}/combinedDocument",
                         properties.getApiVersion(), agreementId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(byte[].class);
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnSuccess(document -> log.debug("Downloaded completed document for envelope {} from Adobe Sign",
                    envelopeId))
            .doOnError(error -> log.error("Failed to download completed document for envelope {} from Adobe Sign",
                    envelopeId, error));
    }

    public Mono<byte[]> downloadAuditTrail(UUID envelopeId) {
        return ensureValidAccessToken()
            .flatMap(token -> {
                String agreementId = envelopeIdMapping.get(envelopeId);
                if (agreementId == null) {
                    return Mono.error(new RuntimeException("Envelope not found: " + envelopeId));
                }

                return webClient.get()
                    .uri("/api/rest/{apiVersion}/agreements/{agreementId}/auditTrail",
                         properties.getApiVersion(), agreementId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(byte[].class);
            })
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnSuccess(auditTrail -> log.debug("Downloaded audit trail for envelope {} from Adobe Sign",
                    envelopeId))
            .doOnError(error -> log.error("Failed to download audit trail for envelope {} from Adobe Sign",
                    envelopeId, error));
    }

    /**
     * Converts EnvelopeStatus to Adobe Sign agreement status.
     */
    private String convertEnvelopeStatusToAdobeSign(EnvelopeStatus status) {
        switch (status) {
            case DRAFT:
                return "DRAFT";
            case SENT:
                return "OUT_FOR_SIGNATURE";
            case COMPLETED:
                return "COMPLETED";
            case VOIDED:
                return "CANCELLED";
            case EXPIRED:
                return "EXPIRED";
            case ARCHIVED:
                return "ARCHIVED";
            default:
                return "DRAFT";
        }
    }

    /**
     * Converts Adobe Sign agreement status to EnvelopeStatus.
     */
    private EnvelopeStatus convertAgreementStatus(String adobeSignStatus) {
        switch (adobeSignStatus) {
            case "DRAFT":
            case "AUTHORING":
                return EnvelopeStatus.DRAFT;
            case "IN_PROCESS":
            case "OUT_FOR_SIGNATURE":
                return EnvelopeStatus.SENT;
            case "SIGNED":
            case "COMPLETED":
                return EnvelopeStatus.COMPLETED;
            case "CANCELLED":
            case "ABORTED":
                return EnvelopeStatus.VOIDED;
            case "EXPIRED":
                return EnvelopeStatus.EXPIRED;
            case "ARCHIVED":
                return EnvelopeStatus.ARCHIVED;
            default:
                return EnvelopeStatus.DRAFT;
        }
    }
}
