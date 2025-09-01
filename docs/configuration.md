# Firefly ECM Configuration Guide

This guide covers all configuration options available in the Firefly ECM Library.

## Configuration Structure

The Firefly ECM Library uses Spring Boot's configuration properties system with the prefix `firefly.ecm`.

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: "s3"
    properties: {}
    connection: {}
    features: {}
    defaults: {}
    performance: {}
```

## Core Configuration

### Basic Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `firefly.ecm.enabled` | Boolean | `true` | Enable/disable ECM functionality |
| `firefly.ecm.adapter-type` | String | - | Adapter type to use (required) |
| `firefly.ecm.properties` | Map | `{}` | Adapter-specific configuration |

## Connection Configuration

Configure connection settings for adapters:

```yaml
firefly:
  ecm:
    connection:
      connect-timeout: "PT30S"      # Connection timeout (ISO-8601 duration)
      read-timeout: "PT5M"          # Read timeout (ISO-8601 duration)
      max-connections: 100          # Maximum concurrent connections
      retry-attempts: 3             # Number of retry attempts
```

### Connection Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `connect-timeout` | Duration | `PT30S` | Connection timeout |
| `read-timeout` | Duration | `PT5M` | Read timeout |
| `max-connections` | Integer | `100` | Maximum concurrent connections |
| `retry-attempts` | Integer | `3` | Number of retry attempts |

## Feature Configuration

Enable/disable specific ECM features:

```yaml
firefly:
  ecm:
    features:
      document-management: true     # Basic document CRUD operations
      content-storage: true         # Binary content storage
      versioning: true              # Document versioning
      folder-management: true       # Folder operations
      folder-hierarchy: true        # Hierarchical folder structure
      permissions: true             # Access control
      security: true                # Security features
      search: true                  # Document search
      auditing: true                # Audit logging
      esignature: false             # Digital signatures
      virus-scanning: false         # Virus scanning
      content-extraction: false     # Content extraction
```

### Feature Flags

| Feature | Default | Description |
|---------|---------|-------------|
| `document-management` | `true` | Basic document CRUD operations |
| `content-storage` | `true` | Binary content storage and retrieval |
| `versioning` | `true` | Document version management |
| `folder-management` | `true` | Folder creation and management |
| `folder-hierarchy` | `true` | Hierarchical folder structures |
| `permissions` | `true` | Access control and permissions |
| `security` | `true` | Security features and encryption |
| `search` | `true` | Document search capabilities |
| `auditing` | `true` | Audit trail and logging |
| `esignature` | `false` | Digital signature workflows |
| `virus-scanning` | `false` | Virus scanning integration |
| `content-extraction` | `false` | Text and metadata extraction |

## Default Settings

Configure default behavior and limits:

```yaml
firefly:
  ecm:
    defaults:
      max-file-size-mb: 100         # Maximum file size in MB
      allowed-extensions:           # Allowed file extensions
        - "pdf"
        - "doc"
        - "docx"
        - "txt"
        - "jpg"
        - "png"
      blocked-extensions:           # Blocked file extensions
        - "exe"
        - "bat"
        - "cmd"
        - "scr"
      checksum-algorithm: "SHA-256" # Checksum algorithm
      default-folder: "/"           # Default folder path
```

### Default Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `max-file-size-mb` | Long | `100` | Maximum file size in MB |
| `allowed-extensions` | List<String> | `["pdf", "doc", "docx", "txt", "jpg", "png"]` | Allowed file extensions |
| `blocked-extensions` | List<String> | `["exe", "bat", "cmd", "scr"]` | Blocked file extensions |
| `checksum-algorithm` | String | `"SHA-256"` | Checksum algorithm |
| `default-folder` | String | `"/"` | Default folder path |

## Performance Configuration

Optimize performance settings:

```yaml
firefly:
  ecm:
    performance:
      batch-size: 100               # Batch operation size
      cache-enabled: true           # Enable caching
      cache-expiration: "PT30M"     # Cache expiration time
      compression-enabled: true     # Enable compression
