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

import com.firefly.core.ecm.domain.enums.idp.ValidationLevel;
import com.firefly.core.ecm.domain.model.idp.ExtractedData;
import com.firefly.core.ecm.domain.model.idp.ValidationResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Port interface for document validation and verification operations in Intelligent Document Processing (IDP).
 * 
 * <p>This interface defines the contract for validating documents and extracted data
 * using various validation techniques including business rule validation, format
 * verification, data consistency checks, and compliance validation. It follows the
 * hexagonal architecture pattern where this port defines the business requirements,
 * and adapters provide concrete implementations for different validation providers.</p>
 * 
 * <p>Key capabilities provided by this port:</p>
 * <ul>
 *   <li><strong>Data Format Validation:</strong> Verify data types, formats, and patterns</li>
 *   <li><strong>Business Rule Validation:</strong> Apply domain-specific validation rules</li>
 *   <li><strong>Cross-field Validation:</strong> Validate relationships between data fields</li>
 *   <li><strong>Compliance Validation:</strong> Check against regulatory and industry standards</li>
 *   <li><strong>Document Integrity:</strong> Verify document completeness and authenticity</li>
 *   <li><strong>Quality Assessment:</strong> Evaluate data quality and reliability metrics</li>
 * </ul>
 * 
 * <p>The interface supports multiple validation levels from basic format checks to
 * comprehensive compliance validation. All operations return reactive types (Mono/Flux)
 * for non-blocking processing and can be configured for different validation rigor levels.</p>
 * 
 * <p>Typical usage patterns:</p>
 * <pre>
 * {@code
 * // Validate extracted data with standard rules
 * Mono<ValidationResult> result = validationPort.validateExtractedData(extractedData, ValidationLevel.STANDARD);
 * 
 * // Validate document with custom rules
 * Map<String, Object> rules = Map.of("invoice_total_max", 10000, "required_fields", List.of("date", "amount"));
 * Mono<ValidationResult> result = validationPort.validateDocumentWithRules(documentId, rules);
 * 
 * // Batch validation
 * Flux<ValidationResult> results = validationPort.validateMultipleDocuments(documentIds, ValidationLevel.COMPREHENSIVE);
 * }
 * </pre>
 * 
 * @author Firefly Software Solutions Inc.
 * @since 1.0.0
 * @see ValidationResult
 * @see ValidationLevel
 * @see ExtractedData
 */
public interface DocumentValidationPort {
    
    /**
     * Validate a document using the specified validation level.
     * 
     * <p>This method performs comprehensive document validation including content
     * verification, format validation, and business rule checks. The validation
     * level determines the depth and rigor of validation performed.</p>
     * 
     * <p>The validation process includes:</p>
     * <ul>
     *   <li>Document format and structure validation</li>
     *   <li>Content completeness and consistency checks</li>
     *   <li>Business rule application based on document type</li>
     *   <li>Quality assessment and confidence scoring</li>
     * </ul>
     * 
     * @param documentId the UUID of the document to validate
     * @param validationLevel the level of validation rigor to apply
     * @return a Mono containing the validation result with detailed findings
     * @throws IllegalArgumentException if documentId is null or validationLevel is not supported
     * @see ValidationLevel
     * @see ValidationResult
     */
    Mono<ValidationResult> validateDocument(UUID documentId, ValidationLevel validationLevel);
    
    /**
     * Validate extracted data against predefined rules and patterns.
     * 
     * <p>This method validates extracted data to ensure accuracy, completeness,
     * and compliance with expected formats and business rules. It's typically
     * used after data extraction to verify the quality of extracted information.</p>
     * 
     * <p>The validation includes:</p>
     * <ul>
     *   <li>Data type and format verification</li>
     *   <li>Range and constraint validation</li>
     *   <li>Pattern matching and regex validation</li>
     *   <li>Cross-field consistency checks</li>
     * </ul>
     * 
     * @param extractedData the list of extracted data to validate
     * @param validationLevel the level of validation rigor to apply
     * @return a Mono containing the validation result for the extracted data
     * @throws IllegalArgumentException if extractedData is null or empty
     */
    Mono<ValidationResult> validateExtractedData(List<ExtractedData> extractedData, ValidationLevel validationLevel);
    
    /**
     * Validate a document with custom validation rules.
     * 
     * <p>This method allows for validation using custom business rules and
     * constraints specific to the organization or use case. Rules can be
     * defined for specific document types, fields, or business scenarios.</p>
     * 
     * <p>Common rule types include:</p>
     * <ul>
     *   <li><code>required_fields</code> - List of mandatory fields</li>
     *   <li><code>field_formats</code> - Expected formats for specific fields</li>
     *   <li><code>value_ranges</code> - Acceptable value ranges for numeric fields</li>
     *   <li><code>cross_field_rules</code> - Relationships between fields</li>
     *   <li><code>business_rules</code> - Domain-specific validation logic</li>
     * </ul>
     * 
     * @param documentId the UUID of the document to validate
     * @param validationRules custom validation rules and constraints
     * @return a Mono containing the validation result with rule-specific findings
     * @throws IllegalArgumentException if documentId is null or validationRules is invalid
     */
    Mono<ValidationResult> validateDocumentWithRules(UUID documentId, Map<String, Object> validationRules);
    
