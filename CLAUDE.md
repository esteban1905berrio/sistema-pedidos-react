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

---

### ⚠️ REGLA: Tests Manuales/Debug (CommandLineRunner)

**OBLIGATORIO**: Para tests manuales o de debug que requieren ejecución aislada, usar el patrón `CommandLineRunner` en lugar de JUnit.

**Razón**: JUnit con `mvn test -Dtest=TestClass#methodName` frecuentemente ejecuta todos los métodos en lugar del específico. El patrón `CommandLineRunner` garantiza ejecución aislada.

**Ubicación**: `src/test/java/com/crystal/mcp/sapserver/manual/`

**Patrón CommandLineRunner (OBLIGATORIO para debug):**

```java
@Profile("!test")  // Excluir del perfil test
@SpringBootApplication
@ComponentScan(basePackages = "com.crystal.mcp.sapserver")
public class ManualMyServiceTest implements CommandLineRunner {

    @Autowired
    private MyService myService;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManualMyServiceTest.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║        Manual Test: MyService        ║");
        System.out.println("╚══════════════════════════════════════╝");

        try {
            // Test específico aquí
            testMyMethod();

            System.out.println("✅ TEST COMPLETED");
        } catch (Exception e) {
            System.err.println("❌ TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void testMyMethod() {
        // Implementación del test
    }
}
```

**Ejecución:**
```bash
mvn spring-boot:run -Dspring-boot.run.mainClass=com.crystal.mcp.sapserver.manual.ManualMyServiceTest
```

**Cuándo usar cada patrón:**

| Escenario | Patrón | Comando |
|-----------|--------|---------|
| Tests automatizados CI/CD | JUnit `@SpringBootTest` | `mvn test` |
| Debug de un método específico | CommandLineRunner | `mvn spring-boot:run -Dspring-boot.run.mainClass=...` |
| Validación manual SAP | CommandLineRunner | `mvn spring-boot:run -Dspring-boot.run.mainClass=...` |
| Tests de regresión | JUnit `@SpringBootTest` | `mvn test -Dtest=TestClass` |

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

### ⚠️ REGLA: MCP Resources - Templates vs Static

**IMPORTANTE**: Este servidor implementa **Resource Templates** (URIs con placeholders como `{name}`, `{id}`) en lugar de Static Resources.

**Implicación para Claude Code:**
- `ListMcpResourcesTool` solo lista **Static Resources**
- Los **Resource Templates NO aparecen** en la lista pero **funcionan correctamente**
- Usar el recurso estático `sap://server/info` para ver todos los templates disponibles

**Cómo usar los Resources:**

```bash
# 1. Obtener lista de templates disponibles (recurso estático)
ReadMcpResourceTool server=giralmcp uri=sap://server/info

# 2. Leer un recurso específico reemplazando placeholders
ReadMcpResourceTool server=giralmcp uri=sap://class/CL_ABAP_CHAR_UTILITIES/methods
ReadMcpResourceTool server=giralmcp uri=sap://transport/DEVK900123/info
ReadMcpResourceTool server=giralmcp uri=sap://package/ZCX/objects
ReadMcpResourceTool server=giralmcp uri=sap://table/MARA/fields
```

**Resource Templates disponibles:**

| URI Template | Descripción |
|--------------|-------------|
| `sap://server/info` | Info del servidor (ESTÁTICO) |
| `sap://class/{name}/definition` | Código fuente definición |
| `sap://class/{name}/implementation` | Código fuente implementación |
| `sap://class/{name}/methods` | Lista de métodos (JSON) |
| `sap://class/{name}/attributes` | Lista de atributos (JSON) |
| `sap://transport/{id}/info` | Metadata de transporte |
| `sap://transport/{id}/objects` | Objetos en transporte |
| `sap://package/{name}/objects` | Objetos en paquete |
| `sap://package/{name}/hierarchy` | Jerarquía de paquetes |
| `sap://table/{name}/fields` | Campos de tabla DDIC |

---

### ⚠️ REGLA: Sistema SAP por Defecto

**OBLIGATORIO**: Cuando se trabaje con herramientas MCP de SAP, usar **gdcmcp** como sistema por defecto.

- **Sistema por defecto**: `mcp__gdcmcp__*` (GDC - Sistema de desarrollo principal)
- **Solo usar giralmcp** cuando el usuario lo especifique explícitamente
- Aplica a todas las operaciones: lectura, búsqueda, creación, modificación

