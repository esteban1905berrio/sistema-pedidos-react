# AbapAssistant Skill

This skill provides comprehensive ABAP development capabilities through the MCP server for SAP ABAP systems.

## Overview

AbapAssistant enables Claude Code to interact with SAP ABAP systems using the ADT (ABAP Development Tools) interface via RFC. It provides a complete toolkit for reading, creating, modifying, and analyzing ABAP code.

## Capabilities

### 1. Read Objects

**Classes:**
- `get_class_source(class_name, include_type?)` - Read ABAP class source code
  - `include_type`: main, implementation, testclasses, macros
- `get_class_structure(class_name)` - Get class metadata without source
- `get_class_includes(class_name)` - List all available includes
- `get_class_components(class_name)` - Get methods, attributes, events, types

**Programs:**
- `get_program_source(program_name)` - Read program/report source
- `get_include_source(program_name, include_name)` - Read program includes

**Generic:**
- `get_object_source(object_uri)` - Read any ABAP object by ADT URI
- `search_objects(query)` - Find objects by pattern (supports wildcards)

**Examples:**
```python
# Read a class
class_source = get_class_source("ZCL_TEST")

# Search for objects
results = search_objects("ZCL_*")
```

### 2. Create Objects

**Creation Tools:**
- `create_class(class_name, package, description, transport?)` - Create new ABAP class
- `create_function_group(fg_name, package, description, transport?)` - Create function group
- `create_function_module(fm_name, fg_name, package, description, transport?)` - Create function module

**Validation:**
- `validate_object_name(object_name, object_type?)` - Validate SAP naming conventions

**Examples:**
```python
# Create class in $TMP (local)
create_class("ZCL_TEST", "$TMP", "Test Class")

# Create class in transportable package
create_class("ZCL_PROD", "ZPACKAGE", "Production Class", "DEVK900123")

# Create function module
create_function_group("ZTEST_FG", "ZPACKAGE", "Test FG", "DEVK900123")
create_function_module("ZTEST_FM", "ZTEST_FG", "ZPACKAGE", "Test FM", "DEVK900123")
```

### 3. Modify Objects (HIGH-LEVEL WORKFLOWS - NEW!)

**Complete Modification Workflows:**

These tools execute the full ADT modification flow automatically:
**LOCK → SYNTAX_CHECK → MODIFY → UNLOCK → ACTIVATE**

**Available Workflows:**

- `modify_function_module(fm_name, fg_name, new_source, transport?, auto_activate?, validate_syntax?)`
- `modify_class(class_name, new_source, include_type?, transport?, auto_activate?, validate_syntax?)`
- `modify_program(program_name, new_source, transport?, auto_activate?, validate_syntax?)`
- `modify_include(include_name, program_name, new_source, transport?, auto_activate?, validate_syntax?)`

**Parameters:**
- `auto_activate` (default: True) - Automatically activate after modification
- `validate_syntax` (default: True) - Check syntax before saving

**Returns:**
```python
{
    "success": True/False,
    "locked": True,
    "syntax_valid": True,
    "modified": True,
    "unlocked": True,
    "activated": True,
    "messages": [...],
    "lock_handle": "..." (if not auto_activate)
}
```

**Examples:**
```python
# Modify function module with full workflow
result = modify_function_module(
    "ZTEST_FM",
    "ZTEST_FG",
    "FUNCTION ZTEST_FM.\\n  rv_result = 'Hello'.\\nENDFUNCTION.",
    transport="DEVK900123",
    auto_activate=True,
    validate_syntax=True
)

if result["success"]:
    print("✓ Modification completed successfully")
else:
    print("✗ Errors:", result["messages"])

# Modify class definition
modify_class(
    "ZCL_TEST",
    "CLASS zcl_test DEFINITION PUBLIC.\\n  PUBLIC SECTION.\\n    METHODS test.\\nENDCLASS.",
    include_type="main",
    validate_syntax=True
)
```

