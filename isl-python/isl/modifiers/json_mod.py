"""JSON modifier for ISL."""
from __future__ import annotations
import json
from typing import Any


_SENTINEL = object()


def apply_json_modifier(value: Any, name: str, args: list) -> Any:
    if name == 'json':
        return json.dumps(value, default=str)
    if name == 'json.parse':
        if isinstance(value, str):
            try:
                return json.loads(value)
            except (json.JSONDecodeError, ValueError):
                return value
        return value
    return _SENTINEL
