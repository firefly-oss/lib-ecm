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

/**
 * Enumeration of ECM adapter configuration profiles.
 * Defines the minimum configuration level required for an adapter.
 */
public enum AdapterProfile {
    
    /**
     * Basic profile with minimal configuration requirements.
     * Suitable for simple file storage adapters.
     */
    BASIC,
    
    /**
     * Standard profile with moderate configuration requirements.
     * Suitable for cloud storage adapters with authentication.
     */
    STANDARD,
    
    /**
     * Advanced profile with comprehensive configuration requirements.
     * Suitable for enterprise ECM systems with complex setup.
     */
    ADVANCED,
    
    /**
     * Enterprise profile with full configuration requirements.
     * Suitable for enterprise-grade ECM systems with security and compliance features.
     */
    ENTERPRISE
}
