"""Service for ABAP data query and preview operations."""

import logging
import xml.etree.ElementTree as ET
from typing import List, Dict, Any, Optional

from app.core.rfc_adapter import RfcAdapter

logger = logging.getLogger(__name__)


class QueryService:
    """
    Service for querying and previewing ABAP data.

    This service provides tools to:
    - Preview table contents
    - Execute custom queries
    - Filter and limit data retrieval
    """

    def __init__(self, adapter: RfcAdapter):
        """
        Initialize the query service.

        Args:
            adapter: RfcAdapter instance for ADT API calls to SAP system
        """
        self.adapter = adapter
        logger.debug("QueryService initialized")

    def get_table_contents(
        self,
        table_name: str,
        max_rows: int = 100,
        where_clause: Optional[str] = None,
        fields: Optional[List[str]] = None
    ) -> Dict[str, Any]:
        """
        Get preview of table contents (data preview).

        Args:
            table_name: Name of the database table
            max_rows: Maximum number of rows to return (default: 100, max: 1000)
            where_clause: Optional WHERE clause for filtering (e.g., "MANDT = '100'")
            fields: Optional list of specific fields to retrieve (default: all)

        Returns:
            Dictionary with table data and metadata

        Example:
            >>> service.get_table_contents("USR02", max_rows=10)
            {
                "table_name": "USR02",
                "rows": [...],
                "columns": [...],
                "row_count": 10
            }

            >>> service.get_table_contents("T000", where_clause="MANDT = '100'", max_rows=5)
            {
                "table_name": "T000",
                "rows": [...],
                ...
            }
        """
        logger.info(f"Getting table contents for: {table_name} (max_rows: {max_rows})")

        # Validate max_rows
        if max_rows > 1000:
            logger.warning(f"max_rows {max_rows} exceeds limit, setting to 1000")
            max_rows = 1000

        # Build SQL SELECT statement
        sql_statement = self._build_sql_select(
            table_name=table_name,
            where_clause=where_clause,
            fields=fields
        )

        # Use DDIC-based data preview endpoint
        response = self.adapter.request(
            uri="/sap/bc/adt/datapreview/ddic",
            method="POST",
            params={
                "rowNumber": max_rows,
                "ddicEntityName": table_name
            },
            body=sql_statement,
            content_type="text/plain"
        )

        if response.status_code == 200:
            table_data = self._parse_table_data(response.text, table_name)
            logger.info(f"Retrieved {table_data.get('row_count', 0)} rows from {table_name}")
            return table_data
        else:
            error_msg = f"Failed to get table contents for '{table_name}': {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def run_query(self, query_definition: Dict[str, Any]) -> Dict[str, Any]:
        """
        Execute a custom query with advanced options.

        Args:
            query_definition: Dictionary with query configuration:
                - sql: SQL SELECT statement (or table name)
                - max_rows: Maximum rows to return
                - parameters: Optional query parameters

        Returns:
            Dictionary with query results

        Example:
            >>> service.run_query({
            ...     "sql": "SELECT * FROM USR02 WHERE BNAME LIKE 'A%'",
            ...     "max_rows": 50
            ... })
            {
                "rows": [...],
                "columns": [...],
                ...
            }
        """
        logger.info(f"Running custom query: {query_definition.get('sql', 'N/A')[:100]}")

        # Build query from definition
        sql = query_definition.get('sql', '')
        max_rows = query_definition.get('max_rows', 100)
        parameters = query_definition.get('parameters', {})

        # For simple table queries, use get_table_contents
        if sql.upper().startswith('SELECT * FROM ') and 'WHERE' not in sql.upper():
            table_name = sql.split('FROM')[1].strip().split()[0]
            return self.get_table_contents(table_name, max_rows=max_rows)

        # Build advanced query XML
        query_body = self._build_advanced_query_xml(sql, max_rows, parameters)

        response = self.adapter.request(
            uri="/sap/bc/adt/datapreview/freestyle",
            method="POST",
            params={},
            body=query_body,
            content_type="application/xml"
        )

        if response.status_code == 200:
            query_data = self._parse_query_results(response.text)
            logger.info(f"Query executed successfully, returned {query_data.get('row_count', 0)} rows")
            return query_data
        else:
            error_msg = f"Failed to execute query: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # Private helper methods

    def _build_sql_select(
        self,
        table_name: str,
        where_clause: Optional[str] = None,
        fields: Optional[List[str]] = None
    ) -> str:
        """
        Build SQL SELECT statement for data preview.

        Format: SELECT TABLE~FIELD1, TABLE~FIELD2 FROM TABLE

        Args:
            table_name: Table name
            where_clause: Optional WHERE clause
            fields: Optional field list

        Returns:
            SQL SELECT statement string
        """
        # Build SELECT clause
        # For now use simple SELECT * - field extraction from DDIC needs namespace fix
        # TODO: Implement proper field extraction once DDIC XML parsing is fixed
        if fields:
            # User provided specific fields
            select_fields = ', '.join([f"{table_name}~{field}" for field in fields])
        else:
            # Use SELECT * for all fields
            select_fields = "*"

        # Build complete SQL
        sql = f"SELECT {select_fields} FROM {table_name}"

        if where_clause:
            sql += f" WHERE {where_clause}"

        logger.debug(f"Built SQL: {sql}")
        return sql

    def _build_query_xml(
        self,
        table_name: str,
        max_rows: int,
        where_clause: Optional[str] = None,
        fields: Optional[List[str]] = None
    ) -> str:
        """
        Build XML body for table query (legacy freestyle endpoint).

        Args:
            table_name: Table name
            max_rows: Maximum rows
            where_clause: Optional WHERE clause
            fields: Optional field list

        Returns:
            XML string for query request
        """
        # Build SELECT clause
        if fields:
            select_clause = ', '.join(fields)
        else:
            select_clause = '*'

        # Build complete SQL
        sql = f"SELECT {select_clause} FROM {table_name}"
        if where_clause:
            sql += f" WHERE {where_clause}"

        # Build XML (simplified version)
        xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<dataPreview:queryRequest xmlns:dataPreview="http://www.sap.com/adt/dataPreview">
    <dataPreview:sqlStatement>{sql}</dataPreview:sqlStatement>
    <dataPreview:maxRows>{max_rows}</dataPreview:maxRows>
