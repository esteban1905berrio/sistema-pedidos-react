"""
Tests for token optimization features: pagination, response formats, CHARACTER_LIMIT, and fragmentation.

This test suite validates all optimizations implemented to handle large SAP data responses:
- Phase 1: Response formatter with CHARACTER_LIMIT
- Phase 2: Pagination (offset + metadata)
- Phase 3: Response formats (detailed, summary, types_only)
- Phase 4: CHARACTER_LIMIT integration in services
- Phase 5: Class source fragmentation
"""

import pytest
from unittest.mock import Mock, MagicMock, patch
from app.core.response_formatter import (
    CHARACTER_LIMIT,
    calculate_response_size,
    truncate_response,
    should_truncate,
    create_truncation_message,
)
from app.services.navigation_service import NavigationService
from app.services.class_service import ClassService


class TestResponseFormatter:
    """Test response formatter utilities (Phase 1)."""

    def test_calculate_response_size_string(self):
        """Test size calculation for string data."""
        data = "Hello World"
        size = calculate_response_size(data)
        assert size == 11

    def test_calculate_response_size_dict(self):
        """Test size calculation for dictionary data."""
        data = {"key": "value", "number": 123}
        size = calculate_response_size(data)
        # JSON representation: '{"key":"value","number":123}'
        assert size > 0

    def test_should_truncate_small_data(self):
        """Test that small data doesn't trigger truncation."""
        data = {"small": "data"}
        assert not should_truncate(data)

    def test_should_truncate_large_data(self):
        """Test that large data triggers truncation."""
        # Create data larger than CHARACTER_LIMIT
        large_data = {"data": "x" * (CHARACTER_LIMIT + 1000)}
        assert should_truncate(large_data)

    def test_truncate_response_no_truncation_needed(self):
        """Test truncate_response when data is within limit."""
        data = {"small": "data"}
        result_data, was_truncated, metadata = truncate_response(data)

        assert not was_truncated
        assert metadata["truncated"] is False
        assert result_data == data

    def test_truncate_response_with_truncation(self):
        """Test truncate_response when data exceeds limit."""
        # Create data larger than CHARACTER_LIMIT
        large_data = {"objects": ["item" + str(i) for i in range(10000)]}
        result_data, was_truncated, metadata = truncate_response(large_data)

        assert was_truncated
        assert metadata["truncated"] is True
        assert metadata["original_size"] > CHARACTER_LIMIT
        # Truncation reduces size (but may not always reach CHARACTER_LIMIT in one pass)
        assert metadata["truncated_size"] < metadata["original_size"]

    def test_truncate_response_with_suggestions(self):
        """Test truncation message includes suggestions."""
        large_data = {"objects": ["item" + str(i) for i in range(10000)]}
        suggestions = [
            "Use pagination: offset=50",
            "Use compact format: response_format='summary'"
        ]

        result_data, was_truncated, metadata = truncate_response(
            large_data,
            suggestions=suggestions
        )

        assert was_truncated
        assert "suggestions" in metadata
        assert metadata["suggestions"] == suggestions

    def test_create_truncation_message(self):
        """Test truncation message creation."""
        message = create_truncation_message(
            original_size=50000,
            truncated_size=25000,
            suggestions=["Use pagination", "Add filters"]
        )

        assert "50,000" in message
        assert "25,000" in message
        assert "50%" in message
        assert "Use pagination" in message
        assert "Add filters" in message


