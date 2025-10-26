"""Service for ABAP repository navigation operations."""

import logging
import xml.etree.ElementTree as ET
from typing import List, Dict, Any
from datetime import datetime

from app.core.rfc_adapter import RfcAdapter
from app.services.base_service import BaseService

logger = logging.getLogger(__name__)


class NavigationService(BaseService):
    """
    Service for navigating the ABAP repository tree structure.

    This service provides tools to navigate packages, folders, and object hierarchies.
    """

    def get_node_contents(self, node_uri: str, project_name: str = None) -> List[Dict[str, Any]]:
        """
        Get contents of a repository node (package, folder, etc.).

        Args:
            node_uri: URI of the node to explore
            project_name: Optional project name context

        Returns:
            List of objects/nodes contained in the specified node

        Example:
            >>> service.get_node_contents("/sap/bc/adt/packages/ZTEST")
            [
                {
                    "name": "ZCL_TEST_CLASS",
                    "type": "CLAS/OC",
                    "uri": "/sap/bc/adt/oo/classes/zcl_test_class",
                    "description": "Test class"
                },
                ...
            ]
        """
        logger.info(f"Getting node contents for: {node_uri}")

        params = {}
        if project_name:
            params['projectname'] = project_name

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri="/sap/bc/adt/repository/nodestructure",
                method="GET",
                params={**params, 'parent_uri': node_uri},
                body=""
            )

        if response.status_code == 200:
            contents = self._parse_node_contents(response.text)
            logger.info(f"Retrieved {len(contents)} items from node")
            return contents
        else:
            error_msg = f"Failed to get node contents: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def find_object_path(self, object_uri: str) -> Dict[str, Any]:
        """
        Find the complete path of an object in the repository tree.

        Args:
            object_uri: URI of the object

        Returns:
            Dictionary with object path information

        Example:
            >>> service.find_object_path("/sap/bc/adt/oo/classes/zcl_test")
            {
                "uri": "/sap/bc/adt/oo/classes/zcl_test",
                "path": ["$TMP", "Source Code Library", "Classes"],
                "parent_package": "$TMP",
                "full_path": "$TMP > Source Code Library > Classes > ZCL_TEST"
            }
        """
        logger.info(f"Finding object path for: {object_uri}")

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri="/sap/bc/adt/repository/nodestructure",
                method="GET",
                params={'uri': object_uri},
                body=""
            )

        if response.status_code == 200:
            path_data = self._parse_object_path(response.text, object_uri)
            logger.info(f"Object path found: {path_data.get('full_path', 'unknown')}")
            return path_data
        else:
            error_msg = f"Failed to find object path: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_package_objects(
        self,
        package_name: str,
        max_rows: int = 50,
        offset: int = 0,
        object_types: List[str] = None,
        author: str = None,
        created_from: str = None,
        created_to: str = None,
        response_format: str = "detailed"
    ) -> Dict[str, Any]:
        """
        Get ABAP objects from a package with pagination and filtering.

        Returns objects grouped by type with the following TADIR fields:
        - PGMID: Program ID (e.g., 'R3TR' for repository objects)
        - OBJECT: Object type (CLAS, PROG, FUGR, TABL, etc.)
        - OBJ_NAME: Object name
        - SRCSYSTEM: Source system
        - AUTHOR: Author/creator
        - DEVCLASS: Development class (package)
        - CREATED_ON: Creation date
        - CHECK_DATE: Last verification date

        Args:
            package_name: Package name (e.g., 'ZMMI1229_0', '$TMP')
            max_rows: Maximum objects per page (default: 50, max: 1000)
            offset: Number of objects to skip for pagination (default: 0)
            object_types: Optional list of object types to filter (e.g., ['CLAS', 'PROG'])
            author: Optional author filter (e.g., 'DEVELOPER')
            created_from: Optional start date filter in YYYY-MM-DD format (e.g., '2025-01-01')
            created_to: Optional end date filter in YYYY-MM-DD format (e.g., '2025-12-31')
            response_format: Response format (default: "detailed")
                - "detailed": All TADIR fields for each object
                - "summary": Only object names + counts by type
                - "types_only": Only counts by type (ultra-compact)

        Returns:
            Dictionary with package objects grouped by type + pagination metadata
            (format varies based on response_format parameter)

        Example:
            >>> # First page (default)
            >>> service.get_package_objects("ZMMI1229_0")
            >>> # Second page
            >>> service.get_package_objects("ZMMI1229_0", offset=50)
            >>> # With filters
            >>> service.get_package_objects("ZMMI1229_0", object_types=['CLAS', 'PROG'])
            >>> service.get_package_objects("ZMMI1229_0", author='DEVELOPER')
            >>> service.get_package_objects("ZMMI1229_0", created_from='2025-01-01', created_to='2025-12-31')
            {
                "package_name": "ZMMI1229_0",
                "total_objects": 241,
                "returned_objects": 50,
                "object_types": {
                    "CLAS": {
                        "count": 10,
                        "objects": [...]
                    }
                },
                "pagination": {
                    "has_more": true,
                    "next_offset": 50,
                    "current_page": 1,
                    "page_size": 50,
                    "total_pages": 5
                },
                "metadata": {...}
            }
        """
        logger.info(
            f"Getting package objects for: {package_name} "
            f"(max_rows: {max_rows}, offset: {offset}, format: {response_format}, "
            f"object_types: {object_types}, author: {author}, "
            f"created_from: {created_from}, created_to: {created_to})"
        )

        # Validate response_format
        valid_formats = ["detailed", "summary", "types_only"]
        if response_format not in valid_formats:
            logger.warning(
                f"Invalid response_format '{response_format}', using 'detailed'. "
                f"Valid formats: {valid_formats}"
            )
            response_format = "detailed"

        # Validate max_rows
        if max_rows > 1000:
            logger.warning(f"max_rows {max_rows} exceeds limit, setting to 1000")
            max_rows = 1000

        # Query TADIR table with specific fields
        from app.services.query_service import QueryService
        query_service = QueryService(self.pool)

        try:
            # Define fields to retrieve from TADIR
            fields = [
                "PGMID",
                "OBJECT",
                "OBJ_NAME",
                "SRCSYSTEM",
                "AUTHOR",
                "DEVCLASS",
                "CREATED_ON",
                "CHECK_DATE"
            ]

            # Build WHERE clause with filters
            where_conditions = [f"DEVCLASS = '{package_name}'"]

            # Filter by object types
            if object_types and len(object_types) > 0:
                # Build IN clause for object types
                types_list = "', '".join(object_types)
                where_conditions.append(f"OBJECT IN ('{types_list}')")
                logger.debug(f"Filtering by object types: {object_types}")

            # Filter by author
            if author:
                where_conditions.append(f"AUTHOR = '{author}'")
                logger.debug(f"Filtering by author: {author}")

            # Filter by creation date range
            if created_from:
                # Convert YYYY-MM-DD to SAP format YYYYMMDD
                sap_date_from = created_from.replace('-', '')
                where_conditions.append(f"CREATED_ON >= '{sap_date_from}'")
                logger.debug(f"Filtering from date: {created_from} (SAP: {sap_date_from})")

            if created_to:
                # Convert YYYY-MM-DD to SAP format YYYYMMDD
                sap_date_to = created_to.replace('-', '')
                where_conditions.append(f"CREATED_ON <= '{sap_date_to}'")
                logger.debug(f"Filtering to date: {created_to} (SAP: {sap_date_to})")

            # Combine all conditions with AND
            where_clause = " AND ".join(where_conditions)
            logger.debug(f"Final WHERE clause: {where_clause}")

            # First, get total count (without limit/offset) for pagination metadata
            # We do this by querying with a small limit just to get row_count from TADIR
            count_data = query_service.get_table_contents(
                table_name="TADIR",
                max_rows=1,  # Minimal query to get count
                offset=0,
                where_clause=where_clause,
                fields=["OBJECT"]  # Just one field for efficiency
            )

            # Note: The total count should ideally come from a COUNT(*) query,
            # but ADT API doesn't provide that. We'll approximate by checking
            # if we got all results or not.
            # For now, we'll query the full page and check if there are more results

            # Query TADIR for the package with pagination
            table_data = query_service.get_table_contents(
                table_name="TADIR",
                max_rows=max_rows,
                offset=offset,
                where_clause=where_clause,
                fields=fields
            )

            # Parse and group results
            filters_applied = {
                'object_types': object_types,
                'author': author,
                'created_from': created_from,
                'created_to': created_to
            }

            # Check if we got fewer rows than requested (indicates last page)
            actual_rows_returned = len(table_data.get('rows', []))

            result = self._group_package_objects(
                table_data,
                package_name,
                max_rows,
                offset,
                actual_rows_returned,
                filters_applied
            )

            logger.info(
                f"Retrieved {result['total_objects']} objects from package '{package_name}' "
                f"({len(result['object_types'])} different types)"
            )

            # Apply response format transformation
            if response_format == "summary":
                result = self._format_summary(result)
            elif response_format == "types_only":
                result = self._format_types_only(result)
            # "detailed" format is already in result, no transformation needed

            # Check response size and truncate if needed
            pagination_info = result.get('pagination', {})
            next_offset = pagination_info.get('next_offset', offset + max_rows)

            suggestions = [
                f"Use pagination: get_package_objects('{package_name}', offset={next_offset})",
                "Use compact format: response_format='summary' (90% smaller) or 'types_only' (99% smaller)",
                "Add filters: object_types=['CLAS'], author='USERNAME', or created_from='2025-01-01'"
            ]

            result_data, truncation_info = self._check_and_truncate(result, suggestions)

            # Add truncation info to metadata
            if 'metadata' not in result_data:
                result_data['metadata'] = {}
            result_data['metadata']['truncation'] = truncation_info

            return result_data

        except Exception as e:
            error_msg = f"Failed to get package objects for '{package_name}': {str(e)}"
            logger.error(error_msg)
            raise Exception(error_msg)

    # Private parsing methods

    def _parse_node_contents(self, xml_text: str) -> List[Dict[str, Any]]:
        """
        Parse node contents XML response.

        Args:
            xml_text: XML response from SAP

        Returns:
            List of node content items
        """
        try:
            root = ET.fromstring(xml_text)
            contents = []

            # Namespaces for repository structure
            ns = {
                'repo': 'http://www.sap.com/adt/repository',
                'adtcore': 'http://www.sap.com/adt/core'
            }

            # Find all repository objects
            for obj in root.findall('.//repo:object', ns):
                item = {
                    'name': obj.get(f'{{{ns["adtcore"]}}}name', ''),
                    'type': obj.get(f'{{{ns["adtcore"]}}}type', ''),
                    'uri': obj.get(f'{{{ns["adtcore"]}}}uri', ''),
                    'description': obj.get(f'{{{ns["adtcore"]}}}description', ''),
                    'package': obj.get(f'{{{ns["adtcore"]}}}package', ''),
                }
                contents.append(item)

            # Also check for folder structures
            for folder in root.findall('.//repo:folder', ns):
                item = {
                    'name': folder.get(f'{{{ns["adtcore"]}}}name', ''),
                    'type': 'FOLDER',
                    'uri': folder.get(f'{{{ns["adtcore"]}}}uri', ''),
                    'description': folder.get(f'{{{ns["adtcore"]}}}description', ''),
                }
                contents.append(item)

            return contents

        except ET.ParseError as e:
            logger.error(f"Failed to parse node contents XML: {e}")
            raise Exception(f"XML parsing error: {e}")

    def _parse_object_path(self, xml_text: str, object_uri: str) -> Dict[str, Any]:
        """
        Parse object path XML response.

        Args:
            xml_text: XML response from SAP
            object_uri: Original object URI

        Returns:
            Dictionary with path information
        """
        try:
            root = ET.fromstring(xml_text)

            ns = {
                'repo': 'http://www.sap.com/adt/repository',
                'adtcore': 'http://www.sap.com/adt/core'
            }

            path_elements = []
            parent_package = None

            # Navigate through parent nodes
            for node in root.findall('.//repo:node', ns):
                name = node.get(f'{{{ns["adtcore"]}}}name', '')
                node_type = node.get(f'{{{ns["adtcore"]}}}type', '')

                path_elements.append(name)

                if node_type in ['DEVC/K', 'DEVC/L']:  # Package types
                    parent_package = name

            path_data = {
                'uri': object_uri,
                'path': path_elements,
                'parent_package': parent_package or '$TMP',
                'full_path': ' > '.join(path_elements) if path_elements else object_uri
            }

            return path_data

        except ET.ParseError as e:
            logger.error(f"Failed to parse object path XML: {e}")
            # Return minimal path data on parse error
            return {
                'uri': object_uri,
                'path': [],
                'parent_package': 'unknown',
                'full_path': object_uri,
                'error': str(e)
            }

    def _group_package_objects(
        self,
        table_data: Dict[str, Any],
        package_name: str,
        max_rows: int,
        offset: int,
        actual_rows_returned: int,
        filters_applied: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """
        Group package objects by type and add pagination metadata.

        Args:
            table_data: Raw table data from QueryService
            package_name: Package name
            max_rows: Max rows requested (page size)
            offset: Current offset
            actual_rows_returned: Actual number of rows returned
            filters_applied: Dictionary with applied filters

        Returns:
            Dictionary with grouped objects and pagination metadata
        """
        # Calculate pagination info
        # has_more: True if we got exactly max_rows (likely more data exists)
        # This is an approximation since we don't have exact total count
        has_more = actual_rows_returned >= max_rows
        next_offset = offset + max_rows if has_more else None
        current_page = (offset // max_rows) + 1

        # Initialize result structure with pagination
        result = {
            'package_name': package_name,
            'total_objects': 0,  # Will be updated as we count objects
            'returned_objects': actual_rows_returned,
            'object_types': {},
            'pagination': {
                'has_more': has_more,
                'next_offset': next_offset,
                'current_offset': offset,
                'current_page': current_page,
                'page_size': max_rows
            },
            'metadata': {
                'query_time': datetime.utcnow().isoformat() + 'Z',
                'max_rows': max_rows,
                'offset': offset,
                'truncated': False,
                'fields': [
                    'PGMID', 'OBJECT', 'OBJ_NAME', 'SRCSYSTEM',
                    'AUTHOR', 'DEVCLASS', 'CREATED_ON', 'CHECK_DATE'
                ],
                'filters': filters_applied or {}
            }
        }

        # Check if we got any rows
        rows = table_data.get('rows', [])
        if not rows:
            logger.info(f"No objects found in package '{package_name}'")
            return result

        # Group objects by type
        for row in rows:
            object_type = row.get('OBJECT', 'UNKNOWN').strip()

            # Initialize object type if not exists
            if object_type not in result['object_types']:
                result['object_types'][object_type] = {
                    'count': 0,
                    'objects': []
                }

            # Format the object data
            obj_data = {
                'pgmid': row.get('PGMID', '').strip(),
                'object': object_type,
                'obj_name': row.get('OBJ_NAME', '').strip(),
                'srcsystem': row.get('SRCSYSTEM', '').strip(),
                'author': row.get('AUTHOR', '').strip(),
                'devclass': row.get('DEVCLASS', '').strip(),
                'created_on': self._format_sap_date(row.get('CREATED_ON', '')),
                'check_date': self._format_sap_date(row.get('CHECK_DATE', ''))
            }

            # Add to object type group
            result['object_types'][object_type]['objects'].append(obj_data)
            result['object_types'][object_type]['count'] += 1
            result['total_objects'] += 1

        # Check if results were truncated
        if table_data.get('row_count', 0) >= max_rows:
            result['metadata']['truncated'] = True
            logger.warning(
                f"Results truncated at {max_rows} objects for package '{package_name}'"
            )

        return result

    def _format_sap_date(self, sap_date: str) -> str:
        """
        Format SAP date from YYYYMMDD to YYYY-MM-DD.

        Args:
            sap_date: Date in SAP format (YYYYMMDD) or empty string

        Returns:
            Formatted date string (YYYY-MM-DD) or empty string
        """
        if not sap_date or not sap_date.strip():
            return ''

        sap_date = sap_date.strip()

        # SAP date format is YYYYMMDD (8 characters)
        if len(sap_date) == 8 and sap_date.isdigit():
            try:
                year = sap_date[0:4]
                month = sap_date[4:6]
                day = sap_date[6:8]
                return f"{year}-{month}-{day}"
            except Exception as e:
                logger.warning(f"Failed to format date '{sap_date}': {e}")
                return sap_date
        else:
            # Return as-is if not in expected format
            return sap_date

    def _format_summary(self, detailed_result: Dict[str, Any]) -> Dict[str, Any]:
        """
        Transform detailed result to summary format.

        Summary format includes only object names and counts by type,
        reducing response size by ~90% compared to detailed format.

        Args:
            detailed_result: Result in detailed format

        Returns:
            Result in summary format with only names + counts

        Example:
            Input (detailed):
            {
                "object_types": {
                    "CLAS": {
                        "count": 7,
                        "objects": [
                            {"obj_name": "ZCL_1", "author": "DEV", ...},
                            {"obj_name": "ZCL_2", "author": "DEV", ...}
                        ]
                    }
                }
            }

            Output (summary):
            {
                "object_types": {
                    "CLAS": {
                        "count": 7,
                        "names": ["ZCL_1", "ZCL_2", ...]
                    }
                }
            }
        """
        summary_result = {
            'package_name': detailed_result.get('package_name'),
            'total_objects': detailed_result.get('total_objects', 0),
            'returned_objects': detailed_result.get('returned_objects', 0),
            'object_types': {},
            'pagination': detailed_result.get('pagination', {}),
            'metadata': detailed_result.get('metadata', {})
        }

        # Transform object_types to summary format
        for obj_type, type_data in detailed_result.get('object_types', {}).items():
            # Extract just the names from the full object data
            names = [obj.get('obj_name', '') for obj in type_data.get('objects', [])]

            summary_result['object_types'][obj_type] = {
                'count': type_data.get('count', 0),
                'names': names
            }

        # Update metadata to indicate format
        summary_result['metadata']['response_format'] = 'summary'

        return summary_result

    def _format_types_only(self, detailed_result: Dict[str, Any]) -> Dict[str, Any]:
        """
        Transform detailed result to types_only format (ultra-compact).

        Types_only format includes ONLY counts by object type,
        reducing response size by ~99% compared to detailed format.
        Ideal for initial package exploration.

        Args:
            detailed_result: Result in detailed format

        Returns:
            Result in types_only format with only type counts

        Example:
            Input (detailed):
            {
                "object_types": {
                    "CLAS": {"count": 7, "objects": [...]},
                    "PROG": {"count": 121, "objects": [...]}
                }
            }

            Output (types_only):
            {
                "object_types": {
                    "CLAS": 7,
                    "PROG": 121
                }
            }
        """
        types_only_result = {
            'package_name': detailed_result.get('package_name'),
            'total_objects': detailed_result.get('total_objects', 0),
            'returned_objects': detailed_result.get('returned_objects', 0),
            'object_types': {},
            'pagination': detailed_result.get('pagination', {}),
            'metadata': detailed_result.get('metadata', {})
        }

        # Transform object_types to types_only format (just counts)
        for obj_type, type_data in detailed_result.get('object_types', {}).items():
            types_only_result['object_types'][obj_type] = type_data.get('count', 0)

        # Update metadata to indicate format
        types_only_result['metadata']['response_format'] = 'types_only'

        return types_only_result
