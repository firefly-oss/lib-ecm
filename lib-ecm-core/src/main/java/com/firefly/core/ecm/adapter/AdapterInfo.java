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
package com.firefly.core.ecm.adapter;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;

/**
 * Information about a registered ECM adapter.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class AdapterInfo {
    
    /**
     * Spring bean name of the adapter
     */
    private final String beanName;
    
    /**
     * The actual adapter bean instance
     */
    private final Object adapterBean;
    
    /**
     * Adapter type identifier
     */
    private final String type;
    
    /**
     * Adapter priority
     */
    private final Integer priority;
    
    /**
     * Adapter description
     */
    private final String description;
    
    /**
     * Adapter version
     */
    private final String version;
    
    /**
     * Adapter vendor
     */
    private final String vendor;
    
    /**
     * Required configuration properties
     */
    private final Set<String> requiredProperties;
    
    /**
     * Optional configuration properties
     */
    private final Set<String> optionalProperties;
    
    /**
     * Supported features
     */
    private final Set<AdapterFeature> supportedFeatures;
    
    /**
     * Minimum required profile
     */
    private final AdapterProfile minimumProfile;
    
    /**
     * Check if adapter supports a specific feature.
     *
     * @param feature the feature to check
     * @return true if feature is supported, false otherwise
     */
    public boolean supportsFeature(AdapterFeature feature) {
        return supportedFeatures != null && supportedFeatures.contains(feature);
    }
    
    /**
     * Check if adapter requires a specific property.
     *
     * @param property the property to check
     * @return true if property is required, false otherwise
     */
    public boolean requiresProperty(String property) {
        return requiredProperties != null && requiredProperties.contains(property);
    }
    
    /**
     * Check if adapter supports a specific property.
     *
     * @param property the property to check
     * @return true if property is supported (required or optional), false otherwise
     */
    public boolean supportsProperty(String property) {
        return requiresProperty(property) || 
               (optionalProperties != null && optionalProperties.contains(property));
    }
    
    /**
     * Get adapter bean cast to specific type.
     *
     * @param type the type to cast to
     * @param <T> the type parameter
     * @return the adapter bean cast to the specified type
     * @throws ClassCastException if the adapter bean cannot be cast to the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T getAdapterBean(Class<T> type) {
        return (T) adapterBean;
    }
}
