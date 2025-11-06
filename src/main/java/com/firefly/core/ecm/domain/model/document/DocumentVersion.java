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

import com.firefly.core.ecm.domain.enums.document.VersionStatus;
import com.firefly.core.ecm.domain.enums.document.VersionType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Document version entity representing a specific version of a document.
 * Maintains version history and allows rollback capabilities.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class DocumentVersion {
    
    /**
     * Unique version identifier (UUID)
     */
    private final UUID id;
    
    /**
     * Parent document ID (UUID)
     */
    private final UUID documentId;
    
    /**
     * Version number (1, 2, 3, etc.)
     */
    private final Integer versionNumber;
    
    /**
     * Version label (e.g., "1.0", "2.1", "Draft", "Final")
     */
    private final String versionLabel;
    
    /**
     * Version comment/description
     */
    private final String comment;
    
    /**
     * Document size in bytes for this version
     */
    private final Long size;
    
    /**
     * Storage path for this version
     */
    private final String storagePath;
    
    /**
     * Document checksum for this version
     */
    private final String checksum;
    
    /**
     * Checksum algorithm used
     */
    private final String checksumAlgorithm;
    
    /**
     * MIME type for this version
     */
    private final String mimeType;
    
    /**
     * File extension for this version
     */
    private final String extension;
    
    /**
     * User who created this version (UUID)
     */
    private final UUID createdBy;
    
    /**
     * Version creation timestamp
     */
    private final Instant createdAt;
    
    /**
     * Whether this is the current/active version
     */
    private final Boolean current;
    
    /**
     * Whether this version is a major version
     */
    private final Boolean majorVersion;
    
    /**
     * Version-specific metadata
     */
    private final Map<String, Object> metadata;
    
    /**
     * Version status
     */
    private final VersionStatus status;
    
    /**
     * Previous version ID (UUID) - for version chain
     */
    private final UUID previousVersionId;
    
    /**
     * Next version ID (UUID) - for version chain
     */
    private final UUID nextVersionId;
    
    /**
     * Version type (AUTO, MANUAL, CHECKPOINT)
     */
    private final VersionType versionType;
    
    /**
     * Content changes summary
     */
    private final String changesSummary;
}
