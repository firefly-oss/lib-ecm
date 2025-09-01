# Intelligent Document Processing (IDP) Integration Guide

## Overview

The Firefly OpenCore Platform ECM library provides comprehensive support for Intelligent Document Processing (IDP) through a hexagonal architecture that allows seamless integration with various IDP providers. This guide covers the implementation, configuration, and usage of IDP capabilities.

## Current Implementation Status

✅ **Available Now:**
- Complete port interface definitions for all IDP operations
- Domain models and enumerations for IDP data structures
- Autoconfiguration framework with feature toggles
- Comprehensive documentation and integration guides

🚧 **Coming Soon:**
- Concrete adapter implementations for AWS Textract, Azure Form Recognizer, and Google Document AI
- Ready-to-use adapter packages for immediate deployment

The current implementation provides the **complete foundation** for IDP integration. You can start building your IDP workflows using the port interfaces, and concrete adapters can be implemented following the provided integration guides.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Core Components](#core-components)
3. [Configuration](#configuration)
4. [Port Interfaces](#port-interfaces)
5. [Domain Models](#domain-models)
6. [Integration Guides](#integration-guides)
7. [Usage Examples](#usage-examples)
8. [Best Practices](#best-practices)

## Architecture Overview

The IDP implementation follows the hexagonal architecture pattern with the following components:

```
┌─────────────────────────────────────────────────────────────┐
│                    Business Logic                           │
├─────────────────────────────────────────────────────────────┤
│  DocumentExtractionPort  │  DocumentClassificationPort      │
│  DocumentValidationPort  │  DataExtractionPort              │
├─────────────────────────────────────────────────────────────┤
│                    Adapter Layer                            │
│  AWS Textract Adapter*   │  Azure Form Recognizer Adapter*  │
│  Google Document AI*     │  Custom IDP Adapters*            │
└─────────────────────────────────────────────────────────────┘

*Note: Adapter implementations are not yet available. The framework provides
the port interfaces and infrastructure for implementing these adapters.
```

### Key Benefits

- **Provider Agnostic**: Switch between IDP providers without changing business logic
- **Extensible**: Add new IDP providers through adapter implementation
- **Configurable**: Enable/disable features through properties
- **Reactive**: Non-blocking operations using Project Reactor
- **Comprehensive**: Support for text extraction, classification, validation, and structured data extraction

## Core Components

### Port Interfaces

The IDP functionality is exposed through four main port interfaces:

1. **DocumentExtractionPort**: Text extraction and OCR operations
2. **DocumentClassificationPort**: Document classification and categorization
3. **DocumentValidationPort**: Document validation and verification
4. **DataExtractionPort**: Structured and semi-structured data extraction

### Domain Models

Key domain models for IDP operations:

- **DocumentProcessingRequest**: Request for document processing
- **DocumentProcessingResult**: Complete processing results
- **ExtractedData**: Individual extracted data elements
- **ClassificationResult**: Document classification outcomes
- **ValidationResult**: Validation results and quality metrics

### Enumerations

Supporting enumerations for type safety:

- **DocumentType**: Supported document types (invoice, contract, etc.)
- **ProcessingStatus**: Processing lifecycle states
- **ExtractionType**: Types of data extraction operations
- **ClassificationConfidence**: Confidence levels for classifications
- **ValidationLevel**: Validation rigor levels

## Configuration

### Enabling IDP Features

Add the following to your `application.yml`:

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: "aws-textract"  # Note: IDP adapters not yet implemented
    features:
      idp: true
```

### IDP Configuration Using Existing Properties

Since IDP adapters are not yet implemented, configuration will use the existing `properties` map pattern when adapters become available:

```yaml
firefly:
  ecm:
    # Use existing connection settings for timeouts and retries
    connection:
      connect-timeout: PT30S
      read-timeout: PT5M
      retry-attempts: 3
      max-connections: 100

    # Provider-specific properties (when adapters are implemented)
    properties:
      # Example AWS Textract properties (not yet implemented)
      region: "us-east-1"
      bucket-name: "idp-processing-bucket"
      access-key-id: "${AWS_ACCESS_KEY_ID}"
      secret-access-key: "${AWS_SECRET_ACCESS_KEY}"

      # Example Azure Form Recognizer properties (not yet implemented)
      endpoint: "https://your-form-recognizer.cognitiveservices.azure.com/"
      api-key: "${AZURE_FORM_RECOGNIZER_KEY}"

      # Example Google Document AI properties (not yet implemented)
      project-id: "your-gcp-project"
      location: "us"
      credentials-path: "path/to/service-account.json"
```

## Port Interfaces

### DocumentExtractionPort

Handles text extraction and OCR operations:

```java
@Autowired
private DocumentExtractionPort extractionPort;

// Extract text using OCR
Mono<ExtractedData> textResult = extractionPort.extractText(documentId, ExtractionType.OCR_TEXT);

// Process document comprehensively
DocumentProcessingRequest request = DocumentProcessingRequest.builder()
    .documentId(documentId)
    .extractionTypes(List.of(ExtractionType.OCR_TEXT, ExtractionType.HANDWRITING_RECOGNITION))
    .validationLevel(ValidationLevel.STANDARD)
    .build();

Mono<DocumentProcessingResult> result = extractionPort.processDocument(request);
```

### DocumentClassificationPort

Handles document classification and categorization:

```java
@Autowired
private DocumentClassificationPort classificationPort;

// Classify document
Mono<ClassificationResult> classification = classificationPort.classifyDocument(documentId);

// Classify with custom configuration
Map<String, Object> config = Map.of(
    "model", "financial_documents",
    "threshold", 0.85,
    "maxAlternatives", 3
);
Mono<ClassificationResult> result = classificationPort.classifyDocumentWithConfig(documentId, config);
```

### DocumentValidationPort

Handles document validation and verification:

```java
@Autowired
private DocumentValidationPort validationPort;

// Validate document with standard rules
Mono<ValidationResult> validation = validationPort.validateDocument(documentId, ValidationLevel.STANDARD);

// Validate with custom business rules
Map<String, Object> rules = Map.of(
    "required_fields", List.of("invoice_number", "date", "total"),
    "max_amount", 10000.0,
    "date_format", "yyyy-MM-dd"
);
Mono<ValidationResult> result = validationPort.validateDocumentWithRules(documentId, rules);
```

### DataExtractionPort

Handles structured and semi-structured data extraction:

```java
@Autowired
private DataExtractionPort dataExtractionPort;

// Extract form fields
Flux<ExtractedData> formData = dataExtractionPort.extractFormFields(documentId);

// Extract table data
Flux<ExtractedData> tableData = dataExtractionPort.extractTableData(documentId);

// Extract using template
Mono<List<ExtractedData>> templateData = dataExtractionPort.extractWithTemplate(documentId, "invoice_template_v2");
```

## Integration Guides

### AWS Textract Integration

See [AWS Textract Integration Guide](aws-textract-integration.md) for detailed setup instructions.

### Azure Form Recognizer Integration

See [Azure Form Recognizer Integration Guide](azure-form-recognizer-integration.md) for detailed setup instructions.

### Google Document AI Integration

See [Google Document AI Integration Guide](google-document-ai-integration.md) for detailed setup instructions.

## Usage Examples

### Complete Document Processing Workflow

```java
@Service
public class DocumentProcessingService {
    
    @Autowired
    private DocumentExtractionPort extractionPort;
    
    @Autowired
    private DocumentClassificationPort classificationPort;
    
    @Autowired
    private DocumentValidationPort validationPort;
    
    @Autowired
    private DataExtractionPort dataExtractionPort;
    
    public Mono<ProcessedDocument> processDocument(UUID documentId) {
        return classificationPort.classifyDocument(documentId)
            .flatMap(classification -> {
                DocumentType docType = classification.getDocumentType();
                return extractDocumentData(documentId, docType);
            })
            .flatMap(extractedData -> validateData(extractedData))
            .map(this::buildProcessedDocument);
    }
    
    private Mono<List<ExtractedData>> extractDocumentData(UUID documentId, DocumentType docType) {
        return switch (docType) {
            case INVOICE -> dataExtractionPort.extractLineItems(documentId).collectList();
            case CONTRACT -> dataExtractionPort.extractKeyValuePairs(documentId).collectList();
            case FORM_DOCUMENT -> dataExtractionPort.extractFormFields(documentId).collectList();
            default -> extractionPort.extractText(documentId, ExtractionType.OCR_TEXT)
                .map(List::of);
        };
    }
    
    private Mono<List<ExtractedData>> validateData(List<ExtractedData> data) {
        return validationPort.validateExtractedData(data, ValidationLevel.STANDARD)
            .map(validationResult -> {
                if (validationResult.getAllRulesPassed()) {
                    return data;
                } else {
                    throw new ValidationException("Data validation failed: " + 
                        validationResult.getErrors());
                }
            });
    }
}
```

## Best Practices

### 1. Error Handling

Always handle errors gracefully and provide meaningful feedback:

```java
extractionPort.extractText(documentId, ExtractionType.OCR_TEXT)
    .onErrorResume(throwable -> {
        log.error("Text extraction failed for document {}: {}", documentId, throwable.getMessage());
        return Mono.just(ExtractedData.builder()
            .extractionType(ExtractionType.OCR_TEXT)
            .rawValue("Extraction failed")
            .confidence(0)
            .build());
    });
```

### 2. Configuration Management

Use environment-specific configurations:

```yaml
# application-dev.yml
firefly.ecm.idp.confidence-threshold: 0.7

# application-prod.yml
firefly.ecm.idp.confidence-threshold: 0.9
```

### 3. Performance Optimization

Use batch processing for multiple documents:

```java
Flux<ClassificationResult> results = classificationPort.classifyDocuments(documentIds);
```

### 4. Monitoring and Metrics

Implement proper monitoring for IDP operations:

```java
@Component
public class IdpMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordProcessingTime(String operation, Duration duration) {
        Timer.Sample.start(meterRegistry)
            .stop(Timer.builder("idp.processing.time")
                .tag("operation", operation)
                .register(meterRegistry));
    }
}
```

### 5. Security Considerations

- Ensure proper authentication for cloud IDP services
- Implement data encryption for sensitive documents
- Use secure storage for processing results
- Audit all IDP operations for compliance

## Next Steps

1. Choose your IDP provider and follow the specific integration guide
2. Configure the library according to your requirements
3. Implement custom adapters if needed
4. Set up monitoring and alerting
5. Test with your document types and workflows

For specific integration guides, see the provider-specific documentation in this directory.
