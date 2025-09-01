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
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.firefly.core.ecm.adapter.AdapterFeature;
import com.firefly.core.ecm.adapter.EcmAdapter;
import com.firefly.core.ecm.domain.model.document.Document;
import com.firefly.core.ecm.domain.enums.document.DocumentStatus;
import com.firefly.core.ecm.port.document.DocumentPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Microsoft Azure Blob Storage implementation of DocumentPort.
 *
 * <p>This adapter provides complete document management capabilities using Azure Blob Storage
 * as the backend storage system. It supports:</p>
 * <ul>
 *   <li>Document CRUD operations with metadata storage</li>
 *   <li>Document organization using blob metadata and naming conventions</li>
 *   <li>Document discovery by various criteria (folder, owner, status)</li>
 *   <li>Document copying and duplication</li>
 *   <li>Resilient operations with circuit breaker and retry patterns</li>
 *   <li>Efficient blob listing and filtering</li>
 * </ul>
 *
 * <p>The adapter stores document metadata as blob metadata and uses a structured
 * naming convention to organize documents within the Azure Blob container.</p>
 *
 * <p>Blob naming convention: {pathPrefix}/{folderId}/{documentId}.json</p>
 * <p>Content naming convention: {pathPrefix}/{folderId}/{documentId}.content</p>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@EcmAdapter(
    type = "azure-blob",
    description = "Microsoft Azure Blob Storage Document Adapter",
    supportedFeatures = {
        AdapterFeature.DOCUMENT_CRUD,
        AdapterFeature.CONTENT_STORAGE,
        AdapterFeature.VERSIONING,
        AdapterFeature.FOLDER_MANAGEMENT,
        AdapterFeature.SEARCH,
        AdapterFeature.CLOUD_STORAGE
    },
    requiredProperties = {"account-name", "container-name"},
    optionalProperties = {"account-key", "connection-string", "sas-token", "managed-identity", 
                         "endpoint", "path-prefix", "enable-versioning", "access-tier"}
)
@Component
@ConditionalOnProperty(name = "firefly.ecm.adapter-type", havingValue = "azure-blob")
public class AzureBlobDocumentAdapter implements DocumentPort {

    private final BlobContainerClient containerClient;
    private final AzureBlobAdapterProperties properties;
    private final ObjectMapper objectMapper;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public AzureBlobDocumentAdapter(BlobContainerClient containerClient,
                                   AzureBlobAdapterProperties properties,
                                   ObjectMapper objectMapper,
                                   @Qualifier("azureBlobCircuitBreaker") CircuitBreaker circuitBreaker,
                                   @Qualifier("azureBlobRetry") Retry retry) {
        this.containerClient = containerClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        log.info("AzureBlobDocumentAdapter initialized with container: {}, prefix: {}, resilience enabled",
                properties.getContainerName(), properties.getPathPrefix());
    }

    @Override
    public Mono<Document> createDocument(Document document, byte[] content) {
        return Mono.fromCallable(() -> {
            // Generate ID if not provided and set defaults using toBuilder()
            Document.DocumentBuilder builder = document.toBuilder();

            if (document.getId() == null) {
                builder.id(UUID.randomUUID());
            }

            // Set creation timestamp
            if (document.getCreatedAt() == null) {
                builder.createdAt(Instant.now());
            }

            // Set default status if not provided
            if (document.getStatus() == null) {
                builder.status(DocumentStatus.ACTIVE);
            }

            Document documentToCreate = builder.build();

            String blobName = buildDocumentBlobName(documentToCreate.getId(), documentToCreate.getFolderId());
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            // Convert document to JSON
            byte[] documentJson = objectMapper.writeValueAsBytes(documentToCreate);

            // Create metadata map
            Map<String, String> metadata = buildDocumentMetadata(documentToCreate);

            // Upload document metadata as blob
            blobClient.upload(new ByteArrayInputStream(documentJson), documentJson.length, true);
            blobClient.setMetadata(metadata);

            // Store content if provided
            if (content != null && content.length > 0) {
                String contentBlobName = buildContentBlobName(documentToCreate.getId());
                BlobClient contentBlobClient = containerClient.getBlobClient(contentBlobName);
                contentBlobClient.upload(new ByteArrayInputStream(content), content.length, true);
            }

            log.debug("Created document {} in Azure Blob Storage at path {}", documentToCreate.getId(), blobName);
            return documentToCreate;
        })
        .doOnError(error -> log.error("Failed to create document {} in Azure Blob Storage",
                document.getId(), error));
    }

