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
 * Signature request status enumeration.
 */
public enum SignatureRequestStatus {
    
    /**
     * Request is being created
     */
    CREATED,
    
    /**
     * Request has been sent to signer
     */
    SENT,
    
    /**
     * Request has been delivered to signer
     */
    DELIVERED,
    
    /**
     * Signer has viewed the request
     */
    VIEWED,
    
    /**
     * Signer is currently signing
     */
    SIGNING,
    
    /**
     * Request has been signed/completed
     */
    SIGNED,
    
    /**
     * Request has been declined by signer
     */
    DECLINED,
    
    /**
     * Request has expired
     */
    EXPIRED,
    
    /**
     * Request has been voided
     */
    VOIDED,
    
    /**
     * Request delivery failed
     */
    DELIVERY_FAILED,
    
    /**
     * Request is pending (waiting for previous signer)
     */
    PENDING,
    
    /**
     * Request has been delegated to another signer
     */
    DELEGATED
}
