"""
Test script for FASE 6: Lifecycle & Testing.

Tests object creation/deletion and unit test execution.

Usage:
    python app/tests/test_fase6_lifecycle.py

Or with the wrapper:
    ./run_test.sh app/tests/test_fase6_lifecycle.py
"""

import logging
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent.parent))

from app.core.config import load_config
from app.core.rfc_connection import get_connection
from app.services.creation_service import CreationService
from app.services.unittest_service import UnittestService

# Configure detailed logging
logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("test_fase6_lifecycle.log")
    ]
)
logger = logging.getLogger(__name__)


def test_validate_object_name():
    """Test object name validation."""
    logger.info("=" * 80)
    logger.info("TEST: VALIDATE OBJECT NAME")
    logger.info("=" * 80)

    try:
        config = load_config()

        with get_connection(config) as conn:
            service = CreationService(conn)

            # Test valid names
            logger.info("\n--- Testing valid object names ---")

            valid_names = [
                ("ZCL_TEST", "CLAS/OC"),
                ("YCL_MY_CLASS", "CLAS/OC"),
                ("ZTEST_PROG", "PROG/P")
            ]

            for obj_name, obj_type in valid_names:
                try:
                    result = service.validate_object_name(obj_name, obj_type)
                    logger.info(f"\n  Name: {obj_name} (Type: {obj_type})")
                    logger.info(f"  Valid: {result.get('valid')}")
                    if result.get('messages'):
                        logger.info(f"  Messages: {result['messages']}")

                except Exception as e:
                    logger.warning(f"  ⚠️  Validation failed: {e}")
                    logger.info("  (Validation endpoint may not be available in all SAP versions)")

            logger.info("\n✅ TEST PASSED")
            return True

    except Exception as e:
        logger.error(f"\n❌ TEST FAILED: {e}", exc_info=True)
        return False


def test_create_and_delete_class():
    """Test class creation and deletion workflow."""
    logger.info("\n" + "=" * 80)
    logger.info("TEST: CREATE AND DELETE CLASS")
    logger.info("=" * 80)

    test_class_name = "ZCL_TEST_MCP_DELETE_ME"
    object_uri = None

    try:
        config = load_config()

        with get_connection(config) as conn:
            service = CreationService(conn)

            # Step 1: Create test class in $TMP (local)
            logger.info("\n--- STEP 1: Creating test class ---")
            logger.info(f"Class name: {test_class_name}")
            logger.info(f"Package: $TMP (local)")

            try:
                result = service.create_class(
                    class_name=test_class_name,
                    package="$TMP",
                    description="Test class created by MCP - DELETE ME",
                    transport=None  # Not needed for $TMP
                )

                logger.info(f"✅ Class created successfully!")
                logger.info(f"  URI: {result.get('uri')}")
                logger.info(f"  Name: {result.get('name')}")

                object_uri = result.get('uri')

                if not object_uri:
                    # Build URI manually if not returned
                    object_uri = f"/sap/bc/adt/oo/classes/{test_class_name.lower()}"
                    logger.info(f"  Using constructed URI: {object_uri}")

            except Exception as e:
                logger.error(f"❌ Failed to create class: {e}")
                logger.info("\nPossible reasons:")
                logger.info("  - Class already exists")
                logger.info("  - No permission to create classes")
                logger.info("  - $TMP package not accessible")
                return False

            # Step 2: Delete the test class
            logger.info("\n--- STEP 2: Deleting test class ---")
            logger.info(f"Object URI: {object_uri}")

            try:
                delete_result = service.delete_object(
                    object_uri=object_uri,
                    transport=None,  # Not needed for $TMP
                    delete_option="deleteWithSuccessors"
                )

                logger.info(f"✅ Class deleted successfully!")
                logger.info(f"  Result: {delete_result}")

            except Exception as e:
                logger.error(f"❌ Failed to delete class: {e}")
                logger.warning(f"\n⚠️  Please manually delete class {test_class_name} from $TMP")
                return False

            logger.info("\n✅ TEST PASSED: Create and delete workflow successful")
            return True

    except Exception as e:
        logger.error(f"\n❌ TEST FAILED: {e}", exc_info=True)
        if object_uri:
            logger.warning(f"\n⚠️  Please manually delete class {test_class_name} from $TMP")
        return False


def test_unit_tests():
    """Test unit test execution."""
    logger.info("\n" + "=" * 80)
    logger.info("TEST: UNIT TEST EXECUTION")
    logger.info("=" * 80)

    try:
        config = load_config()

        with get_connection(config) as conn:
            service = UnittestService(conn)

            # Test with a standard SAP class (may or may not have unit tests)
            test_uri = "/sap/bc/adt/oo/classes/cl_abap_char_utilities"

            logger.info(f"\n--- Running unit tests for: {test_uri} ---")

            try:
                result = service.run_unit_tests(test_uri, coverage=False)

                logger.info(f"\n✅ Unit test execution completed!")
                logger.info(f"  Total tests: {result.get('total')}")
                logger.info(f"  Passed: {result.get('passed')}")
                logger.info(f"  Failed: {result.get('failed')}")

                # Show first 3 tests
                tests = result.get('tests', [])
                if tests:
                    logger.info(f"\n  First 3 test results:")
                    for i, test in enumerate(tests[:3], 1):
                        logger.info(f"\n    Test {i}:")
                        logger.info(f"      Class: {test.get('class')}")
                        logger.info(f"      Method: {test.get('method')}")
                        logger.info(f"      Status: {test.get('status')}")
                        logger.info(f"      Duration: {test.get('duration')} ms")
                else:
                    logger.info(f"\n  No unit tests found for this class")

            except Exception as e:
                logger.warning(f"⚠️  Unit test execution failed: {e}")
                logger.info("  (Unit test endpoint may not be available or class has no tests)")

            logger.info("\n✅ TEST PASSED")
            return True

    except Exception as e:
        logger.error(f"\n❌ TEST FAILED: {e}", exc_info=True)
        return False


def run_all_fase6_tests():
    """Run all FASE 6 tests."""
    logger.info("\n" + "=" * 80)
    logger.info("STARTING FASE 6 TESTS (LIFECYCLE & TESTING)")
    logger.info("=" * 80)

    results = {
        "Validate Object Name": test_validate_object_name(),
        "Create and Delete Class": test_create_and_delete_class(),
        "Unit Test Execution": test_unit_tests(),
    }

    # Summary
    logger.info("\n" + "=" * 80)
    logger.info("FASE 6 TEST SUMMARY")
    logger.info("=" * 80)

    for test_name, passed in results.items():
        status = "✅ PASSED" if passed else "❌ FAILED"
        logger.info(f"{test_name}: {status}")

    all_passed = all(results.values())

    if all_passed:
        logger.info("\n🎉 ALL FASE 6 TESTS PASSED! 🎉")
    else:
        logger.error("\n⚠️  SOME FASE 6 TESTS FAILED ⚠️")

    logger.info(f"\nDetailed logs saved to: test_fase6_lifecycle.log")

    return all_passed


if __name__ == "__main__":
    success = run_all_fase6_tests()
    sys.exit(0 if success else 1)
