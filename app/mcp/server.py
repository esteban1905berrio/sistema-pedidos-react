"""Main MCP server for ABAP operations via RFC."""

import logging
from mcp.server.fastmcp import FastMCP

from app.core.config import load_config
from app.core.rfc_connection import get_connection
from app.services.class_service import ClassService
from app.services.search_service import SearchService
from app.services.program_service import ProgramService
from app.services.discovery_service import DiscoveryService
from app.services.navigation_service import NavigationService
from app.services.ddic_service import DdicService
from app.services.query_service import QueryService
from app.mcp.tools.class_tools import register_class_tools
from app.mcp.tools.search_tools import register_search_tools
from app.mcp.tools.program_tools import register_program_tools
from app.mcp.tools.discovery_tools import register_discovery_tools
from app.mcp.tools.navigation_tools import register_navigation_tools
from app.mcp.tools.ddic_tools import register_ddic_tools
from app.mcp.tools.query_tools import register_query_tools

# Configure logging
logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

# Create MCP server
mcp = FastMCP("ABAP-ADT-RFC-Server")


def initialize_services():
    """
    Initialize services with RFC connection.

    Returns:
        tuple: All initialized service instances
    """
    try:
        # Load configuration from environment
        config = load_config()
        logger.info(f"Loaded SAP configuration for {config.ashost}:{config.sysnr}")

        # Get RFC connection from pool
        conn = get_connection(config).__enter__()
        logger.info("RFC connection established")

        # Initialize services
        class_service = ClassService(conn)
        search_service = SearchService(conn)
        program_service = ProgramService(conn)
        discovery_service = DiscoveryService(conn)
        navigation_service = NavigationService(conn)
        ddic_service = DdicService(conn)
        query_service = QueryService(conn)

        return (
            class_service,
            search_service,
            program_service,
            discovery_service,
            navigation_service,
            ddic_service,
            query_service,
        )

    except Exception as e:
        logger.error(f"Failed to initialize services: {e}")
        raise


# Initialize services
logger.info("Initializing ABAP services...")
(
    class_service,
    search_service,
    program_service,
    discovery_service,
    navigation_service,
    ddic_service,
    query_service,
) = initialize_services()
logger.info("Services initialized successfully")

# Register tools
logger.info("Registering MCP tools...")
register_class_tools(mcp, class_service)
register_search_tools(mcp, search_service)
register_program_tools(mcp, program_service)
register_discovery_tools(mcp, discovery_service)
register_navigation_tools(mcp, navigation_service)
register_ddic_tools(mcp, ddic_service)
register_query_tools(mcp, query_service)
logger.info("MCP tools registered")


# Add server information
@mcp.resource("about://server")
def get_server_info() -> str:
    """Get information about this MCP server."""
    return """
# ABAP ADT RFC Server

This MCP server provides tools to interact with SAP ABAP systems via RFC.

## Available Tools

### Class Operations
- **get_class_source**: Get source code of ABAP classes
- **get_class_structure**: Get class structure and metadata
- **get_class_includes**: Get all includes (main, test, macros) for a class
- **get_class_components**: Get categorized class components by type
- **get_object_source**: Get source code for any ABAP object by URI
- **get_object_structure**: Get structure metadata for any ABAP object

### Search Operations
- **search_objects**: Search for ABAP objects by name pattern

### Program Operations
- **get_program_source**: Get source code of ABAP programs
- **get_include_source**: Get source code of program includes

### Discovery Operations
- **get_object_types**: List all available ABAP object types
- **adt_discovery**: Get ADT capabilities and available endpoints
- **get_feature_details**: Get details about specific ADT features

### Navigation Operations
- **get_node_contents**: Navigate repository tree structure
- **find_object_path**: Find complete path to an object in repository tree

### Data Dictionary (DDIC) Operations
- **get_ddic_element**: Get table/structure/data element definitions
- **ddic_repository_access**: Direct access to DDIC repository
- **get_annotation_definitions**: Get CDS annotation definitions
- **package_search_help**: Package name autocomplete/search help

### Query & Data Preview Operations
- **get_table_contents**: Preview table data with filtering and limits
- **run_query**: Execute custom SQL queries or advanced data retrieval

## Configuration

The server reads SAP connection details from environment variables:
- SAP_ASHOST: Application server host
- SAP_SYSNR: System number
- SAP_CLIENT: Client number
- SAP_USER: Username
- SAP_PASSWD: Password
- SAP_LANG: Language (optional, default: EN)
- SAP_ROUTER: SAP router string (optional)

## Usage with Claude Code

Use this server with Claude Code to:
1. Search for ABAP objects in your SAP system
2. Read and analyze ABAP source code
3. Understand class structures and dependencies
4. Navigate repository tree structure
5. Access Data Dictionary metadata
6. Preview table contents and execute queries
7. Discover available object types and ADT features

Example queries:
- "Show me all custom classes starting with Z"
- "Get the source code of class CL_ABAP_CHAR_UTILITIES"
- "What methods does class CL_HTTP_CLIENT have?"
- "Show me the structure of table MARA"
- "Preview the first 10 rows of table USR02"
- "What object types are available in the repository?"
"""


if __name__ == "__main__":
    logger.info("Starting ABAP ADT RFC MCP Server...")
    mcp.run(transport="stdio")
