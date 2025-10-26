"""RFC adapter for converting HTTP-style requests to SAP RFC calls."""

import logging
import signal
from typing import Literal, Dict, Any, Optional
from pyrfc import Connection
from app.core.retry_handler import retry_on_network_error, rfc_circuit_breaker

logger = logging.getLogger(__name__)

# Default RFC call timeout in seconds
# Set to 30s to allow for slower network connections and complex operations
# Balance between responsiveness and allowing time for operations to complete
RFC_CALL_TIMEOUT = 30


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
            # Execute RFC call with retry logic and circuit breaker
            result = self._call_with_retry(request_dict)

            # Wrap response
            response = RfcResponse(result)

            logger.debug(f"RFC Response: Status {response.status_code}")
            logger.debug(f"Response body length: {len(response.text)}")

            return response

        except Exception as e:
            logger.error(f"RFC call failed: {e}")
            raise

    @retry_on_network_error()
    @rfc_circuit_breaker
    def _call_with_retry(self, request_dict: Dict[str, Any]) -> Dict[str, Any]:
        """
        Execute RFC call with retry logic and timeout protection.

        This method is decorated with retry logic and circuit breaker.
        It will automatically retry on transient network errors with
        exponential backoff. It also includes a timeout to prevent
        hanging indefinitely on slow/non-responsive endpoints.

        Args:
            request_dict: RFC request dictionary

        Returns:
            Dict[str, Any]: RFC response

        Raises:
            TimeoutError: If RFC call exceeds timeout
            Exception: If all retry attempts fail
        """
        def timeout_handler(signum, frame):
            raise TimeoutError(
                f"RFC call timed out after {RFC_CALL_TIMEOUT} seconds. "
                "The SAP system may be slow, overloaded, or the endpoint may not exist."
            )

        # Set alarm for timeout (Unix only)
        signal.signal(signal.SIGALRM, timeout_handler)
        signal.alarm(RFC_CALL_TIMEOUT)

        try:
            result = self.conn.call("SADT_REST_RFC_ENDPOINT", REQUEST=request_dict)
            signal.alarm(0)  # Cancel alarm on success
            return result
        except Exception as e:
            signal.alarm(0)  # Cancel alarm on error
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
        """
        Build request headers with defaults.

        The Accept header is automatically matched to the Content-Type for:
        - SAP ADT-specific content types (application/vnd.sap.adt.*)
        - Plain text requests (text/plain)

        This ensures SAP returns the expected format and prevents 406 errors.
        """
        # Match Accept header to content_type for SAP ADT and text/plain
        if content_type.startswith("application/vnd.sap.adt"):
            # For SAP ADT-specific content types, use same for Accept
            accept_type = content_type
        elif content_type == "text/plain":
            # For plain text, match Accept to ensure compact responses
            accept_type = "text/plain"
        else:
            # For generic types, accept any format
            accept_type = "*/*"

        default_headers = {
            "Accept": accept_type,
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
