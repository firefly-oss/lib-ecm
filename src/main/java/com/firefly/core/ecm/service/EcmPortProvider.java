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
import com.firefly.core.ecm.config.EcmProperties;
import com.firefly.core.ecm.port.document.*;
import com.firefly.core.ecm.port.folder.*;
import com.firefly.core.ecm.port.security.*;
import com.firefly.core.ecm.port.audit.*;
import com.firefly.core.ecm.port.esignature.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service that provides ECM ports with proper adapter selection and logging.
 * Handles the logic for selecting adapters and logging when no adapter is found.
 */
@Slf4j
@Service
public class EcmPortProvider {
    
    private final AdapterSelector adapterSelector;
    private final EcmProperties ecmProperties;
    
    public EcmPortProvider(AdapterSelector adapterSelector, EcmProperties ecmProperties) {
        this.adapterSelector = adapterSelector;
        this.ecmProperties = ecmProperties;
    }
    
    /**
     * Get document port with adapter selection.
     */
    public Optional<DocumentPort> getDocumentPort() {
        Optional<DocumentPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), DocumentPort.class);
        if (port.isEmpty()) {
            log.warn("No DocumentPort adapter found for type: {}. Document management features will not be available.", 
                    ecmProperties.getAdapterType());
        }
        return port;
    }
    
    /**
     * Get document content port with adapter selection.
     */
    public Optional<DocumentContentPort> getDocumentContentPort() {
        Optional<DocumentContentPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), DocumentContentPort.class);
        if (port.isEmpty()) {
            log.warn("No DocumentContentPort adapter found for type: {}. Document content storage features will not be available.", 
                    ecmProperties.getAdapterType());
        }
        return port;
    }
    
    /**
     * Get document version port with adapter selection.
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
     * Get document search port with adapter selection.
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
     * Get folder port with adapter selection.
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
     * Get folder hierarchy port with adapter selection.
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
     * Get permission port with adapter selection.
     */
    public Optional<PermissionPort> getPermissionPort() {
        Optional<PermissionPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), PermissionPort.class);
        if (port.isEmpty()) {
            log.warn("No PermissionPort adapter found for type: {}. Permission management features will not be available.", 
                    ecmProperties.getAdapterType());
        }
        return port;
    }
    
    /**
     * Get document security port with adapter selection.
     */
    public Optional<DocumentSecurityPort> getDocumentSecurityPort() {
        Optional<DocumentSecurityPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), DocumentSecurityPort.class);
        if (port.isEmpty()) {
            log.warn("No DocumentSecurityPort adapter found for type: {}. Document security features will not be available.", 
                    ecmProperties.getAdapterType());
        }
        return port;
    }
    
    /**
     * Get audit port with adapter selection.
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
     * Get signature envelope port with adapter selection.
     */
    public Optional<SignatureEnvelopePort> getSignatureEnvelopePort() {
        Optional<SignatureEnvelopePort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), SignatureEnvelopePort.class);
        if (port.isEmpty()) {
            log.warn("No SignatureEnvelopePort adapter found for type: {}. eSignature envelope features will not be available.", 
                    ecmProperties.getAdapterType());
        }
        return port;
    }
    
    /**
     * Get signature request port with adapter selection.
     */
    public Optional<SignatureRequestPort> getSignatureRequestPort() {
        Optional<SignatureRequestPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), SignatureRequestPort.class);
        if (port.isEmpty()) {
            log.warn("No SignatureRequestPort adapter found for type: {}. eSignature request features will not be available.", 
                    ecmProperties.getAdapterType());
        }
        return port;
    }
    
    /**
     * Get signature validation port with adapter selection.
     */
    public Optional<SignatureValidationPort> getSignatureValidationPort() {
        Optional<SignatureValidationPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), SignatureValidationPort.class);
        if (port.isEmpty()) {
            log.warn("No SignatureValidationPort adapter found for type: {}. Signature validation features will not be available.", 
                    ecmProperties.getAdapterType());
        }
        return port;
    }
    
    /**
     * Get signature proof port with adapter selection.
     */
    public Optional<SignatureProofPort> getSignatureProofPort() {
        Optional<SignatureProofPort> port = adapterSelector.selectAdapter(ecmProperties.getAdapterType(), SignatureProofPort.class);
        if (port.isEmpty()) {
            log.warn("No SignatureProofPort adapter found for type: {}. Signature proof features will not be available.", 
                    ecmProperties.getAdapterType());
        }
        return port;
    }
}
