# PR: Get Package Hierarchy

## Requirement

Implementar una herramienta MCP para recuperar la jerarquía de paquetes SAP (padres e hijos) a partir de la tabla TDEVC.

**User Story**: Como desarrollador, necesito conocer la estructura jerárquica de paquetes SAP para entender las relaciones padre-hijo y poder extraer objetos de paquetes relacionados.

---

## Technical Design

### ABAP Function Module: Z_CX_GET_PACKAGE_HIERARCHY

**Function Group**: ZGFCX_1
**Purpose**: Query TDEVC table to get package hierarchy (parents or children)

#### Signature

```abap
FUNCTION Z_CX_GET_PACKAGE_HIERARCHY
  IMPORTING
    VALUE(IV_PACKAGE_NAME) TYPE DEVCLASS
    VALUE(IV_MODE) TYPE CHAR1 DEFAULT 'C'
    VALUE(IV_RECURSIVE) TYPE CHAR1 DEFAULT ''
  EXPORTING
    VALUE(EV_SUCCESS) TYPE CHAR1
    VALUE(EV_MESSAGE) TYPE STRING
    VALUE(EV_HIERARCHY_JSON) TYPE STRING
  EXCEPTIONS
    PACKAGE_NOT_FOUND
    QUERY_ERROR.
```

#### Parameters

**IMPORTING**:
- `IV_PACKAGE_NAME` (TYPE DEVCLASS): Package name to query
- `IV_MODE` (TYPE CHAR1): Query mode
  - `'C'` = Get Children (default) - returns subpackages
  - `'P'` = Get Parents - returns parent packages
- `IV_RECURSIVE` (TYPE CHAR1): Recursive search
  - `''` = Only direct level (default)
  - `'X'` = Recursive (all levels)

**EXPORTING**:
- `EV_SUCCESS` (TYPE CHAR1): 'X' = success, '' = error
- `EV_MESSAGE` (TYPE STRING): Status message
- `EV_HIERARCHY_JSON` (TYPE STRING): JSON with hierarchy results

**EXCEPTIONS**:
- `PACKAGE_NOT_FOUND`: Package does not exist in TDEVC
- `QUERY_ERROR`: Database query error

---

### TDEVC Table Structure

```
TDEVC - Package Header
├── DEVCLASS (PK)      - Package Name
├── PARENTCL           - Parent Package
├── CTEXT              - Short Text
├── CREATED_ON         - Creation Date
├── AS4USER            - Creator
└── DLVUNIT            - Software Component
```

**Query Patterns**:

1. **Get Children (Mode = 'C')**:
```sql
SELECT devclass, parentcl, ctext
  FROM tdevc
  WHERE parentcl = iv_package_name
```

2. **Get Parents (Mode = 'P')**:
```sql
SELECT devclass, parentcl, ctext
  FROM tdevc
  WHERE devclass = iv_package_name
-- Then recursively query PARENTCL
```

---

### JSON Response Format

#### Children Mode ('C')

```json
{
  "success": true,
  "mode": "children",
  "recursive": false,
  "packageName": "ZCX",
  "hierarchy": [
    {
      "packageName": "ZCXENH",
      "parentPackage": "ZCX",
      "description": "Enhancements Package",
      "level": 1,
      "hasChildren": true,
      "children": []
    },
    {
      "packageName": "ZCXR1003",
      "parentPackage": "ZCX",
      "description": "R1003 Package",
      "level": 1,
      "hasChildren": false,
      "children": []
    }
  ],
  "totalPackages": 2
}
```

#### Parents Mode ('P')

```json
{
  "success": true,
  "mode": "parents",
  "recursive": false,
  "packageName": "ZCXR1003",
  "hierarchy": [
    {
      "packageName": "ZCX",
      "parentPackage": "",
      "description": "Crystal Main Package",
      "level": 1,
      "hasChildren": true
    }
  ],
  "totalPackages": 1
}
```

