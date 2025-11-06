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
package com.firefly.core.ecm.domain.model.audit;

import com.firefly.core.ecm.domain.enums.audit.AuditEventType;
import com.firefly.core.ecm.domain.enums.audit.AuditSeverity;
import com.firefly.core.ecm.domain.enums.security.ResourceType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Audit event entity for tracking all ECM system activities.
 * Uses UUID for event ID and Long for user IDs as per Firefly standards.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class AuditEvent {
    
    /**
     * Unique audit event identifier (UUID)
     */
    private final UUID id;
    
    /**
     * Event type/action performed
     */
    private final AuditEventType eventType;
    
    /**
     * Resource ID that was affected (UUID)
     */
    private final UUID resourceId;
    
    /**
     * Resource type that was affected
     */
    private final ResourceType resourceType;
    
    /**
     * User who performed the action (Long)
     */
    private final UUID userId;
    
    /**
     * User's session ID
     */
    private final String sessionId;
    
    /**
     * Event timestamp
     */
    private final Instant timestamp;
    
    /**
     * Event description/summary
     */
    private final String description;
    
    /**
     * Detailed event message
     */
    private final String details;
    
    /**
     * Event severity level
     */
    private final AuditSeverity severity;
    
    /**
     * Source IP address
     */
    private final String ipAddress;
    
    /**
     * User agent string
     */
    private final String userAgent;
    
    /**
     * Application/service that generated the event
     */
    private final String source;
    
    /**
     * Event correlation ID for tracking related events
     */
    private final String correlationId;
    
    /**
     * Previous values (for update operations)
     */
    private final Map<String, Object> previousValues;
    
    /**
     * New values (for update operations)
     */
    private final Map<String, Object> newValues;
    
    /**
     * Additional event metadata
     */
    private final Map<String, Object> metadata;
    
    /**
     * Whether the operation was successful
     */
    private final Boolean success;
    
    /**
     * Error message if operation failed
     */
    private final String errorMessage;
    
    /**
     * Error code if operation failed
     */
    private final String errorCode;
    
    /**
     * Operation duration in milliseconds
     */
    private final Long durationMs;
    
    /**
     * Tenant/organization ID for multi-tenant systems
     */
    private final String tenantId;
    
    /**
     * Compliance/regulatory tags
     */
    private final java.util.Set<String> complianceTags;
}
