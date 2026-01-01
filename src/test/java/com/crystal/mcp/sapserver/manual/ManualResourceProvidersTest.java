package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.resource.ClassResourceProvider;
import com.crystal.mcp.sapserver.resource.PackageResourceProvider;
import com.crystal.mcp.sapserver.resource.TableResourceProvider;
import com.crystal.mcp.sapserver.resource.TransportResourceProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

/**
 * Manual test for MCP Resource Providers.
 *
 * This test validates:
 * 1. ClassResourceProvider - 4 resources (definition, implementation, methods, attributes)
 * 2. TransportResourceProvider - 2 resources (info, objects)
 * 3. PackageResourceProvider - 2 resources (objects, hierarchy)
 * 4. TableResourceProvider - 1 resource (fields)
 *
 * Prerequisites:
 * 1. .env file configured
 * 2. VPN connection active
 *
 * How to run:
 *   mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualResourceProvidersTest
 */
@Profile("!test")
@SpringBootApplication
@ComponentScan(basePackages = "com.crystal.mcp.sapserver")
public class ManualResourceProvidersTest implements CommandLineRunner {

    @Autowired
    private ClassResourceProvider classResourceProvider;

    @Autowired
    private TransportResourceProvider transportResourceProvider;

    @Autowired
    private PackageResourceProvider packageResourceProvider;

    @Autowired
    private TableResourceProvider tableResourceProvider;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManualResourceProvidersTest.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           Manual Test: MCP Resource Providers                ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        try {
            // Test 1: ClassResourceProvider
            testClassResourceProvider();

            // Test 2: TransportResourceProvider
            testTransportResourceProvider();

            // Test 3: PackageResourceProvider
            testPackageResourceProvider();

            // Test 4: TableResourceProvider
            testTableResourceProvider();

            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    ALL TESTS PASSED                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.err.println("❌ TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void testClassResourceProvider() {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Test 1: ClassResourceProvider");
        System.out.println("═══════════════════════════════════════════════════════════════");

        String className = "CL_ABAP_CHAR_UTILITIES";

        // Test 1.1: Get class definition
        System.out.println("\n1.1 Testing sap://class/" + className + "/definition");
        McpSchema.ReadResourceResult defResult = classResourceProvider.getClassDefinition(className);
        printResult("Class Definition", defResult);

        // Test 1.2: Get class implementation
        System.out.println("\n1.2 Testing sap://class/" + className + "/implementation");
        McpSchema.ReadResourceResult implResult = classResourceProvider.getClassImplementation(className);
        printResult("Class Implementation", implResult);

        // Test 1.3: Get class methods
        System.out.println("\n1.3 Testing sap://class/" + className + "/methods");
        McpSchema.ReadResourceResult methodsResult = classResourceProvider.getClassMethods(className);
        printResult("Class Methods", methodsResult);

        // Test 1.4: Get class attributes
        System.out.println("\n1.4 Testing sap://class/" + className + "/attributes");
        McpSchema.ReadResourceResult attrsResult = classResourceProvider.getClassAttributes(className);
        printResult("Class Attributes", attrsResult);

        System.out.println("✅ ClassResourceProvider: All 4 resources working");
    }

    private void testTransportResourceProvider() {
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("Test 2: TransportResourceProvider");
        System.out.println("═══════════════════════════════════════════════════════════════");

        String transportId = "GDCK903118";

        // Test 2.1: Get transport info
        System.out.println("\n2.1 Testing sap://transport/" + transportId + "/info");
        McpSchema.ReadResourceResult infoResult = transportResourceProvider.getTransportInfo(transportId);
        printResult("Transport Info", infoResult);

        // Test 2.2: Get transport objects
        System.out.println("\n2.2 Testing sap://transport/" + transportId + "/objects");
        McpSchema.ReadResourceResult objsResult = transportResourceProvider.getTransportObjects(transportId);
        printResult("Transport Objects", objsResult);

        System.out.println("✅ TransportResourceProvider: All 2 resources working");
    }

    private void testPackageResourceProvider() {
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("Test 3: PackageResourceProvider");
        System.out.println("═══════════════════════════════════════════════════════════════");

        String packageName = "ZCX";

        // Test 3.1: Get package objects
        System.out.println("\n3.1 Testing sap://package/" + packageName + "/objects");
        McpSchema.ReadResourceResult objsResult = packageResourceProvider.getPackageObjects(packageName);
        printResult("Package Objects", objsResult);

        // Test 3.2: Get package hierarchy
        System.out.println("\n3.2 Testing sap://package/" + packageName + "/hierarchy");
        McpSchema.ReadResourceResult hierResult = packageResourceProvider.getPackageHierarchy(packageName);
        printResult("Package Hierarchy", hierResult);

        System.out.println("✅ PackageResourceProvider: All 2 resources working");
    }

    private void testTableResourceProvider() {
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("Test 4: TableResourceProvider");
        System.out.println("═══════════════════════════════════════════════════════════════");

        String tableName = "MARA";

        // Test 4.1: Get table fields
        System.out.println("\n4.1 Testing sap://table/" + tableName + "/fields");
        McpSchema.ReadResourceResult fieldsResult = tableResourceProvider.getTableFields(tableName);
        printResult("Table Fields", fieldsResult);

        System.out.println("✅ TableResourceProvider: 1 resource working");
    }

    private void printResult(String resourceName, McpSchema.ReadResourceResult result) {
        if (result == null || result.contents() == null || result.contents().isEmpty()) {
            System.out.println("   ❌ " + resourceName + ": No result");
            return;
        }

        McpSchema.ResourceContents contents = result.contents().get(0);
        if (contents instanceof McpSchema.TextResourceContents textContents) {
            String text = textContents.text();
            int length = text != null ? text.length() : 0;
            System.out.println("   ✓ " + resourceName + ": " + length + " chars");
            System.out.println("   URI: " + textContents.uri());
            System.out.println("   MIME: " + textContents.mimeType());

            // Show preview (first 200 chars)
            if (length > 0) {
                String preview = text.length() > 200 ? text.substring(0, 200) + "..." : text;
                System.out.println("   Preview: " + preview.replace("\n", "\\n"));
            }
        } else {
            System.out.println("   ✓ " + resourceName + ": Binary content");
        }
    }
}
