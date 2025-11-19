package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.ObjectInOpenOTResult;
import com.crystal.mcp.sapserver.model.TransportInfoListResult;
import com.crystal.mcp.sapserver.model.TransportInfoResult;
import com.crystal.mcp.sapserver.model.TransportListResult;
import com.crystal.mcp.sapserver.model.TransportObjectsResult;
import com.crystal.mcp.sapserver.service.TransportService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for SAP Transport System Operations.
 *
 * This component provides tools for working with SAP CTS (Change and Transport System).
 * Part of Progressive Discovery architecture.
 *
 * Spring AI MCP Server automatically discovers and registers @McpTool methods.
 *
 * Progressive Discovery Workflow:
 * Stage 1: list_user_transports → Find available transports
 * Stage 2: get_transport_info → Get metadata (lightweight)
 * Stage 3: get_transport_objects → Get detailed object list (if needed)
 *
 * Available Tools:
 * - list_user_transports: List transport requests for a user
 * - get_transport_info: Get transport metadata without objects (NEW)
 * - get_transport_objects: Get objects from a transport
 * - get_object_in_open_ot: Check if object is in open transport
 *
 * Future Tools:
 * - create_transport: Create new transport request
 * - add_to_transport: Add objects to transport
 * - release_transport: Release transport
 */
@Component
@RequiredArgsConstructor
public class TransportTools {

    private final TransportService transportService;

    /**
     * MCP Tool: List transport requests for a user.
     *
     * This tool retrieves transport requests from the SAP CTS system.
     * Returns lightweight list of transports without detailed objects.
     *
     * Token Optimization:
     * - Stage 1 (list_user_transports): ~500 tokens → Find transports
     * - Stage 2 (get_transport_objects): ~2,000+ tokens → Get details
     *
     * Use Case:
     * Use this tool to:
     * - Find your own transports
     * - Find transports for another user
     * - Filter by status (modifiable vs released)
     * - Identify transport numbers for detailed queries
     *
     * Status Values:
     * - D: Modifiable (development in progress)
     * - R: Released (ready for QA/Production)
     * - (empty): All statuses
     *
     * Workflow Example:
     * 1. User: "What transports do I have?"
     * 2. Claude: list_user_transports() → Gets user's transports
     * 3. User: "Show me only released ones"
     * 4. Claude: list_user_transports(status="R") → Filters released
     * 5. User: "What's in DEVK900123?"
     * 6. Claude: get_transport_objects("DEVK900123") → Gets details
     *
     * @param user   user ID (null for current user)
     * @param status status filter: "D" (modifiable), "R" (released), null (all)
     * @return TransportListResult containing list of transports
     */
    @McpTool(
            description = "List transport requests for a user in SAP CTS system. " +
                    "Progressive Discovery Stage 1: Find available transports. " +
                    "Returns lightweight list without object details. " +
                    "Use get_transport_objects to fetch detailed objects. " +
                    "Token cost: ~500 tokens. " +
                    "Status values: 'D' (modifiable), 'R' (released), null (all)"
    )
    public TransportListResult list_user_transports(
            @McpToolParam(
                    description = "User ID to list transports for. " +
                            "Leave empty for current user. " +
                            "Examples: 'DEVELOPER', 'BASIS_USER'",
                    required = false
            )
            String user,
            @McpToolParam(
                    description = "Status filter: 'D' for modifiable, 'R' for released, null for all. " +
                            "Default: null (all statuses)",
                    required = false
            )
            String status
    ) {
        return transportService.listUserTransports(user, status);
    }

