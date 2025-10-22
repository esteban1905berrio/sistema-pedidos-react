"""MCP tools for ABAP class operations."""

from typing import Literal
from mcp.server.fastmcp import FastMCP
from pydantic import Field

from app.services.class_service import ClassService


def register_class_tools(mcp: FastMCP, class_service: ClassService):
    """
    Register class-related tools with the MCP server.

    Args:
        mcp: FastMCP server instance
        class_service: ClassService instance for ABAP class operations
    """

    @mcp.tool(
        name="get_class_source",
        description="Get the source code of an ABAP class from SAP system. "
        "Returns the complete class definition including methods, attributes, and implementation.",
        annotations={
            "title": "Get Class Source Code",
            "readOnlyHint": True,
            "destructiveHint": False,
            "idempotentHint": True,
            "openWorldHint": True
        }
    )
    def get_class_source(
        class_name: str = Field(
            description="Name of the ABAP class (e.g., 'CL_ABAP_CHAR_UTILITIES', 'ZTEST_CLASS')"
        ),
        version: Literal["active", "inactive"] = Field(
            default="active",
            description="Version to retrieve: 'active' for activated code or 'inactive' for draft",
        ),
    ) -> str:
        """
        Retrieve the source code of an ABAP class.

        This tool fetches the complete source code of a class from the SAP system,
        including class definition, methods, and implementation.

        Example usage:
        - get_class_source("CL_ABAP_CHAR_UTILITIES")
        - get_class_source("ZTEST_CLASS", version="inactive")
        """
        return class_service.get_class_source(class_name, version=version)

    @mcp.tool(
        name="get_class_structure",
        description="[USE ONLY WHEN EXPLICITLY REQUESTED] Get the structure and metadata of an ABAP class. "
        "Returns class components (methods, attributes), visibility, and other metadata without the full source code. "
        "IMPORTANT: Only use this tool when the user specifically asks for class structure/metadata. "
        "For general class information, use get_class_source instead.",
        annotations={
            "title": "Get Class Structure",
            "readOnlyHint": True,
            "destructiveHint": False,
            "idempotentHint": True,
            "openWorldHint": True
        }
    )
    def get_class_structure(
        class_name: str = Field(
            description="Name of the ABAP class (e.g., 'CL_ABAP_CHAR_UTILITIES')"
        ),
        version: Literal["active", "inactive"] = Field(
            default="active", description="Version to retrieve: 'active' or 'inactive'"
        ),
    ) -> dict:
        """
        Retrieve the structure and metadata of an ABAP class.

        ⚠️ USE ONLY WHEN EXPLICITLY REQUESTED BY USER

        This tool provides an overview of a class including:
        - Class name, type, and visibility
        - List of methods with their signatures
        - Attributes and their properties
        - Related links and components

        Useful for understanding class architecture without reading full source code.
        However, prefer get_class_source for most queries unless structure-only is specifically requested.

        Example usage:
        - get_class_structure("CL_ABAP_CHAR_UTILITIES")
        """
        return class_service.get_class_structure(class_name, version=version)

    @mcp.tool(
        name="get_object_source",
        description="Get source code for any ABAP object using its ADT URI. "
        "Generic method that works with classes, programs, includes, and other ADT objects.",
    )
    def get_object_source(
        object_uri: str = Field(
            description="Full ADT URI to the object (e.g., '/sap/bc/adt/oo/classes/ZTEST/source/main')"
        ),
        version: Literal["active", "inactive"] = Field(
            default="active", description="Version to retrieve: 'active' or 'inactive'"
        ),
    ) -> str:
        """
        Retrieve source code for any ABAP object by its ADT URI.

        This is a generic tool that can fetch source code for various ABAP object types
        when you have the specific ADT URI.

        Example usage:
        - get_object_source("/sap/bc/adt/oo/classes/ZTEST/source/main")
        - get_object_source("/sap/bc/adt/programs/programs/ZTEST_PROG/source/main")
        """
        return class_service.get_object_source(object_uri, version=version)

    @mcp.tool(
        name="get_class_includes",
        description="Get all includes of an ABAP class (main, testclasses, macros, etc.). "
        "Returns list of include types with their URIs and metadata. "
        "Useful for understanding class structure and accessing specific include types.",
    )
    def get_class_includes(
        class_name: str = Field(
            description="Name of the ABAP class (e.g., 'ZCL_TEST_CLASS')"
        )
    ) -> list:
        """
        Retrieve all includes of an ABAP class.

        Returns information about all include types for the class:
        - Main class definition (CLAS/OC)
        - Test classes (CLAS/OCT)
        - Macros (CLAS/OM)
        - Local types (CLAS/OL)
        - And other include types

        Example usage:
        - get_class_includes("ZCL_TEST_CLASS")
        - get_class_includes("CL_ABAP_CHAR_UTILITIES")
        """
        return class_service.get_class_includes(class_name)

    @mcp.tool(
        name="get_class_components",
        description="Get detailed component information for a class (methods, attributes, events, types). "
        "Returns components categorized by type: methods, attributes, events, types. "
        "More detailed than get_class_structure.",
    )
    def get_class_components(
        class_name: str = Field(
            description="Name of the ABAP class (e.g., 'ZCL_TEST_CLASS')"
        ),
        version: Literal["active", "inactive"] = Field(
            default="active",
            description="Version to retrieve: 'active' or 'inactive'"
        )
    ) -> dict:
        """
        Retrieve detailed component information for a class.

        Returns components organized by category:
        - methods: All methods (public, protected, private)
        - attributes: All attributes/fields
        - events: All events
        - types: All type definitions
        - other: Any other components

        Example usage:
        - get_class_components("ZCL_TEST_CLASS")
        - get_class_components("CL_ABAP_CHAR_UTILITIES", version="active")
        """
        return class_service.get_class_components(class_name, version)

    @mcp.tool(
        name="get_object_structure",
        description="Get structure for any ABAP object by URI (generic, not class-specific). "
        "Works with any object type: classes, programs, function groups, etc. "
        "Returns object structure with components and metadata.",
    )
    def get_object_structure(
        object_uri: str = Field(
            description="URI of the ABAP object (e.g., '/sap/bc/adt/oo/classes/zcl_test')"
        )
    ) -> dict:
        """
        Retrieve structure for any ABAP object.

        Generic tool that works with any object type when you have the URI.
        Returns structure information including:
        - Object name, type, description
        - Components/elements
        - Related links
        - Metadata

        Example usage:
        - get_object_structure("/sap/bc/adt/oo/classes/zcl_test")
        - get_object_structure("/sap/bc/adt/programs/programs/ztest_prog")
        """
        return class_service.get_object_structure(object_uri)
