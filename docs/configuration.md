# Firefly ECM Configuration Guide

This guide covers configuration for the Firefly ECM Library and its adapter implementations.

## Overview

Configuration is split between:

1. **Core Library Configuration** (lib-ecm): Basic ECM settings and adapter selection
2. **Adapter Configuration**: Adapter-specific settings (provided by each adapter library)

## Prerequisites

Before configuring, ensure you have added the required dependencies:

```xml
<!-- Core library (required) -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-ecm</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Add adapter libraries as needed -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-ecm-adapter-s3</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Core Configuration (lib-ecm)

The core library uses Spring Boot's configuration properties system with the prefix `firefly.ecm`.

### Basic Settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `firefly.ecm.enabled` | Boolean | `true` | Enable/disable ECM functionality |
| `firefly.ecm.adapter-type` | String | - | Document storage adapter to use (e.g., "s3", "azure-blob") |
| `firefly.ecm.esignature.provider` | String | - | eSignature provider to use (e.g., "docusign", "adobe-sign") |

### Example Core Configuration

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: s3           # Select S3 adapter for document storage
    esignature:
      provider: docusign       # Select DocuSign for eSignatures
```

## Adapter Configuration

Adapter-specific configuration is provided under `firefly.ecm.adapter.<adapter-name>`.

### Amazon S3 Adapter Configuration

**Requires**: `lib-ecm-adapter-s3` dependency

```yaml
firefly:
  ecm:
    adapter-type: s3
    adapter:
      s3:
        bucket-name: my-documents-bucket    # Required: S3 bucket name
        region: us-east-1                   # Required: AWS region
        access-key: ${AWS_ACCESS_KEY_ID}    # Optional: AWS access key (uses default credentials if not set)
        secret-key: ${AWS_SECRET_ACCESS_KEY} # Optional: AWS secret key
        endpoint: https://s3.amazonaws.com  # Optional: Custom S3 endpoint (for S3-compatible services)
```

See [S3 Integration Guide](guides/s3-integration.md) for complete setup instructions.

### Azure Blob Storage Adapter Configuration

**Requires**: `lib-ecm-adapter-azure-blob` dependency

```yaml
firefly:
  ecm:
    adapter-type: azure-blob
    adapter:
      azure-blob:
        account-name: mystorageaccount      # Required: Azure storage account name
        container-name: documents           # Required: Blob container name
        account-key: ${AZURE_STORAGE_KEY}   # Optional: Account key (uses managed identity if not set)
        connection-string: ${AZURE_STORAGE_CONNECTION_STRING}  # Optional: Full connection string
```

See [Azure Blob Integration Guide](guides/azure-integration.md) for complete setup instructions.

### DocuSign Adapter Configuration

**Requires**: `lib-ecm-adapter-docusign` dependency

```yaml
firefly:
  ecm:
    esignature:
      provider: docusign
    adapter:
      docusign:
        integration-key: ${DOCUSIGN_INTEGRATION_KEY}  # Required: DocuSign integration key
        user-id: ${DOCUSIGN_USER_ID}                  # Required: DocuSign user ID
        account-id: ${DOCUSIGN_ACCOUNT_ID}            # Required: DocuSign account ID
        private-key: ${DOCUSIGN_PRIVATE_KEY}          # Required: RSA private key for JWT
        base-url: https://demo.docusign.net           # Optional: DocuSign API base URL
```

See [DocuSign Integration Guide](guides/docusign-integration.md) for complete setup instructions.

### Adobe Sign Adapter Configuration

**Requires**: `lib-ecm-adapter-adobe-sign` dependency

```yaml
firefly:
  ecm:
    esignature:
      provider: adobe-sign
    adapter:
      adobe-sign:
        client-id: ${ADOBE_SIGN_CLIENT_ID}            # Required: Adobe Sign client ID
        client-secret: ${ADOBE_SIGN_CLIENT_SECRET}    # Required: Adobe Sign client secret
        base-url: https://api.na1.adobesign.com       # Optional: Adobe Sign API base URL
```
## Complete Configuration Examples

### Example 1: S3 Document Storage Only

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: s3
    adapter:
      s3:
        bucket-name: my-documents
        region: us-east-1
        access-key: ${AWS_ACCESS_KEY_ID}
        secret-key: ${AWS_SECRET_ACCESS_KEY}
