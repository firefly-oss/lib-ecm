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
import com.firefly.core.ecm.port.document.DocumentContentPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Amazon S3 implementation of DocumentContentPort.
 *
 * <p>This adapter handles binary content operations using Amazon S3:</p>
 * <ul>
 *   <li>Streaming content download</li>
 *   <li>Byte array content retrieval</li>
 *   <li>Range requests for partial content</li>
 *   <li>Content validation and integrity checks</li>
 *   <li>Multipart upload for large files</li>
 * </ul>
 *
 * <p>The adapter supports both streaming and byte array operations,
 * automatically handling large files through S3's multipart upload
 * capabilities when configured.</p>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@EcmAdapter(
    type = "s3-content",
    description = "Amazon S3 Document Content Adapter",
    supportedFeatures = {
        AdapterFeature.CONTENT_STORAGE,
        AdapterFeature.STREAMING,
        AdapterFeature.CLOUD_STORAGE
    },
    requiredProperties = {"bucket-name", "region"},
    optionalProperties = {"access-key", "secret-key", "endpoint", "path-prefix"}
)
@Component
@ConditionalOnProperty(name = "firefly.ecm.adapter-type", havingValue = "s3")
public class S3DocumentContentAdapter implements DocumentContentPort {

    private final S3Client s3Client;
    private final S3AdapterProperties properties;
    private final DataBufferFactory dataBufferFactory;
    private final S3Presigner s3Presigner;

    public S3DocumentContentAdapter(S3Client s3Client, S3AdapterProperties properties, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.properties = properties;
        this.dataBufferFactory = new DefaultDataBufferFactory();
        this.s3Presigner = s3Presigner;
        log.info("S3DocumentContentAdapter initialized with bucket: {}", properties.getBucketName());
    }

