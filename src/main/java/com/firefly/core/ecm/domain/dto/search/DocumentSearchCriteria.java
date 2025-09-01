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
package com.firefly.core.ecm.domain.dto.search;

import com.firefly.core.ecm.domain.enums.document.ContentType;
import com.firefly.core.ecm.domain.enums.document.DocumentStatus;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Search criteria for advanced document search operations.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class DocumentSearchCriteria {
    
    /**
     * Full-text search query
     */
    private final String query;
    
    /**
     * Document name pattern (supports wildcards)
     */
    private final String namePattern;
    
    /**
     * Folder ID to search within
     */
    private final UUID folderId;
    
    /**
     * Whether to include subfolders in search
     */
    private final Boolean includeSubfolders;
    
    /**
     * Document owner ID
     */
    private final Long ownerId;
    
    /**
     * Document creator ID
     */
    private final Long createdBy;
    
    /**
     * Document modifier ID
     */
    private final Long modifiedBy;
    
    /**
     * Document status filter
     */
    private final DocumentStatus status;
    
    /**
     * Content type filter
     */
    private final ContentType contentType;
    
    /**
     * MIME type filter
     */
    private final String mimeType;
    
    /**
     * File extension filter
     */
    private final String extension;
    
    /**
     * Minimum file size in bytes
     */
    private final Long minSize;
    
    /**
     * Maximum file size in bytes
     */
    private final Long maxSize;
    
    /**
     * Creation date range start
     */
    private final Instant createdAfter;
    
    /**
     * Creation date range end
     */
    private final Instant createdBefore;
    
    /**
     * Modification date range start
     */
    private final Instant modifiedAfter;
    
    /**
     * Modification date range end
     */
    private final Instant modifiedBefore;
    
    /**
     * Tags to search for
     */
    private final Set<String> tags;
    
    /**
     * Whether all tags must match (true) or any tag (false)
     */
    private final Boolean matchAllTags;
    
    /**
     * Metadata criteria
     */
    private final Map<String, Object> metadata;
    
    /**
     * Whether document is encrypted
     */
    private final Boolean encrypted;
    
    /**
     * Whether document is under legal hold
     */
    private final Boolean legalHold;
    
    /**
     * Maximum number of results to return
     */
    private final Integer limit;
    
    /**
     * Number of results to skip (for pagination)
     */
    private final Integer offset;
    
    /**
     * Sort field
     */
    private final String sortBy;
    
    /**
     * Sort direction (ASC or DESC)
     */
    private final String sortDirection;
}
