package com.crystal.mcp.sapserver.model;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for package objects.
 *
 * This record represents ABAP objects contained in a development package,
 * retrieved from the TADIR table (Repository Object Directory).
 *
 * Progressive Discovery Integration:
 * Use to explore packages and find objects by type, author, or creation date.
 * Supports pagination for large packages.
 *
 * @param packageName      package/devclass name
 * @param totalObjects     total objects in package (with filters applied)
 * @param returnedObjects  objects returned in this page
 * @param objectTypes      objects grouped by type
 * @param pagination       pagination metadata
 * @param filters          filters applied (if any)
 */
public record PackageObjectsResult(
        String packageName,
        int totalObjects,
        int returnedObjects,
        Map<String, ObjectTypeGroup> objectTypes,
        Pagination pagination,
        Map<String, String> filters
) {
    /**
     * Group of objects of the same type.
     *
     * @param count   number of objects of this type
     * @param objects list of objects of this type
     */
    public record ObjectTypeGroup(
            int count,
            List<ObjectInfo> objects
    ) {
    }

    /**
     * Individual ABAP object in a package (TADIR record).
     *
     * Contains all relevant fields from TADIR table:
     * - PGMID: Program ID (e.g., 'R3TR' for repository objects)
     * - OBJECT: Object type (CLAS, PROG, FUGR, TABL, etc.)
     * - OBJ_NAME: Object name
     * - SRCSYSTEM: Source system
     * - AUTHOR: Author/creator
     * - DEVCLASS: Development class (package)
     * - CREATED_ON: Creation date (YYYY-MM-DD)
     * - CHECK_DATE: Last verification date (YYYY-MM-DD)
     *
     * @param pgmid      program ID (R3TR for repository objects)
     * @param objectType object type (CLAS, PROG, FUGR, TABL, etc.)
     * @param objName    object name
     * @param srcSystem  source system
     * @param author     creator/author
     * @param devClass   development class (package)
     * @param createdOn  creation date (YYYY-MM-DD)
     * @param checkDate  last verification date (YYYY-MM-DD)
     */
    public record ObjectInfo(
            String pgmid,
            String objectType,
            String objName,
            String srcSystem,
            String author,
            String devClass,
            String createdOn,
            String checkDate
    ) {
    }

    /**
     * Pagination metadata.
     *
     * @param hasMore     whether there are more pages
     * @param nextOffset  offset for next page
     * @param currentPage current page number
     * @param pageSize    page size used
     * @param totalPages  total pages available
     */
    public record Pagination(
            boolean hasMore,
            int nextOffset,
            int currentPage,
            int pageSize,
            int totalPages
    ) {
    }
}
