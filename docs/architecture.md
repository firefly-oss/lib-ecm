# Firefly ECM Architecture Guide

This guide explains the architecture and design principles of the Firefly ECM Library.

## Overview

The Firefly ECM Library implements **Hexagonal Architecture** (also known as Ports and Adapters pattern) with a clear separation between:

1. **Port Interface Library** (this library): Defines business contracts and domain models
2. **Adapter Implementation Libraries** (separate repositories): Provide concrete implementations for specific technologies

This architecture enables:

- **Vendor Independence**: Switch between storage providers without changing business logic
- **Testability**: Mock external dependencies for unit testing
- **Maintainability**: Clear separation of concerns
- **Extensibility**: Add new adapters without modifying the core library
- **Modularity**: Include only the adapters you need

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    YOUR APPLICATION                             │
│                                                                 │
│  Uses port interfaces to interact with ECM functionality       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Depends on
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              LIB-ECM (Port Interface Library)                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    DOMAIN MODELS                          │  │
│  │  • Document      • Folder        • Permission             │  │
│  │  • AuditEvent    • SignatureEnvelope                      │  │
│  │  • DocumentVersion • FolderPermission                     │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    PORT INTERFACES                        │  │
│  │  • DocumentPort           • PermissionPort                │  │
│  │  • DocumentContentPort    • AuditPort                     │  │
│  │  • SignatureEnvelopePort  • FolderPort                    │  │
│  │  • DocumentVersionPort    • SearchPort                    │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              ADAPTER INFRASTRUCTURE                       │  │
│  │  • EcmPortProvider        • AdapterSelector               │  │
│  │  • AdapterRegistry        • Auto-Configuration            │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Implemented by (separate libraries)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ADAPTER LIBRARIES                            │
│                    (Separate Repositories)                      │
│                                                                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │ lib-ecm-adapter- │  │ lib-ecm-adapter- │  │ lib-ecm-     │  │
│  │       s3         │  │   docusign       │  │ adapter-     │  │
│  │                  │  │                  │  │ azure-blob   │  │
│  │ Implements:      │  │ Implements:      │  │              │  │
│  │ • DocumentPort   │  │ • SignatureEnv.. │  │ Implements:  │  │
│  │ • ContentPort    │  │ • SignatureReq.. │  │ • DocumentP..│  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
│                                                                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │ lib-ecm-adapter- │  │ lib-ecm-adapter- │  │ lib-ecm-     │  │
│  │   adobe-sign     │  │   alfresco       │  │ adapter-     │  │
│  │                  │  │                  │  │ aws-textract │  │
│  │ (Planned)        │  │ (Planned)        │  │ (Planned)    │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Core Concepts

### Hexagonal Architecture with Library Separation

The Firefly ECM implementation separates hexagonal architecture across multiple libraries:

#### 1. Port Interface Library (lib-ecm)

This library contains:

- **Domain Layer**: Business entities and rules (Document, Folder, SignatureEnvelope, etc.)
- **Port Layer**: Interface contracts for external interactions
- **Adapter Infrastructure**: Framework for discovering and selecting adapters

**Key characteristics:**
- No concrete adapter implementations (except no-op fallbacks)
- No dependencies on external provider SDKs (AWS, Azure, DocuSign, etc.)
- Provides graceful degradation with no-op adapters
- Stable API that rarely changes

#### 2. Adapter Implementation Libraries (separate repositories)

Each adapter library:

- **Depends on lib-ecm** for port interfaces and domain models
- **Implements specific port interfaces** for a particular technology
- **Includes provider SDK dependencies** (e.g., AWS SDK, DocuSign SDK)
- **Registers itself** via Spring Boot auto-configuration

**Key characteristics:**
- Can be updated independently of the core library
- Applications choose which adapters to include
- Multiple adapters can coexist (e.g., S3 + DocuSign)
- Adapter-specific configuration properties

## Core Components in lib-ecm

### Domain Models

Located in `com.firefly.core.ecm.domain.model`, these represent the core business entities:

#### Document Management
- **Document**: Core document entity with metadata
- **DocumentVersion**: Document version information
- **Folder**: Folder/directory structure
- **FolderPermission**: Folder-level permissions

