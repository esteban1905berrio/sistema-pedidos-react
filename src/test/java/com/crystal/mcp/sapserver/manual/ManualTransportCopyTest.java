package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.model.TransportCopyRequest;
import com.crystal.mcp.sapserver.model.TransportCopyResult;
import com.crystal.mcp.sapserver.service.TransportCopyService;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Manual Test for Transport Copy Functionality.
 *
 * <p><b>Purpose:</b> Allows manual testing of transport copy creation in SAP system
 * without running full Spring Boot MCP server. Useful for:
 * <ul>
 *   <li>Testing ABAP function module after activation</li>
 *   <li>Validating end-to-end workflow (QUERY → CREATE → COPY → RELEASE)</li>
 *   <li>Testing different scenarios (with/without release, custom prefix, etc.)</li>
 * </ul>
 *
 * <p><b>How to Run:</b>
 * <pre>
 * # From command line:
 * mvn test-compile exec:java -Dexec.mainClass="com.crystal.mcp.sapserver.manual.ManualTransportCopyTest"
 *
 * # Or from IDE (IntelliJ/Eclipse):
 * Right-click → Run 'ManualTransportCopyTest.main()'
 * </pre>
 *
 * <p><b>Configuration:</b>
 * Edit the constants below to customize test parameters:
 * <ul>
 *   <li>{@code TEST_SOURCE_TRANSPORT}: Source transport to copy (must exist in SAP)</li>
 *   <li>{@code TEST_TARGET_SYSTEM}: Target system (null for auto-detect)</li>
 *   <li>{@code TEST_PREFIX}: Description prefix (default: "MANUAL_TEST")</li>
 *   <li>{@code TEST_AUTO_RELEASE}: Whether to release automatically (default: false)</li>
 * </ul>
 *
 * <p><b>Prerequisites:</b>
 * <ul>
 *   <li>SAP connection configured via environment variables (SAP_ASHOST, SAP_USER, etc.)</li>
 *   <li>Class {@code ZCLCX_TRANSPORT_MANAGEMENT} activated in GDC</li>
 *   <li>Function module {@code ZCX_CREATE_TRANSPORT_COPY} activated in ZGFCX_1</li>
 *   <li>Valid source transport exists (e.g., CADK911511)</li>
 * </ul>
 *
 * <p><b>Warning:</b> This test creates REAL transport orders in SAP!
 * You must manually delete test transports after testing using SE09/SE10.
 *
 * @author Crystal Development Team
 * @since 2025-11-18
 * @see TransportCopyService
 * @see TransportCopyRequest
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.crystal.mcp.sapserver")
public class ManualTransportCopyTest implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ManualTransportCopyTest.class);

    @Autowired
    private TransportCopyService transportCopyService;

    @Autowired
    private JCoDestination destination;

    // ================================
    // TEST CONFIGURATION - EDIT HERE
    // ================================

    /**
     * Source transport request to copy.
     * IMPORTANT: Must be a valid transport in your SAP system.
     * You can find transports using SE09/SE10 or list_user_transports MCP tool.
     */
    private static final String TEST_SOURCE_TRANSPORT = "CADK911177, CADK911122";

    /**
     * Target system for the transport copy.
     * - null or empty: Auto-detect from source transport (recommended)
     * - "S4D", "S4Q", "S4P", etc.: Specify target system explicitly
     */
    private static final String TEST_TARGET_SYSTEM = null;

    /**
     * Prefix for transport copy description.
     * Final description: "<prefix>: <original_description>"
     * Max length: 60 chars total (including original description)
     */
    private static final String TEST_PREFIX = "MANUAL_TEST";

    /**
     * Auto-release transport after creation.
     * - false: Transport remains modifiable (recommended for testing)
     * - true: Transport is released automatically (cannot modify after)
     */
    private static final boolean TEST_AUTO_RELEASE = false;

    // ================================
    // MAIN METHOD
    // ================================

    public static void main(String[] args) {
        logger.info("=".repeat(80));
        logger.info("MANUAL TRANSPORT COPY TEST");
        logger.info("=".repeat(80));

        SpringApplication.run(ManualTransportCopyTest.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            logger.info("\n" + "=".repeat(80));
            logger.info("STARTING MANUAL TRANSPORT COPY TEST");
            logger.info("=".repeat(80));

            // Print configuration
            printConfiguration();

            // Check prerequisites
            if (!checkPrerequisites()) {
                logger.error("❌ Prerequisites check failed. Aborting test.");
                System.exit(1);
            }

            // Run test scenarios
            logger.info("\n" + "=".repeat(80));
            logger.info("RUNNING TEST SCENARIOS");
            logger.info("=".repeat(80));

            // Scenario 1: Create transport copy with configured settings
            testCreateTransportCopy();

            // Scenario 2: Test with defaults (uncomment to run)
            // testCreateTransportCopyWithDefaults();

            // Scenario 3: Test without release (uncomment to run)
            // testCreateTransportCopyWithoutRelease();

            logger.info("\n" + "=".repeat(80));
            logger.info("✅ ALL TESTS COMPLETED SUCCESSFULLY");
            logger.info("=".repeat(80));

            logger.warn("\n⚠️  IMPORTANT: Remember to manually delete test transports!");
            logger.warn("⚠️  Use SE09/SE10 or delete_object MCP tool");

        } catch (Exception e) {
            logger.error("\n" + "=".repeat(80));
            logger.error("❌ TEST FAILED WITH ERROR");
            logger.error("=".repeat(80), e);
            System.exit(1);
        }
    }

    // ================================
    // TEST SCENARIOS
    // ================================

    /**
     * Scenario 1: Create transport copy with configured settings.
     *
     * <p>This is the main test scenario using the configuration constants
     * defined at the top of this class.
     */
    private void testCreateTransportCopy() throws JCoException {
        logger.info("\n📝 SCENARIO 1: Create Transport Copy (Configured Settings)");
        logger.info("-".repeat(80));

        // Build request
        TransportCopyRequest request = new TransportCopyRequest(
            TEST_SOURCE_TRANSPORT,
            null,  // sourceTransports (null for single mode)
            TEST_TARGET_SYSTEM,
            TEST_PREFIX,
            TEST_AUTO_RELEASE
        );

        logger.info("Request Parameters:");
        logger.info("  - Source Transport: {}", request.sourceTransport());
        logger.info("  - Target System: {}", request.targetSystem() != null ? request.targetSystem() : "auto-detect");
        logger.info("  - Description Prefix: {}", request.getDescriptionPrefixOrDefault());
        logger.info("  - Auto Release: {}", request.autoRelease());

        // Execute service
        logger.info("\n🚀 Executing transport copy...");
        long startTime = System.currentTimeMillis();
        TransportCopyResult result = transportCopyService.createTransportCopy(request);
        long duration = System.currentTimeMillis() - startTime;

        // Print result
        printResult(result, duration);

        // Verify result
        if (!result.success()) {
            throw new RuntimeException("Transport copy failed: " + result.message());
        }

        logger.info("\n✅ SCENARIO 1 PASSED");
    }

    /**
     * Scenario 2: Create transport copy with defaults.
     *
     * <p>Uses helper method that applies default values:
     * <ul>
     *   <li>Target system: Auto-detect from source</li>
     *   <li>Description prefix: "COPIA"</li>
     *   <li>Auto-release: true</li>
     * </ul>
     */
    private void testCreateTransportCopyWithDefaults() throws JCoException {
        logger.info("\n📝 SCENARIO 2: Create Transport Copy (Defaults)");
        logger.info("-".repeat(80));

        logger.info("Using defaults:");
        logger.info("  - Target System: auto-detect");
        logger.info("  - Description Prefix: COPIA");
        logger.info("  - Auto Release: true");

        // Execute service
        logger.info("\n🚀 Executing transport copy...");
        long startTime = System.currentTimeMillis();
        TransportCopyResult result = transportCopyService.createTransportCopyWithDefaults(
            TEST_SOURCE_TRANSPORT
        );
        long duration = System.currentTimeMillis() - startTime;

        // Print result
        printResult(result, duration);

        // Verify result
        if (!result.success()) {
            throw new RuntimeException("Transport copy failed: " + result.message());
        }

        logger.info("\n✅ SCENARIO 2 PASSED");
    }

    /**
     * Scenario 3: Create transport copy without release.
     *
     * <p>Creates a transport copy that remains modifiable.
     * Useful for adding additional objects before release.
     */
    private void testCreateTransportCopyWithoutRelease() throws JCoException {
        logger.info("\n📝 SCENARIO 3: Create Transport Copy (Without Release)");
        logger.info("-".repeat(80));

        logger.info("Configuration:");
        logger.info("  - Source Transport: {}", TEST_SOURCE_TRANSPORT);
        logger.info("  - Target System: auto-detect");
        logger.info("  - Description Prefix: NO_RELEASE_TEST");
        logger.info("  - Auto Release: false");

        // Execute service
        logger.info("\n🚀 Executing transport copy...");
        long startTime = System.currentTimeMillis();
        TransportCopyResult result = transportCopyService.createTransportCopyWithoutRelease(
            TEST_SOURCE_TRANSPORT,
            null,  // Auto-detect target system
            "NO_RELEASE_TEST"
        );
        long duration = System.currentTimeMillis() - startTime;

        // Print result
        printResult(result, duration);

        // Verify result
        if (!result.success()) {
            throw new RuntimeException("Transport copy failed: " + result.message());
        }

        logger.info("\n✅ SCENARIO 3 PASSED");
    }

    // ================================
    // HELPER METHODS
    // ================================

    /**
     * Prints test configuration.
     */
    private void printConfiguration() {
        logger.info("\nTest Configuration:");
        logger.info("  - Source Transport: {}", TEST_SOURCE_TRANSPORT);
        logger.info("  - Target System: {}", TEST_TARGET_SYSTEM != null ? TEST_TARGET_SYSTEM : "auto-detect");
        logger.info("  - Description Prefix: {}", TEST_PREFIX);
        logger.info("  - Auto Release: {}", TEST_AUTO_RELEASE);
    }

    /**
     * Checks prerequisites before running tests.
     *
     * @return true if all prerequisites are met
     */
    private boolean checkPrerequisites() {
        logger.info("\n🔍 Checking Prerequisites...");
        logger.info("-".repeat(80));

        boolean allChecksPassed = true;

        // Check 1: SAP connection
        try {
            destination.ping();
            logger.info("✅ SAP Connection: OK");
        } catch (JCoException e) {
            logger.error("❌ SAP Connection: FAILED", e);
            allChecksPassed = false;
        }

        // Check 2: Function module exists
        boolean fmAvailable = transportCopyService.isFunctionModuleAvailable();
        if (fmAvailable) {
            logger.info("✅ Function Module ZCX_CREATE_TRANSPORT_COPY: Available");
        } else {
            logger.error("❌ Function Module ZCX_CREATE_TRANSPORT_COPY: NOT FOUND");
            logger.error("   Ensure it's created and activated in package ZGFCX_1");
            allChecksPassed = false;
        }

        // Check 3: Source transport format
        if (TEST_SOURCE_TRANSPORT == null || TEST_SOURCE_TRANSPORT.isEmpty()) {
            logger.error("❌ Source Transport: NOT CONFIGURED");
            logger.error("   Edit TEST_SOURCE_TRANSPORT constant in this class");
            allChecksPassed = false;
        } else if (!TEST_SOURCE_TRANSPORT.matches("^[A-Z]{4}K\\d{6}$")) {
            logger.warn("⚠️  Source Transport: Format warning (expected: CADK######)");
            logger.warn("   Current value: {}", TEST_SOURCE_TRANSPORT);
        } else {
            logger.info("✅ Source Transport: {} (format OK)", TEST_SOURCE_TRANSPORT);
        }

        return allChecksPassed;
    }

    /**
     * Prints transport copy result in formatted output.
     *
     * @param result The transport copy result
     * @param duration Execution duration in milliseconds
     */
    private void printResult(TransportCopyResult result, long duration) {
        logger.info("\n📊 Result:");
        logger.info("-".repeat(80));
        logger.info("Status: {} ({})", result.status(), result.getStatusDescription());
        logger.info("Success: {}", result.success());
        logger.info("New Transport: {}", result.newTransportNumber() != null ? result.newTransportNumber() : "N/A");
        logger.info("Message: {}", result.message());
        logger.info("Duration: {} ms", duration);

        // Display release log if available
        if (result.releaseLog() != null && !result.releaseLog().isEmpty()) {
            logger.info("\n📋 Release Log:");
            logger.info("-".repeat(80));

            // Split log by lines for better readability
            String[] logLines = result.releaseLog().split("\\r?\\n");
            for (String line : logLines) {
                logger.info(line);
            }

            logger.info("-".repeat(80));
            logger.info("Log lines: {}, Log size: {} bytes", logLines.length, result.releaseLog().length());
        } else {
            logger.info("\n📋 Release Log: Not available");
        }

        if (result.success()) {
            logger.info("\n✅ Transport copy created successfully!");
            logger.info("🔗 Verify in SAP: SE09 → Transport: {}", result.newTransportNumber());
            logger.warn("⚠️  Remember to delete test transport: {}", result.newTransportNumber());
        } else {
            logger.error("\n❌ Transport copy failed!");
            logger.error("Error: {}", result.message());
        }
    }
}
