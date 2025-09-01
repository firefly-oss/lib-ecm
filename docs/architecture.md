# Firefly ECM Architecture Guide

This guide explains the architecture and design principles of the Firefly ECM Library.

## Overview

The Firefly ECM Library implements **Hexagonal Architecture** (also known as Ports and Adapters pattern) to provide a clean separation between business logic and external systems. This architecture enables:

- **Vendor Independence**: Switch between storage providers without changing business logic
- **Testability**: Mock external dependencies for unit testing
- **Maintainability**: Clear separation of concerns
- **Extensibility**: Add new adapters without modifying existing code

## Architecture Diagram

```
┌───────────────────────────────────────────────────────────┐
│                    APPLICATION CORE                       │
│  ┌─────────────────────────────────────────────────────┐  │
│  │                DOMAIN MODELS                        │  │
│  │  • Document      • Folder        • Permission       │  │
│  │  • AuditEvent    • SignatureEnvelope                │  │
│  │  • DocumentVersion • FolderPermission               │  │
│  └─────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────┐  │
│  │                    PORTS                            │  │
│  │  • DocumentPort           • PermissionPort          │  │
│  │  • DocumentContentPort    • AuditPort               │  │
│  │  • SignatureEnvelopePort  • FolderPort              │  │
│  │  • DocumentVersionPort    • SearchPort              │  │
│  └─────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────┐  │
│  │                   SERVICES                          │  │
│  │  • EcmPortProvider        • AdapterSelector         │  │
│  │  • AdapterRegistry        • ConfigurationValidator  │  │
│  └─────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────┘
                              │
                              │ Dependency Inversion
                              ▼
┌───────────────────────────────────────────────────────────┐
│                       ADAPTERS                            │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐  │
│  │   AWS S3    │     │   DocuSign  │     │  Alfresco   │  │
│  │   Adapter   │     │   Adapter   │     │   Adapter   │  │
│  └─────────────┘     └─────────────┘     └─────────────┘  │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐  │
│  │ Azure Blob  │     │ Adobe Sign  │     │    MinIO    │  │
│  │   Adapter   │     │   Adapter   │     │   Adapter   │  │
│  └─────────────┘     └─────────────┘     └─────────────┘  │
└───────────────────────────────────────────────────────────┘
```

## Core Components

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
- **FolderPermissionPort**: Folder permissions

#### Security Ports
- **PermissionPort**: Access control
- **SecurityPort**: Security operations

#### Audit Ports
- **AuditPort**: Audit logging
- **CompliancePort**: Compliance operations

#### eSignature Ports
- **SignatureEnvelopePort**: Envelope management
- **SignatureRequestPort**: Signature requests
- **SignatureValidationPort**: Signature validation

### Adapters

Located in `com.firefly.core.ecm.adapter`, these implement the ports for specific technologies:

#### Storage Adapters
- **S3Adapter**: Amazon S3 implementation
- **AlfrescoAdapter**: Alfresco Content Services
- **AzureBlobAdapter**: Azure Blob Storage (planned)
- **MinIOAdapter**: MinIO Object Storage (planned)

#### eSignature Adapters
- **DocuSignAdapter**: DocuSign integration
- **AdobeSignAdapter**: Adobe Sign integration (planned)

## Adapter System

### Adapter Registration

Adapters are automatically discovered and registered using Spring's component scanning:

```java
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
public class S3Adapter implements DocumentPort, DocumentContentPort {
    // Implementation
}
```

### Adapter Selection

The `AdapterSelector` chooses the appropriate adapter based on configuration:

1. **Type Matching**: Matches `firefly.ecm.adapter-type` with adapter type
2. **Feature Validation**: Ensures adapter supports required features
3. **Configuration Validation**: Validates required properties are present
4. **Priority Resolution**: Selects highest priority adapter if multiple match

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

### Configuration Properties

The `EcmProperties` class defines the configuration structure:

```java
@ConfigurationProperties(prefix = "firefly.ecm")
public class EcmProperties {
    private Boolean enabled = true;
    private String adapterType;
    private Map<String, Object> properties;
    private Connection connection = new Connection();
    private Features features = new Features();
    private Defaults defaults = new Defaults();
    private Performance performance = new Performance();
}
```

### Auto-Configuration

The `EcmAutoConfiguration` class automatically configures the ECM system:

1. **Property Binding**: Binds configuration properties
2. **Adapter Discovery**: Scans for adapter components
3. **Port Provider Setup**: Configures port provider
4. **Feature Validation**: Validates feature compatibility

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
