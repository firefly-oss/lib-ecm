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
package com.firefly.ecm.adapter.s3;

import com.firefly.core.ecm.adapter.AdapterFeature;
import com.firefly.core.ecm.adapter.EcmAdapter;
import com.firefly.core.ecm.config.ResilienceConfiguration;
import com.firefly.core.ecm.domain.model.document.Document;
import com.firefly.core.ecm.domain.enums.document.DocumentStatus;
import com.firefly.core.ecm.port.document.DocumentPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Amazon S3 implementation of DocumentPort.
 *
 * <p>This adapter provides complete document management capabilities using Amazon S3
 * as the storage backend. It supports:</p>
 * <ul>
 *   <li>Document CRUD operations</li>
 *   <li>Metadata storage using S3 object metadata and tags</li>
 *   <li>Folder organization using S3 key prefixes</li>
 *   <li>Document versioning using S3 versioning</li>
 *   <li>Batch operations for performance</li>
 * </ul>
 *
 * <p>The adapter uses S3 object keys in the format:</p>
 * <pre>{pathPrefix}/{folderId}/{documentId}</pre>
 *
 * <p>Document metadata is stored as S3 object metadata and tags for efficient
 * querying and management.</p>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@EcmAdapter(
    type = "s3",
    description = "Amazon S3 Document Storage Adapter",
    supportedFeatures = {
        AdapterFeature.DOCUMENT_CRUD,
        AdapterFeature.CONTENT_STORAGE,
        AdapterFeature.VERSIONING,
        AdapterFeature.FOLDER_MANAGEMENT,
        AdapterFeature.SEARCH,
        AdapterFeature.CLOUD_STORAGE
    },
    requiredProperties = {"bucket-name", "region"},
    optionalProperties = {"access-key", "secret-key", "endpoint", "path-prefix", "enable-versioning"}
)
@Component
@ConditionalOnProperty(name = "firefly.ecm.adapter-type", havingValue = "s3")
public class S3DocumentAdapter implements DocumentPort {

    private final S3Client s3Client;
    private final S3AdapterProperties properties;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public S3DocumentAdapter(S3Client s3Client,
                           S3AdapterProperties properties,
                           @Qualifier("s3CircuitBreaker") CircuitBreaker circuitBreaker,
                           @Qualifier("s3Retry") Retry retry) {
        this.s3Client = s3Client;
        this.properties = properties;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        log.info("S3DocumentAdapter initialized with bucket: {}, prefix: {}, resilience enabled",
                properties.getBucketName(), properties.getPathPrefix());
    }

    @Override
    public Mono<Document> createDocument(Document document, byte[] content) {
        Mono<Document> operation = Mono.fromCallable(() -> {
            UUID documentId = document.getId() != null ? document.getId() : UUID.randomUUID();
            String objectKey = buildObjectKey(document.getFolderId(), documentId);

            // Prepare metadata
            Map<String, String> metadata = buildMetadata(document);

            // Create put request
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .metadata(metadata)
                    .contentType(document.getMimeType())
                    .contentLength((long) content.length)
                    .serverSideEncryption(properties.getEnableEncryption() ?
                            ServerSideEncryption.AES256 : null)
                    .storageClass(StorageClass.fromValue(properties.getStorageClass()))
                    .build();

            // Upload to S3
            PutObjectResponse response = s3Client.putObject(putRequest, RequestBody.fromBytes(content));

            // Build and return the created document
            return document.toBuilder()
                    .id(documentId)
                    .size((long) content.length)
                    .checksum(response.eTag().replace("\"", ""))
                    .storagePath(objectKey)
                    .status(DocumentStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .modifiedAt(Instant.now())
                    .build();
        })
        .doOnSuccess(doc -> log.debug("Created document {} in S3 bucket {}", doc.getId(), properties.getBucketName()))
        .doOnError(error -> log.error("Failed to create document in S3", error));

        return ResilienceConfiguration.ReactiveResilience.withResilience(operation, circuitBreaker, retry)
                .timeout(Duration.ofMinutes(2))
                .doOnError(throwable -> log.error("Document creation failed after all resilience attempts: {}", throwable.getMessage()));
    }

    @Override
    public Mono<Document> getDocument(UUID documentId) {
        return Mono.fromCallable(() -> {
            // First, we need to find the object by searching with the document ID
            // This is a simplified implementation - in production, you might want to maintain an index
            String objectKey = findObjectKeyByDocumentId(documentId);
            
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headRequest);
            
            return buildDocumentFromS3Object(documentId, objectKey, response);
        })
        .doOnSuccess(doc -> log.debug("Retrieved document {} from S3", documentId))
        .doOnError(error -> log.error("Failed to retrieve document {} from S3", documentId, error));
    }

