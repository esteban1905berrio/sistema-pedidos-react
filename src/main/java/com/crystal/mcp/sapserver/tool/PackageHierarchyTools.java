package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.PackageHierarchyResult;
import com.crystal.mcp.sapserver.service.PackageHierarchyService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for SAP package hierarchy operations.
 *
 * <p>Provides tools to query SAP package parent-child relationships using TDEVC table.</p>
 *
 * <h2>Available Tools</h2>
 * <ul>
 *   <li><b>get_package_hierarchy</b>: Get package children or parents (bidirectional query)</li>
 * </ul>
 *
 * <h2>Usage Patterns</h2>
 * <pre>
 * Progressive Discovery Stage 0: Find related packages before listing objects
 *
 * Use before get_package_objects to understand package structure:
 * 1. get_package_hierarchy('ZCX', 'C', false) → direct children of ZCX
 * 2. get_package_objects('ZCXENH') → objects in child package
 * </pre>
 *
 * @see PackageHierarchyService
 */
@Component
@RequiredArgsConstructor
public class PackageHierarchyTools {

    private final PackageHierarchyService packageHierarchyService;

    /**
     * Get SAP package hierarchy (children or parents).
     *
     * <p><b>Progressive Discovery Stage 0</b>: Find related packages before listing objects.
     * Use before get_package_objects to understand package structure.</p>
     *
     * <h3>Mode 'C' (children)</h3>
     * Get subpackages of a parent package:
     * <ul>
     *   <li>get_package_hierarchy('ZCX', 'C', false) → direct children of ZCX</li>
     *   <li>get_package_hierarchy('ZCX', 'C', true) → all descendants of ZCX (recursive)</li>
     * </ul>
     *
     * <h3>Mode 'P' (parents)</h3>
     * Get parent packages of a child package:
     * <ul>
     *   <li>get_package_hierarchy('ZCXR1003', 'P', false) → direct parent</li>
     *   <li>get_package_hierarchy('ZCXR1003', 'P', true) → all ancestors (recursive)</li>
     * </ul>
     *
     * <h3>Response Format</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "mode": "children",
     *   "recursive": false,
     *   "packageName": "ZCX",
     *   "hierarchy": [
     *     {
     *       "packageName": "ZCXENH",
     *       "parentPackage": "ZCX",
     *       "description": "Enhancements Package",
     *       "level": 1,
     *       "hasChildren": true
     *     }
     *   ],
     *   "totalPackages": 1
     * }
     * }</pre>
     *
     * @param packageName Package name to query (e.g., 'ZCX', 'ZCXR1003', '$TMP')
     * @param mode Query mode: 'C' for children (default), 'P' for parents
     * @param recursive Recursive search: false for direct level only (default), true for all levels
     * @return JSON string with package hierarchy
     */
    @McpTool(
        description = """
            Get SAP package hierarchy (children or parents).

            Progressive Discovery Stage 0: Find related packages before listing objects.
            Use before get_package_objects to understand package structure.

            Mode 'C' (children): Get subpackages of a parent package.
            Mode 'P' (parents): Get parent packages of a child package.

            Examples:
            - get_package_hierarchy('ZCX', 'C', false) -> direct children of ZCX
            - get_package_hierarchy('ZCX', 'C', true) -> all descendants of ZCX (recursive)
            - get_package_hierarchy('ZCXR1003', 'P', false) -> direct parent
            - get_package_hierarchy('ZCXR1003', 'P', true) -> all ancestors (recursive)
            """
    )
    public String getPackageHierarchy(
            @McpToolParam(description = "Package name to query. Examples: 'ZCX', 'ZCXR1003', '$TMP'")
            String packageName,
            @McpToolParam(description = "Query mode: 'C' for children (default), 'P' for parents", required = false)
            String mode,
            @McpToolParam(description = "Recursive search: false for direct level only (default), true for all levels", required = false)
            Boolean recursive) {

        try {
            String queryMode = (mode != null) ? mode : "C";
            boolean isRecursive = (recursive != null) ? recursive : false;

            PackageHierarchyResult result = packageHierarchyService.getPackageHierarchy(
                packageName,
                queryMode,
                isRecursive
            );

            if (!result.success()) {
                return String.format("{\"success\":false,\"error\":\"%s\"}",
                    result.message());
            }

            return result.hierarchy().toString();

        } catch (Exception e) {
            return String.format("{\"success\":false,\"error\":\"%s\"}",
                e.getMessage());
        }
    }
}
