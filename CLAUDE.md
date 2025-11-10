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
├── pom.xml                       # ⭐ Maven configuration (root)
├── README_JAVA.md                # Java project documentation (detailed)
├── README.md                     # Main README (to be updated)
├── CLAUDE.md                     # This file
└── .mcp.json                     # MCP server configuration
```

### File Naming Conventions

**Java Services**: `<Category>Service.java` (e.g., `ClassService.java`, `TransportService.java`)
**Java Tools**: `<Category>Tools.java` (e.g., `ClassTools.java`, `TransportTools.java`)
**Java Tests**: `<Category>ServiceTest.java` (e.g., `ClassServiceTest.java`)
**Logs**: Store in `logs/java/` directory

---

## Core Development Commands

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
java -Djava.library.path=./lib -jar target/sap-mcp-server-0.1.0-POC.jar  # Run JAR

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

Edit `.mcp.json` in project root:

```json
{
  "mcpServers": {
    "giralmcp": {
      "command": "mvn",
      "args": ["spring-boot:run", "-f", "/absolute/path/pom.xml"],
      "env": {
        "JAVA_HOME": "/path/to/jdk-21",
        "SAP_ASHOST": "sap.server.com",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "100",
        "SAP_USER": "usuario",
        "SAP_PASSWD": "contraseña",
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

**Services** (`src/main/java/.../service/`):
- Business logic for ABAP operations
- Use `RfcAdapter` to call ADT REST APIs
- Parse XML/JSON responses
- Provide clean interfaces for MCP tools

**Tools** (`src/main/java/.../tool/`):
- MCP tool definitions using Spring AI MCP annotations
- Registered automatically via component scan
- Return JSON-formatted responses

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

# Java
JAVA_HOME=/path/to/jdk-21
```

---

## Important Development Notes

### SAP JCo Library Installation

SAP JCo libraries **cannot be redistributed** (SAP license). Each developer must:

1. Download from SAP Support Portal (requires S-user)
2. Place files in `lib/` directory:
   ```
   lib/
   ├── sapjco3.jar                    # JAR (all platforms)
   └── libsapjco3.dylib               # Native (macOS)
       libsapjco3.so                  # Native (Linux)
       sapjco3.dll                    # Native (Windows)
   ```
3. See `lib/README.md` for detailed instructions

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

**Solution**:
```bash
# Verify library exists
ls -l lib/

# Run with library path
java -Djava.library.path=./lib -jar target/sap-mcp-server-0.1.0-POC.jar
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
