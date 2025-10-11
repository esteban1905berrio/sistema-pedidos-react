"""RFC connection pool management."""

import logging
from typing import Optional
from contextlib import contextmanager
from threading import Lock
from pyrfc import Connection

from .config import SAPConfig

logger = logging.getLogger(__name__)


class RfcConnectionPool:
    """
    Thread-safe connection pool for SAP RFC connections.

    Manages a pool of RFC connections to avoid creating new connections
    for each request, improving performance and resource usage.
    """

    def __init__(self, config: SAPConfig, pool_size: int = 5):
        """
        Initialize the connection pool.

        Args:
            config: SAP connection configuration
            pool_size: Maximum number of connections in the pool
        """
        self.config = config
        self.pool_size = pool_size
        self._connections: list[Optional[Connection]] = []
        self._lock = Lock()
        self._available: list[bool] = []

    def _create_connection(self) -> Connection:
        """Create a new RFC connection."""
        params = {
            "ashost": self.config.ashost,
            "sysnr": self.config.sysnr,
            "client": self.config.client,
            "user": self.config.user,
            "passwd": self.config.passwd,
            "lang": self.config.lang,
        }

        if self.config.saprouter:
            params["saprouter"] = self.config.saprouter

        logger.info(f"Creating new RFC connection to {self.config.ashost}")
        return Connection(**params)

    @contextmanager
    def get_connection(self):
        """
        Get a connection from the pool (context manager).

        Usage:
            with pool.get_connection() as conn:
                result = conn.call('SADT_REST_RFC_ENDPOINT', REQUEST=request_dict)

        Yields:
            Connection: An available RFC connection
        """
        conn = None
        conn_index = -1

        try:
            with self._lock:
                # Try to find an available connection
                for i, available in enumerate(self._available):
                    if available:
                        conn = self._connections[i]
                        self._available[i] = False
                        conn_index = i
                        logger.debug(f"Reusing connection {i} from pool")
                        break

                # If no available connection and pool not full, create new one
                if conn is None and len(self._connections) < self.pool_size:
                    conn = self._create_connection()
                    self._connections.append(conn)
                    self._available.append(False)
                    conn_index = len(self._connections) - 1
                    logger.debug(f"Created new connection {conn_index} (pool size: {len(self._connections)})")

            # If still no connection, wait for one to become available
            if conn is None:
                logger.warning("Pool exhausted, waiting for available connection...")
                # Simple retry loop (could be improved with condition variable)
                import time
                while conn is None:
                    time.sleep(0.1)
                    with self._lock:
                        for i, available in enumerate(self._available):
                            if available:
                                conn = self._connections[i]
                                self._available[i] = False
                                conn_index = i
                                break

            yield conn

        finally:
            # Return connection to pool
            if conn_index >= 0:
                with self._lock:
                    self._available[conn_index] = True
                    logger.debug(f"Returned connection {conn_index} to pool")

    def close_all(self):
        """Close all connections in the pool."""
        with self._lock:
            for i, conn in enumerate(self._connections):
                if conn is not None:
                    try:
                        conn.close()
                        logger.info(f"Closed connection {i}")
                    except Exception as e:
                        logger.error(f"Error closing connection {i}: {e}")
            self._connections.clear()
            self._available.clear()


# Global connection pool instance
_connection_pool: Optional[RfcConnectionPool] = None
_pool_lock = Lock()


def get_connection_pool(config: Optional[SAPConfig] = None) -> RfcConnectionPool:
    """
    Get or create the global connection pool.

    Args:
        config: SAP configuration (required on first call)

    Returns:
        RfcConnectionPool: The global connection pool instance

    Raises:
        ValueError: If config is not provided on first call
    """
    global _connection_pool

    with _pool_lock:
        if _connection_pool is None:
            if config is None:
                raise ValueError("Config must be provided when creating connection pool")
            _connection_pool = RfcConnectionPool(config)
            logger.info("Initialized global RFC connection pool")

    return _connection_pool


@contextmanager
def get_connection(config: Optional[SAPConfig] = None):
    """
    Convenience function to get a connection from the global pool.

    Args:
        config: SAP configuration (required on first call)

    Yields:
        Connection: An RFC connection from the pool
    """
    pool = get_connection_pool(config)
    with pool.get_connection() as conn:
        yield conn
