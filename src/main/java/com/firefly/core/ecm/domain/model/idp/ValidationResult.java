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

import com.firefly.core.ecm.domain.enums.idp.ValidationLevel;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Validation result entity representing document and data validation outcomes from IDP processing.
 * 
 * <p>This entity represents the result of validation operations performed on
 * documents and extracted data during IDP processing. Validation ensures data
 * quality, consistency, and compliance with business rules and standards.</p>
 * 
 * <p>The validation result includes:</p>
 * <ul>
 *   <li>Overall validation status and outcome</li>
 *   <li>Individual validation rule results</li>
 *   <li>Error and warning details</li>
 *   <li>Quality metrics and scores</li>
 *   <li>Compliance and regulatory validation results</li>
 * </ul>
 * 
 * <p>Validation results help determine whether extracted data is suitable
 * for downstream processing and business use.</p>
 * 
 * @author Firefly Software Solutions Inc.
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class ValidationResult {
    
    /**
     * Unique validation result identifier (UUID).
     * 
     * <p>This ID uniquely identifies this validation result
     * within the processing operation.</p>
     */
    private final UUID id;
    
    /**
     * Validation level that was applied.
     * 
     * <p>The level of validation rigor that was applied
     * during the validation process.</p>
     */
    private final ValidationLevel validationLevel;
    
    /**
     * Overall validation status.
     * 
     * <p>Overall result of the validation process
     * (PASSED, FAILED, WARNING, PARTIAL).</p>
     */
    private final String validationStatus;
    
    /**
     * Whether all validation rules passed.
     * 
     * <p>Indicates if all applied validation rules
     * passed successfully without errors.</p>
     */
    private final Boolean allRulesPassed;
    
    /**
     * Individual validation rule results.
     * 
     * <p>Detailed results for each validation rule
     * that was applied during the validation process.</p>
     */
    private final List<ValidationRuleResult> ruleResults;
    
    /**
     * Validation errors encountered.
     * 
     * <p>List of validation errors that prevented
     * successful validation of the data.</p>
     */
    private final List<ValidationError> errors;
    
    /**
     * Validation warnings generated.
     * 
     * <p>List of validation warnings that indicate
     * potential issues but don't prevent validation success.</p>
     */
    private final List<ValidationWarning> warnings;
    
    /**
     * Overall validation score (0-100).
     * 
     * <p>Aggregate score representing the overall
     * quality and validity of the validated data.</p>
     */
    private final Integer validationScore;
    
    /**
     * Data quality metrics.
     * 
     * <p>Various quality metrics calculated during
     * validation (completeness, accuracy, consistency, etc.).</p>
     */
    private final Map<String, Double> qualityMetrics;
    
    /**
     * Compliance validation results.
     * 
     * <p>Results of compliance checks against
     * regulatory standards and industry requirements.</p>
     */
    private final Map<String, Boolean> complianceResults;
    
    /**
     * Validation timestamp.
     * 
     * <p>When the validation process was performed.</p>
     */
    private final Instant validatedAt;
    
    /**
     * Validation processing time in milliseconds.
     * 
     * <p>Time taken to complete the validation process,
     * useful for performance monitoring.</p>
     */
    private final Long processingTimeMs;
    
    /**
     * Validator name or system that performed validation.
     * 
     * <p>Identifies the validation system or component
     * that performed the validation checks.</p>
     */
    private final String validatorName;
    
    /**
     * Validation rules version.
     * 
     * <p>Version of the validation rules or schema
     * that was applied during validation.</p>
     */
    private final String rulesVersion;
    
    /**
     * Additional validation metadata.
     * 
     * <p>Additional properties and metadata associated
     * with the validation result.</p>
     */
    private final Map<String, Object> metadata;
    
    /**
     * Individual validation rule result.
     * 
     * <p>Represents the result of applying a single
     * validation rule to the data.</p>
     */
    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    public static class ValidationRuleResult {
        
        /**
         * Validation rule identifier.
         */
        private final String ruleId;
        
        /**
         * Validation rule name or description.
         */
        private final String ruleName;
        
        /**
         * Whether the rule passed validation.
         */
        private final Boolean passed;
        
        /**
         * Rule validation message.
         */
        private final String message;
        
        /**
         * Severity level of the rule (ERROR, WARNING, INFO).
         */
        private final String severity;
        
        /**
         * Field or data element that was validated.
         */
        private final String fieldName;
        
        /**
         * Expected value or pattern for the rule.
         */
        private final String expectedValue;
        
        /**
         * Actual value that was validated.
         */
        private final String actualValue;
    }
    
    /**
     * Validation error details.
     * 
     * <p>Represents a validation error that prevented
     * successful validation of the data.</p>
     */
    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    public static class ValidationError {
        
        /**
         * Error code or identifier.
         */
        private final String errorCode;
        
        /**
         * Error message description.
         */
        private final String message;
        
        /**
         * Field or data element that caused the error.
         */
        private final String fieldName;
        
        /**
         * Invalid value that caused the error.
         */
        private final String invalidValue;
        
        /**
         * Suggested correction or valid value.
         */
        private final String suggestedValue;
        
        /**
         * Error severity level.
         */
        private final String severity;
    }
    
    /**
     * Validation warning details.
     * 
     * <p>Represents a validation warning that indicates
     * a potential issue but doesn't prevent validation success.</p>
     */
    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    public static class ValidationWarning {
        
        /**
         * Warning code or identifier.
         */
        private final String warningCode;
        
        /**
         * Warning message description.
         */
        private final String message;
        
        /**
         * Field or data element that triggered the warning.
         */
        private final String fieldName;
        
        /**
         * Value that triggered the warning.
         */
        private final String value;
        
        /**
         * Recommended action or suggestion.
         */
        private final String recommendation;
    }
}
