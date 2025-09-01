# Adobe Sign Integration Guide

This guide provides step-by-step instructions for integrating Adobe Sign with the Firefly ECM Library for eSignature capabilities.

## Overview

The Adobe Sign adapter provides comprehensive eSignature functionality through Adobe Sign's REST API, supporting:

- **Signature Envelopes**: Create, send, and manage signature workflows
- **Signature Requests**: Individual signer management and tracking
- **Signature Validation**: Verify signatures and audit trails
- **Embedded Signing**: Generate signing URLs for integrated workflows
- **OAuth 2.0 Authentication**: Secure API access with refresh tokens

## Prerequisites

### Adobe Sign Account Setup

1. **Adobe Sign Account**: Ensure you have an Adobe Sign business account
2. **API Access**: Enable API access in your Adobe Sign account
3. **Application Registration**: Register your application in Adobe Sign Developer Console

### Required Information

You'll need the following from your Adobe Sign account:
- **Client ID**: Your application's client identifier
- **Client Secret**: Your application's client secret
- **Refresh Token**: OAuth refresh token for API access
- **Base URL**: Adobe Sign API base URL (varies by region)

## Configuration

### 1. Add Dependencies

Add the Adobe Sign adapter dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.firefly.ecm</groupId>
    <artifactId>lib-ecm-adapter-adobe-sign</artifactId>
    <version>${firefly.ecm.version}</version>
</dependency>
```

### 2. Application Configuration

Configure Adobe Sign in your `application.yml`:

```yaml
firefly:
  ecm:
    enabled: true
    esignature:
      provider: adobe-sign
    adapter:
      adobe-sign:
        client-id: ${ADOBE_SIGN_CLIENT_ID}
        client-secret: ${ADOBE_SIGN_CLIENT_SECRET}
        refresh-token: ${ADOBE_SIGN_REFRESH_TOKEN}
        base-url: https://api.na1.adobesign.com  # Adjust for your region
        api-version: v6
        webhook-url: ${ADOBE_SIGN_WEBHOOK_URL:}
        webhook-secret: ${ADOBE_SIGN_WEBHOOK_SECRET:}
        max-retries: 3
        token-expiration: 3600
```

### 3. Environment Variables

Set the following environment variables:

```bash
export ADOBE_SIGN_CLIENT_ID="your-client-id"
export ADOBE_SIGN_CLIENT_SECRET="your-client-secret"
export ADOBE_SIGN_REFRESH_TOKEN="your-refresh-token"
export ADOBE_SIGN_WEBHOOK_URL="https://your-app.com/webhooks/adobe-sign"
export ADOBE_SIGN_WEBHOOK_SECRET="your-webhook-secret"
```

## Usage Examples

### Basic Envelope Creation

```java
@Autowired
private EcmPortProvider portProvider;

public Mono<SignatureEnvelope> createSignatureEnvelope() {
    SignatureEnvelopePort envelopePort = portProvider.getSignatureEnvelopePort()
        .orElseThrow(() -> new ServiceUnavailableException("Adobe Sign not available"));
    
    // Create signature requests
    SignatureRequest signerRequest = SignatureRequest.builder()
        .id(UUID.randomUUID())
        .signerEmail("signer@example.com")
        .signerName("John Doe")
        .signingOrder(1)
        .required(true)
        .build();
    
    // Create envelope
    SignatureEnvelope envelope = SignatureEnvelope.builder()
        .id(UUID.randomUUID())
        .title("Contract Signature")
        .description("Please sign this contract")
        .documentIds(List.of(documentId))
        .signatureRequests(List.of(signerRequest))
        .createdBy(currentUserId)
        .build();
    
    return envelopePort.createEnvelope(envelope);
}
```

### Sending an Envelope

```java
public Mono<SignatureEnvelope> sendEnvelope(UUID envelopeId, UUID sentBy) {
    SignatureEnvelopePort envelopePort = portProvider.getSignatureEnvelopePort()
        .orElseThrow(() -> new ServiceUnavailableException("Adobe Sign not available"));
    
    return envelopePort.sendEnvelope(envelopeId, sentBy);
}
```

### Generating Signing URLs

```java
public Mono<String> getSigningUrl(UUID envelopeId, String signerEmail) {
    SignatureEnvelopePort envelopePort = portProvider.getSignatureEnvelopePort()
        .orElseThrow(() -> new ServiceUnavailableException("Adobe Sign not available"));
    
    return envelopePort.getSigningUrl(
        envelopeId, 
        signerEmail, 
        "https://your-app.com/signing-complete", 
        "en_US"
    );
}
```

## Advanced Configuration

### Regional Endpoints

Adobe Sign uses different base URLs for different regions:

- **North America**: `https://api.na1.adobesign.com`
- **Europe**: `https://api.eu1.adobesign.com`
- **Asia Pacific**: `https://api.ap1.adobesign.com`
- **Australia**: `https://api.au1.adobesign.com`

### Webhook Configuration

Configure webhooks for real-time status updates:

```yaml
firefly:
  ecm:
    adapter:
      adobe-sign:
        webhook-url: https://your-app.com/webhooks/adobe-sign
        webhook-secret: your-webhook-secret
```

### Resilience Configuration

The adapter includes built-in resilience patterns:

```yaml
firefly:
  ecm:
    adapter:
      adobe-sign:
        max-retries: 3
        circuit-breaker:
          failure-rate-threshold: 50
          wait-duration-in-open-state: 30s
          sliding-window-size: 10
```

## Security Considerations

1. **Secure Storage**: Store client secrets and refresh tokens securely
2. **Token Rotation**: Implement refresh token rotation for enhanced security
3. **Webhook Security**: Validate webhook signatures using the webhook secret
4. **Network Security**: Use HTTPS for all communications
5. **Access Control**: Implement proper access controls for envelope operations

## Troubleshooting

### Common Issues

1. **Authentication Errors**: Verify client ID, secret, and refresh token
2. **Regional Issues**: Ensure correct base URL for your Adobe Sign region
3. **Document Access**: Verify document storage adapter is properly configured
4. **Network Timeouts**: Adjust timeout settings for large documents

### Logging

Enable debug logging for troubleshooting:

```yaml
logging:
  level:
    com.firefly.ecm.adapter.adobesign: DEBUG
```

## API Reference

For detailed API documentation, see:
- [SignatureEnvelopePort](../api/signature-envelope-port.md)
- [SignatureRequestPort](../api/signature-request-port.md)
- [SignatureValidationPort](../api/signature-validation-port.md)

## Support

For additional support:
- Check the [Adobe Sign API Documentation](https://secure.na1.adobesign.com/public/docs/restapi/v6)
- Review the [Firefly ECM Architecture Guide](../architecture.md)
- Contact Firefly Software Solutions support