**Workflow Steps:**
1. **Lock** - Acquire exclusive lock on object
2. **Syntax Check** (optional) - Validate ABAP syntax before saving
3. **Modify** - Save new source code
4. **Unlock** - Release lock (always executed in finally block)
5. **Activate** (optional) - Activate the changes

**Benefits:**
- ✅ Automatic error handling with try-finally
- ✅ Prevents saving code with syntax errors
- ✅ One tool call instead of 4-5 low-level calls
- ✅ Clear success/failure reporting
- ✅ Detailed messages for debugging

### 4. Low-Level Operations (Advanced)

**For advanced control when high-level workflows don't fit:**

- `lock(object_uri)` - Lock object, returns LOCK_HANDLE
- `set_object_source(object_uri, source, lock_handle, transport?)` - Modify source
- `unlock(object_uri, lock_handle)` - Release lock
- `activate(object_name, object_uri)` - Activate object

**When to use low-level tools:**
- Custom workflows not covered by high-level tools
- Need precise control over each step
- Building your own orchestration logic

**Example:**
```python
# Manual workflow (advanced)
lock_handle = None
try:
    # 1. Lock
    lock_handle = lock("/sap/bc/adt/oo/classes/ztest/source/main")

    # 2. Modify
    set_object_source(
        "/sap/bc/adt/oo/classes/ztest/source/main",
        "CLASS ztest DEFINITION...",
        lock_handle,
        transport="DEVK900123"
    )
finally:
    # 3. Always unlock
    if lock_handle:
        unlock("/sap/bc/adt/oo/classes/ztest/source/main", lock_handle)

# 4. Activate
activate("ZTEST", "/sap/bc/adt/oo/classes/ztest")
```

### 5. Code Analysis

**Quality Tools:**
- `syntax_check(object_uri, include_uri, source)` - Validate ABAP syntax
- `prettyprint(source)` - Format code according to SAP standards
- `get_inactive_objects()` - List inactive objects for current user
- `run_unit_tests(object_uri, coverage?)` - Execute ABAP unit tests

**Transport & Dependencies:**
- `transport_info(object_uri)` - Get transport history
- `get_where_used(object_uri)` - Find usage locations
- `get_transport_reference(pgmid, obj_wbtype, obj_name)` - Transport references

**Examples:**
```python
# Check syntax
result = syntax_check(
    "/sap/bc/adt/oo/classes/ztest",
    "/sap/bc/adt/oo/classes/ztest/source/main",
    "CLASS ztest DEFINITION..."
)

if result["has_errors"]:
    for msg in result["messages"]:
        print(f"Line {msg['line']}: {msg['text']}")

# Format code
formatted = prettyprint("data: lv_test type string.")
```

### 6. CDS & RAP Objects

**CDS Views:**
- `get_cds_view_metadata(cds_name)` - Get CDS metadata
- `get_cds_view_source(cds_name)` - Get DDL source
- `search_cds_views_by_sqlview(pattern)` - Search by SQL view name

**RAP Objects:**
- `get_service_binding(binding_name)` - Service binding metadata
- `get_service_definition_metadata(srvd_name)` - Service definition
- `get_behavior_definition(bdef_name)` - Behavior definition source
- `get_metadata_extension(ddlx_name)` - UI annotations (DDLX)
- `explore_rap_object(object_name)` - Intelligent RAP exploration

### 7. Enhancements (Ampliaciones)

- `search_enhancements(package)` - Find all enhancements in package
- `get_enhancement_metadata(enhancement_name)` - Enhancement details
- `get_enhancement_source(enhancement_name)` - Enhancement code

### 8. Data Dictionary & Query

**DDIC:**
- `get_ddic_element(element_name, element_type)` - Table/structure/domain definitions
- `package_search_help(query)` - Package autocomplete

**Query:**
- `get_table_contents(table_name, max_rows?, where_clause?)` - Preview table data
- `run_query(query_definition)` - Execute custom SQL

### 9. Transport Management

