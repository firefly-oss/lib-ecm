# Alfresco Integration Guide

This guide shows how to integrate Alfresco Content Services as an enterprise ECM backend for the Firefly ECM Library.

## Prerequisites

- Alfresco Content Services 7.0+
- Java 17+
- Spring Boot 3.0+
- Firefly ECM Library
- Network access to Alfresco server

## 1. Dependencies

Add the required dependencies to your `pom.xml`:

```xml
<dependencies>
    <!-- Firefly ECM Library -->
    <dependency>
        <groupId>com.firefly</groupId>
        <artifactId>lib-ecm</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Alfresco Java SDK -->
    <dependency>
        <groupId>org.alfresco</groupId>
        <artifactId>alfresco-java-sdk</artifactId>
        <version>5.2.0</version>
    </dependency>
    
    <!-- Apache Chemistry OpenCMIS for CMIS protocol -->
    <dependency>
        <groupId>org.apache.chemistry.opencmis</groupId>
        <artifactId>chemistry-opencmis-client-impl</artifactId>
        <version>1.1.0</version>
    </dependency>
    
    <!-- HTTP Client for REST API calls -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
</dependencies>
```

## 2. Alfresco Server Setup

### Alfresco Content Services

**Option A: Docker Deployment**
```bash
# Using Alfresco Docker Compose
git clone https://github.com/Alfresco/acs-deployment.git
cd acs-deployment/docker-compose
docker-compose up -d
```

**Option B: Alfresco Cloud**
- Sign up for Alfresco Cloud
- Get your tenant URL and credentials

### Create Service Account

1. Access Alfresco Admin Console
2. Create a dedicated service account for API access
3. Grant appropriate permissions:
   - Consumer (read access)
   - Editor (read/write access)
   - Collaborator (full access except delete)
   - Coordinator (full access)

## 3. Application Configuration

Configure your `application.yml`:

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: "alfresco"
    properties:
      # Alfresco server configuration
      server-url: "http://localhost:8080/alfresco"
      api-url: "http://localhost:8080/alfresco/api/-default-/public/alfresco/versions/1"
      
      # Authentication
      username: "service-account"
      password: "${ALFRESCO_PASSWORD}"
      
      # Repository configuration
      repository-id: "-default-"
      root-folder-path: "/Company Home/ECM Documents"
      
      # Connection settings
      connection-timeout: 30000
      read-timeout: 60000
      max-connections: 50
      
      # Features
      use-cmis: true
      enable-versioning: true
      enable-aspects: true
      
    features:
      document-management: true
      content-storage: true
      versioning: true
      folder-management: true
      permissions: true
      search: true
      auditing: true
      metadata-extraction: true
      workflow: true

# Alfresco-specific logging
logging:
  level:
    com.firefly.core.ecm: INFO
    org.alfresco: DEBUG
    org.apache.chemistry.opencmis: WARN
```

## 4. Implementation Example

### Alfresco Document Service

```java
@Service
public class AlfrescoDocumentService {
    
    @Autowired
    private DocumentPort documentPort;
    
    @Autowired
    private DocumentContentPort contentPort;
    
    @Autowired
    private FolderPort folderPort;
    
    public Mono<Document> uploadDocumentToFolder(
            String folderPath, 
            String fileName, 
            byte[] content, 
            String mimeType,
            Map<String, Object> metadata) {
        
        // Ensure folder exists
        return folderPort.createFolderPath(folderPath)
            .flatMap(folder -> {
                Document document = Document.builder()
                    .name(fileName)
                    .mimeType(mimeType)
                    .size((long) content.length)
                    .status(DocumentStatus.ACTIVE)
                    .folderId(folder.getId())
                    .metadata(metadata)
                    .createdAt(Instant.now())
                    .build();
                
                return documentPort.createDocument(document, content);
            });
    }
    
    public Mono<Document> addAspectToDocument(UUID documentId, String aspectName, Map<String, Object> properties) {
        // Alfresco-specific: Add aspect with properties
        return documentPort.getDocument(documentId)
            .flatMap(document -> {
                Map<String, Object> updatedMetadata = new HashMap<>(document.getMetadata());
                updatedMetadata.put("aspects", Arrays.asList(aspectName));
                updatedMetadata.putAll(properties);
                
                Document updatedDocument = document.toBuilder()
                    .metadata(updatedMetadata)
                    .modifiedAt(Instant.now())
                    .build();
                
                return documentPort.updateDocument(updatedDocument);
            });
    }
    
