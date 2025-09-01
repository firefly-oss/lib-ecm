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
package com.firefly.core.ecm.adapter;

/**
 * Enumeration of ECM adapter features.
 * Used to declare what capabilities an adapter supports.
 */
public enum AdapterFeature {
    
    /**
     * Basic document CRUD operations
     */
    DOCUMENT_CRUD,
    
    /**
     * Document content storage and retrieval
     */
    CONTENT_STORAGE,
    
    /**
     * Document versioning support
     */
    VERSIONING,
    
    /**
     * Folder management
     */
    FOLDER_MANAGEMENT,
    
    /**
     * Hierarchical folder structure
     */
    FOLDER_HIERARCHY,
    
    /**
     * Permission management
     */
    PERMISSIONS,
    
    /**
     * Document security features
     */
    SECURITY,
    
    /**
     * Full-text search capabilities
     */
    SEARCH,
    
    /**
     * Metadata search
     */
    METADATA_SEARCH,
    
    /**
     * Audit trail logging
     */
    AUDIT_TRAIL,
    
    /**
     * eSignature envelope management
     */
    ESIGNATURE_ENVELOPES,
    
    /**
     * eSignature requests
     */
    ESIGNATURE_REQUESTS,
    
    /**
     * Signature validation
     */
    SIGNATURE_VALIDATION,
    
    /**
     * Signature proof generation
     */
    SIGNATURE_PROOF,
    
    /**
     * Document encryption
     */
    ENCRYPTION,
    
    /**
     * Content streaming
     */
    STREAMING,
    
    /**
     * Batch operations
     */
    BATCH_OPERATIONS,
    
    /**
     * Transaction support
     */
    TRANSACTIONS,
    
    /**
     * Backup and restore
     */
    BACKUP_RESTORE,
    
    /**
     * Multi-tenancy support
     */
    MULTI_TENANCY,
    
    /**
     * Cloud storage integration
     */
    CLOUD_STORAGE,
    
    /**
     * Enterprise ECM integration
     */
    ENTERPRISE_ECM,
    
    /**
     * Compliance features
     */
    COMPLIANCE,
    
    /**
     * Legal hold management
     */
    LEGAL_HOLD,
    
    /**
     * Retention policies
     */
    RETENTION_POLICIES,

    /**
     * Text extraction and OCR capabilities
     */
    TEXT_EXTRACTION,

    /**
     * Optical Character Recognition (OCR)
     */
    OCR,

    /**
     * Document classification and categorization
     */
    DOCUMENT_CLASSIFICATION,

    /**
     * Document validation and verification
     */
    DOCUMENT_VALIDATION,

    /**
     * Structured data extraction (forms, tables, key-value pairs)
     */
    STRUCTURED_DATA_EXTRACTION,

    /**
     * Form processing and field extraction
     */
    FORM_PROCESSING,

    /**
     * Table data extraction
     */
    TABLE_EXTRACTION,

    /**
     * Handwriting recognition
     */
    HANDWRITING_RECOGNITION,

    /**
     * Layout analysis and document structure detection
     */
    LAYOUT_ANALYSIS,

    /**
     * Named entity recognition and extraction
     */
    ENTITY_EXTRACTION,

    /**
     * Document quality assessment and confidence scoring
     */
    QUALITY_ASSESSMENT,

    /**
     * Custom template-based extraction
     */
    TEMPLATE_EXTRACTION,

    /**
     * Intelligent Document Processing (IDP) capabilities
     */
    INTELLIGENT_DOCUMENT_PROCESSING
}
