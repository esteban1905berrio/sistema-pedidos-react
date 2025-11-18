# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is a **Java-based MCP (Model Context Protocol) server** that enables LLM tools like Claude Code to interact with SAP ABAP systems using **Spring Boot** and **SAP JCo**.

**Project Status**: Active migration from Python (PyRFC) to Java (SAP JCo)
- **Main Project**: `src/` - Java MCP Server (Spring Boot 3.4.0 + SAP JCo 3.1.x)
- **Legacy Reference**: `python-legacy/` - Python MCP Server (59 tools, functional, archived)

**Project Purpose**: Implement a production-ready MCP server for SAP ABAP that:
- Uses Spring AI MCP SDK 1.1.0-M4 for standardized MCP integration
- Leverages SAP JCo native connection pooling for enterprise-grade performance
- Provides the same 59 tools as the Python version (migration in progress - currently 1/59)
- Implements optimizations: Progressive Discovery, Workflow-based Tools
- Allows direct SAP system access via RFC instead of HTTP-based ADT endpoints

**See Migration Plan**: `docs/requirements/mcp/migration_plan.md`

---

## Technology Stack

**Version Policy**: Use LATEST STABLE versions of all libraries unless technical incompatibilities exist.

### Current Java Stack

- **Java 21+** (LTS version)
- **Spring Boot 3.4.0** (Application framework)
- **Spring AI MCP SDK 1.1.0-M4** (Official Java SDK for MCP servers)
- **SAP JCo 3.1.x** (SAP Java Connector for RFC)
- **Maven 3.9+** (Build tool)
- **JUnit 5** (Testing)
- **SLF4J + Logback** (Logging)

### Legacy Python Stack (python-legacy/)

- **Python 3.11+**
- **PyRFC** (SAP RFC connector)
- **FastMCP** (MCP server framework)
- **pytest** (Testing)

---

## Project Organization

### Directory Structure

```
brootpersonalagent/
├── src/                          # ⭐ Java MCP Server (MAIN PROJECT)
│   └── main/java/com/crystal/mcp/sapserver/
│       ├── SapMcpServerApplication.java       # Main class
│       ├── config/
│       │   └── JCoConfiguration.java          # JCo connection pool config
│       ├── service/
│       │   ├── RfcAdapter.java               # HTTP-to-RFC bridge
│       │   └── ClassService.java             # Business logic
│       ├── tool/
│       │   └── ClassTools.java               # MCP tool definitions
│       └── model/
│           └── ClassSourceResult.java        # DTOs
│
├── lib/                          # SAP JCo SDK (sapjco3.jar + native libs)
│   ├── sapjco3.jar                           # Platform-independent JAR
│   ├── libsapjco3.dylib                      # macOS native library
│   ├── libsapjco3.so                         # Linux native library
│   ├── sapjco3.dll                           # Windows native library
│   └── README.md                             # JCo installation instructions
│
├── python-legacy/                # 📦 Archived Python Project (REFERENCE ONLY)
│   ├── app/                      # Python source (59 MCP tools)
│   ├── PyRFC/                    # SAP RFC SDK bindings
│   ├── .venv/                    # Virtual environment
│   ├── pyproject.toml
│   └── PYTHON_LEGACY.md          # Usage instructions
│
├── docs/                         # Documentation
│   ├── requirements/
│   │   └── mcp/
│   │       └── migration_plan.md # ⭐ Complete migration plan
│   ├── research/
│   │   └── abap_mcp_tools_strategy_2025.md
│   └── architecture/
│
├── logs/                         # Application logs
│   ├── python/                   # Python MCP server logs
│   └── java/                     # Java MCP server logs
│
├── resources/                    # Reference projects and resources
│
├── start-mcp.bat                 # ⭐ Windows startup script (Command Prompt)
├── start-mcp.ps1                 # ⭐ Windows startup script (PowerShell)
├── start-mcp.sh                  # ⭐ macOS/Linux startup script
├── pom.xml                       # ⭐ Maven configuration (root)
├── README_JAVA.md                # Java project documentation (detailed)
├── README.md                     # Main README with multi-OS setup instructions
├── CLAUDE.md                     # This file
└── .mcp.json                     # MCP server configuration (local, gitignored)
```

