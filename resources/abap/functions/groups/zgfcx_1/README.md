# Function Group ZGFCX_1

## Overview

Function group containing MCP (Model Context Protocol) helper functions for SAP operations.

**Package**: `$TMP` (Local objects)
**System**: GDC
**Retrieved**: 2025-11-20

---

## Structure

```
zgfcx_1/
├── README.md                                    # This file
├── lzgfcx_1top.abap                            # TOP include (main declarations)
├── lzgfcx_1uxx.abap                            # Function module includes index
└── fmodules/                                   # Function modules
    ├── zcx_create_transport_copy.abap          # Create transport copy
    ├── zcx_getddicsource.abap                  # Get DDIC object structure
    ├── z_cx_get_transport_objects.abap         # Get transport objects
    ├── z_cx_get_object_in_open_ot.abap         # Find objects in open transports
    └── z_cx_get_transport_info.abap            # Get transport metadata
```

---

## Function Modules

### 1. ZCX_CREATE_TRANSPORT_COPY

**Purpose**: Create a copy of an existing transport request with all its objects.

**Signature**:
```abap
FUNCTION ZCX_CREATE_TRANSPORT_COPY
  IMPORTING
    VALUE(IV_TRANSPORT_REQUEST) TYPE STRING          " Source transport(s) - comma-separated
    VALUE(IV_TARGET_SYSTEM) TYPE TMSCSYS-SYSNAM OPTIONAL
    VALUE(IV_DESCRIPTION_PREFIX) TYPE STRING DEFAULT 'COPIA'
    VALUE(IV_AUTO_RELEASE) TYPE CHAR1 DEFAULT ABAP_TRUE
  EXPORTING
    VALUE(EV_NEW_TRANSPORT) TYPE TRKORR             " New transport number
    VALUE(EV_STATUS) TYPE CHAR1                     " 'S' = Success, 'E' = Error
    VALUE(EV_MESSAGE) TYPE STRING
    VALUE(EV_LOG) TYPE STRING
  EXCEPTIONS
    TRANSPORT_NOT_FOUND
    CREATION_FAILED
    OBJECTS_COPY_FAILED
    RELEASE_FAILED.
```

**Features**:
- Supports single or multiple source transports (comma-separated)
- Auto-generates description with prefix
- Optional auto-release after creation
- Validates transport existence before copy
- Uses class `ZCLCX_TRANSPORT_MANAGEMENT` for copy logic

**Example**:
```abap
CALL FUNCTION 'ZCX_CREATE_TRANSPORT_COPY'
  EXPORTING
    iv_transport_request = 'S4DK123456'
    iv_target_system     = 'S4D'
    iv_description_prefix = 'BACKUP'
    iv_auto_release      = abap_true
  IMPORTING
    ev_new_transport     = lv_new_ot
    ev_status            = lv_status
    ev_message           = lv_message
  EXCEPTIONS
    transport_not_found  = 1
    creation_failed      = 2
    OTHERS               = 3.
```

---

### 2. ZCX_GETDDICSOURCE

**Purpose**: Retrieve DDIC object structure (table/structure/view) from DD03L.

**Signature**:
```abap
FUNCTION ZCX_GETDDICSOURCE
  IMPORTING
    VALUE(OBJECT_NAME) TYPE TABNAME                 " Table/structure/view name
  EXPORTING
    VALUE(OBJECT_TYPE) TYPE CHAR10                  " TABLE, STRUCTURE, VIEW, APPEND
    VALUE(OBJECT_STATUS) TYPE CHAR10                " ACTIVE
    VALUE(FIELDS_JSON) TYPE STRING                  " JSON array of fields
  EXCEPTIONS
    OBJECT_NOT_FOUND
    INVALID_OBJECT_TYPE.
```

**Features**:
- Returns field metadata: name, type, key flag, foreign keys
- Supports tables (TRANSP), structures (INTTAB), views, append structures
- Returns JSON format for easy parsing
- Filters out .INCLUDE pseudo-fields

