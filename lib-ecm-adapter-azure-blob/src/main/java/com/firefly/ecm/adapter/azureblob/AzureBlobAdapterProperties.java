/*
 * Copyright (c) 2024 Firefly Software Solutions Inc.
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

package com.firefly.ecm.adapter.azureblob;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * Configuration properties for the Microsoft Azure Blob Storage ECM adapter.
 *
 * <p>This class defines all configuration options available for the Azure Blob Storage adapter,
 * supporting both environment variables and properties file configuration following
 * Spring Boot externalized configuration patterns.</p>
 *
 * <p>Required properties:</p>
 * <ul>
 *   <li><strong>account-name:</strong> The Azure Storage account name</li>
 *   <li><strong>container-name:</strong> The blob container name for document storage</li>
 * </ul>
 *
 * <p>Authentication options (one required):</p>
 * <ul>
 *   <li><strong>account-key:</strong> Azure Storage account key</li>
 *   <li><strong>connection-string:</strong> Azure Storage connection string</li>
 *   <li><strong>sas-token:</strong> Shared Access Signature token</li>
 *   <li><strong>managed-identity:</strong> Use Azure Managed Identity (default: false)</li>
 * </ul>
 *
 * <p>Optional properties:</p>
 * <ul>
 *   <li><strong>endpoint:</strong> Custom blob service endpoint</li>
 *   <li><strong>path-prefix:</strong> Prefix for all blob names</li>
 *   <li><strong>enable-versioning:</strong> Enable blob versioning</li>
 *   <li><strong>access-tier:</strong> Default access tier for blobs</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "firefly.ecm.adapter.azure-blob")
public class AzureBlobAdapterProperties {

    /**
     * The Azure Storage account name.
     * This is a required property.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_ACCOUNT_NAME}</p>
     */
    @NotBlank(message = "Azure Storage account name is required")
    private String accountName;

    /**
     * The blob container name where documents will be stored.
     * This is a required property.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_CONTAINER_NAME}</p>
     */
    @NotBlank(message = "Azure Blob container name is required")
    private String containerName;

    /**
     * Azure Storage account key for authentication.
     * Optional if using other authentication methods.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_ACCOUNT_KEY}</p>
     */
    private String accountKey;

    /**
     * Azure Storage connection string for authentication.
     * Optional if using other authentication methods.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_CONNECTION_STRING}</p>
     */
    private String connectionString;

    /**
     * Shared Access Signature (SAS) token for authentication.
     * Optional if using other authentication methods.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_SAS_TOKEN}</p>
     */
    private String sasToken;

    /**
     * Whether to use Azure Managed Identity for authentication.
     * Optional, defaults to false.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_MANAGED_IDENTITY}</p>
     */
    private Boolean managedIdentity = false;

    /**
     * Custom blob service endpoint URL.
     * Optional, used for custom Azure environments or emulators.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_ENDPOINT}</p>
     */
    private String endpoint;

    /**
     * Path prefix for all blob names in the container.
     * Optional, defaults to "documents/".
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_PATH_PREFIX}</p>
     */
    private String pathPrefix = "documents/";

    /**
     * Whether to enable blob versioning for documents.
     * Optional, defaults to true.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_ENABLE_VERSIONING}</p>
     */
    private Boolean enableVersioning = true;

    /**
     * Default access tier for stored blobs.
     * Optional, defaults to "Hot". Valid values: Hot, Cool, Archive.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_ACCESS_TIER}</p>
     */
    private String accessTier = "Hot";

    /**
     * Maximum number of retry attempts for failed operations.
     * Optional, defaults to 3.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_MAX_RETRIES}</p>
     */
    @Min(value = 0, message = "Max retries must be non-negative")
    @Max(value = 10, message = "Max retries must not exceed 10")
    private Integer maxRetries = 3;

    /**
     * Timeout in seconds for blob operations.
     * Optional, defaults to 30 seconds.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_TIMEOUT_SECONDS}</p>
     */
    @Min(value = 1, message = "Timeout must be at least 1 second")
    @Max(value = 300, message = "Timeout must not exceed 300 seconds")
    private Integer timeoutSeconds = 30;

    /**
     * Block size in bytes for blob uploads.
     * Optional, defaults to 4MB (4194304 bytes).
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_BLOCK_SIZE}</p>
     */
    @Min(value = 1024, message = "Block size must be at least 1KB")
    private Long blockSize = 4194304L; // 4MB

    /**
     * Threshold in bytes for using block blob upload vs single upload.
     * Optional, defaults to 256MB (268435456 bytes).
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_BLOCK_UPLOAD_THRESHOLD}</p>
     */
    @Min(value = 1024, message = "Block upload threshold must be at least 1KB")
    private Long blockUploadThreshold = 268435456L; // 256MB

    /**
     * Whether to enable server-side encryption for stored blobs.
     * Optional, defaults to true.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_ENABLE_ENCRYPTION}</p>
     */
    private Boolean enableEncryption = true;

    /**
     * Customer-managed encryption key URL for blob encryption.
     * Optional, used for customer-managed keys.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_ENCRYPTION_KEY_URL}</p>
     */
    private String encryptionKeyUrl;

    /**
     * Whether to enable soft delete for blobs.
     * Optional, defaults to true.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_ENABLE_SOFT_DELETE}</p>
     */
    private Boolean enableSoftDelete = true;

    /**
     * Soft delete retention period in days.
     * Optional, defaults to 7 days.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_AZURE_BLOB_SOFT_DELETE_RETENTION_DAYS}</p>
     */
    @Min(value = 1, message = "Soft delete retention must be at least 1 day")
    @Max(value = 365, message = "Soft delete retention must not exceed 365 days")
    private Integer softDeleteRetentionDays = 7;
}
