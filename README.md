# SAP ABAP MCP Server

**Production-ready MCP server for SAP ABAP integration** using **Spring Boot 3.4.0** and **SAP JCo 3.1.x**.

[![Java](https://img.shields.io/badge/Java-21+-orange)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen)](https://spring.io/projects/spring-boot)
[![MCP Tools](https://img.shields.io/badge/MCP%20Tools-36-blue)](docs/requirements/mcp/)
[![MCP Resources](https://img.shields.io/badge/MCP%20Resources-9-green)](docs/requirements/mcp/resources_implementation_plan.md)
[![License](https://img.shields.io/badge/License-Internal-red)](LICENSE)

---

## Table of Contents

- [Overview](#overview)
- [MCP Tools Catalog](#mcp-tools-catalog)
- [MCP Resources Catalog](#mcp-resources-catalog)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Building & Running](#building--running)
- [Testing Strategy](#testing-strategy)
- [Architecture](#architecture)
- [Troubleshooting](#troubleshooting)

---

## Overview

This project implements a **Model Context Protocol (MCP) server** that enables AI assistants like **Claude Code** to interact with SAP ABAP systems directly via RFC. It provides **36 production-ready tools** for:

- **Source Code Management**: Read, modify, and activate ABAP objects (classes, programs, function modules)
- **Object Creation**: Create classes, interfaces, function groups, function modules, and tables
- **Transport Management**: Create, copy, search, and release transport requests
- **System Analysis**: Analyze dumps (ST22), check syntax, navigate package hierarchy
- **Progressive Discovery**: Efficient token usage through staged information retrieval

### Key Features

- **Spring AI MCP SDK 1.1.0-M4** for standardized MCP integration
- **SAP JCo 3.1.x** native connection pooling (5-10 concurrent connections)
- **Stateful Connections** for reliable LOCK → MODIFY → UNLOCK workflows
- **Multi-platform**: Windows, macOS, Linux support
- **STDIO Transport**: Compatible with Claude Desktop and Claude Code

---

## MCP Tools Catalog

### Summary: 36 Tools in 17 Categories

| Category | Tools | Description |
|----------|-------|-------------|
| [Source Code](#source-code-tools) | 6 | Read source code for classes, programs, includes |
| [Modification](#modification-tools) | 4 | Modify classes, programs, function modules |
| [Object Creation](#creation-tools) | 5 | Create ABAP objects and tables |
| [Search & Navigation](#search--navigation-tools) | 4 | Search objects, navigate packages |
| [Transport Management](#transport-management-tools) | 5 | List, search, query transport requests |
| [Transport Operations](#transport-operations-tools) | 6 | Create, copy, modify, release transports |
| [Activation & Syntax](#activation--syntax-tools) | 3 | Activate objects, check syntax |
| [System Analysis](#system-analysis-tools) | 3 | Analyze dumps, package hierarchy |

---

### Source Code Tools

Tools for retrieving ABAP source code with Progressive Discovery architecture.

| Tool | Description | Token Cost |
|------|-------------|------------|
| `get_class_source` | Get ABAP class source code (definition, implementation, testclasses, macros) | ~2,000+ |
| `get_class_includes` | List class includes without fetching source (Stage 2.5) | ~200 |
| `get_program_source` | Get program source (reports, module pools) | ~3,000+ |
| `get_include_source` | Get specific include source code | ~500-2,000 |
| `get_object_source` | Generic source retrieval by ADT URI (Stage 3) | ~3,000+ |
| `get_object_structure` | Get object metadata without source (Stage 2) | ~800 |

**Progressive Discovery Pattern:**
1. **Stage 1**: `search_objects` → Find objects (~300 tokens)
2. **Stage 2**: `get_object_structure` → Get metadata (~800 tokens)
3. **Stage 2.5**: `get_class_includes` → List includes (~200 tokens)
4. **Stage 3**: `get_object_source` → Get full source (~3,000+ tokens)

---

### Modification Tools

Tools for modifying ABAP source code with automatic LOCK → MODIFY → UNLOCK workflow.

| Tool | Description | Workflow |
|------|-------------|----------|
| `modify_class` | Modify class source (main, implementations, testclasses) | Stateful: LOCK → MODIFY → UNLOCK |
| `modify_program_source` | Modify program or include source | Stateful: LOCK → MODIFY → UNLOCK |
| `modify_function_module` | Modify function module source | Stateful: LOCK → MODIFY → UNLOCK |
| `delete_object` | Delete ABAP object (class, interface, program, FM) | Stateful: LOCK → DELETE → UNLOCK |

**Key Features:**
- Automatic stateful session management
- Lock handles persisted throughout workflow
- Automatic unlock even on failure
- Transport assignment support

---

### Creation Tools

Tools for creating new ABAP objects in the repository.

| Tool | Description | Package Support |
|------|-------------|-----------------|
| `create_class` | Create new ABAP class | $TMP or transportable |
| `create_interface` | Create new ABAP interface | $TMP or transportable |
| `create_function_group` | Create function group container | $TMP or transportable |
| `create_function_module` | Create function module (normal or RFC-enabled) | Requires existing FG |
| `create_table` | Create transparent table with structured fields | $TMP or transportable |

**Examples:**
```
create_class('ZCL_TEST', 'Test Class', '$TMP', null, null)
create_function_module('Z_FM_TEST', 'ZTEST_FG', 'Test FM', null, 'rfc')
create_table('ZTAB1', 'Test Table', [{name:'field1', type:'char10', isKey:true}], '$TMP', null)
```

---

### Search & Navigation Tools

Tools for finding and navigating ABAP objects.

| Tool | Description | Token Cost |
|------|-------------|------------|
| `search_objects` | Search by name pattern with wildcards | ~300-500 |
| `get_package_objects` | List objects in a package with filtering | ~1,000-5,000 |
| `getPackageHierarchy` | Get package parent/child relationships | ~500-1,000 |
| `get_ddic_source` | Get DDIC object structure (tables, views, structures) | ~500-2,000 |

**Search Examples:**
```
search_objects('ZCL_*')        → Classes starting with ZCL_
search_objects('*invoice*')    → Objects containing "invoice"
search_objects('CL_ABAP_*')    → Standard ABAP classes
```

---

### Transport Management Tools

Tools for querying transport request information.

| Tool | Description | Use Case |
|------|-------------|----------|
| `list_user_transports` | List transports for a user | "Show my open transports" |
| `get_transport_objects` | Get objects in a transport | "What's in CADK911088?" |
| `get_transport_info` | Get transport metadata (status, owner, dates) | "What's the status of X?" |
| `get_object_in_open_ot` | Check if object is in an open transport | "Can I modify ZCL_TEST?" |
| `search_transports` | Search transports by criteria | "Find transports for PSR001" |

**Status Codes:**
- `D`: Modifiable (open)
- `R`: Released
- `L`: Protected (locked)

---

### Transport Operations Tools

Tools for creating and managing transport requests.

| Tool | Description | Workflow |
|------|-------------|----------|
| `create_transport_request` | Create new OT (Workbench, Customizing, Copy) | CREATE → COPY_OBJECTS → RELEASE |
| `create_transport_copy` | Copy objects from existing transport(s) | QUERY → CREATE → COPY → RELEASE |
| `add_objects_to_transport` | Add objects to a transport | Auto-assigns to task |
| `force_add_objects_to_transport` | Add objects bypassing lock validation | For locked objects |
| `modify_transport_description` | Change transport description | Max 60 chars |
| `release_transport` | Release transport with all tasks | Requires confirmation |
| `release_task` | Release single task | Direct release |
| `get_transport_log` | Get transport log (errors/warnings only) | Troubleshooting |

**Transport Types:**
- `K`: Workbench (development objects)
- `W`: Customizing (configuration)
- `T`: Transport of Copies

---

### Activation & Syntax Tools

Tools for validating and activating ABAP objects.

| Tool | Description | Use Case |
|------|-------------|----------|
| `check_syntax` | Check syntax (errors, warnings, info) | Before activation |
| `getInactiveObjects` | List all inactive objects | Find what needs activation |
| `activateObjects` | Activate one or more objects | After modification |

**Workflow Pattern:**
1. Modify object → `modify_class`
2. Check syntax → `check_syntax` (version='inactive')
3. Fix errors if any
4. Activate → `activateObjects`

---

### System Analysis Tools

Tools for system diagnostics and analysis.

| Tool | Description | Equivalent |
|------|-------------|------------|
| `list_dumps` | List ABAP dumps by date/user | ST22 |
| `get_dump_details` | Get full dump analysis | ST22 detail |
| `getPackageHierarchy` | Navigate package tree | SE80 |

**Dump Analysis Workflow:**
1. `list_dumps('2025-01-15', '2025-01-15', 'DEVELOPER')` → Find dumps
2. `get_dump_details(dumpId)` → Get full analysis
3. Review: error type, call stack, source code, variables

---

## MCP Resources Catalog

MCP Resources provide **read-only URI-based access** to SAP data with automatic caching support. Unlike Tools, Resources are lightweight and optimized for token efficiency.

### Summary: 9 Resources in 4 Categories

| Category | Resources | Description |
|----------|-----------|-------------|
| [Class Resources](#class-resources) | 4 | Source code, methods, attributes |
| [Transport Resources](#transport-resources) | 2 | Transport info and objects |
| [Package Resources](#package-resources) | 2 | Package objects and hierarchy |
| [Table Resources](#table-resources) | 1 | DDIC field definitions |

---

### Class Resources

| URI Pattern | Description | MIME Type | Token Cost |
|------------|-------------|-----------|------------|
| `sap://class/{name}/definition` | Class definition source | `text/plain` | ~2,000+ |
| `sap://class/{name}/implementation` | Class implementation source | `text/plain` | ~2,000+ |
| `sap://class/{name}/methods` | List of methods with metadata | `application/json` | ~300-500 |
| `sap://class/{name}/attributes` | List of attributes with metadata | `application/json` | ~200-400 |

**Example:** `sap://class/CL_ABAP_CHAR_UTILITIES/methods`

---

### Transport Resources

| URI Pattern | Description | MIME Type | Token Cost |
|------------|-------------|-----------|------------|
| `sap://transport/{id}/info` | Transport metadata (owner, status, dates) | `application/json` | ~300-500 |
| `sap://transport/{id}/objects` | Objects in transport | `application/json` | ~500-2000 |

**Example:** `sap://transport/DEVK900123/info`

---

### Package Resources

| URI Pattern | Description | MIME Type | Token Cost |
|------------|-------------|-----------|------------|
| `sap://package/{name}/objects` | Objects in package from TADIR | `application/json` | ~500-2000 |
| `sap://package/{name}/hierarchy` | Subpackages from TDEVC | `application/json` | ~300-800 |

**Example:** `sap://package/ZCX/objects`

---

### Table Resources

| URI Pattern | Description | MIME Type | Token Cost |
|------------|-------------|-----------|------------|
| `sap://table/{name}/fields` | Field definitions from DD03L | `application/json` | ~400-1500 |

**Example:** `sap://table/MARA/fields`

---

### Resources vs Tools

| Aspect | Tools | Resources |
|--------|-------|-----------|
| **Purpose** | Execute actions | Expose read-only data |
| **Side Effects** | Yes (modifications) | No (read-only) |
| **Caching** | Not recommended | Client can cache |
| **Discovery** | `tools/list` | `resources/templates/list` |
| **Token Efficiency** | Full response | Optimized structure |

### Important: Resource Templates vs Static Resources

This server implements **Resource Templates** (URIs with placeholders like `{name}`, `{id}`) rather than Static Resources. This design choice has implications:

**Resource Types:**
- **Static Resources**: Fixed URIs like `sap://server/info` (no placeholders)
- **Resource Templates**: Parameterized URIs like `sap://class/{name}/definition`

**Discovery Limitation:**
- Claude Code's `ListMcpResourcesTool` only lists **Static Resources**
- **Resource Templates are NOT listed** but work correctly when read directly
- Use the static resource `sap://server/info` to see all available templates

**How to Use Resources:**

```bash
# 1. Get list of available resource templates (static resource)
ReadMcpResourceTool server=giralmcp uri=sap://server/info

# 2. Read a specific resource by replacing placeholders
ReadMcpResourceTool server=giralmcp uri=sap://class/CL_ABAP_CHAR_UTILITIES/methods
ReadMcpResourceTool server=giralmcp uri=sap://transport/DEVK900123/info
ReadMcpResourceTool server=giralmcp uri=sap://package/ZCX/objects
```

**Why Resource Templates?**
- SAP objects are dynamic (classes, transports, packages vary per system)
- Parameterized URIs allow access to any object without pre-registration
- More flexible than registering thousands of static resources

---

## Prerequisites

| Component | Version | Download |
|-----------|---------|----------|
| **Java** | 21+ (LTS) | [Adoptium OpenJDK](https://adoptium.net/) |
| **Maven** | 3.9+ | [Apache Maven](https://maven.apache.org/) |
| **SAP JCo** | 3.1.9+ | [SAP Support Portal](https://support.sap.com/en/product/connectors/jco.html) |

```bash
# Verify installation
java -version   # Should be 21+
mvn -version    # Should be 3.9+
```

---

## Installation

### 1. Clone Repository

```bash
git clone <repository-url>
cd giralmcp
```

### 2. Install SAP JCo Libraries

SAP JCo libraries cannot be redistributed (SAP license). Download from SAP Support Portal:

| Platform | File | Target |
|----------|------|--------|
| Windows x64 | `SAPJCO3_NTAMD64_*.ZIP` | `lib/sapjco3.dll` + `lib/sapjco3.jar` |
| macOS Intel | `sapjco3-darwinintel64-*.tgz` | `lib/libsapjco3.dylib` + `lib/sapjco3.jar` |
| macOS ARM | `sapjco3-darwinarm64-*.tgz` | `lib/libsapjco3.dylib` + `lib/sapjco3.jar` |
| Linux x64 | `sapjco3-linuxx86_64-*.tgz` | `lib/libsapjco3.so` + `lib/sapjco3.jar` |

### 3. Verify Installation

```bash
ls lib/
# Should show: sapjco3.jar + platform-specific native library
```

---

## Configuration

### Environment Variables

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `SAP_ASHOST` | Yes | SAP application server | `sap.company.com` |
| `SAP_SYSNR` | Yes | System number | `00` |
| `SAP_CLIENT` | Yes | Client number | `100` |
| `SAP_USER` | Yes | SAP username | `DEVELOPER` |
| `SAP_PASSWD` | Yes | SAP password | `*****` |
| `SAP_LANG` | No | Language (default: EN) | `EN` |
| `SAP_ROUTER` | No | SAP router string | `/H/router/S/3299` |
| `SAP_POOL_CAPACITY` | No | Pool size (default: 5) | `5` |
| `SAP_PEAK_LIMIT` | No | Peak limit (default: 10) | `10` |

### Claude Desktop Configuration

**Windows** (`%APPDATA%\Claude\claude_desktop_config.json`):
```json
{
  "mcpServers": {
    "giralmcp": {
      "command": "mvn.cmd",
      "args": ["spring-boot:run", "-f", "C:\\path\\to\\giralmcp\\pom.xml"],
      "env": {
        "JAVA_HOME": "C:\\Program Files\\Eclipse Adoptium\\jdk-21",
        "SAP_ASHOST": "sap.company.com",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "100",
        "SAP_USER": "username",
        "SAP_PASSWD": "password"
      }
    }
  }
}
```

**macOS** (`~/Library/Application Support/Claude/claude_desktop_config.json`):
```json
{
  "mcpServers": {
    "giralmcp": {
      "command": "mvn",
      "args": ["spring-boot:run", "-f", "/path/to/giralmcp/pom.xml"],
      "env": {
        "JAVA_HOME": "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home",
        "SAP_ASHOST": "sap.company.com",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "100",
        "SAP_USER": "username",
        "SAP_PASSWD": "password"
      }
    }
  }
}
```

---

## Building & Running

### Compilation

```bash
# Clean and compile
mvn clean compile

# Compile with test classes
mvn clean test-compile

# Build JAR (skip tests)
mvn clean package -DskipTests

# Build JAR (with tests)
mvn clean package
```

### Running the Server

**Option 1: Via Maven (recommended for development)**
```bash
mvn spring-boot:run
```

**Option 2: Via startup scripts**
```bash
# Windows
start-mcp.bat

# macOS/Linux
./start-mcp.sh
```

**Option 3: Via JAR**
```bash
# Windows
java -Djava.library.path=.\lib -jar target\sap-mcp-server-0.1.0-POC.jar

# macOS/Linux
java -Djava.library.path=./lib -jar target/sap-mcp-server-0.1.0-POC.jar
```

---

## Testing Strategy

### Test Types

| Type | Location | Purpose | Command |
|------|----------|---------|---------|
| **Unit Tests** | `src/test/java/.../` | Test services with mocks | `mvn test` |
| **Manual Tests** | `src/test/java/.../manual/` | Test against real SAP | `mvn spring-boot:run -Dspring-boot.run.mainClass=...` |
| **Integration Tests** | `src/test/java/.../integration/` | Full workflow tests | `mvn verify` |

### Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=ClassServiceTest

# Specific test method
mvn test -Dtest=ClassServiceTest#testGetClassSource

# Skip tests
mvn package -DskipTests
```

### Manual Tests (CommandLineRunner Pattern)

For testing against real SAP systems, we use CommandLineRunner pattern instead of JUnit:

```bash
# Run manual test for transport modification
mvn spring-boot:run \
  -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.transport.ManualTransportModificationTest
```

**Available Manual Tests:**
- `ManualTransportModificationTest` - Test add objects, release, force-add
- `ManualTransportCreationTest` - Test transport creation
- `ManualTransportCopyTest` - Test transport copy operations
- `ManualTransportLogTest` - Test transport log retrieval
- `ManualTransportSearchTest` - Test transport search
- `ManualDumpServiceTest` - Test dump analysis

### Test Coverage

Target: 80%+ for business logic

```bash
# Generate coverage report
mvn test jacoco:report

# View report
open target/site/jacoco/index.html
```

---

## Architecture

### High-Level Flow

```
Claude Code (LLM)
    ↓ STDIO (JSON-RPC)
Spring AI MCP Server
    ↓
RfcAdapter (HTTP-style → RFC)
    ↓
SAP JCo (Connection Pool)
    ↓
SADT_REST_RFC_ENDPOINT (FM)
    ↓
SAP ADT REST API
    ↓
SAP ABAP System
```

### Key Components

| Component | Purpose |
|-----------|---------|
| `JCoConfiguration` | Connection pool setup (5-10 connections) |
| `RfcAdapter` | HTTP-to-RFC adapter for ADT API |
| `StatefulModificationService` | LOCK → MODIFY → UNLOCK workflow |
| `*Service` | Business logic per domain |
| `*Tools` | MCP tool definitions (auto-discovered) |

### Stateful vs Stateless Operations

| Operation Type | Mode | Reason |
|----------------|------|--------|
| Read operations | Stateless | No locks needed |
| Create operations | Stateless | No locks needed |
| **Modify operations** | **Stateful** | Lock persistence required |
| Query operations | Stateless | No locks needed |

---

## Troubleshooting

### Common Issues

| Error | Cause | Solution |
|-------|-------|----------|
| `no sapjco3 in java.library.path` | JCo library not found | Verify `lib/` contains native library |
| `Connect to SAP gateway failed` | Network issue | Check VPN, SAP_ROUTER |
| `SADT_REST_RFC_ENDPOINT not found` | ADT not installed | Contact SAP Basis |
| `Type conflict during function module call` | FM signature mismatch | Fix ABAP FM in SAP |
| `Object locked by another user` | Concurrent modification | Use `get_object_in_open_ot` to check |

### Debug Logging

Enable debug logging in `application.yml`:
```yaml
logging:
  level:
    com.crystal.mcp: DEBUG
```

Logs location: `logs/java/sap-mcp-server.log`

---

## Documentation

- **Developer Guide**: [CLAUDE.md](CLAUDE.md)
- **SAP JCo Setup**: [lib/README.md](lib/README.md)
- **Python Legacy**: [python-legacy/PYTHON_LEGACY.md](python-legacy/PYTHON_LEGACY.md)

---

## References

- [Spring AI MCP SDK](https://spring.io/blog/2025/02/14/mcp-java-sdk-released-2)
- [SAP JCo Documentation](https://support.sap.com/en/product/connectors/jco.html)
- [Model Context Protocol](https://modelcontextprotocol.io)

---

## Status

**Version**: 0.2.0
**Tools**: 36/36 (100% of current scope)
**Last Updated**: 2025-12-05
**Team**: Crystal Development Team
