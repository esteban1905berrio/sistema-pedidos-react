package com.crystal.mcp.sapserver.resource;

import com.crystal.mcp.sapserver.model.PackageHierarchyResult;
import com.crystal.mcp.sapserver.model.PackageObjectsResult;
import com.crystal.mcp.sapserver.service.NavigationService;
import com.crystal.mcp.sapserver.service.PackageHierarchyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP Resource Provider for SAP Packages.
 *
 * Exposes SAP package information as read-only MCP Resources using URI templates.
 * Resources are lightweight alternatives to Tools for data that:
 * - Is read-only (no side effects)
 * - Can be cached by clients
 * - Follows a predictable URI structure
 *
 * URI Template Pattern: sap://package/{name}/{aspect}
 *
 * Implemented Resources:
 * - sap://package/{name}/objects   - Objects in the package (TADIR)
 * - sap://package/{name}/hierarchy - Package hierarchy (children)
 *
 * Token Optimization:
 * - objects: ~500-2000 tokens (depends on object count)
 * - hierarchy: ~300-800 tokens (depends on subpackage count)
 *
 * Usage Example:
 * Client requests: resources/read { uri: "sap://package/ZCX/objects" }
 * Response: JSON with list of objects grouped by type
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PackageResourceProvider {

    private final NavigationService navigationService;
    private final PackageHierarchyService packageHierarchyService;
    private final ObjectMapper objectMapper;

    /**
     * Get objects in a package.
     *
     * Returns ABAP objects contained in the development package
     * from TADIR table, grouped by object type.
     *
     * Returned data:
     * - objectTypes: Objects grouped by type (CLAS, PROG, etc.)
     * - Each object: pgmid, objectType, objName, author, createdOn
     * - Pagination info included
     *
     * MIME Type: application/json
     *
     * Token Optimization: ~500-2000 tokens (depends on object count)
     *
     * Example URI: sap://package/ZCX/objects
     *
     * @param name package name (e.g., "ZCX", "$TMP", "ZFINANCE")
     * @return ReadResourceResult with package objects
     */
    @McpResource(
            uri = "sap://package/{name}/objects",
            name = "Package Objects",
            description = "List of ABAP objects in package grouped by type. Token-optimized: ~500-2000 tokens",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult getPackageObjects(String name) {
        log.info("Resource request: sap://package/{}/objects", name);

        // Get first page of objects (default 50)
        PackageObjectsResult result = navigationService.getPackageObjects(
                name, 50, 0, null, null, null, null
        );

        // Convert to lightweight JSON structure
        List<ObjectSummary> objects = new ArrayList<>();
        for (Map.Entry<String, PackageObjectsResult.ObjectTypeGroup> entry : result.objectTypes().entrySet()) {
            for (PackageObjectsResult.ObjectInfo obj : entry.getValue().objects()) {
                objects.add(new ObjectSummary(
                        obj.pgmid(),
                        obj.objectType(),
                        obj.objName(),
                        obj.author(),
                        obj.createdOn()
                ));
            }
        }

        PackageObjectsData data = new PackageObjectsData(
                result.packageName(),
                result.totalObjects(),
                result.returnedObjects(),
                objects,
                result.pagination().hasMore(),
                result.pagination().nextOffset()
        );

        String jsonContent = serializeToJson(data);

        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(
                        "sap://package/" + name + "/objects",
                        "application/json",
                        jsonContent
                )
        ));
    }

    /**
     * Get package hierarchy (subpackages).
     *
     * Returns the child packages of a parent package
     * from TDEVC table (Package Directory).
     *
     * Returned data:
     * - packageName: Parent package
     * - hierarchy: List of child packages with descriptions
     * - Each child: packageName, parentPackage, description, level, hasChildren
     *
     * MIME Type: application/json
     *
     * Token Optimization: ~300-800 tokens (depends on subpackage count)
     *
     * Example URI: sap://package/ZCX/hierarchy
     *
     * @param name package name (e.g., "ZCX", "ZFINANCE")
     * @return ReadResourceResult with package hierarchy
     */
    @McpResource(
            uri = "sap://package/{name}/hierarchy",
            name = "Package Hierarchy",
            description = "Subpackages of a parent package from TDEVC. Token-optimized: ~300-800 tokens",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult getPackageHierarchy(String name) {
        log.info("Resource request: sap://package/{}/hierarchy", name);

        try {
            // Get children (mode = 'C'), non-recursive
            PackageHierarchyResult result = packageHierarchyService.getPackageHierarchy(
                    name, "C", false
            );

            if (!result.success()) {
                String errorJson = "{\"error\": \"" + result.message() + "\", \"packageName\": \"" + name + "\"}";
                return new McpSchema.ReadResourceResult(List.of(
                        new McpSchema.TextResourceContents(
                                "sap://package/" + name + "/hierarchy",
                                "application/json",
                                errorJson
                        )
                ));
            }

            // The hierarchy is already a JsonNode, serialize it directly
            String jsonContent = objectMapper.writeValueAsString(result.hierarchy());

            return new McpSchema.ReadResourceResult(List.of(
                    new McpSchema.TextResourceContents(
                            "sap://package/" + name + "/hierarchy",
                            "application/json",
                            jsonContent
                    )
            ));

        } catch (Exception e) {
            log.error("Error getting package hierarchy: {}", e.getMessage(), e);
            String errorJson = "{\"error\": \"" + e.getMessage() + "\", \"packageName\": \"" + name + "\"}";
            return new McpSchema.ReadResourceResult(List.of(
                    new McpSchema.TextResourceContents(
                            "sap://package/" + name + "/hierarchy",
                            "application/json",
                            errorJson
                    )
            ));
        }
    }

    // ========================================================================
    // Helper DTOs
    // ========================================================================

    /**
     * Lightweight DTO for package object summary.
     */
    private record ObjectSummary(
            String pgmid,
            String objectType,
            String objName,
            String author,
            String createdOn
    ) {}

    /**
     * Container DTO for package objects response.
     */
    private record PackageObjectsData(
            String packageName,
            int totalObjects,
            int returnedObjects,
            List<ObjectSummary> objects,
            boolean hasMore,
            int nextOffset
    ) {}

    /**
     * Serialize object to JSON string.
     */
    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("JSON serialization failed", e);
        }
    }
}
