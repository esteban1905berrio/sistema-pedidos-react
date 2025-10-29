"""Service for ABAP interface operations."""

import logging
import xml.etree.ElementTree as et
from typing import Literal, Dict, Any, List

from app.core.rfc_adapter import RfcAdapter
from app.services.base_service import BaseService

logger = logging.getLogger(__name__)

# XML Namespaces for parsing ADT responses
XML_NAMESPACES = {
    "atom": "http://www.w3.org/2005/Atom",
    "adtcore": "http://www.sap.com/adt/core",
    "abapsource": "http://www.sap.com/adt/abapsource",
}


class InterfaceService(BaseService):
    """Service for ABAP interface operations via RFC."""

    def get_interface_structure(
        self,
        interface_name: str,
        version: Literal["active", "inactive"] = "active",
        with_short_descriptions: bool = True,
    ) -> Dict[str, Any]:
        """
        Get the structure of an ABAP interface (methods, components).

        Args:
            interface_name: Name of the interface (e.g., 'ZIFCXR1002_ALVGRID')
            version: Version to retrieve ('active' or 'inactive')
            with_short_descriptions: Include short descriptions in response

        Returns:
            Dictionary containing interface structure with methods and components
        """
        interface_name_upper = interface_name.upper()
        uri = f"/sap/bc/adt/oo/interfaces/{interface_name_upper.lower()}/objectstructure"
        params = {
            "version": version,
            "withShortDescriptions": "true" if with_short_descriptions else "false",
        }

        logger.info(f"Getting interface structure for {interface_name_upper}")

        try:
            with self._get_adapter() as adapter:
                response = adapter.request(uri=uri, method="GET", params=params, body="")

            # Parse XML response
            root = et.fromstring(response.body)

            result = {
                "interface_name": interface_name_upper,
                "type": root.attrib.get(
                    f"{{{XML_NAMESPACES['adtcore']}}}type", "INTF/OI"
                ),
                "methods": [],
                "links": [],
            }

            # Extract definition links
            for link in root.findall("atom:link", XML_NAMESPACES):
                rel = link.attrib.get("rel", "")
                href = link.attrib.get("href", "")
                link_type = link.attrib.get("type", "")

                if "definitionIdentifier" in rel or "definitionBlock" in rel:
                    result["links"].append(
                        {"rel": rel, "href": href, "type": link_type}
                    )

            # Extract methods
            for element in root.findall(
                "abapsource:objectStructureElement", XML_NAMESPACES
            ):
                method_type = element.attrib.get(
                    f"{{{XML_NAMESPACES['adtcore']}}}type", ""
                )
                method_name = element.attrib.get(
                    f"{{{XML_NAMESPACES['adtcore']}}}name", ""
                )
                level = element.attrib.get("level", "")
                visibility = element.attrib.get("visibility", "")

                if method_type == "INTF/IO":  # Interface method
                    method_info = {
                        "name": method_name,
                        "type": method_type,
                        "level": level,
                        "visibility": visibility,
                        "links": [],
                    }

                    # Extract method links
                    for link in element.findall("atom:link", XML_NAMESPACES):
                        method_info["links"].append(
                            {
                                "rel": link.attrib.get("rel", ""),
                                "href": link.attrib.get("href", ""),
                                "type": link.attrib.get("type", ""),
                            }
                        )

                    result["methods"].append(method_info)

            logger.info(
                f"Successfully retrieved structure for {interface_name_upper} with {len(result['methods'])} methods"
            )
            return result

        except Exception as e:
            logger.error(
                f"Failed to get interface structure for {interface_name_upper}: {str(e)}"
            )
            raise

    def get_interface_source(
        self,
        interface_name: str,
        version: Literal["active", "inactive"] = "active",
    ) -> str:
        """
        Get the source code of an ABAP interface.

        Args:
            interface_name: Name of the interface (e.g., 'ZIFCXR1002_ALVGRID')
            version: Version to retrieve ('active' or 'inactive')

        Returns:
            String containing the interface source code
        """
        interface_name_upper = interface_name.upper()
        uri = f"/sap/bc/adt/oo/interfaces/{interface_name_upper.lower()}/source/main"

        logger.info(
            f"Getting interface source for {interface_name_upper} (version: {version})"
        )

        try:
            with self._get_adapter() as adapter:
                response = adapter.request(uri=uri, method="GET", params={}, body="")

            source_code = response.body.strip()
            logger.info(
                f"Successfully retrieved source for {interface_name_upper} ({len(source_code)} characters)"
            )
            return source_code

        except Exception as e:
            logger.error(
                f"Failed to get interface source for {interface_name_upper}: {str(e)}"
            )
            raise

    def get_interface_includes(self, interface_name: str) -> List[Dict[str, str]]:
        """
        Get all includes of an ABAP interface.

        Args:
            interface_name: Name of the interface

        Returns:
            List of include dictionaries with type, uri, and metadata
        """
        interface_name_upper = interface_name.upper()
        uri = f"/sap/bc/adt/oo/interfaces/{interface_name_upper.lower()}/includes"

        logger.info(f"Getting includes for interface {interface_name_upper}")

        try:
            with self._get_adapter() as adapter:
                response = adapter.request(uri=uri, method="GET", params={}, body="")

            # Parse XML response
            root = et.fromstring(response.body)
            includes = []

            for include in root.findall("include", XML_NAMESPACES):
                include_info = {
                    "type": include.attrib.get("includeType", ""),
                    "uri": include.attrib.get("uri", ""),
                    "name": include.attrib.get("name", ""),
                }
                includes.append(include_info)

            logger.info(
                f"Successfully retrieved {len(includes)} includes for {interface_name_upper}"
            )
            return includes

        except Exception as e:
            logger.error(
                f"Failed to get includes for interface {interface_name_upper}: {str(e)}"
            )
            raise
