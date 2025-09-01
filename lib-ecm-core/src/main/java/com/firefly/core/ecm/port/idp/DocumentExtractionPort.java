/*
 * Copyright 2024 Firefly Software Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.firefly.core.ecm.port.idp;

import com.firefly.core.ecm.domain.enums.idp.ExtractionType;
import com.firefly.core.ecm.domain.model.idp.DocumentProcessingRequest;
import com.firefly.core.ecm.domain.model.idp.DocumentProcessingResult;
import com.firefly.core.ecm.domain.model.idp.ExtractedData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Port interface for document text extraction and OCR operations in Intelligent Document Processing (IDP).
 * 
 * <p>This interface defines the contract for extracting text and other content from documents
 * using various IDP technologies such as Optical Character Recognition (OCR), handwriting
 * recognition, and advanced text analysis. It follows the hexagonal architecture pattern
 * where this port defines the business requirements, and adapters provide concrete
 * implementations for different IDP providers.</p>
 * 
 * <p>Key capabilities provided by this port:</p>
 * <ul>
 *   <li><strong>OCR Processing:</strong> Extract printed text from images and scanned documents</li>
 *   <li><strong>Handwriting Recognition:</strong> Extract handwritten text and signatures</li>
 *   <li><strong>Layout Analysis:</strong> Understand document structure and reading order</li>
 *   <li><strong>Multi-format Support:</strong> Process various document formats (PDF, images, etc.)</li>
 *   <li><strong>Language Detection:</strong> Identify and process multiple languages</li>
 *   <li><strong>Quality Assessment:</strong> Provide confidence scores and quality metrics</li>
 * </ul>
 * 
 * <p>The interface supports both synchronous and asynchronous processing patterns,
 * allowing for real-time extraction as well as batch processing of large documents.
 * All operations return reactive types (Mono/Flux) for non-blocking processing.</p>
 * 
 * <p>Typical usage patterns:</p>
 * <pre>
 * {@code
 * // Extract all text from a document
 * Mono<ExtractedData> textResult = extractionPort.extractText(documentId, ExtractionType.OCR_TEXT);
 * 
 * // Process document with specific configuration
 * DocumentProcessingRequest request = DocumentProcessingRequest.builder()
 *     .documentId(documentId)
 *     .extractionTypes(List.of(ExtractionType.OCR_TEXT, ExtractionType.HANDWRITING_RECOGNITION))
 *     .build();
 * Mono<DocumentProcessingResult> result = extractionPort.processDocument(request);
 * }
 * </pre>
 * 
 * @author Firefly Software Solutions Inc.
 * @since 1.0.0
 * @see DocumentProcessingRequest
 * @see DocumentProcessingResult
 * @see ExtractedData
 * @see ExtractionType
 */
public interface DocumentExtractionPort {
    
    /**
     * Extract text from a document using the specified extraction type.
     * 
     * <p>This method performs text extraction on a single document using the specified
     * extraction method. It supports various extraction types including OCR for printed
     * text, handwriting recognition, and full text extraction with formatting.</p>
     * 
     * <p>The extraction process includes:</p>
     * <ul>
     *   <li>Document format detection and preprocessing</li>
     *   <li>Text extraction using the specified method</li>
     *   <li>Confidence scoring and quality assessment</li>
     *   <li>Location and layout information capture</li>
     * </ul>
     * 
     * @param documentId the UUID of the document to extract text from
     * @param extractionType the type of text extraction to perform
     * @return a Mono containing the extracted text data with metadata
     * @throws IllegalArgumentException if documentId is null or extractionType is not supported
     * @see ExtractionType
     * @see ExtractedData
     */
    Mono<ExtractedData> extractText(UUID documentId, ExtractionType extractionType);
    
    /**
     * Extract text from a document with custom configuration parameters.
     * 
     * <p>This method allows for fine-tuned text extraction with custom configuration
     * options such as language hints, OCR engine settings, confidence thresholds,
     * and output format preferences.</p>
     * 
     * <p>Common configuration parameters include:</p>
     * <ul>
     *   <li><code>language</code> - Expected document language (e.g., "en", "es", "fr")</li>
     *   <li><code>ocrEngine</code> - Specific OCR engine to use</li>
     *   <li><code>confidenceThreshold</code> - Minimum confidence score for text acceptance</li>
     *   <li><code>preserveLayout</code> - Whether to maintain original document layout</li>
     *   <li><code>includeCoordinates</code> - Whether to include text position coordinates</li>
     * </ul>
     * 
     * @param documentId the UUID of the document to extract text from
     * @param extractionType the type of text extraction to perform
     * @param config configuration parameters for the extraction process
     * @return a Mono containing the extracted text data with metadata
     * @throws IllegalArgumentException if documentId is null or config contains invalid parameters
     */
    Mono<ExtractedData> extractTextWithConfig(UUID documentId, ExtractionType extractionType, Map<String, Object> config);
    
