"""Tests for improved get_transport_objects using E070/E071 tables.

This test suite validates the new implementation that queries E070 and E071
tables directly instead of using the ADT API endpoint.

Test Cases:
1. Main transport with no direct objects (CADK911088)
2. Task with objects (CADK911222 - 19 objects)
3. Task with objects (CADK911089 - 14 objects)
4. Filtering by task within main transport
5. Non-existent transport (error handling)
6. Metadata validation
"""

import os
import logging
from dotenv import load_dotenv
from app.core.config import SAPConfig
from app.core.rfc_connection import RfcConnectionPool
from app.services.transport_service import TransportService

# Configure logging
logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Load environment
load_dotenv()


def setup_service() -> TransportService:
    """Setup TransportService with connection pool."""
    # Verify environment variables
    ashost = os.getenv("SAP_ASHOST", "")
    if not ashost:
        raise ValueError("SAP_ASHOST not configured in .env file")

    sap_config = SAPConfig(
        ashost=ashost,
        sysnr=os.getenv("SAP_SYSNR", ""),
        client=os.getenv("SAP_CLIENT", ""),
        user=os.getenv("SAP_USER", ""),
        passwd=os.getenv("SAP_PASSWD", ""),
        lang=os.getenv("SAP_LANG", "EN"),
        saprouter=os.getenv("SAP_ROUTER"),
    )

    logger.info(f"Connecting to SAP: {sap_config.ashost}:{sap_config.sysnr} (client {sap_config.client})")
    connection_pool = RfcConnectionPool(sap_config, pool_size=1)
    return TransportService(connection_pool)


def test_main_transport_with_tasks():
    """
    Test Case 1: Main transport CADK911088 (OT principal vacío).

    Expected:
    - Type: K (Workbench)
    - Direct objects: 0
    - Tasks: 2 (CADK911222, CADK911089)
    - Total objects: 33 (19 + 14 from tasks)
    """
    logger.info("\n" + "="*80)
    logger.info("TEST 1: Main transport with tasks (CADK911088)")
    logger.info("="*80)

    service = setup_service()
    result = service.get_transport_objects("CADK911088")

    assert result['success'] is True, "Request should succeed"
    assert result['transport_number'] == "CADK911088"

    # Validate metadata
    metadata = result['metadata']
    assert metadata['transport_type'] == 'K', "Should be Workbench transport"
    assert metadata['transport_type_desc'] == 'Workbench'
    assert metadata['owner'] == 'L_ABAPS_ITA'
    assert metadata['created_date'] == '2025-10-29'
    assert metadata['parent_transport'] is None, "Main transport has no parent"

    # Validate tasks
    assert len(result['tasks']) == 2, "Should have 2 tasks"
    task_numbers = [task['task_number'] for task in result['tasks']]
    assert 'CADK911222' in task_numbers, "Should include task CADK911222"
    assert 'CADK911089' in task_numbers, "Should include task CADK911089"

    # Validate total objects (should be sum of all tasks)
    assert result['total_objects'] == 33, f"Expected 33 objects, got {result['total_objects']}"

    logger.info(f"✓ Main transport validated successfully")
    logger.info(f"  Type: {metadata['transport_type_desc']}")
    logger.info(f"  Owner: {metadata['owner']}")
    logger.info(f"  Tasks: {len(result['tasks'])}")
    logger.info(f"  Total objects: {result['total_objects']}")


def test_task_with_objects_1():
    """
    Test Case 2: Task CADK911222.

    Expected:
    - Type: S (Task)
    - Parent: CADK911088
    - Objects: 19 (ZCNEX006, ZCNEX007, CI_PRPS, DTELs, PROGs)
    """
    logger.info("\n" + "="*80)
    logger.info("TEST 2: Task with objects (CADK911222)")
    logger.info("="*80)

    service = setup_service()
    result = service.get_transport_objects("CADK911222")

    assert result['success'] is True, "Request should succeed"
    assert result['transport_number'] == "CADK911222"

    # Validate metadata
    metadata = result['metadata']
    assert metadata['transport_type'] == 'S', "Should be Task"
    assert metadata['transport_type_desc'] == 'Task'
    assert metadata['parent_transport'] == 'CADK911088', "Should have parent transport"
    assert metadata['owner'] == 'L_ABAPS_ITA'

    # Validate objects
    assert result['total_objects'] == 19, f"Expected 19 objects, got {result['total_objects']}"
    assert len(result['tasks']) == 0, "Tasks should not have sub-tasks"

    # Validate object types present
    object_types = {obj['object'] for obj in result['objects']}
    assert 'CMOD' in object_types, "Should contain CMOD objects (enhancements)"
    assert 'PROG' in object_types, "Should contain PROG objects"
    assert 'DTEL' in object_types, "Should contain DTEL objects"

    logger.info(f"✓ Task CADK911222 validated successfully")
    logger.info(f"  Type: {metadata['transport_type_desc']}")
    logger.info(f"  Parent: {metadata['parent_transport']}")
    logger.info(f"  Total objects: {result['total_objects']}")
    logger.info(f"  Object types: {object_types}")


