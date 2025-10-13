"""Service for ABAP code quality operations (syntax check, pretty printer)."""

import logging
import xml.etree.ElementTree as ET
import base64
from typing import List, Dict, Any, Optional

from app.core.rfc_adapter import RfcAdapter

logger = logging.getLogger(__name__)


class CodeQualityService:
    """
    Service for code quality operations.

    This service provides tools to:
    - Syntax check ABAP code
    - Pretty print (format) ABAP code
    - Get and set pretty printer settings
    """

    def __init__(self, adapter: RfcAdapter):
        """
        Initialize the code quality service.

        Args:
            adapter: RfcAdapter instance for ADT API calls to SAP system
        """
        self.adapter = adapter
        logger.debug("CodeQualityService initialized")

    # Sprint 5.1: Syntax Check

    def syntax_check(
        self,
        object_uri: str,
        include_uri: str,
        source: str,
        version: str = "active"
    ) -> List[Dict[str, Any]]:
        """
        Perform syntax check on ABAP source code.

        Args:
            object_uri: URI of the object (e.g., '/sap/bc/adt/oo/classes/ztest')
            include_uri: URI of the include (e.g., '/sap/bc/adt/oo/classes/ztest/source/main')
            source: Source code to check
            version: "active" or "inactive"

        Returns:
            List of syntax check messages (errors, warnings, info)

        Example:
            >>> messages = service.syntax_check(
            ...     "/sap/bc/adt/oo/classes/ztest",
            ...     "/sap/bc/adt/oo/classes/ztest/source/main",
            ...     "CLASS ztest DEFINITION..."
            ... )
            >>> for msg in messages:
            ...     print(f"[{msg['type']}] Line {msg['line']}: {msg['text']}")
        """
        logger.info(f"Running syntax check on: {object_uri}")

        # Build XML body with base64-encoded source
        body = self._build_syntax_check_xml(object_uri, include_uri, source, version)

        response = self.adapter.request(
            uri="/sap/bc/adt/checkruns",
            method="POST",
            params={"reporters": "abapCheckRun"},
            body=body,
            content_type="application/vnd.sap.adt.checkobjects+xml"
        )

        if response.status_code == 200:
            messages = self._parse_syntax_check_result(response.text)
            logger.info(f"Syntax check completed. Found {len(messages)} messages")
            return messages
        else:
            error_msg = f"Failed to run syntax check: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # Sprint 5.2: Pretty Printer

    def prettyprint(self, source: str) -> str:
        """
        Format ABAP source code using SAP pretty printer.

        Args:
            source: Unformatted ABAP source code

        Returns:
            Formatted ABAP source code

        Example:
            >>> formatted = service.prettyprint("data: lv_var type string.")
            >>> print(formatted)
            DATA: lv_var TYPE string.
        """
        logger.info("Running pretty printer on source code")

        response = self.adapter.request(
            uri="/sap/bc/adt/abapsource/prettyprinter",
            method="POST",
            params={},
            body=source,
            content_type="text/plain"
        )

        if response.status_code == 200:
            formatted_source = response.text
            logger.info(f"Pretty print completed. Original: {len(source)} chars, Formatted: {len(formatted_source)} chars")
            return formatted_source
        else:
            error_msg = f"Failed to pretty print: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_prettyprint_settings(self) -> Dict[str, Any]:
        """
        Get current pretty printer settings for the user.

        Returns:
            Dictionary with settings (indentation, style, etc.)

        Example:
            >>> settings = service.get_prettyprint_settings()
            >>> print(settings)
            {"indentation": True, "style": "keywordUpper"}
        """
        logger.info("Getting pretty printer settings")

        response = self.adapter.request(
            uri="/sap/bc/adt/abapsource/prettyprinter/settings",
            method="GET",
            params={},
            body=""
        )

        if response.status_code == 200:
            settings = self._parse_prettyprint_settings(response.text)
            logger.info("Retrieved pretty printer settings")
            return settings
        else:
            error_msg = f"Failed to get pretty printer settings: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def set_prettyprint_settings(
        self,
        indent: bool = True,
        style: str = "keywordUpper"
    ) -> bool:
        """
        Set pretty printer settings for the user.

        Args:
            indent: Enable indentation (default: True)
            style: Format style (e.g., "keywordUpper", "keywordLower")

        Returns:
            True if successful

        Example:
            >>> service.set_prettyprint_settings(indent=True, style="keywordUpper")
            True
        """
        logger.info(f"Setting pretty printer settings: indent={indent}, style={style}")

        # Build XML body for settings
        body = self._build_prettyprint_settings_xml(indent, style)

        response = self.adapter.request(
            uri="/sap/bc/adt/abapsource/prettyprinter/settings",
            method="PUT",
            params={},
            body=body,
            content_type="application/xml"
        )

        if response.status_code in [200, 204]:
            logger.info("Pretty printer settings updated successfully")
            return True
        else:
            error_msg = f"Failed to set pretty printer settings: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # Private helper methods

    def _build_syntax_check_xml(
        self,
        object_uri: str,
        include_uri: str,
        source: str,
        version: str
    ) -> str:
        """Build XML body for syntax check request."""
        # Encode source as base64
        source_bytes = source.encode('utf-8')
        source_base64 = base64.b64encode(source_bytes).decode('ascii')

        xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<chkrun:checkObjectList xmlns:chkrun="http://www.sap.com/adt/checkrun" xmlns:adtcore="http://www.sap.com/adt/core">
  <chkrun:checkObject adtcore:uri="{object_uri}" chkrun:version="{version}">
    <chkrun:artifacts>
      <chkrun:artifact chkrun:contentType="text/plain; charset=utf-8" chkrun:uri="{include_uri}">
        <chkrun:content>{source_base64}</chkrun:content>
      </chkrun:artifact>
    </chkrun:artifacts>
  </chkrun:checkObject>
</chkrun:checkObjectList>"""

        return xml

    def _parse_syntax_check_result(self, xml_text: str) -> List[Dict[str, Any]]:
        """Parse syntax check result XML."""
        try:
            root = ET.fromstring(xml_text)

            # Namespaces
            ns = {
                'chkrun': 'http://www.sap.com/adt/checkrun',
                'adtcore': 'http://www.sap.com/adt/core'
            }

            messages = []

            # Find all messages
            for msg_elem in root.findall('.//chkrun:message', ns):
                message = {
                    'type': msg_elem.get('type', 'info'),  # error, warning, info
                    'text': msg_elem.text or '',
                    'line': msg_elem.get('line', ''),
                    'column': msg_elem.get('column', ''),
                    'offset': msg_elem.get('offset', ''),
                    'uri': msg_elem.get('uri', '')
                }
                messages.append(message)

            return messages

        except ET.ParseError as e:
            logger.error(f"Failed to parse syntax check result XML: {e}")
            return []

    def _parse_prettyprint_settings(self, xml_text: str) -> Dict[str, Any]:
        """Parse pretty printer settings XML."""
        try:
            root = ET.fromstring(xml_text)

            settings = {
                'indentation': root.findtext('.//indentation', 'true') == 'true',
                'style': root.findtext('.//style', 'keywordUpper'),
                'raw_xml': xml_text
            }

            return settings

        except ET.ParseError as e:
            logger.error(f"Failed to parse pretty printer settings XML: {e}")
            return {'raw_xml': xml_text, 'error': str(e)}

    def _build_prettyprint_settings_xml(self, indent: bool, style: str) -> str:
        """Build XML body for pretty printer settings."""
        xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <indentation>{str(indent).lower()}</indentation>
  <style>{style}</style>
</settings>"""

        return xml