</dataPreview:queryRequest>"""

        return xml

    def _build_advanced_query_xml(
        self,
        sql: str,
        max_rows: int,
        parameters: Dict[str, Any]
    ) -> str:
        """
        Build XML body for advanced query.

        Args:
            sql: SQL statement
            max_rows: Maximum rows
            parameters: Query parameters

        Returns:
            XML string for query request
        """
        xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<dataPreview:queryRequest xmlns:dataPreview="http://www.sap.com/adt/dataPreview">
    <dataPreview:sqlStatement>{sql}</dataPreview:sqlStatement>
    <dataPreview:maxRows>{max_rows}</dataPreview:maxRows>
</dataPreview:queryRequest>"""

        return xml

    def _parse_table_data(self, xml_text: str, table_name: str) -> Dict[str, Any]:
        """
        Parse table data XML response from ADT data preview.

        The XML format is column-oriented:
        <dataPreview:columns>
          <dataPreview:metadata name="FIELD1" type="C" .../>
          <dataPreview:dataSet>
            <dataPreview:data>value1</dataPreview:data>
            <dataPreview:data>value2</dataPreview:data>
          </dataPreview:dataSet>
        </dataPreview:columns>

        Args:
            xml_text: XML response from SAP
            table_name: Name of the table queried

        Returns:
            Dictionary with table data
        """
        try:
            root = ET.fromstring(xml_text)

            ns = {'dataPreview': 'http://www.sap.com/adt/dataPreview'}

            table_data = {
                'table_name': table_name,
                'columns': [],
                'rows': [],
                'row_count': 0,
                'metadata': {}
            }

            # Parse column-oriented data
            columns_data = []  # List of (column_name, column_values) tuples

            for columns_elem in root.findall('.//dataPreview:columns', ns):
                # Get metadata
                metadata = columns_elem.find('.//dataPreview:metadata', ns)
                if metadata is not None:
                    col_name = metadata.get(f'{{{ns["dataPreview"]}}}name', '')
                    col_type = metadata.get(f'{{{ns["dataPreview"]}}}type', '')
                    col_desc = metadata.get(f'{{{ns["dataPreview"]}}}description', '')
                    col_length = metadata.get(f'{{{ns["dataPreview"]}}}length', '')

                    table_data['columns'].append({
                        'name': col_name,
                        'type': col_type,
                        'length': col_length,
                        'description': col_desc
                    })

                    # Get data values for this column
                    data_set = columns_elem.find('.//dataPreview:dataSet', ns)
                    if data_set is not None:
                        values = [data.text or '' for data in data_set.findall('.//dataPreview:data', ns)]
                        columns_data.append((col_name, values))

            # Convert column-oriented data to row-oriented
            if columns_data:
                num_rows = len(columns_data[0][1])  # Get row count from first column
                for row_idx in range(num_rows):
                    row = {}
                    for col_name, col_values in columns_data:
                        row[col_name] = col_values[row_idx] if row_idx < len(col_values) else ''
                    table_data['rows'].append(row)

            table_data['row_count'] = len(table_data['rows'])

            return table_data

        except ET.ParseError as e:
            logger.error(f"Failed to parse table data XML: {e}")
            return {
                'table_name': table_name,
                'columns': [],
                'rows': [],
                'row_count': 0,
                'error': str(e),
                'raw_xml': xml_text
            }
        except Exception as e:
            logger.error(f"Unexpected error parsing table data: {e}")
            return {
                'table_name': table_name,
                'columns': [],
                'rows': [],
                'row_count': 0,
                'error': str(e),
                'raw_xml': xml_text
            }

    def _parse_query_results(self, xml_text: str) -> Dict[str, Any]:
        """
        Parse query results XML response.

        Args:
            xml_text: XML response from SAP

        Returns:
            Dictionary with query results
        """
        try:
            # Reuse table data parsing logic
            root = ET.fromstring(xml_text)

            # Try to extract table name from XML if available
            table_name = root.get('tableName', 'QUERY_RESULT')

            return self._parse_table_data(xml_text, table_name)

        except ET.ParseError as e:
            logger.error(f"Failed to parse query results XML: {e}")
            return {
                'columns': [],
                'rows': [],
                'row_count': 0,
                'error': str(e),
                'raw_xml': xml_text
            }
