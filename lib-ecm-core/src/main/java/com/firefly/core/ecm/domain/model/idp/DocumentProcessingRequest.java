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

import com.firefly.core.ecm.domain.enums.idp.DocumentType;
import com.firefly.core.ecm.domain.enums.idp.ExtractionType;
import com.firefly.core.ecm.domain.enums.idp.ProcessingStatus;
import com.firefly.core.ecm.domain.enums.idp.ValidationLevel;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Document processing request entity for Intelligent Document Processing (IDP) operations.
 * 
 * <p>This entity represents a request to process a document using IDP capabilities
 * such as text extraction, classification, data extraction, and validation.
 * Uses UUID for request ID and Long for user IDs as per Firefly standards.</p>
 * 
 * <p>The processing request contains all necessary information to:</p>
 * <ul>
 *   <li>Identify the document to be processed</li>
 *   <li>Specify the types of processing operations to perform</li>
 *   <li>Configure processing parameters and validation levels</li>
 *   <li>Track the processing status and progress</li>
 *   <li>Store processing results and metadata</li>
 * </ul>
 * 
 * <p>Processing requests follow a lifecycle from creation through completion,
 * with status updates tracked throughout the process.</p>
 * 
 * @author Firefly Software Solutions Inc.
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class DocumentProcessingRequest {
    
    /**
     * Unique processing request identifier (UUID).
     * 
     * <p>This ID uniquely identifies the processing request across
     * the entire system and is used for tracking and referencing.</p>
     */
    private final UUID id;
    
    /**
     * Document ID to be processed (UUID).
     * 
     * <p>References the document in the ECM system that should
     * be processed by the IDP operations.</p>
     */
    private final UUID documentId;
    
    /**
     * User who created the processing request (UUID).
     *
     * <p>Identifies the user who initiated the IDP processing
     * request for audit and authorization purposes.</p>
     */
    private final Long requestedBy;

    /**
     * Owner of the document being processed (UUID).
     *
     * <p>Identifies the owner of the document for access control
     * and permission validation during processing.</p>
     */
    private final UUID ownerId;
    
    /**
     * Current processing status.
     * 
     * <p>Tracks the current state of the processing request
     * throughout its lifecycle.</p>
     */
    private final ProcessingStatus status;
    
    /**
     * Expected or detected document type.
     * 
     * <p>Specifies the type of document being processed, which
     * influences the processing strategies and extraction patterns used.</p>
     */
    private final DocumentType documentType;
    
    /**
     * List of extraction types to perform.
     * 
     * <p>Specifies which types of data extraction should be
     * performed on the document (OCR, key-value pairs, tables, etc.).</p>
     */
    private final List<ExtractionType> extractionTypes;
    
    /**
     * Validation level to apply during processing.
     * 
     * <p>Determines the rigor and depth of validation checks
     * applied to extracted data and processing results.</p>
     */
    private final ValidationLevel validationLevel;
    
    /**
     * Processing priority level (1-10, where 10 is highest).
     * 
     * <p>Indicates the priority of this processing request
     * relative to other requests in the processing queue.</p>
     */
    private final Integer priority;
    
    /**
     * Request creation timestamp.
     * 
     * <p>Records when the processing request was initially created.</p>
     */
    private final Instant createdAt;
    
    /**
     * Processing start timestamp.
     * 
     * <p>Records when the actual processing began, which may be
     * different from the creation time due to queuing.</p>
     */
    private final Instant startedAt;
    
    /**
     * Processing completion timestamp.
     * 
     * <p>Records when the processing was completed, either
     * successfully or with failure.</p>
     */
    private final Instant completedAt;
    
    /**
     * Last status update timestamp.
     * 
     * <p>Records when the processing status was last updated,
     * useful for tracking progress and detecting stalled requests.</p>
     */
    private final Instant lastUpdatedAt;
    
    /**
     * Processing timeout in seconds.
     * 
     * <p>Maximum time allowed for processing before the request
     * is automatically cancelled. Null means no timeout.</p>
     */
    private final Integer timeoutSeconds;
    
    /**
     * Processing configuration parameters.
     * 
     * <p>Additional configuration options specific to the processing
     * operations, such as OCR settings, confidence thresholds, etc.</p>
     */
    private final Map<String, Object> processingConfig;
    
    /**
     * Request metadata and custom properties.
     * 
     * <p>Additional metadata associated with the processing request,
     * such as business context, workflow information, etc.</p>
     */
    private final Map<String, Object> metadata;
    
    /**
     * External processing job ID from IDP provider.
     * 
     * <p>Reference ID from the external IDP service (AWS Textract,
     * Azure Form Recognizer, etc.) for tracking and correlation.</p>
     */
    private final String externalJobId;
    
    /**
     * Processing error message if failed.
     * 
     * <p>Detailed error information if the processing request
     * failed, including error codes and diagnostic information.</p>
     */
    private final String errorMessage;
    
    /**
     * Processing progress percentage (0-100).
     * 
     * <p>Current progress of the processing operation, useful
     * for long-running operations and user feedback.</p>
     */
    private final Integer progressPercentage;
    
    /**
     * Estimated completion time.
     * 
     * <p>Estimated timestamp when the processing is expected
     * to complete, based on current progress and system load.</p>
     */
    private final Instant estimatedCompletionAt;
    
    /**
     * Number of retry attempts made.
     * 
     * <p>Tracks how many times this processing request has been
     * retried after failures, useful for retry logic and debugging.</p>
     */
    private final Integer retryCount;
    
    /**
     * Maximum number of retry attempts allowed.
     * 
     * <p>Maximum number of times this request should be retried
     * before being marked as permanently failed.</p>
     */
    private final Integer maxRetries;
    
    /**
     * Callback URL for processing completion notifications.
     * 
     * <p>Optional webhook URL to notify when processing is complete,
     * useful for asynchronous processing workflows.</p>
     */
    private final String callbackUrl;
    
    /**
     * Language hint for text processing.
     * 
     * <p>Expected language of the document content, used to
     * optimize OCR and text extraction accuracy.</p>
     */
    private final String languageHint;
    
    /**
     * Whether to perform automatic document classification.
     * 
     * <p>Indicates if the system should attempt to automatically
     * classify the document type if not explicitly specified.</p>
     */
    private final Boolean autoClassify;
}
