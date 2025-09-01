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
 * Port interface for document binary content storage and retrieval operations.
 *
 * <p>This interface defines the contract for managing the actual binary content
 * of documents in the Firefly ECM system. It provides both traditional byte array
 * operations and modern streaming capabilities for efficient handling of large files.</p>
 *
 * <p>Key capabilities:</p>
 * <ul>
 *   <li>Binary content storage with multiple input formats</li>
 *   <li>Streaming support for large file handling</li>
 *   <li>Content retrieval by document ID or storage path</li>
 *   <li>Content integrity verification through checksums</li>
 *   <li>Efficient content existence and size checking</li>
 * </ul>
 *
 * <p>The interface supports both blocking (byte array) and non-blocking (streaming)
 * operations to accommodate different use cases:</p>
 * <ul>
 *   <li>Small files: Use byte array methods for simplicity</li>
 *   <li>Large files: Use streaming methods for memory efficiency</li>
 *   <li>Real-time processing: Use streaming for immediate processing</li>
 * </ul>
 *
 * <p>All operations are reactive and return {@link Mono} or {@link Flux} types
 * to support non-blocking, asynchronous processing patterns.</p>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 * @see DocumentPort
 * @see org.springframework.core.io.buffer.DataBuffer
 * @see reactor.core.publisher.Mono
 * @see reactor.core.publisher.Flux
 */
public interface DocumentContentPort {

    /**
     * Stores document content from a byte array with MIME type information.
     *
     * <p>This method is suitable for smaller files that can be loaded entirely
     * into memory. The content is stored in the backend storage system and a
     * storage path is returned for future reference.</p>
     *
     * <p>The implementation should:</p>
     * <ul>
     *   <li>Generate a unique storage path for the content</li>
     *   <li>Store the content securely in the backend system</li>
     *   <li>Preserve the MIME type information for content serving</li>
     *   <li>Handle storage errors gracefully</li>
     * </ul>
     *
     * @param documentId the unique UUID identifier of the document
     * @param content the binary content as a byte array
     * @param mimeType the MIME type of the content (e.g., "application/pdf")
     * @return a Mono containing the storage path where content was stored
     * @throws IllegalArgumentException if any parameter is null
     * @throws ContentStorageException if the content cannot be stored
     */
    Mono<String> storeContent(UUID documentId, byte[] content, String mimeType);

    /**
     * Stores document content from a reactive stream with MIME type and size information.
     *
     * <p>This method is designed for large files or streaming scenarios where
     * loading the entire content into memory is not feasible. The content is
     * processed as a stream of DataBuffer chunks.</p>
     *
     * <p>Benefits of streaming storage:</p>
     * <ul>
     *   <li>Memory efficient for large files</li>
     *   <li>Supports real-time upload processing</li>
     *   <li>Enables progress tracking and cancellation</li>
     *   <li>Better performance for network transfers</li>
     * </ul>
     *
     * @param documentId the unique UUID identifier of the document
     * @param contentStream the reactive stream of DataBuffer chunks containing the content
     * @param mimeType the MIME type of the content
     * @param contentLength the expected total content length in bytes (may be null if unknown)
     * @return a Mono containing the storage path where content was stored
     * @throws IllegalArgumentException if documentId, contentStream, or mimeType is null
     * @throws ContentStorageException if the content cannot be stored
     */
    Mono<String> storeContentStream(UUID documentId, Flux<DataBuffer> contentStream, String mimeType, Long contentLength);

    /**
     * Retrieves document content as a complete byte array.
     *
     * <p>This method loads the entire document content into memory and returns
     * it as a byte array. It's suitable for smaller files or when the entire
     * content needs to be processed at once.</p>
     *
     * <p>Use this method when:</p>
     * <ul>
     *   <li>File size is manageable (typically under 100MB)</li>
     *   <li>Content needs to be processed as a complete unit</li>
     *   <li>Legacy APIs require byte array input</li>
     * </ul>
     *
     * @param documentId the unique UUID identifier of the document
     * @return a Mono containing the complete document content, empty if not found
     * @throws IllegalArgumentException if documentId is null
     * @see #getContentStream(UUID) for memory-efficient alternative
     */
    Mono<byte[]> getContent(UUID documentId);

    /**
     * Retrieves document content as a reactive stream of DataBuffer chunks.
     *
     * <p>This method provides memory-efficient access to document content by
     * streaming it in chunks. It's the preferred method for large files or
     * when content needs to be processed incrementally.</p>
     *
     * <p>Advantages of streaming retrieval:</p>
     * <ul>
     *   <li>Constant memory usage regardless of file size</li>
     *   <li>Enables real-time processing and transformation</li>
     *   <li>Supports efficient network streaming</li>
     *   <li>Better user experience for large downloads</li>
     * </ul>
     *
     * @param documentId the unique UUID identifier of the document
     * @return a Flux of DataBuffer chunks containing the document content
     * @throws IllegalArgumentException if documentId is null
     */
    Flux<DataBuffer> getContentStream(UUID documentId);
    
