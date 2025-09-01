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

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port interface for document content storage and retrieval operations.
 * Handles the actual binary content of documents with streaming support.
 */
public interface DocumentContentPort {
    
    /**
     * Store document content.
     *
     * @param documentId the document ID
     * @param content the document content as byte array
     * @param mimeType the MIME type of the content
     * @return Mono containing the storage path where content was stored
     */
    Mono<String> storeContent(UUID documentId, byte[] content, String mimeType);
    
    /**
     * Store document content with streaming support.
     *
     * @param documentId the document ID
     * @param contentStream the document content as DataBuffer stream
     * @param mimeType the MIME type of the content
     * @param contentLength the expected content length in bytes
     * @return Mono containing the storage path where content was stored
     */
    Mono<String> storeContentStream(UUID documentId, Flux<DataBuffer> contentStream, String mimeType, Long contentLength);
    
    /**
     * Retrieve document content as byte array.
     *
     * @param documentId the document ID
     * @return Mono containing the document content, empty if not found
     */
    Mono<byte[]> getContent(UUID documentId);
    
    /**
     * Retrieve document content as streaming DataBuffer.
     *
     * @param documentId the document ID
     * @return Flux of DataBuffer containing the document content
     */
    Flux<DataBuffer> getContentStream(UUID documentId);
    
    /**
     * Retrieve document content by storage path.
     *
     * @param storagePath the storage path
     * @return Mono containing the document content, empty if not found
     */
    Mono<byte[]> getContentByPath(String storagePath);
    
    /**
     * Retrieve document content stream by storage path.
     *
     * @param storagePath the storage path
     * @return Flux of DataBuffer containing the document content
     */
    Flux<DataBuffer> getContentStreamByPath(String storagePath);
    
    /**
     * Delete document content.
     *
     * @param documentId the document ID
     * @return Mono indicating completion
     */
    Mono<Void> deleteContent(UUID documentId);
    
    /**
     * Delete document content by storage path.
     *
     * @param storagePath the storage path
     * @return Mono indicating completion
     */
    Mono<Void> deleteContentByPath(String storagePath);
    
    /**
     * Check if document content exists.
     *
     * @param documentId the document ID
     * @return Mono containing true if content exists, false otherwise
     */
    Mono<Boolean> existsContent(UUID documentId);
    
    /**
     * Get content size in bytes.
     *
     * @param documentId the document ID
     * @return Mono containing the content size, empty if not found
     */
    Mono<Long> getContentSize(UUID documentId);
    
    /**
     * Calculate content checksum.
     *
     * @param documentId the document ID
     * @param algorithm the checksum algorithm (e.g., "SHA-256", "MD5")
     * @return Mono containing the calculated checksum
     */
    Mono<String> calculateChecksum(UUID documentId, String algorithm);
    
    /**
     * Verify content integrity using checksum.
     *
     * @param documentId the document ID
     * @param expectedChecksum the expected checksum
     * @param algorithm the checksum algorithm
     * @return Mono containing true if checksum matches, false otherwise
     */
    Mono<Boolean> verifyChecksum(UUID documentId, String expectedChecksum, String algorithm);
}