### File Naming Conventions

**Java Services**: `<Category>Service.java` (e.g., `ClassService.java`, `TransportService.java`)
**Java Tools**: `<Category>Tools.java` (e.g., `ClassTools.java`, `TransportTools.java`)
**Java Tests**: `<Category>ServiceTest.java` (e.g., `ClassServiceTest.java`)
**Logs**: Store in `logs/java/` directory

---

## Core Development Commands

### Quick Start (Recommended)

**Platform-specific startup scripts** handle all configuration automatically:

```bash
# Windows (Command Prompt)
start-mcp.bat

# Windows (PowerShell)
.\start-mcp.ps1

# macOS / Linux
./start-mcp.sh
```

The scripts will:
- ✅ Verify Java and Maven installation
- ✅ Check for SAP JCo libraries (platform-specific)
- ✅ Set library paths automatically
- ✅ Display helpful error messages
- ✅ Start the MCP server

### Java Project (Main)

```bash
# Compilation
mvn clean compile              # Clean and compile
mvn clean package             # Build JAR

# Testing
mvn test                      # Run all tests
mvn test -Dtest=ClassServiceTest  # Run specific test
mvn clean test                # Clean and test

# Execution
mvn spring-boot:run           # Run via Maven

# Run JAR (platform-specific library path)
# Windows:
java -Djava.library.path=.\lib -jar target\sap-mcp-server-0.1.0-POC.jar
# macOS/Linux:
java -Djava.library.path=./lib -jar target/sap-mcp-server-0.1.0-POC.jar

# Code Quality
mvn verify                    # Run verification (includes tests)
mvn clean install             # Install to local Maven repo
```

### Python Legacy Project (Reference Only)

```bash
cd python-legacy/

# Testing (for reference/comparison)
.venv/bin/python -m pytest app/tests/ -v

# Run Python MCP server (via Claude Desktop .mcp.json)
# Servers: CRY, GDC
```

### MCP Server Configuration

**Platform-Specific Configuration for Claude Desktop**:

#### Windows

Edit `%APPDATA%\Claude\claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "giralmcp": {
      "command": "mvn.cmd",
      "args": ["spring-boot:run", "-f", "C:\\path\\to\\giralmcp\\pom.xml"],
      "env": {
        "JAVA_HOME": "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.5.11-hotspot",
        "SAP_ASHOST": "sap.server.com",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "100",
        "SAP_USER": "username",
        "SAP_PASSWD": "password",
        "SAP_LANG": "EN",
        "SAP_ROUTER": "/H/router/S/port",
        "SAP_POOL_CAPACITY": "5",
        "SAP_PEAK_LIMIT": "10"
      }
    }
  }
}
```

**Important**: Use `mvn.cmd` on Windows (not `mvn`).

#### macOS

Edit `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "giralmcp": {
      "command": "mvn",
      "args": ["spring-boot:run", "-f", "/Users/username/giralmcp/pom.xml"],
      "env": {
        "JAVA_HOME": "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home",
        "SAP_ASHOST": "sap.server.com",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "100",
        "SAP_USER": "username",
        "SAP_PASSWD": "password",
        "SAP_LANG": "EN",
        "SAP_ROUTER": "/H/router/S/port",
        "SAP_POOL_CAPACITY": "5",
        "SAP_PEAK_LIMIT": "10"
      }
    }
  }
}
```

#### Linux

Edit `~/.config/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "giralmcp": {
      "command": "mvn",
      "args": ["spring-boot:run", "-f", "/home/username/giralmcp/pom.xml"],
      "env": {
        "JAVA_HOME": "/usr/lib/jvm/java-21-openjdk-amd64",
        "SAP_ASHOST": "sap.server.com",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "100",
        "SAP_USER": "username",
        "SAP_PASSWD": "password",
        "SAP_LANG": "EN",
        "SAP_ROUTER": "/H/router/S/port",
        "SAP_POOL_CAPACITY": "5",
        "SAP_PEAK_LIMIT": "10"
      }
    }
  }
}
```

---

## Development Lifecycle (SDLC)

