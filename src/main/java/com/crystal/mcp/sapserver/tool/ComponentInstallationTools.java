package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.InstallationResult;
import com.crystal.mcp.sapserver.service.ComponentInstallationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * MCP Tools for installing ABAP components to SAP systems.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComponentInstallationTools {

    private final ComponentInstallationService componentInstallationService;

    /**
     * Install ABAP components from local filesystem to SAP system.
     */
    @McpTool(description = "Install ABAP components from local filesystem to SAP system. " +
            "Reads manifest.json and installs function groups, function modules, and classes. " +
            "Handles conflicts via skipExisting or forceOverwrite parameters.")
    public String install_abap_components(
            @McpToolParam(description = "Source directory containing manifest.json. Default: './abap'",
                    required = false)
            String sourcePath,

            @McpToolParam(description = "Target SAP package. Default: '$TMP' (local objects)",
                    required = false)
            String targetPackage,

            @McpToolParam(description = "Transport request. Required if targetPackage != '$TMP'",
                    required = false)
            String transport,

            @McpToolParam(description = "Comma-separated component names to install. Null = all",
                    required = false)
            String components,

            @McpToolParam(description = "Dry run mode - simulate without changes. Default: false",
                    required = false)
            Boolean dryRun,

            @McpToolParam(description = "Skip components that already exist. Default: true",
                    required = false)
            Boolean skipExisting,

            @McpToolParam(description = "Overwrite existing components without confirmation. Default: false",
                    required = false)
            Boolean forceOverwrite) {

        log.info("MCP Tool: install_abap_components called");

        List<String> componentList = null;
        if (components != null && !components.trim().isEmpty()) {
            componentList = Arrays.stream(components.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toUpperCase)
                    .toList();
        }

        boolean dry = dryRun != null ? dryRun : false;
        boolean skip = skipExisting != null ? skipExisting : false;  // Default: don't skip
        boolean force = forceOverwrite != null ? forceOverwrite : true;  // Default: overwrite existing

        InstallationResult result = componentInstallationService.installComponents(
                sourcePath,
                targetPackage,
                transport,
                componentList,
                dry,
                skip,
                force
        );

        // Return concise summary only
        if (result.isSuccess()) {
            return String.format("SUCCESS: Installed %d FGs, %d FMs, %d classes to %s. Transport: %s",
                    result.getFunctionGroupsCreated(),
                    result.getFunctionModulesCreated(),
                    result.getClassesCreated(),
                    result.getTargetPackage(),
                    result.getTransport() != null ? result.getTransport() : "N/A");
        } else {
            return String.format("FAILED: %s. Created: %d FGs, %d FMs, %d classes. Errors: %d",
                    result.getMessage(),
                    result.getFunctionGroupsCreated(),
                    result.getFunctionModulesCreated(),
                    result.getClassesCreated(),
                    result.getErrors().size());
        }
    }
}
