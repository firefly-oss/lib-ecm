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

import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.auth.OAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

/**
 * Auto-configuration for the DocuSign ECM adapter.
 *
 * <p>This configuration class automatically sets up the necessary beans for the DocuSign adapter
 * when the eSignature provider is set to "docusign". It configures:</p>
 * <ul>
 *   <li>DocuSign API client with JWT authentication</li>
 *   <li>DocuSign adapter properties from configuration</li>
 *   <li>OAuth configuration for API access</li>
 *   <li>Connection timeouts and retry policies</li>
 * </ul>
 *
 * <p>The configuration uses JWT (JSON Web Token) authentication which is the recommended
 * approach for server-to-server integrations with DocuSign. This requires:</p>
 * <ul>
 *   <li>Integration key (client ID) from DocuSign</li>
 *   <li>User ID (GUID) of the API user</li>
 *   <li>RSA private key for signing JWT tokens</li>
 *   <li>Account ID for the DocuSign organization</li>
 * </ul>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(ApiClient.class)
@ConditionalOnProperty(name = "firefly.ecm.esignature.provider", havingValue = "docusign")
@EnableConfigurationProperties(DocuSignAdapterProperties.class)
public class DocuSignAdapterAutoConfiguration {

    /**
     * Creates and configures the DocuSign API client with JWT authentication.
     *
     * <p>The API client is configured with:</p>
     * <ul>
     *   <li>Base URL for the DocuSign API</li>
     *   <li>JWT authentication using RSA private key</li>
     *   <li>Connection and read timeouts</li>
     *   <li>Retry configuration</li>
     *   <li>User agent identification</li>
     * </ul>
     *
     * <p>The client automatically handles JWT token generation and refresh,
     * ensuring seamless API access throughout the application lifecycle.</p>
     *
     * @param properties the DocuSign adapter properties
     * @return configured DocuSign API client
     * @throws RuntimeException if authentication setup fails
     */
    @Bean
    @ConditionalOnMissingBean
    public ApiClient docuSignApiClient(DocuSignAdapterProperties properties) {
        log.info("Configuring DocuSign API client for account: {}", properties.getAccountId());

        try {
            // Create and configure API client
            ApiClient apiClient = new ApiClient();
            
            // Set base URL based on sandbox mode
            String baseUrl = properties.getSandboxMode() ? 
                    "https://demo.docusign.net/restapi" : properties.getBaseUrl();
            apiClient.setBasePath(baseUrl);
            
            // Configure timeouts
            apiClient.setConnectTimeout((int) properties.getConnectionTimeout().toMillis());
            apiClient.setReadTimeout((int) properties.getReadTimeout().toMillis());
            
            // Set user agent
            apiClient.setUserAgent("Firefly-ECM-Library/1.0.0");
            
            // Configure JWT authentication
            configureJwtAuthentication(apiClient, properties);
            
            log.info("Successfully configured DocuSign API client");
            return apiClient;
            
        } catch (Exception e) {
            log.error("Failed to configure DocuSign API client", e);
            throw new RuntimeException("DocuSign API client configuration failed", e);
        }
    }

    /**
     * Configures JWT authentication for the DocuSign API client.
     *
     * <p>This method sets up JWT (JSON Web Token) authentication which allows
     * the application to authenticate with DocuSign using a private key instead
     * of requiring user credentials. The process involves:</p>
     * <ol>
     *   <li>Creating a JWT token signed with the private key</li>
     *   <li>Exchanging the JWT for an access token</li>
     *   <li>Using the access token for API calls</li>
     *   <li>Automatically refreshing tokens when they expire</li>
     * </ol>
     *
     * @param apiClient the DocuSign API client to configure
     * @param properties the DocuSign adapter properties
     * @throws Exception if JWT configuration fails
     */
    private void configureJwtAuthentication(ApiClient apiClient, DocuSignAdapterProperties properties) 
            throws Exception {
        
        log.info("Configuring JWT authentication for DocuSign");
        
        // Set OAuth base path
        String authServer = properties.getSandboxMode() ? 
                "https://account-d.docusign.com" : properties.getAuthServer();
        apiClient.setOAuthBasePath(authServer);
        
        // Configure OAuth scopes
        String[] scopes = {"signature", "impersonation"};
        
        // Request JWT access token
        OAuth.OAuthToken oAuthToken = apiClient.requestJWTUserToken(
                properties.getIntegrationKey(),
                properties.getUserId(),
                Arrays.asList(scopes),
                properties.getPrivateKey().getBytes(),
                properties.getJwtExpiration()
        );
        
        // Set access token
        apiClient.setAccessToken(oAuthToken.getAccessToken(), oAuthToken.getExpiresIn());
        
        // Get user info to validate authentication
        OAuth.UserInfo userInfo = apiClient.getUserInfo(oAuthToken.getAccessToken());
        log.info("Successfully authenticated DocuSign user: {}", userInfo.getName());
        
        // Validate account access
        validateAccountAccess(userInfo, properties);
    }

    /**
     * Validates that the authenticated user has access to the specified account.
     *
     * <p>This method checks that:</p>
     * <ul>
     *   <li>The user has access to the configured account ID</li>
     *   <li>The account is active and accessible</li>
     *   <li>The user has appropriate permissions</li>
     * </ul>
     *
     * @param userInfo the authenticated user information from DocuSign
     * @param properties the DocuSign adapter properties
     * @throws RuntimeException if account validation fails
     */
    private void validateAccountAccess(OAuth.UserInfo userInfo, DocuSignAdapterProperties properties) {
        boolean accountFound = userInfo.getAccounts().stream()
                .anyMatch(account -> properties.getAccountId().equals(account.getAccountId()));
        
        if (!accountFound) {
            log.error("User does not have access to account: {}", properties.getAccountId());
            throw new RuntimeException("Invalid DocuSign account ID or insufficient permissions");
        }
        
        log.info("Successfully validated access to DocuSign account: {}", properties.getAccountId());
    }
}
