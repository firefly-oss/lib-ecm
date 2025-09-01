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
package com.firefly.core.ecm.domain.model.document;

import com.firefly.core.ecm.domain.enums.document.ContentType;
import com.firefly.core.ecm.domain.enums.document.DocumentStatus;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Core document entity representing a document in the ECM system.
 * Uses UUID for document ID and Long for owner/user IDs as per Firefly standards.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class Document {
    
    /**
     * Unique document identifier (UUID)
     */
    private final UUID id;
    
    /**
     * Document name/title
     */
    private final String name;
    
    /**
     * Document description
     */
    private final String description;
    
    /**
     * MIME type of the document
     */
    private final String mimeType;
    
    /**
     * File extension
     */
    private final String extension;
    
    /**
     * Document size in bytes
     */
    private final Long size;
    
    /**
     * Storage path in the underlying storage system
     */
    private final String storagePath;
    
    /**
     * Document checksum for integrity verification
     */
    private final String checksum;
    
    /**
     * Checksum algorithm used
     */
    private final String checksumAlgorithm;
    
    /**
     * Current version number
     */
    private final Integer version;
    
    /**
     * Document status
     */
    private final DocumentStatus status;
    
    /**
     * Parent folder ID (UUID)
     */
    private final UUID folderId;
    
    /**
     * Document owner ID (Long)
     */
    private final Long ownerId;
    
    /**
     * User who created the document (Long)
     */
    private final Long createdBy;
    
    /**
     * User who last modified the document (Long)
     */
    private final Long modifiedBy;
    
    /**
     * Document creation timestamp
     */
    private final Instant createdAt;
    
    /**
     * Document last modification timestamp
     */
    private final Instant modifiedAt;
    
    /**
     * Document expiration timestamp (optional)
     */
    private final Instant expiresAt;
    
    /**
     * Document metadata as key-value pairs
     */
    private final Map<String, Object> metadata;
    
    /**
     * Document tags for categorization
     */
    private final java.util.Set<String> tags;
    
    /**
     * Whether the document is encrypted
     */
    private final Boolean encrypted;
    
    /**
     * Content type classification
     */
    private final ContentType contentType;
    
    /**
     * Document retention policy ID
     */
    private final String retentionPolicyId;
    
    /**
     * Whether the document is under legal hold
     */
    private final Boolean legalHold;
}
