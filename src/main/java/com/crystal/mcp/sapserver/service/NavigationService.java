package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.PackageObjectsResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for SAP package and navigation operations.
 *
 * This service handles operations related to SAP package exploration
 * and repository navigation.
 *
 * Progressive Discovery Integration:
 * - Use to explore package contents
 * - Find objects by type, author, or creation date
 * - Supports pagination for large packages
 *
 * Thread Safety: Stateless service, thread-safe via RfcAdapter.
 *
 * Supported operations:
 * - Get objects from a package (with filtering and pagination)
 *
 * Future operations:
 * - Get package hierarchy
 * - Get package interfaces
 * - Search across packages
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NavigationService {

    private final RfcAdapter rfcAdapter;
    private final ObjectMapper objectMapper;

    /**
     * Get ABAP objects from a package with pagination and filtering.
     *
     * This method retrieves objects contained in a development package
     * by querying the TADIR table (Repository Object Directory).
     *
     * Progressive Discovery Integration:
     * - Use to explore package contents
     * - Find objects by type (CLAS, PROG, FUGR, etc.)
     * - Filter by author or creation date
     * - Paginate through large packages
     *
     * TADIR Fields Retrieved:
     * - PGMID: Program ID (e.g., 'R3TR' for repository objects)
     * - OBJECT: Object type (CLAS, PROG, FUGR, TABL, etc.)
     * - OBJ_NAME: Object name
     * - SRCSYSTEM: Source system
     * - AUTHOR: Author/creator
     * - DEVCLASS: Development class (package)
     * - CREATED_ON: Creation date
     * - CHECK_DATE: Last verification date
     *
     * Pagination:
     * - First page: offset=0, maxRows=50 (default)
     * - Second page: offset=50, maxRows=50
     * - Check pagination.hasMore to continue
     *
     * Filters:
     * - objectTypes: ["CLAS", "PROG"] → Only classes and programs
     * - author: "DEVELOPER" → Objects created by DEVELOPER
     * - createdFrom/createdTo: Date range filter
     *
     * Workflow Example:
     * 1. User: "What's in package ZMMI1229_0?"
     * 2. Claude: get_package_objects("ZMMI1229_0") → Gets first 50 objects
     * 3. User: "Show me just the classes"
     * 4. Claude: get_package_objects("ZMMI1229_0", objectTypes=["CLAS"])
     *
     * Reference: python-legacy/app/services/navigation_service.py:104-323
     *
     * @param packageName   package/devclass name (e.g., "ZMMI1229_0", "$TMP")
     * @param maxRows       maximum objects per page (default: 50, max: 1000)
     * @param offset        number of objects to skip for pagination (default: 0)
     * @param objectTypes   optional list of object types to filter
     * @param author        optional author filter
     * @param createdFrom   optional start date filter (YYYY-MM-DD)
     * @param createdTo     optional end date filter (YYYY-MM-DD)
     * @return PackageObjectsResult containing objects and pagination info
     * @throws RuntimeException if query fails
     */
    public PackageObjectsResult getPackageObjects(
            String packageName,
            Integer maxRows,
            Integer offset,
            List<String> objectTypes,
            String author,
            String createdFrom,
            String createdTo
    ) {
        // Validate inputs
        if (packageName == null || packageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Package name cannot be empty");
        }

        // Apply defaults
        int actualMaxRows = (maxRows != null && maxRows > 0) ? Math.min(maxRows, 1000) : 50;
        int actualOffset = (offset != null && offset >= 0) ? offset : 0;

        log.info("Getting package objects for: {} (maxRows: {}, offset: {}, " +
                        "objectTypes: {}, author: {}, createdFrom: {}, createdTo: {})",
                packageName, actualMaxRows, actualOffset, objectTypes, author, createdFrom, createdTo);

        try {
            // Build RFC parameters
            Map<String, String> params = new HashMap<>();
            params.put("IV_PACKAGE_NAME", packageName.toUpperCase());
            params.put("IV_MAX_ROWS", String.valueOf(actualMaxRows));
            params.put("IV_OFFSET", String.valueOf(actualOffset));

            // Object types filter (comma-separated)
            if (objectTypes != null && !objectTypes.isEmpty()) {
                params.put("IV_OBJECT_TYPES", String.join(",", objectTypes));
                log.debug("Filtering by object types: {}", objectTypes);
            }

            // Author filter
            if (author != null && !author.trim().isEmpty()) {
                params.put("IV_AUTHOR", author.trim().toUpperCase());
                log.debug("Filtering by author: {}", author);
            }

            // Date filters (convert YYYY-MM-DD to YYYYMMDD for SAP)
            if (createdFrom != null && !createdFrom.trim().isEmpty()) {
                String sapDateFrom = createdFrom.replace("-", "");
                params.put("IV_CREATED_FROM", sapDateFrom);
                log.debug("Filtering from date: {} (SAP: {})", createdFrom, sapDateFrom);
            }

            if (createdTo != null && !createdTo.trim().isEmpty()) {
                String sapDateTo = createdTo.replace("-", "");
                params.put("IV_CREATED_TO", sapDateTo);
                log.debug("Filtering to date: {} (SAP: {})", createdTo, sapDateTo);
            }

            // Call RFC
            RfcAdapter.RfcFunctionResponse response = rfcAdapter.callFunctionModule(
                    "Z_CX_GET_PACKAGE_OBJECTS",
                    params
            );

            // Get export parameters
            String success = response.getExportParam("EV_SUCCESS");
            String message = response.getExportParam("EV_MESSAGE");
            String objectsJson = response.getExportParam("EV_OBJECTS_JSON");
            String totalCountStr = response.getExportParam("EV_TOTAL_COUNT");
            String hasMoreFlag = response.getExportParam("EV_HAS_MORE");

            log.debug("FM response: success={}, message={}, totalCount={}", success, message, totalCountStr);

            if (!"X".equals(success)) {
                throw new RuntimeException("RFC call failed: " + message);
            }

            // Parse JSON response
            JsonNode jsonRoot = objectMapper.readTree(objectsJson);

            // Build filters applied map
            Map<String, String> filtersApplied = new HashMap<>();
            if (objectTypes != null && !objectTypes.isEmpty()) {
                filtersApplied.put("object_types", String.join(", ", objectTypes));
            }
            if (author != null) filtersApplied.put("author", author);
            if (createdFrom != null) filtersApplied.put("created_from", createdFrom);
            if (createdTo != null) filtersApplied.put("created_to", createdTo);

            // Parse objects from JSON and group by type
            boolean hasMore = "X".equals(hasMoreFlag);
            int totalCount = Integer.parseInt(totalCountStr != null ? totalCountStr : "0");

            PackageObjectsResult result = parseAndGroupObjects(
                    jsonRoot,
                    packageName,
                    actualMaxRows,
                    actualOffset,
                    hasMore,
                    totalCount,
                    filtersApplied
            );

            log.info("Retrieved {} objects from package '{}' ({} different types)",
                    result.returnedObjects(), packageName, result.objectTypes().size());

            return result;

        } catch (Exception e) {
            String errorMsg = String.format(
                    "Failed to get package objects for '%s': %s",
                    packageName,
                    e.getMessage()
            );
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Parse JSON response from RFC and group objects by type.
     *
     * @param jsonRoot       JSON root node from RFC response
     * @param packageName    Package name
     * @param maxRows        Page size
     * @param offset         Current offset
     * @param hasMore        Whether there are more pages
     * @param totalCount     Total count from RFC
     * @param filtersApplied Applied filters
     * @return PackageObjectsResult with grouped objects
     */
    private PackageObjectsResult parseAndGroupObjects(
            JsonNode jsonRoot,
            String packageName,
            int maxRows,
            int offset,
            boolean hasMore,
            int totalCount,
            Map<String, String> filtersApplied
    ) {
        // Group objects by type
        Map<String, PackageObjectsResult.ObjectTypeGroup> objectTypeGroups = new HashMap<>();

        JsonNode objectsArray = jsonRoot.get("objects");
        if (objectsArray != null && objectsArray.isArray()) {
            for (JsonNode objNode : objectsArray) {
                String objectType = getJsonText(objNode, "object");
                if (objectType == null || objectType.isEmpty()) {
                    continue;
                }

                // Create object info
                PackageObjectsResult.ObjectInfo objectInfo = new PackageObjectsResult.ObjectInfo(
                        getJsonText(objNode, "pgmid"),
                        objectType,
                        getJsonText(objNode, "obj_name"),
                        getJsonText(objNode, "srcsystem"),
                        getJsonText(objNode, "author"),
                        getJsonText(objNode, "devclass"),
                        formatSapDate(getJsonText(objNode, "created_on")),
                        null  // CHECK_DATE not included in RFC response
                );

                // Add to type group
                objectTypeGroups.computeIfAbsent(objectType, k ->
                        new PackageObjectsResult.ObjectTypeGroup(0, new ArrayList<>())
                ).objects().add(objectInfo);
            }
        }

        // Update counts for each type
        objectTypeGroups.forEach((type, group) ->
                objectTypeGroups.put(type,
                        new PackageObjectsResult.ObjectTypeGroup(group.objects().size(), group.objects()))
        );

        // Calculate pagination
        int currentPage = (offset / maxRows) + 1;
        int nextOffset = offset + maxRows;

        PackageObjectsResult.Pagination pagination = new PackageObjectsResult.Pagination(
                hasMore,
                hasMore ? nextOffset : offset,
                currentPage,
                maxRows,
                -1  // Total pages unknown (would require full COUNT query)
        );

        return new PackageObjectsResult(
                packageName,
                totalCount,   // Total objects returned by RFC
                totalCount,   // Returned objects (same as total for this page)
                objectTypeGroups,
                pagination,
                filtersApplied
        );
    }

    /**
     * Get text value from JSON node safely.
     *
     * @param node      JSON node
     * @param fieldName Field name
     * @return Text value or null
     */
    private String getJsonText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field != null && !field.isNull()) {
            return field.asText().trim();
        }
        return null;
    }

    /**
     * Format SAP date from YYYYMMDD to YYYY-MM-DD.
     *
     * @param sapDate SAP date in YYYYMMDD format
     * @return Formatted date in YYYY-MM-DD format, or original if invalid
     */
    private String formatSapDate(String sapDate) {
        if (sapDate == null || sapDate.length() != 8) {
            return sapDate;
        }

        try {
            return sapDate.substring(0, 4) + "-" +
                    sapDate.substring(4, 6) + "-" +
                    sapDate.substring(6, 8);
        } catch (Exception e) {
            log.warn("Failed to format date: {}", sapDate);
            return sapDate;
        }
    }
}
