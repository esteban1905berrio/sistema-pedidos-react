package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.TransportCopyRequest;
import com.crystal.mcp.sapserver.model.TransportCopyResult;
import com.sap.conn.jco.JCoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TransportCopyService.
 *
 * <p>These tests require a live SAP connection configured via environment variables.
 * Tests use real transport requests in the GDC system.
 *
 * <p><b>Prerequisites:</b>
 * <ul>
 *   <li>SAP connection configured (SAP_ASHOST, SAP_USER, etc.)</li>
 *   <li>Function module ZCX_CREATE_TRANSPORT_COPY activated in GDC</li>
 *   <li>Class ZCLCX_TRANSPORT_MANAGEMENT activated in GDC</li>
 *   <li>Valid test transport request available (e.g., CADK911511)</li>
 * </ul>
 *
 * <p><b>Important:</b> These tests create real transports in SAP. Use with caution
 * and clean up test transports manually after testing.
 *
 * @author Crystal Development Team
 * @since 2025-11-18
 */
@SpringBootTest
class TransportCopyServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(TransportCopyServiceTest.class);

    @Autowired
    private TransportCopyService transportCopyService;

    /**
     * Known test transport in GDC system.
     * Update this to a valid transport in your environment.
     */
    private static final String TEST_SOURCE_TRANSPORT = "CADK911511";
    private static final String TEST_TARGET_SYSTEM = "S4D";

    @BeforeEach
    void setUp() {
        logger.info("Starting TransportCopyService integration test");
    }

    /**
     * Test: Verify function module exists in SAP system.
     *
     * <p>This is a prerequisite check before running actual transport copy tests.
     */
    @Test
    void testFunctionModuleAvailable() {
        logger.info("Testing if function module ZCX_CREATE_TRANSPORT_COPY is available");

        boolean available = transportCopyService.isFunctionModuleAvailable();

        assertTrue(available,
            "Function module ZCX_CREATE_TRANSPORT_COPY not found in SAP system. " +
            "Ensure it's created and activated in package ZGFCX_1.");

        logger.info("✅ Function module ZCX_CREATE_TRANSPORT_COPY is available");
    }

    /**
     * Test: Create transport copy with default settings.
     *
     * <p><b>Warning:</b> This test creates a real transport in SAP!
     * The transport will be automatically released.
     */
    @Test
    void testCreateTransportCopy_WithDefaults() throws JCoException {
        logger.info("Testing transport copy creation with defaults");

        // Given
        TransportCopyRequest request = TransportCopyRequest.withDefaults(TEST_SOURCE_TRANSPORT);

        // When
        TransportCopyResult result = transportCopyService.createTransportCopy(request);

        // Then
        logger.info("Result: {}", result);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.success(), "Transport copy should succeed");
        assertEquals("S", result.status(), "Status should be 'S' (Success)");
        assertNotNull(result.newTransportNumber(), "New transport number should not be null");
        assertTrue(
            result.newTransportNumber().startsWith("CADK") ||
            result.newTransportNumber().startsWith("DEVK"),
            "Transport number should start with CADK or DEVK"
        );
        assertNotNull(result.message(), "Message should not be null");

        logger.info("✅ Transport copy created successfully: {}", result.newTransportNumber());
        logger.warn("⚠️  Remember to clean up test transport: {}", result.newTransportNumber());
    }

    /**
     * Test: Create transport copy without auto-release.
     *
     * <p><b>Warning:</b> This test creates a real transport in SAP!
     * The transport will remain modifiable (not released).
     */
    @Test
    void testCreateTransportCopy_WithoutRelease() throws JCoException {
        logger.info("Testing transport copy creation without release");

        // Given
        TransportCopyRequest request = TransportCopyRequest.withoutRelease(
            TEST_SOURCE_TRANSPORT,
            TEST_TARGET_SYSTEM,
            "TEST_NO_RELEASE"
        );

        // When
        TransportCopyResult result = transportCopyService.createTransportCopy(request);

        // Then
        logger.info("Result: {}", result);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.success(), "Transport copy should succeed");
        assertEquals("S", result.status(), "Status should be 'S' (Success)");
        assertNotNull(result.newTransportNumber(), "New transport number should not be null");

        logger.info("✅ Transport copy created (not released): {}", result.newTransportNumber());
        logger.warn("⚠️  Remember to clean up test transport: {}", result.newTransportNumber());
    }

    /**
     * Test: Create transport copy with custom prefix.
     *
     * <p><b>Warning:</b> This test creates a real transport in SAP!
     */
    @Test
    void testCreateTransportCopy_WithCustomPrefix() throws JCoException {
        logger.info("Testing transport copy creation with custom prefix");

        // Given
        String customPrefix = "JUNIT_TEST";
        TransportCopyRequest request = new TransportCopyRequest(
            TEST_SOURCE_TRANSPORT,
            TEST_TARGET_SYSTEM,
            customPrefix,
            false  // Don't release for testing
        );

        // When
        TransportCopyResult result = transportCopyService.createTransportCopy(request);

        // Then
        logger.info("Result: {}", result);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.success(), "Transport copy should succeed");
        assertEquals("S", result.status(), "Status should be 'S' (Success)");

        logger.info("✅ Transport copy created with custom prefix: {}", result.newTransportNumber());
        logger.warn("⚠️  Remember to clean up test transport: {}", result.newTransportNumber());
    }

    /**
     * Test: Create transport copy with invalid source transport.
     *
     * <p>This test should fail with TRANSPORT_NOT_FOUND exception.
     */
    @Test
    void testCreateTransportCopy_InvalidSourceTransport() {
        logger.info("Testing transport copy creation with invalid source transport");

        // Given
        TransportCopyRequest request = TransportCopyRequest.withDefaults("INVALID999");

        // When/Then
        assertThrows(Exception.class, () -> {
            TransportCopyResult result = transportCopyService.createTransportCopy(request);
            logger.info("Result: {}", result);

            // If no exception, result should indicate failure
            if (result != null) {
                assertFalse(result.success(), "Should fail with invalid transport");
                assertEquals("E", result.status(), "Status should be 'E' (Error)");
            }
        }, "Should throw exception or return error for invalid transport");

        logger.info("✅ Invalid transport correctly rejected");
    }

    /**
     * Test: Request validation.
     *
     * <p>Tests that empty/null source transport is rejected.
     */
    @Test
    void testCreateTransportCopy_ValidationError() {
        logger.info("Testing transport copy request validation");

        // Given
        TransportCopyRequest request = new TransportCopyRequest(
            null,  // Invalid: null source transport
            TEST_TARGET_SYSTEM,
            "TEST",
            true
        );

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            request.validate();
        }, "Should throw IllegalArgumentException for null source transport");

        logger.info("✅ Validation correctly rejects null source transport");
    }

    /**
     * Test: Description prefix too long.
     *
     * <p>Tests that description prefix length is validated.
     */
    @Test
    void testCreateTransportCopy_DescriptionPrefixTooLong() {
        logger.info("Testing transport copy with long description prefix");

        // Given - 51 characters (over 50 limit)
        String longPrefix = "A".repeat(51);
        TransportCopyRequest request = new TransportCopyRequest(
            TEST_SOURCE_TRANSPORT,
            TEST_TARGET_SYSTEM,
            longPrefix,
            true
        );

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            request.validate();
        }, "Should throw IllegalArgumentException for prefix > 50 chars");

        logger.info("✅ Validation correctly rejects long prefix");
    }

    /**
     * Test: Helper method - createTransportCopyWithDefaults.
     */
    @Test
    void testCreateTransportCopyWithDefaults_HelperMethod() throws JCoException {
        logger.info("Testing helper method: createTransportCopyWithDefaults");

        // When
        TransportCopyResult result = transportCopyService.createTransportCopyWithDefaults(
            TEST_SOURCE_TRANSPORT
        );

        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.success(), "Transport copy should succeed");

        logger.info("✅ Helper method works correctly: {}", result.newTransportNumber());
        logger.warn("⚠️  Remember to clean up test transport: {}", result.newTransportNumber());
    }
}
