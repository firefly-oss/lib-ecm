# Basic Usage Examples

This document provides basic usage examples for the Firefly ECM Library.

## Document Management

### Upload Document

```java
@Service
public class DocumentService {
    
    @Autowired
    private DocumentPort documentPort;
    
    public Mono<Document> uploadDocument(String name, byte[] content, String mimeType) {
        Document document = Document.builder()
            .name(name)
            .mimeType(mimeType)
            .size((long) content.length)
            .status(DocumentStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();
            
        return documentPort.createDocument(document, content);
    }
}
```

### Download Document

```java
@RestController
public class DocumentController {
    
    @Autowired
    private DocumentContentPort contentPort;
    
    @GetMapping("/documents/{id}/download")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadDocument(@PathVariable UUID id) {
        return contentPort.getContentStream(id)
            .collectList()
            .map(buffers -> ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .body(Flux.fromIterable(buffers)));
    }
}
```

### Search Documents

```java
public Flux<Document> searchDocuments(String query) {
    DocumentSearchCriteria criteria = DocumentSearchCriteria.builder()
        .query(query)
        .maxResults(50)
        .build();
        
    return documentPort.searchDocuments(criteria);
}
```

## Folder Management

### Create Folder Structure

```java
public Mono<Folder> createFolderStructure(String path) {
    return folderPort.createFolderPath(path);
}
```

### List Folder Contents

```java
public Flux<Document> listFolderContents(UUID folderId) {
    return folderPort.getFolderContents(folderId);
}
```

## Configuration Examples

### S3 Configuration

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: "s3"
    properties:
      bucket-name: "my-documents"
      region: "us-east-1"
    features:
      document-management: true
      content-storage: true
      versioning: true
```

### Alfresco Configuration

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: "alfresco"
    properties:
      server-url: "http://localhost:8080/alfresco"
      username: "admin"
      password: "admin"
    features:
      document-management: true
      folder-management: true
      permissions: true
```

## Error Handling

### Basic Error Handling

```java
public Mono<Document> uploadDocumentWithErrorHandling(String name, byte[] content) {
    return documentPort.createDocument(document, content)
        .doOnError(error -> log.error("Upload failed for: {}", name, error))
        .onErrorResume(DocumentException.class, error -> {
            // Handle specific document errors
            return Mono.error(new ServiceException("Document upload failed", error));
        })
        .onErrorResume(error -> {
            // Handle all other errors
            return Mono.error(new ServiceException("Unexpected error", error));
        });
}
```

### Retry Logic

```java
public Mono<Document> uploadDocumentWithRetry(String name, byte[] content) {
    return documentPort.createDocument(document, content)
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
            .filter(throwable -> throwable instanceof TransientException))
        .timeout(Duration.ofMinutes(5));
}
```

## Testing Examples

### Unit Test with Mock

```java
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
    
    @Mock
    private DocumentPort documentPort;
    
    @InjectMocks
    private DocumentService documentService;
    
    @Test
    void testUploadDocument() {
        // Given
        Document document = Document.builder()
            .name("test.pdf")
            .build();
        
        when(documentPort.createDocument(any(), any()))
            .thenReturn(Mono.just(document));
        
        // When & Then
        StepVerifier.create(documentService.uploadDocument("test.pdf", new byte[0], "application/pdf"))
            .expectNext(document)
            .verifyComplete();
    }
}
```

### Integration Test

```java
@SpringBootTest
@TestPropertySource(properties = {
    "firefly.ecm.adapter-type=s3",
    "firefly.ecm.properties.bucket-name=test-bucket"
})
class DocumentIntegrationTest {
    
    @Autowired
    private DocumentService documentService;
    
    @Test
    void testDocumentUploadAndRetrieval() {
        byte[] content = "test content".getBytes();
        
        StepVerifier.create(
            documentService.uploadDocument("test.txt", content, "text/plain")
                .flatMap(doc -> documentService.getDocument(doc.getId()))
        )
        .assertNext(document -> {
            assertThat(document.getName()).isEqualTo("test.txt");
            assertThat(document.getSize()).isEqualTo(content.length);
        })
        .verifyComplete();
    }
}
```

## Advanced Usage

### Reactive Streams

```java
public Flux<Document> processDocumentsBatch(List<DocumentRequest> requests) {
    return Flux.fromIterable(requests)
        .flatMap(request -> uploadDocument(request.getName(), request.getContent(), request.getMimeType()))
        .buffer(10) // Process in batches of 10
        .flatMap(Flux::fromIterable)
        .doOnNext(document -> log.info("Processed document: {}", document.getId()));
}
```

### Custom Metadata

```java
public Mono<Document> uploadDocumentWithMetadata(String name, byte[] content, Map<String, Object> metadata) {
    Document document = Document.builder()
        .name(name)
        .mimeType(detectMimeType(content))
        .size((long) content.length)
        .metadata(metadata)
        .status(DocumentStatus.ACTIVE)
        .createdAt(Instant.now())
        .build();
        
    return documentPort.createDocument(document, content);
}
```

### Document Versioning

```java
public Mono<DocumentVersion> createDocumentVersion(UUID documentId, byte[] content, String comment) {
    DocumentVersion version = DocumentVersion.builder()
        .documentId(documentId)
        .versionComment(comment)
        .size((long) content.length)
        .createdAt(Instant.now())
        .build();
        
    return documentVersionPort.createVersion(version, content);
}
```

## Next Steps

- [S3 Integration Guide](../guides/s3-integration.md)
- [Alfresco Integration Guide](../guides/alfresco-integration.md)
- [Configuration Reference](../configuration.md)
- [Architecture Guide](../architecture.md)