### Phase 1: Requirements Analysis
- Use `docs/requirements/pr_*.md` to analyze User Stories
- Present analysis before implementation
- Ask questions to understand functional and technical requirements
- Wait for user approval before proceeding

### Phase 2: Design & Framework Research
- **Mandatory framework research**: Search for existing solutions
  1. Use Context7 MCP for library documentation
  2. Use WebSearch for comparisons and community discussions
  3. Use WebFetch for specific documentation
- Write research findings in `docs/research/`
- Present implementation plan with milestones
- Update PR document with approved plan

### Phase 3: Implementation
- Follow Solution Simplification Rules
- Use TodoWrite tool for task tracking
- Update PR document when concluding each phase
- **For Java**: Follow Spring Boot best practices
- **Reference Python**: Use `python-legacy/` as behavioral reference

### Phase 4: Testing

**Java Testing Standards:**

```bash
# Test location: src/test/java/
# Naming: <Service>Test.java

# Run tests
mvn test

# Run with coverage
mvn test jacoco:report
# Report: target/site/jacoco/index.html
```

**Coverage Requirements:**
- 80%+ coverage for business logic
- All services require corresponding test files
- Integration tests use real SAP connection (configured via env vars)

**Java Test Pattern (MANDATORY):**

```java
@SpringBootTest
class ClassServiceTest {

    @Autowired
    private ClassService classService;

    @Test
    void testGetClassSource() {
        // Given
        String className = "CL_ABAP_CHAR_UTILITIES";

        // When
        ClassSourceResult result = classService.getClassSource(
            className, "active", "main"
        );

        // Then
        assertNotNull(result.getSource());
        assertTrue(result.getSource().contains("CLASS"));
    }
}
```

### Phase 5: Documentation
- JavaDoc for all public methods
- Update README_JAVA.md with new tools
- Document architectural decisions

---

## Migration Strategy

**Current Status**: Phase 0 Complete (Project Reorganization)

**Progress**: 1/59 tools (1.7%)
```
[██░░░░░░░░░░░░░░░░░░] 1/59 tools
```

**Next Phase**: Phase 1 - Paridad Funcional Core (16 tools)

**See Full Plan**: `docs/requirements/mcp/migration_plan.md`

### Using Python as Reference

When migrating tools from Python to Java:

1. **Read Python implementation**: `python-legacy/app/services/<category>_service.py`
2. **Understand ADT API calls**: Check `RfcAdapter` usage in Python
3. **Implement Java equivalent**: Use `RfcAdapter.java` in Java
4. **Compare behavior**: Run both implementations and verify identical results
5. **Document differences**: Note any Java-specific optimizations

**Example Migration Flow:**

```bash
# 1. Read Python implementation
cat python-legacy/app/services/class_service.py

# 2. Identify ADT endpoint
# Example: /sap/bc/adt/oo/classes/{name}/source/main

# 3. Implement Java service
# src/main/java/.../service/ClassService.java

# 4. Create MCP tool
# src/main/java/.../tool/ClassTools.java

# 5. Test
mvn test -Dtest=ClassServiceTest

# 6. Compare with Python
cd python-legacy && .venv/bin/python -m pytest app/tests/test_class_service.py -v
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

**JCoConfiguration** (`src/main/java/.../config/JCoConfiguration.java`):
- Thread-safe connection pool using SAP JCo native pooling
- Reads configuration from environment variables via Spring Boot
- Pool capacity: 5-10 concurrent connections

**RfcAdapter** (`src/main/java/.../service/RfcAdapter.java`):
- Converts HTTP-style requests to RFC calls
- Calls `SADT_REST_RFC_ENDPOINT` function module
- Handles request/response transformation
- **Stateful Context Management**: Provides `beginStatefulContext()` and `endStatefulContext()` for workflows that require session persistence (e.g., LOCK → MODIFY → UNLOCK)

**StatefulModificationService** (`src/main/java/.../service/StatefulModificationService.java`):
- Centralized service for all modification workflows requiring stateful connections
- Manages LOCK/UNLOCK operations with automatic context handling
- Parses ADT XML responses (LOCK_HANDLE, transport info)
- Provides `executeStatefulWorkflow()` wrapper for automatic begin/end context
- **Why needed**: SAP locks (ENQUEUE) must persist in the same session throughout the workflow. Stateless connections lose locks between calls.

**Services** (`src/main/java/.../service/`):
- Business logic for ABAP operations
- Use `RfcAdapter` to call ADT REST APIs
- Parse XML/JSON responses
- Provide clean interfaces for MCP tools
- **Modification operations** delegate to `StatefulModificationService` for centralized lock management

**Tools** (`src/main/java/.../tool/`):
- MCP tool definitions using Spring AI MCP annotations
- Registered automatically via component scan
- Return JSON-formatted responses

---

## Stateful Connections for Lock Management

### Problem Statement

SAP ABAP modifications require a **LOCK → MODIFY → UNLOCK** workflow to prevent concurrent modifications. However, **stateless connections** (default in JCo connection pooling) cause locks to be lost between calls:

```
Stateless Problem:
LOCK (connection A)  → Acquires lock in SAP session A
MODIFY (connection B) → SAP session B doesn't have the lock ❌
Result: Modification fails or bypasses lock
```

### Solution: JCoContext for Stateful Sessions

SAP JCo provides `JCoContext` to maintain the **same session** across multiple RFC calls:

```
Stateful Solution:
JCoContext.begin()
  ├─→ LOCK (session A)   → Acquires lock
  ├─→ MODIFY (session A)  → Same session, lock persists ✅
  └─→ UNLOCK (session A)  → Release lock
