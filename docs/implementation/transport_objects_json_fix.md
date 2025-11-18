# Transport Objects JSON Fix

## Issue

The Function Module `Z_CX_GET_TRANSPORT_OBJECTS` is missing the `tab_key` field in the objects JSON array, causing a NullPointerException in the Java parser.

## Current ABAP Code (Line ~163)

```abap
lv_object_line = |{|
              && |"trkorr":"{ ls_e071-trkorr }",|
              && |"pgmid":"{ ls_e071-pgmid }",|
              && |"object_type":"{ ls_e071-object }",|
              && |"object_name":"{ ls_e071-obj_name }",|
              && |"lock_flag":"{ ls_e071-lockflag }",|
              && |"gennum":"{ ls_e071-gennum }"|  ❌ Missing tab_key!
              && |}|.
```

## Required Fix

Add the `tab_key` field to the object JSON:

```abap
lv_object_line = |{|
              && |"trkorr":"{ ls_e071-trkorr }",|
              && |"pgmid":"{ ls_e071-pgmid }",|
              && |"object_type":"{ ls_e071-object }",|
              && |"object_name":"{ ls_e071-obj_name }",|
              && |"lock_flag":"{ ls_e071-lockflag }",|
              && |"gennum":"{ ls_e071-gennum }",|
              && |"tab_key":"{ ls_e071-tabkey }"|  ✅ Add this line!
              && |}|.
```

## Why This is Needed

The Java `TransportObjectsResult.TransportObject` record expects all 7 fields:

```java
public record TransportObject(
    String trkorr,
    String pgmid,
    String objectType,
    String objectName,
    String lockFlag,
    String gennum,
    String tabKey  // ← This field is missing in JSON
)
```

## How to Fix

### Option 1: Manual Fix in SE37 (Recommended)

1. Open SE37 in SAP
2. Navigate to function module `Z_CX_GET_TRANSPORT_OBJECTS`
3. Go to Source Code tab
4. Find line ~163 where `lv_object_line` is constructed
5. Add the `tab_key` field as shown above
6. Save and activate

### Option 2: ADT Eclipse

1. Open Eclipse with ADT
2. Open function module `Z_CX_GET_TRANSPORT_OBJECTS` in function group `ZGFCX_1`
3. Modify the source code line ~163
4. Save and activate

### Option 3: Use GIRAL MCP Tool

The MCP tool `modify_function_module` has issues with the ADT API format. Manual fix in SE37 is recommended.

## Test After Fix

Run the manual test:

```bash
mvn test -Dtest=ManualTransportObjectsTest#testGetTransportObjects_MainTransport
```

Expected output:
- ✅ Test passes without NullPointerException
- ✅ JSON parsing succeeds
- ✅ All 7 fields present in TransportObject records

## Verification

Check the logs for the JSON output:

```
JSON from FM (length: X bytes): {"success":true,"transport_number":"CADK911293",...}
```

Verify each object has all fields:
```json
{
  "trkorr": "CADK911222",
  "pgmid": "R3TR",
  "object_type": "CLAS",
  "object_name": "ZCLFI_AAC002_PROCESSOR",
  "lock_flag": "X",
  "gennum": "001",
  "tab_key": ""  ← Should be present
}
```

## Status

- ❌ Current: JSON missing `tab_key` field
- ⏳ Pending: Manual fix in SAP system
- ⏳ Pending: Retest after fix
