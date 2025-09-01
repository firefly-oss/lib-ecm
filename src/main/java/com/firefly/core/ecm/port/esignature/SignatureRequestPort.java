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

import com.firefly.core.ecm.domain.model.esignature.SignatureRequest;
import com.firefly.core.ecm.domain.enums.esignature.SignatureRequestStatus;
import com.firefly.core.ecm.domain.enums.esignature.SignatureRequestType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Port interface for signature request management operations.
 * Handles individual signer requests within signature envelopes.
 */
public interface SignatureRequestPort {
    
    /**
     * Create a new signature request.
     *
     * @param signatureRequest the signature request metadata
     * @return Mono containing the created signature request with assigned ID
     */
    Mono<SignatureRequest> createSignatureRequest(SignatureRequest signatureRequest);
    
    /**
     * Get signature request by ID.
     *
     * @param requestId the signature request ID
     * @return Mono containing the signature request, empty if not found
     */
    Mono<SignatureRequest> getSignatureRequest(UUID requestId);
    
    /**
     * Update signature request.
     *
     * @param signatureRequest the updated signature request
     * @return Mono containing the updated signature request
     */
    Mono<SignatureRequest> updateSignatureRequest(SignatureRequest signatureRequest);
    
    /**
     * Delete signature request.
     *
     * @param requestId the signature request ID
     * @return Mono indicating completion
     */
    Mono<Void> deleteSignatureRequest(UUID requestId);
    
    /**
     * Get signature requests for an envelope.
     *
     * @param envelopeId the envelope ID
     * @return Flux of signature requests for the envelope
     */
    Flux<SignatureRequest> getRequestsByEnvelope(UUID envelopeId);
    
    /**
     * Get signature requests by signer.
     *
     * @param signerId the signer user ID
     * @return Flux of signature requests for the signer
     */
    Flux<SignatureRequest> getRequestsBySigner(Long signerId);
    
    /**
     * Get signature requests by signer email.
     *
     * @param signerEmail the signer email address
     * @return Flux of signature requests for the signer email
     */
    Flux<SignatureRequest> getRequestsBySignerEmail(String signerEmail);
    
    /**
     * Get signature requests by status.
     *
     * @param status the signature request status
     * @param limit maximum number of requests to return
     * @return Flux of signature requests with the specified status
     */
    Flux<SignatureRequest> getRequestsByStatus(SignatureRequestStatus status, Integer limit);
    
    /**
     * Get signature requests by type.
     *
     * @param requestType the signature request type
     * @param limit maximum number of requests to return
     * @return Flux of signature requests of the specified type
     */
    Flux<SignatureRequest> getRequestsByType(SignatureRequestType requestType, Integer limit);
    
    /**
     * Get pending signature requests for a signer.
     *
     * @param signerId the signer user ID
     * @return Flux of pending signature requests
     */
    Flux<SignatureRequest> getPendingRequestsBySigner(Long signerId);
    
    /**
     * Get pending signature requests by email.
     *
     * @param signerEmail the signer email address
     * @return Flux of pending signature requests
     */
    Flux<SignatureRequest> getPendingRequestsByEmail(String signerEmail);
    
    /**
     * Mark signature request as viewed.
     *
     * @param requestId the signature request ID
     * @param viewedAt the timestamp when viewed
     * @return Mono containing the updated signature request
     */
    Mono<SignatureRequest> markAsViewed(UUID requestId, Instant viewedAt);
    
    /**
     * Mark signature request as signed.
     *
     * @param requestId the signature request ID
     * @param signedAt the timestamp when signed
     * @param signerIpAddress the signer's IP address
     * @param signerUserAgent the signer's user agent
     * @return Mono containing the updated signature request
     */
    Mono<SignatureRequest> markAsSigned(UUID requestId, Instant signedAt, String signerIpAddress, String signerUserAgent);
    
    /**
     * Mark signature request as declined.
     *
     * @param requestId the signature request ID
     * @param declineReason the reason for declining
     * @return Mono containing the updated signature request
     */
    Mono<SignatureRequest> markAsDeclined(UUID requestId, String declineReason);
    
    /**
     * Mark signature request as completed.
     *
     * @param requestId the signature request ID
     * @param completedAt the timestamp when completed
     * @return Mono containing the updated signature request
     */
    Mono<SignatureRequest> markAsCompleted(UUID requestId, Instant completedAt);
    
    /**
     * Delegate signature request to another signer.
     *
     * @param requestId the signature request ID
     * @param newSignerEmail the new signer email
     * @param newSignerName the new signer name
     * @return Mono containing the updated signature request
     */
    Mono<SignatureRequest> delegateRequest(UUID requestId, String newSignerEmail, String newSignerName);
    
    /**
     * Resend signature request notification.
     *
     * @param requestId the signature request ID
     * @return Mono indicating completion
     */
    Mono<Void> resendNotification(UUID requestId);
    
    /**
     * Get signature requests expiring within a time range.
     *
     * @param fromTime start time
     * @param toTime end time
     * @return Flux of signature requests expiring within the time range
     */
    Flux<SignatureRequest> getExpiringRequests(Instant fromTime, Instant toTime);
    
    /**
     * Check if signature request exists.
     *
     * @param requestId the signature request ID
     * @return Mono containing true if request exists, false otherwise
     */
    Mono<Boolean> existsSignatureRequest(UUID requestId);
}
