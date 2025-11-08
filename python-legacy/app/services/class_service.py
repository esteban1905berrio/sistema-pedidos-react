"""Service for ABAP class operations."""

import logging
import xml.etree.ElementTree as et
from typing import Literal, Dict, Any, List

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


class LinkInfo(Dict[str, str]):
    """Link information in class structure."""

    pass


class ComponentInfo(Dict[str, Any]):
    """Component information in class structure."""

    pass


class ClassStructureResult(Dict[str, Any]):
    """Class structure result."""

    pass


class ClassService(BaseService):
    """Service for ABAP class operations via RFC."""

    pass  # Inherits __init__ and _get_adapter from BaseService

    def get_class_source(
        self,
        class_name: str,
        version: Literal["active", "inactive"] = "active",
        include_type: str = "main",
    ) -> Dict[str, Any]:
        """
        Get the source code of an ABAP class with CHARACTER_LIMIT management.

        Args:
            class_name: Name of the ABAP class (e.g., 'ZCLCXR1002_UTIL')
            version: Version to retrieve ('active' or 'inactive')
            include_type: Type of include ('main', 'testclasses', 'macros', etc.)

        Returns:
            Dict with source code and metadata:
                - source: Source code (potentially truncated)
                - class_name: Class name
                - version: Version retrieved
                - include_type: Include type retrieved
                - metadata: Truncation and fragmentation info

        Raises:
            Exception: If the request fails
        """
        uri = f"/sap/bc/adt/oo/classes/{class_name}/source/{include_type}"
        params = {"version": version} if version else {}

        logger.info(f"Fetching source for class {class_name} ({version}, include: {include_type})")

        # Use connection pool - acquire connection per request
        with self._get_adapter() as adapter:
            response = adapter.request(
                uri=uri,
                method="GET",
                params=params,
                body="",
                content_type="text/plain",
            )

        if response.status_code == 200:
            logger.debug(f"Successfully retrieved source for {class_name}")
            source_code = response.text

            # Build result structure
            result = {
                'source': source_code,
                'class_name': class_name,
                'version': version,
                'include_type': include_type,
                'metadata': {}
            }

            # Check response size and truncate if needed
            suggestions = [
                f"Use get_class_includes('{class_name}') to see available includes",
                f"Retrieve specific includes: get_class_source('{class_name}', include_type='testclasses')",
                "Available include types: 'main', 'implementation', 'testclasses', 'macros'",
                f"Current include '{include_type}' is too large - try fragmenting by include type"
            ]

            result_data, truncation_info = self._check_and_truncate(result, suggestions)
            result_data['metadata']['truncation'] = truncation_info

            return result_data
        else:
            error_msg = f"{response.status_code} - Failed to get class source for {class_name}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def set_class_source(
        self,
        class_name: str,
        source_code: str,
        lock_handle: str,
        include_type: str = "main",
    ) -> bool:
        """
        Set the source code of an ABAP class.

        Args:
            class_name: Name of the ABAP class
            source_code: New source code
            lock_handle: Lock handle obtained from lock operation
            include_type: Type of include

        Returns:
            bool: True if successful

        Raises:
            Exception: If the request fails
        """
        uri = f"/sap/bc/adt/oo/classes/{class_name}/source/{include_type}"

        logger.info(f"Setting source for class {class_name}")

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri=uri,
                method="PUT",
                params={"lockHandle": lock_handle},
                body=source_code,
                content_type="text/plain; charset=utf-8",
            )

        if response.status_code == 200:
            logger.debug(f"Successfully set source for {class_name}")
            return True
        else:
            error_msg = f"{response.status_code} - Failed to set class source for {class_name}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_class_structure(
        self,
        class_name: str,
        version: Literal["active", "inactive"] = "active",
    ) -> ClassStructureResult:
        """
        Get the structure of an ABAP class.

        Retrieves metadata about the class including methods, attributes,
        visibility, and other components.

        Args:
            class_name: Name of the ABAP class
            version: Version to retrieve

        Returns:
            ClassStructureResult: Dictionary with class structure information

        Raises:
            Exception: If the request fails
        """
        uri = f"/sap/bc/adt/oo/classes/{class_name}/objectstructure"

        logger.info(f"Fetching structure for class {class_name}")

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri=uri,
                method="GET",
                params={"version": version, "withShortDescriptions": True},
                body="",
                content_type="application/*",
            )

        if 200 <= response.status_code < 300:
            structure = self._parse_class_structure(response.text)
            logger.debug(f"Successfully retrieved structure for {class_name}")
            return structure
        else:
            error_msg = f"{response.status_code} - Failed to get class structure for {class_name}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def _parse_class_structure(self, xml_content: str) -> ClassStructureResult:
        """
        Parse class structure XML response.

        Args:
            xml_content: XML response from ADT

        Returns:
            ClassStructureResult: Parsed class structure
        """
        root = et.fromstring(xml_content)
        all_attrs = root.attrib

        class_info: ClassStructureResult = {
            "name": "",
            "xml_base": "",
            "visibility": "",
            "final": False,
            "type": "",
            "links": [],
            "components": [],
        }

        # Map XML attributes to structure keys
        attr_mapping = {
            "{http://www.sap.com/adt/core}name": "name",
            "{http://www.w3.org/XML/1998/namespace}base": "xml_base",
            "visibility": "visibility",
            "{http://www.sap.com/adt/core}type": "type",
        }

        for xml_attr, struct_key in attr_mapping.items():
            if xml_attr in all_attrs:
                if struct_key == "final":
                    class_info[struct_key] = all_attrs[xml_attr] == "true"
                else:
                    class_info[struct_key] = all_attrs[xml_attr]

        if "final" in all_attrs:
            class_info["final"] = all_attrs["final"] == "true"

        # Parse links
        for link in root.findall("atom:link", XML_NAMESPACES):
            class_info["links"].append(
                {"rel": link.get("rel", ""), "href": link.get("href", "")}
            )

        # Parse components (methods, attributes, etc.)
        for element in root.findall(".//abapsource:objectStructureElement", XML_NAMESPACES):
            all_element_attrs = element.attrib

            element_info: ComponentInfo = {
                "name": "",
                "type": "",
                "links": [],
            }

            for attr_name, attr_value in all_element_attrs.items():
                if attr_value in ["true", "false"]:
                    element_info[attr_name] = attr_value == "true"
                else:
                    if attr_name in attr_mapping:
                        attr_name = attr_mapping[attr_name]
                    element_info[attr_name] = attr_value

            for link in element.findall("atom:link", XML_NAMESPACES):
                element_info["links"].append(
                    {"rel": link.get("rel", ""), "href": link.get("href", "")}
                )

            class_info["components"].append(element_info)

        return class_info

    def get_object_source(
        self, object_uri: str, version: Literal["active", "inactive"] = "active"
    ) -> str:
        """
        Get source code for any ABAP object by URI.

        Generic method that works with any ADT object URI.

        Args:
            object_uri: Full URI to the object (e.g., '/sap/bc/adt/oo/classes/ZTEST/source/main')
            version: Version to retrieve

        Returns:
            str: Source code

        Raises:
            Exception: If the request fails
        """
        params = {"version": version} if version else {}

        logger.info(f"Fetching source for URI: {object_uri}")

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri=object_uri, method="GET", params=params, body="", content_type="application/xml"
            )

        if response.status_code == 200:
            return response.text
        else:
            error_msg = f"{response.status_code} - Failed to get object source"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_class_includes(self, class_name: str) -> List[Dict[str, Any]]:
        """
        Get all includes of an ABAP class.

        This method queries each standard include type and returns only those that exist.

        Args:
            class_name: Name of the class

        Returns:
            List of include definitions with metadata

        Example:
            >>> service.get_class_includes("ZCL_TEST")
            [
                {
                    "include_type": "definitions",
                    "uri": "/sap/bc/adt/oo/classes/zcl_test/includes/definitions",
                    "exists": true
                },
                {
                    "include_type": "implementations",
                    "uri": "/sap/bc/adt/oo/classes/zcl_test/includes/implementations",
                    "exists": true
                },
                {
                    "include_type": "testclasses",
                    "uri": "/sap/bc/adt/oo/classes/zcl_test/includes/testclasses",
                    "exists": true
                }
            ]
        """
        logger.info(f"Getting includes for class: {class_name}")

        # Standard ABAP class include types
        include_types = ["definitions", "implementations", "testclasses", "macros"]

        includes = []
        class_name_lower = class_name.lower()

        with self._get_adapter() as adapter:
            for include_type in include_types:
                uri = f"/sap/bc/adt/oo/classes/{class_name_lower}/includes/{include_type}"

                try:
                    response = adapter.request(
                        uri=uri,
                        method="GET",
                        params={},
                        body="",
                        content_type="text/plain"
                    )

                    if response.status_code == 200:
                        includes.append({
                            "include_type": include_type,
                            "uri": uri,
                            "exists": True,
                            "size_bytes": len(response.text) if response.text else 0
                        })
                        logger.debug(f"Include '{include_type}' exists for class {class_name}")
                    elif response.status_code == 404:
                        logger.debug(f"Include '{include_type}' does not exist for class {class_name}")
                    else:
                        logger.warning(f"Unexpected status {response.status_code} for include '{include_type}'")

                except Exception as e:
                    logger.warning(f"Error checking include '{include_type}': {e}")
                    continue

        logger.info(f"Retrieved {len(includes)} includes for class {class_name}")
        return includes

    def get_class_components(self, class_name: str, version: Literal["active", "inactive"] = "active") -> Dict[str, Any]:
        """
        Get detailed component information for a class (methods, attributes, events, types).

        This is similar to get_class_structure but focuses specifically on class components.

        Args:
            class_name: Name of the class
            version: Version to retrieve

        Returns:
            Dictionary with categorized component information

        Example:
            >>> service.get_class_components("ZCL_TEST")
            {
                "methods": [...],
                "attributes": [...],
                "events": [...],
                "types": [...]
            }
        """
        logger.info(f"Getting components for class: {class_name}")

        # Use existing get_class_structure as base
        structure = self.get_class_structure(class_name, version)

        # Categorize components by type
        components = {
            "methods": [],
            "attributes": [],
            "events": [],
            "types": [],
            "other": []
        }

        for component in structure.get("components", []):
            comp_type = component.get("type", "").lower()

            if "method" in comp_type or comp_type in ["METH", "CMETHOD"]:
                components["methods"].append(component)
            elif "attr" in comp_type or comp_type in ["ATTR", "CATTR"]:
                components["attributes"].append(component)
            elif "event" in comp_type or comp_type in ["EVNT", "CEVNT"]:
                components["events"].append(component)
            elif "type" in comp_type or comp_type in ["TYPE", "CTYPE"]:
                components["types"].append(component)
            else:
                components["other"].append(component)

        logger.info(f"Found {len(components['methods'])} methods, "
                   f"{len(components['attributes'])} attributes, "
                   f"{len(components['events'])} events, "
                   f"{len(components['types'])} types")

        return components

    def get_object_structure(self, object_uri: str) -> Dict[str, Any]:
        """
        Get structure for any ABAP object (generic version, not class-specific).

        Args:
            object_uri: URI of the object

        Returns:
            Dictionary with object structure

        Example:
            >>> service.get_object_structure("/sap/bc/adt/oo/classes/zcl_test")
            {
                "name": "ZCL_TEST",
                "type": "CLAS/OC",
                "components": [...]
            }
        """
        logger.info(f"Getting object structure for URI: {object_uri}")

        # Add /objectstructure to the URI if not present
        structure_uri = object_uri
        if not structure_uri.endswith('/objectstructure'):
            structure_uri = f"{object_uri}/objectstructure"

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri=structure_uri,
                method="GET",
                params={},
                body=""
            )

        if response.status_code == 200:
            structure = self._parse_object_structure(response.text)
            logger.info(f"Retrieved object structure")
            return structure
        else:
            error_msg = f"{response.status_code} - Failed to get object structure"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")


    def _parse_object_structure(self, xml_text: str) -> Dict[str, Any]:
        """
        Parse generic object structure XML response.

        Args:
            xml_text: XML response from SAP

        Returns:
            Dictionary with object structure
        """
        try:
            root = et.fromstring(xml_text)

            # Use similar logic as _parse_class_structure but more generic
            structure = {
                "name": root.get(f'{{{XML_NAMESPACES["adtcore"]}}}name', ''),
                "type": root.get(f'{{{XML_NAMESPACES["adtcore"]}}}type', ''),
                "uri": root.get(f'{{{XML_NAMESPACES["adtcore"]}}}uri', ''),
                "description": root.get(f'{{{XML_NAMESPACES["adtcore"]}}}description', ''),
                "components": [],
                "links": []
            }

            # Parse links
            for link in root.findall("atom:link", XML_NAMESPACES):
                structure["links"].append({
                    "rel": link.get("rel", ""),
                    "href": link.get("href", "")
                })

            # Parse components/elements
            for element in root.findall(".//abapsource:objectStructureElement", XML_NAMESPACES):
                element_info = {
                    "name": element.get(f'{{{XML_NAMESPACES["adtcore"]}}}name', ''),
                    "type": element.get(f'{{{XML_NAMESPACES["adtcore"]}}}type', ''),
                    "uri": element.get(f'{{{XML_NAMESPACES["adtcore"]}}}uri', ''),
                    "description": element.get(f'{{{XML_NAMESPACES["adtcore"]}}}description', ''),
                    "links": []
                }

                # Parse element links
                for link in element.findall("atom:link", XML_NAMESPACES):
                    element_info["links"].append({
                        "rel": link.get("rel", ""),
                        "href": link.get("href", "")
                    })

                structure["components"].append(element_info)

            return structure

        except et.ParseError as e:
            logger.error(f"Failed to parse object structure XML: {e}")
            raise Exception(f"XML parsing error: {e}")