JCoContext.end()
```

### Implementation Architecture

**Layer 1: RfcAdapter** - Low-level stateful context management
```java
// Thread-safe context tracking
private static final ThreadLocal<Boolean> statefulContextActive;

public void beginStatefulContext() throws JCoException {
    JCoContext.begin(destination);  // Start stateful session
    statefulContextActive.set(true);
}

public void endStatefulContext() throws JCoException {
    try {
        JCoContext.end(destination);  // End stateful session
    } finally {
        statefulContextActive.set(false);  // Always cleanup
    }
}
```

**Layer 2: StatefulModificationService** - High-level workflow orchestration
```java
public <T> T executeStatefulWorkflow(String objectName, StatefulWorkflow<T> workflow) {
    rfcAdapter.beginStatefulContext();  // Start stateful session
    try {
        // All operations here use SAME SAP session
        LockResult lock = lockObject(uri);
        // ... modifications ...
        unlockObject(uri, lock.lockHandle());
        return result;
    } finally {
        rfcAdapter.endStatefulContext();  // Always end session
    }
}
```

**Layer 3: Services** - Business logic
```java
public ProgramModifyResult modifyFunctionModuleSource(...) {
    return statefulModificationService.executeStatefulWorkflow(
        functionModuleName,
        () -> {
            // LOCK → MODIFY → UNLOCK all in same session
            LockResult lock = statefulModificationService.lockObject(fmUri);
            try {
                setObjectSource(..., lock.lockHandle(), ...);
                return result;
            } finally {
                statefulModificationService.unlockObject(fmUri, lock.lockHandle());
            }
        }
    );
}
```

### When to Use Stateful vs Stateless

| Operation Type | Connection Mode | Reason |
|----------------|-----------------|--------|
| **Read operations** (get_class_source, search_objects) | Stateless | No locks needed, better concurrency |
| **Create operations** (create_class, create_function_module) | Stateless | No locks needed (new objects) |
| **Modify operations** (modify_class, modifyFunctionModuleSource) | **Stateful** | Requires LOCK persistence |
| **Transport operations** (list_user_transports, get_transport_objects) | Stateless | Read-only queries |

### Benefits

✅ **Lock Persistence**: Locks maintained throughout workflow
✅ **Thread Safety**: ThreadLocal isolates contexts per thread
✅ **Centralized Logic**: All lock/unlock code in one service
✅ **Automatic Cleanup**: Finally blocks prevent memory leaks
✅ **No Code Duplication**: Services delegate to StatefulModificationService

### Critical Implementation Rules

1. **ALWAYS use try-finally** for `endStatefulContext()` - prevents memory leaks
2. **NO nested contexts** - JCo doesn't support, validated with flag
3. **MINIMIZE time in stateful context** - reserves pool connection
4. **ThreadLocal cleanup** - ALWAYS set false in finally block
5. **Only for modifications** - Read operations stay stateless

### References

- **Investigation**: `docs/research/jco_stateful_connections_analysis.md`
- **Architecture Design**: `docs/requirements/mcp/workflow_based/pr_centralized_stateful_architecture.md`
- **Implementation**: `docs/implementation/stateful_modification_implementation_complete.md`
- **JCo Examples**: `resources/jco/examples/.../StatefulCalls.java`, `StatefulJob.java`

---

## Environment Configuration

**Required Environment Variables** (set in `.mcp.json` or `.env`):

```bash
# SAP Connection (Required)
SAP_ASHOST=your.sap.server.com    # SAP application server
SAP_SYSNR=00                      # System number
SAP_CLIENT=100                    # Client
SAP_USER=username                 # SAP user
SAP_PASSWD=password               # Password

