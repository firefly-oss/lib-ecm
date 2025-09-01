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
package com.firefly.core.ecm.domain.enums.esignature;

/**
 * Authentication method enumeration for signature verification.
 */
public enum AuthenticationMethod {
    
    /**
     * Email-based authentication
     */
    EMAIL,
    
    /**
     * SMS-based authentication
     */
    SMS,
    
    /**
     * Phone call authentication
     */
    PHONE,
    
    /**
     * Knowledge-based authentication (KBA)
     */
    KBA,
    
    /**
     * ID verification with document upload
     */
    ID_VERIFICATION,
    
    /**
     * Digital certificate authentication
     */
    DIGITAL_CERTIFICATE,
    
    /**
     * Biometric authentication
     */
    BIOMETRIC,
    
    /**
     * Two-factor authentication
     */
    TWO_FACTOR,
    
    /**
     * OAuth-based authentication
     */
    OAUTH,
    
    /**
     * SAML-based authentication
     */
    SAML,
    
    /**
     * No additional authentication required
     */
    NONE
}
