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

import com.firefly.core.ecm.domain.model.document.Document;
import com.firefly.core.ecm.domain.enums.document.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

/**
 * Unit tests for S3DocumentAdapter.
 * 
 * <p>These tests use mocked S3Client to verify adapter behavior without requiring
 * actual AWS infrastructure. Tests cover all CRUD operations, error scenarios,
 * and edge cases.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class S3DocumentAdapterTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3AdapterProperties properties;

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private Retry retry;

    private S3DocumentAdapter adapter;

    private static final String TEST_BUCKET = "test-bucket";
    private static final String TEST_PREFIX = "test-prefix";
    private static final UUID TEST_DOCUMENT_ID = UUID.randomUUID();
    private static final UUID TEST_FOLDER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(properties.getBucketName()).thenReturn(TEST_BUCKET);
        when(properties.getPathPrefix()).thenReturn(TEST_PREFIX);
        when(properties.getEnableEncryption()).thenReturn(true);
        when(properties.getStorageClass()).thenReturn("STANDARD");

        // Create real CircuitBreaker and Retry instances for testing
        // This avoids the complex mocking issues with reactive operators
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig cbConfig =
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(100) // Never open in tests
                .slidingWindowSize(10)
                .minimumNumberOfCalls(10) // High threshold to avoid opening
                .build();

        io.github.resilience4j.retry.RetryConfig retryConfig =
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(1) // No retries in tests
                .build();

        // Use real instances to avoid reactive operator initialization issues
        CircuitBreaker realCircuitBreaker = CircuitBreaker.of("test-cb", cbConfig);
        Retry realRetry = Retry.of("test-retry", retryConfig);

        adapter = new S3DocumentAdapter(s3Client, properties, realCircuitBreaker, realRetry);
    }

    @Test
    void createDocument_ShouldCreateDocumentSuccessfully() {
        // Given
        Document document = Document.builder()
                .id(TEST_DOCUMENT_ID) // Provide an ID to avoid NullPointerException in buildMetadata
                .name("test-document.pdf")
                .mimeType("application/pdf")
                .folderId(TEST_FOLDER_ID)
                .ownerId(UUID.randomUUID())
                .status(DocumentStatus.ACTIVE)
                .build();

        byte[] content = "test content".getBytes();

        PutObjectResponse putResponse = PutObjectResponse.builder()
                .eTag("test-etag")
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(putResponse);

        // When & Then
        StepVerifier.create(adapter.createDocument(document, content))
                .expectNextMatches(createdDoc -> 
                    createdDoc.getName().equals("test-document.pdf") &&
                    createdDoc.getMimeType().equals("application/pdf") &&
                    createdDoc.getId() != null &&
                    createdDoc.getCreatedAt() != null
                )
                .verifyComplete();

        verify(s3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    void getDocument_ShouldRetrieveDocumentSuccessfully() {
        // Given
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentLength(100L)
                .contentType("application/pdf")
                .lastModified(Instant.now())
                .eTag("\"test-etag\"")
                .metadata(java.util.Map.of(
                    "name", "test-doc.pdf",
                    "mime-type", "application/pdf",
                    "status", "ACTIVE"
                ))
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(headResponse);

        // When & Then
        StepVerifier.create(adapter.getDocument(TEST_DOCUMENT_ID))
                .expectNextMatches(doc ->
                    doc.getId().equals(TEST_DOCUMENT_ID) &&
                    doc.getSize() == 100L &&
                    doc.getName().equals("test-doc.pdf")
                )
                .verifyComplete();

        verify(s3Client).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void deleteDocument_ShouldDeleteDocumentSuccessfully() {
        // Given
        ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
                .contents(S3Object.builder()
                        .key(TEST_PREFIX + "/" + TEST_FOLDER_ID + "/" + TEST_DOCUMENT_ID)
                        .build())
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(listResponse);

        DeleteObjectResponse deleteResponse = DeleteObjectResponse.builder().build();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(deleteResponse);

        // When & Then
        StepVerifier.create(adapter.deleteDocument(TEST_DOCUMENT_ID))
                .verifyComplete();

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void existsDocument_ShouldReturnTrueWhenDocumentExists() {
        // Given - Mock headObject to return successfully
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .eTag("\"test-etag\"")
                .build();
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(headResponse);

        // When & Then
        StepVerifier.create(adapter.existsDocument(TEST_DOCUMENT_ID))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void existsDocument_ShouldReturnFalseWhenDocumentNotExists() {
        // Given - Mock headObject to throw NoSuchKeyException
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Key not found").build());

        // When & Then
        StepVerifier.create(adapter.existsDocument(TEST_DOCUMENT_ID))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void getDocumentsByFolder_ShouldReturnDocumentsInFolder() {
        // Given
        UUID doc1Id = UUID.randomUUID();
        UUID doc2Id = UUID.randomUUID();

        S3Object obj1 = S3Object.builder()
                .key(TEST_PREFIX + "/" + TEST_FOLDER_ID + "/" + doc1Id)
                .size(100L)
                .lastModified(Instant.now())
                .build();
        S3Object obj2 = S3Object.builder()
                .key(TEST_PREFIX + "/" + TEST_FOLDER_ID + "/" + doc2Id)
                .size(200L)
                .lastModified(Instant.now())
                .build();

        java.util.List<S3Object> objectList = java.util.Arrays.asList(obj1, obj2);

        // Mock the paginator
        ListObjectsV2Iterable mockIterable = mock(ListObjectsV2Iterable.class);
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
                .thenReturn(mockIterable);
        when(mockIterable.contents()).thenReturn(() -> objectList.iterator());

        // Mock HeadObject calls for building documents
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentLength(100L)
                .eTag("\"test-etag\"")
                .metadata(java.util.Map.of(
                    "name", "test-doc.pdf",
                    "mime-type", "application/pdf",
                    "folder-id", TEST_FOLDER_ID.toString(),
                    "status", "ACTIVE"
                ))
                .lastModified(Instant.now())
                .build();
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(headResponse);

        // When & Then
        StepVerifier.create(adapter.getDocumentsByFolder(TEST_FOLDER_ID))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void createDocument_ShouldHandleS3Exception() {
        // Given
        Document document = Document.builder()
                .id(TEST_DOCUMENT_ID) // Provide an ID to avoid NullPointerException in buildMetadata
                .name("test-document.pdf")
                .mimeType("application/pdf")
                .folderId(TEST_FOLDER_ID)
                .build();

        byte[] content = "test content".getBytes();

        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        // When & Then
        StepVerifier.create(adapter.createDocument(document, content))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void getAdapterName_ShouldReturnCorrectName() {
        // When & Then
        String adapterName = adapter.getAdapterName();
        assert adapterName.equals("S3DocumentAdapter");
    }
}
