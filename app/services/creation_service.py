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

    def create_function_group(
        self,
        function_group_name: str,
        package: str,
        description: str,
        transport: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Create a new ABAP function group.

        Args:
            function_group_name: Name of the function group (e.g., 'ZFIAAC002_1')
            package: Package name (e.g., 'ZFI' for transportable)
            description: Function group description
            transport: Transport number (required for transportable packages)

        Returns:
            Dictionary with creation result

        Example:
            >>> result = service.create_function_group(
            ...     "ZFIAAC002_1",
            ...     "ZFI",
            ...     "Function Group for AAC002",
            ...     "CADK911140"
            ... )
            >>> print(result)
            {"success": True, "uri": "/sap/bc/adt/functions/groups/zfiaac002_1"}
        """
        logger.info(f"Creating function group: {function_group_name}")

        # Step 1: Validate function group name
        logger.debug(f"Step 1: Validating function group name")
        with self._get_adapter() as adapter:
            validation_response = adapter.request(
                uri="/sap/bc/adt/functions/validation",
                method="POST",
                params={
                    "objname": function_group_name,
                    "packagename": package,
                    "description": description,
                    "objtype": "FUGR/F"
                },
                body="",
                content_type="application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.StatusMessage"
            )

        if validation_response.status_code not in [200, 201]:
            error_msg = f"Validation failed: {validation_response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{validation_response.text}")

        # Parse validation response
        validation_result = self._parse_validation_status(validation_response.text)
        if validation_result.get("severity") != "OK":
            error_msg = f"Validation failed: {validation_result.get('short_text', 'Unknown error')}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{validation_result.get('long_text', '')}")

        logger.info(f"Validation successful")

        # Step 2: Check registration (optional, may not be required in all systems)
        logger.debug(f"Step 2: Checking object registration")
        fg_uri = f"/sap/bc/adt/functions/groups/{function_group_name.lower()}"

        with self._get_adapter() as adapter:
            registration_response = adapter.request(
                uri="/sap/bc/adt/sscr/registration/objects",
                method="GET",
                params={"uri": fg_uri},
                body="",
                content_type="application/vnd.sap.adt.registration+xml"
            )

        # Registration check is informational - we continue even if it fails
        if registration_response.status_code in [200, 201]:
            logger.debug(f"Registration check completed")
        else:
            logger.warning(f"Registration check returned: {registration_response.status_code}")

        # Step 3: Create the function group
        logger.debug(f"Step 3: Creating function group with transport {transport}")

        # Build creation XML
        body = self._build_create_function_group_xml(
            function_group_name,
            package,
            description
        )

        params = {}
        if transport:
            params["corrNr"] = transport

        with self._get_adapter() as adapter:
            create_response = adapter.request(
                uri="/sap/bc/adt/functions/groups",
                method="POST",
                params=params,
                body=body,
                content_type="application/vnd.sap.adt.functions.groups.v2+xml"
            )

        if create_response.status_code in [200, 201]:
            logger.info(f"Function group created successfully: {fg_uri}")
            return {
                "success": True,
                "uri": fg_uri,
                "name": function_group_name,
                "package": package,
                "transport": transport
            }
        else:
            error_msg = f"Failed to create function group: {create_response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{create_response.text}")

    def create_function_module(
        self,
        function_module_name: str,
        function_group_name: str,
        package: str,
        description: str,
        transport: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Create a new ABAP function module within an existing function group.

        Args:
            function_module_name: Name of the function module (e.g., 'ZFIAAC002_DMEE_NRO_TRASLADO_DAV')
            function_group_name: Name of the parent function group (e.g., 'ZFIAAC002_1')
            package: Package name (e.g., 'ZFI')
            description: Function module description
            transport: Transport number (required for transportable packages)

        Returns:
            Dictionary with creation result

        Example:
            >>> result = service.create_function_module(
            ...     "ZFIAAC002_DMEE_NRO_TRASLADO_DAV",
            ...     "ZFIAAC002_1",
            ...     "ZFI",
            ...     "DMEE number transfer",
            ...     "CADK911140"
            ... )
            >>> print(result)
            {"success": True, "uri": "/sap/bc/adt/functions/groups/zfiaac002_1/fmodules/zfiaac002_dmee_nro_traslado_dav"}
        """
        logger.info(f"Creating function module: {function_module_name} in group {function_group_name}")

        # Step 1: Validate function module name
        logger.debug(f"Step 1: Validating function module name")
        with self._get_adapter() as adapter:
            validation_response = adapter.request(
                uri="/sap/bc/adt/functions/validation",
                method="POST",
                params={
                    "objtype": "FUGR/FF",
                    "objname": function_module_name,
                    "fugrname": function_group_name,
                    "description": description
                },
                body="",
                content_type="application/vnd.sap.as+xml;charset=UTF-8;dataname=com.sap.adt.StatusMessage"
            )

        if validation_response.status_code not in [200, 201]:
            error_msg = f"Validation failed: {validation_response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{validation_response.text}")

        # Parse validation response
        validation_result = self._parse_validation_status(validation_response.text)
        # INFO severity is acceptable for function modules (SAP namespace warning)
        if validation_result.get("severity") not in ["OK", "INFO"]:
            error_msg = f"Validation failed: {validation_result.get('short_text', 'Unknown error')}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{validation_result.get('long_text', '')}")

        logger.info(f"Validation successful (severity: {validation_result.get('severity')})")

        # Step 2: Check registration (optional)
        logger.debug(f"Step 2: Checking object registration")
        fm_uri = f"/sap/bc/adt/functions/groups/{function_group_name.lower()}/fmodules/{function_module_name.lower()}"

        with self._get_adapter() as adapter:
            registration_response = adapter.request(
                uri="/sap/bc/adt/sscr/registration/objects",
                method="GET",
                params={"uri": fm_uri},
                body="",
                content_type="application/vnd.sap.adt.registration+xml"
            )

        # Registration check is informational - we continue even if it fails
        if registration_response.status_code in [200, 201]:
            logger.debug(f"Registration check completed")
        else:
            logger.warning(f"Registration check returned: {registration_response.status_code}")

        # Step 3: Transport check
        logger.debug(f"Step 3: Checking transport requirements")
        transport_check_body = self._build_transport_check_xml(
            package=package,
            uri=fm_uri,
            operation="I"  # Insert operation
        )

        with self._get_adapter() as adapter:
            transport_check_response = adapter.request(
                uri="/sap/bc/adt/cts/transportchecks",
                method="POST",
                params={},
                body=transport_check_body,
                content_type="application/vnd.sap.as+xml; charset=UTF-8; dataname=com.sap.adt.transport.service.checkData"
            )

        if transport_check_response.status_code not in [200, 201]:
            logger.warning(f"Transport check returned: {transport_check_response.status_code}")
            # Continue anyway - transport check is informational

        # Step 4: Create the function module
        logger.debug(f"Step 4: Creating function module with transport {transport}")

        # Build creation XML
        body = self._build_create_function_module_xml(
            function_module_name,
            function_group_name,
            description
        )

        params = {}
        if transport:
            params["corrNr"] = transport

        fg_lower = function_group_name.lower()
        create_uri = f"/sap/bc/adt/functions/groups/{fg_lower}/fmodules"

        with self._get_adapter() as adapter:
            create_response = adapter.request(
                uri=create_uri,
                method="POST",
                params=params,
                body=body,
                content_type="application/vnd.sap.adt.functions.fmodules.v2+xml"
            )

        if create_response.status_code in [200, 201]:
            logger.info(f"Function module created successfully: {fm_uri}")
            return {
                "success": True,
                "uri": fm_uri,
                "name": function_module_name,
                "function_group": function_group_name,
                "package": package,
                "transport": transport
            }
        else:
            error_msg = f"Failed to create function module: {create_response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{create_response.text}")

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

    def _build_create_function_group_xml(
        self,
        function_group_name: str,
        package: str,
        description: str
    ) -> str:
        """Build XML body for function group creation."""
        xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<group:abapFunctionGroup xmlns:group="http://www.sap.com/adt/functions/groups"
                         xmlns:adtcore="http://www.sap.com/adt/core"
                         adtcore:type="FUGR/F"
                         adtcore:description="{description}"
                         adtcore:name="{function_group_name}"
                         adtcore:packageName="{package}">
  <adtcore:packageRef adtcore:name="{package}"/>
