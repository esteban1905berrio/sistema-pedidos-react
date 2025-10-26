"""Debug script for transport_info hanging issue."""

import logging
import sys
from app.core.config import load_config
from app.core.rfc_connection import get_connection_pool
from app.services.transport_service import TransportService

# Configure detailed logging
logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler("logs/debug_transport_info.log"),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


def test_transport_info():
    """Test transport_info with detailed logging."""
    logger.info("=" * 80)
    logger.info("STARTING TRANSPORT_INFO DEBUG TEST")
    logger.info("=" * 80)

    try:
        # Load config
        logger.info("Step 1: Loading configuration...")
        config = load_config()
        logger.info(f"✓ Config loaded: {config.ashost}:{config.sysnr}")

        # Get connection pool
        logger.info("Step 2: Getting connection pool...")
        pool = get_connection_pool(config)
        logger.info(f"✓ Connection pool created with size {pool.pool_size}")

        # Create service
        logger.info("Step 3: Creating TransportService...")
        service = TransportService(pool)
        logger.info("✓ Service created")

        # Test object URI
        obj_uri = "/sap/bc/adt/programs/includes/zsdi1038c_1"
        logger.info(f"Step 4: Calling transport_info for: {obj_uri}")

        # Call with timeout protection
        import signal

        def timeout_handler(signum, frame):
            raise TimeoutError("Operation timed out after 10 seconds")

        # Set 10 second timeout
        signal.signal(signal.SIGALRM, timeout_handler)
        signal.alarm(10)

        try:
            logger.info("  → Calling service.transport_info()...")
            result = service.transport_info(obj_uri)
            signal.alarm(0)  # Cancel alarm

            logger.info("✓ Call completed successfully!")
            logger.info(f"Result keys: {result.keys()}")
            logger.info(f"Total versions: {result.get('total_versions', 'N/A')}")

            if 'versions' in result and len(result['versions']) > 0:
                first_version = result['versions'][0]
                logger.info(f"First version ID: {first_version.get('version_id')}")
                logger.info(f"First transport: {first_version.get('transport_number')}")

            return result

        except TimeoutError as e:
            signal.alarm(0)
            logger.error(f"✗ TIMEOUT: {e}")
            logger.error("The call is hanging - likely in RfcAdapter.request() or RFC call")
            raise
        except Exception as e:
            signal.alarm(0)
            logger.error(f"✗ ERROR during call: {type(e).__name__}: {e}")
            raise

    except Exception as e:
        logger.error(f"✗ FATAL ERROR: {type(e).__name__}: {e}")
        import traceback
        logger.error(traceback.format_exc())
        raise
    finally:
        logger.info("=" * 80)
        logger.info("TEST COMPLETED")
        logger.info("=" * 80)


if __name__ == "__main__":
    try:
        result = test_transport_info()
        print("\n" + "=" * 80)
        print("SUCCESS!")
        print("=" * 80)
        print(f"Object: {result.get('object_name')}")
        print(f"Total versions: {result.get('total_versions')}")
        sys.exit(0)
    except Exception as e:
        print("\n" + "=" * 80)
        print("FAILED!")
        print("=" * 80)
        print(f"Error: {e}")
        sys.exit(1)
