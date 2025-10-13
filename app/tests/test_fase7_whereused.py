"""
Test for FASE 7: Where-Used Analysis
Tests the get_usage_snippets functionality.
"""

import logging
from app.core.config import load_config
from app.core.rfc_connection import get_connection
from app.core.rfc_adapter import RfcAdapter
from app.services.whereused_service import WhereUsedService

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)


def test_get_usage_snippets():
    """
    Test getting usage snippets for a class.

    This test uses the real class ZCLMMI1229_SINCRONIZA_INV_MAWM
    to find where it's being used in the codebase.
    """
    logger.info("=" * 80)
    logger.info("FASE 7 TEST: Where-Used Analysis")
    logger.info("=" * 80)

    # Load configuration
    config = load_config()
    logger.info(f"Connecting to SAP: {config.ashost}:{config.sysnr}")

    # Get connection
    with get_connection(config) as conn:
        # Create adapter
        adapter = RfcAdapter(conn)

        # Initialize service
        service = WhereUsedService(adapter)

        # Test 1: Get usage snippets for the class
        logger.info("\n" + "=" * 80)
        logger.info("TEST 1: Get usage snippets for ZCLMMI1229_SINCRONIZA_INV_MAWM")
        logger.info("=" * 80)

        # Object identifier format:
        # ABAPFullName;package;program;\TY:classname;version
        object_identifier = (
            "ABAPFullName;ZMMI1229_0;ZMMI1229_0C_1;"
            "\\TY:ZCLMMI1229_SINCRONIZA_INV_MAWM;2"
        )

        result = service.get_usage_snippets(object_identifier)

        logger.info(f"\n✅ Object: {result.get('object_identifier', 'N/A')}")
        logger.info(f"✅ Total usages found: {result.get('total_usages', 0)}")

        # Display first 5 usage snippets
        snippets = result.get("code_snippets", [])
        if snippets:
            logger.info(f"\n📋 Showing first 5 of {len(snippets)} usage snippets:")
            for i, snippet in enumerate(snippets[:5], 1):
                logger.info(f"\n--- Usage #{i} ---")
                logger.info(f"URI: {snippet.get('uri', 'N/A')[:100]}...")
                logger.info(f"Content: {snippet.get('content', 'N/A')}")
                logger.info(f"Matches: {snippet.get('matches', 'N/A')}")
                if snippet.get('description'):
                    desc_preview = snippet['description'][:200]
                    logger.info(f"Description: {desc_preview}...")
        else:
            logger.warning("⚠️ No usage snippets found")

        # Test 2: Get limited usage snippets (max 3 results)
        logger.info("\n" + "=" * 80)
        logger.info("TEST 2: Get usage snippets with max_results=3")
        logger.info("=" * 80)

        result_limited = service.get_usage_snippets(object_identifier, max_results=3)

        logger.info(f"\n✅ Total usages: {result_limited.get('total_usages', 0)}")
        logger.info(f"✅ Shown usages: {result_limited.get('shown_usages', 0)}")
        logger.info(f"✅ Limited: {result_limited.get('limited', False)}")

        snippets_limited = result_limited.get("code_snippets", [])
        logger.info(f"\n📋 Returned {len(snippets_limited)} snippets (limited to 3)")

        for i, snippet in enumerate(snippets_limited, 1):
            logger.info(f"\n--- Limited Usage #{i} ---")
            logger.info(f"Content: {snippet.get('content', 'N/A')}")

        # Assertions
        assert result.get('total_usages', 0) > 0, "Should find at least one usage"
        assert len(result.get('code_snippets', [])) > 0, "Should have code snippets"
        assert len(snippets_limited) <= 3, "Limited results should not exceed max_results"

        logger.info("\n" + "=" * 80)
        logger.info("✅ FASE 7 TEST COMPLETED SUCCESSFULLY")
        logger.info("=" * 80)

        return result


if __name__ == "__main__":
    try:
        result = test_get_usage_snippets()
        print("\n✅ Test passed!")
        print(f"Found {result.get('total_usages', 0)} usages of the class")
    except Exception as e:
        logger.error(f"❌ Test failed: {e}", exc_info=True)
        raise
