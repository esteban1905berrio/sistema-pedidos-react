# ST05 API Investigation for Program Analysis Tool

## Executive Summary

SAP provides a complete **RFC-enabled API** in Function Group `ST05_API` (Package ST05) for programmatic control of SQL traces. This enables creating a wrapper FM that:

1. Activates ST05 trace with filters
2. Executes a transaction (CALL TRANSACTION/SUBMIT)
3. Deactivates trace
4. Returns structured trace results for analysis

## Discovered API

### Function Group: ST05_API

| FM | Purpose | RFC |
|----|---------|-----|
| `ST05_ACTIVATE_TRACE` | Activate trace with filters | Yes |
| `ST05_DEACTIVATE_TRACE` | Deactivate trace | Yes |
| `ST05_GET_TRACE_STATE` | Check current trace state | Yes |
| `ST05_GET_TRACE_TABLES` | Get comprehensive results | Yes |
| `ST05_GET_TABLE_ACCESS_RECORDS` | Get table access summary | Yes |
| `ST05_GET_SUMMARY_TABLES` | Get aggregated data | Yes |
| `ST05_GET_DIRECTORY_ENTRIES` | Get trace directory | Yes |
| `ST05_GET_RAW_RECORD` | Get raw records | Yes |

### Authorization Requirements

- **Object**: `S_ADMI_FCD`
- **Values**: `ST0M` (modify/activate), `ST0R` (read)

---

## Key Structures

### Input Structures

#### ST05_API_TRACE_TYPES
Controls which trace types to activate:

| Field | Type | Description |
|-------|------|-------------|
| SQL_ON | PTC_BOOLEAN | SQL trace (SELECT, INSERT, UPDATE, DELETE) |
| BUF_ON | PTC_BOOLEAN | Buffer trace (table buffer accesses) |
| ENQ_ON | PTC_BOOLEAN | Enqueue trace (lock operations) |
| RFC_ON | PTC_BOOLEAN | RFC trace (remote function calls) |
| HTTP_ON | PTC_BOOLEAN | HTTP trace |
| APC_ON | PTC_BOOLEAN | APC trace |
| AMC_ON | PTC_BOOLEAN | AMC trace |
| AUTH_ON | PTC_BOOLEAN | Authorization trace |

#### ST05_API_TRACE_FILTER
Filter criteria for trace:

| Field | Type | Length | Description |
|-------|------|--------|-------------|
| CLIENT | SYMANDT | 3 | Client to trace |
| TRACE_USER | ST05_USER_NAME_PATTERN | 12 | User to trace (supports patterns) |
| TRANSACTION_CODE | PTC_TRANSACTION_CODE | 20 | Transaction filter |
| PROGRAM | PTC_PROGRAM_NAME | 40 | Program filter |
| WP_ID | PTC_WP_ID | 3 | Work process ID |
| INCLUDED_TABLES | ST05_OBJECT_INCL_EXCL | TTYP | Tables to include |
| EXCLUDED_TABLES | ST05_OBJECT_INCL_EXCL | TTYP | Tables to exclude |
| STACK_TRACE_ON | PTC_BOOLEAN | 1 | **Enable call stack capture** |

#### ST05_TRACE_INTERVAL
Time range for reading trace:

| Field | Type | Description |
|-------|------|-------------|
| START_DATE | DATS | Start date |
| START_TIME | TIMS | Start time |
| START_MS | NUMC(3) | Start milliseconds |
| END_DATE | DATS | End date |
| END_TIME | TIMS | End time |
| END_MS | NUMC(3) | End milliseconds |

### Output Structures

#### ST05_DETAILED_RECORD (30 fields)
Complete trace record with execution details:

| Field | Type | Description |
|-------|------|-------------|
| DATE | PTC_DATE | Execution date |
| TIME | PTC_TIME | Execution time |
| DURATION | ST05_DURATION | Duration in microseconds |
| NUMBER_OF_ROWS | INT4 | Rows affected |
| **OBJECT** | CHAR(30) | **Table name** |
| **STATEMENT_WITH_VALUES** | STRING | **SQL with actual values** |
| **STATEMENT_WITH_NAMES** | STRING | **SQL with placeholder names** |
| **VARIABLES** | STRING | **Variable values** |
| **PROGRAM** | CHAR(40) | **Executing program** |
| **OFFSET** | INT4 | **Line number in program** |
| TRANSACTION | CHAR(20) | Transaction code |
| OPERATION | CHAR(7) | DB operation type |
| RETURN_CODE | INT4 | Return code |
| TRACE_TYPE | CHAR(4) | Trace type (SQL, BUF, etc.) |
| RECORD_NUMBER | INT4 | Unique record ID |
| USER_NAME | CHAR(12) | User |
| CLIENT | CHAR(3) | Client |

