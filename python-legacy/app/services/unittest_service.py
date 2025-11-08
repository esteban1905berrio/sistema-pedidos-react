"""Service for ABAP unit test execution operations."""

import logging
import xml.etree.ElementTree as ET
from typing import List, Dict, Any

from app.core.rfc_adapter import RfcAdapter
from app.services.base_service import BaseService

logger = logging.getLogger(__name__)


class UnittestService(BaseService):
    """
    Service for unit test execution.

    This service provides tools to:
    - Execute ABAP unit tests
    - Get unit test results
    """

    def run_unit_tests(
        self,
        object_uri: str,
        coverage: bool = False
    ) -> Dict[str, Any]:
        """
        Execute unit tests for an ABAP object.

        Args:
            object_uri: URI of the object to test
            coverage: Include code coverage analysis (default: False)

        Returns:
            Dictionary with test results

        Example:
            >>> result = service.run_unit_tests(
            ...     "/sap/bc/adt/oo/classes/zcl_test"
            ... )
            >>> print(result)
            {
                "total": 5,
                "passed": 4,
                "failed": 1,
                "tests": [...]
            }
        """
        logger.info(f"Running unit tests for: {object_uri}")

        # Build XML body for unit test execution
        body = self._build_unittest_xml(object_uri, coverage)

        with self._get_adapter() as adapter:
            response = adapter.request(
                uri="/sap/bc/adt/abapunit/testruns",
                method="POST",
                params={},
                body=body,
                content_type="application/vnd.sap.adt.abapunit.testruns.config.v4+xml"
            )

        if response.status_code == 200:
            result = self._parse_unittest_result(response.text)
            logger.info(f"Unit tests completed. Passed: {result.get('passed')}, Failed: {result.get('failed')}")
            return result
        else:
            error_msg = f"Failed to run unit tests: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # Private helper methods

    def _build_unittest_xml(
        self,
        object_uri: str,
        coverage: bool
    ) -> str:
        """Build XML body for unit test execution."""
        coverage_str = "true" if coverage else "false"

        xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<aunit:runConfiguration xmlns:aunit="http://www.sap.com/adt/abapunit">
  <adtcore:uriList xmlns:adtcore="http://www.sap.com/adt/core">
    <adtcore:uri>{object_uri}</adtcore:uri>
  </adtcore:uriList>
  <aunit:options>
    <aunit:uriType value="semantic"/>
    <aunit:testDeterminationStrategy sameProgram="true"/>
    <aunit:testRiskLevels harmless="true" dangerous="true" critical="true"/>
    <aunit:testDurations short="true" medium="true" long="true"/>
    <aunit:withMeasurement value="{coverage_str}"/>
  </aunit:options>
</aunit:runConfiguration>"""

        return xml

    def _parse_unittest_result(self, xml_text: str) -> Dict[str, Any]:
        """Parse unit test result XML."""
        try:
            root = ET.fromstring(xml_text)

            # Namespaces
            ns = {
                'aunit': 'http://www.sap.com/adt/abapunit',
                'adtcore': 'http://www.sap.com/adt/core'
            }

            # Count test results
            total = 0
            passed = 0
            failed = 0
            tests = []

            # Find all test classes and methods
            for testclass_elem in root.findall('.//aunit:testclass', ns):
                class_name = testclass_elem.get('name', '')

                for method_elem in testclass_elem.findall('.//aunit:testmethod', ns):
                    method_name = method_elem.get('name', '')
                    duration = method_elem.get('executionTime', '0')

                    # Check for alerts (failures/errors)
                    alerts = method_elem.findall('.//aunit:alert', ns)
                    has_errors = len(alerts) > 0

                    total += 1
                    if has_errors:
                        failed += 1
                        status = 'failed'
                    else:
                        passed += 1
                        status = 'passed'

                    test_info = {
                        'class': class_name,
                        'method': method_name,
                        'status': status,
                        'duration': duration,
                        'messages': []
                    }

                    # Extract error messages
                    for alert in alerts:
                        alert_type = alert.get('kind', 'error')
                        alert_text = alert.findtext('.//aunit:title', '', ns)
                        test_info['messages'].append({
                            'type': alert_type,
                            'text': alert_text
                        })

                    tests.append(test_info)

            result = {
                'total': total,
                'passed': passed,
                'failed': failed,
                'tests': tests,
                'raw_xml': xml_text
            }

            return result

        except ET.ParseError as e:
            logger.error(f"Failed to parse unit test result XML: {e}")
            return {
                'total': 0,
                'passed': 0,
                'failed': 0,
                'tests': [],
                'error': str(e),
                'raw_xml': xml_text
            }