**Field JSON Structure**:
```json
[
  {
    "fieldname": "MANDT",
    "position": 1,
    "rollname": "MANDT",
    "mandatory": "",
    "checktable": "",
    "adminfield": "",
    "inttype": "C",
    "intlen": 3,
    "datatype": "CLNT",
    "keyflag": "X",
    "reffield": ""
  },
  ...
]
```

**Example**:
```abap
CALL FUNCTION 'ZCX_GETDDICSOURCE'
  EXPORTING
    object_name     = 'MARA'
  IMPORTING
    object_type     = lv_type
    object_status   = lv_status
    fields_json     = lv_fields_json
  EXCEPTIONS
    object_not_found = 1
    OTHERS          = 2.
```

---

### 3. Z_CX_GET_TRANSPORT_OBJECTS

**Purpose**: Get all objects and tasks from a transport request.

**Signature**:
```abap
FUNCTION Z_CX_GET_TRANSPORT_OBJECTS
  IMPORTING
    VALUE(IV_TRANSPORT_NUMBER) TYPE TRKORR
    VALUE(IV_TASK_NUMBER) TYPE TRKORR OPTIONAL      " Filter by task
  EXPORTING
    VALUE(EV_SUCCESS) TYPE CHAR1
    VALUE(EV_MESSAGE) TYPE STRING
    VALUE(EV_TRANSPORT_JSON) TYPE STRING            " Complete JSON response
  EXCEPTIONS
    TRANSPORT_NOT_FOUND
    QUERY_ERROR.
```

**Features**:
- Returns transport metadata (owner, status, dates, description)
- Lists all objects (E071 entries)
- Lists all tasks (for main transports)
- Optional filtering by task number
- Detailed status and type descriptions

**JSON Structure**:
```json
{
  "success": true,
  "transport_number": "S4DK123456",
  "metadata": {
    "transport_number": "S4DK123456",
    "transport_type": "K",
    "transport_type_desc": "Workbench",
    "status": "D",
    "status_desc": "Modifiable",
    "owner": "USERNAME",
    "created_date": "2025-11-20",
    "created_time": "15:30:45",
    "target_system": "S4D",
    "category": "CUST",
    "description": "",
    "parent_transport": ""
  },
  "objects": [
    {
      "trkorr": "S4DK123456",
      "pgmid": "R3TR",
      "object_type": "CLAS",
      "object_name": "ZCL_TEST",
      "lock_flag": "X",
      "gennum": "001",
      "tab_key": ""
    }
  ],
  "total_objects": 1,
  "tasks": [
    {
      "task_number": "S4DK123457",
      "owner": "USERNAME",
      "created_date": "2025-11-20",
      "created_time": "15:31:00",
      "status": "D",
      "status_desc": "Modifiable",
      "description": "",
      "object_count": 1
    }
  ]
}
```

---

### 4. Z_CX_GET_OBJECT_IN_OPEN_OT

**Purpose**: Find if an ABAP object exists in open (non-released) transport requests.

**Signature**:
```abap
FUNCTION Z_CX_GET_OBJECT_IN_OPEN_OT
  IMPORTING
    VALUE(IV_OBJECT_NAME) TYPE STRING               " Object name pattern
    VALUE(IV_OBJECT_TYPE) TYPE STRING OPTIONAL      " Filter by object type
  EXPORTING
    VALUE(EV_SUCCESS) TYPE CHAR1
    VALUE(EV_MESSAGE) TYPE STRING
    VALUE(EV_RESULTS_JSON) TYPE STRING.
```

**Features**:
- Searches E071 with partial matching (LIKE '%name%')
- Filters only open transports (status D or L)
- Returns transport details and lock status
- Useful for checking if object is locked before modification

