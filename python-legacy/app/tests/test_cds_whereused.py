"""
Test for CDS Where-Used Analysis
Tests the complete flow: get_usage_references -> get_usage_snippets
"""

import logging
from app.core.config import load_config
from app.core.rfc_connection import get_connection
from app.core.rfc_adapter import RfcAdapter
from app.services.whereused_service import WhereUsedService

# Configure logging
logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)


def test_cds_whereused_flow():
    """
    Test complete where-used flow for CDS view ZTFI1008_2.

    Flow:
    1. Call get_usage_references to get list of objects that use the CDS
    2. For each reference with object_identifier, call get_usage_snippets
    """
    logger.info("=" * 80)
    logger.info("TEST: CDS Where-Used Analysis Flow")
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

        # Test CDS view
        cds_name = "ZTFI1008_2"
        object_uri = f"/sap/bc/adt/ddic/ddl/sources/{cds_name.lower()}/source/main?version=active"

        # Step 1: Get usage references
        logger.info("\n" + "=" * 80)
        logger.info(f"STEP 1: Get usage references for {cds_name}")
        logger.info("=" * 80)

        references_result = service.get_usage_references(object_uri)

        logger.info(f"\n✅ Total references: {references_result.get('total_references', 0)}")
        logger.info(f"✅ Referenced object ID: {references_result.get('referenced_object_id', 'N/A')}")

        references = references_result.get("references", [])
        logger.info(f"\n📋 Found {len(references)} referencing objects:")

        # Display references
        for i, ref in enumerate(references, 1):
            logger.info(f"\n--- Reference #{i} ---")
            logger.info(f"Name: {ref.get('name', 'N/A')}")
            logger.info(f"Type: {ref.get('type', 'N/A')}")
            logger.info(f"Package: {ref.get('package', 'N/A')}")
            logger.info(f"Description: {ref.get('description', 'N/A')}")
            logger.info(f"URI: {ref.get('uri', 'N/A')}")
            logger.info(f"Can have children: {ref.get('can_have_children', False)}")
            if ref.get('object_identifier'):
                logger.info(f"Object Identifier: {ref['object_identifier']}")
            else:
                logger.info(f"Object Identifier: NOT FOUND")

        # Step 2: Get usage snippets for each reference
        logger.info("\n" + "=" * 80)
        logger.info("STEP 2: Get usage snippets for each reference")
        logger.info("=" * 80)

        snippet_count = 0
        for i, ref in enumerate(references, 1):
            object_id = ref.get('object_identifier')

            # Skip if no object_identifier or if it's a package
            if not object_id or ref.get('type', '').startswith('DEVC'):
                continue

            logger.info(f"\n--- Getting snippets for: {ref.get('name', 'N/A')} ---")

            try:
                snippets_result = service.get_usage_snippets(object_id, max_results=5)

                total = snippets_result.get('total_usages', 0)
                snippets = snippets_result.get('code_snippets', [])

                logger.info(f"✅ Found {total} usages")

                for j, snippet in enumerate(snippets, 1):
                    logger.info(f"\n  Snippet #{j}:")
                    logger.info(f"  URI: {snippet.get('uri', 'N/A')[:80]}...")
                    logger.info(f"  Content: {snippet.get('content', 'N/A')}")
                    logger.info(f"  Matches: {snippet.get('matches', 'N/A')}")
                    snippet_count += 1

            except Exception as e:
                logger.error(f"❌ Error getting snippets for {ref.get('name')}: {e}")

        # Summary
        logger.info("\n" + "=" * 80)
        logger.info("SUMMARY")
        logger.info("=" * 80)
        logger.info(f"✅ CDS View: {cds_name}")
        logger.info(f"✅ Total references: {references_result.get('total_references', 0)}")
        logger.info(f"✅ Code snippets retrieved: {snippet_count}")

        logger.info("\n" + "=" * 80)
        logger.info("✅ TEST COMPLETED SUCCESSFULLY")
        logger.info("=" * 80)

        return references_result


if __name__ == "__main__":
    try:
        result = test_cds_whereused_flow()
        print(f"\n✅ Test passed!")
        print(f"Found {result.get('total_references', 0)} references for ZTFI1008_2")
    except Exception as e:
        logger.error(f"❌ Test failed: {e}", exc_info=True)
        raise
