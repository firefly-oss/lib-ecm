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
 * Service responsible for selecting appropriate ECM adapters based on configuration and requirements.
 *
 * <p>This component implements the adapter selection logic for the ECM system, providing
 * intelligent fallback mechanisms and validation capabilities. It acts as the bridge between
 * the ECM configuration and the available adapter implementations.</p>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Selecting adapters based on configured type preferences</li>
 *   <li>Providing fallback mechanisms when preferred adapters are unavailable</li>
 *   <li>Validating adapter configurations and requirements</li>
 *   <li>Checking adapter availability and compatibility</li>
 * </ul>
 *
 * <p>The selection process follows this priority order:</p>
 * <ol>
 *   <li>Preferred adapter type (if configured and available)</li>
 *   <li>Fallback to any available adapter implementing the required interface</li>
 *   <li>Return empty if no suitable adapter is found</li>
 * </ol>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 * @see AdapterRegistry
 * @see AdapterInfo
 * @see EcmPortProvider
 */
@Slf4j
@Component
public class AdapterSelector {

    /** The adapter registry containing all available adapters. */
    private final AdapterRegistry adapterRegistry;

    /**
     * Constructs a new AdapterSelector with the specified adapter registry.
     *
     * @param adapterRegistry the registry containing available adapters
     */
    @Autowired
    public AdapterSelector(AdapterRegistry adapterRegistry) {
        this.adapterRegistry = adapterRegistry;
    }

    /**
     * Selects an adapter based on the preferred type with intelligent fallback logic.
     *
     * <p>This method implements the primary adapter selection algorithm. It first attempts
     * to find an adapter matching the preferred type. If that fails, it falls back to
     * any available adapter that implements the required interface.</p>
     *
     * <p>Selection process:</p>
     * <ol>
     *   <li>If a preferred type is specified, look for an adapter of that type</li>
     *   <li>Verify the adapter implements the required interface</li>
     *   <li>If preferred type fails, fallback to any compatible adapter</li>
     *   <li>Return empty if no suitable adapter is found</li>
     * </ol>
     *
     * @param <T> the interface type that the adapter must implement
     * @param preferredType the preferred adapter type (e.g., "s3", "azure-blob")
     * @param interfaceClass the interface class that the adapter must implement
     * @return an Optional containing the selected adapter, or empty if none available
     * @see AdapterRegistry#getAdapter(String)
     * @see AdapterRegistry#getAdapter(Class)
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
     * Selects an adapter strictly by type without fallback logic.
     *
     * <p>This method provides a more restrictive selection approach that only
     * considers adapters of the exact specified type. Unlike {@link #selectAdapter},
     * this method does not fall back to other available adapters if the specified
     * type is not found or doesn't implement the required interface.</p>
     *
     * <p>Use this method when you need to ensure a specific adapter type is used
     * and want to fail gracefully if it's not available.</p>
     *
     * @param <T> the interface type that the adapter must implement
     * @param adapterType the specific adapter type to select
     * @param interfaceClass the interface class that the adapter must implement
     * @return an Optional containing the adapter if found and compatible, empty otherwise
     * @see #selectAdapter(String, Class)
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
     * Selects an adapter based solely on interface compatibility with highest priority.
     *
     * <p>This method ignores adapter type preferences and selects the highest priority
     * adapter that implements the specified interface. The priority is typically
     * determined by the adapter's registration order or explicit priority configuration.</p>
     *
     * <p>Use this method when you don't care about the specific adapter implementation
     * and just need any compatible adapter.</p>
     *
     * @param <T> the interface type that the adapter must implement
     * @param interfaceClass the interface class to find an implementation for
     * @return an Optional containing the highest priority compatible adapter, empty if none found
     * @see AdapterRegistry#getAdapter(Class)
     */
    public <T> Optional<T> selectAdapterByInterface(Class<T> interfaceClass) {
        return adapterRegistry.getAdapter(interfaceClass);
    }

    /**
     * Checks whether an adapter is available for the specified type and interface.
     *
     * <p>This method provides a way to test adapter availability without actually
     * retrieving the adapter instance. It's useful for conditional logic and
     * feature availability checks.</p>
     *
     * <p>If no adapter type is specified (null or empty), the method checks for
     * any adapter implementing the interface. If a specific type is provided,
     * it checks for that exact type and interface combination.</p>
     *
     * @param adapterType the adapter type to check (can be null for any type)
     * @param interfaceClass the interface class that must be implemented
     * @return {@code true} if a compatible adapter is available, {@code false} otherwise
     * @see AdapterRegistry#hasAdapter(Class)
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
     * Retrieves detailed information about a specific adapter type.
     *
     * <p>This method provides access to comprehensive adapter metadata including
     * supported features, required properties, version information, and other
     * adapter-specific details. This information is useful for adapter discovery,
     * configuration validation, and runtime introspection.</p>
     *
     * @param adapterType the adapter type identifier to look up
     * @return an Optional containing the AdapterInfo if found, empty otherwise
     * @see AdapterInfo
     * @see AdapterRegistry#getAdapterInfo(String)
     */
    public Optional<AdapterInfo> getAdapterInfo(String adapterType) {
        return adapterRegistry.getAdapterInfo(adapterType);
    }

    /**
     * Validates adapter configuration against the adapter's requirements.
     *
     * <p>This method performs comprehensive validation of adapter configuration,
     * checking that all required properties are provided and that the adapter
     * is properly configured for use. The validation includes:</p>
     * <ul>
     *   <li>Adapter existence verification</li>
     *   <li>Required property presence checking</li>
     *   <li>Configuration completeness validation</li>
     * </ul>
     *
     * <p>The validation result provides detailed information about any issues
     * found, including missing properties and error descriptions, making it
     * easy to diagnose and fix configuration problems.</p>
     *
     * @param adapterType the adapter type to validate
     * @param configuredProperties the set of property keys that have been configured
     * @return a detailed validation result indicating success or failure with specific error information
     * @see AdapterValidationResult
     * @see AdapterInfo#getRequiredProperties()
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