**Use Cases**:
- "Can I modify this object? Who has it locked?"
- Find all transports containing a specific object
- Check if object is in modifiable transport

**JSON Structure**:
```json
{
  "success": true,
  "objectName": "ZCL_TEST",
  "searchPattern": "%ZCL_TEST%",
  "transports": [
    {
      "transportNumber": "S4DK123456",
      "transportType": "K",
      "transportTypeDesc": "Workbench",
      "status": "D",
      "statusDesc": "Modifiable",
      "owner": "USERNAME",
      "createdDate": "2025-11-20",
      "createdTime": "15:30:45",
      "isLocked": true,
      "objectInfo": {
        "objName": "ZCL_TEST",
        "objectType": "CLAS",
        "pgmid": "R3TR"
      },
      "parentTransport": null
    }
  ],
  "totalTransports": 1
}
```

---

### 5. Z_CX_GET_TRANSPORT_INFO

**Purpose**: Get metadata for one or multiple transport requests.

**Signature**:
```abap
FUNCTION Z_CX_GET_TRANSPORT_INFO
  IMPORTING
    VALUE(IV_TRANSPORT_NUMBERS) TYPE STRING         " Comma-separated list
  EXPORTING
    VALUE(EV_SUCCESS) TYPE CHAR1
    VALUE(EV_MESSAGE) TYPE STRING
    VALUE(EV_TRANSPORTS_JSON) TYPE STRING.
```

**Features**:
- Supports multiple transports (comma-separated input)
- Single optimized query with JOINs (ABAP 7.5+)
- Returns object count and task count
- Includes transport descriptions from E07T
- Lightweight alternative to Z_CX_GET_TRANSPORT_OBJECTS

**Differences from Z_CX_GET_TRANSPORT_OBJECTS**:
| Feature | Z_CX_GET_TRANSPORT_INFO | Z_CX_GET_TRANSPORT_OBJECTS |
|---------|------------------------|---------------------------|
| **Input** | Multiple transports (comma-separated) | Single transport |
| **Object List** | No (only count) | Yes (full E071 list) |
| **Task List** | No (only count) | Yes (full task list) |
| **Performance** | Fast (single JOIN query) | Slower (multiple queries) |
| **Use Case** | Lightweight metadata lookup | Detailed object analysis |

**JSON Structure**:
```json
[
  {
    "transport_number": "S4DK123456",
    "transport_type": "K",
    "transport_type_desc": "Workbench",
    "status": "D",
    "status_desc": "Modifiable",
    "owner": "USERNAME",
    "description": "Test transport",
    "created_date": "2025-11-20",
    "created_time": "15:30:45",
    "target_system": "S4D",
    "category": "CUST",
    "parent_transport": null,
    "object_count": 5,
    "task_count": 2
  }
]
```

**Example**:
```abap
CALL FUNCTION 'Z_CX_GET_TRANSPORT_INFO'
  EXPORTING
    iv_transport_numbers = 'S4DK123456,S4DK123457'
  IMPORTING
    ev_success           = lv_success
    ev_transports_json   = lv_json.
```

---

## Includes

### lzgfcx_1top.abap

Main include for the function group. Contains:
- Function pool declaration
- Reference to local class definition include

**Content**:
```abap
FUNCTION-POOL ZGFCX_1.

* INCLUDE LZGFCX_1D...                       " Local class definition
```

### lzgfcx_1uxx.abap

Auto-generated index of function module includes.

**Content**:
```abap
INCLUDE LZGFCX_1U01.  "ZCX_GETDDICSOURCE
INCLUDE LZGFCX_1U02.  "ZCX_CREATE_TRANSPORT_COPY
INCLUDE LZGFCX_1U03.  "Z_CX_GET_TRANSPORT_OBJECTS
INCLUDE LZGFCX_1U04.  "Z_CX_GET_OBJECT_IN_OPEN_OT
INCLUDE LZGFCX_1U05.  "Z_CX_GET_TRANSPORT_INFO
```

