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
package com.firefly.core.ecm.domain.model.idp;

import com.firefly.core.ecm.domain.enums.idp.ProcessingStatus;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Document processing result entity for Intelligent Document Processing (IDP) operations.
 * 
 * <p>This entity represents the complete results of a document processing request,
 * including extracted data, classification results, validation outcomes, and
 * processing metadata. Uses UUID for result ID and references.</p>
 * 
 * <p>The processing result contains:</p>
 * <ul>
 *   <li>Overall processing status and outcome</li>
 *   <li>Extracted data from various processing operations</li>
 *   <li>Classification and validation results</li>
 *   <li>Quality metrics and confidence scores</li>
 *   <li>Processing performance and timing information</li>
 *   <li>Error details and diagnostic information</li>
 * </ul>
 * 
 * <p>Results are immutable once processing is complete and serve as a
 * permanent record of the IDP processing outcome.</p>
 * 
 * @author Firefly Software Solutions Inc.
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class DocumentProcessingResult {
    
    /**
     * Unique processing result identifier (UUID).
     * 
     * <p>This ID uniquely identifies the processing result and
     * can be used for referencing and retrieval.</p>
     */
    private final UUID id;
    
    /**
     * Processing request ID that generated this result (UUID).
     * 
     * <p>References the original processing request that
     * produced this result for traceability.</p>
     */
    private final UUID requestId;
    
    /**
     * Document ID that was processed (UUID).
     * 
     * <p>References the document that was processed to
     * generate this result.</p>
     */
    private final UUID documentId;
    
    /**
     * Final processing status.
     * 
     * <p>The final status of the processing operation
     * (COMPLETED, FAILED, PARTIALLY_COMPLETED, etc.).</p>
     */
    private final ProcessingStatus status;
    
    /**
     * List of extracted data from the document.
     * 
     * <p>Contains all data extracted during processing,
     * organized by extraction type and structured for easy access.</p>
     */
    private final List<ExtractedData> extractedData;
    
    /**
     * Document classification results.
     * 
     * <p>Results of document classification operations,
     * including detected document types and confidence levels.</p>
     */
    private final List<ClassificationResult> classificationResults;
    
    /**
     * Data validation results.
     * 
     * <p>Results of validation operations performed on
     * extracted data and document content.</p>
     */
    private final List<ValidationResult> validationResults;
    
    /**
     * Overall confidence score for the processing results (0-100).
     * 
     * <p>Aggregate confidence score representing the overall
     * reliability of the processing results.</p>
     */
    private final Integer overallConfidence;
    
    /**
     * Quality metrics for the processing operation.
     * 
     * <p>Various quality indicators such as OCR accuracy,
     * extraction completeness, validation pass rates, etc.</p>
     */
    private final Map<String, Double> qualityMetrics;
    
    /**
     * Processing start timestamp.
     * 
     * <p>When the processing operation actually began.</p>
     */
    private final Instant processedAt;
    
    /**
     * Processing completion timestamp.
     * 
     * <p>When the processing operation was completed.</p>
     */
    private final Instant completedAt;
    
    /**
     * Total processing duration.
     * 
     * <p>Total time taken to complete the processing operation,
     * useful for performance monitoring and optimization.</p>
     */
    private final Duration processingDuration;
    
    /**
     * Processing warnings encountered during operation.
     * 
     * <p>Non-fatal issues encountered during processing that
     * may affect result quality but didn't cause failure.</p>
     */
    private final List<String> warnings;
    
    /**
     * Processing errors encountered during operation.
     * 
     * <p>Error messages and details for any failures or
     * issues encountered during processing.</p>
     */
    private final List<String> errors;
    
    /**
     * Detailed processing statistics.
     * 
     * <p>Detailed metrics about the processing operation,
     * such as pages processed, characters extracted, etc.</p>
     */
    private final Map<String, Object> processingStats;
    
    /**
     * External processing job ID from IDP provider.
     * 
     * <p>Reference ID from the external IDP service used
     * for this processing operation.</p>
     */
    private final String externalJobId;
    
    /**
     * IDP adapter name that performed the processing.
     * 
     * <p>Identifies which specific IDP adapter was used
     * for processing (e.g., "AWS Textract", "Azure Form Recognizer").</p>
     */
    private final String adapterName;
    
    /**
     * Processing model version used by the IDP provider.
     * 
     * <p>Version information for the AI/ML models used
     * during processing, useful for result interpretation.</p>
     */
    private final String modelVersion;
    
    /**
     * Raw response data from the IDP provider.
     * 
     * <p>Original response data from the external IDP service,
     * preserved for debugging and advanced analysis.</p>
     */
    private final Map<String, Object> rawResponse;
    
    /**
     * Result metadata and additional information.
     * 
     * <p>Additional metadata associated with the processing
     * result, such as business context, workflow state, etc.</p>
     */
    private final Map<String, Object> metadata;
    
    /**
     * Whether the result requires human review.
     * 
     * <p>Indicates if the processing result should be
     * reviewed by a human due to low confidence or quality issues.</p>
     */
    private final Boolean requiresReview;
    
    /**
     * Review status if human review was performed.
     * 
     * <p>Status of human review process if the result
     * was flagged for manual validation.</p>
     */
    private final String reviewStatus;
    
    /**
     * User who reviewed the result (Long).
     * 
     * <p>Identifies the user who performed manual review
     * of the processing result, if applicable.</p>
     */
    private final Long reviewedBy;
    
    /**
     * Review completion timestamp.
     * 
     * <p>When the human review was completed, if applicable.</p>
     */
    private final Instant reviewedAt;
    
    /**
     * Review comments and feedback.
     * 
     * <p>Comments and feedback from human reviewers
     * about the processing result quality and accuracy.</p>
     */
    private final String reviewComments;
    
    /**
     * Whether the result has been approved for use.
     * 
     * <p>Indicates if the processing result has been
     * approved for downstream use and business processes.</p>
     */
    private final Boolean approved;
    
    /**
     * Result archival status.
     * 
     * <p>Indicates if the processing result has been
     * archived for long-term storage and compliance.</p>
     */
    private final Boolean archived;
    
    /**
     * Archive timestamp if result was archived.
     * 
     * <p>When the processing result was archived for
     * long-term storage.</p>
     */
    private final Instant archivedAt;
}
