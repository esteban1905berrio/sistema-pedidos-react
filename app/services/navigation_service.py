"""Service for ABAP repository navigation operations."""

import logging
import xml.etree.ElementTree as ET
from typing import List, Dict, Any

from app.core.rfc_adapter import RfcAdapter
from app.services.base_service import BaseService

logger = logging.getLogger(__name__)


class NavigationService(BaseService):
    """
    Service for navigating the ABAP repository tree structure.

    This service provides tools to navigate packages, folders, and object hierarchies.
    """

    def get_node_contents(self, node_uri: str, project_name: str = None) -> List[Dict[str, Any]]:
        """
        Get contents of a repository node (package, folder, etc.).

        Args:
            node_uri: URI of the node to explore
            project_name: Optional project name context

        Returns:
            List of objects/nodes contained in the specified node

        Example:
            >>> service.get_node_contents("/sap/bc/adt/packages/ZTEST")
            [
                {
                    "name": "ZCL_TEST_CLASS",
                    "type": "CLAS/OC",
                    "uri": "/sap/bc/adt/oo/classes/zcl_test_class",
                    "description": "Test class"
                },
                ...
            ]
        """
        logger.info(f"Getting node contents for: {node_uri}")

        params = {}
        if project_name:
            params['projectname'] = project_name

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri="/sap/bc/adt/repository/nodestructure",
                method="GET",
                params={**params, 'parent_uri': node_uri},
                body=""
            )

        if response.status_code == 200:
            contents = self._parse_node_contents(response.text)
            logger.info(f"Retrieved {len(contents)} items from node")
            return contents
        else:
            error_msg = f"Failed to get node contents: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def find_object_path(self, object_uri: str) -> Dict[str, Any]:
        """
        Find the complete path of an object in the repository tree.

        Args:
            object_uri: URI of the object

        Returns:
            Dictionary with object path information

        Example:
            >>> service.find_object_path("/sap/bc/adt/oo/classes/zcl_test")
            {
                "uri": "/sap/bc/adt/oo/classes/zcl_test",
                "path": ["$TMP", "Source Code Library", "Classes"],
                "parent_package": "$TMP",
                "full_path": "$TMP > Source Code Library > Classes > ZCL_TEST"
            }
        """
        logger.info(f"Finding object path for: {object_uri}")

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri="/sap/bc/adt/repository/nodestructure",
                method="GET",
                params={'uri': object_uri},
                body=""
            )

        if response.status_code == 200:
            path_data = self._parse_object_path(response.text, object_uri)
            logger.info(f"Object path found: {path_data.get('full_path', 'unknown')}")
            return path_data
        else:
            error_msg = f"Failed to find object path: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # Private parsing methods

    def _parse_node_contents(self, xml_text: str) -> List[Dict[str, Any]]:
        """
        Parse node contents XML response.

        Args:
            xml_text: XML response from SAP

        Returns:
            List of node content items
        """
        try:
            root = ET.fromstring(xml_text)
            contents = []

            # Namespaces for repository structure
            ns = {
                'repo': 'http://www.sap.com/adt/repository',
                'adtcore': 'http://www.sap.com/adt/core'
            }

            # Find all repository objects
            for obj in root.findall('.//repo:object', ns):
                item = {
                    'name': obj.get(f'{{{ns["adtcore"]}}}name', ''),
                    'type': obj.get(f'{{{ns["adtcore"]}}}type', ''),
                    'uri': obj.get(f'{{{ns["adtcore"]}}}uri', ''),
                    'description': obj.get(f'{{{ns["adtcore"]}}}description', ''),
                    'package': obj.get(f'{{{ns["adtcore"]}}}package', ''),
                }
                contents.append(item)

            # Also check for folder structures
            for folder in root.findall('.//repo:folder', ns):
                item = {
                    'name': folder.get(f'{{{ns["adtcore"]}}}name', ''),
                    'type': 'FOLDER',
                    'uri': folder.get(f'{{{ns["adtcore"]}}}uri', ''),
                    'description': folder.get(f'{{{ns["adtcore"]}}}description', ''),
                }
                contents.append(item)

            return contents

        except ET.ParseError as e:
            logger.error(f"Failed to parse node contents XML: {e}")
            raise Exception(f"XML parsing error: {e}")

    def _parse_object_path(self, xml_text: str, object_uri: str) -> Dict[str, Any]:
        """
        Parse object path XML response.

        Args:
            xml_text: XML response from SAP
            object_uri: Original object URI

        Returns:
            Dictionary with path information
        """
        try:
            root = ET.fromstring(xml_text)

            ns = {
                'repo': 'http://www.sap.com/adt/repository',
                'adtcore': 'http://www.sap.com/adt/core'
            }

            path_elements = []
            parent_package = None

            # Navigate through parent nodes
            for node in root.findall('.//repo:node', ns):
                name = node.get(f'{{{ns["adtcore"]}}}name', '')
                node_type = node.get(f'{{{ns["adtcore"]}}}type', '')

                path_elements.append(name)

                if node_type in ['DEVC/K', 'DEVC/L']:  # Package types
                    parent_package = name

            path_data = {
                'uri': object_uri,
                'path': path_elements,
                'parent_package': parent_package or '$TMP',
                'full_path': ' > '.join(path_elements) if path_elements else object_uri
            }

            return path_data

        except ET.ParseError as e:
            logger.error(f"Failed to parse object path XML: {e}")
            # Return minimal path data on parse error
            return {
                'uri': object_uri,
                'path': [],
                'parent_package': 'unknown',
                'full_path': object_uri,
                'error': str(e)
            }
