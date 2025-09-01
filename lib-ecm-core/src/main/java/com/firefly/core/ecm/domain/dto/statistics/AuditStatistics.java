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
package com.firefly.core.ecm.domain.dto.statistics;

import com.firefly.core.ecm.domain.enums.audit.AuditEventType;
import com.firefly.core.ecm.domain.enums.audit.AuditSeverity;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;

/**
 * Audit statistics for a specific time period.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class AuditStatistics {
    
    /**
     * Start time of the statistics period
     */
    private final Instant fromTime;
    
    /**
     * End time of the statistics period
     */
    private final Instant toTime;
    
    /**
     * Total number of audit events
     */
    private final Long totalEvents;
    
    /**
     * Number of successful operations
     */
    private final Long successfulOperations;
    
    /**
     * Number of failed operations
     */
    private final Long failedOperations;
    
    /**
     * Number of security violations
     */
    private final Long securityViolations;
    
    /**
     * Event count by type
     */
    private final Map<AuditEventType, Long> eventsByType;
    
    /**
     * Event count by severity
     */
    private final Map<AuditSeverity, Long> eventsBySeverity;
    
    /**
     * Event count by user
     */
    private final Map<Long, Long> eventsByUser;
    
    /**
     * Event count by IP address
     */
    private final Map<String, Long> eventsByIpAddress;
    
    /**
     * Event count by source application
     */
    private final Map<String, Long> eventsBySource;
    
    /**
     * Most active users (top 10)
     */
    private final Map<Long, Long> topUsers;
    
    /**
     * Most common IP addresses (top 10)
     */
    private final Map<String, Long> topIpAddresses;
    
    /**
     * Most common event types (top 10)
     */
    private final Map<AuditEventType, Long> topEventTypes;
    
    /**
     * Average events per day
     */
    private final Double averageEventsPerDay;
    
    /**
     * Peak events per hour
     */
    private final Long peakEventsPerHour;
    
    /**
     * Number of unique users
     */
    private final Long uniqueUsers;
    
    /**
     * Number of unique IP addresses
     */
    private final Long uniqueIpAddresses;
}