**Transport Operations:**
- `create_transport(description, dev_class, transport_type?)` - Create transport
- `list_user_transports(user?, status?)` - List transports
- `get_transport_request(transport_number)` - Get complete transport data
- `add_object_to_transport(transport_number, object_uri)` - Assign object
- `release_transport(transport_number)` - ⚠️ Release transport (caution!)

## Workflows

### Workflow 1: Modify Function Module (Recommended)

**Use the high-level workflow tool:**

```python
result = modify_function_module(
    function_module_name="ZTEST_FM",
    function_group_name="ZTEST_FG",
    new_source="""FUNCTION ZTEST_FM.
  rv_result = 'Hello World'.
ENDFUNCTION.""",
    transport="DEVK900123",
    auto_activate=True,
    validate_syntax=True
)

if result["success"]:
    print("✓✓✓ Modification complete!")
else:
    print("Errors:", result["messages"])
```

**Why use this workflow:**
- One tool call instead of 4-5
- Automatic error handling
- Syntax validation prevents saving bad code
- Clear success/failure reporting
- Auto-unlock even if errors occur

### Workflow 2: Create + Modify Workflow

```python
# Step 1: Create
create_function_module(
    "ZTEST_FM",
    "ZTEST_FG",
    "ZPACKAGE",
    "Test function module",
    "DEVK900123"
)

# Step 2: Modify with initial implementation
modify_function_module(
    "ZTEST_FM",
    "ZTEST_FG",
    """FUNCTION ZTEST_FM.
  " Initial implementation
  rv_result = 'Hello'.
ENDFUNCTION.""",
    transport="DEVK900123"
)
```

### Workflow 3: Manual Low-Level (Advanced Users)

**Use when you need precise control:**

```python
lock_handle = None
try:
    # 1. Lock
    lock_handle = lock("/sap/bc/adt/functions/groups/zfg/fmodules/zfm")

    # 2. Optional: Syntax check
    syntax_result = syntax_check(
        "/sap/bc/adt/functions/groups/zfg/fmodules/zfm",
        "/sap/bc/adt/functions/groups/zfg/fmodules/zfm/source/main",
        new_source
    )

    if syntax_result["has_errors"]:
        raise Exception("Syntax errors found!")

    # 3. Modify
    set_object_source(
        "/sap/bc/adt/functions/groups/zfg/fmodules/zfm/source/main",
        new_source,
        lock_handle,
        "DEVK900123"
    )
finally:
    # 4. Always unlock (critical!)
    if lock_handle:
        unlock(
            "/sap/bc/adt/functions/groups/zfg/fmodules/zfm/source/main",
            lock_handle
        )

# 5. Activate
activate("ZFIAAC002_DMEE_NRO_TRASL_DAV", "/sap/bc/adt/functions/groups/zfg/fmodules/zfm")
```

### Workflow 4: Batch Modification

```python
# Modify multiple related objects
objects_to_activate = []

# Modify class definition
result1 = modify_class("ZCL_MAIN", source1, auto_activate=False)
objects_to_activate.append({"name": "ZCL_MAIN", "uri": result1["uri"]})

# Modify class implementation
result2 = modify_class("ZCL_MAIN", source2, include_type="implementation", auto_activate=False)

# Modify helper class
result3 = modify_class("ZCL_HELPER", source3, auto_activate=False)
objects_to_activate.append({"name": "ZCL_HELPER", "uri": result3["uri"]})

# Activate all at once
activate_objects(objects_to_activate)
```

## Best Practices

### 1. Always Use High-Level Workflows First

✅ **Recommended:**
```python
modify_function_module(fm_name, fg_name, source, transport)
```

❌ **Avoid (unless necessary):**
```python
lock(uri)
set_object_source(uri, source, handle)
unlock(uri, handle)
activate(name, uri)
```

### 2. Enable Syntax Validation

✅ **Recommended:**
```python
modify_class(name, source, validate_syntax=True)  # Default
```

❌ **Risky:**
```python
modify_class(name, source, validate_syntax=False)  # Can save invalid code
```

### 3. Specify Transports for Transportable Packages

✅ **Correct:**
```python
modify_class("ZCL_PROD", source, transport="DEVK900123")
```

