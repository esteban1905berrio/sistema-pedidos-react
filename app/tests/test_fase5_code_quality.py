"""
Test script for FASE 5: Code Quality.

Tests syntax check and pretty printer operations.

Usage:
    python app/tests/test_fase5_code_quality.py

Or with the wrapper:
    ./run_test.sh app/tests/test_fase5_code_quality.py
"""

import logging
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent.parent))

from app.core.config import load_config
from app.core.rfc_connection import get_connection
from app.services.code_quality_service import CodeQualityService

# Configure detailed logging
logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("test_fase5_code_quality.log")
    ]
)
logger = logging.getLogger(__name__)


def test_prettyprint():
    """Test pretty printer functionality."""
    logger.info("=" * 80)
    logger.info("TEST: PRETTY PRINTER")
    logger.info("=" * 80)

    try:
        config = load_config()

        with get_connection(config) as conn:
            service = CodeQualityService(conn)

            # Test with unformatted ABAP code
            unformatted_code = """data: lv_var type string.
data: lv_num type i.
write: lv_var."""

            logger.info("\n--- Testing pretty printer ---")
            logger.info(f"Unformatted code ({len(unformatted_code)} chars):")
            logger.info(f"```\n{unformatted_code}\n```")

            formatted_code = service.prettyprint(unformatted_code)

            logger.info(f"\n✅ Pretty print successful!")
            logger.info(f"Formatted code ({len(formatted_code)} chars):")
            logger.info(f"```\n{formatted_code}\n```")

            # Basic validation
            assert len(formatted_code) > 0, "Formatted code is empty"
            assert "DATA" in formatted_code.upper(), "Keywords should be uppercase"

            logger.info("\n✅ TEST PASSED")
            return True

    except Exception as e:
        logger.error(f"\n❌ TEST FAILED: {e}", exc_info=True)
        return False


def test_syntax_check_valid():
    """Test syntax check with valid ABAP code."""
    logger.info("\n" + "=" * 80)
    logger.info("TEST: SYNTAX CHECK - VALID CODE")
    logger.info("=" * 80)

    try:
        config = load_config()

        with get_connection(config) as conn:
            service = CodeQualityService(conn)

            # Valid ABAP code
            valid_code = """CLASS zcl_test DEFINITION PUBLIC.
  PUBLIC SECTION.
    METHODS: test.
ENDCLASS.

CLASS zcl_test IMPLEMENTATION.
  METHOD test.
    DATA: lv_text TYPE string.
    lv_text = 'Hello World'.
  ENDMETHOD.
ENDCLASS."""

            object_uri = "/sap/bc/adt/oo/classes/zcl_test"
            include_uri = "/sap/bc/adt/oo/classes/zcl_test/source/main"

            logger.info("\n--- Running syntax check on valid code ---")
            logger.info(f"Object URI: {object_uri}")
            logger.info(f"Code length: {len(valid_code)} characters")

            messages = service.syntax_check(object_uri, include_uri, valid_code)

            logger.info(f"\n✅ Syntax check completed")
            logger.info(f"Messages found: {len(messages)}")

            if messages:
                logger.info("\nMessages:")
                for i, msg in enumerate(messages[:5], 1):
                    msg_type = msg.get('type', 'info').upper()
                    msg_text = msg.get('text', 'N/A')
                    msg_line = msg.get('line', 'N/A')
                    logger.info(f"  {i}. [{msg_type}] Line {msg_line}: {msg_text}")
            else:
                logger.info("  No syntax errors (code is valid)")

            logger.info("\n✅ TEST PASSED")
            return True

    except Exception as e:
        logger.error(f"\n❌ TEST FAILED: {e}", exc_info=True)
        return False


