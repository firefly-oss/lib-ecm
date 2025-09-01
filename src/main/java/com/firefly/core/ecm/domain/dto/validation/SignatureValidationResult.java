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

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Result of signature validation operations.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class SignatureValidationResult {
    
    /**
     * Whether the signature is valid
     */
    private final Boolean valid;
    
    /**
     * Validation status code
     */
    private final String statusCode;
    
    /**
     * Validation status message
     */
    private final String statusMessage;
    
    /**
     * Validation timestamp
     */
    private final Instant validatedAt;
    
    /**
     * Signature algorithm used
     */
    private final String signatureAlgorithm;
    
    /**
     * Hash algorithm used
     */
    private final String hashAlgorithm;
    
    /**
     * Certificate validation result
     */
    private final CertificateValidationResult certificateValidation;
    
    /**
     * Timestamp validation result
     */
    private final TimestampValidationResult timestampValidation;
    
    /**
     * Identity validation result
     */
    private final IdentityValidationResult identityValidation;
    
    /**
     * Integrity check result
     */
    private final Boolean integrityValid;
    
    /**
     * List of validation warnings
     */
    private final List<String> warnings;
    
    /**
     * List of validation errors
     */
    private final List<String> errors;
    
    /**
     * Additional validation details
     */
    private final Map<String, Object> details;
    
    /**
     * Validation confidence score (0-100)
     */
    private final Integer confidenceScore;
    
    /**
     * Compliance validation results
     */
    private final Map<String, Boolean> complianceResults;
}
