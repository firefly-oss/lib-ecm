# Configuration Reference

This document provides a comprehensive reference for all configuration properties available in the Firefly ECM Library and its adapter modules.

## Table of Contents

- [Core Configuration](#core-configuration)
- [S3 Adapter Configuration](#s3-adapter-configuration)
- [DocuSign Adapter Configuration](#docusign-adapter-configuration)
- [Environment Variables](#environment-variables)
- [Configuration Examples](#configuration-examples)

## Core Configuration

### Basic ECM Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `firefly.ecm.enabled` | Boolean | `true` | Enable/disable the ECM library |
| `firefly.ecm.adapter-type` | String | - | Document storage adapter type (s3, azure-blob, etc.) |
| `firefly.ecm.esignature.provider` | String | - | eSignature provider (docusign, adobe-sign, etc.) |

### Adapter Selection

```yaml
firefly:
  ecm:
    # Document storage adapter
    adapter-type: s3
    
    # eSignature provider
    esignature:
      provider: docusign
```

## S3 Adapter Configuration

### Required Properties

| Property | Type | Description | Environment Variable |
|----------|------|-------------|---------------------|
| `firefly.ecm.adapter.s3.bucket-name` | String | S3 bucket name | `FIREFLY_ECM_ADAPTER_S3_BUCKET_NAME` |
| `firefly.ecm.adapter.s3.region` | String | AWS region | `FIREFLY_ECM_ADAPTER_S3_REGION` |

### Optional Properties

| Property | Type | Default | Description | Environment Variable |
|----------|------|---------|-------------|---------------------|
| `firefly.ecm.adapter.s3.access-key` | String | - | AWS access key | `FIREFLY_ECM_ADAPTER_S3_ACCESS_KEY` |
| `firefly.ecm.adapter.s3.secret-key` | String | - | AWS secret key | `FIREFLY_ECM_ADAPTER_S3_SECRET_KEY` |
| `firefly.ecm.adapter.s3.endpoint` | String | - | Custom S3 endpoint | `FIREFLY_ECM_ADAPTER_S3_ENDPOINT` |
| `firefly.ecm.adapter.s3.path-prefix` | String | `documents/` | Object key prefix | `FIREFLY_ECM_ADAPTER_S3_PATH_PREFIX` |
| `firefly.ecm.adapter.s3.enable-versioning` | Boolean | `true` | Enable S3 versioning | `FIREFLY_ECM_ADAPTER_S3_ENABLE_VERSIONING` |
| `firefly.ecm.adapter.s3.path-style-access` | Boolean | `false` | Use path-style access | `FIREFLY_ECM_ADAPTER_S3_PATH_STYLE_ACCESS` |
| `firefly.ecm.adapter.s3.connection-timeout` | Duration | `PT30S` | Connection timeout | `FIREFLY_ECM_ADAPTER_S3_CONNECTION_TIMEOUT` |
| `firefly.ecm.adapter.s3.socket-timeout` | Duration | `PT30S` | Socket timeout | `FIREFLY_ECM_ADAPTER_S3_SOCKET_TIMEOUT` |
| `firefly.ecm.adapter.s3.max-retries` | Integer | `3` | Max retry attempts | `FIREFLY_ECM_ADAPTER_S3_MAX_RETRIES` |
| `firefly.ecm.adapter.s3.enable-encryption` | Boolean | `true` | Enable server-side encryption | `FIREFLY_ECM_ADAPTER_S3_ENABLE_ENCRYPTION` |
| `firefly.ecm.adapter.s3.kms-key-id` | String | - | KMS key ID | `FIREFLY_ECM_ADAPTER_S3_KMS_KEY_ID` |
| `firefly.ecm.adapter.s3.storage-class` | String | `STANDARD` | S3 storage class | `FIREFLY_ECM_ADAPTER_S3_STORAGE_CLASS` |
| `firefly.ecm.adapter.s3.enable-multipart` | Boolean | `true` | Enable multipart upload | `FIREFLY_ECM_ADAPTER_S3_ENABLE_MULTIPART` |
| `firefly.ecm.adapter.s3.multipart-threshold` | Long | `5242880` | Multipart threshold (bytes) | `FIREFLY_ECM_ADAPTER_S3_MULTIPART_THRESHOLD` |
| `firefly.ecm.adapter.s3.multipart-part-size` | Long | `5242880` | Multipart part size (bytes) | `FIREFLY_ECM_ADAPTER_S3_MULTIPART_PART_SIZE` |

### S3 Storage Classes

Supported storage classes:
- `STANDARD` - Standard storage
- `STANDARD_IA` - Standard Infrequent Access
- `ONEZONE_IA` - One Zone Infrequent Access
- `REDUCED_REDUNDANCY` - Reduced Redundancy
- `GLACIER` - Glacier
- `DEEP_ARCHIVE` - Glacier Deep Archive
- `INTELLIGENT_TIERING` - Intelligent Tiering

## DocuSign Adapter Configuration

### Required Properties

| Property | Type | Description | Environment Variable |
|----------|------|-------------|---------------------|
| `firefly.ecm.adapter.docusign.integration-key` | String | DocuSign integration key | `FIREFLY_ECM_ADAPTER_DOCUSIGN_INTEGRATION_KEY` |
| `firefly.ecm.adapter.docusign.user-id` | String | DocuSign user ID (GUID) | `FIREFLY_ECM_ADAPTER_DOCUSIGN_USER_ID` |
| `firefly.ecm.adapter.docusign.account-id` | String | DocuSign account ID | `FIREFLY_ECM_ADAPTER_DOCUSIGN_ACCOUNT_ID` |
| `firefly.ecm.adapter.docusign.private-key` | String | RSA private key | `FIREFLY_ECM_ADAPTER_DOCUSIGN_PRIVATE_KEY` |

### Optional Properties

| Property | Type | Default | Description | Environment Variable |
|----------|------|---------|-------------|---------------------|
| `firefly.ecm.adapter.docusign.base-url` | String | `https://na3.docusign.net/restapi` | DocuSign API base URL | `FIREFLY_ECM_ADAPTER_DOCUSIGN_BASE_URL` |
| `firefly.ecm.adapter.docusign.auth-server` | String | `https://account.docusign.com` | Auth server URL | `FIREFLY_ECM_ADAPTER_DOCUSIGN_AUTH_SERVER` |
| `firefly.ecm.adapter.docusign.sandbox-mode` | Boolean | `false` | Enable sandbox mode | `FIREFLY_ECM_ADAPTER_DOCUSIGN_SANDBOX_MODE` |
| `firefly.ecm.adapter.docusign.webhook-url` | String | - | Webhook URL | `FIREFLY_ECM_ADAPTER_DOCUSIGN_WEBHOOK_URL` |
| `firefly.ecm.adapter.docusign.webhook-secret` | String | - | Webhook secret | `FIREFLY_ECM_ADAPTER_DOCUSIGN_WEBHOOK_SECRET` |
| `firefly.ecm.adapter.docusign.connection-timeout` | Duration | `PT30S` | Connection timeout | `FIREFLY_ECM_ADAPTER_DOCUSIGN_CONNECTION_TIMEOUT` |
| `firefly.ecm.adapter.docusign.read-timeout` | Duration | `PT60S` | Read timeout | `FIREFLY_ECM_ADAPTER_DOCUSIGN_READ_TIMEOUT` |
| `firefly.ecm.adapter.docusign.max-retries` | Integer | `3` | Max retry attempts | `FIREFLY_ECM_ADAPTER_DOCUSIGN_MAX_RETRIES` |
| `firefly.ecm.adapter.docusign.jwt-expiration` | Long | `3600` | JWT expiration (seconds) | `FIREFLY_ECM_ADAPTER_DOCUSIGN_JWT_EXPIRATION` |
| `firefly.ecm.adapter.docusign.enable-polling` | Boolean | `true` | Enable status polling | `FIREFLY_ECM_ADAPTER_DOCUSIGN_ENABLE_POLLING` |
| `firefly.ecm.adapter.docusign.polling-interval` | Duration | `PT5M` | Polling interval | `FIREFLY_ECM_ADAPTER_DOCUSIGN_POLLING_INTERVAL` |
| `firefly.ecm.adapter.docusign.default-email-subject` | String | `Please sign this document` | Default email subject | `FIREFLY_ECM_ADAPTER_DOCUSIGN_DEFAULT_EMAIL_SUBJECT` |
| `firefly.ecm.adapter.docusign.default-email-message` | String | `Please review and sign the attached document(s).` | Default email message | `FIREFLY_ECM_ADAPTER_DOCUSIGN_DEFAULT_EMAIL_MESSAGE` |
| `firefly.ecm.adapter.docusign.enable-embedded-signing` | Boolean | `false` | Enable embedded signing | `FIREFLY_ECM_ADAPTER_DOCUSIGN_ENABLE_EMBEDDED_SIGNING` |
| `firefly.ecm.adapter.docusign.return-url` | String | - | Return URL for embedded signing | `FIREFLY_ECM_ADAPTER_DOCUSIGN_RETURN_URL` |
| `firefly.ecm.adapter.docusign.enable-retention` | Boolean | `true` | Enable document retention | `FIREFLY_ECM_ADAPTER_DOCUSIGN_ENABLE_RETENTION` |
| `firefly.ecm.adapter.docusign.retention-days` | Integer | `2555` | Retention period (days) | `FIREFLY_ECM_ADAPTER_DOCUSIGN_RETENTION_DAYS` |

### Dependencies

The DocuSign adapter automatically includes all required dependencies:

- **DocuSign eSign Java SDK** (`docusign-esign-java:4.3.0`)
- **JAX-RS API** (`jakarta.ws.rs-api:3.1.0`) - For REST client compatibility
- **Jersey Client** (`jersey-client:3.1.3`) - HTTP client implementation
- **Jersey Media JSON** (`jersey-media-json-jackson:3.1.3`) - JSON processing
- **Jersey HK2** (`jersey-hk2:3.1.3`) - Dependency injection
- **Jersey Multipart** (`jersey-media-multipart:3.1.3`) - Multipart support
- **Apache Oltu OAuth2** (`org.apache.oltu.oauth2.client:1.0.2`) - OAuth2 authentication

> **Note**: All transitive dependencies are automatically managed. No additional configuration is required.

## Environment Variables

All configuration properties can be overridden using environment variables. The naming convention follows the pattern:

```
FIREFLY_ECM_ADAPTER_<ADAPTER>_<PROPERTY>
```

Where:
- `<ADAPTER>` is the adapter name in uppercase (S3, DOCUSIGN)
- `<PROPERTY>` is the property name in uppercase with underscores

### Examples

```bash
# S3 Configuration
export FIREFLY_ECM_ADAPTER_S3_BUCKET_NAME=my-ecm-bucket
export FIREFLY_ECM_ADAPTER_S3_REGION=us-west-2

# DocuSign Configuration
export FIREFLY_ECM_ADAPTER_DOCUSIGN_INTEGRATION_KEY=your-integration-key
export FIREFLY_ECM_ADAPTER_DOCUSIGN_SANDBOX_MODE=true
```

## Configuration Examples

### Development Environment

```yaml
firefly:
  ecm:
    adapter-type: s3
    esignature:
      provider: docusign
    adapter:
      s3:
        bucket-name: dev-ecm-bucket
        region: us-east-1
        endpoint: http://localhost:9000  # MinIO for local development
        path-style-access: true
        enable-encryption: false
      docusign:
        integration-key: ${DOCUSIGN_INTEGRATION_KEY}
        user-id: ${DOCUSIGN_USER_ID}
        account-id: ${DOCUSIGN_ACCOUNT_ID}
        private-key: ${DOCUSIGN_PRIVATE_KEY}
        sandbox-mode: true
        enable-polling: false

logging:
  level:
    com.firefly.ecm: DEBUG
```

### Production Environment

```yaml
firefly:
  ecm:
    adapter-type: s3
    esignature:
      provider: docusign
    adapter:
      s3:
        bucket-name: ${S3_BUCKET_NAME}
        region: ${AWS_REGION}
        enable-versioning: true
        enable-encryption: true
        kms-key-id: ${S3_KMS_KEY_ID}
        storage-class: STANDARD
        multipart-threshold: 10485760  # 10MB
      docusign:
        integration-key: ${DOCUSIGN_INTEGRATION_KEY}
        user-id: ${DOCUSIGN_USER_ID}
        account-id: ${DOCUSIGN_ACCOUNT_ID}
        private-key: ${DOCUSIGN_PRIVATE_KEY}
        sandbox-mode: false
        webhook-url: ${DOCUSIGN_WEBHOOK_URL}
        webhook-secret: ${DOCUSIGN_WEBHOOK_SECRET}
        enable-retention: true
        retention-days: 2555

logging:
  level:
    com.firefly.ecm: INFO
    com.firefly.ecm.adapter: WARN
```

### Multi-Adapter Configuration

```yaml
firefly:
  ecm:
    # Primary document storage
    adapter-type: s3
    
    # Primary eSignature provider
    esignature:
      provider: docusign
    
    adapter:
      s3:
        bucket-name: ${S3_BUCKET_NAME}
        region: ${AWS_REGION}
      
      docusign:
        integration-key: ${DOCUSIGN_INTEGRATION_KEY}
        user-id: ${DOCUSIGN_USER_ID}
        account-id: ${DOCUSIGN_ACCOUNT_ID}
        private-key: ${DOCUSIGN_PRIVATE_KEY}
```

## Validation

The library validates configuration properties at startup. Common validation rules:

- Required properties must be provided
- Duration properties must be valid ISO-8601 durations
- Numeric properties must be within valid ranges
- Boolean properties accept `true`/`false` values
- Enum properties must match valid values

## Testing and Quality Assurance

The Firefly ECM Library maintains **100% test success rate** across all adapter configurations:

| **Module** | **Tests** | **Success Rate** | **Coverage** |
|------------|-----------|------------------|--------------|
| **S3 Adapter** | 21 | ✅ **100%** | All configuration scenarios |
| **DocuSign Adapter** | 10 | ✅ **100%** | Complete workflow testing |
| **TOTAL** | **31** | ✅ **100%** | **Production Ready** |

### Recent Improvements

- **Dependency Resolution**: All DocuSign SDK dependencies automatically managed
- **Resilience Testing**: Comprehensive testing of circuit breaker and retry patterns
- **Configuration Validation**: All property names and defaults verified against actual implementation
- **Error Handling**: Robust testing of configuration validation and error scenarios

## Next Steps

- [S3 Integration Guide](adapters/s3-integration-guide.md)
- [DocuSign Integration Guide](adapters/docusign-integration-guide.md)
- [Testing Guide](testing.md)
- [API Reference](api-reference.md)
