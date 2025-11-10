# SAP ABAP MCP Server - Java Implementation

**Enterprise-grade MCP server for SAP ABAP integration** using **Spring Boot 3.4.0** and **SAP JCo 3.1.x**.

[![Java](https://img.shields.io/badge/Java-21+-orange)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Internal-red)](LICENSE)

---

## Overview

This project implements a **Model Context Protocol (MCP) server** that enables AI assistants like Claude Code to interact with SAP ABAP systems. It uses **Spring AI MCP SDK** for standardized MCP integration and **SAP JCo** for enterprise-grade RFC connectivity.

**Migration Status**: Active migration from Python (PyRFC) to Java (SAP JCo)
- **Current**: 1/59 MCP tools implemented
- **Legacy**: Python implementation archived in `python-legacy/` (fully functional)
- **See Plan**: [Migration Plan](docs/requirements/mcp/migration_plan.md)

---

## Features

### Current (Phase 0 - Complete)

✅ **Infrastructure**
- Spring Boot 3.4.0 with Spring AI MCP SDK 1.1.0-M4
- SAP JCo 3.1.x native connection pooling (5-10 concurrent connections)
- HTTP-to-RFC adapter pattern for ADT API access
- STDIO transport for MCP JSON-RPC communication

✅ **Available Tools** (1/59)
- `get_class_source` - Retrieve ABAP class source code

### Planned (Phase 1 - Weeks 2-7)

**16 Core Tools**:
- Repository & Source (9 tools)
- Data Dictionary (4 tools)
- Transport Management (3 tools)

See [Migration Plan](docs/requirements/mcp/migration_plan.md) for complete roadmap.

---

## Architecture

```
Claude Code / Claude Desktop
          ↓ STDIO (JSON-RPC)
   Spring AI MCP Server
          ↓
   RfcAdapter (HTTP → RFC)
          ↓
   SAP JCo Connection Pool
          ↓
   SADT_REST_RFC_ENDPOINT (FM)
          ↓
   SAP ADT REST API
          ↓
   SAP ABAP System
```

**Key Components**:
- **JCoConfiguration**: Thread-safe connection pool using SAP JCo native pooling
- **RfcAdapter**: Converts HTTP-style requests to RFC function module calls
- **Services**: Business logic for ABAP operations (ClassService, etc.)
- **Tools**: MCP tool definitions with Spring AI annotations

See [CLAUDE.md](CLAUDE.md) for detailed architecture documentation.

---

## Quick Start

### Prerequisites

- **Java 21+** ([Adoptium OpenJDK](https://adoptium.net/))
- **Maven 3.9+** ([Apache Maven](https://maven.apache.org/))
- **SAP JCo 3.1.x** ([SAP Support Portal](https://support.sap.com/en/product/connectors/jco.html) - requires S-user)

### Installation

#### 1. Clone Repository

```bash
git clone <repository-url>
cd brootpersonalagent
```

#### 2. Install SAP JCo Libraries

⚠️ **Important**: SAP JCo libraries cannot be redistributed (SAP license).

1. Download from [SAP Support Portal](https://support.sap.com/en/product/connectors/jco.html)
2. Extract and copy to `lib/` directory:

```bash
lib/
├── sapjco3.jar                    # Platform-independent JAR
└── libsapjco3.dylib               # Native library (macOS)
    libsapjco3.so                  # Native library (Linux)
    sapjco3.dll                    # Native library (Windows)
```

See [lib/README.md](lib/README.md) for detailed instructions.

#### 3. Build Project

```bash
# Compile
mvn clean compile

# Run tests (requires SAP connection)
mvn test

# Build JAR
mvn clean package
```

### Configuration

#### Option 1: Claude Desktop Integration (Recommended)

Edit `.mcp.json` in project root:

```json
{
  "mcpServers": {
    "giralmcp": {
      "command": "mvn",
      "args": [
        "spring-boot:run",
        "-f",
        "/absolute/path/to/brootpersonalagent/pom.xml"
      ],
      "env": {
        "JAVA_HOME": "/path/to/jdk-21",
        "SAP_ASHOST": "your.sap.server.com",
        "SAP_SYSNR": "00",
        "SAP_CLIENT": "100",
        "SAP_USER": "your_username",
        "SAP_PASSWD": "your_password",
        "SAP_LANG": "EN",
        "SAP_ROUTER": "/H/router_host/S/port",
        "SAP_POOL_CAPACITY": "5",
        "SAP_PEAK_LIMIT": "10"
      }
    }
  }
}
```

**Required Environment Variables**:
- `SAP_ASHOST`: SAP application server host
- `SAP_SYSNR`: System number (00-99)
- `SAP_CLIENT`: Client number (e.g., 100, 200)
- `SAP_USER`: SAP username
- `SAP_PASSWD`: SAP password

**Optional Variables**:
- `SAP_LANG`: Language (default: EN)
- `SAP_ROUTER`: SAP router string (if using VPN)
- `SAP_POOL_CAPACITY`: Connection pool size (default: 5)
- `SAP_PEAK_LIMIT`: Peak connection limit (default: 10)

#### Option 2: Direct Execution

```bash
# Via Maven
mvn spring-boot:run

# Via JAR
java -Djava.library.path=./lib -jar target/sap-mcp-server-0.1.0-POC.jar
```

---

## Usage

### With Claude Desktop

After configuring `.mcp.json`, the server will be available automatically in Claude Desktop.

**Example prompts**:

```
"Get the source code of class CL_ABAP_CHAR_UTILITIES"
"Show me the implementation of class ZTEST_CLASS"
"Retrieve ABAP class ZCL_UTIL main definition"
```

### Direct Tool Call (JSON-RPC)

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "get_class_source",
    "arguments": {
      "className": "CL_ABAP_CHAR_UTILITIES",
      "version": "active",
      "includeType": "main"
    }
  },
  "id": 1
}
```

---

## Development

### Project Structure

```
brootpersonalagent/
├── src/
│   └── main/java/com/crystal/mcp/sapserver/
│       ├── SapMcpServerApplication.java       # Main class
│       ├── config/
│       │   └── JCoConfiguration.java          # JCo connection config
│       ├── service/
│       │   ├── RfcAdapter.java               # HTTP-to-RFC adapter
│       │   └── ClassService.java             # Business logic
│       ├── tool/
│       │   └── ClassTools.java               # MCP tool definitions
│       └── model/
│           └── ClassSourceResult.java        # DTOs
├── lib/                                       # SAP JCo libraries
├── python-legacy/                             # Python reference (59 tools)
├── docs/
│   ├── requirements/mcp/migration_plan.md     # Migration roadmap
│   └── research/abap_mcp_tools_strategy_2025.md
├── logs/
│   ├── java/                                  # Java server logs
│   └── python/                                # Python server logs
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
mvn clean test                 # Clean and test

# Packaging
mvn clean package              # Build JAR
mvn clean install              # Install to local Maven repo

# Code Quality
mvn verify                     # Run verification
```

### Adding New Tools

See [CLAUDE.md](CLAUDE.md#adding-a-new-mcp-tool) for detailed instructions.

**Quick example**:

1. **Create Service** (`src/main/java/.../service/`)

```java
@Service
public class NewService {
    @Autowired
    private RfcAdapter rfcAdapter;

    public Result doSomething(String param) {
        Map<String, Object> response = rfcAdapter.request(
            "/sap/bc/adt/endpoint",
            "GET",
            Map.of(),
            ""
        );
        return new Result(response);
    }
}
```

2. **Create MCP Tool** (`src/main/java/.../tool/`)

```java
@Component
public class NewTools {
    @Autowired
    private NewService service;

    @Tool(description = "Does something with SAP")
    public String doSomething(
        @Param(description = "Parameter") String param
    ) {
        return service.doSomething(param).toJson();
    }
}
```

3. **Test**

```java
@SpringBootTest
class NewServiceTest {
    @Autowired
    private NewService service;

    @Test
    void testDoSomething() {
        Result result = service.doSomething("test");
        assertNotNull(result);
    }
}
```

---

## Migration from Python

This project is migrating from a Python implementation (PyRFC) to Java (SAP JCo).

**Python Legacy**: See [python-legacy/PYTHON_LEGACY.md](python-legacy/PYTHON_LEGACY.md)
- 59 MCP tools fully functional
- Archived but maintained
- Used as reference for Java implementation

**Migration Progress**:
```
[██░░░░░░░░░░░░░░░░░░] 1/59 tools (1.7%)
```

**See**: [Migration Plan](docs/requirements/mcp/migration_plan.md) for complete roadmap.

---

## Troubleshooting

### JCo Library Not Found

```
java.lang.UnsatisfiedLinkError: no sapjco3 in java.library.path
```

**Solution**:
1. Verify `lib/sapjco3.jar` and native library exist
2. Run with: `java -Djava.library.path=./lib -jar target/sap-mcp-server-0.1.0-POC.jar`

### Connection Timeout

```
JCoException: Connect to SAP gateway failed
```

**Solution**:
1. Verify VPN connection active
2. Check `SAP_ROUTER` configuration
3. Test: `ping <SAP_ASHOST>`

### SADT_REST_RFC_ENDPOINT Not Found

```
Function module SADT_REST_RFC_ENDPOINT not found
```

**Solution**:
- ADT not installed on SAP system
- User lacks ADT authorization
- Contact SAP Basis team

### Maven Build Fails

```bash
# Clean and rebuild
mvn clean install -U

# Skip tests if needed
mvn clean package -DskipTests
```

---

## Testing

### Run Tests

```bash
# All tests
mvn test

# Specific test
mvn test -Dtest=ClassServiceTest

# With coverage
mvn test jacoco:report
# Report: target/site/jacoco/index.html
```

### Test Requirements

- SAP connection configured via environment variables
- 80%+ code coverage target
- Integration tests require live SAP system

---

## Documentation

- **Developer Guide**: [CLAUDE.md](CLAUDE.md)
- **Migration Plan**: [docs/requirements/mcp/migration_plan.md](docs/requirements/mcp/migration_plan.md)
- **Java Implementation Details**: [README_JAVA.md](README_JAVA.md)
- **Python Legacy**: [python-legacy/PYTHON_LEGACY.md](python-legacy/PYTHON_LEGACY.md)
- **SAP JCo Installation**: [lib/README.md](lib/README.md)

---

## References

- [Spring AI MCP SDK](https://spring.io/blog/2025/02/14/mcp-java-sdk-released-2)
- [SAP JCo Documentation](https://support.sap.com/en/product/connectors/jco.html)
- [Model Context Protocol](https://modelcontextprotocol.io)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)

---

## License

Internal use - Crystal Development Team

**SAP JCo License**: Proprietary SAP software. Cannot be redistributed. Each developer must download from SAP Support Portal.

---

## Contributing

This is an internal project. For contributions:

1. Create feature branch: `git checkout -b feature/your-feature`
2. Follow Java code style (Spring Boot conventions)
3. Write tests (80%+ coverage)
4. Update documentation
5. Submit pull request

---

## Status

**Phase 0**: ✅ Complete (Project Reorganization)
**Phase 1**: ⏳ Pending (Core Tool Migration)
**Overall**: 🚧 Active Development

**Last Updated**: 2025-11-08
**Version**: 0.1.0-POC
**Contact**: Crystal Development Team
