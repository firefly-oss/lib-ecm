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

import com.firefly.core.ecm.domain.enums.idp.DocumentType;
import com.firefly.core.ecm.domain.model.idp.ExtractedData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Port interface for structured and semi-structured data extraction operations in Intelligent Document Processing (IDP).
 * 
 * <p>This interface defines the contract for extracting structured data from documents
 * including forms, tables, key-value pairs, and other organized data elements. It focuses
 * on understanding document structure and extracting meaningful data relationships rather
 * than just raw text. It follows the hexagonal architecture pattern where this port
 * defines the business requirements, and adapters provide concrete implementations for
 * different IDP providers.</p>
 * 
 * <p>Key capabilities provided by this port:</p>
 * <ul>
 *   <li><strong>Form Data Extraction:</strong> Extract field names and values from forms</li>
 *   <li><strong>Table Processing:</strong> Extract tabular data with rows, columns, and headers</li>
 *   <li><strong>Key-Value Pairs:</strong> Identify and extract label-value relationships</li>
 *   <li><strong>Line Items:</strong> Extract itemized data from invoices and receipts</li>
 *   <li><strong>Structured Templates:</strong> Process documents with known layouts</li>
 *   <li><strong>Entity Relationships:</strong> Understand relationships between data elements</li>
 * </ul>
 * 
 * <p>The interface supports both template-based extraction for known document formats
 * and intelligent extraction for unknown or varying document structures. All operations
 * return reactive types (Mono/Flux) for non-blocking processing.</p>
 * 
 * <p>Typical usage patterns:</p>
 * <pre>
 * {@code
 * // Extract form fields from a document
 * Flux<ExtractedData> formData = dataExtractionPort.extractFormFields(documentId);
 * 
 * // Extract table data with specific configuration
 * Map<String, Object> config = Map.of("includeHeaders", true, "detectMergedCells", true);
 * Flux<ExtractedData> tableData = dataExtractionPort.extractTableDataWithConfig(documentId, config);
 * 
 * // Extract using document-specific template
 * Mono<List<ExtractedData>> data = dataExtractionPort.extractWithTemplate(documentId, "invoice_template_v2");
 * }
 * </pre>
 * 
 * @author Firefly Software Solutions Inc.
 * @since 1.0.0
 * @see ExtractedData
 * @see DocumentType
 */
public interface DataExtractionPort {
    
    /**
     * Extract form fields and their values from a document.
     * 
     * <p>This method identifies and extracts form fields including text fields,
     * checkboxes, radio buttons, and other form elements. It attempts to
     * associate field labels with their corresponding values.</p>
     * 
     * <p>The form extraction process includes:</p>
     * <ul>
     *   <li>Form field detection and classification</li>
     *   <li>Label-value association and mapping</li>
     *   <li>Field type identification (text, checkbox, etc.)</li>
     *   <li>Value extraction and normalization</li>
     * </ul>
     * 
     * @param documentId the UUID of the document to extract form data from
     * @return a Flux of extracted form field data
     * @throws IllegalArgumentException if documentId is null
     * @see ExtractedData
     */
    Flux<ExtractedData> extractFormFields(UUID documentId);
    
    /**
     * Extract tabular data including rows, columns, and headers.
     * 
     * <p>This method identifies and extracts table structures from documents,
     * preserving the relationships between rows and columns. It handles various
     * table formats including simple grids, complex layouts, and merged cells.</p>
     * 
     * <p>The table extraction includes:</p>
     * <ul>
     *   <li>Table boundary detection</li>
     *   <li>Row and column structure identification</li>
     *   <li>Header row detection and labeling</li>
     *   <li>Cell content extraction and formatting</li>
     *   <li>Merged cell handling</li>
     * </ul>
     * 
     * @param documentId the UUID of the document to extract table data from
     * @return a Flux of extracted table data organized by rows and columns
     * @throws IllegalArgumentException if documentId is null
     */
    Flux<ExtractedData> extractTableData(UUID documentId);
    
    /**
     * Extract tabular data with custom configuration parameters.
     * 
     * <p>This method allows for fine-tuned table extraction with custom
     * configuration options such as header detection settings, cell merging
     * handling, and output format preferences.</p>
     * 
     * <p>Common configuration parameters include:</p>
     * <ul>
     *   <li><code>includeHeaders</code> - Whether to include header row information</li>
     *   <li><code>detectMergedCells</code> - Whether to detect and handle merged cells</li>
     *   <li><code>preserveFormatting</code> - Whether to maintain cell formatting</li>
     *   <li><code>minColumns</code> - Minimum number of columns to consider as table</li>
     *   <li><code>outputFormat</code> - Desired output format (structured, flat, etc.)</li>
     * </ul>
     * 
     * @param documentId the UUID of the document to extract table data from
     * @param config configuration parameters for table extraction
     * @return a Flux of extracted table data with applied configuration
     * @throws IllegalArgumentException if documentId is null or config contains invalid parameters
     */
    Flux<ExtractedData> extractTableDataWithConfig(UUID documentId, Map<String, Object> config);
    
    /**
     * Extract key-value pairs from documents.
     * 
     * <p>This method identifies and extracts label-value relationships throughout
     * the document, such as "Invoice Number: 12345" or "Date: 2024-01-15".
     * It's particularly useful for semi-structured documents with scattered
     * information.</p>
     * 
     * <p>The key-value extraction includes:</p>
     * <ul>
     *   <li>Label pattern recognition</li>
     *   <li>Value association and proximity analysis</li>
     *   <li>Data type inference for values</li>
     *   <li>Confidence scoring for associations</li>
     * </ul>
     * 
     * @param documentId the UUID of the document to extract key-value pairs from
     * @return a Flux of extracted key-value pair data
     * @throws IllegalArgumentException if documentId is null
     */
    Flux<ExtractedData> extractKeyValuePairs(UUID documentId);
    
