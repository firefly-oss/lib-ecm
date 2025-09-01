# Firefly ECM API Reference

This directory contains comprehensive API documentation for the Firefly ECM Library, including all port interfaces, domain models, and configuration options.

## Table of Contents

1. [Port Interfaces](#port-interfaces)
2. [Domain Models](#domain-models)
3. [Configuration](#configuration)
4. [Adapters](#adapters)
5. [Services](#services)

## Port Interfaces

The Firefly ECM Library defines business interfaces (ports) that adapters implement:

### Document Management Ports

- **[DocumentPort](document-port.md)** - Core document CRUD operations
- **[DocumentContentPort](document-content-port.md)** - Binary content operations
- **[DocumentVersionPort](document-version-port.md)** - Document versioning
- **[DocumentSearchPort](document-search-port.md)** - Document search capabilities

### Folder Management Ports

- **[FolderPort](folder-port.md)** - Folder CRUD operations
- **[FolderHierarchyPort](folder-hierarchy-port.md)** - Hierarchical folder operations
- **[FolderPermissionPort](folder-permission-port.md)** - Folder-level permissions

### Security Ports

- **[PermissionPort](permission-port.md)** - Access control and permissions
- **[SecurityPort](security-port.md)** - Security operations

### Audit Ports

- **[AuditPort](audit-port.md)** - Audit logging and compliance
- **[CompliancePort](compliance-port.md)** - Compliance operations

### eSignature Ports

- **[SignatureEnvelopePort](signature-envelope-port.md)** - Envelope management
- **[SignatureRequestPort](signature-request-port.md)** - Signature requests
- **[SignatureValidationPort](signature-validation-port.md)** - Signature validation

## Domain Models

Core business entities and value objects:

### Document Domain

- **[Document](models/document.md)** - Core document entity
- **[DocumentVersion](models/document-version.md)** - Document version information
- **[DocumentStatus](models/document-status.md)** - Document status enumeration

### Folder Domain

- **[Folder](models/folder.md)** - Folder entity
- **[FolderPermission](models/folder-permission.md)** - Folder permissions

### Security Domain

- **[Permission](models/permission.md)** - Access control permissions
- **[AuditEvent](models/audit-event.md)** - Audit trail entries

### eSignature Domain

- **[SignatureEnvelope](models/signature-envelope.md)** - Signature envelope container
- **[SignatureRequest](models/signature-request.md)** - Individual signature requests
- **[SignatureField](models/signature-field.md)** - Signature field definitions

## Configuration

Configuration classes and properties:

- **[EcmProperties](configuration/ecm-properties.md)** - Main configuration class
- **[AdapterConfiguration](configuration/adapter-configuration.md)** - Adapter-specific configuration
- **[FeatureConfiguration](configuration/feature-configuration.md)** - Feature flags and settings

## Adapters

Adapter implementations and interfaces:

- **[Adapter Interface](adapters/adapter-interface.md)** - Base adapter interface
- **[Adapter Features](adapters/adapter-features.md)** - Supported feature enumeration
- **[Adapter Registry](adapters/adapter-registry.md)** - Adapter discovery and registration

## Services

Core service classes:

- **[EcmPortProvider](services/ecm-port-provider.md)** - Central port provider service
- **[AdapterSelector](services/adapter-selector.md)** - Adapter selection logic
- **[ConfigurationValidator](services/configuration-validator.md)** - Configuration validation

## Usage Examples

### Basic Document Operations

```java
// Get document port
DocumentPort documentPort = ecmPortProvider.getPort(DocumentPort.class);

// Create document
Document document = Document.builder()
    .name("example.pdf")
    .mimeType("application/pdf")
    .size(1024L)
    .status(DocumentStatus.ACTIVE)
    .build();

Mono<Document> created = documentPort.createDocument(document, content);

// Retrieve document
Mono<Document> retrieved = documentPort.getDocument(documentId);

// Update document
Mono<Document> updated = documentPort.updateDocument(document);

// Delete document
Mono<Void> deleted = documentPort.deleteDocument(documentId);
```

### Content Operations

```java
// Get content port
DocumentContentPort contentPort = ecmPortProvider.getPort(DocumentContentPort.class);

// Stream content
Flux<DataBuffer> contentStream = contentPort.getContentStream(documentId);

// Get content as bytes
Mono<byte[]> content = contentPort.getContent(documentId);

// Get content range
Flux<DataBuffer> range = contentPort.getContentRange(documentId, 0, 1023);
```

### Folder Operations

```java
// Get folder port
FolderPort folderPort = ecmPortProvider.getPort(FolderPort.class);

// Create folder
Folder folder = Folder.builder()
    .name("Documents")
    .path("/Documents")
    .build();

Mono<Folder> created = folderPort.createFolder(folder);

// List folder contents
Flux<Document> contents = folderPort.getFolderContents(folderId);
```

### eSignature Operations

```java
// Get signature envelope port
SignatureEnvelopePort envelopePort = ecmPortProvider.getPort(SignatureEnvelopePort.class);

// Create signature envelope
SignatureEnvelope envelope = SignatureEnvelope.builder()
    .title("Contract Signature")
    .description("Please sign this contract")
    .documentIds(Arrays.asList(documentId))
    .signatureRequests(signatureRequests)
    .build();

Mono<SignatureEnvelope> created = envelopePort.createEnvelope(envelope);

// Send envelope for signature
Mono<SignatureEnvelope> sent = envelopePort.sendEnvelope(envelopeId, sentBy);

// Get signing URL
Mono<String> signingUrl = envelopePort.getSigningUrl(envelopeId, signerEmail);
```

## Error Handling

All port methods return reactive types (`Mono` or `Flux`) and follow these error handling patterns:

### Common Exceptions

- **`DocumentNotFoundException`** - Document not found
- **`FolderNotFoundException`** - Folder not found
- **`PermissionDeniedException`** - Access denied
- **`AdapterException`** - Adapter-specific errors
- **`ConfigurationException`** - Configuration errors

### Error Handling Example

```java
documentPort.getDocument(documentId)
    .doOnError(DocumentNotFoundException.class, error -> 
        log.warn("Document not found: {}", documentId))
    .onErrorReturn(DocumentNotFoundException.class, null)
    .doOnError(PermissionDeniedException.class, error -> 
        log.error("Access denied for document: {}", documentId))
    .onErrorResume(PermissionDeniedException.class, error -> 
        Mono.error(new SecurityException("Access denied")))
    .subscribe();
```

## Reactive Programming

The Firefly ECM Library uses Project Reactor for reactive programming:

### Key Concepts

- **`Mono<T>`** - Represents 0 or 1 element
- **`Flux<T>`** - Represents 0 to N elements
- **Backpressure** - Automatic flow control
- **Non-blocking** - Efficient resource utilization

### Best Practices

1. **Chain Operations**: Use reactive operators to chain operations
2. **Error Handling**: Handle errors at appropriate levels
3. **Backpressure**: Let Reactor handle backpressure automatically
4. **Testing**: Use `StepVerifier` for testing reactive streams
5. **Blocking**: Avoid blocking operations in reactive chains

## Versioning

The Firefly ECM Library follows semantic versioning:

- **Major Version**: Breaking changes to public APIs
- **Minor Version**: New features, backward compatible
- **Patch Version**: Bug fixes, backward compatible

## Support

For API questions and support:

- **Documentation**: [Firefly ECM Wiki](https://github.com/firefly-oss/lib-ecm/wiki)
- **Issues**: [GitHub Issues](https://github.com/firefly-oss/lib-ecm/issues)
- **Discussions**: [GitHub Discussions](https://github.com/firefly-oss/lib-ecm/discussions)

---

**Note**: This API reference is based on the actual Firefly ECM Library codebase. All interfaces, methods, and examples reflect the real implementation.
