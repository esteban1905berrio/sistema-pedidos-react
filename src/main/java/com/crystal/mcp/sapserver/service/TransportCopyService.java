package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.TransportCopyRequest;
import com.crystal.mcp.sapserver.model.TransportCopyResult;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for creating transport copies (Transport of Copies) from existing transport requests.
 *
 * <p>This service provides functionality to create a complete copy of a transport request,
 * including all objects from the main transport and its tasks. Transport copies are useful for:
 * <ul>
 *   <li>Moving the same objects to multiple systems</li>
 *   <li>Creating backups of transport contents</li>
 *   <li>Re-importing objects after system refreshes</li>
 * </ul>
 *
 * <p>The service calls the ABAP function module {@code ZCX_CREATE_TRANSPORT_COPY}, which:
 * <ol>
 *   <li>Validates the source transport exists</li>
 *   <li>Retrieves all objects from main transport + tasks (via E070 STRKORR)</li>
 *   <li>Creates a new transport of type 'T' (Transport of Copies)</li>
 *   <li>Copies all objects to the new transport</li>
 *   <li>Optionally releases the transport automatically</li>
 * </ol>
 *
 * <p><b>Implementation Note:</b> This service uses stateless connections as transport copy
 * operations don't require locks. The ABAP class {@code ZCLCX_TRANSPORT_MANAGEMENT} handles
 * all workflow orchestration internally.
 *
 * @author Crystal Development Team
 * @since 2025-11-18
 * @see TransportCopyRequest
 * @see TransportCopyResult
 */
@Service
public class TransportCopyService {

    private static final Logger logger = LoggerFactory.getLogger(TransportCopyService.class);
    private static final String FUNCTION_MODULE = "ZCX_CREATE_TRANSPORT_COPY";

    private final com.sap.conn.jco.JCoDestination destination;

    public TransportCopyService(com.sap.conn.jco.JCoDestination destination) {
        this.destination = destination;
    }

    /**
     * Creates a transport copy from an existing transport request.
     *
     * <p>This method calls the SAP function module {@code ZCX_CREATE_TRANSPORT_COPY} which
     * internally uses the ABAP class {@code ZCLCX_TRANSPORT_MANAGEMENT} to:
     * <ol>
     *   <li>Query E070 table for main transport and all related tasks (via STRKORR)</li>
     *   <li>Extract objects from E071 and E071K tables</li>
     *   <li>Create new transport via {@code TR_EXT_CREATE_REQUEST}</li>
     *   <li>Add objects via {@code TR_REQUEST_CHOICE}</li>
     *   <li>Optionally release via {@code TR_RELEASE_REQUEST}</li>
     * </ol>
     *
     * <p><b>Workflow:</b>
     * <pre>
     * QUERY_TASKS → CREATE_TRANSPORT → COPY_OBJECTS → RELEASE (optional)
     * </pre>
     *
     * <p><b>Error Handling:</b> If any step fails, the newly created transport is automatically
     * deleted (rollback) to prevent orphaned transports.
     *
     * @param request The transport copy request parameters
     * @return TransportCopyResult containing the new transport number and operation status
     * @throws JCoException if RFC communication fails
     * @throws IllegalArgumentException if request validation fails
     *
     * @see TransportCopyRequest#validate()
     */
    public TransportCopyResult createTransportCopy(TransportCopyRequest request)
            throws JCoException {

        // Validate request
        request.validate();

        logger.info("Creating transport copy for source: {}, target: {}, prefix: {}, autoRelease: {}",
            request.sourceTransport(),
            request.targetSystem() != null ? request.targetSystem() : "auto",
            request.getDescriptionPrefixOrDefault(),
            request.autoRelease()
        );

        try {
            // Get function module from repository
            JCoFunction function = destination.getRepository().getFunction(FUNCTION_MODULE);

            if (function == null) {
                throw new RuntimeException(
                    FUNCTION_MODULE + " not found in SAP system. " +
                    "Ensure the function module exists in package ZGFCX_1."
                );
            }

            // Set import parameters
            function.getImportParameterList().setValue("IV_TRANSPORT_REQUEST",
                request.getSourceTransportUpperCase());
            function.getImportParameterList().setValue("IV_TARGET_SYSTEM",
                request.getTargetSystemUpperCase());
            function.getImportParameterList().setValue("IV_DESCRIPTION_PREFIX",
                request.getDescriptionPrefixOrDefault());
            function.getImportParameterList().setValue("IV_AUTO_RELEASE",
                request.autoRelease() ? "X" : "");

            // Execute function module
            logger.debug("Executing RFC: {} with parameters: {}", FUNCTION_MODULE, request);
            function.execute(destination);

            // Get export parameters
            String newTransport = function.getExportParameterList().getString("EV_NEW_TRANSPORT");
            String status = function.getExportParameterList().getString("EV_STATUS");
            String message = function.getExportParameterList().getString("EV_MESSAGE");

            // Log result
            if ("S".equals(status)) {
                logger.info("Transport copy created successfully: {} (source: {})",
                    newTransport, request.sourceTransport());
            } else {
                logger.warn("Transport copy creation completed with status {}: {}",
                    status, message);
            }

            // Build result
            TransportCopyResult result = new TransportCopyResult(
                newTransport,
                status,
                message,
                "S".equals(status)
            );

            logger.debug("Transport copy result: {}", result);
            return result;

        } catch (JCoException e) {
            logger.error("RFC error creating transport copy for source: {}",
                request.sourceTransport(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating transport copy for source: {}",
                request.sourceTransport(), e);
            return TransportCopyResult.error("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Creates a transport copy with default settings.
     *
     * <p>Uses default values:
     * <ul>
     *   <li>Target system: Same as source transport</li>
     *   <li>Description prefix: "COPIA"</li>
     *   <li>Auto-release: true</li>
     * </ul>
     *
     * @param sourceTransport Source transport request number
     * @return TransportCopyResult containing the new transport number and operation status
     * @throws JCoException if RFC communication fails
     */
    public TransportCopyResult createTransportCopyWithDefaults(String sourceTransport)
            throws JCoException {
        return createTransportCopy(TransportCopyRequest.withDefaults(sourceTransport));
    }

    /**
     * Creates a transport copy without automatic release.
     *
     * <p>The transport remains modifiable after creation, allowing additional objects
     * to be added before manual release.
     *
     * @param sourceTransport Source transport request number
     * @param targetSystem Target system name (null to use source transport's target)
     * @param descriptionPrefix Description prefix (null to use "COPIA")
     * @return TransportCopyResult containing the new transport number and operation status
     * @throws JCoException if RFC communication fails
     */
    public TransportCopyResult createTransportCopyWithoutRelease(
            String sourceTransport,
            String targetSystem,
            String descriptionPrefix) throws JCoException {

        return createTransportCopy(TransportCopyRequest.withoutRelease(
            sourceTransport,
            targetSystem,
            descriptionPrefix
        ));
    }

    /**
     * Validates if the function module exists in the SAP system.
     *
     * @return true if ZCX_CREATE_TRANSPORT_COPY exists
     */
    public boolean isFunctionModuleAvailable() {
        try {
            JCoFunction function = destination.getRepository().getFunction(FUNCTION_MODULE);
            return function != null;
        } catch (JCoException e) {
            logger.warn("Function module {} not available: {}", FUNCTION_MODULE, e.getMessage());
            return false;
        }
    }
}
