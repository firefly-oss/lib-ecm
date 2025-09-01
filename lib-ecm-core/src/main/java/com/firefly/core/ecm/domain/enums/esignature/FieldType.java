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
 * Signature field type enumeration.
 */
public enum FieldType {
    
    /**
     * Signature field
     */
    SIGNATURE,
    
    /**
     * Initial field
     */
    INITIAL,
    
    /**
     * Text input field
     */
    TEXT,
    
    /**
     * Date field
     */
    DATE,
    
    /**
     * Checkbox field
     */
    CHECKBOX,
    
    /**
     * Radio button field
     */
    RADIO,
    
    /**
     * Dropdown/select field
     */
    DROPDOWN,
    
    /**
     * Number input field
     */
    NUMBER,
    
    /**
     * Email input field
     */
    EMAIL,
    
    /**
     * Phone number field
     */
    PHONE,
    
    /**
     * Address field
     */
    ADDRESS,
    
    /**
     * Company name field
     */
    COMPANY,
    
    /**
     * Title/position field
     */
    TITLE,
    
    /**
     * Full name field
     */
    FULL_NAME,
    
    /**
     * First name field
     */
    FIRST_NAME,
    
    /**
     * Last name field
     */
    LAST_NAME,
    
    /**
     * Approval stamp
     */
    APPROVAL,
    
    /**
     * Decline stamp
     */
    DECLINE,
    
    /**
     * Notary seal
     */
    NOTARY_SEAL,
    
    /**
     * Attachment field
     */
    ATTACHMENT,
    
    /**
     * Formula/calculation field
     */
    FORMULA,
    
    /**
     * View-only field
     */
    VIEW
}
