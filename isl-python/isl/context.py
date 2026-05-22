"""ExecutionContext for ISL."""
from __future__ import annotations
from typing import Any, Callable


class ExecutionContext:
    """Holds variables and extension registrations for ISL execution."""

    def __init__(self):
        self.variables: dict[str, Any] = {}
        self.extensions: dict[str, Callable] = {}

    def set_variable(self, name: str, value: Any):
        """Set a variable. Name may include leading $."""
        key = name.lstrip('$')
        self.variables[key] = value

    def get_variable(self, name: str) -> Any:
        key = name.lstrip('$')
        return self.variables.get(key)

    def register_extension(self, name: str, fn: Callable):
        """Register a callable as an extension method.

        name examples: 'Date.Now', 'MyService.doThing'
        """
        self.extensions[name] = fn
