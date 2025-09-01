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
 * Permission type enumeration defining specific access rights.
 */
public enum PermissionType {
    
    /**
     * Read access to document/folder content
     */
    READ,
    
    /**
     * Write/modify access to document/folder
     */
    WRITE,
    
    /**
     * Delete access to document/folder
     */
    DELETE,
    
    /**
     * Share/grant permissions to others
     */
    SHARE,
    
    /**
     * Execute/download document
     */
    EXECUTE,
    
    /**
     * Create new documents in folder
     */
    CREATE,
    
    /**
     * Move documents/folders
     */
    MOVE,
    
    /**
     * Copy documents/folders
     */
    COPY,
    
    /**
     * View document metadata
     */
    VIEW_METADATA,
    
    /**
     * Modify document metadata
     */
    MODIFY_METADATA,
    
    /**
     * View document versions
     */
    VIEW_VERSIONS,
    
    /**
     * Create new document versions
     */
    CREATE_VERSION,
    
    /**
     * View document audit trail
     */
    VIEW_AUDIT,
    
    /**
     * Manage document permissions
     */
    MANAGE_PERMISSIONS,
    
    /**
     * Check out/lock document
     */
    CHECKOUT,
    
    /**
     * Check in/unlock document
     */
    CHECKIN,
    
    /**
     * Sign documents (eSignature)
     */
    SIGN,
    
    /**
     * Send for signature
     */
    SEND_FOR_SIGNATURE,
    
    /**
     * Full administrative access
     */
    ADMIN
}
