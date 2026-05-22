"""Type conversion modifiers for ISL."""
from __future__ import annotations


_SENTINEL = object()


def apply_type_modifier(value: any, name: str, args: list) -> any:
    if name == 'to.string':
        if args:
            # to.string with format — handled by date modifier
            return _SENTINEL
        if value is None:
            return ''
        return str(value)

    if name == 'to.number':
        if value is None:
            return 0
        try:
            f = float(value)
            return int(f) if f == int(f) else f
        except (TypeError, ValueError):
            return 0

    if name == 'to.integer':
        if value is None:
            return 0
        try:
            return int(float(value))
        except (TypeError, ValueError):
            return 0

    if name == 'to.decimal':
        if value is None:
            return 0.0
        try:
            return float(value)
        except (TypeError, ValueError):
            return 0.0

    if name == 'to.boolean':
        if value is None:
            return False
        if isinstance(value, bool):
            return value
        if isinstance(value, (int, float)):
            return value != 0
        s = str(value).lower()
        return s in ('true', '1', 'yes', 'on')

    return _SENTINEL