    public Mono<Document> createDocumentVersion(UUID documentId, byte[] content, String versionComment) {
        return documentPort.getDocument(documentId)
            .flatMap(document -> {
                DocumentVersion version = DocumentVersion.builder()
                    .documentId(documentId)
                    .versionNumber(getNextVersionNumber(document))
                    .versionComment(versionComment)
                    .size((long) content.length)
                    .createdAt(Instant.now())
                    .createdBy(getCurrentUserId())
                    .build();
                
                return documentPort.createVersion(version, content);
            })
            .map(version -> version.getDocument());
    }
    
    public Flux<Document> searchDocuments(String query, String folderPath) {
        DocumentSearchCriteria criteria = DocumentSearchCriteria.builder()
            .query(query)
            .folderPath(folderPath)
            .includeSubfolders(true)
            .maxResults(100)
            .build();
        
        return documentPort.searchDocuments(criteria);
    }
    
    public Mono<Void> applyRetentionPolicy(UUID documentId, String policyName, Instant retentionDate) {
        Map<String, Object> retentionProperties = Map.of(
            "rma:retentionPolicy", policyName,
            "rma:retentionDate", retentionDate.toString(),
            "rma:recordCategory", "Business Document"
        );
        
        return addAspectToDocument(documentId, "rma:record", retentionProperties)
            .then();
    }
    
    private String getNextVersionNumber(Document document) {
        // Simple version numbering logic
        String currentVersion = document.getVersionNumber();
        if (currentVersion == null) {
            return "1.0";
        }
        
        String[] parts = currentVersion.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        
        return major + "." + (minor + 1);
    }
    
    private Long getCurrentUserId() {
        // Get current user from security context
        return 1L; // Simplified for example
    }
}
```

### Alfresco-Specific Controller

```java
@RestController
@RequestMapping("/api/alfresco")
public class AlfrescoController {
    
    @Autowired
    private AlfrescoDocumentService alfrescoService;
    
    @PostMapping("/folders/{folderPath}/documents")
    public Mono<ResponseEntity<Document>> uploadToFolder(
            @PathVariable String folderPath,
            @RequestPart("file") FilePart file,
            @RequestParam(required = false) Map<String, String> metadata) {
        
        return file.content()
            .collectList()
            .map(this::convertToByteArray)
            .flatMap(content -> alfrescoService.uploadDocumentToFolder(
                folderPath,
                file.filename(),
                content,
                file.headers().getContentType().toString(),
                new HashMap<>(metadata != null ? metadata : Collections.emptyMap())
            ))
            .map(document -> ResponseEntity.status(HttpStatus.CREATED).body(document))
            .onErrorReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }
    
