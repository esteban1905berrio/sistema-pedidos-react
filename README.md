# SAP ABAP MCP Server - Java Implementation

**Enterprise-grade MCP server for SAP ABAP integration** using **Spring Boot 3.4.0** and **SAP JCo 3.1.x**.

[![Java](https://img.shields.io/badge/Java-21+-orange)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Internal-red)](LICENSE)

---

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
  - [Windows](#windows)
  - [macOS](#macos)
  - [Linux](#linux)
- [Configuration](#configuration)
  - [Environment Variables](#environment-variables)
  - [Claude Desktop Integration](#claude-desktop-integration)
- [Running the Server](#running-the-server)
- [Troubleshooting](#troubleshooting)
- [Development](#development)
- [Documentation](#documentation)

---

## Overview

This project implements a **Model Context Protocol (MCP) server** that enables AI assistants like Claude Code to interact with SAP ABAP systems. It uses **Spring AI MCP SDK** for standardized MCP integration and **SAP JCo** for enterprise-grade RFC connectivity.

**Migration Status**: Active migration from Python (PyRFC) to Java (SAP JCo)
- **Current**: 2/59 MCP tools implemented (3.4%)
- **Legacy**: Python implementation archived in `python-legacy/` (fully functional)
- **See Plan**: [Migration Plan](docs/requirements/mcp/migration_plan.md)

### Features

✅ **Infrastructure**
- Spring Boot 3.4.0 with Spring AI MCP SDK 1.1.0-M4
- SAP JCo 3.1.x native connection pooling (5-10 concurrent connections)
- HTTP-to-RFC adapter pattern for ADT API access
- STDIO transport for MCP JSON-RPC communication
- **Multi-platform support**: Windows, macOS, Linux

✅ **Available Tools** (2/59)
- `get_class_source` - Retrieve ABAP class source code
- `modify_program_source` - Modify ABAP program/include with workflow (LOCK → MODIFY → UNLOCK)

---

## Prerequisites

Before installing the SAP MCP Server, ensure you have the following installed:

### All Operating Systems

| Component | Version | Download |
|-----------|---------|----------|
| **Java** | 21+ (LTS) | [Adoptium OpenJDK](https://adoptium.net/) |
| **Maven** | 3.9+ | [Apache Maven](https://maven.apache.org/) |
| **SAP JCo** | 3.1.9+ | [SAP Support Portal](https://support.sap.com/en/product/connectors/jco.html) ⚠️ Requires S-user |

### Verify Installation

```bash
# Check Java version (should be 21+)
java -version

# Check Maven version (should be 3.9+)
mvn -version
```

---

## Installation

Installation steps vary by operating system. Follow the instructions for your platform below.

### Windows

#### 1. Install Prerequisites

**Java 21+**:
1. Download [Adoptium OpenJDK 21](https://adoptium.net/temurin/releases/?version=21&os=windows)
2. Run installer (e.g., `OpenJDK21U-jdk_x64_windows_hotspot_21.0.5_11.msi`)
3. Verify installation:
   ```cmd
   java -version
   ```

**Maven 3.9+**:
1. Download [Apache Maven](https://maven.apache.org/download.cgi) (Binary zip archive)
2. Extract to `C:\Program Files\Apache\maven`
3. Add to System PATH:
   - Open System Properties → Environment Variables
   - Add to `Path`: `C:\Program Files\Apache\maven\bin`
4. Verify installation:
   ```cmd
   mvn -version
   ```

#### 2. Clone Repository

```cmd
git clone <repository-url>
cd giralmcp
```

#### 3. Install SAP JCo Libraries

⚠️ **Important**: SAP JCo libraries cannot be redistributed (SAP license). You must download them yourself.

1. **Download from SAP Support Portal**:
   - Go to https://support.sap.com/en/product/connectors/jco.html
   - Login with S-user credentials
   - Download: `SAPJCO3_NTAMD64_<version>.ZIP` (Windows x64)

2. **Extract and copy to `lib/` directory**:
   ```cmd
   REM Extract downloaded ZIP
   tar -xf SAPJCO3_NTAMD64_3.1.9.ZIP

   REM Copy files to project lib directory
   copy sapjco3.jar giralmcp\lib\
   copy sapjco3.dll giralmcp\lib\
   ```

3. **Verify files**:
   ```cmd
   dir lib\
   ```

   You should see:
   ```
   lib/
   ├── README.md
   ├── sapjco3.jar
   └── sapjco3.dll
   ```

#### 4. Build Project

```cmd
REM Compile
mvn clean compile

REM Run tests (requires SAP connection configured)
mvn test

REM Build JAR
mvn clean package
```

---

### macOS

#### 1. Install Prerequisites

**Java 21+**:
```bash
# Using Homebrew (recommended)
brew install openjdk@21

# Verify installation
java -version

# Set JAVA_HOME (add to ~/.zshrc or ~/.bash_profile)
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
source ~/.zshrc
```

**Maven 3.9+**:
```bash
# Using Homebrew
brew install maven

# Verify installation
mvn -version
```

#### 2. Clone Repository

```bash
git clone <repository-url>
cd giralmcp
```

#### 3. Install SAP JCo Libraries

⚠️ **Important**: SAP JCo libraries cannot be redistributed (SAP license). You must download them yourself.

1. **Download from SAP Support Portal**:
   - Go to https://support.sap.com/en/product/connectors/jco.html
   - Login with S-user credentials
   - Download appropriate version:
     - **Intel Mac**: `sapjco3-darwinintel64-3.1.9.tgz`
     - **Apple Silicon (M1/M2/M3)**: `sapjco3-darwinarm64-3.1.9.tgz`

2. **Extract and copy to `lib/` directory**:
   ```bash
   # Extract downloaded archive
   tar -xzf sapjco3-darwinintel64-3.1.9.tgz  # or darwinarm64

   # Copy files to project lib directory
   cp sapjco3.jar /path/to/giralmcp/lib/
   cp libsapjco3.dylib /path/to/giralmcp/lib/
   ```

3. **Verify files**:
   ```bash
   ls -l lib/
   ```

   You should see:
   ```
   lib/
   ├── README.md
   ├── libsapjco3.dylib
   └── sapjco3.jar
   ```

#### 4. Build Project

```bash
# Compile
mvn clean compile

# Run tests (requires SAP connection configured)
mvn test

# Build JAR
mvn clean package
```

---

### Linux

#### 1. Install Prerequisites

**Java 21+**:
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-21-jdk

# RHEL/CentOS/Fedora
sudo dnf install java-21-openjdk-devel

# Verify installation
java -version
```

**Maven 3.9+**:
```bash
# Ubuntu/Debian
sudo apt install maven

# RHEL/CentOS/Fedora
sudo dnf install maven

# Verify installation
mvn -version
```

#### 2. Clone Repository

```bash
git clone <repository-url>
cd giralmcp
```

#### 3. Install SAP JCo Libraries

⚠️ **Important**: SAP JCo libraries cannot be redistributed (SAP license). You must download them yourself.

1. **Download from SAP Support Portal**:
   - Go to https://support.sap.com/en/product/connectors/jco.html
   - Login with S-user credentials
   - Download appropriate version:
     - **x86_64**: `sapjco3-linuxx86_64-3.1.9.tgz`
     - **ARM64**: `sapjco3-linuxaarch64-3.1.9.tgz`

2. **Extract and copy to `lib/` directory**:
   ```bash
   # Extract downloaded archive
   tar -xzf sapjco3-linuxx86_64-3.1.9.tgz

   # Copy files to project lib directory
   cp sapjco3.jar /path/to/giralmcp/lib/
   cp libsapjco3.so /path/to/giralmcp/lib/
   ```

3. **Verify files**:
   ```bash
   ls -l lib/
   ```

   You should see:
   ```
   lib/
   ├── README.md
   ├── libsapjco3.so
   └── sapjco3.jar
   ```

#### 4. Build Project

```bash
# Compile
mvn clean compile

# Run tests (requires SAP connection configured)
mvn test

# Build JAR
mvn clean package
```

---

## Configuration

### Environment Variables

The SAP MCP Server requires the following environment variables to connect to your SAP system:

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `SAP_ASHOST` | ✅ | SAP application server hostname | `sap.company.com` |
| `SAP_SYSNR` | ✅ | System number | `00` |
| `SAP_CLIENT` | ✅ | Client number | `100` |
| `SAP_USER` | ✅ | SAP username | `DEVELOPER` |
| `SAP_PASSWD` | ✅ | SAP password | `your_password` |
| `SAP_LANG` | ⬜ | Language code (default: EN) | `EN` |
| `SAP_ROUTER` | ⬜ | SAP router string (if using VPN) | `/H/router.com/S/3299` |
| `SAP_POOL_CAPACITY` | ⬜ | Connection pool size (default: 5) | `5` |
| `SAP_PEAK_LIMIT` | ⬜ | Peak connection limit (default: 10) | `10` |

### Claude Desktop Integration

The recommended way to use this MCP server is through Claude Desktop.

#### Configuration File Location

| OS | Config File Location |
|----|---------------------|
| **Windows** | `%APPDATA%\Claude\claude_desktop_config.json` |
| **macOS** | `~/Library/Application Support/Claude/claude_desktop_config.json` |
| **Linux** | `~/.config/Claude/claude_desktop_config.json` |

#### Windows Configuration

Edit `%APPDATA%\Claude\claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "giralmcp": {
      "command": "mvn.cmd",
      "args": [
        "spring-boot:run",
        "-f",
        "C:\\path\\to\\giralmcp\\pom.xml"
      ],
      "env": {
        "JAVA_HOME": "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.5.11-hotspot",
        "SAP_ASHOST": "sap.company.com",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "100",
        "SAP_USER": "your_username",
        "SAP_PASSWD": "your_password",
        "SAP_LANG": "EN",
        "SAP_ROUTER": "/H/router.com/S/3299",
        "SAP_POOL_CAPACITY": "5",
        "SAP_PEAK_LIMIT": "10"
      }
    }
  }
}
```

**Important**: Use `mvn.cmd` on Windows (not `mvn`).

#### macOS Configuration

Edit `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "giralmcp": {
      "command": "mvn",
      "args": [
        "spring-boot:run",
        "-f",
        "/Users/username/giralmcp/pom.xml"
      ],
      "env": {
        "JAVA_HOME": "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home",
        "SAP_ASHOST": "sap.company.com",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "100",
        "SAP_USER": "your_username",
        "SAP_PASSWD": "your_password",
        "SAP_LANG": "EN",
        "SAP_ROUTER": "/H/router.com/S/3299",
        "SAP_POOL_CAPACITY": "5",
        "SAP_PEAK_LIMIT": "10"
      }
    }
  }
}
```

#### Linux Configuration

Edit `~/.config/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "giralmcp": {
      "command": "mvn",
      "args": [
        "spring-boot:run",
        "-f",
        "/home/username/giralmcp/pom.xml"
      ],
      "env": {
        "JAVA_HOME": "/usr/lib/jvm/java-21-openjdk-amd64",
        "SAP_ASHOST": "sap.company.com",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "100",
        "SAP_USER": "your_username",
        "SAP_PASSWD": "your_password",
        "SAP_LANG": "EN",
        "SAP_ROUTER": "/H/router.com/S/3299",
        "SAP_POOL_CAPACITY": "5",
        "SAP_PEAK_LIMIT": "10"
      }
    }
  }
}
```

---

## Running the Server

You can run the SAP MCP Server in three ways:

### Option 1: Using Startup Scripts (Recommended)

We provide platform-specific startup scripts that handle all configuration automatically.

#### Windows (Command Prompt)
```cmd
start-mcp.bat
```

#### Windows (PowerShell)
```powershell
.\start-mcp.ps1
```

#### macOS / Linux
```bash
./start-mcp.sh
```

The scripts will:
- ✅ Verify Java and Maven installation
- ✅ Check for SAP JCo libraries
- ✅ Set library paths automatically
- ✅ Display helpful error messages
- ✅ Start the MCP server

### Option 2: Via Maven

#### Windows
```cmd
mvn spring-boot:run
```

#### macOS / Linux
```bash
mvn spring-boot:run
```

**Note**: Ensure environment variables are set (see [Configuration](#configuration)).

### Option 3: Via JAR File

#### Windows
```cmd
java -Djava.library.path=.\lib -jar target\sap-mcp-server-0.1.0-POC.jar
```

#### macOS / Linux
```bash
java -Djava.library.path=./lib -jar target/sap-mcp-server-0.1.0-POC.jar
```

### Verifying Server is Running

When the server starts successfully, you should see:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.4.0)

[INFO] SAP JCo initialized successfully
[INFO] MCP Server started on STDIO transport
```

---

## Troubleshooting

### Common Issues

#### 1. JCo Library Not Found

**Error**:
```
java.lang.UnsatisfiedLinkError: no sapjco3 in java.library.path
```

**Solution**:

| OS | Steps |
|----|-------|
| **Windows** | 1. Verify `lib\sapjco3.dll` exists<br>2. Run: `java -Djava.library.path=.\lib -jar target\sap-mcp-server-0.1.0-POC.jar` |
| **macOS** | 1. Verify `lib/libsapjco3.dylib` exists<br>2. Run: `java -Djava.library.path=./lib -jar target/sap-mcp-server-0.1.0-POC.jar` |
| **Linux** | 1. Verify `lib/libsapjco3.so` exists<br>2. Run: `java -Djava.library.path=./lib -jar target/sap-mcp-server-0.1.0-POC.jar` |

See [lib/README.md](lib/README.md) for detailed installation instructions.

#### 2. Connection Timeout

**Error**:
```
JCoException: Connect to SAP gateway failed
Connection to partner 'host:port' broken
```

**Solution**:
1. ✅ Verify VPN connection is active
2. ✅ Check `SAP_ROUTER` environment variable is correct
3. ✅ Test connectivity:
   ```bash
   # Windows
   ping sap.company.com

   # macOS/Linux
   ping sap.company.com
   telnet sap.company.com 3300  # Port 3300 + system number
   ```
4. ✅ Contact SAP Basis team to verify:
   - Firewall rules
   - SAP router configuration
   - User authorization

#### 3. SADT_REST_RFC_ENDPOINT Not Found

**Error**:
```
Function module 'SADT_REST_RFC_ENDPOINT' not found
```

**Solution**:
- ❌ ADT (ABAP Development Tools) not installed on SAP system
- ❌ User lacks ADT authorization object `S_ADT_RES`
- ✅ Contact SAP Basis team to:
  1. Install ADT backend components
  2. Assign authorization profile for ADT

#### 4. Maven Build Fails

**Error**:
```
Failed to execute goal on project sap-mcp-server
```

**Solution**:

| OS | Command |
|----|---------|
| **Windows** | `mvn clean install -U` |
| **macOS** | `mvn clean install -U` |
| **Linux** | `mvn clean install -U` |

To skip tests:
```bash
mvn clean package -DskipTests
```

#### 5. Java Version Mismatch

**Error**:
```
Unsupported class file major version 65
```

**Solution**:
1. Verify Java version is 21+:
   ```bash
   java -version
   ```
2. Set `JAVA_HOME` to Java 21:

   | OS | Command |
   |----|---------|
   | **Windows** | `set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot` |
   | **macOS** | `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` |
   | **Linux** | `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` |

#### 6. Claude Desktop Not Detecting MCP Server

**Solution**:
1. ✅ Verify config file location:
   - Windows: `%APPDATA%\Claude\claude_desktop_config.json`
   - macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
   - Linux: `~/.config/Claude/claude_desktop_config.json`

2. ✅ Verify JSON syntax is valid (use https://jsonlint.com/)

3. ✅ Check absolute paths in configuration:
   - Windows: Use `\\` or `/` (not single `\`)
   - macOS/Linux: Use absolute paths starting with `/`

4. ✅ Restart Claude Desktop completely

5. ✅ Check Claude Desktop logs:
   - Windows: `%APPDATA%\Claude\logs\`
   - macOS: `~/Library/Logs/Claude/`
   - Linux: `~/.config/Claude/logs/`

---

## Development

### Project Structure

```
giralmcp/
├── src/
│   ├── main/java/com/crystal/mcp/sapserver/
│   │   ├── SapMcpServerApplication.java       # Main class
│   │   ├── config/
│   │   │   └── JCoConfiguration.java          # JCo connection pool
│   │   ├── service/
│   │   │   ├── RfcAdapter.java               # HTTP-to-RFC adapter
│   │   │   ├── ClassService.java             # Class operations
│   │   │   ├── NavigationService.java        # Search/navigation
│   │   │   └── TransportService.java         # Transport management
│   │   ├── tool/
│   │   │   ├── ClassTools.java               # Class MCP tools
│   │   │   └── NavigationTools.java          # Search MCP tools
│   │   └── model/
│   │       └── *.java                        # DTOs and results
│   └── test/java/                             # Unit and integration tests
├── lib/                                       # SAP JCo libraries
├── python-legacy/                             # Python reference (59 tools)
├── docs/
│   ├── requirements/mcp/migration_plan.md     # Migration roadmap
│   └── research/                              # Research documentation
├── logs/
│   └── java/                                  # Application logs
├── start-mcp.bat                              # Windows startup script
├── start-mcp.ps1                              # PowerShell startup script
├── start-mcp.sh                               # macOS/Linux startup script
├── pom.xml                                    # Maven configuration
├── README.md                                  # This file
└── CLAUDE.md                                  # Developer instructions
```

### Build Commands

```bash
# Development
mvn clean compile              # Compile
mvn spring-boot:run            # Run server

# Testing
mvn test                       # Run all tests
mvn test -Dtest=ClassServiceTest  # Run specific test

# Packaging
mvn clean package              # Build JAR
mvn clean install              # Install to local Maven repo

# Code Quality
mvn verify                     # Run verification
```

### Adding New Tools

See [CLAUDE.md](CLAUDE.md) for detailed developer instructions on adding new MCP tools.

---

## Documentation

- **Developer Guide**: [CLAUDE.md](CLAUDE.md) - Complete development guide
- **Migration Plan**: [docs/requirements/mcp/migration_plan.md](docs/requirements/mcp/migration_plan.md) - Python to Java migration roadmap
- **SAP JCo Installation**: [lib/README.md](lib/README.md) - Detailed JCo setup instructions
- **Python Legacy**: [python-legacy/PYTHON_LEGACY.md](python-legacy/PYTHON_LEGACY.md) - Original Python implementation (59 tools)

---

## References

- [Spring AI MCP SDK](https://spring.io/blog/2025/02/14/mcp-java-sdk-released-2)
- [SAP JCo Documentation](https://support.sap.com/en/product/connectors/jco.html)
- [Model Context Protocol](https://modelcontextprotocol.io)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)

---

## License

Internal use - Crystal Development Team

**SAP JCo License**: Proprietary SAP software. Cannot be redistributed. Each developer must download from SAP Support Portal with valid S-user credentials.

---

## Status

**Phase 0**: ✅ Complete (Project Reorganization)
**Phase 1**: ⏳ Pending (Core Tool Migration - 16 tools)
**Overall**: 🚧 Active Development

**Migration Progress**:
```
[██░░░░░░░░░░░░░░░░░░] 1/59 tools (1.7%)
```

**Last Updated**: 2025-11-10
**Version**: 0.1.0-POC
**Contact**: Crystal Development Team
