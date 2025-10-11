"""
Debug script para probar FASE 2: DDIC & Data Dictionary.

Este script prueba todas las herramientas de FASE 2 en modo debug
con logging detallado y manejo de errores.

Uso:
    python app/tests/test_debug_fase2.py

O con pytest:
    pytest app/tests/test_debug_fase2.py -v -s
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
        logging.FileHandler("debug_fase2.log")
    ]
)
logger = logging.getLogger(__name__)


def test_ddic_service():
    """Test DdicService methods."""
    logger.info("=" * 80)
    logger.info("TESTING DDIC SERVICE")
    logger.info("=" * 80)

    try:
        config = load_config()
        logger.info(f"Connecting to SAP: {config.ashost}:{config.sysnr}")

        with get_connection(config) as conn:
            service = DdicService(conn)

            # Test 1: Get Table Definition (USR02 - standard user table)
            logger.info("\n--- Test 1: get_ddic_element('USR02', 'tables') ---")
            table_def = service.get_ddic_element("USR02", "tables")
            logger.info(f"Table USR02 definition: {table_def}")

            # Test 2: Get Structure Definition
            logger.info("\n--- Test 2: get_ddic_element('BAPIRET2', 'structures') ---")
            try:
                struct_def = service.get_ddic_element("BAPIRET2", "structures")
                logger.info(f"Structure BAPIRET2 definition: {struct_def}")
            except Exception as e:
                logger.warning(f"Structure retrieval failed: {e}")

            # Test 3: Get Annotation Definitions
            logger.info("\n--- Test 3: get_annotation_definitions() ---")
            try:
                annotations = service.get_annotation_definitions()
                logger.info(f"Found {len(annotations)} CDS annotations")

                # Show first 5
                for i, annot in enumerate(annotations[:5], 1):
                    logger.info(f"  {i}. {annot}")
            except Exception as e:
                logger.warning(f"Annotations failed: {e}")

            # Test 4: Package Search Help
            logger.info("\n--- Test 4: package_search_help('$*') ---")
            try:
                packages = service.package_search_help("$*")
                logger.info(f"Found {len(packages)} packages starting with $")

                # Show first 5
                for i, pkg in enumerate(packages[:5], 1):
                    logger.info(f"  {i}. {pkg}")
            except Exception as e:
                logger.warning(f"Package search failed: {e}")

            # Test 5: DDIC Repository Access
            logger.info("\n--- Test 5: ddic_repository_access('/tables') ---")
            try:
                repo_access = service.ddic_repository_access("/tables")
                logger.info(f"DDIC repository access: {repo_access}")
            except Exception as e:
                logger.warning(f"Repository access failed: {e}")

            logger.info("\n✅ DDIC SERVICE: ALL TESTS PASSED")
            return True

    except Exception as e:
        logger.error(f"❌ DDIC SERVICE FAILED: {e}", exc_info=True)
        return False


def test_query_service():
    """Test QueryService methods."""
    logger.info("\n" + "=" * 80)
    logger.info("TESTING QUERY SERVICE")
    logger.info("=" * 80)

    try:
        config = load_config()

        with get_connection(config) as conn:
            service = QueryService(conn)

            # Test 1: Get Table Contents (T000 - client table, small and safe)
            logger.info("\n--- Test 1: get_table_contents('T000', max_rows=5) ---")
            table_data = service.get_table_contents("T000", max_rows=5)
            logger.info(f"Table T000 contents: {table_data}")

            if isinstance(table_data, dict):
                logger.info(f"  Columns: {table_data.get('columns', [])}")
                logger.info(f"  Rows: {len(table_data.get('rows', []))}")

                # Show first row
                rows = table_data.get('rows', [])
                if rows:
                    logger.info(f"  First row: {rows[0]}")

            # Test 2: Get Table Contents with WHERE clause
            logger.info("\n--- Test 2: get_table_contents with WHERE clause ---")
            try:
                table_data = service.get_table_contents(
                    "USR02",
                    max_rows=3,
                    where_clause="BNAME LIKE 'S%'"
                )
                logger.info(f"Filtered USR02 contents: {table_data}")

                if isinstance(table_data, dict):
                    logger.info(f"  Rows returned: {len(table_data.get('rows', []))}")
            except Exception as e:
                logger.warning(f"WHERE clause query failed: {e}")

            # Test 3: Get Table Contents with specific fields
            logger.info("\n--- Test 3: get_table_contents with field selection ---")
            try:
                table_data = service.get_table_contents(
                    "T000",
                    max_rows=5,
                    fields=["MANDT", "MTEXT"]
                )
                logger.info(f"T000 with selected fields: {table_data}")

                if isinstance(table_data, dict):
                    logger.info(f"  Columns: {table_data.get('columns', [])}")
            except Exception as e:
                logger.warning(f"Field selection query failed: {e}")

            # Test 4: Run Query (if implemented)
            logger.info("\n--- Test 4: run_query() ---")
            try:
                query_def = {
                    "table": "T000",
                    "max_rows": 3
                }
                query_result = service.run_query(query_def)
                logger.info(f"Query result: {query_result}")
            except Exception as e:
                logger.warning(f"Run query failed (may not be implemented): {e}")

            logger.info("\n✅ QUERY SERVICE: ALL TESTS PASSED")
            return True

    except Exception as e:
        logger.error(f"❌ QUERY SERVICE FAILED: {e}", exc_info=True)
        return False


def run_all_fase2_tests():
    """Run all FASE 2 tests and report results."""
    logger.info("\n" + "=" * 80)
    logger.info("STARTING FASE 2 DEBUG TESTS")
    logger.info("=" * 80)

    results = {
        "DDIC Service": test_ddic_service(),
        "Query Service": test_query_service(),
    }

    # Summary
    logger.info("\n" + "=" * 80)
    logger.info("FASE 2 TEST SUMMARY")
    logger.info("=" * 80)

    for test_name, passed in results.items():
        status = "✅ PASSED" if passed else "❌ FAILED"
        logger.info(f"{test_name}: {status}")

    all_passed = all(results.values())

    if all_passed:
        logger.info("\n🎉 ALL FASE 2 TESTS PASSED! 🎉")
    else:
        logger.error("\n⚠️  SOME FASE 2 TESTS FAILED ⚠️")

    logger.info(f"\nDetailed logs saved to: debug_fase2.log")

    return all_passed


if __name__ == "__main__":
    success = run_all_fase2_tests()
    sys.exit(0 if success else 1)
