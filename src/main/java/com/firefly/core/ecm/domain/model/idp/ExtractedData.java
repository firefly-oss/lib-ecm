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

import com.firefly.core.ecm.domain.enums.idp.ExtractionType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Extracted data entity representing data extracted from documents during IDP processing.
 * 
 * <p>This entity represents a single piece of extracted data from a document,
 * including the extracted value, its location within the document, confidence
 * metrics, and associated metadata. Each extraction operation may produce
 * multiple ExtractedData instances.</p>
 * 
 * <p>The extracted data includes:</p>
 * <ul>
 *   <li>The actual extracted value and its data type</li>
 *   <li>Location information within the source document</li>
 *   <li>Confidence and quality metrics</li>
 *   <li>Extraction method and processing details</li>
 *   <li>Validation status and results</li>
 * </ul>
 * 
 * <p>This structure supports various types of extracted data including
 * text, numbers, dates, key-value pairs, table data, and complex objects.</p>
 * 
 * @author Firefly Software Solutions Inc.
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class ExtractedData {
    
    /**
     * Unique extracted data identifier (UUID).
     * 
     * <p>This ID uniquely identifies this piece of extracted data
     * within the processing result.</p>
     */
    private final UUID id;
    
    /**
     * Type of extraction that produced this data.
     * 
     * <p>Indicates which extraction operation produced this data
     * (OCR_TEXT, KEY_VALUE_PAIRS, TABLE_DATA, etc.).</p>
     */
    private final ExtractionType extractionType;
    
    /**
     * Field name or label for the extracted data.
     * 
     * <p>Human-readable name or label identifying what this
     * extracted data represents (e.g., "Invoice Number", "Total Amount").</p>
     */
    private final String fieldName;
    
    /**
     * Raw extracted value as string.
     * 
     * <p>The raw extracted value exactly as it appeared in the
     * source document, without any processing or normalization.</p>
     */
    private final String rawValue;
    
    /**
     * Processed and normalized value.
     * 
     * <p>The extracted value after processing, normalization,
     * and type conversion (e.g., formatted dates, cleaned numbers).</p>
     */
    private final Object processedValue;
    
    /**
     * Data type of the extracted value.
     * 
     * <p>The detected or inferred data type of the extracted value
     * (STRING, NUMBER, DATE, BOOLEAN, OBJECT, ARRAY, etc.).</p>
     */
    private final String dataType;
    
    /**
     * Confidence score for this extraction (0-100).
     * 
     * <p>Confidence level indicating how certain the IDP system
     * is about the accuracy of this extracted data.</p>
     */
    private final Integer confidence;
    
    /**
     * Page number where the data was found (1-based).
     * 
     * <p>The page number in the source document where this
     * data was located and extracted from.</p>
     */
    private final Integer pageNumber;
    
    /**
     * Bounding box coordinates for the extracted data.
     * 
     * <p>Coordinates defining the rectangular area in the document
     * where this data was found. Format: [x1, y1, x2, y2] or
     * [left, top, right, bottom].</p>
     */
    private final List<Double> boundingBox;
    
    /**
     * Text region or line number where the data was found.
     * 
     * <p>Additional location information such as text region,
     * line number, or paragraph identifier.</p>
     */
    private final String textRegion;
    
    /**
     * OCR confidence score if applicable (0-100).
     * 
     * <p>Specific confidence score for OCR text recognition
     * if this data was extracted using OCR technology.</p>
     */
    private final Integer ocrConfidence;
    
    /**
     * Whether this data was validated successfully.
     * 
     * <p>Indicates if validation checks were performed on
     * this extracted data and whether they passed.</p>
     */
    private final Boolean validated;
    
    /**
     * Validation error messages if validation failed.
     * 
     * <p>List of validation errors encountered when validating
     * this extracted data against business rules or formats.</p>
     */
    private final List<String> validationErrors;
    
    /**
     * Alternative extraction candidates.
     * 
     * <p>Other possible values that were considered during
     * extraction, useful for manual review and correction.</p>
     */
    private final List<String> alternatives;
    
    /**
     * Extraction method or algorithm used.
     * 
     * <p>Information about the specific method or algorithm
     * used to extract this data (e.g., "regex", "ml_model", "template").</p>
     */
    private final String extractionMethod;
    
    /**
     * Source text context around the extracted data.
     * 
     * <p>Surrounding text context that provides additional
     * information about where and how this data was extracted.</p>
     */
    private final String sourceContext;
    
    /**
     * Language of the extracted text if applicable.
     * 
     * <p>Detected language of the extracted text content,
     * useful for multilingual document processing.</p>
     */
    private final String language;
    
    /**
     * Font information if available.
     * 
     * <p>Font family, size, and style information for the
     * extracted text, if available from the source document.</p>
     */
    private final Map<String, Object> fontInfo;
    
    /**
     * Whether this data was manually corrected.
     * 
     * <p>Indicates if this extracted data was modified or
     * corrected by human review after initial extraction.</p>
     */
    private final Boolean manuallyCorrected;
    
    /**
     * Original value before manual correction.
     * 
     * <p>The original extracted value before any manual
     * corrections were applied during human review.</p>
     */
    private final String originalValue;
    
    /**
     * User who made manual corrections (Long).
     * 
     * <p>Identifies the user who performed manual corrections
     * on this extracted data, if applicable.</p>
     */
    private final Long correctedBy;
    
    /**
     * Additional metadata for the extracted data.
     * 
     * <p>Additional properties and metadata associated with
     * this extracted data, such as business rules applied,
     * processing flags, or custom attributes.</p>
     */
    private final Map<String, Object> metadata;
    
    /**
     * Related extracted data items.
     * 
     * <p>References to other ExtractedData items that are
     * related to this one (e.g., line items in a table,
     * components of a complex field).</p>
     */
    private final List<UUID> relatedDataIds;
    
    /**
     * Quality score for this extraction (0-100).
     * 
     * <p>Overall quality assessment of this extracted data
     * considering confidence, validation, and other factors.</p>
     */
    private final Integer qualityScore;
    
    /**
     * Whether this data requires human review.
     * 
     * <p>Indicates if this extracted data should be flagged
     * for human review due to low confidence or quality issues.</p>
     */
    private final Boolean requiresReview;
}
