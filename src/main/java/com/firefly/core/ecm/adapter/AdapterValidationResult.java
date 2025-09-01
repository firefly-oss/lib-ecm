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
package com.firefly.core.ecm.adapter;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;

/**
 * Result of adapter configuration validation.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class AdapterValidationResult {
    
    /**
     * Whether the adapter configuration is valid
     */
    private final Boolean valid;
    
    /**
     * Error message if validation failed
     */
    private final String errorMessage;
    
    /**
     * Missing required properties
     */
    private final Set<String> missingProperties;
    
    /**
     * Adapter information if validation succeeded
     */
    private final AdapterInfo adapterInfo;
    
    /**
     * Additional validation warnings
     */
    private final Set<String> warnings;
    
    /**
     * Check if validation has warnings.
     *
     * @return true if there are warnings, false otherwise
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
    
    /**
     * Check if validation has missing properties.
     *
     * @return true if there are missing properties, false otherwise
     */
    public boolean hasMissingProperties() {
        return missingProperties != null && !missingProperties.isEmpty();
    }
}
