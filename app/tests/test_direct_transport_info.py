"""Direct test of transport_info without MCP server."""

import logging
import sys
from app.core.config import load_config
from app.core.rfc_connection import get_connection_pool
from app.services.transport_service import TransportService

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)


def main():
    """Test transport_info directly."""
    print("=" * 80)
    print("DIRECT TEST: transport_info (no MCP)")
    print("=" * 80)

    try:
        # 1. Load config
        print("\n[1/4] Loading SAP configuration...")
        config = load_config()
        print(f"✓ Connected to: {config.ashost}:{config.sysnr}")

        # 2. Get connection pool
        print("\n[2/4] Creating RFC connection pool...")
        pool = get_connection_pool(config)
        print(f"✓ Pool created with size: {pool.pool_size}")

        # 3. Create service
        print("\n[3/4] Creating TransportService...")
        service = TransportService(pool)
        print("✓ Service ready")

        # 4. Call transport_info
        obj_uri = "/sap/bc/adt/programs/includes/zsdi1038c_1"
        print(f"\n[4/4] Calling transport_info for: {obj_uri}")
        print("⏳ Waiting for response (timeout: 30s)...")

        result = service.transport_info(obj_uri)

        print("\n" + "=" * 80)
        print("✅ SUCCESS!")
        print("=" * 80)

        # Display results
        print(f"\nObject Name: {result.get('object_name')}")
        print(f"Total Versions: {result.get('total_versions')}")
        print(f"Feed Title: {result.get('feed_title')}")
        print(f"Feed Updated: {result.get('feed_updated')}")

        if result.get('versions'):
            print(f"\n📋 Version History ({len(result['versions'])} versions):")
            print("-" * 80)
            for i, version in enumerate(result['versions'], 1):
                print(f"\n  Version #{i}:")
                print(f"    ID: {version.get('version_id')}")
                print(f"    Author: {version.get('author')}")
                print(f"    Updated: {version.get('updated')}")
                print(f"    Transport: {version.get('transport_number')}")
                print(f"    Title: {version.get('transport_title')}")

                if version.get('transport_links'):
                    print(f"    Transport Links: {len(version['transport_links'])}")

        return 0

    except TimeoutError as e:
        print("\n" + "=" * 80)
        print("⏱️  TIMEOUT ERROR")
        print("=" * 80)
        print(f"Error: {e}")
        print("\nPossible causes:")
        print("  1. SAP system is slow or overloaded")
        print("  2. The endpoint does not exist or is not responding")
        print("  3. Network connectivity issues")
        return 1

    except Exception as e:
        print("\n" + "=" * 80)
        print("❌ ERROR")
        print("=" * 80)
        print(f"Type: {type(e).__name__}")
        print(f"Message: {e}")

        import traceback
        print("\nFull traceback:")
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)