#### Security & Audit
- **Permission**: Access control permissions
- **AuditEvent**: Audit trail entries

#### Digital Signatures
- **SignatureEnvelope**: Container for signature workflows
- **SignatureRequest**: Individual signature requests
- **SignatureField**: Signature field definitions

### Ports (Interfaces)

Located in `com.firefly.core.ecm.port`, these define the business interfaces:

#### Document Ports
- **DocumentPort**: Document CRUD operations
- **DocumentContentPort**: Binary content operations
- **DocumentVersionPort**: Version management
- **DocumentSearchPort**: Search capabilities

#### Folder Ports
- **FolderPort**: Folder management
- **FolderHierarchyPort**: Hierarchical operations

#### Security Ports
- **PermissionPort**: Access control
- **DocumentSecurityPort**: Document security operations

#### Audit Ports
- **AuditPort**: Audit logging

#### eSignature Ports
- **SignatureEnvelopePort**: Envelope management
- **SignatureRequestPort**: Signature requests
- **SignatureValidationPort**: Signature validation
- **SignatureProofPort**: Signature proof and evidence


### Adapter Infrastructure

Located in `com.firefly.core.ecm.adapter`, this provides the framework for adapter discovery and selection:

- **AdapterRegistry**: Maintains registry of available adapters
- **AdapterSelector**: Selects appropriate adapter based on configuration
- **EcmAdapter**: Base interface for all adapters
- **AdapterInfo**: Metadata about adapter capabilities
- **AdapterProfile**: Adapter configuration profiles

## Adapter Implementation Libraries (Separate Repositories)

Adapter implementations are provided in separate libraries that depend on lib-ecm:

### Document Storage Adapters

| Adapter | Library | Status | Implements |
|---------|---------|--------|------------|
| **Amazon S3** | `lib-ecm-adapter-s3` | ✅ Available | DocumentPort, DocumentContentPort |
| **Azure Blob** | `lib-ecm-adapter-azure-blob` | ✅ Available | DocumentPort, DocumentContentPort |
| **MinIO** | `lib-ecm-adapter-minio` | 🔜 Planned | DocumentPort, DocumentContentPort |
| **Alfresco** | `lib-ecm-adapter-alfresco` | 🔜 Planned | DocumentPort, FolderPort, PermissionPort |

### eSignature Adapters

| Adapter | Library | Status | Implements |
|---------|---------|--------|------------|
| **DocuSign** | `lib-ecm-adapter-docusign` | ✅ Available | SignatureEnvelopePort, SignatureRequestPort |
| **Adobe Sign** | `lib-ecm-adapter-adobe-sign` | ✅ Available | SignatureEnvelopePort, SignatureValidationPort |
| **Logalty** | `lib-ecm-adapter-logalty` | 🔜 Planned | SignatureEnvelopePort (eIDAS-compliant) |

### IDP Adapters

| Adapter | Library | Status | Implements |
|---------|---------|--------|------------|
| **AWS Textract** | `lib-ecm-adapter-aws-textract` | 🔜 Planned | DocumentExtractionPort, DataExtractionPort |
| **Azure Form Recognizer** | `lib-ecm-adapter-azure-form-recognizer` | 🔜 Planned | DocumentExtractionPort, DataExtractionPort |
| **Google Document AI** | `lib-ecm-adapter-google-document-ai` | 🔜 Planned | DocumentExtractionPort, DataExtractionPort |

## How Adapters Work

### Adapter Discovery and Registration

When you add an adapter library to your application:

1. **Dependency Resolution**: Maven/Gradle includes the adapter JAR in your classpath
2. **Auto-Configuration**: Spring Boot discovers the adapter's auto-configuration class
3. **Bean Registration**: The adapter registers its implementation beans
4. **Adapter Registry**: The adapter registers itself with the `AdapterRegistry`

Example from an adapter library (e.g., `lib-ecm-adapter-s3`):