    @PostMapping("/documents/{documentId}/aspects/{aspectName}")
    public Mono<ResponseEntity<Document>> addAspect(
            @PathVariable UUID documentId,
            @PathVariable String aspectName,
            @RequestBody Map<String, Object> properties) {
        
        return alfrescoService.addAspectToDocument(documentId, aspectName, properties)
            .map(document -> ResponseEntity.ok(document))
            .onErrorReturn(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/documents/{documentId}/versions")
    public Mono<ResponseEntity<Document>> createVersion(
            @PathVariable UUID documentId,
            @RequestPart("file") FilePart file,
            @RequestParam String versionComment) {
        
        return file.content()
            .collectList()
            .map(this::convertToByteArray)
            .flatMap(content -> alfrescoService.createDocumentVersion(documentId, content, versionComment))
            .map(document -> ResponseEntity.status(HttpStatus.CREATED).body(document))
            .onErrorReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }
    
    @GetMapping("/search")
    public Flux<Document> searchDocuments(
            @RequestParam String query,
            @RequestParam(required = false) String folderPath) {
        
        return alfrescoService.searchDocuments(query, folderPath);
    }
    
    @PostMapping("/documents/{documentId}/retention")
    public Mono<ResponseEntity<Void>> applyRetentionPolicy(
            @PathVariable UUID documentId,
            @RequestParam String policyName,
            @RequestParam String retentionDate) {
        
        Instant retention = Instant.parse(retentionDate);
        
        return alfrescoService.applyRetentionPolicy(documentId, policyName, retention)
            .then(Mono.just(ResponseEntity.ok().<Void>build()))
            .onErrorReturn(ResponseEntity.notFound().build());
    }
    
    private byte[] convertToByteArray(List<DataBuffer> dataBuffers) {
        int totalSize = dataBuffers.stream()
            .mapToInt(DataBuffer::readableByteCount)
            .sum();
        
        byte[] content = new byte[totalSize];
        int position = 0;
        for (DataBuffer buffer : dataBuffers) {
            int bufferSize = buffer.readableByteCount();
            buffer.read(content, position, bufferSize);
            position += bufferSize;
        }
        
        return content;
    }
}
```

## 5. Advanced Features

### Content Models and Aspects

```java
// Define custom content model
@Component
public class CustomContentModel {
    
    public static final String NAMESPACE = "http://company.com/model/content/1.0";
    public static final String PREFIX = "cc";
    
    // Custom document type
    public static final String TYPE_CONTRACT = PREFIX + ":contract";
    
    // Custom aspects
    public static final String ASPECT_CONFIDENTIAL = PREFIX + ":confidential";
    public static final String ASPECT_FINANCIAL = PREFIX + ":financial";
    
    // Custom properties
    public static final String PROP_CONTRACT_VALUE = PREFIX + ":contractValue";
    public static final String PROP_EXPIRY_DATE = PREFIX + ":expiryDate";
    public static final String PROP_CLASSIFICATION = PREFIX + ":classification";
}
```

### Workflow Integration

```java
@Service
public class AlfrescoWorkflowService {
    
    @Autowired
    private WorkflowPort workflowPort;
    
    public Mono<WorkflowInstance> startApprovalWorkflow(UUID documentId, List<String> approvers) {
        Map<String, Object> variables = Map.of(
            "bpm_assignees", approvers,
            "bpm_workflowDescription", "Document approval workflow",
            "firefly_documentId", documentId.toString()
        );
        
        WorkflowDefinition definition = WorkflowDefinition.builder()
            .key("activiti$documentApproval")
            .name("Document Approval")
            .variables(variables)
            .build();
        
        return workflowPort.startWorkflow(definition);
    }
}
```

## 6. Testing

### Integration Test

```java
@SpringBootTest
@TestPropertySource(properties = {
    "firefly.ecm.adapter-type=alfresco",
    "firefly.ecm.properties.server-url=http://localhost:8080/alfresco",
    "firefly.ecm.properties.username=admin",
    "firefly.ecm.properties.password=admin"
})
class AlfrescoIntegrationTest {
    
    @Autowired
    private AlfrescoDocumentService alfrescoService;
    
    @Test
    void testDocumentUploadWithAspects() {
        Map<String, Object> metadata = Map.of(
            "title", "Test Contract",
            "description", "Integration test contract"
        );
        
        StepVerifier.create(
            alfrescoService.uploadDocumentToFolder(
                "/Company Home/Contracts",
                "test-contract.pdf",
                "Test contract content".getBytes(),
                "application/pdf",
                metadata
            )
        )
        .assertNext(document -> {
            assertThat(document.getId()).isNotNull();
            assertThat(document.getName()).isEqualTo("test-contract.pdf");
            assertThat(document.getMetadata()).containsKey("title");
        })
        .verifyComplete();
    }
}
```

## 7. Production Considerations

### Security
- Use HTTPS for all communications
- Configure proper authentication (LDAP, SSO)
- Set up role-based permissions
- Enable audit logging

### Performance
- Configure connection pooling
- Use CMIS for bulk operations
- Implement caching strategies
- Monitor repository performance

### High Availability
- Set up Alfresco clustering
- Configure load balancing
- Implement backup strategies
- Monitor system health

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Connection refused | Check Alfresco server status and URL |
| Authentication failed | Verify username/password and permissions |
| CMIS errors | Check CMIS endpoint configuration |
| Slow performance | Review query optimization and indexing |

## Next Steps

- [Configure custom content models](../examples/alfresco-content-models.md)
- [Set up workflow integration](../examples/alfresco-workflows.md)
- [Implement records management](../examples/alfresco-records-management.md)
- [Configure search and indexing](../examples/alfresco-search.md)
