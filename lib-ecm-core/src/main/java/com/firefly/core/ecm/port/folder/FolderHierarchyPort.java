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
package com.firefly.core.ecm.port.folder;

import com.firefly.core.ecm.domain.model.folder.Folder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port interface for advanced folder hierarchy operations.
 * Provides specialized operations for managing complex folder structures.
 */
public interface FolderHierarchyPort {
    
    /**
     * Get the complete folder tree starting from a root folder.
     *
     * @param rootFolderId the root folder ID (null for all root folders)
     * @param maxDepth maximum depth to traverse (null for unlimited)
     * @return Flux of folders in hierarchical order
     */
    Flux<Folder> getFolderTree(UUID rootFolderId, Integer maxDepth);
    
    /**
     * Get folders at a specific depth level.
     *
     * @param level the depth level (0 for root folders)
     * @return Flux of folders at the specified level
     */
    Flux<Folder> getFoldersAtLevel(Integer level);
    
    /**
     * Calculate the depth of a folder in the hierarchy.
     *
     * @param folderId the folder ID
     * @return Mono containing the folder depth (0 for root folders)
     */
    Mono<Integer> getFolderDepth(UUID folderId);
    
    /**
     * Get all ancestor folders of a folder (parent, grandparent, etc.).
     *
     * @param folderId the folder ID
     * @return Flux of ancestor folders ordered from immediate parent to root
     */
    Flux<Folder> getAncestorFolders(UUID folderId);
    
    /**
     * Check if one folder is an ancestor of another.
     *
     * @param ancestorId the potential ancestor folder ID
     * @param descendantId the potential descendant folder ID
     * @return Mono containing true if ancestorId is an ancestor of descendantId
     */
    Mono<Boolean> isAncestor(UUID ancestorId, UUID descendantId);
    
    /**
     * Check if one folder is a descendant of another.
     *
     * @param descendantId the potential descendant folder ID
     * @param ancestorId the potential ancestor folder ID
     * @return Mono containing true if descendantId is a descendant of ancestorId
     */
    Mono<Boolean> isDescendant(UUID descendantId, UUID ancestorId);
    
    /**
     * Get sibling folders (folders with the same parent).
     *
     * @param folderId the folder ID
     * @return Flux of sibling folders (excluding the folder itself)
     */
    Flux<Folder> getSiblingFolders(UUID folderId);
    
    /**
     * Get the total count of documents in a folder and all its subfolders.
     *
     * @param folderId the folder ID
     * @return Mono containing the total document count
     */
    Mono<Long> getTotalDocumentCount(UUID folderId);
    
    /**
     * Get the total size of all documents in a folder and its subfolders.
     *
     * @param folderId the folder ID
     * @return Mono containing the total size in bytes
     */
    Mono<UUID> getTotalFolderSize(UUID folderId);
    
    /**
     * Get the count of subfolders (direct children only).
     *
     * @param folderId the folder ID
     * @return Mono containing the subfolder count
     */
    Mono<Long> getSubfolderCount(UUID folderId);
    
    /**
     * Get the count of all descendant folders.
     *
     * @param folderId the folder ID
     * @return Mono containing the total descendant folder count
     */
    Mono<Long> getDescendantFolderCount(UUID folderId);
    
    /**
     * Validate folder hierarchy integrity.
     *
     * @param folderId the folder ID to validate (null to validate entire hierarchy)
     * @return Mono containing true if hierarchy is valid, false otherwise
     */
    Mono<Boolean> validateHierarchy(UUID folderId);
    
    /**
     * Rebuild folder paths for a subtree.
     *
     * @param rootFolderId the root folder ID to rebuild paths from
     * @return Mono indicating completion
     */
    Mono<Void> rebuildPaths(UUID rootFolderId);
    
    /**
     * Find the common ancestor of multiple folders.
     *
     * @param folderIds the folder IDs to find common ancestor for
     * @return Mono containing the common ancestor folder, empty if none
     */
    Mono<Folder> findCommonAncestor(java.util.Set<UUID> folderIds);
}