```java
// In the adapter library (separate repository)
@EcmAdapter(
    type = "s3",
    description = "Amazon S3 Document Storage Adapter",
    supportedFeatures = {
        AdapterFeature.DOCUMENT_CRUD,
        AdapterFeature.CONTENT_STORAGE,
        AdapterFeature.STREAMING,
        AdapterFeature.VERSIONING
    },
    requiredProperties = {"bucket-name", "region"},
    optionalProperties = {"access-key", "secret-key", "endpoint"}
)
@Component
@ConditionalOnProperty(name = "firefly.ecm.adapter-type", havingValue = "s3")
public class S3DocumentAdapter implements DocumentPort, DocumentContentPort {
    // Implementation using AWS SDK
}
```

### Adapter Selection

The `AdapterSelector` (provided by lib-ecm) chooses the appropriate adapter based on configuration:

1. **Type Matching**: Matches `firefly.ecm.adapter-type` with adapter type
2. **Feature Validation**: Ensures adapter supports required features
3. **Configuration Validation**: Validates required properties are present
4. **Priority Resolution**: Selects highest priority adapter if multiple match

### Dependency Flow

```
Your Application
    │
    ├─ depends on ──> lib-ecm (port interfaces)
    │
    ├─ depends on ──> lib-ecm-adapter-s3
    │                     │
    │                     └─ depends on ──> lib-ecm
    │                     └─ depends on ──> AWS SDK
    │
    └─ depends on ──> lib-ecm-adapter-docusign
                          │
                          └─ depends on ──> lib-ecm
                          └─ depends on ──> DocuSign SDK
```

### Adapter Features

Adapters declare their capabilities using `AdapterFeature` enum:

- `DOCUMENT_CRUD`: Basic document operations
- `CONTENT_STORAGE`: Binary content storage
- `STREAMING`: Streaming content support
- `VERSIONING`: Document versioning
- `FOLDER_MANAGEMENT`: Folder operations
- `PERMISSIONS`: Access control
- `SEARCH`: Search capabilities
- `AUDIT`: Audit logging
- `ESIGNATURE_ENVELOPES`: Signature envelopes
- `ESIGNATURE_REQUESTS`: Signature requests
- `SIGNATURE_VALIDATION`: Signature validation

## Configuration System

### Configuration in lib-ecm

The core library provides the configuration infrastructure:

```java
@ConfigurationProperties(prefix = "firefly.ecm")
public class EcmProperties {
    private Boolean enabled = true;
    private String adapterType;  // Selects which adapter to use
    private ESignatureProperties esignature = new ESignatureProperties();
    private Map<String, Object> adapter = new HashMap<>();  // Adapter-specific config
}
```

### Adapter-Specific Configuration

Each adapter library defines its own configuration properties. For example:

**S3 Adapter Configuration** (in `lib-ecm-adapter-s3`):
```yaml
firefly:
  ecm:
    adapter-type: s3
    adapter:
      s3:
        bucket-name: my-bucket
        region: us-east-1
```

**DocuSign Adapter Configuration** (in `lib-ecm-adapter-docusign`):
```yaml
firefly:
  ecm:
    esignature:
      provider: docusign
    adapter:
      docusign:
        integration-key: ${DOCUSIGN_KEY}
        user-id: ${DOCUSIGN_USER}
```

### Auto-Configuration

The `EcmAutoConfiguration` class (in lib-ecm) automatically configures the ECM system:

1. **Property Binding**: Binds core configuration properties
2. **Adapter Registry Setup**: Creates the adapter registry
3. **Port Provider Setup**: Configures the port provider
4. **No-op Adapter Registration**: Registers fallback adapters

Each adapter library provides its own auto-configuration that:
1. **Registers adapter beans** when conditions are met
2. **Binds adapter-specific properties**
3. **Validates adapter configuration**

## Service Layer

### EcmPortProvider

Central service that provides access to ports:

```java
@Service
public class EcmPortProvider {
    
    public <T> T getPort(Class<T> portType) {
        return adapterSelector.getAdapter(portType);
    }
    
    public boolean isFeatureEnabled(String feature) {
        return ecmProperties.getFeatures().isEnabled(feature);
    }
}
```

### AdapterRegistry

Maintains registry of available adapters:

```java
@Component
public class AdapterRegistry {
    
    public void registerAdapter(AdapterInfo adapterInfo) {
        adapters.put(adapterInfo.getType(), adapterInfo);
    }
    
    public AdapterInfo getAdapter(String type) {
        return adapters.get(type);
    }
    
    public Set<AdapterInfo> getAdaptersByFeature(AdapterFeature feature) {
        return adapters.values().stream()
            .filter(adapter -> adapter.getSupportedFeatures().contains(feature))
            .collect(Collectors.toSet());
    }
}
```

