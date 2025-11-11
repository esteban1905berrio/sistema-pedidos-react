package com.crystal.mcp.sapserver.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single message from ABAP syntax check.
 *
 * Messages can be errors, warnings, or informational messages returned
 * by the ADT syntax check endpoint (/sap/bc/adt/checkruns).
 *
 * Used in workflow-based modification operations to validate source code
 * before attempting to update it in SAP.
 *
 * Example usage:
 * <pre>
 * {@code
 * List<SyntaxCheckMessage> messages = syntaxCheck(uri, source);
 * long errorCount = messages.stream()
 *     .filter(m -> "error".equals(m.getType()))
 *     .count();
 *
 * if (errorCount > 0) {
 *     throw new RuntimeException("Syntax errors found");
 * }
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyntaxCheckMessage {

    /**
     * Message type.
     * Common values: "error", "warning", "info"
     */
    private String type;

    /**
     * Line number in source code where issue was found.
     * 0 if line information not available.
     */
    private int line;

    /**
     * Column number in source code where issue was found.
     * 0 if column information not available.
     */
    private int column;

    /**
     * Human-readable message text describing the issue.
     */
    private String text;

    /**
     * Severity of the message.
     * Optional field, may be null.
     */
    private String severity;

    /**
     * Check if this message is an error.
     *
     * @return true if type is "error"
     */
    public boolean isError() {
        return "error".equalsIgnoreCase(type);
    }

    /**
     * Check if this message is a warning.
     *
     * @return true if type is "warning"
     */
    public boolean isWarning() {
        return "warning".equalsIgnoreCase(type);
    }

    /**
     * Check if this message is informational.
     *
     * @return true if type is "info"
     */
    public boolean isInfo() {
        return "info".equalsIgnoreCase(type);
    }

    /**
     * Get formatted string representation for logging.
     *
     * @return formatted message like "[ERROR] Line 10: Syntax error"
     */
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(type != null ? type.toUpperCase() : "UNKNOWN").append("]");

        if (line > 0) {
            sb.append(" Line ").append(line);
            if (column > 0) {
                sb.append(":").append(column);
            }
        }

        sb.append(": ").append(text != null ? text : "No message");

        return sb.toString();
    }

    @Override
    public String toString() {
        return toFormattedString();
    }
}
