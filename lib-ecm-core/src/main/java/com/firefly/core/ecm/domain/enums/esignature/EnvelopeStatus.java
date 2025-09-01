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
 * Signature envelope status enumeration.
 */
public enum EnvelopeStatus {
    
    /**
     * Envelope is being created/prepared
     */
    DRAFT,
    
    /**
     * Envelope has been sent to signers
     */
    SENT,
    
    /**
     * Envelope is in progress (some signatures received)
     */
    IN_PROGRESS,
    
    /**
     * All required signatures have been completed
     */
    COMPLETED,
    
    /**
     * Envelope has been declined by a signer
     */
    DECLINED,
    
    /**
     * Envelope has been voided/cancelled
     */
    VOIDED,
    
    /**
     * Envelope has expired
     */
    EXPIRED,
    
    /**
     * Envelope delivery failed
     */
    DELIVERY_FAILED,
    
    /**
     * Envelope is on hold
     */
    ON_HOLD,
    
    /**
     * Envelope is being processed
     */
    PROCESSING,
    
    /**
     * Envelope has been archived
     */
    ARCHIVED
}
