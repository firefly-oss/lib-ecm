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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Service for selecting appropriate ECM adapters based on configuration.
 * Handles adapter selection logic and fallback mechanisms.
 */
@Slf4j
@Component
public class AdapterSelector {
    
    private final AdapterRegistry adapterRegistry;
    
    @Autowired
    public AdapterSelector(AdapterRegistry adapterRegistry) {
        this.adapterRegistry = adapterRegistry;
    }
    
    /**
     * Select adapter by type with fallback logic.
     *
     * @param preferredType the preferred adapter type
     * @param interfaceClass the interface class to implement
     * @param <T> the interface type
     * @return the selected adapter, or empty if none available
     */
    public <T> Optional<T> selectAdapter(String preferredType, Class<T> interfaceClass) {
        if (preferredType != null && !preferredType.trim().isEmpty()) {
            // Try to get adapter by preferred type
            Optional<AdapterInfo> adapterInfo = adapterRegistry.getAdapter(preferredType);
            if (adapterInfo.isPresent()) {
                Object adapterBean = adapterInfo.get().getAdapterBean();
                if (interfaceClass.isInstance(adapterBean)) {
                    log.info("Selected {} adapter of type: {}", interfaceClass.getSimpleName(), preferredType);
                    return Optional.of(interfaceClass.cast(adapterBean));
                } else {
                    log.warn("Adapter type '{}' does not implement {}", preferredType, interfaceClass.getSimpleName());
                }
            } else {
                log.warn("No adapter found for preferred type: {}", preferredType);
            }
        }
        
        // Fallback to any available adapter implementing the interface
        Optional<T> fallbackAdapter = adapterRegistry.getAdapter(interfaceClass);
        if (fallbackAdapter.isPresent()) {
            log.info("Using fallback {} adapter", interfaceClass.getSimpleName());
            return fallbackAdapter;
        }
        
        log.error("No {} adapter available", interfaceClass.getSimpleName());
        return Optional.empty();
    }
    
    /**
     * Select adapter by type only.
     *
     * @param adapterType the adapter type
     * @param interfaceClass the interface class
     * @param <T> the interface type
     * @return the selected adapter, or empty if not found
     */
    public <T> Optional<T> selectAdapterByType(String adapterType, Class<T> interfaceClass) {
        Optional<AdapterInfo> adapterInfo = adapterRegistry.getAdapter(adapterType);
        if (adapterInfo.isPresent()) {
            Object adapterBean = adapterInfo.get().getAdapterBean();
            if (interfaceClass.isInstance(adapterBean)) {
                return Optional.of(interfaceClass.cast(adapterBean));
            }
        }
        return Optional.empty();
    }
    
    /**
     * Select adapter by interface only (highest priority).
     *
     * @param interfaceClass the interface class
     * @param <T> the interface type
     * @return the selected adapter, or empty if not found
     */
    public <T> Optional<T> selectAdapterByInterface(Class<T> interfaceClass) {
        return adapterRegistry.getAdapter(interfaceClass);
    }
    
    /**
     * Check if adapter is available for the given type and interface.
     *
     * @param adapterType the adapter type
     * @param interfaceClass the interface class
     * @return true if adapter is available, false otherwise
     */
    public boolean isAdapterAvailable(String adapterType, Class<?> interfaceClass) {
        if (adapterType == null || adapterType.trim().isEmpty()) {
            return adapterRegistry.hasAdapter(interfaceClass);
        }
        
        Optional<AdapterInfo> adapterInfo = adapterRegistry.getAdapter(adapterType);
        if (adapterInfo.isPresent()) {
            return interfaceClass.isInstance(adapterInfo.get().getAdapterBean());
        }
        
        return false;
    }
    
    /**
     * Get adapter information for the given type.
     *
     * @param adapterType the adapter type
     * @return adapter information, or empty if not found
     */
    public Optional<AdapterInfo> getAdapterInfo(String adapterType) {
        return adapterRegistry.getAdapterInfo(adapterType);
    }
    
    /**
     * Validate adapter configuration requirements.
     *
     * @param adapterType the adapter type
     * @param configuredProperties the configured properties
     * @return validation result
     */
    public AdapterValidationResult validateAdapterConfiguration(String adapterType, java.util.Set<String> configuredProperties) {
        Optional<AdapterInfo> adapterInfo = adapterRegistry.getAdapter(adapterType);
        if (adapterInfo.isEmpty()) {
            return AdapterValidationResult.builder()
                .valid(false)
                .errorMessage("Adapter type '" + adapterType + "' not found")
                .build();
        }
        
        AdapterInfo info = adapterInfo.get();
        java.util.Set<String> missingProperties = new java.util.HashSet<>(info.getRequiredProperties());
        missingProperties.removeAll(configuredProperties);
        
        if (!missingProperties.isEmpty()) {
            return AdapterValidationResult.builder()
                .valid(false)
                .errorMessage("Missing required properties: " + missingProperties)
                .missingProperties(missingProperties)
                .build();
        }
        
        return AdapterValidationResult.builder()
            .valid(true)
            .adapterInfo(info)
            .build();
    }
}
