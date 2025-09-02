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

import com.firefly.core.ecm.domain.enums.audit.AuditEventType;
import com.firefly.core.ecm.domain.enums.audit.AuditSeverity;
import com.firefly.core.ecm.domain.enums.security.ResourceType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Search criteria for audit event queries.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class AuditSearchCriteria {
    
    /**
     * Event types to search for
     */
    private final Set<AuditEventType> eventTypes;
    
    /**
     * Severity levels to search for
     */
    private final Set<AuditSeverity> severities;
    
    /**
     * Resource ID to search for
     */
    private final UUID resourceId;
    
    /**
     * Resource type to search for
     */
    private final ResourceType resourceType;
    
    /**
     * User ID to search for
     */
    private final UUID userId;
    
    /**
     * Session ID to search for
     */
    private final String sessionId;
    
    /**
     * Correlation ID to search for
     */
    private final String correlationId;
    
    /**
     * IP address to search for
     */
    private final String ipAddress;
    
    /**
     * Source application to search for
     */
    private final String source;
    
    /**
     * Start time for search range
     */
    private final Instant fromTime;
    
    /**
     * End time for search range
     */
    private final Instant toTime;
    
    /**
     * Whether to include only successful operations
     */
    private final Boolean successOnly;
    
    /**
     * Whether to include only failed operations
     */
    private final Boolean failuresOnly;
    
    /**
     * Compliance tags to search for
     */
    private final Set<String> complianceTags;
    
    /**
     * Whether all compliance tags must match
     */
    private final Boolean matchAllTags;
    
    /**
     * Text search in event description
     */
    private final String descriptionContains;
    
    /**
     * Text search in event details
     */
    private final String detailsContains;
    
    /**
     * Tenant ID for multi-tenant search
     */
    private final String tenantId;
    
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
