"""Service for high-level ABAP object modification workflows."""

import logging
from typing import Dict, Any, Optional
from contextlib import contextmanager

from app.core.rfc_adapter import RfcAdapter
from app.services.base_service import BaseService
from app.services.object_service import ObjectService
from app.services.activation_service import ActivationService
from app.services.code_quality_service import CodeQualityService

logger = logging.getLogger(__name__)


class ModificationService(BaseService):
    """
    High-level service for complete object modification workflows.

    This service orchestrates the complete ADT modification flow:
    LOCK → [SYNTAX_CHECK] → MODIFY → UNLOCK → ACTIVATE

    Provides type-specific wrappers for:
    - Function Modules
    - Classes
    - Programs
    - Includes
    """

    def __init__(self, connection_pool):
        """Initialize with connection pool and dependent services."""
        super().__init__(connection_pool)
        self.object_service = ObjectService(connection_pool)
        self.activation_service = ActivationService(connection_pool)
        self.quality_service = CodeQualityService(connection_pool)

    # High-Level Workflow Methods

    def modify_function_module(
        self,
        function_module_name: str,
        function_group_name: str,
        new_source: str,
        transport: Optional[str] = None,
        auto_activate: bool = True,
        validate_syntax: bool = True
    ) -> Dict[str, Any]:
        """
        Complete workflow to modify a function module.

        Workflow: LOCK → [SYNTAX_CHECK] → MODIFY → UNLOCK → ACTIVATE

        Args:
            function_module_name: Name of function module (e.g., 'ZFIAAC002_DMEE_NRO_TRASL_DAV')
            function_group_name: Parent function group (e.g., 'ZFIAAC002_1')
            new_source: New source code
            transport: Transport number (required for transportable packages)
            auto_activate: Automatically activate after modification (default: True)
            validate_syntax: Validate syntax before saving (default: True)

        Returns:
            Dictionary with complete workflow results:
            {
                "success": True/False,
                "uri": "/sap/bc/adt/functions/groups/zfg/fmodules/zfm",
                "locked": True,
                "syntax_valid": True,
                "modified": True,
                "unlocked": True,
                "activated": True,
                "messages": [...],
                "lock_handle": "..." (if not auto_activate)
            }

        Example:
            >>> result = service.modify_function_module(
            ...     "ZTEST_FM",
            ...     "ZTEST_FG",
            ...     "FUNCTION ZTEST_FM...\\nENDFUNCTION.",
            ...     transport="DEVK900123"
            ... )
            >>> print(result["success"])
            True

        Raises:
            Exception: If any step fails (lock, syntax, modify, activate)
        """
        logger.info(f"Starting modification workflow for function module: {function_module_name}")

        # Build URIs
        fm_uri = self._build_function_module_uri(function_group_name, function_module_name)
        fm_source_uri = f"{fm_uri}/source/main"

        result = {
            "success": False,
            "uri": fm_uri,
            "function_module": function_module_name,
            "function_group": function_group_name,
            "locked": False,
            "syntax_valid": False,
            "modified": False,
            "unlocked": False,
            "activated": False,
            "messages": [],
            "lock_handle": None
        }

        lock_handle = None

        try:
            # Step 1: Lock object
            logger.info("Step 1/5: Locking object")
            lock_handle = self.object_service.lock(fm_source_uri, access_mode="MODIFY")
            result["locked"] = True
            result["lock_handle"] = lock_handle
            logger.info(f"✓ Object locked (handle: {lock_handle[:20]}...)")

            # Step 2: Syntax check (optional)
            if validate_syntax:
                logger.info("Step 2/5: Validating syntax")
                syntax_result = self.quality_service.syntax_check(
                    object_uri=fm_uri,
                    include_uri=fm_source_uri,
                    source=new_source
                )

                result["syntax_valid"] = syntax_result.get("has_errors", True) == False
                result["messages"].extend(syntax_result.get("messages", []))

                if not result["syntax_valid"]:
                    error_count = len([m for m in syntax_result.get("messages", []) if m.get("type") == "error"])
                    logger.error(f"✗ Syntax validation failed with {error_count} errors")
                    raise Exception(f"Syntax validation failed with {error_count} errors. Check messages for details.")

                logger.info("✓ Syntax validation passed")
            else:
                logger.info("Step 2/5: Skipping syntax validation")
                result["syntax_valid"] = True

            # Step 3: Modify source
            logger.info("Step 3/5: Modifying source code")
            modified = self.object_service.set_object_source(
                object_uri=fm_source_uri,
                source_code=new_source,
                lock_handle=lock_handle,
                transport=transport
            )
            result["modified"] = modified
            logger.info("✓ Source code modified")

            # Step 4: Unlock (always execute in finally block)

        finally:
            # Step 4: Unlock (critical - always execute)
            if lock_handle:
                try:
                    logger.info("Step 4/5: Unlocking object")
                    self.object_service.unlock(fm_source_uri, lock_handle)
                    result["unlocked"] = True
                    logger.info("✓ Object unlocked")
                except Exception as unlock_error:
                    logger.error(f"✗ Failed to unlock object: {unlock_error}")
                    result["messages"].append({
                        "type": "warning",
                        "text": f"Failed to unlock object: {unlock_error}",
                        "step": "unlock"
                    })

        # Step 5: Activate (only if all previous steps succeeded)
        if result["modified"] and auto_activate:
            try:
                logger.info("Step 5/5: Activating object")
                activation_result = self.activation_service.activate(
                    object_name=function_module_name,
                    object_uri=fm_uri,
                    preaudit=True
                )

                result["activated"] = activation_result.get("success", False)
                result["messages"].extend(activation_result.get("messages", []))

                if result["activated"]:
                    logger.info("✓ Object activated successfully")
                else:
                    logger.warning("✗ Activation completed with warnings/errors")

            except Exception as activate_error:
                logger.error(f"✗ Activation failed: {activate_error}")
                result["messages"].append({
                    "type": "error",
                    "text": f"Activation failed: {activate_error}",
                    "step": "activate"
                })
        else:
            logger.info("Step 5/5: Skipping activation (auto_activate=False or modification failed)")

        # Final result
        result["success"] = (
            result["locked"] and
            result["syntax_valid"] and
            result["modified"] and
            result["unlocked"] and
            (result["activated"] if auto_activate else True)
        )

        if result["success"]:
            logger.info(f"✓✓✓ Modification workflow completed successfully for {function_module_name}")
        else:
            logger.error(f"✗✗✗ Modification workflow failed for {function_module_name}")

        return result

    def modify_class(
        self,
        class_name: str,
        new_source: str,
        include_type: str = "main",
        transport: Optional[str] = None,
        auto_activate: bool = True,
        validate_syntax: bool = True
    ) -> Dict[str, Any]:
        """
        Complete workflow to modify an ABAP class.

        Workflow: LOCK → [SYNTAX_CHECK] → MODIFY → UNLOCK → ACTIVATE

        Args:
            class_name: Name of class (e.g., 'ZCL_TEST')
            new_source: New source code
            include_type: Include type (main, implementation, testclasses, macros)
            transport: Transport number (required for transportable packages)
            auto_activate: Automatically activate after modification (default: True)
            validate_syntax: Validate syntax before saving (default: True)

        Returns:
            Dictionary with complete workflow results

        Example:
            >>> result = service.modify_class(
            ...     "ZCL_TEST",
            ...     "CLASS zcl_test DEFINITION PUBLIC...\\nENDCLASS.",
            ...     transport="DEVK900123"
            ... )
            >>> print(result["success"])
            True

        Raises:
            Exception: If any step fails
        """
        logger.info(f"Starting modification workflow for class: {class_name} (include: {include_type})")

        # Build URIs
        class_uri = f"/sap/bc/adt/oo/classes/{class_name.lower()}"
        class_source_uri = f"{class_uri}/source/{include_type}"

        result = {
            "success": False,
            "uri": class_uri,
            "class_name": class_name,
            "include_type": include_type,
            "locked": False,
            "syntax_valid": False,
            "modified": False,
            "unlocked": False,
            "activated": False,
            "messages": [],
            "lock_handle": None
        }

        lock_handle = None

        try:
            # Step 1: Lock
            logger.info("Step 1/5: Locking class")
            lock_handle = self.object_service.lock(class_source_uri, access_mode="MODIFY")
            result["locked"] = True
            result["lock_handle"] = lock_handle
            logger.info(f"✓ Class locked (handle: {lock_handle[:20]}...)")

            # Step 2: Syntax check (optional)
            if validate_syntax:
                logger.info("Step 2/5: Validating syntax")
                syntax_result = self.quality_service.syntax_check(
                    object_uri=class_uri,
                    include_uri=class_source_uri,
                    source=new_source
                )

                result["syntax_valid"] = syntax_result.get("has_errors", True) == False
                result["messages"].extend(syntax_result.get("messages", []))

                if not result["syntax_valid"]:
                    error_count = len([m for m in syntax_result.get("messages", []) if m.get("type") == "error"])
                    logger.error(f"✗ Syntax validation failed with {error_count} errors")
                    raise Exception(f"Syntax validation failed with {error_count} errors")

                logger.info("✓ Syntax validation passed")
            else:
                result["syntax_valid"] = True

            # Step 3: Modify
            logger.info("Step 3/5: Modifying source code")
            modified = self.object_service.set_object_source(
                object_uri=class_source_uri,
                source_code=new_source,
                lock_handle=lock_handle,
                transport=transport
            )
            result["modified"] = modified
            logger.info("✓ Source code modified")

        finally:
            # Step 4: Unlock
            if lock_handle:
                try:
                    logger.info("Step 4/5: Unlocking class")
                    self.object_service.unlock(class_source_uri, lock_handle)
                    result["unlocked"] = True
                    logger.info("✓ Class unlocked")
                except Exception as unlock_error:
                    logger.error(f"✗ Failed to unlock: {unlock_error}")
                    result["messages"].append({
                        "type": "warning",
                        "text": f"Failed to unlock: {unlock_error}",
                        "step": "unlock"
                    })

        # Step 5: Activate
        if result["modified"] and auto_activate:
            try:
                logger.info("Step 5/5: Activating class")
                activation_result = self.activation_service.activate(
                    object_name=class_name,
                    object_uri=class_uri,
                    preaudit=True
                )

                result["activated"] = activation_result.get("success", False)
                result["messages"].extend(activation_result.get("messages", []))

                if result["activated"]:
                    logger.info("✓ Class activated successfully")
                else:
                    logger.warning("✗ Activation completed with warnings")

            except Exception as activate_error:
                logger.error(f"✗ Activation failed: {activate_error}")
                result["messages"].append({
                    "type": "error",
                    "text": f"Activation failed: {activate_error}",
                    "step": "activate"
                })

        # Final result
        result["success"] = (
            result["locked"] and
            result["syntax_valid"] and
            result["modified"] and
            result["unlocked"] and
            (result["activated"] if auto_activate else True)
        )

        if result["success"]:
            logger.info(f"✓✓✓ Class modification completed successfully: {class_name}")
        else:
            logger.error(f"✗✗✗ Class modification failed: {class_name}")

        return result

    def modify_program(
        self,
        program_name: str,
        new_source: str,
        transport: Optional[str] = None,
        auto_activate: bool = True,
        validate_syntax: bool = True
    ) -> Dict[str, Any]:
        """
        Complete workflow to modify an ABAP program/report.

        Workflow: LOCK → [SYNTAX_CHECK] → MODIFY → UNLOCK → ACTIVATE

        Args:
            program_name: Name of program (e.g., 'ZTEST_REPORT')
            new_source: New source code
            transport: Transport number (required for transportable packages)
            auto_activate: Automatically activate after modification (default: True)
            validate_syntax: Validate syntax before saving (default: True)

        Returns:
            Dictionary with complete workflow results

        Example:
            >>> result = service.modify_program(
            ...     "ZTEST_REPORT",
            ...     "REPORT ztest_report.\\nWRITE: / 'Hello World'.",
            ...     transport="DEVK900123"
            ... )
            >>> print(result["success"])
            True

        Raises:
            Exception: If any step fails
        """
        logger.info(f"Starting modification workflow for program: {program_name}")

        # Build URIs
        program_uri = f"/sap/bc/adt/programs/programs/{program_name.lower()}"
        program_source_uri = f"{program_uri}/source/main"

        result = {
            "success": False,
            "uri": program_uri,
            "program_name": program_name,
            "locked": False,
            "syntax_valid": False,
            "modified": False,
            "unlocked": False,
            "activated": False,
            "messages": [],
            "lock_handle": None
        }

        lock_handle = None

        try:
            # Step 1: Lock
            logger.info("Step 1/5: Locking program")
            lock_handle = self.object_service.lock(program_source_uri, access_mode="MODIFY")
            result["locked"] = True
            result["lock_handle"] = lock_handle
            logger.info(f"✓ Program locked (handle: {lock_handle[:20]}...)")

            # Step 2: Syntax check (optional)
            if validate_syntax:
                logger.info("Step 2/5: Validating syntax")
                syntax_result = self.quality_service.syntax_check(
                    object_uri=program_uri,
                    include_uri=program_source_uri,
                    source=new_source
                )

                result["syntax_valid"] = syntax_result.get("has_errors", True) == False
                result["messages"].extend(syntax_result.get("messages", []))

                if not result["syntax_valid"]:
                    error_count = len([m for m in syntax_result.get("messages", []) if m.get("type") == "error"])
                    logger.error(f"✗ Syntax validation failed with {error_count} errors")
                    raise Exception(f"Syntax validation failed with {error_count} errors")

                logger.info("✓ Syntax validation passed")
            else:
                result["syntax_valid"] = True

            # Step 3: Modify
            logger.info("Step 3/5: Modifying source code")
            modified = self.object_service.set_object_source(
                object_uri=program_source_uri,
                source_code=new_source,
                lock_handle=lock_handle,
                transport=transport
            )
            result["modified"] = modified
            logger.info("✓ Source code modified")

        finally:
            # Step 4: Unlock
            if lock_handle:
                try:
                    logger.info("Step 4/5: Unlocking program")
                    self.object_service.unlock(program_source_uri, lock_handle)
                    result["unlocked"] = True
                    logger.info("✓ Program unlocked")
                except Exception as unlock_error:
                    logger.error(f"✗ Failed to unlock: {unlock_error}")
                    result["messages"].append({
                        "type": "warning",
                        "text": f"Failed to unlock: {unlock_error}",
                        "step": "unlock"
                    })

        # Step 5: Activate
        if result["modified"] and auto_activate:
            try:
                logger.info("Step 5/5: Activating program")
                activation_result = self.activation_service.activate(
                    object_name=program_name,
                    object_uri=program_uri,
                    preaudit=True
                )

                result["activated"] = activation_result.get("success", False)
                result["messages"].extend(activation_result.get("messages", []))

                if result["activated"]:
                    logger.info("✓ Program activated successfully")
                else:
                    logger.warning("✗ Activation completed with warnings")

            except Exception as activate_error:
                logger.error(f"✗ Activation failed: {activate_error}")
                result["messages"].append({
                    "type": "error",
                    "text": f"Activation failed: {activate_error}",
                    "step": "activate"
                })

        # Final result
        result["success"] = (
            result["locked"] and
            result["syntax_valid"] and
            result["modified"] and
            result["unlocked"] and
            (result["activated"] if auto_activate else True)
        )

        if result["success"]:
            logger.info(f"✓✓✓ Program modification completed successfully: {program_name}")
        else:
            logger.error(f"✗✗✗ Program modification failed: {program_name}")

        return result

    def modify_include(
        self,
        include_name: str,
        program_name: str,
        new_source: str,
        transport: Optional[str] = None,
        auto_activate: bool = True,
        validate_syntax: bool = True
    ) -> Dict[str, Any]:
        """
        Complete workflow to modify a program include.

        Workflow: LOCK → [SYNTAX_CHECK] → MODIFY → UNLOCK → ACTIVATE

        Args:
            include_name: Name of include (e.g., 'ZTEST_INCLUDE_TOP')
            program_name: Parent program name (e.g., 'ZTEST_PROGRAM')
            new_source: New source code
            transport: Transport number (required for transportable packages)
            auto_activate: Automatically activate after modification (default: True)
            validate_syntax: Validate syntax before saving (default: True)

        Returns:
            Dictionary with complete workflow results

        Example:
            >>> result = service.modify_include(
            ...     "ZTEST_TOP",
            ...     "ZTEST_PROGRAM",
            ...     "DATA: lv_test TYPE string.",
            ...     transport="DEVK900123"
            ... )
            >>> print(result["success"])
            True

        Raises:
            Exception: If any step fails
        """
        logger.info(f"Starting modification workflow for include: {include_name}")

        # Build URIs
        include_uri = f"/sap/bc/adt/programs/includes/{include_name.lower()}"
        include_source_uri = f"{include_uri}/source/main"

        result = {
            "success": False,
            "uri": include_uri,
            "include_name": include_name,
            "program_name": program_name,
            "locked": False,
            "syntax_valid": False,
            "modified": False,
            "unlocked": False,
            "activated": False,
            "messages": [],
            "lock_handle": None
        }

        lock_handle = None

        try:
            # Step 1: Lock
            logger.info("Step 1/5: Locking include")
            lock_handle = self.object_service.lock(include_source_uri, access_mode="MODIFY")
            result["locked"] = True
            result["lock_handle"] = lock_handle
            logger.info(f"✓ Include locked (handle: {lock_handle[:20]}...)")

            # Step 2: Syntax check (optional)
            if validate_syntax:
                logger.info("Step 2/5: Validating syntax")
                syntax_result = self.quality_service.syntax_check(
                    object_uri=include_uri,
                    include_uri=include_source_uri,
                    source=new_source
                )

                result["syntax_valid"] = syntax_result.get("has_errors", True) == False
                result["messages"].extend(syntax_result.get("messages", []))

                if not result["syntax_valid"]:
                    error_count = len([m for m in syntax_result.get("messages", []) if m.get("type") == "error"])
                    logger.error(f"✗ Syntax validation failed with {error_count} errors")
                    raise Exception(f"Syntax validation failed with {error_count} errors")

                logger.info("✓ Syntax validation passed")
            else:
                result["syntax_valid"] = True

            # Step 3: Modify
            logger.info("Step 3/5: Modifying source code")
            modified = self.object_service.set_object_source(
                object_uri=include_source_uri,
                source_code=new_source,
                lock_handle=lock_handle,
                transport=transport
            )
            result["modified"] = modified
            logger.info("✓ Source code modified")

        finally:
            # Step 4: Unlock
            if lock_handle:
                try:
                    logger.info("Step 4/5: Unlocking include")
                    self.object_service.unlock(include_source_uri, lock_handle)
                    result["unlocked"] = True
                    logger.info("✓ Include unlocked")
                except Exception as unlock_error:
                    logger.error(f"✗ Failed to unlock: {unlock_error}")
                    result["messages"].append({
                        "type": "warning",
                        "text": f"Failed to unlock: {unlock_error}",
                        "step": "unlock"
                    })

        # Step 5: Activate
        if result["modified"] and auto_activate:
            try:
                logger.info("Step 5/5: Activating include")
                activation_result = self.activation_service.activate(
                    object_name=include_name,
                    object_uri=include_uri,
                    preaudit=True
                )

                result["activated"] = activation_result.get("success", False)
                result["messages"].extend(activation_result.get("messages", []))

                if result["activated"]:
                    logger.info("✓ Include activated successfully")
                else:
                    logger.warning("✗ Activation completed with warnings")

            except Exception as activate_error:
                logger.error(f"✗ Activation failed: {activate_error}")
                result["messages"].append({
                    "type": "error",
                    "text": f"Activation failed: {activate_error}",
                    "step": "activate"
                })

        # Final result
        result["success"] = (
            result["locked"] and
            result["syntax_valid"] and
            result["modified"] and
            result["unlocked"] and
            (result["activated"] if auto_activate else True)
        )

        if result["success"]:
            logger.info(f"✓✓✓ Include modification completed successfully: {include_name}")
        else:
            logger.error(f"✗✗✗ Include modification failed: {include_name}")

        return result

    # Private helper methods

    def _build_function_module_uri(self, function_group: str, function_module: str) -> str:
        """Build ADT URI for function module."""
        fg_lower = function_group.lower()
        fm_lower = function_module.lower()
        return f"/sap/bc/adt/functions/groups/{fg_lower}/fmodules/{fm_lower}"
