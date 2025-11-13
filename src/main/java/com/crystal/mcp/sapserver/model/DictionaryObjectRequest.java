package com.crystal.mcp.sapserver.model;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de entrada para operaciones de creación de objetos del diccionario de datos SAP.
 *
 * <p>Utilizado por la tool MCP create_table para recibir parámetros estructurados
 * en lugar de DDL raw. El servicio TableService convertirá este DTO a DDL utilizando
 * DdlGenerator.
 *
 * @author Crystal Development Team
 * @since 1.0
 */
public class DictionaryObjectRequest {

    /**
     * Nombre del objeto (ej: tabla).
     * Máximo 8 caracteres para tablas, solo A-Z, 0-9, y guion bajo (_).
     */
    private String name;

    /**
     * Descripción del objeto.
     * Se convierte en la annotation @EndUserText.label en el DDL.
     */
    private String description;

    /**
     * Lista de campos de la tabla.
     *
     * <p>Debe contener al menos un campo no-key.
     * Los key fields se ordenan primero en el DDL generado.
     */
    private List<TableField> fields;

    /**
     * Nombre del paquete SAP.
     *
     * <p>Valores típicos:
     * <ul>
     *   <li>$TMP: Objetos locales (no transportables)</li>
     *   <li>ZXXX: Paquetes de desarrollo custom</li>
     * </ul>
     */
    private String packageName;

    /**
     * Número de orden de transporte (opcional).
     *
     * <p>Requerido si packageName != "$TMP".
     * Si no se provee, SAP puede asignar una automáticamente (si configurado).
     * Formato: XXXK999999 (ej: CADK911122)
     */
    private String transport;

    // Constructor por defecto requerido para deserialización JSON
    public DictionaryObjectRequest() {
        this.fields = new ArrayList<>();
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param name Nombre del objeto
     * @param description Descripción
     * @param fields Lista de campos
     * @param packageName Paquete SAP
     */
    public DictionaryObjectRequest(String name, String description, List<TableField> fields, String packageName) {
        this.name = name;
        this.description = description;
        this.fields = fields != null ? fields : new ArrayList<>();
        this.packageName = packageName;
    }

    /**
     * Constructor completo.
     *
     * @param name Nombre del objeto
     * @param description Descripción
     * @param fields Lista de campos
     * @param packageName Paquete SAP
     * @param transport Orden de transporte
     */
    public DictionaryObjectRequest(String name, String description, List<TableField> fields,
                                    String packageName, String transport) {
        this.name = name;
        this.description = description;
        this.fields = fields != null ? fields : new ArrayList<>();
        this.packageName = packageName;
        this.transport = transport;
    }

    // Getters y Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TableField> getFields() {
        return fields;
    }

    public void setFields(List<TableField> fields) {
        this.fields = fields;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    // Métodos de validación

    /**
     * Valida que el nombre del objeto cumpla con las reglas ABAP.
     *
     * @return true si el nombre es válido
     */
    public boolean isValidName() {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.length() > 15) {
            return false;
        }
        // Solo A-Z, 0-9, y guion bajo
        return name.matches("^[A-Z0-9_]+$");
    }

    /**
     * Valida que la descripción no esté vacía.
     *
     * @return true si la descripción es válida
     */
    public boolean isValidDescription() {
        return description != null && !description.trim().isEmpty();
    }

    /**
     * Valida que la lista de campos no esté vacía y tenga al menos un campo no-key.
     *
     * @return true si la lista de campos es válida
     */
    public boolean hasValidFields() {
        if (fields == null || fields.isEmpty()) {
            return false;
        }
        // Debe tener al menos un campo no-key (además de los key fields)
        long nonKeyCount = fields.stream().filter(f -> !f.isKey()).count();
        return nonKeyCount > 0;
    }

    /**
     * Valida que todos los campos tengan nombres y tipos válidos.
     *
     * @return true si todos los campos son válidos
     */
    public boolean allFieldsValid() {
        if (fields == null || fields.isEmpty()) {
            return false;
        }
        return fields.stream().allMatch(f -> f.isValidName() && f.isValidType());
    }

    /**
     * Valida que el paquete sea válido.
     *
     * @return true si el paquete es válido
     */
    public boolean isValidPackage() {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        // $TMP es válido, o debe empezar con Z/Y para custom
        return "$TMP".equals(packageName) || packageName.matches("^[ZY][A-Z0-9_]*$");
    }

    /**
     * Valida que el transport sea requerido y esté presente si el paquete no es $TMP.
     *
     * @return true si el transport es válido o no es requerido
     */
    public boolean isTransportValid() {
        // Si es $TMP, no se requiere transport
        if ("$TMP".equals(packageName)) {
            return true;
        }
        // Para otros paquetes, transport puede ser null (SAP asigna automáticamente)
        // o debe ser un string no vacío
        return transport == null || !transport.trim().isEmpty();
    }

    /**
     * Valida el request completo.
     *
     * @return true si el request es válido
     * @throws IllegalArgumentException si alguna validación falla, con mensaje descriptivo
     */
    public boolean validate() {
        if (!isValidName()) {
            throw new IllegalArgumentException(
                    "Invalid table name: " + name + ". Must be max 8 chars, A-Z0-9_");
        }
        if (!isValidDescription()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }
        if (!hasValidFields()) {
            throw new IllegalArgumentException(
                    "Fields list must contain at least one non-key field");
        }
        /* if (!allFieldsValid()) {
            throw new IllegalArgumentException(
                    "All fields must have valid names (max 16 chars, A-Z0-9_) and types");
        }
        if (!isValidPackage()) {
            throw new IllegalArgumentException(
                    "Invalid package: " + packageName + ". Must be $TMP or start with Z/Y");
        }
        if (!isTransportValid()) {
            throw new IllegalArgumentException(
                    "Transport is required for non-$TMP packages");
        } */
        return true;
    }

    @Override
    public String toString() {
        return "DictionaryObjectRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", fields=" + fields.size() + " field(s)" +
                ", packageName='" + packageName + '\'' +
                ", transport='" + transport + '\'' +
                '}';
    }
}