#### ST05_KERNEL_CALL_STACK_ITEM (9 fields)
ABAP call stack for each trace record:

| Field | Type | Description |
|-------|------|-------------|
| RECORD_NUMBER | INT4 | Links to ST05_DETAILED_RECORD |
| **LEVEL** | NUMC(10) | Stack depth |
| **PROG_INFO** | STRING | **Program/Include/Line info** |
| **EVENT_TYPE** | CHAR(40) | **FORM, METHOD, FUNCTION, etc.** |
| **EVENT_NAME** | CHAR(61) | **Subroutine/Method/FM name** |
| WP_ID | CHAR(3) | Work process |
| ROLL_ID | NUMC(10) | Roll ID |
| LUW_ID | NUMC(10) | LUW ID |
| SUCCESSOR | NUMC(10) | Next stack entry |

#### ST05_TABLE_ACCESS_RECORD (9 fields)
Aggregated table access summary:

| Field | Type | Description |
|-------|------|-------------|
| **OBJECT** | CHAR(30) | **Table name** |
| STATEMENT_TYPE | STRING | Operation type |
| DURATION | DEC | Total duration |
| NUMBER_OF_ROWS | INT4 | Total rows |
| **NUMBER_OF_EXECUTIONS** | INT4 | **Execution count** |
| **DDTEXT** | CHAR(60) | **Table description** |
| APP_COMPONENT | STRING | Application component |

---

## Wrapper FM Design: ZCX_TRACE_TRANSACTION

### Purpose

Execute a transaction with SQL trace enabled and return structured analysis data suitable for understanding program execution flow and table access patterns.

### Interface

```
FUNCTION ZCX_TRACE_TRANSACTION
  IMPORTING
    VALUE(IV_TRANSACTION) TYPE TCODE           " Transaction to execute
    VALUE(IV_VARIANT) TYPE VARIANT OPTIONAL    " Report variant (for SUBMIT)
    VALUE(IT_BDC_DATA) TYPE BDCDATA_TAB OPTIONAL " BDC data (for CALL TRANSACTION)
    VALUE(IV_TRACE_SQL) TYPE PTC_BOOLEAN DEFAULT 'X'
    VALUE(IV_TRACE_BUFFER) TYPE PTC_BOOLEAN DEFAULT ' '
    VALUE(IV_TRACE_ENQUEUE) TYPE PTC_BOOLEAN DEFAULT ' '
    VALUE(IV_WITH_CALL_STACK) TYPE PTC_BOOLEAN DEFAULT 'X'
    VALUE(IV_MAX_RECORDS) TYPE I DEFAULT 1000  " Limit output
  EXPORTING
    VALUE(ET_DETAILED_RECORDS) TYPE ST05_DETAILED_RECORD_TABLE
    VALUE(ET_CALL_STACK) TYPE ST05_KERNEL_CALL_STACK
    VALUE(ET_TABLE_ACCESS) TYPE ST05_TABLE_ACCESS_RECORD_TABLE
    VALUE(EV_TRACE_DURATION_MS) TYPE I
    VALUE(EV_TOTAL_SQL_STATEMENTS) TYPE I
    VALUE(EV_TABLES_ACCESSED_COUNT) TYPE I
  EXCEPTIONS
    NO_AUTHORITY
    ACTIVATION_ERROR
    EXECUTION_ERROR
    DEACTIVATION_ERROR
    READ_ERROR.
```

### Internal Logic

```
1. GET_TIME -> lv_start_time, lv_start_date

2. Build ls_trace_types:
   - sql_on = iv_trace_sql
   - buf_on = iv_trace_buffer
   - enq_on = iv_trace_enqueue

3. Build ls_filter:
   - trace_user = sy-uname
   - client = sy-mandt
   - stack_trace_on = iv_with_call_stack

4. CALL FUNCTION 'ST05_ACTIVATE_TRACE'
   EXPORTING
     trace_types = ls_trace_types
     trace_filter = ls_filter
   EXCEPTIONS
     no_authority = 1
     ...

5. Execute transaction:
   IF it_bdc_data IS NOT INITIAL.
     CALL TRANSACTION iv_transaction USING it_bdc_data
       MODE 'N' UPDATE 'S'.
   ELSEIF iv_variant IS NOT INITIAL.
     SUBMIT (lv_program) VIA SELECTION-SCREEN
       USING SELECTION-SET iv_variant AND RETURN.
   ELSE.
     " Error: need BDC or variant
   ENDIF.

6. GET_TIME -> lv_end_time, lv_end_date

7. CALL FUNCTION 'ST05_DEACTIVATE_TRACE'
   EXPORTING
     trace_types = ls_trace_types
     all_users = ' '
   ...

8. Build ls_interval:
   - start_date = lv_start_date
   - start_time = lv_start_time
   - end_date = lv_end_date
   - end_time = lv_end_time

9. CALL FUNCTION 'ST05_GET_TRACE_TABLES'
   EXPORTING
     trace_types = ls_trace_types
     trace_interval = ls_interval
     kind = 'DET+'  " Detailed + call stack
   IMPORTING
     detailed_record_table = et_detailed_records
     kernel_call_stack = et_call_stack
     table_access_record_table = et_table_access

10. Apply iv_max_records limit

11. Calculate summary:
    - ev_trace_duration_ms = lv_end_time - lv_start_time
    - ev_total_sql_statements = lines( et_detailed_records )
    - ev_tables_accessed_count = lines( et_table_access )
```

