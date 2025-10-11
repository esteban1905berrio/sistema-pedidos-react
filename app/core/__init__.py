"""Core module for RFC connection and adapter functionality."""

from .rfc_connection import RfcConnectionPool, get_connection
from .rfc_adapter import RfcAdapter
from .config import SAPConfig, load_config

__all__ = [
    "RfcConnectionPool",
    "get_connection",
    "RfcAdapter",
    "SAPConfig",
    "load_config",
]