#### Recursive Children Mode ('C' + 'X')

```json
{
  "success": true,
  "mode": "children",
  "recursive": true,
  "packageName": "ZCX",
  "hierarchy": [
    {
      "packageName": "ZCXENH",
      "parentPackage": "ZCX",
      "description": "Enhancements Package",
      "level": 1,
      "hasChildren": true,
      "children": [
        {
          "packageName": "ZCXENH_SUB1",
          "parentPackage": "ZCXENH",
          "description": "Sub Enhancement",
          "level": 2,
          "hasChildren": false,
          "children": []
        }
      ]
    },
    {
      "packageName": "ZCXR1003",
      "parentPackage": "ZCX",
      "description": "R1003 Package",
      "level": 1,
      "hasChildren": false,
      "children": []
    }
  ],
  "totalPackages": 3
}
```

---

### ABAP Implementation Logic

#### Algorithm (Children Mode - Non-Recursive)

```abap
DATA: lt_packages TYPE STANDARD TABLE OF tdevc,
      lv_json     TYPE string.

" Step 1: Validate package exists
SELECT SINGLE devclass
  FROM tdevc
  INTO @DATA(lv_check)
  WHERE devclass = @iv_package_name.

IF sy-subrc <> 0.
  RAISE package_not_found.
ENDIF.

" Step 2: Get direct children
SELECT devclass, parentcl, ctext
  FROM tdevc
  INTO TABLE @lt_packages
  WHERE parentcl = @iv_package_name.

" Step 3: Build JSON
" ... (JSON construction logic)
```

#### Algorithm (Children Mode - Recursive)

```abap
" Recursive approach using internal table for levels
DATA: lt_all_packages TYPE STANDARD TABLE OF tdevc,
      lt_temp_packages TYPE STANDARD TABLE OF tdevc,
      lv_level TYPE i VALUE 1.

" Get direct children
SELECT devclass, parentcl, ctext
  FROM tdevc
  INTO TABLE @lt_all_packages
  WHERE parentcl = @iv_package_name.

" While there are packages at current level
WHILE lt_all_packages IS NOT INITIAL.
  CLEAR lt_temp_packages.

  " For each package at current level, find its children
  LOOP AT lt_all_packages INTO DATA(ls_pkg).
    SELECT devclass, parentcl, ctext
      FROM tdevc
      APPENDING TABLE @lt_temp_packages
      WHERE parentcl = @ls_pkg-devclass.
  ENDLOOP.

  " Add children to result
  APPEND LINES OF lt_temp_packages TO lt_all_packages.

  " Exit if no more children
  IF lt_temp_packages IS INITIAL.
    EXIT.
  ENDIF.

  lv_level = lv_level + 1.
ENDWHILE.
```

#### Algorithm (Parents Mode)

```abap
DATA: lt_parents TYPE STANDARD TABLE OF tdevc,
      lv_current_pkg TYPE devclass,
      lv_level TYPE i VALUE 1.

lv_current_pkg = iv_package_name.

" Navigate up the hierarchy
WHILE lv_current_pkg IS NOT INITIAL.
  SELECT SINGLE devclass, parentcl, ctext
    FROM tdevc
    INTO @DATA(ls_parent)
    WHERE devclass = @lv_current_pkg.

  IF sy-subrc <> 0.
    EXIT.
  ENDIF.

  APPEND ls_parent TO lt_parents.

  " Move to parent
  lv_current_pkg = ls_parent-parentcl.

  " Exit if no parent or non-recursive
  IF lv_current_pkg IS INITIAL OR iv_recursive <> 'X'.
    EXIT.
  ENDIF.

  lv_level = lv_level + 1.
ENDWHILE.
```

---

### Java MCP Tool Implementation

#### Service: PackageHierarchyService.java