---

## MCP Tool Design: trace_transaction_analysis

### Tool Interface

```java
@Tool(description = "Execute SAP transaction with SQL trace and return execution analysis")
public TraceAnalysisResult traceTransactionAnalysis(
    @ToolParam(description = "Transaction code to execute") String transactionCode,
    @ToolParam(description = "Report variant name (optional)") String variant,
    @ToolParam(description = "BDC data as JSON array (optional)") String bdcDataJson,
    @ToolParam(description = "Include SQL trace", defaultValue = "true") boolean traceSql,
    @ToolParam(description = "Include call stack", defaultValue = "true") boolean withCallStack,
    @ToolParam(description = "Max records to return", defaultValue = "500") int maxRecords
)
```

### Output Structure

```json
{
  "summary": {
    "transactionCode": "MB51",
    "executionDurationMs": 1234,
    "totalSqlStatements": 456,
    "tablesAccessedCount": 23
  },
  "executionFlow": [
    {
      "sequence": 1,
      "program": "SAPLMIGO",
      "offset": 1234,
      "event": "PERFORM READ_MATERIAL",
      "table": "MARA",
      "operation": "SELECT",
      "duration": 12,
      "rows": 1
    }
  ],
  "tableAccessSummary": [
    {
      "table": "MARA",
      "description": "Material Master",
      "executions": 15,
      "totalRows": 150,
      "totalDuration": 234
    }
  ],
  "callStack": [
    {
      "recordNumber": 1,
      "level": 0,
      "programInfo": "SAPLMIGO line 1234",
      "eventType": "FUNCTION",
      "eventName": "MIGO_BAPI_GOODSMVT_CREATE"
    }
  ]
}
```

---

## Use Cases

### 1. Understanding Value Calculation

**User Question**: "How is field NETWR calculated in transaction VA03?"

**Workflow**:
1. MCP tool calls `ZCX_TRACE_TRANSACTION` with:
   - IV_TRANSACTION = 'VA03'
   - IT_BDC_DATA = (BDC to open specific sales order)
   - IV_WITH_CALL_STACK = 'X'

2. Analyze results:
   - Find VBAP table reads (NETWR field)
   - Trace call stack to find calculation logic
   - Identify FMs/methods that populate the value

3. Return:
   - Execution flow: SAPMV45A → FM RV_PRICE_PRINT → ...
   - Tables: VBAP, KONV, PRCD_ELEMENTS
   - Key programs/lines for detailed analysis

### 2. Finding Table Dependencies

**User Question**: "Which tables does transaction ME21N access?"

**Workflow**:
1. Execute trace with SQL_ON
2. Return `tableAccessSummary` showing all tables with counts

### 3. Performance Analysis

**User Question**: "Why is MB51 slow?"

**Workflow**:
1. Execute trace
2. Sort by duration descending
3. Identify expensive SQL statements and their programs

---

## Implementation Plan

### Phase 1: Create Custom FM
1. Create FM `ZCX_TRACE_TRANSACTION` in Function Group `ZGFCX_1`
2. Test manually via SE37

### Phase 2: MCP Integration
1. Create Java service `TraceService.java`
2. Create Java tool `TraceTools.java`
3. Add JSON serialization for complex result structures

### Phase 3: Enhanced Analysis
1. Add smart filtering (exclude SAP internal tables)
2. Add call stack compression (show only relevant entries)
3. Add field-level tracking (which fields from each table)

---

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Authorization S_ADMI_FCD required | Document requirement, check before execution |
| Large trace output | IV_MAX_RECORDS parameter, time-based filtering |
| Trace interference with other users | Filter by SY-UNAME only |
| Transaction execution side effects | Use MODE 'N' (no display), consider read-only transactions |

---

## References

- Function Group: ST05_API (Package ST05)
- Class: CL_ST05_TRACE_MAIN_M (internal trace management)
- Transaction: ST05 (SQL Trace)
- Documentation: SAP Note 2435796 (ST05 API usage)
