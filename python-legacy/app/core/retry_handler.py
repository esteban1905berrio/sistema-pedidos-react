"""Retry logic with exponential backoff for RFC operations."""

import logging
import time
from typing import Callable, Any, TypeVar
from functools import wraps

logger = logging.getLogger(__name__)

T = TypeVar('T')


class RetryConfig:
    """Configuration for retry behavior."""

    def __init__(
        self,
        max_attempts: int = 3,
        base_delay: float = 0.1,
        max_delay: float = 2.0,
        exponential_base: float = 2.0,
    ):
        """
        Initialize retry configuration.

        Args:
            max_attempts: Maximum number of retry attempts
            base_delay: Initial delay in seconds
            max_delay: Maximum delay in seconds
            exponential_base: Base for exponential backoff
        """
        self.max_attempts = max_attempts
        self.base_delay = base_delay
        self.max_delay = max_delay
        self.exponential_base = exponential_base

    def get_delay(self, attempt: int) -> float:
        """
        Calculate delay for given attempt using exponential backoff.

        Args:
            attempt: Current attempt number (0-indexed)

        Returns:
            float: Delay in seconds
        """
        delay = self.base_delay * (self.exponential_base ** attempt)
        return min(delay, self.max_delay)


# Default retry config
DEFAULT_RETRY_CONFIG = RetryConfig(
    max_attempts=3,
    base_delay=0.1,  # 100ms
    max_delay=2.0,    # 2 seconds
    exponential_base=2.0
)


def is_retryable_error(exception: Exception) -> bool:
    """
    Check if an exception is retryable (transient network error).

    Args:
        exception: Exception to check

    Returns:
        bool: True if error should be retried
    """
    # Import here to avoid circular dependency
    try:
        from pyrfc import RfcCommunicationException, ABAPApplicationError

        if isinstance(exception, RfcCommunicationException):
            # Connection errors are retryable
            error_msg = str(exception).lower()
            retryable_patterns = [
                'connection reset',
                'connection broken',
                'timeout',
                'connection closed',
                'connection lost',
                'network error',
                'errno 54',  # Connection reset by peer
                'errno 104', # Connection reset by peer (Linux)
                'errno 110', # Connection timed out
            ]
            return any(pattern in error_msg for pattern in retryable_patterns)

        # ABAP application errors are NOT retryable
        if isinstance(exception, ABAPApplicationError):
            return False

    except ImportError:
        pass

    # By default, don't retry unless we're sure it's safe
    return False


def retry_on_network_error(
    config: RetryConfig = DEFAULT_RETRY_CONFIG
) -> Callable:
    """
    Decorator to retry a function on network errors with exponential backoff.

    Args:
        config: RetryConfig instance

    Returns:
        Decorator function

    Example:
        @retry_on_network_error()
        def call_rfc_function():
            return conn.call('SOME_RFC_FUNCTION')
    """
    def decorator(func: Callable[..., T]) -> Callable[..., T]:
        @wraps(func)
        def wrapper(*args, **kwargs) -> T:
            last_exception = None

            for attempt in range(config.max_attempts):
                try:
                    return func(*args, **kwargs)

                except Exception as e:
                    last_exception = e

                    # Check if this error is retryable
                    if not is_retryable_error(e):
                        logger.debug(f"Error is not retryable: {e}")
                        raise

                    # Check if we have more attempts left
                    if attempt < config.max_attempts - 1:
                        delay = config.get_delay(attempt)
                        logger.warning(
                            f"Attempt {attempt + 1}/{config.max_attempts} failed: {e}. "
                            f"Retrying in {delay:.2f}s..."
                        )
                        time.sleep(delay)
                    else:
                        logger.error(
                            f"All {config.max_attempts} attempts failed. Last error: {e}"
                        )
                        raise

            # Should never reach here, but just in case
            if last_exception:
                raise last_exception

        return wrapper
    return decorator


class CircuitBreaker:
    """
    Circuit breaker pattern to prevent cascade failures.

    After a threshold of consecutive failures, the circuit "opens"
    and stops trying to call the function for a cooldown period.
    """

    def __init__(
        self,
        failure_threshold: int = 5,
        cooldown_seconds: float = 60.0
    ):
        """
        Initialize circuit breaker.

        Args:
            failure_threshold: Number of consecutive failures before opening
            cooldown_seconds: Time to wait before retrying after circuit opens
        """
        self.failure_threshold = failure_threshold
        self.cooldown_seconds = cooldown_seconds
        self.consecutive_failures = 0
        self.circuit_open_time = None

    def is_open(self) -> bool:
        """Check if circuit is currently open."""
        if self.circuit_open_time is None:
            return False

        # Check if cooldown period has passed
        if time.time() - self.circuit_open_time >= self.cooldown_seconds:
            logger.info("Circuit breaker cooldown period ended, closing circuit")
            self.circuit_open_time = None
            self.consecutive_failures = 0
            return False

        return True

    def record_success(self):
        """Record a successful operation."""
        self.consecutive_failures = 0
        if self.circuit_open_time:
            logger.info("Circuit breaker: successful operation, closing circuit")
            self.circuit_open_time = None

    def record_failure(self):
        """Record a failed operation."""
        self.consecutive_failures += 1

        if self.consecutive_failures >= self.failure_threshold:
            if not self.is_open():
                logger.error(
                    f"Circuit breaker: {self.consecutive_failures} consecutive failures, "
                    f"opening circuit for {self.cooldown_seconds}s"
                )
                self.circuit_open_time = time.time()

    def __call__(self, func: Callable[..., T]) -> Callable[..., T]:
        """Use circuit breaker as a decorator."""
        @wraps(func)
        def wrapper(*args, **kwargs) -> T:
            if self.is_open():
                raise Exception(
                    f"Circuit breaker is open. Too many consecutive failures. "
                    f"Try again in {int(self.cooldown_seconds - (time.time() - self.circuit_open_time))}s"
                )

            try:
                result = func(*args, **kwargs)
                self.record_success()
                return result
            except Exception as e:
                self.record_failure()
                raise

        return wrapper


# Global circuit breaker instance for RFC operations
rfc_circuit_breaker = CircuitBreaker(
    failure_threshold=5,
    cooldown_seconds=60.0
)
