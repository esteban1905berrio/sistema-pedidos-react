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
- Run test flows via `python run_flow.py <flow_id>`
- 80%+ coverage for business logic
- Healthcare logic: 95%+ coverage (authentication, medical data processing)
- Mock external services (never call real APIs in tests)

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

**Implemented Tools**:
- ✅ `get_class_source`: Get complete ABAP class source code
- ✅ `get_class_structure`: Get class metadata, methods, attributes
- ✅ `get_object_source`: Get source for any ABAP object by ADT URI
- ✅ `search_objects`: Search ABAP objects by name pattern (supports wildcards)
- ✅ `get_program_source`: Get ABAP program/report source code
- ✅ `get_include_source`: Get program include source code

**To Be Implemented** (from reference projects):
- 🔲 `lock`/`unlock`: Lock/unlock objects for editing
- 🔲 `set_object_source`: Modify ABAP object source code
- 🔲 `activate`: Activate ABAP objects after modification
- 🔲 `create`: Create new ABAP objects
- 🔲 `delete`: Delete ABAP objects
- 🔲 `transport_info`: Get transport request information
- 🔲 `create_transport`: Create transport requests
- 🔲 `syntax_check`: Perform syntax checks on code
- 🔲 `prettyprint`: Format ABAP code
- 🔲 `code_completion`: Get code completion suggestions
- 🔲 `get_table`: Retrieve ABAP table/structure definitions
- 🔲 `run_unit_tests`: Execute ABAP unit tests

## Core Development Commands

### Setup and Installation

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

```bash
# Set required environment variables first
export SAPNWRFC_HOME=/Users/local/nwrfcsdk
export DYLD_LIBRARY_PATH=$SAPNWRFC_HOME/lib:$DYLD_LIBRARY_PATH

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

ADT URIs follow the pattern:
- Classes: `/sap/bc/adt/oo/classes/{CLASS_NAME}/source/{INCLUDE_TYPE}`
- Programs: `/sap/bc/adt/programs/programs/{PROGRAM_NAME}`
- Search: `/sap/bc/adt/repository/informationsystem/search`
- Lock: `{OBJECT_URI}?_action=LOCK&accessMode=MODIFY`
- Activate: `/sap/bc/adt/activation`

Query parameters like `version=active` and `withShortDescriptions=true` control response content.

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
