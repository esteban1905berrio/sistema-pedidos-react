"""
Test para medir performance de get_class_source con clases grandes.
"""
import time
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent.parent))

from app.core.config import load_config
from app.core.rfc_connection import RfcConnectionPool
from app.core.rfc_adapter import RfcAdapter
from app.services.class_service import ClassService


def measure_class_source_performance():
    """Medir tiempo de respuesta para obtener código fuente de clase grande."""

    print("=" * 80)
    print("PERFORMANCE TEST: get_class_source - ZCLCXR1002_UTIL")
    print("=" * 80)

    # Load configuration
    config = load_config()

    # Initialize connections
    pool = RfcConnectionPool(config)
    service = ClassService(pool)

    class_name = "ZCLCXR1002_UTIL"

    # Test 1: Main include (definition + implementation)
    print(f"\n1. Testing MAIN include (full class)...")
    start_time = time.time()

    try:
        result = service.get_class_source(
            class_name=class_name,
            include_type="main",
            version="active"
        )
        source = result['source']
        metadata = result.get('metadata', {})

        end_time = time.time()
        elapsed = end_time - start_time

        # Analyze response
        source_length = len(source)
        lines = source.count('\n')

        print(f"   ✓ Success!")
        print(f"   ⏱️  Response time: {elapsed:.2f} seconds")
        print(f"   📊 Source length: {source_length:,} characters")
        print(f"   📄 Lines of code: {lines:,}")
        print(f"   📦 Size in KB: {source_length / 1024:.2f} KB")

        # Estimate token count (rough approximation: ~4 chars per token)
        estimated_tokens = source_length / 4
        print(f"   🎯 Estimated tokens: {estimated_tokens:,.0f}")

        # Check metadata for truncation
        if metadata.get('was_truncated'):
            print(f"   ⚠️  Response was TRUNCATED")
            print(f"   📏 Original size: {metadata.get('original_size', 0):,} chars")
            print(f"   ✂️  Truncated to: {metadata.get('truncated_size', 0):,} chars")

        if estimated_tokens > 25000:
            print(f"   ⚠️  WARNING: Exceeds MCP token limit (25,000)")
            print(f"   💡 Recommendation: Use fragmentation by include type")

    except Exception as e:
        end_time = time.time()
        elapsed = end_time - start_time
        print(f"   ❌ Error after {elapsed:.2f}s: {str(e)}")

    # Test 2: Try implementation only
    print(f"\n2. Testing IMPLEMENTATION include only...")
    start_time = time.time()

    try:
        result = service.get_class_source(
            class_name=class_name,
            include_type="implementation",
            version="active"
        )
        source = result['source']

        end_time = time.time()
        elapsed = end_time - start_time

        source_length = len(source)
        lines = source.count('\n')
        estimated_tokens = source_length / 4

        print(f"   ✓ Success!")
        print(f"   ⏱️  Response time: {elapsed:.2f} seconds")
        print(f"   📊 Source length: {source_length:,} characters")
        print(f"   📄 Lines of code: {lines:,}")
        print(f"   🎯 Estimated tokens: {estimated_tokens:,.0f}")

    except Exception as e:
        end_time = time.time()
        elapsed = end_time - start_time
        print(f"   ❌ Error after {elapsed:.2f}s: {str(e)}")

    # Test 3: Raw adapter timing
    print(f"\n3. Testing RAW RFC adapter call (direct)...")
    start_time = time.time()

    try:
        with pool.get_connection() as conn:
            adapter = RfcAdapter(conn)
            uri = f"/sap/bc/adt/oo/classes/{class_name.lower()}/source/main"
            response = adapter.request(
                uri=uri,
                method="GET",
                params={"version": "active"}
            )

        end_time = time.time()
        elapsed = end_time - start_time

        body_length = len(response.text)

        print(f"   ✓ Success!")
        print(f"   ⏱️  RFC call time: {elapsed:.2f} seconds")
        print(f"   📊 Response body: {body_length:,} characters")
        print(f"   📡 Status code: {response.status_code}")

    except Exception as e:
        end_time = time.time()
        elapsed = end_time - start_time
        print(f"   ❌ Error after {elapsed:.2f}s: {str(e)}")

        # Summary
        print("\n" + "=" * 80)
        print("ANALYSIS SUMMARY")
        print("=" * 80)
        print("""
Key Findings:
1. The class ZCLCXR1002_UTIL is very large (2634 lines)
2. When retrieved as 'main' include, it exceeds MCP token limits
3. Current implementation returns the full source in one call

Recommendations:
- Use include_type parameter for fragmentation:
  * 'main' for definition section only
  * 'implementation' for implementation section only
  * 'testclasses' for test classes
  * 'macros' for macros

- For LLMs with token limits, retrieve sections separately
- Consider adding a 'summary' mode that returns structure only
        """)


if __name__ == "__main__":
    measure_class_source_performance()
