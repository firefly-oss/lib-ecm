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
package com.firefly.core.ecm.port.esignature;

import com.firefly.core.ecm.domain.model.esignature.SignatureEnvelope;
import com.firefly.core.ecm.domain.enums.esignature.EnvelopeStatus;
import com.firefly.core.ecm.domain.enums.esignature.SignatureProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Port interface for signature envelope management operations in the Firefly ECM system.
 *
 * <p>This interface defines the contract for managing signature envelopes throughout their
 * complete lifecycle, from creation to completion and archival. It follows the hexagonal
 * architecture pattern where this port defines the business requirements, and adapters
 * provide concrete implementations for different eSignature providers.</p>
 *
 * <p>Key capabilities provided by this port:</p>
 * <ul>
 *   <li><strong>Envelope Lifecycle Management:</strong> Create, send, track, and archive envelopes</li>
 *   <li><strong>Multi-Provider Support:</strong> Abstract interface supporting various eSignature providers</li>
 *   <li><strong>Status Tracking:</strong> Monitor envelope progress and completion status</li>
 *   <li><strong>Query Operations:</strong> Find envelopes by various criteria (status, creator, provider)</li>
 *   <li><strong>Embedded Signing:</strong> Generate signing URLs for integrated workflows</li>
 *   <li><strong>Compliance Features:</strong> Archive completed envelopes for audit and compliance</li>
 * </ul>
 *
 * <p>All operations return reactive types (Mono/Flux) for non-blocking processing and
 * integration with reactive application architectures. The interface uses UUIDs for
 * entity identifiers following Firefly's architectural standards.</p>
 *
 * <p>Typical usage patterns:</p>
 * <pre>
 * {@code
 * // Create and send an envelope
 * SignatureEnvelope envelope = SignatureEnvelope.builder()
 *     .title("Contract Signature")
 *     .documentIds(List.of(documentId))
 *     .signatureRequests(signatureRequests)
 *     .build();
 *
 * Mono<SignatureEnvelope> result = envelopePort.createEnvelope(envelope)
 *     .flatMap(created -> envelopePort.sendEnvelope(created.getId(), senderId));
 * }
 * </pre>
 *
 * @author Firefly Software Solutions Inc.
 * @since 1.0.0
 * @see SignatureEnvelope
 * @see SignatureRequest
 * @see EnvelopeStatus
 * @see SignatureProvider
 */
public interface SignatureEnvelopePort {
    
    /**
     * Creates a new signature envelope with documents and signature requests.
     *
     * <p>This method creates a new envelope containing one or more documents that require
     * signatures from specified participants. The implementation should:</p>
     * <ul>
     *   <li>Generate a unique envelope ID if not provided</li>
     *   <li>Validate that all referenced documents exist and are accessible</li>
     *   <li>Create the envelope in the eSignature provider's system</li>
     *   <li>Set up signature requests for all specified participants</li>
     *   <li>Return the created envelope with provider-specific metadata</li>
     * </ul>
     *
     * <p>The envelope will be in DRAFT status after creation and must be explicitly
     * sent using {@link #sendEnvelope(UUID, UUID)} to initiate the signing process.</p>
     *
     * @param envelope the envelope metadata containing documents, signature requests, and settings
     * @return a Mono containing the created envelope with assigned ID and provider metadata
     * @throws IllegalArgumentException if envelope is null or contains invalid data
     * @throws RuntimeException if envelope creation fails or referenced documents are not found
     * @see #sendEnvelope(UUID, UUID)
     * @see SignatureEnvelope
     * @see EnvelopeStatus#DRAFT
     */
    Mono<SignatureEnvelope> createEnvelope(SignatureEnvelope envelope);
    
    /**
     * Get envelope by ID.
     *
     * @param envelopeId the envelope ID
     * @return Mono containing the envelope, empty if not found
     */
    Mono<SignatureEnvelope> getEnvelope(UUID envelopeId);
    
    /**
     * Update envelope metadata.
     *
     * @param envelope the updated envelope
     * @return Mono containing the updated envelope
     */
    Mono<SignatureEnvelope> updateEnvelope(SignatureEnvelope envelope);
    
    /**
     * Sends a signature envelope to all participants for signing.
     *
     * <p>This method initiates the signing process by sending the envelope to all
     * configured signers. The implementation should:</p>
     * <ul>
     *   <li>Validate that the envelope exists and is in a sendable state (typically DRAFT)</li>
     *   <li>Send notification emails to all participants with signing instructions</li>
     *   <li>Update the envelope status to SENT</li>
     *   <li>Record the sent timestamp and sender information</li>
     *   <li>Generate signing URLs for participants if using embedded signing</li>
     * </ul>
     *
     * <p>Once sent, participants will receive email notifications with links to access
     * and sign the documents. The envelope status will be updated to reflect the
     * current signing progress.</p>
     *
     * @param envelopeId the unique UUID identifier of the envelope to send
     * @param sentBy the UUID of the user sending the envelope (for audit purposes)
     * @return a Mono containing the sent envelope with updated status and metadata
     * @throws IllegalArgumentException if envelopeId or sentBy is null
     * @throws RuntimeException if envelope is not found, not in sendable state, or sending fails
     * @see EnvelopeStatus#SENT
     * @see SignatureEnvelope
     */
    Mono<SignatureEnvelope> sendEnvelope(UUID envelopeId, UUID sentBy);