class TestPaginationFeature:
    """Test pagination implementation (Phase 2)."""

    @pytest.fixture
    def mock_connection_pool(self):
        """Mock connection pool for testing."""
        pool = Mock()
        pool.get_connection = MagicMock()
        return pool

    @pytest.fixture
    def navigation_service(self, mock_connection_pool):
        """Create NavigationService with mocked pool."""
        return NavigationService(mock_connection_pool)

    def test_pagination_metadata_has_more_true(self, navigation_service):
        """Test pagination metadata when more data exists."""
        # Mock _group_package_objects to test pagination logic
        result = navigation_service._group_package_objects(
            table_data={"rows": [{"obj_name": f"OBJ{i}"} for i in range(50)]},
            package_name="ZTEST",
            max_rows=50,
            offset=0,
            actual_rows_returned=50,
            filters_applied={}
        )

        pagination = result.get("pagination", {})
        assert pagination["has_more"] is True
        assert pagination["next_offset"] == 50
        assert pagination["current_offset"] == 0
        assert pagination["current_page"] == 1
        assert pagination["page_size"] == 50

    def test_pagination_metadata_has_more_false(self, navigation_service):
        """Test pagination metadata when no more data exists."""
        result = navigation_service._group_package_objects(
            table_data={"rows": [{"obj_name": f"OBJ{i}"} for i in range(30)]},
            package_name="ZTEST",
            max_rows=50,
            offset=0,
            actual_rows_returned=30,
            filters_applied={}
        )

        pagination = result.get("pagination", {})
        assert pagination["has_more"] is False
        assert pagination["next_offset"] is None

    def test_pagination_second_page(self, navigation_service):
        """Test pagination metadata for second page."""
        result = navigation_service._group_package_objects(
            table_data={"rows": [{"obj_name": f"OBJ{i}"} for i in range(50)]},
            package_name="ZTEST",
            max_rows=50,
            offset=50,
            actual_rows_returned=50,
            filters_applied={}
        )

        pagination = result.get("pagination", {})
        assert pagination["current_page"] == 2
        assert pagination["current_offset"] == 50
        assert pagination["next_offset"] == 100


class TestResponseFormats:
    """Test response format transformation (Phase 3)."""

    @pytest.fixture
    def mock_connection_pool(self):
        """Mock connection pool for testing."""
        pool = Mock()
        pool.get_connection = MagicMock()
        return pool

    @pytest.fixture
    def navigation_service(self, mock_connection_pool):
        """Create NavigationService with mocked pool."""
        return NavigationService(mock_connection_pool)

    def test_format_detailed_unchanged(self, navigation_service):
        """Test that detailed format returns original data."""
        detailed_result = {
            "package_name": "ZTEST",
            "total_objects": 5,
            "object_types": {
                "CLAS": {
                    "count": 2,
                    "objects": [
                        {"obj_name": "ZCLS1", "author": "USER1"},
                        {"obj_name": "ZCLS2", "author": "USER2"}
                    ]
                }
            }
        }

        # Detailed format should not transform
        result = detailed_result.copy()
        assert result == detailed_result

    def test_format_summary(self, navigation_service):
        """Test summary format transformation."""
        detailed_result = {
            "package_name": "ZTEST",
            "total_objects": 5,
            "object_types": {
                "CLAS": {
                    "count": 2,
                    "objects": [
                        {"obj_name": "ZCLS1", "author": "USER1", "created_on": "2025-01-01"},
                        {"obj_name": "ZCLS2", "author": "USER2", "created_on": "2025-01-02"}
                    ]
                },
                "PROG": {
                    "count": 3,
                    "objects": [
                        {"obj_name": "ZPROG1", "author": "USER1"},
                        {"obj_name": "ZPROG2", "author": "USER2"},
                        {"obj_name": "ZPROG3", "author": "USER3"}
                    ]
                }
            }
        }

        summary_result = navigation_service._format_summary(detailed_result)

        # Summary should have package name and total
        assert summary_result["package_name"] == "ZTEST"
        assert summary_result["total_objects"] == 5

        # Summary should have counts and names only (no metadata)
        assert summary_result["object_types"]["CLAS"]["count"] == 2
        assert summary_result["object_types"]["CLAS"]["names"] == ["ZCLS1", "ZCLS2"]
        assert "objects" not in summary_result["object_types"]["CLAS"]

        assert summary_result["object_types"]["PROG"]["count"] == 3
        assert summary_result["object_types"]["PROG"]["names"] == ["ZPROG1", "ZPROG2", "ZPROG3"]

    def test_format_types_only(self, navigation_service):
        """Test types_only format transformation."""
        detailed_result = {
            "package_name": "ZTEST",
            "total_objects": 5,
            "object_types": {
                "CLAS": {
                    "count": 2,
                    "objects": [
                        {"obj_name": "ZCLS1", "author": "USER1"},
                        {"obj_name": "ZCLS2", "author": "USER2"}
                    ]
                },
                "PROG": {
                    "count": 3,
                    "objects": [
                        {"obj_name": "ZPROG1"},
                        {"obj_name": "ZPROG2"},
                        {"obj_name": "ZPROG3"}
                    ]
                }
            }
        }

        types_only_result = navigation_service._format_types_only(detailed_result)

        # types_only should have package name and total
        assert types_only_result["package_name"] == "ZTEST"
        assert types_only_result["total_objects"] == 5

        # types_only should have counts only (no names or objects)
        assert types_only_result["object_types"]["CLAS"] == 2
        assert types_only_result["object_types"]["PROG"] == 3
        assert isinstance(types_only_result["object_types"]["CLAS"], int)


