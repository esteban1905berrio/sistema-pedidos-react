"""Service for ABAP object lock/unlock and modification operations."""

import logging
import xml.etree.ElementTree as ET
from typing import Optional

from app.core.rfc_adapter import RfcAdapter

logger = logging.getLogger(__name__)


class ObjectService:
    """
    Service for managing ABAP object locks and modifications.

    This service provides tools to:
    - Lock objects for editing
    - Unlock objects after editing
    - Modify object source code
    """

    def __init__(self, adapter: RfcAdapter):
        """
        Initialize the object service.

        Args:
            adapter: RfcAdapter instance for ADT API calls to SAP system
        """
        self.adapter = adapter
        logger.debug("ObjectService initialized")

    # Sprint 4.1: Lock/Unlock

    def lock(
        self,
        object_uri: str,
        access_mode: str = "MODIFY"
    ) -> str:
        """
        Lock an ABAP object for editing.

        Args:
            object_uri: URI of the object to lock (e.g., '/sap/bc/adt/oo/classes/ztest/source/main')
            access_mode: Access mode (default: "MODIFY")

        Returns:
            LOCK_HANDLE string to be used for subsequent operations

        Example:
            >>> service.lock("/sap/bc/adt/oo/classes/ztest/source/main")
            "XYZ123456789ABC"

        Raises:
            Exception: If lock fails (object already locked, no permissions, etc.)
        """
        logger.info(f"Locking object: {object_uri}")

        response = self.adapter.request(
            uri=object_uri,
            method="POST",
            params={
                "_action": "LOCK",
                "accessMode": access_mode
            },
            body=""
        )

        if response.status_code == 200:
            # Extract LOCK_HANDLE from response
            lock_handle = self._extract_lock_handle(response.text, response.headers)
            logger.info(f"Object locked successfully. Handle: {lock_handle[:20]}...")
            return lock_handle
        else:
            error_msg = f"Failed to lock object: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def unlock(
        self,
        object_uri: str,
        lock_handle: str
    ) -> bool:
        """
        Unlock an ABAP object after editing.

        Args:
            object_uri: URI of the object to unlock
            lock_handle: Lock handle obtained from lock() operation

        Returns:
            True if unlock successful

        Example:
            >>> service.unlock("/sap/bc/adt/oo/classes/ztest/source/main", "XYZ123...")
            True

        Raises:
            Exception: If unlock fails
        """
        logger.info(f"Unlocking object: {object_uri}")

        response = self.adapter.request(
            uri=object_uri,
            method="POST",
            params={
                "_action": "UNLOCK",
                "lockHandle": lock_handle
            },
            body=""
        )

        if response.status_code in [200, 204]:
            logger.info(f"Object unlocked successfully")
            return True
        else:
            error_msg = f"Failed to unlock object: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # Sprint 4.2: Source Management

    def set_object_source(
        self,
        object_uri: str,
        source_code: str,
        lock_handle: str,
        content_type: str = "text/plain; charset=utf-8",
        transport: Optional[str] = None
    ) -> bool:
        """
        Modify the source code of an ABAP object.

        IMPORTANT: Object must be locked before calling this method.

        Args:
            object_uri: URI of the object (with /source/main)
            source_code: New source code content
            lock_handle: Lock handle from lock() operation
            content_type: Content type (default: text/plain with UTF-8)
            transport: Transport number (optional)

        Returns:
            True if modification successful

        Example:
            >>> lock_handle = service.lock("/sap/bc/adt/oo/classes/ztest/source/main")
            >>> service.set_object_source(
            ...     "/sap/bc/adt/oo/classes/ztest/source/main",
            ...     "CLASS ztest DEFINITION PUBLIC...",
            ...     lock_handle
            ... )
            True

        Raises:
            Exception: If modification fails (invalid lock, syntax errors, etc.)
        """
        logger.info(f"Setting source for object: {object_uri}")

        params = {"lockHandle": lock_handle}
        if transport:
            params["corrNr"] = transport

        response = self.adapter.request(
            uri=object_uri,
            method="PUT",
            params=params,
            body=source_code,
            content_type=content_type
        )

        if response.status_code in [200, 204]:
            logger.info(f"Source code updated successfully")
            return True
        else:
            error_msg = f"Failed to set object source: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # Private helper methods

    def _extract_lock_handle(
        self,
        response_text: str,
        response_headers: dict
    ) -> str:
        """
        Extract LOCK_HANDLE from response.

        The lock handle can be in XML body or in headers.

        Args:
            response_text: Response body (XML)
            response_headers: Response headers

        Returns:
            LOCK_HANDLE string

        Raises:
            Exception: If LOCK_HANDLE not found
        """
        # Try to extract from XML body
        try:
            root = ET.fromstring(response_text)

            # Try common paths for LOCK_HANDLE
            lock_handle_elem = root.find(".//LOCK_HANDLE")
            if lock_handle_elem is not None and lock_handle_elem.text:
                return lock_handle_elem.text

            # Try with namespace
            for ns in ["", "lock:", "adtcore:"]:
                lock_handle_elem = root.find(f".//{ns}LOCK_HANDLE")
                if lock_handle_elem is not None and lock_handle_elem.text:
                    return lock_handle_elem.text

            # Check if it's an attribute
            lock_handle = root.get("LOCK_HANDLE")
            if lock_handle:
                return lock_handle

        except ET.ParseError:
            logger.debug("Response is not XML, checking headers")

        # Try to extract from headers
        for header_key in ["LOCK_HANDLE", "lockHandle", "Lock-Handle"]:
            if header_key in response_headers:
                return response_headers[header_key]

        # If response is plain text, it might be the lock handle itself
        if response_text and len(response_text) < 100 and not response_text.startswith("<"):
            # Likely a plain text lock handle
            return response_text.strip()

        raise Exception(
            f"LOCK_HANDLE not found in response.\n"
            f"Response text: {response_text[:200]}\n"
            f"Headers: {response_headers}"
        )
