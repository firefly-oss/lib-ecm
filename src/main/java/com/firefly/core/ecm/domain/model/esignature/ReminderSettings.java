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

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;

/**
 * Reminder settings for signature envelopes.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class ReminderSettings {
    
    /**
     * Whether reminders are enabled
     */
    private final Boolean enabled;
    
    /**
     * Initial reminder delay after envelope is sent
     */
    private final Duration initialDelay;
    
    /**
     * Interval between subsequent reminders
     */
    private final Duration reminderInterval;
    
    /**
     * Maximum number of reminders to send
     */
    private final Integer maxReminders;
    
    /**
     * Custom reminder message
     */
    private final String customMessage;
    
    /**
     * Whether to send reminders on weekends
     */
    private final Boolean includeWeekends;
    
    /**
     * Preferred time of day to send reminders (24-hour format)
     */
    private final String preferredTime;
    
    /**
     * Time zone for reminder scheduling
     */
    private final String timeZone;
}
