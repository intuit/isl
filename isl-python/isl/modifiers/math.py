"""Math modifiers for ISL."""
from __future__ import annotations


_SENTINEL = object()


def apply_math_modifier(value: any, name: str, args: list) -> any:
    if name == 'precision':
        n = int(args[0]) if args else 2
        try:
            return round(float(value), n)
        except (TypeError, ValueError):
            return value
    if name == 'abs':
        try:
            return abs(float(value))
        except (TypeError, ValueError):
            return value
    if name == 'ceil':
        import math
        try:
            return math.ceil(float(value))
        except (TypeError, ValueError):
            return value
    if name == 'floor':
        import math
        try:
            return math.floor(float(value))
        except (TypeError, ValueError):
            return value
    return _SENTINEL
