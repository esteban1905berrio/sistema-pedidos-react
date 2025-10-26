"""MCP tools for ABAP repository navigation operations."""

from typing import List, Dict, Any, Optional
from mcp.server.fastmcp import FastMCP
from pydantic import Field

from app.services.navigation_service import NavigationService


def register_navigation_tools(mcp: FastMCP, navigation_service: NavigationService):
    """
    Register navigation-related tools with the MCP server.

    Args:
        mcp: FastMCP server instance
        navigation_service: NavigationService instance for repository navigation operations
    """

    @mcp.tool(
        name="get_node_contents",
        description="Get contents of a repository node (package, folder, etc.). "
        "Returns list of objects and sub-nodes contained in the specified repository node. "
        "Useful for browsing the package/folder structure.",
    )
    def get_node_contents(
        node_uri: str = Field(
            description="URI of the repository node (e.g., '/sap/bc/adt/packages/ZTEST', '/sap/bc/adt/packages/$TMP')"
        ),
        project_name: str = Field(
            default=None,
            description="Optional project name for context (usually not needed)"
        )
    ) -> List[Dict[str, Any]]:
        """
        Retrieve contents of a repository node.

        Returns all objects and sub-folders contained in a package or folder,
        including classes, programs, function groups, and other ABAP objects.

        Example usage:
        - get_node_contents("/sap/bc/adt/packages/ZTEST")
        - get_node_contents("/sap/bc/adt/packages/$TMP")
        """
        return navigation_service.get_node_contents(node_uri, project_name)

    @mcp.tool(
        name="find_object_path",
        description="Find the complete repository path of an ABAP object. "
        "Returns the full path from root package to the object, including all parent folders. "
        "Useful for understanding object location in the repository tree.",
    )
    def find_object_path(
        object_uri: str = Field(
            description="URI of the ABAP object (e.g., '/sap/bc/adt/oo/classes/zcl_test')"
        )
    ) -> Dict[str, Any]:
        """
        Find the complete path of an object in the repository tree.

        Returns comprehensive path information including:
        - Full path from root to object
        - Parent package
        - Path elements
        - Object location

        Example usage:
        - find_object_path("/sap/bc/adt/oo/classes/zcl_test")
        - find_object_path("/sap/bc/adt/programs/programs/ztest_prog")
        """
        return navigation_service.find_object_path(object_uri)

    @mcp.tool(
        name="get_package_objects",
        description="Get ABAP objects from a package with pagination support. "
        "Returns object list grouped by type (CLAS, PROG, FUGR, etc.) with essential metadata: "
        "PGMID, OBJECT, OBJ_NAME, SRCSYSTEM, AUTHOR, DEVCLASS, CREATED_ON, CHECK_DATE. "
        "Supports pagination (offset/limit), filtering by object type, author, and creation date range. "
        "Useful for exploring package contents and analyzing object composition.",
    )
    def get_package_objects(
        package_name: str = Field(
            description="Package name (e.g., 'ZMMI1229_0', 'Z_UTILITIES', '$TMP')",
            min_length=1,
            max_length=30
        ),
        max_rows: int = Field(
            default=50,
            description="Maximum objects per page (default: 50, max: 1000)",
            ge=1,
            le=1000
        ),
        offset: int = Field(
            default=0,
            description="Number of objects to skip for pagination (default: 0). "
            "Use pagination.next_offset from previous response to get next page.",
            ge=0
        ),
        object_types: Optional[List[str]] = Field(
            default=None,
            description="Optional list of object types to filter (e.g., ['CLAS', 'PROG', 'FUGR']). "
            "Common types: CLAS (classes), PROG (programs), FUGR (function groups), "
            "TABL (tables), DTEL (data elements), DOMA (domains), TTYP (table types)"
        ),
        author: Optional[str] = Field(
            default=None,
            description="Optional author filter to show only objects created by specific user (e.g., 'DEVELOPER')",
            max_length=12
        ),
        created_from: Optional[str] = Field(
            default=None,
            description="Optional start date filter in YYYY-MM-DD format (e.g., '2025-01-01'). "
            "Returns objects created on or after this date",
            pattern=r'^\d{4}-\d{2}-\d{2}$'
        ),
        created_to: Optional[str] = Field(
            default=None,
            description="Optional end date filter in YYYY-MM-DD format (e.g., '2025-12-31'). "
            "Returns objects created on or before this date",
            pattern=r'^\d{4}-\d{2}-\d{2}$'
        ),
        response_format: str = Field(
            default="detailed",
            description="Response format: 'detailed' (all fields, default), 'summary' (names + counts), 'types_only' (counts only). "
            "Use 'types_only' for quick overview, 'summary' for moderate detail, 'detailed' for complete information.",
            pattern="^(detailed|summary|types_only)$"
        )
    ) -> Dict[str, Any]:
        """
        Retrieve all ABAP objects from a package with advanced filtering.

        Queries the TADIR table and returns objects grouped by type with the following fields:
        - PGMID: Program ID (e.g., 'R3TR' for repository objects)
        - OBJECT: Object type (CLAS, PROG, FUGR, TABL, DTEL, etc.)
        - OBJ_NAME: Object name
        - SRCSYSTEM: Source system
        - AUTHOR: Author/creator of the object
        - DEVCLASS: Development class (package)
        - CREATED_ON: Creation date (formatted as YYYY-MM-DD)
        - CHECK_DATE: Last verification date (formatted as YYYY-MM-DD)

        Returns comprehensive package analysis with pagination:
        - Total object count in current page
        - Objects grouped by type (CLAS, PROG, FUGR, etc.)
        - Count per object type
        - Complete metadata for each object
        - Pagination metadata (has_more, next_offset)
        - Applied filters in metadata

        Example usage:
        - get_package_objects("ZFI") - Get first page (50 objects)
        - get_package_objects("ZFI", offset=50) - Get second page
        - get_package_objects("ZFI", max_rows=100) - Custom page size
        - get_package_objects("ZFI", offset=100, max_rows=20) - Third page, 20 objects
        - get_package_objects("$TMP", object_types=["CLAS", "PROG"]) - Only classes and programs
        - get_package_objects("ZMMI1229_0", author="DEVELOPER") - Only objects by specific author
        - get_package_objects("ZFII1008_0", created_from="2025-01-01", created_to="2025-12-31") - Date range
        - get_package_objects("$TMP", object_types=["CLAS"], author="SAP") - Combined filters

        Pagination workflow:
        1. First call: get_package_objects("ZFI") → Returns objects + pagination.has_more=true
        2. Check response.pagination.has_more
        3. If true, call get_package_objects("ZFI", offset=response.pagination.next_offset)
        4. Repeat until pagination.has_more=false

        Use cases:
        - Package content exploration and documentation (use pagination for large packages)
        - Object inventory and classification by type
        - Author analysis and contribution tracking
        - Change analysis within date ranges
        - Package dependency analysis
        - Migration planning and impact assessment
        - Audit and compliance reporting
        """
        return navigation_service.get_package_objects(
            package_name,
            max_rows,
            offset,
            object_types,
            author,
            created_from,
            created_to
        )
