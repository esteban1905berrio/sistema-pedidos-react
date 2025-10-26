# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is an MCP (Model Context Protocol) server that enables LLM tools like Claude Code to interact with SAP ABAP systems via the SAP NetWeaver RFC SDK.

**Project Purpose**: Implement a comprehensive MCP server for SAP ABAP that combines ADT (ABAP Development Tools) interface with RFC connectivity. The goal is to replicate and expand the functionalities available in:
- **mcp-abap-abap-adt-api**: TypeScript MCP server using HTTP/ADT APIs
- **abap-adt-py**: Python library for ADT REST API interactions

This implementation provides the same capabilities but through RFC instead of HTTP, allowing direct SAP system access without requiring HTTP-based ADT endpoints.

## Development Lifecycle (SDLC)

This section defines the formal software development lifecycle to be followed for all feature implementations and enhancements.

### Phase 1: Requirements Analysis
- Use `docs/requirements/pr_*.md` to analyze and refine User Stories with AI agent (requirements-analyst-ai)
- Present analysis before implementation
- Asks questions to broaden understanding of the requirement and avoid overlooking important points. Questions from the functional and technical perspective of the requirement.
- Wait for user approval before proceeding

### Phase 2: Design & Framework Research
- **Mandatory framework research**: Search for existing solutions before custom implementation
  1. **Use Context7 MCP** for official library documentation (`mcp__context7__resolve-library-id` → `mcp__context7__get-library-docs`)
  2. **Use WebSearch** for framework comparisons, blog posts, and community discussions (2024-2025)
  3. **Use WebFetch** for specific documentation pages when needed
- Create comparison document evaluating frameworks vs custom approaches
- **After research concludes, write research findings in `docs/research/`**
- Validate reusable functions in existing codebase (avoid duplication)
- Present proposal with decision matrix showing framework options
- Present implementation plan broken down by phases with clear milestones
- Wait for user approval before proceeding
- Write/update PR document with approved plan, phases, and phase status in `docs/requirements/pr_*.md`

### Phase 3: Implementation
- Follow Solution Simplification Rules (minimize file creation, remove unused code)
- Implement only approved design (no temporary solutions without authorization)
- Use TodoWrite tool for task tracking and progress visibility
- When concluding each phase, update PR document with files modified (without extensive source code details)

### Phase 4: Testing

**Test Organization:**
- **All tests MUST be created in `app/tests/` directory**
- Test naming convention: `test_<category>_<functionality>.py`
- Integration tests that require SAP connection: Use prefix `test_fase*` or `test_<category>_category.py`
- Debug/exploratory tests: Use prefix `test_debug_*.py`

**Test Execution:**
```bash
# Run all tests
.venv/bin/python -m pytest app/tests/ -v

# Run specific category tests
.venv/bin/python -m pytest app/tests/test_cds_category.py -v
.venv/bin/python -m pytest app/tests/test_enhancement_category.py -v

# Run with coverage
.venv/bin/python -m pytest app/tests/ --cov=app --cov-report=html
```

**Coverage Requirements:**
- 80%+ coverage for business logic
- All new services require corresponding test files
- Mock external services when appropriate (never call real SAP APIs in unit tests)

**Test Setup Pattern (MANDATORY):**

When creating integration tests that need SAP connection, **ALWAYS** follow this exact pattern:

```python
import os
from dotenv import load_dotenv
from app.core.config import SAPConfig
from app.core.rfc_connection import RfcConnectionPool
from app.services.class_service import ClassService  # or any service

load_dotenv()

def test_something():
    # Step 1: Create SAPConfig from environment
    sap_config = SAPConfig(
        ashost=os.getenv("SAP_ASHOST", ""),
        sysnr=os.getenv("SAP_SYSNR", ""),
        client=os.getenv("SAP_CLIENT", ""),
        user=os.getenv("SAP_USER", ""),
        passwd=os.getenv("SAP_PASSWD", ""),
        lang=os.getenv("SAP_LANG", "EN"),
        saprouter=os.getenv("SAP_ROUTER"),
    )

    # Step 2: Create RfcConnectionPool with SAPConfig
    connection_pool = RfcConnectionPool(sap_config, pool_size=1)

    # Step 3: Create service with connection pool
    service = ClassService(connection_pool)

    # Step 4: Use the service
    result = service.get_class_source("ZCLCXR1002_UTIL")
```

