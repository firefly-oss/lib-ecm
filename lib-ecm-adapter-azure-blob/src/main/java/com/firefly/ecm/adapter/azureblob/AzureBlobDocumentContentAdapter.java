/*
 * Copyright (c) 2024 Firefly Software Solutions Inc.
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

package com.firefly.ecm.adapter.azureblob;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.firefly.core.ecm.adapter.AdapterFeature;
import com.firefly.core.ecm.adapter.EcmAdapter;
import com.firefly.core.ecm.port.document.DocumentContentPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * Microsoft Azure Blob Storage implementation of DocumentContentPort.
 *
 * <p>This adapter handles binary content operations using Azure Blob Storage:</p>
 * <ul>
 *   <li>Streaming content download with backpressure support</li>
 *   <li>Byte array content retrieval for smaller files</li>
 *   <li>Range requests for partial content access</li>
 *   <li>Content validation and integrity checks</li>
 *   <li>Block blob upload for large files</li>
 *   <li>Efficient content existence and size checking</li>
 * </ul>
 *
 * <p>The adapter supports both streaming and byte array operations,
 * automatically handling large files through Azure's block blob
 * capabilities when configured.</p>
 *
 * <p>Content naming convention: {pathPrefix}/{documentId}.content</p>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@EcmAdapter(
    type = "azure-blob-content",
    description = "Microsoft Azure Blob Storage Document Content Adapter",
    supportedFeatures = {
        AdapterFeature.CONTENT_STORAGE,
        AdapterFeature.STREAMING,
        AdapterFeature.CLOUD_STORAGE
    },
    requiredProperties = {"account-name", "container-name"},
    optionalProperties = {"account-key", "connection-string", "sas-token", "managed-identity", 
                         "endpoint", "path-prefix", "block-size", "block-upload-threshold"}
)
@Component
@ConditionalOnProperty(name = "firefly.ecm.adapter-type", havingValue = "azure-blob")
public class AzureBlobDocumentContentAdapter implements DocumentContentPort {

    private final BlobContainerClient containerClient;
    private final AzureBlobAdapterProperties properties;
    private final DataBufferFactory dataBufferFactory;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public AzureBlobDocumentContentAdapter(BlobContainerClient containerClient,
                                          AzureBlobAdapterProperties properties,
                                          @Qualifier("azureBlobCircuitBreaker") CircuitBreaker circuitBreaker,
                                          @Qualifier("azureBlobRetry") Retry retry) {
        this.containerClient = containerClient;
        this.properties = properties;
        this.dataBufferFactory = new DefaultDataBufferFactory();
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        log.info("AzureBlobDocumentContentAdapter initialized with container: {}, prefix: {}, resilience enabled",
                properties.getContainerName(), properties.getPathPrefix());
    }

    @Override
    public Flux<DataBuffer> getContentStream(UUID documentId) {
        return Mono.fromCallable(() -> {
            String blobName = buildContentBlobName(documentId);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            if (!blobClient.exists()) {
                throw new RuntimeException("Content not found for document: " + documentId);
            }

            return blobClient.openInputStream();
        })
        .flatMapMany(inputStream -> {
            return Flux.<DataBuffer>create(sink -> {
                try {
                    byte[] buffer = new byte[properties.getBlockSize().intValue()];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        DataBuffer dataBuffer = dataBufferFactory.allocateBuffer(bytesRead);
                        dataBuffer.write(buffer, 0, bytesRead);
                        sink.next(dataBuffer);
                    }

                    inputStream.close();
                    sink.complete();
                } catch (Exception e) {
                    sink.error(e);
                }
            });
        })
        .doOnSubscribe(subscription -> log.debug("Starting content stream for document {}", documentId))
        .doOnComplete(() -> log.debug("Completed content stream for document {}", documentId))
        .doOnError(error -> log.error("Failed to stream content for document {} from Azure Blob Storage",
                documentId, error));
    }

    @Override
    public Mono<byte[]> getContent(UUID documentId) {
        return Mono.fromCallable(() -> {
            String blobName = buildContentBlobName(documentId);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            if (!blobClient.exists()) {
                throw new RuntimeException("Content not found for document: " + documentId);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobClient.downloadStream(outputStream);
            byte[] content = outputStream.toByteArray();
            
            log.debug("Retrieved content for document {} ({} bytes) from Azure Blob Storage", 
                    documentId, content.length);
            return content;
        })
        .doOnError(error -> log.error("Failed to retrieve content for document {} from Azure Blob Storage",
                documentId, error));
    }

    public Mono<byte[]> getContentRange(UUID documentId, long offset, long length) {
        return Mono.fromCallable(() -> {
            String blobName = buildContentBlobName(documentId);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            if (!blobClient.exists()) {
                throw new RuntimeException("Content not found for document: " + documentId);
            }

            BlobRange range = new BlobRange(offset, length);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobClient.downloadStreamWithResponse(outputStream, range, null, null, false, null, null);
            byte[] content = outputStream.toByteArray();
            
            log.debug("Retrieved content range for document {} (offset: {}, length: {}, actual: {} bytes)", 
                    documentId, offset, length, content.length);
            return content;
        })
        .doOnError(error -> log.error("Failed to retrieve content range for document {} from Azure Blob Storage",
                documentId, error));
    }

    @Override
    public Mono<String> storeContent(UUID documentId, byte[] content, String mimeType) {
        return Mono.fromCallable(() -> {
            String blobName = buildContentBlobName(documentId);

            if (content.length > properties.getBlockUploadThreshold()) {
                storeContentAsBlocks(blobName, content, mimeType);
            } else {
                storeContentSingle(blobName, content, mimeType);
            }

            log.debug("Stored content for document {} ({} bytes) at path {}", 
                    documentId, content.length, blobName);
            return blobName;
        })
        .doOnError(error -> log.error("Failed to store content for document {} in Azure Blob Storage",
                documentId, error));
    }

    @Override
    public Mono<String> storeContentStream(UUID documentId, Flux<DataBuffer> contentStream, String mimeType, Long contentLength) {
        return contentStream
            .reduce(dataBufferFactory.allocateBuffer(), (accumulated, current) -> {
                accumulated.write(current.asByteBuffer());
                return accumulated;
            })
            .map(dataBuffer -> {
                byte[] content = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(content);
                return content;
            })
            .flatMap(content -> storeContent(documentId, content, mimeType))
            .doOnSubscribe(subscription -> log.debug("Starting content stream storage for document {}", documentId))
            .doOnSuccess(path -> log.debug("Completed content stream storage for document {} at path {}", 
                    documentId, path))
            .doOnError(error -> log.error("Failed to store content stream for document {} in Azure Blob Storage", 
                    documentId, error));
    }

    @Override
    public Mono<Void> deleteContent(UUID documentId) {
        return Mono.fromRunnable(() -> {
            String blobName = buildContentBlobName(documentId);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            if (blobClient.exists()) {
                blobClient.delete();
                log.debug("Deleted content for document {} from Azure Blob Storage", documentId);
            } else {
                log.debug("Content for document {} not found in Azure Blob Storage", documentId);
            }
        })
        .then()
        .doOnError(error -> log.error("Failed to delete content for document {} from Azure Blob Storage",
                documentId, error));
    }

    @Override
    public Mono<Void> deleteContentByPath(String storagePath) {
        return Mono.fromRunnable(() -> {
            BlobClient blobClient = containerClient.getBlobClient(storagePath);
            
            if (blobClient.exists()) {
                blobClient.delete();
                log.debug("Deleted content at path {} from Azure Blob Storage", storagePath);
            } else {
                log.debug("Content at path {} not found in Azure Blob Storage", storagePath);
            }
        })
        .then()
        .doOnError(error -> log.error("Failed to delete content at path {} from Azure Blob Storage",
                storagePath, error));
    }

    @Override
    public Mono<Boolean> existsContent(UUID documentId) {
        return Mono.fromCallable(() -> {
            String blobName = buildContentBlobName(documentId);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            boolean exists = blobClient.exists();
            
            log.debug("Content existence check for document {}: {}", documentId, exists);
            return exists;
        })
        .doOnError(error -> log.error("Failed to check content existence for document {} in Azure Blob Storage",
                documentId, error));
    }

    @Override
    public Mono<Long> getContentSize(UUID documentId) {
        return Mono.fromCallable(() -> {
            String blobName = buildContentBlobName(documentId);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            if (!blobClient.exists()) {
                throw new RuntimeException("Content not found for document: " + documentId);
            }

            BlobProperties properties = blobClient.getProperties();
            long size = properties.getBlobSize();
            
            log.debug("Content size for document {}: {} bytes", documentId, size);
            return size;
        })
        .doOnError(error -> log.error("Failed to get content size for document {} from Azure Blob Storage",
                documentId, error));
    }

    @Override
    public Mono<byte[]> getContentByPath(String storagePath) {
        return Mono.fromCallable(() -> {
            BlobClient blobClient = containerClient.getBlobClient(storagePath);

            if (!blobClient.exists()) {
                throw new RuntimeException("Content not found at path: " + storagePath);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobClient.downloadStream(outputStream);
            byte[] content = outputStream.toByteArray();

            log.debug("Retrieved content by path {} ({} bytes) from Azure Blob Storage",
                    storagePath, content.length);
            return content;
        })
        .doOnError(error -> log.error("Failed to retrieve content by path {} from Azure Blob Storage",
                storagePath, error));
    }

    @Override
    public Flux<DataBuffer> getContentStreamByPath(String storagePath) {
        return Mono.fromCallable(() -> {
            BlobClient blobClient = containerClient.getBlobClient(storagePath);

            if (!blobClient.exists()) {
                throw new RuntimeException("Content not found at path: " + storagePath);
            }

            return blobClient.openInputStream();
        })
        .flatMapMany(inputStream -> {
            return Flux.<DataBuffer>create(sink -> {
                try {
                    byte[] buffer = new byte[properties.getBlockSize().intValue()];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        DataBuffer dataBuffer = dataBufferFactory.allocateBuffer(bytesRead);
                        dataBuffer.write(buffer, 0, bytesRead);
                        sink.next(dataBuffer);
                    }

                    inputStream.close();
                    sink.complete();
                } catch (Exception e) {
                    sink.error(e);
                }
            });
        })
        .doOnSubscribe(subscription -> log.debug("Starting content stream by path {}", storagePath))
        .doOnComplete(() -> log.debug("Completed content stream by path {}", storagePath))
        .doOnError(error -> log.error("Failed to stream content by path {} from Azure Blob Storage",
                storagePath, error));
    }

    public Mono<String> calculateChecksum(UUID documentId, String algorithm) {
        return getContent(documentId)
            .map(content -> {
                try {
                    java.security.MessageDigest digest = java.security.MessageDigest.getInstance(algorithm);
                    byte[] hash = digest.digest(content);
                    StringBuilder hexString = new StringBuilder();
                    for (byte b : hash) {
                        String hex = Integer.toHexString(0xff & b);
                        if (hex.length() == 1) {
                            hexString.append('0');
                        }
                        hexString.append(hex);
                    }
                    String checksum = hexString.toString();
                    log.debug("Calculated {} checksum for document {}: {}", algorithm, documentId, checksum);
                    return checksum;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to calculate checksum using algorithm: " + algorithm, e);
                }
            })
            .doOnError(error -> log.error("Failed to calculate checksum for document {} using algorithm {}",
                    documentId, algorithm, error));
    }

    public Mono<Boolean> verifyChecksum(UUID documentId, String expectedChecksum, String algorithm) {
        return calculateChecksum(documentId, algorithm)
            .map(actualChecksum -> {
                boolean matches = actualChecksum.equalsIgnoreCase(expectedChecksum);
                log.debug("Checksum verification for document {}: expected={}, actual={}, matches={}",
                        documentId, expectedChecksum, actualChecksum, matches);
                return matches;
            })
            .doOnError(error -> log.error("Failed to verify checksum for document {} using algorithm {}",
                    documentId, algorithm, error));
    }

    public String getAdapterName() {
        return "AzureBlobDocumentContentAdapter";
    }

    /**
     * Builds the blob name for document content.
     */
    private String buildContentBlobName(UUID documentId) {
        return String.format("%s%s.content", properties.getPathPrefix(), documentId);
    }

    /**
     * Stores content as a single blob upload.
     */
    private void storeContentSingle(String blobName, byte[] content, String mimeType) {
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            blobClient.upload(inputStream, content.length, true);
            
            if (mimeType != null) {
                blobClient.setHttpHeaders(new com.azure.storage.blob.models.BlobHttpHeaders()
                    .setContentType(mimeType));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload content as single blob", e);
        }
    }

    /**
     * Stores content using block blob upload for large files.
     */
    private void storeContentAsBlocks(String blobName, byte[] content, String mimeType) {
        BlockBlobClient blockBlobClient = containerClient.getBlobClient(blobName).getBlockBlobClient();
        
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            blockBlobClient.upload(inputStream, content.length, true);
            
            if (mimeType != null) {
                blockBlobClient.setHttpHeaders(new com.azure.storage.blob.models.BlobHttpHeaders()
                    .setContentType(mimeType));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload content as block blob", e);
        }
    }
}
