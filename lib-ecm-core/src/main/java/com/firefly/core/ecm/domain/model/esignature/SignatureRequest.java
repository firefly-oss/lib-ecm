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
package com.firefly.core.ecm.domain.model.esignature;

import com.firefly.core.ecm.domain.enums.esignature.AuthenticationMethod;
import com.firefly.core.ecm.domain.enums.esignature.SignatureRequestStatus;
import com.firefly.core.ecm.domain.enums.esignature.SignatureRequestType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Signature request entity representing a request for a specific signer.
 * Uses UUID for request ID and Long for user IDs as per Firefly standards.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class SignatureRequest {
    
    /**
     * Unique signature request identifier (UUID)
     */
    private final UUID id;
    
    /**
     * Parent envelope ID (UUID)
     */
    private final UUID envelopeId;
    
    /**
     * Signer user ID (Long) - null for external signers
     */
    private final Long signerId;
    
    /**
     * Signer email address
     */
    private final String signerEmail;
    
    /**
     * Signer full name
     */
    private final String signerName;
    
    /**
     * Signer role/title
     */
    private final String signerRole;
    
    /**
     * Signing order/sequence
     */
    private final Integer signingOrder;
    
    /**
     * Current request status
     */
    private final SignatureRequestStatus status;
    
    /**
     * Request type (SIGNATURE, INITIAL, APPROVAL, etc.)
     */
    private final SignatureRequestType requestType;
    
    /**
     * Whether this signature is required
     */
    private final Boolean required;
    
    /**
     * List of signature fields/tabs for this signer
     */
    private final List<SignatureField> signatureFields;
    
    /**
     * Request creation timestamp
     */
    private final Instant createdAt;
    
    /**
     * Request sent timestamp
     */
    private final Instant sentAt;
    
    /**
     * Request viewed timestamp
     */
    private final Instant viewedAt;
    
    /**
     * Request signed timestamp
     */
    private final Instant signedAt;
    
    /**
     * Request completion timestamp
     */
    private final Instant completedAt;
    
    /**
     * Request expiration timestamp
     */
    private final Instant expiresAt;
    
    /**
     * Decline reason if declined
     */
    private final String declineReason;
    
    /**
     * Signer IP address when signed
     */
    private final String signerIpAddress;
    
    /**
     * Signer user agent when signed
     */
    private final String signerUserAgent;
    
    /**
     * Authentication method used
     */
    private final AuthenticationMethod authMethod;
    
    /**
     * External signer ID from provider
     */
    private final String externalSignerId;
    
    /**
     * Signing URL for external access
     */
    private final String signingUrl;
    
    /**
     * Request metadata
     */
    private final Map<String, Object> metadata;
    
    /**
     * Custom message for this signer
     */
    private final String customMessage;
    
    /**
     * Language preference for signer
     */
    private final String language;
    
    /**
     * Time zone for signer
     */
    private final String timeZone;
}