# Optional
SAP_LANG=EN                       # Language (default: EN)
SAP_ROUTER=/H/router/S/port       # SAP router if needed

# JCo Pooling
SAP_POOL_CAPACITY=5               # Pool size (default: 5)
SAP_PEAK_LIMIT=10                 # Peak limit (default: 10)

# Java (platform-specific)
# Windows:
JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot
# macOS:
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
# Linux:
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

---

## Important Development Notes

### ⚠️ CRITICAL: ABAP Function Module Signatures

**REGLA FUNDAMENTAL**: Las firmas de Function Modules NUNCA deben incluir comentarios.

**❌ NUNCA HACER ESTO**:
```abap
FUNCTION ZCX_GETDDICSOURCE.
*"----------------------------------------------------------------------
*"*"Local Interface:
*"  IMPORTING
*"     VALUE(OBJECT_NAME) TYPE  TABNAME
*"----------------------------------------------------------------------
```

**✅ SIEMPRE HACER ESTO**:
```abap
FUNCTION ZCX_GETDDICSOURCE
  IMPORTING
    VALUE(OBJECT_NAME) TYPE TABNAME
  EXPORTING
    VALUE(OBJECT_TYPE) TYPE CHAR10
    VALUE(FIELDS_JSON) TYPE STRING
  EXCEPTIONS
    OBJECT_NOT_FOUND.
```

**Por qué**: SAP ADT API rechaza firmas con comentarios (HTTP 400: "Parameter comment blocks are not allowed")

**Documentación completa**: `docs/development_rules/abap_function_module_rules.md`

**Formato correcto**:
- Sin comentarios `*"` en sección de parámetros
- Sin bloques decorativos `*"----`
- Solo keywords (IMPORTING, EXPORTING, EXCEPTIONS) y parámetros
- Punto final (`.`) después de firma
- Indentación de 2-4 espacios

**Ejemplo completo**:
```abap
FUNCTION Z_MI_FUNCION
  IMPORTING
    VALUE(IV_PARAM1) TYPE STRING
    VALUE(IV_PARAM2) TYPE MATNR
  EXPORTING
    VALUE(EV_RESULT) TYPE CHAR10
  CHANGING
    VALUE(CV_STATUS) TYPE CHAR1
  TABLES
    ET_DATA STRUCTURE SFLIGHT
  EXCEPTIONS
    INPUT_INVALID
    NO_DATA_FOUND
    SYSTEM_ERROR.

  " Implementación aquí

ENDFUNCTION.
```

### Multi-Platform Support

This project supports **Windows, macOS, and Linux**:

**Platform-Specific Files:**
- **Windows**: `sapjco3.dll` (lib/), `start-mcp.bat`, `start-mcp.ps1`, `mvn.cmd`
- **macOS**: `libsapjco3.dylib` (lib/), `start-mcp.sh`, `mvn`
- **Linux**: `libsapjco3.so` (lib/), `start-mcp.sh`, `mvn`

**Cross-Platform Compatibility:**
- ✅ Maven automatically sets `java.library.path` via `pom.xml` configuration
- ✅ Startup scripts handle platform-specific library detection
- ✅ Claude Desktop config uses platform-specific paths and commands
- ✅ Java code is 100% portable (no platform-specific code)

### SAP JCo Library Installation