```java
package com.crystal.mcp.sapserver.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PackageHierarchyService {

    private final RfcAdapter rfcAdapter;
    private final ObjectMapper objectMapper;

    public PackageHierarchyService(RfcAdapter rfcAdapter, ObjectMapper objectMapper) {
        this.rfcAdapter = rfcAdapter;
        this.objectMapper = objectMapper;
    }

    public PackageHierarchyResult getPackageHierarchy(
            String packageName,
            String mode,
            boolean recursive) throws Exception {

        // Call FM via RFC
        Map<String, String> params = new HashMap<>();
        params.put("IV_PACKAGE_NAME", packageName.toUpperCase());
        params.put("IV_MODE", mode.toUpperCase());
        params.put("IV_RECURSIVE", recursive ? "X" : "");

        RfcResponse response = rfcAdapter.callFunction(
            "Z_CX_GET_PACKAGE_HIERARCHY",
            params
        );

        // Parse JSON response
        String hierarchyJson = response.getExportParam("EV_HIERARCHY_JSON");

        return new PackageHierarchyResult(
            response.getExportParam("EV_SUCCESS").equals("X"),
            response.getExportParam("EV_MESSAGE"),
            objectMapper.readTree(hierarchyJson)
        );
    }
}

record PackageHierarchyResult(
    boolean success,
    String message,
    JsonNode hierarchy
) {}
```

#### Tool: PackageHierarchyTools.java

```java
package com.crystal.mcp.sapserver.tool;

import org.springframework.ai.mcp.spec.McpSchema;
import org.springframework.ai.mcp.spring.McpTool;
import org.springframework.stereotype.Component;

@Component
public class PackageHierarchyTools {

    private final PackageHierarchyService packageHierarchyService;

    public PackageHierarchyTools(PackageHierarchyService packageHierarchyService) {
        this.packageHierarchyService = packageHierarchyService;
    }

    @McpTool(
        description = """
            Get SAP package hierarchy (children or parents).

            Progressive Discovery Stage 0: Find related packages before listing objects.
            Use before get_package_objects to understand package structure.

            Mode 'C' (children): Get subpackages of a parent package.
            Mode 'P' (parents): Get parent packages of a child package.

            Examples:
            - get_package_hierarchy('ZCX', 'C', false) -> direct children of ZCX
            - get_package_hierarchy('ZCX', 'C', true) -> all descendants of ZCX (recursive)
            - get_package_hierarchy('ZCXR1003', 'P', false) -> direct parent
            - get_package_hierarchy('ZCXR1003', 'P', true) -> all ancestors (recursive)
            """,
        schema = @McpSchema(
            properties = {
                @McpSchema.Property(
                    name = "packageName",
                    description = "Package name to query. Examples: 'ZCX', 'ZCXR1003', '$TMP'",
                    type = McpSchema.Type.STRING,
                    required = true
                ),
                @McpSchema.Property(
                    name = "mode",
                    description = "Query mode: 'C' for children (default), 'P' for parents",
                    type = McpSchema.Type.STRING,
                    required = false
                ),
                @McpSchema.Property(
                    name = "recursive",
                    description = "Recursive search: false for direct level only (default), true for all levels",
                    type = McpSchema.Type.BOOLEAN,
                    required = false
                )
            }
        )
    )
    public String getPackageHierarchy(
            String packageName,
            String mode,
            Boolean recursive) {

        try {
            String queryMode = (mode != null) ? mode : "C";
            boolean isRecursive = (recursive != null) ? recursive : false;

            PackageHierarchyResult result = packageHierarchyService.getPackageHierarchy(
                packageName,
                queryMode,
                isRecursive
            );

            if (!result.success()) {
                return String.format("{\"success\":false,\"error\":\"%s\"}",
                    result.message());
            }

            return result.hierarchy().toString();

        } catch (Exception e) {
            return String.format("{\"success\":false,\"error\":\"%s\"}",
                e.getMessage());
        }
    }
}
```

---

