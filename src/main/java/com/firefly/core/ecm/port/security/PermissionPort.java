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
package com.firefly.core.ecm.port.security;

import com.firefly.core.ecm.domain.model.security.Permission;
import com.firefly.core.ecm.domain.enums.security.PermissionType;
import com.firefly.core.ecm.domain.enums.security.PrincipalType;
import com.firefly.core.ecm.domain.enums.security.ResourceType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

/**
 * Port interface for permission management operations.
 * Handles access control, permission granting/revoking, and authorization checks.
 */
public interface PermissionPort {
    
    /**
     * Grant a permission to a principal for a resource.
     *
     * @param permission the permission to grant
     * @return Mono containing the granted permission
     */
    Mono<Permission> grantPermission(Permission permission);
    
    /**
     * Revoke a permission by ID.
     *
     * @param permissionId the permission ID to revoke
     * @return Mono indicating completion
     */
    Mono<Void> revokePermission(UUID permissionId);
    
    /**
     * Revoke all permissions for a principal on a resource.
     *
     * @param resourceId the resource ID
     * @param resourceType the resource type
     * @param principalId the principal ID
     * @param principalType the principal type
     * @return Mono indicating completion
     */
    Mono<Void> revokeAllPermissions(UUID resourceId, ResourceType resourceType, Long principalId, PrincipalType principalType);
    
    /**
     * Check if a principal has a specific permission on a resource.
     *
     * @param resourceId the resource ID
     * @param resourceType the resource type
     * @param principalId the principal ID
     * @param principalType the principal type
     * @param permissionType the permission type to check
     * @return Mono containing true if permission is granted, false otherwise
     */
    Mono<Boolean> hasPermission(UUID resourceId, ResourceType resourceType, Long principalId, PrincipalType principalType, PermissionType permissionType);
    
    /**
     * Get all permissions for a resource.
     *
     * @param resourceId the resource ID
     * @param resourceType the resource type
     * @return Flux of permissions for the resource
     */
    Flux<Permission> getResourcePermissions(UUID resourceId, ResourceType resourceType);
    
    /**
     * Get all permissions for a principal.
     *
     * @param principalId the principal ID
     * @param principalType the principal type
     * @return Flux of permissions for the principal
     */
    Flux<Permission> getPrincipalPermissions(Long principalId, PrincipalType principalType);
    
    /**
     * Get effective permissions for a principal on a resource (including inherited).
     *
     * @param resourceId the resource ID
     * @param resourceType the resource type
     * @param principalId the principal ID
     * @param principalType the principal type
     * @return Flux of effective permissions
     */
    Flux<Permission> getEffectivePermissions(UUID resourceId, ResourceType resourceType, Long principalId, PrincipalType principalType);
    
    /**
     * Get all permission types that a principal has on a resource.
     *
     * @param resourceId the resource ID
     * @param resourceType the resource type
     * @param principalId the principal ID
     * @param principalType the principal type
     * @return Mono containing set of permission types
     */
    Mono<Set<PermissionType>> getGrantedPermissionTypes(UUID resourceId, ResourceType resourceType, Long principalId, PrincipalType principalType);
    
    /**
     * Update an existing permission.
     *
     * @param permission the updated permission
     * @return Mono containing the updated permission
     */
    Mono<Permission> updatePermission(Permission permission);
    
    /**
     * Get permission by ID.
     *
     * @param permissionId the permission ID
     * @return Mono containing the permission, empty if not found
     */
    Mono<Permission> getPermission(UUID permissionId);
    
    /**
     * Check if a permission exists.
     *
     * @param permissionId the permission ID
     * @return Mono containing true if permission exists, false otherwise
     */
    Mono<Boolean> existsPermission(UUID permissionId);
    
    /**
     * Copy permissions from one resource to another.
     *
     * @param sourceResourceId the source resource ID
     * @param sourceResourceType the source resource type
     * @param targetResourceId the target resource ID
     * @param targetResourceType the target resource type
     * @return Mono indicating completion
     */
    Mono<Void> copyPermissions(UUID sourceResourceId, ResourceType sourceResourceType, UUID targetResourceId, ResourceType targetResourceType);
    
    /**
     * Inherit permissions from parent resource.
     *
     * @param resourceId the resource ID
     * @param resourceType the resource type
     * @param parentResourceId the parent resource ID
     * @param parentResourceType the parent resource type
     * @return Mono indicating completion
     */
    Mono<Void> inheritPermissions(UUID resourceId, ResourceType resourceType, UUID parentResourceId, ResourceType parentResourceType);
}
