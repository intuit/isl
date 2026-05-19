"""Date modifiers for ISL."""
from __future__ import annotations
from datetime import datetime
from typing import Any


_SENTINEL = object()


def _java_fmt_to_python(fmt: str) -> str:
    """Convert Java date format string to Python strptime/strftime format."""
    # Order matters — longer patterns first
    mapping = [
        ("yyyy", "%Y"),
        ("yy", "%y"),
        ("MM", "%m"),
        ("dd", "%d"),
        ("HH", "%H"),
        ("hh", "%I"),
        ("mm", "%M"),
        ("ss", "%S"),
        ("SSS", "%f"),
        ("XXX", "%z"),
        ("XX", "%z"),
        ("X", "%z"),
        ("Z", "%z"),
        ("a", "%p"),
    ]
    result = fmt
    # Remove literal single-quoted sections for now (e.g. 'T')
    result = result.replace("'T'", "T")
    result = result.replace("'", "")
    for java, py in mapping:
        result = result.replace(java, py)
    return result


# Store parsed datetime between date.parse and to.string
_DATE_SENTINEL = '___isl_datetime___'


class ISLDatetime:
    """Wrapper to carry a parsed datetime through the modifier pipeline."""
    def __init__(self, dt: datetime):
        self.dt = dt

    def __str__(self):
        return self.dt.isoformat()


def apply_date_modifier(value: Any, name: str, args: list) -> Any:
    if name == 'date.parse':
        fmt_java = str(args[0]) if args else "yyyy-MM-dd'T'HH:mm:ssXXX"
        fmt_py = _java_fmt_to_python(fmt_java)
        s = str(value) if value is not None else ''
        try:
            dt = datetime.strptime(s, fmt_py)
            return ISLDatetime(dt)
        except ValueError:
            # Try without timezone
            try:
                fmt_no_tz = fmt_py.replace('%z', '').rstrip()
                s_no_tz = s
                # Strip timezone suffix
                import re
                s_no_tz = re.sub(r'[+-]\d{2}:?\d{2}$', '', s)
                dt = datetime.strptime(s_no_tz.strip(), fmt_no_tz.strip())
                return ISLDatetime(dt)
            except ValueError:
                return value

    if name == 'to.string' and args:
        fmt_java = str(args[0])
        fmt_py = _java_fmt_to_python(fmt_java)
        if isinstance(value, ISLDatetime):
            return value.dt.strftime(fmt_py)
        if isinstance(value, datetime):
            return value.strftime(fmt_py)
        # try to parse ISO format
        try:
            dt = datetime.fromisoformat(str(value))
            return dt.strftime(fmt_py)
        except (ValueError, TypeError):
            return str(value)

    if name == 'date.format':
        fmt_java = str(args[0]) if args else "yyyy-MM-dd"
        fmt_py = _java_fmt_to_python(fmt_java)
        if isinstance(value, ISLDatetime):
            return value.dt.strftime(fmt_py)
        if isinstance(value, datetime):
            return value.strftime(fmt_py)
        return str(value)

    return _SENTINEL
