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
 * Validation level enumeration for Intelligent Document Processing (IDP) operations.
 * 
 * <p>This enumeration defines the different levels of validation that can be
 * applied to documents and extracted data during IDP processing. Each level
 * represents a different depth and rigor of validation checks.</p>
 * 
 * <p>Validation levels are organized by increasing thoroughness:</p>
 * <ul>
 *   <li><strong>BASIC:</strong> Simple format and structure checks</li>
 *   <li><strong>STANDARD:</strong> Business rule validation and cross-field checks</li>
 *   <li><strong>COMPREHENSIVE:</strong> Advanced validation with external data sources</li>
 *   <li><strong>STRICT:</strong> Maximum validation with regulatory compliance</li>
 * </ul>
 * 
 * <p>Higher validation levels provide greater accuracy but may require more
 * processing time and resources. The appropriate level depends on the use case,
 * compliance requirements, and acceptable risk tolerance.</p>
 * 
 * @author Firefly Software Solutions Inc.
 * @since 1.0.0
 */
public enum ValidationLevel {
    
    /**
     * No validation performed.
     * 
     * <p>Data is extracted and returned without any validation checks.
     * This is the fastest option but provides no quality assurance.</p>
     */
    NONE,
    
    /**
     * Basic validation with simple format and structure checks.
     * 
     * <p>Performs fundamental validation such as:</p>
     * <ul>
     *   <li>Data type validation (numbers, dates, text)</li>
     *   <li>Required field presence checks</li>
     *   <li>Basic format validation (email, phone, postal codes)</li>
     *   <li>Length and range constraints</li>
     * </ul>
     */
    BASIC,
    
    /**
     * Standard validation with business rule checks and cross-field validation.
     * 
     * <p>Includes all basic validation plus:</p>
     * <ul>
     *   <li>Business rule validation</li>
     *   <li>Cross-field consistency checks</li>
     *   <li>Calculated field verification</li>
     *   <li>Standard industry format validation</li>
     *   <li>Duplicate detection within document</li>
     * </ul>
     */
    STANDARD,
    
    /**
     * Comprehensive validation with advanced checks and external data verification.
     * 
     * <p>Includes all standard validation plus:</p>
     * <ul>
     *   <li>External database lookups</li>
     *   <li>Third-party service validation</li>
     *   <li>Historical data comparison</li>
     *   <li>Advanced pattern recognition</li>
     *   <li>Machine learning-based anomaly detection</li>
     * </ul>
     */
    COMPREHENSIVE,
    
    /**
     * Strict validation with maximum rigor and regulatory compliance.
     * 
     * <p>Includes all comprehensive validation plus:</p>
     * <ul>
     *   <li>Regulatory compliance checks</li>
     *   <li>Legal requirement validation</li>
     *   <li>Industry-specific standards verification</li>
     *   <li>Multi-source data reconciliation</li>
     *   <li>Audit trail generation</li>
     *   <li>Digital signature verification</li>
     * </ul>
     */
    STRICT,
    
    /**
     * Custom validation level defined by user requirements.
     * 
     * <p>Allows for application-specific validation rules and checks
     * that may not fit into the standard validation levels.</p>
     */
    CUSTOM,
    
    /**
     * Validation level determined automatically based on document type and content.
     * 
     * <p>The system automatically selects the most appropriate validation
     * level based on document characteristics, confidence levels, and
     * configured policies.</p>
     */
    AUTOMATIC
}
