# DocuSign Integration Guide

This comprehensive guide demonstrates how to integrate DocuSign for digital signatures with the Firefly ECM Library. You'll learn to build a complete eSignature adapter that implements the ECM signature ports and provides full DocuSign functionality.

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Project Setup](#2-project-setup)
3. [DocuSign Developer Account Setup](#3-docusign-developer-account-setup)
4. [Application Configuration](#4-application-configuration)
5. [DocuSign Adapter Implementation](#5-docusign-adapter-implementation)
6. [Service Layer Implementation](#6-service-layer-implementation)
7. [REST API Implementation](#7-rest-api-implementation)
8. [Testing](#8-testing)
9. [Production Deployment](#9-production-deployment)
10. [Troubleshooting](#10-troubleshooting)

## 1. Prerequisites

Before starting, ensure you have:

- **DocuSign Developer Account** with API access
- **Java 17+** installed and configured
- **Spring Boot 3.0+** knowledge
- **Maven 3.6+** or **Gradle 7.0+**
- **Firefly ECM Library** understanding
- **Document storage system** (S3, file system, etc.) for storing documents to be signed
- **Basic understanding of JWT authentication**
- **SSL/TLS certificates** for production webhook endpoints

## 2. Project Setup

### 2.1 Maven Dependencies

Add the required dependencies to your `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>firefly-ecm-docusign-demo</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <docusign.version>4.2.0</docusign.version>
        <jwt.version>0.11.5</jwt.version>
    </properties>

    <dependencies>
        <!-- Firefly ECM Library -->
        <dependency>
            <groupId>com.firefly</groupId>
            <artifactId>lib-ecm</artifactId>
            <version>1.0.0</version>
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
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- DocuSign Java SDK -->
        <dependency>
            <groupId>com.docusign</groupId>
            <artifactId>docusign-esign-java</artifactId>
            <version>${docusign.version}</version>
        </dependency>

        <!-- JWT Authentication Libraries -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Reactive Streams -->
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-core</artifactId>
        </dependency>
        <dependency>
            <groupId>io.projectreactor.addons</groupId>
            <artifactId>reactor-extra</artifactId>
        </dependency>

        <!-- JSON Processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <!-- Apache Commons for utilities -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>2.11.0</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.wiremock</groupId>
            <artifactId>wiremock-standalone</artifactId>
            <version>3.0.1</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2.2 Project Structure

Create the following directory structure:

```
src/
├── main/
│   ├── java/
│   │   └── com/example/ecm/
│   │       ├── EcmDocuSignDemoApplication.java
│   │       ├── adapter/
│   │       │   ├── DocuSignSignatureEnvelopeAdapter.java
│   │       │   ├── DocuSignSignatureRequestAdapter.java
│   │       │   └── DocuSignWebhookHandler.java
│   │       ├── service/
│   │       │   ├── SignatureService.java
│   │       │   ├── EnvelopeService.java
│   │       │   └── DocumentService.java
│   │       ├── controller/
│   │       │   ├── SignatureController.java
│   │       │   ├── EnvelopeController.java
│   │       │   └── WebhookController.java
│   │       ├── config/
│   │       │   ├── DocuSignConfiguration.java
│   │       │   └── SecurityConfiguration.java
│   │       ├── dto/
│   │       │   ├── EnvelopeCreateRequest.java
│   │       │   ├── SignatureFieldRequest.java
│   │       │   └── WebhookEventDto.java
│   │       └── util/
│   │           ├── DocuSignMapper.java
│   │           └── JwtTokenGenerator.java
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       ├── docusign-private.key
│       └── logback-spring.xml
└── test/
    └── java/
        └── com/example/ecm/
            ├── adapter/
            ├── service/
            └── integration/
```

## 3. DocuSign Developer Account Setup

Setting up a DocuSign developer account is essential for testing and development. DocuSign provides a comprehensive sandbox environment that mirrors production capabilities.

### 3.1 Create DocuSign Developer Account

**Step 1: Register for Developer Account**

1. **Visit DocuSign Developer Center**:
   - Go to [developers.docusign.com](https://developers.docusign.com/)
   - Click "Get a Developer Account"
   - Fill out the registration form with accurate information
   - Use a valid email address (you'll need to verify it)

2. **Complete Email Verification**:
   - Check your email for verification link
   - Click the verification link to activate your account
   - Set up your password and security questions

3. **Access Developer Dashboard**:
   - Log into [account-d.docusign.com](https://account-d.docusign.com) (demo environment)
   - This is your sandbox environment separate from production
   - Familiarize yourself with the interface

### 3.2 Create Integration Application

**Step 1: Navigate to Apps and Keys**

1. **Access Admin Panel**:
   - In your DocuSign demo account, click on your profile (top-right)
   - Select "Admin" from the dropdown menu
   - In the left sidebar, click "Apps and Keys"

2. **Understand the Interface**:
   - "My Apps & Keys" section shows your applications
   - Each application has an Integration Key (like an API key)
   - Applications can have multiple authentication methods

**Step 2: Create New Application**

1. **Add New App**:
   - Click "Add App and Integration Key"
   - Fill out the application details:
     - **App Name**: "Firefly ECM Signature Integration"
     - **Description**: "Integration between Firefly ECM and DocuSign for digital signatures"
     - **App Type**: Select "Server Application" (for backend integration)

2. **Save and Note Integration Key**:
   - After creating the app, you'll see an "Integration Key"
   - **CRITICAL**: Copy and securely save this Integration Key
   - Format: `12345678-1234-1234-1234-123456789012`
   - This is your application's unique identifier

### 3.3 Generate RSA Key Pair for JWT Authentication

DocuSign uses JWT (JSON Web Tokens) with RSA public/private key pairs for secure authentication. This is more secure than basic authentication.

**Step 1: Generate Private Key**

```bash
# Create a secure directory for keys
mkdir -p ~/.docusign/keys
cd ~/.docusign/keys

# Generate a 2048-bit RSA private key
openssl genrsa -out docusign-private.key 2048

# Verify the private key was created correctly
openssl rsa -in docusign-private.key -check -noout
# Should output: "RSA key ok"

# Set secure permissions (Unix/Linux/Mac only)
chmod 600 docusign-private.key

# Display the private key (you'll need this for your application)
cat docusign-private.key
```

**Step 2: Generate Public Key**

```bash
# Generate the corresponding public key from the private key
openssl rsa -in docusign-private.key -pubout -out docusign-public.key

# Verify the public key
openssl rsa -pubin -in docusign-public.key -text -noout

# Display the public key (you'll upload this to DocuSign)
cat docusign-public.key
```

**Step 3: Understand Key Usage**

- **Private Key**: Stays on your server, used to sign JWT tokens
- **Public Key**: Uploaded to DocuSign, used to verify your JWT tokens
- **Security**: Never share your private key; only the public key goes to DocuSign

### 3.4 Configure Application Authentication

**Step 1: Add RSA Public Key to DocuSign**

1. **Access Service Integration**:
   - In your DocuSign app settings, find "Service Integration" section
   - Look for "RSA Keypairs" or "Authentication"

2. **Upload Public Key**:
   - Click "Add RSA Keypair"
   - Copy the **entire content** of `docusign-public.key`
   - Include the `-----BEGIN PUBLIC KEY-----` and `-----END PUBLIC KEY-----` lines
   - Paste into the text area
   - Click "Add" or "Save"

3. **Verify Upload**:
   - You should see the key listed with a fingerprint
   - Note the key ID if provided (some versions show this)

**Step 2: Configure Redirect URIs**

Even though we'll use JWT authentication, DocuSign requires redirect URIs:

1. **Add Redirect URIs**:
   - In "Additional settings" section, find "Redirect URIs"
   - Add these URIs:
     - `http://localhost:8080/auth/callback`
     - `http://localhost:8080/auth/success`
     - `https://yourdomain.com/auth/callback` (for production)

2. **Understand URI Usage**:
   - These are used for OAuth flows (not JWT)
   - Required by DocuSign even if not used
   - Useful for testing embedded signing

**Step 3: Set Application Permissions**

1. **Configure Scopes**:
   - Ensure your app has these scopes enabled:
     - `signature` - Send and manage envelopes
     - `impersonation` - Act on behalf of users
   - These should be enabled by default for new applications

2. **Understand Permissions**:
   - `signature`: Allows creating, sending, and managing envelopes
   - `impersonation`: Allows the app to act as the authenticated user

### 3.5 Collect Required Information

**Step 1: Get User and Account IDs**

1. **Find User ID**:
   - In DocuSign Admin, go to "My Account"
   - Look for "User ID" (format: `12345678-1234-1234-1234-123456789012`)
   - Copy and save this value

2. **Find Account ID**:
   - In the same section, look for "Account ID"
   - Format: `12345678-1234-1234-1234-123456789012`
   - This identifies your DocuSign account

3. **Find Base URL**:
   - Note the base URL for API calls
   - Demo: `https://demo.docusign.net/restapi`
   - Production: `https://na1.docusign.net/restapi` (varies by region)

**Step 2: Grant Application Consent**

Before your application can make API calls, you need to grant it permission:

1. **Generate Consent URL**:
   Replace `{INTEGRATION_KEY}` with your actual integration key:
   ```
   https://account-d.docusign.com/oauth/auth?response_type=code&scope=signature%20impersonation&client_id={INTEGRATION_KEY}&redirect_uri=http://localhost:8080/auth/callback
   ```

2. **Grant Consent**:
   - Open the URL in your browser
   - Log in with your DocuSign developer account
   - Review the permissions requested
   - Click "Allow" to grant permissions
   - You'll be redirected to your callback URL (which will show an error - that's expected)

3. **Verify Consent**:
   - Back in DocuSign Admin → Apps and Keys
   - Your app should show "Individual Consent Granted"
   - This consent lasts until revoked

### 3.6 Configuration Summary

You should now have these values (save them securely):

```bash
# DocuSign Configuration Values
DOCUSIGN_INTEGRATION_KEY=12345678-1234-1234-1234-123456789012
DOCUSIGN_USER_ID=12345678-1234-1234-1234-123456789012
DOCUSIGN_ACCOUNT_ID=12345678-1234-1234-1234-123456789012

# Environment URLs
DOCUSIGN_BASE_URL=https://demo.docusign.net/restapi
DOCUSIGN_OAUTH_BASE_URL=https://account-d.docusign.com

# Files (copy to your project)
DOCUSIGN_PRIVATE_KEY_PATH=src/main/resources/docusign-private.key
```

**Step 3: Copy Private Key to Project**

```bash
# Copy the private key to your project resources
cp ~/.docusign/keys/docusign-private.key src/main/resources/

# Verify the file is in place
ls -la src/main/resources/docusign-private.key

# Add to .gitignore to prevent committing to version control
echo "src/main/resources/docusign-private.key" >> .gitignore
```

> **🔒 Security Warning**: Never commit private keys to version control. Use environment variables or secure configuration management in production.

## 4. Application Configuration

Now we'll configure the application to integrate DocuSign with the Firefly ECM Library. This configuration supports both document storage and digital signatures.

### 4.1 Main Application Configuration

Create `src/main/resources/application.yml` with comprehensive configuration:

```yaml
# Spring Boot Configuration
spring:
  application:
    name: "firefly-ecm-docusign-demo"

  # WebFlux configuration for reactive processing
  webflux:
    multipart:
      max-in-memory-size: 10MB
      max-disk-usage-per-part: 100MB
      max-parts: 128
      max-headers-size: 10KB

  # Jackson configuration for JSON processing
  jackson:
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false
    default-property-inclusion: non_null

# Server configuration
server:
  port: 8080
  error:
    include-message: always
    include-binding-errors: always
  # SSL configuration for production webhooks
  ssl:
    enabled: false  # Set to true in production
    # key-store: classpath:keystore.p12
    # key-store-password: ${SSL_KEYSTORE_PASSWORD}
    # key-store-type: PKCS12

# Firefly ECM Configuration
firefly:
  ecm:
    # Enable ECM functionality
    enabled: true

    # Primary adapter for document storage (you need a document storage system)
    # This example assumes you have S3 configured, but you can use any storage adapter
    adapter-type: "s3"  # or "filesystem", "alfresco", etc.

    # Document storage configuration (example with S3)
    properties:
      bucket-name: "${ECM_S3_BUCKET:firefly-ecm-documents-dev}"
      region: "${AWS_REGION:us-east-1}"
      path-prefix: "documents/"

    # Feature flags - enable eSignature functionality
    features:
      document-management: true         # Basic document operations
      content-storage: true             # Binary content storage
      versioning: true                  # Document versioning
      folder-management: true           # Folder operations
      permissions: true                 # Access control
      search: true                      # Document search
      auditing: true                    # Audit logging
      esignature: true                  # Enable eSignature features

    # eSignature configuration
    esignature:
      # Specify DocuSign as the eSignature provider
      provider: "docusign"

      # DocuSign-specific configuration
      properties:
        # Authentication credentials (from environment variables for security)
        integration-key: "${DOCUSIGN_INTEGRATION_KEY}"
        user-id: "${DOCUSIGN_USER_ID}"
        account-id: "${DOCUSIGN_ACCOUNT_ID}"

        # DocuSign API endpoints
        # Demo environment (for development and testing)
        base-url: "https://demo.docusign.net/restapi"
        oauth-base-url: "https://account-d.docusign.com"

        # Production environment (uncomment for production)
        # base-url: "https://na1.docusign.net/restapi"  # Varies by region
        # oauth-base-url: "https://account.docusign.com"

        # Private key for JWT authentication
        private-key-path: "classpath:docusign-private.key"

        # JWT token settings
        token-expiration-hours: 1         # Tokens expire after 1 hour
        token-refresh-threshold-minutes: 10  # Refresh token 10 minutes before expiry

        # Default envelope settings
        default-envelope-subject: "Please sign this document"
        default-envelope-message: "Please review and sign the attached document."
        default-envelope-expiration-days: 30  # Envelopes expire after 30 days

        # Webhook settings for receiving status updates
        webhook-url: "https://yourdomain.com/api/webhooks/docusign"
        webhook-enabled: false            # Disable for local development
        webhook-secret: "${DOCUSIGN_WEBHOOK_SECRET:}"  # For webhook verification

        # API timeout and retry settings
        api-timeout-seconds: 30           # API call timeout
        connection-timeout-seconds: 10    # Connection timeout
        max-retry-attempts: 3             # Number of retry attempts
        retry-delay-seconds: 2            # Delay between retries

        # Embedded signing settings
        embedded-signing-enabled: true    # Enable embedded signing
        embedded-return-url: "http://localhost:8080/api/signatures/signing-complete"

        # Advanced features
        enable-envelope-purging: false    # Auto-purge completed envelopes
        purge-delay-days: 90             # Days to wait before purging
        enable-bulk-operations: true     # Enable bulk envelope operations

    # Default settings for document and signature operations
    defaults:
      max-file-size-mb: 100             # Maximum file size for documents
      allowed-extensions:               # Allowed file types for signing
        - "pdf"
        - "doc"
        - "docx"
        - "txt"
        - "jpg"
        - "png"
      blocked-extensions:               # Blocked file types
        - "exe"
        - "bat"
        - "cmd"
        - "scr"
      checksum-algorithm: "SHA-256"     # File integrity verification

    # Performance optimization
    performance:
      batch-size: 50                    # Batch size for bulk operations
      cache-enabled: true               # Enable metadata caching
      cache-expiration: "PT30M"         # Cache expiration (30 minutes)
      compression-enabled: true         # Enable content compression
      async-pool-size: 10               # Thread pool for async operations

# Logging configuration
logging:
  level:
    # ECM library logging
    com.firefly.core.ecm: INFO
    com.example.ecm: DEBUG

    # DocuSign SDK logging
    com.docusign: INFO
    com.docusign.esign.client: DEBUG   # Enable for API debugging

    # HTTP client logging (for debugging DocuSign API calls)
    # org.apache.http: DEBUG
    # org.apache.http.wire: DEBUG

    # Spring framework
    org.springframework.web: INFO
    org.springframework.security: INFO

    # Root logger
    root: INFO

  # Log pattern for better readability
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# Actuator endpoints for monitoring
management:
  endpoints:
    web:
      exposure:
        include: "health,info,metrics,env,configprops,docusign"
      base-path: "/actuator"
  endpoint:
    health:
      show-details: always
      show-components: always
    env:
      show-values: when-authorized

  # Custom health indicators
  health:
    docusign:
      enabled: true
    diskspace:
      enabled: true
      threshold: 10GB

# Application information
info:
  app:
    name: "Firefly ECM DocuSign Demo"
    description: "Demonstration of Firefly ECM Library with DocuSign eSignature"
    version: "1.0.0"
  ecm:
    adapter: "DocuSign eSignature"
    features: "Digital Signatures, Envelope Management, Embedded Signing"
  docusign:
    environment: "demo"
    api-version: "v2.1"
```

### 4.2 Environment-Specific Configurations

**Development Configuration** (`application-dev.yml`):

```yaml
# Development-specific settings
firefly:
  ecm:
    esignature:
      properties:
        # Use demo environment
        base-url: "https://demo.docusign.net/restapi"
        oauth-base-url: "https://account-d.docusign.com"

        # Shorter expiration for testing
        default-envelope-expiration-days: 7

        # Enable webhook for local testing (use ngrok or similar)
        webhook-enabled: false
        webhook-url: "https://your-ngrok-url.ngrok.io/api/webhooks/docusign"

        # More verbose logging
        api-timeout-seconds: 60  # Longer timeout for debugging

# Enable debug logging for development
logging:
  level:
    com.firefly.core.ecm: DEBUG
    com.example.ecm: DEBUG
    com.docusign: DEBUG
    com.docusign.esign.client: DEBUG

# Development server settings
server:
  port: 8080
  ssl:
    enabled: false  # No SSL for local development
```

**Production Configuration** (`application-prod.yml`):

```yaml
# Production-specific settings
firefly:
  ecm:
    esignature:
      properties:
        # Use production environment
        base-url: "https://na1.docusign.net/restapi"  # Adjust region as needed
        oauth-base-url: "https://account.docusign.com"

        # Production envelope settings
        default-envelope-expiration-days: 30

        # Enable webhooks for production
        webhook-enabled: true
        webhook-url: "https://yourdomain.com/api/webhooks/docusign"
        webhook-secret: "${DOCUSIGN_WEBHOOK_SECRET}"

        # Production timeouts
        api-timeout-seconds: 30
        connection-timeout-seconds: 10

        # Enable advanced features
        enable-envelope-purging: true
        purge-delay-days: 90

# Production logging (less verbose)
logging:
  level:
    com.firefly.core.ecm: INFO
    com.docusign: WARN
    root: WARN

  # Log to file in production
  file:
    name: "/var/log/firefly-ecm/application.log"
    max-size: 100MB
    max-history: 30

# Production server settings
server:
  port: 8443
  ssl:
    enabled: true
    key-store: "classpath:keystore.p12"
    key-store-password: "${SSL_KEYSTORE_PASSWORD}"
    key-store-type: "PKCS12"
    key-alias: "firefly-ecm"
```

### 4.3 Environment Variables Setup

Create a script to manage environment variables. Save as `load-docusign-env.sh`:

```bash
#!/bin/bash

# Load environment variables for DocuSign integration
# Usage: source load-docusign-env.sh

echo "🔧 Loading DocuSign environment variables..."

# DocuSign credentials (replace with your actual values)
export DOCUSIGN_INTEGRATION_KEY="12345678-1234-1234-1234-123456789012"
export DOCUSIGN_USER_ID="12345678-1234-1234-1234-123456789012"
export DOCUSIGN_ACCOUNT_ID="12345678-1234-1234-1234-123456789012"

# Webhook security (generate a random secret)
export DOCUSIGN_WEBHOOK_SECRET="your-webhook-secret-here"

# SSL configuration for production
export SSL_KEYSTORE_PASSWORD="your-keystore-password"

# Document storage configuration (if using S3)
export ECM_S3_BUCKET="firefly-ecm-documents-dev"
export AWS_REGION="us-east-1"

# Validate required variables
required_vars=("DOCUSIGN_INTEGRATION_KEY" "DOCUSIGN_USER_ID" "DOCUSIGN_ACCOUNT_ID")

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "❌ Required environment variable $var is not set"
        return 1
    fi
done

echo "✅ DocuSign environment variables loaded successfully"
echo "   Integration Key: ${DOCUSIGN_INTEGRATION_KEY:0:8}..."
echo "   User ID: ${DOCUSIGN_USER_ID:0:8}..."
echo "   Account ID: ${DOCUSIGN_ACCOUNT_ID:0:8}..."

# Test DocuSign connectivity (optional)
echo "🔍 Testing DocuSign connectivity..."
# This would require the application to be running
# curl -s "http://localhost:8080/actuator/health/docusign" | jq '.'
```

### 4.4 Security Configuration

Create a security configuration class for webhook verification:

```java
// src/main/java/com/example/ecm/config/SecurityConfiguration.java
package com.example.ecm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration for DocuSign integration.
 * Configures webhook endpoint security and CORS settings.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Disable CSRF for webhook endpoints
            .csrf().disable()

            // Configure authorization
            .authorizeExchange(exchanges -> exchanges
                // Allow public access to webhook endpoints
                .pathMatchers("/api/webhooks/**").permitAll()

                // Allow public access to health checks
                .pathMatchers("/actuator/health/**").permitAll()

                // Allow public access to signing completion pages
                .pathMatchers("/api/signatures/signing-complete").permitAll()

                // Require authentication for all other endpoints
                .anyExchange().authenticated()
            )

            // Configure CORS for embedded signing
            .cors(cors -> cors.configurationSource(request -> {
                var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                corsConfig.setAllowedOriginPatterns(java.util.List.of("*"));
                corsConfig.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                corsConfig.setAllowedHeaders(java.util.List.of("*"));
                corsConfig.setAllowCredentials(true);
                return corsConfig;
            }))

            .build();
    }
}

## 5. DocuSign Adapter Implementation

Now we'll implement the complete DocuSign adapter that integrates with the actual Firefly ECM signature ports. This implementation will handle envelope management, signature requests, and all DocuSign-specific functionality.

### 5.1 DocuSign Configuration Class

First, create the DocuSign configuration and client setup:

```java
// src/main/java/com/example/ecm/config/DocuSignConfiguration.java
package com.example.ecm.config;

import com.firefly.core.ecm.config.EcmProperties;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.auth.OAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Configuration class for DocuSign integration.
 * Sets up DocuSign API client with JWT authentication.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "firefly.ecm.esignature.provider", havingValue = "docusign")
public class DocuSignConfiguration {

    @Autowired
    private EcmProperties ecmProperties;

    /**
     * Creates and configures DocuSign API client with JWT authentication.
     *
     * @return Configured ApiClient for DocuSign operations
     * @throws Exception if configuration fails
     */
    @Bean
    public ApiClient docuSignApiClient() throws Exception {
        log.info("Configuring DocuSign API client");

        // Extract configuration properties
        String integrationKey = ecmProperties.getEsignaturePropertyAsString("integration-key");
        String userId = ecmProperties.getEsignaturePropertyAsString("user-id");
        String accountId = ecmProperties.getEsignaturePropertyAsString("account-id");
        String baseUrl = ecmProperties.getEsignaturePropertyAsString("base-url");
        String oauthBaseUrl = ecmProperties.getEsignaturePropertyAsString("oauth-base-url");
        String privateKeyPath = ecmProperties.getEsignaturePropertyAsString("private-key-path");
        Integer tokenExpirationHours = ecmProperties.getEsignaturePropertyAsInteger("token-expiration-hours");

        // Validate required configuration
        validateConfiguration(integrationKey, userId, accountId, baseUrl, oauthBaseUrl, privateKeyPath);

        // Create and configure API client
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(baseUrl);

        // Configure timeouts
        Integer apiTimeout = ecmProperties.getEsignaturePropertyAsInteger("api-timeout-seconds");
        Integer connectionTimeout = ecmProperties.getEsignaturePropertyAsInteger("connection-timeout-seconds");

        if (apiTimeout != null) {
            apiClient.setConnectTimeout(apiTimeout * 1000);
        }
        if (connectionTimeout != null) {
            apiClient.setReadTimeout(connectionTimeout * 1000);
        }

        // Load private key for JWT authentication
        byte[] privateKeyBytes = loadPrivateKey(privateKeyPath);

        // Configure JWT authentication
        List<String> scopes = List.of(OAuth.Scope_SIGNATURE, OAuth.Scope_IMPERSONATION);

        apiClient.configureJWTAuthorizationFlow(
            privateKeyBytes,                                    // Private key for signing JWT
            oauthBaseUrl,                                      // OAuth base URL
            integrationKey,                                    // Integration key (client ID)
            userId,                                           // User ID
            tokenExpirationHours != null ? tokenExpirationHours * 3600 : 3600  // Token expiration in seconds
        );

        log.info("DocuSign API client configured successfully:");
        log.info("  Base URL: {}", baseUrl);
        log.info("  OAuth URL: {}", oauthBaseUrl);
        log.info("  Integration Key: {}...", integrationKey.substring(0, 8));
        log.info("  User ID: {}...", userId.substring(0, 8));
        log.info("  Account ID: {}...", accountId.substring(0, 8));

        return apiClient;
    }

    /**
     * Validates required DocuSign configuration properties.
     */
    private void validateConfiguration(String integrationKey, String userId, String accountId,
                                     String baseUrl, String oauthBaseUrl, String privateKeyPath) {
        if (integrationKey == null || integrationKey.trim().isEmpty()) {
            throw new IllegalStateException("DocuSign integration key is required");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalStateException("DocuSign user ID is required");
        }
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalStateException("DocuSign account ID is required");
        }
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalStateException("DocuSign base URL is required");
        }
        if (oauthBaseUrl == null || oauthBaseUrl.trim().isEmpty()) {
            throw new IllegalStateException("DocuSign OAuth base URL is required");
        }
        if (privateKeyPath == null || privateKeyPath.trim().isEmpty()) {
            throw new IllegalStateException("DocuSign private key path is required");
        }
    }

    /**
     * Loads the private key from the specified path.
     * Supports both classpath and file system paths.
     */
    private byte[] loadPrivateKey(String privateKeyPath) throws IOException {
        if (privateKeyPath.startsWith("classpath:")) {
            // Load from classpath
            String resourcePath = privateKeyPath.substring("classpath:".length());
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    throw new IOException("Private key file not found in classpath: " + resourcePath);
                }
                byte[] keyBytes = is.readAllBytes();
                log.info("Private key loaded from classpath: {} ({} bytes)", resourcePath, keyBytes.length);
                return keyBytes;
            }
        } else {
            // Load from file system
            byte[] keyBytes = Files.readAllBytes(Paths.get(privateKeyPath));
            log.info("Private key loaded from file system: {} ({} bytes)", privateKeyPath, keyBytes.length);
            return keyBytes;
        }
    }
}
```

### 5.2 DocuSign Signature Envelope Adapter

Create the main envelope adapter implementing SignatureEnvelopePort:

```java
// src/main/java/com/example/ecm/adapter/DocuSignSignatureEnvelopeAdapter.java
package com.example.ecm.adapter;

import com.firefly.core.ecm.adapter.EcmAdapter;
import com.firefly.core.ecm.adapter.AdapterFeature;
import com.firefly.core.ecm.config.EcmProperties;
import com.firefly.core.ecm.domain.model.esignature.SignatureEnvelope;
import com.firefly.core.ecm.domain.model.esignature.SignatureRequest;
import com.firefly.core.ecm.domain.model.esignature.SignatureField;
import com.firefly.core.ecm.domain.enums.esignature.EnvelopeStatus;
import com.firefly.core.ecm.domain.enums.esignature.SignatureRequestStatus;
import com.firefly.core.ecm.domain.enums.esignature.FieldType;
import com.firefly.core.ecm.domain.enums.esignature.SignatureProvider;
import com.firefly.core.ecm.port.esignature.SignatureEnvelopePort;
import com.firefly.core.ecm.port.document.DocumentContentPort;

// DocuSign SDK imports
import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.model.*;

// Spring imports
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Reactive imports
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

// Java standard library
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DocuSign implementation of SignatureEnvelopePort.
 *
 * This adapter provides complete envelope management capabilities using DocuSign
 * as the eSignature provider. It supports:
 * - Envelope creation with documents and signers
 * - Envelope sending and status tracking
 * - Embedded and remote signing workflows
 * - Envelope voiding and archiving
 * - Status synchronization with DocuSign
 * - Webhook integration for real-time updates
 */
@Slf4j
@EcmAdapter(
    type = "docusign",
    description = "DocuSign eSignature Envelope Adapter",
    supportedFeatures = {
        AdapterFeature.ESIGNATURE_ENVELOPES,
        AdapterFeature.ESIGNATURE_REQUESTS,
        AdapterFeature.SIGNATURE_VALIDATION
    },
    requiredProperties = {"integration-key", "user-id", "account-id", "base-url", "private-key-path"},
    optionalProperties = {"oauth-base-url", "token-expiration-hours", "webhook-url", "webhook-secret"}
)
@Component
@ConditionalOnProperty(name = "firefly.ecm.esignature.provider", havingValue = "docusign")
public class DocuSignSignatureEnvelopeAdapter implements SignatureEnvelopePort {

    private final ApiClient apiClient;
    private final EnvelopesApi envelopesApi;
    private final EcmProperties ecmProperties;
    private final String accountId;
    private final DocumentContentPort documentContentPort;

    // Cache for envelope mappings (in production, use a proper cache or database)
    private final Map<UUID, String> envelopeIdMapping = new HashMap<>();
    private final Map<String, UUID> externalIdMapping = new HashMap<>();

    @Autowired
    public DocuSignSignatureEnvelopeAdapter(ApiClient apiClient, EcmProperties ecmProperties,
                                           DocumentContentPort documentContentPort) {
        this.apiClient = apiClient;
        this.ecmProperties = ecmProperties;
        this.documentContentPort = documentContentPort;
        this.accountId = ecmProperties.getEsignaturePropertyAsString("account-id");
        this.envelopesApi = new EnvelopesApi(apiClient);

        log.info("DocuSignSignatureEnvelopeAdapter initialized for account: {}...",
                accountId.substring(0, 8));
    }

    @Override
    public Mono<SignatureEnvelope> createEnvelope(SignatureEnvelope envelope) {
        return Mono.fromCallable(() -> {
            log.info("Creating DocuSign envelope: {}", envelope.getTitle());

            // Step 1: Create DocuSign envelope definition
            EnvelopeDefinition envelopeDefinition = new EnvelopeDefinition();

            // Set envelope metadata
            envelopeDefinition.setEmailSubject(envelope.getTitle());
            envelopeDefinition.setEmailBlurb(envelope.getDescription() != null ?
                envelope.getDescription() : getDefaultEnvelopeMessage());
            envelopeDefinition.setStatus("created");  // Create in draft status

            // Set envelope expiration if specified
            if (envelope.getExpiresAt() != null) {
                String expirationDate = formatDateForDocuSign(envelope.getExpiresAt());
                envelopeDefinition.setExpire(expirationDate);
                envelopeDefinition.setExpireEnabled("true");
            }

            // Step 2: Add documents to the envelope
            List<Document> documents = new ArrayList<>();
            int documentIndex = 1; // DocuSign uses 1-based indexing

            for (UUID documentId : envelope.getDocumentIds()) {
                try {
                    Document docuSignDoc = createDocuSignDocument(documentId, documentIndex);
                    documents.add(docuSignDoc);
                    documentIndex++;
                    log.debug("Added document {} to envelope", documentId);
                } catch (Exception e) {
                    log.error("Failed to add document {} to envelope", documentId, e);
                    throw new RuntimeException("Failed to add document to envelope: " + documentId, e);
                }
            }

            if (documents.isEmpty()) {
                throw new IllegalArgumentException("Envelope must contain at least one document");
            }

            envelopeDefinition.setDocuments(documents);

            // Step 3: Configure recipients (signers)
            Recipients recipients = new Recipients();
            List<Signer> signers = new ArrayList<>();

            for (SignatureRequest request : envelope.getSignatureRequests()) {
                Signer signer = createDocuSignSigner(request, envelope.getDocumentIds());
                signers.add(signer);
                log.debug("Added signer: {} (order: {})", request.getSignerEmail(), request.getSigningOrder());
            }

            recipients.setSigners(signers);
            envelopeDefinition.setRecipients(recipients);

            // Step 4: Set additional envelope properties
            configureEnvelopeSettings(envelopeDefinition, envelope);

            // Step 5: Create the envelope in DocuSign
            log.debug("Creating envelope in DocuSign...");
            EnvelopeSummary result = envelopesApi.createEnvelope(accountId, envelopeDefinition);

            // Step 6: Store mapping between Firefly and DocuSign IDs
            UUID fireflyEnvelopeId = envelope.getId() != null ? envelope.getId() : UUID.randomUUID();
            String docuSignEnvelopeId = result.getEnvelopeId();

            envelopeIdMapping.put(fireflyEnvelopeId, docuSignEnvelopeId);
            externalIdMapping.put(docuSignEnvelopeId, fireflyEnvelopeId);

            log.info("✅ Envelope created successfully in DocuSign:");
            log.info("   Firefly ID: {}", fireflyEnvelopeId);
            log.info("   DocuSign ID: {}", docuSignEnvelopeId);
            log.info("   Status: {}", result.getStatus());

            // Step 7: Return the updated envelope with DocuSign information
            return envelope.toBuilder()
                .id(fireflyEnvelopeId)
                .externalEnvelopeId(docuSignEnvelopeId)
                .status(EnvelopeStatus.DRAFT)
                .createdAt(Instant.now())
                .modifiedAt(Instant.now())
                .build();

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(throwable -> {
              log.error("Failed to create DocuSign envelope: {}", envelope.getTitle(), throwable);
              if (throwable instanceof ApiException) {
                  ApiException apiException = (ApiException) throwable;
                  log.error("DocuSign API Error: Status={}, Body={}",
                           apiException.getCode(), apiException.getResponseBody());
              }
          });
    }

    @Override
    public Mono<SignatureEnvelope> getEnvelope(UUID envelopeId) {
        return Mono.fromCallable(() -> {
            log.debug("Retrieving envelope: {}", envelopeId);

            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }

            // Get envelope from DocuSign
            Envelope docuSignEnvelope = envelopesApi.getEnvelope(accountId, docuSignEnvelopeId);

            // Convert DocuSign envelope to Firefly envelope
            SignatureEnvelope envelope = convertFromDocuSignEnvelope(docuSignEnvelope, envelopeId);

            log.debug("Retrieved envelope: {} -> {}", envelopeId, docuSignEnvelopeId);
            return envelope;

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to get envelope: {}", envelopeId, error));
    }

    @Override
    public Mono<SignatureEnvelope> updateEnvelope(SignatureEnvelope envelope) {
        return Mono.fromCallable(() -> {
            log.debug("Updating envelope: {}", envelope.getId());

            String docuSignEnvelopeId = envelopeIdMapping.get(envelope.getId());
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelope.getId());
            }

            // Create update request
            Envelope updateRequest = new Envelope();
            updateRequest.setEmailSubject(envelope.getTitle());
            updateRequest.setEmailBlurb(envelope.getDescription());

            // Update envelope in DocuSign
            EnvelopeUpdateSummary result = envelopesApi.update(accountId, docuSignEnvelopeId, updateRequest);

            log.info("Envelope updated: {} -> {}", envelope.getId(), docuSignEnvelopeId);

            return envelope.toBuilder()
                .modifiedAt(Instant.now())
                .build();

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to update envelope: {}", envelope.getId(), error));
    }

    @Override
    public Mono<SignatureEnvelope> sendEnvelope(UUID envelopeId, Long sentBy) {
        return Mono.fromCallable(() -> {
            log.info("Sending envelope to signers: {}", envelopeId);

            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }

            // Create update request to change status to "sent"
            Envelope updateRequest = new Envelope();
            updateRequest.setStatus("sent");

            // Send the envelope via DocuSign API
            EnvelopeUpdateSummary result = envelopesApi.update(accountId, docuSignEnvelopeId, updateRequest);

            log.info("✅ Envelope sent successfully:");
            log.info("   Firefly ID: {}", envelopeId);
            log.info("   DocuSign ID: {}", docuSignEnvelopeId);
            log.info("   New Status: {}", result.getEnvelopeStatus());

            // Get updated envelope to return current state
            return getEnvelope(envelopeId).block().toBuilder()
                .status(EnvelopeStatus.SENT)
                .sentBy(sentBy)
                .sentAt(Instant.now())
                .modifiedAt(Instant.now())
                .build();

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to send envelope: {}", envelopeId, error));
    }

    @Override
    public Mono<SignatureEnvelope> voidEnvelope(UUID envelopeId, String voidReason, Long voidedBy) {
        return Mono.fromCallable(() -> {
            log.info("Voiding envelope: {} (reason: {})", envelopeId, voidReason);

            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }

            // Create void request
            Envelope voidRequest = new Envelope();
            voidRequest.setStatus("voided");
            voidRequest.setVoidedReason(voidReason);

            // Void the envelope in DocuSign
            EnvelopeUpdateSummary result = envelopesApi.update(accountId, docuSignEnvelopeId, voidRequest);

            log.info("✅ Envelope voided successfully: {} -> {}", envelopeId, docuSignEnvelopeId);

            // Get updated envelope to return current state
            return getEnvelope(envelopeId).block().toBuilder()
                .status(EnvelopeStatus.VOIDED)
                .voided(true)
                .voidReason(voidReason)
                .voidedBy(voidedBy)
                .voidedAt(Instant.now())
                .modifiedAt(Instant.now())
                .build();

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to void envelope: {}", envelopeId, error));
    }

    @Override
    public Mono<String> getSigningUrl(UUID envelopeId, String signerEmail) {
        return Mono.fromCallable(() -> {
            log.debug("Generating signing URL for: {} in envelope: {}", signerEmail, envelopeId);

            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }

            // Create recipient view request for embedded signing
            RecipientViewRequest viewRequest = new RecipientViewRequest();

            // URL where user will be redirected after signing
            String returnUrl = ecmProperties.getEsignaturePropertyAsString("embedded-return-url");
            if (returnUrl == null) {
                returnUrl = "http://localhost:8080/api/signatures/signing-complete";
            }
            viewRequest.setReturnUrl(returnUrl);

            // Authentication method
            viewRequest.setAuthenticationMethod("none");  // Use stronger auth in production

            // Signer information
            viewRequest.setEmail(signerEmail);
            viewRequest.setUserName(findSignerNameByEmail(envelopeId, signerEmail));

            // Client user ID for embedded signing (must match the recipient)
            viewRequest.setClientUserId(signerEmail);

            // Optional: Frame ancestors for iframe embedding
            viewRequest.setFrameAncestors(Arrays.asList("http://localhost:8080", "https://yourdomain.com"));

            // Generate the signing URL
            ViewUrl result = envelopesApi.createRecipientView(accountId, docuSignEnvelopeId, viewRequest);

            String signingUrl = result.getUrl();
            log.info("✅ Signing URL generated for: {} (length: {} chars)", signerEmail, signingUrl.length());

            return signingUrl;

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to generate signing URL for {} in envelope {}",
                                       signerEmail, envelopeId, error));
    }

    @Override
    public Mono<Boolean> existsEnvelope(UUID envelopeId) {
        return Mono.fromCallable(() -> {
            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                return false;
            }

            try {
                envelopesApi.getEnvelope(accountId, docuSignEnvelopeId);
                return true;
            } catch (ApiException e) {
                if (e.getCode() == 404) {
                    return false;
                }
                throw e;
            }
        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to check envelope existence: {}", envelopeId, error))
          .onErrorReturn(false);
    }

    @Override
    public Mono<SignatureEnvelope> getEnvelopeByExternalId(String externalEnvelopeId, SignatureProvider provider) {
        return Mono.fromCallable(() -> {
            if (provider != SignatureProvider.DOCUSIGN) {
                throw new IllegalArgumentException("This adapter only supports DocuSign provider");
            }

            UUID fireflyEnvelopeId = externalIdMapping.get(externalEnvelopeId);
            if (fireflyEnvelopeId == null) {
                throw new RuntimeException("Envelope not found with external ID: " + externalEnvelopeId);
            }

            return getEnvelope(fireflyEnvelopeId).block();

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to get envelope by external ID: {}", externalEnvelopeId, error));
    }

    @Override
    public Mono<SignatureEnvelope> syncEnvelopeStatus(UUID envelopeId) {
        return Mono.fromCallable(() -> {
            log.debug("Syncing envelope status with DocuSign: {}", envelopeId);

            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }

            // Get current status from DocuSign
            Envelope docuSignEnvelope = envelopesApi.getEnvelope(accountId, docuSignEnvelopeId);

            // Convert to Firefly envelope with updated status
            SignatureEnvelope syncedEnvelope = convertFromDocuSignEnvelope(docuSignEnvelope, envelopeId);

            log.debug("Envelope status synced: {} -> {}", envelopeId, syncedEnvelope.getStatus());
            return syncedEnvelope;

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to sync envelope status: {}", envelopeId, error));
    }

    @Override
    public Mono<Void> resendEnvelope(UUID envelopeId) {
        return Mono.fromRunnable(() -> {
            log.info("Resending envelope: {}", envelopeId);

            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }

            try {
                // DocuSign doesn't have a direct "resend" API, but we can update the envelope
                // to trigger notifications to pending signers
                Envelope updateRequest = new Envelope();
                updateRequest.setStatus("sent");  // Ensure it's in sent status

                envelopesApi.update(accountId, docuSignEnvelopeId, updateRequest);
                log.info("✅ Envelope resent: {} -> {}", envelopeId, docuSignEnvelopeId);

            } catch (ApiException e) {
                log.error("Failed to resend envelope: {} -> {}", envelopeId, docuSignEnvelopeId, e);
                throw new RuntimeException("Failed to resend envelope", e);
            }

        }).subscribeOn(Schedulers.boundedElastic())
          .then();
    }

    @Override
    public Mono<SignatureEnvelope> archiveEnvelope(UUID envelopeId) {
        return Mono.fromCallable(() -> {
            log.info("Archiving envelope: {}", envelopeId);

            // For DocuSign, archiving typically means marking as completed and storing metadata
            // DocuSign automatically archives completed envelopes after a certain period

            SignatureEnvelope envelope = getEnvelope(envelopeId).block();
            if (envelope == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }

            // Mark as archived in our system
            SignatureEnvelope archivedEnvelope = envelope.toBuilder()
                .archived(true)
                .archivedAt(Instant.now())
                .modifiedAt(Instant.now())
                .build();

            log.info("✅ Envelope archived: {}", envelopeId);
            return archivedEnvelope;

        }).subscribeOn(Schedulers.boundedElastic())
          .doOnError(error -> log.error("Failed to archive envelope: {}", envelopeId, error));
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Creates a DocuSign document from a Firefly document.
     * Retrieves the document content and creates a DocuSign document object.
     */
    private Document createDocuSignDocument(UUID documentId, int documentIndex) throws Exception {
        log.debug("Creating DocuSign document for: {} (index: {})", documentId, documentIndex);

        // Retrieve document content from storage
        byte[] content = documentContentPort.getContent(documentId).block();
        if (content == null || content.length == 0) {
            throw new RuntimeException("Document content not found or empty: " + documentId);
        }

        // Get document metadata (in production, get this from DocumentPort)
        String fileName = "document-" + documentId + ".pdf"; // Simplified for tutorial
        String mimeType = documentContentPort.getContentType(documentId).block();

        // Create DocuSign document
        Document document = new Document();
        document.setDocumentBase64(Base64.getEncoder().encodeToString(content));
        document.setName(fileName);
        document.setFileExtension(getFileExtension(fileName));
        document.setDocumentId(String.valueOf(documentIndex));

        log.debug("DocuSign document created: {} -> {} ({} bytes)",
                 documentId, fileName, content.length);

        return document;
    }

    /**
     * Creates a DocuSign signer from a Firefly signature request.
     */
    private Signer createDocuSignSigner(SignatureRequest request, List<UUID> documentIds) {
        log.debug("Creating DocuSign signer: {} (order: {})",
                 request.getSignerEmail(), request.getSigningOrder());

        Signer signer = new Signer();

        // Basic signer information
        signer.setEmail(request.getSignerEmail());
        signer.setName(request.getSignerName());
        signer.setRecipientId(String.valueOf(request.getSigningOrder()));
        signer.setRoutingOrder(String.valueOf(request.getSigningOrder()));

        // Set client user ID for embedded signing
        signer.setClientUserId(request.getSignerEmail());

        // Configure authentication requirements
        configureSignerAuthentication(signer, request.getAuthRequirements());

        // Add signature fields/tabs
        Tabs tabs = new Tabs();

        // Create signature tabs from signature fields
        List<SignHere> signHereTabs = new ArrayList<>();
        List<DateSigned> dateSignedTabs = new ArrayList<>();
        List<Text> textTabs = new ArrayList<>();

        if (request.getSignatureFields() != null) {
            for (SignatureField field : request.getSignatureFields()) {
                switch (field.getFieldType()) {
                    case SIGNATURE:
                        SignHere signHere = createSignHereTab(field);
                        signHereTabs.add(signHere);
                        break;
                    case DATE:
                        DateSigned dateSigned = createDateSignedTab(field);
                        dateSignedTabs.add(dateSigned);
                        break;
                    case TEXT:
                        Text textTab = createTextTab(field);
                        textTabs.add(textTab);
                        break;
                    default:
                        log.warn("Unsupported field type: {}", field.getFieldType());
                }
            }
        } else {
            // Create default signature fields if none specified
            signHereTabs.addAll(createDefaultSignatureFields(documentIds));
        }

        tabs.setSignHereTabs(signHereTabs);
        tabs.setDateSignedTabs(dateSignedTabs);
        tabs.setTextTabs(textTabs);

        signer.setTabs(tabs);

        log.debug("DocuSign signer created with {} signature fields",
                 signHereTabs.size() + dateSignedTabs.size() + textTabs.size());

        return signer;
    }

    /**
     * Creates a SignHere tab from a signature field.
     */
    private SignHere createSignHereTab(SignatureField field) {
        SignHere signHere = new SignHere();

        signHere.setDocumentId(getDocumentIndex(field.getDocumentId()).toString());
        signHere.setPageNumber(field.getPageNumber().toString());
        signHere.setXPosition(field.getXPosition().toString());
        signHere.setYPosition(field.getYPosition().toString());

        if (field.getWidth() != null) {
            signHere.setWidth(field.getWidth().intValue());
        }
        if (field.getHeight() != null) {
            signHere.setHeight(field.getHeight().intValue());
        }

        signHere.setRequired(field.getRequired() != null ? field.getRequired().toString() : "true");
        signHere.setTabLabel(field.getName() != null ? field.getName() : "Signature");

        return signHere;
    }

    /**
     * Creates a DateSigned tab from a signature field.
     */
    private DateSigned createDateSignedTab(SignatureField field) {
        DateSigned dateSigned = new DateSigned();

        dateSigned.setDocumentId(getDocumentIndex(field.getDocumentId()).toString());
        dateSigned.setPageNumber(field.getPageNumber().toString());
        dateSigned.setXPosition(field.getXPosition().toString());
        dateSigned.setYPosition(field.getYPosition().toString());

        if (field.getWidth() != null) {
            dateSigned.setWidth(field.getWidth().intValue());
        }
        if (field.getHeight() != null) {
            dateSigned.setHeight(field.getHeight().intValue());
        }

        dateSigned.setRequired(field.getRequired() != null ? field.getRequired().toString() : "true");
        dateSigned.setTabLabel(field.getName() != null ? field.getName() : "Date");

        return dateSigned;
    }

    /**
     * Creates a Text tab from a signature field.
     */
    private Text createTextTab(SignatureField field) {
        Text textTab = new Text();

        textTab.setDocumentId(getDocumentIndex(field.getDocumentId()).toString());
        textTab.setPageNumber(field.getPageNumber().toString());
        textTab.setXPosition(field.getXPosition().toString());
        textTab.setYPosition(field.getYPosition().toString());

        if (field.getWidth() != null) {
            textTab.setWidth(field.getWidth().intValue());
        }
        if (field.getHeight() != null) {
            textTab.setHeight(field.getHeight().intValue());
        }

        textTab.setRequired(field.getRequired() != null ? field.getRequired().toString() : "false");
        textTab.setTabLabel(field.getName() != null ? field.getName() : "Text");
        textTab.setValue(field.getValue());

        return textTab;
    }

    /**
     * Creates default signature fields when none are specified.
     */
    private List<SignHere> createDefaultSignatureFields(List<UUID> documentIds) {
        List<SignHere> signHereTabs = new ArrayList<>();

        if (!documentIds.isEmpty()) {
            // Add signature field to the first document
            SignHere signHere = new SignHere();
            signHere.setDocumentId("1");  // First document
            signHere.setPageNumber("1");  // First page
            signHere.setXPosition("100"); // X position in pixels
            signHere.setYPosition("200"); // Y position in pixels
            signHere.setWidth(150);       // Width in pixels
            signHere.setHeight(50);       // Height in pixels
            signHere.setRequired("true");
            signHere.setTabLabel("Signature");

            signHereTabs.add(signHere);
        }

        return signHereTabs;
    }

    /**
     * Configures authentication requirements for a signer.
     */
    private void configureSignerAuthentication(Signer signer, Object authRequirements) {
        // In a real implementation, you would configure:
        // - Phone authentication
        // - SMS authentication
        // - Knowledge-based authentication
        // - ID verification

        // For this tutorial, we'll use basic email authentication
        signer.setRequireIdLookup("false");

        // Example of adding phone authentication:
        // if (authRequirements != null && authRequirements.requiresPhone()) {
        //     RecipientPhoneAuthentication phoneAuth = new RecipientPhoneAuthentication();
        //     phoneAuth.setRecipMayProvideNumber("true");
        //     signer.setPhoneAuthentication(phoneAuth);
        // }
    }

    /**
     * Configures additional envelope settings.
     */
    private void configureEnvelopeSettings(EnvelopeDefinition envelopeDefinition, SignatureEnvelope envelope) {
        // Configure envelope notifications
        Notification notification = new Notification();
        notification.setUseAccountDefaults("true");
        envelopeDefinition.setNotification(notification);

        // Configure envelope custom fields for metadata
        if (envelope.getMetadata() != null && !envelope.getMetadata().isEmpty()) {
            List<CustomField> customFields = new ArrayList<>();

            envelope.getMetadata().forEach((key, value) -> {
                TextCustomField customField = new TextCustomField();
                customField.setName(key);
                customField.setValue(value.toString());
                customField.setRequired("false");
                customField.setShow("false");
                customFields.add(customField);
            });

            CustomFields customFieldsContainer = new CustomFields();
            customFieldsContainer.setTextCustomFields(customFields);
            envelopeDefinition.setCustomFields(customFieldsContainer);
        }

        // Configure webhook URL if enabled
        Boolean webhookEnabled = ecmProperties.getEsignaturePropertyAsBoolean("webhook-enabled");
        String webhookUrl = ecmProperties.getEsignaturePropertyAsString("webhook-url");

        if (Boolean.TRUE.equals(webhookEnabled) && webhookUrl != null) {
            EventNotification eventNotification = new EventNotification();
            eventNotification.setUrl(webhookUrl);
            eventNotification.setLoggingEnabled("true");
            eventNotification.setRequireAcknowledgment("true");

            // Configure events to listen for
            List<EnvelopeEvent> envelopeEvents = Arrays.asList(
                createEnvelopeEvent("sent"),
                createEnvelopeEvent("delivered"),
                createEnvelopeEvent("completed"),
                createEnvelopeEvent("declined"),
                createEnvelopeEvent("voided")
            );
            eventNotification.setEnvelopeEvents(envelopeEvents);

            envelopeDefinition.setEventNotification(eventNotification);
        }
    }

    /**
     * Creates an envelope event for webhook notifications.
     */
    private EnvelopeEvent createEnvelopeEvent(String eventName) {
        EnvelopeEvent event = new EnvelopeEvent();
        event.setEnvelopeEventStatusCode(eventName);
        event.setIncludeDocuments("false");  // Don't include documents in webhook
        return event;
    }

    /**
     * Converts DocuSign envelope to Firefly envelope.
     */
    private SignatureEnvelope convertFromDocuSignEnvelope(Envelope docuSignEnvelope, UUID fireflyEnvelopeId) {
        // Convert DocuSign status to Firefly status
        EnvelopeStatus status = convertDocuSignStatus(docuSignEnvelope.getStatus());

        // Parse dates
        Instant createdAt = parseDocuSignDate(docuSignEnvelope.getCreatedDateTime());
        Instant sentAt = parseDocuSignDate(docuSignEnvelope.getSentDateTime());
        Instant completedAt = parseDocuSignDate(docuSignEnvelope.getCompletedDateTime());

        return SignatureEnvelope.builder()
            .id(fireflyEnvelopeId)
            .externalEnvelopeId(docuSignEnvelope.getEnvelopeId())
            .title(docuSignEnvelope.getEmailSubject())
            .description(docuSignEnvelope.getEmailBlurb())
            .status(status)
            .createdAt(createdAt)
            .sentAt(sentAt)
            .completedAt(completedAt)
            .modifiedAt(Instant.now())
            .build();
    }

    /**
     * Converts DocuSign envelope status to Firefly envelope status.
     */
    private EnvelopeStatus convertDocuSignStatus(String docuSignStatus) {
        if (docuSignStatus == null) {
            return EnvelopeStatus.DRAFT;
        }

        switch (docuSignStatus.toLowerCase()) {
            case "created":
            case "draft":
                return EnvelopeStatus.DRAFT;
            case "sent":
            case "delivered":
                return EnvelopeStatus.SENT;
            case "completed":
                return EnvelopeStatus.COMPLETED;
            case "declined":
                return EnvelopeStatus.DECLINED;
            case "voided":
                return EnvelopeStatus.VOIDED;
            default:
                log.warn("Unknown DocuSign status: {}", docuSignStatus);
                return EnvelopeStatus.DRAFT;
        }
    }

    /**
     * Utility methods for data conversion and formatting.
     */
    private String getDefaultEnvelopeMessage() {
        return ecmProperties.getEsignaturePropertyAsString("default-envelope-message");
    }

    private String formatDateForDocuSign(Instant instant) {
        // DocuSign expects MM/DD/YYYY format
        return instant.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
    }

    private Instant parseDocuSignDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(dateString);
        } catch (Exception e) {
            log.warn("Failed to parse DocuSign date: {}", dateString);
            return null;
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "pdf";
        }

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }

        return "pdf";
    }

    private Integer getDocumentIndex(UUID documentId) {
        // In a real implementation, you would maintain a mapping
        // For this tutorial, we'll use a simple approach
        return 1; // Simplified - always return first document
    }

    private String findSignerNameByEmail(UUID envelopeId, String email) {
        // In a real implementation, you would look up the signer name
        // from the envelope's signature requests
        return "Signer"; // Simplified for tutorial
    }
}
```

## 6. Service Layer Implementation

Create comprehensive service classes that use the DocuSign adapters:

### 6.1 Signature Service

```java
// src/main/java/com/example/ecm/service/SignatureService.java
package com.example.ecm.service;

import com.firefly.core.ecm.domain.model.esignature.SignatureEnvelope;
import com.firefly.core.ecm.domain.model.esignature.SignatureRequest;
import com.firefly.core.ecm.domain.model.esignature.SignatureField;
import com.firefly.core.ecm.domain.enums.esignature.EnvelopeStatus;
import com.firefly.core.ecm.domain.enums.esignature.SignatureRequestStatus;
import com.firefly.core.ecm.domain.enums.esignature.FieldType;
import com.firefly.core.ecm.port.esignature.SignatureEnvelopePort;
import com.firefly.core.ecm.port.esignature.SignatureRequestPort;
import com.firefly.core.ecm.port.document.DocumentPort;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for eSignature operations.
 *
 * This service provides business logic for signature workflows:
 * - Creating signature envelopes with documents
 * - Managing signature requests and signers
 * - Tracking signature progress
 * - Generating signing URLs for embedded signing
 * - Handling signature completion and validation
 */
@Slf4j
@Service
public class SignatureService {

    @Autowired
    private SignatureEnvelopePort signatureEnvelopePort;

    @Autowired
    private SignatureRequestPort signatureRequestPort;

    @Autowired
    private DocumentPort documentPort;

    /**
     * Creates a signature envelope with documents and signers.
     *
     * This method:
     * 1. Validates that all documents exist and are accessible
     * 2. Creates signature requests for each signer with proper ordering
     * 3. Generates default signature fields if none provided
     * 4. Creates the envelope in DocuSign
     * 5. Returns the created envelope with all metadata
     *
     * @param title Envelope title/subject
     * @param description Envelope description/message
     * @param documentIds List of document IDs to include in the envelope
     * @param signerEmails List of signer email addresses
     * @param expirationDays Number of days until envelope expires (optional)
     * @return A Mono containing the created envelope
     */
    public Mono<SignatureEnvelope> createSignatureEnvelope(
            String title,
            String description,
            List<UUID> documentIds,
            List<String> signerEmails,
            Integer expirationDays) {

        // Validate input parameters
        if (title == null || title.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Envelope title is required"));
        }

        if (documentIds == null || documentIds.isEmpty()) {
            return Mono.error(new IllegalArgumentException("At least one document is required"));
        }

        if (signerEmails == null || signerEmails.isEmpty()) {
            return Mono.error(new IllegalArgumentException("At least one signer is required"));
        }

        // Validate that all documents exist
        return validateDocuments(documentIds)
            .flatMap(validDocuments -> {
                log.info("Creating signature envelope: '{}' with {} documents and {} signers",
                        title, documentIds.size(), signerEmails.size());

                // Create signature requests for each signer
                List<SignatureRequest> signatureRequests = new ArrayList<>();

                for (int i = 0; i < signerEmails.size(); i++) {
                    String email = signerEmails.get(i);

                    // Validate email format
                    if (!isValidEmail(email)) {
                        return Mono.error(new IllegalArgumentException("Invalid email address: " + email));
                    }

                    // Create signature fields for this signer
                    List<SignatureField> signatureFields = createDefaultSignatureFields(documentIds, i);

                    SignatureRequest request = SignatureRequest.builder()
                        .signerEmail(email)
                        .signerName(extractNameFromEmail(email))
                        .signingOrder(i + 1) // 1-based ordering
                        .status(SignatureRequestStatus.PENDING)
                        .signatureFields(signatureFields)
                        .createdAt(Instant.now())
                        .build();

                    signatureRequests.add(request);
                }

                // Calculate expiration date
                Instant expiresAt = null;
                if (expirationDays != null && expirationDays > 0) {
                    expiresAt = Instant.now().plus(expirationDays, ChronoUnit.DAYS);
                }

                // Create the envelope
                SignatureEnvelope envelope = SignatureEnvelope.builder()
                    .title(title)
                    .description(description)
                    .documentIds(documentIds)
                    .signatureRequests(signatureRequests)
                    .status(EnvelopeStatus.DRAFT)
                    .expiresAt(expiresAt)
                    .createdAt(Instant.now())
                    .build();

                return signatureEnvelopePort.createEnvelope(envelope);
            })
            .doOnSuccess(envelope -> {
                log.info("✅ Signature envelope created successfully:");
                log.info("   Envelope ID: {}", envelope.getId());
                log.info("   DocuSign ID: {}", envelope.getExternalEnvelopeId());
                log.info("   Title: {}", envelope.getTitle());
                log.info("   Documents: {}", envelope.getDocumentIds().size());
                log.info("   Signers: {}", envelope.getSignatureRequests().size());
                log.info("   Status: {}", envelope.getStatus());
            })
            .doOnError(error -> {
                log.error("❌ Failed to create signature envelope: {}", title, error);
            });
    }

    /**
     * Sends an envelope to signers for signature.
     *
     * @param envelopeId The envelope ID to send
     * @param sentBy The user ID sending the envelope
     * @return A Mono containing the updated envelope
     */
    public Mono<SignatureEnvelope> sendEnvelopeForSignature(UUID envelopeId, Long sentBy) {
        if (envelopeId == null) {
            return Mono.error(new IllegalArgumentException("Envelope ID is required"));
        }

        return signatureEnvelopePort.sendEnvelope(envelopeId, sentBy)
            .doOnSuccess(envelope -> {
                log.info("📧 Envelope sent to signers:");
                log.info("   Envelope ID: {}", envelope.getId());
                log.info("   Status: {}", envelope.getStatus());
                log.info("   Sent at: {}", envelope.getSentAt());
                log.info("   Sent by: {}", envelope.getSentBy());
            })
            .doOnError(error -> {
                log.error("❌ Failed to send envelope: {}", envelopeId, error);
            });
    }

    /**
     * Gets a signing URL for embedded signing.
     *
     * @param envelopeId The envelope ID
     * @param signerEmail The signer's email address
     * @return A Mono containing the signing URL
     */
    public Mono<String> getEmbeddedSigningUrl(UUID envelopeId, String signerEmail) {
        if (envelopeId == null) {
            return Mono.error(new IllegalArgumentException("Envelope ID is required"));
        }

        if (signerEmail == null || signerEmail.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Signer email is required"));
        }

        return signatureEnvelopePort.getSigningUrl(envelopeId, signerEmail)
            .doOnSuccess(url -> {
                log.info("🔗 Generated signing URL for: {} in envelope: {}", signerEmail, envelopeId);
                log.info("   URL length: {} characters", url.length());
            })
            .doOnError(error -> {
                log.error("❌ Failed to generate signing URL for {} in envelope {}",
                         signerEmail, envelopeId, error);
            });
    }

    /**
     * Gets envelope status and progress information.
     *
     * @param envelopeId The envelope ID
     * @return A Mono containing the envelope with current status
     */
    public Mono<SignatureEnvelope> getEnvelopeStatus(UUID envelopeId) {
        if (envelopeId == null) {
            return Mono.error(new IllegalArgumentException("Envelope ID is required"));
        }

        return signatureEnvelopePort.getEnvelope(envelopeId)
            .doOnNext(envelope -> {
                log.debug("📊 Envelope status retrieved:");
                log.debug("   ID: {}", envelope.getId());
                log.debug("   Status: {}", envelope.getStatus());
                log.debug("   Created: {}", envelope.getCreatedAt());
                if (envelope.getSentAt() != null) {
                    log.debug("   Sent: {}", envelope.getSentAt());
                }
                if (envelope.getCompletedAt() != null) {
                    log.debug("   Completed: {}", envelope.getCompletedAt());
                }
            })
            .doOnError(error -> {
                log.error("❌ Failed to get envelope status: {}", envelopeId, error);
            });
    }

    /**
     * Creates a simple signature workflow for a single document.
     * This is a convenience method for common use cases.
     *
     * @param documentId The document to sign
     * @param signerEmail The signer's email address
     * @param title Optional title (uses default if null)
     * @return A Mono containing the created and sent envelope
     */
    public Mono<SignatureEnvelope> createSimpleSignatureWorkflow(
            UUID documentId,
            String signerEmail,
            String title) {

        if (documentId == null) {
            return Mono.error(new IllegalArgumentException("Document ID is required"));
        }

        if (signerEmail == null || signerEmail.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Signer email is required"));
        }

        // Get document information to create a meaningful title
        return documentPort.getDocument(documentId)
            .flatMap(document -> {
                String envelopeTitle = title != null ? title : "Please sign: " + document.getName();

                return createSignatureEnvelope(
                    envelopeTitle,
                    "Please review and sign the attached document.",
                    Arrays.asList(documentId),
                    Arrays.asList(signerEmail),
                    30 // 30 days expiration
                );
            })
            .flatMap(envelope -> sendEnvelopeForSignature(envelope.getId(), 1L)) // System user
            .doOnSuccess(envelope -> {
                log.info("🚀 Simple signature workflow created and sent:");
                log.info("   Document: {}", documentId);
                log.info("   Signer: {}", signerEmail);
                log.info("   Envelope ID: {}", envelope.getId());
                log.info("   DocuSign ID: {}", envelope.getExternalEnvelopeId());
            })
            .doOnError(error -> {
                log.error("❌ Failed to create simple signature workflow for document {} and signer {}",
                         documentId, signerEmail, error);
            });
    }

    /**
     * Voids an envelope with a reason.
     *
     * @param envelopeId The envelope ID to void
     * @param voidReason The reason for voiding
     * @param voidedBy The user ID voiding the envelope
     * @return A Mono containing the voided envelope
     */
    public Mono<SignatureEnvelope> voidEnvelope(UUID envelopeId, String voidReason, Long voidedBy) {
        if (envelopeId == null) {
            return Mono.error(new IllegalArgumentException("Envelope ID is required"));
        }

        if (voidReason == null || voidReason.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Void reason is required"));
        }

        return signatureEnvelopePort.voidEnvelope(envelopeId, voidReason, voidedBy)
            .doOnSuccess(envelope -> {
                log.info("🚫 Envelope voided:");
                log.info("   Envelope ID: {}", envelope.getId());
                log.info("   Reason: {}", voidReason);
                log.info("   Voided by: {}", voidedBy);
            })
            .doOnError(error -> {
                log.error("❌ Failed to void envelope: {}", envelopeId, error);
            });
    }

    /**
     * Resends an envelope to pending signers.
     *
     * @param envelopeId The envelope ID to resend
     * @return A Mono indicating completion
     */
    public Mono<Void> resendEnvelope(UUID envelopeId) {
        if (envelopeId == null) {
            return Mono.error(new IllegalArgumentException("Envelope ID is required"));
        }

        return signatureEnvelopePort.resendEnvelope(envelopeId)
            .doOnSuccess(unused -> {
                log.info("🔄 Envelope resent to pending signers: {}", envelopeId);
            })
            .doOnError(error -> {
                log.error("❌ Failed to resend envelope: {}", envelopeId, error);
            });
    }

    /**
     * Syncs envelope status with DocuSign.
     *
     * @param envelopeId The envelope ID to sync
     * @return A Mono containing the updated envelope
     */
    public Mono<SignatureEnvelope> syncEnvelopeStatus(UUID envelopeId) {
        if (envelopeId == null) {
            return Mono.error(new IllegalArgumentException("Envelope ID is required"));
        }

        return signatureEnvelopePort.syncEnvelopeStatus(envelopeId)
            .doOnSuccess(envelope -> {
                log.debug("🔄 Envelope status synced: {} -> {}", envelopeId, envelope.getStatus());
            })
            .doOnError(error -> {
                log.error("❌ Failed to sync envelope status: {}", envelopeId, error);
            });
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Validates that all documents exist and are accessible.
     */
    private Mono<List<com.firefly.core.ecm.domain.model.document.Document>> validateDocuments(List<UUID> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return Mono.error(new IllegalArgumentException("At least one document is required"));
        }

        // Check that all documents exist
        List<Mono<com.firefly.core.ecm.domain.model.document.Document>> documentChecks = documentIds.stream()
            .map(documentPort::getDocument)
            .collect(Collectors.toList());

        return Flux.merge(documentChecks)
            .collectList()
            .map(documents -> {
                if (documents.size() != documentIds.size()) {
                    throw new IllegalArgumentException("One or more documents not found");
                }

                // Validate document status and accessibility
                for (com.firefly.core.ecm.domain.model.document.Document doc : documents) {
                    if (doc.getStatus() != com.firefly.core.ecm.domain.enums.document.DocumentStatus.ACTIVE) {
                        throw new IllegalArgumentException("Document is not active: " + doc.getId());
                    }
                }

                return documents;
            });
    }

    /**
     * Creates default signature fields for documents.
     * In a real application, you might allow users to position these fields.
     */
    private List<SignatureField> createDefaultSignatureFields(List<UUID> documentIds, int signerIndex) {
        List<SignatureField> fields = new ArrayList<>();

        // Add signature fields to the first document
        if (!documentIds.isEmpty()) {
            UUID firstDocumentId = documentIds.get(0);

            // Calculate Y position based on signer index to avoid overlapping
            double baseYPosition = 200.0 + (signerIndex * 100.0);

            // Signature field
            SignatureField signatureField = SignatureField.builder()
                .name("Signature")
                .fieldType(FieldType.SIGNATURE)
                .documentId(firstDocumentId)
                .pageNumber(1)
                .xPosition(100.0)
                .yPosition(baseYPosition)
                .width(150.0)
                .height(50.0)
                .required(true)
                .build();

            // Date field
            SignatureField dateField = SignatureField.builder()
                .name("Date")
                .fieldType(FieldType.DATE)
                .documentId(firstDocumentId)
                .pageNumber(1)
                .xPosition(300.0)
                .yPosition(baseYPosition)
                .width(100.0)
                .height(20.0)
                .required(true)
                .build();

            fields.add(signatureField);
            fields.add(dateField);
        }

        return fields;
    }

    /**
     * Extracts a name from an email address.
     * This is a simple implementation - in production, you'd get the name from user data.
     */
    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "Unknown Signer";
        }

        String localPart = email.substring(0, email.indexOf("@"));

        // Convert dots and underscores to spaces and capitalize
        return Arrays.stream(localPart.split("[._]"))
            .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }

    /**
     * Validates email address format.
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // Simple email validation - use a proper validator in production
        return email.contains("@") && email.contains(".") && email.length() > 5;
    }
}
```

### REST Controller

```java
@RestController
@RequestMapping("/api/signatures")
public class SignatureController {
    
    @Autowired
    private SignatureService signatureService;
    
    @PostMapping("/envelopes")
    public Mono<ResponseEntity<SignatureEnvelope>> createEnvelope(
            @RequestBody CreateEnvelopeRequest request) {
        
        return signatureService.createSignatureEnvelope(
                request.getTitle(),
                request.getDescription(),
                request.getDocumentIds(),
                request.getSignerEmails()
            )
            .map(envelope -> ResponseEntity.status(HttpStatus.CREATED).body(envelope))
            .onErrorReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }
    
    @PostMapping("/envelopes/{envelopeId}/send")
    public Mono<ResponseEntity<SignatureEnvelope>> sendEnvelope(
            @PathVariable UUID envelopeId,
            @RequestParam(defaultValue = "1") Long sentBy) {
        
        return signatureService.sendEnvelopeForSignature(envelopeId, sentBy)
            .map(envelope -> ResponseEntity.ok(envelope))
            .onErrorReturn(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/envelopes/{envelopeId}")
    public Mono<ResponseEntity<SignatureEnvelope>> getEnvelope(@PathVariable UUID envelopeId) {
        return signatureService.getEnvelopeStatus(envelopeId)
            .map(envelope -> ResponseEntity.ok(envelope))
            .onErrorReturn(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/envelopes/{envelopeId}/signing-url")
    public Mono<ResponseEntity<Map<String, String>>> getSigningUrl(
            @PathVariable UUID envelopeId,
            @RequestParam String signerEmail) {
        
        return signatureService.getEmbeddedSigningUrl(envelopeId, signerEmail)
            .map(url -> ResponseEntity.ok(Map.of("signingUrl", url)))
            .onErrorReturn(ResponseEntity.notFound().build());
    }
    
    public static class CreateEnvelopeRequest {
        private String title;
        private String description;
        private List<UUID> documentIds;
        private List<String> signerEmails;
        
        // Getters and setters...
    }
}
```

## 5. Testing

### Integration Test

```java
@SpringBootTest
@TestPropertySource(properties = {
    "firefly.ecm.esignature.provider=docusign",
    "firefly.ecm.esignature.properties.integration-key=${DOCUSIGN_INTEGRATION_KEY}",
    "firefly.ecm.esignature.properties.account-id=${DOCUSIGN_ACCOUNT_ID}"
})
class DocuSignIntegrationTest {
    
    @Autowired
    private SignatureEnvelopePort envelopePort;
    
    @Test
    void testEnvelopeCreationAndSending() {
        SignatureRequest request = SignatureRequest.builder()
            .signerEmail("signer@example.com")
            .signerName("John Doe")
            .signingOrder(1)
            .build();
        
        SignatureEnvelope envelope = SignatureEnvelope.builder()
            .title("Test Contract")
            .description("Please sign this test contract")
            .documentIds(List.of(UUID.randomUUID()))
            .signatureRequests(List.of(request))
            .build();
        
        StepVerifier.create(envelopePort.createEnvelope(envelope))
            .assertNext(createdEnvelope -> {
                assertThat(createdEnvelope.getId()).isNotNull();
                assertThat(createdEnvelope.getExternalEnvelopeId()).isNotEmpty();
                assertThat(createdEnvelope.getStatus()).isEqualTo(EnvelopeStatus.DRAFT);
            })
            .verifyComplete();
    }
}
```

## 6. Production Considerations

### Security
- Use production DocuSign environment
- Implement webhook signature verification
- Store private keys securely
- Use proper authentication methods

### Monitoring
- Track envelope status changes
- Monitor API usage and limits
- Set up error alerting
- Log all signature activities

### Compliance
- Ensure eIDAS compliance for EU
- Follow ESIGN Act requirements for US
- Implement proper audit trails
- Configure retention policies

## Troubleshooting

| Issue | Solution |
|-------|----------|
| JWT authentication failed | Check integration key and private key |
| Account not found | Verify account ID |
| Consent required | Complete OAuth consent process |
| Document not found | Ensure document exists in storage |

## Next Steps

- [Configure webhook notifications](../examples/docusign-webhooks.md)
- [Implement advanced authentication](../examples/signature-authentication.md)
- [Set up bulk signing workflows](../examples/bulk-signing.md)
- [Add signature validation](../examples/signature-validation.md)
