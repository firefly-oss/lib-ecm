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
 * Authentication level enumeration for signature security.
 */
public enum AuthenticationLevel {
    
    /**
     * No authentication required
     */
    NONE,
    
    /**
     * Basic authentication (email verification)
     */
    BASIC,
    
    /**
     * Standard authentication (email + SMS or phone)
     */
    STANDARD,
    
    /**
     * Enhanced authentication (multiple factors)
     */
    ENHANCED,
    
    /**
     * High security authentication (KBA + ID verification)
     */
    HIGH,
    
    /**
     * Maximum security authentication (all available methods)
     */
    MAXIMUM
}
