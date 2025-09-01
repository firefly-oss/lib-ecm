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
package com.firefly.core.ecm.port.document;

import com.firefly.core.ecm.domain.model.document.DocumentVersion;
import com.firefly.core.ecm.domain.enums.document.VersionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port interface for document version management operations.
 * Handles document versioning, history, and rollback capabilities.
 */
public interface DocumentVersionPort {
    
    /**
     * Create a new version of a document.
     *
     * @param documentVersion the version metadata
     * @param content the version content as byte array
     * @return Mono containing the created document version
     */
    Mono<DocumentVersion> createVersion(DocumentVersion documentVersion, byte[] content);
    
    /**
     * Get a specific document version by ID.
     *
     * @param versionId the version ID
     * @return Mono containing the document version, empty if not found
     */
    Mono<DocumentVersion> getVersion(UUID versionId);
    
    /**
     * Get the current/active version of a document.
     *
     * @param documentId the document ID
     * @return Mono containing the current version, empty if not found
     */
    Mono<DocumentVersion> getCurrentVersion(UUID documentId);
    
    /**
     * Get all versions of a document.
     *
     * @param documentId the document ID
     * @return Flux of document versions ordered by version number
     */
    Flux<DocumentVersion> getVersionHistory(UUID documentId);
    
    /**
     * Get versions by type.
     *
     * @param documentId the document ID
     * @param versionType the version type
     * @return Flux of document versions of the specified type
     */
    Flux<DocumentVersion> getVersionsByType(UUID documentId, VersionType versionType);
    
    /**
     * Set a version as the current/active version.
     *
     * @param versionId the version ID to make current
     * @return Mono containing the updated version
     */
    Mono<DocumentVersion> setCurrentVersion(UUID versionId);
    
    /**
     * Delete a specific version.
     *
     * @param versionId the version ID
     * @return Mono indicating completion
     */
    Mono<Void> deleteVersion(UUID versionId);
    
    /**
     * Get the next version number for a document.
     *
     * @param documentId the document ID
     * @return Mono containing the next version number
     */
    Mono<Integer> getNextVersionNumber(UUID documentId);
    
    /**
     * Get version by document ID and version number.
     *
     * @param documentId the document ID
     * @param versionNumber the version number
     * @return Mono containing the document version, empty if not found
     */
    Mono<DocumentVersion> getVersionByNumber(UUID documentId, Integer versionNumber);
    
    /**
     * Get the previous version of a specific version.
     *
     * @param versionId the current version ID
     * @return Mono containing the previous version, empty if none
     */
    Mono<DocumentVersion> getPreviousVersion(UUID versionId);
    
    /**
     * Get the next version of a specific version.
     *
     * @param versionId the current version ID
     * @return Mono containing the next version, empty if none
     */
    Mono<DocumentVersion> getNextVersion(UUID versionId);
    
    /**
     * Compare two versions and get differences.
     *
     * @param fromVersionId the source version ID
     * @param toVersionId the target version ID
     * @return Mono containing version comparison result
     */
    Mono<String> compareVersions(UUID fromVersionId, UUID toVersionId);
    
    /**
     * Restore a document to a specific version.
     *
     * @param documentId the document ID
     * @param versionId the version ID to restore to
     * @param createBackup whether to create a backup of current version
     * @return Mono containing the restored version
     */
    Mono<DocumentVersion> restoreVersion(UUID documentId, UUID versionId, Boolean createBackup);
}
