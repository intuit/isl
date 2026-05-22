"""String modifiers for ISL."""
from __future__ import annotations
import re


def apply_string_modifier(value: any, name: str, args: list) -> any:
    """Apply a string modifier. Returns None if not a string modifier."""
    if name == 'trim':
        return str(value).strip() if value is not None else ''
    if name == 'upperCase':
        return str(value).upper() if value is not None else ''
    if name == 'lowerCase':
        return str(value).lower() if value is not None else ''
    if name == 'capitalize':
        s = str(value) if value is not None else ''
        return s[0].upper() + s[1:] if s else s
    if name == 'titleCase':
        return str(value).title() if value is not None else ''
    if name == 'length':
        if isinstance(value, (str, list)):
            return len(value)
        return 0
    if name == 'padStart':
        n = int(args[0]) if args else 0
        ch = str(args[1]) if len(args) > 1 else ' '
        return str(value).rjust(n, ch)
    if name == 'padEnd':
        n = int(args[0]) if args else 0
        ch = str(args[1]) if len(args) > 1 else ' '
        return str(value).ljust(n, ch)
    if name == 'split':
        sep = str(args[0]) if args else ','
        return str(value).split(sep) if value is not None else []
    if name == 'replace':
        a = str(args[0]) if args else ''
        b = str(args[1]) if len(args) > 1 else ''
        return str(value).replace(a, b) if value is not None else ''
    if name == 'startsWith':
        s = str(args[0]) if args else ''
        return str(value).startswith(s) if value is not None else False
    if name == 'endsWith':
        s = str(args[0]) if args else ''
        return str(value).endswith(s) if value is not None else False
    if name == 'contains':
        s = str(args[0]) if args else ''
        return s in str(value) if value is not None else False
    if name == 'matches':
        pattern = str(args[0]) if args else ''
        return bool(re.search(pattern, str(value))) if value is not None else False
    if name == 'truncate':
        n = int(args[0]) if args else 100
        suffix = str(args[1]) if len(args) > 1 else '...'
        s = str(value) if value is not None else ''
        return s[:n] + suffix if len(s) > n else s
    if name == 'substring':
        start = int(args[0]) if args else 0
        end = int(args[1]) if len(args) > 1 else None
        s = str(value) if value is not None else ''
        return s[start:end]
    return _SENTINEL


_SENTINEL = object()
