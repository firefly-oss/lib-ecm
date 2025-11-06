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
 * Document version status enumeration.
 */
public enum VersionStatus {
    
    /**
     * Version is active and current
     */
    CURRENT,
    
    /**
     * Version is superseded by a newer version
     */
    SUPERSEDED,
    
    /**
     * Version is archived
     */
    ARCHIVED,
    
    /**
     * Version is deleted
     */
    DELETED,
    
    /**
     * Version is a draft/work in progress
     */
    DRAFT,
    
    /**
     * Version is pending approval
     */
    PENDING_APPROVAL,
    
    /**
     * Version is approved
     */
    APPROVED,
    
    /**
     * Version is rejected
     */
    REJECTED
}