    /**
     * Extract line items from invoices, receipts, and similar documents.
     * 
     * <p>This method specializes in extracting itemized data typically found
     * in financial documents, including product descriptions, quantities,
     * prices, and totals. It understands the structure of line item tables.</p>
     * 
     * <p>The line item extraction includes:</p>
     * <ul>
     *   <li>Line item table detection</li>
     *   <li>Column header identification (description, quantity, price, etc.)</li>
     *   <li>Row-by-row data extraction</li>
     *   <li>Calculation verification and validation</li>
     *   <li>Total and subtotal identification</li>
     * </ul>
     * 
     * @param documentId the UUID of the document to extract line items from
     * @return a Flux of extracted line item data
     * @throws IllegalArgumentException if documentId is null
     */
    Flux<ExtractedData> extractLineItems(UUID documentId);
    
    /**
     * Extract data using a predefined template for known document types.
     * 
     * <p>This method uses predefined templates that specify exactly where
     * to find specific data elements in documents with known layouts.
     * Templates can be created for common document types like invoices,
     * contracts, or forms with consistent structures.</p>
     * 
     * <p>Template-based extraction includes:</p>
     * <ul>
     *   <li>Template matching and alignment</li>
     *   <li>Precise field location extraction</li>
     *   <li>Template-specific validation rules</li>
     *   <li>High-accuracy extraction for known formats</li>
     * </ul>
     * 
     * @param documentId the UUID of the document to extract data from
     * @param templateId the identifier of the template to use
     * @return a Mono containing all extracted data according to the template
     * @throws IllegalArgumentException if documentId or templateId is null
     */
    Mono<List<ExtractedData>> extractWithTemplate(UUID documentId, String templateId);
    
    /**
     * Extract data from a document provided as an input stream.
     * 
     * <p>This method allows for data extraction from documents that are not yet
     * stored in the ECM system. The document content is provided directly as
     * an input stream, making it suitable for real-time processing scenarios.</p>
     * 
     * @param documentStream the input stream containing the document content
     * @param mimeType the MIME type of the document
     * @param documentType the expected document type for optimized extraction
     * @return a Flux of extracted structured data
     * @throws IllegalArgumentException if documentStream is null or mimeType is unsupported
     */
    Flux<ExtractedData> extractDataFromStream(InputStream documentStream, String mimeType, DocumentType documentType);
    
    /**
     * Extract specific named entities and their relationships.
     * 
     * <p>This method focuses on extracting specific types of entities such as
     * dates, monetary amounts, addresses, phone numbers, and other structured
     * data types. It also identifies relationships between these entities.</p>
     * 
     * <p>Entity extraction includes:</p>
     * <ul>
     *   <li>Named entity recognition (NER)</li>
     *   <li>Entity type classification</li>
     *   <li>Relationship identification</li>
     *   <li>Context-aware extraction</li>
     * </ul>
     * 
     * @param documentId the UUID of the document to extract entities from
     * @param entityTypes list of entity types to extract (e.g., "DATE", "MONEY", "PERSON")
     * @return a Flux of extracted entity data with relationships
     * @throws IllegalArgumentException if documentId is null or entityTypes is empty
     */
    Flux<ExtractedData> extractNamedEntities(UUID documentId, List<String> entityTypes);
    
    /**
     * Create or update a data extraction template.
     * 
     * <p>This method allows organizations to define custom extraction templates
     * for their specific document types and layouts. Templates can specify
     * field locations, extraction rules, and validation criteria.</p>
     * 
     * @param templateId the unique identifier for the template
     * @param templateDefinition the template definition including field mappings and rules
     * @param templateMetadata additional metadata about the template
     * @return a Mono containing the template creation result
     * @throws IllegalArgumentException if templateId is null or templateDefinition is invalid
     */
    Mono<String> createExtractionTemplate(String templateId, Map<String, Object> templateDefinition, Map<String, Object> templateMetadata);
    
    /**
     * Get available extraction templates and their definitions.
     * 
     * <p>This method returns information about all available extraction templates,
     * including both built-in templates and custom templates defined by the
     * organization.</p>
     * 
     * @return a Flux of template information including IDs, definitions, and metadata
     */
    Flux<Map<String, Object>> getAvailableTemplates();
    
    /**
     * Get supported document types for structured data extraction.
     * 
     * <p>This method returns the list of document types that are supported
     * for structured data extraction by the current adapter implementation.</p>
     * 
     * @return a Flux of supported document types
     * @see DocumentType
     */
    Flux<DocumentType> getSupportedDocumentTypes();
    
    /**
     * Get extraction capabilities and features supported by the adapter.
     * 
     * <p>This method provides information about the specific extraction
     * capabilities supported by the current adapter implementation,
     * helping users understand what types of data can be extracted.</p>
     * 
     * @return a Mono containing capability information and feature descriptions
     */
    Mono<Map<String, Object>> getExtractionCapabilities();
    
    /**
     * Returns the name of the adapter implementation for identification and logging.
     * 
     * <p>This method provides a way to identify which specific adapter implementation
     * is being used. The name should be descriptive and unique among all available
     * adapters (e.g., "AWS Textract Data Extractor", "Azure Form Recognizer Extractor").</p>
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
