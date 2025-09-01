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
package com.firefly.core.ecm.config;

import com.firefly.core.ecm.adapter.AdapterRegistry;
import com.firefly.core.ecm.adapter.AdapterSelector;
import com.firefly.core.ecm.port.document.*;
import com.firefly.core.ecm.port.folder.*;
import com.firefly.core.ecm.port.security.*;
import com.firefly.core.ecm.port.audit.*;
import com.firefly.core.ecm.port.esignature.*;
import com.firefly.core.ecm.port.idp.*;
import com.firefly.core.ecm.service.EcmPortProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot auto-configuration for the Firefly ECM (Enterprise Content Management) system.
 *
 * <p>This configuration class automatically sets up the ECM infrastructure based on
 * application properties and feature flags. It handles:</p>
 * <ul>
 *   <li>Adapter discovery and registration</li>
 *   <li>Port provider configuration</li>
 *   <li>Conditional bean creation based on feature flags</li>
 *   <li>Component scanning for ECM-related beans</li>
 * </ul>
 *
 * <p>The auto-configuration is activated when the property {@code firefly.ecm.enabled}
 * is set to {@code true} (which is the default). Individual features can be disabled
 * using the {@code firefly.ecm.features.*} properties.</p>
 *
 * <p>Example configuration to enable only basic document management:</p>
 * <pre>
 * firefly:
 *   ecm:
 *     enabled: true
 *     adapter-type: s3
 *     features:
 *       document-management: true
 *       content-storage: true
 *       versioning: false
 *       esignature: false
 * </pre>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 * @see EcmProperties
 * @see EcmPortProvider
 * @see AdapterSelector
 * @see org.springframework.boot.autoconfigure.AutoConfiguration
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(EcmProperties.class)
@ComponentScan(basePackages = "com.firefly.core.ecm")
@ConditionalOnProperty(prefix = "firefly.ecm", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EcmAutoConfiguration {

    /**
     * Configures the central ECM port provider that manages adapter selection and port provisioning.
     *
     * <p>The port provider acts as a factory for ECM ports, selecting the appropriate
     * adapter implementation based on the configured adapter type and feature flags.
     * It serves as the main entry point for accessing ECM functionality.</p>
     *
     * @param adapterSelector the adapter selector for choosing appropriate adapters
     * @param ecmProperties the ECM configuration properties
     * @return a configured EcmPortProvider instance
     * @see EcmPortProvider
     * @see AdapterSelector
     */
    @Bean
    public EcmPortProvider ecmPortProvider(AdapterSelector adapterSelector, EcmProperties ecmProperties) {
        log.info("Configuring ECM Port Provider with adapter type: {}", ecmProperties.getAdapterType());
        return new EcmPortProvider(adapterSelector, ecmProperties);
    }

    /**
     * Configures the document port for basic document CRUD operations.
     *
     * <p>This bean is only created when the {@code document-management} feature is enabled.
     * The document port provides core functionality for creating, reading, updating,
     * and deleting documents in the ECM system.</p>
     *
     * @param portProvider the ECM port provider
     * @return a DocumentPort implementation
     * @throws IllegalStateException if no suitable DocumentPort adapter is available
     * @see DocumentPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "document-management", havingValue = "true", matchIfMissing = true)
    public DocumentPort documentPort(EcmPortProvider portProvider) {
        return portProvider.getDocumentPort()
            .orElseThrow(() -> new IllegalStateException("No DocumentPort adapter available"));
    }

    /**
     * Configures the document content port for binary content operations.
     *
     * <p>This bean is only created when the {@code content-storage} feature is enabled.
     * The document content port handles the storage and retrieval of document binary
     * content, including upload, download, and streaming operations.</p>
     *
     * @param portProvider the ECM port provider
     * @return a DocumentContentPort implementation
     * @throws IllegalStateException if no suitable DocumentContentPort adapter is available
     * @see DocumentContentPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "content-storage", havingValue = "true", matchIfMissing = true)
    public DocumentContentPort documentContentPort(EcmPortProvider portProvider) {
        return portProvider.getDocumentContentPort()
            .orElseThrow(() -> new IllegalStateException("No DocumentContentPort adapter available"));
    }
    
    /**
     * Configures the document version port for document versioning capabilities.
     *
     * <p>This bean is only created when the {@code versioning} feature is enabled.
     * The document version port provides functionality for managing document versions,
     * including creating new versions, retrieving version history, and comparing versions.</p>
     *
     * @param portProvider the ECM port provider
     * @return a DocumentVersionPort implementation
     * @throws IllegalStateException if no suitable DocumentVersionPort adapter is available
     * @see DocumentVersionPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "versioning", havingValue = "true", matchIfMissing = true)
    public DocumentVersionPort documentVersionPort(EcmPortProvider portProvider) {
        return portProvider.getDocumentVersionPort()
            .orElseThrow(() -> new IllegalStateException("No DocumentVersionPort adapter available"));
    }

    /**
     * Configures the document search port for search and query capabilities.
     *
     * <p>This bean is only created when the {@code search} feature is enabled.
     * The document search port provides functionality for searching documents by
     * metadata, content, and other criteria using various search strategies.</p>
     *
     * @param portProvider the ECM port provider
     * @return a DocumentSearchPort implementation
     * @throws IllegalStateException if no suitable DocumentSearchPort adapter is available
     * @see DocumentSearchPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "search", havingValue = "true", matchIfMissing = true)
    public DocumentSearchPort documentSearchPort(EcmPortProvider portProvider) {
        return portProvider.getDocumentSearchPort()
            .orElseThrow(() -> new IllegalStateException("No DocumentSearchPort adapter available"));
    }

    /**
     * Configures the folder port for basic folder management operations.
     *
     * <p>This bean is only created when the {@code folder-management} feature is enabled.
     * The folder port provides functionality for creating, reading, updating, and
     * deleting folders in the ECM system.</p>
     *
     * @param portProvider the ECM port provider
     * @return a FolderPort implementation
     * @throws IllegalStateException if no suitable FolderPort adapter is available
     * @see FolderPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "folder-management", havingValue = "true", matchIfMissing = true)
    public FolderPort folderPort(EcmPortProvider portProvider) {
        return portProvider.getFolderPort()
            .orElseThrow(() -> new IllegalStateException("No FolderPort adapter available"));
    }

    /**
     * Configures the folder hierarchy port for hierarchical folder operations.
     *
     * <p>This bean is only created when the {@code folder-hierarchy} feature is enabled.
     * The folder hierarchy port provides functionality for managing hierarchical
     * folder structures, including tree navigation, path resolution, and parent-child relationships.</p>
     *
     * @param portProvider the ECM port provider
     * @return a FolderHierarchyPort implementation
     * @throws IllegalStateException if no suitable FolderHierarchyPort adapter is available
     * @see FolderHierarchyPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "folder-hierarchy", havingValue = "true", matchIfMissing = true)
    public FolderHierarchyPort folderHierarchyPort(EcmPortProvider portProvider) {
        return portProvider.getFolderHierarchyPort()
            .orElseThrow(() -> new IllegalStateException("No FolderHierarchyPort adapter available"));
    }

    /**
     * Configures the permission port for access control and permission management.
     *
     * <p>This bean is only created when the {@code permissions} feature is enabled.
     * The permission port provides functionality for managing user and group permissions
     * on documents and folders, including granting, revoking, and checking access rights.</p>
     *
     * @param portProvider the ECM port provider
     * @return a PermissionPort implementation
     * @throws IllegalStateException if no suitable PermissionPort adapter is available
     * @see PermissionPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "permissions", havingValue = "true", matchIfMissing = true)
    public PermissionPort permissionPort(EcmPortProvider portProvider) {
        return portProvider.getPermissionPort()
            .orElseThrow(() -> new IllegalStateException("No PermissionPort adapter available"));
    }
    
    /**
     * Configures the document security port for document-level security operations.
     *
     * <p>This bean is only created when the {@code security} feature is enabled.
     * The document security port provides functionality for applying security policies,
     * encryption, digital rights management, and other security-related operations on documents.</p>
     *
     * @param portProvider the ECM port provider
     * @return a DocumentSecurityPort implementation
     * @throws IllegalStateException if no suitable DocumentSecurityPort adapter is available
     * @see DocumentSecurityPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "security", havingValue = "true", matchIfMissing = true)
    public DocumentSecurityPort documentSecurityPort(EcmPortProvider portProvider) {
        return portProvider.getDocumentSecurityPort()
            .orElseThrow(() -> new IllegalStateException("No DocumentSecurityPort adapter available"));
    }

    /**
     * Configures the audit port for audit trail and compliance logging.
     *
     * <p>This bean is only created when the {@code auditing} feature is enabled.
     * The audit port provides functionality for logging user actions, system events,
     * and maintaining compliance records for regulatory requirements.</p>
     *
     * @param portProvider the ECM port provider
     * @return an AuditPort implementation
     * @throws IllegalStateException if no suitable AuditPort adapter is available
     * @see AuditPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "auditing", havingValue = "true", matchIfMissing = true)
    public AuditPort auditPort(EcmPortProvider portProvider) {
        return portProvider.getAuditPort()
            .orElseThrow(() -> new IllegalStateException("No AuditPort adapter available"));
    }

    /**
     * Configures the signature envelope port for eSignature envelope management.
     *
     * <p>This bean is only created when the {@code esignature} feature is explicitly enabled.
     * The signature envelope port provides functionality for creating, managing, and
     * tracking signature envelopes that contain documents requiring electronic signatures.</p>
     *
     * @param portProvider the ECM port provider
     * @return a SignatureEnvelopePort implementation
     * @throws IllegalStateException if no suitable SignatureEnvelopePort adapter is available
     * @see SignatureEnvelopePort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "esignature", havingValue = "true", matchIfMissing = false)
    public SignatureEnvelopePort signatureEnvelopePort(EcmPortProvider portProvider) {
        return portProvider.getSignatureEnvelopePort()
            .orElseThrow(() -> new IllegalStateException("No SignatureEnvelopePort adapter available"));
    }

    /**
     * Configures the signature request port for managing individual signature requests.
     *
     * <p>This bean is only created when the {@code esignature} feature is explicitly enabled.
     * The signature request port provides functionality for creating, sending, and
     * tracking individual signature requests within signature envelopes.</p>
     *
     * @param portProvider the ECM port provider
     * @return a SignatureRequestPort implementation
     * @throws IllegalStateException if no suitable SignatureRequestPort adapter is available
     * @see SignatureRequestPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "esignature", havingValue = "true", matchIfMissing = false)
    public SignatureRequestPort signatureRequestPort(EcmPortProvider portProvider) {
        return portProvider.getSignatureRequestPort()
            .orElseThrow(() -> new IllegalStateException("No SignatureRequestPort adapter available"));
    }

    /**
     * Configures the signature validation port for validating electronic signatures.
     *
     * <p>This bean is only created when the {@code esignature} feature is explicitly enabled.
     * The signature validation port provides functionality for validating the authenticity,
     * integrity, and legal compliance of electronic signatures.</p>
     *
     * @param portProvider the ECM port provider
     * @return a SignatureValidationPort implementation
     * @throws IllegalStateException if no suitable SignatureValidationPort adapter is available
     * @see SignatureValidationPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "esignature", havingValue = "true", matchIfMissing = false)
    public SignatureValidationPort signatureValidationPort(EcmPortProvider portProvider) {
        return portProvider.getSignatureValidationPort()
            .orElseThrow(() -> new IllegalStateException("No SignatureValidationPort adapter available"));
    }

    /**
     * Configures the signature proof port for generating signature proof and certificates.
     *
     * <p>This bean is only created when the {@code esignature} feature is explicitly enabled.
     * The signature proof port provides functionality for generating tamper-evident
     * proof documents and certificates that demonstrate the validity of electronic signatures.</p>
     *
     * @param portProvider the ECM port provider
     * @return a SignatureProofPort implementation
     * @throws IllegalStateException if no suitable SignatureProofPort adapter is available
     * @see SignatureProofPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "esignature", havingValue = "true", matchIfMissing = false)
    public SignatureProofPort signatureProofPort(EcmPortProvider portProvider) {
        return portProvider.getSignatureProofPort()
            .orElseThrow(() -> new IllegalStateException("No SignatureProofPort adapter available"));
    }

    /**
     * Configures the document extraction port for text extraction and OCR operations.
     *
     * <p>This bean is only created when the {@code idp} feature is explicitly enabled.
     * The document extraction port provides functionality for extracting text and other content
     * from documents using various IDP technologies such as OCR, handwriting recognition,
     * and advanced text analysis.</p>
     *
     * @param portProvider the ECM port provider
     * @return a DocumentExtractionPort implementation
     * @throws IllegalStateException if no suitable DocumentExtractionPort adapter is available
     * @see DocumentExtractionPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "idp", havingValue = "true", matchIfMissing = false)
    public DocumentExtractionPort documentExtractionPort(EcmPortProvider portProvider) {
        return portProvider.getDocumentExtractionPort()
            .orElseThrow(() -> new IllegalStateException("No DocumentExtractionPort adapter available"));
    }

    /**
     * Configures the document classification port for document classification and categorization.
     *
     * <p>This bean is only created when the {@code idp} feature is explicitly enabled.
     * The document classification port provides functionality for automatically classifying
     * and categorizing documents using various IDP technologies such as machine learning
     * models, rule-based systems, and template matching.</p>
     *
     * @param portProvider the ECM port provider
     * @return a DocumentClassificationPort implementation
     * @throws IllegalStateException if no suitable DocumentClassificationPort adapter is available
     * @see DocumentClassificationPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "idp", havingValue = "true", matchIfMissing = false)
    public DocumentClassificationPort documentClassificationPort(EcmPortProvider portProvider) {
        return portProvider.getDocumentClassificationPort()
            .orElseThrow(() -> new IllegalStateException("No DocumentClassificationPort adapter available"));
    }

    /**
     * Configures the document validation port for document validation and verification.
     *
     * <p>This bean is only created when the {@code idp} feature is explicitly enabled.
     * The document validation port provides functionality for validating documents and
     * extracted data using various validation techniques including business rule validation,
     * format verification, data consistency checks, and compliance validation.</p>
     *
     * @param portProvider the ECM port provider
     * @return a DocumentValidationPort implementation
     * @throws IllegalStateException if no suitable DocumentValidationPort adapter is available
     * @see DocumentValidationPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "idp", havingValue = "true", matchIfMissing = false)
    public DocumentValidationPort documentValidationPort(EcmPortProvider portProvider) {
        return portProvider.getDocumentValidationPort()
            .orElseThrow(() -> new IllegalStateException("No DocumentValidationPort adapter available"));
    }

    /**
     * Configures the data extraction port for structured and semi-structured data extraction.
     *
     * <p>This bean is only created when the {@code idp} feature is explicitly enabled.
     * The data extraction port provides functionality for extracting structured data from
     * documents including forms, tables, key-value pairs, and other organized data elements.</p>
     *
     * @param portProvider the ECM port provider
     * @return a DataExtractionPort implementation
     * @throws IllegalStateException if no suitable DataExtractionPort adapter is available
     * @see DataExtractionPort
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "idp", havingValue = "true", matchIfMissing = false)
    public DataExtractionPort dataExtractionPort(EcmPortProvider portProvider) {
        return portProvider.getDataExtractionPort()
            .orElseThrow(() -> new IllegalStateException("No DataExtractionPort adapter available"));
    }
}