**Ejemplo**:
```
✅ CORRECTO: mcp__gdcmcp__get_class_source
✅ CORRECTO: mcp__gdcmcp__search_objects
❌ INCORRECTO: mcp__giralmcp__get_class_source (solo si usuario lo pide)
```

---

### ⚠️ REGLA: Function Group por Defecto para FMs Custom

**OBLIGATORIO**: Los Function Modules custom del MCP Server deben crearse en el Function Group **ZGFCX_1**.

- **Function Group por defecto**: `ZGFCX_1`
- **Ubicación en SAP**: Package `ZCX` o subpaquete correspondiente
- **Convención de nombres FM**: `ZCX_*` o `Z_CX_*`

**FMs existentes en ZGFCX_1**:
- `ZCX_GETDDICSOURCE` - Obtener estructura DDIC
- `ZCX_CREATE_TRANSPORT_COPY` - Crear copia de transporte
- `Z_CX_GET_TRANSPORT_OBJECTS` - Obtener objetos de transporte
- `Z_CX_GET_OBJECT_IN_OPEN_OT` - Buscar objeto en OTs abiertas
- `Z_CX_GET_TRANSPORT_INFO` - Metadata de transportes
- `Z_CX_GET_PACKAGE_HIERARCHY` - Jerarquía de paquetes
- `ZCX_CREATE_TRANSPORT_REQUEST` - Crear nuevo transporte
- `ZCX_GET_DUMP_DETAIL` - Detalles de dump ST22
- `ZCX_GET_TRANSPORT_LOG` - Log de transporte
- `Z_CX_SEARCH_TRANSPORTS` - Búsqueda de transportes

---

### ⚠️ REGLA: Tests Manuales (Usuario Primero)

**OBLIGATORIO**: NO ejecutar tests manuales automáticamente. El usuario probará primero manualmente.

- **NUNCA** ejecutar `mvn spring-boot:run -Dspring-boot.run.mainClass=...` automáticamente
- **SIEMPRE** proporcionar el comando para compilar/recompilar el proyecto
- **SOLO** ejecutar tests cuando el usuario lo solicite explícitamente

**Comando para compilar**:
```bash
mvn clean compile
```

**Comando para compilar con tests**:
```bash
mvn clean test-compile
```

---

### ⚠️⚠️⚠️ CRITICAL: ABAP Function Module Signatures (MANDATORY)

**REGLAS FUNDAMENTALES** - Violar estas reglas causa errores de SAP ADT API:

#### REGLA 1: SIEMPRE incluir la firma completa

**NUNCA** omitir la firma del Function Module. **SIEMPRE** debe incluirse inmediatamente después de `FUNCTION <nombre>`:

**❌ NUNCA HACER ESTO** (firma omitida):
```abap
FUNCTION Z_MI_FUNCION.
  " You can use the template 'functionModuleParameter' to add here the signature!

  DATA: lv_result TYPE string.
  " ... código ...
ENDFUNCTION.
```

**✅ SIEMPRE HACER ESTO** (firma completa):
```abap
FUNCTION Z_MI_FUNCION
  IMPORTING
    VALUE(IV_PARAM1) TYPE STRING
  EXPORTING
    VALUE(EV_RESULT) TYPE STRING.

  DATA: lv_result TYPE string.
  " ... código ...
ENDFUNCTION.
```

#### REGLA 2: NUNCA incluir comentarios en la firma

**❌ NUNCA HACER ESTO** (comentarios en firma):
```abap
FUNCTION ZCX_GETDDICSOURCE.
*"----------------------------------------------------------------------
*"*"Local Interface:
*"  IMPORTING
*"     VALUE(OBJECT_NAME) TYPE  TABNAME
*"----------------------------------------------------------------------
```

**✅ SIEMPRE HACER ESTO** (sin comentarios):
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

#### Por qué estas reglas son críticas

1. **Firma omitida**: Si no se incluye la firma, SAP ADT API inserta un placeholder vacío que invalida los parámetros del FM
2. **Comentarios en firma**: SAP ADT API rechaza con HTTP 400: "Parameter comment blocks are not allowed"

#### Checklist OBLIGATORIO antes de modificar un FM

- [ ] ¿La firma incluye TODOS los parámetros IMPORTING?
- [ ] ¿La firma incluye TODOS los parámetros EXPORTING?
- [ ] ¿La firma incluye TODOS los parámetros CHANGING (si aplica)?
- [ ] ¿La firma incluye TODOS los parámetros TABLES (si aplica)?
- [ ] ¿La firma incluye TODAS las EXCEPTIONS?
- [ ] ¿La firma NO tiene comentarios `*"` ni bloques `*"----`?
- [ ] ¿La firma termina con punto `.` después del último parámetro o excepción?

