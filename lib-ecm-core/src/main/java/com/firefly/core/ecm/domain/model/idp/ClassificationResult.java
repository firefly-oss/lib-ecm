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

import com.firefly.core.ecm.domain.enums.idp.ClassificationConfidence;
import com.firefly.core.ecm.domain.enums.idp.DocumentType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Classification result entity representing document classification outcomes from IDP processing.
 * 
 * <p>This entity represents the result of document classification operations,
 * including the identified document type, confidence levels, alternative
 * classifications, and supporting evidence. Classification helps determine
 * the appropriate processing strategies and extraction patterns.</p>
 * 
 * <p>The classification result includes:</p>
 * <ul>
 *   <li>Primary classification with confidence level</li>
 *   <li>Alternative classifications and their confidence scores</li>
 *   <li>Evidence and features that support the classification</li>
 *   <li>Classification method and model information</li>
 *   <li>Quality metrics and validation status</li>
 * </ul>
 * 
 * <p>Classification results can be used to route documents to appropriate
 * processing workflows and apply document-type-specific extraction rules.</p>
 * 
 * @author Firefly Software Solutions Inc.
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class ClassificationResult {
    
    /**
     * Unique classification result identifier (UUID).
     * 
     * <p>This ID uniquely identifies this classification result
     * within the processing operation.</p>
     */
    private final UUID id;
    
    /**
     * Primary classified document type.
     * 
     * <p>The document type that the classification system
     * determined as the most likely match for the document.</p>
     */
    private final DocumentType documentType;
    
    /**
     * Confidence level for the primary classification.
     * 
     * <p>Confidence level indicating how certain the system
     * is about the primary document type classification.</p>
     */
    private final ClassificationConfidence confidence;
    
    /**
     * Numerical confidence score (0-100).
     * 
     * <p>Precise numerical confidence score for the primary
     * classification, providing more granular confidence information.</p>
     */
    private final Double confidenceScore;
    
    /**
     * Alternative classification candidates.
     * 
     * <p>Other possible document types that were considered
     * during classification, ranked by confidence score.</p>
     */
    private final List<AlternativeClassification> alternatives;
    
    /**
     * Classification features and evidence.
     * 
     * <p>Key features, patterns, or evidence that supported
     * the classification decision (e.g., keywords, layout patterns).</p>
     */
    private final List<String> classificationFeatures;
    
    /**
     * Classification method or algorithm used.
     * 
     * <p>Information about the classification method used
     * (e.g., "ml_model", "rule_based", "template_matching").</p>
     */
    private final String classificationMethod;
    
    /**
     * Model name and version used for classification.
     * 
     * <p>Identifies the specific AI/ML model and version
     * used for document classification.</p>
     */
    private final String modelVersion;
    
    /**
     * Classification categories or tags.
     * 
     * <p>Additional categorical information about the document
     * beyond the primary document type (e.g., "financial", "legal").</p>
     */
    private final List<String> categories;
    
    /**
     * Document subtype or variant.
     * 
     * <p>More specific subtype within the primary document type
     * (e.g., "purchase_invoice" vs "service_invoice").</p>
     */
    private final String subtype;
    
    /**
     * Industry or domain classification.
     * 
     * <p>Industry-specific classification if the document
     * is associated with a particular business domain.</p>
     */
    private final String industry;
    
    /**
     * Language of the document.
     * 
     * <p>Detected primary language of the document content,
     * which may influence classification decisions.</p>
     */
    private final String language;
    
    /**
     * Document format classification.
     * 
     * <p>Classification of the document format characteristics
     * (e.g., "structured", "semi_structured", "unstructured").</p>
     */
    private final String format;
    
    /**
     * Layout classification.
     * 
     * <p>Classification of the document layout type
     * (e.g., "form", "table", "text_heavy", "image_heavy").</p>
     */
    private final String layout;
    
    /**
     * Quality assessment of the classification.
     * 
     * <p>Overall quality score for the classification result
     * considering various factors like confidence, consistency, etc.</p>
     */
    private final Integer qualityScore;
    
    /**
     * Whether the classification was validated.
     * 
     * <p>Indicates if the classification result was validated
     * against known patterns, rules, or human review.</p>
     */
    private final Boolean validated;
    
    /**
     * Validation errors if classification validation failed.
     * 
     * <p>List of validation errors encountered when validating
     * the classification result.</p>
     */
    private final List<String> validationErrors;
    
    /**
     * Whether this classification requires human review.
     * 
     * <p>Indicates if the classification result should be
     * reviewed by a human due to low confidence or ambiguity.</p>
     */
    private final Boolean requiresReview;
    
    /**
     * Processing time for classification in milliseconds.
     * 
     * <p>Time taken to perform the classification operation,
     * useful for performance monitoring.</p>
     */
    private final Long processingTimeMs;
    
    /**
     * Raw classification scores from the model.
     * 
     * <p>Raw output scores from the classification model
     * for all considered document types.</p>
     */
    private final Map<String, Double> rawScores;
    
    /**
     * Additional metadata for the classification result.
     * 
     * <p>Additional properties and metadata associated with
     * the classification, such as processing parameters,
     * business context, or custom attributes.</p>
     */
    private final Map<String, Object> metadata;
    
    /**
     * Alternative classification candidate.
     * 
     * <p>Represents an alternative document type classification
     * with its associated confidence score.</p>
     */
    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    public static class AlternativeClassification {
        
        /**
         * Alternative document type.
         */
        private final DocumentType documentType;
        
        /**
         * Confidence level for this alternative.
         */
        private final ClassificationConfidence confidence;
        
        /**
         * Numerical confidence score (0-100).
         */
        private final Double confidenceScore;
        
        /**
         * Reason or evidence for this alternative classification.
         */
        private final String reason;
    }
}