def test_task_with_objects_2():
    """
    Test Case 3: Task CADK911089.

    Expected:
    - Type: S (Task)
    - Parent: CADK911088
    - Objects: 14
    """
    logger.info("\n" + "="*80)
    logger.info("TEST 3: Task with objects (CADK911089)")
    logger.info("="*80)

    service = setup_service()
    result = service.get_transport_objects("CADK911089")

    assert result['success'] is True, "Request should succeed"
    assert result['transport_number'] == "CADK911089"

    # Validate metadata
    metadata = result['metadata']
    assert metadata['transport_type'] == 'S', "Should be Task"
    assert metadata['parent_transport'] == 'CADK911088', "Should have parent transport"

    # Validate objects
    assert result['total_objects'] == 14, f"Expected 14 objects, got {result['total_objects']}"
    assert len(result['tasks']) == 0, "Tasks should not have sub-tasks"

    logger.info(f"✓ Task CADK911089 validated successfully")
    logger.info(f"  Total objects: {result['total_objects']}")


def test_filter_by_task():
    """
    Test Case 4: Filter objects by task within main transport.

    Request: get_transport_objects("CADK911088", task_number="CADK911222")

    Expected:
    - Should return only objects from CADK911222
    - Total objects: 19 (not 33)
    """
    logger.info("\n" + "="*80)
    logger.info("TEST 4: Filter by task (CADK911088 -> CADK911222)")
    logger.info("="*80)

    service = setup_service()
    result = service.get_transport_objects("CADK911088", task_number="CADK911222")

    assert result['success'] is True, "Request should succeed"
    assert result['transport_number'] == "CADK911088"

    # Validate filtered objects
    assert result['total_objects'] == 19, f"Expected 19 filtered objects, got {result['total_objects']}"

    # All objects should belong to CADK911222
    for obj in result['objects']:
        assert obj['trkorr'] == 'CADK911222', f"Object {obj['obj_name']} belongs to wrong task: {obj['trkorr']}"

    logger.info(f"✓ Task filter validated successfully")
    logger.info(f"  Filtered to task: CADK911222")
    logger.info(f"  Objects returned: {result['total_objects']}")


def test_nonexistent_transport():
    """
    Test Case 5: Non-existent transport (error handling).

    Request: get_transport_objects("ZZZK999999")

    Expected:
    - success: False
    - error message present
    - Empty objects/tasks lists
    """
    logger.info("\n" + "="*80)
    logger.info("TEST 5: Non-existent transport (ZZZK999999)")
    logger.info("="*80)

    service = setup_service()
    result = service.get_transport_objects("ZZZK999999")

    assert result['success'] is False, "Should fail for non-existent transport"
    assert 'error' in result, "Should contain error message"
    assert result['total_objects'] == 0, "Should have no objects"
    assert len(result['tasks']) == 0, "Should have no tasks"

    logger.info(f"✓ Error handling validated successfully")
    logger.info(f"  Error: {result.get('error', 'N/A')}")


def test_metadata_completeness():
    """
    Test Case 6: Validate metadata completeness.

    Ensure all expected metadata fields are present and correctly formatted.
    """
    logger.info("\n" + "="*80)
    logger.info("TEST 6: Metadata completeness")
    logger.info("="*80)

    service = setup_service()
    result = service.get_transport_objects("CADK911222")

    metadata = result['metadata']

    # Required fields
    required_fields = [
        'transport_number', 'transport_type', 'transport_type_desc',
        'status', 'status_desc', 'owner', 'created_date', 'created_time',
        'target_system', 'category', 'parent_transport'
    ]

    for field in required_fields:
        assert field in metadata, f"Missing required field: {field}"

    # Validate formats
    assert len(metadata['created_date']) == 10, "Date should be YYYY-MM-DD format"
    assert metadata['created_date'].count('-') == 2, "Date should have 2 hyphens"

    assert len(metadata['created_time']) == 8, "Time should be HH:MM:SS format"
    assert metadata['created_time'].count(':') == 2, "Time should have 2 colons"

    logger.info(f"✓ Metadata completeness validated successfully")
    logger.info(f"  All {len(required_fields)} required fields present")
    logger.info(f"  Date format: {metadata['created_date']}")
    logger.info(f"  Time format: {metadata['created_time']}")


def run_all_tests():
    """Run all test cases."""
    logger.info("\n" + "="*80)
    logger.info("RUNNING ALL TESTS FOR get_transport_objects (E070/E071)")
    logger.info("="*80 + "\n")

    tests = [
        ("Main Transport with Tasks", test_main_transport_with_tasks),
        ("Task with Objects 1", test_task_with_objects_1),
        ("Task with Objects 2", test_task_with_objects_2),
        ("Filter by Task", test_filter_by_task),
        ("Non-existent Transport", test_nonexistent_transport),
        ("Metadata Completeness", test_metadata_completeness),
    ]

    passed = 0
    failed = 0

    for name, test_func in tests:
        try:
            test_func()
            passed += 1
            logger.info(f"\n✅ {name}: PASSED\n")
        except AssertionError as e:
            failed += 1
            logger.error(f"\n❌ {name}: FAILED")
            logger.error(f"   Reason: {e}\n")
        except Exception as e:
            failed += 1
            logger.error(f"\n❌ {name}: ERROR")
            logger.error(f"   Exception: {e}\n")

    logger.info("\n" + "="*80)
    logger.info("TEST SUMMARY")
    logger.info("="*80)
    logger.info(f"Total tests: {len(tests)}")
    logger.info(f"Passed: {passed}")
    logger.info(f"Failed: {failed}")
    logger.info("="*80 + "\n")

    return failed == 0


if __name__ == "__main__":
    success = run_all_tests()
    exit(0 if success else 1)
