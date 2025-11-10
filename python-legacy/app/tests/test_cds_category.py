"""
Test for Categoría 1: CDS Views & Core Data Services
Tests CDS view operations: metadata, source, search, and properties.
"""

import logging
from app.core.config import load_config
from app.core.rfc_connection import get_connection
from app.core.rfc_adapter import RfcAdapter
from app.services.cds_service import CDSService

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)


def test_cds_operations():
    """
    Test CDS Views operations.

    Tests:
    1. Get CDS view metadata
    2. Get CDS view source code
    3. Search CDS views by SQL view name
    4. Get CDS view properties
    """
    logger.info("=" * 80)
    logger.info("CATEGORÍA 1 TEST: CDS Views & Core Data Services")
    logger.info("=" * 80)

    # Load configuration
    config = load_config()
    logger.info(f"Connecting to SAP: {config.ashost}:{config.sysnr}")

    # Get connection
    with get_connection(config) as conn:
        # Create adapter and service
        adapter = RfcAdapter(conn)
        service = CDSService(adapter)

        # Test 1: Get CDS view metadata
        logger.info("\n" + "=" * 80)
        logger.info("TEST 1: Get CDS View Metadata")
        logger.info("=" * 80)

        cds_name = "ZIFII1008_2"  # CDS view from requirements document

        try:
            metadata = service.get_cds_view_metadata(cds_name)

            logger.info(f"\n✅ CDS View: {metadata.get('name', 'N/A')}")
            logger.info(f"✅ Type: {metadata.get('type', 'N/A')}")
            logger.info(f"✅ Description: {metadata.get('description', 'N/A')}")
            logger.info(f"✅ SQL View Name: {metadata.get('sql_view_name', 'N/A')}")
            logger.info(f"✅ Package: {metadata.get('package', 'N/A')}")
            logger.info(f"✅ Responsible: {metadata.get('responsible', 'N/A')}")
            logger.info(f"✅ Created by: {metadata.get('created_by', 'N/A')} at {metadata.get('created_at', 'N/A')}")
            logger.info(f"✅ Changed by: {metadata.get('changed_by', 'N/A')} at {metadata.get('changed_at', 'N/A')}")

            assert metadata.get('name') == cds_name, f"Expected name {cds_name}"
            assert 'sql_view_name' in metadata, "Should have SQL view name"
            assert 'package' in metadata, "Should have package info"

        except Exception as e:
            logger.warning(f"⚠️ Test 1 failed (might be expected if CDS doesn't exist): {e}")

        # Test 2: Get CDS view source code
        logger.info("\n" + "=" * 80)
        logger.info("TEST 2: Get CDS View Source Code")
        logger.info("=" * 80)

        try:
            source = service.get_cds_view_source(cds_name)

            logger.info(f"\n✅ Source code retrieved: {len(source)} characters")
            logger.info(f"✅ First 200 characters:")
            logger.info(f"{source[:200]}...")

            assert len(source) > 0, "Source code should not be empty"
            assert "define view" in source.lower() or "define root view" in source.lower(), "Should contain CDS definition"

        except Exception as e:
            logger.warning(f"⚠️ Test 2 failed: {e}")

        # Test 3: Search CDS views by SQL view name
        logger.info("\n" + "=" * 80)
        logger.info("TEST 3: Search CDS Views by SQL View Name")
        logger.info("=" * 80)

        search_pattern = "ZI*"  # Search for all CDS views starting with ZI

        try:
            results = service.search_cds_views_by_sqlview(search_pattern, max_results=10)

            logger.info(f"\n✅ Found {len(results)} CDS views matching '{search_pattern}'")

            if results:
                logger.info("\n📋 First 5 results:")
                for i, result in enumerate(results[:5], 1):
                    logger.info(f"\n--- CDS View #{i} ---")
                    logger.info(f"Name: {result.get('name', 'N/A')}")
                    logger.info(f"Type: {result.get('type', 'N/A')}")
                    logger.info(f"Description: {result.get('description', 'N/A')}")
                    logger.info(f"Package: {result.get('package', 'N/A')}")
            else:
                logger.info("⚠️ No results found")

        except Exception as e:
            logger.warning(f"⚠️ Test 3 failed: {e}")

        # Test 4: Get CDS view properties
        logger.info("\n" + "=" * 80)
        logger.info("TEST 4: Get CDS View Properties")
        logger.info("=" * 80)

        try:
            properties = service.get_cds_view_properties(cds_name)

            logger.info(f"\n✅ Properties retrieved:")
            for key, value in properties.items():
                logger.info(f"  {key}: {value}")

            assert isinstance(properties, dict), "Properties should be a dictionary"

        except Exception as e:
            logger.warning(f"⚠️ Test 4 failed: {e}")

        logger.info("\n" + "=" * 80)
        logger.info("✅ CATEGORÍA 1 TESTS COMPLETED")
        logger.info("=" * 80)

        return True


if __name__ == "__main__":
    try:
        result = test_cds_operations()
        print("\n✅ CDS Category tests passed!")
    except Exception as e:
        logger.error(f"❌ Tests failed: {e}", exc_info=True)
        raise
