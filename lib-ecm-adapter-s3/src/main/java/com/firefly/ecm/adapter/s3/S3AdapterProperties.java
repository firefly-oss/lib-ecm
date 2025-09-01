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
package com.firefly.ecm.adapter.s3;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Configuration properties for the Amazon S3 ECM adapter.
 *
 * <p>This class defines all configuration options available for the S3 adapter,
 * supporting both environment variables and properties file configuration following
 * Spring Boot externalized configuration patterns.</p>
 *
 * <p>Required properties:</p>
 * <ul>
 *   <li><strong>bucket-name:</strong> The S3 bucket name for document storage</li>
 *   <li><strong>region:</strong> AWS region where the bucket is located</li>
 * </ul>
 *
 * <p>Optional properties:</p>
 * <ul>
 *   <li><strong>access-key:</strong> AWS access key (if not using IAM roles)</li>
 *   <li><strong>secret-key:</strong> AWS secret key (if not using IAM roles)</li>
 *   <li><strong>endpoint:</strong> Custom S3 endpoint (for S3-compatible services)</li>
 *   <li><strong>path-prefix:</strong> Prefix for all object keys</li>
 *   <li><strong>enable-versioning:</strong> Enable S3 object versioning</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "firefly.ecm.adapter.s3")
public class S3AdapterProperties {

    /**
     * The S3 bucket name where documents will be stored.
     * This is a required property.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_BUCKET_NAME}</p>
     */
    @NotBlank(message = "S3 bucket name is required")
    private String bucketName;

    /**
     * AWS region where the S3 bucket is located.
     * This is a required property.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_REGION}</p>
     */
    @NotBlank(message = "AWS region is required")
    private String region;

    /**
     * AWS access key for authentication.
     * Optional if using IAM roles or instance profiles.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_ACCESS_KEY}</p>
     */
    private String accessKey;

    /**
     * AWS secret key for authentication.
     * Optional if using IAM roles or instance profiles.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_SECRET_KEY}</p>
     */
    private String secretKey;

    /**
     * Custom S3 endpoint URL.
     * Optional, used for S3-compatible services like MinIO.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_ENDPOINT}</p>
     */
    private String endpoint;

    /**
     * Path prefix for all object keys in the bucket.
     * Optional, defaults to "documents/".
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_PATH_PREFIX}</p>
     */
    private String pathPrefix = "documents/";

    /**
     * Whether to enable S3 object versioning for documents.
     * Optional, defaults to true.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_ENABLE_VERSIONING}</p>
     */
    private Boolean enableVersioning = true;

    /**
     * Whether to use path-style access for S3 requests.
     * Optional, defaults to false (virtual-hosted-style).
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_PATH_STYLE_ACCESS}</p>
     */
    private Boolean pathStyleAccess = false;

    /**
     * Connection timeout for S3 client.
     * Optional, defaults to 30 seconds.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_CONNECTION_TIMEOUT}</p>
     */
    @NotNull
    private Duration connectionTimeout = Duration.ofSeconds(30);

    /**
     * Socket timeout for S3 client.
     * Optional, defaults to 30 seconds.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_SOCKET_TIMEOUT}</p>
     */
    @NotNull
    private Duration socketTimeout = Duration.ofSeconds(30);

    /**
     * Maximum number of retry attempts for failed requests.
     * Optional, defaults to 3.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_MAX_RETRIES}</p>
     */
    private Integer maxRetries = 3;

    /**
     * Whether to enable server-side encryption for stored objects.
     * Optional, defaults to true.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_ENABLE_ENCRYPTION}</p>
     */
    private Boolean enableEncryption = true;

    /**
     * KMS key ID for server-side encryption.
     * Optional, uses default S3 encryption if not specified.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_KMS_KEY_ID}</p>
     */
    private String kmsKeyId;

    /**
     * Storage class for uploaded objects.
     * Optional, defaults to STANDARD.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_STORAGE_CLASS}</p>
     */
    private String storageClass = "STANDARD";

    /**
     * Whether to enable multipart upload for large files.
     * Optional, defaults to true.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_ENABLE_MULTIPART}</p>
     */
    private Boolean enableMultipart = true;

    /**
     * Minimum file size threshold for multipart upload.
     * Optional, defaults to 5MB.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_MULTIPART_THRESHOLD}</p>
     */
    private Long multipartThreshold = 5L * 1024 * 1024; // 5MB

    /**
     * Part size for multipart uploads.
     * Optional, defaults to 5MB.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_S3_MULTIPART_PART_SIZE}</p>
     */
    private Long multipartPartSize = 5L * 1024 * 1024; // 5MB
}
