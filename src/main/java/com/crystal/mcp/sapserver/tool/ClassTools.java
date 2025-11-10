package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.ClassSourceResult;
import com.crystal.mcp.sapserver.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for ABAP Class Operations.
 *
 * This component defines MCP (Model Context Protocol) tools that enable
 * LLM agents like Claude to interact with ABAP classes in SAP systems.
 *
 * Spring AI MCP Server automatically discovers and registers methods annotated
 * with @McpTool. No manual registration needed.
 *
 * POC Phase Tools:
 * - get_class_source: Retrieve ABAP class source code
 *
 * Post-POC Tools (to be implemented):
 * - get_class_structure: Get class metadata and components
 * - get_class_includes: List all include types for a class
 * - get_class_components: Detailed component information
 */
@Component
@RequiredArgsConstructor
public class ClassTools {

    private final ClassService classService;

    /**
     * MCP Tool: Get ABAP class source code.
     *
     * This tool enables Claude to retrieve the complete source code of any ABAP class
     * from the SAP system, including different include types (definition, implementation,
     * test classes, macros).
     *
     * Example Claude prompt:
     * "Use the get_class_source tool to fetch the source code for class CL_ABAP_CHAR_UTILITIES"
     *
     * @param className   name of the ABAP class (e.g., "CL_ABAP_CHAR_UTILITIES", "ZTEST_CLASS")
     * @param version     version to retrieve: "active" (default) or "inactive"
     * @param includeType include type: "main" (default), "implementation", "testclasses", "macros"
     * @return ClassSourceResult containing source code and metadata
     */
    @McpTool(
            description = "Get the source code of an ABAP class from SAP system. " +
                    "Returns the complete class definition including methods, attributes, and implementation. " +
                    "Supports fragmentation by include type to handle large classes."
    )
    public ClassSourceResult get_class_source(
            @McpToolParam(description = "Name of the ABAP class (e.g., 'CL_ABAP_CHAR_UTILITIES', 'ZTEST_CLASS')", required = true)
            String className,
            @McpToolParam(description = "Version to retrieve: 'active' for activated code or 'inactive' for draft", required = false)
            String version,
            @McpToolParam(description = "Include type: 'main' (definition), 'implementation', 'testclasses', 'macros'", required = false)
            String includeType
    ) {
        // Apply defaults
        String actualVersion = (version != null && !version.isEmpty()) ? version : "active";
        String actualIncludeType = (includeType != null && !includeType.isEmpty()) ? includeType : "main";

        return classService.getClassSource(className, actualVersion, actualIncludeType);
    }
}