## Reactive Programming

The library uses Project Reactor for reactive programming:

### Benefits
- **Non-blocking I/O**: Efficient resource utilization
- **Backpressure Handling**: Automatic flow control
- **Composable Operations**: Chain operations declaratively
- **Error Handling**: Comprehensive error handling strategies

### Usage Patterns

```java
// Reactive document upload
public Mono<Document> uploadDocument(String name, byte[] content) {
    return Mono.fromCallable(() -> createDocument(name, content))
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(doc -> log.info("Document uploaded: {}", doc.getId()))
        .doOnError(error -> log.error("Upload failed", error));
}

// Reactive content streaming
public Flux<DataBuffer> streamContent(UUID documentId) {
    return contentPort.getContentStream(documentId)
        .doOnSubscribe(sub -> log.info("Starting stream: {}", documentId))
        .doOnComplete(() -> log.info("Stream completed: {}", documentId));
}
```

## Error Handling

### Exception Hierarchy

- **EcmException**: Base exception for all ECM operations
- **DocumentNotFoundException**: Document not found
- **AdapterException**: Adapter-specific errors
- **ConfigurationException**: Configuration errors
- **SecurityException**: Security-related errors

### Error Handling Strategies

1. **Graceful Degradation**: Continue operation with reduced functionality
2. **Retry Logic**: Automatic retry for transient failures
3. **Circuit Breaker**: Prevent cascading failures
4. **Fallback Mechanisms**: Alternative processing paths

## Testing Strategy

### Unit Testing

- **Mock Adapters**: Test business logic without external dependencies
- **Port Testing**: Test port implementations independently
- **Service Testing**: Test service layer with mocked ports

### Integration Testing

- **Adapter Testing**: Test adapters with real external systems
- **End-to-End Testing**: Test complete workflows
- **Configuration Testing**: Test different configuration scenarios

## Security Considerations

### Authentication & Authorization

- **Adapter-level Security**: Each adapter handles its own authentication
- **Permission System**: Fine-grained access control
- **Audit Logging**: Complete audit trail for compliance

### Data Protection

- **Encryption at Rest**: Adapter-specific encryption
- **Encryption in Transit**: HTTPS/TLS for all communications
- **Data Masking**: Sensitive data protection in logs

## Performance Optimization

### Caching Strategy

- **Metadata Caching**: Cache document metadata
- **Content Caching**: Cache frequently accessed content
- **Configuration Caching**: Cache adapter configurations

### Connection Management

- **Connection Pooling**: Efficient connection reuse
- **Timeout Configuration**: Prevent hanging operations
- **Retry Logic**: Handle transient failures

## Monitoring & Observability

### Metrics

- **Operation Metrics**: Document operations per second
- **Performance Metrics**: Response times and throughput
- **Error Metrics**: Error rates and types

### Logging

- **Structured Logging**: JSON-formatted logs
- **Correlation IDs**: Track operations across components
- **Audit Logging**: Compliance and security auditing

## Extension Points

### Custom Adapters

Implement custom adapters by:

1. Implementing required port interfaces
2. Adding `@EcmAdapter` annotation
3. Registering as Spring component
4. Providing configuration properties

### Custom Features

Add custom features by:

1. Defining new port interfaces
2. Implementing in adapters
3. Adding feature flags
4. Updating configuration

## Best Practices

### Adapter Development

- **Implement Required Interfaces**: Implement all required ports
- **Handle Errors Gracefully**: Provide meaningful error messages
- **Support Configuration**: Use configuration properties
- **Add Comprehensive Tests**: Unit and integration tests

### Application Integration

- **Use Port Interfaces**: Don't depend on adapter implementations
- **Handle Reactive Streams**: Use proper reactive patterns
- **Configure Properly**: Validate configuration on startup
- **Monitor Operations**: Add metrics and logging

## Next Steps

- [Configuration Guide](configuration.md)
- [Integration Guides](guides/)
- [API Reference](api/)
- [Examples](examples/)
