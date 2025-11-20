package com.crystal.mcp.sapserver.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a class modification workflow.
 *
 * This result contains information about each step of the modification process:
 * LOCK → MODIFY → UNLOCK
 *
 * Based on ProgramModifyResult and Python implementation: modification_service.py
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClassModifyResult {

    private boolean success;
    private String uri;
    private String className;
    private String includeType; // "main", "implementation", "testclasses", "macros"
    private boolean locked;
    private boolean modified;
    private boolean unlocked;
    private boolean activated;
    private String lockHandle;
    private String transportNumber;
    private String transportUser;
    private String transportDescription;
    private String version;
    private List<Message> messages;

    public ClassModifyResult() {
        this.success = false;
        this.locked = false;
        this.modified = false;
        this.unlocked = false;
        this.activated = false;
        this.messages = new ArrayList<>();
    }

    /**
     * Message from workflow steps.
     */
    public static class Message {
        private String type; // "info", "warning", "error"
        private String text;
        private String step; // "lock", "modify", "unlock"

        public Message(String type, String text, String step) {
            this.type = type;
            this.text = text;
            this.step = step;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getStep() {
            return step;
        }

        public void setStep(String step) {
            this.step = step;
        }
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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getIncludeType() {
        return includeType;
    }

    public void setIncludeType(String includeType) {
        this.includeType = includeType;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public String getLockHandle() {
        return lockHandle;
    }

    public void setLockHandle(String lockHandle) {
        this.lockHandle = lockHandle;
    }

    public String getTransportNumber() {
        return transportNumber;
    }

    public void setTransportNumber(String transportNumber) {
        this.transportNumber = transportNumber;
    }

    public String getTransportUser() {
        return transportUser;
    }

    public void setTransportUser(String transportUser) {
        this.transportUser = transportUser;
    }

    public String getTransportDescription() {
        return transportDescription;
    }

    public void setTransportDescription(String transportDescription) {
        this.transportDescription = transportDescription;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public void addMessage(String type, String text, String step) {
        this.messages.add(new Message(type, text, step));
    }
}
