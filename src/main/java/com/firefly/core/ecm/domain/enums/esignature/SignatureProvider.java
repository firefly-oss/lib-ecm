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
package com.firefly.core.ecm.domain.enums.esignature;

/**
 * eSignature provider enumeration.
 */
public enum SignatureProvider {
    
    /**
     * DocuSign eSignature provider
     */
    DOCUSIGN,
    
    /**
     * Logalty eSignature provider
     */
    LOGALTY,
    
    /**
     * Adobe Sign eSignature provider
     */
    ADOBE_SIGN,
    
    /**
     * HelloSign eSignature provider
     */
    HELLOSIGN,
    
    /**
     * PandaDoc eSignature provider
     */
    PANDADOC,
    
    /**
     * SignNow eSignature provider
     */
    SIGNNOW,
    
    /**
     * eSignLive eSignature provider
     */
    ESIGNLIVE,
    
    /**
     * Secured Signing eSignature provider
     */
    SECURED_SIGNING,
    
    /**
     * Custom/internal eSignature provider
     */
    CUSTOM,
    
    /**
     * Mock provider for testing
     */
    MOCK
}