**CRITICAL**:
- ❌ **NEVER** try to create `RfcConnectionPool` directly with connection params
- ❌ **NEVER** create services without a `RfcConnectionPool` instance
- ✅ **ALWAYS** use the pattern: `SAPConfig` → `RfcConnectionPool` → `Service`

**Logging:**
- **All log files MUST be stored in `logs/` directory** (ignored by git)
- Use Python logging module with appropriate levels
- Debug logs for development troubleshooting
- Info logs for operation tracking
- Never commit log files to repository

### Phase 5: Documentation
- Use `claude-md-updater` agent for automatic documentation
- Update architecture docs in `docs/architecture/`
- Document patterns, technologies, and architectural decisions

### Phase 6: Deployment
- Use GCP deployment scripts (`deploy-gcp.sh` for development or `deploy-gcp-production.sh` for production)
- Health verification via `/health` endpoint
- Monitor logs and performance metrics

## Automatic Documentation Pattern

**Use the claude-md-updater agent** to automatically document completed phases/modules:

### Template Command:
```
"Usa el claude-md-updater agent para documentar [FASE_NOMBRE] que acabamos de [ACCION], incluyendo [COMPONENTES_CLAVE]"
```

### Documentation References:
- **Current Architecture**: `docs/architecture/current_architecture.md`
- **MCP Migration Strategy**: `docs/architecture/mcp-architecture-strategy.md`
- **Localization System**: `docs/architecture/localization-system.md`
- **Terms & Conditions Flow**: `docs/architecture/terms-conditions.md`
- **LangGraph Guidelines**: `docs/architecture/langgrapf_rules.md`

## Technology Stack

**Version Policy**: Use LATEST STABLE versions of all libraries unless technical incompatibilities exist. When incompatibilities are found, document the specific constraint reason in requirements.txt comments.

### Current Status

**Project Metrics:**
- ✅ **59 MCP Tools** implemented across 10 categories
- ✅ **17 Services** created
- ✅ **Unified Architecture**: All services use `RfcAdapter`
- ✅ **67% Testing Coverage** (40/59 tools fully tested)

**Tool Categories (59 total):**
1. **Repository & Source** (9 tools) - Class, program, object source code operations
2. **Data Dictionary** (4 tools) - DDIC elements, annotations, package search
3. **Query & Preview** (2 tools) - Table contents, SQL queries
4. **Transport Management** (14 tools) - Complete transport lifecycle
5. **Object Modification** (3 tools) - Lock, unlock, modify source
6. **Activation** (3 tools) - Activate objects, batch activation, inactive list
7. **Code Quality** (4 tools) - Syntax check, pretty print, settings
8. **Lifecycle** (4 tools) - Create, delete, validate, unit tests
9. **Where-Used Analysis** (2 tools) - Usage snippets, dependencies
10. **CDS Views** (4 tools) - CDS metadata, source, search, properties
11. **RAP Objects** (8 tools) - Service bindings, definitions, OData, DDLX, BDEF, explorer
12. **Enhancements** (3 tools) - Search, metadata, source (ENHO types)

**See [README.md](README.md) for complete tool list and usage examples.**

## Recent Improvements (2025-10-21)

### ✅ Critical Stability Fixes Implemented

The server has undergone major stability improvements to fix connection management issues:

- **Connection Lifecycle**: Fixed critical bug where connections were never released
- **Health Validation**: Added automatic connection health checks before reuse
- **Retry Logic**: Implemented exponential backoff retry (3 attempts) for network errors
- **Error Handling**: Actionable, educational error messages for LLMs
- **Circuit Breaker**: Protection against cascade failures

**Result**: Server stability improved from 2/10 → 9/10

**See**: [docs/STABILITY_IMPROVEMENTS.md](docs/STABILITY_IMPROVEMENTS.md) for complete details.