    /**
     * Validate multiple documents in a batch operation.
     * 
     * <p>This method performs validation on multiple documents efficiently,
     * optimizing performance through batch processing. It's more efficient than
     * multiple individual validation calls for large document sets.</p>
     * 
     * <p>The batch validation includes:</p>
     * <ul>
     *   <li>Parallel processing of multiple documents</li>
     *   <li>Resource optimization and load balancing</li>
     *   <li>Error isolation (failures don't affect other documents)</li>
     *   <li>Progress tracking and partial results</li>
     * </ul>
     * 
     * @param documentIds list of document UUIDs to validate
     * @param validationLevel the level of validation rigor to apply
     * @return a Flux of validation results, one for each document
     * @throws IllegalArgumentException if documentIds is null or empty
     */
    Flux<ValidationResult> validateMultipleDocuments(List<UUID> documentIds, ValidationLevel validationLevel);
    
    /**
     * Validate document compliance with regulatory standards.
     * 
     * <p>This method performs specialized validation to ensure documents comply
     * with specific regulatory requirements, industry standards, or legal
     * frameworks. It's essential for organizations in regulated industries.</p>
     * 
     * <p>Common compliance standards include:</p>
     * <ul>
     *   <li>Financial regulations (SOX, PCI-DSS, Basel III)</li>
     *   <li>Healthcare standards (HIPAA, HL7, DICOM)</li>
     *   <li>Legal requirements (eIDAS, GDPR, CCPA)</li>
     *   <li>Industry standards (ISO, ANSI, IEEE)</li>
     * </ul>
     * 
     * @param documentId the UUID of the document to validate
     * @param complianceStandard the regulatory or compliance standard to validate against
     * @param complianceConfig configuration parameters for compliance validation
     * @return a Mono containing the compliance validation result
     * @throws IllegalArgumentException if documentId is null or complianceStandard is not supported
     */
    Mono<ValidationResult> validateCompliance(UUID documentId, String complianceStandard, Map<String, Object> complianceConfig);
    
    /**
     * Validate data integrity and authenticity of a document.
     * 
     * <p>This method performs integrity checks to ensure the document has not
     * been tampered with and that the extracted data accurately represents
     * the original document content. It includes digital signature verification
     * and checksum validation where applicable.</p>
     * 
     * <p>The integrity validation includes:</p>
     * <ul>
     *   <li>Digital signature verification</li>
     *   <li>Document checksum validation</li>
     *   <li>Metadata consistency checks</li>
     *   <li>Timestamp verification</li>
     *   <li>Chain of custody validation</li>
     * </ul>
     * 
     * @param documentId the UUID of the document to validate
     * @return a Mono containing the integrity validation result
     * @throws IllegalArgumentException if documentId is null
     */
    Mono<ValidationResult> validateIntegrity(UUID documentId);
    
    /**
     * Validate data quality and completeness metrics.
     * 
     * <p>This method assesses the quality of extracted data by evaluating
     * completeness, accuracy, consistency, and other quality dimensions.
     * It provides metrics that help determine the reliability of the data
     * for downstream processing.</p>
     * 
     * <p>Quality metrics include:</p>
     * <ul>
     *   <li>Completeness - percentage of required fields populated</li>
     *   <li>Accuracy - confidence scores and validation pass rates</li>
     *   <li>Consistency - internal data consistency checks</li>
     *   <li>Timeliness - data freshness and currency validation</li>
     *   <li>Validity - format and constraint compliance</li>
     * </ul>
     * 
     * @param extractedData the list of extracted data to assess
     * @return a Mono containing quality metrics and assessment results
     * @throws IllegalArgumentException if extractedData is null or empty
     */
    Mono<Map<String, Double>> validateDataQuality(List<ExtractedData> extractedData);
    
    /**
     * Create and register custom validation rules.
     * 
     * <p>This method allows organizations to define and register custom
     * validation rules that can be reused across multiple validation
     * operations. Rules can be defined using various formats including
     * JSON schemas, regular expressions, or custom validation logic.</p>
     * 
     * @param ruleName the unique name for the validation rule
     * @param ruleDefinition the rule definition (schema, logic, or configuration)
     * @param ruleMetadata additional metadata about the rule (description, version, etc.)
     * @return a Mono containing the registration result and rule ID
     * @throws IllegalArgumentException if ruleName is null or ruleDefinition is invalid
     */
    Mono<String> createValidationRule(String ruleName, Map<String, Object> ruleDefinition, Map<String, Object> ruleMetadata);
    
    /**
     * Get available validation rules and their definitions.
     * 
     * <p>This method returns information about all available validation rules,
     * including both built-in rules and custom rules defined by the organization.
     * This helps in understanding available validation capabilities.</p>
     * 
     * @return a Flux of validation rule information including names, definitions, and metadata
     */
    Flux<Map<String, Object>> getAvailableValidationRules();
    
    /**
     * Get supported compliance standards for validation.
     * 
     * <p>This method returns the list of regulatory and compliance standards
     * that are supported for validation by the current adapter implementation.</p>
     * 
     * @return a Flux of supported compliance standard names and descriptions
     */
    Flux<Map<String, String>> getSupportedComplianceStandards();
    
    /**
     * Get validation performance metrics and statistics.
     * 
     * <p>This method provides performance metrics for validation operations,
     * including processing times, success rates, and resource utilization.
     * This information is useful for monitoring and optimization.</p>
     * 
     * @param timeRangeStart the start time for metrics collection
     * @param timeRangeEnd the end time for metrics collection
     * @return a Mono containing validation performance metrics
     */
    Mono<Map<String, Object>> getValidationMetrics(java.time.Instant timeRangeStart, java.time.Instant timeRangeEnd);
    
    /**
     * Returns the name of the adapter implementation for identification and logging.
     * 
     * <p>This method provides a way to identify which specific adapter implementation
     * is being used. The name should be descriptive and unique among all available
     * adapters (e.g., "Enterprise Validation Engine", "Cloud Validation Service").</p>
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
