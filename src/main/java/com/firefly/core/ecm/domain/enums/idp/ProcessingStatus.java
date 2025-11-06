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
package com.firefly.core.ecm.domain.enums.idp;

/**
 * Processing status enumeration for Intelligent Document Processing (IDP) operations.
 * 
 * <p>This enumeration defines the various states that a document processing request
 * can be in during its lifecycle. It provides a standardized way to track the
 * progress of IDP operations across different adapters and implementations.</p>
 * 
 * <p>The processing lifecycle typically follows this flow:</p>
 * <ol>
 *   <li><strong>PENDING:</strong> Request submitted but not yet started</li>
 *   <li><strong>PROCESSING:</strong> Active processing in progress</li>
 *   <li><strong>COMPLETED:</strong> Processing finished successfully</li>
 *   <li><strong>FAILED:</strong> Processing encountered an error</li>
 * </ol>
 * 
 * <p>Additional states handle special cases like cancellation, validation,
 * and partial completion scenarios.</p>
 * 
 * @author Firefly Software Solutions Inc.
 * @since 1.0.0
 */
public enum ProcessingStatus {
    
    /**
     * Processing request has been submitted but not yet started.
     * 
     * <p>This is the initial state when a document processing request
     * is created and queued for processing.</p>
     */
    PENDING,
    
    /**
     * Document processing is currently in progress.
     * 
     * <p>The IDP system is actively analyzing the document and
     * extracting information.</p>
     */
    PROCESSING,
    
    /**
     * Document processing has completed successfully.
     * 
     * <p>All requested operations (extraction, classification, validation)
     * have been completed and results are available.</p>
     */
    COMPLETED,
    
    /**
     * Document processing has failed due to an error.
     * 
     * <p>Processing could not be completed due to technical issues,
     * unsupported document format, or other errors.</p>
     */
    FAILED,
    
    /**
     * Document processing has been cancelled by user request.
     * 
     * <p>Processing was stopped before completion, either by explicit
     * user action or system timeout.</p>
     */
    CANCELLED,
    
    /**
     * Document processing completed but requires manual validation.
     * 
     * <p>Automated processing finished but confidence levels are below
     * threshold, requiring human review and validation.</p>
     */
    REQUIRES_VALIDATION,
    
    /**
     * Document processing completed partially with some operations successful.
     * 
     * <p>Some processing operations completed successfully while others
     * failed or could not be performed.</p>
     */
    PARTIALLY_COMPLETED,
    
    /**
     * Document processing is queued and waiting for available resources.
     * 
     * <p>Request is in the processing queue but waiting for system
     * resources or rate limiting constraints.</p>
     */
    QUEUED,
    
    /**
     * Document processing has been paused and can be resumed.
     * 
     * <p>Processing was temporarily stopped but can be continued
     * from the current state.</p>
     */
    PAUSED,
    
    /**
     * Document processing is being retried after a previous failure.
     * 
     * <p>System is attempting to reprocess the document after
     * a recoverable error occurred.</p>
     */
    RETRYING,
    
    /**
     * Document processing completed but with warnings or quality issues.
     * 
     * <p>Processing finished successfully but with lower confidence
     * or quality indicators that may require attention.</p>
     */
    COMPLETED_WITH_WARNINGS,
    
    /**
     * Document processing timed out before completion.
     * 
     * <p>Processing exceeded the maximum allowed time limit and
     * was terminated automatically.</p>
     */
    TIMEOUT,
    
    /**
     * Document processing is being validated by human reviewers.
     * 
     * <p>Automated processing completed but results are under
     * human review for accuracy verification.</p>
     */
    UNDER_REVIEW,
    
    /**
     * Document processing results have been approved after review.
     * 
     * <p>Human validation completed and results have been
     * approved for use.</p>
     */
    APPROVED,
    
    /**
     * Document processing results have been rejected after review.
     * 
     * <p>Human validation completed but results were deemed
     * inaccurate or insufficient.</p>
     */
    REJECTED
}
