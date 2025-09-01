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
package com.firefly.ecm.adapter.docusign;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Configuration properties for the DocuSign ECM adapter.
 *
 * <p>This class defines all configuration options available for the DocuSign adapter,
 * supporting both environment variables and properties file configuration following
 * Spring Boot externalized configuration patterns.</p>
 *
 * <p>Required properties:</p>
 * <ul>
 *   <li><strong>integration-key:</strong> DocuSign integration key (client ID)</li>
 *   <li><strong>user-id:</strong> DocuSign user ID (GUID)</li>
 *   <li><strong>account-id:</strong> DocuSign account ID</li>
 *   <li><strong>private-key:</strong> RSA private key for JWT authentication</li>
 * </ul>
 *
 * <p>Optional properties:</p>
 * <ul>
 *   <li><strong>base-url:</strong> DocuSign API base URL (defaults to production)</li>
 *   <li><strong>auth-server:</strong> DocuSign authentication server URL</li>
 *   <li><strong>webhook-url:</strong> URL for DocuSign webhook notifications</li>
 *   <li><strong>webhook-secret:</strong> Secret for webhook signature validation</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "firefly.ecm.adapter.docusign")
public class DocuSignAdapterProperties {

    /**
     * DocuSign integration key (client ID) for API authentication.
     * This is a required property.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_INTEGRATION_KEY}</p>
     */
    @NotBlank(message = "DocuSign integration key is required")
    private String integrationKey;

    /**
     * DocuSign user ID (GUID) for the API user.
     * This is a required property.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_USER_ID}</p>
     */
    @NotBlank(message = "DocuSign user ID is required")
    private String userId;

    /**
     * DocuSign account ID for the organization.
     * This is a required property.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_ACCOUNT_ID}</p>
     */
    @NotBlank(message = "DocuSign account ID is required")
    private String accountId;

    /**
     * RSA private key for JWT authentication with DocuSign.
     * This is a required property.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_PRIVATE_KEY}</p>
     */
    @NotBlank(message = "DocuSign private key is required")
    private String privateKey;

    /**
     * DocuSign API base URL.
     * Optional, defaults to production environment.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_BASE_URL}</p>
     */
    private String baseUrl = "https://na3.docusign.net/restapi";

    /**
     * DocuSign authentication server URL.
     * Optional, defaults to production auth server.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_AUTH_SERVER}</p>
     */
    private String authServer = "https://account.docusign.com";

    /**
     * Webhook URL for receiving DocuSign event notifications.
     * Optional, webhooks will be disabled if not provided.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_WEBHOOK_URL}</p>
     */
    private String webhookUrl;

    /**
     * Secret key for validating webhook signatures from DocuSign.
     * Optional, but recommended for security if webhooks are enabled.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_WEBHOOK_SECRET}</p>
     */
    private String webhookSecret;

    /**
     * Whether to enable sandbox mode for testing.
     * Optional, defaults to false (production mode).
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_SANDBOX_MODE}</p>
     */
    private Boolean sandboxMode = false;

    /**
     * Connection timeout for DocuSign API calls.
     * Optional, defaults to 30 seconds.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_CONNECTION_TIMEOUT}</p>
     */
    @NotNull
    private Duration connectionTimeout = Duration.ofSeconds(30);

    /**
     * Read timeout for DocuSign API calls.
     * Optional, defaults to 60 seconds.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_READ_TIMEOUT}</p>
     */
    @NotNull
    private Duration readTimeout = Duration.ofSeconds(60);

    /**
     * Maximum number of retry attempts for failed API calls.
     * Optional, defaults to 3.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_MAX_RETRIES}</p>
     */
    private Integer maxRetries = 3;

    /**
     * JWT token expiration time in seconds.
     * Optional, defaults to 3600 seconds (1 hour).
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_JWT_EXPIRATION}</p>
     */
    private Long jwtExpiration = 3600L;

    /**
     * Whether to enable automatic envelope status polling.
     * Optional, defaults to true.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_ENABLE_POLLING}</p>
     */
    private Boolean enablePolling = true;

    /**
     * Interval for polling envelope status updates.
     * Optional, defaults to 5 minutes.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_POLLING_INTERVAL}</p>
     */
    @NotNull
    private Duration pollingInterval = Duration.ofMinutes(5);

    /**
     * Default envelope email subject template.
     * Optional, defaults to a standard subject.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_DEFAULT_EMAIL_SUBJECT}</p>
     */
    private String defaultEmailSubject = "Please sign this document";

    /**
     * Default envelope email message template.
     * Optional, defaults to a standard message.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_DEFAULT_EMAIL_MESSAGE}</p>
     */
    private String defaultEmailMessage = "Please review and sign the attached document(s).";

    /**
     * Whether to enable embedded signing by default.
     * Optional, defaults to false (remote signing).
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_ENABLE_EMBEDDED_SIGNING}</p>
     */
    private Boolean enableEmbeddedSigning = false;

    /**
     * Default return URL for embedded signing sessions.
     * Optional, required if embedded signing is enabled.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_RETURN_URL}</p>
     */
    private String returnUrl;

    /**
     * Whether to enable document retention after completion.
     * Optional, defaults to true.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_ENABLE_RETENTION}</p>
     */
    private Boolean enableRetention = true;

    /**
     * Document retention period in days.
     * Optional, defaults to 2555 days (7 years).
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_DOCUSIGN_RETENTION_DAYS}</p>
     */
    private Integer retentionDays = 2555;
}
