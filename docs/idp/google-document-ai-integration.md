# Google Document AI Integration Guide

## Overview

This guide provides step-by-step instructions for integrating Google Document AI with the Firefly OpenCore Platform ECM library. Google Document AI is a platform that uses machine learning to extract, classify, and understand documents.

## Prerequisites

- Google Cloud Platform (GCP) account with billing enabled
- Google Cloud CLI (gcloud) installed and configured
- Java 17 or higher
- Spring Boot 3.x application

## Step 1: Google Cloud Setup

### 1.1 Enable Document AI API

Enable the Document AI API in your GCP project:

```bash
# Set your project ID
export PROJECT_ID=your-gcp-project-id
gcloud config set project $PROJECT_ID

# Enable Document AI API
gcloud services enable documentai.googleapis.com

# Enable Cloud Storage API (for document storage)
gcloud services enable storage.googleapis.com
```

### 1.2 Create Service Account

Create a service account with necessary permissions:

```bash
# Create service account
gcloud iam service-accounts create firefly-document-ai \
    --description="Service account for Firefly ECM Document AI integration" \
    --display-name="Firefly Document AI"

# Grant necessary roles
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:firefly-document-ai@$PROJECT_ID.iam.gserviceaccount.com" \
    --role="roles/documentai.apiUser"

gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:firefly-document-ai@$PROJECT_ID.iam.gserviceaccount.com" \
    --role="roles/storage.objectAdmin"

# Create and download service account key
gcloud iam service-accounts keys create firefly-document-ai-key.json \
    --iam-account=firefly-document-ai@$PROJECT_ID.iam.gserviceaccount.com
```

### 1.3 Create Cloud Storage Bucket

Create a bucket for storing documents:

```bash
# Create bucket
gsutil mb gs://firefly-document-ai-bucket

# Set bucket permissions
gsutil iam ch serviceAccount:firefly-document-ai@$PROJECT_ID.iam.gserviceaccount.com:objectAdmin \
    gs://firefly-document-ai-bucket
```

### 1.4 Create Document AI Processors

Create processors for different document types:

```bash
# Create a general document processor
gcloud alpha documentai processors create \
    --location=us \
    --display-name="Firefly General Processor" \
    --type=OCR_PROCESSOR

# Create an invoice processor
gcloud alpha documentai processors create \
    --location=us \
    --display-name="Firefly Invoice Processor" \
    --type=INVOICE_PROCESSOR

# Create a form parser processor
gcloud alpha documentai processors create \
    --location=us \
    --display-name="Firefly Form Parser" \
    --type=FORM_PARSER_PROCESSOR
```

## Step 2: Dependencies

Add the Google Cloud SDK dependencies to your `pom.xml` (adapter implementation not yet available):

```xml
<!-- Note: Google Document AI adapter implementation is not yet available -->
<!-- You will need to implement the adapter using these dependencies -->

<!-- Google Cloud dependencies -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-document-ai</artifactId>
    <version>2.27.0</version>
</dependency>
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-storage</artifactId>
    <version>2.26.1</version>
</dependency>
<dependency>
    <groupId>com.google.auth</groupId>
    <artifactId>google-auth-library-oauth2-http</artifactId>
    <version>1.19.0</version>
</dependency>
```

## Step 3: Configuration

### 3.1 Application Properties

Configure the Google Document AI adapter in your `application.yml`:

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: "google-document-ai"  # Note: adapter not yet implemented
    features:
      idp: true

    # Use existing connection settings
    connection:
      connect-timeout: PT30S
      read-timeout: PT5M
      retry-attempts: 3
      max-connections: 100

    # Google Document AI configuration (when adapter is implemented)
    properties:
      project-id: "your-gcp-project-id"
      location: "us"  # or "eu" based on your preference
      credentials-path: "path/to/firefly-document-ai-key.json"

      # Alternative: Use environment variable
      # credentials-json: "${GOOGLE_APPLICATION_CREDENTIALS_JSON}"

      # Additional properties for Google Document AI adapter implementation
      ocr-processor-id: "projects/your-project/locations/us/processors/ocr-processor-id"
      invoice-processor-id: "projects/your-project/locations/us/processors/invoice-processor-id"
      form-processor-id: "projects/your-project/locations/us/processors/form-processor-id"
      storage-bucket-name: "firefly-document-ai-bucket"
      storage-temp-folder: "temp/"
      confidence-threshold: 0.8
      include-text-changes: false
      enable-layout-analysis: true
      enable-entity-extraction: true
      enable-classification: true

# Standard Google Cloud configuration (separate from Firefly ECM)
spring:
  cloud:
    gcp:
      project-id: "your-gcp-project-id"
      credentials:
        location: "classpath:firefly-document-ai-key.json"
```

### 3.2 Environment Variables

Set the following environment variables:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=path/to/firefly-document-ai-key.json
export GOOGLE_CLOUD_PROJECT=your-gcp-project-id

# Alternative: Use JSON content directly
export GOOGLE_APPLICATION_CREDENTIALS_JSON='{"type":"service_account",...}'
```

