/*
 * Copyright 2025 Firefly Software Solutions Inc
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


package com.firefly.core.ecm.adapter.noop;

import com.firefly.core.ecm.port.audit.AuditPort;
import com.firefly.core.ecm.port.document.*;
import com.firefly.core.ecm.port.esignature.*;
import com.firefly.core.ecm.port.folder.*;
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

}
