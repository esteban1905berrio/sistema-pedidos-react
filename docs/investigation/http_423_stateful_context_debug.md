# HTTP 423 "Invalid Lock Handle" - Investigation Report

**Date**: 2025-11-18
**Status**: INVESTIGATING
**Severity**: CRITICAL BLOCKER

## Problem Statement

When attempting to modify ABAP class `ZCLCX_TRANSPORT_MANAGEMENT` in the GDC system using `mcp__gdcmcp__modify_class`, the operation fails with:

```
HTTP 423 - Resource CLASS ZCLCX_TRANSPORT_MANAGEMENT is not locked
(invalid lock handle: 09A657F5A31409E026115EBA2A606FA4DB8EBC49)
```

This error occurs during the PUT operation (Step 2: Modify source) after a successful LOCK operation (Step 1).

## Architecture Overview

### MCP Server Configuration
- **giralmcp**: Java MCP server instance connecting to CRY SAP system
- **gdcmcp**: Java MCP server instance connecting to GDC SAP system
- Both use the SAME Java codebase but are SEPARATE PROCESSES with different environment variables

### Stateful Workflow Implementation
```
ClassService.modifyClass()
    → StatefulModificationService.executeStatefulWorkflow()
        → RfcAdapter.beginStatefulContext()  # JCoContext.begin(destination)
            → StatefulModificationService.lockObject()  # POST ...?_action=LOCK
            → ClassService.setObjectSource()            # PUT .../source/main?lockHandle=...
            → StatefulModificationService.unlockObject() # POST ...?_action=UNLOCK
        → RfcAdapter.endStatefulContext()    # JCoContext.end(destination)
```

## Code Analysis

### 1. Dependency Injection (✅ CORRECT)
Both `ClassService` and `StatefulModificationService` inject the SAME `RfcAdapter` singleton:

**ClassService.java:47-48**
```java
private final RfcAdapter rfcAdapter;
private final StatefulModificationService statefulModificationService;
```

**StatefulModificationService.java:70**
```java
private final RfcAdapter rfcAdapter;
```

Spring ensures this is the same singleton instance.

### 2. JCoDestination Configuration (✅ CORRECT)
**JCoConfiguration.java:74-128**
- Single `@Bean` method creates ONE `JCoDestination` per Spring ApplicationContext
- Environment variables configure connection (`SAP_ASHOST`, `SAP_USER`, etc.)
- Each MCP server process (giralmcp, gdcmcp) has its own ApplicationContext and JCoDestination

### 3. Stateful Context Management (✅ APPEARS CORRECT)
**RfcAdapter.java:95-114**
```java
public void beginStatefulContext() throws JCoException {
    // ThreadLocal flag check (prevents nested contexts)
    if (statefulContextActive.get()) {
        throw new IllegalStateException("Stateful context already active");
    }

    JCoContext.begin(destination);  // ← Starts stateful session
    statefulContextActive.set(true);
}
```

**RfcAdapter.java:136-159**
```java
public void endStatefulContext() throws JCoException {
    try {
        JCoContext.end(destination);  // ← Ends stateful session
    } finally {
        statefulContextActive.set(false);  // ← Always cleanup ThreadLocal
    }
}
```

**RfcAdapter.java:189-263**
```java
public RfcResponse request(...) throws JCoException {
    // Get function from repository
    JCoFunction function = destination.getRepository()
        .getFunction("SADT_REST_RFC_ENDPOINT");

    // Execute RFC (should use stateful session if active)
    function.execute(destination);  // ← Passes destination parameter

    return parseResponse(response);
}
```

### 4. Workflow Execution (✅ APPEARS CORRECT)
**StatefulModificationService.java:157-197**
```java
public <T> T executeStatefulWorkflow(String objectName, StatefulWorkflow<T> workflow) {
    try {
        rfcAdapter.beginStatefulContext();  // ← START
        try {
            T result = workflow.execute();  // ← EXECUTE (LOCK → PUT → UNLOCK)
            return result;
        } finally {
            rfcAdapter.endStatefulContext();  // ← END (always)
        }
    } catch (Exception e) {
        throw new RuntimeException("Failed to execute stateful workflow", e);
    }
}
```

**ClassService.java:285-350**
```java
ClassModifyResult workflowResult = statefulModificationService.executeStatefulWorkflow(
    className,
    () -> {
        // LOCK
        LockResult lock = statefulModificationService.lockObject(classUri);

        try {
            // MODIFY (setObjectSource calls rfcAdapter.request() internally)
            boolean modified = setObjectSource(sourceUri, newSource, lock.lockHandle(), ...);
            return result;
        } finally {
            // UNLOCK
            statefulModificationService.unlockObject(classUri, lock.lockHandle());
        }
    }
);
```

## SAP JCo Official Example Comparison

**From: resources/jco/examples/.../StatefulCalls.java:46-64**
```java
JCoContext.begin(destination);
try {
    executeCalls(destination, incrementCounter, getCounter);
} finally {
    JCoContext.end(destination);
}

// executeCalls method:
incrementCounter.execute(destination);  // ← Must pass destination!
```

**Key Insight from JavaDoc (Line 16)**:
> "With the default SessionReferenceProvider, each THREAD is considered a session,
> and stateful sequences will work as long as ALL calls belonging to one session
> are executed within the SAME THREAD."