    @Override
    public Mono<byte[]> getContent(UUID documentId) {
        return Mono.fromCallable(() -> {
            String objectKey = buildObjectKey(documentId);
            
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .build();

            try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getRequest)) {
                return response.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read content from S3", e);
            }
        })
        .doOnSuccess(content -> log.debug("Retrieved content for document {} ({} bytes)", documentId, content.length))
        .doOnError(error -> log.error("Failed to retrieve content for document {} from S3", documentId, error));
    }

    @Override
    public Flux<DataBuffer> getContentStream(UUID documentId) {
        return Flux.<DataBuffer>create(sink -> {
            try {
                String objectKey = buildObjectKey(documentId);
                
                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(properties.getBucketName())
                        .key(objectKey)
                        .build();

                ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getRequest);
                
                byte[] buffer = new byte[8192]; // 8KB buffer
                int bytesRead;
                
                while ((bytesRead = response.read(buffer)) != -1) {
                    DataBuffer dataBuffer = dataBufferFactory.allocateBuffer(bytesRead);
                    dataBuffer.write(buffer, 0, bytesRead);
                    sink.next(dataBuffer);
                }
                
                response.close();
                sink.complete();
                
            } catch (Exception e) {
                log.error("Failed to stream content for document {} from S3", documentId, e);
                sink.error(e);
            }
        })
        .doOnComplete(() -> log.debug("Completed streaming content for document {}", documentId))
        .doOnError(error -> log.error("Error streaming content for document {} from S3", documentId, error));
    }

    public Mono<byte[]> getContentRange(UUID documentId, Long start, Long end) {
        return Mono.fromCallable(() -> {
            String objectKey = buildObjectKey(documentId);
            String range = String.format("bytes=%d-%d", start, end);
            
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .range(range)
                    .build();

            try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getRequest)) {
                return response.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read content range from S3", e);
            }
        })
        .doOnSuccess(content -> log.debug("Retrieved content range for document {} ({}-{}, {} bytes)", 
                documentId, start, end, content.length))
        .doOnError(error -> log.error("Failed to retrieve content range for document {} from S3", documentId, error));
    }

    @Override
    public Mono<String> storeContent(UUID documentId, byte[] content, String mimeType) {
        return Mono.fromCallable(() -> {
            String objectKey = buildObjectKey(documentId);

            if (properties.getEnableMultipart() && content.length > properties.getMultipartThreshold()) {
                storeContentMultipart(objectKey, content);
            } else {
                storeContentSingle(objectKey, content);
            }

            return objectKey;
        })
        .doOnSuccess(path -> log.debug("Stored content for document {} ({} bytes) at path {}",
                documentId, content.length, path))
        .doOnError(error -> log.error("Failed to store content for document {} in S3", documentId, error));
    }

    public Mono<Void> storeContent(UUID documentId, byte[] content) {
        return Mono.<Void>fromRunnable(() -> {
            String objectKey = buildObjectKey(documentId);
            
            if (properties.getEnableMultipart() && content.length > properties.getMultipartThreshold()) {
                storeContentMultipart(objectKey, content);
            } else {
                storeContentSingle(objectKey, content);
            }
        })
        .doOnSuccess(v -> log.debug("Stored content for document {} ({} bytes)", documentId, content.length))
        .doOnError(error -> log.error("Failed to store content for document {} in S3", documentId, error));
    }

    @Override
    public Mono<String> storeContentStream(UUID documentId, Flux<DataBuffer> contentStream, String mimeType, Long contentLength) {
        return contentStream
                .reduce(new ByteArrayOutputStream(), (outputStream, dataBuffer) -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        outputStream.write(bytes);
                        return outputStream;
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read from data buffer", e);
                    } finally {
                        // DataBuffer.release() method may not be available in all versions
                        // In production, proper resource management should be implemented
                    }
                })
                .flatMap(outputStream -> {
                    byte[] content = outputStream.toByteArray();
                    return storeContent(documentId, content)
                            .then(Mono.just(buildObjectKey(documentId)));
                })
                .doOnSuccess(path -> log.debug("Stored streamed content for document {} at path {}", documentId, path))
                .doOnError(error -> log.error("Failed to store streamed content for document {} in S3", documentId, error));
    }

    @Override
    public Mono<Void> deleteContent(UUID documentId) {
        return Mono.<Void>fromRunnable(() -> {
            String objectKey = buildObjectKey(documentId);
            
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteRequest);
        })
        .doOnSuccess(v -> log.debug("Deleted content for document {}", documentId))
        .doOnError(error -> log.error("Failed to delete content for document {} from S3", documentId, error));
    }

    @Override
    public Mono<Boolean> existsContent(UUID documentId) {
        return Mono.fromCallable(() -> {
            try {
                String objectKey = buildObjectKey(documentId);
                
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
        .doOnError(error -> log.error("Error checking content existence for document {}", documentId, error));
    }

    @Override
    public Mono<Long> getContentSize(UUID documentId) {
        return Mono.fromCallable(() -> {
            String objectKey = buildObjectKey(documentId);
            
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headRequest);
            return response.contentLength();
        })
        .doOnSuccess(size -> log.debug("Retrieved content size for document {}: {} bytes", documentId, size))
        .doOnError(error -> log.error("Failed to get content size for document {} from S3", documentId, error));
    }

    @Override
    public Flux<DataBuffer> getContentStreamByPath(String storagePath) {
        return Flux.<DataBuffer>create(sink -> {
            try {
                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(properties.getBucketName())
                        .key(storagePath)
                        .build();

                ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getRequest);

                byte[] buffer = new byte[8192]; // 8KB buffer
                int bytesRead;

                while ((bytesRead = response.read(buffer)) != -1) {
                    DataBuffer dataBuffer = dataBufferFactory.allocateBuffer(bytesRead);
                    dataBuffer.write(buffer, 0, bytesRead);
                    sink.next(dataBuffer);
                }

                response.close();
                sink.complete();

            } catch (Exception e) {
                log.error("Failed to stream content by path {} from S3", storagePath, e);
                sink.error(e);
            }
        })
        .doOnComplete(() -> log.debug("Completed streaming content by path {}", storagePath))
        .doOnError(error -> log.error("Error streaming content by path {} from S3", storagePath, error));
    }

    @Override
    public Mono<byte[]> getContentByPath(String storagePath) {
        return Mono.fromCallable(() -> {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(storagePath)
                    .build();

            try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getRequest)) {
                return response.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read content from S3 by path", e);
            }
        })
        .doOnSuccess(content -> log.debug("Retrieved content by path {} ({} bytes)", storagePath, content.length))
        .doOnError(error -> log.error("Failed to retrieve content by path {} from S3", storagePath, error));
    }

    @Override
    public Mono<Void> deleteContentByPath(String storagePath) {
        return Mono.<Void>fromRunnable(() -> {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(storagePath)
                    .build();

            s3Client.deleteObject(deleteRequest);
        })
        .doOnSuccess(v -> log.debug("Deleted content by path {}", storagePath))
        .doOnError(error -> log.error("Failed to delete content by path {} from S3", storagePath, error))
        .then();
    }

    @Override
    public Mono<String> calculateChecksum(UUID documentId, String algorithm) {
        return getContent(documentId)
                .map(content -> {
                    try {
                        java.security.MessageDigest digest = java.security.MessageDigest.getInstance(algorithm);
                        byte[] hash = digest.digest(content);
                        return bytesToHex(hash);
                    } catch (java.security.NoSuchAlgorithmException e) {
                        throw new RuntimeException("Unsupported checksum algorithm: " + algorithm, e);
                    }
                })
                .doOnSuccess(checksum -> log.debug("Calculated {} checksum for document {}: {}", algorithm, documentId, checksum))
                .doOnError(error -> log.error("Failed to calculate checksum for document {}", documentId, error));
    }

    @Override
    public Mono<Boolean> verifyChecksum(UUID documentId, String expectedChecksum, String algorithm) {
        return getContent(documentId)
                .map(content -> {
                    try {
                        java.security.MessageDigest digest = java.security.MessageDigest.getInstance(algorithm);
                        byte[] hash = digest.digest(content);
                        String actualChecksum = bytesToHex(hash);
                        return expectedChecksum.equalsIgnoreCase(actualChecksum);
                    } catch (java.security.NoSuchAlgorithmException e) {
                        throw new RuntimeException("Unsupported checksum algorithm: " + algorithm, e);
                    }
                })
                .doOnSuccess(matches -> log.debug("Checksum verification for document {}: {}", documentId, matches))
                .doOnError(error -> log.error("Failed to verify checksum for document {}", documentId, error));
    }

    /**
     * Generates a pre-signed URL for secure document upload.
     *
     * @param documentId the document ID
     * @param expirationMinutes expiration time in minutes
     * @return Mono containing the pre-signed upload URL
     */
    public Mono<String> generateUploadUrl(UUID documentId, int expirationMinutes) {
        return Mono.fromCallable(() -> {
            String objectKey = buildObjectKey(documentId);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(java.time.Duration.ofMinutes(expirationMinutes))
                    .putObjectRequest(putRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            return presignedRequest.url().toString();
        })
        .doOnSuccess(url -> log.debug("Generated upload URL for document {} (expires in {} minutes)",
                documentId, expirationMinutes))
        .doOnError(error -> log.error("Failed to generate upload URL for document {}", documentId, error));
    }

    /**
     * Generates a pre-signed URL for secure document download.
     *
     * @param documentId the document ID
     * @param expirationMinutes expiration time in minutes
     * @return Mono containing the pre-signed download URL
     */
    public Mono<String> generateDownloadUrl(UUID documentId, int expirationMinutes) {
        return Mono.fromCallable(() -> {
            String objectKey = buildObjectKey(documentId);

            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(java.time.Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        })
        .doOnSuccess(url -> log.debug("Generated download URL for document {} (expires in {} minutes)",
                documentId, expirationMinutes))
        .doOnError(error -> log.error("Failed to generate download URL for document {}", documentId, error));
    }

    /**
     * Initiates a multipart upload for large files.
     *
     * @param documentId the document ID
     * @param mimeType the content MIME type
     * @return Mono containing the upload ID
     */
    public Mono<String> initiateMultipartUpload(UUID documentId, String mimeType) {
        return Mono.fromCallable(() -> {
            String objectKey = buildObjectKey(documentId);

            CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .contentType(mimeType)
                    .serverSideEncryption(properties.getEnableEncryption() ?
                            ServerSideEncryption.AES256 : null)
                    .build();

            CreateMultipartUploadResponse response = s3Client.createMultipartUpload(request);
            return response.uploadId();
        })
        .doOnSuccess(uploadId -> log.debug("Initiated multipart upload for document {} with upload ID: {}",
                documentId, uploadId))
        .doOnError(error -> log.error("Failed to initiate multipart upload for document {}", documentId, error));
    }

    /**
     * Uploads a part in a multipart upload.
     *
     * @param documentId the document ID
     * @param uploadId the multipart upload ID
     * @param partNumber the part number (1-based)
     * @param partData the part data
     * @return Mono containing the ETag for the uploaded part
     */
    public Mono<String> uploadPart(UUID documentId, String uploadId, int partNumber, byte[] partData) {
        return Mono.fromCallable(() -> {
            String objectKey = buildObjectKey(documentId);

            UploadPartRequest request = UploadPartRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();

            UploadPartResponse response = s3Client.uploadPart(request, RequestBody.fromBytes(partData));
            return response.eTag();
        })
        .doOnSuccess(eTag -> log.debug("Uploaded part {} for document {} with ETag: {}",
                partNumber, documentId, eTag))
        .doOnError(error -> log.error("Failed to upload part {} for document {}", partNumber, documentId, error));
    }

    /**
     * Completes a multipart upload.
     *
     * @param documentId the document ID
     * @param uploadId the multipart upload ID
     * @param parts list of completed parts with their ETags
     * @return Mono containing the final ETag
     */
    public Mono<String> completeMultipartUpload(UUID documentId, String uploadId,
                                               java.util.List<CompletedPart> parts) {
        return Mono.fromCallable(() -> {
            String objectKey = buildObjectKey(documentId);

            CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder()
                    .parts(parts)
                    .build();

            CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .uploadId(uploadId)
                    .multipartUpload(completedUpload)
                    .build();

            CompleteMultipartUploadResponse response = s3Client.completeMultipartUpload(request);
            return response.eTag();
        })
        .doOnSuccess(eTag -> log.debug("Completed multipart upload for document {} with final ETag: {}",
                documentId, eTag))
        .doOnError(error -> log.error("Failed to complete multipart upload for document {}", documentId, error));
    }

    /**
     * Aborts a multipart upload.
     *
     * @param documentId the document ID
     * @param uploadId the multipart upload ID
     * @return Mono indicating completion
     */
    public Mono<Void> abortMultipartUpload(UUID documentId, String uploadId) {
        return Mono.<Void>fromRunnable(() -> {
            String objectKey = buildObjectKey(documentId);

            AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .uploadId(uploadId)
                    .build();

            s3Client.abortMultipartUpload(request);
        })
        .doOnSuccess(v -> log.debug("Aborted multipart upload for document {} with upload ID: {}",
                documentId, uploadId))
        .doOnError(error -> log.error("Failed to abort multipart upload for document {}", documentId, error));
    }

    public String getAdapterName() {
        return "S3DocumentContentAdapter";
    }

    // Helper method for checksum verification
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // Helper methods

    private String buildObjectKey(UUID documentId) {
        return properties.getPathPrefix() + documentId;
    }

    private void storeContentSingle(String objectKey, byte[] content) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(objectKey)
                .contentLength((long) content.length)
                .serverSideEncryption(properties.getEnableEncryption() ? 
                        ServerSideEncryption.AES256 : null)
                .storageClass(StorageClass.fromValue(properties.getStorageClass()))
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(content));
    }

    private void storeContentMultipart(String objectKey, byte[] content) {
        // Initiate multipart upload
        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(properties.getBucketName())
                .key(objectKey)
                .serverSideEncryption(properties.getEnableEncryption() ? 
                        ServerSideEncryption.AES256 : null)
                .storageClass(StorageClass.fromValue(properties.getStorageClass()))
                .build();

        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        String uploadId = createResponse.uploadId();

        try {
            // Upload parts
            int partSize = properties.getMultipartPartSize().intValue();
            int partNumber = 1;
            int offset = 0;
            
            while (offset < content.length) {
                int currentPartSize = Math.min(partSize, content.length - offset);
                byte[] partData = new byte[currentPartSize];
                System.arraycopy(content, offset, partData, 0, currentPartSize);
                
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(properties.getBucketName())
                        .key(objectKey)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .contentLength((long) currentPartSize)
                        .build();

                s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(partData));
                
                offset += currentPartSize;
                partNumber++;
            }

            // Complete multipart upload
            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .uploadId(uploadId)
                    .build();

            s3Client.completeMultipartUpload(completeRequest);
            
        } catch (Exception e) {
            // Abort multipart upload on error
            AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .uploadId(uploadId)
                    .build();
            
            s3Client.abortMultipartUpload(abortRequest);
            throw new RuntimeException("Failed to complete multipart upload", e);
        }
    }
}
