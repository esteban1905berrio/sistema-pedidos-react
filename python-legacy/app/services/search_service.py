"""Service for ABAP object search operations."""

import logging
from xml.etree import ElementTree as et
from typing import List, Dict

from app.core.rfc_adapter import RfcAdapter
from app.services.base_service import BaseService

logger = logging.getLogger(__name__)

# XML Namespaces for parsing ADT responses
XML_NAMESPACES = {
    "chkl": "http://www.sap.com/abapxml/checklist",
    "atom": "http://www.w3.org/2005/Atom",
    "adtcore": "http://www.sap.com/adt/core",
    "exc": "http://www.sap.com/abapxml/types/communicationframework",
    "asx": "http://www.sap.com/abapxml",
    "aunit": "http://www.sap.com/adt/aunit",
    "chkrun": "http://www.sap.com/adt/checkrun",
    "abapsource": "http://www.sap.com/adt/abapsource",
}


class SearchService(BaseService):
    """Service for searching ABAP objects via RFC."""

    def search_objects(self, query: str, max_results: int = 10) -> List[Dict[str, str]]:
        """
        Search for ABAP objects using quick search.

        Args:
            query: Search query (object name pattern, e.g., 'Z*', 'ZTEST*')
            max_results: Maximum number of results to return

        Returns:
            List[Dict[str, str]]: List of matching objects with their attributes

        Raises:
            Exception: If the request fails

        Example:
            >>> service = SearchService(connection)
            >>> results = service.search_objects("ZTEST*", max_results=5)
            >>> for obj in results:
            ...     print(f"{obj['name']} - {obj['type']}")
        """
        uri = "/sap/bc/adt/repository/informationsystem/search"

        logger.info(f"Searching for objects matching: {query}")

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri=uri,
                method="GET",
                params={"operation": "quickSearch", "query": query, "maxResults": max_results},
                body="",
            )

        if response.status_code == 200:
            elements = self._parse_search_results(response.text)
            logger.debug(f"Found {len(elements)} objects matching {query}")
            return elements
        else:
            error_msg = f"{response.status_code} - Failed to search objects"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def _parse_search_results(self, xml_text: str) -> List[Dict[str, str]]:
        """
        Parse search results from XML response.

        Args:
            xml_text: XML response text

        Returns:
            List[Dict[str, str]]: List of object attributes
        """
        root = et.fromstring(xml_text)
        elements = root.findall("adtcore:objectReference", XML_NAMESPACES)

        processed_elements = []
        for element in elements:
            cleaned = self._element_to_dict(element)
            processed_elements.append(cleaned)

        return processed_elements

    def _element_to_dict(self, element: et.Element) -> Dict[str, str]:
        """
        Convert XML element attributes to dictionary with cleaned keys.

        Args:
            element: XML element

        Returns:
            Dict[str, str]: Dictionary of attributes with namespace stripped
        """
        cleaned = {}
        for key, value in element.attrib.items():
            # Strip namespace from attribute name
            clean_key = self._strip_namespace(key)
            cleaned[clean_key] = value
        return cleaned

    def _strip_namespace(self, name: str) -> str:
        """
        Strip namespace from XML tag or attribute name.

        Args:
            name: Tag or attribute name with namespace

        Returns:
            str: Name without namespace
        """
        if name.startswith("{"):
            return name.split("}", 1)[1]
        if ":" in name:
            return name.split(":", 1)[1]
        return name