**Key Files Changed**:
- `app/core/rfc_connection.py` - Connection health checks
- `app/core/rfc_adapter.py` - Retry logic
- `app/core/retry_handler.py` - NEW retry/circuit breaker module
- `app/core/error_handler.py` - NEW error formatting
- `app/services/base_service.py` - NEW base class for all services
- `app/mcp/server.py` - Fixed connection lifecycle

---

## Project Organization

### Directory Structure

```
brootpersonalagent/
├── app/                           # Main application code
│   ├── core/                      # Core infrastructure (RFC, adapter, config)
│   ├── services/                  # 17 business logic services
│   ├── mcp/                       # MCP server and tool registration
│   │   ├── server.py              # Main MCP server
│   │   └── tools/                 # 59 MCP tool definitions
│   └── tests/                     # ⚠️ ALL TESTS GO HERE
│       ├── test_fase*.py          # Phase validation tests
│       ├── test_*_category.py     # Category tests (CDS, RAP, Enhancement)
│       └── test_debug_*.py        # Debug/exploratory tests
│
├── docs/                          # Documentation
│   ├── requirements/              # PR and requirement documents
│   └── architecture/              # Architecture documentation
│
├── logs/                          # ⚠️ ALL LOG FILES GO HERE (git ignored)
│   ├── dev_rfc.log
│   ├── test_*.log
│   └── debug_*.log
│
├── PyRFC/                         # SAP RFC SDK bindings (reference)
├── .env                           # Environment variables (git ignored)
├── .env.example                   # Example environment configuration
├── .gitignore                     # Git ignore rules
├── .mcp.json                      # MCP server configuration (git ignored)
├── CLAUDE.md                      # This file - Claude Code instructions
├── README.md                      # Project documentation
├── INSTALLATION.md                # Detailed installation guide
├── pyproject.toml                 # Python project configuration
├── uv.lock                        # Dependency lock file
└── setup.sh                       # Unified setup script
```

### File Naming Conventions

**Services:** `<category>_service.py` (e.g., `cds_service.py`, `transport_service.py`)
**Tools:** `<category>_tools.py` (e.g., `cds_tools.py`, `transport_tools.py`)
**Tests:** `test_<category>_<functionality>.py` (e.g., `test_cds_category.py`)
**Logs:** Store ALL logs in `logs/` directory (never in project root)

### What NOT to Commit

- ❌ Log files (*.log) - use `logs/` directory
- ❌ Environment files (.env, .mcp.json)
- ❌ IDE config (.vscode/, .idea/, *.code-workspace)
- ❌ Python artifacts (__pycache__/, *.pyc, .pytest_cache/)
- ❌ Virtual environments (.venv/, venv/)

## Core Development Commands

### Setup and Installation

**Automated Setup (Recommended)**:
```bash
# Run unified setup script (handles everything automatically)
./setup.sh
```

The script will:
- Detect your OS (macOS/Linux/Windows)
- Configure SAP RFC SDK environment variables
- Create virtual environment
- Install all dependencies (uv or pip)
- Compile PyRFC automatically
- Validate `.env` configuration
- Configure Claude Desktop
- Verify installation

**Manual Setup** (if needed):
```bash
# Create virtual environment
python3 -m venv .venv
source .venv/bin/activate

# Install dependencies (using uv)
uv sync

# OR install manually
pip install pydantic python-dotenv mcp pytest pytest-asyncio pytest-cov

# Compile and install PyRFC (requires SAP NetWeaver RFC SDK)
cd PyRFC
export SAPNWRFC_HOME=/Users/local/nwrfcsdk
export DYLD_LIBRARY_PATH=$SAPNWRFC_HOME/lib:$DYLD_LIBRARY_PATH
python3 -m pip install .
cd ..
```

### Running and Testing

**Note**: After running `./setup.sh`, the environment is configured automatically. The virtual environment already has access to the SAP RFC SDK libraries.

```bash
# Run MCP server directly (for testing)
.venv/bin/python -m app.main

# Run unit tests
.venv/bin/python -m pytest app/tests/ -v

# Run specific test files
.venv/bin/python -m pytest app/tests/test_config.py -v
.venv/bin/python -m pytest app/tests/test_rfc_adapter.py -v

# Run integration tests (requires SAP connection)
.venv/bin/python -m pytest app/tests/test_integration.py -v -s

# With coverage
.venv/bin/python -m pytest app/tests/ --cov=app --cov-report=html
```

