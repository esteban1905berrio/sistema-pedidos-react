package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.ObjectSourceResult;
import com.crystal.mcp.sapserver.model.ObjectStructure;
import com.crystal.mcp.sapserver.service.ObjectService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for Generic ABAP Object Operations.
 *
 * This component provides generic tools that work across all ABAP object types.
 * Part of Progressive Discovery architecture.
 *
 * Spring AI MCP Server automatically discovers and registers @McpTool methods.
 *
 * Progressive Discovery Workflow:
 * Stage 1: search_objects (SearchTools) → Find objects, get URIs
 * Stage 2: get_object_structure (ObjectTools) → Get metadata without source
 * Stage 3: get_object_source (ObjectTools) → Fetch full source on demand
 *
 * Phase 1 Tools:
 * - get_object_structure: Get object metadata without source code (Stage 2)
 * - get_object_source: Get source code by URI (Stage 3)
 *
 * Future Tools:
 * - lock_object: Lock object for editing
 * - unlock_object: Release object lock
 * - set_object_source: Update object source code
 */
@Component
@RequiredArgsConstructor
public class ObjectTools {

        private final ObjectService objectService;

        /**
         * MCP Tool: Get structure/metadata for any ABAP object without source code.
         *
         * This tool implements Progressive Discovery Stage 2: Get metadata before
         * fetching source.
         * Returns object components (methods, attributes, includes) without source
         * code.
         *
         * Token Optimization:
         * - Stage 1 (search_objects): ~500 tokens → Find objects
         * - Stage 2 (get_object_structure): ~800 tokens → Get metadata (THIS)
         * - Stage 3 (get_object_source): ~3,000+ tokens → Get full source
         *
         * Use Case:
         * After search_objects identifies potential objects, use this tool to:
         * - Verify it's the right object (check description, components)
         * - See available methods/attributes without loading full source
         * - Decide if you need the full source code (Stage 3)
         *
         * Works with:
         * - Classes: /sap/bc/adt/oo/classes/{name}
         * - Programs: /sap/bc/adt/programs/programs/{name}
         * - Function Groups: /sap/bc/adt/functions/groups/{name}
         * - Interfaces: /sap/bc/adt/oo/interfaces/{name}
         *
         * Workflow Example:
         * 1. User: "Find invoice-related classes"
         * 2. Claude: search_objects("*invoice*") → Gets URIs
         * 3. User: "What methods does the first one have?"
         * 4. Claude: get_object_structure(uri) → Gets metadata with methods list
         * 5. User: "Show me the EXECUTE method implementation"
         * 6. Claude: get_object_source(uri) → Gets full source (only when needed)
         *
         * @param objectUri ADT URI for the object (from search results, without
         *                  /objectstructure)
         * @return ObjectStructure containing metadata and components
         */
        @McpTool(description = "Get structure/metadata for any ABAP object (class, program, function group, etc.) " +
                        "without fetching source code. Progressive Discovery Stage 2. " +
                        "Use after search_objects to see object components (methods, attributes) " +
                        "before deciding if full source is needed. " +
                        "Token cost: ~800 tokens (much cheaper than full source). " +
                        "Example URIs: '/sap/bc/adt/oo/classes/ZCL_TEST', " +
                        "'/sap/bc/adt/programs/programs/ZREP001'")
        public ObjectStructure get_object_structure(
                        @McpToolParam(description = "ADT URI of the object (without /objectstructure suffix). " +
                                        "Obtain from search_objects results. " +
                                        "Format examples: " +
                                        "'/sap/bc/adt/oo/classes/{name}' (class), " +
                                        "'/sap/bc/adt/programs/programs/{name}' (program), " +
                                        "'/sap/bc/adt/functions/groups/{name}' (function group)", required = true) String objectUri) {
                return objectService.getObjectStructure(objectUri);
        }

        /**
         * MCP Tool: Get source code for any ABAP object by URI.
         *
         * This tool implements Progressive Discovery Stage 3: Fetch Full Source.
         * Only use after search_objects identified the object and you need the actual
         * code.
         *
         * Token Optimization:
         * - Stage 1 (search_objects): ~500 tokens → Get object URIs
         * - Stage 2 (get_object_structure): ~800 tokens → Get metadata
         * - Stage 3 (get_object_source): ~3,000+ tokens → Get full source
         *
         * Generic URI-based access works with:
         * - Classes: /sap/bc/adt/oo/classes/{name}/source/main
         * - Programs: /sap/bc/adt/programs/programs/{name}/source/main
         * - Function Groups: /sap/bc/adt/functions/groups/{name}/source/main
         * - Interfaces: /sap/bc/adt/oo/interfaces/{name}/source/main
         * - Includes: /sap/bc/adt/programs/includes/{name}/source/main
         *
         * Workflow Example:
         * 1. User: "Find invoice-related classes"
         * 2. Claude: search_objects("*invoice*") → Gets URIs
         * 3. User: "Show me the first one"
         * 4. Claude: get_object_source(uri_from_step_2) → Gets source
         *
         * @param objectUri ADT URI for the object (from search results)
         * @param version   version to retrieve: "active" (default) or "inactive"
         * @return ObjectSourceResult containing source code
         */
        @McpTool(description = "Get source code for any ABAP object (class, program, function group, etc.) by URI. " +
                        "This is the generic Stage 3 tool for Progressive Discovery. " +
                        "Use after search_objects provides the object URI. " +
                        "Works with any ADT-compatible object type. " +
                        "Token cost: ~3,000+ tokens (only use when source code is needed). " +
                        "Example URIs: '/sap/bc/adt/oo/classes/ZCL_TEST/source/main', " +
                        "'/sap/bc/adt/programs/programs/ZREP001/source/main'. " +
                        "WARNING: For Function Groups or Programs with includes, " +
                        "this tool implies context loss (only fetches main source). " +
                        "For deep analysis involving global variables/includes, PREFER 'extract_abap_components'.")
        public ObjectSourceResult get_object_source(
                        @McpToolParam(description = "ADT URI of the object. " +
                                        "Obtain from search_objects results. " +
                                        "Format examples: " +
                                        "'/sap/bc/adt/oo/classes/{name}/source/main' (class), " +
                                        "'/sap/bc/adt/programs/programs/{name}/source/main' (program), " +
                                        "'/sap/bc/adt/functions/groups/{name}/source/main' (function group)", required = true) String objectUri,
                        @McpToolParam(description = "Version to retrieve: 'active' for activated code or 'inactive' for draft. "
                                        +
                                        "Default: 'active'", required = false) String version) {
                return objectService.getObjectSource(objectUri, version);
        }
}
