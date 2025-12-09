package com.crystal.mcp.sapserver.resource;

import com.crystal.mcp.sapserver.model.ClassSourceResult;
import com.crystal.mcp.sapserver.model.ObjectStructure;
import com.crystal.mcp.sapserver.service.ClassService;
import com.crystal.mcp.sapserver.service.ObjectService;
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
 * MCP Resource Provider for ABAP Classes.
 *
 * Exposes ABAP class information as read-only MCP Resources using URI templates.
 * Resources are lightweight alternatives to Tools for data that:
 * - Is read-only (no side effects)
 * - Can be cached by clients
 * - Follows a predictable URI structure
 *
 * URI Template Pattern: sap://class/{name}/{aspect}
 *
 * Implemented Resources:
 * - sap://class/{name}/definition     - Class definition source code
 * - sap://class/{name}/implementation - Class implementation source code
 * - sap://class/{name}/methods        - List of methods with metadata (JSON)
 * - sap://class/{name}/attributes     - List of attributes with metadata (JSON)
 *
 * Token Optimization:
 * - methods/attributes: ~300-500 tokens (JSON metadata only)
 * - definition/implementation: ~2,000+ tokens (full source code)
 *
 * Usage Example:
 * Client requests: resources/read { uri: "sap://class/ZCL_INVOICE/methods" }
 * Response: JSON array with method names, visibility, parameters
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassResourceProvider {

    private final ClassService classService;
    private final ObjectService objectService;
    private final ObjectMapper objectMapper;

    /**
     * Static resource: Server information and available resource templates.
     *
     * This is a STATIC resource (no URI template) that provides:
     * - Server name and version
     * - List of available resource templates
     * - Usage examples
     *
     * MIME Type: application/json
     *
     * Example URI: sap://server/info (static, no parameters)
     *
     * @return ReadResourceResult with server info
     */
    @McpResource(
            uri = "sap://server/info",
            name = "SAP MCP Server Info",
            description = "Server information and list of available SAP resource templates",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult getServerInfo() {
        log.info("Resource request: sap://server/info (static)");

        String serverInfo = """
            {
                "server": "SAP ABAP MCP Server",
                "version": "0.1.0-POC",
                "description": "MCP Server for SAP ABAP using Spring AI and SAP JCo",
                "resourceTemplates": [
                    {
                        "uri": "sap://class/{name}/definition",
                        "description": "ABAP class definition source code"
                    },
                    {
                        "uri": "sap://class/{name}/implementation",
                        "description": "ABAP class implementation source code"
                    },
                    {
                        "uri": "sap://class/{name}/methods",
                        "description": "List of methods in ABAP class (JSON)"
                    },
                    {
                        "uri": "sap://class/{name}/attributes",
                        "description": "List of attributes in ABAP class (JSON)"
                    },
                    {
                        "uri": "sap://transport/{id}/info",
                        "description": "Transport request metadata"
                    },
                    {
                        "uri": "sap://transport/{id}/objects",
                        "description": "Objects in transport request"
                    },
                    {
                        "uri": "sap://package/{name}/objects",
                        "description": "Objects in SAP package"
                    },
                    {
                        "uri": "sap://package/{name}/hierarchy",
                        "description": "Package hierarchy (children/parents)"
                    },
                    {
                        "uri": "sap://table/{name}/fields",
                        "description": "Table/structure field definitions"
                    }
                ],
                "examples": [
                    "sap://class/ZCL_INVOICE/definition",
                    "sap://transport/DEVK900123/info",
                    "sap://package/ZCX/objects"
                ]
            }
            """;

        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(
                        "sap://server/info",
                        "application/json",
                        serverInfo
                )
        ));
    }

    /**
     * Get ABAP class definition source code.
     *
     * Returns the class definition including:
     * - PUBLIC section (public methods, attributes, types)
     * - PROTECTED section (protected methods, attributes)
     * - PRIVATE section (private methods, attributes)
     *
     * MIME Type: text/plain (ABAP source code)
     *
     * Example URI: sap://class/ZCL_INVOICE/definition
     *
     * @param name class name (e.g., "ZCL_INVOICE", "CL_ABAP_CHAR_UTILITIES")
     * @return ReadResourceResult with class definition source
     */
    @McpResource(
            uri = "sap://class/{name}/definition",
            name = "Class Definition",
            description = "ABAP class definition source code including PUBLIC, PROTECTED, and PRIVATE sections",
            mimeType = "text/plain"
    )
    public McpSchema.ReadResourceResult getClassDefinition(String name) {
        log.info("Resource request: sap://class/{}/definition", name);

        ClassSourceResult result = classService.getClassSource(name, "active", "main");

        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(
                        "sap://class/" + name + "/definition",
                        "text/plain",
                        result.source()
                )
        ));
    }

    /**
     * Get ABAP class implementation source code.
     *
     * Returns the method implementations for the class.
     *
     * MIME Type: text/plain (ABAP source code)
     *
     * Example URI: sap://class/ZCL_INVOICE/implementation
     *
     * @param name class name (e.g., "ZCL_INVOICE", "CL_ABAP_CHAR_UTILITIES")
     * @return ReadResourceResult with class implementation source
     */
    @McpResource(
            uri = "sap://class/{name}/implementation",
            name = "Class Implementation",
            description = "ABAP class implementation source code with method implementations",
            mimeType = "text/plain"
    )
    public McpSchema.ReadResourceResult getClassImplementation(String name) {
        log.info("Resource request: sap://class/{}/implementation", name);

        ClassSourceResult result = classService.getClassSource(name, "active", "implementations");

        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(
                        "sap://class/" + name + "/implementation",
                        "text/plain",
                        result.source()
                )
        ));
    }

    /**
     * Get list of methods in an ABAP class.
     *
     * Returns JSON array with method metadata:
     * - name: method name
     * - type: component type (METHOD)
     * - uri: ADT URI to access method details
     * - description: method description
     *
     * MIME Type: application/json
     *
     * Token Optimization: ~300-500 tokens (metadata only, no source code)
     *
     * Example URI: sap://class/ZCL_INVOICE/methods
     * Example Response:
     * [
     *   {"name": "CONSTRUCTOR", "type": "METHOD", "description": "Class constructor"},
     *   {"name": "PROCESS_INVOICE", "type": "METHOD", "description": "Process invoice data"}
     * ]
     *
     * @param name class name (e.g., "ZCL_INVOICE", "CL_ABAP_CHAR_UTILITIES")
     * @return ReadResourceResult with JSON array of methods
     */
    @McpResource(
            uri = "sap://class/{name}/methods",
            name = "Class Methods",
            description = "List of methods in ABAP class with metadata (JSON). Token-optimized: ~300-500 tokens",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult getClassMethods(String name) {
        log.info("Resource request: sap://class/{}/methods", name);

        // Get object structure which contains methods
        String objectUri = "/sap/bc/adt/oo/classes/" + name.toLowerCase();
        ObjectStructure structure = objectService.getObjectStructure(objectUri);

        // Filter components to get only methods
        List<MethodInfo> methods = structure.components().stream()
                .filter(c -> "METHOD".equalsIgnoreCase(c.type()) || c.type().contains("METHOD"))
                .map(c -> new MethodInfo(c.name(), c.type(), c.description(), c.uri()))
                .collect(Collectors.toList());

        String jsonContent = serializeToJson(methods);

        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(
                        "sap://class/" + name + "/methods",
                        "application/json",
                        jsonContent
                )
        ));
    }

    /**
     * Get list of attributes in an ABAP class.
     *
     * Returns JSON array with attribute metadata:
     * - name: attribute name
     * - type: component type (ATTRIBUTE, DATA, etc.)
     * - uri: ADT URI to access attribute details
     * - description: attribute description
     *
     * MIME Type: application/json
     *
     * Token Optimization: ~200-400 tokens (metadata only)
     *
     * Example URI: sap://class/ZCL_INVOICE/attributes
     * Example Response:
     * [
     *   {"name": "MV_STATUS", "type": "DATA", "description": "Current status"},
     *   {"name": "MT_ITEMS", "type": "DATA", "description": "Invoice items table"}
     * ]
     *
     * @param name class name (e.g., "ZCL_INVOICE", "CL_ABAP_CHAR_UTILITIES")
     * @return ReadResourceResult with JSON array of attributes
     */
    @McpResource(
            uri = "sap://class/{name}/attributes",
            name = "Class Attributes",
            description = "List of attributes in ABAP class with metadata (JSON). Token-optimized: ~200-400 tokens",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult getClassAttributes(String name) {
        log.info("Resource request: sap://class/{}/attributes", name);

        // Get object structure which contains attributes
        String objectUri = "/sap/bc/adt/oo/classes/" + name.toLowerCase();
        ObjectStructure structure = objectService.getObjectStructure(objectUri);

        // Filter components to get only attributes (DATA, ATTRIBUTE types)
        List<AttributeInfo> attributes = structure.components().stream()
                .filter(c -> isAttributeType(c.type()))
                .map(c -> new AttributeInfo(c.name(), c.type(), c.description(), c.uri()))
                .collect(Collectors.toList());

        String jsonContent = serializeToJson(attributes);

        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(
                        "sap://class/" + name + "/attributes",
                        "application/json",
                        jsonContent
                )
        ));
    }

    // ========================================================================
    // Helper DTOs and Methods
    // ========================================================================

    /**
     * Lightweight DTO for method information in JSON response.
     */
    private record MethodInfo(
            String name,
            String type,
            String description,
            String uri
    ) {}

    /**
     * Lightweight DTO for attribute information in JSON response.
     */
    private record AttributeInfo(
            String name,
            String type,
            String description,
            String uri
    ) {}

    /**
     * Check if component type represents an attribute.
     *
     * @param type component type from ObjectStructure
     * @return true if it's an attribute type
     */
    private boolean isAttributeType(String type) {
        if (type == null) return false;
        String upperType = type.toUpperCase();
        return upperType.contains("DATA") ||
               upperType.contains("ATTRIBUTE") ||
               upperType.contains("CLASS-DATA") ||
               upperType.contains("INSTANCE") ||
               upperType.contains("CONSTANT");
    }

    /**
     * Serialize object to JSON string.
     *
     * @param object object to serialize
     * @return JSON string
     * @throws RuntimeException if serialization fails
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
