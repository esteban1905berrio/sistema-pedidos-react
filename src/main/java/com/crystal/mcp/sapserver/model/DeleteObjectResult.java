package com.crystal.mcp.sapserver.model;

import lombok.Builder;
import lombok.Data;

/**
 * Result model for delete_object MCP tool.
 *
 * Contains complete information about the deletion operation including:
 * - Success/failure status
 * - Object metadata (name, type, package)
 * - Transport information
 * - Error details if applicable
 *
 * This model follows the pattern established by other result models
 * (ClassSourceResult, ProgramModifyResult) for consistency.
 *
 * @see com.crystal.mcp.sapserver.tool.DeletionTools#delete_object
 */
@Data
@Builder
public class DeleteObjectResult {

    /**
     * Indicates if deletion was successful.
     */
    private boolean success;

    /**
     * Object name (e.g., "ZCL_TEST", "ZFI_DMEE_COLPATRIA_R4").
     */
    private String objectName;

    /**
     * Object type (CLAS, INTF, FUGR, FUNC, PROG).
     */
    private String objectType;

    /**
     * Development package (devclass).
     */
    private String devclass;

    /**
     * Transport request number used for deletion.
     * Can be user-provided or auto-assigned from LOCK.
     */
    private String transportNumber;

    /**
     * Transport owner (CORRUSER from LOCK).
     */
    private String transportUser;

    /**
     * Transport description.
     */
    private String transportDescription;

    /**
     * ADT URI of the deleted object.
     */
    private String objectUri;

    /**
     * Error message if deletion failed.
     */
    private String errorMessage;

    /**
     * Error details (stack trace, HTTP response) for debugging.
     */
    private String errorDetails;

    /**
     * Factory method for successful deletion.
     *
     * @param objectName object name
     * @param objectType object type
     * @param devclass development package
     * @param transportNumber transport number
     * @param transportUser transport user
     * @param transportDescription transport description
     * @param objectUri ADT URI
     * @return success result
     */
    public static DeleteObjectResult success(
            String objectName,
            String objectType,
            String devclass,
            String transportNumber,
            String transportUser,
            String transportDescription,
            String objectUri
    ) {
        return DeleteObjectResult.builder()
                .success(true)
                .objectName(objectName)
                .objectType(objectType)
                .devclass(devclass)
                .transportNumber(transportNumber)
                .transportUser(transportUser)
                .transportDescription(transportDescription)
                .objectUri(objectUri)
                .build();
    }

    /**
     * Factory method for failed deletion.
     *
     * @param objectName object name
     * @param objectType object type
     * @param objectUri ADT URI
     * @param errorMessage error message
     * @param errorDetails error details
     * @return failure result
     */
    public static DeleteObjectResult failure(
            String objectName,
            String objectType,
            String objectUri,
            String errorMessage,
            String errorDetails
    ) {
        return DeleteObjectResult.builder()
                .success(false)
                .objectName(objectName)
                .objectType(objectType)
                .objectUri(objectUri)
                .errorMessage(errorMessage)
                .errorDetails(errorDetails)
                .build();
    }
}
