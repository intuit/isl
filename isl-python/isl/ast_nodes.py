"""AST nodes for ISL."""
from __future__ import annotations
from dataclasses import dataclass, field
from typing import Any, Optional


# ── Top-level ────────────────────────────────────────────────────────────────

@dataclass
class Module:
    functions: list[FunctionDecl] = field(default_factory=list)
    statements: list[Any] = field(default_factory=list)  # flat statements (no functions)


@dataclass
class FunctionDecl:
    name: str
    params: list[str]  # variable names (without $)
    body: list[Any]
    is_modifier: bool = False
    cached: bool = False


# ── Statements ────────────────────────────────────────────────────────────────

@dataclass
class AssignProperty:
    """key: value  or  key.sub.path: value"""
    path: list[str]  # property path segments
    value: Any
    dynamic_key: Optional[Any] = None  # interpolated key


@dataclass
class AssignVariable:
    """$var = value"""
    name: str  # without $
    value: Any


@dataclass
class ReturnStatement:
    value: Any


@dataclass
class IfStatement:
    condition: Any
    true_body: list[Any]
    false_body: list[Any] = field(default_factory=list)


@dataclass
class InlineIf:
    condition: Any
    true_val: Any
    false_val: Any = None


@dataclass
class SwitchStatement:
    subject: Any
    cases: list[SwitchCase]
    else_case: Optional[Any] = None


@dataclass
class SwitchCase:
    op: Optional[str]  # relop string or None for default match
    match_val: Any
    body: Any


@dataclass
class ForEach:
    iterator_var: str  # without $
    iterable: Any
    body: list[Any]
    body_object: Optional[Any] = None  # trailing declare_object
    parallel: bool = False


@dataclass
class WhileLoop:
    condition: Any
    body: list[Any]
    max_loops: int = 50


# ── Expressions ────────────────────────────────────────────────────────────────

@dataclass
class VariableSelector:
    """$var.path.parts  with optional index/filter"""
    parts: list[SelectorPart]


@dataclass
class SelectorPart:
    name: Optional[str]  # None for bare $
    index: Optional[int] = None
    condition: Optional[Any] = None
    text_key: Optional[str] = None  # ["quoted"] access


@dataclass
class ModifierChain:
    value: Any
    modifiers: list[Modifier]


@dataclass
class Modifier:
    name: str  # e.g. "upperCase", "trim", "map", "filter"
    args: list[Any] = field(default_factory=list)
    condition: Optional[Any] = None  # for filter/if modifiers
    is_filter: bool = False
    is_map: bool = False
    is_cond: bool = False


@dataclass
class FunctionCall:
    service: str
    name: Optional[str]
    args: list[Any]
    body: list[Any] = field(default_factory=list)  # trailing block


@dataclass
class Coalesce:
    left: Any
    right: Any


@dataclass
class Spread:
    source: Any


# ── Literals & collections ─────────────────────────────────────────────────────

@dataclass
class Literal:
    value: Any  # str, int, float, bool, None


@dataclass
class ArrayLiteral:
    elements: list[Any]


@dataclass
class ObjectLiteral:
    """{ key: val, ... }"""
    entries: list[Any]  # list of AssignProperty / AssignVariable / Spread


# ── Math ───────────────────────────────────────────────────────────────────────

@dataclass
class MathExpr:
    op: str  # '+', '-', '*', '/'
    left: Any
    right: Any


@dataclass
class MathBlock:
    """{{ expr }}"""
    expr: Any


# ── Interpolation ──────────────────────────────────────────────────────────────

@dataclass
class Interpolation:
    """Backtick string: sequence of parts."""
    parts: list[Any]  # str | VariableSelector | ModifierChain | MathBlock | FunctionCall


@dataclass
class InterpolationText:
    text: str


@dataclass
class InterpolationVar:
    """$varname bare reference inside backtick"""
    name: str  # without $
