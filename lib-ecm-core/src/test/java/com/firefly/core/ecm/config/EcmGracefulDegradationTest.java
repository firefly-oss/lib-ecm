package com.firefly.core.ecm.config;

import com.firefly.core.ecm.adapter.noop.NoOpAdapterFactory;
import com.firefly.core.ecm.port.document.DocumentContentPort;
import com.firefly.core.ecm.port.document.DocumentPort;
import com.firefly.core.ecm.port.security.DocumentSecurityPort;
import com.firefly.core.ecm.service.EcmPortProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ECM graceful degradation functionality.
 *
 * <p>This test class verifies that the ECM system can start successfully and provide
 * no-op adapters as fallbacks when no real adapter implementations are available.
 * It ensures that the application doesn't fail during startup and that appropriate
 * warnings are logged when ECM functionality is attempted but not available.</p>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest(classes = {
    EcmAutoConfiguration.class,
    NoOpAdapterFactory.class
})
@TestPropertySource(properties = {
    "firefly.ecm.enabled=true",
    "firefly.ecm.adapter-type=nonexistent-adapter",
    "firefly.ecm.features.document-management=true",
    "firefly.ecm.features.content-storage=true",
    "firefly.ecm.features.security=true"
})
class EcmGracefulDegradationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private EcmPortProvider ecmPortProvider;

    @Autowired
    private DocumentPort documentPort;

    @Autowired
    private DocumentContentPort documentContentPort;

    @Autowired
    private DocumentSecurityPort documentSecurityPort;

    /**
     * Verifies that the Spring application context starts successfully even when no adapters are configured.
     */
    @Test
    void shouldStartApplicationContextWithoutAdapters() {
        assertThat(applicationContext).isNotNull();
        assertThat(ecmPortProvider).isNotNull();
    }

    /**
     * Verifies that no-op adapter beans are created and injected when no real adapters are available.
     */
    @Test
    void shouldCreateNoOpAdapterBeans() {
        assertThat(documentPort).isNotNull();
        assertThat(documentPort.getClass().getSimpleName()).contains("Proxy");

        assertThat(documentContentPort).isNotNull();
        assertThat(documentContentPort.getClass().getSimpleName()).contains("Proxy");

        assertThat(documentSecurityPort).isNotNull();
        assertThat(documentSecurityPort.getClass().getSimpleName()).contains("Proxy");
    }

    /**
     * Verifies that no-op adapters return appropriate responses for query operations.
     */
    @Test
    void shouldReturnEmptyResultsForQueryOperations() {
        UUID testDocumentId = UUID.randomUUID();

        // Document retrieval should return empty
        StepVerifier.create(documentPort.getDocument(testDocumentId))
                .verifyComplete();

        // Content retrieval should return empty
        StepVerifier.create(documentContentPort.getContent(testDocumentId))
                .verifyComplete();

        // Content size should return empty
        StepVerifier.create(documentContentPort.getContentSize(testDocumentId))
                .verifyComplete();
    }

    /**
     * Verifies that no-op adapters return permissive defaults for security operations.
     */
    @Test
    void shouldReturnPermissiveDefaultsForSecurityOperations() {
        UUID testDocumentId = UUID.randomUUID();
        UUID testUserId = UUID.randomUUID();

        // Access checks should return true (permissive default)
        StepVerifier.create(documentSecurityPort.canAccessDocument(testDocumentId, testUserId, "READ"))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(documentSecurityPort.canDeleteDocument(testDocumentId, testUserId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(documentSecurityPort.canModifyDocument(testDocumentId, testUserId))
                .expectNext(true)
                .verifyComplete();
    }

    /**
     * Verifies that no-op adapters return error signals for modification operations.
     */
    @Test
    void shouldReturnErrorSignalsForModificationOperations() {
        UUID testDocumentId = UUID.randomUUID();
        byte[] testContent = "test content".getBytes();

        // Document creation should return error
        StepVerifier.create(documentPort.createDocument(null, testContent))
                .expectError(UnsupportedOperationException.class)
                .verify();

        // Document deletion should return error
        StepVerifier.create(documentPort.deleteDocument(testDocumentId))
                .expectError(UnsupportedOperationException.class)
                .verify();

        // Content storage should return error (with correct parameters)
        StepVerifier.create(documentContentPort.storeContent(testDocumentId, testContent, "application/octet-stream"))
                .expectError(UnsupportedOperationException.class)
                .verify();

        // Content deletion should return error
        StepVerifier.create(documentContentPort.deleteContent(testDocumentId))
                .expectError(UnsupportedOperationException.class)
                .verify();

        // Security operations should return error
        StepVerifier.create(documentSecurityPort.encryptContent(testDocumentId, testContent, "key"))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }

    /**
     * Verifies that the EcmPortProvider returns empty optionals when no real adapters are available.
     */
    @Test
    void shouldReturnEmptyOptionalsFromPortProvider() {
        // Port provider should return empty optionals for missing adapters
        assertThat(ecmPortProvider.getDocumentPort()).isEmpty();
        assertThat(ecmPortProvider.getDocumentContentPort()).isEmpty();
        assertThat(ecmPortProvider.getDocumentSecurityPort()).isEmpty();
        assertThat(ecmPortProvider.getPermissionPort()).isEmpty();
        assertThat(ecmPortProvider.getSignatureEnvelopePort()).isEmpty();
    }
}
