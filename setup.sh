#!/bin/bash
# Unified setup script for ABAP ADT RFC MCP Server
# This script performs complete installation and configuration

set -e

echo "=========================================="
echo "ABAP ADT RFC MCP Server - Setup"
echo "=========================================="
echo ""

# ============================================
# 1. Detect Operating System
# ============================================
echo "[1/8] Detecting operating system..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    OS="macos"
    CONFIG_DIR="$HOME/Library/Application Support/Claude"
    DYLD_VAR="DYLD_LIBRARY_PATH"
    echo "   Detected: macOS"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS="linux"
    CONFIG_DIR="$HOME/.config/Claude"
    DYLD_VAR="LD_LIBRARY_PATH"
    echo "   Detected: Linux"
elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    OS="windows"
    CONFIG_DIR="$APPDATA/Claude"
    DYLD_VAR="PATH"
    echo "   Detected: Windows"
else
    echo "   ERROR: Unsupported operating system: $OSTYPE"
    exit 1
fi

CONFIG_FILE="$CONFIG_DIR/claude_desktop_config.json"
echo "   Config file: $CONFIG_FILE"
echo ""

# ============================================
# 2. Configure SAP RFC SDK Path
# ============================================
echo "[2/8] Configuring SAP NetWeaver RFC SDK..."

# Default SDK path (can be overridden)
if [ -z "$SAPNWRFC_HOME" ]; then
    if [[ "$OS" == "macos" ]]; then
        SAPNWRFC_HOME="/Users/local/nwrfcsdk"
    elif [[ "$OS" == "linux" ]]; then
        SAPNWRFC_HOME="/usr/local/nwrfcsdk"
    elif [[ "$OS" == "windows" ]]; then
        SAPNWRFC_HOME="C:/nwrfcsdk"
    fi
fi

echo "   SAPNWRFC_HOME: $SAPNWRFC_HOME"

# Verify SDK exists
if [ ! -d "$SAPNWRFC_HOME" ]; then
    echo "   WARNING: SAP RFC SDK not found at $SAPNWRFC_HOME"
    echo "   Please install SAP NetWeaver RFC SDK and set SAPNWRFC_HOME"
    echo ""
    read -p "   Continue anyway? (y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Set library path
if [[ "$OS" == "macos" ]]; then
    export DYLD_LIBRARY_PATH="$SAPNWRFC_HOME/lib:$DYLD_LIBRARY_PATH"
elif [[ "$OS" == "linux" ]]; then
    export LD_LIBRARY_PATH="$SAPNWRFC_HOME/lib:$LD_LIBRARY_PATH"
elif [[ "$OS" == "windows" ]]; then
    export PATH="$SAPNWRFC_HOME/lib:$PATH"
fi

echo "   Library path configured"
echo ""

# ============================================
# 3. Get Project Directory
# ============================================
echo "[3/8] Resolving project directory..."
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
VENV_PYTHON="$PROJECT_DIR/.venv/bin/python"
echo "   Project: $PROJECT_DIR"
echo ""

# ============================================
# 4. Create Virtual Environment
# ============================================
echo "[4/8] Setting up Python virtual environment..."
if [ ! -d "$PROJECT_DIR/.venv" ]; then
    echo "   Creating new virtual environment..."
    python3 -m venv "$PROJECT_DIR/.venv"
    echo "   Virtual environment created"
else
    echo "   Virtual environment already exists"
fi
echo ""

# ============================================
# 5. Install Dependencies
# ============================================
echo "[5/8] Installing Python dependencies..."

# Check if uv is available
if command -v uv &> /dev/null; then
    echo "   Using uv package manager..."
    cd "$PROJECT_DIR"
    uv sync
else
    echo "   Using pip..."
    "$VENV_PYTHON" -m pip install --upgrade pip
    "$VENV_PYTHON" -m pip install pydantic python-dotenv mcp pytest pytest-asyncio pytest-cov
fi

