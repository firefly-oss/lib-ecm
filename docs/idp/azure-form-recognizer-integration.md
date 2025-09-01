# Azure Form Recognizer Integration Guide

## Overview

This guide provides step-by-step instructions for integrating Azure Form Recognizer with the Firefly OpenCore Platform ECM library. Azure Form Recognizer is a cognitive service that uses machine learning to extract text, key-value pairs, selection marks, and table data from documents.

## Prerequisites

- Azure subscription with Form Recognizer service
- Azure CLI installed and configured
- Java 17 or higher
- Spring Boot 3.x application

## Step 1: Azure Setup

### 1.1 Create Form Recognizer Resource

Create a Form Recognizer resource in Azure:

```bash
# Create resource group
az group create --name firefly-ecm-rg --location eastus

# Create Form Recognizer resource
az cognitiveservices account create \
    --name firefly-form-recognizer \
    --resource-group firefly-ecm-rg \
    --kind FormRecognizer \
    --sku S0 \
    --location eastus \
    --yes
```

### 1.2 Get Service Keys

Retrieve the endpoint and keys:

```bash
# Get endpoint
az cognitiveservices account show \
    --name firefly-form-recognizer \
    --resource-group firefly-ecm-rg \
    --query "properties.endpoint" \
    --output tsv

# Get keys
az cognitiveservices account keys list \
    --name firefly-form-recognizer \
    --resource-group firefly-ecm-rg
```

### 1.3 Create Storage Account (Optional)

For large documents and custom models:

```bash
# Create storage account
az storage account create \
    --name fireflyecmstorage \
    --resource-group firefly-ecm-rg \
    --location eastus \
    --sku Standard_LRS

# Create container
az storage container create \
    --name documents \
    --account-name fireflyecmstorage
```

## Step 2: Dependencies

Add the Azure SDK dependencies to your `pom.xml` (adapter implementation not yet available):

```xml
<!-- Note: Azure Form Recognizer adapter implementation is not yet available -->
<!-- You will need to implement the adapter using these dependencies -->

<!-- Azure SDK dependencies -->
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-ai-formrecognizer</artifactId>
    <version>4.1.0</version>
</dependency>
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-storage-blob</artifactId>
    <version>12.23.0</version>
</dependency>
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-identity</artifactId>
    <version>1.10.0</version>
</dependency>
```

## Step 3: Configuration

### 3.1 Application Properties

Configure the Azure Form Recognizer adapter in your `application.yml`:

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: "azure-form-recognizer"  # Note: adapter not yet implemented
    features:
      idp: true

    # Use existing connection settings
    connection:
      connect-timeout: PT30S
      read-timeout: PT5M
      retry-attempts: 3
      max-connections: 100

    # Azure Form Recognizer configuration (when adapter is implemented)
    properties:
      endpoint: "https://firefly-form-recognizer.cognitiveservices.azure.com/"
      api-key: "${AZURE_FORM_RECOGNIZER_KEY}"

      # Alternative: Use Azure AD authentication
      # client-id: "${AZURE_CLIENT_ID}"
      # client-secret: "${AZURE_CLIENT_SECRET}"
      # tenant-id: "${AZURE_TENANT_ID}"

      # Additional properties for Azure Form Recognizer adapter implementation
      api-version: "2023-07-31"
      confidence-threshold: 0.8
      include-text-details: true
      include-selection-marks: true
      storage-account-name: "fireflyecmstorage"
      storage-account-key: "${AZURE_STORAGE_KEY}"
      storage-container-name: "documents"
      invoice-model-id: "your-custom-invoice-model-id"
      contract-model-id: "your-custom-contract-model-id"

# Standard Azure SDK configuration (separate from Firefly ECM)
azure:
  application-id: "firefly-ecm-app"
```

### 3.2 Environment Variables

Set the following environment variables:

```bash
export AZURE_FORM_RECOGNIZER_KEY=your-form-recognizer-key
export AZURE_STORAGE_KEY=your-storage-account-key

# For Azure AD authentication (alternative)
export AZURE_CLIENT_ID=your-client-id
export AZURE_CLIENT_SECRET=your-client-secret
export AZURE_TENANT_ID=your-tenant-id
```

## Step 4: Adapter Implementation

**Note: The Azure Form Recognizer adapter is not yet implemented. This section shows how you would implement it using the provided port interfaces.**

Here's how you would implement the Azure Form Recognizer adapter for all four IDP port interfaces:

### 4.1 DocumentExtractionPort Implementation

```java
@EcmAdapter(
    type = "azure-form-recognizer",
    description = "Azure Form Recognizer Document Extraction Adapter",
    supportedFeatures = {AdapterFeature.TEXT_EXTRACTION, AdapterFeature.OCR, AdapterFeature.FORM_PROCESSING}
)
@Component
public class AzureFormRecognizerExtractionAdapter implements DocumentExtractionPort {
    
