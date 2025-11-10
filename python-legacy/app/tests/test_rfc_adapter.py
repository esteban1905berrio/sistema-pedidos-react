"""Unit tests for RFC adapter."""

import pytest
from unittest.mock import Mock, MagicMock
from app.core.rfc_adapter import RfcAdapter, RfcResponse


class TestRfcResponse:
    """Test RfcResponse wrapper."""

    def test_parse_successful_response(self):
        """Test parsing a successful RFC response."""
        rfc_result = {
            "RESPONSE_LINE": {"STATUS_CODE": "200", "REASON_PHRASE": "OK"},
            "HEADER_FIELDS": [
                {"NAME": "Content-Type", "VALUE": "text/plain"},
                {"NAME": "Content-Length", "VALUE": "42"},
            ],
            "BODY": "Sample response body",
        }

        response = RfcResponse(rfc_result)

        assert response.status_code == 200
        assert response.text == "Sample response body"
        assert response.headers["Content-Type"] == "text/plain"
        assert response.headers["Content-Length"] == "42"

    def test_parse_error_response(self):
        """Test parsing an error RFC response."""
        rfc_result = {
            "RESPONSE_LINE": {"STATUS_CODE": "404", "REASON_PHRASE": "Not Found"},
            "HEADER_FIELDS": [],
            "BODY": "Object not found",
        }

        response = RfcResponse(rfc_result)

        assert response.status_code == 404
        assert response.text == "Object not found"

    def test_parse_response_with_missing_fields(self):
        """Test parsing response with missing fields."""
        rfc_result = {}

        response = RfcResponse(rfc_result)

        assert response.status_code == 200  # Default
        assert response.text == ""
        assert response.headers == {}


class TestRfcAdapter:
    """Test RfcAdapter functionality."""

    @pytest.fixture
    def mock_connection(self):
        """Create a mock RFC connection."""
        conn = Mock()
        conn.call = MagicMock(
            return_value={
                "RESPONSE_LINE": {"STATUS_CODE": "200"},
                "HEADER_FIELDS": [{"NAME": "Content-Type", "VALUE": "text/plain"}],
                "BODY": "Test response",
            }
        )
        return conn

    @pytest.fixture
    def adapter(self, mock_connection):
        """Create RfcAdapter instance with mock connection."""
        return RfcAdapter(mock_connection)

    def test_simple_get_request(self, adapter, mock_connection):
        """Test a simple GET request."""
        response = adapter.request(
            uri="/sap/bc/adt/oo/classes/ZTEST", method="GET", params=None, body=""
        )

        assert response.status_code == 200
        assert response.text == "Test response"

        # Verify RFC was called correctly
        mock_connection.call.assert_called_once()
        call_args = mock_connection.call.call_args
        assert call_args[0][0] == "SADT_REST_RFC_ENDPOINT"

        request_dict = call_args[1]["REQUEST"]
        assert request_dict["REQUEST_LINE"]["METHOD"] == "GET"
        assert request_dict["REQUEST_LINE"]["URI"] == "/sap/bc/adt/oo/classes/ZTEST"

    def test_request_with_query_parameters(self, adapter, mock_connection):
        """Test request with query parameters."""
        response = adapter.request(
            uri="/sap/bc/adt/repository/informationsystem/search",
            method="GET",
            params={"operation": "quickSearch", "query": "ZTEST", "maxResults": 10},
            body="",
        )

        assert response.status_code == 200

        call_args = mock_connection.call.call_args
        request_dict = call_args[1]["REQUEST"]
        uri = request_dict["REQUEST_LINE"]["URI"]

        assert "operation=quickSearch" in uri
        assert "query=ZTEST" in uri
        assert "maxResults=10" in uri

    def test_request_with_body(self, adapter, mock_connection):
        """Test POST request with body."""
        body_content = "<xml>Test content</xml>"

        response = adapter.request(
            uri="/sap/bc/adt/oo/classes/ZTEST/source/main",
            method="PUT",
            params={"lockHandle": "123456"},
            body=body_content,
            content_type="text/plain",
        )

        assert response.status_code == 200

        call_args = mock_connection.call.call_args
        request_dict = call_args[1]["REQUEST"]

        assert request_dict["REQUEST_LINE"]["METHOD"] == "PUT"
        assert request_dict["BODY"] == body_content
        assert "lockHandle=123456" in request_dict["REQUEST_LINE"]["URI"]

    def test_custom_headers(self, adapter, mock_connection):
        """Test request with custom headers."""
        response = adapter.request(
            uri="/sap/bc/adt/test",
            method="GET",
            headers={"X-Custom-Header": "CustomValue", "Accept": "application/json"},
            body="",
        )

        assert response.status_code == 200

        call_args = mock_connection.call.call_args
        request_dict = call_args[1]["REQUEST"]
        headers = {h["NAME"]: h["VALUE"] for h in request_dict["HEADER_FIELDS"]}

        assert headers["X-Custom-Header"] == "CustomValue"
        assert headers["Accept"] == "application/json"

    def test_statefulness_header(self, adapter, mock_connection):
        """Test that statefulness is included in headers."""
        adapter.set_statefulness("stateful")

        response = adapter.request(uri="/sap/bc/adt/test", method="GET", body="")

        call_args = mock_connection.call.call_args
        request_dict = call_args[1]["REQUEST"]
        headers = {h["NAME"]: h["VALUE"] for h in request_dict["HEADER_FIELDS"]}

        assert headers["X-sap-adt-sessiontype"] == "stateful"

    def test_uri_without_params(self, adapter):
        """Test URI building without parameters."""
        uri = adapter._build_uri("/sap/bc/adt/test", None)
        assert uri == "/sap/bc/adt/test"

    def test_uri_with_params(self, adapter):
        """Test URI building with parameters."""
        uri = adapter._build_uri("/sap/bc/adt/test", {"param1": "value1", "param2": "value2"})
        assert "param1=value1" in uri
        assert "param2=value2" in uri
        assert uri.startswith("/sap/bc/adt/test?")

    def test_uri_with_none_params(self, adapter):
        """Test URI building with None parameter values."""
        uri = adapter._build_uri("/sap/bc/adt/test", {"param1": "value1", "param2": None})
        assert "param1=value1" in uri
        assert "param2" not in uri
