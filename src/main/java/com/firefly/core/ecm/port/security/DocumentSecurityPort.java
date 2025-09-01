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

import com.firefly.core.ecm.domain.model.document.Document;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port interface for document security operations.
 * Handles document encryption, access validation, and security policies.
 */
public interface DocumentSecurityPort {
    
    /**
     * Encrypt document content.
     *
     * @param documentId the document ID
     * @param content the content to encrypt
     * @param encryptionKey the encryption key
     * @return Mono containing the encrypted content
     */
    Mono<byte[]> encryptContent(UUID documentId, byte[] content, String encryptionKey);
    
    /**
     * Decrypt document content.
     *
     * @param documentId the document ID
     * @param encryptedContent the encrypted content
     * @param decryptionKey the decryption key
     * @return Mono containing the decrypted content
     */
    Mono<byte[]> decryptContent(UUID documentId, byte[] encryptedContent, String decryptionKey);
    
    /**
     * Check if a user can access a document.
     *
     * @param documentId the document ID
     * @param userId the user ID
     * @param operation the operation being performed
     * @return Mono containing true if access is allowed, false otherwise
     */
    Mono<Boolean> canAccessDocument(UUID documentId, Long userId, String operation);
    
    /**
     * Validate document access based on security policies.
     *
     * @param document the document
     * @param userId the user ID
     * @param ipAddress the client IP address
     * @param userAgent the client user agent
     * @return Mono containing true if access is valid, false otherwise
     */
    Mono<Boolean> validateAccess(Document document, Long userId, String ipAddress, String userAgent);
    
    /**
     * Apply security classification to a document.
     *
     * @param documentId the document ID
     * @param classification the security classification
     * @param userId the user applying the classification
     * @return Mono containing the updated document
     */
    Mono<Document> applySecurityClassification(UUID documentId, String classification, Long userId);
    
    /**
     * Check if a document is under legal hold.
     *
     * @param documentId the document ID
     * @return Mono containing true if under legal hold, false otherwise
     */
    Mono<Boolean> isUnderLegalHold(UUID documentId);
    
    /**
     * Apply legal hold to a document.
     *
     * @param documentId the document ID
     * @param holdReason the reason for the legal hold
     * @param userId the user applying the hold
     * @return Mono containing the updated document
     */
    Mono<Document> applyLegalHold(UUID documentId, String holdReason, Long userId);
    
    /**
     * Remove legal hold from a document.
     *
     * @param documentId the document ID
     * @param userId the user removing the hold
     * @return Mono containing the updated document
     */
    Mono<Document> removeLegalHold(UUID documentId, Long userId);
    
    /**
     * Check if a document can be deleted based on security policies.
     *
     * @param documentId the document ID
     * @param userId the user attempting deletion
     * @return Mono containing true if deletion is allowed, false otherwise
     */
    Mono<Boolean> canDeleteDocument(UUID documentId, Long userId);
    
    /**
     * Check if a document can be modified based on security policies.
     *
     * @param documentId the document ID
     * @param userId the user attempting modification
     * @return Mono containing true if modification is allowed, false otherwise
     */
    Mono<Boolean> canModifyDocument(UUID documentId, Long userId);
    
    /**
     * Get documents that are under legal hold.
     *
     * @return Flux of documents under legal hold
     */
    Flux<Document> getDocumentsUnderLegalHold();
    
    /**
     * Get documents by security classification.
     *
     * @param classification the security classification
     * @return Flux of documents with the specified classification
     */
    Flux<Document> getDocumentsByClassification(String classification);
    
    /**
     * Scan document content for security threats.
     *
     * @param documentId the document ID
     * @param content the document content
     * @return Mono containing true if content is safe, false if threats detected
     */
    Mono<Boolean> scanForThreats(UUID documentId, byte[] content);
    
    /**
     * Quarantine a document due to security concerns.
     *
     * @param documentId the document ID
     * @param reason the quarantine reason
     * @param userId the user initiating quarantine
     * @return Mono containing the updated document
     */
    Mono<Document> quarantineDocument(UUID documentId, String reason, Long userId);
    
    /**
     * Release a document from quarantine.
     *
     * @param documentId the document ID
     * @param userId the user releasing from quarantine
     * @return Mono containing the updated document
     */
    Mono<Document> releaseFromQuarantine(UUID documentId, Long userId);
}
