#!/bin/bash
# ============================================================================
# SAP MCP Server - macOS/Linux Startup Script
# ============================================================================
# This script starts the SAP MCP Server on macOS/Linux using Maven
#
# Prerequisites:
#   1. Java 21+ installed
#   2. Maven 3.9+ installed
#   3. SAP JCo libraries installed in lib/ directory
#   4. Environment variables configured (or set in .mcp.json)
#
# Usage:
#   ./start-mcp.sh
# ============================================================================

echo "Starting SAP MCP Server (macOS/Linux)..."
echo ""

# Detect OS
OS_TYPE=$(uname -s)
case "$OS_TYPE" in
    Darwin*)
        PLATFORM="macOS"
        JCO_LIB="lib/libsapjco3.dylib"
        ;;
    Linux*)
        PLATFORM="Linux"
        JCO_LIB="lib/libsapjco3.so"
        ;;
    *)
        echo "ERROR: Unsupported operating system: $OS_TYPE"
        exit 1
        ;;
esac

echo "Detected platform: $PLATFORM"
echo ""

# Detect Java Home if not set
if [ -z "$JAVA_HOME" ]; then
    echo "WARNING: JAVA_HOME is not set"
    echo "Attempting to detect Java installation..."

    if [ "$PLATFORM" = "macOS" ]; then
        # Try to detect Java on macOS
        JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null)
        if [ -z "$JAVA_HOME" ]; then
            JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null)
        fi
    fi

    if [ -z "$JAVA_HOME" ]; then
        if ! command -v java &> /dev/null; then
            echo "ERROR: Java not found. Please install Java 21+ and set JAVA_HOME"
            exit 1
        fi
    else
        export JAVA_HOME
        echo "Auto-detected JAVA_HOME: $JAVA_HOME"
    fi
else
    echo "Using JAVA_HOME: $JAVA_HOME"
fi

# Verify Java version
echo "Checking Java version..."
JAVA_VERSION=$(java -version 2>&1 | head -n 1)
if [[ ! "$JAVA_VERSION" =~ "21" ]]; then
    echo "WARNING: Java 21 recommended. Current version:"
    java -version
    echo ""
fi

# Verify Maven installation
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven not found. Please install Maven 3.9+ and add to PATH"
    exit 1
fi

# Verify SAP JCo libraries
if [ ! -f "$JCO_LIB" ]; then
    echo "ERROR: SAP JCo library not found: $JCO_LIB"
    echo "Please download SAP JCo for $PLATFORM and extract to lib/ directory"
    echo "See lib/README.md for installation instructions"
    exit 1
fi

if [ ! -f "lib/sapjco3.jar" ]; then
    echo "ERROR: SAP JCo JAR not found: lib/sapjco3.jar"
    echo "Please download SAP JCo for $PLATFORM and extract to lib/ directory"
    echo "See lib/README.md for installation instructions"
    exit 1
fi

echo "============================================================================"
echo "SAP JCo libraries found:"
echo "  - lib/sapjco3.jar"
echo "  - $JCO_LIB"
echo "============================================================================"
echo ""

# Set library path for JCo
LIB_PATH="$(pwd)/lib"
export DYLD_LIBRARY_PATH="$LIB_PATH:$DYLD_LIBRARY_PATH"  # macOS
export LD_LIBRARY_PATH="$LIB_PATH:$LD_LIBRARY_PATH"      # Linux

echo "Starting Maven Spring Boot application..."
echo ""

# Start the application
mvn spring-boot:run

if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: Application failed to start"
    echo "Check logs in logs/java/sap-mcp-server.log for details"
    exit 1
fi