Our implementation follows this pattern correctly:
- ✅ Surrounds calls with `JCoContext.begin()` / `JCoContext.end()`
- ✅ Passes `destination` to `function.execute(destination)`
- ✅ Executes all calls synchronously on same thread (no async/await, no thread pool)

## Enhanced Logging Added

**RfcAdapter.java**:
- Line 104: `🔵 BEGIN STATEFUL CONTEXT | Thread: {} | Destination: {} | Instance: {}`
- Line 112: `✅ Stateful context STARTED | JCoContext active: {}`
- Line 202: `📡 RFC REQUEST | Method: {} | URI: {} | Stateful: {} | Thread: {}`
- Line 248: `✅ RFC RESPONSE | Duration: {} ms | Stateful after call: {}`
- Line 145: `🔴 END STATEFUL CONTEXT | Thread: {} | Destination: {}`

**StatefulModificationService.java**:
- Line 238: `🔒 LOCK REQUEST | URI: {} | Stateful active: {}`
- Line 268: `🔓 LOCK SUCCESS | Handle: {} | Transport: {} | User: {}`

**ClassService.java**:
- Line 878: `💾 SET SOURCE | URI: {} | Lock Handle: {} | Transport: {} | Stateful: {}`

**Logging Configuration**: Updated `application.yml` to enable file logging (logs/sap-mcp-server.log)

## Hypotheses

### Hypothesis 1: Thread Switching (❓ UNLIKELY)
**Theory**: Workflow execution switches threads between LOCK and PUT.
**Evidence Against**: All code is synchronous, no thread pools or async operations visible.
**How to Verify**: Check thread IDs in enhanced logs.

### Hypothesis 2: JCoContext Not Active (❓ POSSIBLE)
**Theory**: `JCoContext.isStateful(destination)` returns FALSE during PUT operation.
**Evidence For**: Would explain "invalid lock handle" error.
**How to Verify**: Check `Stateful:` flag in `📡 RFC REQUEST` logs for PUT operation.

### Hypothesis 3: Lock Handle Expiry (❓ UNLIKELY)
**Theory**: Lock handle times out between LOCK and PUT.
**Evidence Against**: Operations are immediate (< 1 second apart).
**How to Verify**: Check `Duration:` in RFC response logs.

### Hypothesis 4: Different SAP Sessions (⭐ MOST LIKELY)
**Theory**: LOCK and PUT are executing in different SAP work processes/sessions despite JCoContext.
**Evidence For**:
- Error message explicitly states "Resource is not locked (invalid lock handle)"
- This is the EXACT behavior when lock is in session A but PUT tries to use it in session B
**Evidence Against**: JCoContext API is designed to prevent exactly this.
**How to Verify**:
- Check if `JCoContext.begin()` is actually being called before LOCK
- Verify same `destination` instance is used (check `Instance:` hash code in logs)
- Check SAP ABAP system logs for ENQUEUE entries

### Hypothesis 5: Cached/Stale Code (❓ POSSIBLE)
**Theory**: gdcmcp MCP server is still running old code without stateful workflow.
**Evidence For**: Claude Desktop caches MCP servers, might not reload after rebuild.
**How to Verify**: Restart Claude Desktop and retry operation, check logs appear.

## Next Steps

1. **Restart Claude Desktop** to ensure latest code is loaded for gdcmcp server
2. **Re-run modification** and capture logs from `logs/sap-mcp-server.log` (or `logs/gdcmcp-sap-mcp-server.log` if separate log files)
3. **Analyze log sequence**:
   - Verify `🔵 BEGIN STATEFUL CONTEXT` appears BEFORE `🔒 LOCK REQUEST`
   - Check thread IDs match across all operations
   - Verify `Stateful: true` for BOTH LOCK and PUT requests
   - Compare `Dest Instance:` hash codes (must be identical)
4. **If still failing after verification**:
   - Check SAP system logs (SM21, ST22) for lock/ENQUEUE errors
   - Try direct ADT API calls via curl to isolate issue
   - Consider SAP JCo version incompatibility or bug

## Open Questions

1. Does `JCoDestinationManager.getDestination(name)` return same object instance across multiple calls?
   - **Answer**: YES (per SAP documentation, destinations are cached by name)

2. Does `destination.getRepository().getFunction()` consume a new connection from pool?
   - **Answer**: NO (getRepository() is lightweight, doesn't affect stateful context)

3. Is there any Spring AI MCP middleware that might wrap calls in different threads?
   - **Answer**: UNKNOWN - need to check Spring AI MCP SDK source

4. Could there be TWO different JCoDestination instances (despite singleton)?
   - **Answer**: NO in same process, but YES across giralmcp vs gdcmcp (expected)

## References

- **SAP JCo Stateful Example**: `resources/jco/examples/.../StatefulCalls.java`
- **Architecture Design**: `docs/requirements/mcp/workflow_based/pr_centralized_stateful_architecture.md`
- **Implementation Doc**: `docs/implementation/stateful_modification_implementation_complete.md`
- **JCo Analysis**: `docs/research/jco_stateful_connections_analysis.md`

---

**Last Updated**: 2025-11-18 00:42 ART
**Investigator**: Claude Code (Sonnet 4.5)
