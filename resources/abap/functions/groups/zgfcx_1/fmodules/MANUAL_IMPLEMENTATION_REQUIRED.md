# Manual Implementation Required: Z_CX_GET_PACKAGE_HIERARCHY

## Problem

El ADT API (`SADT_REST_RFC_ENDPOINT`) rechaza consistentemente las modificaciones de function modules con el error:

```
HTTP 400 - The statement FUNCTION is missing
```

Este es un **bug conocido o limitación** del ADT API cuando se intenta modificar function modules vía RFC/HTTP.

## Solution: Manual Code Entry in SE37

### Steps

1. **Open SE37 transaction** in SAP GUI
2. **Navigate to**: Function Group ZGFCX_1 → Function Module Z_CX_GET_PACKAGE_HIERARCHY
3. **Verify signature** is already configured:
   ```abap
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
     QUERY_ERROR
   ```

4. **Copy implementation code** from: `z_cx_get_package_hierarchy.abap` (in this directory)

5. **Paste into SE37 editor** (Source Code tab)

6. **Save** (Ctrl+S)

7. **Activate** (Ctrl+F3)

8. **Test in SE37**:
   ```abap
   IV_PACKAGE_NAME = 'ZCX'
   IV_MODE = 'C'
   IV_RECURSIVE = ''
   ```

## Implementation Code Location

Full implementation: `./z_cx_get_package_hierarchy.abap`

## Expected Behavior

### Children Mode (IV_MODE = 'C')
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
      "hasChildren": true
    }
  ],
  "totalPackages": 1
}
```

### Parents Mode (IV_MODE = 'P')
```json
{
  "success": true,
  "mode": "parents",
  "recursive": false,
  "packageName": "ZCXENH",
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

## Next Steps After Manual Implementation

Once the FM is successfully implemented and activated in SAP:

1. **Update TodoWrite**: Mark "Implementar lógica de consulta a TDEVC" as completed
2. **Proceed to Java MCP Tool**: Create PackageHierarchyService.java and PackageHierarchyTools.java
3. **Test via MCP**: Use giralmcp to call the FM and verify JSON responses

## Root Cause Analysis

**ADT API Limitation**: The `SADT_REST_RFC_ENDPOINT` function module used by the Java MCP server has limitations when modifying function module source code. The signature must be configured separately (done), and the implementation code must be entered manually in SE37.

**Future Solution**: Investigate alternative ADT endpoints or direct RFC calls to `RS_FUNCTIONMODULE_INSERT` for programmatic FM updates.

---

**Created**: 2025-11-20
**Status**: Awaiting manual implementation in SE37
**Blocker**: ADT API limitation
