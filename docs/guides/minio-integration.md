# MinIO Integration Guide

> **⚠️ IMPLEMENTATION STATUS: PLANNED**
>
> This guide describes the planned MinIO integration for the Firefly ECM Library. While MinIO is S3-compatible and could potentially work with the existing S3 adapter, a dedicated MinIO adapter is **not yet implemented** but is planned for a future release. This guide serves as a design specification and implementation roadmap.
>
> **Currently Available Adapters:**
> - ✅ **S3 Adapter** - Fully implemented and tested (21/21 tests passing) - May work with MinIO via S3 compatibility
> - ✅ **DocuSign Adapter** - Fully implemented and tested (10/10 tests passing)
>
> **Planned Adapters:**
> - 🚧 **MinIO Adapter** - Design complete, implementation planned
> - 🚧 **Azure Blob Adapter** - Design planned
> - 🚧 **Alfresco Adapter** - Design planned

This guide demonstrates how to integrate MinIO as a self-hosted, S3-compatible document storage backend for the Firefly ECM Library once the dedicated adapter is implemented. MinIO provides high-performance object storage that's compatible with Amazon S3 APIs while giving you full control over your data.

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [MinIO Server Setup](#2-minio-server-setup)
3. [Project Setup](#3-project-setup)
4. [Application Configuration](#4-application-configuration)
5. [MinIO Adapter Implementation](#5-minio-adapter-implementation)
6. [Testing](#6-testing)
7. [Production Deployment](#7-production-deployment)
8. [Troubleshooting](#8-troubleshooting)

## 1. Prerequisites

Before starting, ensure you have:

- **Docker** or **Kubernetes** for MinIO deployment
- **Java 17+** installed and configured
- **Spring Boot 3.0+** knowledge
- **Maven 3.6+** or **Gradle 7.0+**
- **Firefly ECM Library** understanding
- **MinIO Client (mc)** for administration

## 2. MinIO Server Setup

### 2.1 Docker Deployment (Development)

**Single Node Setup**:

```bash
# Create MinIO data directory
mkdir -p ~/minio/data

# Run MinIO server
docker run -d \
  --name minio-server \
  -p 9000:9000 \
  -p 9001:9001 \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin123" \
  -v ~/minio/data:/data \
  minio/minio server /data --console-address ":9001"

# Verify MinIO is running
curl http://localhost:9000/minio/health/live
```

**Docker Compose Setup**:

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  minio:
    image: minio/minio:latest
    container_name: firefly-minio
    ports:
      - "9000:9000"    # API port
      - "9001:9001"    # Console port
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin123
      MINIO_BROWSER_REDIRECT_URL: http://localhost:9001
    volumes:
      - minio_data:/data
    command: server /data --console-address ":9001"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 30s
      timeout: 20s
      retries: 3

volumes:
  minio_data:
    driver: local
```

```bash
# Start MinIO with Docker Compose
docker-compose up -d

# Check status
docker-compose ps
```

### 2.2 MinIO Client Configuration

**Install MinIO Client**:

```bash
# Download MinIO client
curl https://dl.min.io/client/mc/release/linux-amd64/mc \
  --create-dirs \
  -o $HOME/minio-binaries/mc

chmod +x $HOME/minio-binaries/mc
export PATH=$PATH:$HOME/minio-binaries/

# Add to your shell profile
echo 'export PATH=$PATH:$HOME/minio-binaries/' >> ~/.bashrc
```

**Configure MinIO Client**:

```bash
# Add MinIO server alias
mc alias set local http://localhost:9000 minioadmin minioadmin123

# Test connection
mc admin info local

# Create bucket for ECM documents
mc mb local/firefly-ecm-documents

# Set bucket policy (optional - for public read access)
mc anonymous set download local/firefly-ecm-documents

# List buckets
mc ls local
```

### 2.3 Production Deployment (Kubernetes)

For production, deploy MinIO on Kubernetes:

```yaml
# minio-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: minio
  namespace: firefly-ecm
spec:
  replicas: 1
  selector:
    matchLabels:
      app: minio
  template:
    metadata:
      labels:
        app: minio
    spec:
      containers:
      - name: minio
        image: minio/minio:latest
        args:
        - server
        - /data
        - --console-address
        - ":9001"
        env:
        - name: MINIO_ROOT_USER
          valueFrom:
            secretKeyRef:
              name: minio-secret
              key: root-user
        - name: MINIO_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: minio-secret
              key: root-password
        ports:
        - containerPort: 9000
        - containerPort: 9001
        volumeMounts:
        - name: data
          mountPath: /data
        livenessProbe:
          httpGet:
            path: /minio/health/live
            port: 9000
          initialDelaySeconds: 30
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /minio/health/ready
            port: 9000
          initialDelaySeconds: 10
          periodSeconds: 10
      volumes:
      - name: data
        persistentVolumeClaim:
          claimName: minio-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: minio-service
  namespace: firefly-ecm
spec:
  selector:
    app: minio
  ports:
  - name: api
    port: 9000
    targetPort: 9000
  - name: console
    port: 9001
    targetPort: 9001
  type: ClusterIP
```

## 3. Project Setup

### 3.1 Maven Dependencies

Since MinIO is S3-compatible, we can reuse the AWS S3 SDK:

```xml
<dependencies>
    <!-- Firefly ECM Library -->
    <dependency>
        <groupId>com.firefly</groupId>
        <artifactId>lib-ecm</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- AWS SDK for S3 (works with MinIO) -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
        <version>2.20.26</version>
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
</dependencies>
```

## 4. Application Configuration

### 4.1 Main Application Configuration

Create `src/main/resources/application.yml`:

```yaml
# Spring Boot Configuration
spring:
  application:
    name: "firefly-ecm-minio-demo"

# Firefly ECM Configuration
firefly:
  ecm:
    enabled: true
    adapter-type: "minio"  # or "s3" with MinIO endpoint
    
    # MinIO configuration (S3-compatible)
    properties:
      # Required properties
      bucket-name: "firefly-ecm-documents"
      region: "us-east-1"  # MinIO doesn't use regions, but S3 SDK requires it
      
      # MinIO-specific properties
      endpoint: "http://localhost:9000"  # MinIO server endpoint
      path-style-access: true            # Required for MinIO
      
      # Authentication
      access-key: "${MINIO_ACCESS_KEY:minioadmin}"
      secret-key: "${MINIO_SECRET_KEY:minioadmin123}"
      
      # Optional properties
      path-prefix: "documents/"
      
      # Connection settings
      max-connections: 50
      connection-timeout-seconds: 30
      socket-timeout-seconds: 300
      retry-attempts: 3
      
      # MinIO features
      enable-multipart-upload: true
      multipart-threshold-mb: 100
      multipart-part-size-mb: 10
    
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
    software.amazon.awssdk: INFO
    software.amazon.awssdk.request: WARN
```

### 4.2 Environment Variables

Create `load-minio-env.sh`:

```bash
#!/bin/bash

echo "🔧 Loading MinIO environment variables..."

# MinIO server configuration
export MINIO_ENDPOINT="http://localhost:9000"
export MINIO_ACCESS_KEY="minioadmin"
export MINIO_SECRET_KEY="minioadmin123"
export MINIO_BUCKET="firefly-ecm-documents"

# For production, use stronger credentials
# export MINIO_ACCESS_KEY="your-access-key"
# export MINIO_SECRET_KEY="your-secret-key"

# Validate required variables
required_vars=("MINIO_ENDPOINT" "MINIO_ACCESS_KEY" "MINIO_SECRET_KEY")

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "❌ Required environment variable $var is not set"
        return 1
    fi
done

echo "✅ MinIO environment variables loaded successfully"
echo "   Endpoint: $MINIO_ENDPOINT"
echo "   Access Key: ${MINIO_ACCESS_KEY:0:8}..."
echo "   Bucket: $MINIO_BUCKET"

# Test MinIO connectivity
echo "🔍 Testing MinIO connectivity..."
curl -s "$MINIO_ENDPOINT/minio/health/live" > /dev/null
if [ $? -eq 0 ]; then
    echo "✅ MinIO server is accessible"
else
    echo "❌ MinIO server is not accessible at $MINIO_ENDPOINT"
fi
```

## 5. MinIO Adapter Implementation

### 5.1 MinIO Configuration Class

Create the MinIO configuration:

```java
// src/main/java/com/example/ecm/config/MinIOConfiguration.java
package com.example.ecm.config;

import com.firefly.core.ecm.config.EcmProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.time.Duration;

/**
 * Configuration class for MinIO integration.
 * Creates S3-compatible client configured for MinIO server.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "firefly.ecm.adapter-type", havingValue = "minio")
public class MinIOConfiguration {
    
    @Autowired
    private EcmProperties ecmProperties;
    
    /**
     * Creates S3 client configured for MinIO server.
     * MinIO is S3-compatible, so we can use the AWS S3 SDK.
     */
    @Bean
    public S3Client minioS3Client() {
        log.info("Configuring MinIO S3-compatible client");
        
        // Extract MinIO configuration
        String endpoint = ecmProperties.getAdapterPropertyAsString("endpoint");
        String accessKey = ecmProperties.getAdapterPropertyAsString("access-key");
        String secretKey = ecmProperties.getAdapterPropertyAsString("secret-key");
        String region = ecmProperties.getAdapterPropertyAsString("region");
        Integer connectionTimeout = ecmProperties.getAdapterPropertyAsInteger("connection-timeout-seconds");
        Integer socketTimeout = ecmProperties.getAdapterPropertyAsInteger("socket-timeout-seconds");
        
        // Validate required configuration
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalStateException("MinIO endpoint is required");
        }
        if (accessKey == null || accessKey.trim().isEmpty()) {
            throw new IllegalStateException("MinIO access key is required");
        }
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException("MinIO secret key is required");
        }
        
        // Create credentials
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        // Build client configuration
        ClientOverrideConfiguration.Builder configBuilder = ClientOverrideConfiguration.builder();
        
        if (connectionTimeout != null) {
            configBuilder.apiCallTimeout(Duration.ofSeconds(connectionTimeout));
        }
        if (socketTimeout != null) {
            configBuilder.apiCallAttemptTimeout(Duration.ofSeconds(socketTimeout));
        }
        
        // Configure S3 settings for MinIO compatibility
        S3Configuration s3Config = S3Configuration.builder()
            .pathStyleAccessEnabled(true)  // Required for MinIO
            .build();
        
        // Build S3 client
        S3Client client = S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(region != null ? region : "us-east-1"))
            .serviceConfiguration(s3Config)
            .overrideConfiguration(configBuilder.build())
            .build();
        
        log.info("MinIO S3 client configured successfully:");
        log.info("  Endpoint: {}", endpoint);
        log.info("  Access Key: {}...", accessKey.substring(0, Math.min(8, accessKey.length())));
        log.info("  Region: {}", region);
        
        return client;
    }
}
```

> **Note**: Since MinIO is S3-compatible, you can reuse the S3 adapter implementation from the [S3 Integration Guide](s3-integration.md). The key differences are:
> 
> 1. **Endpoint Configuration**: Point to your MinIO server instead of AWS
> 2. **Path-Style Access**: Enable path-style access (required for MinIO)
> 3. **Authentication**: Use MinIO access keys instead of AWS credentials
> 4. **No AWS-Specific Features**: Some AWS-specific features may not be available

## 6. Key Differences from AWS S3

When using MinIO with the S3 adapter, consider these differences:

### Supported Features
- ✅ Basic S3 operations (PUT, GET, DELETE, LIST)
- ✅ Multipart uploads
- ✅ Object metadata
- ✅ Bucket policies
- ✅ Versioning
- ✅ Server-side encryption

### Limitations
- ❌ AWS-specific storage classes (GLACIER, DEEP_ARCHIVE)
- ❌ AWS IAM integration
- ❌ CloudWatch metrics
- ❌ S3 Transfer Acceleration
- ❌ Cross-region replication

### MinIO-Specific Features
- ✅ MinIO Console for administration
- ✅ Distributed mode for high availability
- ✅ Erasure coding for data protection
- ✅ Kubernetes-native deployment
- ✅ Multi-tenancy support

## 7. Testing

### 7.1 Basic Connectivity Test

```java
// Test MinIO connectivity
@Test
void testMinIOConnectivity() {
    // Verify bucket exists
    HeadBucketRequest request = HeadBucketRequest.builder()
        .bucket("firefly-ecm-documents")
        .build();
    
    assertDoesNotThrow(() -> s3Client.headBucket(request));
}
```

### 7.2 Integration Test

```bash
# Test script for MinIO integration
#!/bin/bash

echo "🧪 Testing MinIO integration..."

# Test file upload
echo "Test content" > test-file.txt
mc cp test-file.txt local/firefly-ecm-documents/test/

# Test file download
mc cp local/firefly-ecm-documents/test/test-file.txt downloaded-file.txt

# Verify content
if cmp -s test-file.txt downloaded-file.txt; then
    echo "✅ File upload/download test passed"
else
    echo "❌ File upload/download test failed"
fi

# Cleanup
rm test-file.txt downloaded-file.txt
mc rm local/firefly-ecm-documents/test/test-file.txt
```

## 8. Production Considerations

### High Availability
- Deploy MinIO in distributed mode (4+ nodes)
- Use Kubernetes for container orchestration
- Configure proper load balancing
- Set up monitoring and alerting

### Security
- Use strong access keys and secret keys
- Enable TLS/SSL encryption
- Configure bucket policies for access control
- Regular security updates

### Performance
- Use SSD storage for better performance
- Configure appropriate erasure coding
- Monitor disk usage and performance
- Optimize network configuration

### Backup and Recovery
- Implement regular backup strategies
- Test disaster recovery procedures
- Monitor data integrity
- Document recovery processes

## 9. Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| Connection refused | MinIO server not running | Check MinIO server status |
| Access denied | Invalid credentials | Verify access key and secret key |
| Bucket not found | Bucket doesn't exist | Create bucket using MinIO client |
| Path-style errors | Path-style access disabled | Enable path-style access in configuration |
| SSL errors | Certificate issues | Configure proper SSL certificates |

## Next Steps

1. **Deploy MinIO**: Set up MinIO server in your environment
2. **Implement Adapter**: Use the S3 adapter with MinIO configuration
3. **Test Integration**: Verify all ECM operations work correctly
4. **Monitor Performance**: Set up monitoring and alerting
5. **Plan Scaling**: Design for growth and high availability

For complete adapter implementation, refer to the [S3 Integration Guide](s3-integration.md) and use the MinIO configuration shown above.