```

### Performance Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `batch-size` | Integer | `100` | Batch operation size |
| `cache-enabled` | Boolean | `true` | Enable caching |
| `cache-expiration` | Duration | `PT30M` | Cache expiration time |
| `compression-enabled` | Boolean | `true` | Enable compression |

## Adapter-Specific Configuration

### Amazon S3 Adapter

```yaml
firefly:
  ecm:
    adapter-type: "s3"
    properties:
      bucket-name: "my-documents"   # S3 bucket name (required)
      region: "us-east-1"           # AWS region (required)
      access-key: "${AWS_ACCESS_KEY}" # AWS access key
      secret-key: "${AWS_SECRET_KEY}" # AWS secret key
      endpoint: ""                  # Custom endpoint for S3-compatible services
      path-prefix: "documents/"     # Path prefix for all documents
      encryption: "AES256"          # Server-side encryption
      storage-class: "STANDARD"     # S3 storage class
```

#### S3 Required Properties
- `bucket-name`: S3 bucket name
- `region`: AWS region

#### S3 Optional Properties
- `access-key`: AWS access key (use IAM roles in production)
- `secret-key`: AWS secret key (use IAM roles in production)
- `endpoint`: Custom endpoint for S3-compatible services
- `path-prefix`: Path prefix for document organization
- `encryption`: Server-side encryption (AES256, aws:kms)
- `storage-class`: S3 storage class (STANDARD, STANDARD_IA, GLACIER)

### Alfresco Adapter

```yaml
firefly:
  ecm:
    adapter-type: "alfresco"
    properties:
      server-url: "http://localhost:8080/alfresco"  # Alfresco server URL (required)
      api-url: "http://localhost:8080/alfresco/api/-default-/public/alfresco/versions/1"  # API URL
      username: "admin"             # Username (required)
      password: "${ALFRESCO_PASSWORD}" # Password (required)
      repository-id: "-default-"    # Repository ID
      root-folder-path: "/Company Home" # Root folder path
      use-cmis: true                # Use CMIS protocol
      enable-versioning: true       # Enable versioning
      enable-aspects: true          # Enable aspects
```

#### Alfresco Required Properties
- `server-url`: Alfresco server URL
- `username`: Alfresco username
- `password`: Alfresco password

#### Alfresco Optional Properties
- `api-url`: REST API URL
- `repository-id`: Repository identifier
- `root-folder-path`: Root folder path
- `use-cmis`: Enable CMIS protocol
- `enable-versioning`: Enable document versioning
- `enable-aspects`: Enable Alfresco aspects

## Environment Variables

Use environment variables for sensitive configuration:

```bash
# AWS Configuration
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export AWS_DEFAULT_REGION=us-east-1

# Alfresco Configuration
export ALFRESCO_USERNAME=admin
export ALFRESCO_PASSWORD=admin

# DocuSign Configuration (if eSignature enabled)
export DOCUSIGN_INTEGRATION_KEY=your-integration-key
export DOCUSIGN_USER_ID=your-user-id
export DOCUSIGN_ACCOUNT_ID=your-account-id
```

## Configuration Profiles

Use Spring profiles for different environments:

### Development Profile (`application-dev.yml`)

```yaml
firefly:
  ecm:
    adapter-type: "s3"
    properties:
      bucket-name: "dev-documents"
      region: "us-east-1"
    features:
      auditing: false
      virus-scanning: false

logging:
  level:
    com.firefly.core.ecm: DEBUG
```

### Production Profile (`application-prod.yml`)

```yaml
firefly:
  ecm:
    adapter-type: "s3"
    properties:
      bucket-name: "prod-documents"
      region: "us-east-1"
      encryption: "AES256"
      storage-class: "STANDARD"
    features:
      auditing: true
      virus-scanning: true
    performance:
      cache-enabled: true
      compression-enabled: true

logging:
  level:
    com.firefly.core.ecm: INFO
```

## Configuration Validation

The library validates configuration on startup:

- Required properties for selected adapter
- Feature compatibility with adapter
- Connection settings validity
- Performance settings ranges

### Common Validation Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `Adapter type not specified` | Missing `adapter-type` | Set `firefly.ecm.adapter-type` |
| `Required property missing` | Missing adapter property | Check adapter documentation |
| `Invalid timeout value` | Invalid duration format | Use ISO-8601 format (PT30S) |
| `Unsupported feature` | Feature not supported by adapter | Disable feature or change adapter |

## Configuration Examples

Complete configuration examples are available in:
- [`application-ecm-example.yml`](../src/main/resources/application-ecm-example.yml)
- [Integration guides](guides/)

## Next Steps

- [S3 Integration Guide](guides/s3-integration.md)
- [Alfresco Integration Guide](guides/alfresco-integration.md)
- [DocuSign Integration Guide](guides/docusign-integration.md)
- [API Reference](api/)
