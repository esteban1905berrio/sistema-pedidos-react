package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.PackageObjectsResult;
import com.crystal.mcp.sapserver.service.NavigationService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP Tools for SAP Package Navigation Operations.
 *
 * This component provides tools for exploring SAP packages and
 * navigating the repository structure.
 *
 * Spring AI MCP Server automatically discovers and registers @McpTool methods.
 *
 * Progressive Discovery Integration:
 * - Use to explore package contents
 * - Find objects by type, author, or creation date
 * - Supports pagination for large packages
 *
 * Phase 1 Tools:
 * - get_package_objects: Get objects from a package (placeholder)
 *
 * Future Tools:
 * - get_package_hierarchy: Get package structure
 * - get_package_interfaces: Get package interfaces
 * - search_packages: Search across packages
 */
@Component
@RequiredArgsConstructor
public class NavigationTools {

    private final NavigationService navigationService;

    /**
     * MCP Tool: Get ABAP objects from a package.
     *
     * This tool retrieves objects contained in a development package
     * with support for pagination and filtering.
     *
     * NOTE: This is a placeholder implementation for Phase 1.
     * Full implementation requires direct RFC calls to TADIR table.
     *
     * Token Optimization:
     * - Pagination reduces token usage for large packages
     * - Filtering by type reduces irrelevant objects
     * - Typical: ~1,000-5,000 tokens (depends on page size)
     *
     * Use Case:
     * Use this tool to:
     * - Explore package contents
     * - Find objects by type (CLAS, PROG, FUGR, etc.)
     * - Filter by author or creation date
     * - Navigate large packages with pagination
     *
     * Object Types:
     * - CLAS: Classes
     * - PROG: Programs
     * - FUGR: Function Groups
     * - TABL: Tables
     * - DTEL: Data Elements
     * - DOMA: Domains
     * - And many more...
     *
     * Pagination:
     * - First page: offset=0, maxRows=50 (default)
     * - Second page: offset=50, maxRows=50
     * - Check pagination.hasMore to continue
     *
     * Workflow Example:
     * 1. User: "What's in package ZMMI1229_0?"
     * 2. Claude: get_package_objects("ZMMI1229_0") → Gets first 50 objects
     * 3. User: "Show me just the classes"
     * 4. Claude: get_package_objects("ZMMI1229_0", objectTypes=["CLAS"])
     * 5. User: "What about the next page?"
     * 6. Claude: get_package_objects("ZMMI1229_0", offset=50)
     *
     * @param packageName   package/devclass name (e.g., "ZMMI1229_0", "$TMP")
     * @param maxRows       maximum objects per page (default: 50, max: 1000)
     * @param offset        number of objects to skip for pagination (default: 0)
     * @param objectTypes   optional list of object types to filter
     * @param author        optional author filter
     * @param createdFrom   optional start date filter (YYYY-MM-DD)
     * @param createdTo     optional end date filter (YYYY-MM-DD)
     * @return PackageObjectsResult containing objects and pagination info
     */
    @McpTool(
            description = "Get ABAP objects from a development package with pagination and filtering. " +
                    "Retrieves objects from TADIR table grouped by type. " +
                    "Supports filtering by object types, author, and creation date. " +
                    "Token cost: ~1,000-5,000 tokens (depends on page size). " +
                    "NOTE: Phase 1 placeholder - full implementation requires RFC table access. " +
                    "Example packages: 'ZMMI1229_0', '$TMP', 'Z_CUSTOM'"
    )
    public PackageObjectsResult get_package_objects(
            @McpToolParam(
                    description = "Package/devclass name. " +
                            "Examples: 'ZMMI1229_0', '$TMP' (local objects), 'Z_CUSTOM'",
                    required = true
            )
            String packageName,
            @McpToolParam(
                    description = "Maximum objects per page (default: 50, max: 1000). " +
                            "Use smaller values for faster responses.",
                    required = false
            )
            Integer maxRows,
            @McpToolParam(
                    description = "Number of objects to skip for pagination (default: 0). " +
                            "For second page, use offset=50 (assuming maxRows=50).",
                    required = false
            )
            Integer offset,
            @McpToolParam(
                    description = "Optional list of object types to filter. " +
                            "Examples: ['CLAS', 'PROG'], ['FUGR'], ['TABL', 'DTEL']. " +
                            "Leave empty for all types.",
                    required = false
            )
            List<String> objectTypes,
            @McpToolParam(
                    description = "Optional author filter. " +
                            "Examples: 'DEVELOPER', 'BASIS_USER'. " +
                            "Leave empty for all authors.",
                    required = false
            )
            String author,
            @McpToolParam(
                    description = "Optional start date filter in YYYY-MM-DD format. " +
                            "Example: '2025-01-01'. " +
                            "Leave empty for no date filter.",
                    required = false
            )
            String createdFrom,
            @McpToolParam(
                    description = "Optional end date filter in YYYY-MM-DD format. " +
                            "Example: '2025-12-31'. " +
                            "Leave empty for no date filter.",
                    required = false
            )
            String createdTo
    ) {
        return navigationService.getPackageObjects(
                packageName,
                maxRows,
                offset,
                objectTypes,
                author,
                createdFrom,
                createdTo
        );
    }
}
