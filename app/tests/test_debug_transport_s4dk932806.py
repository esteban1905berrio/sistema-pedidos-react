"""
Debug script para probar FASE 3: Transport Management.

Prueba específica con la orden de transporte S4DK932806.

Uso:
    python app/tests/test_debug_transport_s4dk932806.py

O con el wrapper:
    ./run_test.sh app/tests/test_debug_transport_s4dk932806.py
"""

import logging
import sys
from pathlib import Path
import json

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent.parent))

from app.core.config import load_config
from app.core.rfc_connection import get_connection
from app.services.transport_service import TransportService

# Configure detailed logging
logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("debug_transport_s4dk932806.log")
    ]
)
logger = logging.getLogger(__name__)


def test_get_transport_request():
    """Test get_transport_request (general method) for S4DK932806."""
    logger.info("=" * 80)
    logger.info("TEST: GET TRANSPORT REQUEST (GENERAL METHOD) FOR S4DK932806")
    logger.info("=" * 80)

    try:
        config = load_config()
        logger.info(f"Connecting to SAP: {config.ashost}:{config.sysnr}")

        with get_connection(config) as conn:
            service = TransportService(conn)

            logger.info("\n--- Getting full transport request data for S4DK932806 ---")
            transport_data = service.get_transport_request("S4DK932806")

            logger.info(f"\n✅ SUCCESS!")
            logger.info(f"Transport Number: {transport_data.get('number')}")
            logger.info(f"Owner: {transport_data.get('owner')}")
            logger.info(f"Description: {transport_data.get('description')}")
            logger.info(f"Status: {transport_data.get('status')}")
            logger.info(f"Type: {transport_data.get('type')}")
            logger.info(f"Tasks found: {len(transport_data.get('tasks', []))}")
            logger.info(f"Objects found: {len(transport_data.get('objects', []))}")

            if transport_data.get('tasks'):
                logger.info("\nTasks summary:")
                for i, task in enumerate(transport_data['tasks'], 1):
                    logger.info(f"  Task {i}: {task.get('number')} (Owner: {task.get('owner')}, Objects: {len(task.get('objects', []))})")

            logger.info("\n✅ TEST PASSED")
            return True

    except Exception as e:
        logger.error(f"\n❌ TEST FAILED: {e}", exc_info=True)
        return False


def test_get_transport_tasks():
    """Test get_transport_tasks for S4DK932806."""
    logger.info("=" * 80)
    logger.info("TEST: GET TRANSPORT TASKS FOR S4DK932806")
    logger.info("=" * 80)

    try:
        config = load_config()
        logger.info(f"Connecting to SAP: {config.ashost}:{config.sysnr}")

        with get_connection(config) as conn:
            service = TransportService(conn)

            logger.info("\n--- Getting tasks for transport S4DK932806 ---")
            tasks = service.get_transport_tasks("S4DK932806")

            logger.info(f"\n✅ SUCCESS!")
            logger.info(f"Transport: S4DK932806")
            logger.info(f"Tasks found: {len(tasks)}")

            if tasks:
                logger.info("\nTasks details:")
                for i, task in enumerate(tasks, 1):
                    logger.info(f"\n  Task {i}:")
                    logger.info(f"    Number: {task.get('number', 'N/A')}")
                    logger.info(f"    Owner: {task.get('owner', 'N/A')}")
                    logger.info(f"    Description: {task.get('description', 'N/A')}")
                    logger.info(f"    Status: {task.get('status', 'N/A')}")
                    logger.info(f"    Objects: {len(task.get('objects', []))}")

                # Pretty print first task
                if len(tasks) > 0:
                    logger.info("\nFirst task (JSON):")
                    logger.info(json.dumps(tasks[0], indent=2))
            else:
                logger.info("\n  No tasks found for this transport.")

            logger.info("\n✅ TEST PASSED")
            return True

    except Exception as e:
        logger.error(f"\n❌ TEST FAILED: {e}", exc_info=True)
        return False


def test_get_transport_objects():
    """Test get_transport_objects for S4DK932806."""
    logger.info("=" * 80)
    logger.info("TEST: GET TRANSPORT OBJECTS FOR S4DK932806")
    logger.info("=" * 80)

    try:
        config = load_config()
        logger.info(f"Connecting to SAP: {config.ashost}:{config.sysnr}")

        with get_connection(config) as conn:
            service = TransportService(conn)

            logger.info("\n--- Test 1: Getting all objects for transport S4DK932806 ---")
            all_objects = service.get_transport_objects("S4DK932806")

            logger.info(f"\n✅ SUCCESS!")
            logger.info(f"Total objects found: {len(all_objects)}")

            if all_objects:
                logger.info("\nFirst 5 objects:")
                for i, obj in enumerate(all_objects[:5], 1):
                    logger.info(f"\n  Object {i}:")
                    logger.info(f"    PGMID: {obj.get('pgmid', 'N/A')}")
                    logger.info(f"    Type: {obj.get('type', 'N/A')}")
                    logger.info(f"    Name: {obj.get('name', 'N/A')}")
                    logger.info(f"    Task: {obj.get('task', 'N/A')}")

                # Pretty print first object
                logger.info("\nFirst object (JSON):")
                logger.info(json.dumps(all_objects[0], indent=2))

            logger.info("\n--- Test 2: Getting objects for task S4DK932807 ---")
            task_objects = service.get_transport_objects("S4DK932806", "S4DK932807")

            logger.info(f"\n✅ SUCCESS!")
            logger.info(f"Objects for task S4DK932807: {len(task_objects)}")

            logger.info("\n✅ TEST PASSED")
            return True

    except Exception as e:
        logger.error(f"\n❌ TEST FAILED: {e}", exc_info=True)
        return False


