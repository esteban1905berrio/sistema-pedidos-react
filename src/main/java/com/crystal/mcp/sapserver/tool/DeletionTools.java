package com.crystal.mcp.sapserver.tool;

import com.crystal.mcp.sapserver.model.DeleteObjectResult;
import com.crystal.mcp.sapserver.service.StatefulModificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tools for ABAP Object Deletion.
 *
 * This component provides tools for deleting ABAP objects using stateful workflows.
 * Implements the complete LOCK → DELETE → UNLOCK pattern required by ADT.
 *
 * Supported Object Types:
 * - CLAS: Classes
 * - INTF: Interfaces
 * - FUGR: Function Groups
 * - FUNC: Function Modules (requires functionGroupName)
 * - PROG: Programs
 *
 * Workflow:
 * 1. Build object URI based on type and name
 * 2. Transport check (get object metadata)
 * 3. Lock object
 * 4. Delete object
 * 5. Unlock object (always in finally block)
 *
 * Error Handling:
 * - Lock conflicts: Shows details of lock holder (user, transport, date)
 * - Delete failures: Shows underlying SAP error message
 * - Unlock failures: Logged as warning, doesn't override main result
 *
 * Spring AI MCP Server automatically discovers and registers @McpTool methods.
 *
 * @see StatefulModificationService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeletionTools {

    private final StatefulModificationService statefulModificationService;

    /**
     * MCP Tool: Delete an ABAP object from SAP system.
     *
     * This tool implements a complete stateful workflow for object deletion:
     * - Validates object type and builds ADT URI
     * - Performs transport check to get object metadata
     * - Locks object in stateful session
     * - Deletes object
     * - Unlocks object (always executed, even on error)
     *
     * Transport Handling:
     * - If transport parameter is provided, uses it for deletion
     * - If not provided, uses transport number from LOCK response (auto-assigned)
     * - For local objects ($TMP), transport may be empty
     *
     * Object Type Support:
     * - CLAS: Classes - /sap/bc/adt/oo/classes/{name}
     * - INTF: Interfaces - /sap/bc/adt/oo/interfaces/{name}
     * - FUGR: Function Groups - /sap/bc/adt/functions/groups/{name}
     * - FUNC: Function Modules - /sap/bc/adt/functions/groups/{fg}/fmodules/{name}
     * - PROG: Programs - /sap/bc/adt/programs/programs/{name}
     *
     * Error Scenarios:
     * 1. Object locked by another user:
     *    - Error shows lock holder details (user, transport, timestamp)
     * 2. Insufficient permissions:
     *    - Error shows HTTP 401/403 with SAP message
     * 3. Object not found:
     *    - Error shows HTTP 404
     * 4. Delete failure:
     *    - Error shows underlying SAP error
     *    - Object is unlocked automatically
     *
     * Usage Examples:
     * - Delete class: delete_object("ZCL_TEST", "CLAS", null, null)
     * - Delete function module: delete_object("Z_TEST_FM", "FUNC", "ZTEST_FG", null)
     * - Delete with transport: delete_object("ZCL_TEST", "CLAS", null, "CADK910827")
     *
     * @param objectName         Object name (e.g., "ZCL_TEST", "Z_TEST_FM")
     * @param objectType         Object type: CLAS, INTF, FUGR, FUNC, PROG
     * @param functionGroupName  Required for FUNC type, null otherwise
     * @param transport          Optional transport number (uses auto-assigned if null)
     * @return DeleteObjectResult with success status and details
     */
    @McpTool(
            description = "Delete an ABAP object (class, interface, function group/module, program) from SAP system. " +
                    "Uses stateful workflow: LOCK → DELETE → UNLOCK. " +
                    "Supports all object types: CLAS, INTF, FUGR, FUNC, PROG. " +
                    "For FUNC type, functionGroupName is required. " +
                    "Transport can be provided or auto-assigned from LOCK. " +
                    "Shows detailed errors for lock conflicts, permissions, or deletion failures. " +
                    "Always unlocks object even on error to prevent orphaned locks."
    )
    public DeleteObjectResult delete_object(
            @McpToolParam(
                    description = "Object name (e.g., 'ZCL_TEST' for class, 'Z_TEST_FM' for function module, " +
                            "'ZREP_INVOICE' for program). Case-insensitive.",
                    required = true
            )
            String objectName,
            @McpToolParam(
                    description = "Object type: CLAS (class), INTF (interface), FUGR (function group), " +
                            "FUNC (function module), PROG (program). Case-insensitive.",
                    required = true
            )
            String objectType,
            @McpToolParam(
                    description = "Function group name (required for FUNC type only). " +
                            "Examples: 'ZTEST_FG', 'ZFI_UTILS'. Leave null for other object types.",
                    required = false
            )
            String functionGroupName,
            @McpToolParam(
                    description = "Transport request number (optional). " +
                            "If not provided, uses transport from LOCK response (auto-assigned by SAP). " +
                            "Examples: 'CADK910827', 'DEVK900123'. Leave null for auto-assignment.",
                    required = false
            )
            String transport
    ) {
        log.info("Delete object request: name={}, type={}, functionGroup={}, transport={}",
                objectName, objectType, functionGroupName, transport);

        try {
            // 1. Build URI based on object type
            String objectUri = StatefulModificationService.buildObjectUri(
                    objectType,
                    objectName,
                    functionGroupName
            );

            log.debug("Built object URI: {}", objectUri);

            // 2. Execute stateful deletion workflow
            return statefulModificationService.executeStatefulWorkflow(
                    objectName,
                    () -> {
                        // 2.1 Transport check (get object metadata)
                        StatefulModificationService.TransportCheckResult checkResult =
                                statefulModificationService.transportCheck(objectUri);

                        log.info("Transport check completed: pgmid={}, object={}, devclass={}, result={}",
                                checkResult.pgmid(), checkResult.object(),
                                checkResult.devclass(), checkResult.result());

                        // 2.2 Lock object
                        StatefulModificationService.LockResult lockResult =
                                statefulModificationService.lockObject(objectUri);

                        log.info("Object locked: lockHandle={}, transport={}, user={}",
                                lockResult.lockHandle(), lockResult.transportNumber(),
                                lockResult.transportUser());

                        try {
                            // 2.3 Delete object
                            String effectiveTransport = (transport != null && !transport.isEmpty())
                                    ? transport
                                    : lockResult.transportNumber();

                            statefulModificationService.deleteObject(
                                    objectUri,
                                    lockResult.lockHandle(),
                                    effectiveTransport
                            );

                            log.info("Object successfully deleted: {} (type: {})", objectName, objectType);

                            // 2.4 Build success result
                            return DeleteObjectResult.success(
                                    objectName,
                                    objectType,
                                    checkResult.devclass(),
                                    effectiveTransport,
                                    lockResult.transportUser(),
                                    lockResult.transportDescription(),
                                    objectUri
                            );

                        } finally {
                            // 2.5 Always unlock (even on error)
                            statefulModificationService.unlockObject(objectUri, lockResult.lockHandle());
                        }
                    }
            );

        } catch (IllegalArgumentException e) {
            // Validation error (unsupported object type, missing functionGroupName)
            log.error("Validation error: {}", e.getMessage());
            return DeleteObjectResult.failure(
                    objectName,
                    objectType,
                    null,
                    "Validation error: " + e.getMessage(),
                    null
            );

        } catch (RuntimeException e) {
            // SAP error (lock conflict, permission denied, delete failure, etc.)
            log.error("Delete object failed: {}", e.getMessage(), e);

            String errorMessage = e.getMessage();
            String errorDetails = extractErrorDetails(e);

            return DeleteObjectResult.failure(
                    objectName,
                    objectType,
                    null,
                    errorMessage,
                    errorDetails
            );
        }
    }

    /**
     * Extract detailed error information from exception.
     *
     * Provides user-friendly error messages for common scenarios:
     * - Lock conflicts (HTTP 423)
     * - Permission errors (HTTP 401/403)
     * - Not found errors (HTTP 404)
     * - Other SAP errors
     *
     * @param e exception to analyze
     * @return error details string
     */
    private String extractErrorDetails(Exception e) {
        String message = e.getMessage();

        if (message == null) {
            return "Unknown error";
        }

        // HTTP 423 Locked - Object locked by another user
        if (message.contains("HTTP 423") || message.contains("locked by another user")) {
            return "Object is locked by another user. " +
                    "Check lock details and wait for release or contact the lock holder.";
        }

        // HTTP 401/403 - Permission denied
        if (message.contains("HTTP 401") || message.contains("HTTP 403") ||
                message.contains("Insufficient permissions")) {
            return "Insufficient permissions to delete this object. " +
                    "Contact your SAP administrator to grant necessary authorizations.";
        }

        // HTTP 404 - Object not found
        if (message.contains("HTTP 404")) {
            return "Object not found in SAP system. " +
                    "Verify object name and type are correct.";
        }

        // Generic error
        return message;
    }
}