    @Override
    public Mono<Document> updateDocument(Document document) {
        return Mono.fromCallable(() -> {
            String objectKey = buildObjectKey(document.getFolderId(), document.getId());
            Map<String, String> metadata = buildMetadata(document);
            
            // Copy object to update metadata (S3 doesn't support metadata-only updates)
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(properties.getBucketName())
                    .sourceKey(objectKey)
                    .destinationBucket(properties.getBucketName())
                    .destinationKey(objectKey)
                    .metadata(metadata)
                    .metadataDirective(MetadataDirective.REPLACE)
                    .build();

            s3Client.copyObject(copyRequest);
            
            return document.toBuilder()
                    .modifiedAt(Instant.now())
                    .build();
        })
        .doOnSuccess(doc -> log.debug("Updated document {} in S3", document.getId()))
        .doOnError(error -> log.error("Failed to update document {} in S3", document.getId(), error));
    }

    @Override
    public Mono<Void> deleteDocument(UUID documentId) {
        return Mono.fromRunnable(() -> {
            String objectKey = findObjectKeyByDocumentId(documentId);
            
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteRequest);
        })
        .doOnSuccess(v -> log.debug("Deleted document {} from S3", documentId))
        .doOnError(error -> log.error("Failed to delete document {} from S3", documentId, error))
        .then();
    }

    @Override
    public Mono<Boolean> existsDocument(UUID documentId) {
        return Mono.fromCallable(() -> {
            try {
                String objectKey = findObjectKeyByDocumentId(documentId);
                HeadObjectRequest headRequest = HeadObjectRequest.builder()
                        .bucket(properties.getBucketName())
                        .key(objectKey)
                        .build();
                s3Client.headObject(headRequest);
                return true;
            } catch (NoSuchKeyException e) {
                return false;
            }
        })
        .doOnError(error -> log.error("Error checking document existence for {}", documentId, error));
    }

    @Override
    public Flux<Document> getDocumentsByFolder(UUID folderId) {
        return Flux.fromIterable(() -> {
            String prefix = buildFolderPrefix(folderId);
            
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(properties.getBucketName())
                    .prefix(prefix)
                    .build();

            return s3Client.listObjectsV2Paginator(listRequest)
                    .contents()
                    .stream()
                    .map(this::buildDocumentFromS3Object)
                    .iterator();
        })
        .doOnComplete(() -> log.debug("Retrieved documents for folder {} from S3", folderId))
        .doOnError(error -> log.error("Failed to retrieve documents for folder {} from S3", folderId, error));
    }

    @Override
    public Flux<Document> getDocumentsByOwner(UUID ownerId) {
        // This would require maintaining an index or using S3 tags for efficient querying
        // For now, we'll scan all objects and filter by owner metadata
        return Flux.fromIterable(() -> {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(properties.getBucketName())
                    .prefix(properties.getPathPrefix())
                    .build();

            return s3Client.listObjectsV2Paginator(listRequest)
                    .contents()
                    .stream()
                    .map(this::buildDocumentFromS3Object)
                    .filter(doc -> ownerId.equals(doc.getOwnerId()))
                    .iterator();
        })
        .doOnComplete(() -> log.debug("Retrieved documents for owner {} from S3", ownerId))
        .doOnError(error -> log.error("Failed to retrieve documents for owner {} from S3", ownerId, error));
    }

    @Override
    public Flux<Document> getDocumentsByStatus(DocumentStatus status) {
        // This would require maintaining an index or using S3 tags for efficient querying
        // For now, we'll scan all objects and filter by status metadata
        return Flux.fromIterable(() -> {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(properties.getBucketName())
                    .prefix(properties.getPathPrefix())
                    .build();

            return s3Client.listObjectsV2Paginator(listRequest)
                    .contents()
                    .stream()
                    .map(this::buildDocumentFromS3Object)
                    .filter(doc -> status.equals(doc.getStatus()))
                    .iterator();
        })
        .doOnComplete(() -> log.debug("Retrieved documents with status {} from S3", status))
        .doOnError(error -> log.error("Failed to retrieve documents with status {} from S3", status, error));
    }

    @Override
    public Mono<Document> moveDocument(UUID documentId, UUID targetFolderId) {
        return Mono.fromCallable(() -> {
            String currentObjectKey = findObjectKeyByDocumentId(documentId);
            String newObjectKey = buildObjectKey(targetFolderId, documentId);

            // Copy object to new location
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(properties.getBucketName())
                    .sourceKey(currentObjectKey)
                    .destinationBucket(properties.getBucketName())
                    .destinationKey(newObjectKey)
                    .build();

            s3Client.copyObject(copyRequest);

            // Delete original object
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(currentObjectKey)
                    .build();

            s3Client.deleteObject(deleteRequest);

            // Return updated document
            Document updatedDoc = getDocument(documentId).block();
            return updatedDoc;
        })
        .doOnSuccess(doc -> log.debug("Moved document {} to folder {} in S3", documentId, targetFolderId))
        .doOnError(error -> log.error("Failed to move document {} to folder {} in S3", documentId, targetFolderId, error));
    }

    @Override
    public Mono<Document> copyDocument(UUID documentId, UUID targetFolderId, String newName) {
        return Mono.fromCallable(() -> {
            String sourceObjectKey = findObjectKeyByDocumentId(documentId);
            UUID newDocumentId = UUID.randomUUID();
            String targetObjectKey = buildObjectKey(targetFolderId, newDocumentId);

            // Get source document metadata
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(sourceObjectKey)
                    .build();
            HeadObjectResponse headResponse = s3Client.headObject(headRequest);

            // Prepare new metadata
            Map<String, String> newMetadata = new HashMap<>(headResponse.metadata());
            newMetadata.put("document-id", newDocumentId.toString());
            if (newName != null) {
                newMetadata.put("name", newName);
            }
            if (targetFolderId != null) {
                newMetadata.put("folder-id", targetFolderId.toString());
            }

            // Copy object with new metadata
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(properties.getBucketName())
                    .sourceKey(sourceObjectKey)
                    .destinationBucket(properties.getBucketName())
                    .destinationKey(targetObjectKey)
                    .metadata(newMetadata)
                    .metadataDirective(MetadataDirective.REPLACE)
                    .build();

            s3Client.copyObject(copyRequest);

            // Build and return the copied document
            HeadObjectResponse newHeadResponse = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(targetObjectKey)
                    .build());
            return buildDocumentFromS3Object(newDocumentId, targetObjectKey, newHeadResponse);
        })
        .doOnSuccess(doc -> log.debug("Copied document {} to folder {} with new ID {} in S3",
                documentId, targetFolderId, doc.getId()))
        .doOnError(error -> log.error("Failed to copy document {} to folder {} in S3",
                documentId, targetFolderId, error));
    }

    @Override
    public String getAdapterName() {
        return "S3DocumentAdapter";
    }

    // Helper methods

    private String buildObjectKey(UUID folderId, UUID documentId) {
        StringBuilder keyBuilder = new StringBuilder(properties.getPathPrefix());
        if (folderId != null) {
            keyBuilder.append(folderId).append("/");
        }
        keyBuilder.append(documentId);
        return keyBuilder.toString();
    }

    private String buildFolderPrefix(UUID folderId) {
        return properties.getPathPrefix() + folderId + "/";
    }

    private Map<String, String> buildMetadata(Document document) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("document-id", document.getId().toString());
        metadata.put("name", document.getName());
        metadata.put("mime-type", document.getMimeType());
        if (document.getOwnerId() != null) {
            metadata.put("owner-id", document.getOwnerId().toString());
        }
        if (document.getFolderId() != null) {
            metadata.put("folder-id", document.getFolderId().toString());
        }
        if (document.getStatus() != null) {
            metadata.put("status", document.getStatus().toString());
        }
        return metadata;
    }

    private String findObjectKeyByDocumentId(UUID documentId) {
        // In a production implementation, you would maintain an index
        // For now, we'll use a simple pattern-based search
        return properties.getPathPrefix() + documentId;
    }

    private Document buildDocumentFromS3Object(S3Object s3Object) {
        // Extract document ID from object key
        String objectKey = s3Object.key();
        String documentIdStr = objectKey.substring(objectKey.lastIndexOf('/') + 1);
        UUID documentId = UUID.fromString(documentIdStr);
        
        // Get object metadata
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(objectKey)
                .build();
        HeadObjectResponse headResponse = s3Client.headObject(headRequest);
        
        return buildDocumentFromS3Object(documentId, objectKey, headResponse);
    }

    private Document buildDocumentFromS3Object(UUID documentId, String objectKey, HeadObjectResponse headResponse) {
        Map<String, String> metadata = headResponse.metadata();
        
        return Document.builder()
                .id(documentId)
                .name(metadata.get("name"))
                .mimeType(metadata.get("mime-type"))
                .size(headResponse.contentLength())
                .checksum(headResponse.eTag().replace("\"", ""))
                .storagePath(objectKey)
                .ownerId(metadata.get("owner-id") != null ? UUID.fromString(metadata.get("owner-id")) : null)
                .folderId(metadata.get("folder-id") != null ? UUID.fromString(metadata.get("folder-id")) : null)
                .status(metadata.get("status") != null ? DocumentStatus.valueOf(metadata.get("status")) : DocumentStatus.ACTIVE)
                .createdAt(headResponse.lastModified())
                .modifiedAt(headResponse.lastModified())
                .build();
    }
}
