package com.crystal.mcp.sapserver.model;

/**
 * Extraction scope types for ABAP Extraction Tool.
 *
 * <p>Defines the 4 supported extraction scopes:
 * <ul>
 *   <li>{@link #USER} - Objects created/modified by a specific user</li>
 *   <li>{@link #PACKAGE} - Objects from package hierarchy (recursive)</li>
 *   <li>{@link #TRANSPORT} - Objects from specific transport request(s)</li>
 *   <li>{@link #LIST} - Specific objects by name</li>
 * </ul>
 *
 * @see AbapExtractionService
 * @see ExtractionDiscovery
 */
public enum ExtractionScope {

    /**
     * Extract objects created/modified by a specific SAP user.
     *
     * <p>Discovery method: FM ZCX_UTIL_GET_USER_OBJECTS (searches TADIR by AUTHOR)
     *
     * <p>Input: username (optional, defaults to current user)
     */
    USER("user", "Objects by User", "Extract objects created/modified by a specific user"),

    /**
     * Extract objects from a package and its subpackages (recursive).
     *
     * <p>Discovery method:
     * <ol>
     *   <li>PackageHierarchyService.getPackageHierarchy() to find subpackages</li>
     *   <li>NavigationService.getPackageObjects() for each package</li>
     * </ol>
     *
     * <p>Input: package name(s), comma-separated
     */
    PACKAGE("package", "Objects by Package", "Extract objects from package hierarchy (recursive)"),

    /**
     * Extract objects from specific transport request(s).
     *
     * <p>Discovery method: TransportService.getTransportObjects()
     *
     * <p>Input: transport number(s), comma-separated
     */
    TRANSPORT("transport", "Objects by Transport", "Extract objects from specific transport request(s)"),

    /**
     * Extract specific objects by name.
     *
     * <p>Discovery method: SearchService.searchObjects() to resolve object types
     *
     * <p>Input: object names, comma-separated (any type: classes, programs, FMs, etc.)
     */
    LIST("list", "Specific Objects", "Extract specific objects by name");

    private final String code;
    private final String displayName;
    private final String description;

    ExtractionScope(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get the scope code used in MCP tool parameters.
     *
     * @return lowercase scope code (user, package, transport, list)
     */
    public String getCode() {
        return code;
    }

    /**
     * Get human-readable display name.
     *
     * @return display name for UI/logging
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get scope description.
     *
     * @return description of what this scope extracts
     */
    public String getDescription() {
        return description;
    }

    /**
     * Parse scope from string code.
     *
     * @param code scope code (case-insensitive)
     * @return matching ExtractionScope
     * @throws IllegalArgumentException if code doesn't match any scope
     */
    public static ExtractionScope fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Scope code cannot be null or empty");
        }

        String normalizedCode = code.trim().toLowerCase();
        for (ExtractionScope scope : values()) {
            if (scope.code.equals(normalizedCode)) {
                return scope;
            }
        }

        throw new IllegalArgumentException(
                "Invalid scope: '" + code + "'. Valid values: user, package, transport, list"
        );
    }

    @Override
    public String toString() {
        return code;
    }
}