    private final DocumentAnalysisClient documentAnalysisClient;
    private final BlobServiceClient blobServiceClient;
    private final AzureFormRecognizerProperties properties;
    
    @Override
    public Mono<ExtractedData> extractText(UUID documentId, ExtractionType extractionType) {
        return getDocumentFromEcm(documentId)
            .flatMap(document -> analyzeDocument(document, "prebuilt-read"))
            .map(this::convertToExtractedData);
    }
    
    @Override
    public Mono<DocumentProcessingResult> processDocument(DocumentProcessingRequest request) {
        return Mono.fromCallable(() -> {
            String modelId = selectModelForDocument(request);
            return analyzeDocumentWithModel(request.getDocumentId(), modelId);
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    private Mono<AnalyzeResult> analyzeDocument(Document document, String modelId) {
        return Mono.fromCallable(() -> {
            BinaryData documentData = BinaryData.fromBytes(document.getContent());
            
            SyncPoller<OperationResult, AnalyzeResult> analyzePoller = 
                documentAnalysisClient.beginAnalyzeDocument(modelId, documentData);
                
            return analyzePoller.getFinalResult();
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    private String selectModelForDocument(DocumentProcessingRequest request) {
        // Select appropriate model based on document type or request parameters
        return switch (request.getDocumentType()) {
            case INVOICE -> "prebuilt-invoice";
            case RECEIPT -> "prebuilt-receipt";
            case BUSINESS_CARD -> "prebuilt-businessCard";
            case IDENTITY_DOCUMENT -> "prebuilt-idDocument";
            default -> "prebuilt-document";
        };
    }
}
```

### 4.2 DataExtractionPort Implementation

```java
@EcmAdapter(type = "azure-form-recognizer")
@Component
public class AzureFormRecognizerDataAdapter implements DataExtractionPort {
    
    @Override
    public Flux<ExtractedData> extractFormFields(UUID documentId) {
        return analyzeWithModel(documentId, "prebuilt-document")
            .flatMapMany(result -> Flux.fromIterable(result.getKeyValuePairs()))
            .map(this::convertKeyValuePairToExtractedData);
    }
    
    @Override
    public Flux<ExtractedData> extractTableData(UUID documentId) {
        return analyzeWithModel(documentId, "prebuilt-document")
            .flatMapMany(result -> Flux.fromIterable(result.getTables()))
            .flatMap(this::convertTableToExtractedData);
    }
    
    @Override
    public Mono<List<ExtractedData>> extractWithTemplate(UUID documentId, String templateId) {
        String customModelId = properties.getCustomModels().get(templateId);
        if (customModelId == null) {
            return Mono.error(new IllegalArgumentException("Unknown template: " + templateId));
        }
        
        return analyzeWithModel(documentId, customModelId)
            .map(this::convertAnalyzeResultToExtractedData);
    }
    
    private Flux<ExtractedData> convertTableToExtractedData(DocumentTable table) {
        return Flux.fromIterable(table.getCells())
            .map(cell -> ExtractedData.builder()
                .extractionType(ExtractionType.TABLE_DATA)
                .fieldName(String.format("table_%d_row_%d_col_%d", 
                    table.getRowCount(), cell.getRowIndex(), cell.getColumnIndex()))
                .rawValue(cell.getContent())
                .processedValue(cell.getContent())
                .confidence((int) (cell.getConfidence() * 100))
                .boundingBox(convertBoundingRegion(cell.getBoundingRegions()))
                .build());
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
    
    @Autowired
    private DocumentClassificationPort classificationPort;
    
    public Mono<InvoiceData> processInvoice(UUID documentId) {
        return classificationPort.classifyDocument(documentId)
            .filter(result -> result.getDocumentType() == DocumentType.INVOICE)
            .flatMap(classification -> extractInvoiceData(documentId))
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Document is not an invoice")));
    }
    
    private Mono<InvoiceData> extractInvoiceData(UUID documentId) {
        return dataExtractionPort.extractWithTemplate(documentId, "invoice-model")
            .map(this::mapToInvoiceData);
    }
    
    private InvoiceData mapToInvoiceData(List<ExtractedData> extractedData) {
        Map<String, Object> fields = extractedData.stream()
            .collect(Collectors.toMap(
                ExtractedData::getFieldName,
                ExtractedData::getProcessedValue
            ));
            
        return InvoiceData.builder()
            .invoiceId((String) fields.get("InvoiceId"))
            .vendorName((String) fields.get("VendorName"))
            .invoiceDate(parseDate((String) fields.get("InvoiceDate")))
            .dueDate(parseDate((String) fields.get("DueDate")))
            .subtotal(parseAmount((String) fields.get("SubTotal")))
            .totalTax(parseAmount((String) fields.get("TotalTax")))
            .invoiceTotal(parseAmount((String) fields.get("InvoiceTotal")))
            .lineItems(extractLineItems(extractedData))
            .build();
    }
}
```

### 5.2 Receipt Processing

```java
@Service
public class ReceiptProcessingService {
    
    @Autowired
    private DocumentExtractionPort extractionPort;
    
    public Mono<ReceiptData> processReceipt(UUID documentId) {
        DocumentProcessingRequest request = DocumentProcessingRequest.builder()
            .documentId(documentId)
            .extractionTypes(List.of(ExtractionType.KEY_VALUE_PAIRS, ExtractionType.LINE_ITEMS))
            .documentType(DocumentType.RECEIPT)
            .build();
            
        return extractionPort.processDocument(request)
            .map(this::convertToReceiptData);
    }
    
    private ReceiptData convertToReceiptData(DocumentProcessingResult result) {
        List<ExtractedData> extractedData = result.getExtractedData();
        
        return ReceiptData.builder()
            .merchantName(findFieldValue(extractedData, "MerchantName"))
            .transactionDate(parseDate(findFieldValue(extractedData, "TransactionDate")))
            .total(parseAmount(findFieldValue(extractedData, "Total")))
            .items(extractReceiptItems(extractedData))
            .build();
    }
}
```

### 5.3 Custom Model Training

```java
@Service
public class CustomModelService {
    
    private final DocumentModelAdministrationClient adminClient;
    
    public Mono<String> trainCustomModel(String modelName, List<String> trainingDocumentUrls) {
        return Mono.fromCallable(() -> {
            BuildDocumentModelOptions options = new BuildDocumentModelOptions()
                .setModelId(modelName)
                .setDescription("Custom model for " + modelName);
                
            SyncPoller<OperationResult, DocumentModelDetails> buildPoller = 
                adminClient.beginBuildDocumentModel(trainingDocumentUrls, 
                    DocumentBuildMode.TEMPLATE, options);
                    
            DocumentModelDetails model = buildPoller.getFinalResult();
            return model.getModelId();
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
}
```

## Step 6: Monitoring and Troubleshooting

### 6.1 Enable Logging

```yaml
logging:
  level:
    com.firefly.core.ecm.adapter.azure: DEBUG
    com.azure.ai.formrecognizer: DEBUG
```

### 6.2 Health Checks

```java
@Component
public class AzureFormRecognizerHealthIndicator implements HealthIndicator {
    
    private final DocumentAnalysisClient client;
    
    @Override
    public Health health() {
        try {
            // Test connection with a simple operation
            client.getResourceDetails();
            return Health.up()
                .withDetail("service", "Azure Form Recognizer")
                .withDetail("status", "Connected")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("service", "Azure Form Recognizer")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### 6.3 Common Issues and Solutions

#### Issue: "Unauthorized" Error
**Solution**: Verify API key and endpoint configuration.

#### Issue: "Model not found" Error
**Solution**: Ensure custom model IDs are correct and models are trained.

#### Issue: Low extraction accuracy
**Solution**: Use prebuilt models for standard documents or train custom models.

## Step 7: Advanced Features

### 7.1 Batch Processing

```java
@Service
public class BatchProcessingService {
    
    @Autowired
    private DocumentExtractionPort extractionPort;
    
    public Flux<DocumentProcessingResult> processBatch(List<UUID> documentIds) {
        return Flux.fromIterable(documentIds)
            .flatMap(this::processDocument, 5) // Process 5 documents concurrently
            .onErrorContinue((error, item) -> 
                log.error("Failed to process document {}: {}", item, error.getMessage()));
    }
    
    private Mono<DocumentProcessingResult> processDocument(UUID documentId) {
        DocumentProcessingRequest request = DocumentProcessingRequest.builder()
            .documentId(documentId)
            .extractionTypes(List.of(ExtractionType.OCR_TEXT, ExtractionType.FORM_FIELDS))
            .build();
            
        return extractionPort.processDocument(request);
    }
}
```

### 7.2 Custom Classification

```java
@Component
public class CustomDocumentClassifier {
    
    public DocumentType classifyDocument(List<ExtractedData> extractedData) {
        String text = extractedData.stream()
            .map(ExtractedData::getRawValue)
            .collect(Collectors.joining(" "));
            
        if (containsInvoiceKeywords(text)) {
            return DocumentType.INVOICE;
        } else if (containsContractKeywords(text)) {
            return DocumentType.CONTRACT;
        } else {
            return DocumentType.UNKNOWN;
        }
    }
    
    private boolean containsInvoiceKeywords(String text) {
        return text.toLowerCase().contains("invoice") || 
               text.toLowerCase().contains("bill to") ||
               text.toLowerCase().contains("total due");
    }
}
```

## Next Steps

1. Test the integration with sample documents
2. Train custom models for your specific document types
3. Implement error handling and retry logic
4. Set up monitoring and alerting
5. Optimize performance based on your use case

For more advanced features and customization options, refer to the [Azure Form Recognizer documentation](https://docs.microsoft.com/en-us/azure/cognitive-services/form-recognizer/) and the Firefly ECM library documentation.
