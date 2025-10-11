# ABAP ADT RFC MCP Server

MCP (Model Context Protocol) server for interacting with SAP ABAP systems via RFC SDK. This server enables Claude Code and other LLM tools to read and analyze ABAP code directly from SAP systems.

## Features

### Available Tools

#### Class Operations
- **get_class_source**: Get complete source code of ABAP classes
- **get_class_structure**: Get class metadata, methods, and attributes
- **get_object_source**: Get source code for any ABAP object by URI

#### Search Operations
- **search_objects**: Search for ABAP objects by name pattern with wildcards

#### Program Operations
- **get_program_source**: Get source code of ABAP programs/reports
- **get_include_source**: Get source code of program includes

## Prerequisites

1. **SAP NetWeaver RFC SDK** installed
   - Download from SAP Support Portal
   - Install to `/Users/local/nwrfcsdk` (or configure path)

2. **Python 3.11+** with virtual environment

3. **SAP System Access**
   - Application server host and port
   - Valid SAP credentials
   - ADT (ABAP Development Tools) enabled

## Installation

### 1. Clone and Setup

```bash
cd /Users/bastianroot/CursorIDEWorkspace/brootpersonalagent

# Create virtual environment
python3 -m venv .venv
source .venv/bin/activate

# Install dependencies
pip install pydantic python-dotenv mcp pytest pytest-asyncio pytest-cov

# Compile and install PyRFC
cd PyRFC
export SAPNWRFC_HOME=/Users/local/nwrfcsdk
export DYLD_LIBRARY_PATH=$SAPNWRFC_HOME/lib:$DYLD_LIBRARY_PATH
python3 -m pip install .
cd ..
```

### 2. Configure Environment

Create `.env` file with your SAP credentials:

```bash
# Required Settings
SAP_ASHOST=your.sap.server.com
SAP_SYSNR=00
SAP_CLIENT=100
SAP_USER=your_username
SAP_PASSWD=your_password

# Optional Settings
SAP_LANG=EN
SAP_ROUTER=/H/router.host/S/sapdp99

# Test Configuration (optional)
TEST_CLASS_NAME=CL_ABAP_CHAR_UTILITIES
TEST_SEARCH_QUERY=CL_ABAP*
TEST_PROGRAM_NAME=SAPBC_START_PROGRAMS
```

### 3. Configure MCP for Claude Code

The `.mcp.json` file is already configured:

```json
{
  "mcpServers": {
    "ABAP-ADT-RFC-Server": {
      "command": "/Users/bastianroot/CursorIDEWorkspace/brootpersonalagent/.venv/bin/python",
      "args": ["-m", "app.main"],
      "cwd": "/Users/bastianroot/CursorIDEWorkspace/brootpersonalagent",
      "env": {
        "SAPNWRFC_HOME": "/Users/local/nwrfcsdk",
        "DYLD_LIBRARY_PATH": "/Users/local/nwrfcsdk/lib",
        "PYTHONPATH": "/Users/bastianroot/CursorIDEWorkspace/brootpersonalagent"
      }
    }
  }
}
```

## Usage

### With Claude Code

1. **Restart Claude Code** to load the MCP server
2. The server will appear as "ABAP-ADT-RFC-Server" in available tools
3. Ask Claude Code questions about your ABAP code:

```
Example queries:
- "Search for all custom classes starting with Z"
- "Show me the source code of class CL_ABAP_CHAR_UTILITIES"
- "What methods does class CL_HTTP_CLIENT have?"
- "Get the structure of class ZMY_CUSTOM_CLASS"
- "Find all programs matching ZREP*"
```

### Direct Testing

```bash
# Set environment variables
export SAPNWRFC_HOME=/Users/local/nwrfcsdk
export DYLD_LIBRARY_PATH=$SAPNWRFC_HOME/lib:$DYLD_LIBRARY_PATH

# Run unit tests
.venv/bin/python -m pytest app/tests/test_config.py -v
.venv/bin/python -m pytest app/tests/test_rfc_adapter.py -v

# Run integration tests (requires SAP connection)
.venv/bin/python -m pytest app/tests/test_integration.py -v
```

## Architecture

```
app/
├── core/
│   ├── config.py           # SAP configuration management
│   ├── rfc_connection.py   # RFC connection pool
│   └── rfc_adapter.py      # HTTP-style adapter for RFC calls
├── services/
│   ├── class_service.py    # ABAP class operations
│   ├── search_service.py   # Object search
│   └── program_service.py  # Program operations
├── mcp/
│   ├── server.py           # Main MCP server
│   └── tools/
│       ├── class_tools.py  # MCP tools for classes
│       ├── search_tools.py # MCP tools for search
│       └── program_tools.py# MCP tools for programs
└── tests/
    ├── test_config.py
    ├── test_rfc_adapter.py
    └── test_integration.py
```

## API Examples

### Search for Objects

```python
from app.services.search_service import SearchService

results = search_service.search_objects("Z*", max_results=10)
for obj in results:
    print(f"{obj['name']} - {obj['type']}")
```

### Get Class Source

```python
from app.services.class_service import ClassService

source = class_service.get_class_source("CL_ABAP_CHAR_UTILITIES")
print(source)
```

### Get Class Structure

```python
structure = class_service.get_class_structure("CL_HTTP_CLIENT")
print(f"Methods: {len(structure['components'])}")
```

## Development

### Running Tests

```bash
# Unit tests
.venv/bin/python -m pytest app/tests/ -v

# With coverage
.venv/bin/python -m pytest app/tests/ --cov=app --cov-report=html

# Integration tests only
.venv/bin/python -m pytest app/tests/test_integration.py -v -s
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

## Troubleshooting

### RFC Library Not Found

```bash
# Ensure environment variables are set
export SAPNWRFC_HOME=/Users/local/nwrfcsdk
export DYLD_LIBRARY_PATH=$SAPNWRFC_HOME/lib:$DYLD_LIBRARY_PATH
```

### Connection Errors

- Verify SAP credentials in `.env`
- Check network connectivity to SAP server
- Confirm SAP router string if using

### MCP Server Not Loading

- Restart Claude Code completely
- Check `.mcp.json` configuration
- Verify Python virtual environment path
- Check logs for error messages

## Security Notes

- **Never commit `.env`** file with credentials
- Use environment variables for sensitive data
- Consider using SAP Secure Network Communication (SNC)
- Implement proper access controls in SAP

## License

[Add your license here]

## Contributing

[Add contributing guidelines here]