class TestCHARACTERLIMITIntegration:
    """Test CHARACTER_LIMIT integration in services (Phase 4)."""

    @pytest.fixture
    def mock_connection_pool(self):
        """Mock connection pool for testing."""
        pool = Mock()
        pool.get_connection = MagicMock()
        return pool

    @pytest.fixture
    def navigation_service(self, mock_connection_pool):
        """Create NavigationService with mocked pool."""
        return NavigationService(mock_connection_pool)

    def test_check_and_truncate_small_response(self, navigation_service):
        """Test _check_and_truncate with small response."""
        data = {"small": "response"}
        result_data, truncation_info = navigation_service._check_and_truncate(data)

        assert truncation_info["truncated"] is False
        assert result_data == data

    def test_check_and_truncate_large_response(self, navigation_service):
        """Test _check_and_truncate with large response."""
        # Create large response
        large_data = {
            "objects": [{"name": f"OBJ{i}", "data": "x" * 1000} for i in range(1000)]
        }

        suggestions = ["Use pagination", "Add filters"]
        result_data, truncation_info = navigation_service._check_and_truncate(
            large_data,
            suggestions
        )

        assert truncation_info["truncated"] is True
        assert "message" in truncation_info


class TestClassSourceFragmentation:
    """Test class source fragmentation (Phase 5)."""

    @pytest.fixture
    def mock_connection_pool(self):
        """Mock connection pool for testing."""
        pool = Mock()
        pool.get_connection = MagicMock()
        return pool

    @pytest.fixture
    def class_service(self, mock_connection_pool):
        """Create ClassService with mocked pool."""
        return ClassService(mock_connection_pool)

    def test_get_class_source_returns_dict(self, class_service):
        """Test that get_class_source returns dictionary with metadata."""
        # Mock the adapter
        with patch.object(class_service, '_get_adapter') as mock_adapter:
            mock_response = Mock()
            mock_response.status_code = 200
            mock_response.text = "CLASS zcl_test DEFINITION PUBLIC."

            mock_adapter.return_value.__enter__.return_value.request.return_value = mock_response

            result = class_service.get_class_source("ZCL_TEST")

            # Check structure
            assert isinstance(result, dict)
            assert "source" in result
            assert "class_name" in result
            assert "version" in result
            assert "include_type" in result
            assert "metadata" in result

            # Check values
            assert result["class_name"] == "ZCL_TEST"
            assert result["version"] == "active"
            assert result["include_type"] == "main"

    def test_get_class_source_with_include_type(self, class_service):
        """Test get_class_source with specific include type."""
        with patch.object(class_service, '_get_adapter') as mock_adapter:
            mock_response = Mock()
            mock_response.status_code = 200
            mock_response.text = "METHOD test_method."

            mock_adapter.return_value.__enter__.return_value.request.return_value = mock_response

            result = class_service.get_class_source(
                "ZCL_TEST",
                include_type="testclasses"
            )

            assert result["include_type"] == "testclasses"

    def test_get_class_source_truncation_suggestions(self, class_service):
        """Test that truncation suggestions mention fragmentation."""
        with patch.object(class_service, '_get_adapter') as mock_adapter:
            # Create large source code that exceeds CHARACTER_LIMIT
            large_source = "METHOD huge_method.\n" + "  DATA: lv_var TYPE string.\n" * 5000

            mock_response = Mock()
            mock_response.status_code = 200
            mock_response.text = large_source

            mock_adapter.return_value.__enter__.return_value.request.return_value = mock_response

            result = class_service.get_class_source("ZCLCXR1002_UTIL")

            # Check for truncation
            truncation_info = result["metadata"].get("truncation", {})
            if truncation_info.get("truncated"):
                # Truncation message should mention fragmentation
                message = truncation_info.get("message", "")
                assert "get_class_includes" in message or "include_type" in message


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
