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

import com.firefly.core.ecm.domain.enums.esignature.TextAlignment;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Font settings for signature fields.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class FontSettings {
    
    /**
     * Font family name
     */
    private final String fontFamily;
    
    /**
     * Font size in points
     */
    private final Integer fontSize;
    
    /**
     * Font color in hex format (e.g., "#000000")
     */
    private final String fontColor;
    
    /**
     * Whether font is bold
     */
    private final Boolean bold;
    
    /**
     * Whether font is italic
     */
    private final Boolean italic;
    
    /**
     * Whether font is underlined
     */
    private final Boolean underline;
    
    /**
     * Text alignment
     */
    private final TextAlignment alignment;
    
    /**
     * Background color in hex format
     */
    private final String backgroundColor;
    
    /**
     * Border color in hex format
     */
    private final String borderColor;
    
    /**
     * Border width in pixels
     */
    private final Integer borderWidth;
}
