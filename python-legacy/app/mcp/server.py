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
from app.services.transport_service import TransportService
from app.services.object_service import ObjectService
from app.services.activation_service import ActivationService
from app.services.code_quality_service import CodeQualityService
from app.services.creation_service import CreationService
from app.services.unittest_service import UnittestService
from app.services.whereused_service import WhereUsedService
from app.services.cds_service import CDSService
from app.services.rap_service import RAPService
from app.services.enhancement_service import EnhancementService
from app.services.modification_service import ModificationService
from app.services.interface_service import InterfaceService
from app.mcp.tools.class_tools import register_class_tools
from app.mcp.tools.search_tools import register_search_tools
from app.mcp.tools.program_tools import register_program_tools
from app.mcp.tools.discovery_tools import register_discovery_tools
from app.mcp.tools.navigation_tools import register_navigation_tools
from app.mcp.tools.ddic_tools import register_ddic_tools
from app.mcp.tools.query_tools import register_query_tools
from app.mcp.tools.transport_tools import register_transport_tools
from app.mcp.tools.object_tools import register_object_tools
from app.mcp.tools.activation_tools import register_activation_tools
from app.mcp.tools.code_quality_tools import register_code_quality_tools
from app.mcp.tools.creation_tools import register_creation_tools
from app.mcp.tools.unittest_tools import register_unittest_tools
from app.mcp.tools.whereused_tools import register_whereused_tools
from app.mcp.tools.cds_tools import register_cds_tools
from app.mcp.tools.rap_tools import register_rap_tools
from app.mcp.tools.enhancement_tools import register_enhancement_tools
from app.mcp.tools.modification_tools import register_modification_tools
from app.mcp.tools.interface_tools import register_interface_tools

# Configure logging
logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

# Create MCP server
mcp = FastMCP("ABAP-ADT-RFC-Server")

# Global services cache (lazy-loaded)
_services_cache = None


def get_services():
    """
    Get services (lazy-loaded on first call).

    Returns:
        tuple: All initialized service instances
    """
    global _services_cache

    if _services_cache is not None:
        return _services_cache

    logger.info("Initializing ABAP services...")
    _services_cache = initialize_services()
    logger.info("Services initialized successfully")
    return _services_cache


def initialize_services():
    """
    Initialize services with RFC connection pool.

    Returns:
        tuple: All initialized service instances
    """
    try:
        # Load configuration from environment
        config = load_config()
        logger.info(f"Loaded SAP configuration for {config.ashost}:{config.sysnr}")

        # Initialize connection pool (not acquiring connection yet)
        from app.core.rfc_connection import get_connection_pool
        pool = get_connection_pool(config)
        logger.info("RFC connection pool initialized")

        # Store config and pool for per-request connection acquisition
        # Services will use the pool to get connections when needed
        from app.core.rfc_adapter import RfcAdapter
        logger.info("RfcAdapter will use connection pool for requests")

        # Initialize all services with connection pool (not adapter yet)
        # Services will create adapters per-request using the pool
        class_service = ClassService(pool)
        search_service = SearchService(pool)
        program_service = ProgramService(pool)
        discovery_service = DiscoveryService(pool)
        navigation_service = NavigationService(pool)
        ddic_service = DdicService(pool)
        query_service = QueryService(pool)
        transport_service = TransportService(pool)
        object_service = ObjectService(pool)
        activation_service = ActivationService(pool)
        code_quality_service = CodeQualityService(pool)
        creation_service = CreationService(pool)
        unittest_service = UnittestService(pool)
        whereused_service = WhereUsedService(pool)
        cds_service = CDSService(pool)
        rap_service = RAPService(pool)
        enhancement_service = EnhancementService(pool)
        modification_service = ModificationService(pool)
        interface_service = InterfaceService(pool)

        return (
            class_service,
            search_service,
            program_service,
            discovery_service,
            navigation_service,
            ddic_service,
            query_service,
            transport_service,
            object_service,
            activation_service,
            code_quality_service,
            creation_service,
            unittest_service,
            whereused_service,
            cds_service,
            rap_service,
            enhancement_service,
            modification_service,
            interface_service,
        )

    except Exception as e:
        logger.error(f"Failed to initialize services: {e}")
        raise


# Register tools (services will be lazy-loaded on first use)
logger.info("Registering MCP tools...")


def get_class_service():
    return get_services()[0]


def get_search_service():
    return get_services()[1]


def get_program_service():
    return get_services()[2]


def get_discovery_service():
    return get_services()[3]


def get_navigation_service():
    return get_services()[4]


def get_ddic_service():
    return get_services()[5]


def get_query_service():
    return get_services()[6]


def get_transport_service():
    return get_services()[7]


def get_object_service():
    return get_services()[8]


def get_activation_service():
    return get_services()[9]


def get_code_quality_service():
    return get_services()[10]


def get_creation_service():
    return get_services()[11]


def get_unittest_service():
    return get_services()[12]


def get_whereused_service():
    return get_services()[13]


def get_cds_service():
    return get_services()[14]


def get_rap_service():
    return get_services()[15]


def get_enhancement_service():
    return get_services()[16]


def get_modification_service():
    return get_services()[17]


def get_interface_service():
    return get_services()[18]


