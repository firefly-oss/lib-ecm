# Testing Guide

This guide covers testing strategies and best practices for the Firefly ECM Library, including unit testing, integration testing, and end-to-end testing approaches.

## Table of Contents

1. [Testing Philosophy](#1-testing-philosophy)
2. [Test Structure](#2-test-structure)
3. [Unit Testing](#3-unit-testing)
4. [Integration Testing](#4-integration-testing)
5. [Adapter Testing](#5-adapter-testing)
6. [Service Layer Testing](#6-service-layer-testing)
7. [End-to-End Testing](#7-end-to-end-testing)
8. [Test Data Management](#8-test-data-management)
9. [Performance Testing](#9-performance-testing)
10. [Best Practices](#10-best-practices)

## 1. Testing Philosophy

The Firefly ECM Library follows a comprehensive testing strategy based on the **Test Pyramid**:

```
    /\
   /  \     E2E Tests (Few)
  /____\    - Full system integration
 /      \   - Real external services
/__________\ Integration Tests (Some)
            - Component integration
            - Mock external services
            Unit Tests (Many)
            - Individual components
            - Fast and isolated
```

### Testing Principles

- **Fast Feedback**: Unit tests provide immediate feedback
- **Isolation**: Tests should not depend on external systems
- **Repeatability**: Tests should produce consistent results
- **Clarity**: Tests should be easy to understand and maintain
- **Coverage**: Critical paths should be thoroughly tested

## 2. Test Structure

### 2.1 Directory Structure

```
src/test/java/
├── unit/                           # Unit tests
│   ├── domain/                     # Domain model tests
│   ├── port/                       # Port interface tests
│   └── service/                    # Service layer tests
├── integration/                    # Integration tests
│   ├── adapter/                    # Adapter integration tests
│   ├── database/                   # Database integration tests
│   └── external/                   # External service tests
├── e2e/                           # End-to-end tests
│   ├── scenarios/                  # Business scenario tests
│   └── performance/                # Performance tests
└── fixtures/                      # Test data and utilities
    ├── data/                       # Test data files
    ├── builders/                   # Test object builders
    └── mocks/                      # Mock implementations
```

### 2.2 Test Dependencies

Add testing dependencies to your `pom.xml`:

```xml
<dependencies>
    <!-- Spring Boot Test Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Reactor Test -->
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Testcontainers for integration testing -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>localstack</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- WireMock for external service mocking -->
    <dependency>
        <groupId>com.github.tomakehurst</groupId>
        <artifactId>wiremock-jre8</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ for fluent assertions -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 3. Unit Testing

### 3.1 Domain Model Testing

Test domain entities and value objects:

```java
// Test domain model behavior
@ExtendWith(MockitoExtension.class)
class DocumentTest {
    
    @Test
    void shouldCreateDocumentWithRequiredFields() {
        // Given
        String name = "test-document.pdf";
        String mimeType = "application/pdf";
        Long size = 1024L;
        
        // When
        Document document = Document.builder()
            .name(name)
            .mimeType(mimeType)
            .size(size)
            .status(DocumentStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();
        
        // Then
        assertThat(document.getName()).isEqualTo(name);
        assertThat(document.getMimeType()).isEqualTo(mimeType);
        assertThat(document.getSize()).isEqualTo(size);
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
        assertThat(document.getCreatedAt()).isNotNull();
    }
    
    @Test
    void shouldValidateDocumentName() {
        // Given/When/Then
        assertThatThrownBy(() -> Document.builder()
            .name("")  // Invalid empty name
            .mimeType("application/pdf")
            .size(1024L)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Document name cannot be empty");
    }
}
```

### 3.2 Service Layer Testing

Test business logic with mocked dependencies:

```java
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
    
    @Mock
    private DocumentPort documentPort;
    
    @Mock
    private DocumentContentPort contentPort;
    
    @InjectMocks
    private DocumentService documentService;
    
    @Test
    void shouldUploadDocumentSuccessfully() {
        // Given
        String fileName = "test.pdf";
        byte[] content = "test content".getBytes();
        String mimeType = "application/pdf";
        
        Document expectedDocument = Document.builder()
            .id(UUID.randomUUID())
            .name(fileName)
            .mimeType(mimeType)
            .size((long) content.length)
            .status(DocumentStatus.ACTIVE)
            .build();
        
        when(documentPort.createDocument(any(Document.class), eq(content)))
            .thenReturn(Mono.just(expectedDocument));
        
        // When
        Mono<Document> result = documentService.uploadDocument(fileName, content, mimeType);
        
        // Then
        StepVerifier.create(result)
            .assertNext(document -> {
                assertThat(document.getName()).isEqualTo(fileName);
                assertThat(document.getMimeType()).isEqualTo(mimeType);
                assertThat(document.getSize()).isEqualTo(content.length);
                assertThat(document.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
            })
            .verifyComplete();
        
        verify(documentPort).createDocument(any(Document.class), eq(content));
    }
    
    @Test
    void shouldHandleUploadFailure() {
        // Given
        String fileName = "test.pdf";
        byte[] content = "test content".getBytes();
        String mimeType = "application/pdf";
        
        when(documentPort.createDocument(any(Document.class), eq(content)))
            .thenReturn(Mono.error(new RuntimeException("Storage error")));
        
        // When
        Mono<Document> result = documentService.uploadDocument(fileName, content, mimeType);
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> 
                throwable instanceof RuntimeException && 
                throwable.getMessage().equals("Storage error"))
            .verify();
    }
}
```

## 4. Integration Testing

### 4.1 Spring Boot Integration Tests

Test Spring Boot application context and component integration:

```java
@SpringBootTest
@TestPropertySource(properties = {
    "firefly.ecm.adapter-type=mock",
    "firefly.ecm.enabled=true"
})
class EcmIntegrationTest {
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private EcmPortProvider portProvider;
    
    @Test
    void shouldLoadApplicationContext() {
        assertThat(documentService).isNotNull();
        assertThat(portProvider).isNotNull();
    }
    
    @Test
    void shouldProvideDocumentPort() {
        // When
        DocumentPort documentPort = portProvider.getPort(DocumentPort.class);
        
        // Then
        assertThat(documentPort).isNotNull();
    }
}
```

### 4.2 Database Integration Tests

Test database operations with embedded database:

```java
@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DocumentRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Test
    void shouldSaveAndFindDocument() {
        // Given
        DocumentEntity document = DocumentEntity.builder()
            .name("test.pdf")
            .mimeType("application/pdf")
            .size(1024L)
            .status(DocumentStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();
        
        // When
        DocumentEntity saved = documentRepository.save(document);
        entityManager.flush();
        
        // Then
        Optional<DocumentEntity> found = documentRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("test.pdf");
    }
}
```

## 5. Adapter Testing

### 5.1 Mock Adapter Testing

Test adapter implementations with mocked external services:

```java
@ExtendWith(MockitoExtension.class)
class S3DocumentAdapterTest {
    
    @Mock
    private S3Client s3Client;
    
    @Mock
    private EcmProperties ecmProperties;
    
    private S3DocumentAdapter adapter;
    
    @BeforeEach
    void setUp() {
        when(ecmProperties.getAdapterPropertyAsString("bucket-name"))
            .thenReturn("test-bucket");
        when(ecmProperties.getAdapterPropertyAsString("region"))
            .thenReturn("us-east-1");
        
        adapter = new S3DocumentAdapter(s3Client, ecmProperties);
    }
    
    @Test
    void shouldCreateDocumentInS3() {
        // Given
        Document document = Document.builder()
            .name("test.pdf")
            .mimeType("application/pdf")
            .size(1024L)
            .build();
        
        byte[] content = "test content".getBytes();
        
        PutObjectResponse response = PutObjectResponse.builder()
            .eTag("test-etag")
            .build();
        
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(response);
        
        // When
        Mono<Document> result = adapter.createDocument(document, content);
        
        // Then
        StepVerifier.create(result)
            .assertNext(createdDoc -> {
                assertThat(createdDoc.getName()).isEqualTo("test.pdf");
                assertThat(createdDoc.getStoragePath()).isNotNull();
                assertThat(createdDoc.getSize()).isEqualTo(1024L);
            })
            .verifyComplete();
        
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
```

### 5.2 Testcontainers Integration

Test with real external services using Testcontainers:

```java
@SpringBootTest
@Testcontainers
class S3AdapterIntegrationTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
        .withServices(LocalStackContainer.Service.S3);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("firefly.ecm.adapter-type", () -> "s3");
        registry.add("firefly.ecm.properties.bucket-name", () -> "test-bucket");
        registry.add("firefly.ecm.properties.region", () -> "us-east-1");
        registry.add("firefly.ecm.properties.endpoint", localstack::getEndpointOverride);
        registry.add("firefly.ecm.properties.access-key", localstack::getAccessKey);
        registry.add("firefly.ecm.properties.secret-key", localstack::getSecretKey);
    }
    
    @Autowired
    private DocumentService documentService;
    
    @Test
    void shouldUploadAndRetrieveDocument() {
        // Given
        String fileName = "integration-test.pdf";
        byte[] content = "Integration test content".getBytes();
        String mimeType = "application/pdf";
        
        // When - Upload document
        Mono<Document> uploadResult = documentService.uploadDocument(fileName, content, mimeType);
        
        // Then - Verify upload
        StepVerifier.create(uploadResult)
            .assertNext(document -> {
                assertThat(document.getId()).isNotNull();
                assertThat(document.getName()).isEqualTo(fileName);
                assertThat(document.getSize()).isEqualTo(content.length);
                
                // When - Retrieve document
                Mono<Document> retrieveResult = documentService.getDocument(document.getId());
                
                // Then - Verify retrieval
                StepVerifier.create(retrieveResult)
                    .assertNext(retrieved -> {
                        assertThat(retrieved.getId()).isEqualTo(document.getId());
                        assertThat(retrieved.getName()).isEqualTo(fileName);
                    })
                    .verifyComplete();
            })
            .verifyComplete();
    }
}
```

## 6. Service Layer Testing

### 6.1 Business Logic Testing

Test complex business scenarios:

```java
@SpringBootTest
@TestPropertySource(properties = {
    "firefly.ecm.adapter-type=mock"
})
class SignatureWorkflowTest {
    
    @Autowired
    private SignatureService signatureService;
    
    @MockBean
    private DocumentPort documentPort;
    
    @MockBean
    private SignatureEnvelopePort envelopePort;
    
    @Test
    void shouldCreateCompleteSignatureWorkflow() {
        // Given
        UUID documentId = UUID.randomUUID();
        String signerEmail = "signer@example.com";
        String title = "Contract Signature";
        
        Document document = Document.builder()
            .id(documentId)
            .name("contract.pdf")
            .status(DocumentStatus.ACTIVE)
            .build();
        
        SignatureEnvelope envelope = SignatureEnvelope.builder()
            .id(UUID.randomUUID())
            .title(title)
            .status(EnvelopeStatus.DRAFT)
            .build();
        
        when(documentPort.getDocument(documentId)).thenReturn(Mono.just(document));
        when(envelopePort.createEnvelope(any())).thenReturn(Mono.just(envelope));
        when(envelopePort.sendEnvelope(any(), any())).thenReturn(Mono.just(envelope.toBuilder()
            .status(EnvelopeStatus.SENT).build()));
        
        // When
        Mono<SignatureEnvelope> result = signatureService.createSimpleSignatureWorkflow(
            documentId, signerEmail, title);
        
        // Then
        StepVerifier.create(result)
            .assertNext(sentEnvelope -> {
                assertThat(sentEnvelope.getTitle()).isEqualTo(title);
                assertThat(sentEnvelope.getStatus()).isEqualTo(EnvelopeStatus.SENT);
            })
            .verifyComplete();
        
        verify(documentPort).getDocument(documentId);
        verify(envelopePort).createEnvelope(any());
        verify(envelopePort).sendEnvelope(any(), any());
    }
}
```

## 7. End-to-End Testing

### 7.1 Full System Testing

Test complete user scenarios:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "firefly.ecm.adapter-type=mock"
})
class DocumentManagementE2ETest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void shouldCompleteDocumentLifecycle() {
        // Given
        MultiValueMap<String, HttpEntity<?>> parts = new LinkedMultiValueMap<>();
        parts.add("file", new FileSystemResource("src/test/resources/test-document.pdf"));
        parts.add("description", new HttpEntity<>("Test document"));
        
        // When - Upload document
        webTestClient.post()
            .uri("/api/documents/upload")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(parts))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(Document.class)
            .value(document -> {
                assertThat(document.getId()).isNotNull();
                assertThat(document.getName()).isEqualTo("test-document.pdf");
                
                UUID documentId = document.getId();
                
                // When - Get document
                webTestClient.get()
                    .uri("/api/documents/{id}", documentId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(Document.class)
                    .value(retrieved -> {
                        assertThat(retrieved.getId()).isEqualTo(documentId);
                        assertThat(retrieved.getName()).isEqualTo("test-document.pdf");
                    });
                
                // When - Download document
                webTestClient.get()
                    .uri("/api/documents/{id}/download", documentId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType("application/pdf");
                
                // When - Delete document
                webTestClient.delete()
                    .uri("/api/documents/{id}", documentId)
                    .exchange()
                    .expectStatus().isNoContent();
            });
    }
}
```

## 8. Test Data Management

### 8.1 Test Data Builders

Create reusable test data builders:

```java
public class DocumentTestDataBuilder {
    
    private UUID id = UUID.randomUUID();
    private String name = "test-document.pdf";
    private String mimeType = "application/pdf";
    private Long size = 1024L;
    private DocumentStatus status = DocumentStatus.ACTIVE;
    private Instant createdAt = Instant.now();
    
    public static DocumentTestDataBuilder aDocument() {
        return new DocumentTestDataBuilder();
    }
    
    public DocumentTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }
    
    public DocumentTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public DocumentTestDataBuilder withSize(Long size) {
        this.size = size;
        return this;
    }
    
    public DocumentTestDataBuilder withStatus(DocumentStatus status) {
        this.status = status;
        return this;
    }
    
    public Document build() {
        return Document.builder()
            .id(id)
            .name(name)
            .mimeType(mimeType)
            .size(size)
            .status(status)
            .createdAt(createdAt)
            .build();
    }
}
```

### 8.2 Test Fixtures

Organize test data files:

```
src/test/resources/
├── fixtures/
│   ├── documents/
│   │   ├── sample.pdf
│   │   ├── sample.docx
│   │   └── sample.txt
│   ├── data/
│   │   ├── test-documents.json
│   │   └── test-envelopes.json
│   └── config/
│       ├── test-application.yml
│       └── integration-test.yml
```

## 9. Performance Testing

### 9.1 Load Testing

Test system performance under load:

```java
@Test
void shouldHandleConcurrentDocumentUploads() {
    // Given
    int numberOfConcurrentUploads = 100;
    byte[] content = "Performance test content".getBytes();
    
    // When
    List<Mono<Document>> uploads = IntStream.range(0, numberOfConcurrentUploads)
        .mapToObj(i -> documentService.uploadDocument("perf-test-" + i + ".txt", content, "text/plain"))
        .collect(Collectors.toList());
    
    // Then
    StepVerifier.create(Flux.merge(uploads))
        .expectNextCount(numberOfConcurrentUploads)
        .verifyComplete();
}
```

## 10. Best Practices

### Testing Best Practices

1. **Test Naming**: Use descriptive test method names that explain the scenario
2. **Given-When-Then**: Structure tests clearly with setup, action, and verification
3. **Single Responsibility**: Each test should verify one specific behavior
4. **Test Independence**: Tests should not depend on each other
5. **Fast Execution**: Keep unit tests fast and integration tests reasonable
6. **Meaningful Assertions**: Use specific assertions that provide clear failure messages
7. **Test Coverage**: Aim for high coverage of critical business logic
8. **Reactive Testing**: Use StepVerifier for testing reactive streams
9. **Mock Judiciously**: Mock external dependencies but not internal logic
10. **Clean Test Code**: Apply the same quality standards to test code as production code

### Running Tests

```bash
# Run all tests
mvn test

# Run only unit tests
mvn test -Dtest="*Test"

# Run only integration tests
mvn test -Dtest="*IntegrationTest"

# Run with coverage
mvn test jacoco:report

# Run performance tests
mvn test -Dtest="*PerformanceTest"
```

This comprehensive testing approach ensures the reliability, maintainability, and performance of the Firefly ECM Library across all components and integration scenarios.
