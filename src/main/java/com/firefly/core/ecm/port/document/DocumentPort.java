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

import com.firefly.core.ecm.domain.model.document.Document;
import com.firefly.core.ecm.domain.enums.document.DocumentStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port interface defining core document CRUD (Create, Read, Update, Delete) operations.
 *
 * <p>This interface represents the primary business contract for document management
 * in the Firefly ECM system. It follows the hexagonal architecture pattern where
 * this port defines the business requirements, and adapters provide concrete
 * implementations for different storage backends.</p>
 *
 * <p>Key features provided by this port:</p>
 * <ul>
 *   <li>Complete document lifecycle management (create, read, update, delete)</li>
 *   <li>Document organization and movement between folders</li>
 *   <li>Document discovery by various criteria (folder, owner, status)</li>
 *   <li>Document copying and duplication</li>
 *   <li>Reactive programming model using Project Reactor</li>
 * </ul>
 *
 * <p>All operations are designed to be non-blocking and return reactive types
 * ({@link Mono} for single results, {@link Flux} for multiple results) to support
 * high-performance, scalable applications.</p>
 *
 * <p>Implementations must handle:</p>
 * <ul>
 *   <li>Document metadata persistence</li>
 *   <li>Content storage coordination</li>
 *   <li>Unique ID generation and assignment</li>
 *   <li>Audit trail maintenance</li>
 *   <li>Error handling and validation</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 * @see Document
 * @see DocumentContentPort
 * @see DocumentVersionPort
 * @see reactor.core.publisher.Mono
 * @see reactor.core.publisher.Flux
 */
public interface DocumentPort {

    /**
     * Creates a new document with both metadata and content in a single operation.
     *
     * <p>This method handles the complete document creation process, including:</p>
     * <ul>
     *   <li>Generating a unique document ID if not provided</li>
     *   <li>Storing the document content in the backend storage</li>
     *   <li>Persisting document metadata</li>
     *   <li>Setting creation timestamps and audit information</li>
     *   <li>Calculating and storing content checksums</li>
     * </ul>
     *
     * <p>The returned document will have all system-generated fields populated,
     * including the assigned ID, storage path, creation timestamps, and checksum.</p>
     *
     * @param document the document metadata to create; ID may be null for auto-generation
     * @param content the binary content of the document as a byte array
     * @return a Mono containing the created document with all system fields populated
     * @throws IllegalArgumentException if document or content is null
     */
    Mono<Document> createDocument(Document document, byte[] content);

    /**
     * Retrieves document metadata by its unique identifier.
     *
     * <p>This method returns only the document metadata without the binary content.
     * Use {@link DocumentContentPort#getContent(UUID)} to retrieve the actual
     * document content separately.</p>
     *
     * @param documentId the unique Long identifier of the document
     * @return a Mono containing the document metadata if found, empty Mono if not found
     * @throws IllegalArgumentException if documentId is null
     * @see DocumentContentPort#getContent(UUID)
     */
    Mono<Document> getDocument(UUID documentId);

    /**
     * Updates the metadata of an existing document.
     *
     * <p>This method updates only the document metadata; it does not modify the
     * document content. The document ID must be provided and must reference an
     * existing document. System fields like creation timestamp and ID cannot be
     * modified through this method.</p>
     *
     * <p>The modification timestamp and modified-by user will be automatically
     * updated by the implementation.</p>
     *
     * @param document the document with updated metadata; must include valid ID
     * @return a Mono containing the updated document with refreshed system fields
     * @throws IllegalArgumentException if document or document ID is null
     */
    Mono<Document> updateDocument(Document document);

    /**
     * Permanently deletes a document and its content.
     *
     * <p>This operation removes both the document metadata and its associated
     * binary content from the storage system. The operation is irreversible
     * unless the document is under legal hold or retention policies prevent deletion.</p>
     *
     * <p>Implementations should verify that the document can be legally deleted
     * before proceeding with the deletion.</p>
     *
     * @param documentId the unique Long identifier of the document to delete
     * @return a Mono that completes when the deletion is finished
     * @throws IllegalArgumentException if documentId is null
     */
    Mono<Void> deleteDocument(UUID documentId);

    /**
     * Checks whether a document exists in the system.
     *
     * <p>This is a lightweight operation that verifies document existence without
     * retrieving the full document metadata. Useful for validation and conditional
     * logic before performing other operations.</p>
     *
     * @param documentId the unique Long identifier of the document to check
     * @return a Mono containing true if the document exists, false otherwise
     * @throws IllegalArgumentException if documentId is null
     */
    Mono<Boolean> existsDocument(UUID documentId);