❌ **Will fail:**
```python
modify_class("ZCL_PROD", source)  # Missing transport for transportable package
```

### 4. Check Result Status

✅ **Recommended:**
```python
result = modify_class(name, source)
if result["success"]:
    print("Modified and activated")
else:
    for msg in result["messages"]:
        print(f"Error: {msg['text']}")
```

### 5. Use $TMP for Testing

```python
# Local testing (no transport needed)
create_class("ZCL_TEST", "$TMP", "Test class")
modify_class("ZCL_TEST", test_source)
```

### 6. Batch Activation for Multiple Objects

```python
# When modifying related objects, activate together
modify_class("ZCL_A", source_a, auto_activate=False)
modify_class("ZCL_B", source_b, auto_activate=False)
modify_class("ZCL_C", source_c, auto_activate=False)

activate_objects([
    {"name": "ZCL_A", "uri": "/sap/bc/adt/oo/classes/zcl_a"},
    {"name": "ZCL_B", "uri": "/sap/bc/adt/oo/classes/zcl_b"},
    {"name": "ZCL_C", "uri": "/sap/bc/adt/oo/classes/zcl_c"}
])
```

## ADT URI Patterns

### Function Module
```
URI:        /sap/bc/adt/functions/groups/{fg}/fmodules/{fm}
Source URI: /sap/bc/adt/functions/groups/{fg}/fmodules/{fm}/source/main
```

### Class
```
URI:        /sap/bc/adt/oo/classes/{name}
Source URI: /sap/bc/adt/oo/classes/{name}/source/{include_type}
            include_type: main, implementation, testclasses, macros
```

### Program
```
URI:        /sap/bc/adt/programs/programs/{name}
Source URI: /sap/bc/adt/programs/programs/{name}/source/main
```

### Include
```
URI:        /sap/bc/adt/programs/includes/{name}
Source URI: /sap/bc/adt/programs/includes/{name}/source/main
```

## Successful Workflows Documentation

This section tracks successfully tested and integrated workflows:

### ✅ Workflow: Function Module Modification (2025-10-24)
- **Status:** Implemented and tested
- **Tools:** `modify_function_module`
- **Pattern:** LOCK → SYNTAX → MODIFY → UNLOCK → ACTIVATE
- **Files:**
  - `app/services/modification_service.py`
  - `app/mcp/tools/modification_tools.py`
  - `app/tests/test_modification_workflow.py`

### ✅ Workflow: Class Modification (2025-10-24)
- **Status:** Implemented and tested
- **Tools:** `modify_class`
- **Pattern:** LOCK → SYNTAX → MODIFY → UNLOCK → ACTIVATE
- **Files:** Same as Function Module

### ✅ Workflow: Program Modification (2025-10-24)
- **Status:** Implemented
- **Tools:** `modify_program`, `modify_include`
- **Pattern:** LOCK → SYNTAX → MODIFY → UNLOCK → ACTIVATE

## Tool Count

**Total Tools:** 63+ MCP tools across all categories

- **Read Operations:** 9 tools
- **Create Operations:** 4 tools
- **Modify Operations (High-Level):** 4 NEW tools
- **Modify Operations (Low-Level):** 3 tools
- **Activation:** 3 tools
- **Code Quality:** 4 tools
- **Transport:** 14 tools
- **CDS/RAP:** 12 tools
- **Enhancements:** 3 tools
- **Where-Used:** 2 tools
- **DDIC/Query:** 6 tools
- **Discovery:** 3 tools

## Integration with CLAUDE.MD

Every successfully tested workflow should be documented in:
1. This skill file (AbapAssistant/README.md)
2. `CLAUDE.MD` project instructions
3. `docs/requirements/pr_*.md` for the specific PR

## Version History

- **v1.0** (2025-01-XX): Initial skill creation
- **v2.0** (2025-10-24): Added high-level modification workflows

---

**Note:** This skill requires the ABAP-ADT-RFC-Server MCP server to be running and properly configured with SAP credentials.
