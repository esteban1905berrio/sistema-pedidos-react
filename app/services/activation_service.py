"""Service for ABAP object activation operations."""

import logging
import xml.etree.ElementTree as ET
from typing import List, Dict, Any, Optional

from app.core.rfc_adapter import RfcAdapter

logger = logging.getLogger(__name__)


class ActivationService:
    """
    Service for activating ABAP objects.

    This service provides tools to:
    - Activate single objects
    - Activate multiple objects in batch
    - Get list of inactive objects
    """

    def __init__(self, adapter: RfcAdapter):
        """
        Initialize the activation service.

        Args:
            adapter: RfcAdapter instance for ADT API calls to SAP system
        """
        self.adapter = adapter
        logger.debug("ActivationService initialized")

    # Sprint 4.3: Activation

    def activate(
        self,
        object_name: str,
        object_uri: str,
        preaudit: bool = True
    ) -> Dict[str, Any]:
        """
        Activate a single ABAP object.

        Args:
            object_name: Name of the object (e.g., 'ZTEST_CLASS')
            object_uri: URI of the object (e.g., '/sap/bc/adt/oo/classes/ztest')
            preaudit: Request preaudit check before activation (default: True)

        Returns:
            Dictionary with activation results:
            {
                "success": True/False,
                "activation_executed": True/False,
                "generation_executed": True/False,
                "messages": [...],
                "raw_xml": "..."
            }

        Example:
            >>> service.activate("ZTEST_CLASS", "/sap/bc/adt/oo/classes/ztest")
            {"success": True, "activation_executed": True, ...}

        Raises:
            Exception: If activation request fails
        """
        logger.info(f"Activating object: {object_name} ({object_uri})")

        # Build XML body for activation
        body = self._build_activation_xml([{
            "name": object_name,
            "uri": object_uri
        }])

        params = {
            "method": "activate",
            "preauditRequested": "true" if preaudit else "false"
        }

        response = self.adapter.request(
            uri="/sap/bc/adt/activation",
            method="POST",
            params=params,
            body=body,
            content_type="application/vnd.sap.adt.activation+xml"
        )

        if response.status_code == 200:
            result = self._parse_activation_result(response.text)
            logger.info(f"Activation completed. Success: {result.get('success')}")
            return result
        else:
            error_msg = f"Failed to activate object: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def activate_objects(
        self,
        objects: List[Dict[str, str]],
        preaudit: bool = True
    ) -> Dict[str, Any]:
        """
        Activate multiple ABAP objects in batch.

        Args:
            objects: List of objects to activate, each with 'name' and 'uri'
                Example: [
                    {"name": "ZTEST1", "uri": "/sap/bc/adt/oo/classes/ztest1"},
                    {"name": "ZTEST2", "uri": "/sap/bc/adt/oo/classes/ztest2"}
                ]
            preaudit: Request preaudit check before activation (default: True)

        Returns:
            Dictionary with batch activation results

        Example:
            >>> service.activate_objects([
            ...     {"name": "ZTEST1", "uri": "/sap/bc/adt/oo/classes/ztest1"},
            ...     {"name": "ZTEST2", "uri": "/sap/bc/adt/oo/classes/ztest2"}
            ... ])
            {"success": True, "activated": 2, ...}

        Raises:
            Exception: If activation request fails
        """
        logger.info(f"Activating {len(objects)} objects in batch")

        # Build XML body for batch activation
        body = self._build_activation_xml(objects)

        params = {
            "method": "activate",
            "preauditRequested": "true" if preaudit else "false"
        }

        response = self.adapter.request(
            uri="/sap/bc/adt/activation",
            method="POST",
            params=params,
            body=body,
            content_type="application/vnd.sap.adt.activation+xml"
        )

        if response.status_code == 200:
            result = self._parse_activation_result(response.text)
            logger.info(f"Batch activation completed. Success: {result.get('success')}")
            return result
        else:
            error_msg = f"Failed to activate objects: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_inactive_objects(self) -> List[Dict[str, Any]]:
        """
        Get list of inactive objects for current user.

        Returns:
            List of inactive objects with metadata

        Example:
            >>> service.get_inactive_objects()
            [
                {
                    "uri": "/sap/bc/adt/oo/classes/ztest",
                    "name": "ZTEST",
                    "type": "CLAS/OC",
                    "user": "USER01",
                    "date": "2025-01-10"
                },
                ...
            ]

        Raises:
            Exception: If request fails
        """
        logger.info("Getting inactive objects for current user")

        response = self.adapter.request(
            uri="/sap/bc/adt/activation/inactiveobjects",
            method="GET",
            params={},
            body=""
        )

        if response.status_code == 200:
            inactive_objects = self._parse_inactive_objects(response.text)
            logger.info(f"Found {len(inactive_objects)} inactive objects")
            return inactive_objects
        else:
            error_msg = f"Failed to get inactive objects: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # Private helper methods

    def _build_activation_xml(self, objects: List[Dict[str, str]]) -> str:
        """
        Build XML body for activation request.

        Args:
            objects: List of objects with 'name' and 'uri'

        Returns:
            XML string
        """
        xml_parts = [
            '<?xml version="1.0" encoding="UTF-8"?>',
            '<adtcore:objectReferences xmlns:adtcore="http://www.sap.com/adt/core">'
        ]

        for obj in objects:
            xml_parts.append(
                f'  <adtcore:objectReference '
                f'adtcore:uri="{obj["uri"]}" '
                f'adtcore:name="{obj["name"]}"/>'
            )

        xml_parts.append('</adtcore:objectReferences>')

        return '\n'.join(xml_parts)

    def _parse_activation_result(self, xml_text: str) -> Dict[str, Any]:
        """
        Parse activation result XML.

        Args:
            xml_text: XML response from activation endpoint

        Returns:
            Dictionary with activation results
        """
        try:
            root = ET.fromstring(xml_text)

            # Namespaces
            ns = {
                'adtcore': 'http://www.sap.com/adt/core',
                'chkrun': 'http://www.sap.com/adt/checkrun'
            }

            # Check if activation was executed
            activation_executed = root.get("activationExecuted") == "true"
            generation_executed = root.get("generationExecuted") == "true"

            # Extract messages (errors, warnings, info)
            messages = []
            for msg_elem in root.findall(".//chkrun:message", ns):
                message = {
                    "type": msg_elem.get("type", "info"),
                    "text": msg_elem.text or "",
                    "uri": msg_elem.get("uri", ""),
                    "line": msg_elem.get("line", "")
                }
                messages.append(message)

            # Check for errors
            has_errors = any(msg["type"] == "error" for msg in messages)

            result = {
                "success": activation_executed and not has_errors,
                "activation_executed": activation_executed,
                "generation_executed": generation_executed,
                "messages": messages,
                "raw_xml": xml_text
            }

            return result

        except ET.ParseError as e:
            logger.error(f"Failed to parse activation result XML: {e}")
            return {
                "success": False,
                "error": str(e),
                "raw_xml": xml_text
            }

    def _parse_inactive_objects(self, xml_text: str) -> List[Dict[str, Any]]:
        """
        Parse inactive objects XML.

        Args:
            xml_text: XML response from inactiveobjects endpoint

        Returns:
            List of inactive objects
        """
        try:
            root = ET.fromstring(xml_text)

            # Namespaces
            ns = {'adtcore': 'http://www.sap.com/adt/core'}

            inactive_objects = []
            for ref_elem in root.findall(".//adtcore:objectReference", ns):
                obj = {
                    "uri": ref_elem.get(f"{{{ns['adtcore']}}}uri", ""),
                    "name": ref_elem.get(f"{{{ns['adtcore']}}}name", ""),
                    "type": ref_elem.get(f"{{{ns['adtcore']}}}type", ""),
                    "description": ref_elem.get(f"{{{ns['adtcore']}}}description", ""),
                    "package": ref_elem.get(f"{{{ns['adtcore']}}}package", "")
                }
                inactive_objects.append(obj)

            return inactive_objects

        except ET.ParseError as e:
            logger.error(f"Failed to parse inactive objects XML: {e}")
            return []
