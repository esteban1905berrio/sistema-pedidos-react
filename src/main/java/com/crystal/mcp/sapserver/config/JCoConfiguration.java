package com.crystal.mcp.sapserver.config;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * SAP JCo (Java Connector) Configuration.
 *
 * This configuration class sets up the SAP JCo connection pool for RFC
 * communication
 * with SAP systems. It provides thread-safe, pooled connections that can be
 * reused
 * across multiple MCP tool invocations.
 *
 * Key Features:
 * - Programmatic destination configuration (no .jcoDestination files needed)
 * - Automatic connection pooling (pool-capacity and peak-limit)
 * - Health check via ping() on startup
 * - Environment variable-based configuration
 *
 * Thread Safety: JCo connections are thread-safe by design and managed
 * automatically
 * by JCoDestinationManager.
 */
@Slf4j
@Configuration
public class JCoConfiguration {

    @Value("${sap.jco.ashost}")
    private String ashost;

    @Value("${sap.jco.sysnr}")
    private String sysnr;

    @Value("${sap.jco.client}")
    private String client;

    @Value("${sap.jco.user}")
    private String user;

    @Value("${sap.jco.passwd}")
    private String passwd;

    @Value("${sap.jco.lang}")
    private String lang;

    @Value("${sap.jco.router:}")
    private String router;

    @Value("${sap.jco.pool-capacity:5}")
    private String poolCapacity;

    @Value("${sap.jco.peak-limit:10}")
    private String peakLimit;

    @Value("${sap.jco.destination-name:SAP_SYSTEM}")
    private String destinationName;

    /**
     * Creates and configures the SAP JCo destination bean.
     *
     * The destination is the primary interface for executing RFC calls to SAP.
     * It manages a pool of connections automatically, reusing them for performance.
     *
     * @return configured JCoDestination instance
     * @throws JCoException if connection setup or ping fails
     */
    @Bean
    public JCoDestination jcoDestination() throws JCoException {
        log.info("Configuring SAP JCo destination: {}", destinationName);

        // Register custom destination data provider
        CustomDestinationDataProvider provider = new CustomDestinationDataProvider();

        try {
            com.sap.conn.jco.ext.Environment.registerDestinationDataProvider(provider);
            log.debug("DestinationDataProvider registered successfully");
        } catch (IllegalStateException e) {
            // Provider already registered (e.g., during hot reload)
            log.debug("DestinationDataProvider already registered");
        }

        // Configure destination properties
        Properties connectProperties = new Properties();
        connectProperties.setProperty(DestinationDataProvider.JCO_ASHOST, ashost);
        connectProperties.setProperty(DestinationDataProvider.JCO_SYSNR, sysnr);
        connectProperties.setProperty(DestinationDataProvider.JCO_CLIENT, client);
        connectProperties.setProperty(DestinationDataProvider.JCO_USER, user);
        connectProperties.setProperty(DestinationDataProvider.JCO_PASSWD, passwd);
        connectProperties.setProperty(DestinationDataProvider.JCO_LANG, lang);

        log.info("SAP Connection attempt with: Host={}, Client={}, User={}, SysNr={}, Lang={}",
                ashost, client, user, sysnr, lang);

        // Optional SAP Router (for VPN scenarios)
        if (router != null && !router.isEmpty()) {
            connectProperties.setProperty(DestinationDataProvider.JCO_SAPROUTER, router);
            log.debug("SAP Router configured: {}", router);
        }

        // Connection pooling configuration
        connectProperties.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY, poolCapacity);
        connectProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT, peakLimit);

        log.debug("Pool configuration: capacity={}, peak={}", poolCapacity, peakLimit);

        // Store properties in provider
        provider.setDestinationProperties(destinationName, connectProperties);

        // Get destination from manager
        JCoDestination destination = JCoDestinationManager.getDestination(destinationName);

        log.info("JCo Destination configured: {} ({}:{})", destinationName, ashost, sysnr);

        // Test connection health
        try {
            destination.ping();
            log.info("✓ JCo connection test successful");
        } catch (JCoException e) {
            log.error("✗ JCo connection test failed: {}", e.getMessage());
            throw e;
        }

        return destination;
    }

    /**
     * Custom DestinationDataProvider for programmatic configuration.
     *
     * This allows us to configure SAP connections via Spring properties
     * instead of requiring .jcoDestination files on the filesystem.
     */
    private static class CustomDestinationDataProvider implements DestinationDataProvider {

        private Properties destinationProperties;

        /**
         * Sets the configuration properties for a destination.
         *
         * @param destinationName name of the destination
         * @param properties      connection properties
         */
        public void setDestinationProperties(String destinationName, Properties properties) {
            this.destinationProperties = properties;
        }

        @Override
        public Properties getDestinationProperties(String destinationName) {
            return destinationProperties;
        }

        @Override
        public void setDestinationDataEventListener(DestinationDataEventListener listener) {
            // Event listener not needed for POC
        }

        @Override
        public boolean supportsEvents() {
            return false;
        }
    }
}
