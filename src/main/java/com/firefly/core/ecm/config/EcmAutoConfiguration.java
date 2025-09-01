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
import com.firefly.core.ecm.service.EcmPortProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for ECM system.
 * Configures adapters, ports, and services based on application properties.
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(EcmProperties.class)
@ComponentScan(basePackages = "com.firefly.core.ecm")
@ConditionalOnProperty(prefix = "firefly.ecm", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EcmAutoConfiguration {
    
    /**
     * Configure ECM port provider that manages adapter selection and port provisioning.
     */
    @Bean
    public EcmPortProvider ecmPortProvider(AdapterSelector adapterSelector, EcmProperties ecmProperties) {
        log.info("Configuring ECM Port Provider with adapter type: {}", ecmProperties.getAdapterType());
        return new EcmPortProvider(adapterSelector, ecmProperties);
    }
    
    /**
     * Configure document port with adapter selection.
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "document-management", havingValue = "true", matchIfMissing = true)
    public DocumentPort documentPort(EcmPortProvider portProvider) {
        return portProvider.getDocumentPort()
            .orElseThrow(() -> new IllegalStateException("No DocumentPort adapter available"));
    }
    
    /**
     * Configure document content port with adapter selection.
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "content-storage", havingValue = "true", matchIfMissing = true)
    public DocumentContentPort documentContentPort(EcmPortProvider portProvider) {
        return portProvider.getDocumentContentPort()
            .orElseThrow(() -> new IllegalStateException("No DocumentContentPort adapter available"));
    }
    
    /**
     * Configure document version port with adapter selection.
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "versioning", havingValue = "true", matchIfMissing = true)
    public DocumentVersionPort documentVersionPort(EcmPortProvider portProvider) {
        return portProvider.getDocumentVersionPort()
            .orElseThrow(() -> new IllegalStateException("No DocumentVersionPort adapter available"));
    }
    
    /**
     * Configure document search port with adapter selection.
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "search", havingValue = "true", matchIfMissing = true)
    public DocumentSearchPort documentSearchPort(EcmPortProvider portProvider) {
        return portProvider.getDocumentSearchPort()
            .orElseThrow(() -> new IllegalStateException("No DocumentSearchPort adapter available"));
    }
    
    /**
     * Configure folder port with adapter selection.
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "folder-management", havingValue = "true", matchIfMissing = true)
    public FolderPort folderPort(EcmPortProvider portProvider) {
        return portProvider.getFolderPort()
            .orElseThrow(() -> new IllegalStateException("No FolderPort adapter available"));
    }
    
    /**
     * Configure folder hierarchy port with adapter selection.
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "folder-hierarchy", havingValue = "true", matchIfMissing = true)
    public FolderHierarchyPort folderHierarchyPort(EcmPortProvider portProvider) {
        return portProvider.getFolderHierarchyPort()
            .orElseThrow(() -> new IllegalStateException("No FolderHierarchyPort adapter available"));
    }
    
    /**
     * Configure permission port with adapter selection.
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "permissions", havingValue = "true", matchIfMissing = true)
    public PermissionPort permissionPort(EcmPortProvider portProvider) {
        return portProvider.getPermissionPort()
            .orElseThrow(() -> new IllegalStateException("No PermissionPort adapter available"));
    }
    
    /**
     * Configure document security port with adapter selection.
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "security", havingValue = "true", matchIfMissing = true)
    public DocumentSecurityPort documentSecurityPort(EcmPortProvider portProvider) {
        return portProvider.getDocumentSecurityPort()
            .orElseThrow(() -> new IllegalStateException("No DocumentSecurityPort adapter available"));
    }
    
    /**
     * Configure audit port with adapter selection.
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "auditing", havingValue = "true", matchIfMissing = true)
    public AuditPort auditPort(EcmPortProvider portProvider) {
        return portProvider.getAuditPort()
            .orElseThrow(() -> new IllegalStateException("No AuditPort adapter available"));
    }
    
    /**
     * Configure signature envelope port with adapter selection.
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "esignature", havingValue = "true", matchIfMissing = false)
    public SignatureEnvelopePort signatureEnvelopePort(EcmPortProvider portProvider) {
        return portProvider.getSignatureEnvelopePort()
            .orElseThrow(() -> new IllegalStateException("No SignatureEnvelopePort adapter available"));
    }
    
    /**
     * Configure signature request port with adapter selection.
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "esignature", havingValue = "true", matchIfMissing = false)
    public SignatureRequestPort signatureRequestPort(EcmPortProvider portProvider) {
        return portProvider.getSignatureRequestPort()
            .orElseThrow(() -> new IllegalStateException("No SignatureRequestPort adapter available"));
    }
    
    /**
     * Configure signature validation port with adapter selection.
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "esignature", havingValue = "true", matchIfMissing = false)
    public SignatureValidationPort signatureValidationPort(EcmPortProvider portProvider) {
        return portProvider.getSignatureValidationPort()
            .orElseThrow(() -> new IllegalStateException("No SignatureValidationPort adapter available"));
    }
    
    /**
     * Configure signature proof port with adapter selection.
     */
    @Bean
    @ConditionalOnProperty(prefix = "firefly.ecm.features", name = "esignature", havingValue = "true", matchIfMissing = false)
    public SignatureProofPort signatureProofPort(EcmPortProvider portProvider) {
        return portProvider.getSignatureProofPort()
            .orElseThrow(() -> new IllegalStateException("No SignatureProofPort adapter available"));
    }
}