    /**
     * Void/cancel an envelope.
     *
     * @param envelopeId the envelope ID
     * @param voidReason the reason for voiding
     * @param voidedBy the user voiding the envelope (UUID)
     * @return Mono containing the voided envelope
     */
    Mono<SignatureEnvelope> voidEnvelope(UUID envelopeId, String voidReason, UUID voidedBy);
    
    /**
     * Delete an envelope.
     *
     * @param envelopeId the envelope ID
     * @return Mono indicating completion
     */
    Mono<Void> deleteEnvelope(UUID envelopeId);
    
    /**
     * Get envelopes by status.
     *
     * @param status the envelope status
     * @param limit maximum number of envelopes to return
     * @return Flux of envelopes with the specified status
     */
    Flux<SignatureEnvelope> getEnvelopesByStatus(EnvelopeStatus status, Integer limit);
    
    /**
     * Get envelopes created by a user.
     *
     * @param createdBy the user ID (UUID)
     * @param limit maximum number of envelopes to return
     * @return Flux of envelopes created by the user
     */
    Flux<SignatureEnvelope> getEnvelopesByCreator(UUID createdBy, Integer limit);

    /**
     * Get envelopes sent by a user.
     *
     * @param sentBy the user ID (UUID)
     * @param limit maximum number of envelopes to return
     * @return Flux of envelopes sent by the user
     */
    Flux<SignatureEnvelope> getEnvelopesBySender(UUID sentBy, Integer limit);
    
    /**
     * Get envelopes by provider.
     *
     * @param provider the signature provider
     * @param limit maximum number of envelopes to return
     * @return Flux of envelopes using the specified provider
     */
    Flux<SignatureEnvelope> getEnvelopesByProvider(SignatureProvider provider, Integer limit);
    
    /**
     * Get envelopes expiring within a time range.
     *
     * @param fromTime start time
     * @param toTime end time
     * @return Flux of envelopes expiring within the time range
     */
    Flux<SignatureEnvelope> getExpiringEnvelopes(Instant fromTime, Instant toTime);
    
    /**
     * Get completed envelopes within a time range.
     *
     * @param fromTime start time
     * @param toTime end time
     * @return Flux of envelopes completed within the time range
     */
    Flux<SignatureEnvelope> getCompletedEnvelopes(Instant fromTime, Instant toTime);
    
    /**
     * Check if envelope exists.
     *
     * @param envelopeId the envelope ID
     * @return Mono containing true if envelope exists, false otherwise
     */
    Mono<Boolean> existsEnvelope(UUID envelopeId);
    
    /**
     * Get envelope by external provider ID.
     *
     * @param externalEnvelopeId the external envelope ID
     * @param provider the signature provider
     * @return Mono containing the envelope, empty if not found
     */
    Mono<SignatureEnvelope> getEnvelopeByExternalId(String externalEnvelopeId, SignatureProvider provider);
    
    /**
     * Sync envelope status with external provider.
     *
     * @param envelopeId the envelope ID
     * @return Mono containing the updated envelope
     */
    Mono<SignatureEnvelope> syncEnvelopeStatus(UUID envelopeId);
    
    /**
     * Get envelope signing URL for a specific signer.
     *
     * @param envelopeId the envelope ID
     * @param signerEmail the signer email
     * @param signerName the signer's full name
     * @param clientUserId the client-specific user identifier for embedded signing
     * @return Mono containing the signing URL
     */
    Mono<String> getSigningUrl(UUID envelopeId, String signerEmail, String signerName, String clientUserId);

    /**
     * Get signing URL for a specific signer (backward compatibility method).
     * Uses default values for signerName and generates clientUserId.
     *
     * @param envelopeId the envelope ID
     * @param signerEmail the signer email
     * @return Mono containing the signing URL
     */
    default Mono<String> getSigningUrl(UUID envelopeId, String signerEmail) {
        return getSigningUrl(envelopeId, signerEmail, "Signer", UUID.randomUUID().toString());
    }

    /**
     * Resend envelope to pending signers.
     *
     * @param envelopeId the envelope ID
     * @return Mono indicating completion
     */
    Mono<Void> resendEnvelope(UUID envelopeId);
    
    /**
     * Archive completed envelope.
     *
     * @param envelopeId the envelope ID
     * @return Mono containing the archived envelope
     */
    Mono<SignatureEnvelope> archiveEnvelope(UUID envelopeId);
}