## Step 4: Adapter Implementation

**Note: The Google Document AI adapter is not yet implemented. This section shows how you would implement it using the provided port interfaces.**

Here's how you would implement the Google Document AI adapter for all four IDP port interfaces:

### 4.1 DocumentExtractionPort Implementation

```java
@EcmAdapter(
    type = "google-document-ai",
    description = "Google Document AI Extraction Adapter",
    supportedFeatures = {AdapterFeature.TEXT_EXTRACTION, AdapterFeature.OCR, AdapterFeature.LAYOUT_ANALYSIS}
)
@Component
public class GoogleDocumentAiExtractionAdapter implements DocumentExtractionPort {
    
    private final DocumentProcessorServiceClient client;
    private final Storage storage;
    private final GoogleDocumentAiProperties properties;
    
    @Override
    public Mono<ExtractedData> extractText(UUID documentId, ExtractionType extractionType) {
        return getDocumentFromEcm(documentId)
            .flatMap(document -> processDocument(document, getProcessorForType(extractionType)))
            .map(this::convertToExtractedData);
    }
    
    @Override
    public Mono<DocumentProcessingResult> processDocument(DocumentProcessingRequest request) {
        return Mono.fromCallable(() -> {
            String processorName = selectProcessor(request);
            return processDocumentWithProcessor(request.getDocumentId(), processorName);
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    private Mono<ProcessResponse> processDocument(Document document, String processorName) {
        return Mono.fromCallable(() -> {
            // Convert document to Google Cloud format
            RawDocument rawDocument = RawDocument.newBuilder()
                .setContent(ByteString.copyFrom(document.getContent()))
                .setMimeType(document.getMimeType())
                .build();
                
            ProcessRequest request = ProcessRequest.newBuilder()
                .setName(processorName)
                .setRawDocument(rawDocument)
                .build();
                
            return client.processDocument(request);
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    private String selectProcessor(DocumentProcessingRequest request) {
        return switch (request.getDocumentType()) {
            case INVOICE -> properties.getProcessors().get("invoice");
            case FORM_DOCUMENT -> properties.getProcessors().get("form");
            default -> properties.getProcessors().get("ocr");
        };
    }
}
```

### 4.2 DocumentClassificationPort Implementation

```java
@EcmAdapter(type = "google-document-ai")
@Component
public class GoogleDocumentAiClassificationAdapter implements DocumentClassificationPort {
    
    @Override
    public Mono<ClassificationResult> classifyDocument(UUID documentId) {
        return processWithGeneralProcessor(documentId)
            .map(this::analyzeDocumentStructure)
            .map(this::classifyBasedOnStructure);
    }
    
    private ClassificationResult classifyBasedOnStructure(DocumentStructureAnalysis analysis) {
        DocumentType detectedType = detectDocumentType(analysis);
        ClassificationConfidence confidence = calculateConfidence(analysis);
        
        return ClassificationResult.builder()
            .documentType(detectedType)
            .confidence(confidence)
            .confidenceScore(analysis.getConfidenceScore())
            .classificationMethod("google_document_ai_structure_analysis")
            .classificationFeatures(analysis.getKeyFeatures())
            .build();
    }
    
    private DocumentType detectDocumentType(DocumentStructureAnalysis analysis) {
        // Implement classification logic based on Document AI entities and structure
        if (analysis.hasInvoiceEntities()) {
            return DocumentType.INVOICE;
        } else if (analysis.hasFormStructure()) {
            return DocumentType.FORM_DOCUMENT;
        } else if (analysis.hasTableStructure()) {
            return DocumentType.TABLE_DOCUMENT;
        } else {
            return DocumentType.UNSTRUCTURED_TEXT;
        }
    }
}
```

## Step 5: Usage Examples

### 5.1 Invoice Processing

```java
@Service
public class InvoiceProcessingService {
    
    @Autowired
    private DataExtractionPort dataExtractionPort;
    
    public Mono<InvoiceData> processInvoice(UUID documentId) {
        return dataExtractionPort.extractWithTemplate(documentId, "invoice")
            .map(this::convertToInvoiceData);
    }
    
    private InvoiceData convertToInvoiceData(List<ExtractedData> extractedData) {
        Map<String, Object> entities = extractedData.stream()
            .collect(Collectors.toMap(
                ExtractedData::getFieldName,
                ExtractedData::getProcessedValue
            ));
            
        return InvoiceData.builder()
            .invoiceNumber((String) entities.get("invoice_id"))
            .supplierName((String) entities.get("supplier_name"))
            .invoiceDate(parseDate((String) entities.get("invoice_date")))
            .dueDate(parseDate((String) entities.get("due_date")))
            .netAmount(parseAmount((String) entities.get("net_amount")))
            .totalAmount(parseAmount((String) entities.get("total_amount")))
            .currency((String) entities.get("currency"))
            .lineItems(extractLineItems(extractedData))
            .build();
    }
}
```

