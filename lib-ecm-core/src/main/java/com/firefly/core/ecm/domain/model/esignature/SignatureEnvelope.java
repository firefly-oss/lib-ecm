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

import com.firefly.core.ecm.domain.enums.esignature.EnvelopePriority;
import com.firefly.core.ecm.domain.enums.esignature.EnvelopeStatus;
import com.firefly.core.ecm.domain.enums.esignature.SignatureProvider;
import com.firefly.core.ecm.domain.enums.esignature.SigningOrder;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Signature envelope entity representing a collection of documents for signing.
 * Uses Long for envelope ID and Long for user IDs as per updated Firefly standards.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class SignatureEnvelope {
    
    /**
     * Unique envelope identifier (UUID)
     */
    private final UUID id;
    
    /**
     * Envelope title/subject
     */
    private final String title;
    
    /**
     * Envelope description/message
     */
    private final String description;
    
    /**
     * Current envelope status
     */
    private final EnvelopeStatus status;
    
    /**
     * List of document IDs included in this envelope
     */
    private final List<UUID> documentIds;
    
    /**
     * List of signature requests in this envelope
     */
    private final List<SignatureRequest> signatureRequests;
    
    /**
     * User who created the envelope (UUID)
     */
    private final UUID createdBy;

    /**
     * Envelope creation timestamp
     */
    private final Instant createdAt;

    /**
     * User who sent the envelope (UUID)
     */
    private final Long sentBy;
    
    /**
     * Envelope sent timestamp
     */
    private final Instant sentAt;
    
    /**
     * Envelope completion timestamp
     */
    private final Instant completedAt;
    
    /**
     * Envelope expiration timestamp
     */
    private final Instant expiresAt;
    
    /**
     * Last modification timestamp
     */
    private final Instant modifiedAt;
    
    /**
     * Envelope metadata
     */
    private final Map<String, Object> metadata;
    
    /**
     * External provider envelope ID (DocuSign, Logalty, etc.)
     */
    private final String externalEnvelopeId;
    
    /**
     * Signature provider type
     */
    private final SignatureProvider provider;
    
    /**
     * Envelope priority
     */
    private final EnvelopePriority priority;
    
    /**
     * Whether envelope requires all signers to complete
     */
    private final Boolean requireAllSigners;
    
    /**
     * Signing order enforcement
     */
    private final SigningOrder signingOrder;
    
    /**
     * Reminder settings
     */
    private final ReminderSettings reminderSettings;
    
    /**
     * Authentication requirements
     */
    private final AuthenticationRequirements authRequirements;
    
    /**
     * Envelope callback URL for notifications
     */
    private final String callbackUrl;
    
    /**
     * Whether envelope is voided
     */
    private final Boolean voided;
    
    /**
     * Void reason if envelope is voided
     */
    private final String voidReason;
}
