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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for S3DocumentContentAdapter.
 * 
 * <p>These tests use mocked S3Client and S3Presigner to verify adapter behavior
 * without requiring actual AWS infrastructure. Tests cover content operations,
 * streaming, pre-signed URLs, and error scenarios.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class S3DocumentContentAdapterTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3AdapterProperties properties;

    private S3DocumentContentAdapter adapter;

    private static final String TEST_BUCKET = "test-bucket";
    private static final String TEST_PREFIX = "test-prefix";
    private static final UUID TEST_DOCUMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(properties.getBucketName()).thenReturn(TEST_BUCKET);
        when(properties.getPathPrefix()).thenReturn(TEST_PREFIX);
        when(properties.getEnableMultipart()).thenReturn(false);
        when(properties.getMultipartThreshold()).thenReturn(5 * 1024 * 1024L); // 5MB
        when(properties.getEnableEncryption()).thenReturn(true);

        adapter = new S3DocumentContentAdapter(s3Client, properties, s3Presigner);
    }

    @Test
    void getContent_ShouldRetrieveContentSuccessfully() {
        // Given
        byte[] expectedContent = "test content".getBytes();
        ResponseInputStream<GetObjectResponse> responseStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new ByteArrayInputStream(expectedContent)
        );

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(responseStream);

        // When & Then
        StepVerifier.create(adapter.getContent(TEST_DOCUMENT_ID))
                .expectNextMatches(content -> java.util.Arrays.equals(content, expectedContent))
                .verifyComplete();

        verify(s3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    void storeContent_ShouldStoreContentSuccessfully() {
        // Given
        byte[] content = "test content".getBytes();
        String mimeType = "text/plain";

        PutObjectResponse putResponse = PutObjectResponse.builder()
                .eTag("test-etag")
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(putResponse);

        // When & Then
        StepVerifier.create(adapter.storeContent(TEST_DOCUMENT_ID, content, mimeType))
                .expectNextMatches(path -> path.contains(TEST_DOCUMENT_ID.toString()))
                .verifyComplete();

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void getContentStream_ShouldStreamContentSuccessfully() {
        // Given
        byte[] expectedContent = "test content for streaming".getBytes();
        ResponseInputStream<GetObjectResponse> responseStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new ByteArrayInputStream(expectedContent)
        );

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(responseStream);

        // When & Then
        StepVerifier.create(adapter.getContentStream(TEST_DOCUMENT_ID))
                .expectNextCount(1) // At least one DataBuffer chunk
                .verifyComplete();

        verify(s3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    void storeContentStream_ShouldStoreStreamedContentSuccessfully() {
        // Given
        DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
        DataBuffer buffer = bufferFactory.wrap("test streaming content".getBytes());
        Flux<DataBuffer> contentStream = Flux.just(buffer);
        String mimeType = "text/plain";

        PutObjectResponse putResponse = PutObjectResponse.builder()
                .eTag("test-etag")
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(putResponse);

        // When & Then
        StepVerifier.create(adapter.storeContentStream(TEST_DOCUMENT_ID, contentStream, mimeType, 100L))
                .expectNextMatches(path -> path.contains(TEST_DOCUMENT_ID.toString()))
                .verifyComplete();

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void deleteContent_ShouldDeleteContentSuccessfully() {
        // Given
        DeleteObjectResponse deleteResponse = DeleteObjectResponse.builder().build();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(deleteResponse);

        // When & Then
        StepVerifier.create(adapter.deleteContent(TEST_DOCUMENT_ID))
                .verifyComplete();

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void existsContent_ShouldReturnTrueWhenContentExists() {
        // Given
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentLength(100L)
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(headResponse);

        // When & Then
        StepVerifier.create(adapter.existsContent(TEST_DOCUMENT_ID))
                .expectNext(true)
                .verifyComplete();

        verify(s3Client).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void existsContent_ShouldReturnFalseWhenContentNotExists() {
        // Given
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        // When & Then
        StepVerifier.create(adapter.existsContent(TEST_DOCUMENT_ID))
                .expectNext(false)
                .verifyComplete();

        verify(s3Client).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void getContentSize_ShouldReturnCorrectSize() {
        // Given
        long expectedSize = 1024L;
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentLength(expectedSize)
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(headResponse);

        // When & Then
        StepVerifier.create(adapter.getContentSize(TEST_DOCUMENT_ID))
                .expectNext(expectedSize)
                .verifyComplete();

        verify(s3Client).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void calculateChecksum_ShouldCalculateCorrectChecksum() {
        // Given
        byte[] content = "test content".getBytes();
        ResponseInputStream<GetObjectResponse> responseStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new ByteArrayInputStream(content)
        );

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(responseStream);

        // When & Then
        StepVerifier.create(adapter.calculateChecksum(TEST_DOCUMENT_ID, "SHA-256"))
                .expectNextMatches(checksum -> checksum != null && !checksum.isEmpty())
                .verifyComplete();

        verify(s3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    void verifyChecksum_ShouldReturnTrueForMatchingChecksum() {
        // Given
        byte[] content = "test content".getBytes();
        ResponseInputStream<GetObjectResponse> responseStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new ByteArrayInputStream(content)
        );

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(responseStream);

        // Calculate expected checksum - correct SHA-1 of "test content"
        String expectedChecksum = "1eebdf4fdc9fc7bf283031b93f9aef3338de9052"; // SHA-1 of "test content"

        // When & Then
        StepVerifier.create(adapter.verifyChecksum(TEST_DOCUMENT_ID, expectedChecksum, "SHA-1"))
                .expectNext(true)
                .verifyComplete();

        verify(s3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    void generateUploadUrl_ShouldGenerateValidUrl() throws Exception {
        // Given
        URL expectedUrl = new URL("https://test-bucket.s3.amazonaws.com/test-key?presigned=true");
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(expectedUrl);

        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        // When & Then
        StepVerifier.create(adapter.generateUploadUrl(TEST_DOCUMENT_ID, 60))
                .expectNext(expectedUrl.toString())
                .verifyComplete();

        verify(s3Presigner).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void generateDownloadUrl_ShouldGenerateValidUrl() throws Exception {
        // Given
        URL expectedUrl = new URL("https://test-bucket.s3.amazonaws.com/test-key?presigned=true");
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(expectedUrl);

        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        // When & Then
        StepVerifier.create(adapter.generateDownloadUrl(TEST_DOCUMENT_ID, 60))
                .expectNext(expectedUrl.toString())
                .verifyComplete();

        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void storeContent_ShouldHandleS3Exception() {
        // Given
        byte[] content = "test content".getBytes();
        String mimeType = "text/plain";

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        // When & Then
        StepVerifier.create(adapter.storeContent(TEST_DOCUMENT_ID, content, mimeType))
                .expectError(RuntimeException.class)
                .verify();
    }
}