## Usage Examples

### Example 1: Get Direct Children of ZCX

**MCP Tool Call**:
```
get_package_hierarchy(packageName='ZCX', mode='C', recursive=false)
```

**Result**:
```json
{
  "success": true,
  "mode": "children",
  "recursive": false,
  "packageName": "ZCX",
  "hierarchy": [
    {"packageName": "ZCXENH", "parentPackage": "ZCX", "level": 1},
    {"packageName": "ZCXR1003", "parentPackage": "ZCX", "level": 1}
  ],
  "totalPackages": 2
}
```

### Example 2: Get All Descendants of ZCX (Recursive)

**MCP Tool Call**:
```
get_package_hierarchy(packageName='ZCX', mode='C', recursive=true)
```

**Result**: Full tree with all levels

### Example 3: Get Parent of ZCXR1003

**MCP Tool Call**:
```
get_package_hierarchy(packageName='ZCXR1003', mode='P', recursive=false)
```

**Result**:
```json
{
  "success": true,
  "mode": "parents",
  "packageName": "ZCXR1003",
  "hierarchy": [
    {"packageName": "ZCX", "parentPackage": "", "level": 1}
  ],
  "totalPackages": 1
}
```

---

## Testing Strategy

### Manual Tests

1. **Test Children Mode (Direct)**:
```abap
CALL FUNCTION 'Z_CX_GET_PACKAGE_HIERARCHY'
  EXPORTING
    iv_package_name = 'ZCX'
    iv_mode         = 'C'
    iv_recursive    = ''
  IMPORTING
    ev_success      = lv_success
    ev_hierarchy_json = lv_json.
```

2. **Test Children Mode (Recursive)**:
```abap
CALL FUNCTION 'Z_CX_GET_PACKAGE_HIERARCHY'
  EXPORTING
    iv_package_name = 'ZCX'
    iv_mode         = 'C'
    iv_recursive    = 'X'
  IMPORTING
    ev_success      = lv_success
    ev_hierarchy_json = lv_json.
```

3. **Test Parents Mode**:
```abap
CALL FUNCTION 'Z_CX_GET_PACKAGE_HIERARCHY'
  EXPORTING
    iv_package_name = 'ZCXR1003'
    iv_mode         = 'P'
    iv_recursive    = 'X'
  IMPORTING
    ev_success      = lv_success
    ev_hierarchy_json = lv_json.
```

### Integration Tests (Java)

```java
@Test
void testGetPackageHierarchy_Children() {
    PackageHierarchyResult result = packageHierarchyService.getPackageHierarchy(
        "ZCX", "C", false
    );

    assertTrue(result.success());
    assertTrue(result.hierarchy().get("totalPackages").asInt() >= 0);
}

@Test
void testGetPackageHierarchy_Parents() {
    PackageHierarchyResult result = packageHierarchyService.getPackageHierarchy(
        "ZCXR1003", "P", false
    );

    assertTrue(result.success());
}
```

---

## Implementation Checklist

- [ ] Create FM Z_CX_GET_PACKAGE_HIERARCHY in SAP (function group ZGFCX_1)
- [ ] Implement children mode (direct)
- [ ] Implement children mode (recursive)
- [ ] Implement parents mode (direct)
- [ ] Implement parents mode (recursive)
- [ ] Test FM in SAP using SE37
- [ ] Create PackageHierarchyService.java
- [ ] Create PackageHierarchyTools.java
- [ ] Add integration tests
- [ ] Update ZGFCX_1 README.md
- [ ] Update giralmcp README_JAVA.md with new tool

---

## Related Documentation

- **TDEVC Table**: SAP table for package headers
- **Function Group**: `resources/abap/functions/groups/zgfcx_1/`
- **Java Implementation**: `src/main/java/com/crystal/mcp/sapserver/`

---

**Created**: 2025-11-20
**Status**: Design Complete - Ready for Implementation
