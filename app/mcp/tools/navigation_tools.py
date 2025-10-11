"""MCP tools for ABAP repository navigation operations."""

from typing import List, Dict, Any
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