```

### Example 2: Azure Blob + DocuSign

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: azure-blob
    esignature:
      provider: docusign
    adapter:
      azure-blob:
        account-name: mystorageaccount
        container-name: documents
        account-key: ${AZURE_STORAGE_KEY}
      docusign:
        integration-key: ${DOCUSIGN_INTEGRATION_KEY}
        user-id: ${DOCUSIGN_USER_ID}
        account-id: ${DOCUSIGN_ACCOUNT_ID}
        private-key: ${DOCUSIGN_PRIVATE_KEY}
```

### Example 3: S3 + Adobe Sign

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: s3
    esignature:
      provider: adobe-sign
    adapter:
      s3:
        bucket-name: my-documents
        region: us-west-2
      adobe-sign:
        client-id: ${ADOBE_SIGN_CLIENT_ID}
        client-secret: ${ADOBE_SIGN_CLIENT_SECRET}
```
## Graceful Degradation

If no adapter is configured or available, the library will:

1. **Log a warning** at startup indicating no adapter was found
2. **Use no-op adapters** that return empty results or throw `UnsupportedOperationException`
3. **Allow the application to start** without failing

This allows you to include the library without being forced to configure an adapter immediately.

```
WARN: No adapter found for type 'document'. Using no-op adapter.
WARN: No adapter found for type 'esignature'. Using no-op adapter.
```

## Environment Variables

Use environment variables for sensitive configuration:

```bash
# AWS Configuration (for S3 adapter)
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export AWS_DEFAULT_REGION=us-east-1

# Azure Configuration (for Azure Blob adapter)
export AZURE_STORAGE_ACCOUNT=mystorageaccount
export AZURE_STORAGE_KEY=your-storage-key

# DocuSign Configuration (for DocuSign adapter)
export DOCUSIGN_INTEGRATION_KEY=your-integration-key
export DOCUSIGN_USER_ID=your-user-id
export DOCUSIGN_ACCOUNT_ID=your-account-id
export DOCUSIGN_PRIVATE_KEY=your-private-key

# Adobe Sign Configuration (for Adobe Sign adapter)
export ADOBE_SIGN_CLIENT_ID=your-client-id
export ADOBE_SIGN_CLIENT_SECRET=your-client-secret
```

## Configuration Profiles

Use Spring profiles for different environments:

### Development Profile (`application-dev.yml`)

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: s3
    adapter:
      s3:
        bucket-name: dev-documents
        region: us-east-1

logging:
  level:
    com.firefly.core.ecm: DEBUG
```

### Production Profile (`application-prod.yml`)

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: s3
    esignature:
      provider: docusign
    adapter:
      s3:
        bucket-name: prod-documents
        region: us-east-1
      docusign:
        integration-key: ${DOCUSIGN_INTEGRATION_KEY}
        user-id: ${DOCUSIGN_USER_ID}
        account-id: ${DOCUSIGN_ACCOUNT_ID}
        private-key: ${DOCUSIGN_PRIVATE_KEY}

logging:
  level:
    com.firefly.core.ecm: INFO
```

## Configuration Validation

Each adapter library validates its own configuration on startup:

- Required properties for the adapter
- Connection settings validity
- Credential format and availability

### Common Configuration Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| `No adapter found` | Adapter library not in classpath | Add adapter dependency to pom.xml |
| `Adapter type not specified` | Missing `adapter-type` | Set `firefly.ecm.adapter-type` |
| `Required property missing` | Missing adapter property | Check adapter integration guide |
| `Authentication failed` | Invalid credentials | Verify environment variables |

## Troubleshooting

### No Adapter Found Warning

If you see warnings like:
```
WARN: No adapter found for type 'document'. Using no-op adapter.
```

**Causes:**
1. Adapter library not added to dependencies
2. `adapter-type` doesn't match any registered adapter
3. Adapter auto-configuration conditions not met

**Solutions:**
1. Add the adapter dependency to your `pom.xml`
2. Verify `firefly.ecm.adapter-type` matches the adapter name
3. Check adapter-specific configuration requirements

## Next Steps

- [S3 Integration Guide](guides/s3-integration.md) - Complete S3 adapter setup
- [Azure Blob Integration Guide](guides/azure-integration.md) - Complete Azure Blob adapter setup
- [DocuSign Integration Guide](guides/docusign-integration.md) - Complete DocuSign adapter setup
- [Architecture Guide](architecture.md) - Understanding the hexagonal architecture
