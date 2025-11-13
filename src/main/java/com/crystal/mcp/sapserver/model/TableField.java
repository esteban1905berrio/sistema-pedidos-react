package com.crystal.mcp.sapserver.model;

/**
 * Representa un campo de una tabla SAP Data Dictionary.
 *
 * <p>Utilizado para la creación de tablas mediante la tool MCP create_table.
 * El campo debe cumplir con las convenciones de nomenclatura ABAP:
 * <ul>
 *   <li>Nombre: máximo 16 caracteres, solo A-Z, 0-9, y guion bajo (_)</li>
 *   <li>Tipo: puede ser un tipo built-in de ABAP (ej: abap.char(10))
 *       o un tipo de referencia del diccionario (ej: matnr, gjahr)</li>
 * </ul>
 *
 * @author Crystal Development Team
 * @since 1.0
 */
public class TableField {

    /**
     * Nombre del campo (max 16 chars, A-Z0-9_).
     * Ejemplos: "mat", "gjahr", "description"
     */
    private String name;

    /**
     * Tipo de dato ABAP.
     *
     * <p>Puede ser:
     * <ul>
     *   <li>Tipo built-in: "abap.char(10)", "abap.numc(8)", "abap.dec(13,2)"</li>
     *   <li>Tipo de referencia: "matnr", "gjahr", "bukrs"</li>
     * </ul>
     */
    private String type;

    /**
     * Indica si el campo es parte de la clave primaria.
     *
     * <p>Nota: El campo "client" se agrega automáticamente como primer key field
     * en tablas client-dependent.
     */
    private boolean isKey;

    /**
     * Descripción del campo (opcional).
     *
     * <p>Para futura extensión: permitir documentación inline en DDL.
     * Actualmente no utilizado en la generación DDL.
     */
    private String description;

    // Constructor por defecto requerido para deserialización JSON (Spring MCP)
    public TableField() {
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param name Nombre del campo
     * @param type Tipo de dato ABAP
     * @param isKey true si es campo clave
     */
    public TableField(String name, String type, boolean isKey) {
        this.name = name;
        this.type = type;
        this.isKey = isKey;
    }

    /**
     * Constructor completo.
     *
     * @param name Nombre del campo
     * @param type Tipo de dato ABAP
     * @param isKey true si es campo clave
     * @param description Descripción del campo
     */
    public TableField(String name, String type, boolean isKey, String description) {
        this.name = name;
        this.type = type;
        this.isKey = isKey;
        this.description = description;
    }

    // Getters y Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isKey() {
        return isKey;
    }

    public void setKey(boolean key) {
        isKey = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Valida que el nombre del campo cumpla con las reglas ABAP.
     *
     * @return true si el nombre es válido
     */
    public boolean isValidName() {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.length() > 16) {
            return false;
        }
        // Solo A-Z, 0-9, y guion bajo
        return name.matches("^[A-Z0-9_]+$");
    }

    /**
     * Valida que el tipo de dato sea válido.
     *
     * <p>Validación básica: no vacío. Validación detallada delegada a DdlGenerator.
     *
     * @return true si el tipo no está vacío
     */
    public boolean isValidType() {
        return type != null && !type.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "TableField{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", isKey=" + isKey +
                (description != null ? ", description='" + description + '\'' : "") +
                '}';
    }
}
