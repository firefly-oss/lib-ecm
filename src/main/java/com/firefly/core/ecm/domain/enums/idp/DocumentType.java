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
 * Document type enumeration for Intelligent Document Processing (IDP) operations.
 * 
 * <p>This enumeration defines the various types of documents that can be processed
 * by IDP systems for classification, data extraction, and validation purposes.
 * Each document type may have specific processing rules and extraction patterns.</p>
 * 
 * <p>The document types are organized into categories:</p>
 * <ul>
 *   <li><strong>Financial Documents:</strong> Invoices, receipts, bank statements, etc.</li>
 *   <li><strong>Identity Documents:</strong> Passports, driver's licenses, ID cards, etc.</li>
 *   <li><strong>Legal Documents:</strong> Contracts, agreements, court documents, etc.</li>
 *   <li><strong>Medical Documents:</strong> Medical records, prescriptions, lab results, etc.</li>
 *   <li><strong>Business Documents:</strong> Purchase orders, delivery notes, forms, etc.</li>
 *   <li><strong>Generic Documents:</strong> Unstructured text, images, mixed content, etc.</li>
 * </ul>
 * 
 * @author Firefly Software Solutions Inc.
 * @since 1.0.0
 */
public enum DocumentType {
    
    // Financial Documents
    /**
     * Invoice document containing billing information and line items
     */
    INVOICE,
    
    /**
     * Receipt document showing proof of payment
     */
    RECEIPT,
    
    /**
     * Bank statement showing account transactions
     */
    BANK_STATEMENT,
    
    /**
     * Purchase order document for procurement
     */
    PURCHASE_ORDER,
    
    /**
     * Credit note or refund document
     */
    CREDIT_NOTE,
    
    /**
     * Tax document or tax return
     */
    TAX_DOCUMENT,
    
    /**
     * Financial report or statement
     */
    FINANCIAL_REPORT,
    
    // Identity Documents
    /**
     * Passport document for international travel
     */
    PASSPORT,
    
    /**
     * Driver's license for vehicle operation
     */
    DRIVERS_LICENSE,
    
    /**
     * National identity card
     */
    NATIONAL_ID,
    
    /**
     * Social security card
     */
    SOCIAL_SECURITY_CARD,
    
    /**
     * Birth certificate document
     */
    BIRTH_CERTIFICATE,
    
    /**
     * Visa or immigration document
     */
    VISA,
    
    // Legal Documents
    /**
     * Legal contract or agreement
     */
    CONTRACT,
    
    /**
     * Court document or legal filing
     */
    COURT_DOCUMENT,
    
    /**
     * Legal certificate or license
     */
    LEGAL_CERTIFICATE,
    
    /**
     * Power of attorney document
     */
    POWER_OF_ATTORNEY,
    
    /**
     * Will or testament document
     */
    WILL,
    
    /**
     * Deed or property document
     */
    DEED,
    
    // Medical Documents
    /**
     * Medical record or patient file
     */
    MEDICAL_RECORD,
    
    /**
     * Medical prescription
     */
    PRESCRIPTION,
    
    /**
     * Laboratory test results
     */
    LAB_RESULTS,
    
    /**
     * Medical insurance document
     */
    INSURANCE_DOCUMENT,
    
    /**
     * Medical certificate or clearance
     */
    MEDICAL_CERTIFICATE,
    
    /**
     * Vaccination record or certificate
     */
    VACCINATION_RECORD,
    
    // Business Documents
    /**
     * Delivery note or shipping document
     */
    DELIVERY_NOTE,
    
    /**
     * Business form or application
     */
    BUSINESS_FORM,
    
    /**
     * Employee document or HR record
     */
    EMPLOYEE_DOCUMENT,
    
    /**
     * Business license or permit
     */
    BUSINESS_LICENSE,
    
    /**
     * Insurance policy document
     */
    INSURANCE_POLICY,
    
    /**
     * Warranty or guarantee document
     */
    WARRANTY,
    
    // Generic Documents
    /**
     * Unstructured text document
     */
    UNSTRUCTURED_TEXT,
    
    /**
     * Image document requiring OCR
     */
    IMAGE_DOCUMENT,
    
    /**
     * Mixed content document with various elements
     */
    MIXED_CONTENT,
    
    /**
     * Table or spreadsheet document
     */
    TABLE_DOCUMENT,
    
    /**
     * Form document with fields
     */
    FORM_DOCUMENT,
    
    /**
     * Email or message document
     */
    EMAIL,
    
    /**
     * Unknown or unclassified document type
     */
    UNKNOWN,
    
    /**
     * Custom document type defined by user
     */
    CUSTOM
}
