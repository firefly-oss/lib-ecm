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

import com.firefly.core.ecm.domain.enums.esignature.AuthenticationLevel;
import com.firefly.core.ecm.domain.enums.esignature.AuthenticationMethod;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Authentication requirements for signature envelopes.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class AuthenticationRequirements {
    
    /**
     * Required authentication methods
     */
    private final List<AuthenticationMethod> requiredMethods;
    
    /**
     * Minimum authentication level required
     */
    private final AuthenticationLevel minimumLevel;
    
    /**
     * Whether ID verification is required
     */
    private final Boolean requireIdVerification;
    
    /**
     * Whether phone verification is required
     */
    private final Boolean requirePhoneVerification;
    
    /**
     * Whether knowledge-based authentication is required
     */
    private final Boolean requireKba;
    
    /**
     * Number of KBA questions required
     */
    private final Integer kbaQuestionCount;
    
    /**
     * Minimum KBA score required (percentage)
     */
    private final Integer minimumKbaScore;
    
    /**
     * Whether digital certificate is required
     */
    private final Boolean requireDigitalCertificate;
    
    /**
     * Whether biometric verification is required
     */
    private final Boolean requireBiometric;
    
    /**
     * Access code for additional security
     */
    private final String accessCode;
    
    /**
     * Whether to require signer to be physically present
     */
    private final Boolean requirePhysicalPresence;
    
    /**
     * Maximum allowed authentication attempts
     */
    private final Integer maxAuthAttempts;
}
