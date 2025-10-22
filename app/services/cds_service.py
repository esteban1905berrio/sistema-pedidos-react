"""Service for CDS Views operations."""

import logging
from typing import Dict, Any, Optional, List
from xml.etree import ElementTree as ET

from app.core.rfc_adapter import RfcAdapter
from app.services.base_service import BaseService

logger = logging.getLogger(__name__)


class CDSService(BaseService):
    """Service for CDS Views and Core Data Services operations."""

    def get_cds_view_metadata(
        self,
        cds_name: str,
        version: str = "active"
    ) -> Dict[str, Any]:
        """
        Get metadata of a CDS view.

        Args:
            cds_name: Name of the CDS view (e.g., 'ZI_RAP_ZTCXR1003_1')
            version: Version ('active' or 'inactive')

        Returns:
            Dictionary with CDS view metadata including:
            - name: CDS view name
            - type: Object type (DDLS)
            - description: Description text
            - sql_view_name: Associated SQL view name
            - package: Package name
            - responsible: Responsible user
            - created_by: Creator
            - created_at: Creation timestamp
            - changed_by: Last modifier
            - changed_at: Last change timestamp
            - source_uri: URI to source code
            - master_language: Master language

        Example:
            >>> service.get_cds_view_metadata("ZI_RAP_ZTCXR1003_1")
            {
                "name": "ZI_RAP_ZTCXR1003_1",
                "sql_view_name": "ZI_RAP_ZTCXR1003_1",
                "description": "RAP View for Table ZTCXR1003_1",
                ...
            }
        """
        logger.info(f"Getting CDS view metadata for: {cds_name}")

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri=f"/sap/bc/adt/ddic/ddl/sources/{cds_name.lower()}",
                method="GET",
                params={"version": version},
                content_type="application/vnd.sap.adt.ddic.ddlsources.v2+xml"
            )

            if response.status_code == 200:
                return self._parse_cds_metadata(response.text)
            else:
                error_msg = f"{response.status_code} - Failed to get CDS metadata for {cds_name}"
                logger.error(error_msg)
                raise Exception(f"{error_msg}\n{response.text}")

    def get_cds_view_source(
        self,
        cds_name: str,
        version: str = "active"
    ) -> str:
        """
        Get DDL source code of a CDS view.

        Args:
            cds_name: Name of the CDS view
            version: Version ('active' or 'inactive')

        Returns:
            DDL source code as string

        Example:
            >>> service.get_cds_view_source("ZI_RAP_ZTCXR1003_1")
            "@AbapCatalog.viewEnhancementCategory: [#NONE]
             @AccessControl.authorizationCheck: #NOT_REQUIRED
             define view ZI_RAP_ZTCXR1003_1 as select from ztcxr1003_1
             {
               key id,
               field1,
               field2
             }"
        """
        logger.info(f"Getting CDS view source for: {cds_name}")

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri=f"/sap/bc/adt/ddic/ddl/sources/{cds_name.lower()}/source/main",
                method="GET",
                params={"version": version},
                content_type="text/plain"
            )

        if response.status_code == 200:
            logger.info(f"Retrieved CDS source for {cds_name} ({len(response.text)} characters)")
            return response.text
        else:
            error_msg = f"{response.status_code} - Failed to get CDS source for {cds_name}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def search_cds_views_by_sqlview(
        self,
        sql_view_name: str,
        max_results: Optional[int] = None
    ) -> List[Dict[str, Any]]:
        """
        Search CDS views by SQL view name.

        Args:
            sql_view_name: SQL view name pattern (supports wildcards *)
            max_results: Maximum number of results to return

        Returns:
            List of matching CDS views with basic info

        Example:
            >>> service.search_cds_views_by_sqlview("ZI_RAP*")
            [
                {
                    "name": "ZI_RAP_ZTCXR1003_1",
                    "sql_view_name": "ZI_RAP_ZTCXR1003_1",
                    "type": "DDLS",
                    "description": "RAP View..."
                },
                ...
            ]
        """
        logger.info(f"Searching CDS views by SQL view name: {sql_view_name}")

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri="/sap/bc/adt/repository/informationsystem/search",
                method="GET",
                params={
                    "objectType": "VIEW/DV",
                    "name": sql_view_name,
                    "maxResults": str(max_results) if max_results else "100"
                }
            )

        if response.status_code == 200:
            results = self._parse_search_results(response.text)
            logger.info(f"Found {len(results)} CDS views matching {sql_view_name}")
            return results
        else:
            error_msg = f"{response.status_code} - Failed to search CDS views"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_cds_view_properties(
        self,
        cds_name: str
    ) -> Dict[str, Any]:
        """
        Get properties of a CDS view object.

        Args:
            cds_name: Name of the CDS view

        Returns:
            Dictionary with properties:
            - package: Package name
            - owner: Owner user
            - api_state: API state (if defined)
            - application_component: Application component
            - software_component: Software component

        Example:
            >>> service.get_cds_view_properties("ZI_RAP_ZTCXR1003_1")
            {
                "package": "ZRAP_DEMO",
                "owner": "DEVELOPER",
                "api_state": "RELEASED",
                ...
            }
        """
        logger.info(f"Getting CDS view properties for: {cds_name}")

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri="/sap/bc/adt/repository/informationsystem/objectproperties/values",
                method="GET",
                params={
                    "objectName": cds_name,
                    "objectType": "DDLS"
                }
            )

        if response.status_code == 200:
            return self._parse_object_properties(response.text)
        else:
            error_msg = f"{response.status_code} - Failed to get CDS properties for {cds_name}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def _parse_cds_metadata(self, xml_body: str) -> Dict[str, Any]:
        """
        Parse CDS metadata XML response.

        Args:
            xml_body: XML response body

        Returns:
            Dictionary with parsed metadata
        """
        try:
            root = ET.fromstring(xml_body)

            # Define namespaces
            ns = {
                "ddl": "http://www.sap.com/adt/ddic/ddlsources",
                "adtcore": "http://www.sap.com/adt/core",
                "atom": "http://www.w3.org/2005/Atom"
            }

            metadata = {
                "name": root.get("{http://www.sap.com/adt/core}name", ""),
                "type": root.get("{http://www.sap.com/adt/core}type", ""),
                "description": root.get("{http://www.sap.com/adt/core}description", ""),
                "master_language": root.get("{http://www.sap.com/adt/core}masterLanguage", ""),
                "responsible": root.get("{http://www.sap.com/adt/core}responsible", ""),
                "created_by": root.get("{http://www.sap.com/adt/core}createdBy", ""),
                "created_at": root.get("{http://www.sap.com/adt/core}createdAt", ""),
                "changed_by": root.get("{http://www.sap.com/adt/core}changedBy", ""),
                "changed_at": root.get("{http://www.sap.com/adt/core}changedAt", ""),
                "version": root.get("{http://www.sap.com/adt/core}version", ""),
            }

            # Get SQL view name
            sql_view = root.find("ddl:sqlViewName", ns)
            if sql_view is not None and sql_view.text:
                metadata["sql_view_name"] = sql_view.text

            # Get source URI
            source_link = root.find(".//atom:link[@rel='http://www.sap.com/adt/relations/source']", ns)
            if source_link is not None:
                metadata["source_uri"] = source_link.get("href", "")

            # Get package reference
            package_ref = root.find("adtcore:packageRef", ns)
            if package_ref is not None:
                metadata["package"] = package_ref.get("{http://www.sap.com/adt/core}name", "")

            return metadata

        except ET.ParseError as e:
            logger.error(f"Failed to parse CDS metadata: {e}")
            return {"error": f"XML parse error: {str(e)}"}
        except Exception as e:
            logger.error(f"Error processing CDS metadata: {e}")
            return {"error": f"Processing error: {str(e)}"}

    def _parse_search_results(self, xml_body: str) -> List[Dict[str, Any]]:
        """
        Parse search results XML.

        Args:
            xml_body: XML response body

        Returns:
            List of search results
        """
        try:
            root = ET.fromstring(xml_body)

            # Define namespaces
            ns = {
                "adtcore": "http://www.sap.com/adt/core",
                "atom": "http://www.w3.org/2005/Atom"
            }

            results = []
            for obj in root.findall(".//adtcore:objectReference", ns):
                result = {
                    "name": obj.get("{http://www.sap.com/adt/core}name", ""),
                    "type": obj.get("{http://www.sap.com/adt/core}type", ""),
                    "description": obj.get("{http://www.sap.com/adt/core}description", ""),
                    "uri": obj.get("{http://www.sap.com/adt/core}uri", ""),
                    "package": obj.get("{http://www.sap.com/adt/core}packageName", "")
                }
                results.append(result)

            return results

        except ET.ParseError as e:
            logger.error(f"Failed to parse search results: {e}")
            return []
        except Exception as e:
            logger.error(f"Error processing search results: {e}")
            return []

    def _parse_object_properties(self, xml_body: str) -> Dict[str, Any]:
        """
        Parse object properties XML.

        Args:
            xml_body: XML response body

        Returns:
            Dictionary with object properties
        """
        try:
            root = ET.fromstring(xml_body)

            properties = {}
            for prop in root.findall(".//property"):
                key = prop.get("key", "")
                value = prop.get("value", "")
                if key:
                    properties[key.lower().replace(" ", "_")] = value

            return properties

        except ET.ParseError as e:
            logger.error(f"Failed to parse object properties: {e}")
            return {"error": f"XML parse error: {str(e)}"}
        except Exception as e:
            logger.error(f"Error processing object properties: {e}")
            return {"error": f"Processing error: {str(e)}"}
