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

import com.firefly.core.ecm.adapter.AdapterFeature;
import com.firefly.core.ecm.adapter.AdapterRegistry;
import com.firefly.core.ecm.adapter.AdapterSelector;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for the Firefly ECM (Enterprise Content Management) system.
 *
 * <p>This class provides comprehensive configuration options for the ECM library,
 * including adapter selection, connection settings, feature flags, default values,
 * and performance tuning parameters. All properties are bound from the application
 * configuration using the prefix "firefly.ecm".</p>
 *
 * <p>Example configuration in application.yml:</p>
 * <pre>
 * firefly:
 *   ecm:
 *     enabled: true
 *     adapter-type: s3
 *     properties:
 *       bucket-name: my-documents
 *       region: us-east-1
 *     defaults:
 *       max-file-size-mb: 100
 *       allowed-extensions: [pdf, doc, docx]
 * </pre>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 * @see EcmAutoConfiguration
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 */
@Data
@ConfigurationProperties(prefix = "firefly.ecm")
public class EcmProperties {

    /**
     * Enables or disables the ECM system functionality.
     *
     * <p>When set to {@code false}, the ECM auto-configuration will be skipped
     * and no ECM beans will be created. This is useful for disabling ECM
     * functionality in certain environments or profiles.</p>
     *
     * @defaultValue true
     */
    private Boolean enabled = true;

    /**
     * Specifies the adapter type to use for ECM operations.
     *
     * <p>This property determines which adapter implementation will be selected
     * for handling ECM operations. Currently available values:</p>
     * <ul>
     *   <li>"s3" - Amazon S3 adapter ✅ Available</li>
     * </ul>
     *
     * <p>For eSignature operations, use the esignature.provider property:</p>
     * <ul>
     *   <li>"docusign" - DocuSign eSignature adapter ✅ Available</li>
     * </ul>
     *
     * <p>Planned adapters (not yet implemented):</p>
     * <ul>
     *   <li>"azure-blob" - Azure Blob Storage adapter (planned)</li>
     *   <li>"minio" - MinIO adapter (planned)</li>
     *   <li>"alfresco" - Alfresco adapter (planned)</li>
     * </ul>
     *
     * <p>The adapter must be available on the classpath and properly configured
     * with the required properties.</p>
     *
     * @see AdapterSelector
     * @see AdapterRegistry
     */
    private String adapterType;

    /**
     * Adapter-specific configuration properties.
     *
     * <p>This map contains key-value pairs of configuration properties that are
     * specific to the selected adapter. The structure and required properties
     * depend on the adapter type being used.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>S3 adapter: bucket-name, region, access-key-id, secret-access-key</li>
     *   <li>Azure adapter: account-name, account-key, container-name</li>
     *   <li>Alfresco adapter: base-url, username, password</li>
     * </ul>
     *
     * @see #getAdapterProperty(String)
     * @see #hasAdapterProperty(String)
     */
    private Map<String, Object> properties;

    /**
     * Connection-related configuration settings.
     *
     * <p>Contains timeout values, connection pool settings, and retry configuration
     * that apply to the underlying adapter connections.</p>
     *
     * @see Connection
     */
    private Connection connection = new Connection();

    /**
     * Feature flags controlling which ECM capabilities are enabled.
     *
     * <p>Allows fine-grained control over which features are available in the
     * ECM system. Features can be enabled or disabled based on requirements
     * and adapter capabilities.</p>
     *
     * @see Features
     * @see AdapterFeature
     */
    private Features features = new Features();

    /**
     * Default values and constraints for ECM operations.
     *
     * <p>Defines system-wide defaults such as file size limits, allowed file
     * extensions, and default folder locations.</p>
     *
     * @see Defaults
     */
    private Defaults defaults = new Defaults();

    /**
     * Performance optimization settings.
     *
     * <p>Contains configuration for caching, compression, batch processing,
     * and other performance-related features.</p>
     *
     * @see Performance
     */
    private Performance performance = new Performance();
    
    /**
     * Connection configuration settings for ECM adapter connections.
     *
     * <p>This class defines timeout values, connection pool limits, and retry
     * behavior for connections to the underlying ECM storage systems. These
     * settings help ensure reliable communication with external services.</p>
     *
     * <p>Example configuration:</p>
     * <pre>
     * firefly:
     *   ecm:
     *     connection:
     *       connect-timeout: PT30S
     *       read-timeout: PT5M
     *       max-connections: 100
     *       retry-attempts: 3
     * </pre>
     *
     * @since 1.0
     */
    @Data
    public static class Connection {

        /**
         * Maximum time to wait when establishing a connection to the ECM service.
         *
         * <p>This timeout applies to the initial connection establishment phase.
         * If the connection cannot be established within this time, the operation
         * will fail with a timeout exception.</p>
         *
         * @defaultValue PT30S (30 seconds)
         */
        private Duration connectTimeout = Duration.ofSeconds(30);

