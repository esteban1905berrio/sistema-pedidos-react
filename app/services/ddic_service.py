"""Service for ABAP Data Dictionary (DDIC) operations."""

import logging
import xml.etree.ElementTree as ET
from typing import List, Dict, Any

from app.core.rfc_adapter import RfcAdapter
from pyrfc import Connection

logger = logging.getLogger(__name__)


class DdicService:
    """
    Service for accessing ABAP Data Dictionary (DDIC) information.

    This service provides tools to access dictionary objects like:
    - Tables and structures
    - Data elements and domains
    - Table types
    - CDS annotations
    - Package information
    """

    def __init__(self, connection: Connection):
        """
        Initialize the DDIC service.

        Args:
            connection: Active RFC connection to SAP system
        """
        self.adapter = RfcAdapter(connection)
        logger.debug("DdicService initialized")

    def get_ddic_element(self, element_name: str, element_type: str) -> Dict[str, Any]:
        """
        Get definition of a DDIC element (table, structure, data element, etc.).

        Args:
            element_name: Name of the DDIC element
            element_type: Type of element ('tables', 'structures', 'dataelements', 'domains', 'tableTypes')

        Returns:
            Dictionary with element definition and metadata

        Example:
            >>> service.get_ddic_element("USR02", "tables")
            {
                "name": "USR02",
                "type": "TABLE",
                "fields": [...],
                "description": "User Master Record"
            }
        """
        logger.info(f"Getting DDIC element: {element_name} (type: {element_type})")

        # Validate element type
        valid_types = ['tables', 'structures', 'dataelements', 'domains', 'tableTypes']
        if element_type not in valid_types:
            raise ValueError(f"Invalid element type: {element_type}. Must be one of {valid_types}")

        response = self.adapter.request(
            uri=f"/sap/bc/adt/ddic/{element_type}/{element_name.lower()}",
            method="GET",
            params={},
            body=""
        )

        if response.status_code == 200:
            element_data = self._parse_ddic_element(response.text, element_type)
            logger.info(f"Retrieved DDIC element: {element_name}")
            return element_data
        else:
            error_msg = f"Failed to get DDIC element '{element_name}': {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def ddic_repository_access(self, path: str) -> Dict[str, Any]:
        """
        Access DDIC repository by path.

        Args:
            path: Repository path to access

        Returns:
            Dictionary with repository data

        Example:
            >>> service.ddic_repository_access("/tables/usr02")
            {...}
        """
        logger.info(f"Accessing DDIC repository path: {path}")

        response = self.adapter.request(
            uri=f"/sap/bc/adt/ddic/repository/{path}",
            method="GET",
            params={},
            body=""
        )

        if response.status_code == 200:
            repo_data = self._parse_repository_data(response.text)
            logger.info(f"Retrieved DDIC repository data for path: {path}")
            return repo_data
        else:
            error_msg = f"Failed to access DDIC repository path '{path}': {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_annotation_definitions(self) -> List[Dict[str, Any]]:
        """
        Get available CDS annotation definitions.

        Returns:
            List of annotation definitions

        Example:
            >>> service.get_annotation_definitions()
            [
                {
                    "name": "AbapCatalog.sqlViewName",
                    "type": "STRING",
                    "description": "SQL view name for CDS view"
                },
                ...
            ]
        """
        logger.info("Getting CDS annotation definitions")

        response = self.adapter.request(
            uri="/sap/bc/adt/ddic/annotations",
            method="GET",
            params={},
            body=""
        )

        if response.status_code == 200:
            annotations = self._parse_annotations(response.text)
            logger.info(f"Retrieved {len(annotations)} annotation definitions")
            return annotations
        else:
            error_msg = f"Failed to get annotation definitions: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def package_search_help(self, query: str, max_results: int = 50) -> List[str]:
        """
        Search for packages (autocomplete/search help).

        Args:
            query: Search query (supports wildcards)
            max_results: Maximum number of results to return

        Returns:
            List of package names matching the query

        Example:
            >>> service.package_search_help("Z*")
            ["ZTEST", "ZTEST_UTILS", "ZPACKAGE", ...]
        """
        logger.info(f"Searching for packages: {query}")

        params = {
            'operation': 'quickSearch',
            'query': query,
            'maxResults': str(max_results)
        }

        response = self.adapter.request(
            uri="/sap/bc/adt/packages",
            method="GET",
            params=params,
            body=""
        )

        if response.status_code == 200:
            packages = self._parse_package_list(response.text)
            logger.info(f"Found {len(packages)} packages matching '{query}'")
            return packages
        else:
            error_msg = f"Failed to search packages: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # Private parsing methods

    def _parse_ddic_element(self, xml_text: str, element_type: str) -> Dict[str, Any]:
        """
        Parse DDIC element XML response.

        Args:
            xml_text: XML response from SAP
            element_type: Type of element being parsed

        Returns:
            Dictionary with element data
        """
        try:
            root = ET.fromstring(xml_text)

            # Common namespaces for DDIC
            ns = {
                'ddic': 'http://www.sap.com/adt/ddic',
                'adtcore': 'http://www.sap.com/adt/core',
                'abapsource': 'http://www.sap.com/adt/abapsource'
            }

            element_data = {
                'name': root.get(f'{{{ns["adtcore"]}}}name', ''),
                'type': root.get(f'{{{ns["adtcore"]}}}type', ''),
                'description': root.get(f'{{{ns["adtcore"]}}}description', ''),
                'element_type': element_type,
                'fields': [],
                'properties': {},
                'raw_xml': xml_text  # Keep raw XML for advanced usage
            }

            # Parse fields for tables/structures
            if element_type in ['tables', 'structures']:
                for field in root.findall('.//ddic:field', ns):
                    field_info = {
                        'name': field.get('name', ''),
                        'type': field.get('type', ''),
                        'length': field.get('length', ''),
                        'decimals': field.get('decimals', ''),
                        'description': field.get('description', ''),
                        'key': field.get('key', '') == 'true'
                    }
                    element_data['fields'].append(field_info)

            # Extract all attributes as properties
            for attr_name, attr_value in root.attrib.items():
                if attr_name not in [f'{{{ns["adtcore"]}}}name', f'{{{ns["adtcore"]}}}type']:
                    element_data['properties'][attr_name] = attr_value

            return element_data

        except ET.ParseError as e:
            logger.error(f"Failed to parse DDIC element XML: {e}")
            raise Exception(f"XML parsing error: {e}")

    def _parse_repository_data(self, xml_text: str) -> Dict[str, Any]:
        """
        Parse DDIC repository access XML response.

        Args:
            xml_text: XML response from SAP

        Returns:
            Dictionary with repository data
        """
        try:
            root = ET.fromstring(xml_text)

            repo_data = {
                'content': {},
                'raw_xml': xml_text
            }

            # Extract repository content
            for child in root:
                tag = child.tag.split('}')[-1] if '}' in child.tag else child.tag
                repo_data['content'][tag] = {
                    'text': child.text,
                    'attributes': dict(child.attrib),
                    'children': len(list(child))
                }

            return repo_data

        except ET.ParseError as e:
            logger.error(f"Failed to parse repository data XML: {e}")
            raise Exception(f"XML parsing error: {e}")

    def _parse_annotations(self, xml_text: str) -> List[Dict[str, Any]]:
        """
        Parse CDS annotations XML response.

        Args:
            xml_text: XML response from SAP

        Returns:
            List of annotation definitions
        """
        try:
            root = ET.fromstring(xml_text)
            annotations = []

            ns = {
                'ddic': 'http://www.sap.com/adt/ddic',
                'adtcore': 'http://www.sap.com/adt/core'
            }

            # Find all annotation elements
            for annotation in root.findall('.//ddic:annotation', ns):
                anno_info = {
                    'name': annotation.get(f'{{{ns["adtcore"]}}}name', ''),
                    'type': annotation.get('type', ''),
                    'description': annotation.get(f'{{{ns["adtcore"]}}}description', ''),
                    'scope': annotation.get('scope', ''),
                }
                annotations.append(anno_info)

            return annotations

        except ET.ParseError as e:
            logger.error(f"Failed to parse annotations XML: {e}")
            # Return empty list on parse error
            return []

    def _parse_package_list(self, xml_text: str) -> List[str]:
        """
        Parse package search results XML response.

        Args:
            xml_text: XML response from SAP

        Returns:
            List of package names
        """
        try:
            root = ET.fromstring(xml_text)
            packages = []

            ns = {
                'adtcore': 'http://www.sap.com/adt/core',
                'package': 'http://www.sap.com/adt/packages'
            }

            # Find all package elements
            for pkg in root.findall('.//package:package', ns):
                pkg_name = pkg.get(f'{{{ns["adtcore"]}}}name', '')
                if pkg_name:
                    packages.append(pkg_name)

            # Also try alternative structure
            if not packages:
                for pkg in root.findall('.//*[@name]'):
                    pkg_name = pkg.get('name', '')
                    if pkg_name:
                        packages.append(pkg_name)

            return list(set(packages))  # Remove duplicates

        except ET.ParseError as e:
            logger.error(f"Failed to parse package list XML: {e}")
            return []
