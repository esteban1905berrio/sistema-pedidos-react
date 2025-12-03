package com.crystal.mcp.sapserver.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Result of package hierarchy query operation.
 *
 * Contains success status, message, and JSON hierarchy data from SAP TDEVC table.
 *
 * @param success Whether the operation succeeded
 * @param message Status message from SAP
 * @param hierarchy JSON hierarchy data (children or parents)
 */
public record PackageHierarchyResult(
    boolean success,
    String message,
    JsonNode hierarchy
) {}