    /**
     * MCP Tool: Get objects from a transport request.
     *
     * This tool retrieves detailed information about objects contained
     * in a transport request, including metadata and complete object list.
     *
     * NOTE: This is a placeholder implementation for Phase 1.
     * Full implementation requires direct RFC calls to E070/E071 tables.
     *
     * Token Optimization:
     * - More expensive than list_user_transports
     * - Returns complete object list and metadata
     * - Typical: ~2,000+ tokens (depends on object count)
     *
     * Use Case:
     * After list_user_transports identifies a transport, use this to:
     * - See complete list of objects in transport
     * - Check transport metadata (owner, status, dates)
     * - Identify tasks within main transport
     * - Verify what will be transported
     *
     * Workflow Example:
     * 1. User: "Show me what's in DEVK900123"
     * 2. Claude: get_transport_objects("DEVK900123") → Gets full details
     *
     * @param transportNumber transport request number (e.g., "DEVK900123")
     * @param taskNumber      optional task number to filter (for main transports)
     * @return TransportObjectsResult containing objects and metadata
     */
    @McpTool(
            description = "Get objects from a transport request in SAP CTS system. " +
                    "Progressive Discovery Stage 2: Get detailed object list. " +
                    "Returns complete object list and metadata. " +
                    "Token cost: ~2,000+ tokens (depends on object count). " +
                    "NOTE: Phase 1 placeholder - full implementation requires RFC table access. " +
                    "Example transport numbers: 'DEVK900123', 'CADK911088'"
    )
    public TransportObjectsResult get_transport_objects(
            @McpToolParam(
                    description = "Transport request number. " +
                            "Examples: 'DEVK900123' (main transport), 'DEVK900124' (task)",
                    required = true
            )
            String transportNumber,
            @McpToolParam(
                    description = "Optional task number to filter objects (when querying main transport). " +
                            "Leave empty to get all objects.",
                    required = false
            )
            String taskNumber
    ) {
        return transportService.getTransportObjects(transportNumber, taskNumber);
    }

    /**
     * MCP Tool: Get transport request metadata without loading objects.
     *
     * This tool retrieves complete metadata for a transport request
     * from E070 table without loading the full object list. Use this
     * when you need transport information but don't need to see all objects.
     *
     * Use Cases:
     * - "Who owns transport DEVK900123?" → Returns owner, status
     * - "Is transport CADK911088 released?" → Returns status
     * - "When was transport created?" → Returns dates
     * - "What's the description of this transport?" → Returns description
     * - Quick transport lookups before detailed queries
     *
     * Token Optimization:
     * - Lightweight: ~500-800 tokens (much cheaper than get_transport_objects)
     * - Progressive Discovery: Use this first, then get_transport_objects if needed
     * - Ideal for quick metadata checks
     *
     * Workflow Example:
     * 1. User: "Is transport DEVK900123 ready to release?"
     * 2. Claude: get_transport_info("DEVK900123") → Checks status
     * 3. Result: status="D" (Modifiable), has_objects=true, has_tasks=true
     * 4. Claude: "Transport is modifiable, not yet released"
     *
     * Progressive Discovery Integration:
     * - Stage 1: list_user_transports → Find transports
     * - Stage 2: get_transport_info → Get metadata
     * - Stage 3: get_transport_objects → Get full object list (if needed)
     *
     * Returned Fields:
     * - transport_number: OT number
     * - transport_type: K (Workbench), S (Task), T (Transport of Copies)
     * - status: D (Modifiable), R (Released), L (Protected)
     * - owner: User who created the transport
     * - description: Transport description text
     * - created_date: YYYY-MM-DD format
     * - created_time: HH:MM:SS format
     * - target_system: Destination system (e.g., S4Q, S4P)
     * - parent_transport: Parent OT number (for tasks)
     * - has_objects: Boolean indicating if transport contains objects
     * - has_tasks: Boolean indicating if transport has tasks
     *
     * @param transportNumber Transport request number (main OT or task)
     *                        Examples: "CADK911088", "DEVK900123"
     * @return TransportInfoResult with complete metadata
     */
    @McpTool(
            description = "Get transport request metadata without loading objects. " +
                    "Retrieves complete metadata from E070 table: owner, status, dates, description. " +
                    "Lightweight alternative to get_transport_objects. " +
                    "Token cost: ~500-800 tokens (much cheaper than full object list). " +
                    "Use when you need metadata only. For full object details, use get_transport_objects. " +
                    "Progressive Discovery: list_user_transports → get_transport_info → get_transport_objects"
    )
    public TransportInfoListResult get_transport_info(
            @McpToolParam(
                    description = "Transport request number (main OT or task). " +
                            "Examples: 'CADK911088', 'DEVK900123', 'S4DK932806'",
                    required = true
            )
            String transportNumber
    ) {
        return transportService.getTransportInfo(transportNumber);
    }

