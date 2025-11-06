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

import com.firefly.core.ecm.domain.enums.idp.DocumentType;
import com.firefly.core.ecm.domain.model.idp.ClassificationResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Port interface for document classification and categorization operations in Intelligent Document Processing (IDP).
 * 
 * <p>This interface defines the contract for automatically classifying and categorizing documents
 * using various IDP technologies such as machine learning models, rule-based systems, and
 * template matching. It follows the hexagonal architecture pattern where this port defines
 * the business requirements, and adapters provide concrete implementations for different
 * IDP providers.</p>
 * 
 * <p>Key capabilities provided by this port:</p>
 * <ul>
 *   <li><strong>Document Type Classification:</strong> Identify document types (invoice, contract, etc.)</li>
 *   <li><strong>Content Categorization:</strong> Classify documents by content and purpose</li>
 *   <li><strong>Industry Classification:</strong> Categorize by business domain or industry</li>
 *   <li><strong>Format Detection:</strong> Identify document structure and layout patterns</li>
 *   <li><strong>Language Detection:</strong> Identify document language and locale</li>
 *   <li><strong>Confidence Scoring:</strong> Provide reliability metrics for classifications</li>
 * </ul>
 * 
 * <p>The interface supports both single document classification and batch processing,
 * with configurable classification models and custom taxonomies. All operations return
 * reactive types (Mono/Flux) for non-blocking processing.</p>
 * 
 * <p>Typical usage patterns:</p>
 * <pre>
 * {@code
 * // Classify a single document
 * Mono<ClassificationResult> result = classificationPort.classifyDocument(documentId);
 * 
 * // Classify with custom configuration
 * Map<String, Object> config = Map.of("model", "financial_documents", "threshold", 0.8);
 * Mono<ClassificationResult> result = classificationPort.classifyDocumentWithConfig(documentId, config);
 * 
 * // Batch classification
 * Flux<ClassificationResult> results = classificationPort.classifyDocuments(documentIds);
 * }
 * </pre>
 * 
 * @author Firefly Software Solutions Inc.
 * @since 1.0.0
 * @see ClassificationResult
 * @see DocumentType
 */
public interface DocumentClassificationPort {
    
    /**
     * Classify a document to determine its type and category.
     * 
     * <p>This method performs automatic document classification using the default
     * classification model and settings. It analyzes document content, structure,
     * and visual features to determine the most likely document type.</p>
     * 
     * <p>The classification process includes:</p>
     * <ul>
     *   <li>Document content analysis and feature extraction</li>
     *   <li>Layout and structure pattern recognition</li>
     *   <li>Text analysis and keyword detection</li>
     *   <li>Confidence scoring and alternative suggestions</li>
     * </ul>
     * 
     * @param documentId the Long of the document to classify
     * @return a Mono containing the classification result with confidence scores
     * @throws IllegalArgumentException if documentId is null
     * @see ClassificationResult
     * @see DocumentType
     */
    Mono<ClassificationResult> classifyDocument(UUID documentId);
    
    /**
     * Classify a document with custom configuration parameters.
     * 
     * <p>This method allows for fine-tuned document classification with custom
     * configuration options such as specific classification models, confidence
     * thresholds, target taxonomies, and processing preferences.</p>
     * 
     * <p>Common configuration parameters include:</p>
     * <ul>
     *   <li><code>model</code> - Specific classification model to use</li>
     *   <li><code>threshold</code> - Minimum confidence threshold for classification</li>
     *   <li><code>taxonomy</code> - Target classification taxonomy or schema</li>
     *   <li><code>language</code> - Expected document language for optimization</li>
     *   <li><code>industry</code> - Industry context for specialized classification</li>
     *   <li><code>maxAlternatives</code> - Maximum number of alternative classifications</li>
     * </ul>
     * 
     * @param documentId the Long of the document to classify
     * @param config configuration parameters for the classification process
     * @return a Mono containing the classification result with metadata
     * @throws IllegalArgumentException if documentId is null or config contains invalid parameters
     */
    Mono<ClassificationResult> classifyDocumentWithConfig(UUID documentId, Map<String, Object> config);
    
    /**
     * Classify a document provided as an input stream.
     * 
     * <p>This method allows for document classification from documents that are not yet
     * stored in the ECM system. The document content is provided directly as an input
     * stream, making it suitable for real-time classification scenarios.</p>
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
     * @return a Mono containing the classification result
     * @throws IllegalArgumentException if documentStream is null or mimeType is unsupported
     */
    Mono<ClassificationResult> classifyDocumentFromStream(InputStream documentStream, String mimeType);
    
