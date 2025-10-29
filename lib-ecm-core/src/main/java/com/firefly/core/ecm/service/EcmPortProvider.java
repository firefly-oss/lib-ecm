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
package com.firefly.core.ecm.service;

import com.firefly.core.ecm.adapter.AdapterSelector;
import com.firefly.core.ecm.config.EcmAutoConfiguration;
import com.firefly.core.ecm.config.EcmProperties;
import com.firefly.core.ecm.port.document.*;
import com.firefly.core.ecm.port.folder.*;
import com.firefly.core.ecm.port.security.*;
import com.firefly.core.ecm.port.audit.*;
import com.firefly.core.ecm.port.esignature.*;
import com.firefly.core.ecm.port.idp.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Central service that provides access to ECM ports with proper adapter selection and logging.
 *
 * <p>This service acts as a factory for ECM ports, handling the complex logic of selecting
 * the appropriate adapter implementation based on the configured adapter type. It provides
 * a unified interface for accessing all ECM functionality while abstracting away the
 * underlying adapter selection mechanism.</p>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Adapter selection based on configuration</li>
 *   <li>Port provisioning with proper error handling</li>
 *   <li>Logging when adapters are not available</li>
 *   <li>Graceful degradation when features are unavailable</li>
 * </ul>
 *
 * <p>The service uses the hexagonal architecture pattern, where ports define the business
 * interfaces and adapters provide the concrete implementations. This allows for easy
 * swapping of underlying storage systems without changing business logic.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @Autowired
 * private EcmPortProvider portProvider;
 *
 * public void uploadDocument(Document document, InputStream content) {
 *     DocumentPort documentPort = portProvider.getDocumentPort()
 *         .orElseThrow(() -> new ServiceUnavailableException("Document management not available"));
 *
 *     DocumentContentPort contentPort = portProvider.getDocumentContentPort()
 *         .orElseThrow(() -> new ServiceUnavailableException("Content storage not available"));
 *
 *     Document savedDoc = documentPort.create(document);
 *     contentPort.store(savedDoc.getId(), content);
 * }
 * }
 * </pre>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 * @see AdapterSelector
 * @see EcmProperties
 * @see EcmAutoConfiguration
 */
@Slf4j
@Service
public class EcmPortProvider {

    /** The adapter selector responsible for choosing appropriate adapters. */
    private final AdapterSelector adapterSelector;

    /** The ECM configuration properties. */
    private final EcmProperties ecmProperties;

    /**
     * Constructs a new EcmPortProvider with the specified dependencies.
     *
     * @param adapterSelector the adapter selector for choosing implementations
     * @param ecmProperties the ECM configuration properties
     */
    public EcmPortProvider(AdapterSelector adapterSelector, EcmProperties ecmProperties) {
        this.adapterSelector = adapterSelector;
        this.ecmProperties = ecmProperties;
    }
    
