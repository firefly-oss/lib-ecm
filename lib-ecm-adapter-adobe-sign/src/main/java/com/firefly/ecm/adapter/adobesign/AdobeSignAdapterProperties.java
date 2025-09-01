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

package com.firefly.ecm.adapter.adobesign;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.Duration;

/**
 * Configuration properties for the Adobe Sign ECM adapter.
 *
 * <p>This class defines all configuration options available for the Adobe Sign adapter,
 * supporting both environment variables and properties file configuration following
 * Spring Boot externalized configuration patterns.</p>
 *
 * <p>Required properties:</p>
 * <ul>
 *   <li><strong>client-id:</strong> Adobe Sign application client ID</li>
 *   <li><strong>client-secret:</strong> Adobe Sign application client secret</li>
 *   <li><strong>refresh-token:</strong> OAuth refresh token for API access</li>
 * </ul>
 *
 * <p>Optional properties:</p>
 * <ul>
 *   <li><strong>base-url:</strong> Adobe Sign API base URL (defaults to production)</li>
 *   <li><strong>api-version:</strong> Adobe Sign API version</li>
 *   <li><strong>webhook-url:</strong> URL for Adobe Sign webhook notifications</li>
 *   <li><strong>webhook-secret:</strong> Secret for webhook signature validation</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "firefly.ecm.adapter.adobe-sign")
public class AdobeSignAdapterProperties {

    /**
     * Adobe Sign application client ID for OAuth authentication.
     * This is a required property.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_CLIENT_ID}</p>
     */
    @NotBlank(message = "Adobe Sign client ID is required")
    private String clientId;

    /**
     * Adobe Sign application client secret for OAuth authentication.
     * This is a required property.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_CLIENT_SECRET}</p>
     */
    @NotBlank(message = "Adobe Sign client secret is required")
    private String clientSecret;

    /**
     * OAuth refresh token for Adobe Sign API access.
     * This is a required property.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_REFRESH_TOKEN}</p>
     */
    @NotBlank(message = "Adobe Sign refresh token is required")
    private String refreshToken;

    /**
     * Adobe Sign API base URL.
     * Optional, defaults to production environment.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_BASE_URL}</p>
     */
    private String baseUrl = "https://api.na1.adobesign.com";

    /**
     * Adobe Sign API version.
     * Optional, defaults to v6.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_API_VERSION}</p>
     */
    private String apiVersion = "v6";

    /**
     * Webhook URL for receiving Adobe Sign event notifications.
     * Optional, webhooks will be disabled if not provided.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_WEBHOOK_URL}</p>
     */
    private String webhookUrl;

    /**
     * Webhook secret for validating Adobe Sign webhook signatures.
     * Optional, but recommended for security.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_WEBHOOK_SECRET}</p>
     */
    private String webhookSecret;

    /**
     * Connection timeout for Adobe Sign API calls.
     * Optional, defaults to 30 seconds.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_CONNECTION_TIMEOUT}</p>
     */
    private Duration connectionTimeout = Duration.ofSeconds(30);

    /**
     * Read timeout for Adobe Sign API calls.
     * Optional, defaults to 60 seconds.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_READ_TIMEOUT}</p>
     */
    private Duration readTimeout = Duration.ofSeconds(60);

    /**
     * Maximum number of retry attempts for failed API calls.
     * Optional, defaults to 3.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_MAX_RETRIES}</p>
     */
    @Min(value = 0, message = "Max retries must be non-negative")
    @Max(value = 10, message = "Max retries must not exceed 10")
    private Integer maxRetries = 3;

    /**
     * Access token expiration time in seconds.
     * Optional, defaults to 3600 seconds (1 hour).
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_TOKEN_EXPIRATION}</p>
     */
    @Min(value = 300, message = "Token expiration must be at least 5 minutes")
    @Max(value = 86400, message = "Token expiration must not exceed 24 hours")
    private Integer tokenExpiration = 3600;

    /**
     * Default email subject for signature requests.
     * Optional, defaults to a standard subject.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_DEFAULT_EMAIL_SUBJECT}</p>
     */
    private String defaultEmailSubject = "Please sign this document";

    /**
     * Default email message template for signature requests.
     * Optional, defaults to a standard message.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_DEFAULT_EMAIL_MESSAGE}</p>
     */
    private String defaultEmailMessage = "Please review and sign the attached document(s).";

    /**
     * Whether to enable embedded signing by default.
     * Optional, defaults to false (email signing).
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_ENABLE_EMBEDDED_SIGNING}</p>
     */
    private Boolean enableEmbeddedSigning = false;

    /**
     * Default return URL for embedded signing sessions.
     * Optional, required if embedded signing is enabled.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_RETURN_URL}</p>
     */
    private String returnUrl;

    /**
     * Whether to enable document retention after completion.
     * Optional, defaults to true.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_ENABLE_DOCUMENT_RETENTION}</p>
     */
    private Boolean enableDocumentRetention = true;

    /**
     * Document retention period in days.
     * Optional, defaults to 365 days.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_DOCUMENT_RETENTION_DAYS}</p>
     */
    @Min(value = 1, message = "Document retention must be at least 1 day")
    @Max(value = 3650, message = "Document retention must not exceed 10 years")
    private Integer documentRetentionDays = 365;

    /**
     * Whether to enable automatic reminders for pending signatures.
     * Optional, defaults to true.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_ENABLE_REMINDERS}</p>
     */
    private Boolean enableReminders = true;

    /**
     * Reminder frequency in days.
     * Optional, defaults to 3 days.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_REMINDER_FREQUENCY_DAYS}</p>
     */
    @Min(value = 1, message = "Reminder frequency must be at least 1 day")
    @Max(value = 30, message = "Reminder frequency must not exceed 30 days")
    private Integer reminderFrequencyDays = 3;

    /**
     * Default agreement expiration in days.
     * Optional, defaults to 30 days.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_DEFAULT_EXPIRATION_DAYS}</p>
     */
    @Min(value = 1, message = "Agreement expiration must be at least 1 day")
    @Max(value = 365, message = "Agreement expiration must not exceed 365 days")
    private Integer defaultExpirationDays = 30;

    /**
     * Whether to enable in-person signing.
     * Optional, defaults to false.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_ENABLE_IN_PERSON_SIGNING}</p>
     */
    private Boolean enableInPersonSigning = false;

    /**
     * Whether to enable written signatures.
     * Optional, defaults to true.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_ENABLE_WRITTEN_SIGNATURES}</p>
     */
    private Boolean enableWrittenSignatures = true;

    /**
     * Whether to require signer identity verification.
     * Optional, defaults to false.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_REQUIRE_IDENTITY_VERIFICATION}</p>
     */
    private Boolean requireIdentityVerification = false;

    /**
     * Identity verification method.
     * Optional, defaults to "NONE". Valid values: NONE, PHONE, KBA, WEB_IDENTITY.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_IDENTITY_VERIFICATION_METHOD}</p>
     */
    private String identityVerificationMethod = "NONE";

    /**
     * Whether to enable password protection for agreements.
     * Optional, defaults to false.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_ENABLE_PASSWORD_PROTECTION}</p>
     */
    private Boolean enablePasswordProtection = false;

    /**
     * Default password for protected agreements.
     * Optional, used only if password protection is enabled.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_DEFAULT_PASSWORD}</p>
     */
    private String defaultPassword;

    /**
     * Whether to enable audit trail generation.
     * Optional, defaults to true.
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_ENABLE_AUDIT_TRAIL}</p>
     */
    private Boolean enableAuditTrail = true;

    /**
     * Locale for agreement language.
     * Optional, defaults to "en_US".
     *
     * <p>Environment variable: {@code FIREFLY_ECM_ADAPTER_ADOBE_SIGN_LOCALE}</p>
     */
    private String locale = "en_US";
}