# Compile PyRFC if directory exists
if [ -d "$PROJECT_DIR/PyRFC" ]; then
    echo "   Compiling PyRFC bindings..."
    cd "$PROJECT_DIR/PyRFC"
    "$VENV_PYTHON" -m pip install .
    cd "$PROJECT_DIR"
    echo "   PyRFC installed"
fi
echo ""

# ============================================
# 6. Validate Environment Configuration
# ============================================
echo "[6/8] Validating environment configuration..."

if [ ! -f "$PROJECT_DIR/.env" ]; then
    echo "   WARNING: .env file not found"
    echo "   Creating .env.example..."

    cat > "$PROJECT_DIR/.env.example" << 'EOF'
# SAP Connection Configuration
SAP_ASHOST=your.sap.server.com
SAP_SYSNR=00
SAP_CLIENT=100
SAP_USER=your_username
SAP_PASSWD=your_password
SAP_LANG=EN
SAP_ROUTER=/H/router/S/port

# Test Configuration
TEST_CLASS_NAME=CL_ABAP_CHAR_UTILITIES
TEST_SEARCH_QUERY=CL_ABAP*
TEST_PROGRAM_NAME=SAPBC_START_PROGRAMS
EOF

    echo "   Please copy .env.example to .env and configure your SAP credentials"
    echo ""
    read -p "   Continue? (y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo "   .env file found"
fi
echo ""

# ============================================
# 7. Configure Claude Desktop
# ============================================
echo "[7/8] Configuring Claude Desktop..."

# Create config directory if needed
mkdir -p "$CONFIG_DIR"

# Prepare environment variables for MCP
if [[ "$OS" == "macos" ]]; then
    LIB_PATH_VALUE="$SAPNWRFC_HOME/lib:\\\$DYLD_LIBRARY_PATH"
elif [[ "$OS" == "linux" ]]; then
    LIB_PATH_VALUE="$SAPNWRFC_HOME/lib:\\\$LD_LIBRARY_PATH"
elif [[ "$OS" == "windows" ]]; then
    LIB_PATH_VALUE="$SAPNWRFC_HOME\\\\lib;\\\$PATH"
    VENV_PYTHON="$PROJECT_DIR/.venv/Scripts/python.exe"
fi

# Create or update config file
cat > "$CONFIG_FILE" << EOF
{
  "mcpServers": {
    "ABAP-ADT-RFC-Server": {
      "command": "$VENV_PYTHON",
      "args": ["-m", "app.main"],
      "cwd": "$PROJECT_DIR",
      "env": {
        "SAPNWRFC_HOME": "$SAPNWRFC_HOME",
        "$DYLD_VAR": "$LIB_PATH_VALUE"
      }
    }
  }
}
EOF

echo "   Configuration written to: $CONFIG_FILE"
echo ""

# ============================================
# 8. Validate Installation
# ============================================
echo "[8/8] Validating installation..."

# Test Python import
if "$VENV_PYTHON" -c "import pyrfc" 2>/dev/null; then
    echo "   PyRFC: OK"
else
    echo "   PyRFC: WARNING - Could not import pyrfc module"
fi

if "$VENV_PYTHON" -c "import mcp" 2>/dev/null; then
    echo "   MCP: OK"
else
    echo "   MCP: ERROR - Could not import mcp module"
    exit 1
fi

echo ""
echo "=========================================="
echo "Setup Complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Configure .env with your SAP credentials"
echo "  2. Restart Claude Desktop"
echo "  3. Look for the wrench icon in Claude Desktop"
echo "  4. Verify 'ABAP-ADT-RFC-Server' shows green indicator"
echo ""
echo "Test the installation:"
echo "  .venv/bin/python -m pytest app/tests/test_integration.py -v"
echo ""
echo "Run the MCP server manually:"
echo "  .venv/bin/python -m app.main"
echo ""
echo "Environment variables set:"
echo "  export SAPNWRFC_HOME=$SAPNWRFC_HOME"
echo "  export $DYLD_VAR=$SAPNWRFC_HOME/lib:\$$DYLD_VAR"
echo ""
