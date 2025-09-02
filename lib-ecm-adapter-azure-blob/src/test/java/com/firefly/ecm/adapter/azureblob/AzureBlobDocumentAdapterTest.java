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

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.firefly.core.ecm.domain.model.document.Document;
import com.firefly.core.ecm.domain.enums.document.DocumentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AzureBlobDocumentAdapter.
 *
 * <p>This test class provides comprehensive coverage of the Azure Blob Storage document adapter,
 * including:</p>
 * <ul>
 *   <li>Document CRUD operations</li>
 *   <li>Document search and filtering</li>
 *   <li>Error handling and resilience</li>
 *   <li>Configuration validation</li>
 *   <li>Blob storage integration</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AzureBlobDocumentAdapterTest {

    @Mock
    private BlobContainerClient containerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private Retry retry;

    private AzureBlobAdapterProperties properties;
    private ObjectMapper objectMapper;
    private AzureBlobDocumentAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new AzureBlobAdapterProperties();
        properties.setAccountName("testaccount");
        properties.setContainerName("testcontainer");
        properties.setPathPrefix("documents/");

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        adapter = new AzureBlobDocumentAdapter(
            containerClient, 
            properties, 
            objectMapper, 
            circuitBreaker, 
            retry
        );
    }

    @Test
    void createDocument_ShouldCreateDocumentSuccessfully() {
        // Given
        UUID documentId = UUID.randomUUID();
        UUID folderId = UUID.randomUUID();
        Document document = Document.builder()
            .id(documentId)
            .name("test-document.pdf")
            .mimeType("application/pdf")
            .size(1024L)
            .folderId(folderId)
            .ownerId(UUID.randomUUID())
            .status(DocumentStatus.ACTIVE)
            .build();

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        doNothing().when(blobClient).upload(any(ByteArrayInputStream.class), anyLong(), anyBoolean());
        doNothing().when(blobClient).setMetadata(anyMap());

        // When
        Mono<Document> result = adapter.createDocument(document, null);

        // Then
        StepVerifier.create(result)
            .assertNext(createdDoc -> {
                assertThat(createdDoc.getId()).isEqualTo(documentId);
                assertThat(createdDoc.getName()).isEqualTo("test-document.pdf");
                assertThat(createdDoc.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
                assertThat(createdDoc.getCreatedAt()).isNotNull();
            })
            .verifyComplete();

        verify(containerClient).getBlobClient(contains(documentId.toString()));
        verify(blobClient).upload(any(ByteArrayInputStream.class), anyLong(), eq(true));
        verify(blobClient).setMetadata(anyMap());
    }

    @Test
    void createDocument_WithContent_ShouldCreateDocumentAndStoreContent() {
        // Given
        UUID documentId = UUID.randomUUID();
        Document document = Document.builder()
            .id(documentId)
            .name("test-document.pdf")
            .mimeType("application/pdf")
            .size(1024L)
            .status(DocumentStatus.ACTIVE)
            .build();
        byte[] content = "test content".getBytes();

        BlobClient contentBlobClient = mock(BlobClient.class);
        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient, contentBlobClient);
        doNothing().when(blobClient).upload(any(ByteArrayInputStream.class), anyLong(), anyBoolean());
        doNothing().when(blobClient).setMetadata(anyMap());
        doNothing().when(contentBlobClient).upload(any(ByteArrayInputStream.class), anyLong(), anyBoolean());

        // When
        Mono<Document> result = adapter.createDocument(document, content);

        // Then
        StepVerifier.create(result)
            .assertNext(createdDoc -> {
                assertThat(createdDoc.getId()).isEqualTo(documentId);
                assertThat(createdDoc.getName()).isEqualTo("test-document.pdf");
            })
            .verifyComplete();

        verify(containerClient, times(2)).getBlobClient(anyString());
        verify(contentBlobClient).upload(any(ByteArrayInputStream.class), eq((long) content.length), eq(true));
    }

    @Test
    void getDocument_ShouldRetrieveDocumentSuccessfully() throws Exception {
        // Given
        UUID documentId = UUID.randomUUID();
        Document expectedDocument = Document.builder()
            .id(documentId)
            .name("test-document.pdf")
            .mimeType("application/pdf")
            .size(1024L)
            .status(DocumentStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();

        BlobItem blobItem = mock(BlobItem.class);
        when(blobItem.getName()).thenReturn("documents/folder/" + documentId + ".json");

        @SuppressWarnings("unchecked")
        com.azure.core.http.rest.PagedIterable<BlobItem> pagedIterable = mock(com.azure.core.http.rest.PagedIterable.class);
        when(pagedIterable.iterator()).thenReturn(Arrays.asList(blobItem).iterator());
        when(containerClient.listBlobs(any(ListBlobsOptions.class), isNull()))
            .thenReturn(pagedIterable);
        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        objectMapper.writeValue(outputStream, expectedDocument);
        doAnswer(invocation -> {
            ByteArrayOutputStream stream = invocation.getArgument(0);
            stream.write(outputStream.toByteArray());
            return null;
        }).when(blobClient).downloadStream(any(ByteArrayOutputStream.class));

        // When
        Mono<Document> result = adapter.getDocument(documentId);

        // Then
        StepVerifier.create(result)
            .assertNext(document -> {
                assertThat(document.getId()).isEqualTo(documentId);
                assertThat(document.getName()).isEqualTo("test-document.pdf");
                assertThat(document.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
            })
            .verifyComplete();

        verify(containerClient).listBlobs(any(ListBlobsOptions.class), isNull());
        verify(blobClient).downloadStream(any(ByteArrayOutputStream.class));
    }

    @Test
    void getDocument_WhenNotFound_ShouldReturnEmpty() {
        // Given
        UUID documentId = UUID.randomUUID();

        @SuppressWarnings("unchecked")
        com.azure.core.http.rest.PagedIterable<BlobItem> emptyPagedIterable = mock(com.azure.core.http.rest.PagedIterable.class);
        when(emptyPagedIterable.iterator()).thenReturn(Arrays.<BlobItem>asList().iterator());
        when(containerClient.listBlobs(any(ListBlobsOptions.class), isNull()))
            .thenReturn(emptyPagedIterable);

        // When
        Mono<Document> result = adapter.getDocument(documentId);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(containerClient).listBlobs(any(ListBlobsOptions.class), isNull());
    }

    @Test
    void updateDocument_ShouldUpdateDocumentSuccessfully() throws Exception {
        // Given
        UUID documentId = UUID.randomUUID();
        UUID folderId = UUID.randomUUID();
        Document document = Document.builder()
            .id(documentId)
            .name("updated-document.pdf")
            .mimeType("application/pdf")
            .size(2048L)
            .folderId(folderId)
            .status(DocumentStatus.ACTIVE)
            .createdAt(Instant.now().minusSeconds(3600))
            .build();

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);
        doNothing().when(blobClient).upload(any(ByteArrayInputStream.class), anyLong(), anyBoolean());
        doNothing().when(blobClient).setMetadata(anyMap());

        // When
        Mono<Document> result = adapter.updateDocument(document);

        // Then
        StepVerifier.create(result)
            .assertNext(updatedDoc -> {
                assertThat(updatedDoc.getId()).isEqualTo(documentId);
                assertThat(updatedDoc.getName()).isEqualTo("updated-document.pdf");
                assertThat(updatedDoc.getModifiedAt()).isNotNull();
            })
            .verifyComplete();

        verify(blobClient).exists();
        verify(blobClient).upload(any(ByteArrayInputStream.class), anyLong(), eq(true));
        verify(blobClient).setMetadata(anyMap());
    }

    @Test
    void updateDocument_WhenNotFound_ShouldThrowException() {
        // Given
        UUID documentId = UUID.randomUUID();
        Document document = Document.builder()
            .id(documentId)
            .name("updated-document.pdf")
            .build();

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(false);

        // When
        Mono<Document> result = adapter.updateDocument(document);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> 
                throwable instanceof RuntimeException && 
                throwable.getMessage().contains("Document not found"))
            .verify();

        verify(blobClient).exists();
        verify(blobClient, never()).upload(any(), anyLong(), anyBoolean());
    }

    @Test
    void deleteDocument_ShouldDeleteDocumentAndContentSuccessfully() {
        // Given
        UUID documentId = UUID.randomUUID();
        BlobItem documentBlobItem = mock(BlobItem.class);
        BlobItem contentBlobItem = mock(BlobItem.class);
        when(documentBlobItem.getName()).thenReturn("documents/folder/" + documentId + ".json");
        when(contentBlobItem.getName()).thenReturn("documents/" + documentId + ".content");

        @SuppressWarnings("unchecked")
        com.azure.core.http.rest.PagedIterable<BlobItem> pagedIterable = mock(com.azure.core.http.rest.PagedIterable.class);
        when(pagedIterable.iterator()).thenReturn(Arrays.asList(documentBlobItem, contentBlobItem).iterator());
        when(containerClient.listBlobs(any(ListBlobsOptions.class), isNull()))
            .thenReturn(pagedIterable);

        BlobClient documentBlobClient = mock(BlobClient.class);
        BlobClient contentBlobClient = mock(BlobClient.class);
        when(containerClient.getBlobClient(documentBlobItem.getName())).thenReturn(documentBlobClient);
        when(containerClient.getBlobClient(contentBlobItem.getName())).thenReturn(contentBlobClient);

        // When
        Mono<Void> result = adapter.deleteDocument(documentId);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(containerClient).listBlobs(any(ListBlobsOptions.class), isNull());
        verify(documentBlobClient).delete();
        verify(contentBlobClient).delete();
    }

    @Test
    void getDocumentsByFolder_ShouldReturnDocumentsInFolder() throws Exception {
        // Given
        UUID folderId = UUID.randomUUID();
        Long documentId1 = Long.randomUUID();
        Long documentId2 = Long.randomUUID();

        Document document1 = Document.builder()
            .id(documentId1)
            .name("doc1.pdf")
            .folderId(folderId)
            .status(DocumentStatus.ACTIVE)
            .build();

        Document document2 = Document.builder()
            .id(documentId2)
            .name("doc2.pdf")
            .folderId(folderId)
            .status(DocumentStatus.ACTIVE)
            .build();

        BlobItem blobItem1 = mock(BlobItem.class);
        BlobItem blobItem2 = mock(BlobItem.class);
        when(blobItem1.getName()).thenReturn("documents/" + folderId + "/" + documentId1 + ".json");
        when(blobItem2.getName()).thenReturn("documents/" + folderId + "/" + documentId2 + ".json");

        @SuppressWarnings("unchecked")
        com.azure.core.http.rest.PagedIterable<BlobItem> pagedIterable = mock(com.azure.core.http.rest.PagedIterable.class);
        when(pagedIterable.iterator()).thenReturn(Arrays.asList(blobItem1, blobItem2).iterator());
        when(containerClient.listBlobs(any(ListBlobsOptions.class), isNull()))
            .thenReturn(pagedIterable);

        BlobClient blobClient1 = mock(BlobClient.class);
        BlobClient blobClient2 = mock(BlobClient.class);
        when(containerClient.getBlobClient(blobItem1.getName())).thenReturn(blobClient1);
        when(containerClient.getBlobClient(blobItem2.getName())).thenReturn(blobClient2);

        // Mock document downloads
        doAnswer(invocation -> {
            ByteArrayOutputStream stream = invocation.getArgument(0);
            objectMapper.writeValue(stream, document1);
            return null;
        }).when(blobClient1).downloadStream(any(ByteArrayOutputStream.class));

        doAnswer(invocation -> {
            ByteArrayOutputStream stream = invocation.getArgument(0);
            objectMapper.writeValue(stream, document2);
            return null;
        }).when(blobClient2).downloadStream(any(ByteArrayOutputStream.class));

        // When
        Flux<Document> result = adapter.getDocumentsByFolder(folderId);

        // Then
        StepVerifier.create(result)
            .assertNext(doc -> assertThat(doc.getName()).isEqualTo("doc1.pdf"))
            .assertNext(doc -> assertThat(doc.getName()).isEqualTo("doc2.pdf"))
            .verifyComplete();

        verify(containerClient).listBlobs(any(ListBlobsOptions.class), isNull());
        verify(blobClient1).downloadStream(any(ByteArrayOutputStream.class));
        verify(blobClient2).downloadStream(any(ByteArrayOutputStream.class));
    }

    @Test
    void existsDocument_WhenDocumentExists_ShouldReturnTrue() {
        // Given
        UUID documentId = UUID.randomUUID();
        BlobItem blobItem = mock(BlobItem.class);
        when(blobItem.getName()).thenReturn("documents/folder/" + documentId + ".json");
        @SuppressWarnings("unchecked")
        com.azure.core.http.rest.PagedIterable<BlobItem> pagedIterable = mock(com.azure.core.http.rest.PagedIterable.class);
        when(pagedIterable.iterator()).thenReturn(Arrays.asList(blobItem).iterator());
        when(containerClient.listBlobs(any(ListBlobsOptions.class), isNull()))
            .thenReturn(pagedIterable);

        // When
        Mono<Boolean> result = adapter.existsDocument(documentId);

        // Then
        StepVerifier.create(result)
            .expectNext(true)
            .verifyComplete();

        verify(containerClient).listBlobs(any(ListBlobsOptions.class), isNull());
    }

    @Test
    void existsDocument_WhenDocumentDoesNotExist_ShouldReturnFalse() {
        // Given
        UUID documentId = UUID.randomUUID();
        @SuppressWarnings("unchecked")
        com.azure.core.http.rest.PagedIterable<BlobItem> emptyPagedIterable = mock(com.azure.core.http.rest.PagedIterable.class);
        when(emptyPagedIterable.iterator()).thenReturn(Arrays.<BlobItem>asList().iterator());
        when(containerClient.listBlobs(any(ListBlobsOptions.class), isNull()))
            .thenReturn(emptyPagedIterable);

        // When
        Mono<Boolean> result = adapter.existsDocument(documentId);

        // Then
        StepVerifier.create(result)
            .expectNext(false)
            .verifyComplete();

        verify(containerClient).listBlobs(any(ListBlobsOptions.class), isNull());
    }

    @Test
    void getAdapterName_ShouldReturnCorrectName() {
        // When
        String adapterName = adapter.getAdapterName();

        // Then
        assertThat(adapterName).isEqualTo("AzureBlobDocumentAdapter");
    }
}
