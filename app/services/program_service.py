"""Service for ABAP program operations."""

import logging
from typing import Literal

from app.core.rfc_adapter import RfcAdapter

logger = logging.getLogger(__name__)


class ProgramService:
    """Service for ABAP program operations via RFC."""

    def __init__(self, adapter: RfcAdapter):
        """
        Initialize program service.

        Args:
            adapter: RfcAdapter instance for ADT API calls
        """
        self.adapter = adapter

    def get_program_source(
        self, program_name: str, version: Literal["active", "inactive"] = "active"
    ) -> str:
        """
        Get the source code of an ABAP program.

        Args:
            program_name: Name of the ABAP program (e.g., 'ZTEST_PROGRAM')
            version: Version to retrieve ('active' or 'inactive')

        Returns:
            str: Source code of the program

        Raises:
            Exception: If the request fails

        Example:
            >>> service = ProgramService(connection)
            >>> source = service.get_program_source("ZTEST_PROGRAM")
            >>> print(source)
        """
        uri = f"/sap/bc/adt/programs/programs/{program_name}/source/main"
        params = {"version": version} if version else {}

        logger.info(f"Fetching source for program {program_name} ({version})")

        response = self.adapter.request(
            uri=uri,
            method="GET",
            params=params,
            body="",
            content_type="text/plain",
        )

        if response.status_code == 200:
            logger.debug(f"Successfully retrieved source for {program_name}")
            return response.text
        else:
            error_msg = f"{response.status_code} - Failed to get program source for {program_name}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def set_program_source(
        self, program_name: str, source_code: str, lock_handle: str
    ) -> bool:
        """
        Set the source code of an ABAP program.

        Args:
            program_name: Name of the ABAP program
            source_code: New source code
            lock_handle: Lock handle obtained from lock operation

        Returns:
            bool: True if successful

        Raises:
            Exception: If the request fails
        """
        uri = f"/sap/bc/adt/programs/programs/{program_name}/source/main"

        logger.info(f"Setting source for program {program_name}")

        response = self.adapter.request(
            uri=uri,
            method="PUT",
            params={"lockHandle": lock_handle},
            body=source_code,
            content_type="text/plain; charset=utf-8",
        )

        if response.status_code == 200:
            logger.debug(f"Successfully set source for {program_name}")
            return True
        else:
            error_msg = (
                f"{response.status_code} - Failed to set program source for {program_name}"
            )
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_include_source(
        self, program_name: str, include_name: str, version: Literal["active", "inactive"] = "active"
    ) -> str:
        """
        Get the source code of a program include.

        Args:
            program_name: Name of the main ABAP program
            include_name: Name of the include
            version: Version to retrieve

        Returns:
            str: Source code of the include

        Raises:
            Exception: If the request fails
        """
        uri = f"/sap/bc/adt/programs/programs/{program_name}/includes/{include_name}/source/main"
        params = {"version": version} if version else {}

        logger.info(f"Fetching source for include {include_name} in {program_name}")

        response = self.adapter.request(
            uri=uri,
            method="GET",
            params=params,
            body="",
            content_type="text/plain",
        )

        if response.status_code == 200:
            logger.debug(f"Successfully retrieved include {include_name}")
            return response.text
        else:
            error_msg = f"{response.status_code} - Failed to get include source for {include_name}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")
