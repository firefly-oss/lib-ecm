# AWS Textract Integration Guide

## Overview

This guide provides step-by-step instructions for integrating Amazon Textract with the Firefly OpenCore Platform ECM library. AWS Textract is a machine learning service that automatically extracts text, handwriting, and data from scanned documents.

## Prerequisites

- AWS Account with Textract service access
- AWS CLI configured or IAM credentials available
- Java 17 or higher
- Spring Boot 3.x application

## Step 1: AWS Setup

### 1.1 Create IAM Policy

Create an IAM policy with the necessary Textract permissions:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "textract:DetectDocumentText",
                "textract:AnalyzeDocument",
                "textract:StartDocumentTextDetection",
                "textract:GetDocumentTextDetection",
                "textract:StartDocumentAnalysis",
                "textract:GetDocumentAnalysis"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:PutObject"
            ],
            "Resource": [
                "arn:aws:s3:::your-textract-bucket/*"
            ]
        }
    ]
}
```

### 1.2 Create IAM User or Role

Create an IAM user or role and attach the policy created above.

### 1.3 Create S3 Bucket

Create an S3 bucket for storing documents to be processed:

```bash
aws s3 mb s3://your-textract-bucket --region us-east-1
```

## Step 2: Dependencies

Add the AWS SDK dependencies to your `pom.xml` (adapter implementation not yet available):

```xml
<!-- Note: AWS Textract adapter implementation is not yet available -->
<!-- You will need to implement the adapter using these dependencies -->

<!-- AWS SDK dependencies -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>textract</artifactId>
    <version>2.21.0</version>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.21.0</version>
</dependency>
```

## Step 3: Configuration

### 3.1 Application Properties

Configure the AWS Textract adapter in your `application.yml`:

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: "aws-textract"  # Note: adapter not yet implemented
    features:
      idp: true

    # Use existing connection settings
    connection:
      connect-timeout: PT30S
      read-timeout: PT5M
      retry-attempts: 3
      max-connections: 100

    # AWS Textract configuration (when adapter is implemented)
    properties:
      region: "us-east-1"
      bucket-name: "your-textract-bucket"
      access-key-id: "${AWS_ACCESS_KEY_ID}"
      secret-access-key: "${AWS_SECRET_ACCESS_KEY}"

      # Additional properties for AWS Textract adapter implementation
      async-processing: true
      max-pages: 3000
      confidence-threshold: 0.8
      detect-tables: true
      detect-forms: true
      detect-signatures: true

# Standard AWS SDK configuration (separate from Firefly ECM)
aws:
  region:
    static: us-east-1
  credentials:
    access-key: "${AWS_ACCESS_KEY_ID}"
    secret-key: "${AWS_SECRET_ACCESS_KEY}"
```

### 3.2 Environment Variables

Set the following environment variables:

```bash
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export AWS_DEFAULT_REGION=us-east-1
```

## Step 4: Adapter Implementation

**Note: The AWS Textract adapter is not yet implemented. This section shows how you would implement it using the provided port interfaces.**

Here's how you would implement the AWS Textract adapter for all four IDP port interfaces:

### 4.1 DocumentExtractionPort Implementation Example

```java
// Example implementation - not yet available in the library
@EcmAdapter(
    type = "aws-textract",
    description = "AWS Textract Document Extraction Adapter",
    supportedFeatures = {AdapterFeature.TEXT_EXTRACTION, AdapterFeature.OCR}
)
@Component
public class AwsTextractExtractionAdapter implements DocumentExtractionPort {
    
    private final TextractClient textractClient;
    private final S3Client s3Client;
    private final AwsTextractProperties properties;
    
    @Override
    public Mono<ExtractedData> extractText(UUID documentId, ExtractionType extractionType) {
        return getDocumentFromEcm(documentId)
            .flatMap(document -> uploadToS3(document))
            .flatMap(s3Location -> performTextExtraction(s3Location, extractionType))
            .map(this::convertToExtractedData);
    }
    
    @Override
    public Mono<DocumentProcessingResult> processDocument(DocumentProcessingRequest request) {
        return Mono.fromCallable(() -> {
            // Start async Textract job
            String jobId = startDocumentAnalysis(request);
            return pollForCompletion(jobId);
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    private String startDocumentAnalysis(DocumentProcessingRequest request) {
        StartDocumentAnalysisRequest analysisRequest = StartDocumentAnalysisRequest.builder()
            .documentLocation(DocumentLocation.builder()
                .s3Object(S3Object.builder()
                    .bucket(properties.getS3Bucket())
                    .name(getS3Key(request.getDocumentId()))
                    .build())
                .build())
            .featureTypes(mapExtractionTypesToFeatures(request.getExtractionTypes()))
            .build();
            
        StartDocumentAnalysisResponse response = textractClient.startDocumentAnalysis(analysisRequest);
        return response.jobId();
    }
}
```

### 4.2 DocumentClassificationPort Implementation Example

