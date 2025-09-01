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
package com.firefly.core.ecm.domain.enums.idp;

/**
 * Classification confidence enumeration for Intelligent Document Processing (IDP) operations.
 * 
 * <p>This enumeration defines confidence levels for document classification results.
 * It provides a standardized way to express how certain the IDP system is about
 * its classification decisions, helping users understand the reliability of results.</p>
 * 
 * <p>Confidence levels are typically mapped to numerical ranges:</p>
 * <ul>
 *   <li><strong>VERY_HIGH:</strong> 95-100% confidence</li>
 *   <li><strong>HIGH:</strong> 85-94% confidence</li>
 *   <li><strong>MEDIUM:</strong> 70-84% confidence</li>
 *   <li><strong>LOW:</strong> 50-69% confidence</li>
 *   <li><strong>VERY_LOW:</strong> Below 50% confidence</li>
 * </ul>
 * 
 * <p>These confidence levels help determine whether results should be:</p>
 * <ul>
 *   <li>Automatically accepted (VERY_HIGH, HIGH)</li>
 *   <li>Flagged for review (MEDIUM)</li>
 *   <li>Rejected or require manual processing (LOW, VERY_LOW)</li>
 * </ul>
 * 
 * @author Firefly Software Solutions Inc.
 * @since 1.0.0
 */
public enum ClassificationConfidence {
    
    /**
     * Very high confidence level (95-100%).
     * 
     * <p>The classification result is extremely reliable and can be
     * automatically accepted without human review. The system has
     * very strong indicators supporting this classification.</p>
     */
    VERY_HIGH,
    
    /**
     * High confidence level (85-94%).
     * 
     * <p>The classification result is highly reliable and typically
     * acceptable for automated processing. Strong indicators support
     * this classification with minimal ambiguity.</p>
     */
    HIGH,
    
    /**
     * Medium confidence level (70-84%).
     * 
     * <p>The classification result is moderately reliable but may
     * benefit from human review. Some indicators support this
     * classification but there may be minor ambiguities.</p>
     */
    MEDIUM,
    
    /**
     * Low confidence level (50-69%).
     * 
     * <p>The classification result has limited reliability and should
     * be reviewed by humans. Weak indicators support this classification
     * and there are significant ambiguities or competing possibilities.</p>
     */
    LOW,
    
    /**
     * Very low confidence level (below 50%).
     * 
     * <p>The classification result is unreliable and should not be
     * used without human verification. Very weak or conflicting
     * indicators make this classification highly uncertain.</p>
     */
    VERY_LOW,
    
    /**
     * Unknown confidence level.
     * 
     * <p>The confidence level could not be determined or the
     * classification system does not provide confidence metrics.</p>
     */
    UNKNOWN,
    
    /**
     * Not applicable confidence level.
     * 
     * <p>Confidence measurement is not applicable for this type
     * of classification or processing operation.</p>
     */
    NOT_APPLICABLE
}
