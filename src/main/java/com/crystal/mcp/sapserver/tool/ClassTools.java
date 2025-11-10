package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.ClassIncludeResult;
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
 * Progressive Discovery Workflow:
 * Stage 1: search_objects (SearchTools) → Find classes
 * Stage 2: get_object_structure (ObjectTools) → Get class metadata
 * Stage 2.5: get_class_includes (ClassTools) → List includes (NEW)
 * Stage 3: get_class_source or get_include_source → Get source code
 *
 * Phase 1 Tools:
 * - get_class_source: Retrieve ABAP class source code
 * - get_class_includes: List all include types for a class (NEW)
 *
 * Future Tools:
 * - get_class_components: Detailed component information
 * - lock_class: Lock class for editing
 * - unlock_class: Release class lock
 * - set_class_source: Update class source code
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

    /**
     * MCP Tool: Get all includes of an ABAP class.
     *
     * This tool implements Progressive Discovery Stage 2.5: List includes before fetching.
     * Identifies which include types exist for a class without fetching source code.
     *
     * Token Optimization:
     * - Checks existence only: ~200 tokens
     * - Avoids fetching source: saves ~2,000+ tokens per include
     * - Allows selective fetching of only needed includes
     * - Enables parallel fetching of multiple includes
     *
     * Use Case:
     * After identifying a class, use this tool to:
     * - See which includes exist (definitions, implementations, testclasses, macros)
     * - Check include sizes before fetching
     * - Decide which includes to fetch (selective)
     * - Prepare for parallel fetching with get_include_source
     *
     * Standard ABAP Class Includes:
     * - definitions: Class definition (PUBLIC, PROTECTED, PRIVATE sections)
     * - implementations: Method implementations
     * - testclasses: Unit test classes
     * - macros: ABAP macro definitions
     *
     * Workflow Example:
     * 1. User: "What includes does ZCL_INVOICE have?"
     * 2. Claude: get_class_includes("ZCL_INVOICE") → definitions (50KB), implementations (120KB)
     * 3. User: "Show me just the definitions"
     * 4. Claude: get_include_source("ZCL_INVOICE", "definitions") → Get specific include
     *
     * Parallelization Strategy:
     * After this tool identifies includes, use get_include_source multiple times
     * in parallel to fetch different includes simultaneously (LLM can parallelize).
     *
     * @param className name of the ABAP class (e.g., "ZCL_TEST")
     * @return ClassIncludeResult containing list of includes with existence info
     */
    @McpTool(
            description = "Get all includes of an ABAP class. " +
                    "Progressive Discovery Stage 2.5: List includes before fetching source. " +
                    "Checks existence of standard include types (definitions, implementations, testclasses, macros) " +
                    "without fetching source code. " +
                    "Token cost: ~200 tokens (vs ~2,000+ per include if fetching source). " +
                    "Enables selective and parallel fetching with get_include_source. " +
                    "Example: 'ZCL_INVOICE', 'CL_ABAP_CHAR_UTILITIES'"
    )
    public ClassIncludeResult get_class_includes(
            @McpToolParam(
                    description = "Name of the ABAP class. " +
                            "Examples: 'ZCL_INVOICE', 'CL_ABAP_CHAR_UTILITIES', 'ZCL_TEST'",
                    required = true
            )
            String className
    ) {
        return classService.getClassIncludes(className);
    }
}
