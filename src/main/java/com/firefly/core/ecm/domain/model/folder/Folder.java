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
package com.firefly.core.ecm.domain.model.folder;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Folder entity representing a hierarchical folder structure in the ECM system.
 * Uses UUID for folder ID and Long for owner/user IDs as per Firefly standards.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class Folder {
    
    /**
     * Unique folder identifier (UUID)
     */
    private final UUID id;
    
    /**
     * Folder name
     */
    private final String name;
    
    /**
     * Folder description
     */
    private final String description;
    
    /**
     * Parent folder ID (UUID) - null for root folders
     */
    private final UUID parentId;
    
    /**
     * Full path from root (e.g., "/Documents/Projects/2024")
     */
    private final String path;
    
    /**
     * Folder depth level (0 for root)
     */
    private final Integer level;
    
    /**
     * Folder owner ID (Long)
     */
    private final Long ownerId;
    
    /**
     * User who created the folder (Long)
     */
    private final Long createdBy;
    
    /**
     * User who last modified the folder (Long)
     */
    private final Long modifiedBy;
    
    /**
     * Folder creation timestamp
     */
    private final Instant createdAt;
    
    /**
     * Folder last modification timestamp
     */
    private final Instant modifiedAt;
    
    /**
     * Folder metadata as key-value pairs
     */
    private final Map<String, Object> metadata;
    
    /**
     * Folder tags for categorization
     */
    private final java.util.Set<String> tags;
    
    /**
     * Whether the folder is system-managed
     */
    private final Boolean systemFolder;
    
    /**
     * Whether the folder is hidden
     */
    private final Boolean hidden;
    
    /**
     * Folder access permissions
     */
    private final FolderPermissions permissions;
    
    /**
     * Maximum allowed file size in this folder (bytes)
     */
    private final Long maxFileSize;
    
    /**
     * Allowed file extensions in this folder
     */
    private final java.util.Set<String> allowedExtensions;
    
    /**
     * Blocked file extensions in this folder
     */
    private final java.util.Set<String> blockedExtensions;
    
    /**
     * Folder retention policy ID
     */
    private final String retentionPolicyId;
}