# Pass getter functions instead of service instances
register_class_tools(mcp, get_class_service)
register_search_tools(mcp, get_search_service)
register_program_tools(mcp, get_program_service)
register_discovery_tools(mcp, get_discovery_service)
register_navigation_tools(mcp, get_navigation_service)
register_ddic_tools(mcp, get_ddic_service)
register_query_tools(mcp, get_query_service)
register_transport_tools(mcp, get_transport_service)
register_object_tools(mcp, get_object_service)
register_activation_tools(mcp, get_activation_service)
register_code_quality_tools(mcp, get_code_quality_service)
register_creation_tools(mcp, get_creation_service)
register_unittest_tools(mcp, get_unittest_service)
register_whereused_tools(mcp, get_whereused_service)
register_cds_tools(mcp, get_cds_service)
register_rap_tools(mcp, get_rap_service)
register_enhancement_tools(mcp, get_enhancement_service)
register_modification_tools(mcp, get_modification_service)
register_interface_tools(mcp, get_interface_service)
logger.info("MCP tools registered (services will lazy-load on first use)")


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

### Transport Management Operations
- **get_transport_tasks**: Get tasks associated with a transport
- **get_transport_objects**: Get ABAP objects from a transport or task (recommended for complete data)
- **transport_info**: Get transport information for an object
- **create_transport**: Create a new transport request
- **list_user_transports**: List transport requests for a user
- **add_object_to_transport**: Assign an object to a transport request
- **release_transport**: Release a transport request (⚠️ Use with caution!)
- **get_transport_config**: Get transport configuration for the system
- **delete_transport**: Delete a transport request (⚠️ Non-released only!)
- **set_transport_owner**: Change owner of a transport request
- **add_transport_user**: Add a collaborator to a transport
- **get_system_users**: Get list of users in the SAP system
- **get_transport_reference**: Get transport references for an object

### Object Modification Operations
- **lock**: Lock an ABAP object for editing (returns LOCK_HANDLE)
- **unlock**: Unlock an object after editing
- **set_object_source**: Modify source code of an object (requires lock)

### Activation Operations
- **activate**: Activate a single ABAP object
- **activate_objects**: Activate multiple objects in batch
- **get_inactive_objects**: Get list of inactive objects for current user

### Code Quality Operations
- **syntax_check**: Check ABAP code for syntax errors
- **prettyprint**: Format ABAP code according to SAP standards
- **get_prettyprint_settings**: Get current pretty printer settings
- **set_prettyprint_settings**: Configure pretty printer (indentation, keyword style)

### Object Lifecycle Operations
- **create_class**: Create a new ABAP class
- **delete_object**: Delete an ABAP object (⚠️ use with caution!)
- **validate_object_name**: Validate object name according to SAP conventions

### Unit Testing Operations
- **run_unit_tests**: Execute ABAP unit tests with optional code coverage

### Where-Used Analysis Operations
- **get_where_used**: Find where an ABAP object is used across the system
- **get_where_used_dependencies**: Get detailed dependency graph for an object

### CDS Views & Core Data Services Operations
- **get_cds_view_metadata**: Get metadata for CDS views including SQL view name
- **get_cds_view_source**: Get DDL source code of CDS views
- **search_cds_views_by_sqlview**: Search CDS views by SQL view name pattern
- **get_cds_view_properties**: Get package, owner, and API state properties

### RAP Objects & OData Services Operations
- **get_service_binding**: Get service binding metadata (SRVB)
- **get_service_definition_metadata**: Get service definition metadata (SRVD)
- **get_service_definition_source**: Get service definition source code
- **get_odata_service_info**: Get OData service information and endpoints
- **get_metadata_extension**: Get metadata extension (DDLX) for UI annotations
- **get_ddlx_parser_info**: Get annotation definitions for metadata extensions
- **get_behavior_definition**: Get behavior definition (BDEF) source code
- **explore_rap_object**: Intelligent exploration of RAP object relationships

### Enhancement Operations (Ampliaciones)
- **search_enhancements**: Search for enhancements in a package (ENHO types)
- **get_enhancement_metadata**: Get enhancement metadata including hook implementations
- **get_enhancement_source**: Get enhancement source code (ENHANCEMENT blocks)

### High-Level Modification Workflows (NEW!)
- **modify_function_module**: Complete workflow to modify function module (LOCK → SYNTAX → MODIFY → UNLOCK → ACTIVATE)
- **modify_class**: Complete workflow to modify ABAP class with automatic validation and activation
- **modify_program**: Complete workflow to modify ABAP program/report with validation
- **modify_include**: Complete workflow to modify program include with validation

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
8. Manage transport requests and change management
9. Create and release transports programmatically
10. Collaborate on transports with multiple users
11. **Lock, edit, and activate ABAP objects**
12. **Complete end-to-end modification workflows**
13. **Work with CDS Views and Core Data Services**
14. **Explore RAP objects and OData services**
15. **Analyze enhancements and hook implementations**

Example queries:
- "Show me all custom classes starting with Z"
- "Get the source code of class CL_ABAP_CHAR_UTILITIES"
- "What methods does class CL_HTTP_CLIENT have?"
- "Show me the structure of table MARA"
- "Preview the first 10 rows of table USR02"
- "What object types are available in the repository?"
- "Create a new transport for my changes"
- "List my open transport requests"
- "What transport is object ZTEST_CLASS in?"
- "Show me all tasks and objects in transport S4DK932806"
- "What objects are in task S4DK932807?"
- "Lock class ZTEST_CLASS for editing"
- "Modify the source code of ZTEST_CLASS"
- "Activate class ZTEST_CLASS"
- "Show me all my inactive objects"
- "Get metadata for CDS view ZIFII1008_2"
- "Show me the DDL source code of CDS view ZIFII1008_2"
- "Find all enhancements in package ZI1008"
- "Get the source code of enhancement ZFII1008_1"
- "Show me the service binding for ZMYSERVICE"
- "Get the behavior definition for ZMYBO"
- "Explore the RAP object ZMYSERVICEDEF"
"""


if __name__ == "__main__":
    logger.info("Starting ABAP ADT RFC MCP Server...")
    mcp.run(transport="stdio")