</group:abapFunctionGroup>"""

        return xml

    def _parse_validation_status(self, xml_text: str) -> Dict[str, Any]:
        """Parse ADT validation status response."""
        try:
            root = ET.fromstring(xml_text)

            # Navigate to DATA element using namespace
            ns = {'asx': 'http://www.sap.com/abapxml'}

            # Find DATA element
            data_elem = root.find('.//DATA')
            if data_elem is None:
                return {
                    "severity": "ERROR",
                    "short_text": "Could not parse validation response",
                    "long_text": xml_text
                }

            severity = data_elem.find('SEVERITY')
            short_text = data_elem.find('SHORT_TEXT')
            long_text = data_elem.find('LONG_TEXT')

            return {
                "severity": severity.text if severity is not None and severity.text else "ERROR",
                "short_text": short_text.text if short_text is not None and short_text.text else "",
                "long_text": long_text.text if long_text is not None and long_text.text else ""
            }

        except ET.ParseError as e:
            logger.error(f"Failed to parse validation status XML: {e}")
            return {
                "severity": "ERROR",
                "short_text": "XML parsing failed",
                "long_text": str(e)
            }

    def _build_create_function_module_xml(
        self,
        function_module_name: str,
        function_group_name: str,
        description: str
    ) -> str:
        """Build XML body for function module creation."""
        xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<fmodule:abapFunctionModule xmlns:fmodule="http://www.sap.com/adt/functions/fmodules"
                            xmlns:adtcore="http://www.sap.com/adt/core"
                            adtcore:description="{description}"
                            adtcore:name="{function_module_name}"
                            adtcore:type="FUGR/FF">
  <adtcore:containerRef adtcore:name="{function_group_name}"
                        adtcore:type="FUGR/F"
                        adtcore:uri="/sap/bc/adt/functions/groups/{function_group_name.lower()}"/>
</fmodule:abapFunctionModule>"""

        return xml

    def _build_transport_check_xml(
        self,
        package: str,
        uri: str,
        operation: str = "I"
    ) -> str:
        """Build XML body for transport check request."""
        xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<asx:abap xmlns:asx="http://www.sap.com/abapxml" version="1.0">
  <asx:values>
    <DATA>
      <PGMID/>
      <OBJECT/>
      <OBJECTNAME/>
      <DEVCLASS>{package}</DEVCLASS>
      <SUPER_PACKAGE/>
      <OPERATION>{operation}</OPERATION>
      <URI>{uri}</URI>
    </DATA>
  </asx:values>
</asx:abap>"""

        return xml
