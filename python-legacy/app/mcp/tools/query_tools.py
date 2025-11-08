"""MCP tools for ABAP data query and preview operations."""

from typing import Callable, Dict, Any, List, Optional
from mcp.server.fastmcp import FastMCP
from pydantic import Field

from app.services.query_service import QueryService


def register_query_tools(mcp: FastMCP, service_getter: Callable[[], QueryService]):
    """
    Register query-related tools with the MCP server.

    Args:
        mcp: FastMCP server instance
        query_service: Callable that returns QueryService instance (lazy-loaded) for data query operations
    """

    @mcp.tool(
        name="get_table_contents",
        description="Preview contents of an ABAP database table with filtering and limits. "
        "Returns rows, columns, and metadata. Supports WHERE clauses for filtering. "
        "Maximum 1000 rows per request.",
    )
    def get_table_contents(
        table_name: str = Field(
            description="Name of the database table (e.g., 'USR02', 'T000', 'MARA')"
        ),
        max_rows: int = Field(
            default=100,
            description="Maximum number of rows to return (default: 100, max: 1000)"
        ),
        where_clause: Optional[str] = Field(
            default=None,
            description="Optional WHERE clause for filtering (e.g., \"MANDT = '100'\", \"BNAME LIKE 'A%'\")"
        ),
        fields: Optional[List[str]] = Field(
            default=None,
            description="Optional list of specific fields to retrieve (default: all fields)"
        )
    ) -> Dict[str, Any]:
        """
        Retrieve table contents with filtering.

        Returns comprehensive table data:
        - Column definitions (name, type, length)
        - Data rows
        - Row count
        - Metadata

        Supports:
        - WHERE clauses for filtering
        - Field selection for specific columns
        - Row limits for performance

        Example usage:
        - get_table_contents("USR02", max_rows=10)
        - get_table_contents("T000", where_clause="MANDT = '100'", max_rows=5)
        - get_table_contents("MARA", fields=["MATNR", "MTART", "MATKL"], max_rows=20)
        - get_table_contents("USR02", where_clause="BNAME LIKE 'A%'")
        """
        return service_getter().get_table_contents(
            table_name=table_name,
            max_rows=max_rows,
            where_clause=where_clause,
            fields=fields
        )

    @mcp.tool(
        name="run_query",
        description="Execute a custom SQL query or advanced data retrieval. "
        "Supports complex queries with custom SQL statements. "
        "For simple table queries, prefer get_table_contents.",
    )
    def run_query(
        query_definition: Dict[str, Any] = Field(
            description="Query definition with 'sql' (SQL statement or table name), "
                       "'max_rows' (limit), and optional 'parameters'"
        )
    ) -> Dict[str, Any]:
        """
        Execute custom query with advanced options.

        Provides flexible query execution with:
        - Custom SQL statements
        - Query parameters
        - Row limits

        Query definition structure:
        {
            "sql": "SELECT * FROM USR02 WHERE BNAME LIKE 'A%'",
            "max_rows": 50,
            "parameters": {}  # Optional
        }

        Example usage:
        - run_query({"sql": "SELECT * FROM USR02 WHERE BNAME LIKE 'A%'", "max_rows": 50})
        - run_query({"sql": "SELECT MATNR, MTART FROM MARA", "max_rows": 100})
        """
        return service_getter().run_query(query_definition)