SAP JCo libraries **cannot be redistributed** (SAP license). Each developer must:

1. **Download from SAP Support Portal** (requires S-user):
   - Windows: `SAPJCO3_NTAMD64_<version>.ZIP`
   - macOS Intel: `sapjco3-darwinintel64-<version>.tgz`
   - macOS ARM: `sapjco3-darwinarm64-<version>.tgz`
   - Linux x64: `sapjco3-linuxx86_64-<version>.tgz`
   - Linux ARM: `sapjco3-linuxaarch64-<version>.tgz`

2. **Place files in `lib/` directory**:
   ```
   lib/
   ├── sapjco3.jar                    # JAR (all platforms)
   └── sapjco3.dll                    # Windows
       libsapjco3.dylib               # macOS
       libsapjco3.so                  # Linux
   ```

3. **See `lib/README.md`** for detailed platform-specific installation instructions

### XML Parsing

ADT REST API responses are XML. Java services use:
- `javax.xml.parsers.DocumentBuilder` for parsing
- XPath for querying elements
- Namespace-aware parsing required

### Logging

**Critical**: STDOUT must be clean for MCP STDIO transport
- All logging goes to files only (configured in `application.yml`)
- Console logging completely disabled
- Log files: `logs/java/sap-mcp-server.log`

### Testing Strategy

- **Unit tests**: Test services without SAP connection (use mocks)
- **Integration tests**: Require live SAP connection via env vars
- Use Spring Boot `@SpringBootTest` for integration tests
- Coverage target: 80%+

---

## Available MCP Tools

### Current (1 tool)

- ✅ `get_class_source` - Get ABAP class source code

### Planned (Phase 1 - 16 tools)

**Repository & Source (9 tools)**:
- `get_class_structure`, `get_object_source`, `get_class_includes`,
  `get_class_components`, `get_object_structure`, `search_objects`,
  `get_program_source`, `get_include_source`

**Data Dictionary (4 tools)**:
- `get_ddic_element`, `ddic_repository_access`, `get_annotation_definitions`,
  `package_search_help`

**Transport Management (3 tools)**:
- `list_user_transports`, `get_transport_objects`, `transport_info`

**See Full List**: `python-legacy/PYTHON_LEGACY.md` (59 tools documented)

---

## Troubleshooting

### Error: "JCo library not found"

```
java.lang.UnsatisfiedLinkError: no sapjco3 in java.library.path
```

**Solution** (platform-specific):

**Windows**:
```cmd
REM Verify library exists
dir lib\

REM Run with library path
java -Djava.library.path=.\lib -jar target\sap-mcp-server-0.1.0-POC.jar

REM Or use startup script
start-mcp.bat
```

**macOS/Linux**:
```bash
# Verify library exists
ls -l lib/

# Run with library path
java -Djava.library.path=./lib -jar target/sap-mcp-server-0.1.0-POC.jar

# Or use startup script
./start-mcp.sh
```

### Error: "Connection timeout"

```
JCoException: Connect to SAP gateway failed
```

**Solution**:
1. Verify VPN active
2. Verify `SAP_ROUTER` in configuration
3. Test connectivity: `ping <SAP_ASHOST>`

### Error: "SADT_REST_RFC_ENDPOINT not found"

**Solution**:
- ADT not installed on SAP system
- User lacks ADT authorization
- Contact SAP Basis team

### Maven build fails

```bash
# Clean and rebuild
mvn clean install -U

# Skip tests if needed
mvn clean package -DskipTests
```

---

## References

- **Migration Plan**: `docs/requirements/mcp/migration_plan.md`
- **Java Documentation**: `README_JAVA.md`
- **Python Legacy**: `python-legacy/PYTHON_LEGACY.md`
- **Spring AI MCP SDK**: https://spring.io/blog/2025/02/14/mcp-java-sdk-released-2
- **SAP JCo Documentation**: https://support.sap.com/en/product/connectors/jco.html
- **Model Context Protocol**: https://modelcontextprotocol.io

---

**Last Updated**: 2025-11-08 (Phase 0 Complete)
**Project Status**: Java Migration In Progress (1/59 tools)
**Contact**: Crystal Development Team
