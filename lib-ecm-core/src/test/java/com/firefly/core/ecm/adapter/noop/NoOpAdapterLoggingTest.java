package com.firefly.core.ecm.adapter.noop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.firefly.core.ecm.port.document.DocumentPort;
import com.firefly.core.ecm.port.document.DocumentContentPort;
import com.firefly.core.ecm.port.security.DocumentSecurityPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for no-op adapter logging behavior.
 *
 * <p>This test class verifies that no-op adapters log appropriate warning messages
 * when their methods are invoked, providing clear feedback to developers about
 * missing functionality.</p>
 *
 * @author Firefly Software Solutions Inc.
 * @version 1.0
 * @since 1.0
 */
class NoOpAdapterLoggingTest {

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        // Set up log capture for NoOpAdapterBase where the actual logging happens
        logger = (Logger) LoggerFactory.getLogger(NoOpAdapterBase.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.WARN);
    }

    @AfterEach
    void tearDown() {
        // Clean up log capture
        logger.detachAppender(listAppender);
    }

    /**
     * Verifies that no-op document adapter logs warnings when methods are called.
     */
    @Test
    void shouldLogWarningsForDocumentAdapterMethods() {
        DocumentPort adapter = new NoOpGenericAdapter<>("DocumentPort", DocumentPort.class).getProxy();
        UUID testDocumentId = UUID.randomUUID();

        // Call a query method
        StepVerifier.create(adapter.getDocument(testDocumentId))
                .verifyComplete();

        // Call a modification method and expect error
        StepVerifier.create(adapter.deleteDocument(testDocumentId))
                .expectError(UnsupportedOperationException.class)
                .verify();

        // Verify warning logs were generated
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(2);

        // Check that warning messages contain expected content
        boolean foundGetDocumentWarning = logEvents.stream()
                .anyMatch(event -> event.getLevel() == Level.WARN &&
                        event.getFormattedMessage().contains("getDocument"));

        boolean foundDeleteDocumentWarning = logEvents.stream()
                .anyMatch(event -> event.getLevel() == Level.WARN &&
                        event.getFormattedMessage().contains("deleteDocument"));

        assertThat(foundGetDocumentWarning).isTrue();
        assertThat(foundDeleteDocumentWarning).isTrue();
    }

    /**
     * Verifies that no-op content adapter logs warnings when methods are called.
     */
    @Test
    void shouldLogWarningsForContentAdapterMethods() {
        DocumentContentPort adapter = new NoOpGenericAdapter<>("DocumentContentPort", DocumentContentPort.class).getProxy();
        UUID testDocumentId = UUID.randomUUID();
        byte[] testContent = "test content".getBytes();

        // Call a query method
        StepVerifier.create(adapter.getContent(testDocumentId))
                .verifyComplete();

        // Call a modification method and expect error
        StepVerifier.create(adapter.storeContent(testDocumentId, testContent, "application/octet-stream"))
                .expectError(UnsupportedOperationException.class)
                .verify();

        // Verify warning logs were generated
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(2);

        // Check that warning messages contain expected content
        boolean foundGetContentWarning = logEvents.stream()
                .anyMatch(event -> event.getLevel() == Level.WARN &&
                        event.getFormattedMessage().contains("getContent"));

        boolean foundStoreContentWarning = logEvents.stream()
                .anyMatch(event -> event.getLevel() == Level.WARN &&
                        event.getFormattedMessage().contains("storeContent"));

        assertThat(foundGetContentWarning).isTrue();
        assertThat(foundStoreContentWarning).isTrue();
    }

    /**
     * Verifies that no-op security adapter logs warnings when methods are called.
     */
    @Test
    void shouldLogWarningsForSecurityAdapterMethods() {
        DocumentSecurityPort adapter = new NoOpGenericAdapter<>("DocumentSecurityPort", DocumentSecurityPort.class).getProxy();
        UUID testDocumentId = UUID.randomUUID();
        UUID testUserId = UUID.randomUUID();

        // Call a permission check method (should return true with warning)
        StepVerifier.create(adapter.canAccessDocument(testDocumentId, testUserId, "READ"))
                .expectNext(true)
                .verifyComplete();

        // Call an encryption method and expect error
        StepVerifier.create(adapter.encryptContent(testDocumentId, "content".getBytes(), "key"))
                .expectError(UnsupportedOperationException.class)
                .verify();

        // Verify warning logs were generated
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(2);

        // Check that warning messages contain expected content
        boolean foundAccessWarning = logEvents.stream()
                .anyMatch(event -> event.getLevel() == Level.WARN &&
                        event.getFormattedMessage().contains("canAccessDocument"));

        boolean foundEncryptWarning = logEvents.stream()
                .anyMatch(event -> event.getLevel() == Level.WARN &&
                        event.getFormattedMessage().contains("encryptContent"));

        assertThat(foundAccessWarning).isTrue();
        assertThat(foundEncryptWarning).isTrue();
    }

    /**
     * Verifies that adapter names are correctly returned for identification.
     */
    @Test
    void shouldReturnCorrectAdapterNames() {
        DocumentPort documentAdapter = new NoOpGenericAdapter<>("DocumentPort", DocumentPort.class).getProxy();
        DocumentContentPort contentAdapter = new NoOpGenericAdapter<>("DocumentContentPort", DocumentContentPort.class).getProxy();
        DocumentSecurityPort securityAdapter = new NoOpGenericAdapter<>("DocumentSecurityPort", DocumentSecurityPort.class).getProxy();

        // Test that getAdapterName method works for DocumentPort (which has this method)
        assertThat(documentAdapter.getAdapterName()).isEqualTo("NoOpDocumentPortAdapter");

        // Test that the adapters are proxy instances
        assertThat(documentAdapter.getClass().getSimpleName()).contains("Proxy");
        assertThat(contentAdapter.getClass().getSimpleName()).contains("Proxy");
        assertThat(securityAdapter.getClass().getSimpleName()).contains("Proxy");
    }
}
