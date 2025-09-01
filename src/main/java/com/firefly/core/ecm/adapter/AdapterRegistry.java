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
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for ECM adapters.
 * Manages adapter discovery, registration, and selection.
 */
@Slf4j
@Component
public class AdapterRegistry {
    
    private final ApplicationContext applicationContext;
    private final Map<String, List<AdapterInfo>> adaptersByType = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<AdapterInfo>> adaptersByInterface = new ConcurrentHashMap<>();
    
    @Autowired
    public AdapterRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    /**
     * Initialize the registry by discovering all ECM adapters.
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing ECM Adapter Registry");
        discoverAdapters();
        logRegisteredAdapters();
    }
    
    /**
     * Discover and register all ECM adapters in the application context.
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
     * Register an adapter with the registry.
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
     * Get adapter by type with highest priority.
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
     * Get all adapters of a specific type.
     */
    public List<AdapterInfo> getAdapters(String type) {
        return adaptersByType.getOrDefault(type, Collections.emptyList())
            .stream()
            .sorted(Comparator.comparingInt(AdapterInfo::getPriority).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Get adapter implementing a specific interface with highest priority.
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
     * Get all adapters implementing a specific interface.
     */
    public <T> List<T> getAdapters(Class<T> interfaceClass) {
        return adaptersByInterface.getOrDefault(interfaceClass, Collections.emptyList())
            .stream()
            .sorted(Comparator.comparingInt(AdapterInfo::getPriority).reversed())
            .map(info -> interfaceClass.cast(info.getAdapterBean()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get all registered adapter types.
     */
    public Set<String> getRegisteredTypes() {
        return new HashSet<>(adaptersByType.keySet());
    }
    
    /**
     * Check if an adapter type is registered.
     */
    public boolean hasAdapter(String type) {
        return adaptersByType.containsKey(type) && !adaptersByType.get(type).isEmpty();
    }
    
    /**
     * Check if an adapter implementing a specific interface is registered.
     */
    public boolean hasAdapter(Class<?> interfaceClass) {
        return adaptersByInterface.containsKey(interfaceClass) && !adaptersByInterface.get(interfaceClass).isEmpty();
    }
    
    /**
     * Get adapter information by type and priority.
     */
    public Optional<AdapterInfo> getAdapterInfo(String type) {
        return getAdapter(type);
    }
    
    /**
     * Log all registered adapters for debugging.
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
