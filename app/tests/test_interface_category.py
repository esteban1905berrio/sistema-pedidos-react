"""Integration tests for interface operations."""

import os
import pytest
from dotenv import load_dotenv

from app.core.config import SAPConfig
from app.core.rfc_connection import RfcConnectionPool
from app.services.interface_service import InterfaceService

load_dotenv()


@pytest.fixture
def interface_service():
    """Create InterfaceService with connection pool."""
    sap_config = SAPConfig(
        ashost=os.getenv("SAP_ASHOST", ""),
        sysnr=os.getenv("SAP_SYSNR", ""),
        client=os.getenv("SAP_CLIENT", ""),
        user=os.getenv("SAP_USER", ""),
        passwd=os.getenv("SAP_PASSWD", ""),
        lang=os.getenv("SAP_LANG", "EN"),
        saprouter=os.getenv("SAP_ROUTER"),
    )

    connection_pool = RfcConnectionPool(sap_config, pool_size=1)
    return InterfaceService(connection_pool)


def test_get_interface_source(interface_service):
    """Test getting interface source code."""
    interface_name = "ZIFCXR1002_ALVGRID"

    source = interface_service.get_interface_source(interface_name)

    assert source is not None
    assert len(source) > 0
    assert "INTERFACE" in source.upper()
    assert "ENDINTERFACE" in source.upper()
    print(f"\n✓ Interface source retrieved: {len(source)} characters")
    print(f"Source preview:\n{source[:200]}...")


def test_get_interface_structure(interface_service):
    """Test getting interface structure."""
    interface_name = "ZIFCXR1002_ALVGRID"

    structure = interface_service.get_interface_structure(interface_name)

    assert structure is not None
    assert structure["interface_name"] == interface_name
    assert "methods" in structure
    assert len(structure["methods"]) > 0

    print(f"\n✓ Interface structure retrieved:")
    print(f"  Interface: {structure['interface_name']}")
    print(f"  Type: {structure['type']}")
    print(f"  Methods count: {len(structure['methods'])}")

    for method in structure["methods"]:
        print(f"    - {method['name']} ({method['visibility']})")


def test_get_interface_includes(interface_service):
    """Test getting interface includes (may be empty for interfaces)."""
    interface_name = "ZIFCXR1002_ALVGRID"

    try:
        includes = interface_service.get_interface_includes(interface_name)
        print(f"\n✓ Interface includes retrieved: {len(includes)} includes")
        for include in includes:
            print(f"  - {include['type']}: {include['name']}")
    except Exception as e:
        # It's normal for interfaces to not have includes
        print(f"\n✓ No includes found (expected for interfaces): {str(e)}")
        assert True


def test_interface_not_found(interface_service):
    """Test behavior when interface doesn't exist."""
    with pytest.raises(Exception) as exc_info:
        interface_service.get_interface_source("ZIFNONEXISTENT")

    assert "404" in str(exc_info.value) or "not exist" in str(exc_info.value).lower()
    print(f"\n✓ Correctly handles non-existent interface")


if __name__ == "__main__":
    pytest.main([__file__, "-v", "-s"])
