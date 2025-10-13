"""MCP tool registration for RAP (RESTful ABAP Programming) objects operations."""

from mcp.server.fastmcp import FastMCP
from pydantic import Field
from typing import Optional

from app.services.rap_service import RAPService


def register_rap_tools(mcp: FastMCP, rap_service: RAPService):
    """Register RAP objects tools with MCP server."""

    # ===========================================================================
    # SERVICE BINDING & DEFINITION
    # ===========================================================================

    @mcp.tool(
        name="get_service_binding",
        description="Get Service Binding metadata for RAP (RESTful ABAP Programming) objects. "
                   "Returns binding type (OData V2/V4, UI, Web API), publication status, and associated Service Definition. "
                   "Use this to understand how a RAP service is exposed."
    )
    def get_service_binding(
        binding_name: str = Field(
            description="Name of the Service Binding (e.g., 'Z_SERVICE_UI')"
        ),
        version: str = Field(
            default="active",
            description="Version to retrieve: 'active' or 'inactive'"
        )
    ) -> dict:
        """Get Service Binding metadata."""
        return rap_service.get_service_binding(binding_name, version)

    @mcp.tool(
        name="get_service_definition_metadata",
        description="Get Service Definition metadata for RAP objects. "
                   "Returns exposed entities, associations, and other service structure information. "
                   "Service Definitions define which CDS views are exposed in the service."
    )
    def get_service_definition_metadata(
        srvd_name: str = Field(
            description="Name of the Service Definition (e.g., 'Z_SERVICE')"
        ),
        version: str = Field(
            default="active",
            description="Version to retrieve: 'active' or 'inactive'"
        )
    ) -> dict:
        """Get Service Definition metadata."""
        return rap_service.get_service_definition_metadata(srvd_name, version)

    @mcp.tool(
        name="get_service_definition_source",
        description="Get Service Definition source code. "
                   "Returns the complete service definition with expose statements. "
                   "Shows which CDS entities are exposed and with what aliases."
    )
    def get_service_definition_source(
        srvd_name: str = Field(
            description="Name of the Service Definition (e.g., 'Z_SERVICE')"
        ),
        version: str = Field(
            default="active",
            description="Version to retrieve: 'active' or 'inactive'"
        )
    ) -> str:
        """Get Service Definition source code."""
        return rap_service.get_service_definition_source(srvd_name, version)

    @mcp.tool(
        name="get_odata_service_info",
        description="Get OData service information including entity sets, service version, and namespace. "
                   "Use this to understand the structure of an OData service published from RAP."
    )
    def get_odata_service_info(
        service_name: str = Field(
            description="Name of the OData service (e.g., 'Z_SERVICE_0001')"
        )
    ) -> dict:
        """Get OData service information."""
        return rap_service.get_odata_service_info(service_name)

    # ===========================================================================
    # METADATA EXTENSION (DDLX)
    # ===========================================================================

    @mcp.tool(
        name="get_metadata_extension",
        description="Get Metadata Extension (DDLX) for RAP objects. "
                   "Metadata Extensions contain UI annotations that enhance CDS views for Fiori applications. "
                   "Returns annotated view name, layer information, and UI annotations."
    )
    def get_metadata_extension(
        ddlx_name: str = Field(
            description="Name of the Metadata Extension (e.g., 'ZC_RAP_ZTCXR1003_1')"
        ),
        version: str = Field(
            default="active",
            description="Version to retrieve: 'active' or 'inactive'"
        )
    ) -> dict:
        """Get Metadata Extension metadata."""
        return rap_service.get_metadata_extension(ddlx_name, version)

    @mcp.tool(
        name="get_ddlx_parser_info",
        description="Get DDLX parser information and available annotation definitions. "
                   "Returns the complete set of annotations that can be used in Metadata Extensions. "
                   "Useful for understanding which UI annotations are available."
    )
    def get_ddlx_parser_info() -> dict:
        """Get DDLX parser info and annotation definitions."""
        return rap_service.get_ddlx_parser_info()

    # ===========================================================================
    # BEHAVIOR DEFINITION (BDEF)
    # ===========================================================================

    @mcp.tool(
        name="get_behavior_definition",
        description="Get Behavior Definition (BDEF) source code for RAP managed objects. "
                   "Behavior Definitions specify operations (create, update, delete), validations, "
                   "determinations, and actions for RAP business objects. "
                   "Can be managed, unmanaged, or projection."
    )
    def get_behavior_definition(
        bdef_name: str = Field(
            description="Name of the Behavior Definition (e.g., 'ZI_RAP_ZTCXR1003_1')"
        ),
        version: str = Field(
            default="active",
            description="Version to retrieve: 'active' or 'inactive'"
        )
    ) -> str:
        """Get Behavior Definition source code."""
        return rap_service.get_behavior_definition(bdef_name, version)

    # ===========================================================================
    # RAP EXPLORER - INTELLIGENT COMPONENT LOADING
    # ===========================================================================

    @mcp.tool(
        name="explore_rap_object",
        description="Intelligently explore a RAP object and load all related components. "
                   "This tool automatically detects the object type and recursively loads: "
                   "Service Binding → Service Definition → CDS Views → Metadata Extensions → Behavior Definitions. "
                   "Returns a complete map of the RAP structure with all relationships. "
                   "Use this when you need to understand the complete RAP architecture for a service."
    )
    def explore_rap_object(
        object_name: str = Field(
            description="Name of the RAP object to explore (e.g., 'Z_SERVICE_UI', 'Z_SERVICE', 'ZI_ENTITY')"
        ),
        object_type: Optional[str] = Field(
            default=None,
            description="Optional type hint: 'SRVB' (Service Binding), 'SRVD' (Service Definition), 'CDS', 'DDLX', 'BDEF'"
        )
    ) -> dict:
        """
        Explore RAP object and load all related components.

        Returns complete RAP structure with all relationships.
        """
        return rap_service.explore_rap_object(object_name, object_type)