### Code Quality

```bash
# Type checking
pyright app/

# Linting
ruff check app/

# Formatting
ruff format app/
```

## Architecture

### High-Level Structure

The system uses a three-layer architecture:

1. **Core Layer** (`app/core/`): RFC connection management and HTTP-to-RFC adapter
2. **Service Layer** (`app/services/`): Business logic for ABAP operations
3. **MCP Layer** (`app/mcp/`): MCP server and tool registration

### Key Architectural Patterns

**RFC Adapter Pattern**: The `RfcAdapter` class mimics HTTP request/response patterns used by `abap-adt-py` but executes requests via the SAP RFC function module `SADT_REST_RFC_ENDPOINT`. This allows the server to call ADT (ABAP Development Tools) REST APIs through RFC instead of HTTP.

**Connection Pooling**: Thread-safe connection pool (`RfcConnectionPool`) manages multiple RFC connections to improve performance and avoid connection overhead. Connections are reused across requests.

**Service-Based Design**: Each ABAP operation type has a dedicated service:
- `ClassService`: Class source code and structure retrieval
- `SearchService`: Object search with wildcard support
- `ProgramService`: Program and include source retrieval

### Data Flow

```
MCP Tool Call → Service Method → RfcAdapter → SADT_REST_RFC_ENDPOINT (RFC) → ADT API → ABAP System
```

The `RfcAdapter` converts HTTP-style requests (URI, method, headers, body) into RFC call structures that `SADT_REST_RFC_ENDPOINT` understands.

### Critical Components

**RfcAdapter** (`app/core/rfc_adapter.py`):
- Converts HTTP-style requests to RFC SADT_REST_RFC_ENDPOINT calls
- Handles request/response transformation
- Manages session statefulness for lock operations

**RfcConnectionPool** (`app/core/rfc_connection.py`):
- Thread-safe connection pooling with configurable pool size
- Context manager interface for connection acquisition/release
- Automatic connection creation up to pool limit

**Services** (`app/services/`):
- Use RfcAdapter to call ADT REST APIs
- Parse XML responses (using XML namespaces)
- Provide clean Python interfaces for MCP tools

## Environment Configuration

Required environment variables in `.env`:

```bash
# Required
SAP_ASHOST=your.sap.server.com    # SAP application server host
SAP_SYSNR=00                      # System number
SAP_CLIENT=100                    # Client number
SAP_USER=username                 # SAP username
SAP_PASSWD=password               # SAP password

# Optional
SAP_LANG=EN                       # Language (default: EN)
SAP_ROUTER=/H/router/S/port       # SAP router string if needed

# Test Configuration
TEST_CLASS_NAME=CL_ABAP_CHAR_UTILITIES
TEST_SEARCH_QUERY=CL_ABAP*
TEST_PROGRAM_NAME=SAPBC_START_PROGRAMS
```

## MCP Server Configuration

The `.mcp.json` file configures Claude Code to use this server:

```json
{
  "mcpServers": {
    "ABAP-ADT-RFC-Server": {
      "command": ".venv/bin/python",
      "args": ["-m", "app.main"],
      "env": {
        "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
        "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib"
      }
    }
  }
}
```

## Available MCP Tools

### Class Operations
- `get_class_source`: Get complete ABAP class source code
- `get_class_structure`: Get class metadata, methods, attributes
- `get_object_source`: Get source for any ABAP object by ADT URI

### Search Operations
- `search_objects`: Search ABAP objects by name pattern (supports wildcards)

### Program Operations
- `get_program_source`: Get ABAP program/report source code
- `get_include_source`: Get program include source code

### High-Level Modification Workflows (NEW - 2025-10-24)

**Complete ADT Modification Flow:** `LOCK → SYNTAX_CHECK → MODIFY → UNLOCK → ACTIVATE`

These workflows automate the entire modification process with built-in error handling, syntax validation, and automatic activation.