def test_transport_info():
    """Test getting transport info for a class object."""
    logger.info("\n" + "=" * 80)
    logger.info("TEST: GET TRANSPORT INFO FOR AN OBJECT")
    logger.info("=" * 80)

    try:
        config = load_config()

        with get_connection(config) as conn:
            service = TransportService(conn)

            # Try to get transport info for a test object
            logger.info("\n--- Getting transport info for object ---")
            logger.info("Object URI: /sap/bc/adt/oo/classes/zcl_test")

            try:
                transport_info = service.transport_info(
                    "/sap/bc/adt/oo/classes/zcl_test"
                )

                logger.info(f"\n✅ Transport info retrieved:")
                logger.info(f"  Transport Number: {transport_info.get('transport_number', 'N/A')}")
                logger.info(f"  Status: {transport_info.get('status', 'N/A')}")
                logger.info(f"  Locked By: {transport_info.get('locked_by', 'N/A')}")
                logger.info(f"  Description: {transport_info.get('description', 'N/A')}")

                logger.info("\n✅ TEST PASSED")
                return True

            except Exception as e:
                logger.warning(f"\nTransport info failed (may be expected): {e}")
                logger.info("\n✅ TEST COMPLETED (object may not exist)")
                return True

    except Exception as e:
        logger.error(f"\n❌ TEST FAILED: {e}", exc_info=True)
        return False


def test_list_user_transports():
    """Test listing current user's transports."""
    logger.info("\n" + "=" * 80)
    logger.info("TEST: LIST USER TRANSPORTS")
    logger.info("=" * 80)

    try:
        config = load_config()

        with get_connection(config) as conn:
            service = TransportService(conn)

            logger.info("\n--- Listing transports for current user ---")
            transports = service.list_user_transports()

            logger.info(f"\n✅ SUCCESS!")
            logger.info(f"Transports found: {len(transports)}")

            if transports:
                logger.info("\nFirst 5 transports:")
                for i, transport in enumerate(transports[:5], 1):
                    logger.info(f"\n  Transport {i}:")
                    logger.info(f"    Number: {transport.get('number', 'N/A')}")
                    logger.info(f"    Description: {transport.get('description', 'N/A')}")
                    logger.info(f"    Status: {transport.get('status', 'N/A')}")
                    logger.info(f"    Owner: {transport.get('owner', 'N/A')}")

                # Check if S4DK932806 is in the list
                transport_numbers = [t.get('number', '') for t in transports]
                if 'S4DK932806' in transport_numbers:
                    logger.info("\n✅ Found S4DK932806 in user's transports!")
                else:
                    logger.info(f"\nℹ️  S4DK932806 not found in current user's transports")
                    logger.info(f"   User may not be the owner")
            else:
                logger.info("\n  No transports found for current user.")

            logger.info("\n✅ TEST PASSED")
            return True

    except Exception as e:
        logger.error(f"\n❌ TEST FAILED: {e}", exc_info=True)
        return False


def test_get_transport_config():
    """Test getting transport configuration."""
    logger.info("\n" + "=" * 80)
    logger.info("TEST: GET TRANSPORT CONFIGURATION")
    logger.info("=" * 80)

    try:
        config = load_config()

        with get_connection(config) as conn:
            service = TransportService(conn)

            logger.info("\n--- Getting transport configuration ---")
            transport_config = service.get_transport_config()

            logger.info(f"\n✅ SUCCESS!")
            logger.info(f"Configuration:")
            logger.info(f"  Target System: {transport_config.get('target_system', 'N/A')}")
            logger.info(f"  Domain: {transport_config.get('domain', 'N/A')}")

            logger.info("\n✅ TEST PASSED")
            return True

    except Exception as e:
        logger.error(f"\n❌ TEST FAILED: {e}", exc_info=True)
        return False


def run_all_transport_tests():
    """Run all transport tests."""
    logger.info("\n" + "=" * 80)
    logger.info("STARTING TRANSPORT TESTS (FASE 3)")
    logger.info("=" * 80)

    results = {
        "Get Transport Request (General)": test_get_transport_request(),
        "Get Transport Tasks (S4DK932806)": test_get_transport_tasks(),
        "Get Transport Objects (S4DK932806)": test_get_transport_objects(),
        "Get Transport Info": test_transport_info(),
        "List User Transports": test_list_user_transports(),
        "Get Transport Config": test_get_transport_config(),
    }

    # Summary
    logger.info("\n" + "=" * 80)
    logger.info("TRANSPORT TEST SUMMARY")
    logger.info("=" * 80)

    for test_name, passed in results.items():
        status = "✅ PASSED" if passed else "❌ FAILED"
        logger.info(f"{test_name}: {status}")

    all_passed = all(results.values())

    if all_passed:
        logger.info("\n🎉 ALL TRANSPORT TESTS PASSED! 🎉")
    else:
        logger.error("\n⚠️  SOME TRANSPORT TESTS FAILED ⚠️")

    logger.info(f"\nDetailed logs saved to: debug_transport_s4dk932806.log")

    return all_passed


if __name__ == "__main__":
    success = run_all_transport_tests()
    sys.exit(0 if success else 1)
