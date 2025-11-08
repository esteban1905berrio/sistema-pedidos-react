package com.crystal.mcp.sapserver.integration;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for SAP JCo connection.
 *
 * This test requires:
 * - Active VPN connection to SAP network
 * - Valid SAP credentials in environment variables
 * - SAP JCo native libraries in lib/ directory
 *
 * To run:
 * 1. Set environment variables (see .env.example)
 * 2. Ensure VPN is connected
 * 3. Run: mvn verify -Dtest=JCoConnectionTest
 *
 * This test is DISABLED by default in the POC phase.
 * Enable by setting: @EnabledIf("#{environment.acceptsProfiles('integration')}")
 */
@SpringBootTest
@ActiveProfiles("test")
class JCoConnectionTest {

    @Autowired(required = false)
    private JCoDestination destination;

    /**
     * Test basic JCo connection via ping.
     *
     * This verifies that:
     * - JCo configuration is correct
     * - Native libraries are loaded
     * - SAP system is reachable
     * - Credentials are valid
     * - Connection pool is functional
     */
    @Test
    void testConnectionPing() throws JCoException {
        // Given: JCo destination bean should be available
        assertThat(destination)
                .withFailMessage("JCoDestination bean not found. Check configuration.")
                .isNotNull();

        // When: Ping SAP system
        destination.ping();

        // Then: Connection successful (no exception thrown)
        System.out.println("✓ JCo connection successful");
        System.out.println("  Destination: " + destination.getDestinationName());
        System.out.println("  System ID: " + destination.getAttributes().getSystemID());
        System.out.println("  Client: " + destination.getAttributes().getClient());
        System.out.println("  User: " + destination.getAttributes().getUser());
        System.out.println("  Language: " + destination.getAttributes().getLanguage());
    }

    /**
     * Test connection metadata.
     *
     * Verifies that connection properties are correctly configured.
     */
    @Test
    void testConnectionMetadata() throws JCoException {
        assertThat(destination).isNotNull();

        // Verify connection properties
        assertThat(destination.getDestinationName()).isNotEmpty();
        assertThat(destination.getClient()).isNotEmpty();
        assertThat(destination.getUser()).isNotEmpty();
        assertThat(destination.getLanguage()).isNotEmpty();

        System.out.println("✓ Connection metadata validated");
    }
}
