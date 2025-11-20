# Manual Tests - Execution Guide

## Problem Statement

Maven test execution has issues running individual tests - it tends to run ALL tests even when specifying a single test class/method. This is a known Maven/Spring Boot test issue.

## Solution

All manual tests in this package use `@Disabled` annotation by default. To run them:

### Step 1: Enable the Test

Open the test file and **comment out** the `@Disabled` annotation:

```java
@SpringBootTest
// @Disabled("Manual test - enable explicitly to run")  ← Comment this line
class ManualJCoConnectionTest {
```

### Step 2: Run the Test

```bash
# Run specific test class
mvn test -Dtest=ManualJCoConnectionTest

# Run specific test method
mvn test -Dtest=ManualJCoConnectionTest#testJCoConnectionPool
```

### Step 3: Re-disable After Testing

**IMPORTANT**: After testing, uncomment the `@Disabled` annotation to prevent the test from running in CI/CD or during full test suite execution.

```java
@SpringBootTest
@Disabled("Manual test - enable explicitly to run")  ← Uncomment this
class ManualJCoConnectionTest {
```

## Available Manual Tests

### ManualJCoConnectionTest.java
Tests SAP JCo connection and basic RFC calls.

**Tests:**
- Connection pooling
- RFC metadata retrieval
- Basic connectivity

**Use Case**: Verify SAP connection configuration, debug connection issues

### ManualClassServiceTest.java
Tests class source retrieval (read-only operations).

**Tests:**
- `getClassSource()` - Retrieve ABAP class source code
- Different include types (main, implementation)

**Use Case**: Verify Progressive Discovery Stage 3, debug source retrieval

### ManualTransportObjectsTest.java
Tests transport query operations (read-only).

**Tests:**
- `list_user_transports` - List user's transports
- `get_transport_objects` - Get objects in transport

**Use Case**: Debug transport queries, verify transport object retrieval

## Debug Mode

### Option 1: IDE Debug (Recommended)

1. Enable the test (comment `@Disabled`)
2. Open test file in your IDE
3. Right-click on test method
4. Select "Debug 'testName()'"
5. Set breakpoints in:
   - `ClassService.getClassSource()` - for class retrieval tests
   - `RfcAdapter.request()` - for connection tests
   - `TransportService.listUserTransports()` - for transport tests

### Option 2: Maven Remote Debug

```bash
# Terminal 1: Start Maven in debug mode (waits for debugger)
mvn -Dmaven.surefire.debug test -Dtest=ManualJCoConnectionTest

# Terminal 2: Attach your IDE debugger to localhost:5005
```

## Best Practices

1. **Always re-disable tests after use** - prevents accidental execution
2. **Use descriptive test names** - clear intent and debugging context
3. **Add comprehensive logging** - System.out.println for visibility
4. **Document prerequisites** - what state the system should be in
5. **Keep tests isolated** - each test should be runnable independently

## Troubleshooting

### Test runs all tests instead of one

**Solution**: Use `@Disabled` pattern as described above.

### Test fails with "No tests found"

**Cause**: `@Disabled` annotation is active.
**Solution**: Comment out `@Disabled` before running.

### Test runs but hangs

**Cause**: Waiting for SAP connection or long-running operation.
**Solution**: Check SAP connection, increase timeout, or use debug mode.

## Last Updated

2025-11-20
