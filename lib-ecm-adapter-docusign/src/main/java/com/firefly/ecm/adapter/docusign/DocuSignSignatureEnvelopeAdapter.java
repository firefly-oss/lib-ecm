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

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.model.*;
import com.firefly.core.ecm.adapter.AdapterFeature;
import com.firefly.core.ecm.adapter.EcmAdapter;
import com.firefly.core.ecm.domain.model.esignature.SignatureEnvelope;
import com.firefly.core.ecm.domain.enums.esignature.EnvelopeStatus;
import com.firefly.core.ecm.domain.enums.esignature.SignatureProvider;
import com.firefly.core.ecm.port.esignature.SignatureEnvelopePort;
import com.firefly.core.ecm.port.document.DocumentContentPort;
import com.firefly.core.ecm.port.document.DocumentPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DocuSign implementation of SignatureEnvelopePort.
 *
 * <p>This adapter provides complete envelope management capabilities using DocuSign
 * as the eSignature provider. It supports:</p>
 * <ul>
 *   <li>Envelope creation with documents and signers</li>
 *   <li>Real document integration with storage adapters</li>
 *   <li>Envelope sending and status tracking</li>
 *   <li>Embedded and remote signing workflows</li>
 *   <li>Envelope voiding and archiving</li>
 *   <li>Status synchronization with DocuSign</li>
 *   <li>Bulk operations for multiple envelopes</li>
 *   <li>Template-based envelope creation</li>
 * </ul>
 *
 * <p>The adapter maintains a mapping between ECM envelope IDs and DocuSign envelope IDs
 * to provide seamless integration with the ECM system.</p>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@EcmAdapter(
    type = "docusign",
    description = "DocuSign eSignature Envelope Adapter",
    supportedFeatures = {
        AdapterFeature.ESIGNATURE_ENVELOPES,
        AdapterFeature.ESIGNATURE_REQUESTS,
        AdapterFeature.SIGNATURE_VALIDATION
    },
    requiredProperties = {"integration-key", "user-id", "account-id", "private-key"},
    optionalProperties = {"base-url", "auth-server", "sandbox-mode", "return-url"}
)
@Component
@ConditionalOnProperty(name = "firefly.ecm.esignature.provider", havingValue = "docusign")
public class DocuSignSignatureEnvelopeAdapter implements SignatureEnvelopePort {

    private final ApiClient apiClient;
    private final EnvelopesApi envelopesApi;
    private final DocuSignAdapterProperties properties;
    private final DocumentContentPort documentContentPort;
    private final DocumentPort documentPort;

    // Cache for envelope mappings (in production, use a proper cache or database)
    private final Map<UUID, String> envelopeIdMapping = new ConcurrentHashMap<>();
    private final Map<String, UUID> externalIdMapping = new ConcurrentHashMap<>();

    public DocuSignSignatureEnvelopeAdapter(ApiClient apiClient,
                                          DocuSignAdapterProperties properties,
                                          DocumentContentPort documentContentPort,
                                          DocumentPort documentPort) {
        this.apiClient = apiClient;
        this.properties = properties;
        this.documentContentPort = documentContentPort;
        this.documentPort = documentPort;
        this.envelopesApi = new EnvelopesApi(apiClient);
        log.info("DocuSignSignatureEnvelopeAdapter initialized for account: {} with document integration",
                properties.getAccountId());
    }

    @Override
    public Mono<SignatureEnvelope> createEnvelope(SignatureEnvelope envelope) {
        return Mono.fromCallable(() -> {
            UUID envelopeId = envelope.getId() != null ? envelope.getId() : UUID.randomUUID();
            
            // Create DocuSign envelope definition
            EnvelopeDefinition envelopeDefinition = buildEnvelopeDefinition(envelope);
            
            // Create envelope in DocuSign
            EnvelopeSummary envelopeSummary = envelopesApi.createEnvelope(
                    properties.getAccountId(), 
                    envelopeDefinition
            );
            
            String docuSignEnvelopeId = envelopeSummary.getEnvelopeId();
            
            // Store mapping
            envelopeIdMapping.put(envelopeId, docuSignEnvelopeId);
            externalIdMapping.put(docuSignEnvelopeId, envelopeId);
            
            // Build and return the created envelope
            return envelope.toBuilder()
                    .id(envelopeId)
                    .externalEnvelopeId(docuSignEnvelopeId)
                    .status(EnvelopeStatus.DRAFT)
                    .createdAt(Instant.now())
                    .modifiedAt(Instant.now())
                    .build();
        })
        .doOnSuccess(env -> log.debug("Created envelope {} in DocuSign with ID {}",
                env.getId(), env.getExternalEnvelopeId()))
        .doOnError(error -> log.error("Failed to create envelope in DocuSign", error));
    }

