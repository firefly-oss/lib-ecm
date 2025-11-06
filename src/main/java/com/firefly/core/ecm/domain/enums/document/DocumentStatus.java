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
package com.firefly.core.ecm.domain.enums.document;

/**
 * Document status enumeration.
 */
public enum DocumentStatus {
    
    /**
     * Document is being created/uploaded
     */
    CREATING,
    
    /**
     * Document is active and available
     */
    ACTIVE,
    
    /**
     * Document is checked out for editing
     */
    CHECKED_OUT,
    
    /**
     * Document is locked for exclusive access
     */
    LOCKED,
    
    /**
     * Document is archived
     */
    ARCHIVED,
    
    /**
     * Document is deleted (soft delete)
     */
    DELETED,
    
    /**
     * Document is under review
     */
    UNDER_REVIEW,
    
    /**
     * Document is approved
     */
    APPROVED,
    
    /**
     * Document is rejected
     */
    REJECTED,
    
    /**
     * Document is expired
     */
    EXPIRED,
    
    /**
     * Document is quarantined due to security issues
     */
    QUARANTINED,
    
    /**
     * Document is corrupted or invalid
     */
    CORRUPTED
}
