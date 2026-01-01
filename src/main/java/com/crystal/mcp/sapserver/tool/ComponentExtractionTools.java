package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.ExtractionResult;
import com.crystal.mcp.sapserver.model.ExtractionScope;
import com.crystal.mcp.sapserver.service.AbapExtractionService;
import com.crystal.mcp.sapserver.service.ComponentExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * MCP Tools for extracting ABAP components from SAP systems.
 *
 * <p>
 * These tools implement Phase 2 of the ABAP Component Distribution system,
 * allowing extraction of ABAP source code from SAP to the local filesystem.
 *
 * <p>
 * Available tools:
 * <ul>
 * <li>extract_abap_components - Extract ABAP components based on
 * manifest.json</li>
 * <li>sync_manifest_with_code - Synchronize manifest with FMs used in Java
 * code</li>
 * </ul>
 *
 * <p>
 * Usage example:
 * 
 * <pre>
 * // Extract all components to default location
 * extract_abap_components(null, null, true, true)
 *
 * // Extract specific function module
 * extract_abap_components("./abap", "ZCX_GETDDICSOURCE", true, true)
 *
 * // Sync manifest before extraction (done automatically, but can be called manually)
 * sync_manifest_with_code("./abap")
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComponentExtractionTools {

        private final ComponentExtractionService componentExtractionService;
        private final AbapExtractionService abapExtractionService;

        /**
         * Extract ABAP components from SAP system to local filesystem.
         *
         * <p>
         * This tool reads the manifest.json catalog and extracts the specified
         * components (or all components if none specified) from the connected SAP
         * system. The source code is saved in ADT-compatible format.
         *
         * <p>
         * Workflow:
         * <ol>
         * <li>Read manifest.json to identify components</li>
         * <li>For each component, call appropriate service (ClassService,
         * ProgramService)</li>
         * <li>Write source files to the target directory structure</li>
         * <li>Optionally update manifest.json with extraction metadata</li>
         * </ol>
         *
         * <p>
         * Directory structure created:
         * 
         * <pre>
         * {target_path}/
         * ├── manifest.json
         * ├── functions/groups/{name}/
         * │   ├── includes/
         * │   │   ├── l{name}top.abap
         * │   │   └── l{name}uxx.abap
         * │   └── fmodules/{fm_name}/
         * │       └── {fm_name}.asfunc
         * └── classlib/classes/{name}/
         *     └── {name}.aclass
         * </pre>
         *
         * @param targetPath      Base directory for extraction. Default: "./abap"
         * @param components      Comma-separated list of component names to extract.
         *                        If null or empty, extracts all components from
         *                        manifest.
         *                        Examples: "ZCX_GETDDICSOURCE",
         *                        "ZCX_GETDDICSOURCE,ZCLCX_TRANSPORT_MANAGEMENT"
         * @param includeMetadata Whether to generate metadata JSON files. Default: true
         * @param updateManifest  Whether to update manifest.json with extraction date.
         *                        Default: true
         * @return ExtractionResult containing summary of extraction operation
         */
        /**
         * Extract ABAP components from SAP system to local filesystem.
         *
         * <p>
         * This unified tool supports two modes:
         * <ol>
         * <li><strong>Manifest Mode (Legacy):</strong> Extracts components defined in
         * manifest.json.
         * Used when 'scope' is null or 'manifest'.</li>
         * <li><strong>Dynamic Scope Mode:</strong> Discovers and requests extraction of
         * objects
         * based on dynamic criteria (user, package, transport, list).</li>
         * </ol>
         *
         * @param scope           Extraction scope: "manifest", "user", "package",
         *                        "transport", "list"
         * @param scopeInput      Input for the scope (e.g. username, package name)
         * @param targetPath      Base directory. Default: "./abap"
         * @param components      (Legacy) Comma-separated list for Manifest mode.
         * @param includeMetadata Generate metadata. Default: true
         * @param updateManifest  Update manifest. Default: true
         */
        @McpTool(description = "Extract ABAP components to local filesystem. " +
                        "Supports multiple scopes: " +
                        "'manifest' (read manifest.json), " +
                        "'user' (by author), " +
                        "'package' (by devclass), " +
                        "'transport' (by trkorr), " +
                        "'list' (by name). " +
                        "Examples: scope='package' input='ZCX' | scope='manifest'")
        public ExtractionResult extract_abap_components(
                        @McpToolParam(description = "Extraction scope: 'manifest' (default), 'user', 'package', 'transport', 'list'", required = false) String scope,

                        @McpToolParam(description = "Input for scope (e.g. package name, username, transport number)", required = false) String input,

                        @McpToolParam(description = "Base directory. Default: './abap'", required = false) String targetPath,

                        @McpToolParam(description = "Legacy: Comma-separated component names (only for scope='manifest')", required = false) String components,

                        @McpToolParam(description = "Generate metadata. Default: true", required = false) Boolean includeMetadata,

                        @McpToolParam(description = "Update manifest. Default: true", required = false) Boolean updateManifest) {

                log.info("MCP Tool: extract_abap_components scope={}, input={}, target={}", scope, input, targetPath);

                // Default to manifest mode if scope is missing
                String effectiveScope = (scope == null || scope.isEmpty()) ? "manifest" : scope.toLowerCase();

                if ("manifest".equals(effectiveScope)) {
                        // Legacy Manifest Flow
                        List<String> componentList = null;
                        if (components != null && !components.trim().isEmpty()) {
                                componentList = Arrays.stream(components.split(","))
                                                .map(String::trim).filter(s -> !s.isEmpty()).map(String::toUpperCase)
                                                .toList();
                        }
                        return componentExtractionService.extractComponents(
                                        targetPath, componentList,
                                        includeMetadata != null ? includeMetadata : true,
                                        updateManifest != null ? updateManifest : true);
                } else {
                        // New Dynamic Scope Flow
                        // 1. Discover
                        ExtractionScope extractionScope = ExtractionScope.fromCode(effectiveScope);
                        com.crystal.mcp.sapserver.model.ExtractionDiscovery discovery = abapExtractionService
                                        .discover(extractionScope, input);

                        if (discovery.objects().isEmpty()) {
                                return ExtractionResult.builder()
                                                .success(true)
                                                .message("No objects found for scope: " + effectiveScope)
                                                .filesWritten(0)
                                                .build();
                        }

                        // 2. Extract
                        return componentExtractionService.extractDiscoveredObjects(discovery.objects(), targetPath);
                }
        }

        /**
         * Synchronize manifest.json with function modules actually used in the MCP
         * server code.
         *
         * <p>
         * This tool scans the Java source code to detect custom FM references
         * (Z_CX_*, ZCX_*) and updates the manifest to include any missing FMs.
         *
         * <p>
         * This is useful for:
         * <ul>
         * <li>Ensuring all FMs used by the MCP server are in the manifest</li>
         * <li>Detecting FMs that were added to the code but not the manifest</li>
         * <li>Auditing manifest completeness before extraction</li>
         * </ul>
         *
         * <p>
         * Note: This sync is automatically performed by extract_abap_components,
         * but can be called manually to preview changes without extraction.
         *
         * @param targetPath Path to ABAP components directory containing manifest.json
         * @return ManifestSyncResult with details of synchronization
         */
        @McpTool(description = "Synchronize manifest.json with function modules used in Java code. " +
                        "Scans Java source for custom FM references (Z_CX_*, ZCX_*) and updates manifest. " +
                        "Automatically called by extract_abap_components, but can be run separately. " +
                        "Token cost: ~100-300 tokens.")
        public ComponentExtractionService.ManifestSyncResult sync_manifest_with_code(
                        @McpToolParam(description = "Path to ABAP components directory containing manifest.json. "
                                        + "Default: './abap'", required = false) String targetPath) {

                log.info("MCP Tool: sync_manifest_with_code called with targetPath={}", targetPath);

                ComponentExtractionService.ManifestSyncResult result = componentExtractionService
                                .syncManifestWithUsedFMs(targetPath);

                log.info("Sync completed: success={}, added={}, extra={}",
                                result.isSuccess(),
                                result.getAddedFMs() != null ? result.getAddedFMs().size() : 0,
                                result.getExtraFMs() != null ? result.getExtraFMs().size() : 0);

                return result;
        }
}
