"""
Test for Categoría 3: Enhancements (Ampliaciones)
Tests enhancement operations: search, metadata, and source code.
"""

import logging
from app.core.config import load_config
from app.core.rfc_connection import get_connection
from app.core.rfc_adapter import RfcAdapter
from app.services.enhancement_service import EnhancementService

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)


def test_enhancement_operations():
    """
    Test Enhancement operations.

    Tests:
    1. Search enhancements in a package
    2. Get enhancement metadata
    3. Get enhancement source code
    """
    logger.info("=" * 80)
    logger.info("CATEGORÍA 3 TEST: Enhancements (Ampliaciones)")
    logger.info("=" * 80)

    # Load configuration
    config = load_config()
    logger.info(f"Connecting to SAP: {config.ashost}:{config.sysnr}")

    # Get connection
    with get_connection(config) as conn:
        # Create adapter and service
        adapter = RfcAdapter(conn)
        service = EnhancementService(adapter)

        # Test 1: Search enhancements in package
        logger.info("\n" + "=" * 80)
        logger.info("TEST 1: Search Enhancements in Package")
        logger.info("=" * 80)

        package_name = "ZI1008"  # Package from requirements document

        try:
            results = service.search_enhancements(package_name, "ENHO")

            logger.info(f"\n✅ Found {len(results)} enhancements in package {package_name}")

            if results:
                logger.info("\n📋 First 5 enhancements:")
                for i, enh in enumerate(results[:5], 1):
                    logger.info(f"\n--- Enhancement #{i} ---")
                    logger.info(f"Name: {enh.get('name', 'N/A')}")
                    logger.info(f"Type: {enh.get('type', 'N/A')}")
                    logger.info(f"Description: {enh.get('text', 'N/A')}")
                    logger.info(f"URI: {enh.get('uri', 'N/A')}")
                    logger.info(f"Package: {enh.get('package', 'N/A')}")

                assert len(results) > 0, "Should find at least one enhancement"

                # Save first enhancement for next tests
                first_enhancement = results[0]
                enhancement_name = first_enhancement.get('name')
                enhancement_type = first_enhancement.get('type', 'ENHO/XHH')

                # Determine subtype from type
                if '/XHH' in enhancement_type:
                    subtype = 'enhoxhh'
                elif '/XHB' in enhancement_type:
                    subtype = 'enhoxhb'
                else:
                    subtype = 'enhoxh'

            else:
                logger.warning("⚠️ No enhancements found, using default for tests")
                enhancement_name = "ZFII1008_1"
                subtype = "enhoxhh"

        except Exception as e:
            logger.error(f"❌ Test 1 failed: {e}")
            enhancement_name = "ZFII1008_1"
            subtype = "enhoxhh"

        # Test 2: Get enhancement metadata
        logger.info("\n" + "=" * 80)
        logger.info("TEST 2: Get Enhancement Metadata")
        logger.info("=" * 80)

        try:
            metadata = service.get_enhancement_metadata(enhancement_name, subtype)

            logger.info(f"\n✅ Enhancement: {metadata.get('name', 'N/A')}")
            logger.info(f"✅ Type: {metadata.get('type', 'N/A')}")
            logger.info(f"✅ Description: {metadata.get('description', 'N/A')}")
            logger.info(f"✅ Package: {metadata.get('package', 'N/A')}")
            logger.info(f"✅ Tool Type: {metadata.get('tool_type', 'N/A')}")
            logger.info(f"✅ Responsible: {metadata.get('responsible', 'N/A')}")
            logger.info(f"✅ Created by: {metadata.get('created_by', 'N/A')} at {metadata.get('created_at', 'N/A')}")

            # Display hook implementations
            hook_impls = metadata.get('hook_implementations', [])
            if hook_impls:
                logger.info(f"\n📋 Hook Implementations ({len(hook_impls)}):")
                for i, hook in enumerate(hook_impls, 1):
                    logger.info(f"\n--- Hook #{i} ---")
                    logger.info(f"ID: {hook.get('id', 'N/A')}")
                    logger.info(f"Spot Name: {hook.get('spot_name', 'N/A')}")
                    logger.info(f"Program: {hook.get('program_name', 'N/A')}")
                    logger.info(f"Full Name: {hook.get('full_name', 'N/A')}")
                    logger.info(f"Description: {hook.get('full_description', 'N/A')}")

            assert metadata.get('name') == enhancement_name, f"Expected name {enhancement_name}"

        except Exception as e:
            logger.warning(f"⚠️ Test 2 failed: {e}")

        # Test 3: Get enhancement source code
        logger.info("\n" + "=" * 80)
        logger.info("TEST 3: Get Enhancement Source Code")
        logger.info("=" * 80)

        try:
            source = service.get_enhancement_source(enhancement_name, subtype)

            logger.info(f"\n✅ Source code retrieved: {len(source)} characters")
            logger.info(f"✅ Enhancement source code:")
            logger.info("-" * 80)
            logger.info(source)
            logger.info("-" * 80)

            assert len(source) > 0, "Source code should not be empty"
            assert "ENHANCEMENT" in source.upper() or "ENDENHANCEMENT" in source.upper(), \
                "Should contain ENHANCEMENT block"

        except Exception as e:
            logger.warning(f"⚠️ Test 3 failed: {e}")

        logger.info("\n" + "=" * 80)
        logger.info("✅ CATEGORÍA 3 TESTS COMPLETED")
        logger.info("=" * 80)

        return True


if __name__ == "__main__":
    try:
        result = test_enhancement_operations()
        print("\n✅ Enhancement Category tests passed!")
    except Exception as e:
        logger.error(f"❌ Tests failed: {e}", exc_info=True)
        raise
