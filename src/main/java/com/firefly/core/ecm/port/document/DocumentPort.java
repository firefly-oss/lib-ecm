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
 * Port interface for document CRUD operations.
 * Adapters must implement this interface to provide document management capabilities.
 */
public interface DocumentPort {
    
    /**
     * Create a new document with content.
     *
     * @param document the document metadata
     * @param content the document content as byte array
     * @return Mono containing the created document with assigned ID and storage path
     */
    Mono<Document> createDocument(Document document, byte[] content);
    
    /**
     * Get document metadata by ID.
     *
     * @param documentId the document ID
     * @return Mono containing the document metadata, empty if not found
     */
    Mono<Document> getDocument(UUID documentId);
    
    /**
     * Update document metadata.
     *
     * @param document the updated document metadata
     * @return Mono containing the updated document
     */
    Mono<Document> updateDocument(Document document);
    
    /**
     * Delete a document by ID.
     *
     * @param documentId the document ID
     * @return Mono indicating completion
     */
    Mono<Void> deleteDocument(UUID documentId);
    
    /**
     * Check if a document exists.
     *
     * @param documentId the document ID
     * @return Mono containing true if document exists, false otherwise
     */
    Mono<Boolean> existsDocument(UUID documentId);
    
    /**
     * Get documents by folder ID.
     *
     * @param folderId the folder ID
     * @return Flux of documents in the folder
     */
    Flux<Document> getDocumentsByFolder(UUID folderId);
    
    /**
     * Get documents by owner ID.
     *
     * @param ownerId the owner ID
     * @return Flux of documents owned by the user
     */
    Flux<Document> getDocumentsByOwner(Long ownerId);
    
    /**
     * Get documents by status.
     *
     * @param status the document status
     * @return Flux of documents with the specified status
     */
    Flux<Document> getDocumentsByStatus(DocumentStatus status);
    
    /**
     * Move document to a different folder.
     *
     * @param documentId the document ID
     * @param targetFolderId the target folder ID
     * @return Mono containing the updated document
     */
    Mono<Document> moveDocument(UUID documentId, UUID targetFolderId);
    
    /**
     * Copy document to a different folder.
     *
     * @param documentId the document ID
     * @param targetFolderId the target folder ID
     * @param newName optional new name for the copy
     * @return Mono containing the copied document
     */
    Mono<Document> copyDocument(UUID documentId, UUID targetFolderId, String newName);
    
    /**
     * Get the adapter name for identification.
     *
     * @return the adapter name
     */
    String getAdapterName();
}
