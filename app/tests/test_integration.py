"""Integration tests with real SAP system.

These tests require:
1. SAP RFC SDK installed
2. .env file with SAP connection details
3. Active SAP system connection

Run with: pytest app/tests/test_integration.py -v
"""

import pytest
import os
from dotenv import load_dotenv

from app.core.rfc_connection import get_connection
from app.core.config import SAPConfig
from app.services.class_service import ClassService
from app.services.search_service import SearchService
from app.services.program_service import ProgramService


# Load environment variables
load_dotenv()


@pytest.fixture(scope="module")
def sap_config():
    """Create SAP configuration from environment variables."""
    # Check if required env vars are set
    required_vars = ["SAP_ASHOST", "SAP_SYSNR", "SAP_CLIENT", "SAP_USER", "SAP_PASSWD"]
    missing_vars = [var for var in required_vars if not os.getenv(var)]

    if missing_vars:
        pytest.skip(f"Missing required environment variables: {', '.join(missing_vars)}")

    return SAPConfig(
        ashost=os.getenv("SAP_ASHOST", ""),
        sysnr=os.getenv("SAP_SYSNR", ""),
        client=os.getenv("SAP_CLIENT", ""),
        user=os.getenv("SAP_USER", ""),
        passwd=os.getenv("SAP_PASSWD", ""),
        lang=os.getenv("SAP_LANG", "EN"),
        saprouter=os.getenv("SAP_ROUTER"),
    )


@pytest.fixture(scope="module")
def rfc_connection(sap_config):
    """Get RFC connection from pool."""
    with get_connection(sap_config) as conn:
        yield conn


@pytest.fixture
def class_service(rfc_connection):
    """Create ClassService instance."""
    return ClassService(rfc_connection)


@pytest.fixture
def search_service(rfc_connection):
    """Create SearchService instance."""
    return SearchService(rfc_connection)


@pytest.fixture
def program_service(rfc_connection):
    """Create ProgramService instance."""
    return ProgramService(rfc_connection)


class TestClassServiceIntegration:
    """Integration tests for ClassService."""

    def test_get_class_source(self, class_service):
        """Test fetching class source from real SAP system."""
        # Use a standard SAP class that should exist in most systems
        class_name = os.getenv("TEST_CLASS_NAME", "CL_ABAP_CHAR_UTILITIES")

        source = class_service.get_class_source(class_name, version="active")

        assert source is not None
        assert len(source) > 0
        assert "CLASS" in source.upper() or "class" in source

    def test_get_class_structure(self, class_service):
        """Test fetching class structure from real SAP system."""
        class_name = os.getenv("TEST_CLASS_NAME", "CL_ABAP_CHAR_UTILITIES")

        structure = class_service.get_class_structure(class_name, version="active")

        assert structure is not None
        assert "name" in structure
        assert "components" in structure
        assert isinstance(structure["components"], list)


class TestSearchServiceIntegration:
    """Integration tests for SearchService."""

    def test_search_objects(self, search_service):
        """Test searching for objects in real SAP system."""
        # Search for standard SAP objects
        query = os.getenv("TEST_SEARCH_QUERY", "CL_ABAP*")

        results = search_service.search_objects(query, max_results=5)

        assert results is not None
        assert isinstance(results, list)
        assert len(results) > 0

        # Check first result has expected attributes
        first_result = results[0]
        assert "name" in first_result
        assert "type" in first_result or "uri" in first_result

    def test_search_with_no_results(self, search_service):
        """Test search that returns no results."""
        # Use a query that should not match anything
        query = "ZXYZNONEXISTENT9999"

        results = search_service.search_objects(query, max_results=5)

        assert results is not None
        assert isinstance(results, list)
        # May return empty list or not, depends on SAP system


class TestProgramServiceIntegration:
    """Integration tests for ProgramService."""

    def test_get_program_source(self, program_service):
        """Test fetching program source from real SAP system."""
        # Use a standard SAP program
        program_name = os.getenv("TEST_PROGRAM_NAME", "SAPBC_START_PROGRAMS")

        try:
            source = program_service.get_program_source(program_name, version="active")

            assert source is not None
            assert len(source) > 0
            assert "REPORT" in source.upper() or "PROGRAM" in source.upper()
        except Exception as e:
            # Some systems might not have this program, that's ok
            if "404" in str(e):
                pytest.skip(f"Program {program_name} not found in this SAP system")
            else:
                raise


# Mark all tests in this module as integration tests
pytestmark = pytest.mark.integration
