package com.crystal.mcp.sapserver.model;

/**
 * DTO de salida para operaciones de creación de objetos del diccionario de datos SAP.
 *
 * <p>Retornado por la tool MCP create_table después de crear exitosamente una tabla.
 * Contiene información sobre el objeto creado, incluyendo su URI ADT, versión,
 * y detalles de transporte si aplica.
 *
 * @author Crystal Development Team
 * @since 1.0
 */
public class DictionaryObjectResult {

    /**
     * URI ADT del objeto creado.
     * Ejemplo: "/sap/bc/adt/ddic/tables/ytmp_1"
     */
    private String uri;

    /**
     * Nombre del objeto creado (uppercase).
     * Ejemplo: "YTMP_1"
     */
    private String name;

    /**
     * Versión del objeto.
     * Valores típicos: "active", "inactive"
     *
     * <p>Nota: Los objetos recién creados están en versión "inactive"
     * hasta que se activen explícitamente.
     */
    private String version;

    /**
     * Nombre del paquete donde se creó el objeto.
     * Ejemplo: "$TMP", "ZTEST"
     */
    private String packageName;

    /**
     * Número de orden de transporte asignada (si aplica).
     * null si el objeto es local ($TMP).
     * Ejemplo: "CADK911122"
     */
    private String transport;

    /**
     * Indica si el objeto es local (creado en $TMP).
     * true si packageName = "$TMP", false en caso contrario.
     */
    private boolean isLocal;

    /**
     * Timestamp de creación (ISO 8601 format).
     * Ejemplo: "2025-11-13T14:32:14Z"
     */
    private String createdAt;

    /**
     * Usuario que creó el objeto.
     * Ejemplo: "SEBLONDO"
     */
    private String createdBy;

    /**
     * Mensaje de éxito o información adicional.
     * Ejemplo: "Table YTMP_1 created successfully in package $TMP"
     */
    private String message;

    // Constructor por defecto
    public DictionaryObjectResult() {
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param uri URI ADT del objeto
     * @param name Nombre del objeto
     * @param version Versión del objeto
     * @param packageName Paquete
     * @param transport Orden de transporte
     * @param isLocal true si es local ($TMP)
     */
    public DictionaryObjectResult(String uri, String name, String version,
                                   String packageName, String transport, boolean isLocal) {
        this.uri = uri;
        this.name = name;
        this.version = version;
        this.packageName = packageName;
        this.transport = transport;
        this.isLocal = isLocal;
    }

    // Getters y Setters

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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean local) {
        isLocal = local;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Builder para construcción fluida del resultado.
     *
     * @return nueva instancia de Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern para DictionaryObjectResult.
     */
    public static class Builder {
        private final DictionaryObjectResult result;

        public Builder() {
            this.result = new DictionaryObjectResult();
        }

        public Builder uri(String uri) {
            result.uri = uri;
            return this;
        }

        public Builder name(String name) {
            result.name = name;
            return this;
        }

        public Builder version(String version) {
            result.version = version;
            return this;
        }

        public Builder packageName(String packageName) {
            result.packageName = packageName;
            return this;
        }

        public Builder transport(String transport) {
            result.transport = transport;
            return this;
        }

        public Builder isLocal(boolean isLocal) {
            result.isLocal = isLocal;
            return this;
        }

        public Builder createdAt(String createdAt) {
            result.createdAt = createdAt;
            return this;
        }

        public Builder createdBy(String createdBy) {
            result.createdBy = createdBy;
            return this;
        }

        public Builder message(String message) {
            result.message = message;
            return this;
        }

        public DictionaryObjectResult build() {
            return result;
        }
    }

    @Override
    public String toString() {
        return "DictionaryObjectResult{" +
                "uri='" + uri + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", packageName='" + packageName + '\'' +
                ", transport='" + transport + '\'' +
                ", isLocal=" + isLocal +
                ", createdAt='" + createdAt + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