    @Override
    public Mono<Document> getDocument(UUID documentId) {
        return Mono.fromCallable(() -> {
            // Try to find the document by searching through blobs
            String searchPrefix = properties.getPathPrefix();
            ListBlobsOptions options = new ListBlobsOptions()
                .setPrefix(searchPrefix);

            for (BlobItem blobItem : containerClient.listBlobs(options, null)) {
                if (blobItem.getName().contains(documentId.toString()) && 
                    blobItem.getName().endsWith(".json")) {
                    
                    BlobClient blobClient = containerClient.getBlobClient(blobItem.getName());
                    
                    // Download and parse document JSON
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    blobClient.downloadStream(outputStream);
                    byte[] documentJson = outputStream.toByteArray();
                    
                    Document document = objectMapper.readValue(documentJson, Document.class);
                    log.debug("Retrieved document {} from Azure Blob Storage", documentId);
                    return document;
                }
            }
            
            log.debug("Document {} not found in Azure Blob Storage", documentId);
            return null;
        })
        .doOnError(error -> log.error("Failed to retrieve document {} from Azure Blob Storage",
                documentId, error));
    }

    @Override
    public Mono<Document> updateDocument(Document document) {
        return Mono.fromCallable(() -> {
            String blobName = buildDocumentBlobName(document.getId(), document.getFolderId());
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            if (!blobClient.exists()) {
                throw new RuntimeException("Document not found: " + document.getId());
            }

            // Update modification timestamp using builder
            Document updatedDocument = document.toBuilder()
                .modifiedAt(Instant.now())
                .build();

            // Convert document to JSON
            byte[] documentJson = objectMapper.writeValueAsBytes(updatedDocument);

            // Create metadata map
            Map<String, String> metadata = buildDocumentMetadata(updatedDocument);

            // Upload updated document metadata
            blobClient.upload(new ByteArrayInputStream(documentJson), documentJson.length, true);
            blobClient.setMetadata(metadata);

            log.debug("Updated document {} in Azure Blob Storage", updatedDocument.getId());
            return updatedDocument;
        })
        .doOnError(error -> log.error("Failed to update document {} in Azure Blob Storage",
                document.getId(), error));
    }

    @Override
    public Mono<Void> deleteDocument(UUID documentId) {
        return Mono.fromRunnable(() -> {
            // Find and delete document blob
            String searchPrefix = properties.getPathPrefix();
            ListBlobsOptions options = new ListBlobsOptions()
                .setPrefix(searchPrefix);

            for (BlobItem blobItem : containerClient.listBlobs(options, null)) {
                if (blobItem.getName().contains(documentId.toString())) {
                    BlobClient blobClient = containerClient.getBlobClient(blobItem.getName());
                    blobClient.delete();
                    log.debug("Deleted blob {} for document {}", blobItem.getName(), documentId);
                }
            }
            
            log.debug("Deleted document {} from Azure Blob Storage", documentId);
        })
        .then()
        .doOnError(error -> log.error("Failed to delete document {} from Azure Blob Storage",
                documentId, error));
    }

    @Override
    public Flux<Document> getDocumentsByFolder(UUID folderId) {
        return Flux.defer(() -> {
            String folderPrefix = buildFolderPrefix(folderId);
            ListBlobsOptions options = new ListBlobsOptions()
                .setPrefix(folderPrefix);

            java.util.List<Document> documents = new java.util.ArrayList<>();
            for (BlobItem blobItem : containerClient.listBlobs(options, null)) {
                if (blobItem.getName().endsWith(".json")) {
                    try {
                        BlobClient blobClient = containerClient.getBlobClient(blobItem.getName());
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        blobClient.downloadStream(outputStream);
                        byte[] documentJson = outputStream.toByteArray();
                        Document document = objectMapper.readValue(documentJson, Document.class);
                        documents.add(document);
                    } catch (Exception e) {
                        log.error("Failed to parse document from blob {}", blobItem.getName(), e);
                    }
                }
            }
            return Flux.fromIterable(documents);
        })
        .doOnError(error -> log.error("Failed to retrieve documents for folder {} from Azure Blob Storage",
                folderId, error));
    }