### 5.2 Form Processing

```java
@Service
public class FormProcessingService {
    
    @Autowired
    private DocumentExtractionPort extractionPort;
    
    public Mono<Map<String, Object>> processForm(UUID documentId) {
        DocumentProcessingRequest request = DocumentProcessingRequest.builder()
            .documentId(documentId)
            .extractionTypes(List.of(ExtractionType.FORM_FIELDS))
            .documentType(DocumentType.FORM_DOCUMENT)
            .build();
            
        return extractionPort.processDocument(request)
            .map(result -> result.getExtractedData().stream()
                .collect(Collectors.toMap(
                    ExtractedData::getFieldName,
                    ExtractedData::getProcessedValue
                )));
    }
}
```

### 5.3 Batch Processing with Custom Processors

```java
@Service
public class BatchDocumentProcessor {
    
    @Autowired
    private DocumentExtractionPort extractionPort;
    
    public Flux<DocumentProcessingResult> processBatch(List<UUID> documentIds) {
        return Flux.fromIterable(documentIds)
            .flatMap(this::processDocument, 3) // Process 3 documents concurrently
            .onErrorContinue((error, item) -> 
                log.error("Failed to process document {}: {}", item, error.getMessage()));
    }
    
    private Mono<DocumentProcessingResult> processDocument(UUID documentId) {
        return extractionPort.processDocument(
            DocumentProcessingRequest.builder()
                .documentId(documentId)
                .extractionTypes(List.of(
                    ExtractionType.OCR_TEXT,
                    ExtractionType.FORM_FIELDS,
                    ExtractionType.TABLE_DATA
                ))
                .validationLevel(ValidationLevel.STANDARD)
                .build()
        );
    }
}
```

## Step 6: Monitoring and Troubleshooting

### 6.1 Enable Logging

```yaml
logging:
  level:
    com.firefly.core.ecm.adapter.google: DEBUG
    com.google.cloud.documentai: DEBUG
```

### 6.2 Health Checks

```java
@Component
public class GoogleDocumentAiHealthIndicator implements HealthIndicator {
    
    private final DocumentProcessorServiceClient client;
    private final GoogleDocumentAiProperties properties;
    
    @Override
    public Health health() {
        try {
            // Test connection by listing processors
            String parent = String.format("projects/%s/locations/%s", 
                properties.getProjectId(), properties.getLocation());
            client.listProcessors(parent);
            
            return Health.up()
                .withDetail("service", "Google Document AI")
                .withDetail("project", properties.getProjectId())
                .withDetail("location", properties.getLocation())
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("service", "Google Document AI")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### 6.3 Common Issues and Solutions

#### Issue: "Permission denied" Error
**Solution**: Verify service account permissions and API enablement.

#### Issue: "Processor not found" Error
**Solution**: Check processor IDs and ensure they exist in the correct location.

#### Issue: "Quota exceeded" Error
**Solution**: Monitor API usage and request quota increases if needed.

## Step 7: Advanced Features

### 7.1 Custom Processor Training

```java
@Service
public class CustomProcessorService {
    
    private final DocumentProcessorServiceClient client;
    
    public Mono<String> trainCustomProcessor(String displayName, List<String> trainingDocuments) {
        return Mono.fromCallable(() -> {
            // Create processor
            Processor processor = Processor.newBuilder()
                .setDisplayName(displayName)
                .setType("CUSTOM_EXTRACTION_PROCESSOR")
                .build();
                
            String parent = String.format("projects/%s/locations/%s", 
                properties.getProjectId(), properties.getLocation());
                
            Operation operation = client.createProcessorAsync(parent, processor).get();
            
            // Train processor with documents
            // Implementation depends on specific training requirements
            
            return processor.getName();
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
}
```

### 7.2 Performance Optimization

```java
@Configuration
public class GoogleDocumentAiOptimization {
    
    @Bean
    public DocumentProcessorServiceSettings documentProcessorSettings() {
        return DocumentProcessorServiceSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(getCredentials()))
            .setTransportChannelProvider(
                DocumentProcessorServiceSettings.defaultGrpcTransportProviderBuilder()
                    .setMaxInboundMessageSize(20 * 1024 * 1024) // 20MB
                    .setKeepAliveTime(Duration.ofMinutes(1))
                    .build())
            .build();
    }
    
    @Bean
    public ExecutorService documentProcessingExecutor() {
        return Executors.newFixedThreadPool(10);
    }
}
```

## Next Steps

1. Test the integration with sample documents
2. Create custom processors for your specific document types
3. Implement error handling and retry logic
4. Set up monitoring and alerting
5. Optimize performance based on your use case

For more advanced features and customization options, refer to the [Google Document AI documentation](https://cloud.google.com/document-ai/docs) and the Firefly ECM library documentation.
