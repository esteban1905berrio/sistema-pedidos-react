package com.crystal.mcp.sapserver.manual;

import com.crystal.mcp.sapserver.service.FunctionModuleScanner;

import java.util.Set;

/**
 * Manual test for FunctionModuleScanner.
 *
 * <p>Tests scanning Java source code for custom FM references.
 *
 * <p>Run with:
 * <pre>
 * mvn exec:java -Dexec.mainClass="com.crystal.mcp.sapserver.manual.ManualFunctionModuleScannerTest"
 * </pre>
 */
public class ManualFunctionModuleScannerTest {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║         Manual Test: FunctionModuleScanner               ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        try {
            FunctionModuleScanner scanner = new FunctionModuleScanner();

            // Scan source directory
            System.out.println("\n📂 Scanning src/main/java for custom FMs...\n");
            Set<String> detectedFMs = scanner.scanForUsedFunctionModules("src/main/java");

            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println("📋 Detected " + detectedFMs.size() + " custom Function Modules:");
            System.out.println("═══════════════════════════════════════════════════════════");

            int i = 1;
            for (String fm : detectedFMs) {
                System.out.printf("  %2d. %s%n", i++, fm);
            }

            // Compare with expected list from manifest
            Set<String> manifestFMs = Set.of(
                    "ZCX_GETDDICSOURCE",
                    "ZCX_CREATE_TRANSPORT_COPY",
                    "Z_CX_GET_TRANSPORT_OBJECTS",
                    "Z_CX_GET_OBJECT_IN_OPEN_OT",
                    "Z_CX_GET_TRANSPORT_INFO",
                    "Z_CX_GET_PACKAGE_HIERARCHY",
                    "ZCX_CREATE_TRANSPORT_REQUEST",
                    "ZCX_GET_DUMP_DETAIL",
                    "ZCX_MODIFY_TRANSPORT_REQUEST",
                    "Z_CX_SEARCH_TRANSPORTS",
                    "ZCX_GET_TRANSPORT_LOGS"
            );

            FunctionModuleScanner.ScanDifference diff = scanner.compareWithManifest(detectedFMs, manifestFMs);

            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("🔍 Comparison with manifest.json:");
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println("  Detected in code: " + diff.detectedCount());
            System.out.println("  Defined in manifest: " + diff.manifestCount());

            if (!diff.missingInManifest().isEmpty()) {
                System.out.println("\n  ⚠️  FMs in code but MISSING from manifest:");
                for (String fm : diff.missingInManifest()) {
                    System.out.println("      ➕ " + fm);
                }
            }

            if (!diff.extraInManifest().isEmpty()) {
                System.out.println("\n  ℹ️  FMs in manifest but NOT found in code:");
                for (String fm : diff.extraInManifest()) {
                    System.out.println("      ❓ " + fm);
                }
            }

            if (diff.isSynchronized()) {
                System.out.println("\n  ✅ Manifest is SYNCHRONIZED with code!");
            } else {
                System.out.println("\n  ⚠️  Manifest needs SYNC - run extract_abap_components");
            }

            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("✅ TEST COMPLETED SUCCESSFULLY");
            System.out.println("═══════════════════════════════════════════════════════════");

        } catch (Exception e) {
            System.err.println("❌ TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
