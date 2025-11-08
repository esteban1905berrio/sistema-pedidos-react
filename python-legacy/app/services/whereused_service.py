"""Service for where-used analysis operations."""

import logging
from typing import List, Dict, Any, Optional
from xml.etree import ElementTree as ET

from app.core.rfc_adapter import RfcAdapter
from app.services.base_service import BaseService

logger = logging.getLogger(__name__)


class WhereUsedService(BaseService):
    """Service for finding where ABAP objects are used."""

    def get_usage_references(
        self,
        object_uri: str,
        object_type: str = "DDLS"
    ) -> Dict[str, Any]:
        """
        Get usage references for an object (first step in where-used analysis).

        This method returns a list of objects that reference the given object,
        along with their object identifiers needed for get_usage_snippets.

        Args:
            object_uri: URI to the object (e.g., /sap/bc/adt/ddic/ddl/sources/ztfi1008_2/source/main?version=active)
            object_type: Type of object (DDLS for CDS views)

        Returns:
            Dictionary with:
            - total_references: Number of references found
            - referenced_object_id: Main object identifier
            - references: List of objects that use this object:
                - name: Object name
                - type: Object type
                - description: Description
                - uri: Object URI
                - package: Package name
                - object_identifier: Identifier for usage snippets call
                - can_have_children: Whether object has child references

        Example:
            >>> service.get_usage_references(
            ...     "/sap/bc/adt/ddic/ddl/sources/ztfi1008_2/source/main?version=active"
            ... )
            {
                "total_references": 3,
                "referenced_object_id": "ABAPFullName;\\TY:ZTFI1008_2",
                "references": [
                    {
                        "name": "ZIFII1008_1",
                        "type": "DDLS/DF",
                        "object_identifier": "DDLFullName;ZIFII1008_1;\\TY:ZTFI1008_2;2",
                        ...
                    }
                ]
            }
        """
        logger.info(f"Getting usage references for: {object_uri}")

        # Build XML request body
        body = self._build_usage_references_xml()

        # Build full URI with query parameters
        full_uri = f"/sap/bc/adt/repository/informationsystem/usageReferences?uri={object_uri}"

        # Make POST request
        with self._get_adapter() as adapter:
            response = adapter.request(
                uri=full_uri,
                method="POST",
                params={},
                body=body,
                headers={
                    "Accept": "application/vnd.sap.adt.repository.usagereferences.result.v1+xml"
                },
                content_type="application/vnd.sap.adt.repository.usagereferences.request.v1+xml"
            )

            # Log response for debugging
            logger.debug(f"Response status: {response.status_code}")
            logger.debug(f"Response XML (first 1000 chars): {response.text[:1000]}")

            # Parse XML response
            result = self._parse_usage_references_response(response.text)

            logger.info(
                f"Found {result.get('total_references', 0)} references for {object_uri}"
            )

            return result

    def get_usage_snippets(
        self,
        object_identifier: str,
        max_results: Optional[int] = None
    ) -> Dict[str, Any]:
        """
        Get usage snippets showing where an object is used.

        The object_identifier format is:
        ABAPFullName;package;program;\TY:classname;version

        Example:
            ABAPFullName;ZMMI1229_0;ZMMI1229_0C_1;\TY:ZCLMMI1229_SINCRONIZA_INV_MAWM;2

        Args:
            object_identifier: Full identifier of the object
            max_results: Maximum number of results to return (optional)

        Returns:
            Dictionary with:
            - object_identifier: The object being searched
            - total_usages: Total number of usages found
            - code_snippets: List of usage locations with:
                - uri: Location URI with line numbers
                - content: Code snippet showing usage
                - description: Context description
                - matches: Match details (position, access type, grade)

        Example:
            >>> service.get_usage_snippets(
            ...     "ABAPFullName;ZMMI1229_0;ZMMI1229_0C_1;\\TY:ZCLMMI1229_SINCRONIZA_INV_MAWM;2"
            ... )
            {
                "object_identifier": "ABAPFullName;...",
                "total_usages": 18,
                "code_snippets": [
                    {
                        "uri": "/sap/bc/adt/programs/includes/zmmi1229_0c_1/source/main?...",
                        "content": "zclmmi1229_sincroniza_inv_mawm=>gc_sobrante",
                        "description": "Various Usage Kinds...",
                        "matches": "43-73,accessUnknown,gradeDirect;..."
                    },
                    ...
                ]
            }
        """
        logger.info(f"Getting usage snippets for: {object_identifier}")

        # Build XML request body
        body = self._build_usage_snippets_xml(object_identifier)

        # Make POST request
        with self._get_adapter() as adapter:
            response = adapter.request(
                uri="/sap/bc/adt/repository/informationsystem/usageSnippets",
                method="POST",
                params={},
                body=body,
                headers={
                    "Accept": "application/vnd.sap.adt.repository.usagesnippets.result.v1+xml"
                },
                content_type="application/vnd.sap.adt.repository.usagesnippets.request.v1+xml"
            )

        # Log response for debugging
        logger.debug(f"Response status: {response.status_code}")
        logger.debug(f"Response body (first 1000 chars): {response.text[:1000]}")

        # Parse XML response
        result = self._parse_usage_snippets_response(response.text)

        # Apply max_results if specified
        if max_results and "code_snippets" in result:
            result["code_snippets"] = result["code_snippets"][:max_results]
            result["limited"] = True
            result["shown_usages"] = len(result["code_snippets"])

        logger.info(
            f"Found {result.get('total_usages', 0)} usages for {object_identifier}"
        )

        return result

    def _build_usage_snippets_xml(self, object_identifier: str) -> str:
        """
        Build XML request body for usage snippets.

        Args:
            object_identifier: Full object identifier

        Returns:
            XML string
        """
        xml_template = '''<?xml version="1.0" encoding="UTF-8"?>
<usagereferences:usageSnippetRequest xmlns:usagereferences="http://www.sap.com/adt/ris/usageReferences">
  <usagereferences:objectIdentifiers>
    <usagereferences:objectIdentifier optional="false">{object_id}</usagereferences:objectIdentifier>
  </usagereferences:objectIdentifiers>
  <usagereferences:affectedObjects/>
</usagereferences:usageSnippetRequest>'''

        return xml_template.format(object_id=object_identifier)

    def _parse_usage_snippets_response(self, xml_body: str) -> Dict[str, Any]:
        """
        Parse XML response from usage snippets endpoint.

        Args:
            xml_body: XML response body

        Returns:
            Dictionary with parsed results
        """
        try:
            root = ET.fromstring(xml_body)

            # Define namespace
            ns = {
                "usageReferences": "http://www.sap.com/adt/ris/usageReferences"
            }

            result = {
                "code_snippets": [],
                "total_usages": 0
            }

            # Find all codeSnippetObject elements
            snippet_objects = root.findall(
                ".//usageReferences:codeSnippetObject",
                ns
            )

            for snippet_obj in snippet_objects:
                # Get object identifier (no namespace)
                obj_id_elem = snippet_obj.find("objectIdentifier")
                if obj_id_elem is not None and obj_id_elem.text:
                    result["object_identifier"] = obj_id_elem.text

                # Get all code snippets
                snippets = snippet_obj.findall(
                    ".//usageReferences:codeSnippet",
                    ns
                )

                for snippet in snippets:
                    snippet_data = {
                        "uri": snippet.get("uri", ""),
                        "matches": snippet.get("matches", ""),
                        "content": "",
                        "description": ""
                    }

                    # Get content (no namespace)
                    content_elem = snippet.find("content")
                    if content_elem is not None and content_elem.text:
                        snippet_data["content"] = content_elem.text.strip()

                    # Get description (no namespace)
                    desc_elem = snippet.find("description")
                    if desc_elem is not None and desc_elem.text:
                        snippet_data["description"] = desc_elem.text.strip()

                    result["code_snippets"].append(snippet_data)

            result["total_usages"] = len(result["code_snippets"])

            return result

        except ET.ParseError as e:
            logger.error(f"Failed to parse usage snippets response: {e}")
            return {
                "error": f"XML parse error: {str(e)}",
                "total_usages": 0,
                "code_snippets": []
            }
        except Exception as e:
            logger.error(f"Error processing usage snippets: {e}")
            return {
                "error": f"Processing error: {str(e)}",
                "total_usages": 0,
                "code_snippets": []
            }

    def _build_usage_references_xml(self) -> str:
        """
        Build XML request body for usage references.

        Returns:
            XML string
        """
        xml_template = '''<?xml version="1.0" encoding="UTF-8"?>
<usagereferences:usageReferenceRequest xmlns:usagereferences="http://www.sap.com/adt/ris/usageReferences">
  <usagereferences:affectedObjects/>
</usagereferences:usageReferenceRequest>'''

        return xml_template

    def _parse_usage_references_response(self, xml_body: str) -> Dict[str, Any]:
        """
        Parse XML response from usage references endpoint.

        Args:
            xml_body: XML response body

        Returns:
            Dictionary with parsed results including:
            - total_references: Number of references found
            - referenced_object_id: Main object identifier
            - references: List of referencing objects with details
        """
        try:
            root = ET.fromstring(xml_body)

            # Define namespace
            ns = {
                "usageReferences": "http://www.sap.com/adt/ris/usageReferences",
                "adtcore": "http://www.sap.com/adt/core"
            }

            result = {
                "references": [],
                "total_references": 0,
                "referenced_object_id": ""
            }

            # Get numberOfResults attribute
            num_results = root.get("numberOfResults")
            if num_results:
                result["total_references"] = int(num_results)

            # Get referenced object identifier
            ref_obj_id = root.get("referencedObjectIdentifier")
            if ref_obj_id:
                result["referenced_object_id"] = ref_obj_id

            # Find all referencedObject elements
            ref_objects = root.findall(
                ".//usageReferences:referencedObject",
                ns
            )

            for ref_obj in ref_objects:
                ref_data = {
                    "uri": ref_obj.get("uri", ""),
                    "parent_uri": ref_obj.get("parentUri", ""),
                    "is_result": ref_obj.get("isResult", "false") == "true",
                    "can_have_children": ref_obj.get("canHaveChildren", "false") == "true",
                    "usage_information": ref_obj.get("usageInformation", "")
                }

                # Get ADT object info
                adt_obj = ref_obj.find("usageReferences:adtObject", ns)
                if adt_obj is not None:
                    ref_data["name"] = adt_obj.get("{http://www.sap.com/adt/core}name", "")
                    ref_data["type"] = adt_obj.get("{http://www.sap.com/adt/core}type", "")
                    ref_data["description"] = adt_obj.get("{http://www.sap.com/adt/core}description", "")
                    ref_data["responsible"] = adt_obj.get("{http://www.sap.com/adt/core}responsible", "")

                    # Get package reference
                    pkg_ref = adt_obj.find("adtcore:packageRef", ns)
                    if pkg_ref is not None:
                        ref_data["package"] = pkg_ref.get("{http://www.sap.com/adt/core}name", "")

                # Get object identifier (for usage snippets call)
                obj_id_elem = ref_obj.find("objectIdentifier")
                if obj_id_elem is not None and obj_id_elem.text:
                    ref_data["object_identifier"] = obj_id_elem.text
                    logger.debug(f"Found object_identifier: {obj_id_elem.text}")
                else:
                    logger.debug(f"No object_identifier found for {ref_data.get('name')}")

                result["references"].append(ref_data)

            return result

        except ET.ParseError as e:
            logger.error(f"Failed to parse usage references response: {e}")
            return {
                "error": f"XML parse error: {str(e)}",
                "total_references": 0,
                "references": []
            }
        except Exception as e:
            logger.error(f"Error processing usage references: {e}")
            return {
                "error": f"Processing error: {str(e)}",
                "total_references": 0,
                "references": []
            }
