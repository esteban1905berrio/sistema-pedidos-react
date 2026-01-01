package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.PrerequisiteCheckResult;
import com.crystal.mcp.sapserver.model.ValidationResult;
import com.crystal.mcp.sapserver.service.ComponentPrerequisiteService;
import com.crystal.mcp.sapserver.service.ComponentValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for ABAP component management (prerequisites and validation).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComponentManagementTools {

    private final ComponentPrerequisiteService prerequisiteService;
    private final ComponentValidationService validationService;

    /**
     * Check prerequisites for ABAP component installation.
     */
    @McpTool(description = "Check prerequisites before installing ABAP components. " +
            "Verifies /UI2/CL_JSON availability, ADT endpoint, and required FMs.")
    public String check_abap_prerequisites(
            @McpToolParam(description = "Path to manifest.json. Default: './abap/manifest.json'",
                    required = false)
            String manifestPath) {

        log.info("MCP Tool: check_abap_prerequisites called");

        PrerequisiteCheckResult result = prerequisiteService.checkPrerequisites(manifestPath);

        long passed = result.getChecks().stream().filter(PrerequisiteCheckResult.CheckItem::isPassed).count();
        long total = result.getChecks().size();

        if (result.isSuccess()) {
            return String.format("PREREQUISITES OK: %d/%d checks passed. %s",
                    passed, total, result.getMessage());
        } else {
            return String.format("PREREQUISITES FAILED: %d/%d checks passed. Errors: %s",
                    passed, total, String.join("; ", result.getErrors()));
        }
    }

    /**
     * Validate local ABAP components against SAP system.
     */
    @McpTool(description = "Validate local ABAP components against SAP system. " +
            "Compares checksums to detect differences between local files and SAP.")
    public String validate_abap_components(
            @McpToolParam(description = "Source directory with manifest.json. Default: './abap'",
                    required = false)
            String sourcePath,

            @McpToolParam(description = "Compare checksums (slower but accurate). Default: true",
                    required = false)
            Boolean checkChecksums) {

        log.info("MCP Tool: validate_abap_components called");

        boolean checksums = checkChecksums != null ? checkChecksums : true;
        ValidationResult result = validationService.validateComponents(sourcePath, checksums);

        if (result.isSuccess()) {
            return String.format("VALIDATION OK: All %d components in sync with SAP",
                    result.getTotalComponents());
        } else {
            return String.format("VALIDATION: %d total, %d match, %d mismatch, %d missing in SAP, %d missing local",
                    result.getTotalComponents(),
                    result.getMatchingComponents(),
                    result.getMismatchedComponents(),
                    result.getMissingInSap(),
                    result.getMissingLocal());
        }
    }
}
