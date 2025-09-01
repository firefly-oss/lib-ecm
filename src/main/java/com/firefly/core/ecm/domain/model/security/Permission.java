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
package com.firefly.core.ecm.domain.model.security;

import com.firefly.core.ecm.domain.enums.security.PermissionType;
import com.firefly.core.ecm.domain.enums.security.PrincipalType;
import com.firefly.core.ecm.domain.enums.security.ResourceType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

/**
 * Permission entity representing access control for documents and folders.
 * Uses UUID for resource IDs and Long for user/group IDs as per Firefly standards.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class Permission {
    
    /**
     * Unique permission identifier (UUID)
     */
    private final UUID id;
    
    /**
     * Resource ID (document or folder UUID)
     */
    private final UUID resourceId;
    
    /**
     * Resource type (DOCUMENT or FOLDER)
     */
    private final ResourceType resourceType;
    
    /**
     * Principal ID (user or group Long ID)
     */
    private final Long principalId;
    
    /**
     * Principal type (USER or GROUP)
     */
    private final PrincipalType principalType;
    
    /**
     * Permission type
     */
    private final PermissionType permissionType;
    
    /**
     * Whether permission is granted or denied
     */
    private final Boolean granted;
    
    /**
     * User who granted this permission (Long)
     */
    private final Long grantedBy;
    
    /**
     * Permission grant timestamp
     */
    private final Instant grantedAt;
    
    /**
     * Permission expiration timestamp (optional)
     */
    private final Instant expiresAt;
    
    /**
     * Whether this permission is inherited from parent
     */
    private final Boolean inherited;
    
    /**
     * Parent permission ID if inherited
     */
    private final UUID parentPermissionId;
    
    /**
     * Permission conditions/constraints
     */
    private final String conditions;
    
    /**
     * Whether this permission can be delegated
     */
    private final Boolean delegatable;
    
    /**
     * Permission priority (higher number = higher priority)
     */
    private final Integer priority;
}
