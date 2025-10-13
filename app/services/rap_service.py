"""Service for RAP (RESTful ABAP Programming) objects operations."""

import logging
from typing import Dict, Any, Optional, List
from xml.etree import ElementTree as ET

from app.core.rfc_adapter import RfcAdapter

logger = logging.getLogger(__name__)


class RAPService:
    """Service for RAP objects: Service Binding, Service Definition, Metadata Extension, Behavior Definition."""

    def __init__(self, adapter: RfcAdapter):
        """
        Initialize RAPService.

        Args:
            adapter: RFC adapter for making ADT requests
        """
        self.adapter = adapter

    # ============================================================================
    # SERVICE BINDING & DEFINITION
    # ============================================================================

    def get_service_binding(
        self,
        binding_name: str,
        version: str = "active"
    ) -> Dict[str, Any]:
        """
        Get Service Binding metadata.

        Args:
            binding_name: Name of the Service Binding
            version: Version ('active' or 'inactive')

        Returns:
            Dictionary with Service Binding metadata:
            - name: Binding name
            - type: Type (SRVB)
            - service_definition: Associated Service Definition
            - binding_type: OData V2/V4, UI, Web API
            - published: Publication status
            - service_version: Service version

        Example:
            >>> service.get_service_binding("Z_SERVICE_UI")
            {
                "name": "Z_SERVICE_UI",
                "binding_type": "ODATA_V2_UI",
                "published": true,
                ...
            }
        """
        logger.info(f"Getting Service Binding metadata for: {binding_name}")

        response = self.adapter.request(
            uri=f"/sap/bc/adt/businessservices/bindings/{binding_name.lower()}",
            method="GET",
            params={"version": version},
            content_type="application/vnd.sap.adt.businessservices.odatav2+xml"
        )

        if response.status_code == 200:
            return self._parse_service_binding(response.text)
        else:
            error_msg = f"{response.status_code} - Failed to get Service Binding for {binding_name}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_service_definition_metadata(
        self,
        srvd_name: str,
        version: str = "active"
    ) -> Dict[str, Any]:
        """
        Get Service Definition metadata.

        Args:
            srvd_name: Name of the Service Definition
            version: Version ('active' or 'inactive')

        Returns:
            Dictionary with Service Definition metadata

        Example:
            >>> service.get_service_definition_metadata("Z_SERVICE")
            {
                "name": "Z_SERVICE",
                "type": "SRVD/SRV",
                "exposed_entities": [...],
                ...
            }
        """
        logger.info(f"Getting Service Definition metadata for: {srvd_name}")

        response = self.adapter.request(
            uri=f"/sap/bc/adt/ddic/srvd/sources/{srvd_name.lower()}",
            method="GET",
            params={"version": version},
            content_type="application/vnd.sap.adt.ddic.srvd.v1+xml"
        )

        if response.status_code == 200:
            return self._parse_service_definition_metadata(response.text)
        else:
            error_msg = f"{response.status_code} - Failed to get Service Definition for {srvd_name}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_service_definition_source(
        self,
        srvd_name: str,
        version: str = "active"
    ) -> str:
        """
        Get Service Definition source code.

        Args:
            srvd_name: Name of the Service Definition
            version: Version ('active' or 'inactive')

        Returns:
            Service Definition source code

        Example:
            >>> service.get_service_definition_source("Z_SERVICE")
            "@EndUserText.label: 'Service Definition'
             define service Z_SERVICE {
               expose ZI_Entity as Entity;
             }"
        """
        logger.info(f"Getting Service Definition source for: {srvd_name}")

        response = self.adapter.request(
            uri=f"/sap/bc/adt/ddic/srvd/sources/{srvd_name.lower()}/source/main",
            method="GET",
            params={"version": version},
            content_type="text/plain"
        )

        if response.status_code == 200:
            logger.info(f"Retrieved Service Definition source for {srvd_name} ({len(response.text)} characters)")
            return response.text
        else:
            error_msg = f"{response.status_code} - Failed to get Service Definition source for {srvd_name}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_odata_service_info(
        self,
        service_name: str
    ) -> Dict[str, Any]:
        """
        Get OData service information.

        Args:
            service_name: Name of the OData service

        Returns:
            Dictionary with OData service info:
            - entity_sets: List of entity sets
            - service_version: OData version
            - namespace: Service namespace

        Example:
            >>> service.get_odata_service_info("Z_SERVICE_0001")
            {
                "entity_sets": ["Entity1", "Entity2"],
                "service_version": "V2",
                ...
            }
        """
        logger.info(f"Getting OData service info for: {service_name}")

        response = self.adapter.request(
            uri=f"/sap/bc/adt/businessservices/odatav2/{service_name.lower()}",
            method="GET",
            params={}
        )

        if response.status_code == 200:
            return self._parse_odata_service_info(response.text)
        else:
            error_msg = f"{response.status_code} - Failed to get OData service info for {service_name}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # ============================================================================
    # METADATA EXTENSION (DDLX)
    # ============================================================================

    def get_metadata_extension(
        self,
        ddlx_name: str,
        version: str = "active"
    ) -> Dict[str, Any]:
        """
        Get Metadata Extension (DDLX) metadata.

        Args:
            ddlx_name: Name of the Metadata Extension
            version: Version ('active' or 'inactive')

        Returns:
            Dictionary with Metadata Extension info:
            - name: DDLX name
            - annotated_view: CDS view being annotated
            - layer: Extension layer
            - annotations: UI annotations

        Example:
            >>> service.get_metadata_extension("ZC_RAP_ZTCXR1003_1")
            {
                "name": "ZC_RAP_ZTCXR1003_1",
                "annotated_view": "ZC_RAP_ZTCXR1003_1",
                ...
            }
        """
        logger.info(f"Getting Metadata Extension for: {ddlx_name}")

        response = self.adapter.request(
            uri=f"/sap/bc/adt/ddic/ddlx/sources/{ddlx_name.lower()}",
            method="GET",
            params={"version": version},
            content_type="application/vnd.sap.adt.ddic.ddlx.v1+xml"
        )

        if response.status_code == 200:
            return self._parse_metadata_extension(response.text)
        else:
            error_msg = f"{response.status_code} - Failed to get Metadata Extension for {ddlx_name}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_ddlx_parser_info(self) -> Dict[str, Any]:
        """
        Get DDLX parser information and annotation definitions.

        Returns:
            Dictionary with DDLX parser info and available annotations

        Example:
            >>> service.get_ddlx_parser_info()
            {
                "annotation_definitions": "...",
                "dfa_file": "...",
                ...
            }
        """
        logger.info("Getting DDLX parser info")

        response = self.adapter.request(
            uri="/sap/bc/adt/ddic/ddlx/parser/info",
            method="GET",
            params={},
            content_type="application/vnd.sap.adt.ddlx.parserinfo.v3+xml"
        )

        if response.status_code == 200:
            return self._parse_ddlx_parser_info(response.text)
        else:
            error_msg = f"{response.status_code} - Failed to get DDLX parser info"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # ============================================================================
    # BEHAVIOR DEFINITION (BDEF)
    # ============================================================================

    def get_behavior_definition(
        self,
        bdef_name: str,
        version: str = "active"
    ) -> str:
        """
        Get Behavior Definition source code.

        Args:
            bdef_name: Name of the Behavior Definition
            version: Version ('active' or 'inactive')

        Returns:
            Behavior Definition source code

        Example:
            >>> service.get_behavior_definition("ZI_RAP_ZTCXR1003_1")
            "managed implementation in class zbp_i_rap_ztcxr1003_1 unique;

             define behavior for ZI_RAP_ZTCXR1003_1
             {
               create;
               update;
               delete;
             }"
        """
        logger.info(f"Getting Behavior Definition for: {bdef_name}")

        response = self.adapter.request(
            uri=f"/sap/bc/adt/bo/behaviordefinitions/{bdef_name.lower()}",
            method="GET",
            params={"version": version},
            content_type="application/vnd.sap.adt.blues.v1+xml"
        )

        if response.status_code == 200:
            logger.info(f"Retrieved Behavior Definition for {bdef_name} ({len(response.text)} characters)")
            return response.text
        else:
            error_msg = f"{response.status_code} - Failed to get Behavior Definition for {bdef_name}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # ============================================================================
    # RAP EXPLORER - INTELLIGENT COMPONENT LOADING
    # ============================================================================

    def explore_rap_object(
        self,
        object_name: str,
        object_type: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Explore a RAP object and load all related components.

        This is the intelligent RAP explorer that detects the object type
        and recursively loads all related RAP components.

        Args:
            object_name: Name of the RAP object
            object_type: Type hint (SRVB, SRVD, CDS, DDLX, BDEF) - optional

        Returns:
            Dictionary with complete RAP structure:
            - root_object: Original object info
            - service_binding: Service Binding (if applicable)
            - service_definition: Service Definition (if applicable)
            - cds_views: Related CDS views
            - metadata_extensions: Related Metadata Extensions
            - behavior_definitions: Related Behavior Definitions
            - relationships: Map of object relationships

        Example:
            >>> service.explore_rap_object("Z_SERVICE_UI")
            {
                "root_object": {"name": "Z_SERVICE_UI", "type": "SRVB"},
                "service_binding": {...},
                "service_definition": {...},
                "cds_views": [...],
                "metadata_extensions": [...],
                "behavior_definitions": [...],
                "relationships": {...}
            }
        """
        logger.info(f"Exploring RAP object: {object_name} (type: {object_type or 'auto-detect'})")

        result = {
            "root_object": {"name": object_name, "type": object_type},
            "service_binding": None,
            "service_definition": None,
            "cds_views": [],
            "metadata_extensions": [],
            "behavior_definitions": [],
            "relationships": {}
        }

        # TODO: Implement intelligent detection and recursive loading
        # This is a complex method that requires:
        # 1. Auto-detect object type if not provided
        # 2. Load the root object
        # 3. Parse relationships from metadata
        # 4. Recursively load related components
        # 5. Build relationship map

        logger.warning("RAP Explorer implementation pending - requires complex relationship parsing")

        return result

    # ============================================================================
    # PARSING METHODS
    # ============================================================================

    def _parse_service_binding(self, xml_body: str) -> Dict[str, Any]:
        """Parse Service Binding XML."""
        try:
            root = ET.fromstring(xml_body)
            # TODO: Implement proper XML parsing for Service Binding
            return {"raw_xml": xml_body[:500]}
        except Exception as e:
            logger.error(f"Failed to parse Service Binding: {e}")
            return {"error": str(e)}

    def _parse_service_definition_metadata(self, xml_body: str) -> Dict[str, Any]:
        """Parse Service Definition metadata XML."""
        try:
            root = ET.fromstring(xml_body)
            # TODO: Implement proper XML parsing for Service Definition
            return {"raw_xml": xml_body[:500]}
        except Exception as e:
            logger.error(f"Failed to parse Service Definition: {e}")
            return {"error": str(e)}

    def _parse_odata_service_info(self, xml_body: str) -> Dict[str, Any]:
        """Parse OData service info XML."""
        try:
            root = ET.fromstring(xml_body)
            # TODO: Implement proper XML parsing for OData info
            return {"raw_xml": xml_body[:500]}
        except Exception as e:
            logger.error(f"Failed to parse OData service info: {e}")
            return {"error": str(e)}

    def _parse_metadata_extension(self, xml_body: str) -> Dict[str, Any]:
        """Parse Metadata Extension XML."""
        try:
            root = ET.fromstring(xml_body)
            # TODO: Implement proper XML parsing for Metadata Extension
            return {"raw_xml": xml_body[:500]}
        except Exception as e:
            logger.error(f"Failed to parse Metadata Extension: {e}")
            return {"error": str(e)}

    def _parse_ddlx_parser_info(self, xml_body: str) -> Dict[str, Any]:
        """Parse DDLX parser info XML."""
        try:
            root = ET.fromstring(xml_body)
            # TODO: Implement proper XML parsing for DDLX parser info
            return {"raw_xml": xml_body[:500]}
        except Exception as e:
            logger.error(f"Failed to parse DDLX parser info: {e}")
            return {"error": str(e)}
