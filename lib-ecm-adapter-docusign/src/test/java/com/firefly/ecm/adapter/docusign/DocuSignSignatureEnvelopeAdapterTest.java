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
import com.firefly.core.ecm.domain.model.esignature.SignatureEnvelope;
import com.firefly.core.ecm.domain.enums.esignature.EnvelopeStatus;
import com.firefly.core.ecm.domain.enums.esignature.SignatureProvider;
import com.firefly.core.ecm.port.document.DocumentContentPort;
import com.firefly.core.ecm.port.document.DocumentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocuSignSignatureEnvelopeAdapter.
 * 
 * <p>These tests use mocked DocuSign API client to verify adapter behavior
 * without requiring actual DocuSign infrastructure. Tests cover all envelope
 * operations, error scenarios, and edge cases.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocuSignSignatureEnvelopeAdapterTest {

    // Don't mock ApiClient directly as it cannot be mocked by Mockito
    private ApiClient apiClient;

    @Mock
    private EnvelopesApi envelopesApi;

    @Mock
    private DocuSignAdapterProperties properties;

    @Mock
    private DocumentContentPort documentContentPort;

    @Mock
    private DocumentPort documentPort;

    private DocuSignSignatureEnvelopeAdapter adapter;

    private static final String TEST_ACCOUNT_ID = "test-account-id";
    private static final String TEST_DOCUSIGN_ENVELOPE_ID = "docusign-envelope-123";
    private static final UUID TEST_ENVELOPE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(properties.getAccountId()).thenReturn(TEST_ACCOUNT_ID);
        when(properties.getIntegrationKey()).thenReturn("test-integration-key");
        when(properties.getUserId()).thenReturn("test-user-id");
        when(properties.getPrivateKey()).thenReturn("test-private-key");
        when(properties.getBaseUrl()).thenReturn("https://demo.docusign.net/restapi");
        when(properties.getReturnUrl()).thenReturn("https://example.com/return");

        // Create a real ApiClient instance for testing (cannot be mocked)
        apiClient = new ApiClient();
        apiClient.setBasePath("https://demo.docusign.net/restapi");

        adapter = new DocuSignSignatureEnvelopeAdapter(apiClient, properties, documentContentPort, documentPort);
        
        // Use reflection to set the mocked envelopesApi
        try {
            java.lang.reflect.Field field = DocuSignSignatureEnvelopeAdapter.class.getDeclaredField("envelopesApi");
            field.setAccessible(true);
            field.set(adapter, envelopesApi);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock", e);
        }
    }

    @Test
    void createEnvelope_ShouldCreateEnvelopeSuccessfully() throws Exception {
        // Given
        SignatureEnvelope envelope = SignatureEnvelope.builder()
                .title("Test Envelope")
                .description("Test Description")
                .provider(SignatureProvider.DOCUSIGN)
                .status(EnvelopeStatus.DRAFT)
                .createdBy(UUID.randomUUID())
                .build();

        EnvelopeSummary envelopeSummary = new EnvelopeSummary();
        envelopeSummary.setEnvelopeId(TEST_DOCUSIGN_ENVELOPE_ID);
        envelopeSummary.setStatus("created");

        when(envelopesApi.createEnvelope(eq(TEST_ACCOUNT_ID), any(EnvelopeDefinition.class)))
                .thenReturn(envelopeSummary);

        // When & Then
        StepVerifier.create(adapter.createEnvelope(envelope))
                .expectNextMatches(createdEnvelope -> 
                    createdEnvelope.getTitle().equals("Test Envelope") &&
                    createdEnvelope.getExternalEnvelopeId().equals(TEST_DOCUSIGN_ENVELOPE_ID) &&
                    createdEnvelope.getId() != null
                )
                .verifyComplete();

        verify(envelopesApi).createEnvelope(eq(TEST_ACCOUNT_ID), any(EnvelopeDefinition.class));
    }

    @Test
    void getEnvelope_ShouldRetrieveEnvelopeSuccessfully() throws Exception {
        // Given
        // First, create an envelope to establish the mapping
        adapter.getEnvelopeIdMapping().put(TEST_ENVELOPE_ID, TEST_DOCUSIGN_ENVELOPE_ID);

        Envelope docuSignEnvelope = new Envelope();
        docuSignEnvelope.setEnvelopeId(TEST_DOCUSIGN_ENVELOPE_ID);
        docuSignEnvelope.setStatus("sent");
        docuSignEnvelope.setEmailSubject("Test Subject");

        when(envelopesApi.getEnvelope(TEST_ACCOUNT_ID, TEST_DOCUSIGN_ENVELOPE_ID))
                .thenReturn(docuSignEnvelope);

        // When & Then
        StepVerifier.create(adapter.getEnvelope(TEST_ENVELOPE_ID))
                .expectNextMatches(envelope -> 
                    envelope.getId().equals(TEST_ENVELOPE_ID) &&
                    envelope.getExternalEnvelopeId().equals(TEST_DOCUSIGN_ENVELOPE_ID)
                )
                .verifyComplete();

        verify(envelopesApi).getEnvelope(TEST_ACCOUNT_ID, TEST_DOCUSIGN_ENVELOPE_ID);
    }

    @Test
    void sendEnvelope_ShouldSendEnvelopeSuccessfully() throws Exception {
        // Given
        adapter.getEnvelopeIdMapping().put(TEST_ENVELOPE_ID, TEST_DOCUSIGN_ENVELOPE_ID);
        UUID sentBy = UUID.randomUUID();

        EnvelopeUpdateSummary updateSummary = new EnvelopeUpdateSummary();
        updateSummary.setEnvelopeId(TEST_DOCUSIGN_ENVELOPE_ID);

        when(envelopesApi.update(eq(TEST_ACCOUNT_ID), eq(TEST_DOCUSIGN_ENVELOPE_ID), any(Envelope.class)))
                .thenReturn(updateSummary);

        // Mock the getEnvelope call that happens after sending
        Envelope docuSignEnvelope = new Envelope();
        docuSignEnvelope.setEnvelopeId(TEST_DOCUSIGN_ENVELOPE_ID);
        docuSignEnvelope.setStatus("sent");

        when(envelopesApi.getEnvelope(TEST_ACCOUNT_ID, TEST_DOCUSIGN_ENVELOPE_ID))
                .thenReturn(docuSignEnvelope);

        // When & Then
        StepVerifier.create(adapter.sendEnvelope(TEST_ENVELOPE_ID, sentBy))
                .expectNextMatches(envelope -> 
                    envelope.getId().equals(TEST_ENVELOPE_ID)
                )
                .verifyComplete();

        verify(envelopesApi).update(eq(TEST_ACCOUNT_ID), eq(TEST_DOCUSIGN_ENVELOPE_ID), any(Envelope.class));
    }

    @Test
    void voidEnvelope_ShouldVoidEnvelopeSuccessfully() throws Exception {
        // Given
        adapter.getEnvelopeIdMapping().put(TEST_ENVELOPE_ID, TEST_DOCUSIGN_ENVELOPE_ID);
        UUID voidedBy = UUID.randomUUID();
        String voidReason = "Test void reason";

        when(envelopesApi.update(eq(TEST_ACCOUNT_ID), eq(TEST_DOCUSIGN_ENVELOPE_ID), any(Envelope.class)))
                .thenReturn(new EnvelopeUpdateSummary());

        // Mock the getEnvelope call that happens after voiding
        Envelope docuSignEnvelope = new Envelope();
        docuSignEnvelope.setEnvelopeId(TEST_DOCUSIGN_ENVELOPE_ID);
        docuSignEnvelope.setStatus("voided");
        docuSignEnvelope.setVoidedReason(voidReason);

        when(envelopesApi.getEnvelope(TEST_ACCOUNT_ID, TEST_DOCUSIGN_ENVELOPE_ID))
                .thenReturn(docuSignEnvelope);

        // When & Then
        StepVerifier.create(adapter.voidEnvelope(TEST_ENVELOPE_ID, voidReason, voidedBy))
                .expectNextMatches(envelope -> 
                    envelope.getId().equals(TEST_ENVELOPE_ID)
                )
                .verifyComplete();

        verify(envelopesApi).update(eq(TEST_ACCOUNT_ID), eq(TEST_DOCUSIGN_ENVELOPE_ID), any(Envelope.class));
    }

    @Test
    void existsEnvelope_ShouldReturnTrueWhenEnvelopeExists() throws Exception {
        // Given
        adapter.getEnvelopeIdMapping().put(TEST_ENVELOPE_ID, TEST_DOCUSIGN_ENVELOPE_ID);

        Envelope docuSignEnvelope = new Envelope();
        docuSignEnvelope.setEnvelopeId(TEST_DOCUSIGN_ENVELOPE_ID);

        when(envelopesApi.getEnvelope(TEST_ACCOUNT_ID, TEST_DOCUSIGN_ENVELOPE_ID))
                .thenReturn(docuSignEnvelope);

        // When & Then
        StepVerifier.create(adapter.existsEnvelope(TEST_ENVELOPE_ID))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void existsEnvelope_ShouldReturnFalseWhenEnvelopeNotExists() {
        // Given - no mapping exists for the envelope ID

        // When & Then
        StepVerifier.create(adapter.existsEnvelope(TEST_ENVELOPE_ID))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void getEnvelopesByStatus_ShouldReturnEnvelopesWithStatus() throws Exception {
        // Given
        EnvelopesInformation envelopesInfo = new EnvelopesInformation();
        Envelope envelope1 = new Envelope();
        envelope1.setEnvelopeId("envelope-1");
        envelope1.setStatus("sent");
        
        Envelope envelope2 = new Envelope();
        envelope2.setEnvelopeId("envelope-2");
        envelope2.setStatus("sent");
        
        envelopesInfo.setEnvelopes(java.util.Arrays.asList(envelope1, envelope2));

        when(envelopesApi.listStatusChanges(eq(TEST_ACCOUNT_ID), any(EnvelopesApi.ListStatusChangesOptions.class)))
                .thenReturn(envelopesInfo);

        // When & Then
        StepVerifier.create(adapter.getEnvelopesByStatus(EnvelopeStatus.SENT, 10))
                .expectNextCount(2)
                .verifyComplete();

        verify(envelopesApi).listStatusChanges(eq(TEST_ACCOUNT_ID), any(EnvelopesApi.ListStatusChangesOptions.class));
    }

    @Test
    void getSigningUrl_ShouldGenerateSigningUrl() throws Exception {
        // Given
        adapter.getEnvelopeIdMapping().put(TEST_ENVELOPE_ID, TEST_DOCUSIGN_ENVELOPE_ID);
        String signerEmail = "signer@example.com";

        ViewUrl viewUrl = new ViewUrl();
        viewUrl.setUrl("https://demo.docusign.net/signing/startinsession.aspx?t=123");

        when(envelopesApi.createRecipientView(eq(TEST_ACCOUNT_ID), eq(TEST_DOCUSIGN_ENVELOPE_ID), any(RecipientViewRequest.class)))
                .thenReturn(viewUrl);

        // When & Then
        StepVerifier.create(adapter.getSigningUrl(TEST_ENVELOPE_ID, signerEmail))
                .expectNext(viewUrl.getUrl())
                .verifyComplete();

        verify(envelopesApi).createRecipientView(eq(TEST_ACCOUNT_ID), eq(TEST_DOCUSIGN_ENVELOPE_ID), any(RecipientViewRequest.class));
    }

    @Test
    void createEnvelope_ShouldHandleDocuSignException() throws Exception {
        // Given
        SignatureEnvelope envelope = SignatureEnvelope.builder()
                .title("Test Envelope")
                .provider(SignatureProvider.DOCUSIGN)
                .build();

        when(envelopesApi.createEnvelope(eq(TEST_ACCOUNT_ID), any(EnvelopeDefinition.class)))
                .thenThrow(new RuntimeException("DocuSign API error"));

        // When & Then
        StepVerifier.create(adapter.createEnvelope(envelope))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void syncEnvelopeStatus_ShouldSyncStatusSuccessfully() throws Exception {
        // Given
        adapter.getEnvelopeIdMapping().put(TEST_ENVELOPE_ID, TEST_DOCUSIGN_ENVELOPE_ID);

        Envelope docuSignEnvelope = new Envelope();
        docuSignEnvelope.setEnvelopeId(TEST_DOCUSIGN_ENVELOPE_ID);
        docuSignEnvelope.setStatus("completed");

        when(envelopesApi.getEnvelope(TEST_ACCOUNT_ID, TEST_DOCUSIGN_ENVELOPE_ID))
                .thenReturn(docuSignEnvelope);

        // When & Then
        StepVerifier.create(adapter.syncEnvelopeStatus(TEST_ENVELOPE_ID))
                .expectNextMatches(envelope -> 
                    envelope.getId().equals(TEST_ENVELOPE_ID) &&
                    envelope.getStatus() == EnvelopeStatus.COMPLETED
                )
                .verifyComplete();

        verify(envelopesApi).getEnvelope(TEST_ACCOUNT_ID, TEST_DOCUSIGN_ENVELOPE_ID);
    }
}