**Available Workflows:**
- `modify_function_module(fm_name, fg_name, new_source, transport?, auto_activate?, validate_syntax?)`
- `modify_class(class_name, new_source, include_type?, transport?, auto_activate?, validate_syntax?)`
- `modify_program(program_name, new_source, transport?, auto_activate?, validate_syntax?)`
- `modify_include(include_name, program_name, new_source, transport?, auto_activate?, validate_syntax?)`

**Key Features:**
- ✅ One tool call instead of 4-5 low-level operations
- ✅ Automatic syntax validation (prevents saving invalid code)
- ✅ Guaranteed lock release via try-finally blocks
- ✅ Detailed status reporting for each step
- ✅ Optional auto-activation control

**Example Usage:**
```python
result = modify_function_module(
    "ZTEST_FM",
    "ZTEST_FG",
    "FUNCTION ZTEST_FM.\n  rv_result = 'Hello'.\nENDFUNCTION.",
    transport="DEVK900123",
    auto_activate=True,
    validate_syntax=True
)

if result["success"]:
    print("✓ Modification completed successfully")
else:
    for msg in result["messages"]:
        print(f"Error: {msg['text']}")
```

**Architecture:** Three-tier hybrid design
- **Tier 1 (Infrastructure):** Low-level operations (lock, unlock, modify, activate)
- **Tier 2 (Workflows):** High-level orchestration with type-specific logic
- **Tier 3 (MCP Tools):** LLM-friendly interface with clear descriptions

**Documentation:**
- Full workflow documentation: `docs/architecture/modification-workflows.md`
- Skill documentation: `.claude/skills/abap-assistant/README.md`
- Implementation guide: `docs/requirements/pr_flow_object_create.md`

## Important Development Notes

### SAP NetWeaver RFC SDK Dependency

This project requires the SAP NetWeaver RFC SDK to be installed locally. The `PyRFC` subdirectory contains Python bindings that must be compiled against the SDK.

**Critical environment variables** must be set before running any code:
```bash
export SAPNWRFC_HOME=/Users/local/nwrfcsdk
export DYLD_LIBRARY_PATH=$SAPNWRFC_HOME/lib:$DYLD_LIBRARY_PATH
```

### XML Parsing

ADT REST API responses are XML. Services use `xml.etree.ElementTree` with predefined namespaces (`XML_NAMESPACES` in `class_service.py`) to parse responses.

### Statefulness

The RfcAdapter supports both stateless and stateful sessions via the `set_statefulness()` method. Stateful sessions are required for operations that use SAP locks (not currently implemented but the infrastructure is ready).

### Testing Strategy

- **Unit tests**: Test configuration, RFC adapter, and connection pooling without SAP connection
- **Integration tests**: Require live SAP connection configured via `.env`
- Use `pytest-asyncio` for async test support (configured in `pyproject.toml`)

### Reference Projects

The repository includes three reference projects that serve as the basis for this implementation:

- **`PyRFC/`**: Python RFC SDK bindings (compiled C extension)
  - Source: https://github.com/SAP/PyRFC
  - Purpose: Provides low-level RFC connectivity to SAP systems
  - Used by: Core layer for RFC connection management

- **`abap-adt-py/`**: Python library for ADT REST API interactions over HTTP
  - Source: https://github.com/yourusername/abap-adt-py
  - Purpose: Reference for ADT API endpoints and request/response patterns
  - Use as guide for: URI patterns, XML parsing, ADT operations

- **`mcp-abap-abap-adt-api/`**: TypeScript MCP server using HTTP/ADT APIs
  - Source: https://github.com/mario-andreschak/mcp-abap-abap-adt-api
  - Purpose: Reference for MCP tool definitions and workflows
  - Use as guide for: Tool interfaces, MCP server structure, user workflows

**Key Difference**: This implementation (`app/`) uses RFC (`SADT_REST_RFC_ENDPOINT`) instead of HTTP to access ADT APIs, providing direct SAP system access without network-level HTTP requirements.

## Common Development Workflows

### Adding a New MCP Tool

When implementing new tools from the reference projects, follow this pattern:

1. **Research the reference implementation**:
   - Check `mcp-abap-abap-adt-api/` for the tool's MCP interface and expected parameters
   - Check `abap-adt-py/` for the ADT API endpoints and HTTP patterns
   - Document the ADT URI, HTTP method, headers, and response format

