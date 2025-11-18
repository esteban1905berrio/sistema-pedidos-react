package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.ClassIncludeResult;
import com.crystal.mcp.sapserver.model.ClassSourceResult;
import com.crystal.mcp.sapserver.model.DdicSourceResult;
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

    /**
     * MCP Tool: Get DDIC object structure (table/structure/view) from DD03L.
     *
     * This tool retrieves metadata about database tables, structures, and views
     * by calling the custom Function Module ZCX_GETDDICSOURCE, which queries DD03L.
     *
     * Use Cases:
     * - Understand table structure before writing code
     * - Discover field names, types, and key fields
     * - Identify foreign key relationships (check tables)
     * - Analyze data element usage (rollname)
     * - Generate code templates based on structure
     *
     * Returned Field Metadata:
     * - fieldname: Field name
     * - position: Position in table
     * - rollname: Data element (type)
     * - mandatory: 'X' if required field
     * - checktable: Foreign key table (if any)
     * - inttype: Internal type (C=char, N=numeric, D=date, etc.)
     * - intlen: Internal length
     * - datatype: ABAP data type
     * - keyflag: 'X' if primary key field
     * - reffield: Reference field (for foreign keys)
     *
     * Object Types:
     * - TABLE: Transparent tables (database tables)
     * - STRUCTURE: Internal structures (no database persistence)
     * - VIEW: Database views
     * - APPEND: Append structures
     *
     * Examples:
     * - get_ddic_source("MARA") → Material master table structure
     * - get_ddic_source("DD03L") → Table field definition structure
     * - get_ddic_source("T001") → Company codes table
     * - get_ddic_source("V_T001") → Company codes view
     *
     * Note: Requires FM ZCX_GETDDICSOURCE to be activated in SAP system.
     * See docs/abap/FM_ZCX_GETDDICSOURCE_SIGNATURE.md for setup instructions.
     *
     * @param objectName name of table/structure/view (e.g., "MARA", "DD03L", "V_T001")
     * @return DdicSourceResult containing object metadata and field list
     */
    @McpTool(
            description = "Get DDIC object structure (table/structure/view) from DD03L. " +
                    "Retrieves field metadata by calling FM ZCX_GETDDICSOURCE, which queries DD03L system table. " +
                    "Returns object type, status, and detailed field list with names, types, keys, and foreign key relationships. " +
                    "Useful for understanding table structures before writing code, discovering field definitions, and analyzing data models. " +
                    "Supports tables (MARA), structures (DD03L), views (V_T001), and append structures. " +
                    "Example: get_ddic_source('MARA') returns material master table structure with all fields."
    )
    public DdicSourceResult get_ddic_source(
            @McpToolParam(
                    description = "Name of the table, structure, or view to retrieve. " +
                            "Examples: 'MARA' (material master), 'DD03L' (table fields), 'T001' (company codes), 'V_T001' (view). " +
                            "Case-insensitive (will be converted to uppercase).",
                    required = true
            )
            String objectName
    ) {
        return classService.getDdicSource(objectName);
    }

    /**
     * MCP Tool: Modify an ABAP class source code.
     * <p>
     * This is a workflow-based tool that orchestrates the complete ADT modification flow:
     * LOCK → MODIFY → UNLOCK
     * <p>
     * Similar to modify_program_source but specifically for ABAP classes.
     * Supports modification of different include types (main/definitions, implementations, testclasses, macros).
     * <p>
     * Workflow Steps:
     * 1. LOCK: Acquires exclusive lock on the class
     *    - Returns transport number (system-assigned or existing)
     *    - Fails if object is already locked by another user
     * 2. MODIFY: Updates source code with new content
     *    - Uses transport from LOCK response
     *    - Validates lock handle before modification
     * 3. UNLOCK: Releases lock (ALWAYS executed, even on failure)
     *    - Critical step to prevent orphaned locks
     * <p>
     * Based on Python implementation: modification_service.py::modify_class()
     *
     * @param className   name of the class (e.g., "ZCL_TEST")
     * @param newSource   new source code to set (complete replacement)
     * @param includeType include type to modify (main, implementations, testclasses, macros)
     * @param transport   optional transport number (if null, uses system-assigned transport from LOCK)
     * @return ProgramModifyResult with detailed workflow execution status
     */
    @McpTool(
            description = "Modify ABAP class source code with complete workflow (LOCK → MODIFY → UNLOCK). " +
                    "Workflow-based tool that handles locking, modification, and unlocking automatically. " +
                    "Supports modification of different include types (main, implementations, testclasses, macros). " +
                    "Returns transport number from lock operation if not provided. " +
                    "Fails with error if object is already locked by another user. " +
                    "Always unlocks object even on failure (prevents orphaned locks). " +
                    "Example: modify_class('ZCL_TEST', new_code, 'main', null)"
    )
    public com.crystal.mcp.sapserver.model.ProgramModifyResult modify_class(
            @McpToolParam(
                    description = "Name of the class to modify. " +
                            "Examples: 'ZCL_TEST', 'ZCL_INVOICE_PROCESSOR', 'YCL_UTILS'",
                    required = true
            )
            String className,
            @McpToolParam(
                    description = "New source code to set (complete replacement). " +
                            "Must be valid ABAP syntax. " +
                            "For main: Include CLASS/ENDCLASS statements. " +
                            "For implementations: Include METHOD/ENDMETHOD statements.",
                    required = true
            )
            String newSource,
            @McpToolParam(
                    description = "Include type to modify. " +
                            "'main' for class definition (PUBLIC, PROTECTED, PRIVATE sections), " +
                            "'implementations' for method implementations, " +
                            "'testclasses' for unit test classes, " +
                            "'macros' for ABAP macro definitions. " +
                            "Default: 'main'",
                    required = false
            )
            String includeType,
            @McpToolParam(
                    description = "Optional transport number. " +
                            "If not provided, uses system-assigned transport from LOCK operation. " +
                            "Example: 'CADK911122', 'DEVK900123'. " +
                            "Leave null to use automatic transport assignment.",
                    required = false
            )
            String transport
    ) {
        // Apply default for includeType
        String actualIncludeType = (includeType != null && !includeType.isEmpty()) ? includeType : "main";

        return classService.modifyClassSource(className, newSource, actualIncludeType, transport);
    }
}
