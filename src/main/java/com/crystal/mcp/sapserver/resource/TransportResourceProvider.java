package com.crystal.mcp.sapserver.resource;

import com.crystal.mcp.sapserver.model.TransportInfoListResult;
import com.crystal.mcp.sapserver.model.TransportObjectsResult;
import com.crystal.mcp.sapserver.service.TransportService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP Resource Provider for SAP Transports.
 *
 * Exposes SAP transport information as read-only MCP Resources using URI templates.
 * Resources are lightweight alternatives to Tools for data that:
 * - Is read-only (no side effects)
 * - Can be cached by clients
 * - Follows a predictable URI structure
 *
 * URI Template Pattern: sap://transport/{id}/{aspect}
 *
 * Implemented Resources:
 * - sap://transport/{id}/info    - Transport metadata (owner, status, dates)
 * - sap://transport/{id}/objects - List of objects in transport (JSON)
 *
 * Token Optimization:
 * - info: ~300-500 tokens (metadata only)
 * - objects: ~500-2000 tokens (depends on object count)
 *
 * Usage Example:
 * Client requests: resources/read { uri: "sap://transport/DEVK900123/info" }
 * Response: JSON with transport metadata
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransportResourceProvider {

    private final TransportService transportService;
    private final ObjectMapper objectMapper;

    /**
     * Get transport metadata (info).
     *
     * Returns lightweight metadata about a transport:
     * - owner: Transport owner user ID
     * - status: Status code and description
     * - dates: Creation date/time
     * - targetSystem: Target system for import
     * - type: Transport type (Workbench, Customizing, etc.)
     *
     * MIME Type: application/json
     *
     * Token Optimization: ~300-500 tokens (metadata only, no objects)
     *
     * Example URI: sap://transport/DEVK900123/info
     *
     * @param id transport number (e.g., "DEVK900123", "CADK911088")
     * @return ReadResourceResult with transport metadata
     */
    @McpResource(
            uri = "sap://transport/{id}/info",
            name = "Transport Info",
            description = "Transport request metadata including owner, status, dates, and target system. Token-optimized: ~300-500 tokens",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult getTransportInfo(String id) {
        log.info("Resource request: sap://transport/{}/info", id);

        TransportInfoListResult result = transportService.getTransportInfo(id);
        TransportInfoListResult.TransportInfo info = result.getFirst();

        if (info == null) {
            // Transport not found - return error JSON
            String errorJson = "{\"error\": \"Transport not found\", \"transportNumber\": \"" + id + "\"}";
            return new McpSchema.ReadResourceResult(List.of(
                    new McpSchema.TextResourceContents(
                            "sap://transport/" + id + "/info",
                            "application/json",
                            errorJson
                    )
            ));
        }

        // Convert to lightweight JSON
        TransportMetadata metadata = new TransportMetadata(
                info.transportNumber(),
                info.transportType(),
                info.transportTypeDesc(),
                info.status(),
                info.statusDesc(),
                info.owner(),
                info.description(),
                info.createdDate(),
                info.createdTime(),
                info.targetSystem(),
                info.hasObjects(),
                info.hasTasks()
        );

        String jsonContent = serializeToJson(metadata);

        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(
                        "sap://transport/" + id + "/info",
                        "application/json",
                        jsonContent
                )
        ));
    }

    /**
     * Get objects in a transport.
     *
     * Returns list of ABAP objects contained in the transport:
     * - pgmid: Program ID (R3TR, LIMU)
     * - objectType: Object type (CLAS, PROG, FUGR, etc.)
     * - objectName: Object name
     * - lockFlag: Lock status
     *
     * Also includes tasks for main transports.
     *
     * MIME Type: application/json
     *
     * Token Optimization: ~500-2000 tokens (depends on object count)
     *
     * Example URI: sap://transport/DEVK900123/objects
     *
     * @param id transport number (e.g., "DEVK900123", "CADK911088")
     * @return ReadResourceResult with transport objects
     */
    @McpResource(
            uri = "sap://transport/{id}/objects",
            name = "Transport Objects",
            description = "List of ABAP objects in transport request with metadata. Token-optimized: ~500-2000 tokens",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult getTransportObjects(String id) {
        log.info("Resource request: sap://transport/{}/objects", id);

        TransportObjectsResult result = transportService.getTransportObjects(id, null);

        // Convert to lightweight JSON structure
        List<ObjectInfo> objects = result.objects().stream()
                .map(o -> new ObjectInfo(
                        o.pgmid(),
                        o.objectType(),
                        o.objectName(),
                        o.lockFlag()
                ))
                .collect(Collectors.toList());

        List<TaskInfo> tasks = result.tasks().stream()
                .map(t -> new TaskInfo(
                        t.taskNumber(),
                        t.owner(),
                        t.status(),
                        t.statusDesc(),
                        t.description(),
                        t.objectCount()
                ))
                .collect(Collectors.toList());

        TransportObjectsData data = new TransportObjectsData(
                result.transportNumber(),
                result.success(),
                result.totalObjects(),
                objects,
                tasks
        );

        String jsonContent = serializeToJson(data);

        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(
                        "sap://transport/" + id + "/objects",
                        "application/json",
                        jsonContent
                )
        ));
    }

    // ========================================================================
    // Helper DTOs
    // ========================================================================

    /**
     * Lightweight DTO for transport metadata.
     */
    private record TransportMetadata(
            String transportNumber,
            String type,
            String typeDesc,
            String status,
            String statusDesc,
            String owner,
            String description,
            String createdDate,
            String createdTime,
            String targetSystem,
            boolean hasObjects,
            boolean hasTasks
    ) {}

    /**
     * Lightweight DTO for transport object.
     */
    private record ObjectInfo(
            String pgmid,
            String objectType,
            String objectName,
            String lockFlag
    ) {}

    /**
     * Lightweight DTO for transport task.
     */
    private record TaskInfo(
            String taskNumber,
            String owner,
            String status,
            String statusDesc,
            String description,
            int objectCount
    ) {}

    /**
     * Container DTO for transport objects response.
     */
    private record TransportObjectsData(
            String transportNumber,
            boolean success,
            int totalObjects,
            List<ObjectInfo> objects,
            List<TaskInfo> tasks
    ) {}

    /**
     * Serialize object to JSON string.
     */
    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("JSON serialization failed", e);
        }
    }
}