```java
// Example implementation - not yet available in the library
@EcmAdapter(type = "aws-textract")
@Component
public class AwsTextractClassificationAdapter implements DocumentClassificationPort {
    
    @Override
    public Mono<ClassificationResult> classifyDocument(UUID documentId) {
        return extractText(documentId)
            .map(this::analyzeDocumentStructure)
            .map(this::classifyBasedOnContent);
    }
    
    private ClassificationResult classifyBasedOnContent(DocumentStructure structure) {
        // Implement classification logic based on extracted structure
        DocumentType detectedType = detectDocumentType(structure);
        ClassificationConfidence confidence = calculateConfidence(structure);
        
        return ClassificationResult.builder()
            .documentType(detectedType)
            .confidence(confidence)
            .confidenceScore(calculateNumericConfidence(structure))
            .classificationMethod("aws_textract_structure_analysis")
            .build();
    }
}
```

## Step 5: Usage Examples

### 5.1 Basic Text Extraction

```java
@Service
public class DocumentService {
    
    @Autowired
    private DocumentExtractionPort extractionPort;
    
    public Mono<String> extractTextFromDocument(UUID documentId) {
        return extractionPort.extractText(documentId, ExtractionType.OCR_TEXT)
            .map(ExtractedData::getProcessedValue)
            .cast(String.class);
    }
}
```

### 5.2 Form Data Extraction

```java
@Service
public class InvoiceProcessingService {
    
    @Autowired
    private DataExtractionPort dataExtractionPort;
    
    public Mono<InvoiceData> processInvoice(UUID documentId) {
        return dataExtractionPort.extractFormFields(documentId)
            .collectMap(
                data -> data.getFieldName(),
                data -> data.getProcessedValue()
            )
            .map(this::mapToInvoiceData);
    }
    
    private InvoiceData mapToInvoiceData(Map<String, Object> fields) {
        return InvoiceData.builder()
            .invoiceNumber((String) fields.get("INVOICE_NUMBER"))
            .date(parseDate((String) fields.get("INVOICE_DATE")))
            .total(parseAmount((String) fields.get("TOTAL")))
            .build();
    }
}
```

### 5.3 Asynchronous Processing

```java
@Service
public class AsyncDocumentProcessor {
    
    @Autowired
    private DocumentExtractionPort extractionPort;
    
    public Mono<String> processLargeDocument(UUID documentId) {
        DocumentProcessingRequest request = DocumentProcessingRequest.builder()
            .documentId(documentId)
            .extractionTypes(List.of(
                ExtractionType.OCR_TEXT,
                ExtractionType.TABLE_DATA,
                ExtractionType.FORM_FIELDS
            ))
            .validationLevel(ValidationLevel.STANDARD)
            .timeoutSeconds(600)  // 10 minutes
            .build();
            
        return extractionPort.processDocument(request)
            .map(result -> result.getId().toString());
    }
}
```

## Step 6: Monitoring and Troubleshooting

### 6.1 Enable Logging

Add logging configuration to monitor Textract operations:

```yaml
logging:
  level:
    com.firefly.core.ecm.adapter.aws: DEBUG
    software.amazon.awssdk.services.textract: DEBUG
```

### 6.2 Metrics Collection

Implement custom metrics for monitoring:

```java
@Component
public class TextractMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordExtractionTime(Duration duration) {
        Timer.builder("textract.extraction.time")
            .register(meterRegistry)
            .record(duration);
    }
    
    public void recordExtractionSuccess() {
        Counter.builder("textract.extraction.success")
            .register(meterRegistry)
            .increment();
    }
    
    public void recordExtractionFailure(String errorType) {
        Counter.builder("textract.extraction.failure")
            .tag("error.type", errorType)
            .register(meterRegistry)
            .increment();
    }
}
```

### 6.3 Common Issues and Solutions

#### Issue: "Access Denied" Error
**Solution**: Verify IAM permissions and ensure the S3 bucket policy allows access.

#### Issue: "Document too large" Error
**Solution**: Enable asynchronous processing for documents larger than 5MB.

#### Issue: Low confidence scores
**Solution**: Ensure document quality is good and adjust confidence thresholds.

## Step 7: Advanced Configuration

### 7.1 Custom Document Types

Configure custom document type detection:

```yaml
firefly:
  ecm:
    aws:
      textract:
        custom-classifiers:
          - name: "company_invoices"
            keywords: ["INVOICE", "BILL TO", "TOTAL DUE"]
            confidence-boost: 0.1
          - name: "contracts"
            keywords: ["AGREEMENT", "TERMS", "SIGNATURE"]
            confidence-boost: 0.15
```

### 7.2 Performance Tuning

```yaml
firefly:
  ecm:
    aws:
      textract:
        # Parallel processing
        max-concurrent-jobs: 10
        
        # Caching
        cache-results: true
        cache-ttl: 3600  # seconds
        
        # Optimization
        skip-empty-pages: true
        merge-adjacent-text: true
```

## Next Steps

1. Test the integration with sample documents
2. Implement error handling and retry logic
3. Set up monitoring and alerting
4. Optimize performance based on your use case
5. Consider implementing custom classification models

For more advanced features and customization options, refer to the [AWS Textract documentation](https://docs.aws.amazon.com/textract/) and the Firefly ECM library documentation.
