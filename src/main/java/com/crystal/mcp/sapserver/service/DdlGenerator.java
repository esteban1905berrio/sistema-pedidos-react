package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.TableField;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio para generar DDL (Data Definition Language) de objetos del diccionario SAP.
 *
 * <p>Convierte estructuras Java (List&lt;TableField&gt;) a sintaxis DDL válida para SAP.
 * Soporta generación de tablas transparentes con annotations estándar.
 *
 * <p><b>Características:</b>
 * <ul>
 *   <li>Generación de annotations @AbapCatalog estándar</li>
 *   <li>Ordenamiento automático: key fields primero, luego non-key fields</li>
 *   <li>Alineación de columnas para legibilidad</li>
 *   <li>Validación de tipos de datos ABAP</li>
 *   <li>Generación automática del campo "client" para tablas client-dependent</li>
 * </ul>
 *
 * @author Crystal Development Team
 * @since 1.0
 */
@Service
public class DdlGenerator {

    /**
     * Tipos de datos ABAP built-in válidos (prefijo).
     *
     * <p>Ejemplos: abap.char(10), abap.numc(8), abap.dec(13,2)
     */
    private static final Set<String> VALID_BUILTIN_TYPES = Set.of(
            "abap.char", "abap.numc", "abap.dec", "abap.curr",
            "abap.int1", "abap.int2", "abap.int4", "abap.int8",
            "abap.quan", "abap.dats", "abap.tims", "abap.clnt",
            "abap.lang", "abap.cuky", "abap.unit", "abap.raw",
            "abap.lchr", "abap.lraw", "abap.string", "abap.rawstring"
    );

    /**
     * Tipos de datos ABAP de referencia comunes (para validación básica).
     *
     * <p>Esta lista no es exhaustiva; SAP tiene miles de tipos de referencia.
     * La validación real se hace en tiempo de ejecución por SAP.
     */
    private static final Set<String> COMMON_REFERENCE_TYPES = Set.of(
            "matnr", "werks", "lgort", "bukrs", "gjahr", "monat",
            "vkorg", "vtweg", "spart", "kunnr", "lifnr", "pernr",
            "mandt", "spras", "waers", "meins", "vbeln", "posnr"
    );

    /**
     * Genera DDL completo para una tabla transparente.
     *
     * @param tableName Nombre de la tabla (lowercase esperado)
     * @param description Descripción de la tabla
     * @param fields Lista de campos (no incluir "client", se agrega automáticamente)
     * @return DDL completo con annotations y definición de tabla
     * @throws IllegalArgumentException si los parámetros son inválidos
     */
    public String generateTableDdl(String tableName, String description, List<TableField> fields) {
        // Validaciones básicas
        validateInput(tableName, description, fields);

        // Validar tipos de datos
        validateFieldTypes(fields);

        // Validar nombres duplicados
        validateNoDuplicateFieldNames(fields);

        StringBuilder ddl = new StringBuilder();

        // 1. Annotations estándar
        ddl.append(generateAnnotations(description));
        ddl.append("\n");

        // 2. Definición de tabla
        ddl.append("define table ").append(tableName.toLowerCase()).append(" {\n");

        // 3. Campo "client" (siempre primero para tablas client-dependent)
        ddl.append("  key client : abap.clnt;\n");

        // 4. Key fields (ordenados alfabéticamente para consistencia)
        List<TableField> keyFields = fields.stream()
                .filter(TableField::isKey)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());

        for (TableField field : keyFields) {
            ddl.append("  ").append(formatFieldLine(field, true)).append("\n");
        }

