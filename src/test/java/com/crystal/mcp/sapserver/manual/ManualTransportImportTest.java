package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.config.JCoConfiguration;
import com.crystal.mcp.sapserver.model.TransportImportResult;
import com.crystal.mcp.sapserver.service.TransportImportService;
import com.crystal.mcp.sapserver.tool.TransportImportTools;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Disabled("Manual test requiring SAP connection and specific transport data")
public class ManualTransportImportTest {

    @Autowired
    private TransportImportTools transportImportTools;

    @Autowired
    private TransportImportService transportImportService;

    @Autowired
    private JCoConfiguration jCoConfiguration;

    @Test
    void testBeanInjection() {
        assertThat(transportImportTools).isNotNull();
        assertThat(transportImportService).isNotNull();
        assertThat(jCoConfiguration).isNotNull();
    }

    /**
     * Manual test to verify transport import.
     * Requires:
     * 1. Valid destination configuration in environment/properties
     * 2. Existing released transport in target buffer (or use mock/dev system)
     * 3. Update variables below before running
     */
    @Test
    void testImportTransportManual() {
        // SETUP: Change these values to valid ones for your manual test
        String targetSystem = "S4Q";
        String targetClient = "100";
        String transportNumber = "DK900123";

        System.out.println("Starting Manual Transport Import Test...");

        TransportImportResult result = transportImportTools.import_transport_requests(
                targetSystem,
                targetClient,
                transportNumber,
                false, // ignoreLock
                true // importAgain
        );

        System.out.println("Result received:");
        System.out.println("Success: " + (result.errorCount() == 0));
        System.out.println("Message: " + result.results().get(0).message());

        // Assertions (adapt based on expected outcome)
        // assertThat(result.successCount()).isEqualTo(1);
    }
}