---

## Dependencies

### External Classes

- **ZCLCX_TRANSPORT_MANAGEMENT**: Used by `ZCX_CREATE_TRANSPORT_COPY`
  - Method: `generar_orden_copia()`
  - Purpose: Handles transport copy creation logic

### Database Tables

| Table | Purpose | Used By |
|-------|---------|---------|
| **E070** | Transport requests | All FMs |
| **E071** | Transport objects | Z_CX_GET_TRANSPORT_OBJECTS, Z_CX_GET_OBJECT_IN_OPEN_OT |
| **E07T** | Transport descriptions | Z_CX_GET_TRANSPORT_INFO |
| **DD02L** | Table headers | ZCX_GETDDICSOURCE |
| **DD03L** | Table fields | ZCX_GETDDICSOURCE |

---

## Common Patterns

### Transport Status Codes

| Code | Description | Modifiable? |
|------|-------------|-------------|
| **D** | Modifiable | Yes |
| **L** | Protected | No (protected from changes) |
| **R** | Released | No (released to target) |
| **N** | Modifiable (Protected) | Yes (but protected) |
| **O** | Released (With Import Protection) | No |

### Transport Type Codes

| Code | Description | Has Tasks? |
|------|-------------|------------|
| **K** | Workbench | Yes |
| **S** | Task | No (is a task itself) |
| **T** | Transport of Copies | Yes |
| **W** | Workbench Request | Yes |
| **C** | Customizing | Yes |

### Object Type Codes (DDIC)

| Code | Description | Source Table |
|------|-------------|--------------|
| **TRANSP** | Transparent Table | DD02L-TABCLASS |
| **INTTAB** | Internal Table/Structure | DD02L-TABCLASS |
| **VIEW** | View | DD02L-TABCLASS |
| **APPEND** | Append Structure | DD02L-TABCLASS |

---

## Error Handling

All function modules follow consistent error handling:

1. **Input Validation**: Check required parameters
2. **Database Queries**: Handle SY-SUBRC <> 0
3. **Exception Raising**: Use specific exception types
4. **Return Values**: Provide status ('X'/'') and message
5. **JSON Errors**: Return `{"success":false,"error":"..."}` on failure

---

## Performance Considerations

### Progressive Discovery Pattern

For transport operations, use this pattern to minimize queries:

1. **Stage 1**: `Z_CX_GET_TRANSPORT_INFO` - Get metadata only (lightweight)
2. **Stage 2**: `Z_CX_GET_TRANSPORT_OBJECTS` - Get full details only if needed

### Query Optimization

- **Z_CX_GET_TRANSPORT_INFO**: Single JOIN query for multiple transports
- **Z_CX_GET_TRANSPORT_OBJECTS**: Multiple queries for detailed object list
- **Z_CX_GET_OBJECT_IN_OPEN_OT**: Uses UP TO 1000 ROWS to prevent full table scan

---

## Version History

| Date | Description |
|------|-------------|
| 2025-11-13 | Created function group ZGFCX_1 |
| 2025-11-14 | Added ZCX_GETDDICSOURCE |
| 2025-11-17 | Added ZCX_CREATE_TRANSPORT_COPY |
| 2025-11-18 | Added Z_CX_GET_TRANSPORT_OBJECTS |
| 2025-11-18 | Added Z_CX_GET_OBJECT_IN_OPEN_OT |
| 2025-11-19 | Added Z_CX_GET_TRANSPORT_INFO |
| 2025-11-20 | Documentation created |

---

## Related Documentation

- **Java MCP Server**: `src/main/java/com/crystal/mcp/sapserver/`
- **Migration Plan**: `docs/requirements/mcp/migration_plan.md`
- **Development Rules**: `docs/development_rules/abap_function_module_rules.md`

---

**Last Updated**: 2025-11-20
**Maintained By**: Crystal Development Team