    /**
     * Retrieves document content by its storage path as a complete byte array.
     *
     * <p>This method provides direct access to content using the internal storage
     * path rather than the document ID. It's useful for low-level operations,
     * content migration, or when working with storage-specific functionality.</p>
     *
     * <p>Note: Storage paths are adapter-specific and should generally not be
     * exposed to end users. This method is primarily for internal system use.</p>
     *
     * @param storagePath the internal storage path where the content is located
     * @return a Mono containing the document content, empty if not found
     * @throws IllegalArgumentException if storagePath is null or empty
     */
    Mono<byte[]> getContentByPath(String storagePath);

    /**
     * Retrieves document content by its storage path as a reactive stream.
     *
     * <p>Similar to {@link #getContentByPath(String)} but returns content as
     * a stream for memory-efficient processing of large files. This method
     * provides direct storage access using the internal path.</p>
     *
     * @param storagePath the internal storage path where the content is located
     * @return a Flux of DataBuffer chunks containing the document content
     * @throws IllegalArgumentException if storagePath is null or empty
     */
    Flux<DataBuffer> getContentStreamByPath(String storagePath);

    /**
     * Permanently deletes document content from the storage system.
     *
     * <p>This operation removes the binary content associated with the document
     * from the backend storage. The operation is irreversible and should be
     * coordinated with document metadata deletion to maintain consistency.</p>
     *
     * <p>The implementation should handle cases where:</p>
     * <ul>
     *   <li>Content doesn't exist (should complete successfully)</li>
     *   <li>Storage system is temporarily unavailable</li>
     *   <li>Content is locked or in use by other operations</li>
     * </ul>
     *
     * @param documentId the unique UUID identifier of the document
     * @return a Mono that completes when the deletion is finished
     * @throws IllegalArgumentException if documentId is null
     * @throws ContentDeletionException if the content cannot be deleted
     */
    Mono<Void> deleteContent(UUID documentId);

    /**
     * Permanently deletes content by its storage path.
     *
     * <p>This method provides direct content deletion using the internal storage
     * path. It's useful for cleanup operations, content migration, or when
     * working with orphaned content that lacks document metadata.</p>
     *
     * @param storagePath the internal storage path of the content to delete
     * @return a Mono that completes when the deletion is finished
     * @throws IllegalArgumentException if storagePath is null or empty
     * @throws ContentDeletionException if the content cannot be deleted
     */
    Mono<Void> deleteContentByPath(String storagePath);

    /**
     * Checks whether content exists for the specified document.
     *
     * <p>This is a lightweight operation that verifies content existence without
     * retrieving the actual content. It's useful for validation, conditional
     * logic, and system health checks.</p>
     *
     * @param documentId the unique UUID identifier of the document
     * @return a Mono containing true if content exists, false otherwise
     * @throws IllegalArgumentException if documentId is null
     */
    Mono<Boolean> existsContent(UUID documentId);

    /**
     * Retrieves the size of document content in bytes.
     *
     * <p>This method provides efficient access to content size information
     * without retrieving the actual content. The size information is useful
     * for storage calculations, transfer planning, and user interface display.</p>
     *
     * @param documentId the unique UUID identifier of the document
     * @return a Mono containing the content size in bytes, empty if content not found
     * @throws IllegalArgumentException if documentId is null
     */
    Mono<Long> getContentSize(UUID documentId);

    /**
     * Calculates a cryptographic checksum of the document content.
     *
     * <p>This method computes a hash value of the document content using the
     * specified algorithm. The checksum can be used for integrity verification,
     * duplicate detection, and change tracking.</p>
     *
     * <p>Supported algorithms typically include:</p>
     * <ul>
     *   <li>SHA-256 (recommended for security)</li>
     *   <li>SHA-1 (legacy support)</li>
     *   <li>MD5 (fast but less secure)</li>
     * </ul>
     *
     * @param documentId the unique UUID identifier of the document
     * @param algorithm the checksum algorithm to use (e.g., "SHA-256", "MD5")
     * @return a Mono containing the calculated checksum as a hexadecimal string
     * @throws IllegalArgumentException if documentId or algorithm is null
     * @throws UnsupportedAlgorithmException if the algorithm is not supported
     */
    Mono<String> calculateChecksum(UUID documentId, String algorithm);

    /**
     * Verifies document content integrity by comparing checksums.
     *
     * <p>This method calculates the checksum of the current content and compares
     * it with the expected value. It's essential for detecting content corruption,
     * verifying successful transfers, and maintaining data integrity.</p>
     *
     * <p>The verification process:</p>
     * <ol>
     *   <li>Calculates the current content checksum using the specified algorithm</li>
     *   <li>Compares it with the expected checksum (case-insensitive)</li>
     *   <li>Returns true if they match, false otherwise</li>
     * </ol>
     *
     * @param documentId the unique UUID identifier of the document
     * @param expectedChecksum the expected checksum value to compare against
     * @param algorithm the checksum algorithm to use for calculation
     * @return a Mono containing true if checksums match, false otherwise
     * @throws IllegalArgumentException if any parameter is null
     * @throws UnsupportedAlgorithmException if the algorithm is not supported
     */
    Mono<Boolean> verifyChecksum(UUID documentId, String expectedChecksum, String algorithm);
}
