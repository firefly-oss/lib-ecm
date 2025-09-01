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

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central registry for ECM adapter discovery, registration, and management.
 *
 * <p>This component is responsible for automatically discovering all ECM adapters
 * in the Spring application context and providing efficient access to them based
 * on type or interface requirements. It implements the registry pattern to manage
 * the collection of available adapters and their metadata.</p>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Automatic adapter discovery using Spring's component scanning</li>
 *   <li>Adapter registration with metadata extraction</li>
 *   <li>Priority-based adapter selection</li>
 *   <li>Interface-based adapter lookup</li>
 *   <li>Adapter availability checking</li>
 *   <li>Thread-safe concurrent access</li>
 * </ul>
 *
 * <p>The registry maintains two primary indexes:</p>
 * <ul>
 *   <li><strong>By Type:</strong> Adapters grouped by their type identifier (e.g., "s3", "azure-blob")</li>
 *   <li><strong>By Interface:</strong> Adapters grouped by the port interfaces they implement</li>
 * </ul>
 *
 * <p>Adapter discovery process:</p>
 * <ol>
 *   <li>Scan for beans annotated with {@link EcmAdapter}</li>
 *   <li>Extract adapter metadata from annotations</li>
 *   <li>Register adapters in both type and interface indexes</li>
 *   <li>Sort by priority for efficient selection</li>
 * </ol>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 * @see EcmAdapter
 * @see AdapterInfo
 * @see AdapterSelector
 */
@Slf4j
@Component
public class AdapterRegistry {

    /** Spring application context for bean discovery. */
    private final ApplicationContext applicationContext;

    /** Thread-safe map of adapters indexed by type identifier. */
    private final Map<String, List<AdapterInfo>> adaptersByType = new ConcurrentHashMap<>();

    /** Thread-safe map of adapters indexed by implemented interface. */
    private final Map<Class<?>, List<AdapterInfo>> adaptersByInterface = new ConcurrentHashMap<>();

