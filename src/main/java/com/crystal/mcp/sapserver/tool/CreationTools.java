package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.CreationResult;
import com.crystal.mcp.sapserver.service.CreationService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for ABAP Object Creation.
 * <p>
 * This component provides tools for creating ABAP objects:
 * - Function Groups (FUGR/F)
 * - Function Modules (FUGR/FF)
 * - Classes (CLAS/OC)
 * - Interfaces (INTF/OI)
 * <p>
 * Spring AI MCP Server automatically discovers and registers @McpTool methods.
 * <p>
 * Based on Python implementation: creation_service.py
 * <p>
 * Phase 1 Tools (4 tools):
 * - create_function_group: Create new function group
 * - create_function_module: Create new function module in function group
 * - create_class: Create new ABAP class
 * - create_interface: Create new ABAP interface
 * <p>
 * NOTE: Object deletion has been moved to {@link DeletionTools}
 */
@Component
@RequiredArgsConstructor
public class CreationTools {

    private final CreationService creationService;

    /**
     * MCP Tool: Create a new function group.
     * <p>
     * A function group is a container for function modules in ABAP.
     * It provides shared data and includes for multiple function modules.
     * <p>
     * Workflow:
     * 1. Validates function group name format
     * 2. Registers in ABAP Workbench repository
     * 3. Creates function group structure
     * <p>
     * Based on Python: creation_service.py::create_function_group()
     *
     * @param functionGroupName name of the function group (e.g., "ZTEST_FG")
     * @param description       description (max 60 chars)
     * @param packageName       package (e.g., "$TMP" for local, "ZPACKAGE" for development)
     * @param transport         optional transport number (null for local objects in $TMP)
     * @return CreationResult with success status and details
     */
    @McpTool(
            description = "Create a new ABAP function group. " +
                    "Function groups are containers for function modules, providing shared data and includes. " +
                    "Workflow: VALIDATE → REGISTER → CREATE. " +
                    "Name must start with letter, max 26 chars, only A-Z0-9_. " +
                    "Use $TMP package for local objects (no transport needed). " +
                    "Example: create_function_group('ZTEST_FG', 'Test Function Group', '$TMP', null)"
    )
    public CreationResult create_function_group(
            @McpToolParam(
                    description = "Name of the function group. " +
                            "Must start with letter, max 26 chars, only A-Z0-9_. " +
                            "Examples: 'ZTEST_FG', 'ZFI_UTILS', 'ZMMI_PROCESS'",
                    required = true
            )
            String functionGroupName,
            @McpToolParam(
                    description = "Description of the function group (max 60 chars). " +
                            "Example: 'Test Function Group for Invoice Processing'",
                    required = true
            )
            String description,
            @McpToolParam(
                    description = "Package name. " +
                            "Use '$TMP' for local objects (no transport). " +
                            "Use development package for shared objects (requires transport). " +
                            "Examples: '$TMP', 'ZPACKAGE', 'ZFI'",
                    required = true
            )
            String packageName,
            @McpToolParam(
                    description = "Optional transport number. " +
                            "Required for non-local packages. " +
                            "Leave null for $TMP objects. " +
                            "Examples: 'CADK911122', 'DEVK900123', null",
                    required = false
            )
            String transport
    ) {
        return creationService.createFunctionGroup(functionGroupName, description, packageName, transport);
    }

