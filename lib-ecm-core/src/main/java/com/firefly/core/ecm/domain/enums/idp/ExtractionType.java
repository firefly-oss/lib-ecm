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
 * Extraction type enumeration for Intelligent Document Processing (IDP) operations.
 * 
 * <p>This enumeration defines the various types of data extraction that can be
 * performed on documents during IDP processing. Each extraction type represents
 * a different approach to analyzing and extracting information from documents.</p>
 * 
 * <p>The extraction types are organized into categories:</p>
 * <ul>
 *   <li><strong>Text Extraction:</strong> OCR, handwriting recognition, text analysis</li>
 *   <li><strong>Structured Data:</strong> Forms, tables, key-value pairs</li>
 *   <li><strong>Visual Elements:</strong> Images, signatures, logos, barcodes</li>
 *   <li><strong>Semantic Analysis:</strong> Entities, relationships, sentiment</li>
 *   <li><strong>Layout Analysis:</strong> Document structure, regions, formatting</li>
 * </ul>
 * 
 * @author Firefly Software Solutions Inc.
 * @since 1.0.0
 */
public enum ExtractionType {
    
    // Text Extraction
    /**
     * Optical Character Recognition (OCR) for printed text extraction
     */
    OCR_TEXT,
    
    /**
     * Handwriting recognition for handwritten text extraction
     */
    HANDWRITING_RECOGNITION,
    
    /**
     * Full text extraction including all readable content
     */
    FULL_TEXT,
    
    /**
     * Raw text extraction without formatting or structure
     */
    RAW_TEXT,
    
    /**
     * Formatted text extraction preserving layout and styling
     */
    FORMATTED_TEXT,
    
    // Structured Data Extraction
    /**
     * Key-value pair extraction from forms and documents
     */
    KEY_VALUE_PAIRS,
    
    /**
     * Table data extraction with rows and columns
     */
    TABLE_DATA,
    
    /**
     * Form field extraction with field names and values
     */
    FORM_FIELDS,
    
    /**
     * Line item extraction from invoices and receipts
     */
    LINE_ITEMS,
    
    /**
     * Header and footer information extraction
     */
    HEADER_FOOTER,
    
    /**
     * Metadata extraction from document properties
     */
    METADATA,
    
    // Visual Elements
    /**
     * Image extraction from documents
     */
    IMAGES,
    
    /**
     * Signature detection and extraction
     */
    SIGNATURES,
    
    /**
     * Logo and brand mark detection
     */
    LOGOS,
    
    /**
     * Barcode and QR code extraction
     */
    BARCODES,
    
    /**
     * Stamp and seal detection
     */
    STAMPS,
    
    /**
     * Chart and graph data extraction
     */
    CHARTS,
    
    // Semantic Analysis
    /**
     * Named entity recognition (persons, organizations, locations)
     */
    NAMED_ENTITIES,
    
    /**
     * Date and time extraction
     */
    DATES_TIMES,
    
    /**
     * Currency and monetary amount extraction
     */
    MONETARY_AMOUNTS,
    
    /**
     * Phone number extraction
     */
    PHONE_NUMBERS,
    
    /**
     * Email address extraction
     */
    EMAIL_ADDRESSES,
    
    /**
     * Address and location extraction
     */
    ADDRESSES,
    
    /**
     * Identification number extraction (SSN, tax ID, etc.)
     */
    IDENTIFICATION_NUMBERS,
    
    /**
     * Relationship extraction between entities
     */
    RELATIONSHIPS,
    
    /**
     * Sentiment analysis of text content
     */
    SENTIMENT,
    
    /**
     * Language detection and analysis
     */
    LANGUAGE_DETECTION,
    
    // Layout Analysis
    /**
     * Document layout and structure analysis
     */
    LAYOUT_ANALYSIS,
    
    /**
     * Page region detection and classification
     */
    PAGE_REGIONS,
    
    /**
     * Reading order determination
     */
    READING_ORDER,
    
    /**
     * Font and formatting analysis
     */
    FORMATTING_ANALYSIS,
    
    /**
     * Document orientation detection
     */
    ORIENTATION,
    
    /**
     * Page boundary detection
     */
    PAGE_BOUNDARIES,
    
    // Specialized Extraction
    /**
     * Medical information extraction (diagnoses, medications, etc.)
     */
    MEDICAL_INFORMATION,
    
    /**
     * Legal clause and term extraction
     */
    LEGAL_TERMS,
    
    /**
     * Financial data extraction (amounts, accounts, etc.)
     */
    FINANCIAL_DATA,
    
    /**
     * Personal identifiable information (PII) extraction
     */
    PII_DATA,
    
    /**
     * Classification codes and categories
     */
    CLASSIFICATION_CODES,
    
    /**
     * Quality metrics and confidence scores
     */
    QUALITY_METRICS,
    
    /**
     * Custom extraction type defined by user or adapter
     */
    CUSTOM,
    
    /**
     * All available extraction types for comprehensive processing
     */
    ALL
}
