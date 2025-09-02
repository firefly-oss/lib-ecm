package com.firefly.core.ecm.adapter.noop;

import com.firefly.core.ecm.port.audit.AuditPort;
import com.firefly.core.ecm.port.document.*;
import com.firefly.core.ecm.port.esignature.*;
import com.firefly.core.ecm.port.folder.*;
import com.firefly.core.ecm.port.idp.*;
import com.firefly.core.ecm.port.security.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory for creating no-op adapter implementations for all ECM port interfaces.
 *
 * <p>This factory provides a centralized way to create no-op adapters that serve as
 * fallbacks when no real adapter implementations are available. These adapters allow
 * the ECM system to start successfully while providing clear feedback about missing
 * functionality.</p>
 *
 * <p>All no-op adapters created by this factory:</p>
 * <ul>
 *   <li>Log warnings when methods are called</li>
 *   <li>Return empty results for query operations</li>
 *   <li>Return error signals for modification operations</li>
 *   <li>Use permissive defaults for security operations</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 * @see NoOpAdapterBase
 */
@Slf4j
@Component
public class NoOpAdapterFactory {

    /**
     * Creates a no-op DocumentPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public DocumentPort createDocumentPort() {
        log.info("Creating no-op DocumentPort adapter as fallback");
        return new NoOpGenericAdapter<>("DocumentPort", DocumentPort.class).getProxy();
    }

    /**
     * Creates a no-op DocumentContentPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public DocumentContentPort createDocumentContentPort() {
        log.info("Creating no-op DocumentContentPort adapter as fallback");
        return new NoOpGenericAdapter<>("DocumentContentPort", DocumentContentPort.class).getProxy();
    }

    /**
     * Creates a no-op DocumentSecurityPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public DocumentSecurityPort createDocumentSecurityPort() {
        log.info("Creating no-op DocumentSecurityPort adapter as fallback");
        return new NoOpGenericAdapter<>("DocumentSecurityPort", DocumentSecurityPort.class).getProxy();
    }

    /**
     * Creates a no-op DocumentVersionPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public DocumentVersionPort createDocumentVersionPort() {
        log.info("Creating no-op DocumentVersionPort adapter as fallback");
        return new NoOpGenericAdapter<>("DocumentVersionPort", DocumentVersionPort.class).getProxy();
    }

    /**
     * Creates a no-op DocumentSearchPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public DocumentSearchPort createDocumentSearchPort() {
        log.info("Creating no-op DocumentSearchPort adapter as fallback");
        return new NoOpGenericAdapter<>("DocumentSearchPort", DocumentSearchPort.class).getProxy();
    }

    /**
     * Creates a no-op FolderPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public FolderPort createFolderPort() {
        log.info("Creating no-op FolderPort adapter as fallback");
        return new NoOpGenericAdapter<>("FolderPort", FolderPort.class).getProxy();
    }

    /**
     * Creates a no-op FolderHierarchyPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public FolderHierarchyPort createFolderHierarchyPort() {
        log.info("Creating no-op FolderHierarchyPort adapter as fallback");
        return new NoOpGenericAdapter<>("FolderHierarchyPort", FolderHierarchyPort.class).getProxy();
    }

    /**
     * Creates a no-op PermissionPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public PermissionPort createPermissionPort() {
        log.info("Creating no-op PermissionPort adapter as fallback");
        return new NoOpGenericAdapter<>("PermissionPort", PermissionPort.class).getProxy();
    }

    /**
     * Creates a no-op AuditPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public AuditPort createAuditPort() {
        log.info("Creating no-op AuditPort adapter as fallback");
        return new NoOpGenericAdapter<>("AuditPort", AuditPort.class).getProxy();
    }

    /**
     * Creates a no-op SignatureEnvelopePort adapter.
     *
     * @return a new no-op adapter instance
     */
    public SignatureEnvelopePort createSignatureEnvelopePort() {
        log.info("Creating no-op SignatureEnvelopePort adapter as fallback");
        return new NoOpGenericAdapter<>("SignatureEnvelopePort", SignatureEnvelopePort.class).getProxy();
    }

    /**
     * Creates a no-op SignatureRequestPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public SignatureRequestPort createSignatureRequestPort() {
        log.info("Creating no-op SignatureRequestPort adapter as fallback");
        return new NoOpGenericAdapter<>("SignatureRequestPort", SignatureRequestPort.class).getProxy();
    }

    /**
     * Creates a no-op SignatureValidationPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public SignatureValidationPort createSignatureValidationPort() {
        log.info("Creating no-op SignatureValidationPort adapter as fallback");
        return new NoOpGenericAdapter<>("SignatureValidationPort", SignatureValidationPort.class).getProxy();
    }

    /**
     * Creates a no-op SignatureProofPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public SignatureProofPort createSignatureProofPort() {
        log.info("Creating no-op SignatureProofPort adapter as fallback");
        return new NoOpGenericAdapter<>("SignatureProofPort", SignatureProofPort.class).getProxy();
    }

    /**
     * Creates a no-op DocumentExtractionPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public DocumentExtractionPort createDocumentExtractionPort() {
        log.info("Creating no-op DocumentExtractionPort adapter as fallback");
        return new NoOpGenericAdapter<>("DocumentExtractionPort", DocumentExtractionPort.class).getProxy();
    }

    /**
     * Creates a no-op DocumentClassificationPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public DocumentClassificationPort createDocumentClassificationPort() {
        log.info("Creating no-op DocumentClassificationPort adapter as fallback");
        return new NoOpGenericAdapter<>("DocumentClassificationPort", DocumentClassificationPort.class).getProxy();
    }

    /**
     * Creates a no-op DocumentValidationPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public DocumentValidationPort createDocumentValidationPort() {
        log.info("Creating no-op DocumentValidationPort adapter as fallback");
        return new NoOpGenericAdapter<>("DocumentValidationPort", DocumentValidationPort.class).getProxy();
    }

    /**
     * Creates a no-op DataExtractionPort adapter.
     *
     * @return a new no-op adapter instance
     */
    public DataExtractionPort createDataExtractionPort() {
        log.info("Creating no-op DataExtractionPort adapter as fallback");
        return new NoOpGenericAdapter<>("DataExtractionPort", DataExtractionPort.class).getProxy();
    }
}