        /**
         * Maximum time to wait for data to be received from the ECM service.
         *
         * <p>This timeout applies to read operations after a connection has been
         * established. It's particularly important for large file uploads/downloads
         * where data transfer may take considerable time.</p>
         *
         * @defaultValue PT5M (5 minutes)
         */
        private Duration readTimeout = Duration.ofMinutes(5);

        /**
         * Maximum number of concurrent connections to maintain in the connection pool.
         *
         * <p>This setting controls the size of the connection pool used by the
         * underlying HTTP client. Higher values allow more concurrent operations
         * but consume more system resources.</p>
         *
         * @defaultValue 100
         */
        private Integer maxConnections = 100;

        /**
         * Number of retry attempts for failed operations.
         *
         * <p>When an operation fails due to transient errors (network issues,
         * temporary service unavailability), the system will retry the operation
         * up to this many times before giving up.</p>
         *
         * @defaultValue 3
         */
        private Integer retryAttempts = 3;
    }
    
    /**
     * Feature flags configuration for controlling ECM capabilities.
     *
     * <p>This class provides fine-grained control over which ECM features are
     * enabled in the system. Features can be disabled to reduce complexity,
     * improve performance, or when the underlying adapter doesn't support them.</p>
     *
     * <p>Example configuration:</p>
     * <pre>
     * firefly:
     *   ecm:
     *     features:
     *       document-management: true
     *       versioning: true
     *       esignature: false
     *       virus-scanning: false
     * </pre>
     *
     * @since 1.0
     * @see AdapterFeature
     */
    @Data
    public static class Features {

        /** Enables basic document CRUD operations. @defaultValue true */
        private Boolean documentManagement = true;

        /** Enables document content storage and retrieval. @defaultValue true */
        private Boolean contentStorage = true;

        /** Enables document versioning capabilities. @defaultValue true */
        private Boolean versioning = true;

        /** Enables folder management operations. @defaultValue true */
        private Boolean folderManagement = true;

        /** Enables hierarchical folder structure support. @defaultValue true */
        private Boolean folderHierarchy = true;

        /** Enables permission and access control features. @defaultValue true */
        private Boolean permissions = true;

        /** Enables security-related features. @defaultValue true */
        private Boolean security = true;

        /** Enables search capabilities. @defaultValue true */
        private Boolean search = true;

        /** Enables audit trail logging. @defaultValue true */
        private Boolean auditing = true;

        /** Enables eSignature functionality. @defaultValue false */
        private Boolean esignature = false;

        /** Enables virus scanning for uploaded files. @defaultValue false */
        private Boolean virusScanning = false;

        /** Enables content extraction and indexing. @defaultValue false */
        private Boolean contentExtraction = false;

        /** Enables Intelligent Document Processing (IDP) capabilities. @defaultValue false */
        private Boolean idp = false;
    }
    
    /**
     * Default settings and constraints for ECM operations.
     *
     * <p>This class defines system-wide default values and constraints that apply
     * to document uploads, file validation, and folder operations. These settings
     * help ensure security and consistency across the ECM system.</p>
     *
     * <p>Example configuration:</p>
     * <pre>
     * firefly:
     *   ecm:
     *     defaults:
     *       max-file-size-mb: 100
     *       allowed-extensions: [pdf, doc, docx, txt, jpg, png]
     *       blocked-extensions: [exe, bat, cmd, scr]
     *       checksum-algorithm: SHA-256
     *       default-folder: "/"
     * </pre>
     *
     * @since 1.0
     */
    @Data
    public static class Defaults {

        /**
         * Maximum allowed file size for document uploads in megabytes.
         *
         * <p>This setting enforces a global limit on the size of files that can be
         * uploaded to the ECM system. Files exceeding this limit will be rejected
         * during upload validation. The limit helps prevent system abuse and
         * ensures reasonable storage usage.</p>
         *
         * <p>Note: Individual adapters may have their own size limits that could
         * be lower than this setting. The effective limit will be the minimum of
         * this setting and the adapter's limit.</p>
         *
         * @defaultValue 100L (100 megabytes)
         */
        private Long maxFileSizeMb = 100L;

        /**
         * List of file extensions that are allowed for upload.
         *
         * <p>Only files with extensions in this list will be accepted for upload.
         * Extensions should be specified without the leading dot (e.g., "pdf", not ".pdf").
         * The comparison is case-insensitive.</p>
         *
         * @defaultValue ["pdf", "doc", "docx", "txt", "jpg", "png"]
         */
        private List<String> allowedExtensions = List.of("pdf", "doc", "docx", "txt", "jpg", "png");

        /**
         * List of file extensions that are explicitly blocked from upload.
         *
         * <p>Files with extensions in this list will be rejected even if they
         * appear in the allowed extensions list. This provides an additional
         * security layer to prevent potentially dangerous file types.</p>
         *
         * @defaultValue ["exe", "bat", "cmd", "scr"]
         */
        private List<String> blockedExtensions = List.of("exe", "bat", "cmd", "scr");

        /**
         * Algorithm used for calculating file checksums.
         *
         * <p>Checksums are used for file integrity verification and duplicate
         * detection. Common algorithms include SHA-256, SHA-1, and MD5.
         * SHA-256 is recommended for security reasons.</p>
         *
         * @defaultValue "SHA-256"
         */
        private String checksumAlgorithm = "SHA-256";

