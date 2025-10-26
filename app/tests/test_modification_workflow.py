"""
Integration tests for ModificationService workflows.

These tests require a live SAP connection and a DEV system.
They test the complete modification workflows (LOCK → MODIFY → UNLOCK → ACTIVATE).
"""

import os
import pytest
from dotenv import load_dotenv

from app.core.config import SAPConfig
from app.core.rfc_connection import RfcConnectionPool
from app.services.modification_service import ModificationService
from app.services.creation_service import CreationService

load_dotenv()


@pytest.fixture
def sap_config():
    """Create SAP configuration from environment."""
    return SAPConfig(
        ashost=os.getenv("SAP_ASHOST", ""),
        sysnr=os.getenv("SAP_SYSNR", ""),
        client=os.getenv("SAP_CLIENT", ""),
        user=os.getenv("SAP_USER", ""),
        passwd=os.getenv("SAP_PASSWD", ""),
        lang=os.getenv("SAP_LANG", "EN"),
        saprouter=os.getenv("SAP_ROUTER"),
    )


@pytest.fixture
def connection_pool(sap_config):
    """Create connection pool."""
    return RfcConnectionPool(sap_config, pool_size=1)


@pytest.fixture
def modification_service(connection_pool):
    """Create modification service."""
    return ModificationService(connection_pool)


@pytest.fixture
def creation_service(connection_pool):
    """Create creation service for setup."""
    return CreationService(connection_pool)


class TestModificationWorkflows:
    """Test high-level modification workflows."""

    def test_modify_class_workflow(self, modification_service, creation_service):
        """Test complete class modification workflow."""
        # Test class
        class_name = "ZCL_TEST_MODIFICATION"
        package = "$TMP"  # Local package for testing

        # Create test class first
        print(f"\n1. Creating test class: {class_name}")
        try:
            creation_result = creation_service.create_class(
                class_name=class_name,
                package=package,
                description="Test class for modification workflow"
            )
            print(f"   ✓ Class created: {creation_result}")
        except Exception as e:
            print(f"   Note: Class may already exist: {e}")

        # Modify class definition
        print(f"\n2. Modifying class definition")
        new_source = f"""CLASS {class_name.lower()} DEFINITION PUBLIC FINAL CREATE PUBLIC.
  PUBLIC SECTION.
    METHODS hello_world
      RETURNING VALUE(rv_message) TYPE string.
ENDCLASS.

CLASS {class_name.lower()} IMPLEMENTATION.
  METHOD hello_world.
    rv_message = 'Hello from modified class!'.
  ENDMETHOD.
ENDCLASS."""

        result = modification_service.modify_class(
            class_name=class_name,
            new_source=new_source,
            include_type="main",
            transport=None,  # $TMP doesn't need transport
            auto_activate=True,
            validate_syntax=True
        )

        # Assertions
        print(f"\n3. Verifying modification results")
        assert result is not None, "Result should not be None"
        assert result["locked"] is True, "Class should be locked"
        assert result["syntax_valid"] is True, "Syntax should be valid"
        assert result["modified"] is True, "Class should be modified"
        assert result["unlocked"] is True, "Class should be unlocked"
        assert result["activated"] is True, "Class should be activated"
        assert result["success"] is True, "Overall workflow should succeed"

        print(f"   ✓ Locked: {result['locked']}")
        print(f"   ✓ Syntax valid: {result['syntax_valid']}")
        print(f"   ✓ Modified: {result['modified']}")
        print(f"   ✓ Unlocked: {result['unlocked']}")
        print(f"   ✓ Activated: {result['activated']}")
        print(f"   ✓ Success: {result['success']}")

        # Check messages
        if result.get("messages"):
            print(f"\n4. Messages from workflow:")
            for msg in result["messages"]:
                print(f"   - [{msg.get('type', 'info')}] {msg.get('text', '')}")

        print(f"\n✓✓✓ Class modification workflow completed successfully")

    def test_modify_class_with_syntax_error(self, modification_service, creation_service):
        """Test modification workflow with intentional syntax error."""
        class_name = "ZCL_TEST_SYNTAX_ERROR"
        package = "$TMP"

        # Create test class
        print(f"\n1. Creating test class: {class_name}")
        try:
            creation_service.create_class(
                class_name=class_name,
                package=package,
                description="Test class for syntax error"
            )
        except Exception as e:
            print(f"   Note: Class may already exist: {e}")

        # Attempt to modify with invalid syntax
        print(f"\n2. Attempting modification with syntax error")
        invalid_source = f"""CLASS {class_name.lower()} DEFINITION PUBLIC.
  PUBLIC SECTION.
    METHODS test_method
      RETURNING VALUE(rv_result TYPE string.  " Missing closing parenthesis
ENDCLASS."""

        # This should raise an exception due to syntax error
        with pytest.raises(Exception) as exc_info:
            modification_service.modify_class(
                class_name=class_name,
                new_source=invalid_source,
                validate_syntax=True  # Syntax check enabled
            )

        print(f"\n3. Syntax error correctly detected:")
        print(f"   Error: {str(exc_info.value)}")
        assert "Syntax validation failed" in str(exc_info.value)
        print(f"   ✓ Workflow correctly prevented saving invalid syntax")

    def test_modify_without_syntax_check(self, modification_service, creation_service):
        """Test modification without syntax validation."""
        class_name = "ZCL_TEST_NO_SYNTAX"
        package = "$TMP"

        # Create test class
        print(f"\n1. Creating test class: {class_name}")
        try:
            creation_service.create_class(
                class_name=class_name,
                package=package,
                description="Test class without syntax check"
            )
        except Exception:
            pass

        # Modify without syntax check
        print(f"\n2. Modifying class without syntax validation")
        new_source = f"""CLASS {class_name.lower()} DEFINITION PUBLIC.
  PUBLIC SECTION.
    METHODS test.
ENDCLASS.

CLASS {class_name.lower()} IMPLEMENTATION.
  METHOD test.
    WRITE: / 'Test'.
  ENDMETHOD.
ENDCLASS."""

        result = modification_service.modify_class(
            class_name=class_name,
            new_source=new_source,
            validate_syntax=False,  # Skip syntax check
            auto_activate=False  # Don't activate (may fail if syntax is bad)
        )

        # Assertions
        print(f"\n3. Verifying results (no syntax check)")
        assert result["locked"] is True
        assert result["syntax_valid"] is True  # Skipped, so marked as valid
        assert result["modified"] is True
        assert result["unlocked"] is True
        # Not checking activated since we set auto_activate=False

        print(f"   ✓ Modification without syntax check completed")


class TestModificationServiceComponents:
    """Test individual components of modification service."""

    def test_build_function_module_uri(self, modification_service):
        """Test URI building for function modules."""
        uri = modification_service._build_function_module_uri(
            "ZTEST_FG",
            "ZTEST_FM"
        )

        expected = "/sap/bc/adt/functions/groups/ztest_fg/fmodules/ztest_fm"
        assert uri == expected, f"Expected {expected}, got {uri}"
        print(f"✓ Function module URI built correctly: {uri}")


if __name__ == "__main__":
    # Run tests with pytest
    pytest.main([__file__, "-v", "-s"])
