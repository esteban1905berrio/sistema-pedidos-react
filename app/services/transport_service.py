"""Service for ABAP transport management operations."""

import logging
import xml.etree.ElementTree as ET
from typing import List, Dict, Any, Optional

from app.core.rfc_adapter import RfcAdapter

logger = logging.getLogger(__name__)


class TransportService:
    """
    Service for managing ABAP transport requests (CTS).

    This service provides tools to:
    - Get transport information for objects
    - Create new transport requests
    - List user transports
    - Release transports
    - Manage transport collaboration
    """

    def __init__(self, adapter: RfcAdapter):
        """
        Initialize the transport service.

        Args:
            adapter: RfcAdapter instance for ADT API calls to SAP system
        """
        self.adapter = adapter
        logger.debug("TransportService initialized")

    # Sprint 3.1: Transport Info & Creation

    def transport_info(
        self,
        obj_source_url: str,
        dev_class: Optional[str] = None,
        operation: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Get transport information for an object.

        Args:
            obj_source_url: URI of the object
            dev_class: Development class (optional)
            operation: Operation type (optional)

        Returns:
            Dictionary with transport information

        Example:
            >>> service.transport_info("/sap/bc/adt/oo/classes/ztest")
            {
                "transport_number": "DEVK900123",
                "status": "modifiable",
                "locked_by": "USER01"
            }
        """
        logger.info(f"Getting transport info for: {obj_source_url}")

        params = {"uri": obj_source_url}
        if dev_class:
            params["devclass"] = dev_class
        if operation:
            params["operation"] = operation

        response = self.adapter.request(
            uri="/sap/bc/adt/cts/transportinformation",
            method="GET",
            params=params,
            body=""
        )

        if response.status_code == 200:
            transport_data = self._parse_transport_info(response.text)
            logger.info(f"Retrieved transport info")
            return transport_data
        else:
            error_msg = f"Failed to get transport info: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def create_transport(
        self,
        description: str,
        dev_class: str,
        transport_type: str = "K"
    ) -> str:
        """
        Create a new transport request.

        Args:
            description: Transport description
            dev_class: Development class/package
            transport_type: Type of transport (K=Workbench, C=Customizing)

        Returns:
            Transport number (TRKORR)

        Example:
            >>> service.create_transport("My new feature", "ZPACKAGE")
            "DEVK900124"
        """
        logger.info(f"Creating transport: {description} (package: {dev_class})")

        # Build XML body for transport creation
        body = f"""<?xml version="1.0" encoding="UTF-8"?>
<tm:request xmlns:tm="http://www.sap.com/adt/cts/transports">
    <tm:type>{transport_type}</tm:type>
    <tm:description>{description}</tm:description>
    <tm:target></tm:target>
</tm:request>"""

        response = self.adapter.request(
            uri="/sap/bc/adt/cts/transports",
            method="POST",
            params={"devclass": dev_class},
            body=body,
            content_type="application/vnd.sap.adt.transportrequests.v1+xml"
        )

        if response.status_code in [200, 201]:
            transport_number = self._extract_transport_number(response.text)
            logger.info(f"Created transport: {transport_number}")
            return transport_number
        else:
            error_msg = f"Failed to create transport: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # Sprint 3.2: Transport Listing

    def list_user_transports(
        self,
        user: Optional[str] = None,
        status: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """
        List transport requests for a user.

        Args:
            user: User ID (default: current user)
            status: Filter by status (e.g., 'R' for released, 'D' for modifiable)

        Returns:
            List of transport dictionaries

        Example:
            >>> service.list_user_transports()
            [
                {"number": "DEVK900123", "description": "...", "status": "D"},
                ...
            ]
        """
        logger.info(f"Listing transports for user: {user or 'current'}")

        params = {}
        if user:
            params["user"] = user
        if status:
            params["status"] = status

        response = self.adapter.request(
            uri="/sap/bc/adt/cts/transports",
            method="GET",
            params=params,
            body=""
        )

        if response.status_code == 200:
            transports = self._parse_transport_list(response.text)
            logger.info(f"Found {len(transports)} transports")
            return transports
        else:
            error_msg = f"Failed to list transports: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_transport_request(self, transport_number: str) -> Dict[str, Any]:
        """
        Get full transport request data (general method).

        This is the base method that retrieves complete transport data including
        tasks and objects. Other methods like get_transport_tasks() and
        get_transport_objects() use this method and extract specific data.

        Args:
            transport_number: Transport request number

        Returns:
            Dictionary with complete transport data

        Example:
            >>> service.get_transport_request("S4DK932806")
            {
                "number": "S4DK932806",
                "owner": "SEBLONDO",
                "description": "...",
                "tasks": [...],
                "objects": [...],
                "raw_xml": "..."
            }
        """
        logger.info(f"Getting transport request data: {transport_number}")

        response = self.adapter.request(
            uri=f"/sap/bc/adt/cts/transportrequests/{transport_number}",
            method="GET",
            params={},
            body=""
        )

        if response.status_code == 200:
            transport_data = self._parse_transport_request(response.text)
            logger.info(f"Retrieved transport data with {len(transport_data.get('tasks', []))} tasks")
            return transport_data
        else:
            error_msg = f"Failed to get transport request: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_transport_tasks(self, transport_number: str) -> List[Dict[str, Any]]:
        """
        Get tasks for a transport request.

        Args:
            transport_number: Transport request number

        Returns:
            List of task dictionaries

        Example:
            >>> service.get_transport_tasks("S4DK932806")
            [{"number": "S4DK932807", "owner": "SEBLONDO", ...}, ...]
        """
        logger.info(f"Getting tasks for transport: {transport_number}")

        # Use general method to retrieve full transport data
        transport_data = self.get_transport_request(transport_number)
        tasks = transport_data.get('tasks', [])

        logger.info(f"Found {len(tasks)} tasks")
        return tasks

    def get_transport_objects(
        self,
        transport_number: str,
        task_number: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """
        Get objects from a transport request.

        Args:
            transport_number: Transport request number
            task_number: Optional task number to filter objects by task

        Returns:
            List of object dictionaries

        Example:
            >>> service.get_transport_objects("S4DK932806")
            [{"pgmid": "LIMU", "type": "METH", "name": "...", ...}, ...]

            >>> service.get_transport_objects("S4DK932806", "S4DK932807")
            [{"pgmid": "LIMU", "type": "METH", "name": "...", "task": "S4DK932807"}, ...]
        """
        logger.info(f"Getting objects for transport: {transport_number}")

        # Use general method to retrieve full transport data
        transport_data = self.get_transport_request(transport_number)
        objects = transport_data.get('objects', [])

        # Filter by task if specified
        if task_number:
            objects = [obj for obj in objects if obj.get('task') == task_number]
            logger.info(f"Found {len(objects)} objects for task {task_number}")
        else:
            logger.info(f"Found {len(objects)} total objects")

        return objects

    # Sprint 3.3: Object Assignment & Release

    def add_object_to_transport(
        self,
        transport_number: str,
        object_uri: str,
        lock_handle: Optional[str] = None
    ) -> bool:
        """
        Add/assign an object to a transport request.

        Args:
            transport_number: Transport number
            object_uri: URI of the object to add
            lock_handle: Lock handle if object is locked

        Returns:
            True if successful

        Example:
            >>> service.add_object_to_transport("DEVK900123", "/sap/bc/adt/oo/classes/ztest")
            True
        """
        logger.info(f"Adding object to transport {transport_number}: {object_uri}")

        params = {"corrNr": transport_number}
        if lock_handle:
            params["lockHandle"] = lock_handle

        response = self.adapter.request(
            uri=object_uri,
            method="POST",
            params=params,
            body="",
            content_type="application/xml"
        )

        if response.status_code in [200, 204]:
            logger.info(f"Successfully added object to transport")
            return True
        else:
            error_msg = f"Failed to add object to transport: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def release_transport(
        self,
        transport_number: str,
        ignore_atc: bool = False
    ) -> Dict[str, Any]:
        """
        Release a transport request.

        Args:
            transport_number: Transport number to release
            ignore_atc: Ignore ATC errors (default: False)

        Returns:
            Dictionary with release result

        Example:
            >>> service.release_transport("DEVK900123")
            {"status": "released", "messages": [...]}
        """
        logger.info(f"Releasing transport: {transport_number}")

        params = {}
        if ignore_atc:
            params["ignore_atc"] = "true"

        response = self.adapter.request(
            uri=f"/sap/bc/adt/cts/transports/{transport_number}/release",
            method="POST",
            params=params,
            body=""
        )

        if response.status_code in [200, 204]:
            result = self._parse_release_result(response.text)
            logger.info(f"Transport released successfully")
            return result
        else:
            error_msg = f"Failed to release transport: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_transport_config(self) -> Dict[str, Any]:
        """
        Get transport configuration for the system.

        Returns:
            Dictionary with transport configuration

        Example:
            >>> service.get_transport_config()
            {"target_system": "QAS", "domain": "D", ...}
        """
        logger.info("Getting transport configuration")

        response = self.adapter.request(
            uri="/sap/bc/adt/cts/transportconfiguration",
            method="GET",
            params={},
            body=""
        )

        if response.status_code == 200:
            config = self._parse_transport_config(response.text)
            logger.info("Retrieved transport configuration")
            return config
        else:
            error_msg = f"Failed to get transport config: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def delete_transport(self, transport_number: str) -> bool:
        """
        Delete a transport request (only if not released).

        Args:
            transport_number: Transport number to delete

        Returns:
            True if successful

        Example:
            >>> service.delete_transport("DEVK900123")
            True
        """
        logger.info(f"Deleting transport: {transport_number}")

        response = self.adapter.request(
            uri=f"/sap/bc/adt/cts/transports/{transport_number}",
            method="DELETE",
            params={},
            body=""
        )

        if response.status_code in [200, 204]:
            logger.info(f"Transport deleted successfully")
            return True
        else:
            error_msg = f"Failed to delete transport: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # Sprint 3.4: Transport Collaboration

    def set_transport_owner(
        self,
        transport_number: str,
        target_user: str
    ) -> bool:
        """
        Change owner of a transport request.

        Args:
            transport_number: Transport number
            target_user: New owner user ID

        Returns:
            True if successful

        Example:
            >>> service.set_transport_owner("DEVK900123", "USER02")
            True
        """
        logger.info(f"Setting transport {transport_number} owner to: {target_user}")

        body = f"""<?xml version="1.0" encoding="UTF-8"?>
<tm:owner xmlns:tm="http://www.sap.com/adt/cts/transports">{target_user}</tm:owner>"""

        response = self.adapter.request(
            uri=f"/sap/bc/adt/cts/transports/{transport_number}/owner",
            method="POST",
            params={},
            body=body,
            content_type="application/xml"
        )

        if response.status_code in [200, 204]:
            logger.info(f"Transport owner changed successfully")
            return True
        else:
            error_msg = f"Failed to set transport owner: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def add_transport_user(
        self,
        transport_number: str,
        user: str
    ) -> bool:
        """
        Add a collaborator user to a transport.

        Args:
            transport_number: Transport number
            user: User ID to add

        Returns:
            True if successful

        Example:
            >>> service.add_transport_user("DEVK900123", "USER02")
            True
        """
        logger.info(f"Adding user {user} to transport: {transport_number}")

        body = f"""<?xml version="1.0" encoding="UTF-8"?>
<tm:user xmlns:tm="http://www.sap.com/adt/cts/transports">{user}</tm:user>"""

        response = self.adapter.request(
            uri=f"/sap/bc/adt/cts/transports/{transport_number}/users",
            method="POST",
            params={},
            body=body,
            content_type="application/xml"
        )

        if response.status_code in [200, 201, 204]:
            logger.info(f"User added to transport successfully")
            return True
        else:
            error_msg = f"Failed to add user to transport: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_system_users(self) -> List[str]:
        """
        Get list of users in the SAP system.

        Returns:
            List of user IDs

        Example:
            >>> service.get_system_users()
            ["USER01", "USER02", "USER03", ...]
        """
        logger.info("Getting system users")

        response = self.adapter.request(
            uri="/sap/bc/adt/cts/users",
            method="GET",
            params={},
            body=""
        )

        if response.status_code == 200:
            users = self._parse_user_list(response.text)
            logger.info(f"Found {len(users)} users")
            return users
        else:
            error_msg = f"Failed to get system users: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    def get_transport_reference(
        self,
        pgmid: str,
        obj_wbtype: str,
        obj_name: str,
        tr_number: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Get transport references for an object.

        Args:
            pgmid: Program ID (e.g., 'R3TR')
            obj_wbtype: Workbench type (e.g., 'PROG', 'CLAS')
            obj_name: Object name
            tr_number: Optional transport number

        Returns:
            Dictionary with transport references

        Example:
            >>> service.get_transport_reference("R3TR", "CLAS", "ZTEST")
            {"transports": [...], "references": [...]}
        """
        logger.info(f"Getting transport reference for: {obj_wbtype} {obj_name}")

        params = {
            "pgmid": pgmid,
            "obj_wbtype": obj_wbtype,
            "obj_name": obj_name
        }
        if tr_number:
            params["tr_number"] = tr_number

        response = self.adapter.request(
            uri="/sap/bc/adt/cts/transportreference",
            method="GET",
            params=params,
            body=""
        )

        if response.status_code == 200:
            references = self._parse_transport_references(response.text)
            logger.info("Retrieved transport references")
            return references
        else:
            error_msg = f"Failed to get transport references: {response.status_code}"
            logger.error(error_msg)
            raise Exception(f"{error_msg}\n{response.text}")

    # Private parsing methods

    def _parse_transport_info(self, xml_text: str) -> Dict[str, Any]:
        """Parse transport information XML."""
        try:
            root = ET.fromstring(xml_text)
            ns = {'tm': 'http://www.sap.com/adt/cts/transports'}

            info = {
                'transport_number': root.findtext('.//tm:number', '', ns),
                'status': root.findtext('.//tm:status', '', ns),
                'locked_by': root.findtext('.//tm:locked_by', '', ns),
                'description': root.findtext('.//tm:description', '', ns),
                'raw_xml': xml_text
            }
            return info
        except ET.ParseError as e:
            logger.error(f"Failed to parse transport info XML: {e}")
            return {'raw_xml': xml_text, 'error': str(e)}

    def _extract_transport_number(self, xml_text: str) -> str:
        """Extract transport number from creation response."""
        try:
            root = ET.fromstring(xml_text)
            ns = {'tm': 'http://www.sap.com/adt/cts/transports'}
            transport_num = root.findtext('.//tm:number', '', ns)
            if not transport_num:
                # Try alternative paths
                transport_num = root.get('number', '')
            return transport_num
        except Exception as e:
            logger.error(f"Failed to extract transport number: {e}")
            raise Exception(f"Could not extract transport number from response: {xml_text[:200]}")

    def _parse_transport_list(self, xml_text: str) -> List[Dict[str, Any]]:
        """Parse list of transports XML."""
        try:
            root = ET.fromstring(xml_text)
            ns = {'tm': 'http://www.sap.com/adt/cts/transports'}

            transports = []
            for transport_elem in root.findall('.//tm:transport', ns):
                transport = {
                    'number': transport_elem.findtext('.//tm:number', '', ns),
                    'description': transport_elem.findtext('.//tm:description', '', ns),
                    'status': transport_elem.findtext('.//tm:status', '', ns),
                    'owner': transport_elem.findtext('.//tm:owner', '', ns)
                }
                transports.append(transport)

            return transports
        except ET.ParseError as e:
            logger.error(f"Failed to parse transport list XML: {e}")
            return []

    def _parse_transport_request(self, xml_text: str) -> Dict[str, Any]:
        """
        Parse full transport request XML.

        Parses the complete transport request structure including:
        - Main request metadata (number, owner, description, status)
        - Tasks with their metadata
        - Objects within each task

        Args:
            xml_text: XML response from /sap/bc/adt/cts/transportrequests/{number}

        Returns:
            Dictionary with complete transport data structure
        """
        try:
            root = ET.fromstring(xml_text)
            # Namespace for transport management
            ns = {'tm': 'http://www.sap.com/cts/adt/tm'}

            # Find the main request element
            request_elem = root.find('.//tm:request', ns)
            if request_elem is None:
                # Maybe root is the request itself
                request_elem = root

            # Extract main request attributes
            transport_data = {
                'number': request_elem.get(f'{{{ns["tm"]}}}number', ''),
                'owner': request_elem.get(f'{{{ns["tm"]}}}owner', ''),
                'description': request_elem.get(f'{{{ns["tm"]}}}description', ''),
                'status': request_elem.get(f'{{{ns["tm"]}}}status', ''),
                'type': request_elem.get(f'{{{ns["tm"]}}}type', ''),
                'target': request_elem.get(f'{{{ns["tm"]}}}target', ''),
                'tasks': [],
                'objects': [],
                'raw_xml': xml_text
            }

            # Parse tasks
            for task_elem in request_elem.findall('.//tm:task', ns):
                task = {
                    'number': task_elem.get(f'{{{ns["tm"]}}}number', ''),
                    'parent': task_elem.get(f'{{{ns["tm"]}}}parent', ''),
                    'owner': task_elem.get(f'{{{ns["tm"]}}}owner', ''),
                    'description': task_elem.get(f'{{{ns["tm"]}}}description', ''),
                    'status': task_elem.get(f'{{{ns["tm"]}}}status', ''),
                    'type': task_elem.get(f'{{{ns["tm"]}}}type', ''),
                    'objects': []
                }

                # Parse objects within this task
                for obj_elem in task_elem.findall('.//tm:abap_object', ns):
                    obj = {
                        'pgmid': obj_elem.get(f'{{{ns["tm"]}}}pgmid', ''),
                        'type': obj_elem.get(f'{{{ns["tm"]}}}type', ''),
                        'name': obj_elem.get(f'{{{ns["tm"]}}}name', ''),
                        'wbtype': obj_elem.get(f'{{{ns["tm"]}}}wbtype', ''),
                        'locked': obj_elem.get(f'{{{ns["tm"]}}}locked', ''),
                        'task': task['number']  # Link object to task
                    }
                    task['objects'].append(obj)
                    # Also add to global objects list
                    transport_data['objects'].append(obj)

                transport_data['tasks'].append(task)

            return transport_data

        except ET.ParseError as e:
            logger.error(f"Failed to parse transport request XML: {e}")
            return {
                'number': '',
                'tasks': [],
                'objects': [],
                'error': str(e),
                'raw_xml': xml_text
            }
        except Exception as e:
            logger.error(f"Unexpected error parsing transport request: {e}")
            return {
                'number': '',
                'tasks': [],
                'objects': [],
                'error': str(e),
                'raw_xml': xml_text
            }

    def _parse_release_result(self, xml_text: str) -> Dict[str, Any]:
        """Parse release result XML."""
        return {
            'status': 'released',
            'messages': [],
            'raw_xml': xml_text
        }

    def _parse_transport_config(self, xml_text: str) -> Dict[str, Any]:
        """Parse transport configuration XML."""
        try:
            root = ET.fromstring(xml_text)
            ns = {'tm': 'http://www.sap.com/adt/cts/transports'}

            config = {
                'target_system': root.findtext('.//tm:target_system', '', ns),
                'domain': root.findtext('.//tm:domain', '', ns),
                'raw_xml': xml_text
            }
            return config
        except ET.ParseError as e:
            logger.error(f"Failed to parse transport config XML: {e}")
            return {'raw_xml': xml_text, 'error': str(e)}

    def _parse_user_list(self, xml_text: str) -> List[str]:
        """Parse system users list XML."""
        try:
            root = ET.fromstring(xml_text)
            ns = {'tm': 'http://www.sap.com/adt/cts/transports'}

            users = []
            for user_elem in root.findall('.//tm:user', ns):
                user = user_elem.text or user_elem.get('name', '')
                if user:
                    users.append(user)

            return users
        except ET.ParseError as e:
            logger.error(f"Failed to parse user list XML: {e}")
            return []

    def _parse_transport_references(self, xml_text: str) -> Dict[str, Any]:
        """Parse transport references XML."""
        try:
            root = ET.fromstring(xml_text)
            ns = {'tm': 'http://www.sap.com/adt/cts/transports'}

            references = {
                'transports': [],
                'references': [],
                'raw_xml': xml_text
            }

            for ref_elem in root.findall('.//tm:reference', ns):
                ref = {
                    'transport': ref_elem.findtext('.//tm:transport', '', ns),
                    'type': ref_elem.findtext('.//tm:type', '', ns)
                }
                references['references'].append(ref)

            return references
        except ET.ParseError as e:
            logger.error(f"Failed to parse transport references XML: {e}")
            return {'raw_xml': xml_text, 'error': str(e)}
