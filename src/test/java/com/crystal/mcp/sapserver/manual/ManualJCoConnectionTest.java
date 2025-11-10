package com.crystal.mcp.sapserver.manual;

import com.sap.conn.jco.*;
import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * Manual test for SAP JCo connection WITHOUT Spring Boot.
 *
 * This test validates:
 * 1. JCo native libraries are loaded correctly
 * 2. SAP connection can be established
 * 3. Basic ping works
 *
 * Run this BEFORE Spring Boot tests to isolate JCo issues.
 *
 * Prerequisites:
 * 1. JCo libraries in lib/ directory (sapjco3.jar + libsapjco3.so)
 * 2. .env file with SAP credentials
 * 3. VPN connection active (if required)
 *
 * How to run:
 *   mvn test -Dtest=ManualJCoConnectionTest
 *
 * Or directly with java:
 *   java -cp "target/classes:lib/sapjco3.jar" \
 *        -Djava.library.path=./lib \
 *        com.crystal.mcp.sapserver.manual.ManualJCoConnectionTest
 */
public class ManualJCoConnectionTest {

    private static final String DESTINATION_NAME = "SAP_MANUAL_TEST";

    public static void main(String[] args) {
        System.out.println("=== Manual SAP JCo Connection Test ===\n");

        try {
            // Step 1: Load .env file
            System.out.println("Step 1: Loading configuration from .env file...");
            Properties envProps = loadEnvFile();
            printConfiguration(envProps);

            // Step 2: Register destination provider
            System.out.println("\nStep 2: Registering JCo destination provider...");
            SimpleDestinationDataProvider provider = new SimpleDestinationDataProvider();
            com.sap.conn.jco.ext.Environment.registerDestinationDataProvider(provider);

            // Step 3: Configure destination
            System.out.println("Step 3: Configuring SAP destination...");
            Properties destProps = new Properties();
            destProps.setProperty(DestinationDataProvider.JCO_ASHOST,
                    envProps.getProperty("SAP_ASHOST"));
            destProps.setProperty(DestinationDataProvider.JCO_SYSNR,
                    envProps.getProperty("SAP_SYSNR"));
            destProps.setProperty(DestinationDataProvider.JCO_CLIENT,
                    envProps.getProperty("SAP_CLIENT"));
            destProps.setProperty(DestinationDataProvider.JCO_USER,
                    envProps.getProperty("SAP_USER"));
            destProps.setProperty(DestinationDataProvider.JCO_PASSWD,
                    envProps.getProperty("SAP_PASSWD"));
            destProps.setProperty(DestinationDataProvider.JCO_LANG,
                    envProps.getProperty("SAP_LANG", "EN"));

            // Optional: SAP Router
            String router = envProps.getProperty("SAP_ROUTER");
            if (router != null && !router.isEmpty()) {
                destProps.setProperty(DestinationDataProvider.JCO_SAPROUTER, router);
                System.out.println("  SAP Router configured: " + router);
            }

            // Connection pooling
            destProps.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY,
                    envProps.getProperty("SAP_POOL_CAPACITY", "5"));
            destProps.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT,
                    envProps.getProperty("SAP_PEAK_LIMIT", "10"));

            provider.setDestinationProperties(DESTINATION_NAME, destProps);

            // Step 4: Get destination
            System.out.println("\nStep 4: Getting JCo destination...");
            JCoDestination destination = JCoDestinationManager.getDestination(DESTINATION_NAME);
            System.out.println("  ✓ Destination created: " + destination.getDestinationName());

            // Step 5: Test connection (ping)
            System.out.println("\nStep 5: Testing connection (ping)...");
            destination.ping();
            System.out.println("  ✓ Ping successful!");

            // Step 6: Get connection metadata
            System.out.println("\nStep 6: Connection metadata:");
            System.out.println("  System ID: " + destination.getAttributes().getSystemID());
            System.out.println("  Client: " + destination.getAttributes().getClient());
            System.out.println("  User: " + destination.getAttributes().getUser());
            System.out.println("  Language: " + destination.getAttributes().getLanguage());
            System.out.println("  Host: " + envProps.getProperty("SAP_ASHOST"));
            System.out.println("  System Number: " + envProps.getProperty("SAP_SYSNR"));

            // Step 7: Test function module call (SADT_REST_RFC_ENDPOINT check)
            System.out.println("\nStep 7: Checking SADT_REST_RFC_ENDPOINT availability...");
            JCoFunction function = destination.getRepository()
                    .getFunction("SADT_REST_RFC_ENDPOINT");

            if (function == null) {
                System.out.println("  ✗ WARNING: SADT_REST_RFC_ENDPOINT not found!");
                System.out.println("    ADT may not be installed or user lacks authorization.");
            } else {
                System.out.println("  ✓ SADT_REST_RFC_ENDPOINT found!");
            }

            System.out.println("\n=== ALL TESTS PASSED ✓ ===");
            System.out.println("\nConnection is working! You can now proceed with Spring Boot tests.");

        } catch (Exception e) {
            System.err.println("\n=== TEST FAILED ✗ ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Load .env file from project root
     */
    private static Properties loadEnvFile() throws Exception {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(".env")) {
            props.load(fis);
            System.out.println("  ✓ .env file loaded");
            return props;
        } catch (Exception e) {
            System.err.println("  ✗ Failed to load .env file");
            System.err.println("    Make sure .env exists in project root with SAP credentials");
            throw e;
        }
    }

    /**
     * Print configuration (masking password)
     */
    private static void printConfiguration(Properties props) {
        System.out.println("  SAP_ASHOST: " + props.getProperty("SAP_ASHOST"));
        System.out.println("  SAP_SYSNR: " + props.getProperty("SAP_SYSNR"));
        System.out.println("  SAP_CLIENT: " + props.getProperty("SAP_CLIENT"));
        System.out.println("  SAP_USER: " + props.getProperty("SAP_USER"));
        System.out.println("  SAP_PASSWD: " + maskPassword(props.getProperty("SAP_PASSWD")));
        System.out.println("  SAP_LANG: " + props.getProperty("SAP_LANG", "EN"));
    }

    private static String maskPassword(String password) {
        if (password == null || password.length() < 3) {
            return "***";
        }
        return password.substring(0, 2) + "***" + password.substring(password.length() - 1);
    }

    /**
     * Simple DestinationDataProvider implementation
     */
    static class SimpleDestinationDataProvider implements DestinationDataProvider {
        private Properties properties;

        public void setDestinationProperties(String destinationName, Properties props) {
            this.properties = props;
        }

        @Override
        public Properties getDestinationProperties(String destinationName) {
            return properties;
        }

        @Override
        public void setDestinationDataEventListener(DestinationDataEventListener listener) {
            // Not needed
        }

        @Override
        public boolean supportsEvents() {
            return false;
        }
    }
}
