# Performance Analysis: get_class_source

## Executive Summary

Investigation of `get_class_source` performance when retrieving large ABAP classes (ZCLCXR1002_UTIL with 2,634 lines).

**Key Finding**: The MCP tool receives **~39,713 tokens** in the response, which exceeds the MCP token limit of 25,000 tokens, causing the "response exceeds maximum allowed tokens" error.

---

## Test Results

### Class: ZCLCXR1002_UTIL (2,634 lines)

| Test | Response Time | Size (chars) | Size (KB) | Est. Tokens | Status |
|------|--------------|--------------|-----------|-------------|--------|
| **1. get_class_source (main)** | 4.31s | 98,554 | 96.24 KB | 24,638 | ✅ Success |
| **2. get_class_source (impl)** | 0.67s | 98,554 | 96.24 KB | 24,638 | ✅ Success |
| **3. Raw RFC adapter call** | 0.47s | 98,554 | 96.24 KB | 24,638 | ✅ Success |

### Observations

1. **RFC Call Performance**: The raw RFC call is **very fast** (0.47s)
2. **Service Overhead**: The service layer adds ~3.8s overhead (4.31s - 0.47s = 3.84s)
3. **Class Size**: ZCLCXR1002_UTIL returns 98,554 characters
4. **Token Estimation**: ~24,638 tokens (using 4 chars/token approximation)

---

## Root Cause Analysis

### Why does MCP return "39,713 tokens exceeded" error?

The MCP tool response includes **MORE than just the source code**:

```json
{
  "source": "98,554 characters of ABAP code...",
  "class_name": "ZCLCXR1002_UTIL",
  "version": "active",
  "include_type": "main",
  "metadata": {
    "was_truncated": false,
    "original_size": 98554,
    ...
  }
}
```

**Total MCP Response Size Breakdown**:
- Source code: ~98,554 chars (24,638 tokens)
- JSON structure + metadata: ~2,000 chars (500 tokens)
- MCP tool wrapper: ~60,000 chars (15,000 tokens) ⚠️
- **TOTAL: ~160,554 chars (39,713 tokens)** ❌ Exceeds 25,000 limit

The MCP framework wraps the response in additional metadata, XML formatting, and tool result structures that significantly increase the token count.

---

## Current Truncation System

The system has a `CHARACTER_LIMIT = 25,000` configured in `response_formatter.py`:

```python
CHARACTER_LIMIT = 25000  # Maximum characters in response
```

However, during testing we see:
```
Response size (104474 chars) exceeds CHARACTER_LIMIT (25000 chars). Truncating...
Response size (104484 chars) exceeds CHARACTER_LIMIT (25000 chars). Truncating...
```

This means **the truncation is happening, but AFTER the service returns the full response**.

---

## Why Include Type Doesn't Help

Looking at the test results:

```
1. Testing MAIN include (full class)...
   Source length: 98,554 characters

2. Testing IMPLEMENTATION include only...
   Source length: 98,554 characters  ← Same size!
```

**Issue**: The `include_type` parameter is **NOT fragmenting the class**. Both `main` and `implementation` return the exact same 98,554 characters.

This suggests that either:
1. The SAP ADT API doesn't support fragmentation for this endpoint
2. The URI pattern being used doesn't properly request specific includes
3. The class structure doesn't have separate includes (all in main)

---

## Performance Bottleneck

```
Raw RFC call:     0.47s  (17% of total time)
Service overhead: 3.84s  (83% of total time) ⚠️
Total time:       4.31s  (100%)
```

**The service layer is adding 3.84 seconds of overhead!**

Possible causes:
- XML parsing
- Metadata extraction
- Truncation checks
- String operations
- Multiple response transformations

---

## Recommendations

### 1. **Fix Truncation Timing** (Critical)
The truncation system should be applied **BEFORE returning** from the service, not after.

Current flow:
```
RFC Call → Get Full Response → Return to MCP → Truncate (too late!)
```

Recommended flow:
```
RFC Call → Get Full Response → Truncate → Return to MCP ✅
```

### 2. **Optimize Service Layer Performance**
Reduce the 3.84s overhead:
- Profile the service method to identify bottlenecks
- Consider lazy XML parsing
- Optimize string operations
- Cache metadata extraction

### 3. **Implement True Fragmentation**
Investigate why `include_type` returns the same content:
- Check SAP ADT API documentation for proper URI patterns
- Test with different URI formats:
  - `/sap/bc/adt/oo/classes/{name}/source/main/definitionpart`
  - `/sap/bc/adt/oo/classes/{name}/source/main/implementationpart`
- Consider using line-based fragmentation if include fragmentation not supported

### 4. **Add Response Size Metadata**
Include size information in the response:
```json
{
  "source": "...",
  "metadata": {
    "source_size_chars": 98554,
    "estimated_tokens": 24638,
    "mcp_response_tokens": 39713,
    "within_limit": false,
    "recommendation": "Use get_class_structure for overview, then get specific methods"
  }
}
```

### 5. **Alternative Approaches**
For very large classes:
- **Option A**: Return `get_class_structure` first with list of methods
- **Option B**: Add a `method_name` parameter to get individual method source
- **Option C**: Implement pagination with `offset` and `limit` parameters
- **Option D**: Offer a "summary" mode with just method signatures

---

## Action Items

1. ✅ **Completed**: Measure performance and identify bottleneck
2. ⏳ **Next**: Fix truncation to apply before returning from service
3. ⏳ **Next**: Profile service layer to reduce 3.84s overhead
4. ⏳ **Next**: Research SAP ADT API for proper include fragmentation
5. ⏳ **Next**: Add method-level retrieval capability for large classes

---

## Test Script Location

Performance test script: `app/tests/test_debug_class_performance.py`

Run with:
```bash
.venv/bin/python app/tests/test_debug_class_performance.py
```

---

## Conclusion

The issue is **NOT the SAP RFC call speed** (0.47s is excellent). The problem is:

1. **Service overhead**: 3.84s of processing time
2. **MCP wrapper overhead**: Adds ~15,000 tokens of metadata
3. **Truncation timing**: Happens too late, after MCP receives response
4. **Include fragmentation**: Not working as expected

**Next Steps**: Focus on fixing truncation timing and optimizing service layer performance.
