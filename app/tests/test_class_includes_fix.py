"""Test for get_class_includes() fix - validate correct URI usage."""

import logging
import os
from dotenv import load_dotenv
from app.core.config import SAPConfig
from app.core.rfc_connection import RfcConnectionPool
from app.services.class_service import ClassService

# Load environment
load_dotenv()

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)


def test_get_class_includes():
    """Test get_class_includes with corrected URI pattern."""

    logger.info("=" * 80)
    logger.info("TEST: get_class_includes() with corrected URI")
    logger.info("=" * 80)

    # Create SAP config
    sap_config = SAPConfig(
        ashost=os.getenv("SAP_ASHOST", ""),
        sysnr=os.getenv("SAP_SYSNR", ""),
        client=os.getenv("SAP_CLIENT", ""),
        user=os.getenv("SAP_USER", ""),
        passwd=os.getenv("SAP_PASSWD", ""),
        lang=os.getenv("SAP_LANG", "EN"),
        saprouter=os.getenv("SAP_ROUTER"),
    )

    # Create connection pool
    connection_pool = RfcConnectionPool(sap_config, pool_size=1)

    service = ClassService(connection_pool)

    # Test with a known class
    class_name = "ZCLCXR1002_UTIL"

    logger.info(f"\nTesting get_class_includes for class: {class_name}")
    logger.info("-" * 80)

    try:
        includes = service.get_class_includes(class_name)

        logger.info(f"\n✅ Successfully retrieved includes for {class_name}")
        logger.info(f"Found {len(includes)} includes:\n")

        for include in includes:
            logger.info(f"  📄 Include Type: {include['include_type']}")
            logger.info(f"     URI: {include['uri']}")
            logger.info(f"     Exists: {include['exists']}")
            logger.info(f"     Size: {include['size_bytes']} bytes")
            logger.info("")

        # Validate expected structure
        assert len(includes) > 0, "Should have at least one include"

        for include in includes:
            assert "include_type" in include, "Include should have 'include_type'"
            assert "uri" in include, "Include should have 'uri'"
            assert "exists" in include, "Include should have 'exists'"
            assert include["exists"] is True, "All returned includes should exist"

            # Validate URI format
            expected_uri = f"/sap/bc/adt/oo/classes/{class_name.lower()}/includes/{include['include_type']}"
            assert include["uri"] == expected_uri, f"URI should match expected format: {expected_uri}"

        logger.info("✅ All validations passed!")

        return includes

    except Exception as e:
        logger.error(f"\n❌ Error: {e}", exc_info=True)
        raise


if __name__ == "__main__":
    test_get_class_includes()