    /**
     * Extract multiple types of data from a document in a single operation.
     * 
     * <p>This method performs multiple extraction operations on a single document,
     * optimizing performance by processing the document once for all requested
     * extraction types. This is more efficient than multiple individual extraction calls.</p>
     * 
     * <p>The method supports combining different extraction types such as:</p>
     * <ul>
     *   <li>OCR text extraction with handwriting recognition</li>
     *   <li>Text extraction with image and signature detection</li>
     *   <li>Full text with table and form field extraction</li>
     * </ul>
     * 
     * @param documentId the UUID of the document to process
     * @param extractionTypes list of extraction types to perform
     * @return a Flux of extracted data, one for each successful extraction type
     * @throws IllegalArgumentException if documentId is null or extractionTypes is empty
     */
    Flux<ExtractedData> extractMultipleTypes(UUID documentId, List<ExtractionType> extractionTypes);
    
    /**
     * Process a document according to a comprehensive processing request.
     * 
     * <p>This method handles complete document processing based on a detailed
     * processing request that specifies extraction types, validation levels,
     * processing configuration, and other parameters. It provides the most
     * comprehensive processing capabilities.</p>
     * 
     * <p>The processing includes:</p>
     * <ul>
     *   <li>Document preprocessing and optimization</li>
     *   <li>Multiple extraction operations as specified</li>
     *   <li>Quality assessment and confidence scoring</li>
     *   <li>Result aggregation and formatting</li>
     *   <li>Error handling and retry logic</li>
     * </ul>
     * 
     * @param request the comprehensive document processing request
     * @return a Mono containing the complete processing result
     * @throws IllegalArgumentException if request is null or contains invalid parameters
     * @see DocumentProcessingRequest
     * @see DocumentProcessingResult
     */
    Mono<DocumentProcessingResult> processDocument(DocumentProcessingRequest request);
    
    /**
     * Extract text from a document provided as an input stream.
     * 
     * <p>This method allows for text extraction from documents that are not yet
     * stored in the ECM system. The document content is provided directly as
     * an input stream, making it suitable for real-time processing scenarios.</p>
     * 
     * <p>The method handles:</p>
     * <ul>
     *   <li>Document format detection from the stream</li>
     *   <li>Temporary processing without permanent storage</li>
     *   <li>Memory-efficient streaming processing</li>
     *   <li>Cleanup of temporary resources</li>
     * </ul>
     * 
     * @param documentStream the input stream containing the document content
     * @param mimeType the MIME type of the document (e.g., "application/pdf", "image/jpeg")
     * @param extractionType the type of text extraction to perform
     * @return a Mono containing the extracted text data
     * @throws IllegalArgumentException if documentStream is null or mimeType is unsupported
     */
    Mono<ExtractedData> extractTextFromStream(InputStream documentStream, String mimeType, ExtractionType extractionType);
    
    /**
     * Get the processing status of an asynchronous extraction operation.
     * 
     * <p>This method allows tracking the progress of long-running extraction
     * operations that were started asynchronously. It provides status information
     * including progress percentage, estimated completion time, and any errors.</p>
     * 
     * @param requestId the UUID of the processing request to check
     * @return a Mono containing the current processing request status
     * @throws IllegalArgumentException if requestId is null
     * @see DocumentProcessingRequest
     */
    Mono<DocumentProcessingRequest> getProcessingStatus(UUID requestId);
    
    /**
     * Cancel an ongoing asynchronous extraction operation.
     * 
     * <p>This method attempts to cancel a running extraction operation.
     * Cancellation may not be immediate and depends on the current processing
     * stage and the capabilities of the underlying IDP provider.</p>
     * 
     * @param requestId the UUID of the processing request to cancel
     * @return a Mono containing true if cancellation was successful, false otherwise
     * @throws IllegalArgumentException if requestId is null
     */
    Mono<Boolean> cancelProcessing(UUID requestId);
    
    /**
     * Get supported extraction types for this adapter implementation.
     * 
     * <p>This method returns the list of extraction types that are supported
     * by the current adapter implementation. Different IDP providers may
     * support different sets of extraction capabilities.</p>
     * 
     * @return a Flux of supported extraction types
     * @see ExtractionType
     */
    Flux<ExtractionType> getSupportedExtractionTypes();
    
    /**
     * Get supported document formats for extraction.
     * 
     * <p>This method returns the list of document MIME types that are supported
     * for text extraction by the current adapter implementation.</p>
     * 
     * @return a Flux of supported MIME types (e.g., "application/pdf", "image/jpeg")
     */
    Flux<String> getSupportedFormats();
    
    /**
     * Returns the name of the adapter implementation for identification and logging.
     * 
     * <p>This method provides a way to identify which specific adapter implementation
     * is being used. The name should be descriptive and unique among all available
     * adapters (e.g., "AWS Textract Adapter", "Azure Form Recognizer Adapter").</p>
     * 
     * <p>This information is useful for:</p>
     * <ul>
     *   <li>Logging and debugging</li>
     *   <li>Administrative monitoring</li>
     *   <li>Feature capability detection</li>
     *   <li>Error reporting and diagnostics</li>
     * </ul>
     * 
     * @return a non-null string identifying the adapter implementation
     */
    String getAdapterName();
}
