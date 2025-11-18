package com.crystal.mcp.sapserver.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Result object for DDIC object structure retrieval.
 *
 * <p>Contains metadata about database tables, structures, and views
 * retrieved from DD02L and DD03L system tables.
 *
 * <p>Returned by ClassService.getDdicSource() which calls FM ZCX_GETDDICSOURCE.
 *
 * @author Crystal Development Team
 * @since 1.0
 */
public class DdicSourceResult {

    /**
     * Name of the DDIC object (table/structure/view).
     * Example: "MARA", "DD03L", "V_T001"
     */
    private String objectName;

    /**
     * Type of the DDIC object.
     * Values: "TABLE", "STRUCTURE", "VIEW", "APPEND", "UNKNOWN"
     */
    private String objectType;

    /**
     * Status of the object.
     * Values: "ACTIVE", "INACTIVE"
     */
    private String objectStatus;

    /**
     * Number of fields in the object.
     */
    private int fieldCount;

    /**
     * List of field metadata.
     */
    private List<DdicField> fields;

    /**
     * Raw JSON string from FM response (for debugging/reference).
     */
    private String rawJson;

    // Default constructor
    public DdicSourceResult() {
        this.fields = new ArrayList<>();
    }

    /**
     * Constructor with main parameters.
     *
     * @param objectName name of the DDIC object
     * @param objectType type (TABLE/STRUCTURE/VIEW)
     * @param objectStatus status (ACTIVE/INACTIVE)
     * @param fields list of field metadata
     */
    public DdicSourceResult(String objectName, String objectType,
                            String objectStatus, List<DdicField> fields) {
        this.objectName = objectName;
        this.objectType = objectType;
        this.objectStatus = objectStatus;
        this.fields = fields != null ? fields : new ArrayList<>();
        this.fieldCount = this.fields.size();
    }

    /**
     * Parse fields JSON string from FM ZCX_GETDDICSOURCE.
     *
     * <p>Expected format:
     * <pre>
     * [
     *   {
     *     "fieldname": "MANDT",
     *     "position": 1,
     *     "rollname": "MANDT",
     *     "mandatory": "X",
     *     "checktable": "T000",
     *     "adminfield": "0",
     *     "inttype": "C",
     *     "intlen": 3,
     *     "datatype": "CLNT",
     *     "keyflag": "X",
     *     "reffield": ""
     *   }
     * ]
     * </pre>
     *
     * @param fieldsJson JSON string from FM
     * @return list of DdicField objects
     */
    public static List<DdicField> parseFieldsJson(String fieldsJson) {
        List<DdicField> fields = new ArrayList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonArray = mapper.readTree(fieldsJson);

            if (jsonArray.isArray()) {
                for (JsonNode node : jsonArray) {
                    DdicField field = new DdicField();
                    field.setFieldname(node.get("fieldname").asText());
                    field.setPosition(node.get("position").asInt());
                    field.setRollname(node.get("rollname").asText());
                    field.setMandatory(node.get("mandatory").asText());
                    field.setChecktable(node.get("checktable").asText());
                    field.setAdminfield(node.get("adminfield").asText());
                    field.setInttype(node.get("inttype").asText());
                    field.setIntlen(node.get("intlen").asInt());
                    field.setDatatype(node.get("datatype").asText());
                    field.setKeyflag(node.get("keyflag").asText());
                    field.setReffield(node.get("reffield").asText());

                    fields.add(field);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse fields JSON: " + e.getMessage(), e);
        }

        return fields;
    }

    // Getters and setters

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectStatus() {
        return objectStatus;
    }

    public void setObjectStatus(String objectStatus) {
        this.objectStatus = objectStatus;
    }

    public int getFieldCount() {
        return fieldCount;
    }

    public void setFieldCount(int fieldCount) {
        this.fieldCount = fieldCount;
    }

    public List<DdicField> getFields() {
        return fields;
    }

    public void setFields(List<DdicField> fields) {
        this.fields = fields;
        this.fieldCount = fields != null ? fields.size() : 0;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    /**
     * Field metadata from DD03L.
     */
    public static class DdicField {
        private String fieldname;    // Field name
        private int position;         // Position in table
        private String rollname;      // Data element
        private String mandatory;     // 'X' if mandatory
        private String checktable;    // Foreign key table
        private String adminfield;    // Administrative field flag
        private String inttype;       // Internal type (C, N, D, etc.)
        private int intlen;          // Internal length
        private String datatype;      // ABAP data type
        private String keyflag;       // 'X' if key field
        private String reffield;      // Reference field

        // Getters and setters

        public String getFieldname() {
            return fieldname;
        }

        public void setFieldname(String fieldname) {
            this.fieldname = fieldname;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public String getRollname() {
            return rollname;
        }

        public void setRollname(String rollname) {
            this.rollname = rollname;
        }

        public String getMandatory() {
            return mandatory;
        }

        public void setMandatory(String mandatory) {
            this.mandatory = mandatory;
        }

        public String getChecktable() {
            return checktable;
        }

        public void setChecktable(String checktable) {
            this.checktable = checktable;
        }

        public String getAdminfield() {
            return adminfield;
        }

        public void setAdminfield(String adminfield) {
            this.adminfield = adminfield;
        }

        public String getInttype() {
            return inttype;
        }

        public void setInttype(String inttype) {
            this.inttype = inttype;
        }

        public int getIntlen() {
            return intlen;
        }

        public void setIntlen(int intlen) {
            this.intlen = intlen;
        }

        public String getDatatype() {
            return datatype;
        }

        public void setDatatype(String datatype) {
            this.datatype = datatype;
        }

        public String getKeyflag() {
            return keyflag;
        }

        public void setKeyflag(String keyflag) {
            this.keyflag = keyflag;
        }

        public String getReffield() {
            return reffield;
        }

        public void setReffield(String reffield) {
            this.reffield = reffield;
        }

        @Override
        public String toString() {
            return "DdicField{" +
                    "fieldname='" + fieldname + '\'' +
                    ", position=" + position +
                    ", rollname='" + rollname + '\'' +
                    ", keyflag='" + keyflag + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "DdicSourceResult{" +
                "objectName='" + objectName + '\'' +
                ", objectType='" + objectType + '\'' +
                ", objectStatus='" + objectStatus + '\'' +
                ", fieldCount=" + fieldCount +
                '}';
    }
}
