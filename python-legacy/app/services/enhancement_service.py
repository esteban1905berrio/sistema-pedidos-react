"""Service for Enhancement (Ampliaciones) operations."""

import logging
from typing import Dict, Any, List
from xml.etree import ElementTree as ET

from app.core.rfc_adapter import RfcAdapter
from app.services.base_service import BaseService

logger = logging.getLogger(__name__)


class EnhancementService(BaseService):
    """Service for Enhancement operations (Hook Implementations, BAdI, etc.)."""

    def search_enhancements(
        self,
        package: str,
        enhancement_type: str = "ENHO"
    ) -> List[Dict[str, Any]]:
        """
        Search for enhancements in a package using virtual folders.

        Args:
            package: Package name to search in
            enhancement_type: Enhancement type (default: "ENHO")

        Returns:
            List of enhancements with:
            - name: Enhancement name
            - type: Enhancement type (ENHO/XHH, ENHO/XH, ENHO/XHB)
            - uri: ADT URI
            - text: Description
            - package: Package name

        Example:
            >>> service.search_enhancements("ZI1008", "ENHO")
            [
                {
                    "name": "ZFII1008_1",
                    "type": "ENHO/XHH",
                    "text": "Derivar Segmento para documentos FI-CO",
                    "uri": "/sap/bc/adt/enhancements/enhoxhh/zfii1008_1",
                    "package": "ZI1008"
                },
                ...
            ]
        """
        logger.info(f"Searching enhancements in package: {package} (type: {enhancement_type})")

        # Build XML request body with facet preselection
        body = self._build_search_xml(package, enhancement_type)

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri="/sap/bc/adt/repository/informationsystem/virtualfolders/contents",
                method="POST",
                params={},
                body=body,
                content_type="application/vnd.sap.adt.repository.virtualfolders.request.v1+xml"
            )

            if response.status_code == 200:
                results = self._parse_search_results(response.text)
                logger.info(f"Found {len(results)} enhancements in package {package}")
                return results
            else:
                error_msg = f"{response.status_code} - Failed to search enhancements in {package}"
                logger.error(error_msg)
                raise Exception(f"{error_msg}\n{response.text}")

    def get_enhancement_metadata(
        self,
        enhancement_name: str,
        enhancement_subtype: str = "enhoxhh"
    ) -> Dict[str, Any]:
        """
        Get enhancement metadata including hook implementations.

        Args:
            enhancement_name: Name of the enhancement
            enhancement_subtype: Subtype (enhoxhh, enhoxh, enhoxhb)
                - enhoxhh: Hook Implementation (Explicit Enhancement)
                - enhoxh: Enhancement Implementation
                - enhoxhb: Enhancement Implementation with BAdI

        Returns:
            Dictionary with enhancement metadata:
            - name: Enhancement name
            - type: Enhancement type
            - description: Description
            - package: Package
            - tool_type: Tool type (e.g., HOOK_IMPL)
            - enhanced_object: Object being enhanced
            - hook_implementations: List of hook implementations
            - spot_name: Enhancement spot name
            - program_name: Program name
            - full_name: Full enhancement name

        Example:
            >>> service.get_enhancement_metadata("ZFII1008_1", "enhoxhh")
            {
                "name": "ZFII1008_1",
                "type": "ENHO/XHH",
                "description": "Derivar Segmento para documentos FI-CO",
                "tool_type": "HOOK_IMPL",
                "hook_implementations": [
                    {
                        "id": "1",
                        "spot_name": "ES_SAPLKALE",
                        "program_name": "SAPLKALE",
                        "full_name": "\\PR:SAPLKALE\\EX:IDOC_INPUT_CODCMT_G1\\EI"
                    }
                ],
                ...
            }
        """
        logger.info(f"Getting enhancement metadata for: {enhancement_name} (subtype: {enhancement_subtype})")

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri=f"/sap/bc/adt/enhancements/{enhancement_subtype}/{enhancement_name.lower()}",
                method="GET",
                params={},
                content_type="application/vnd.sap.adt.enh.enhoxhh.v3+xml"
            )

        if response.status_code == 200:
            return self._parse_enhancement_metadata(response.text)
        else:
            error_msg = f"{response.status_code} - Failed to get enhancement metadata for {enhancement_name}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_enhancement_source(
        self,
        enhancement_name: str,
        enhancement_subtype: str = "enhoxhh"
    ) -> str:
        """
        Get enhancement source code.

        Args:
            enhancement_name: Name of the enhancement
            enhancement_subtype: Subtype (enhoxhh, enhoxh, enhoxhb)

        Returns:
            Enhancement source code as string

        Example:
            >>> service.get_enhancement_source("ZFII1008_1", "enhoxhh")
            "ENHANCEMENT 1  .
               zclfi_exits_gestion_costos=>modifica_segmentos_idoc_codcmt(
                 CHANGING c_ti_datos_idoc = idoc_data[]
                          c_ti_idoc_contrl = idoc_contrl[] ).
             ENDENHANCEMENT."
        """
        logger.info(f"Getting enhancement source for: {enhancement_name} (subtype: {enhancement_subtype})")

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri=f"/sap/bc/adt/enhancements/{enhancement_subtype}/{enhancement_name.lower()}/source/main",
                method="GET",
                params={},
                content_type="text/plain"
            )

        if response.status_code == 200:
            logger.info(f"Retrieved enhancement source for {enhancement_name} ({len(response.text)} characters)")
            return response.text
        else:
            error_msg = f"{response.status_code} - Failed to get enhancement source for {enhancement_name}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # ============================================================================
    # HELPER METHODS
    # ============================================================================

    def _build_search_xml(self, package: str, enhancement_type: str) -> str:
        """
        Build XML request for enhancement search.

        Args:
            package: Package name
            enhancement_type: Enhancement type

        Returns:
            XML string
        """
        xml_template = '''<?xml version="1.0" encoding="UTF-8"?>
<vfs:virtualFoldersRequest xmlns:vfs="http://www.sap.com/adt/ris/virtualFolders" objectSearchPattern="*">
  <vfs:preselection facet="package">
    <vfs:value>{package}</vfs:value>
  </vfs:preselection>
  <vfs:preselection facet="type">
    <vfs:value>{enhancement_type}</vfs:value>
  </vfs:preselection>
  <vfs:facetorder/>
</vfs:virtualFoldersRequest>'''

        return xml_template.format(package=package, enhancement_type=enhancement_type)

    def _parse_search_results(self, xml_body: str) -> List[Dict[str, Any]]:
        """
        Parse virtual folders search results.

        Args:
            xml_body: XML response body

        Returns:
            List of enhancement objects
        """
        try:
            root = ET.fromstring(xml_body)

            # Define namespaces
            ns = {
                "vfs": "http://www.sap.com/adt/ris/virtualFolders",
                "atom": "http://www.w3.org/2005/Atom"
            }

            results = []

            # Find all object elements
            for obj in root.findall(".//vfs:object", ns):
                result = {
                    "name": obj.get("name", ""),
                    "type": obj.get("type", ""),
                    "text": obj.get("text", ""),
                    "uri": obj.get("uri", ""),
                    "package": obj.get("package", ""),
                    "expandable": obj.get("expandable", "false") == "true"
                }
                results.append(result)

            return results

        except ET.ParseError as e:
            logger.error(f"Failed to parse search results: {e}")
            return []
        except Exception as e:
            logger.error(f"Error processing search results: {e}")
            return []

    def _parse_enhancement_metadata(self, xml_body: str) -> Dict[str, Any]:
        """
        Parse enhancement metadata XML.

        Args:
            xml_body: XML response body

        Returns:
            Dictionary with parsed metadata
        """
        try:
            root = ET.fromstring(xml_body)

            # Define namespaces
            ns = {
                "enho": "http://www.sap.com/adt/enhancements/enho",
                "adtcore": "http://www.sap.com/adt/core",
                "atom": "http://www.w3.org/2005/Atom",
                "enhcore": "http://www.sap.com/abapsource/enhancementscore"
            }

            metadata = {
                "name": root.get("{http://www.sap.com/adt/core}name", ""),
                "type": root.get("{http://www.sap.com/adt/core}type", ""),
                "description": root.get("{http://www.sap.com/adt/core}description", ""),
                "responsible": root.get("{http://www.sap.com/adt/core}responsible", ""),
                "created_by": root.get("{http://www.sap.com/adt/core}createdBy", ""),
                "created_at": root.get("{http://www.sap.com/adt/core}createdAt", ""),
                "changed_by": root.get("{http://www.sap.com/adt/core}changedBy", ""),
                "changed_at": root.get("{http://www.sap.com/adt/core}changedAt", ""),
                "master_language": root.get("{http://www.sap.com/adt/core}masterLanguage", ""),
            }

            # Get package reference
            package_ref = root.find("adtcore:packageRef", ns)
            if package_ref is not None:
                metadata["package"] = package_ref.get("{http://www.sap.com/adt/core}name", "")

            # Get content common (tool type, etc.)
            content_common = root.find("enho:contentCommon", ns)
            if content_common is not None:
                metadata["tool_type"] = content_common.get("{http://www.sap.com/adt/enhancements/enho}toolType", "")

            # Get hook implementations
            hook_implementations = []
            content_specific = root.find("enho:contentSpecific", ns)
            if content_specific is not None:
                hook_tech = content_specific.find("enho:hookTechnology", ns)
                if hook_tech is not None:
                    for hook_impl in hook_tech.findall("enho:hookImplementation", ns):
                        impl = {
                            "id": hook_impl.get("{http://www.sap.com/adt/enhancements/enho}id", ""),
                            "spot_name": hook_impl.get("{http://www.sap.com/adt/enhancements/enho}spotname", ""),
                            "program_name": hook_impl.get("{http://www.sap.com/adt/enhancements/enho}programname", ""),
                            "full_name": hook_impl.get("{http://www.sap.com/adt/enhancements/enho}full_name", ""),
                            "full_description": hook_impl.get("{http://www.sap.com/adt/enhancements/enho}full_description", ""),
                        }
                        hook_implementations.append(impl)

            metadata["hook_implementations"] = hook_implementations

            return metadata

        except ET.ParseError as e:
            logger.error(f"Failed to parse enhancement metadata: {e}")
            return {"error": f"XML parse error: {str(e)}"}
        except Exception as e:
            logger.error(f"Error processing enhancement metadata: {e}")
            return {"error": f"Processing error: {str(e)}"}
