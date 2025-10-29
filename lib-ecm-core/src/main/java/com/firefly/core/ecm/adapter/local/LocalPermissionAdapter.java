/*
 * Copyright 2025 Firefly Software Solutions Inc.
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

package com.firefly.core.ecm.adapter.local;

import com.firefly.core.ecm.adapter.AdapterFeature;
import com.firefly.core.ecm.adapter.EcmAdapter;
import com.firefly.core.ecm.domain.enums.security.PermissionType;
import com.firefly.core.ecm.domain.enums.security.PrincipalType;
import com.firefly.core.ecm.domain.enums.security.ResourceType;
import com.firefly.core.ecm.domain.model.security.Permission;
import com.firefly.core.ecm.port.security.PermissionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local in-memory implementation of PermissionPort for development/testing.
 * Provides a functional, non-stub adapter to satisfy hexagonal port contracts.
 */
@Slf4j
@Component
@EcmAdapter(
        type = "local-permissions",
        description = "Local in-memory PermissionPort adapter",
        supportedFeatures = { AdapterFeature.PERMISSIONS }
)
@ConditionalOnProperty(name = "firefly.ecm.permissions.enabled", havingValue = "true", matchIfMissing = true)
public class LocalPermissionAdapter implements PermissionPort {

    private final Map<UUID, Permission> store = new ConcurrentHashMap<>();

    @Override
    public Mono<Permission> grantPermission(Permission permission) {
        UUID id = permission.getId() != null ? permission.getId() : UUID.randomUUID();
        Permission toSave = permission.toBuilder()
                .id(id)
                .grantedAt(permission.getGrantedAt() != null ? permission.getGrantedAt() : Instant.now())
                .build();
        store.put(id, toSave);
        log.debug("Granted permission {} for resource {} to principal {}", toSave.getPermissionType(), toSave.getResourceId(), toSave.getPrincipalId());
        return Mono.just(toSave);
    }

    @Override
    public Mono<Void> revokePermission(UUID permissionId) {
        store.remove(permissionId);
        return Mono.empty();
    }

    @Override
    public Mono<Void> revokeAllPermissions(UUID resourceId, ResourceType resourceType, UUID principalId, PrincipalType principalType) {
        store.values().removeIf(p -> Objects.equals(p.getResourceId(), resourceId)
                && p.getResourceType() == resourceType
                && Objects.equals(p.getPrincipalId(), principalId)
                && p.getPrincipalType() == principalType);
        return Mono.empty();
    }

    @Override
    public Mono<Boolean> hasPermission(UUID resourceId, ResourceType resourceType, UUID principalId, PrincipalType principalType, PermissionType permissionType) {
        Instant now = Instant.now();
        boolean result = store.values().stream().anyMatch(p ->
                Objects.equals(p.getResourceId(), resourceId)
                        && p.getResourceType() == resourceType
                        && Objects.equals(p.getPrincipalId(), principalId)
                        && p.getPrincipalType() == principalType
                        && p.getPermissionType() == permissionType
                        && Boolean.TRUE.equals(p.getGranted())
                        && (p.getExpiresAt() == null || p.getExpiresAt().isAfter(now))
        );
        return Mono.just(result);
    }

    @Override
    public Flux<Permission> getResourcePermissions(UUID resourceId, ResourceType resourceType) {
        return Flux.fromStream(store.values().stream()
                .filter(p -> Objects.equals(p.getResourceId(), resourceId) && p.getResourceType() == resourceType));
    }

    @Override
    public Flux<Permission> getPrincipalPermissions(UUID principalId, PrincipalType principalType) {
        return Flux.fromStream(store.values().stream()
                .filter(p -> Objects.equals(p.getPrincipalId(), principalId) && p.getPrincipalType() == principalType));
    }

    @Override
    public Flux<Permission> getEffectivePermissions(UUID resourceId, ResourceType resourceType, UUID principalId, PrincipalType principalType) {
        // For local adapter, effective == direct (no inheritance beyond stored entries)
        return getResourcePermissions(resourceId, resourceType)
                .filter(p -> Objects.equals(p.getPrincipalId(), principalId) && p.getPrincipalType() == principalType);
    }

    @Override
    public Mono<Set<PermissionType>> getGrantedPermissionTypes(UUID resourceId, ResourceType resourceType, UUID principalId, PrincipalType principalType) {
        Set<PermissionType> types = new HashSet<>();
        store.values().forEach(p -> {
            if (Objects.equals(p.getResourceId(), resourceId)
                    && p.getResourceType() == resourceType
                    && Objects.equals(p.getPrincipalId(), principalId)
                    && p.getPrincipalType() == principalType
                    && Boolean.TRUE.equals(p.getGranted())) {
                types.add(p.getPermissionType());
            }
        });
        return Mono.just(types);
    }

    @Override
    public Mono<Permission> updatePermission(Permission permission) {
        if (permission.getId() == null) {
            return Mono.error(new IllegalArgumentException("Permission ID is required for update"));
        }
        store.put(permission.getId(), permission);
        return Mono.just(permission);
    }

    @Override
    public Mono<Permission> getPermission(UUID permissionId) {
        return Mono.justOrEmpty(store.get(permissionId));
    }

    @Override
    public Mono<Boolean> existsPermission(UUID permissionId) {
        return Mono.just(store.containsKey(permissionId));
    }

    @Override
    public Mono<Void> copyPermissions(UUID sourceResourceId, ResourceType sourceResourceType, UUID targetResourceId, ResourceType targetResourceType) {
        store.values().stream()
                .filter(p -> Objects.equals(p.getResourceId(), sourceResourceId) && p.getResourceType() == sourceResourceType)
                .forEach(p -> {
                    UUID newId = UUID.randomUUID();
                    Permission copy = p.toBuilder()
                            .id(newId)
                            .resourceId(targetResourceId)
                            .resourceType(targetResourceType)
                            .inherited(true)
                            .parentPermissionId(p.getId())
                            .build();
                    store.put(newId, copy);
                });
        return Mono.empty();
    }

    @Override
    public Mono<Void> inheritPermissions(UUID resourceId, ResourceType resourceType, UUID parentResourceId, ResourceType parentResourceType) {
        // Simple inheritance: copy from parent
        return copyPermissions(parentResourceId, parentResourceType, resourceId, resourceType);
    }
}