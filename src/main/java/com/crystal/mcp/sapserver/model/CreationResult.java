package com.crystal.mcp.sapserver.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Generic result model for ABAP object creation operations.
 *
 * Used for:
 * - Function Group creation
 * - Function Module creation
 * - Class creation
 * - Interface creation
 *
 * Based on Python implementation: creation_service.py
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreationResult {

    private boolean success;
    private String uri;
    private String name;
    private String objectType;
    private String package_;
    private String transport;
    private String parentName;  // For function modules (function group name)
    private String message;

    public CreationResult() {
        this.success = false;
    }

    public CreationResult(boolean success, String uri, String name, String objectType) {
        this.success = success;
        this.uri = uri;
        this.name = name;
        this.objectType = objectType;
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getPackage_() {
        return package_;
    }

    public void setPackage_(String package_) {
        this.package_ = package_;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