        /**
         * Default folder path for document uploads when no folder is specified.
         *
         * <p>When documents are uploaded without specifying a target folder,
         * they will be placed in this default location. The path should use
         * forward slashes as separators.</p>
         *
         * @defaultValue "/"
         */
        private String defaultFolder = "/";
    }
    
    /**
     * Performance optimization settings for ECM operations.
     *
     * <p>This class contains configuration options that affect the performance
     * characteristics of the ECM system, including caching, compression, and
     * batch processing settings.</p>
     *
     * <p>Example configuration:</p>
     * <pre>
     * firefly:
     *   ecm:
     *     performance:
     *       batch-size: 100
     *       cache-enabled: true
     *       cache-expiration: PT30M
     *       compression-enabled: true
     * </pre>
     *
     * @since 1.0
     */
    @Data
    public static class Performance {

        /**
         * Number of items to process in a single batch operation.
         *
         * <p>This setting controls the batch size for bulk operations such as
         * multiple document uploads, batch deletions, or bulk metadata updates.
         * Larger batch sizes can improve throughput but may increase memory usage
         * and the risk of timeout errors.</p>
         *
         * @defaultValue 100
         */
        private Integer batchSize = 100;

        /**
         * Enables or disables caching for ECM operations.
         *
         * <p>When enabled, frequently accessed data such as document metadata,
         * folder structures, and permission information will be cached to improve
         * response times. Caching is particularly beneficial for read-heavy workloads.</p>
         *
         * @defaultValue true
         */
        private Boolean cacheEnabled = true;

        /**
         * Duration after which cached items expire and are removed from the cache.
         *
         * <p>This setting controls how long items remain in the cache before being
         * considered stale and requiring refresh from the underlying storage.
         * Shorter expiration times ensure fresher data but may reduce cache effectiveness.</p>
         *
         * @defaultValue PT30M (30 minutes)
         */
        private Duration cacheExpiration = Duration.ofMinutes(30);

        /**
         * Enables or disables compression for data transfer operations.
         *
         * <p>When enabled, document content and metadata will be compressed during
         * transfer to reduce bandwidth usage and improve transfer speeds, especially
         * for large files or slow network connections.</p>
         *
         * @defaultValue true
         */
        private Boolean compressionEnabled = true;
    }
    
    /**
     * Retrieves an adapter-specific property value by key.
     *
     * <p>This method provides access to the raw property values stored in the
     * adapter properties map. The returned value may be of any type depending
     * on how the property was configured.</p>
     *
     * @param key the property key to look up, must not be null
     * @return the property value as an Object, or {@code null} if the key is not found
     *         or if the properties map is null
     * @throws NullPointerException if key is null
     * @see #getAdapterPropertyAsString(String)
     * @see #getAdapterPropertyAsInteger(String)
     * @see #getAdapterPropertyAsBoolean(String)
     */
    public Object getAdapterProperty(String key) {
        return properties != null ? properties.get(key) : null;
    }

    /**
     * Retrieves an adapter property value as a String.
     *
     * <p>This method converts the property value to a String using the
     * {@code toString()} method. This is useful for properties that may be
     * stored as different types but need to be used as strings.</p>
     *
     * @param key the property key to look up, must not be null
     * @return the property value as a String, or {@code null} if the key is not found
     *         or if the value is null
     * @throws NullPointerException if key is null
     */
    public String getAdapterPropertyAsString(String key) {
        Object value = getAdapterProperty(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Retrieves an adapter property value as an Integer.
     *
     * <p>This method attempts to convert the property value to an Integer.
     * If the value is already an Integer, it is returned directly. If the value
     * is a String, it attempts to parse it as an Integer. For any other type
     * or if parsing fails, {@code null} is returned.</p>
     *
     * @param key the property key to look up, must not be null
     * @return the property value as an Integer, or {@code null} if the key is not found,
     *         the value cannot be converted to an Integer, or if parsing fails
     * @throws NullPointerException if key is null
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
     * Retrieves an adapter property value as a Boolean.
     *
     * <p>This method attempts to convert the property value to a Boolean.
     * If the value is already a Boolean, it is returned directly. If the value
     * is a String, it uses {@code Boolean.valueOf()} to parse it. For any other
     * type, {@code null} is returned.</p>
     *
     * @param key the property key to look up, must not be null
     * @return the property value as a Boolean, or {@code null} if the key is not found
     *         or the value is not a Boolean or String
     * @throws NullPointerException if key is null
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
     * Checks whether an adapter property with the specified key exists.
     *
     * <p>This method returns {@code true} if the properties map contains the
     * specified key, regardless of whether the associated value is null.
     * It returns {@code false} if the key is not present or if the properties
     * map itself is null.</p>
     *
     * @param key the property key to check for, must not be null
     * @return {@code true} if the property exists, {@code false} otherwise
     * @throws NullPointerException if key is null
     */
    public boolean hasAdapterProperty(String key) {
        return properties != null && properties.containsKey(key);
    }
}
