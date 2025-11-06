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
package com.firefly.core.ecm.domain.model.esignature;

import com.firefly.core.ecm.domain.enums.esignature.FieldType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;
import java.util.UUID;

/**
 * Signature field entity representing a specific field/tab for signing.
 * Defines where and how a signer should interact with the document.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class SignatureField {
    
    /**
     * Unique field identifier (UUID)
     */
    private final UUID id;
    
    /**
     * Field name/label
     */
    private final String name;
    
    /**
     * Field type (SIGNATURE, TEXT, DATE, etc.)
     */
    private final FieldType fieldType;
    
    /**
     * Document ID where this field is located
     */
    private final UUID documentId;
    
    /**
     * Page number in the document (1-based)
     */
    private final Integer pageNumber;
    
    /**
     * X coordinate position on the page
     */
    private final Double xPosition;
    
    /**
     * Y coordinate position on the page
     */
    private final Double yPosition;
    
    /**
     * Field width
     */
    private final Double width;
    
    /**
     * Field height
     */
    private final Double height;
    
    /**
     * Whether this field is required
     */
    private final Boolean required;
    
    /**
     * Whether this field is read-only
     */
    private final Boolean readOnly;
    
    /**
     * Field value (for text fields, checkboxes, etc.)
     */
    private final String value;
    
    /**
     * Default value for the field
     */
    private final String defaultValue;
    
    /**
     * Field validation pattern/rules
     */
    private final String validationPattern;
    
    /**
     * Field tooltip/help text
     */
    private final String tooltip;
    
    /**
     * Field tab order
     */
    private final Integer tabOrder;
    
    /**
     * Field font settings
     */
    private final FontSettings fontSettings;
    
    /**
     * Field metadata
     */
    private final Map<String, Object> metadata;
    
    /**
     * External field ID from provider
     */
    private final String externalFieldId;
    
    /**
     * Field group/category
     */
    private final String fieldGroup;
    
    /**
     * Whether field is conditionally visible
     */
    private final Boolean conditional;
    
    /**
     * Condition for field visibility
     */
    private final String visibilityCondition;
}
