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

/**
 * Result of certificate validation operations.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class CertificateValidationResult {
    
    /**
     * Whether the certificate is valid
     */
    private final Boolean valid;
    
    /**
     * Certificate subject name
     */
    private final String subjectName;
    
    /**
     * Certificate issuer name
     */
    private final String issuerName;
    
    /**
     * Certificate serial number
     */
    private final String serialNumber;
    
    /**
     * Certificate not valid before date
     */
    private final Instant notValidBefore;
    
    /**
     * Certificate not valid after date
     */
    private final Instant notValidAfter;
    
    /**
     * Whether certificate is expired
     */
    private final Boolean expired;
    
    /**
     * Whether certificate is revoked
     */
    private final Boolean revoked;
    
    /**
     * Certificate revocation reason
     */
    private final String revocationReason;
    
    /**
     * Certificate chain validation result
     */
    private final Boolean chainValid;
    
    /**
     * Certificate trust anchor
     */
    private final String trustAnchor;
    
    /**
     * Certificate key usage
     */
    private final List<String> keyUsage;
    
    /**
     * Certificate extended key usage
     */
    private final List<String> extendedKeyUsage;
    
    /**
     * Certificate validation errors
     */
    private final List<String> errors;
    
    /**
     * Certificate validation warnings
     */
    private final List<String> warnings;
}
