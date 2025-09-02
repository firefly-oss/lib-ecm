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
package com.firefly.core.ecm.port.audit;

import com.firefly.core.ecm.domain.model.audit.AuditEvent;
import com.firefly.core.ecm.domain.enums.audit.AuditEventType;
import com.firefly.core.ecm.domain.enums.audit.AuditSeverity;
import com.firefly.core.ecm.domain.enums.security.ResourceType;
import com.firefly.core.ecm.domain.dto.search.AuditSearchCriteria;
import com.firefly.core.ecm.domain.dto.statistics.AuditStatistics;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Port interface for audit trail and compliance logging operations.
 * Handles recording, querying, and managing audit events for compliance purposes.
 */
public interface AuditPort {
    
    /**
     * Record an audit event.
     *
     * @param auditEvent the audit event to record
     * @return Mono containing the recorded audit event with assigned ID
     */
    Mono<AuditEvent> recordEvent(AuditEvent auditEvent);
    
    /**
     * Get audit event by ID.
     *
     * @param eventId the audit event ID
     * @return Mono containing the audit event, empty if not found
     */
    Mono<AuditEvent> getEvent(UUID eventId);
    
    /**
     * Get audit events for a specific resource.
     *
     * @param resourceId the resource ID
     * @param resourceType the resource type
     * @return Flux of audit events for the resource
     */
    Flux<AuditEvent> getResourceAuditTrail(UUID resourceId, ResourceType resourceType);
    
    /**
     * Get audit events for a specific user.
     *
     * @param userId the user ID
     * @return Flux of audit events for the user
     */
    Flux<AuditEvent> getUserAuditTrail(UUID userId);
    
    /**
     * Get audit events by event type.
     *
     * @param eventType the event type
     * @param limit maximum number of events to return
     * @return Flux of audit events of the specified type
     */
    Flux<AuditEvent> getEventsByType(AuditEventType eventType, Integer limit);
    
    /**
     * Get audit events by severity level.
     *
     * @param severity the severity level
     * @param limit maximum number of events to return
     * @return Flux of audit events with the specified severity
     */
    Flux<AuditEvent> getEventsBySeverity(AuditSeverity severity, Integer limit);
    
    /**
     * Get audit events within a time range.
     *
     * @param fromTime start time (inclusive)
     * @param toTime end time (inclusive)
     * @param limit maximum number of events to return
     * @return Flux of audit events within the time range
     */
    Flux<AuditEvent> getEventsByTimeRange(Instant fromTime, Instant toTime, Integer limit);
    
    /**
     * Get audit events by correlation ID.
     *
     * @param correlationId the correlation ID
     * @return Flux of related audit events
     */
    Flux<AuditEvent> getEventsByCorrelationId(String correlationId);
    
    /**
     * Get audit events by session ID.
     *
     * @param sessionId the session ID
     * @return Flux of audit events for the session
     */
    Flux<AuditEvent> getEventsBySessionId(String sessionId);
    
    /**
     * Get audit events by IP address.
     *
     * @param ipAddress the IP address
     * @param limit maximum number of events to return
     * @return Flux of audit events from the IP address
     */
    Flux<AuditEvent> getEventsByIpAddress(String ipAddress, Integer limit);
    
    /**
     * Get failed operations audit events.
     *
     * @param limit maximum number of events to return
     * @return Flux of failed operation audit events
     */
    Flux<AuditEvent> getFailedOperations(Integer limit);
    
    /**
     * Get security violation audit events.
     *
     * @param limit maximum number of events to return
     * @return Flux of security violation audit events
     */
    Flux<AuditEvent> getSecurityViolations(Integer limit);
    
    /**
     * Search audit events by criteria.
     *
     * @param searchCriteria the search criteria
     * @return Flux of audit events matching the criteria
     */
    Flux<AuditEvent> searchEvents(AuditSearchCriteria searchCriteria);
    
    /**
     * Get audit statistics for a time period.
     *
     * @param fromTime start time
     * @param toTime end time
     * @return Mono containing audit statistics
     */
    Mono<AuditStatistics> getAuditStatistics(Instant fromTime, Instant toTime);
    
    /**
     * Archive old audit events.
     *
     * @param olderThan archive events older than this timestamp
     * @return Mono containing the number of archived events
     */
    Mono<Long> archiveEvents(Instant olderThan);
    
    /**
     * Delete archived audit events.
     *
     * @param olderThan delete archived events older than this timestamp
     * @return Mono containing the number of deleted events
     */
    Mono<Long> deleteArchivedEvents(Instant olderThan);
    
    /**
     * Get compliance tags used in audit events.
     *
     * @return Flux of compliance tags
     */
    Flux<String> getComplianceTags();
    
    /**
     * Get audit events by compliance tags.
     *
     * @param complianceTags the compliance tags to search for
     * @param matchAll whether all tags must match (true) or any tag (false)
     * @param limit maximum number of events to return
     * @return Flux of audit events with the specified compliance tags
     */
    Flux<AuditEvent> getEventsByComplianceTags(Set<String> complianceTags, Boolean matchAll, Integer limit);
}
