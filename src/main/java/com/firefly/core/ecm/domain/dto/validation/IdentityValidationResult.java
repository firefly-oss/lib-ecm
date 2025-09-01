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
package com.firefly.core.ecm.domain.dto.validation;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Result of identity validation operations.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class IdentityValidationResult {
    
    /**
     * Whether the identity is valid
     */
    private final Boolean valid;
    
    /**
     * Validated identity
     */
    private final String identity;
    
    /**
     * Identity confidence score (0-100)
     */
    private final Integer confidenceScore;
    
    /**
     * Identity validation method used
     */
    private final String validationMethod;
    
    /**
     * Identity validation errors
     */
    private final List<String> errors;
    
    /**
     * Identity validation warnings
     */
    private final List<String> warnings;
}
