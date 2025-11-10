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
     * @param type    object type (CLAS, PROG, FUGR, etc.)
     * @param count   number of objects of this type
     * @param objects list of objects of this type
     */
    public record ObjectTypeGroup(
            String type,
            int count,
            List<PackageObject> objects
    ) {
    }

    /**
     * Individual ABAP object in a package.
     *
     * @param objectType object type (CLAS, PROG, FUGR, TABL, etc.)
     * @param objectName object name
     * @param author     creator/author
     * @param createdOn  creation date (YYYY-MM-DD)
     * @param pgmid      program ID (R3TR for repository objects)
     */
    public record PackageObject(
            String objectType,
            String objectName,
            String author,
            String createdOn,
            String pgmid
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
