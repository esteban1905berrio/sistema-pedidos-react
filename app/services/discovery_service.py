"""Service for ABAP repository discovery operations."""

import logging
import xml.etree.ElementTree as ET
from typing import List, Dict, Any

from app.core.rfc_adapter import RfcAdapter
from pyrfc import Connection

logger = logging.getLogger(__name__)


class DiscoveryService:
    """
    Service for discovering ABAP repository capabilities and object types.

    This service provides tools to discover what's available in the SAP system:
    - Object types supported
    - ADT features available
    - Feature details and capabilities
    """

    def __init__(self, connection: Connection):
        """
        Initialize the discovery service.

        Args:
            connection: Active RFC connection to SAP system
        """
        self.adapter = RfcAdapter(connection)
        logger.debug("DiscoveryService initialized")

    def get_object_types(self) -> List[Dict[str, Any]]:
        """
        Get list of all ABAP object types available in the repository.

        Returns:
            List of object type definitions with metadata

        Example:
            >>> service.get_object_types()
            [
                {
                    "type": "CLAS/OC",
                    "name": "Class (ABAP Objects)",
                    "category": "Source Code"
                },
                ...
            ]
        """
        logger.info("Getting object types from SAP system")

        response = self.adapter.request(
            uri="/sap/bc/adt/repository/typestructure",
            method="GET",
            params={},
            body=""
        )

        if response.status_code == 200:
            object_types = self._parse_object_types(response.text)
            logger.info(f"Retrieved {len(object_types)} object types")
            return object_types
        else:
            error_msg = f"Failed to get object types: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def adt_discovery(self) -> Dict[str, Any]:
        """
        Get ADT discovery information - capabilities available in the SAP system.

        Returns:
            Dictionary with ADT capabilities and endpoints

        Example:
            >>> service.adt_discovery()
            {
                "version": "1.0",
                "features": ["transport", "activation", "syntax_check"],
                "endpoints": {...}
            }
        """
        logger.info("Getting ADT discovery information")

        response = self.adapter.request(
            uri="/sap/bc/adt/discovery",
            method="GET",
            params={},
            body=""
        )

        if response.status_code == 200:
            discovery_data = self._parse_adt_discovery(response.text)
            logger.info("ADT discovery information retrieved successfully")
            return discovery_data
        else:
            error_msg = f"Failed to get ADT discovery: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_feature_details(self, feature_name: str) -> Dict[str, Any]:
        """
        Get detailed information about a specific ADT feature.

        Args:
            feature_name: Name of the feature (e.g., "transportchecks", "activation")

        Returns:
            Dictionary with feature details and capabilities

        Example:
            >>> service.get_feature_details("activation")
            {
                "name": "activation",
                "version": "1.0",
                "supported_operations": ["activate", "preaudit"],
                ...
            }
        """
        logger.info(f"Getting details for feature: {feature_name}")

        response = self.adapter.request(
            uri=f"/sap/bc/adt/discovery/{feature_name}",
            method="GET",
            params={},
            body=""
        )

        if response.status_code == 200:
            feature_data = self._parse_feature_details(response.text)
            logger.info(f"Feature details for '{feature_name}' retrieved")
            return feature_data
        else:
            error_msg = f"Failed to get feature details for '{feature_name}': {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # Private parsing methods

    def _parse_object_types(self, xml_text: str) -> List[Dict[str, Any]]:
        """
        Parse object types XML response.

        Args:
            xml_text: XML response from SAP

        Returns:
            List of object type dictionaries
        """
        try:
            root = ET.fromstring(xml_text)
            object_types = []

            # ADT type structure namespace
            ns = {
                'adttype': 'http://www.sap.com/adt/typestructure',
                'adtcore': 'http://www.sap.com/adt/core'
            }

            # Find all object type elements
            for obj_type in root.findall('.//adttype:objectType', ns):
                type_info = {
                    'type': obj_type.get(f'{{{ns["adtcore"]}}}type', ''),
                    'name': obj_type.get(f'{{{ns["adtcore"]}}}name', ''),
                    'description': obj_type.get(f'{{{ns["adtcore"]}}}description', ''),
                    'uri': obj_type.get(f'{{{ns["adtcore"]}}}uri', ''),
                }
                object_types.append(type_info)

            return object_types

        except ET.ParseError as e:
            logger.error(f"Failed to parse object types XML: {e}")
            raise Exception(f"XML parsing error: {e}")

    def _parse_adt_discovery(self, xml_text: str) -> Dict[str, Any]:
        """
        Parse ADT discovery XML response.

        Args:
            xml_text: XML response from SAP

        Returns:
            Dictionary with discovery data
        """
        try:
            root = ET.fromstring(xml_text)

            # Basic discovery info
            discovery_data = {
                'version': root.get('version', 'unknown'),
                'features': [],
                'collections': [],
                'raw_xml': xml_text  # Keep raw XML for advanced usage
            }

            # Extract available features/collections
            ns = {'atom': 'http://www.w3.org/2005/Atom'}

            for collection in root.findall('.//atom:collection', ns):
                href = collection.get('href', '')
                title_elem = collection.find('atom:title', ns)
                title = title_elem.text if title_elem is not None else ''

                discovery_data['collections'].append({
                    'href': href,
                    'title': title
                })

            return discovery_data

        except ET.ParseError as e:
            logger.error(f"Failed to parse ADT discovery XML: {e}")
            raise Exception(f"XML parsing error: {e}")

    def _parse_feature_details(self, xml_text: str) -> Dict[str, Any]:
        """
        Parse feature details XML response.

        Args:
            xml_text: XML response from SAP

        Returns:
            Dictionary with feature details
        """
        try:
            root = ET.fromstring(xml_text)

            feature_data = {
                'name': root.tag,
                'attributes': dict(root.attrib),
                'content': {},
                'raw_xml': xml_text
            }

            # Extract nested elements
            for child in root:
                tag = child.tag.split('}')[-1] if '}' in child.tag else child.tag
                feature_data['content'][tag] = {
                    'text': child.text,
                    'attributes': dict(child.attrib)
                }

            return feature_data

        except ET.ParseError as e:
            logger.error(f"Failed to parse feature details XML: {e}")
            raise Exception(f"XML parsing error: {e}")