#### Formato correcto de firma

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

**Elementos de formato**:
- Sin comentarios `*"` en sección de parámetros
- Sin bloques decorativos `*"----`
- Solo keywords (IMPORTING, EXPORTING, CHANGING, TABLES, EXCEPTIONS) y parámetros
- Punto final (`.`) después del último elemento de la firma
- Indentación de 2 espacios para keywords, 4 espacios para parámetros
- Línea en blanco entre firma y código de implementación

**Documentación completa**: `docs/development_rules/abap_function_module_rules.md`

---

### ⚠️ REGLA: Manejo de JSON en ABAP con /ui2/cl_json

**OBLIGATORIO**: Para serializar/deserializar JSON en ABAP, **SIEMPRE** usar la clase estándar `/ui2/cl_json`.

**NUNCA** hacer parsing manual de strings JSON. La clase `/ui2/cl_json` es la forma estándar y robusta de manejar JSON en ABAP.

#### Deserialización (JSON String → Internal Table)

```abap
TYPES: BEGIN OF ty_object,
         pgmid    TYPE pgmid,
         object   TYPE trobjtype,
         obj_name TYPE sobj_name,
       END OF ty_object,
       tt_objects TYPE STANDARD TABLE OF ty_object WITH DEFAULT KEY.

DATA: lt_objects TYPE tt_objects.

" Deserializar JSON a tabla interna
/ui2/cl_json=>deserialize(
  EXPORTING
    json        = iv_json_string
    pretty_name = /ui2/cl_json=>pretty_mode-camel_case
  CHANGING
    data        = lt_objects ).
```

#### Serialización (Internal Table → JSON String)

```abap
DATA: lt_tasks TYPE STANDARD TABLE OF trkorr WITH DEFAULT KEY,
      lv_json  TYPE string.

" Serializar tabla interna a JSON
lv_json = /ui2/cl_json=>serialize(
  data        = lt_tasks
  pretty_name = /ui2/cl_json=>pretty_mode-low_case ).
```

#### Opciones de pretty_name

| Modo | Uso | Ejemplo |
|------|-----|---------|
| `pretty_mode-camel_case` | Input JSON con camelCase | `{"objName": "ZTEST"}` |
| `pretty_mode-low_case` | Output JSON en minúsculas | `{"obj_name": "ZTEST"}` |
| `pretty_mode-none` | Sin transformación | Mantiene nombres ABAP |

#### Checklist OBLIGATORIO

- [ ] ¿Usas `/ui2/cl_json=>deserialize()` para parsear JSON de entrada?
- [ ] ¿Usas `/ui2/cl_json=>serialize()` para generar JSON de salida?
- [ ] ¿Definiste `TYPES` para las estructuras de datos?
- [ ] ¿Elegiste el `pretty_name` apropiado según el formato esperado?

**❌ NUNCA hacer esto** (parsing manual):
```abap
" MAL - Parsing manual propenso a errores
FIND REGEX '"obj_name"\s*:\s*"([^"]+)"' IN lv_json SUBMATCHES lv_name.
```

**✅ SIEMPRE hacer esto** (usar /ui2/cl_json):
```abap
" BIEN - Clase estándar SAP
/ui2/cl_json=>deserialize( ... ).
```

---

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

### Error: "Type conflict during a function module call"

```
JCo error: Type conflict during a function module call
```

**Causa**: Este error **NO** es del MCP server Java. El problema está en el Function Module ABAP en SAP.

**Explicación**: Ocurre cuando un FM custom (ej: `ZCX_GET_TRANSPORT_LOGS`) llama internamente a otro FM estándar de SAP con un tipo de parámetro incorrecto.

**Ejemplos comunes**:
- Pasar `STRING` donde se espera `CHAR`
- Usar estructura incorrecta para tablas
- Tipos IMPORTING/EXPORTING que no coinciden con la firma del FM llamado

**Solución**:
1. Revisar el FM custom en SAP via SE37/SE80
2. Verificar las llamadas internas a otros FMs (ej: `STRF_READ_COFILE`, `TRINT_READ_LOG`)
3. Comparar tipos de parámetros con la firma del FM llamado
4. Corregir el tipo en el FM custom y activar

**Nota**: El MCP server solo reporta el error de JCo. La corrección debe hacerse en ABAP.

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
