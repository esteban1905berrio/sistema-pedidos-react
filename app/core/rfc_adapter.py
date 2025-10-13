"""RFC adapter for converting HTTP-style requests to SAP RFC calls."""

import logging
from typing import Literal, Dict, Any, Optional
from pyrfc import Connection

logger = logging.getLogger(__name__)


class RfcResponse:
    """Wrapper for RFC response to mimic HTTP response interface."""

    def __init__(self, rfc_result: Dict[str, Any]):
        """
        Initialize RFC response.

        Args:
            rfc_result: Raw result from SADT_REST_RFC_ENDPOINT call
        """
        self._result = rfc_result
        # The actual response is nested under 'RESPONSE' key
        self._response = rfc_result.get("RESPONSE", {})
        self.status_code = self._parse_status_code()
        self.text = self._parse_body()
        self.headers = self._parse_headers()

    def _parse_status_code(self) -> int:
        """Extract HTTP status code from RFC response."""
        try:
            status_line = self._response.get("STATUS_LINE", {})
            status_code_str = status_line.get("STATUS_CODE", "200")
            return int(status_code_str)
        except (ValueError, KeyError) as e:
            logger.warning(f"Failed to parse status code: {e}, defaulting to 200")
            return 200

    def _parse_body(self) -> str:
        """Extract response body from RFC response."""
        message_body = self._response.get("MESSAGE_BODY", b"")

        # MESSAGE_BODY is bytes, need to decode
        if isinstance(message_body, bytes):
            try:
                return message_body.decode("utf-8")
            except UnicodeDecodeError:
                logger.warning("Failed to decode MESSAGE_BODY as UTF-8, trying latin-1")
                return message_body.decode("latin-1", errors="replace")

        return str(message_body) if message_body else ""

    def _parse_headers(self) -> Dict[str, str]:
        """Extract response headers from RFC response."""
        headers = {}
        header_fields = self._response.get("HEADER_FIELDS", [])
        for field in header_fields:
            name = field.get("NAME", "")
            value = field.get("VALUE", "")
            if name:
                headers[name] = value
        return headers


class RfcAdapter:
    """
    Adapter to convert HTTP-style requests to SAP RFC calls.

    This adapter mimics the HTTP request interface used by abap-adt-py
    but executes requests via RFC SADT_REST_RFC_ENDPOINT instead.
    """

    def __init__(self, connection: Connection):
        """
        Initialize the RFC adapter.

        Args:
            connection: Active RFC connection
        """
        self.conn = connection
        self.statefulness: Literal["stateless", "stateful"] = "stateless"

    def request(
        self,
        uri: str,
        method: Literal["GET", "POST", "PUT", "DELETE"],
        headers: Optional[Dict[str, str]] = None,
        params: Optional[Dict[str, Any]] = None,
        body: str = "",
        content_type: str = "application/xml",
    ) -> RfcResponse:
        """
        Execute an HTTP-style request via RFC.

        Args:
            uri: URI path (e.g., "/sap/bc/adt/oo/classes/ZTEST/source/main")
            method: HTTP method
            headers: Additional headers to include
            params: Query parameters
            body: Request body
            content_type: Content-Type header value

        Returns:
            RfcResponse: Response object with status_code, text, and headers

        Raises:
            Exception: If RFC call fails
        """
        # Build full URI with query parameters
        full_uri = self._build_uri(uri, params)

        # Build headers
        request_headers = self._build_headers(headers, content_type)

        # Build RFC request structure
        request_dict = {
            "REQUEST_LINE": {
                "METHOD": method,
                "URI": full_uri,
                "VERSION": "HTTP/1.1",
            },
            "HEADER_FIELDS": [
                {"NAME": name, "VALUE": value} for name, value in request_headers.items()
            ],
        }

        # Add body if present
        # RFC expects MESSAGE_BODY as bytes
        if body:
            # Convert string body to bytes
            if isinstance(body, str):
                request_dict["MESSAGE_BODY"] = body.encode('utf-8')
            else:
                request_dict["MESSAGE_BODY"] = body

        # Log request details
        logger.debug(f"RFC Request: {method} {full_uri}")
        logger.debug(f"Headers: {request_headers}")
        if body:
            logger.debug(f"Body length: {len(body)}")

        try:
            # Execute RFC call
            result = self.conn.call("SADT_REST_RFC_ENDPOINT", REQUEST=request_dict)

            # Wrap response
            response = RfcResponse(result)

            logger.debug(f"RFC Response: Status {response.status_code}")
            logger.debug(f"Response body length: {len(response.text)}")

            return response

        except Exception as e:
            logger.error(f"RFC call failed: {e}")
            raise

    def _build_uri(self, uri: str, params: Optional[Dict[str, Any]]) -> str:
        """Build full URI with query parameters."""
        if not params:
            return uri

        query_parts = []
        for key, value in params.items():
            if value is not None:
                query_parts.append(f"{key}={value}")

        if query_parts:
            query_string = "&".join(query_parts)
            return f"{uri}?{query_string}"

        return uri

    def _build_headers(
        self, headers: Optional[Dict[str, str]], content_type: str
    ) -> Dict[str, str]:
        """Build request headers with defaults."""
        default_headers = {
            "Accept": "*/*",
            "Cache-Control": "no-cache",
            "Content-Type": content_type,
            "X-sap-adt-sessiontype": self.statefulness,
        }

        if headers:
            default_headers.update(headers)

        return default_headers

    def set_statefulness(self, statefulness: Literal["stateless", "stateful"]) -> None:
        """
        Set the session statefulness.

        Args:
            statefulness: "stateful" for operations requiring locks, "stateless" otherwise
        """
        self.statefulness = statefulness
        logger.debug(f"Set statefulness to: {statefulness}")
