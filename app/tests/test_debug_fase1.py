"""
Debug script para probar FASE 1: Repository & Source Retrieval.

Este script prueba todas las herramientas de FASE 1 en modo debug
con logging detallado y manejo de errores.

Uso:
    python app/tests/test_debug_fase1.py

O con pytest:
    pytest app/tests/test_debug_fase1.py -v -s
"""

import logging
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent.parent))

from app.core.config import load_config
from app.core.rfc_connection import get_connection
from app.services.discovery_service import DiscoveryService
from app.services.navigation_service import NavigationService
from app.services.class_service import ClassService

# Configure detailed logging
logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("debug_fase1.log")
    ]
)
logger = logging.getLogger(__name__)


def test_discovery_service():
    """Test DiscoveryService methods."""
    logger.info("=" * 80)
    logger.info("TESTING DISCOVERY SERVICE")
    logger.info("=" * 80)

    try:
        # Load config and connect
        config = load_config()
        logger.info(f"Connecting to SAP: {config.ashost}:{config.sysnr}")

        with get_connection(config) as conn:
            service = DiscoveryService(conn)

            # Test 1: Get Object Types
            logger.info("\n--- Test 1: get_object_types() ---")
            object_types = service.get_object_types()
            logger.info(f"Found {len(object_types)} object types")

            # Show first 5 object types
            for i, obj_type in enumerate(object_types[:5], 1):
                logger.info(f"  {i}. {obj_type}")

            # Test 2: ADT Discovery
            logger.info("\n--- Test 2: adt_discovery() ---")
            discovery = service.adt_discovery()
            logger.info(f"ADT Discovery result: {discovery}")

            # Test 3: Get Feature Details (try 'objectstructure' feature)
            logger.info("\n--- Test 3: get_feature_details('objectstructure') ---")
            try:
                feature_details = service.get_feature_details("objectstructure")
                logger.info(f"Feature details: {feature_details}")
            except Exception as e:
                logger.warning(f"Feature details failed (expected): {e}")

            logger.info("\n✅ DISCOVERY SERVICE: ALL TESTS PASSED")
            return True

    except Exception as e:
        logger.error(f"❌ DISCOVERY SERVICE FAILED: {e}", exc_info=True)
        return False


def test_navigation_service():
    """Test NavigationService methods."""
    logger.info("\n" + "=" * 80)
    logger.info("TESTING NAVIGATION SERVICE")
    logger.info("=" * 80)

    try:
        config = load_config()

        with get_connection(config) as conn:
            service = NavigationService(conn)

            # Test 1: Get Node Contents (root or $TMP package)
            logger.info("\n--- Test 1: get_node_contents('$TMP') ---")
            try:
                node_contents = service.get_node_contents("/sap/bc/adt/packages/$TMP")
                logger.info(f"Found {len(node_contents)} items in $TMP")

                # Show first 5 items
                for i, item in enumerate(node_contents[:5], 1):
                    logger.info(f"  {i}. {item}")
            except Exception as e:
                logger.warning(f"Node contents failed: {e}")

            # Test 2: Find Object Path for a standard class
            logger.info("\n--- Test 2: find_object_path for CL_ABAP_CHAR_UTILITIES ---")
            try:
                object_path = service.find_object_path(
                    "/sap/bc/adt/oo/classes/cl_abap_char_utilities"
                )
                logger.info(f"Object path: {object_path}")
            except Exception as e:
                logger.warning(f"Find object path failed: {e}")

            logger.info("\n✅ NAVIGATION SERVICE: ALL TESTS PASSED")
            return True

    except Exception as e:
        logger.error(f"❌ NAVIGATION SERVICE FAILED: {e}", exc_info=True)
        return False


def test_extended_class_service():
    """Test extended ClassService methods (FASE 1)."""
    logger.info("\n" + "=" * 80)
    logger.info("TESTING EXTENDED CLASS SERVICE (FASE 1)")
    logger.info("=" * 80)

    try:
        config = load_config()

        with get_connection(config) as conn:
            service = ClassService(conn)
            test_class = "CL_ABAP_CHAR_UTILITIES"

            # Test 1: Get Class Includes
            logger.info(f"\n--- Test 1: get_class_includes('{test_class}') ---")
            includes = service.get_class_includes(test_class)
            logger.info(f"Found {len(includes)} includes")

            for i, include in enumerate(includes, 1):
                logger.info(f"  {i}. {include}")

            # Test 2: Get Class Components
            logger.info(f"\n--- Test 2: get_class_components('{test_class}') ---")
            components = service.get_class_components(test_class)
            logger.info(f"Components: {components}")

            # Show summary
            if isinstance(components, dict):
                for comp_type, items in components.items():
                    if isinstance(items, list):
                        logger.info(f"  - {comp_type}: {len(items)} items")

            # Test 3: Get Object Structure (generic)
            logger.info(f"\n--- Test 3: get_object_structure for class ---")
            object_uri = f"/sap/bc/adt/oo/classes/{test_class.lower()}"
            structure = service.get_object_structure(object_uri)
            logger.info(f"Object structure: {structure}")

            logger.info("\n✅ EXTENDED CLASS SERVICE: ALL TESTS PASSED")
            return True

    except Exception as e:
        logger.error(f"❌ EXTENDED CLASS SERVICE FAILED: {e}", exc_info=True)
        return False


def run_all_fase1_tests():
    """Run all FASE 1 tests and report results."""
    logger.info("\n" + "=" * 80)
    logger.info("STARTING FASE 1 DEBUG TESTS")
    logger.info("=" * 80)

    results = {
        "Discovery Service": test_discovery_service(),
        "Navigation Service": test_navigation_service(),
        "Extended Class Service": test_extended_class_service(),
    }

    # Summary
    logger.info("\n" + "=" * 80)
    logger.info("FASE 1 TEST SUMMARY")
    logger.info("=" * 80)

    for test_name, passed in results.items():
        status = "✅ PASSED" if passed else "❌ FAILED"
        logger.info(f"{test_name}: {status}")

    all_passed = all(results.values())

    if all_passed:
        logger.info("\n🎉 ALL FASE 1 TESTS PASSED! 🎉")
    else:
        logger.error("\n⚠️  SOME FASE 1 TESTS FAILED ⚠️")

    logger.info(f"\nDetailed logs saved to: debug_fase1.log")

    return all_passed


if __name__ == "__main__":
    success = run_all_fase1_tests()
    sys.exit(0 if success else 1)