    @Override
    public Mono<SignatureEnvelope> getEnvelope(UUID envelopeId) {
        return Mono.fromCallable(() -> {
            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }
            
            Envelope docuSignEnvelope = envelopesApi.getEnvelope(
                    properties.getAccountId(), 
                    docuSignEnvelopeId
            );
            
            return buildSignatureEnvelopeFromDocuSign(envelopeId, docuSignEnvelope);
        })
        .doOnSuccess(env -> log.debug("Retrieved envelope {} from DocuSign", envelopeId))
        .doOnError(error -> log.error("Failed to retrieve envelope {} from DocuSign", envelopeId, error));
    }

    @Override
    public Mono<SignatureEnvelope> updateEnvelope(SignatureEnvelope envelope) {
        return Mono.fromCallable(() -> {
            String docuSignEnvelopeId = envelopeIdMapping.get(envelope.getId());
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelope.getId());
            }
            
            // Update envelope in DocuSign (limited operations available after creation)
            Envelope envelopeUpdate = new Envelope();
            envelopeUpdate.setEmailSubject(envelope.getTitle());
            envelopeUpdate.setEmailBlurb(envelope.getDescription());

            envelopesApi.update(properties.getAccountId(), docuSignEnvelopeId, envelopeUpdate);

            return envelope.toBuilder()
                    .modifiedAt(Instant.now())
                    .build();
        })
        .doOnSuccess(env -> log.debug("Updated envelope {} in DocuSign", envelope.getId()))
        .doOnError(error -> log.error("Failed to update envelope {} in DocuSign", envelope.getId(), error));
    }

    @Override
    public Mono<Void> deleteEnvelope(UUID envelopeId) {
        return Mono.fromRunnable(() -> {
            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }
            
            try {
                // Void the envelope in DocuSign (cannot be deleted, only voided)
                Envelope envelopeUpdate = new Envelope();
                envelopeUpdate.setStatus("voided");

                envelopesApi.update(properties.getAccountId(), docuSignEnvelopeId, envelopeUpdate);

                // Remove from mappings
                envelopeIdMapping.remove(envelopeId);
                externalIdMapping.remove(docuSignEnvelopeId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete envelope in DocuSign", e);
            }
        })
        .doOnSuccess(v -> log.debug("Voided envelope {} in DocuSign", envelopeId))
        .doOnError(error -> log.error("Failed to void envelope {} in DocuSign", envelopeId, error))
        .then();
    }

    @Override
    public Mono<SignatureEnvelope> sendEnvelope(UUID envelopeId, UUID sentBy) {
        return Mono.fromCallable(() -> {
            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }

            // Send the envelope
            Envelope envelopeUpdate = new Envelope();
            envelopeUpdate.setStatus("sent");

            EnvelopeUpdateSummary updateSummary = envelopesApi.update(
                    properties.getAccountId(),
                    docuSignEnvelopeId,
                    envelopeUpdate
            );

            // Get updated envelope details
            return getEnvelope(envelopeId).block();
        })
        .doOnSuccess(env -> log.debug("Sent envelope {} in DocuSign by user {}", envelopeId, sentBy))
        .doOnError(error -> log.error("Failed to send envelope {} in DocuSign", envelopeId, error));
    }

    @Override
    public Mono<SignatureEnvelope> voidEnvelope(UUID envelopeId, String voidReason, UUID voidedBy) {
        return Mono.fromCallable(() -> {
            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }

            // Void the envelope in DocuSign
            Envelope envelopeUpdate = new Envelope();
            envelopeUpdate.setStatus("voided");
            envelopeUpdate.setVoidedReason(voidReason);

            envelopesApi.update(properties.getAccountId(), docuSignEnvelopeId, envelopeUpdate);

            // Get updated envelope details
            return getEnvelope(envelopeId).block();
        })
        .doOnSuccess(env -> log.debug("Voided envelope {} in DocuSign by user {} with reason: {}",
                envelopeId, voidedBy, voidReason))
        .doOnError(error -> log.error("Failed to void envelope {} in DocuSign", envelopeId, error));
    }

    @Override
    public Mono<Boolean> existsEnvelope(UUID envelopeId) {
        return Mono.fromCallable(() -> {
            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                return false;
            }
            
            try {
                envelopesApi.getEnvelope(properties.getAccountId(), docuSignEnvelopeId);
                return true;
            } catch (Exception e) {
                return false;
            }
        })
        .doOnError(error -> log.error("Error checking envelope existence for {}", envelopeId, error));
    }

    @Override
    public Flux<SignatureEnvelope> getEnvelopesByStatus(EnvelopeStatus status, Integer limit) {
        return Flux.fromIterable(() -> {
            try {
                String docuSignStatus = mapToDocuSignStatus(status);

                // Use the correct DocuSign API method signature
                EnvelopesApi.ListStatusChangesOptions options = envelopesApi.new ListStatusChangesOptions();
                if (limit != null) {
                    options.setCount(limit.toString());
                }
                options.setStatus(docuSignStatus);

                EnvelopesInformation envelopesInfo = envelopesApi.listStatusChanges(
                        properties.getAccountId(),
                        options
                );

                return envelopesInfo.getEnvelopes().stream()
                        .map(this::buildSignatureEnvelopeFromDocuSign)
                        .iterator();
            } catch (Exception e) {
                log.error("Failed to retrieve envelopes by status {} from DocuSign", status, e);
                return Collections.<SignatureEnvelope>emptyList().iterator();
            }
        })
        .doOnComplete(() -> log.debug("Retrieved envelopes with status {} from DocuSign", status))
        .doOnError(error -> log.error("Failed to retrieve envelopes by status {} from DocuSign", status, error));
    }

    @Override
    public Flux<SignatureEnvelope> getEnvelopesByCreator(UUID createdBy, Integer limit) {
        // DocuSign doesn't directly support filtering by creator, so we'll implement a basic version
        return getEnvelopesByStatus(EnvelopeStatus.DRAFT, limit)
                .filter(envelope -> createdBy.equals(envelope.getCreatedBy()))
                .doOnComplete(() -> log.debug("Retrieved envelopes created by {} from DocuSign", createdBy))
                .doOnError(error -> log.error("Failed to retrieve envelopes created by {} from DocuSign", createdBy, error));
    }

    @Override
    public Flux<SignatureEnvelope> getEnvelopesBySender(UUID sentBy, Integer limit) {
        // DocuSign doesn't directly support filtering by sender, so we'll implement a basic version
        return getEnvelopesByStatus(EnvelopeStatus.SENT, limit)
                .filter(envelope -> sentBy.equals(envelope.getSentBy()))
                .doOnComplete(() -> log.debug("Retrieved envelopes sent by {} from DocuSign", sentBy))
                .doOnError(error -> log.error("Failed to retrieve envelopes sent by {} from DocuSign", sentBy, error));
    }

    @Override
    public Flux<SignatureEnvelope> getEnvelopesByProvider(SignatureProvider provider, Integer limit) {
        // Since this is the DocuSign adapter, all envelopes are from DocuSign
        if (provider == SignatureProvider.DOCUSIGN) {
            return getEnvelopesByStatus(EnvelopeStatus.SENT, limit);
        }
        return Flux.empty();
    }

    @Override
    public Flux<SignatureEnvelope> getExpiringEnvelopes(Instant fromTime, Instant toTime) {
        return Mono.fromCallable(() -> {
            try {
                // Query envelopes within the time range and filter for those with expiration dates
                EnvelopesApi.ListStatusChangesOptions options = envelopesApi.new ListStatusChangesOptions();
                options.setFromDate(fromTime.toString());
                options.setToDate(toTime.toString());
                options.setStatus("sent,delivered"); // Only active envelopes can expire

                EnvelopesInformation envelopesInfo = envelopesApi.listStatusChanges(
                        properties.getAccountId(), options);

                if (envelopesInfo.getEnvelopes() == null) {
                    return java.util.Collections.<SignatureEnvelope>emptyList();
                }

                // Filter envelopes that have expiration dates within the specified range
                return envelopesInfo.getEnvelopes().stream()
                        .filter(env -> env.getExpireDateTime() != null)
                        .filter(env -> {
                            try {
                                Instant expireTime = Instant.parse(env.getExpireDateTime());
                                return !expireTime.isBefore(fromTime) && !expireTime.isAfter(toTime);
                            } catch (Exception e) {
                                log.warn("Failed to parse expiration date for envelope {}: {}",
                                        env.getEnvelopeId(), env.getExpireDateTime());
                                return false;
                            }
                        })
                        .map(env -> {
                            UUID internalId = externalIdMapping.get(env.getEnvelopeId());
                            return buildSignatureEnvelopeFromDocuSign(internalId, env);
                        })
                        .collect(java.util.stream.Collectors.toList());

            } catch (Exception e) {
                log.error("Failed to retrieve expiring envelopes from DocuSign", e);
                throw new RuntimeException("Failed to retrieve expiring envelopes", e);
            }
        })
        .flatMapMany(Flux::fromIterable)
        .doOnComplete(() -> log.debug("Retrieved expiring envelopes from {} to {} from DocuSign", fromTime, toTime))
        .doOnError(error -> log.error("Failed to retrieve expiring envelopes from DocuSign", error));
    }

    @Override
    public Flux<SignatureEnvelope> getCompletedEnvelopes(Instant fromTime, Instant toTime) {
        return Flux.fromIterable(() -> {
            try {
                // Use the correct DocuSign API method signature
                EnvelopesApi.ListStatusChangesOptions options = envelopesApi.new ListStatusChangesOptions();
                options.setFromDate(fromTime.toString());
                options.setToDate(toTime.toString());
                options.setStatus("completed");

                EnvelopesInformation envelopesInfo = envelopesApi.listStatusChanges(
                        properties.getAccountId(),
                        options
                );

                return envelopesInfo.getEnvelopes().stream()
                        .map(this::buildSignatureEnvelopeFromDocuSign)
                        .iterator();
            } catch (Exception e) {
                log.error("Failed to retrieve completed envelopes from DocuSign", e);
                return Collections.<SignatureEnvelope>emptyList().iterator();
            }
        })
        .doOnComplete(() -> log.debug("Retrieved completed envelopes from {} to {} from DocuSign", fromTime, toTime))
        .doOnError(error -> log.error("Failed to retrieve completed envelopes from DocuSign", error));
    }

    @Override
    public Mono<SignatureEnvelope> getEnvelopeByExternalId(String externalEnvelopeId, SignatureProvider provider) {
        if (provider != SignatureProvider.DOCUSIGN) {
            return Mono.empty();
        }

        UUID envelopeId = externalIdMapping.get(externalEnvelopeId);
        if (envelopeId != null) {
            return getEnvelope(envelopeId);
        }
        return Mono.empty();
    }

    @Override
    public Mono<SignatureEnvelope> syncEnvelopeStatus(UUID envelopeId) {
        return Mono.fromCallable(() -> {
            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }

            // Get fresh status from DocuSign
            Envelope docuSignEnvelope = envelopesApi.getEnvelope(
                    properties.getAccountId(),
                    docuSignEnvelopeId
            );

            return buildSignatureEnvelopeFromDocuSign(envelopeId, docuSignEnvelope);
        })
        .doOnSuccess(env -> log.debug("Synced envelope status for {} from DocuSign", envelopeId))
        .doOnError(error -> log.error("Failed to sync envelope status for {} from DocuSign", envelopeId, error));
    }

    @Override
    public Mono<String> getSigningUrl(UUID envelopeId, String signerEmail, String signerName, String clientUserId) {
        return Mono.fromCallable(() -> {
            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }

            // Validate required parameters
            if (signerEmail == null || signerEmail.trim().isEmpty()) {
                throw new IllegalArgumentException("Signer email is required");
            }
            if (signerName == null || signerName.trim().isEmpty()) {
                throw new IllegalArgumentException("Signer name is required");
            }
            if (clientUserId == null || clientUserId.trim().isEmpty()) {
                throw new IllegalArgumentException("Client user ID is required");
            }

            // Create recipient view request for embedded signing
            RecipientViewRequest viewRequest = new RecipientViewRequest();
            viewRequest.setReturnUrl(properties.getReturnUrl() != null ?
                    properties.getReturnUrl() : "https://www.docusign.com/defdemo-03/home.php");
            viewRequest.setAuthenticationMethod("none");
            viewRequest.setEmail(signerEmail);
            viewRequest.setUserName(signerName);
            viewRequest.setClientUserId(clientUserId);

            ViewUrl viewUrl = envelopesApi.createRecipientView(
                    properties.getAccountId(),
                    docuSignEnvelopeId,
                    viewRequest
            );

            return viewUrl.getUrl();
        })
        .doOnSuccess(url -> log.debug("Generated signing URL for envelope {} and signer {} ({})",
                envelopeId, signerName, signerEmail))
        .doOnError(error -> log.error("Failed to generate signing URL for envelope {} and signer {} ({})",
                envelopeId, signerName, signerEmail, error));
    }

    @Override
    public Mono<Void> resendEnvelope(UUID envelopeId) {
        return Mono.fromRunnable(() -> {
            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }

            try {
                // Use DocuSign's notification API to resend notifications to pending recipients
                // First, get the current envelope to identify pending recipients
                Envelope currentEnvelope = envelopesApi.getEnvelope(properties.getAccountId(), docuSignEnvelopeId);

                if (!"sent".equals(currentEnvelope.getStatus()) && !"delivered".equals(currentEnvelope.getStatus())) {
                    throw new IllegalStateException("Cannot resend envelope that is not in sent or delivered status");
                }

                // Create a notification request to resend to all pending recipients
                Notification notification = new Notification();
                notification.setUseAccountDefaults("false");
                notification.setReminders(new Reminders());
                notification.getReminders().setReminderEnabled("true");
                notification.getReminders().setReminderDelay("1"); // Send reminder immediately
                notification.getReminders().setReminderFrequency("0"); // One-time reminder

                // Update the envelope with the notification settings to trigger resend
                Envelope updateEnvelope = new Envelope();
                updateEnvelope.setNotification(notification);

                envelopesApi.update(properties.getAccountId(), docuSignEnvelopeId, updateEnvelope);

                log.info("Successfully triggered resend for envelope {} in DocuSign", envelopeId);

            } catch (Exception e) {
                log.error("Failed to resend envelope {} in DocuSign", envelopeId, e);
                throw new RuntimeException("Failed to resend envelope", e);
            }
        })
        .doOnSuccess(v -> log.debug("Resent envelope {} in DocuSign", envelopeId))
        .doOnError(error -> log.error("Failed to resend envelope {} in DocuSign", envelopeId, error))
        .then();
    }

    @Override
    public Mono<SignatureEnvelope> archiveEnvelope(UUID envelopeId) {
        return Mono.fromCallable(() -> {
            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }

            try {
                // DocuSign doesn't have a direct archive API, but we can use custom fields
                // to mark the envelope as archived
                CustomFields customFields = new CustomFields();
                List<TextCustomField> textCustomFields = new ArrayList<>();

                TextCustomField archivedField = new TextCustomField();
                archivedField.setName("archived");
                archivedField.setValue("true");
                archivedField.setShow("false"); // Don't show to signers
                textCustomFields.add(archivedField);

                TextCustomField archivedDateField = new TextCustomField();
                archivedDateField.setName("archived_date");
                archivedDateField.setValue(Instant.now().toString());
                archivedDateField.setShow("false");
                textCustomFields.add(archivedDateField);

                customFields.setTextCustomFields(textCustomFields);

                // Update the envelope with archive metadata
                Envelope updateEnvelope = new Envelope();
                updateEnvelope.setCustomFields(customFields);

                envelopesApi.update(properties.getAccountId(), docuSignEnvelopeId, updateEnvelope);

                log.info("Successfully marked envelope {} as archived in DocuSign", envelopeId);

                // Return the updated envelope
                return getEnvelope(envelopeId).block();

            } catch (Exception e) {
                log.error("Failed to archive envelope {} in DocuSign", envelopeId, e);
                throw new RuntimeException("Failed to archive envelope", e);
            }
        })
        .doOnSuccess(env -> log.debug("Archived envelope {} in DocuSign", envelopeId))
        .doOnError(error -> log.error("Failed to archive envelope {} in DocuSign", envelopeId, error));
    }

    /**
     * Creates multiple envelopes in a single batch operation.
     *
     * @param envelopes list of envelopes to create
     * @return Flux of created envelopes
     */
    public Flux<SignatureEnvelope> createEnvelopesBatch(java.util.List<SignatureEnvelope> envelopes) {
        return Flux.fromIterable(envelopes)
                .flatMap(this::createEnvelope, 5) // Process 5 envelopes concurrently
                .doOnComplete(() -> log.debug("Completed batch creation of {} envelopes", envelopes.size()))
                .doOnError(error -> log.error("Failed to create envelopes in batch", error));
    }

    /**
     * Sends multiple envelopes in a single batch operation.
     *
     * @param envelopeIds list of envelope IDs to send
     * @param sentBy the user sending the envelopes
     * @return Flux of sent envelopes
     */
    public Flux<SignatureEnvelope> sendEnvelopesBatch(java.util.List<UUID> envelopeIds, UUID sentBy) {
        return Flux.fromIterable(envelopeIds)
                .flatMap(envelopeId -> sendEnvelope(envelopeId, sentBy), 3) // Process 3 envelopes concurrently
                .doOnComplete(() -> log.debug("Completed batch sending of {} envelopes", envelopeIds.size()))
                .doOnError(error -> log.error("Failed to send envelopes in batch", error));
    }



    /**
     * Creates an envelope from a template.
     *
     * @param templateId the DocuSign template ID
     * @param templateRoles the roles and signers for the template
     * @return Mono containing the created envelope
     */
    public Mono<SignatureEnvelope> createEnvelopeFromTemplate(String templateId,
                                                             java.util.List<TemplateRole> templateRoles) {
        return Mono.fromCallable(() -> {
            UUID envelopeId = UUID.randomUUID();

            EnvelopeDefinition envelopeDefinition = new EnvelopeDefinition();
            envelopeDefinition.setTemplateId(templateId);
            envelopeDefinition.setTemplateRoles(templateRoles);
            envelopeDefinition.setStatus("created");

            EnvelopeSummary envelopeSummary = envelopesApi.createEnvelope(
                    properties.getAccountId(),
                    envelopeDefinition
            );

            // Store mapping
            envelopeIdMapping.put(envelopeId, envelopeSummary.getEnvelopeId());
            externalIdMapping.put(envelopeSummary.getEnvelopeId(), envelopeId);

            return SignatureEnvelope.builder()
                    .id(envelopeId)
                    .externalEnvelopeId(envelopeSummary.getEnvelopeId())
                    .status(com.firefly.core.ecm.domain.enums.esignature.EnvelopeStatus.DRAFT)
                    .provider(SignatureProvider.DOCUSIGN)
                    .createdAt(Instant.now())
                    .build();
        })
        .doOnSuccess(env -> log.debug("Created envelope {} from template {} in DocuSign",
                env.getId(), templateId))
        .doOnError(error -> log.error("Failed to create envelope from template {} in DocuSign",
                templateId, error));
    }

    /**
     * Gets the signing ceremony URL for embedded signing.
     *
     * @param envelopeId the envelope ID
     * @param signerEmail the signer's email
     * @param returnUrl the URL to return to after signing
     * @return Mono containing the embedded signing URL
     */
    public Mono<String> getEmbeddedSigningUrl(UUID envelopeId, String signerEmail, String returnUrl) {
        return Mono.fromCallable(() -> {
            String docuSignEnvelopeId = envelopeIdMapping.get(envelopeId);
            if (docuSignEnvelopeId == null) {
                throw new RuntimeException("Envelope not found: " + envelopeId);
            }

            RecipientViewRequest viewRequest = new RecipientViewRequest();
            viewRequest.setReturnUrl(returnUrl != null ? returnUrl : properties.getReturnUrl());
            viewRequest.setAuthenticationMethod("none");
            viewRequest.setEmail(signerEmail);
            viewRequest.setUserName("Signer");
            viewRequest.setClientUserId("embedded_signer_" + System.currentTimeMillis());

            ViewUrl viewUrl = envelopesApi.createRecipientView(
                    properties.getAccountId(),
                    docuSignEnvelopeId,
                    viewRequest
            );

            return viewUrl.getUrl();
        })
        .doOnSuccess(url -> log.debug("Generated embedded signing URL for envelope {} and signer {}",
                envelopeId, signerEmail))
        .doOnError(error -> log.error("Failed to generate embedded signing URL for envelope {} and signer {}",
                envelopeId, signerEmail, error));
    }

    public String getAdapterName() {
        return "DocuSignSignatureEnvelopeAdapter";
    }

    /**
     * Get envelope ID mapping for testing purposes.
     *
     * @return the envelope ID mapping
     */
    public Map<UUID, String> getEnvelopeIdMapping() {
        return envelopeIdMapping;
    }

    // Helper methods

    private EnvelopeDefinition buildEnvelopeDefinition(SignatureEnvelope envelope) {
        EnvelopeDefinition envelopeDefinition = new EnvelopeDefinition();
        envelopeDefinition.setEmailSubject(envelope.getTitle() != null ?
                envelope.getTitle() : properties.getDefaultEmailSubject());
        envelopeDefinition.setEmailBlurb(envelope.getDescription() != null ?
                envelope.getDescription() : properties.getDefaultEmailMessage());
        envelopeDefinition.setStatus("created"); // Start as draft

        // Add documents - fetch real document content from storage
        List<Document> documents = new ArrayList<>();
        if (envelope.getDocumentIds() != null) {
            for (int i = 0; i < envelope.getDocumentIds().size(); i++) {
                UUID documentId = envelope.getDocumentIds().get(i);
                try {
                    // Fetch document metadata
                    com.firefly.core.ecm.domain.model.document.Document documentMetadata =
                            documentPort.getDocument(documentId).block();

                    if (documentMetadata == null) {
                        log.error("Document not found: {}", documentId);
                        throw new RuntimeException("Document not found: " + documentId);
                    }

                    // Fetch document content
                    byte[] documentContent = documentContentPort.getContent(documentId).block();

                    if (documentContent == null || documentContent.length == 0) {
                        log.error("Document content is empty: {}", documentId);
                        throw new RuntimeException("Document content is empty: " + documentId);
                    }

                    // Convert to Base64 for DocuSign
                    String base64Content = java.util.Base64.getEncoder().encodeToString(documentContent);

                    // Create DocuSign document
                    Document doc = new Document();
                    doc.setDocumentId(String.valueOf(i + 1));
                    doc.setName(documentMetadata.getName());
                    doc.setDocumentBase64(base64Content);

                    // Set file extension if available
                    if (documentMetadata.getName().contains(".")) {
                        String extension = documentMetadata.getName().substring(
                                documentMetadata.getName().lastIndexOf("."));
                        doc.setFileExtension(extension);
                    }

                    documents.add(doc);
                    log.debug("Added document {} ({}) to DocuSign envelope",
                            documentMetadata.getName(), documentId);

                } catch (Exception e) {
                    log.error("Failed to retrieve document {} for DocuSign envelope", documentId, e);
                    throw new RuntimeException("Failed to retrieve document for envelope: " + documentId, e);
                }
            }
        }
        envelopeDefinition.setDocuments(documents);

        // Add recipients from signature requests with full configuration
        Recipients recipients = new Recipients();
        List<Signer> signers = new ArrayList<>();
        if (envelope.getSignatureRequests() != null) {
            for (int i = 0; i < envelope.getSignatureRequests().size(); i++) {
                var signatureRequest = envelope.getSignatureRequests().get(i);
                Signer docuSignSigner = new Signer();
                docuSignSigner.setRecipientId(String.valueOf(i + 1));
                docuSignSigner.setName(signatureRequest.getSignerName());
                docuSignSigner.setEmail(signatureRequest.getSignerEmail());

                // Use actual signing order from signature request, fallback to sequence
                if (signatureRequest.getSigningOrder() != null) {
                    docuSignSigner.setRoutingOrder(signatureRequest.getSigningOrder().toString());
                } else {
                    docuSignSigner.setRoutingOrder(String.valueOf(i + 1));
                }

                // Set client user ID for embedded signing if available
                if (signatureRequest.getExternalSignerId() != null) {
                    docuSignSigner.setClientUserId(signatureRequest.getExternalSignerId());
                }

                // Add role information if available
                if (signatureRequest.getSignerRole() != null) {
                    docuSignSigner.setRoleName(signatureRequest.getSignerRole());
                }

                // Configure authentication if required
                if (signatureRequest.getAuthMethod() != null &&
                    signatureRequest.getAuthMethod() != com.firefly.core.ecm.domain.enums.esignature.AuthenticationMethod.NONE) {
                    RecipientPhoneAuthentication phoneAuth = new RecipientPhoneAuthentication();
                    phoneAuth.setRecipMayProvideNumber("true");
                    docuSignSigner.setPhoneAuthentication(phoneAuth);
                }

                signers.add(docuSignSigner);
            }
        }
        recipients.setSigners(signers);
        envelopeDefinition.setRecipients(recipients);

        return envelopeDefinition;
    }

    private SignatureEnvelope buildSignatureEnvelopeFromDocuSign(UUID envelopeId, Envelope docuSignEnvelope) {
        return SignatureEnvelope.builder()
                .id(envelopeId)
                .externalEnvelopeId(docuSignEnvelope.getEnvelopeId())
                .title(docuSignEnvelope.getEmailSubject())
                .description(docuSignEnvelope.getEmailBlurb())
                .status(mapFromDocuSignStatus(docuSignEnvelope.getStatus()))
                .createdAt(parseDocuSignDateTime(docuSignEnvelope.getCreatedDateTime()))
                .modifiedAt(parseDocuSignDateTime(docuSignEnvelope.getStatusChangedDateTime()))
                .build();
    }

    private SignatureEnvelope buildSignatureEnvelopeFromDocuSign(Envelope docuSignEnvelope) {
        UUID envelopeId = externalIdMapping.get(docuSignEnvelope.getEnvelopeId());
        if (envelopeId == null) {
            envelopeId = UUID.randomUUID(); // Generate new ID for unknown envelopes
            envelopeIdMapping.put(envelopeId, docuSignEnvelope.getEnvelopeId());
            externalIdMapping.put(docuSignEnvelope.getEnvelopeId(), envelopeId);
        }
        return buildSignatureEnvelopeFromDocuSign(envelopeId, docuSignEnvelope);
    }

    private String mapToDocuSignStatus(EnvelopeStatus status) {
        switch (status) {
            case DRAFT: return "created";
            case SENT: return "sent";
            case IN_PROGRESS: return "delivered";
            case COMPLETED: return "completed";
            case DECLINED: return "declined";
            case VOIDED: return "voided";
            default: return "sent";
        }
    }

    private EnvelopeStatus mapFromDocuSignStatus(String docuSignStatus) {
        switch (docuSignStatus.toLowerCase()) {
            case "created": return EnvelopeStatus.DRAFT;
            case "sent": return EnvelopeStatus.SENT;
            case "delivered": return EnvelopeStatus.IN_PROGRESS;
            case "completed": return EnvelopeStatus.COMPLETED;
            case "declined": return EnvelopeStatus.DECLINED;
            case "voided": return EnvelopeStatus.VOIDED;
            default: return EnvelopeStatus.SENT;
        }
    }

    private Instant parseDocuSignDateTime(String dateTimeString) {
        if (dateTimeString == null) {
            return Instant.now();
        }
        try {
            return LocalDateTime.parse(dateTimeString.substring(0, 19)).toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            log.warn("Failed to parse DocuSign datetime: {}", dateTimeString);
            return Instant.now();
        }
    }
}
