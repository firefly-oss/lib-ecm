# Amazon S3 Integration Guide

This guide shows how to integrate the Amazon S3 adapter for document storage with the Firefly ECM Library.

## Overview

The S3 adapter is provided as a **separate library** (`lib-ecm-adapter-s3`) that implements the ECM port interfaces for Amazon S3 storage. This guide covers:

- Adding the S3 adapter dependency
- Configuring AWS credentials and S3 bucket
- Using the adapter in your application
- Testing and troubleshooting

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Add S3 Adapter Dependency](#2-add-s3-adapter-dependency)
3. [AWS Account Configuration](#3-aws-account-configuration)
4. [Application Configuration](#4-application-configuration)
5. [Using the S3 Adapter](#5-using-the-s3-adapter)
6. [Testing](#6-testing)
7. [Production Deployment](#7-production-deployment)
8. [Troubleshooting](#8-troubleshooting)

## 1. Prerequisites

Before starting, ensure you have:

- **AWS Account** with S3 access and billing configured
- **Java 21+** installed and configured
- **Spring Boot 3.0+** application
- **Maven 3.6+** or **Gradle 7.0+**
- **Firefly ECM Library** (`lib-ecm`) already added to your project
- **Basic AWS CLI** knowledge (optional but recommended)

## 2. Add S3 Adapter Dependency

### 2.1 Maven Configuration

Add the S3 adapter library to your `pom.xml`:

```xml
<dependencies>
    <!-- Firefly ECM Core Library (Port Interfaces) -->
    <dependency>
        <groupId>com.firefly</groupId>
        <artifactId>lib-ecm</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- S3 Adapter Implementation -->
    <dependency>
        <groupId>com.firefly</groupId>
        <artifactId>lib-ecm-adapter-s3</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- Your other dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
</dependencies>
```

### 2.2 Gradle Configuration

For Gradle projects, add to your `build.gradle`:

```gradle
dependencies {
    // Firefly ECM Core Library (Port Interfaces)
    implementation 'com.firefly:lib-ecm:1.0.0-SNAPSHOT'

    // S3 Adapter Implementation
    implementation 'com.firefly:lib-ecm-adapter-s3:1.0.0-SNAPSHOT'

    // Your other dependencies
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
}
```

**Note**: The S3 adapter library automatically includes the AWS SDK dependencies you need.


## 3. AWS Account Configuration

### 3.1 Create AWS Account and IAM User

**Step 1: AWS Account Setup**

1. **Create AWS Account** (if you don't have one):
   - Go to [aws.amazon.com](https://aws.amazon.com)
   - Click "Create an AWS Account"
   - Complete the registration process
   - Add a payment method (required even for free tier)

2. **Access AWS Console**:
   - Sign in to [console.aws.amazon.com](https://console.aws.amazon.com)
   - Verify you're in the correct region (top-right corner)

**Step 2: Create IAM User for Application**

> **🔒 Security Best Practice**: Never use root account credentials in applications. Always create dedicated IAM users.

1. **Navigate to IAM**:
   - In AWS Console, search for "IAM"
   - Click on "IAM" service

2. **Create New User**:
   - Click "Users" → "Create user"
   - Username: `firefly-ecm-s3-user`
   - Select "Programmatic access" only

3. **Attach Permissions**:
   - Click "Attach policies directly"
   - Search and select: `AmazonS3FullAccess`
   - For production, create custom policy with minimal permissions:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:PutObject",
                "s3:DeleteObject",
                "s3:ListBucket",
                "s3:GetObjectVersion",
                "s3:PutObjectAcl",
                "s3:GetObjectAcl"
            ],
            "Resource": [
                "arn:aws:s3:::your-company-documents/*",
                "arn:aws:s3:::your-company-documents"
            ]
        }
    ]
}
```

4. **Create Access Keys**:
   - Complete user creation
   - Go to user details → "Security credentials"
   - Click "Create access key"
   - Choose "Application running outside AWS"
   - **IMPORTANT**: Save both keys securely:
     - Access Key ID: `AKIAIOSFODNN7EXAMPLE`
     - Secret Access Key: `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`

### 3.2 Create S3 Bucket

**Step 1: Create Bucket via AWS Console**

1. **Navigate to S3**:
   - Search for "S3" in AWS Console
   - Click "S3" service

2. **Create Bucket**:
   - Click "Create bucket"
   - Bucket name: `your-company-documents-dev` (must be globally unique)
   - Region: `US East (N. Virginia) us-east-1`
   - Keep default settings for development
   - Click "Create bucket"

**Step 2: Configure Bucket for ECM**

1. **Enable Versioning**:
   - Select your bucket → "Properties"
   - Find "Bucket Versioning" → "Edit"
   - Select "Enable" → "Save changes"

2. **Configure Server-Side Encryption**:
   - Go to "Properties" → "Default encryption"
   - Select "Server-side encryption with Amazon S3 managed keys (SSE-S3)"
   - Click "Save changes"

3. **Set up Lifecycle Rules** (optional):
   - Go to "Management" → "Create lifecycle rule"
   - Rule name: `ecm-lifecycle`
   - Configure transitions for cost optimization

**Step 3: Create Bucket via AWS CLI** (Alternative)

```bash
# Install AWS CLI if not already installed
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Configure AWS CLI
aws configure
# Enter your Access Key ID, Secret Access Key, region (us-east-1), and output format (json)

# Create bucket
aws s3 mb s3://your-company-documents-dev --region us-east-1

# Enable versioning
aws s3api put-bucket-versioning \
    --bucket your-company-documents-dev \
    --versioning-configuration Status=Enabled

# Enable server-side encryption
aws s3api put-bucket-encryption \
    --bucket your-company-documents-dev \
    --server-side-encryption-configuration '{
        "Rules": [
            {
                "ApplyServerSideEncryptionByDefault": {
                    "SSEAlgorithm": "AES256"
                }
            }
        ]
    }'

# Verify bucket creation
aws s3 ls
```

### 3.3 Configure AWS Credentials

**Option A: Environment Variables (Recommended for Development)**

```bash
# Add to your shell profile (.bashrc, .zshrc, etc.)
export AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
export AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
export AWS_DEFAULT_REGION=us-east-1

# Reload shell configuration
source ~/.bashrc  # or ~/.zshrc

# Verify configuration
aws sts get-caller-identity
```

**Option B: AWS Credentials File**

```bash
# Create AWS directory
mkdir -p ~/.aws

# Create credentials file
cat > ~/.aws/credentials << 'EOF'
[default]
aws_access_key_id = AKIAIOSFODNN7EXAMPLE
aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
EOF

# Create config file
cat > ~/.aws/config << 'EOF'
[default]
region = us-east-1
output = json
EOF

# Set proper permissions
chmod 600 ~/.aws/credentials
chmod 600 ~/.aws/config
```

**Option C: IAM Roles (Production on EC2/ECS)**

For production deployments on AWS infrastructure:

```yaml
# No credentials needed in application
# AWS automatically provides temporary credentials via instance metadata
# This is the most secure option for production
```

### 3.4 Test AWS Configuration

```bash
# Test S3 access
aws s3 ls s3://your-company-documents-dev

# Test upload
echo "Test file content" > test.txt
aws s3 cp test.txt s3://your-company-documents-dev/test.txt

# Test download
aws s3 cp s3://your-company-documents-dev/test.txt downloaded-test.txt

# Cleanup
rm test.txt downloaded-test.txt
aws s3 rm s3://your-company-documents-dev/test.txt
```

## 4. Application Configuration

### 4.1 Main Application Configuration

Create `src/main/resources/application.yml`:

```yaml
# Spring Boot Configuration
spring:
  application:
    name: "firefly-ecm-s3-demo"

# Firefly ECM Configuration
firefly:
  ecm:
    # Enable ECM functionality
    enabled: true

    # Select S3 adapter for document storage
    adapter-type: s3

    # S3 adapter configuration
    adapter:
      s3:
        # Required properties
        bucket-name: ${S3_BUCKET_NAME:your-company-documents-dev}
        region: ${AWS_REGION:us-east-1}

        # Optional: AWS credentials (uses default credential chain if not specified)
        access-key: ${AWS_ACCESS_KEY_ID:}
        secret-key: ${AWS_SECRET_ACCESS_KEY:}

        # Optional: Custom S3 endpoint (for MinIO or other S3-compatible services)
        # endpoint: http://localhost:9000

        # Optional: Path-style access (required for some S3-compatible services)
        # path-style-access: false
        - "png"
        - "gif"
        - "bmp"
        - "tiff"
        - "zip"
        - "rar"
        - "7z"
      blocked-extensions:                   # Blocked file extensions for security
        - "exe"
        - "bat"
        - "cmd"
        - "com"
        - "scr"
        - "vbs"
        - "js"
        - "jar"
      checksum-algorithm: "SHA-256"         # Algorithm for file integrity verification
      default-folder: "/"                  # Default folder for new documents

    # Performance optimization settings
    performance:
      batch-size: 100                      # Batch size for bulk operations
      cache-enabled: true                  # Enable metadata caching
      cache-expiration: "PT30M"            # Cache expiration time (30 minutes)
      compression-enabled: true            # Enable content compression

      # Async processing settings
      async-pool-size: 10                  # Thread pool size for async operations
      async-queue-capacity: 1000           # Queue capacity for async tasks

# Logging configuration
logging:
  level:
    # ECM library logging
    com.firefly.core.ecm: INFO
    com.example.ecm: DEBUG

    # AWS SDK logging (reduce noise)
    software.amazon.awssdk: WARN
    software.amazon.awssdk.request: WARN

    # HTTP client logging (for debugging S3 requests)
    # software.amazon.awssdk.request: DEBUG

    # Spring framework
    org.springframework.web: INFO
    org.springframework.security: INFO

    # Root logger
    root: INFO

  # Log pattern for better readability
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# Actuator endpoints for monitoring and health checks
management:
  endpoints:
    web:
      exposure:
        include: "health,info,metrics,env,configprops"
      base-path: "/actuator"
  endpoint:
    health:
      show-details: always
      show-components: always
    env:
      show-values: when-authorized

  # Custom health indicators
  health:
    s3:
      enabled: true
    diskspace:
      enabled: true
      threshold: 10GB

# Application information
info:
  app:
    name: "Firefly ECM S3 Demo"
    description: "Demonstration of Firefly ECM Library with Amazon S3"
    version: "1.0.0"
  ecm:
    adapter: "Amazon S3"
    features: "Document Management, Folder Hierarchy, Search, Versioning"
```

### 4.2 Environment-Specific Configurations

**Development Configuration** (`application-dev.yml`):

```yaml
firefly:
  ecm:
    properties:
      bucket-name: "your-company-documents-dev"
    defaults:
      max-file-size-mb: 50  # Smaller limit for development
    performance:
      cache-enabled: false  # Disable caching for development

logging:
  level:
    com.firefly.core.ecm: DEBUG
    com.example.ecm: DEBUG
    software.amazon.awssdk: DEBUG

# Enable debug logging for development
debug: false
```

**Production Configuration** (`application-prod.yml`):

```yaml
firefly:
  ecm:
    properties:
      bucket-name: "your-company-documents-prod"
      encryption: "AES256"
      storage-class: "STANDARD"
      enable-transfer-acceleration: true
    defaults:
      max-file-size-mb: 500  # Higher limit for production
    performance:
      cache-enabled: true
      compression-enabled: true
      async-pool-size: 20

logging:
  level:
    com.firefly.core.ecm: INFO
    software.amazon.awssdk: WARN
    root: WARN

# Production logging to file
  file:
    name: "/var/log/firefly-ecm/application.log"
    max-size: 100MB
    max-history: 30
```

### 4.3 Configuration Validation

Create a configuration validator to ensure proper setup:

```java
// src/main/java/com/example/ecm/config/EcmConfigurationValidator.java
package com.example.ecm.config;

import com.firefly.core.ecm.config.EcmProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@Slf4j
@Component
public class EcmConfigurationValidator implements CommandLineRunner {

    @Autowired
    private EcmProperties ecmProperties;

    @Autowired
    private S3Client s3Client;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== Firefly ECM Configuration Validation ===");

        // Validate ECM is enabled
        if (!ecmProperties.getEnabled()) {
            log.warn("ECM is disabled in configuration");
            return;
        }

        // Validate adapter type
        String adapterType = ecmProperties.getAdapterType();
        log.info("Adapter Type: {}", adapterType);

        if (!"s3".equals(adapterType)) {
            log.warn("Expected adapter type 's3', but found: {}", adapterType);
            return;
        }

        // Validate S3 configuration
        validateS3Configuration();

        log.info("=== Configuration Validation Complete ===");
    }

    private void validateS3Configuration() {
        String bucketName = ecmProperties.getAdapterPropertyAsString("bucket-name");
        String region = ecmProperties.getAdapterPropertyAsString("region");

        log.info("S3 Bucket: {}", bucketName);
        log.info("S3 Region: {}", region);

        if (bucketName == null || bucketName.trim().isEmpty()) {
            log.error("S3 bucket name is not configured");
            return;
        }

        if (region == null || region.trim().isEmpty()) {
            log.error("S3 region is not configured");
            return;
        }

        // Test S3 connectivity
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                .bucket(bucketName)
                .build());
            log.info("✅ S3 bucket '{}' is accessible", bucketName);
        } catch (Exception e) {
            log.error("❌ Failed to access S3 bucket '{}': {}", bucketName, e.getMessage());
        }
    }
}
```

## 5. S3 Adapter Implementation

Now we'll implement the complete S3 adapter that supports all Firefly ECM port interfaces. This implementation will handle document storage, folder management, versioning, and search capabilities.

### 5.1 S3 Configuration Class

First, create the S3 configuration:

```java
// src/main/java/com/example/ecm/config/S3Configuration.java
package com.example.ecm.config;

import com.firefly.core.ecm.config.EcmProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration.Builder;

import java.net.URI;
import java.time.Duration;

/**
 * Configuration class for Amazon S3 integration.
 * Creates and configures S3 clients based on ECM properties.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "firefly.ecm.adapter-type", havingValue = "s3")
public class S3Configuration {

    @Autowired
    private EcmProperties ecmProperties;

    /**
     * Creates synchronous S3 client for blocking operations.
     */
    @Bean
    public S3Client s3Client() {
        log.info("Configuring S3 synchronous client");

        String region = ecmProperties.getAdapterPropertyAsString("region");
        Integer connectionTimeout = ecmProperties.getAdapterPropertyAsInteger("connection-timeout-seconds");
        Integer socketTimeout = ecmProperties.getAdapterPropertyAsInteger("socket-timeout-seconds");
        Integer maxConnections = ecmProperties.getAdapterPropertyAsInteger("max-connections");
        String endpoint = ecmProperties.getAdapterPropertyAsString("endpoint");
        Boolean pathStyleAccess = ecmProperties.getAdapterPropertyAsBoolean("path-style-access");

        // Build client configuration
        ClientOverrideConfiguration.Builder configBuilder = ClientOverrideConfiguration.builder();

        if (connectionTimeout != null) {
            configBuilder.apiCallTimeout(Duration.ofSeconds(connectionTimeout));
        }

        if (socketTimeout != null) {
            configBuilder.apiCallAttemptTimeout(Duration.ofSeconds(socketTimeout));
        }

        // Build S3 client
        software.amazon.awssdk.services.s3.S3ClientBuilder clientBuilder = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .overrideConfiguration(configBuilder.build());

        // Configure S3-specific settings
        Builder s3ConfigBuilder = software.amazon.awssdk.services.s3.S3Configuration.builder();

        if (pathStyleAccess != null) {
            s3ConfigBuilder.pathStyleAccessEnabled(pathStyleAccess);
        }

        clientBuilder.serviceConfiguration(s3ConfigBuilder.build());

        // Custom endpoint (for S3-compatible services)
        if (endpoint != null && !endpoint.trim().isEmpty()) {
            clientBuilder.endpointOverride(URI.create(endpoint));
        }

        S3Client client = clientBuilder.build();
        log.info("S3 client configured successfully for region: {}", region);

        return client;
    }

    /**
     * Creates asynchronous S3 client for non-blocking operations.
     */
    @Bean
    public S3AsyncClient s3AsyncClient() {
        log.info("Configuring S3 asynchronous client");

        String region = ecmProperties.getAdapterPropertyAsString("region");
        Integer maxConnections = ecmProperties.getAdapterPropertyAsInteger("max-connections");
        String endpoint = ecmProperties.getAdapterPropertyAsString("endpoint");

        // Configure async HTTP client
        NettyNioAsyncHttpClient.Builder httpClientBuilder = NettyNioAsyncHttpClient.builder();

        if (maxConnections != null) {
            httpClientBuilder.maxConcurrency(maxConnections);
        }

        // Build async S3 client
        software.amazon.awssdk.services.s3.S3AsyncClientBuilder clientBuilder = S3AsyncClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .httpClientBuilder(httpClientBuilder);

        // Custom endpoint (for S3-compatible services)
        if (endpoint != null && !endpoint.trim().isEmpty()) {
            clientBuilder.endpointOverride(URI.create(endpoint));
        }

        S3AsyncClient client = clientBuilder.build();
        log.info("S3 async client configured successfully");

        return client;
    }
}
```

### 5.2 S3 Document Adapter

Create the main document adapter implementing DocumentPort:

```java
// src/main/java/com/example/ecm/adapter/S3DocumentAdapter.java
package com.example.ecm.adapter;

import com.firefly.core.ecm.adapter.EcmAdapter;
import com.firefly.core.ecm.adapter.AdapterFeature;
import com.firefly.core.ecm.config.EcmProperties;
import com.firefly.core.ecm.domain.model.document.Document;
import com.firefly.core.ecm.domain.enums.document.DocumentStatus;
import com.firefly.core.ecm.port.document.DocumentPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Amazon S3 implementation of DocumentPort.
 *
 * This adapter provides complete document management capabilities using Amazon S3
 * as the storage backend. It supports:
 * - Document CRUD operations
 * - Metadata storage using S3 object metadata and tags
 * - Folder organization using S3 key prefixes
 * - Document versioning using S3 versioning
 * - Batch operations for performance
 */
@Slf4j
@EcmAdapter(
    type = "s3",
    description = "Amazon S3 Document Storage Adapter",
    supportedFeatures = {
        AdapterFeature.DOCUMENT_CRUD,
        AdapterFeature.CONTENT_STORAGE,
        AdapterFeature.VERSIONING,
        AdapterFeature.FOLDER_MANAGEMENT,
        AdapterFeature.SEARCH
    },
    requiredProperties = {"bucket-name", "region"},
    optionalProperties = {"path-prefix", "encryption", "storage-class", "endpoint"}
)
@Component
@ConditionalOnProperty(name = "firefly.ecm.adapter-type", havingValue = "s3")
public class S3DocumentAdapter implements DocumentPort {

    private final S3Client s3Client;
    private final EcmProperties ecmProperties;
    private final String bucketName;
    private final String pathPrefix;

    @Autowired
    public S3DocumentAdapter(S3Client s3Client, EcmProperties ecmProperties) {
        this.s3Client = s3Client;
        this.ecmProperties = ecmProperties;
        this.bucketName = ecmProperties.getAdapterPropertyAsString("bucket-name");
        this.pathPrefix = ecmProperties.getAdapterPropertyAsString("path-prefix");

        log.info("S3DocumentAdapter initialized with bucket: {}, prefix: {}", bucketName, pathPrefix);
    }

    @Override
    public Mono<Document> createDocument(Document document, byte[] content) {
        return Mono.fromCallable(() -> {
            log.debug("Creating document: {} (size: {} bytes)", document.getName(), content.length);

            // Generate unique ID if not provided
            UUID documentId = document.getId() != null ? document.getId() : UUID.randomUUID();

            // Generate S3 key
            String s3Key = generateS3Key(documentId, document.getName());

            // Calculate checksum for integrity verification
            String checksum = calculateChecksum(content, "SHA-256");

            // Prepare S3 metadata
            Map<String, String> metadata = buildS3Metadata(document, checksum);

            // Prepare S3 tags for additional metadata
            Map<String, String> tags = buildS3Tags(document);

            // Build put object request
            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(document.getMimeType())
                .contentLength((long) content.length)
                .metadata(metadata);

            // Apply optional S3 features
            applyS3Features(requestBuilder);

            // Upload to S3
            PutObjectRequest request = requestBuilder.build();
            PutObjectResponse response = s3Client.putObject(request, RequestBody.fromBytes(content));

            // Apply tags if any
            if (!tags.isEmpty()) {
                applyS3Tags(s3Key, tags);
            }

            log.info("Document uploaded successfully: {} -> {}", documentId, s3Key);

            // Return document with S3 information
            return document.toBuilder()
                .id(documentId)
                .storagePath(s3Key)
                .size((long) content.length)
                .checksum(checksum)
                .checksumAlgorithm("SHA-256")
                .status(DocumentStatus.ACTIVE)
                .createdAt(Instant.now())
                .modifiedAt(Instant.now())
                .build();

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to create document: {}", document.getName(), error));
    }

    @Override
    public Mono<Document> getDocument(UUID documentId) {
        return Mono.fromCallable(() -> {
            log.debug("Retrieving document: {}", documentId);

            // Find S3 key for document
            String s3Key = findS3KeyByDocumentId(documentId);

            // Get object metadata
            HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

            HeadObjectResponse response = s3Client.headObject(request);

            // Extract document information from S3 metadata
            Document document = buildDocumentFromS3Metadata(documentId, s3Key, response);

            log.debug("Document retrieved: {} -> {}", documentId, s3Key);
            return document;

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to get document: {}", documentId, error))
          .onErrorReturn(NoSuchKeyException.class, null);
    }

    @Override
    public Mono<Document> updateDocument(Document document) {
        return Mono.fromCallable(() -> {
            log.debug("Updating document: {}", document.getId());

            String s3Key = document.getStoragePath();
            if (s3Key == null) {
                s3Key = findS3KeyByDocumentId(document.getId());
            }

            // Update metadata using copy operation (S3 doesn't support metadata-only updates)
            Map<String, String> metadata = buildS3Metadata(document, document.getChecksum());

            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(s3Key)
                .destinationBucket(bucketName)
                .destinationKey(s3Key)
                .metadata(metadata)
                .metadataDirective(MetadataDirective.REPLACE)
                .build();

            s3Client.copyObject(copyRequest);

            // Update tags
            Map<String, String> tags = buildS3Tags(document);
            if (!tags.isEmpty()) {
                applyS3Tags(s3Key, tags);
            }

            log.info("Document updated: {} -> {}", document.getId(), s3Key);

            return document.toBuilder()
                .modifiedAt(Instant.now())
                .build();

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to update document: {}", document.getId(), error));
    }

    @Override
    public Mono<Void> deleteDocument(UUID documentId) {
        return Mono.fromRunnable(() -> {
            log.debug("Deleting document: {}", documentId);

            String s3Key = findS3KeyByDocumentId(documentId);

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

            s3Client.deleteObject(request);

            log.info("Document deleted: {} -> {}", documentId, s3Key);

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to delete document: {}", documentId, error))
          .then();
    }

    @Override
    public Mono<Boolean> existsDocument(UUID documentId) {
        return Mono.fromCallable(() -> {
            try {
                String s3Key = findS3KeyByDocumentId(documentId);

                HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

                s3Client.headObject(request);
                return true;

            } catch (NoSuchKeyException e) {
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to check document existence: {}", documentId, error))
          .onErrorReturn(false);
    }

    @Override
    public Flux<Document> findDocumentsByFolder(UUID folderId) {
        return Mono.fromCallable(() -> {
            // Convert folder ID to S3 prefix
            String folderPrefix = generateFolderPrefix(folderId);

            ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(folderPrefix)
                .delimiter("/")  // Only direct children, not recursive
                .maxKeys(1000)
                .build();

            List<Document> documents = new ArrayList<>();
            ListObjectsV2Response response;

            do {
                response = s3Client.listObjectsV2(request);

                for (S3Object s3Object : response.contents()) {
                    try {
                        UUID documentId = extractDocumentIdFromS3Key(s3Object.key());
                        Document document = getDocument(documentId).block();
                        if (document != null) {
                            documents.add(document);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to process S3 object: {}", s3Object.key(), e);
                    }
                }

                // Prepare for next page if needed
                request = request.toBuilder()
                    .continuationToken(response.nextContinuationToken())
                    .build();

            } while (response.isTruncated());

            return documents;

        }).subscribeOn(Schedulers.boundedElastic())
          .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<Document> findDocumentsByStatus(DocumentStatus status) {
        return findAllDocuments()
            .filter(document -> status.equals(document.getStatus()));
    }

    @Override
    public Flux<Document> findAllDocuments() {
        return Mono.fromCallable(() -> {
            String prefix = pathPrefix != null ? pathPrefix : "";

            ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .maxKeys(1000)
                .build();

            List<Document> documents = new ArrayList<>();
            ListObjectsV2Response response;

            do {
                response = s3Client.listObjectsV2(request);

                for (S3Object s3Object : response.contents()) {
                    try {
                        UUID documentId = extractDocumentIdFromS3Key(s3Object.key());
                        Document document = getDocument(documentId).block();
                        if (document != null) {
                            documents.add(document);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to process S3 object: {}", s3Object.key(), e);
                    }
                }

                request = request.toBuilder()
                    .continuationToken(response.nextContinuationToken())
                    .build();

            } while (response.isTruncated());

            return documents;

        }).subscribeOn(Schedulers.boundedElastic())
          .flatMapMany(Flux::fromIterable);
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Generates S3 key for a document.
     * Format: [prefix]/folders/[folderId]/documents/[documentId].[extension]
     */
    private String generateS3Key(UUID documentId, String fileName) {
        StringBuilder keyBuilder = new StringBuilder();

        // Add path prefix if configured
        if (pathPrefix != null && !pathPrefix.trim().isEmpty()) {
            keyBuilder.append(pathPrefix);
            if (!pathPrefix.endsWith("/")) {
                keyBuilder.append("/");
            }
        }

        // Add document ID
        keyBuilder.append("documents/").append(documentId);

        // Add file extension if present
        String extension = getFileExtension(fileName);
        if (!extension.isEmpty()) {
            keyBuilder.append(".").append(extension);
        }

        return keyBuilder.toString();
    }

    /**
     * Generates folder prefix for S3 keys.
     */
    private String generateFolderPrefix(UUID folderId) {
        StringBuilder prefixBuilder = new StringBuilder();

        if (pathPrefix != null && !pathPrefix.trim().isEmpty()) {
            prefixBuilder.append(pathPrefix);
            if (!pathPrefix.endsWith("/")) {
                prefixBuilder.append("/");
            }
        }

        prefixBuilder.append("folders/").append(folderId).append("/");

        return prefixBuilder.toString();
    }

    /**
     * Finds S3 key for a document ID.
     * In production, this should be stored in a database for efficiency.
     */
    private String findS3KeyByDocumentId(UUID documentId) {
        String prefix = pathPrefix != null ? pathPrefix : "";
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix += "/";
        }

        // Search for objects with document ID in the key
        String searchPrefix = prefix + "documents/" + documentId;

        ListObjectsV2Request request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(searchPrefix)
            .maxKeys(1)
            .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        if (response.contents().isEmpty()) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        return response.contents().get(0).key();
    }

    /**
     * Extracts document ID from S3 key.
     */
    private UUID extractDocumentIdFromS3Key(String s3Key) {
        // Extract UUID from key pattern: .../documents/[uuid].[extension]
        String[] parts = s3Key.split("/");
        for (String part : parts) {
            if (part.contains(".")) {
                String uuidPart = part.substring(0, part.lastIndexOf("."));
                try {
                    return UUID.fromString(uuidPart);
                } catch (IllegalArgumentException e) {
                    // Not a valid UUID, continue searching
                }
            }
        }
        throw new IllegalArgumentException("Cannot extract document ID from S3 key: " + s3Key);
    }

    /**
     * Builds S3 metadata from document properties.
     */
    private Map<String, String> buildS3Metadata(Document document, String checksum) {
        Map<String, String> metadata = new HashMap<>();

        // Core document properties
        metadata.put("firefly-document-id", document.getId().toString());
        metadata.put("firefly-document-name", document.getName());
        metadata.put("firefly-document-status", document.getStatus().toString());

        if (document.getDescription() != null) {
            metadata.put("firefly-description", document.getDescription());
        }

        if (document.getFolderId() != null) {
            metadata.put("firefly-folder-id", document.getFolderId().toString());
        }

        if (document.getCreatedBy() != null) {
            metadata.put("firefly-created-by", document.getCreatedBy().toString());
        }

        if (document.getModifiedBy() != null) {
            metadata.put("firefly-modified-by", document.getModifiedBy().toString());
        }

        if (checksum != null) {
            metadata.put("firefly-checksum", checksum);
            metadata.put("firefly-checksum-algorithm", "SHA-256");
        }

        metadata.put("firefly-created-at", Instant.now().toString());

        return metadata;
    }

    /**
     * Builds S3 tags from document properties.
     */
    private Map<String, String> buildS3Tags(Document document) {
        Map<String, String> tags = new HashMap<>();

        tags.put("DocumentType", "ECMDocument");
        tags.put("Status", document.getStatus().toString());

        if (document.getFolderId() != null) {
            tags.put("FolderId", document.getFolderId().toString());
        }

        // Add custom metadata as tags
        if (document.getMetadata() != null) {
            document.getMetadata().forEach((key, value) -> {
                if (value != null) {
                    tags.put("meta-" + key, value.toString());
                }
            });
        }

        return tags;
    }

    /**
     * Applies S3 tags to an object.
     */
    private void applyS3Tags(String s3Key, Map<String, String> tags) {
        if (tags.isEmpty()) {
            return;
        }

        Set<Tag> s3Tags = tags.entrySet().stream()
            .map(entry -> Tag.builder()
                .key(entry.getKey())
                .value(entry.getValue())
                .build())
            .collect(Collectors.toSet());

        Tagging tagging = Tagging.builder()
            .tagSet(s3Tags)
            .build();

        PutObjectTaggingRequest request = PutObjectTaggingRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .tagging(tagging)
            .build();

        s3Client.putObjectTagging(request);
    }

    /**
     * Applies S3-specific features to put request.
     */
    private void applyS3Features(PutObjectRequest.Builder requestBuilder) {
        // Server-side encryption
        String encryption = ecmProperties.getAdapterPropertyAsString("encryption");
        if ("AES256".equals(encryption)) {
            requestBuilder.serverSideEncryption(ServerSideEncryption.AES256);
        } else if ("aws:kms".equals(encryption)) {
            requestBuilder.serverSideEncryption(ServerSideEncryption.AWS_KMS);
        }

        // Storage class
        String storageClass = ecmProperties.getAdapterPropertyAsString("storage-class");
        if (storageClass != null) {
            requestBuilder.storageClass(StorageClass.fromValue(storageClass));
        }
    }

    /**
     * Builds Document object from S3 metadata.
     */
    private Document buildDocumentFromS3Metadata(UUID documentId, String s3Key, HeadObjectResponse response) {
        Map<String, String> metadata = response.metadata();

        return Document.builder()
            .id(documentId)
            .name(metadata.get("firefly-document-name"))
            .mimeType(response.contentType())
            .size(response.contentLength())
            .storagePath(s3Key)
            .status(DocumentStatus.valueOf(metadata.getOrDefault("firefly-document-status", "ACTIVE")))
            .description(metadata.get("firefly-description"))
            .folderId(parseUUID(metadata.get("firefly-folder-id")))
            .createdBy(parseLong(metadata.get("firefly-created-by")))
            .modifiedBy(parseLong(metadata.get("firefly-modified-by")))
            .checksum(metadata.get("firefly-checksum"))
            .checksumAlgorithm(metadata.get("firefly-checksum-algorithm"))
            .createdAt(parseInstant(metadata.get("firefly-created-at")))
            .modifiedAt(response.lastModified())
            .build();
    }

    /**
     * Calculates checksum for content integrity verification.
     */
    private String calculateChecksum(byte[] content, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(content);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Checksum algorithm not available: " + algorithm, e);
        }
    }

    /**
     * Extracts file extension from filename.
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }

        return "";
    }

    // Utility parsing methods
    private UUID parseUUID(String value) {
        try {
            return value != null ? UUID.fromString(value) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Long parseLong(String value) {
        try {
            return value != null ? Long.parseLong(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Instant parseInstant(String value) {
        try {
            return value != null ? Instant.parse(value) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
```

### 5.3 S3 Document Content Adapter

Create the content adapter for handling binary content:

```java
// src/main/java/com/example/ecm/adapter/S3DocumentContentAdapter.java
package com.example.ecm.adapter;

import com.firefly.core.ecm.adapter.EcmAdapter;
import com.firefly.core.ecm.adapter.AdapterFeature;
import com.firefly.core.ecm.config.EcmProperties;
import com.firefly.core.ecm.port.document.DocumentContentPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Amazon S3 implementation of DocumentContentPort.
 *
 * This adapter handles binary content operations:
 * - Streaming content download
 * - Byte array content retrieval
 * - Range requests for partial content
 * - Content validation and integrity checks
 */
@Slf4j
@EcmAdapter(
    type = "s3-content",
    description = "Amazon S3 Document Content Adapter",
    supportedFeatures = {
        AdapterFeature.CONTENT_STORAGE,
        AdapterFeature.STREAMING
    }
)
@Component
@ConditionalOnProperty(name = "firefly.ecm.adapter-type", havingValue = "s3")
public class S3DocumentContentAdapter implements DocumentContentPort {

    private final S3Client s3Client;
    private final EcmProperties ecmProperties;
    private final String bucketName;
    private final String pathPrefix;
    private final DataBufferFactory dataBufferFactory;

    @Autowired
    public S3DocumentContentAdapter(S3Client s3Client, EcmProperties ecmProperties) {
        this.s3Client = s3Client;
        this.ecmProperties = ecmProperties;
        this.bucketName = ecmProperties.getAdapterPropertyAsString("bucket-name");
        this.pathPrefix = ecmProperties.getAdapterPropertyAsString("path-prefix");
        this.dataBufferFactory = DefaultDataBufferFactory.sharedInstance;

        log.info("S3DocumentContentAdapter initialized for bucket: {}", bucketName);
    }

    @Override
    public Flux<DataBuffer> getContentStream(UUID documentId) {
        return Mono.fromCallable(() -> {
            log.debug("Starting content stream for document: {}", documentId);

            String s3Key = findS3KeyByDocumentId(documentId);

            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

            return s3Client.getObject(request);

        }).subscribeOn(Schedulers.boundedElastic())
          .flatMapMany(responseInputStream -> {
              // Convert InputStream to reactive DataBuffer stream
              return DataBufferUtils.readInputStream(
                  () -> responseInputStream,
                  dataBufferFactory,
                  8192  // 8KB buffer size
              );
          })
          .doOnSubscribe(subscription ->
              log.debug("Content stream started for document: {}", documentId))
          .doOnComplete(() ->
              log.debug("Content stream completed for document: {}", documentId))
          .doOnError(error ->
              log.error("Content stream failed for document: {}", documentId, error));
    }

    @Override
    public Mono<byte[]> getContent(UUID documentId) {
        return Mono.fromCallable(() -> {
            log.debug("Retrieving content for document: {}", documentId);

            String s3Key = findS3KeyByDocumentId(documentId);

            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

            try (ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(request);
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = responseInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                byte[] content = outputStream.toByteArray();
                log.debug("Content retrieved for document: {} (size: {} bytes)", documentId, content.length);

                return content;

            } catch (IOException e) {
                throw new RuntimeException("Failed to read content from S3", e);
            }

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to get content for document: {}", documentId, error))
          .onErrorReturn(NoSuchKeyException.class, new byte[0]);
    }

    @Override
    public Flux<DataBuffer> getContentRange(UUID documentId, long start, long end) {
        return Mono.fromCallable(() -> {
            log.debug("Retrieving content range for document: {} (bytes {}-{})", documentId, start, end);

            String s3Key = findS3KeyByDocumentId(documentId);

            // S3 range format: "bytes=start-end"
            String range = String.format("bytes=%d-%d", start, end);

            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .range(range)
                .build();

            return s3Client.getObject(request);

        }).subscribeOn(Schedulers.boundedElastic())
          .flatMapMany(responseInputStream -> {
              return DataBufferUtils.readInputStream(
                  () -> responseInputStream,
                  dataBufferFactory,
                  8192
              );
          })
          .doOnSubscribe(subscription ->
              log.debug("Content range stream started for document: {} (bytes {}-{})", documentId, start, end))
          .doOnComplete(() ->
              log.debug("Content range stream completed for document: {}", documentId))
          .doOnError(error ->
              log.error("Content range stream failed for document: {}", documentId, error));
    }

    @Override
    public Mono<Long> getContentSize(UUID documentId) {
        return Mono.fromCallable(() -> {
            log.debug("Getting content size for document: {}", documentId);

            String s3Key = findS3KeyByDocumentId(documentId);

            HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

            HeadObjectResponse response = s3Client.headObject(request);
            Long size = response.contentLength();

            log.debug("Content size for document {}: {} bytes", documentId, size);
            return size;

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to get content size for document: {}", documentId, error))
          .onErrorReturn(NoSuchKeyException.class, 0L);
    }

    @Override
    public Mono<String> getContentType(UUID documentId) {
        return Mono.fromCallable(() -> {
            log.debug("Getting content type for document: {}", documentId);

            String s3Key = findS3KeyByDocumentId(documentId);

            HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

            HeadObjectResponse response = s3Client.headObject(request);
            String contentType = response.contentType();

            log.debug("Content type for document {}: {}", documentId, contentType);
            return contentType;

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to get content type for document: {}", documentId, error))
          .onErrorReturn(NoSuchKeyException.class, "application/octet-stream");
    }

    @Override
    public Mono<Boolean> hasContent(UUID documentId) {
        return Mono.fromCallable(() -> {
            try {
                String s3Key = findS3KeyByDocumentId(documentId);

                HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

                s3Client.headObject(request);
                return true;

            } catch (NoSuchKeyException e) {
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to check content existence for document: {}", documentId, error))
          .onErrorReturn(false);
    }

    /**
     * Finds S3 key for a document ID.
     * In production, this should be cached or stored in a database.
     */
    private String findS3KeyByDocumentId(UUID documentId) {
        String prefix = pathPrefix != null ? pathPrefix : "";
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix += "/";
        }

        String searchPrefix = prefix + "documents/" + documentId;

        ListObjectsV2Request request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(searchPrefix)
            .maxKeys(1)
            .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        if (response.contents().isEmpty()) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        return response.contents().get(0).key();
    }
}
```

## 6. Service Layer Implementation

Create comprehensive service classes that use the S3 adapters:

### 6.1 Document Service

```java
// src/main/java/com/example/ecm/service/DocumentService.java
package com.example.ecm.service;

import com.firefly.core.ecm.domain.model.document.Document;
import com.firefly.core.ecm.domain.enums.document.DocumentStatus;
import com.firefly.core.ecm.port.document.DocumentPort;
import com.firefly.core.ecm.port.document.DocumentContentPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for document operations.
 * Provides business logic and validation on top of the ECM ports.
 */
@Slf4j
@Service
public class DocumentService {

    @Autowired
    private DocumentPort documentPort;

    @Autowired
    private DocumentContentPort documentContentPort;

    /**
     * Uploads a document from a multipart file.
     */
    public Mono<Document> uploadDocument(FilePart filePart, String description, UUID folderId) {
        return filePart.content()
            .collectList()
            .map(this::convertToByteArray)
            .flatMap(content -> {
                // Validate file
                validateFile(filePart.filename(), content);

                // Create document
                Document document = Document.builder()
                    .name(filePart.filename())
                    .mimeType(filePart.headers().getContentType().toString())
                    .description(description)
                    .folderId(folderId)
                    .status(DocumentStatus.ACTIVE)
                    .size((long) content.length)
                    .createdAt(Instant.now())
                    .build();

                return documentPort.createDocument(document, content);
            })
            .doOnSuccess(doc -> log.info("Document uploaded: {} ({})", doc.getId(), doc.getName()))
            .doOnError(error -> log.error("Failed to upload document: {}", filePart.filename(), error));
    }

    /**
     * Gets document metadata.
     */
    public Mono<Document> getDocument(UUID documentId) {
        return documentPort.getDocument(documentId)
            .doOnNext(doc -> log.debug("Retrieved document: {} ({})", doc.getId(), doc.getName()))
            .switchIfEmpty(Mono.error(new RuntimeException("Document not found: " + documentId)));
    }

    /**
     * Downloads document content as a stream.
     */
    public Flux<DataBuffer> downloadDocument(UUID documentId) {
        return documentContentPort.getContentStream(documentId)
            .doOnSubscribe(sub -> log.info("Starting download for document: {}", documentId))
            .doOnComplete(() -> log.info("Download completed for document: {}", documentId));
    }

    /**
     * Updates document metadata.
     */
    public Mono<Document> updateDocument(UUID documentId, String name, String description) {
        return documentPort.getDocument(documentId)
            .flatMap(document -> {
                Document updatedDocument = document.toBuilder()
                    .name(name != null ? name : document.getName())
                    .description(description != null ? description : document.getDescription())
                    .modifiedAt(Instant.now())
                    .build();

                return documentPort.updateDocument(updatedDocument);
            })
            .doOnSuccess(doc -> log.info("Document updated: {} ({})", doc.getId(), doc.getName()));
    }

    /**
     * Deletes a document.
     */
    public Mono<Void> deleteDocument(UUID documentId) {
        return documentPort.deleteDocument(documentId)
            .doOnSuccess(unused -> log.info("Document deleted: {}", documentId));
    }

    /**
     * Lists documents in a folder.
     */
    public Flux<Document> listDocumentsInFolder(UUID folderId) {
        return documentPort.findDocumentsByFolder(folderId)
            .doOnSubscribe(sub -> log.debug("Listing documents in folder: {}", folderId));
    }

    /**
     * Searches documents by name.
     */
    public Flux<Document> searchDocuments(String query) {
        return documentPort.findAllDocuments()
            .filter(doc -> doc.getName().toLowerCase().contains(query.toLowerCase()))
            .doOnSubscribe(sub -> log.debug("Searching documents with query: {}", query));
    }

    // Helper methods
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

    private void validateFile(String filename, byte[] content) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename is required");
        }

        if (content.length == 0) {
            throw new IllegalArgumentException("File content cannot be empty");
        }

        // Add more validation as needed
        long maxSizeMB = 100; // From configuration
        if (content.length > maxSizeMB * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum allowed: " + maxSizeMB + "MB");
        }
    }
}
```

## 7. REST API Implementation

Create REST controllers for the ECM functionality:

```java
// src/main/java/com/example/ecm/controller/DocumentController.java
package com.example.ecm.controller;

import com.example.ecm.service.DocumentService;
import com.firefly.core.ecm.domain.model.document.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for document operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    /**
     * Upload a document.
     *
     * Example:
     * curl -X POST -F "file=@document.pdf" -F "description=My document" \
     *      http://localhost:8080/api/documents/upload
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Document>> uploadDocument(
            @RequestPart("file") FilePart file,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "folderId", required = false) String folderId) {

        UUID folderUUID = folderId != null ? UUID.fromString(folderId) : null;

        return documentService.uploadDocument(file, description, folderUUID)
            .map(document -> ResponseEntity.status(HttpStatus.CREATED).body(document))
            .onErrorReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }

    /**
     * Get document metadata.
     */
    @GetMapping("/{documentId}")
    public Mono<ResponseEntity<Document>> getDocument(@PathVariable UUID documentId) {
        return documentService.getDocument(documentId)
            .map(document -> ResponseEntity.ok(document))
            .onErrorReturn(ResponseEntity.notFound().build());
    }

    /**
     * Download document content.
     */
    @GetMapping("/{documentId}/download")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadDocument(@PathVariable UUID documentId) {
        return documentService.getDocument(documentId)
            .map(document -> {
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION,
                           "attachment; filename=\"" + document.getName() + "\"");
                headers.add(HttpHeaders.CONTENT_TYPE, document.getMimeType());
                headers.add(HttpHeaders.CONTENT_LENGTH, document.getSize().toString());

                Flux<DataBuffer> content = documentService.downloadDocument(documentId);
                return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
            })
            .onErrorReturn(ResponseEntity.notFound().build());
    }

    /**
     * Update document metadata.
     */
    @PutMapping("/{documentId}")
    public Mono<ResponseEntity<Document>> updateDocument(
            @PathVariable UUID documentId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description) {

        return documentService.updateDocument(documentId, name, description)
            .map(document -> ResponseEntity.ok(document))
            .onErrorReturn(ResponseEntity.notFound().build());
    }

    /**
     * Delete document.
     */
    @DeleteMapping("/{documentId}")
    public Mono<ResponseEntity<Void>> deleteDocument(@PathVariable UUID documentId) {
        return documentService.deleteDocument(documentId)
            .then(Mono.just(ResponseEntity.noContent().<Void>build()))
            .onErrorReturn(ResponseEntity.notFound().build());
    }

    /**
     * Search documents.
     */
    @GetMapping("/search")
    public Flux<Document> searchDocuments(@RequestParam String query) {
        return documentService.searchDocuments(query);
    }

    /**
     * List documents in folder.
     */
    @GetMapping("/folder/{folderId}")
    public Flux<Document> listDocumentsInFolder(@PathVariable UUID folderId) {
        return documentService.listDocumentsInFolder(folderId);
    }
}
```

## 8. Testing

### 8.1 Integration Tests

```java
// src/test/java/com/example/ecm/S3IntegrationTest.java
package com.example.ecm;

import com.example.ecm.service.DocumentService;
import com.firefly.core.ecm.domain.model.document.Document;
import com.firefly.core.ecm.domain.enums.document.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "firefly.ecm.adapter-type=s3",
    "firefly.ecm.properties.bucket-name=test-bucket-${random.uuid}",
    "firefly.ecm.properties.region=us-east-1"
})
class S3IntegrationTest {

    @Autowired
    private DocumentService documentService;

    @Test
    void testCompleteDocumentWorkflow() {
        byte[] content = "Test document content for S3 integration".getBytes();

        // Test document creation, retrieval, update, and deletion
        StepVerifier.create(
            // Create document
            documentService.uploadDocument(createFilePart("test.txt", content), "Test document", null)
                .flatMap(createdDoc -> {
                    // Verify creation
                    assertThat(createdDoc.getId()).isNotNull();
                    assertThat(createdDoc.getName()).isEqualTo("test.txt");
                    assertThat(createdDoc.getStatus()).isEqualTo(DocumentStatus.ACTIVE);

                    // Retrieve document
                    return documentService.getDocument(createdDoc.getId());
                })
                .flatMap(retrievedDoc -> {
                    // Verify retrieval
                    assertThat(retrievedDoc.getName()).isEqualTo("test.txt");

                    // Update document
                    return documentService.updateDocument(
                        retrievedDoc.getId(),
                        "updated-test.txt",
                        "Updated description"
                    );
                })
                .flatMap(updatedDoc -> {
                    // Verify update
                    assertThat(updatedDoc.getName()).isEqualTo("updated-test.txt");
                    assertThat(updatedDoc.getDescription()).isEqualTo("Updated description");

                    // Delete document
                    return documentService.deleteDocument(updatedDoc.getId())
                        .then(Mono.just(updatedDoc));
                })
        )
        .expectNextCount(1)
        .expectTimeout(Duration.ofMinutes(2))
        .verifyComplete();
    }

    private FilePart createFilePart(String filename, byte[] content) {
        // Implementation depends on your testing framework
        // This is a simplified example
        return new MockFilePart(filename, content);
    }
}
```

## 9. Production Deployment

### 9.1 Security Configuration

```yaml
# application-prod.yml
firefly:
  ecm:
    properties:
      bucket-name: "${ECM_S3_BUCKET}"
      region: "${AWS_REGION}"
      encryption: "AES256"
      storage-class: "STANDARD"
    defaults:
      max-file-size-mb: 500
    performance:
      cache-enabled: true
      compression-enabled: true

# Use IAM roles instead of access keys
# No AWS credentials in configuration files
```

### 9.2 Monitoring and Health Checks

```java
// Custom health indicator for S3
@Component
public class S3HealthIndicator implements HealthIndicator {

    @Autowired
    private S3Client s3Client;

    @Override
    public Health health() {
        try {
            s3Client.listBuckets();
            return Health.up()
                .withDetail("s3", "Available")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("s3", "Unavailable")
                .withException(e)
                .build();
        }
    }
}
```

## 10. Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `NoCredentialsException` | AWS credentials not configured | Set up environment variables or IAM roles |
| `NoSuchBucket` | Bucket doesn't exist or wrong name | Verify bucket name and region |
| `AccessDenied` | Insufficient IAM permissions | Check IAM user/role permissions |
| `Connection timeout` | Network or region issues | Verify AWS region and network connectivity |
| `Large file upload fails` | File size exceeds limits | Configure multipart upload settings |

## Next Steps

- [Implement folder management](../examples/folder-management.md)
- [Add document versioning](../examples/document-versioning.md)
- [Set up search functionality](../examples/document-search.md)
- [Configure audit logging](../examples/audit-configuration.md)
- [DocuSign Integration](docusign-integration.md)
- [Alfresco Integration](alfresco-integration.md)
```

### REST Controller

```java
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    
    @Autowired
    private DocumentService documentService;
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Document>> uploadDocument(
            @RequestPart("file") FilePart file) {
        
        return file.content()
            .collectList()
            .map(this::convertToByteArray)
            .flatMap(content -> documentService.uploadDocument(
                file.filename(),
                content,
                file.headers().getContentType().toString()
            ))
            .map(document -> ResponseEntity.status(HttpStatus.CREATED).body(document))
            .onErrorReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }
    
    @GetMapping("/{documentId}")
    public Mono<ResponseEntity<Document>> getDocument(@PathVariable UUID documentId) {
        return documentService.getDocument(documentId)
            .map(document -> ResponseEntity.ok(document))
            .onErrorReturn(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{documentId}/download")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadDocument(@PathVariable UUID documentId) {
        return documentService.getDocument(documentId)
            .map(document -> {
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + document.getName() + "\"");
                headers.add(HttpHeaders.CONTENT_TYPE, document.getMimeType());
                
                Flux<DataBuffer> content = documentService.downloadDocument(documentId);
                return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
            })
            .onErrorReturn(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{documentId}")
    public Mono<ResponseEntity<Void>> deleteDocument(@PathVariable UUID documentId) {
        return documentService.deleteDocument(documentId)
            .then(Mono.just(ResponseEntity.noContent().<Void>build()))
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

## 5. Testing

### Integration Test

```java
@SpringBootTest
@TestPropertySource(properties = {
    "firefly.ecm.adapter-type=s3",
    "firefly.ecm.properties.bucket-name=test-bucket",
    "firefly.ecm.properties.region=us-east-1"
})
class S3IntegrationTest {
    
    @Autowired
    private DocumentPort documentPort;
    
    @Test
    void testDocumentUploadAndRetrieval() {
        Document document = Document.builder()
            .name("test-document.pdf")
            .mimeType("application/pdf")
            .status(DocumentStatus.ACTIVE)
            .build();
        
        byte[] content = "Test PDF content".getBytes();
        
        StepVerifier.create(documentPort.createDocument(document, content))
            .assertNext(createdDoc -> {
                assertThat(createdDoc.getId()).isNotNull();
                assertThat(createdDoc.getStoragePath()).isNotEmpty();
                assertThat(createdDoc.getSize()).isEqualTo(content.length);
            })
            .verifyComplete();
    }
}
```

## 6. Production Considerations

### Security
- Use IAM roles instead of access keys in production
- Enable S3 bucket encryption
- Configure proper bucket policies
- Enable CloudTrail for audit logging

### Performance
- Use multipart uploads for large files
- Configure appropriate storage classes
- Enable S3 Transfer Acceleration if needed
- Implement proper retry logic

### Monitoring
- Set up CloudWatch metrics
- Configure S3 access logging
- Monitor costs and usage patterns

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `NoCredentialsException` | Check AWS credentials configuration |
| `NoSuchBucket` | Verify bucket name and region |
| `AccessDenied` | Check IAM permissions |
| Connection timeouts | Verify network connectivity and AWS region |

## Next Steps

- [Configure folder management](../examples/folder-management.md)
- [Set up document versioning](../examples/document-versioning.md)
- [Implement search functionality](../examples/document-search.md)
- [Add audit logging](../examples/audit-configuration.md)