    /**
     * Constructs a new AdapterRegistry with the specified application context.
     *
     * @param applicationContext the Spring application context for bean discovery
     */
    @Autowired
    public AdapterRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Initializes the registry by discovering and registering all ECM adapters.
     *
     * <p>This method is automatically called after bean construction due to the
     * {@link PostConstruct} annotation. It performs the complete adapter discovery
     * and registration process, making all adapters available for selection.</p>
     *
     * <p>The initialization process:</p>
     * <ol>
     *   <li>Discovers all beans annotated with {@link EcmAdapter}</li>
     *   <li>Registers enabled adapters in the registry</li>
     *   <li>Logs the registration results for debugging</li>
     * </ol>
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing ECM Adapter Registry");
        discoverAdapters();
        logRegisteredAdapters();
    }

    /**
     * Discovers and registers all ECM adapters in the Spring application context.
     *
     * <p>This method scans the application context for beans annotated with
     * {@link EcmAdapter} and registers each enabled adapter. Disabled adapters
     * are skipped and logged at debug level.</p>
     *
     * <p>The discovery process handles:</p>
     * <ul>
     *   <li>Annotation-based adapter identification</li>
     *   <li>Enabled/disabled state checking</li>
     *   <li>Metadata extraction from annotations</li>
     *   <li>Error handling for malformed adapters</li>
     * </ul>
     */
    private void discoverAdapters() {
        Map<String, Object> adapters = applicationContext.getBeansWithAnnotation(EcmAdapter.class);

        for (Map.Entry<String, Object> entry : adapters.entrySet()) {
            String beanName = entry.getKey();
            Object adapterBean = entry.getValue();
            EcmAdapter annotation = adapterBean.getClass().getAnnotation(EcmAdapter.class);

            if (annotation != null && annotation.enabled()) {
                registerAdapter(beanName, adapterBean, annotation);
            } else {
                log.debug("Skipping disabled adapter: {}", beanName);
            }
        }
    }
    
    /**
     * Registers an individual adapter with the registry using its metadata.
     *
     * <p>This method creates an {@link AdapterInfo} object from the adapter bean
     * and its annotation, then registers it in both the type-based and interface-based
     * indexes for efficient lookup.</p>
     *
     * <p>Registration process:</p>
     * <ol>
     *   <li>Extract metadata from the {@link EcmAdapter} annotation</li>
     *   <li>Create an AdapterInfo object with all relevant information</li>
     *   <li>Register in the type-based index using the adapter type</li>
     *   <li>Register in interface-based indexes for each implemented port interface</li>
     *   <li>Log successful registration</li>
     * </ol>
     *
     * @param beanName the Spring bean name of the adapter
     * @param adapterBean the actual adapter bean instance
     * @param annotation the EcmAdapter annotation containing metadata
     */
    private void registerAdapter(String beanName, Object adapterBean, EcmAdapter annotation) {
        AdapterInfo adapterInfo = AdapterInfo.builder()
            .beanName(beanName)
            .adapterBean(adapterBean)
            .type(annotation.type())
            .priority(annotation.priority())
            .description(annotation.description())
            .version(annotation.version())
            .vendor(annotation.vendor())
            .requiredProperties(Set.of(annotation.requiredProperties()))
            .optionalProperties(Set.of(annotation.optionalProperties()))
            .supportedFeatures(Set.of(annotation.supportedFeatures()))
            .minimumProfile(annotation.minimumProfile())
            .build();

        // Register by type
        adaptersByType.computeIfAbsent(annotation.type(), k -> new ArrayList<>()).add(adapterInfo);

        // Register by implemented interfaces
        for (Class<?> interfaceClass : adapterBean.getClass().getInterfaces()) {
            if (interfaceClass.getPackage().getName().startsWith("com.firefly.core.ecm.port")) {
                adaptersByInterface.computeIfAbsent(interfaceClass, k -> new ArrayList<>()).add(adapterInfo);
            }
        }

        log.info("Registered ECM adapter: {} (type: {}, priority: {})",
                beanName, annotation.type(), annotation.priority());
    }

    /**
     * Retrieves the highest priority adapter of the specified type.
     *
     * <p>When multiple adapters of the same type are registered, this method
     * returns the one with the highest priority value. This enables adapter
     * precedence and allows for adapter overriding in different environments.</p>
     *
     * @param type the adapter type identifier (e.g., "s3", "azure-blob")
     * @return an Optional containing the highest priority AdapterInfo, empty if none found
     * @throws IllegalArgumentException if type is null
     */
    public Optional<AdapterInfo> getAdapter(String type) {
        List<AdapterInfo> adapters = adaptersByType.get(type);
        if (adapters == null || adapters.isEmpty()) {
            return Optional.empty();
        }

        return adapters.stream()
            .max(Comparator.comparingInt(AdapterInfo::getPriority));
    }

    /**
     * Retrieves all adapters of the specified type, sorted by priority (highest first).
     *
     * <p>This method returns all registered adapters of the given type, ordered
     * by priority in descending order. This is useful for fallback scenarios
     * or when multiple adapters need to be considered.</p>
     *
     * @param type the adapter type identifier
     * @return a List of AdapterInfo objects sorted by priority (highest first), empty if none found
     * @throws IllegalArgumentException if type is null
     */
    public List<AdapterInfo> getAdapters(String type) {
        return adaptersByType.getOrDefault(type, Collections.emptyList())
            .stream()
            .sorted(Comparator.comparingInt(AdapterInfo::getPriority).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Retrieves the highest priority adapter implementing the specified interface.
     *
     * <p>This method provides interface-based adapter lookup, returning the adapter
     * with the highest priority that implements the requested interface. This is
     * the primary method used by the ECM system for adapter selection.</p>
     *
     * <p>The method performs type-safe casting and returns the adapter as the
     * requested interface type, ready for immediate use.</p>
     *
     * @param <T> the interface type
     * @param interfaceClass the interface class to find an implementation for
     * @return an Optional containing the highest priority adapter implementation, empty if none found
     * @throws IllegalArgumentException if interfaceClass is null
     * @throws ClassCastException if the adapter cannot be cast to the requested interface
     */
    public <T> Optional<T> getAdapter(Class<T> interfaceClass) {
        List<AdapterInfo> adapters = adaptersByInterface.get(interfaceClass);
        if (adapters == null || adapters.isEmpty()) {
            return Optional.empty();
        }

        return adapters.stream()
            .max(Comparator.comparingInt(AdapterInfo::getPriority))
            .map(info -> interfaceClass.cast(info.getAdapterBean()));
    }

    /**
     * Retrieves all adapters implementing the specified interface, sorted by priority.
     *
     * <p>This method returns all registered adapters that implement the requested
     * interface, ordered by priority in descending order. This is useful for
     * scenarios requiring multiple adapters or fallback mechanisms.</p>
     *
     * @param <T> the interface type
     * @param interfaceClass the interface class to find implementations for
     * @return a List of adapter implementations sorted by priority (highest first), empty if none found
     * @throws IllegalArgumentException if interfaceClass is null
     * @throws ClassCastException if any adapter cannot be cast to the requested interface
     */
    public <T> List<T> getAdapters(Class<T> interfaceClass) {
        return adaptersByInterface.getOrDefault(interfaceClass, Collections.emptyList())
            .stream()
            .sorted(Comparator.comparingInt(AdapterInfo::getPriority).reversed())
            .map(info -> interfaceClass.cast(info.getAdapterBean()))
            .collect(Collectors.toList());
    }

    /**
     * Returns a set of all registered adapter type identifiers.
     *
     * <p>This method provides a way to discover all available adapter types
     * in the system. The returned set is a copy and modifications will not
     * affect the registry.</p>
     *
     * @return a Set containing all registered adapter type identifiers
     */
    public Set<String> getRegisteredTypes() {
        return new HashSet<>(adaptersByType.keySet());
    }

    /**
     * Checks whether any adapter of the specified type is registered.
     *
     * <p>This method provides a quick way to test adapter availability without
     * retrieving the actual adapter instances. It's useful for conditional
     * logic and feature availability checks.</p>
     *
     * @param type the adapter type identifier to check
     * @return true if at least one adapter of the specified type is registered, false otherwise
     * @throws IllegalArgumentException if type is null
     */
    public boolean hasAdapter(String type) {
        return adaptersByType.containsKey(type) && !adaptersByType.get(type).isEmpty();
    }

    /**
     * Checks whether any adapter implementing the specified interface is registered.
     *
     * <p>This method provides interface-based availability checking without
     * retrieving actual adapter instances. It's useful for determining whether
     * specific ECM features are available in the current configuration.</p>
     *
     * @param interfaceClass the interface class to check for implementations
     * @return true if at least one adapter implementing the interface is registered, false otherwise
     * @throws IllegalArgumentException if interfaceClass is null
     */
    public boolean hasAdapter(Class<?> interfaceClass) {
        return adaptersByInterface.containsKey(interfaceClass) && !adaptersByInterface.get(interfaceClass).isEmpty();
    }

    /**
     * Retrieves detailed adapter information for the specified type.
     *
     * <p>This method returns the same result as {@link #getAdapter(String)} but
     * provides a more explicit name for cases where the caller specifically
     * needs the AdapterInfo metadata rather than just checking availability.</p>
     *
     * @param type the adapter type identifier
     * @return an Optional containing the AdapterInfo for the highest priority adapter of the specified type
     * @throws IllegalArgumentException if type is null
     * @see #getAdapter(String)
     */
    public Optional<AdapterInfo> getAdapterInfo(String type) {
        return getAdapter(type);
    }
    
    /**
     * Logs all registered adapters for debugging and system monitoring purposes.
     *
     * <p>This method provides comprehensive logging of the adapter registration
     * results, including adapter counts, types, priorities, and versions. The
     * logging helps with:</p>
     * <ul>
     *   <li>System startup diagnostics</li>
     *   <li>Configuration verification</li>
     *   <li>Troubleshooting adapter issues</li>
     *   <li>Monitoring adapter availability</li>
     * </ul>
     *
     * <p>If no adapters are found, a warning is logged indicating that the
     * ECM system will not be functional. Otherwise, detailed information
     * about each registered adapter is logged at info level.</p>
     */
    private void logRegisteredAdapters() {
        if (adaptersByType.isEmpty()) {
            log.warn("No ECM adapters found! The system will not be functional.");
            return;
        }

        log.info("Registered ECM adapters by type:");
        for (Map.Entry<String, List<AdapterInfo>> entry : adaptersByType.entrySet()) {
            String type = entry.getKey();
            List<AdapterInfo> adapters = entry.getValue();
            log.info("  Type '{}': {} adapter(s)", type, adapters.size());

            for (AdapterInfo adapter : adapters) {
                log.info("    - {} (priority: {}, version: {})",
                        adapter.getBeanName(), adapter.getPriority(), adapter.getVersion());
            }
        }
    }
}