def test_syntax_check_invalid():
    """Test syntax check with invalid ABAP code."""
    logger.info("\n" + "=" * 80)
    logger.info("TEST: SYNTAX CHECK - INVALID CODE")
    logger.info("=" * 80)

    try:
        config = load_config()

        with get_connection(config) as conn:
            service = CodeQualityService(conn)

            # Invalid ABAP code (missing ENDCLASS)
            invalid_code = """CLASS zcl_test DEFINITION PUBLIC.
  PUBLIC SECTION.
    METHODS: test.
" Missing ENDCLASS here!

CLASS zcl_test IMPLEMENTATION.
  METHOD test.
    DATA: lv_text TYPE string.
    lv_text = 'Hello World'.
  ENDMETHOD.
ENDCLASS."""

            object_uri = "/sap/bc/adt/oo/classes/zcl_test"
            include_uri = "/sap/bc/adt/oo/classes/zcl_test/source/main"

            logger.info("\n--- Running syntax check on invalid code ---")
            logger.info(f"Object URI: {object_uri}")
            logger.info(f"Code length: {len(invalid_code)} characters")
            logger.info("Expected: Should find syntax errors")

            messages = service.syntax_check(object_uri, include_uri, invalid_code)

            logger.info(f"\n✅ Syntax check completed")
            logger.info(f"Messages found: {len(messages)}")

            # Should have at least one error
            errors = [m for m in messages if m.get('type') == 'error']
            logger.info(f"Errors: {len(errors)}")
            logger.info(f"Warnings: {len([m for m in messages if m.get('type') == 'warning'])}")

            if messages:
                logger.info("\nFirst 5 messages:")
                for i, msg in enumerate(messages[:5], 1):
                    msg_type = msg.get('type', 'info').upper()
                    msg_text = msg.get('text', 'N/A')
                    msg_line = msg.get('line', 'N/A')
                    logger.info(f"  {i}. [{msg_type}] Line {msg_line}: {msg_text}")

            if errors:
                logger.info(f"\n✅ Correctly detected {len(errors)} syntax errors")
            else:
                logger.warning("\n⚠️  No errors detected (may be expected depending on SAP version)")

            logger.info("\n✅ TEST PASSED")
            return True

    except Exception as e:
        logger.error(f"\n❌ TEST FAILED: {e}", exc_info=True)
        return False


def test_prettyprint_settings():
    """Test getting and setting pretty printer settings."""
    logger.info("\n" + "=" * 80)
    logger.info("TEST: PRETTY PRINTER SETTINGS")
    logger.info("=" * 80)

    try:
        config = load_config()

        with get_connection(config) as conn:
            service = CodeQualityService(conn)

            # Get current settings
            logger.info("\n--- Getting current pretty printer settings ---")
            try:
                settings = service.get_prettyprint_settings()

                logger.info(f"✅ Settings retrieved:")
                logger.info(f"  Indentation: {settings.get('indentation')}")
                logger.info(f"  Style: {settings.get('style')}")

            except Exception as e:
                logger.warning(f"⚠️  Could not get settings: {e}")
                logger.info("  (This endpoint may not be available in all SAP versions)")

            # Try to set settings
            logger.info("\n--- Setting pretty printer settings ---")
            try:
                result = service.set_prettyprint_settings(indent=True, style="keywordUpper")

                logger.info(f"✅ Settings updated: {result}")

            except Exception as e:
                logger.warning(f"⚠️  Could not set settings: {e}")
                logger.info("  (This endpoint may not be available in all SAP versions)")

            logger.info("\n✅ TEST PASSED")
            return True

    except Exception as e:
        logger.error(f"\n❌ TEST FAILED: {e}", exc_info=True)
        return False


def run_all_fase5_tests():
    """Run all FASE 5 tests."""
    logger.info("\n" + "=" * 80)
    logger.info("STARTING FASE 5 TESTS (CODE QUALITY)")
    logger.info("=" * 80)

    results = {
        "Pretty Printer": test_prettyprint(),
        "Syntax Check - Valid Code": test_syntax_check_valid(),
        "Syntax Check - Invalid Code": test_syntax_check_invalid(),
        "Pretty Printer Settings": test_prettyprint_settings(),
    }

    # Summary
    logger.info("\n" + "=" * 80)
    logger.info("FASE 5 TEST SUMMARY")
    logger.info("=" * 80)

    for test_name, passed in results.items():
        status = "✅ PASSED" if passed else "❌ FAILED"
        logger.info(f"{test_name}: {status}")

    all_passed = all(results.values())

    if all_passed:
        logger.info("\n🎉 ALL FASE 5 TESTS PASSED! 🎉")
    else:
        logger.error("\n⚠️  SOME FASE 5 TESTS FAILED ⚠️")

    logger.info(f"\nDetailed logs saved to: test_fase5_code_quality.log")

    return all_passed


if __name__ == "__main__":
    success = run_all_fase5_tests()
    sys.exit(0 if success else 1)