    /**
     * MCP Tool: Create a new function module in an existing function group.
     * <p>
     * Function modules are reusable procedures that can be called from ABAP programs.
     * They must be created within an existing function group.
     * <p>
     * Workflow:
     * 1. Validates function module name format
     * 2. Verifies function group exists
     * 3. Registers function module in function group
     * 4. Creates function module structure
     * <p>
     * Based on Python: creation_service.py::create_function_module()
     *
     * @param functionModuleName name of the function module (e.g., "Z_TEST_FM")
     * @param functionGroupName  parent function group name
     * @param description        description (max 60 chars)
     * @param transport          optional transport number (null for local)
     * @return CreationResult with success status and details
     */
    @McpTool(
            description = "Create a new ABAP function module in an existing function group. " +
                    "Function modules are reusable procedures callable from ABAP programs. " +
                    "Workflow: VALIDATE → VERIFY_GROUP → REGISTER → CREATE. " +
                    "Name must start with letter, max 30 chars, only A-Z0-9_. " +
                    "Function group must exist before creating function module. " +
                    "Example: create_function_module('Z_TEST_FM', 'ZTEST_FG', 'Test Function Module', null)"
    )
    public CreationResult create_function_module(
            @McpToolParam(
                    description = "Name of the function module. " +
                            "Must start with letter, max 30 chars, only A-Z0-9_. " +
                            "Examples: 'Z_TEST_FM', 'Z_GET_INVOICE', 'Z_PROCESS_ORDER'",
                    required = true
            )
            String functionModuleName,
            @McpToolParam(
                    description = "Parent function group name (must exist). " +
                            "Examples: 'ZTEST_FG', 'ZFI_UTILS', 'ZMMI_PROCESS'",
                    required = true
            )
            String functionGroupName,
            @McpToolParam(
                    description = "Description of the function module (max 60 chars). " +
                            "Example: 'Get Invoice Details by Number'",
                    required = true
            )
            String description,
            @McpToolParam(
                    description = "Optional transport number. " +
                            "Required for non-local function groups. " +
                            "Leave null for $TMP objects. " +
                            "Examples: 'CADK911122', 'DEVK900123', null",
                    required = false
            )
            String transport
    ) {
        return creationService.createFunctionModule(functionModuleName, functionGroupName, description, transport);
    }

    /**
     * MCP Tool: Create a new ABAP class.
     * <p>
     * ABAP classes provide object-oriented programming capabilities.
     * Classes can inherit from other classes and implement interfaces.
     * <p>
     * Workflow:
     * 1. Validates class name format
     * 2. Creates class with metadata (optionally with superclass)
     * <p>
     * Based on Python: creation_service.py::create_class()
     *
     * @param className   name of the class (e.g., "ZCL_TEST")
     * @param description description (max 60 chars)
     * @param packageName package (e.g., "$TMP", "ZPACKAGE")
     * @param transport   optional transport number (null for local)
     * @param superclass  optional superclass name
     * @return CreationResult with success status and details
     */
    @McpTool(
            description = "Create a new ABAP class. " +
                    "ABAP classes provide object-oriented programming capabilities. " +
                    "Workflow: VALIDATE → CREATE. " +
                    "Name must start with letter, max 30 chars, only A-Z0-9_. " +
                    "Can optionally inherit from a superclass. " +
                    "Example: create_class('ZCL_TEST', 'Test Class', '$TMP', null, null)"
    )
    public CreationResult create_class(
            @McpToolParam(
                    description = "Name of the class. " +
                            "Must start with letter, max 30 chars, only A-Z0-9_. " +
                            "Convention: Start with ZCL_ or YCL_ for custom classes. " +
                            "Examples: 'ZCL_TEST', 'ZCL_INVOICE_PROCESSOR', 'YCL_UTILS'",
                    required = true
            )
            String className,
            @McpToolParam(
                    description = "Description of the class (max 60 chars). " +
                            "Example: 'Invoice Processor Class'",
                    required = true
            )
            String description,
            @McpToolParam(
                    description = "Package name. " +
                            "Use '$TMP' for local objects (no transport). " +
                            "Use development package for shared objects (requires transport). " +
                            "Examples: '$TMP', 'ZPACKAGE', 'ZFI'",
                    required = true
            )
            String packageName,
            @McpToolParam(
                    description = "Optional transport number. " +
                            "Required for non-local packages. " +
                            "Leave null for $TMP objects. " +
                            "Examples: 'CADK911122', 'DEVK900123', null",
                    required = false
            )
            String transport,
            @McpToolParam(
                    description = "Optional superclass name (for inheritance). " +
                            "Leave null for classes without inheritance. " +
                            "Examples: 'CL_ABAP_CHAR_UTILITIES', 'ZCL_BASE_PROCESSOR', null",
                    required = false
            )
            String superclass
    ) {
        return creationService.createClass(className, description, packageName, transport, superclass);
    }