        // 5. Non-key fields (ordenados alfabéticamente)
        List<TableField> nonKeyFields = fields.stream()
                .filter(f -> !f.isKey())
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());

        for (TableField field : nonKeyFields) {
            ddl.append("  ").append(formatFieldLine(field, false)).append("\n");
        }

        // 6. Cierre de definición
        ddl.append("\n}");

        return ddl.toString();
    }

    /**
     * Genera las annotations estándar para una tabla.
     *
     * @param description Descripción de la tabla
     * @return String con todas las annotations
     */
    private String generateAnnotations(String description) {
        return String.format(
                "@EndUserText.label : '%s'\n" +
                "@AbapCatalog.enhancementCategory : #NOT_EXTENSIBLE\n" +
                "@AbapCatalog.tableCategory : #TRANSPARENT\n" +
                "@AbapCatalog.deliveryClass : #A\n" +
                "@AbapCatalog.dataMaintenance : #RESTRICTED",
                escapeSingleQuotes(description)
        );
    }

    /**
     * Formatea una línea de campo DDL.
     *
     * @param field Campo a formatear
     * @param isKey true si es campo clave
     * @return String formateado: "key fieldname : type;" o "  fieldname : type;"
     */
    private String formatFieldLine(TableField field, boolean isKey) {
        String prefix = isKey ? "key " : "  ";
        String fieldName = field.getName().toLowerCase();
        String type = field.getType().toLowerCase();

        // Alineación: nombres de campo alineados a columna, tipos alineados a otra columna
        // Ejemplo:
        // key mat    : matnr;
        //   gjahr    : gjahr;
        //   description : abap.char(255);

        // Para simplicidad, alineamos a 10 chars (ajustar si necesario)
        int nameWidth = 10;
        String paddedName = String.format("%-" + nameWidth + "s", fieldName);

        return prefix + paddedName + ": " + type + ";";
    }

    /**
     * Valida la entrada básica.
     *
     * @throws IllegalArgumentException si alguna validación falla
     */
    private void validateInput(String tableName, String description, List<TableField> fields) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be empty");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("Fields list cannot be empty");
        }

        // Validar que no haya campo "client" en la lista (se agrega automáticamente)
        boolean hasClientField = fields.stream()
                .anyMatch(f -> "client".equalsIgnoreCase(f.getName()));
        if (hasClientField) {
            throw new IllegalArgumentException(
                    "Field 'client' is added automatically, do not include it in the fields list");
        }
    }

    /**
     * Valida los tipos de datos de los campos.
     *
     * @param fields Lista de campos
     * @throws IllegalArgumentException si algún tipo es inválido
     */
    private void validateFieldTypes(List<TableField> fields) {
        for (TableField field : fields) {
            String type = field.getType().toLowerCase();

            // Validar que el tipo no esté vacío
            if (type.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Field '" + field.getName() + "' has empty type");
            }

            // Validar tipos built-in (con paréntesis)
            if (type.startsWith("abap.")) {
                String baseType = type.split("\\(")[0]; // "abap.char(10)" -> "abap.char"
                if (!VALID_BUILTIN_TYPES.contains(baseType)) {
                    throw new IllegalArgumentException(
                            "Field '" + field.getName() + "' has invalid built-in type: " + type);
                }
            }
            // Los tipos de referencia no se validan exhaustivamente aquí
            // (SAP tiene miles de tipos custom). La validación real la hace SAP en runtime.
        }
    }

    /**
     * Valida que no haya nombres de campos duplicados.
     *
     * @param fields Lista de campos
     * @throws IllegalArgumentException si hay duplicados
     */
    private void validateNoDuplicateFieldNames(List<TableField> fields) {
        Set<String> seen = new HashSet<>();
        for (TableField field : fields) {
            String nameLower = field.getName().toLowerCase();
            if (seen.contains(nameLower)) {
                throw new IllegalArgumentException(
                        "Duplicate field name: " + field.getName());
            }
            seen.add(nameLower);
        }
    }

    /**
     * Escapa comillas simples en strings para DDL.
     *
     * @param str String a escapar
     * @return String escapado
     */
    private String escapeSingleQuotes(String str) {
        if (str == null) {
            return "";
        }
        // En DDL, las comillas simples se escapan duplicándolas
        return str.replace("'", "''");
    }

    /**
     * Valida si un tipo de dato ABAP es válido (validación básica).
     *
     * <p>Nota: Esta es una validación básica. SAP validará exhaustivamente
     * al momento de crear el objeto.
     *
     * @param type Tipo de dato a validar
     * @return true si el tipo parece válido
     */
    public boolean isValidAbapType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return false;
        }

        String typeLower = type.toLowerCase();

        // Built-in types
        if (typeLower.startsWith("abap.")) {
            String baseType = typeLower.split("\\(")[0];
            return VALID_BUILTIN_TYPES.contains(baseType);
        }

        // Reference types (validación básica: solo A-Z, 0-9, _)
        return typeLower.matches("^[a-z0-9_]+$");
    }
}
