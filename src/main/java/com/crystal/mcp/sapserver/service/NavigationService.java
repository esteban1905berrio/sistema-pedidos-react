package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.PackageObjectsResult;
import com.crystal.mcp.sapserver.model.TableContentsResult;
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
    private final QueryService queryService;

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
            // Build WHERE clause with filters
            List<String> whereConditions = new ArrayList<>();
            whereConditions.add("DEVCLASS = '" + packageName + "'");

            // Filter by object types
            if (objectTypes != null && !objectTypes.isEmpty()) {
                String typesList = objectTypes.stream()
                        .map(t -> "'" + t + "'")
                        .collect(java.util.stream.Collectors.joining(", "));
                whereConditions.add("OBJECT IN (" + typesList + ")");
                log.debug("Filtering by object types: {}", objectTypes);
            }

            // Filter by author
            if (author != null && !author.trim().isEmpty()) {
                whereConditions.add("AUTHOR = '" + author.trim() + "'");
                log.debug("Filtering by author: {}", author);
            }

            // Filter by creation date range
            if (createdFrom != null && !createdFrom.trim().isEmpty()) {
                // Convert YYYY-MM-DD to SAP format YYYYMMDD
                String sapDateFrom = createdFrom.replace("-", "");
                whereConditions.add("CREATED_ON >= '" + sapDateFrom + "'");
                log.debug("Filtering from date: {} (SAP: {})", createdFrom, sapDateFrom);
            }

            if (createdTo != null && !createdTo.trim().isEmpty()) {
                // Convert YYYY-MM-DD to SAP format YYYYMMDD
                String sapDateTo = createdTo.replace("-", "");
                whereConditions.add("CREATED_ON <= '" + sapDateTo + "'");
                log.debug("Filtering to date: {} (SAP: {})", createdTo, sapDateTo);
            }

            // Combine all conditions with AND
            String whereClause = String.join(" AND ", whereConditions);
            log.debug("Final WHERE clause: {}", whereClause);

            // Define fields to retrieve from TADIR
            List<String> fields = List.of(
                    "PGMID",
                    "OBJECT",
                    "OBJ_NAME",
                    "SRCSYSTEM",
                    "AUTHOR",
                    "DEVCLASS",
                    "CREATED_ON",
                    "CHECK_DATE"
            );

            // Query TADIR table with pagination
            // Note: We add +1 to maxRows to detect if there are more pages
            TableContentsResult tableData = queryService.getTableContents(
                    "TADIR",
                    whereClause,
                    actualMaxRows + 1,  // Request one extra row to check for more
                    fields
            );

            // Parse and group results
            Map<String, String> filtersApplied = new HashMap<>();
            if (objectTypes != null && !objectTypes.isEmpty()) {
                filtersApplied.put("object_types", String.join(", ", objectTypes));
            }
            if (author != null) filtersApplied.put("author", author);
            if (createdFrom != null) filtersApplied.put("created_from", createdFrom);
            if (createdTo != null) filtersApplied.put("created_to", createdTo);

            // Check if we got more rows than requested (indicates more pages)
            List<Map<String, String>> rows = tableData.rows();
            boolean hasMore = rows.size() > actualMaxRows;

            // If we got extra row, remove it from results
            if (hasMore) {
                rows = rows.subList(0, actualMaxRows);
            }

            // Group objects by type
            PackageObjectsResult result = groupPackageObjects(
                    rows,
                    packageName,
                    actualMaxRows,
                    actualOffset,
                    hasMore,
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
     * Group TADIR rows by object type.
     *
     * @param rows           List of TADIR rows
     * @param packageName    Package name
     * @param maxRows        Page size
     * @param offset         Current offset
     * @param hasMore        Whether there are more pages
     * @param filtersApplied Applied filters
     * @return PackageObjectsResult with grouped objects
     */
    private PackageObjectsResult groupPackageObjects(
            List<Map<String, String>> rows,
            String packageName,
            int maxRows,
            int offset,
            boolean hasMore,
            Map<String, String> filtersApplied
    ) {
        // Group objects by type
        Map<String, PackageObjectsResult.ObjectTypeGroup> objectTypeGroups = new HashMap<>();

        for (Map<String, String> row : rows) {
            String objectType = row.get("OBJECT");
            if (objectType == null || objectType.isEmpty()) {
                continue;
            }

            // Create object info
            PackageObjectsResult.ObjectInfo objectInfo = new PackageObjectsResult.ObjectInfo(
                    row.get("PGMID"),
                    objectType,
                    row.get("OBJ_NAME"),
                    row.get("SRCSYSTEM"),
                    row.get("AUTHOR"),
                    row.get("DEVCLASS"),
                    formatSapDate(row.get("CREATED_ON")),
                    formatSapDate(row.get("CHECK_DATE"))
            );

            // Add to type group
            objectTypeGroups.computeIfAbsent(objectType, k ->
                    new PackageObjectsResult.ObjectTypeGroup(0, new ArrayList<>())
            ).objects().add(objectInfo);
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
                rows.size(),  // Total objects in current page
                rows.size(),  // Returned objects (same as total for this page)
                objectTypeGroups,
                pagination,
                filtersApplied
        );
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