    /**
     * Retrieves a DocumentPort implementation for basic document CRUD operations.
     *
     * <p>The DocumentPort provides core functionality for creating, reading, updating,
     * and deleting documents in the ECM system. This includes metadata management,
     * document lifecycle operations, and basic document properties.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of document management capabilities.</p>
     *
     * @return an Optional containing the DocumentPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see DocumentPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<DocumentPort> getDocumentPort() {
        Optional<DocumentPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), DocumentPort.class);
        if (port.isEmpty()) {
            log.warn("No DocumentPort adapter found for type: '{}'. " +
                    "A no-op adapter will be used as fallback. " +
                    "To enable document management features, configure a suitable adapter (e.g., s3, azure-blob) " +
                    "in your application properties under 'firefly.ecm.adapter-type'.",
                    ecmProperties.getAdapterType());
        } else {
            log.debug("DocumentPort adapter found: {}", port.get().getAdapterName());
        }
        return port;
    }

    /**
     * Retrieves a DocumentContentPort implementation for binary content operations.
     *
     * <p>The DocumentContentPort handles the storage and retrieval of document binary
     * content, including upload, download, streaming, and content manipulation operations.
     * This port is essential for managing the actual file data associated with documents.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of content storage capabilities.</p>
     *
     * @return an Optional containing the DocumentContentPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see DocumentContentPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<DocumentContentPort> getDocumentContentPort() {
        Optional<DocumentContentPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), DocumentContentPort.class);
        if (port.isEmpty()) {
            log.warn("No DocumentContentPort adapter found for type: '{}'. " +
                    "A no-op adapter will be used as fallback. " +
                    "To enable document content storage features, configure a suitable adapter (e.g., s3, azure-blob) " +
                    "in your application properties under 'firefly.ecm.adapter-type'.",
                    ecmProperties.getAdapterType());
        } else {
            log.debug("DocumentContentPort adapter found: {}", port.get().getClass().getSimpleName());
        }
        return port;
    }

    /**
     * Retrieves a DocumentVersionPort implementation for document versioning capabilities.
     *
     * <p>The DocumentVersionPort provides functionality for managing document versions,
     * including creating new versions, retrieving version history, comparing versions,
     * and managing version-specific metadata and content.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of versioning capabilities.</p>
     *
     * @return an Optional containing the DocumentVersionPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see DocumentVersionPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<DocumentVersionPort> getDocumentVersionPort() {
        Optional<DocumentVersionPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), DocumentVersionPort.class);
        if (port.isEmpty()) {
            log.warn("No DocumentVersionPort adapter found for type: {}. Document versioning features will not be available.",
                    ecmProperties.getAdapterType());
        }
        return port;
    }

    /**
     * Retrieves a DocumentSearchPort implementation for search and query capabilities.
     *
     * <p>The DocumentSearchPort provides functionality for searching documents by
     * metadata, content, and other criteria using various search strategies such as
     * full-text search, metadata queries, and advanced filtering.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of search capabilities.</p>
     *
     * @return an Optional containing the DocumentSearchPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see DocumentSearchPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<DocumentSearchPort> getDocumentSearchPort() {
        Optional<DocumentSearchPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), DocumentSearchPort.class);
        if (port.isEmpty()) {
            log.warn("No DocumentSearchPort adapter found for type: {}. Document search features will not be available.",
                    ecmProperties.getAdapterType());
        }
        return port;
    }
    
    /**
     * Retrieves a FolderPort implementation for basic folder management operations.
     *
     * <p>The FolderPort provides core functionality for creating, reading, updating,
     * and deleting folders in the ECM system. This includes folder metadata management,
     * folder organization, and basic folder properties.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of folder management capabilities.</p>
     *
     * @return an Optional containing the FolderPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see FolderPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<FolderPort> getFolderPort() {
        Optional<FolderPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), FolderPort.class);
        if (port.isEmpty()) {
            log.warn("No FolderPort adapter found for type: {}. Folder management features will not be available.",
                    ecmProperties.getAdapterType());
        }
        return port;
    }

    /**
     * Retrieves a FolderHierarchyPort implementation for hierarchical folder operations.
     *
     * <p>The FolderHierarchyPort provides functionality for managing hierarchical
     * folder structures, including tree navigation, path resolution, parent-child
     * relationships, and recursive operations on folder trees.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of folder hierarchy capabilities.</p>
     *
     * @return an Optional containing the FolderHierarchyPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see FolderHierarchyPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<FolderHierarchyPort> getFolderHierarchyPort() {
        Optional<FolderHierarchyPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), FolderHierarchyPort.class);
        if (port.isEmpty()) {
            log.warn("No FolderHierarchyPort adapter found for type: {}. Folder hierarchy features will not be available.",
                    ecmProperties.getAdapterType());
        }
        return port;
    }

    /**
     * Retrieves a PermissionPort implementation for access control and permission management.
     *
     * <p>The PermissionPort provides functionality for managing user and group permissions
     * on documents and folders, including granting, revoking, and checking access rights.
     * It supports various permission types and principal types (users and groups).</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of permission management capabilities.</p>
     *
     * @return an Optional containing the PermissionPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see PermissionPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<PermissionPort> getPermissionPort() {
        Optional<PermissionPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), PermissionPort.class);
        if (port.isEmpty()) {
            log.warn("No PermissionPort adapter found for type: '{}'. " +
                    "A no-op adapter will be used as fallback. " +
                    "To enable permission management features, configure a suitable adapter " +
                    "in your application properties under 'firefly.ecm.adapter-type'.",
                    ecmProperties.getAdapterType());
        } else {
            log.debug("PermissionPort adapter found: {}", port.get().getClass().getSimpleName());
        }
        return port;
    }

    /**
     * Retrieves a DocumentSecurityPort implementation for document-level security operations.
     *
     * <p>The DocumentSecurityPort provides functionality for applying security policies,
     * encryption, digital rights management, and other security-related operations on
     * documents. This includes content protection and access control enforcement.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of document security capabilities.</p>
     *
     * @return an Optional containing the DocumentSecurityPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see DocumentSecurityPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<DocumentSecurityPort> getDocumentSecurityPort() {
        Optional<DocumentSecurityPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), DocumentSecurityPort.class);
        if (port.isEmpty()) {
            log.warn("No DocumentSecurityPort adapter found for type: '{}'. " +
                    "A no-op adapter will be used as fallback with permissive defaults. " +
                    "To enable document security features, configure a suitable adapter " +
                    "in your application properties under 'firefly.ecm.adapter-type'.",
                    ecmProperties.getAdapterType());
        } else {
            log.debug("DocumentSecurityPort adapter found: {}", port.get().getClass().getSimpleName());
        }
        return port;
    }

    /**
     * Retrieves an AuditPort implementation for audit trail and compliance logging.
     *
     * <p>The AuditPort provides functionality for logging user actions, system events,
     * and maintaining compliance records for regulatory requirements. It supports
     * various audit event types and provides querying capabilities for audit trails.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of audit trail capabilities.</p>
     *
     * @return an Optional containing the AuditPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see AuditPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<AuditPort> getAuditPort() {
        Optional<AuditPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), AuditPort.class);
        if (port.isEmpty()) {
            log.warn("No AuditPort adapter found for type: {}. Audit trail features will not be available.",
                    ecmProperties.getAdapterType());
        }
        return port;
    }
    
    /**
     * Retrieves a SignatureEnvelopePort implementation for eSignature envelope management.
     *
     * <p>The SignatureEnvelopePort provides functionality for creating, managing, and
     * tracking signature envelopes that contain documents requiring electronic signatures.
     * This includes envelope creation, recipient management, sending, and status tracking.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of eSignature envelope capabilities.</p>
     *
     * @return an Optional containing the SignatureEnvelopePort implementation if available,
     *         or empty if no suitable adapter is found
     * @see SignatureEnvelopePort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<SignatureEnvelopePort> getSignatureEnvelopePort() {
        String esignProvider = ecmProperties.getEsignature() != null ? ecmProperties.getEsignature().getProvider() : null;
        Optional<SignatureEnvelopePort> port = adapterSelector.selectAdapter(esignProvider, SignatureEnvelopePort.class);
        if (port.isEmpty()) {
            // Fallback to core adapter-type, then to any available implementation
            port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), SignatureEnvelopePort.class);
        }
        if (port.isEmpty()) {
            log.warn("No SignatureEnvelopePort adapter found (provider='{}', adapterType='{}').", esignProvider, ecmProperties.getAdapterType());
        } else {
            log.debug("SignatureEnvelopePort adapter found: {}", port.get().getClass().getSimpleName());
        }
        return port;
    }

    /**
     * Retrieves a SignatureRequestPort implementation for managing individual signature requests.
     *
     * <p>The SignatureRequestPort provides functionality for creating, sending, and
     * tracking individual signature requests within signature envelopes. This includes
     * request configuration, recipient notification, and signature collection.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of signature request capabilities.</p>
     *
     * @return an Optional containing the SignatureRequestPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see SignatureRequestPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<SignatureRequestPort> getSignatureRequestPort() {
        String esignProvider = ecmProperties.getEsignature() != null ? ecmProperties.getEsignature().getProvider() : null;
        Optional<SignatureRequestPort> port = adapterSelector.selectAdapter(esignProvider, SignatureRequestPort.class);
        if (port.isEmpty()) {
            port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), SignatureRequestPort.class);
        }
        if (port.isEmpty()) {
            log.warn("No SignatureRequestPort adapter found (provider='{}', adapterType='{}').", esignProvider, ecmProperties.getAdapterType());
        }
        return port;
    }

    /**
     * Retrieves a SignatureValidationPort implementation for validating electronic signatures.
     *
     * <p>The SignatureValidationPort provides functionality for validating the authenticity,
     * integrity, and legal compliance of electronic signatures. This includes cryptographic
     * verification, certificate validation, and compliance checking.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of signature validation capabilities.</p>
     *
     * @return an Optional containing the SignatureValidationPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see SignatureValidationPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<SignatureValidationPort> getSignatureValidationPort() {
        String esignProvider = ecmProperties.getEsignature() != null ? ecmProperties.getEsignature().getProvider() : null;
        Optional<SignatureValidationPort> port = adapterSelector.selectAdapter(esignProvider, SignatureValidationPort.class);
        if (port.isEmpty()) {
            port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), SignatureValidationPort.class);
        }
        if (port.isEmpty()) {
            log.warn("No SignatureValidationPort adapter found (provider='{}', adapterType='{}').", esignProvider, ecmProperties.getAdapterType());
        }
        return port;
    }

    /**
     * Retrieves a SignatureProofPort implementation for generating signature proof and certificates.
     *
     * <p>The SignatureProofPort provides functionality for generating tamper-evident
     * proof documents and certificates that demonstrate the validity of electronic signatures.
     * This includes audit trails, completion certificates, and legal proof documentation.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of signature proof capabilities.</p>
     *
     * @return an Optional containing the SignatureProofPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see SignatureProofPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<SignatureProofPort> getSignatureProofPort() {
        String esignProvider = ecmProperties.getEsignature() != null ? ecmProperties.getEsignature().getProvider() : null;
        Optional<SignatureProofPort> port = adapterSelector.selectAdapter(esignProvider, SignatureProofPort.class);
        if (port.isEmpty()) {
            port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), SignatureProofPort.class);
        }
        if (port.isEmpty()) {
            log.warn("No SignatureProofPort adapter found (provider='{}', adapterType='{}').", esignProvider, ecmProperties.getAdapterType());
        }
        return port;
    }

    /**
     * Retrieves a DocumentExtractionPort implementation for text extraction and OCR operations.
     *
     * <p>The DocumentExtractionPort provides functionality for extracting text and other content
     * from documents using various IDP technologies such as Optical Character Recognition (OCR),
     * handwriting recognition, and advanced text analysis. This includes both synchronous and
     * asynchronous processing patterns for real-time and batch processing scenarios.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of document extraction capabilities.</p>
     *
     * @return an Optional containing the DocumentExtractionPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see DocumentExtractionPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<DocumentExtractionPort> getDocumentExtractionPort() {
        Optional<DocumentExtractionPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), DocumentExtractionPort.class);
        if (port.isEmpty()) {
            log.warn("No DocumentExtractionPort adapter found for type: '{}'. " +
                    "A no-op adapter will be used as fallback. " +
                    "To enable document extraction features, configure a suitable IDP adapter (e.g., aws-textract, azure-form-recognizer) " +
                    "in your application properties under 'firefly.ecm.adapter-type'.",
                    ecmProperties.getAdapterType());
        } else {
            log.debug("DocumentExtractionPort adapter found: {}", port.get().getClass().getSimpleName());
        }
        return port;
    }

    /**
     * Retrieves a DocumentClassificationPort implementation for document classification and categorization.
     *
     * <p>The DocumentClassificationPort provides functionality for automatically classifying and
     * categorizing documents using various IDP technologies such as machine learning models,
     * rule-based systems, and template matching. This includes document type identification,
     * content categorization, and confidence scoring.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of document classification capabilities.</p>
     *
     * @return an Optional containing the DocumentClassificationPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see DocumentClassificationPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<DocumentClassificationPort> getDocumentClassificationPort() {
        Optional<DocumentClassificationPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), DocumentClassificationPort.class);
        if (port.isEmpty()) {
            log.warn("No DocumentClassificationPort adapter found for type: '{}'. " +
                    "A no-op adapter will be used as fallback. " +
                    "To enable document classification features, configure a suitable IDP adapter " +
                    "in your application properties under 'firefly.ecm.adapter-type'.",
                    ecmProperties.getAdapterType());
        } else {
            log.debug("DocumentClassificationPort adapter found: {}", port.get().getClass().getSimpleName());
        }
        return port;
    }

    /**
     * Retrieves a DocumentValidationPort implementation for document validation and verification.
     *
     * <p>The DocumentValidationPort provides functionality for validating documents and extracted
     * data using various validation techniques including business rule validation, format
     * verification, data consistency checks, and compliance validation. This supports multiple
     * validation levels from basic format checks to comprehensive compliance validation.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of document validation capabilities.</p>
     *
     * @return an Optional containing the DocumentValidationPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see DocumentValidationPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<DocumentValidationPort> getDocumentValidationPort() {
        Optional<DocumentValidationPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), DocumentValidationPort.class);
        if (port.isEmpty()) {
            log.warn("No DocumentValidationPort adapter found for type: '{}'. " +
                    "A no-op adapter will be used as fallback. " +
                    "To enable document validation features, configure a suitable IDP adapter " +
                    "in your application properties under 'firefly.ecm.adapter-type'.",
                    ecmProperties.getAdapterType());
        } else {
            log.debug("DocumentValidationPort adapter found: {}", port.get().getClass().getSimpleName());
        }
        return port;
    }

    /**
     * Retrieves a DataExtractionPort implementation for structured and semi-structured data extraction.
     *
     * <p>The DataExtractionPort provides functionality for extracting structured data from documents
     * including forms, tables, key-value pairs, and other organized data elements. This focuses
     * on understanding document structure and extracting meaningful data relationships rather
     * than just raw text extraction.</p>
     *
     * <p>If no suitable adapter is found for the configured adapter type, a warning
     * is logged and an empty Optional is returned, allowing the application to
     * gracefully handle the absence of structured data extraction capabilities.</p>
     *
     * @return an Optional containing the DataExtractionPort implementation if available,
     *         or empty if no suitable adapter is found
     * @see DataExtractionPort
     * @see AdapterSelector#selectAdapter(String, Class)
     */
    public Optional<DataExtractionPort> getDataExtractionPort() {
        Optional<DataExtractionPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), DataExtractionPort.class);
        if (port.isEmpty()) {
            log.warn("No DataExtractionPort adapter found for type: '{}'. " +
                    "A no-op adapter will be used as fallback. " +
                    "To enable structured data extraction features, configure a suitable IDP adapter " +
                    "in your application properties under 'firefly.ecm.adapter-type'.",
                    ecmProperties.getAdapterType());
        } else {
            log.debug("DataExtractionPort adapter found: {}", port.get().getClass().getSimpleName());
        }
        return port;
    }
}
