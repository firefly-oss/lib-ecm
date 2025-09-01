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
package com.firefly.core.ecm.domain.enums.security;

/**
 * Resource type enumeration for permissions and audit.
 */
public enum ResourceType {
    
    /**
     * Document resource
     */
    DOCUMENT,
    
    /**
     * Folder resource
     */
    FOLDER,
    
    /**
     * Document version resource
     */
    DOCUMENT_VERSION,
    
    /**
     * Signature envelope resource
     */
    SIGNATURE_ENVELOPE,
    
    /**
     * Signature request resource
     */
    SIGNATURE_REQUEST,
    
    /**
     * System resource
     */
    SYSTEM,
    
    /**
     * User resource
     */
    USER,
    
    /**
     * Group resource
     */
    GROUP
}