2. **Add service method**:
   - Create or extend service class in `app/services/`
   - Use `RfcAdapter` to call ADT endpoint via `SADT_REST_RFC_ENDPOINT`
   - Parse XML/JSON response as needed
   - Add proper error handling and logging

3. **Create MCP tool registration**:
   - Add tool function in `app/mcp/tools/`
   - Define tool schema matching reference MCP server
   - Call service method and return formatted response

4. **Register in MCP server**:
   - Import and register tool in `app/mcp/server.py`
   - Update tool list in server resource

5. **Add tests**:
   - Unit test for service method
   - Integration test with live SAP connection
   - Test error cases and edge conditions

### Example: Implementing Lock/Unlock

Based on `abap-adt-py` and `mcp-abap-abap-adt-api`:

```python
# 1. Service method in app/services/class_service.py
def lock(self, object_url: str) -> str:
    """Lock an ABAP object for editing."""
    response = self.adapter.request(
        uri=object_url,
        method="POST",
        params={"_action": "LOCK", "accessMode": "MODIFY"},
        body="",
    )
    # Extract lock handle from response headers
    return response.headers.get("LOCK_HANDLE", "")

# 2. MCP tool in app/mcp/tools/class_tools.py
@mcp.tool()
def lock(object_url: str) -> str:
    """Lock an ABAP object for editing."""
    return class_service.lock(object_url)
```

### Debugging RFC Calls

Enable debug logging to see RFC request/response details:
```python
logging.basicConfig(level=logging.DEBUG)
```

Use `app/tests/test_debug_rfc.py` for manual RFC call testing.

### Working with ADT URIs

**CRITICAL**: ADT URIs must be **exact and complete**. Many endpoints require specific path segments.

**Verified URI Patterns:**

1. **Class Source**:
   ```
   /sap/bc/adt/oo/classes/{class_name}/source/{include_type}
   include_type: main, implementation, testclasses, macros
   ```

2. **Class Includes** (MUST include type):
   ```
   ✅ CORRECT:   /sap/bc/adt/oo/classes/{class_name}/includes/{include_type}
   ❌ INCORRECT: /sap/bc/adt/oo/classes/{class_name}/includes

   include_type: definitions, implementations, testclasses, macros
   ```

3. **Class Structure**:
   ```
   /sap/bc/adt/oo/classes/{class_name}/objectstructure?version=active&withShortDescriptions=true
   ```

4. **Programs**:
   ```
   /sap/bc/adt/programs/programs/{program_name}
   ```

5. **Search**:
   ```
   /sap/bc/adt/repository/informationsystem/search
   ```

6. **Lock/Unlock**:
   ```
   {object_uri}?_action=LOCK&accessMode=MODIFY
   ```

7. **Activation**:
   ```
   /sap/bc/adt/activation
   ```

**Common Mistakes to Avoid:**
- ❌ Missing required path segments (e.g., `/includes` without `/{type}`)
- ❌ Using wrong HTTP method (GET vs POST)
- ❌ Wrong Content-Type header (use `text/plain` for source code)
- ❌ Forgetting query parameters like `version=active`

**Query Parameters:**
- `version=active|inactive` - Control which version to retrieve
- `withShortDescriptions=true` - Include descriptions in structure
- `_action=LOCK` - Lock operations
- `accessMode=MODIFY|READ` - Access mode for locks

### Implementation Priority

When implementing new tools, follow this priority order:

1. **Object Modification** (enables core workflows):
   - `lock`/`unlock`: Required for all edit operations
   - `set_object_source`: Modify source code
   - `activate`: Activate after changes

2. **Transport Management** (required for production systems):
   - `transport_info`: Get transport details
   - `create_transport`: Create new transport requests

3. **Code Quality** (improves developer experience):
   - `syntax_check`: Validate code before activation
   - `prettyprint`: Format code consistently

4. **Advanced Features**:
   - `create`/`delete`: Object lifecycle
   - `get_table`: Dictionary metadata
   - `run_unit_tests`: Quality assurance
   - `code_completion`: IDE-like features
