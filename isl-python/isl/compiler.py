"""ISL compiler — parse source and return a runnable Transformer."""
from __future__ import annotations
from typing import Any
from .parser import parse
from .interpreter import Interpreter
from .context import ExecutionContext


class ISLTransformer:
    """A compiled ISL module ready to run."""

    def __init__(self, name: str, module, extensions: dict = None):
        self.name = name
        self.module = module
        self._extensions = extensions or {}

    def run(self, function_name: str, ctx: ExecutionContext) -> Any:
        """Execute a named function in this module.

        Args:
            function_name: The ISL function to call (e.g. 'run').
            ctx: ExecutionContext with variables and extensions.

        Returns:
            Python dict/list/str/int/float/bool/None.
        """
        # Merge extensions from ctx and compile-time
        extensions = {**self._extensions, **ctx.extensions}
        interp = Interpreter(self.module, extensions=extensions)

        if self.module.functions:
            # Module has explicit function declarations — call the named one
            if function_name not in interp.functions:
                raise ValueError(f'Function {function_name!r} not found in ISL module {self.name!r}')
            fn = interp.functions[function_name]
            # Build args from ctx variables matching param names
            args = []
            for param in fn.params:
                args.append(ctx.variables.get(param))
            return interp.call_function(function_name, args, ctx)
        else:
            # Flat statements module
            return interp.run_statements(self.module.statements, ctx)


def compile_isl(name: str, source: str, extensions: dict = None) -> ISLTransformer:
    """Parse and compile ISL source code.

    Args:
        name: Identifier for this module (for error messages).
        source: The ISL source code string.
        extensions: Optional dict of extension callables.

    Returns:
        ISLTransformer instance.
    """
    module = parse(source)
    return ISLTransformer(name=name, module=module, extensions=extensions or {})