    /**
     * Retrieves all documents contained within a specific folder.
     *
     * <p>Returns a stream of documents that are directly contained in the specified
     * folder. This does not include documents in subfolders unless the implementation
     * specifically supports recursive folder traversal.</p>
     *
     * @param folderId the unique Long identifier of the folder
     * @return a Flux of documents contained in the folder, empty if folder is empty or doesn't exist
     * @throws IllegalArgumentException if folderId is null
     */
    Flux<Document> getDocumentsByFolder(UUID folderId);

    /**
     * Retrieves all documents owned by a specific user.
     *
     * <p>Returns a stream of documents where the specified user is the owner.
     * This includes documents across all folders that the user owns, regardless
     * of their current location in the folder hierarchy.</p>
     *
     * @param ownerId the Long identifier of the document owner
     * @return a Flux of documents owned by the specified user
     * @throws IllegalArgumentException if ownerId is null
     */
    Flux<Document> getDocumentsByOwner(UUID ownerId);
    
    /**
     * Retrieves all documents with a specific lifecycle status.
     *
     * <p>Returns a stream of documents that are currently in the specified status
     * (e.g., DRAFT, ACTIVE, ARCHIVED, DELETED). This is useful for administrative
     * operations, reporting, and automated processing workflows.</p>
     *
     * @param status the document status to filter by
     * @return a Flux of documents with the specified status
     * @throws IllegalArgumentException if status is null
     * @see DocumentStatus
     */
    Flux<Document> getDocumentsByStatus(DocumentStatus status);

    /**
     * Moves a document from its current folder to a different target folder.
     *
     * <p>This operation changes the document's folder location without creating
     * a copy. The document retains its ID, content, and all other metadata except
     * for the folder reference and modification timestamp.</p>
     *
     * <p>The operation may fail if:</p>
     * <ul>
     *   <li>The user lacks permission to move the document</li>
     *   <li>The target folder doesn't exist or is inaccessible</li>
     *   <li>The document is under policies that prevent movement</li>
     * </ul>
     *
     * @param documentId the unique Long identifier of the document to move
     * @param targetFolderId the unique Long identifier of the destination folder
     * @return a Mono containing the updated document with new folder reference
     * @throws IllegalArgumentException if documentId or targetFolderId is null
     */
    Mono<Document> moveDocument(UUID documentId, UUID targetFolderId);

    /**
     * Creates a copy of a document in a different folder, optionally with a new name.
     *
     * <p>This operation creates a complete copy of the document including both
     * metadata and content. The copy receives a new unique ID and is placed in
     * the specified target folder. If a new name is provided, the copy uses that
     * name; otherwise, it uses the original document's name.</p>
     *
     * <p>The copy operation preserves:</p>
     * <ul>
     *   <li>Document content (binary data)</li>
     *   <li>Most metadata fields</li>
     *   <li>Tags and custom properties</li>
     * </ul>
     *
     * <p>The copy receives new values for:</p>
     * <ul>
     *   <li>Document ID (newly generated UUID)</li>
     *   <li>Creation and modification timestamps</li>
     *   <li>Version number (reset to 1)</li>
     *   <li>Storage path (new location in backend storage)</li>
     * </ul>
     *
     * @param documentId the unique Long identifier of the document to copy
     * @param targetFolderId the unique Long identifier of the destination folder
     * @param newName optional new name for the copied document; if null, uses original name
     * @return a Mono containing the newly created document copy
     * @throws IllegalArgumentException if documentId or targetFolderId is null
     */
    Mono<Document> copyDocument(UUID documentId, UUID targetFolderId, String newName);

    /**
     * Returns the name of the adapter implementation for identification and logging.
     *
     * <p>This method provides a way to identify which specific adapter implementation
     * is being used. The name should be descriptive and unique among all available
     * adapters (e.g., "S3DocumentAdapter", "AzureBlobDocumentAdapter").</p>
     *
     * <p>This information is useful for:</p>
     * <ul>
     *   <li>Logging and debugging</li>
     *   <li>Administrative monitoring</li>
     *   <li>Feature capability detection</li>
     *   <li>Error reporting and diagnostics</li>
     * </ul>
     *
     * @return a non-null string identifying the adapter implementation
     */
    String getAdapterName();
}
