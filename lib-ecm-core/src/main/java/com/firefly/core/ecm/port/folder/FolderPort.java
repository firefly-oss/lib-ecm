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
 * Port interface for folder CRUD operations and hierarchical structure management.
 * Adapters must implement this interface to provide folder management capabilities.
 */
public interface FolderPort {
    
    /**
     * Create a new folder.
     *
     * @param folder the folder metadata
     * @return Mono containing the created folder with assigned ID and path
     */
    Mono<Folder> createFolder(Folder folder);
    
    /**
     * Get folder metadata by ID.
     *
     * @param folderId the folder ID
     * @return Mono containing the folder metadata, empty if not found
     */
    Mono<Folder> getFolder(UUID folderId);
    
    /**
     * Update folder metadata.
     *
     * @param folder the updated folder metadata
     * @return Mono containing the updated folder
     */
    Mono<Folder> updateFolder(Folder folder);
    
    /**
     * Delete a folder by ID.
     *
     * @param folderId the folder ID
     * @param recursive whether to delete all contents recursively
     * @return Mono indicating completion
     */
    Mono<Void> deleteFolder(UUID folderId, Boolean recursive);
    
    /**
     * Check if a folder exists.
     *
     * @param folderId the folder ID
     * @return Mono containing true if folder exists, false otherwise
     */
    Mono<Boolean> existsFolder(UUID folderId);
    
    /**
     * Get root folders (folders with no parent).
     *
     * @return Flux of root folders
     */
    Flux<Folder> getRootFolders();
    
    /**
     * Get child folders of a parent folder.
     *
     * @param parentId the parent folder ID
     * @return Flux of child folders
     */
    Flux<Folder> getChildFolders(UUID parentId);
    
    /**
     * Get all descendant folders of a parent folder.
     *
     * @param parentId the parent folder ID
     * @return Flux of all descendant folders
     */
    Flux<Folder> getDescendantFolders(UUID parentId);
    
    /**
     * Get the parent folder of a folder.
     *
     * @param folderId the folder ID
     * @return Mono containing the parent folder, empty if root folder
     */
    Mono<Folder> getParentFolder(UUID folderId);
    
    /**
     * Get the full path from root to a folder.
     *
     * @param folderId the folder ID
     * @return Flux of folders representing the path from root to target
     */
    Flux<Folder> getFolderPath(UUID folderId);
    
    /**
     * Get folders by owner ID.
     *
     * @param ownerId the owner ID (UUID)
     * @return Flux of folders owned by the user
     */
    Flux<Folder> getFoldersByOwner(UUID ownerId);
    
    /**
     * Move folder to a different parent.
     *
     * @param folderId the folder ID to move
     * @param newParentId the new parent folder ID (null for root)
     * @return Mono containing the updated folder
     */
    Mono<Folder> moveFolder(UUID folderId, UUID newParentId);
    
    /**
     * Copy folder to a different parent.
     *
     * @param folderId the folder ID to copy
     * @param newParentId the new parent folder ID (null for root)
     * @param newName optional new name for the copy
     * @param recursive whether to copy all contents recursively
     * @return Mono containing the copied folder
     */
    Mono<Folder> copyFolder(UUID folderId, UUID newParentId, String newName, Boolean recursive);
    
    /**
     * Get folder by path.
     *
     * @param path the folder path (e.g., "/Documents/Projects/2024")
     * @return Mono containing the folder, empty if not found
     */
    Mono<Folder> getFolderByPath(String path);
    
    /**
     * Check if a folder is empty (contains no documents or subfolders).
     *
     * @param folderId the folder ID
     * @return Mono containing true if folder is empty, false otherwise
     */
    Mono<Boolean> isFolderEmpty(UUID folderId);
}
