"""
Debug script para probar lectura de tabla ZTCXR1000_1.

Este script prueba:
1. Obtener estructura DDIC de la tabla
2. Leer contenido de la tabla
3. Aplicar filtros y límites

Uso:
    python app/tests/test_debug_table_ztcxr1000_1.py
"""

import logging
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent.parent))

from app.core.config import load_config
from app.core.rfc_connection import get_connection
from app.services.ddic_service import DdicService
from app.services.query_service import QueryService

# Configure detailed logging
logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("debug_table_ztcxr1000_1.log")
    ]
)
logger = logging.getLogger(__name__)


def test_table_structure():
    """Test getting DDIC structure of ZTCXR1000_1."""
    logger.info("=" * 80)
    logger.info("TEST 1: GET TABLE STRUCTURE (DDIC)")
    logger.info("=" * 80)

    try:
        config = load_config()
        logger.info(f"Connecting to SAP: {config.ashost}:{config.sysnr}")

        with get_connection(config) as conn:
            service = DdicService(conn)

            # Get table definition
            logger.info("\n--- Getting DDIC definition for ZTCXR1000_1 ---")
            table_def = service.get_ddic_element("ZTCXR1000_1", "tables")

            logger.info(f"\nTable Definition Retrieved:")
            logger.info(f"Type: {type(table_def)}")

            if isinstance(table_def, dict):
                logger.info(f"\nTable Metadata:")
                for key, value in table_def.items():
                    if key != 'fields':  # Show fields separately
                        logger.info(f"  {key}: {value}")

                # Show fields structure
                if 'fields' in table_def:
                    fields = table_def['fields']
                    logger.info(f"\nTable has {len(fields)} fields:")
                    for i, field in enumerate(fields, 1):
                        field_name = field.get('name', 'N/A')
                        field_type = field.get('type', 'N/A')
                        field_length = field.get('length', 'N/A')
                        field_desc = field.get('description', 'N/A')
                        logger.info(f"  {i:2d}. {field_name:20s} | {field_type:10s} | Length: {field_length:5s} | {field_desc}")
            else:
                logger.info(f"Raw response:\n{table_def}")

            logger.info("\n✅ TABLE STRUCTURE TEST PASSED")
            return True

    except Exception as e:
        logger.error(f"❌ TABLE STRUCTURE TEST FAILED: {e}", exc_info=True)
        return False


def test_table_contents():
    """Test reading contents of ZTCXR1000_1."""
    logger.info("\n" + "=" * 80)
    logger.info("TEST 2: GET TABLE CONTENTS")
    logger.info("=" * 80)

    try:
        config = load_config()

        with get_connection(config) as conn:
            service = QueryService(conn)

            # Test 1: Get first 10 rows
            logger.info("\n--- Test 1: Get first 10 rows ---")
            table_data = service.get_table_contents("ZTCXR1000_1", max_rows=10)

            logger.info(f"\nTable Contents Retrieved:")
            logger.info(f"Type: {type(table_data)}")

            if isinstance(table_data, dict):
                columns = table_data.get('columns', [])
                rows = table_data.get('rows', [])

                logger.info(f"\nColumns ({len(columns)}):")
                for i, col in enumerate(columns, 1):
                    logger.info(f"  {i:2d}. {col}")

                logger.info(f"\nRows Retrieved: {len(rows)}")

                # Show first 3 rows
                for i, row in enumerate(rows[:3], 1):
                    logger.info(f"\nRow {i}:")
                    if isinstance(row, dict):
                        for key, value in row.items():
                            logger.info(f"  {key}: {value}")
                    else:
                        logger.info(f"  {row}")

                if len(rows) > 3:
                    logger.info(f"\n... and {len(rows) - 3} more rows")
            else:
                logger.info(f"Raw response:\n{table_data}")

            # Test 2: Get with specific fields
            logger.info("\n--- Test 2: Get specific fields only ---")
            try:
                # Try to get first 2 fields (adjust based on actual table structure)
                table_data_filtered = service.get_table_contents(
                    "ZTCXR1000_1",
                    max_rows=5,
                    fields=None  # Will get all fields first time
                )

                if isinstance(table_data_filtered, dict):
                    columns = table_data_filtered.get('columns', [])
                    if columns and len(columns) >= 2:
                        # Now try with specific fields
                        selected_fields = columns[:2]
                        logger.info(f"Requesting only fields: {selected_fields}")

                        table_data_filtered = service.get_table_contents(
                            "ZTCXR1000_1",
                            max_rows=5,
                            fields=selected_fields
                        )

                        logger.info(f"Filtered result: {table_data_filtered}")
            except Exception as e:
                logger.warning(f"Field filtering test failed: {e}")

            logger.info("\n✅ TABLE CONTENTS TEST PASSED")
            return True

    except Exception as e:
        logger.error(f"❌ TABLE CONTENTS TEST FAILED: {e}", exc_info=True)
        return False


def test_table_query_with_filter():
    """Test querying ZTCXR1000_1 with WHERE clause."""
    logger.info("\n" + "=" * 80)
    logger.info("TEST 3: QUERY WITH WHERE CLAUSE")
    logger.info("=" * 80)

    try:
        config = load_config()

        with get_connection(config) as conn:
            service = QueryService(conn)

            logger.info("\n--- Attempting query with WHERE clause ---")
            logger.info("Note: WHERE clause may fail if not supported by ADT data preview")

            try:
                # Try a simple WHERE clause (adjust based on actual fields)
                table_data = service.get_table_contents(
                    "ZTCXR1000_1",
                    max_rows=5,
                    where_clause="MANDT = '100'"  # Common client field
                )

                logger.info(f"Query with WHERE result: {table_data}")

                if isinstance(table_data, dict):
                    rows = table_data.get('rows', [])
                    logger.info(f"Rows returned with filter: {len(rows)}")

            except Exception as e:
                logger.warning(f"WHERE clause query failed (may not be supported): {e}")
                logger.info("This is expected if ADT data preview doesn't support WHERE clauses")

            logger.info("\n✅ QUERY WITH FILTER TEST COMPLETED")
            return True

    except Exception as e:
        logger.error(f"❌ QUERY WITH FILTER TEST FAILED: {e}", exc_info=True)
        return False


def run_all_table_tests():
    """Run all table tests for ZTCXR1000_1."""
    logger.info("\n" + "=" * 80)
    logger.info("STARTING TABLE TESTS FOR ZTCXR1000_1")
    logger.info("=" * 80)

    results = {
        "Table Structure (DDIC)": test_table_structure(),
        "Table Contents": test_table_contents(),
        "Query with Filter": test_table_query_with_filter(),
    }

    # Summary
    logger.info("\n" + "=" * 80)
    logger.info("TABLE TEST SUMMARY")
    logger.info("=" * 80)

    for test_name, passed in results.items():
        status = "✅ PASSED" if passed else "❌ FAILED"
        logger.info(f"{test_name}: {status}")

    all_passed = all(results.values())

    if all_passed:
        logger.info("\n🎉 ALL TABLE TESTS PASSED! 🎉")
    else:
        logger.error("\n⚠️  SOME TABLE TESTS FAILED ⚠️")

    logger.info(f"\nDetailed logs saved to: debug_table_ztcxr1000_1.log")

    return all_passed


if __name__ == "__main__":
    success = run_all_table_tests()
    sys.exit(0 if success else 1)