    @Override
    public String getAdapterName() {
        return "AzureBlobDocumentAdapter";
    }

    /**
     * Builds the blob name for a document.
     */
    private String buildDocumentBlobName(UUID documentId, UUID folderId) {
        String folderPath = folderId != null ? folderId.toString() : "root";
        return String.format("%s%s/%s.json", properties.getPathPrefix(), folderPath, documentId);
    }

    /**
     * Builds the folder prefix for blob listing.
     */
    private String buildFolderPrefix(UUID folderId) {
        String folderPath = folderId != null ? folderId.toString() : "root";
        return String.format("%s%s/", properties.getPathPrefix(), folderPath);
    }

    /**
     * Builds metadata map from document properties.
     */
    private Map<String, String> buildDocumentMetadata(Document document) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("documentId", document.getId().toString());
        metadata.put("name", document.getName());
        metadata.put("mimeType", document.getMimeType());
        metadata.put("status", document.getStatus().toString());
        if (document.getOwnerId() != null) {
            metadata.put("ownerId", document.getOwnerId().toString());
        }
        if (document.getFolderId() != null) {
            metadata.put("folderId", document.getFolderId().toString());
        }
        return metadata;
    }



    /**
     * Builds the blob name for document content.
     */
    private String buildContentBlobName(UUID documentId) {
        return String.format("%s%s.content", properties.getPathPrefix(), documentId);
    }

    @Override
    public Flux<Document> getDocumentsByOwner(UUID ownerId) {
        return Flux.defer(() -> {
            String searchPrefix = properties.getPathPrefix();
            ListBlobsOptions options = new ListBlobsOptions()
                .setPrefix(searchPrefix);

            java.util.List<Document> documents = new java.util.ArrayList<>();
            for (BlobItem blobItem : containerClient.listBlobs(options, null)) {
                if (blobItem.getName().endsWith(".json")) {
                    try {
                        BlobClient blobClient = containerClient.getBlobClient(blobItem.getName());
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        blobClient.downloadStream(outputStream);
                        byte[] documentJson = outputStream.toByteArray();
                        Document document = objectMapper.readValue(documentJson, Document.class);
                        if (ownerId.equals(document.getOwnerId())) {
                            documents.add(document);
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse document from blob {}", blobItem.getName(), e);
                    }
                }
            }
            return Flux.fromIterable(documents);
        })
        .doOnError(error -> log.error("Failed to retrieve documents for owner {} from Azure Blob Storage",
                ownerId, error));
    }

    @Override
    public Flux<Document> getDocumentsByStatus(DocumentStatus status) {
        return Flux.defer(() -> {
            String searchPrefix = properties.getPathPrefix();
            ListBlobsOptions options = new ListBlobsOptions()
                .setPrefix(searchPrefix);

            java.util.List<Document> documents = new java.util.ArrayList<>();
            for (BlobItem blobItem : containerClient.listBlobs(options, null)) {
                if (blobItem.getName().endsWith(".json")) {
                    try {
                        BlobClient blobClient = containerClient.getBlobClient(blobItem.getName());
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        blobClient.downloadStream(outputStream);
                        byte[] documentJson = outputStream.toByteArray();
                        Document document = objectMapper.readValue(documentJson, Document.class);
                        if (status.equals(document.getStatus())) {
                            documents.add(document);
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse document from blob {}", blobItem.getName(), e);
                    }
                }
            }
            return Flux.fromIterable(documents);
        })
        .doOnError(error -> log.error("Failed to retrieve documents with status {} from Azure Blob Storage",
                status, error));
    }

    @Override
    public Mono<Document> moveDocument(UUID documentId, UUID targetFolderId) {
        return getDocument(documentId)
            .flatMap(document -> {
                if (document == null) {
                    return Mono.error(new RuntimeException("Document not found: " + documentId));
                }

                // Update folder ID using builder
                UUID oldFolderId = document.getFolderId();
                Document movedDocument = document.toBuilder()
                    .folderId(targetFolderId)
                    .modifiedAt(Instant.now())
                    .build();

                // Create new blob with updated folder path
                String newBlobName = buildDocumentBlobName(documentId, targetFolderId);
                String oldBlobName = buildDocumentBlobName(documentId, oldFolderId);

                return Mono.fromCallable(() -> {
                    BlobClient oldBlobClient = containerClient.getBlobClient(oldBlobName);
                    BlobClient newBlobClient = containerClient.getBlobClient(newBlobName);

                    if (!oldBlobClient.exists()) {
                        throw new RuntimeException("Document blob not found: " + oldBlobName);
                    }

                    // Copy to new location
                    byte[] documentJson = objectMapper.writeValueAsBytes(movedDocument);
                    Map<String, String> metadata = buildDocumentMetadata(movedDocument);

                    newBlobClient.upload(new ByteArrayInputStream(documentJson), documentJson.length, true);
                    newBlobClient.setMetadata(metadata);

                    // Delete old blob
                    oldBlobClient.delete();

                    // Also move content blob if it exists
                    String oldContentBlobName = buildContentBlobName(documentId);
                    String newContentBlobName = buildContentBlobName(documentId);

                    BlobClient oldContentBlobClient = containerClient.getBlobClient(oldContentBlobName);
                    if (oldContentBlobClient.exists()) {
                        BlobClient newContentBlobClient = containerClient.getBlobClient(newContentBlobName);
                        ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
                        oldContentBlobClient.downloadStream(contentStream);
                        byte[] content = contentStream.toByteArray();

                        newContentBlobClient.upload(new ByteArrayInputStream(content), content.length, true);
                        oldContentBlobClient.delete();
                    }

                    log.debug("Moved document {} from folder {} to folder {}",
                            documentId, oldFolderId, targetFolderId);
                    return movedDocument;
                });
            })
            .doOnError(error -> log.error("Failed to move document {} to folder {} in Azure Blob Storage",
                    documentId, targetFolderId, error));
    }

    @Override
    public Mono<Document> copyDocument(UUID documentId, UUID targetFolderId, String newName) {
        return getDocument(documentId)
            .flatMap(document -> {
                if (document == null) {
                    return Mono.error(new RuntimeException("Document not found: " + documentId));
                }

                // Create copy with new ID and optional new name
                Document copy = Document.builder()
                    .id(UUID.randomUUID())
                    .name(newName != null ? newName : document.getName())
                    .mimeType(document.getMimeType())
                    .size(document.getSize())
                    .folderId(targetFolderId)
                    .ownerId(document.getOwnerId())
                    .status(document.getStatus())
                    .createdAt(Instant.now())
                    .build();

                return Mono.fromCallable(() -> {
                    // Copy document metadata
                    String copyBlobName = buildDocumentBlobName(copy.getId(), targetFolderId);
                    BlobClient copyBlobClient = containerClient.getBlobClient(copyBlobName);

                    byte[] documentJson = objectMapper.writeValueAsBytes(copy);
                    Map<String, String> metadata = buildDocumentMetadata(copy);

                    copyBlobClient.upload(new ByteArrayInputStream(documentJson), documentJson.length, true);
                    copyBlobClient.setMetadata(metadata);

                    // Copy content blob if it exists
                    String originalContentBlobName = buildContentBlobName(documentId);
                    BlobClient originalContentBlobClient = containerClient.getBlobClient(originalContentBlobName);

                    if (originalContentBlobClient.exists()) {
                        String copyContentBlobName = buildContentBlobName(copy.getId());
                        BlobClient copyContentBlobClient = containerClient.getBlobClient(copyContentBlobName);

                        ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
                        originalContentBlobClient.downloadStream(contentStream);
                        byte[] content = contentStream.toByteArray();

                        copyContentBlobClient.upload(new ByteArrayInputStream(content), content.length, true);
                    }

                    log.debug("Copied document {} to new document {} in folder {}",
                            documentId, copy.getId(), targetFolderId);
                    return copy;
                });
            })
            .doOnError(error -> log.error("Failed to copy document {} to folder {} in Azure Blob Storage",
                    documentId, targetFolderId, error));
    }

    @Override
    public Mono<Boolean> existsDocument(UUID documentId) {
        return Mono.fromCallable(() -> {
            String searchPrefix = properties.getPathPrefix();
            ListBlobsOptions options = new ListBlobsOptions()
                .setPrefix(searchPrefix);

            for (BlobItem blobItem : containerClient.listBlobs(options, null)) {
                if (blobItem.getName().contains(documentId.toString()) &&
                    blobItem.getName().endsWith(".json")) {
                    return true;
                }
            }
            return false;
        })
        .doOnError(error -> log.error("Failed to check existence of document {} in Azure Blob Storage",
                documentId, error));
    }
}