    /**
     * MCP Tool: Create a new ABAP interface.
     * <p>
     * ABAP interfaces define contracts that classes can implement.
     * They provide multiple inheritance and polymorphism capabilities.
     * <p>
     * Workflow:
     * 1. Validates interface name format
     * 2. Creates interface with metadata
     * <p>
     * Based on Python: creation_service.py::create_interface()
     *
     * @param interfaceName name of the interface (e.g., "ZIF_TEST")
     * @param description   description (max 60 chars)
     * @param packageName   package (e.g., "$TMP", "ZPACKAGE")
     * @param transport     optional transport number (null for local)
     * @return CreationResult with success status and details
     */
    @McpTool(
            description = "Create a new ABAP interface. " +
                    "ABAP interfaces define contracts that classes can implement. " +
                    "Workflow: VALIDATE → CREATE. " +
                    "Name must start with letter, max 30 chars, only A-Z0-9_. " +
                    "Convention: Start with ZIF_ or YIF_ for custom interfaces. " +
                    "Example: create_interface('ZIF_TEST', 'Test Interface', '$TMP', null)"
    )
    public CreationResult create_interface(
            @McpToolParam(
                    description = "Name of the interface. " +
                            "Must start with letter, max 30 chars, only A-Z0-9_. " +
                            "Convention: Start with ZIF_ or YIF_ for custom interfaces. " +
                            "Examples: 'ZIF_TEST', 'ZIF_PROCESSOR', 'YIF_LOGGER'",
                    required = true
            )
            String interfaceName,
            @McpToolParam(
                    description = "Description of the interface (max 60 chars). " +
                            "Example: 'Processor Interface'",
                    required = true
            )
            String description,
            @McpToolParam(
                    description = "Package name. " +
                            "Use '$TMP' for local objects (no transport). " +
                            "Use development package for shared objects (requires transport). " +
                            "Examples: '$TMP', 'ZPACKAGE', 'ZFI'",
                    required = true
            )
            String packageName,
            @McpToolParam(
                    description = "Optional transport number. " +
                            "Required for non-local packages. " +
                            "Leave null for $TMP objects. " +
                            "Examples: 'CADK911122', 'DEVK900123', null",
                    required = false
            )
            String transport
    ) {
        return creationService.createInterface(interfaceName, description, packageName, transport);
    }

    /**
     * DEPRECATED: Use DeletionTools.delete_object() instead.
     *
     * This method has been replaced by a superior implementation in DeletionTools
     * that uses stateful workflows and builds URIs automatically.
     *
     * @deprecated Use {@link com.crystal.mcp.sapserver.tool.DeletionTools#delete_object(String, String, String, String)}
     */
    /*
    @McpTool(
            description = "Delete an ABAP object via ADT API. " +
                    "Supports: classes, interfaces, function groups, function modules, programs. " +
                    "Requires ADT URI of the object (obtain via search_objects). " +
                    "Requires transport number for non-local objects. " +
                    "Example: delete_object('/sap/bc/adt/oo/classes/zcl_test', 'CADK911122')"
    )
    public CreationResult delete_object(
            @McpToolParam(
                    description = "ADT URI of the object to delete. " +
                            "Obtain via search_objects or get_object_structure. " +
                            "Examples: " +
                            "'/sap/bc/adt/oo/classes/zcl_test' (class), " +
                            "'/sap/bc/adt/oo/interfaces/zif_test' (interface), " +
                            "'/sap/bc/adt/functions/groups/ztest_fg' (function group), " +
                            "'/sap/bc/adt/programs/programs/ztest_program' (program)",
                    required = true
            )
            String objectUri,
            @McpToolParam(
                    description = "Transport number. " +
                            "Required for non-local objects (not in $TMP). " +
                            "Examples: 'CADK911122', 'DEVK900123'",
                    required = true
            )
            String transport
    ) {
        return creationService.deleteObject(objectUri, transport);
    }
    */
}
