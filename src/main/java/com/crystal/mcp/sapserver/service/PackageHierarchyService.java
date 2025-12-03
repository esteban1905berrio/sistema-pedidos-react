package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.PackageHierarchyResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for retrieving SAP package hierarchy (parent-child relationships).
 *
 * <p>Uses TDEVC table via Z_CX_GET_PACKAGE_HIERARCHY function module to query
 * package hierarchies bidirectionally (children or parents).</p>
 *
 * <h2>Supported Operations</h2>
 * <ul>
 *   <li><b>Children Mode ('C')</b>: Get subpackages of a parent package</li>
 *   <li><b>Parents Mode ('P')</b>: Get parent packages of a child package</li>
 *   <li><b>Recursive</b>: Query all levels (true) or direct level only (false)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get direct children of ZCX
 * PackageHierarchyResult result = service.getPackageHierarchy("ZCX", "C", false);
 *
 * // Get all descendants of ZCX (recursive)
 * PackageHierarchyResult result = service.getPackageHierarchy("ZCX", "C", true);
 *
 * // Get parent of ZCXR1003
 * PackageHierarchyResult result = service.getPackageHierarchy("ZCXR1003", "P", false);
 * }</pre>
 *
 * @see com.crystal.mcp.sapserver.tool.PackageHierarchyTools
 */
@Service
public class PackageHierarchyService {

    private static final Logger logger = LoggerFactory.getLogger(PackageHierarchyService.class);

    private final RfcAdapter rfcAdapter;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new PackageHierarchyService.
     *
     * @param rfcAdapter Adapter for SAP RFC communication
     * @param objectMapper Jackson ObjectMapper for JSON parsing
     */
    public PackageHierarchyService(RfcAdapter rfcAdapter, ObjectMapper objectMapper) {
        this.rfcAdapter = rfcAdapter;
        this.objectMapper = objectMapper;
    }

    /**
     * Get package hierarchy from SAP TDEVC table.
     *
     * <p>Queries package parent-child relationships bidirectionally:</p>
     * <ul>
     *   <li><b>Children Mode ('C')</b>: SELECT WHERE parentcl = packageName</li>
     *   <li><b>Parents Mode ('P')</b>: Navigate up from devclass following parentcl</li>
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
     * @param packageName Package name to query (e.g., "ZCX", "ZCXR1003")
     * @param mode Query mode: 'C' for children, 'P' for parents
     * @param recursive True for all levels, false for direct level only
     * @return PackageHierarchyResult with success status and JSON hierarchy
     * @throws Exception if RFC call fails or JSON parsing fails
     */
    public PackageHierarchyResult getPackageHierarchy(
            String packageName,
            String mode,
            boolean recursive) throws Exception {

        logger.info("Getting package hierarchy: packageName={}, mode={}, recursive={}",
                packageName, mode, recursive);

        // Validate inputs
        if (packageName == null || packageName.trim().isEmpty()) {
            logger.warn("Package name is required");
            return new PackageHierarchyResult(
                    false,
                    "Package name is required",
                    objectMapper.createObjectNode()
                            .put("success", false)
                            .put("error", "Package name is required")
            );
        }

        String upperPackageName = packageName.toUpperCase();
        String upperMode = (mode != null) ? mode.toUpperCase() : "C";

        // Validate mode
        if (!upperMode.equals("C") && !upperMode.equals("P")) {
            logger.warn("Invalid mode: {}. Use 'C' or 'P'", mode);
            return new PackageHierarchyResult(
                    false,
                    "Invalid mode: " + mode + ". Use 'C' or 'P'",
                    objectMapper.createObjectNode()
                            .put("success", false)
                            .put("error", "Invalid mode: " + mode + ". Use 'C' or 'P'")
            );
        }

        // Call FM via RFC
        Map<String, String> params = new HashMap<>();
        params.put("IV_PACKAGE_NAME", upperPackageName);
        params.put("IV_MODE", upperMode);
        params.put("IV_RECURSIVE", recursive ? "X" : "");

        try {
            RfcAdapter.RfcFunctionResponse response = rfcAdapter.callFunctionModule(
                    "Z_CX_GET_PACKAGE_HIERARCHY",
                    params
            );

            // Get export parameters
            String success = response.getExportParam("EV_SUCCESS");
            String message = response.getExportParam("EV_MESSAGE");
            String hierarchyJson = response.getExportParam("EV_HIERARCHY_JSON");

            logger.debug("FM response: success={}, message={}", success, message);

            // Parse JSON response
            JsonNode hierarchy = objectMapper.readTree(hierarchyJson);

            boolean isSuccess = "X".equals(success);

            if (!isSuccess) {
                logger.warn("Package hierarchy query failed: {}", message);
            } else {
                logger.info("Package hierarchy query successful: {} packages found",
                        hierarchy.get("totalPackages").asInt());
            }

            return new PackageHierarchyResult(isSuccess, message, hierarchy);

        } catch (Exception e) {
            logger.error("Failed to get package hierarchy for package: {}", packageName, e);
            return new PackageHierarchyResult(
                    false,
                    "Error: " + e.getMessage(),
                    objectMapper.createObjectNode()
                            .put("success", false)
                            .put("error", e.getMessage())
            );
        }
    }
}
