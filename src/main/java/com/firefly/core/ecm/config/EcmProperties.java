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
package com.firefly.core.ecm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for ECM system.
 */
@Data
@ConfigurationProperties(prefix = "firefly.ecm")
public class EcmProperties {
    
    /**
     * Whether ECM is enabled
     */
    private Boolean enabled = true;
    
    /**
     * Adapter type to use (e.g., "s3", "azure-blob", "minio", "alfresco")
     */
    private String adapterType;
    
    /**
     * Adapter-specific properties
     */
    private Map<String, Object> properties;
    
    /**
     * Connection settings
     */
    private Connection connection = new Connection();
    
    /**
     * Feature flags
     */
    private Features features = new Features();
    
    /**
     * Default settings
     */
    private Defaults defaults = new Defaults();
    
    /**
     * Performance settings
     */
    private Performance performance = new Performance();
    
    /**
     * Connection configuration
     */
    @Data
    public static class Connection {
        private Duration connectTimeout = Duration.ofSeconds(30);
        private Duration readTimeout = Duration.ofMinutes(5);
        private Integer maxConnections = 100;
        private Integer retryAttempts = 3;
    }
    
    /**
     * Feature flags configuration
     */
    @Data
    public static class Features {
        private Boolean documentManagement = true;
        private Boolean contentStorage = true;
        private Boolean versioning = true;
        private Boolean folderManagement = true;
        private Boolean folderHierarchy = true;
        private Boolean permissions = true;
        private Boolean security = true;
        private Boolean search = true;
        private Boolean auditing = true;
        private Boolean esignature = false;
        private Boolean virusScanning = false;
        private Boolean contentExtraction = false;
    }
    
    /**
     * Default settings configuration
     */
    @Data
    public static class Defaults {
        private Long maxFileSizeMb = 100L;
        private List<String> allowedExtensions = List.of("pdf", "doc", "docx", "txt", "jpg", "png");
        private List<String> blockedExtensions = List.of("exe", "bat", "cmd", "scr");
        private String checksumAlgorithm = "SHA-256";
        private String defaultFolder = "/";
    }
    
    /**
     * Performance settings configuration
     */
    @Data
    public static class Performance {
        private Integer batchSize = 100;
        private Boolean cacheEnabled = true;
        private Duration cacheExpiration = Duration.ofMinutes(30);
        private Boolean compressionEnabled = true;
    }
    
    /**
     * Get adapter property value.
     *
     * @param key the property key
     * @return the property value, or null if not found
     */
    public Object getAdapterProperty(String key) {
        return properties != null ? properties.get(key) : null;
    }
    
    /**
     * Get adapter property value as string.
     *
     * @param key the property key
     * @return the property value as string, or null if not found
     */
    public String getAdapterPropertyAsString(String key) {
        Object value = getAdapterProperty(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Get adapter property value as integer.
     *
     * @param key the property key
     * @return the property value as integer, or null if not found or not convertible
     */
    public Integer getAdapterPropertyAsInteger(String key) {
        Object value = getAdapterProperty(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.valueOf((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Get adapter property value as boolean.
     *
     * @param key the property key
     * @return the property value as boolean, or null if not found
     */
    public Boolean getAdapterPropertyAsBoolean(String key) {
        Object value = getAdapterProperty(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.valueOf((String) value);
        }
        return null;
    }
    
    /**
     * Check if adapter property exists.
     *
     * @param key the property key
     * @return true if property exists, false otherwise
     */
    public boolean hasAdapterProperty(String key) {
        return properties != null && properties.containsKey(key);
    }
}
