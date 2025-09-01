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
 * Port interface for signature envelope management operations.
 * Handles envelope creation, sending, tracking, and lifecycle management.
 */
public interface SignatureEnvelopePort {
    
    /**
     * Create a new signature envelope.
     *
     * @param envelope the envelope metadata
     * @return Mono containing the created envelope with assigned ID
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
     * Send envelope to signers.
     *
     * @param envelopeId the envelope ID
     * @param sentBy the user sending the envelope
     * @return Mono containing the sent envelope
     */
    Mono<SignatureEnvelope> sendEnvelope(UUID envelopeId, Long sentBy);
    
    /**
     * Void/cancel an envelope.
     *
     * @param envelopeId the envelope ID
     * @param voidReason the reason for voiding
     * @param voidedBy the user voiding the envelope
     * @return Mono containing the voided envelope
     */
    Mono<SignatureEnvelope> voidEnvelope(UUID envelopeId, String voidReason, Long voidedBy);
    
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
     * @param createdBy the user ID
     * @param limit maximum number of envelopes to return
     * @return Flux of envelopes created by the user
     */
    Flux<SignatureEnvelope> getEnvelopesByCreator(Long createdBy, Integer limit);
    
    /**
     * Get envelopes sent by a user.
     *
     * @param sentBy the user ID
     * @param limit maximum number of envelopes to return
     * @return Flux of envelopes sent by the user
     */
    Flux<SignatureEnvelope> getEnvelopesBySender(Long sentBy, Integer limit);
    
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
     * @return Mono containing the signing URL
     */
    Mono<String> getSigningUrl(UUID envelopeId, String signerEmail);
    
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