    /**
     * MCP Tool: Check if an ABAP object is in open (non-released) transport requests.
     *
     * This tool queries E071 and E070 tables to determine if an ABAP object is:
     * - In a transport request that is not yet released
     * - Locked by another developer (LOCKFLAG = 'X')
     * - Available for modification
     *
     * Use Cases:
     * - "Can I modify this object?" → Check if it's in an open transport
     * - "Who has this object locked?" → See owner and lock status
     * - "Which transport contains this object?" → List all open transports
     * - Development workflow: Check before attempting to edit objects
     *
     * Token Optimization:
     * - Typical cost: ~1,000-2,000 tokens (depends on results)
     * - More efficient than checking transport lists manually
     * - Use after search_objects to verify editability
     *
     * Workflow Example:
     * 1. User: "Can I edit ZCL_INVOICE?"
     * 2. Claude: get_object_in_open_ot("ZCL_INVOICE") → Checks status
     * 3. Result shows:
     *    - Transport: DEVK900123 (Modifiable, owner: USER01)
     *    - Object: ZCL_INVOICE (locked: yes)
     * 4. Claude: "Object is in transport DEVK900123, locked by USER01"
     *
     * Progressive Discovery Integration:
     * - Stage 1: search_objects("ZCL_*") → Find objects
     * - Stage 2: get_object_in_open_ot("ZCL_INVOICE") → Check transport status
     * - Stage 3: get_class_source("ZCL_INVOICE") → Read code (if not locked)
     *
     * Filters:
     * - Only returns OPEN transports (TRSTATUS = 'D' or 'L')
     * - Released transports (TRSTATUS = 'R') are excluded
     * - Supports object type filtering (CLAS, PROG, FUGR, etc.)
     *
     * Search Pattern:
     * - Searches using LIKE '%<objectName>%'
     * - Case-insensitive matching
     * - Supports partial names: "INVOICE" finds "ZCL_INVOICE", "ZFI_INVOICE_PROC"
     *
     * Reference: python-legacy/app/mcp/tools/transport_tools.py
     *
     * @param objectName Object name or pattern to search
     *                   Examples: "ZCL_TEST", "INVOICE", "ZREP_001"
     * @param objectType Optional object type filter: 'CLAS', 'PROG', 'FUGR', 'TABL', etc.
     *                   Leave empty to search all types
     * @return ObjectInOpenOTResult with list of open transports containing the object
     */
    @McpTool(
            description = "Check if an ABAP object is in an open (non-released) transport request. " +
                    "Queries E071 and E070 tables to find objects and their transport status. " +
                    "Returns only transports with status 'D' (Modifiable) or 'L' (Protected). " +
                    "Shows if object is locked (LOCKFLAG = 'X') and by whom. " +
                    "Token cost: ~1,000-2,000 tokens (depends on results). " +
                    "Use Case: 'Can I modify this object? Who has it locked?' " +
                    "Examples: 'ZCL_TEST', 'INVOICE' (finds ZCL_INVOICE, ZFI_INVOICE), 'ZREP_%'"
    )
    public ObjectInOpenOTResult get_object_in_open_ot(
            @McpToolParam(
                    description = "Object name or pattern to search. Supports partial matching: " +
                            "'ZCL_TEST' (finds ZCL_TEST), 'INVOICE' (finds all with INVOICE in name). " +
                            "Search is case-insensitive and uses LIKE '%<name>%' operator.",
                    required = true
            )
            String objectName,

            @McpToolParam(
                    description = "Optional object type filter to narrow search: " +
                            "'CLAS' (classes), 'PROG' (programs), 'FUGR' (function groups), " +
                            "'TABL' (tables), 'DTEL' (data elements), 'INTF' (interfaces). " +
                            "Leave empty to search all types.",
                    required = false
            )
            String objectType
    ) {
        return transportService.getObjectInOpenOT(objectName, objectType);
    }
}
