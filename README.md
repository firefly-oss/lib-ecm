# Firefly ECM Library

> **Enterprise Content Management for the Modern Era**

A comprehensive, production-ready Enterprise Content Management (ECM) library built on hexagonal architecture principles. Designed for the Firefly OpenCore Platform, this library provides a unified interface for document management, digital signatures, and compliance across multiple storage backends and eSignature providers.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0+-green.svg)](https://spring.io/projects/spring-boot)

## 🎯 Purpose & Vision

The Firefly ECM Library solves the challenge of **vendor lock-in** and **integration complexity** in enterprise content management. Instead of being tied to a single ECM system or cloud provider, organizations can:

- **Switch between storage providers** (S3, Azure, MinIO, Alfresco) without changing business logic
- **Integrate multiple eSignature providers** (DocuSign, Adobe Sign) through a unified API
- **Scale horizontally** with cloud-native, reactive architecture
- **Maintain compliance** with built-in audit trails and security features
- **Future-proof applications** with a stable, vendor-agnostic interface

## 🏗️ Architecture

This library implements **Hexagonal Architecture** (Ports and Adapters pattern), providing clean separation between business logic and external systems.

**Key Benefits**: Pluggable adapters, testable design, scalable architecture, reactive programming

## 📦 Package Structure

The library follows **Domain-Driven Design** principles with clear package organization:

```
com.firefly.core.ecm/
├── domain/
│   ├── model/                   # Domain Entities
│   │   ├── document/            # Document, DocumentVersion
│   │   ├── folder/              # Folder, FolderPermissions
│   │   ├── security/            # Permission
│   │   ├── audit/               # AuditEvent
│   │   └── esignature/          # SignatureEnvelope, SignatureRequest
│   ├── enums/                   # Domain Enumerations
│   └── dto/                     # Data Transfer Objects
├── port/                        # Business Interfaces (Ports)
│   ├── document/                # Document management ports
│   ├── folder/                  # Folder management ports
│   ├── security/                # Security and permissions ports
│   ├── audit/                   # Audit and compliance ports
│   └── esignature/              # Digital signature ports
├── adapter/                     # Adapter Infrastructure
├── config/                      # Spring Boot Configuration
└── service/                     # Application Services
```

## 🚀 Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-ecm</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Basic Configuration

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: "s3"  # or "alfresco", "azure-blob", etc.
    properties:
      # Adapter-specific configuration
```

### 3. Use in Your Application

```java
@Service
public class DocumentService {
    
    @Autowired
    private DocumentPort documentPort;
    
    @Autowired
    private DocumentContentPort contentPort;
    
    public Mono<Document> uploadDocument(String name, byte[] content) {
        Document document = Document.builder()
            .name(name)
            .mimeType("application/pdf")
            .size((long) content.length)
            .build();
            
        return documentPort.createDocument(document, content);
    }
}
```

## 📋 Core Features

### Document Management
- **CRUD Operations**: Create, read, update, delete documents
- **Content Storage**: Binary content with streaming support
- **Version Control**: Complete document version history
- **Search & Query**: Full-text and metadata search capabilities

### Folder Management
- **Hierarchical Structure**: Nested folder organization
- **Path Management**: Full path resolution and navigation
- **Bulk Operations**: Move, copy, and organize multiple items

### Security & Permissions
- **Fine-grained Access Control**: Read, write, delete, share permissions
- **Role-based Security**: User and group-based permissions
- **Document Encryption**: Content encryption at rest
- **Legal Hold**: Compliance and retention management

### Digital Signatures
- **Multi-provider Support**: DocuSign, Adobe Sign, and more
- **Envelope Management**: Create, send, track signature workflows
- **Validation & Proof**: Signature verification and audit trails
- **Compliance**: eIDAS, ESIGN Act, and other regulatory standards

### Audit & Compliance
- **Complete Audit Trail**: Track all document and signature activities
- **Compliance Reporting**: Generate regulatory compliance reports
- **Data Retention**: Automated archival and purging policies
- **Security Monitoring**: Real-time security event tracking

## 🔧 Integration Guides

Detailed integration guides are available in the [docs/guides](docs/guides) directory:

- **[Amazon S3 Integration](docs/guides/s3-integration.md)** - Complete guide for S3 document storage
- **[DocuSign Integration](docs/guides/docusign-integration.md)** - Step-by-step DocuSign eSignature setup
- **[Alfresco Integration](docs/guides/alfresco-integration.md)** - Enterprise ECM with Alfresco
- **[Azure Blob Storage](docs/guides/azure-integration.md)** - Microsoft Azure cloud storage
- **[MinIO Integration](docs/guides/minio-integration.md)** - Self-hosted S3-compatible storage

## 📚 Documentation

- **[API Reference](docs/api/)** - Complete API documentation
- **[Configuration Guide](docs/configuration.md)** - All configuration options
- **[Examples](docs/examples/)** - Working code examples
- **[Architecture Guide](docs/architecture.md)** - Detailed architecture documentation
- **[Testing Guide](docs/testing.md)** - Testing strategies and examples

## 🤝 Contributing

We welcome contributions to the Firefly ECM Library! Please see our [Contributing Guide](CONTRIBUTING.md) for details on:

- Code style and conventions
- Testing requirements
- Pull request process
- Issue reporting

## 📄 License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

## 🆘 Support

- **Documentation**: [Firefly ECM Wiki](https://github.com/firefly-oss/lib-ecm/wiki)
- **Issues**: [GitHub Issues](https://github.com/firefly-oss/lib-ecm/issues)
- **Discussions**: [GitHub Discussions](https://github.com/firefly-oss/lib-ecm/discussions)
- **Enterprise Support**: Contact [support@getfirefly.io](mailto:support@firefly-oss.org)

---

**Built with ❤️ by the Firefly OpenCore Platform Team**
