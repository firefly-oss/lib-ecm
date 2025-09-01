# Azure Blob Storage Integration Guide

This guide demonstrates how to integrate Azure Blob Storage as a document storage backend for the Firefly ECM Library. Azure Blob Storage provides enterprise-grade cloud storage with global availability and strong consistency.

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Project Setup](#2-project-setup)
3. [Azure Account Configuration](#3-azure-account-configuration)
4. [Application Configuration](#4-application-configuration)
5. [Azure Adapter Implementation](#5-azure-adapter-implementation)
6. [Service Layer Implementation](#6-service-layer-implementation)
7. [Testing](#7-testing)
8. [Production Deployment](#8-production-deployment)
9. [Troubleshooting](#9-troubleshooting)

## 1. Prerequisites

Before starting, ensure you have:

- **Azure Account** with Blob Storage access
- **Java 17+** installed and configured
- **Spring Boot 3.0+** knowledge
- **Maven 3.6+** or **Gradle 7.0+**
- **Firefly ECM Library** understanding
- **Azure CLI** (optional but recommended)

## 2. Project Setup

### 2.1 Maven Dependencies

Add the required dependencies to your `pom.xml`:

```xml
<dependencies>
    <!-- Firefly ECM Library -->
    <dependency>
        <groupId>com.firefly</groupId>
        <artifactId>lib-ecm</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Azure Blob Storage SDK -->
    <dependency>
        <groupId>com.azure</groupId>
        <artifactId>azure-storage-blob</artifactId>
        <version>12.23.0</version>
    </dependency>
    
    <!-- Azure Identity for authentication -->
    <dependency>
        <groupId>com.azure</groupId>
        <artifactId>azure-identity</artifactId>
        <version>1.10.0</version>
    </dependency>
    
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- Reactive Streams -->
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-core</artifactId>
    </dependency>
</dependencies>
```

## 3. Azure Account Configuration

### 3.1 Create Azure Storage Account

**Step 1: Create Storage Account via Azure Portal**

1. **Sign in to Azure Portal**:
   - Go to [portal.azure.com](https://portal.azure.com)
   - Sign in with your Azure account

2. **Create Storage Account**:
   - Click "Create a resource" → "Storage" → "Storage account"
   - Fill in the details:
     - **Subscription**: Select your subscription
     - **Resource Group**: Create new or select existing
     - **Storage Account Name**: `fireflyecmstorage` (must be globally unique)
     - **Region**: Choose your preferred region
     - **Performance**: Standard (or Premium for high performance)
     - **Redundancy**: LRS (or GRS for geo-redundancy)

3. **Configure Advanced Settings**:
   - **Security**: Enable "Require secure transfer"
   - **Access tier**: Hot (for frequently accessed data)
   - **Blob public access**: Disabled (for security)

**Step 2: Create Blob Container**

1. **Navigate to Storage Account**:
   - Go to your storage account → "Containers"
   - Click "Add container"

2. **Container Settings**:
   - **Name**: `firefly-ecm-documents`
   - **Public access level**: Private (no anonymous access)
   - Click "Create"

### 3.2 Configure Authentication

**Option A: Connection String (Development)**

1. **Get Connection String**:
   - In your storage account → "Access keys"
   - Copy the connection string from key1 or key2

```bash
# Example connection string format
AZURE_STORAGE_CONNECTION_STRING="DefaultEndpointsProtocol=https;AccountName=fireflyecmstorage;AccountKey=your-account-key;EndpointSuffix=core.windows.net"
```

**Option B: Managed Identity (Production)**

For production deployments on Azure:

1. **Enable System Managed Identity**:
   - On your Azure resource (VM, App Service, etc.)
   - Go to "Identity" → "System assigned" → "On"

2. **Grant Storage Permissions**:
   - Go to Storage Account → "Access Control (IAM)"
   - Add role assignment:
     - **Role**: "Storage Blob Data Contributor"
     - **Assign access to**: "Managed Identity"
     - **Select**: Your application's managed identity

## 4. Application Configuration

### 4.1 Main Application Configuration

Create `src/main/resources/application.yml`:

```yaml
# Spring Boot Configuration
spring:
  application:
    name: "firefly-ecm-azure-demo"

# Firefly ECM Configuration
firefly:
  ecm:
    enabled: true
    adapter-type: "azure-blob"
    
    # Azure Blob Storage configuration
    properties:
      # Required properties
      container-name: "firefly-ecm-documents"
      
      # Authentication (choose one)
      connection-string: "${AZURE_STORAGE_CONNECTION_STRING}"
      # OR for managed identity:
      # account-name: "fireflyecmstorage"
      # use-managed-identity: true
      
      # Optional properties
      path-prefix: "documents/"
      endpoint-suffix: "core.windows.net"
      
      # Performance settings
      max-connections: 50
      connection-timeout-seconds: 30
      read-timeout-seconds: 300
      retry-attempts: 3
      
      # Blob properties
      access-tier: "Hot"  # Hot, Cool, Archive
      enable-versioning: true
      enable-soft-delete: true
      soft-delete-retention-days: 30
    
    # Feature configuration
    features:
      document-management: true
      content-storage: true
      versioning: true
      folder-management: true
      permissions: true
      search: true
      auditing: true
    
    # Default settings
    defaults:
      max-file-size-mb: 100
      allowed-extensions:
        - "pdf"
        - "doc"
        - "docx"
        - "txt"
        - "jpg"
        - "png"
      checksum-algorithm: "SHA-256"

# Logging configuration
logging:
  level:
    com.firefly.core.ecm: INFO
    com.example.ecm: DEBUG
    com.azure.storage: INFO
    com.azure.core: WARN
```

### 4.2 Environment Variables

Create `load-azure-env.sh`:

```bash
#!/bin/bash

echo "🔧 Loading Azure environment variables..."

# Azure Storage configuration
export AZURE_STORAGE_CONNECTION_STRING="DefaultEndpointsProtocol=https;AccountName=fireflyecmstorage;AccountKey=your-account-key;EndpointSuffix=core.windows.net"

# Alternative: Managed Identity (for production)
# export AZURE_STORAGE_ACCOUNT_NAME="fireflyecmstorage"
# export AZURE_CLIENT_ID="your-managed-identity-client-id"

# Validate required variables
if [ -z "$AZURE_STORAGE_CONNECTION_STRING" ]; then
    echo "❌ AZURE_STORAGE_CONNECTION_STRING is not set"
    return 1
fi

echo "✅ Azure environment variables loaded successfully"
echo "   Storage Account: $(echo $AZURE_STORAGE_CONNECTION_STRING | grep -o 'AccountName=[^;]*' | cut -d'=' -f2)"
```

## 5. Azure Adapter Implementation

### 5.1 Azure Configuration Class

Create the Azure configuration:

```java
// src/main/java/com/example/ecm/config/AzureConfiguration.java
package com.example.ecm.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.firefly.core.ecm.config.EcmProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Azure Blob Storage integration.
 * Creates and configures Azure Blob Service clients.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "firefly.ecm.adapter-type", havingValue = "azure-blob")
public class AzureConfiguration {
    
    @Autowired
    private EcmProperties ecmProperties;
    
    /**
     * Creates Azure Blob Service client for storage operations.
     */
    @Bean
    public BlobServiceClient blobServiceClient() {
        log.info("Configuring Azure Blob Service client");
        
        String connectionString = ecmProperties.getAdapterPropertyAsString("connection-string");
        String accountName = ecmProperties.getAdapterPropertyAsString("account-name");
        Boolean useManagedIdentity = ecmProperties.getAdapterPropertyAsBoolean("use-managed-identity");
        String endpointSuffix = ecmProperties.getAdapterPropertyAsString("endpoint-suffix");
        
        BlobServiceClientBuilder clientBuilder = new BlobServiceClientBuilder();
        
        if (connectionString != null && !connectionString.trim().isEmpty()) {
            // Use connection string authentication
            clientBuilder.connectionString(connectionString);
            log.info("Azure client configured with connection string");
            
        } else if (Boolean.TRUE.equals(useManagedIdentity) && accountName != null) {
            // Use managed identity authentication
            String endpoint = String.format("https://%s.blob.%s", 
                accountName, endpointSuffix != null ? endpointSuffix : "core.windows.net");
            
            clientBuilder
                .endpoint(endpoint)
                .credential(new DefaultAzureCredentialBuilder().build());
            
            log.info("Azure client configured with managed identity for account: {}", accountName);
            
        } else {
            throw new IllegalStateException(
                "Azure authentication not configured. Provide either connection-string or account-name with use-managed-identity=true");
        }
        
        BlobServiceClient client = clientBuilder.buildClient();
        log.info("Azure Blob Service client configured successfully");
        
        return client;
    }
}
```

> **Note**: This guide provides the foundation for Azure Blob Storage integration. The complete adapter implementation would follow the same patterns as the S3 adapter, implementing the DocumentPort and DocumentContentPort interfaces using Azure Blob Storage APIs instead of S3 APIs.

## 6. Implementation Notes

The Azure Blob Storage adapter implementation would include:

- **AzureBlobDocumentAdapter**: Implementing DocumentPort interface
- **AzureBlobContentAdapter**: Implementing DocumentContentPort interface  
- **Blob metadata management**: Using Azure blob metadata for document properties
- **Container organization**: Using blob prefixes for folder structure
- **Versioning support**: Leveraging Azure blob versioning features
- **Access tier management**: Optimizing costs with Hot/Cool/Archive tiers

## 7. Key Differences from S3

When implementing the Azure adapter, consider these Azure-specific features:

- **Access Tiers**: Hot, Cool, and Archive for cost optimization
- **Blob Types**: Block blobs for documents, append blobs for logs
- **Metadata**: Azure blob metadata vs S3 object metadata
- **Authentication**: Managed Identity vs IAM roles
- **Versioning**: Azure blob versioning vs S3 versioning
- **Soft Delete**: Azure soft delete vs S3 lifecycle policies

## 8. Production Considerations

### Security
- Use Managed Identity instead of connection strings
- Enable Azure Storage encryption at rest
- Configure network access restrictions
- Enable Azure Storage logging and monitoring

### Performance
- Choose appropriate access tiers based on usage patterns
- Use Azure CDN for frequently accessed content
- Configure appropriate timeout and retry settings
- Monitor storage metrics and costs

### Compliance
- Enable Azure Storage audit logging
- Configure data retention policies
- Implement proper backup strategies
- Monitor compliance with Azure Security Center

## 9. Next Steps

To complete the Azure Blob Storage integration:

1. Implement the full adapter classes following the S3 adapter pattern
2. Add comprehensive error handling for Azure-specific exceptions
3. Implement Azure-specific features like access tiers and soft delete
4. Create integration tests with Azure Storage Emulator
5. Add monitoring and alerting for Azure storage operations

For a complete implementation example, refer to the [S3 Integration Guide](s3-integration.md) and adapt the patterns for Azure Blob Storage APIs.
