"""Service for ABAP object creation and deletion operations."""

import logging
import xml.etree.ElementTree as ET
from typing import Dict, Any, Optional

from app.core.rfc_adapter import RfcAdapter
from app.services.base_service import BaseService

logger = logging.getLogger(__name__)


class CreationService(BaseService):
    """
    Service for object lifecycle management (creation/deletion).

    This service provides tools to:
    - Create new ABAP objects (classes, programs, etc.)
    - Delete ABAP objects
    - Validate object names
    """

    def create_class(
        self,
        class_name: str,
        package: str,
        description: str,
        transport: Optional[str] = None,
        class_type: str = "CLAS"
    ) -> Dict[str, Any]:
        """
        Create a new ABAP class.

        Args:
            class_name: Name of the class (e.g., 'ZCL_TEST')
            package: Package name (e.g., '$TMP' for local, 'ZPACKAGE' for transportable)
            description: Class description
            transport: Transport number (required for transportable packages)
            class_type: Class type (default: 'CLAS')

        Returns:
            Dictionary with creation result

        Example:
            >>> result = service.create_class(
            ...     "ZCL_TEST",
            ...     "$TMP",
            ...     "Test Class"
            ... )
            >>> print(result)
            {"success": True, "uri": "/sap/bc/adt/oo/classes/zcl_test"}
        """
        logger.info(f"Creating class: {class_name}")

        # Build creation XML
        body = self._build_create_class_xml(class_name, package, description, class_type)

        params = {}
        if transport:
            params["corrNr"] = transport

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri="/sap/bc/adt/oo/classes",
                method="POST",
                params=params,
                body=body,
                content_type="application/vnd.sap.adt.oo.classes.v2+xml"
            )

        if response.status_code in [200, 201]:
            # Extract URI from response
            object_uri = self._extract_uri_from_response(response.text)
            logger.info(f"Class created successfully: {object_uri}")
            return {
                "success": True,
                "uri": object_uri,
                "name": class_name
            }
        else:
            error_msg = f"Failed to create class: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def delete_object(
        self,
        object_uri: str,
        transport: Optional[str] = None,
        delete_option: str = "deleteWithSuccessors"
    ) -> bool:
        """
        Delete an ABAP object.

        Args:
            object_uri: URI of the object to delete
            transport: Transport number (required for transportable packages)
            delete_option: Delete option (default: "deleteWithSuccessors")

        Returns:
            True if deletion successful

        Example:
            >>> service.delete_object(
            ...     "/sap/bc/adt/oo/classes/zcl_test",
            ...     transport="DEVK900123"
            ... )
            True

        IMPORTANT: Use with caution! This permanently deletes objects.
        """
        logger.info(f"Deleting object: {object_uri}")

        params = {"deleteOption": delete_option}
        if transport:
            params["corrNr"] = transport

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri=object_uri,
                method="DELETE",
                params=params,
                body=""
            )

        if response.status_code in [200, 204]:
            logger.info(f"Object deleted successfully: {object_uri}")
            return True
        else:
            error_msg = f"Failed to delete object: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def validate_object_name(
        self,
        object_name: str,
        object_type: str = "CLAS/OC"
    ) -> Dict[str, Any]:
        """
        Validate an object name according to SAP naming conventions.

        Args:
            object_name: Object name to validate
            object_type: Object type (e.g., "CLAS/OC", "PROG/P")

        Returns:
            Dictionary with validation result

        Example:
            >>> result = service.validate_object_name("ZCL_TEST", "CLAS/OC")
            >>> print(result)
            {"valid": True, "message": "Name is valid"}
        """
        logger.info(f"Validating object name: {object_name} (type: {object_type})")

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri="/sap/bc/adt/repository/validation/objectname",
                method="POST",
                params={
                    "objName": object_name,
                    "objType": object_type
                },
                body=""
            )

        if response.status_code == 200:
            result = self._parse_validation_result(response.text)
            logger.info(f"Validation result: {result}")
            return result
        else:
            error_msg = f"Failed to validate object name: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # Private helper methods

    def _build_create_class_xml(
        self,
        class_name: str,
        package: str,
        description: str,
        class_type: str
    ) -> str:
        """Build XML body for class creation."""
        xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<class:abapClass xmlns:class="http://www.sap.com/adt/oo/classes"
                 xmlns:adtcore="http://www.sap.com/adt/core"
                 adtcore:type="{class_type}"
                 adtcore:description="{description}"
                 adtcore:name="{class_name}"
                 adtcore:packageName="{package}">
  <adtcore:packageRef adtcore:name="{package}"/>
</class:abapClass>"""

        return xml

    def _extract_uri_from_response(self, xml_text: str) -> str:
        """Extract object URI from creation response."""
        try:
            # Try to parse as XML
            root = ET.fromstring(xml_text)

            # Look for URI attribute
            ns = {'adtcore': 'http://www.sap.com/adt/core'}
            uri = root.get(f"{{{ns['adtcore']}}}uri")

            if uri:
                return uri

            # Fallback: look for uri attribute without namespace
            uri = root.get("uri")
            if uri:
                return uri

        except ET.ParseError:
            logger.debug("Response is not XML, URI might be in plain text")

        # If XML parsing failed, return empty string
        return ""

    def _parse_validation_result(self, xml_text: str) -> Dict[str, Any]:
        """Parse validation result XML."""
        try:
            root = ET.fromstring(xml_text)

            # Check for validation messages
            valid = True
            messages = []

            for msg_elem in root.findall('.//*'):
                if msg_elem.text and len(msg_elem.text.strip()) > 0:
                    messages.append(msg_elem.text.strip())
                    if 'error' in msg_elem.tag.lower():
                        valid = False

            return {
                "valid": valid and len(messages) == 0,
                "messages": messages,
                "raw_xml": xml_text
            }

        except ET.ParseError:
            logger.error(f"Failed to parse validation result XML")
            return {
                "valid": False,
                "error": "Could not parse validation response",
                "raw_xml": xml_text
            }
