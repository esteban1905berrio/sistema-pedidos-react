"""Debug test to inspect RFC responses."""

import os
from dotenv import load_dotenv
from pyrfc import Connection

load_dotenv()

def test_raw_rfc_call():
    """Test raw RFC call to see the actual response structure."""
    conn = Connection(
        ashost=os.getenv("SAP_ASHOST"),
        sysnr=os.getenv("SAP_SYSNR"),
        client=os.getenv("SAP_CLIENT"),
        user=os.getenv("SAP_USER"),
        passwd=os.getenv("SAP_PASSWD"),
        saprouter=os.getenv("SAP_ROUTER"),
        lang=os.getenv("SAP_LANG", "EN"),
    )

    # Try the same request as in main.py
    request_dict = {
        "REQUEST_LINE": {
            "METHOD": "GET",
            "URI": "/sap/bc/adt/oo/classes/CL_ABAP_CHAR_UTILITIES/source/main",
            "VERSION": "HTTP/1.1",
        },
        "HEADER_FIELDS": [
            {"NAME": "Accept", "VALUE": "text/plain"},
            {"NAME": "Cache-Control", "VALUE": "no-cache"},
        ],
    }

    print("\n=== REQUEST ===")
    print(request_dict)

    result = conn.call("SADT_REST_RFC_ENDPOINT", REQUEST=request_dict)

    print("\n=== RESPONSE KEYS ===")
    print(result.keys())

    print("\n=== FULL RESPONSE ===")
    for key, value in result.items():
        print(f"\n{key}:")
        if isinstance(value, bytes):
            print(f"BYTES (length={len(value)})")
            print(value[:500].decode('utf-8', errors='replace'))  # First 500 chars
        elif isinstance(value, (list, dict)):
            import json
            try:
                print(json.dumps(value, indent=2))
            except:
                print(repr(value))
        else:
            print(repr(value))

    conn.close()


if __name__ == "__main__":
    test_raw_rfc_call()
