package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.ExtractionResult;
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
 * <p>These tools implement Phase 2 of the ABAP Component Distribution system,
 * allowing extraction of ABAP source code from SAP to the local filesystem.
 *
 * <p>Available tools:
 * <ul>
 *   <li>extract_abap_components - Extract ABAP components based on manifest.json</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * // Extract all components to default location
 * extract_abap_components(null, null, true, true)
 *
 * // Extract specific function module
 * extract_abap_components("./abap", "ZCX_GETDDICSOURCE", true, true)
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComponentExtractionTools {

    private final ComponentExtractionService componentExtractionService;

    /**
     * Extract ABAP components from SAP system to local filesystem.
     *
     * <p>This tool reads the manifest.json catalog and extracts the specified
     * components (or all components if none specified) from the connected SAP
     * system. The source code is saved in ADT-compatible format.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Read manifest.json to identify components</li>
     *   <li>For each component, call appropriate service (ClassService, ProgramService)</li>
     *   <li>Write source files to the target directory structure</li>
     *   <li>Optionally update manifest.json with extraction metadata</li>
     * </ol>
     *
     * <p>Directory structure created:
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
     * @param targetPath       Base directory for extraction. Default: "./abap"
     * @param components       Comma-separated list of component names to extract.
     *                         If null or empty, extracts all components from manifest.
     *                         Examples: "ZCX_GETDDICSOURCE", "ZCX_GETDDICSOURCE,ZCLCX_TRANSPORT_MANAGEMENT"
     * @param includeMetadata  Whether to generate metadata JSON files. Default: true
     * @param updateManifest   Whether to update manifest.json with extraction date. Default: true
     * @return ExtractionResult containing summary of extraction operation
     */
    @McpTool(description = "Extract ABAP components from SAP system to local filesystem. " +
            "Reads manifest.json catalog and extracts function modules, classes, and includes. " +
            "Saves source code in ADT-compatible structure. " +
            "Token cost: ~500-2000 tokens per component (depends on source size).")
    public ExtractionResult extract_abap_components(
            @McpToolParam(description = "Base directory for extraction. Default: './abap'. "
                    + "Must contain a manifest.json file.", required = false)
            String targetPath,

            @McpToolParam(description = "Comma-separated component names to extract. "
                    + "If null or empty, extracts all components from manifest. "
                    + "Examples: 'ZCX_GETDDICSOURCE' or 'ZCX_GETDDICSOURCE,ZCLCX_TRANSPORT_MANAGEMENT'",
                    required = false)
            String components,

            @McpToolParam(description = "Generate metadata JSON files for each component. "
                    + "Default: true", required = false)
            Boolean includeMetadata,

            @McpToolParam(description = "Update manifest.json with extraction date and checksums. "
                    + "Default: true", required = false)
            Boolean updateManifest) {

        log.info("MCP Tool: extract_abap_components called with targetPath={}, components={}, "
                        + "includeMetadata={}, updateManifest={}",
                targetPath, components, includeMetadata, updateManifest);

        // Parse component list
        List<String> componentList = null;
        if (components != null && !components.trim().isEmpty()) {
            componentList = Arrays.stream(components.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toUpperCase)
                    .toList();
        }

        // Set defaults
        boolean metadata = includeMetadata != null ? includeMetadata : true;
        boolean manifest = updateManifest != null ? updateManifest : true;

        // Execute extraction
        ExtractionResult result = componentExtractionService.extractComponents(
                targetPath,
                componentList,
                metadata,
                manifest
        );

        log.info("Extraction completed: success={}, components={}, files={}",
                result.isSuccess(), result.getTotalComponents(), result.getFilesWritten());

        return result;
    }
}