    /**
     * Classify multiple documents in a batch operation.
     * 
     * <p>This method performs classification on multiple documents efficiently,
     * optimizing performance through batch processing. It's more efficient than
     * multiple individual classification calls for large document sets.</p>
     * 
     * <p>The batch processing includes:</p>
     * <ul>
     *   <li>Parallel processing of multiple documents</li>
     *   <li>Resource optimization and load balancing</li>
     *   <li>Error isolation (failures don't affect other documents)</li>
     *   <li>Progress tracking and partial results</li>
     * </ul>
     * 
     * @param documentIds list of document UUIDs to classify
     * @return a Flux of classification results, one for each document
     * @throws IllegalArgumentException if documentIds is null or empty
     */
    Flux<ClassificationResult> classifyDocuments(List<UUID> documentIds);
    
    /**
     * Classify documents by content similarity to a reference document.
     * 
     * <p>This method finds and classifies documents that are similar to a reference
     * document based on content, structure, and other characteristics. It's useful
     * for finding documents of the same type or category.</p>
     * 
     * <p>The similarity classification includes:</p>
     * <ul>
     *   <li>Content similarity analysis</li>
     *   <li>Structural pattern matching</li>
     *   <li>Visual layout comparison</li>
     *   <li>Semantic similarity scoring</li>
     * </ul>
     * 
     * @param referenceDocumentId the Long of the reference document
     * @param candidateDocumentIds list of candidate document UUIDs to classify
     * @param similarityThreshold minimum similarity score (0.0 to 1.0)
     * @return a Flux of classification results for similar documents
     * @throws IllegalArgumentException if referenceDocumentId is null or threshold is invalid
     */
    Flux<ClassificationResult> classifyBySimilarity(UUID referenceDocumentId, List<UUID> candidateDocumentIds, Double similarityThreshold);
    
    /**
     * Train or update a custom classification model with labeled examples.
     * 
     * <p>This method allows for training custom classification models using
     * organization-specific document examples. It supports both initial training
     * and incremental updates to existing models.</p>
     * 
     * <p>The training process includes:</p>
     * <ul>
     *   <li>Feature extraction from training documents</li>
     *   <li>Model training or fine-tuning</li>
     *   <li>Validation and performance assessment</li>
     *   <li>Model deployment and versioning</li>
     * </ul>
     * 
     * @param modelName the name of the custom model to train
     * @param trainingData map of document IDs to their correct classifications
     * @param trainingConfig configuration parameters for the training process
     * @return a Mono containing the training result and model performance metrics
     * @throws IllegalArgumentException if modelName is null or trainingData is empty
     */
    Mono<Map<String, Object>> trainCustomModel(String modelName, Map<UUID, DocumentType> trainingData, Map<String, Object> trainingConfig);
    
    /**
     * Get available classification models and their capabilities.
     * 
     * <p>This method returns information about all available classification models,
     * including their supported document types, languages, and performance
     * characteristics. This helps in selecting the appropriate model for specific use cases.</p>
     * 
     * @return a Flux of model information including names, capabilities, and metadata
     */
    Flux<Map<String, Object>> getAvailableModels();
    
    /**
     * Get supported document types for classification.
     * 
     * <p>This method returns the list of document types that can be identified
     * by the current adapter implementation. Different IDP providers may
     * support different sets of document types.</p>
     * 
     * @return a Flux of supported document types
     * @see DocumentType
     */
    Flux<DocumentType> getSupportedDocumentTypes();
    
    /**
     * Get classification confidence threshold recommendations.
     * 
     * <p>This method provides recommended confidence thresholds for different
     * use cases and quality requirements. It helps in configuring appropriate
     * thresholds for automatic vs. manual review decisions.</p>
     * 
     * @param useCase the use case context (e.g., "high_accuracy", "high_throughput")
     * @return a Mono containing recommended threshold values and guidelines
     */
    Mono<Map<String, Double>> getConfidenceThresholds(String useCase);
    
    /**
     * Validate classification results against known ground truth.
     * 
     * <p>This method validates classification results against manually verified
     * ground truth data to assess model performance and accuracy. It's useful
     * for quality assurance and model evaluation.</p>
     * 
     * @param classificationResults the classification results to validate
     * @param groundTruth the verified correct classifications
     * @return a Mono containing validation metrics and performance statistics
     */
    Mono<Map<String, Object>> validateClassifications(List<ClassificationResult> classificationResults, Map<UUID, DocumentType> groundTruth);
    
    /**
     * Get supported document formats for classification.
     * 
     * <p>This method returns the list of document MIME types that are supported
     * for classification by the current adapter implementation.</p>
     * 
     * @return a Flux of supported MIME types (e.g., "application/pdf", "image/jpeg")
     */
    Flux<String> getSupportedFormats();
    
    /**
     * Returns the name of the adapter implementation for identification and logging.
     * 
     * <p>This method provides a way to identify which specific adapter implementation
     * is being used. The name should be descriptive and unique among all available
     * adapters (e.g., "AWS Comprehend Classifier", "Azure Cognitive Services Classifier").</p>
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
