"""Response formatting utilities for MCP server.

This module provides utilities for managing response sizes, implementing
character limits, and creating truncated responses with helpful guidance.
"""

import json
import logging
from typing import Any, Tuple, Dict, List, Optional

logger = logging.getLogger(__name__)

# Maximum characters allowed in a response to prevent token overflow
# Based on MCP best practices: typically 25,000 characters
CHARACTER_LIMIT = 25000


def calculate_response_size(data: Any) -> int:
    """
    Calculate the size of a response in characters.

    Args:
        data: Response data (dict, list, str, or any JSON-serializable object)

    Returns:
        Number of characters in the stringified response
    """
    if isinstance(data, str):
        return len(data)

    try:
        # Convert to JSON string to get accurate character count
        json_str = json.dumps(data, ensure_ascii=False, default=str)
        return len(json_str)
    except (TypeError, ValueError) as e:
        logger.warning(f"Failed to calculate response size: {e}")
        # Fallback to str() representation
        return len(str(data))


def truncate_response(
    data: Any,
    limit: int = CHARACTER_LIMIT,
    suggestions: Optional[List[str]] = None
) -> Tuple[Any, bool, Dict[str, Any]]:
    """
    Truncate a response if it exceeds the character limit.

    Args:
        data: Response data to potentially truncate
        limit: Character limit (default: CHARACTER_LIMIT)
        suggestions: Optional list of suggestions for the user/LLM

    Returns:
        Tuple of (truncated_data, was_truncated, metadata)

    Example:
        >>> data = {"items": [1, 2, 3, ...1000 items]}
        >>> truncated, was_truncated, meta = truncate_response(data)
        >>> print(meta)
        {
            "truncated": True,
            "original_size": 50000,
            "truncated_size": 25000,
            "message": "Response truncated...",
            "suggestions": [...]
        }
    """
    original_size = calculate_response_size(data)

    # No truncation needed
    if original_size <= limit:
        return data, False, {
            "truncated": False,
            "original_size": original_size
        }

    logger.info(
        f"Response size ({original_size} chars) exceeds limit ({limit} chars). "
        f"Truncating..."
    )

    # Truncate based on data type
    truncated_data = _truncate_by_type(data, limit)
    truncated_size = calculate_response_size(truncated_data)

    # Create truncation message
    message = create_truncation_message(
        original_size=original_size,
        truncated_size=truncated_size,
        suggestions=suggestions or []
    )

    metadata = {
        "truncated": True,
        "original_size": original_size,
        "truncated_size": truncated_size,
        "reduction_percentage": round((1 - truncated_size / original_size) * 100, 2),
        "message": message
    }

    if suggestions:
        metadata["suggestions"] = suggestions

    return truncated_data, True, metadata


def _truncate_by_type(data: Any, limit: int) -> Any:
    """
    Truncate data based on its type.

    Strategy:
    - For dicts with 'objects' or 'items' lists: Reduce list to ~50%
    - For lists: Reduce to ~50% of items
    - For strings: Truncate to limit with ellipsis
    - For other types: Convert to string and truncate

    Args:
        data: Data to truncate
        limit: Character limit

    Returns:
        Truncated data
    """
    if isinstance(data, dict):
        # Handle dictionaries with list fields (common pattern)
        return _truncate_dict_with_lists(data, limit)

    elif isinstance(data, list):
        # Truncate list to approximately half
        target_length = max(1, len(data) // 2)
        return data[:target_length]

    elif isinstance(data, str):
        # Simple string truncation
        if len(data) > limit:
            return data[:limit - 3] + "..."
        return data

    else:
        # For other types, convert to string and truncate
        str_data = str(data)
        if len(str_data) > limit:
            return str_data[:limit - 3] + "..."
        return str_data


def _truncate_dict_with_lists(data: dict, limit: int) -> dict:
    """
    Truncate a dictionary that contains lists.

    Common pattern for MCP responses:
    {
        "package_name": "ZFI",
        "total_objects": 241,
        "object_types": {
            "CLAS": {"count": 7, "objects": [...]},
            "PROG": {"count": 121, "objects": [...]}
        }
    }

    Strategy: Reduce the 'objects' arrays to ~50% while keeping metadata intact.

    Args:
        data: Dictionary to truncate
        limit: Character limit

    Returns:
        Truncated dictionary
    """
    result = {}

    for key, value in data.items():
        if isinstance(value, list):
            # Truncate lists to approximately half
            target_length = max(1, len(value) // 2)
            result[key] = value[:target_length]

        elif isinstance(value, dict):
            # Recursively handle nested dicts
            nested_result = {}
            for nested_key, nested_value in value.items():
                if nested_key == "objects" and isinstance(nested_value, list):
                    # Truncate object lists
                    target_length = max(1, len(nested_value) // 2)
                    nested_result[nested_key] = nested_value[:target_length]
                elif isinstance(nested_value, dict):
                    # Recursively truncate nested dicts
                    nested_result[nested_key] = _truncate_dict_with_lists(
                        nested_value, limit
                    )
                else:
                    # Keep metadata fields intact
                    nested_result[nested_key] = nested_value

            result[key] = nested_result

        else:
            # Keep simple values (strings, numbers, bools) intact
            result[key] = value

    return result


def create_truncation_message(
    original_size: int,
    truncated_size: int,
    suggestions: List[str]
) -> str:
    """
    Create a helpful truncation message with suggestions.

    Args:
        original_size: Original response size in characters
        truncated_size: Truncated response size in characters
        suggestions: List of suggestions for the user/LLM

    Returns:
        Formatted truncation message

    Example:
        >>> msg = create_truncation_message(50000, 25000, [
        ...     "Use pagination: offset=50",
        ...     "Add filters: object_types=['CLAS']"
        ... ])
        >>> print(msg)
        Response truncated from 50,000 to 25,000 characters (50% reduction).

        To get complete results, try:
        - Use pagination: offset=50
        - Add filters: object_types=['CLAS']
    """
    # Format numbers with thousand separators
    orig_str = f"{original_size:,}"
    trunc_str = f"{truncated_size:,}"
    reduction = round((1 - truncated_size / original_size) * 100)

    message = (
        f"Response truncated from {orig_str} to {trunc_str} characters "
        f"({reduction}% reduction)."
    )

    if suggestions:
        message += "\n\nTo get complete results, try:"
        for suggestion in suggestions:
            message += f"\n- {suggestion}"

    return message


def should_truncate(data: Any, limit: int = CHARACTER_LIMIT) -> bool:
    """
    Check if data should be truncated based on size.

    Args:
        data: Data to check
        limit: Character limit

    Returns:
        True if data exceeds limit, False otherwise
    """
    return calculate_response_size(data) > limit


def format_size_human_readable(size: int) -> str:
    """
    Format a size in characters to human-readable format.

    Args:
        size: Size in characters

    Returns:
        Human-readable size string

    Example:
        >>> format_size_human_readable(1500)
        "1.5K"
        >>> format_size_human_readable(50000)
        "50K"
        >>> format_size_human_readable(1500000)
        "1.5M"
    """
    if size < 1000:
        return str(size)
    elif size < 1_000_000:
        return f"{size / 1000:.1f}K"
    else:
        return f"{size / 1_000_000:.1f}M"
