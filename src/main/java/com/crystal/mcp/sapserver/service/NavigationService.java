package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.PackageObjectsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    /**
     * Get ABAP objects from a package with pagination and filtering.
     *
     * This method retrieves objects contained in a development package
     * by querying the TADIR table (Repository Object Directory).
     *
     * NOTE: This is a simplified implementation for Phase 1.
     * Full implementation requires direct RFC calls to TADIR table with
     * SELECT statements, which will be added in Phase 2.
     *
     * Progressive Discovery Integration:
     * - Use to explore package contents
     * - Find objects by type (CLAS, PROG, FUGR, etc.)
     * - Filter by author or creation date
     * - Paginate through large packages
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

        // TODO: Full implementation requires RFC call to TADIR table with SELECT
        // For now, return a placeholder structure
        log.warn("get_package_objects: Full implementation pending (requires RFC table access)");

        Map<String, String> filters = new HashMap<>();
        if (author != null) filters.put("author", author);
        if (createdFrom != null) filters.put("created_from", createdFrom);
        if (createdTo != null) filters.put("created_to", createdTo);
        if (objectTypes != null && !objectTypes.isEmpty()) {
            filters.put("object_types", String.join(", ", objectTypes));
        }

        PackageObjectsResult.Pagination pagination = new PackageObjectsResult.Pagination(
                false,  // hasMore
                0,      // nextOffset
                1,      // currentPage
                actualMaxRows,
                1       // totalPages
        );

        return new PackageObjectsResult(
                packageName,
                0,  // totalObjects
                0,  // returnedObjects
                new HashMap<>(),  // objectTypes (empty)
                pagination,
                filters
        );
    }
}
