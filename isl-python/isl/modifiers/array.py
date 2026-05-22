"""Array modifiers for ISL."""
from __future__ import annotations
from typing import Any, Callable


_SENTINEL = object()


def apply_array_modifier(value: Any, name: str, args: list,
                          eval_fn: Callable = None) -> Any:
    """Apply an array modifier."""
    if name == 'length':
        if isinstance(value, (list, str)):
            return len(value)
        if isinstance(value, dict):
            return len(value)
        return 0

    if name == 'first':
        if isinstance(value, list):
            return value[0] if value else None
        return value

    if name == 'last':
        if isinstance(value, list):
            return value[-1] if value else None
        return value

    if name == 'unique':
        if not isinstance(value, list):
            return value
        seen = []
        result = []
        for item in value:
            key = item if not isinstance(item, dict) else str(item)
            if key not in seen:
                seen.append(key)
                result.append(item)
        return result

    if name == 'sort':
        if not isinstance(value, list):
            return value
        try:
            return sorted(value)
        except TypeError:
            return sorted(value, key=str)

    if name == 'join':
        sep = str(args[0]) if args else ','
        if isinstance(value, list):
            return sep.join(str(i) for i in value)
        return str(value)

    if name == 'reverse':
        if isinstance(value, list):
            return list(reversed(value))
        return value

    if name == 'flatten':
        if not isinstance(value, list):
            return value
        result = []
        for item in value:
            if isinstance(item, list):
                result.extend(item)
            else:
                result.append(item)
        return result

    if name == 'Math.sum':
        init = args[0] if args else 0
        if not isinstance(value, list):
            return init
        total = float(init) if isinstance(init, float) else int(init)
        for item in value:
            if item is not None:
                try:
                    total += float(item) if isinstance(total, float) else _to_num(item)
                except (TypeError, ValueError):
                    pass
        return total

    if name == 'Math.min':
        if not isinstance(value, list) or not value:
            return None
        try:
            return min(float(v) for v in value if v is not None)
        except (TypeError, ValueError):
            return None

    if name == 'Math.max':
        if not isinstance(value, list) or not value:
            return None
        try:
            return max(float(v) for v in value if v is not None)
        except (TypeError, ValueError):
            return None

    if name == 'Math.clamp':
        lo = float(args[0]) if args else 0
        hi = float(args[1]) if len(args) > 1 else float('inf')
        try:
            v = float(value)
            return max(lo, min(hi, v))
        except (TypeError, ValueError):
            return value

    return _SENTINEL


def _to_num(v: Any):
    if isinstance(v, (int, float)):
        return v
    try:
        return int(v)
    except (TypeError, ValueError):
        return float(v)
