"""ISL interpreter — walks the AST and produces Python values."""
from __future__ import annotations
import re
from collections import ChainMap
from datetime import datetime
from typing import Any, Callable, Optional

from .ast_nodes import (
    Module, FunctionDecl, AssignProperty, AssignVariable, ReturnStatement,
    IfStatement, InlineIf, SwitchStatement, SwitchCase,
    ForEach, WhileLoop, VariableSelector, SelectorPart,
    ModifierChain, Modifier, FunctionCall, Coalesce, Spread,
    Literal, ArrayLiteral, ObjectLiteral, MathExpr, MathBlock,
    Interpolation, InterpolationText, InterpolationVar,
)
from .modifiers.string import apply_string_modifier
from .modifiers.array import apply_array_modifier
from .modifiers.math import apply_math_modifier
from .modifiers.type_conv import apply_type_modifier
from .modifiers.date import apply_date_modifier, ISLDatetime
from .modifiers.json_mod import apply_json_modifier

# We use a single sentinel object that all modifier modules share
# Import the sentinels so we can test against them
from .modifiers.string import _SENTINEL as _STR_SENTINEL
from .modifiers.array import _SENTINEL as _ARR_SENTINEL
from .modifiers.math import _SENTINEL as _MATH_SENTINEL
from .modifiers.type_conv import _SENTINEL as _TYPE_SENTINEL
from .modifiers.date import _SENTINEL as _DATE_SENTINEL
from .modifiers.json_mod import _SENTINEL as _JSON_SENTINEL

_SENTINEL = object()  # local sentinel


class ReturnSignal(Exception):
    def __init__(self, value):
        self.value = value


class ISLError(Exception):
    pass


class Interpreter:
    def __init__(self, module: Module, extensions: dict[str, Callable] = None):
        self.module = module
        self.extensions = extensions or {}
        # Build function lookup
        self.functions: dict[str, FunctionDecl] = {
            fn.name: fn for fn in module.functions
        }

    def call_function(self, name: str, args: list[Any], ctx: 'ExecutionContext') -> Any:
        if name not in self.functions:
            raise ISLError(f'Function {name!r} not defined')
        fn = self.functions[name]
        local_vars: dict[str, Any] = {}
        for i, param in enumerate(fn.params):
            local_vars[param] = args[i] if i < len(args) else None
        scope = ChainMap(local_vars, ctx.variables)
        return self._exec_body(fn.body, scope, ctx)

    def run_statements(self, stmts: list, ctx: 'ExecutionContext') -> dict:
        """Run flat statements and return the accumulated result dict."""
        scope = ChainMap({}, ctx.variables)
        result = {}
        for stmt in stmts:
            self._exec_stmt_into(stmt, scope, ctx, result)
        return result

    def _exec_body(self, stmts: list, scope: ChainMap, ctx: 'ExecutionContext') -> Any:
        """Execute function body statements; return value from ReturnStatement."""
        result = {}
        try:
            for stmt in stmts:
                self._exec_stmt_into(stmt, scope, ctx, result)
        except ReturnSignal as ret:
            return ret.value
        # If there's only one key in result and it's not a meaningful name,
        # return the result dict
        return result

    def _exec_stmt_into(self, stmt: Any, scope: ChainMap, ctx: 'ExecutionContext', result: dict):
        if stmt is None:
            return

        if isinstance(stmt, ReturnStatement):
            val = self._eval(stmt.value, scope, ctx)
            raise ReturnSignal(val)

        if isinstance(stmt, AssignVariable):
            val = self._eval(stmt.value, scope, ctx)
            scope[stmt.name] = val
            return

        if isinstance(stmt, AssignProperty):
            val = self._eval(stmt.value, scope, ctx)
            if stmt.dynamic_key is not None:
                key = str(self._eval(stmt.dynamic_key, scope, ctx))
                _deep_set(result, [key], val)
            else:
                _deep_set(result, stmt.path, val)
            return

        if isinstance(stmt, IfStatement):
            cond_val = self._eval_condition(stmt.condition, scope, ctx)
            body = stmt.true_body if cond_val else stmt.false_body
            for s in body:
                self._exec_stmt_into(s, scope, ctx, result)
            return

        if isinstance(stmt, SwitchStatement):
            self._exec_switch(stmt, scope, ctx, result)
            return

        if isinstance(stmt, ForEach):
            # ForEach at statement level — run but discard into result
            self._eval_foreach(stmt, scope, ctx)
            return

        if isinstance(stmt, FunctionCall):
            self._eval_function_call(stmt, scope, ctx)
            return

        if isinstance(stmt, WhileLoop):
            self._eval_while(stmt, scope, ctx)
            return

    def _eval(self, node: Any, scope: ChainMap, ctx: 'ExecutionContext') -> Any:
        """Evaluate an expression node."""
        if node is None:
            return None

        if isinstance(node, Literal):
            return node.value

        if isinstance(node, VariableSelector):
            return self._eval_var_selector(node, scope, ctx)

        if isinstance(node, ModifierChain):
            val = self._eval(node.value, scope, ctx)
            for mod in node.modifiers:
                val = self._apply_modifier(val, mod, scope, ctx)
            return val

        if isinstance(node, FunctionCall):
            return self._eval_function_call(node, scope, ctx)

        if isinstance(node, ObjectLiteral):
            return self._eval_object(node, scope, ctx)

        if isinstance(node, ArrayLiteral):
            return self._eval_array(node, scope, ctx)

        if isinstance(node, MathBlock):
            return self._eval_math(node.expr, scope, ctx)

        if isinstance(node, MathExpr):
            return self._eval_math(node, scope, ctx)

        if isinstance(node, Interpolation):
            return self._eval_interpolation(node, scope, ctx)

        if isinstance(node, InterpolationText):
            return node.text

        if isinstance(node, InterpolationVar):
            return self._lookup_var(node.name, scope)

        if isinstance(node, InlineIf):
            cond = self._eval_condition(node.condition, scope, ctx)
            if cond:
                return self._eval(node.true_val, scope, ctx)
            return self._eval(node.false_val, scope, ctx)

        if isinstance(node, SwitchStatement):
            return self._eval_switch(node, scope, ctx)

        if isinstance(node, ForEach):
            return self._eval_foreach(node, scope, ctx)

        if isinstance(node, WhileLoop):
            return self._eval_while(node, scope, ctx)

        if isinstance(node, Coalesce):
            left = self._eval(node.left, scope, ctx)
            if left is not None:
                return left
            return self._eval(node.right, scope, ctx)

        if isinstance(node, Spread):
            return self._eval(node.source, scope, ctx)

        # Passthrough plain Python values
        return node

    def _eval_var_selector(self, node: VariableSelector, scope: ChainMap,
                            ctx: 'ExecutionContext') -> Any:
        if not node.parts:
            return None

        first = node.parts[0]
        if first.name is None:
            # bare $ — iterator variable
            val = scope.get('$', scope.get('it', None))
        else:
            val = self._lookup_var(first.name, scope)

        val = self._apply_selector_part_access(val, first, scope, ctx)

        for part in node.parts[1:]:
            if val is None:
                return None
            if part.text_key is not None:
                if isinstance(val, dict):
                    val = val.get(part.text_key)
                else:
                    val = None
            elif part.name is not None:
                if isinstance(val, dict):
                    val = val.get(part.name)
                elif isinstance(val, list):
                    # map access over list
                    val = [item.get(part.name) if isinstance(item, dict) else None
                           for item in val]
                else:
                    val = None
            val = self._apply_selector_part_access(val, part, scope, ctx)

        return val

    def _apply_selector_part_access(self, val: Any, part: SelectorPart,
                                     scope: ChainMap, ctx: 'ExecutionContext') -> Any:
        if part.index is not None:
            if isinstance(val, list):
                idx = part.index
                if idx < 0:
                    idx = len(val) + idx
                return val[idx] if 0 <= idx < len(val) else None
        if part.condition is not None:
            if isinstance(val, list):
                return [item for item in val
                        if self._eval_condition(part.condition,
                                                ChainMap({'$': item, 'it': item}, scope),
                                                ctx)]
        return val

    def _lookup_var(self, name: str, scope: ChainMap) -> Any:
        if name in scope:
            return scope[name]
        return None

    def _eval_object(self, node: ObjectLiteral, scope: ChainMap,
                      ctx: 'ExecutionContext') -> dict:
        result = {}
        for entry in node.entries:
            if isinstance(entry, Spread):
                src = self._eval(entry.source, scope, ctx)
                if isinstance(src, dict):
                    result.update(src)
            elif isinstance(entry, AssignProperty):
                val = self._eval(entry.value, scope, ctx)
                if entry.dynamic_key is not None:
                    key = str(self._eval(entry.dynamic_key, scope, ctx))
                    _deep_set(result, [key], val)
                else:
                    _deep_set(result, entry.path, val)
            elif isinstance(entry, AssignVariable):
                val = self._eval(entry.value, scope, ctx)
                scope[entry.name] = val
        return result

    def _eval_array(self, node: ArrayLiteral, scope: ChainMap,
                     ctx: 'ExecutionContext') -> list:
        result = []
        for elem in node.elements:
            if isinstance(elem, Spread):
                src = self._eval(elem.source, scope, ctx)
                if isinstance(src, list):
                    result.extend(src)
                elif src is not None:
                    result.append(src)
            else:
                result.append(self._eval(elem, scope, ctx))
        return result

    def _eval_math(self, node: Any, scope: ChainMap, ctx: 'ExecutionContext') -> Any:
        if isinstance(node, MathBlock):
            return self._eval_math(node.expr, scope, ctx)
        if isinstance(node, MathExpr):
            left = self._eval_math(node.left, scope, ctx)
            right = self._eval_math(node.right, scope, ctx)
            try:
                l = float(left) if left is not None else 0.0
                r = float(right) if right is not None else 0.0
                if node.op == '+':
                    return l + r
                if node.op == '-':
                    return l - r
                if node.op == '*':
                    return l * r
                if node.op == '/':
                    return l / r if r != 0 else 0.0
            except (TypeError, ValueError):
                return 0.0
        if isinstance(node, Literal):
            return node.value
        if isinstance(node, VariableSelector):
            val = self._eval_var_selector(node, scope, ctx)
            try:
                return float(val) if val is not None else 0.0
            except (TypeError, ValueError):
                return 0.0
        if isinstance(node, FunctionCall):
            return self._eval_function_call(node, scope, ctx)
        # plain number
        try:
            return float(node)
        except (TypeError, ValueError):
            return 0.0

    def _eval_interpolation(self, node: Interpolation, scope: ChainMap,
                             ctx: 'ExecutionContext') -> str:
        parts = []
        for part in node.parts:
            if isinstance(part, InterpolationText):
                parts.append(part.text)
            elif isinstance(part, InterpolationVar):
                val = self._lookup_var(part.name, scope)
                parts.append(_to_str(val))
            else:
                val = self._eval(part, scope, ctx)
                parts.append(_to_str(val))
        return ''.join(parts)

    def _eval_condition(self, cond: Any, scope: ChainMap, ctx: 'ExecutionContext') -> bool:
        if cond is None:
            return False
        if isinstance(cond, dict):
            op = cond.get('op')
            if op == 'and':
                return (self._eval_condition(cond['left'], scope, ctx) and
                        self._eval_condition(cond['right'], scope, ctx))
            if op == 'or':
                return (self._eval_condition(cond['left'], scope, ctx) or
                        self._eval_condition(cond['right'], scope, ctx))
            if op == 'not':
                return not self._eval_condition(cond['expr'], scope, ctx)
            if op == 'truthy':
                val = self._eval(cond['expr'], scope, ctx)
                return bool(val)
            if op in ('==', '!=', '<', '<=', '>', '>=',
                      'contains', '!contains', 'startsWith', '!startsWith',
                      'endsWith', '!endsWith', 'in', '!in', 'is', '!is',
                      'matches', '!matches'):
                left = self._eval(cond['left'], scope, ctx)
                right = self._eval(cond['right'], scope, ctx)
                return _compare(left, op, right)
        # Raw boolean
        if isinstance(cond, bool):
            return cond
        # Evaluate as expression
        val = self._eval(cond, scope, ctx)
        return bool(val)

    def _exec_switch(self, node: SwitchStatement, scope: ChainMap,
                      ctx: 'ExecutionContext', result: dict):
        val = self._eval(node.subject, scope, ctx)
        for case in node.cases:
            match_val = self._eval(case.match_val, scope, ctx)
            op = case.op or '=='
            if _compare(val, op, match_val):
                self._exec_stmt_into(case.body, scope, ctx, result)
                return
        if node.else_case is not None:
            self._exec_stmt_into(node.else_case, scope, ctx, result)

    def _eval_switch(self, node: SwitchStatement, scope: ChainMap,
                      ctx: 'ExecutionContext') -> Any:
        val = self._eval(node.subject, scope, ctx)
        for case in node.cases:
            match_val = self._eval(case.match_val, scope, ctx)
            op = case.op or '=='
            if _compare(val, op, match_val):
                return self._eval(case.body, scope, ctx)
        if node.else_case is not None:
            return self._eval(node.else_case, scope, ctx)
        return None

    def _eval_foreach(self, node: ForEach, scope: ChainMap,
                       ctx: 'ExecutionContext') -> list:
        iterable = self._eval(node.iterable, scope, ctx)
        if iterable is None:
            return []
        if not isinstance(iterable, list):
            iterable = [iterable]

        results = []
        for item in iterable:
            local = ChainMap({node.iterator_var: item, '$': item, 'it': item}, scope)
            if node.body_object is not None:
                obj = self._eval(node.body_object, local, ctx)
                results.append(obj)
            elif node.body:
                sub_result = {}
                try:
                    for stmt in node.body:
                        self._exec_stmt_into(stmt, local, ctx, sub_result)
                except ReturnSignal as ret:
                    results.append(ret.value)
                    continue
                if sub_result:
                    results.append(sub_result)
            else:
                results.append(item)
        return results

    def _eval_while(self, node: WhileLoop, scope: ChainMap,
                     ctx: 'ExecutionContext') -> Any:
        max_loops = node.max_loops
        count = 0
        result = None
        while count < max_loops:
            if not self._eval_condition(node.condition, scope, ctx):
                break
            sub_result = {}
            try:
                for stmt in node.body:
                    self._exec_stmt_into(stmt, scope, ctx, sub_result)
            except ReturnSignal as ret:
                return ret.value
            result = sub_result
            count += 1
        return result

    def _eval_function_call(self, node: FunctionCall, scope: ChainMap,
                             ctx: 'ExecutionContext') -> Any:
        args = [self._eval(a, scope, ctx) for a in node.args]

        # @.This.FunctionName — call user-defined function
        if node.service == 'This':
            fn_name = node.name
            if fn_name and fn_name in self.functions:
                fn = self.functions[fn_name]
                local_vars: dict[str, Any] = {}
                for i, param in enumerate(fn.params):
                    local_vars[param] = args[i] if i < len(args) else None
                new_scope = ChainMap(local_vars, scope)
                return self._exec_body(fn.body, new_scope, ctx)

        # @.Date.Now
        if node.service == 'Date' and node.name == 'Now':
            return datetime.now()

        # Check registered extensions
        ext_key_full = f'{node.service}.{node.name}' if node.name else node.service
        if ext_key_full in self.extensions:
            return self.extensions[ext_key_full](*args)

        ext_key_service = node.service
        if ext_key_service in self.extensions:
            return self.extensions[ext_key_service](*args)

        # Unknown — return None
        return None

    def _apply_modifier(self, value: Any, mod: Modifier, scope: ChainMap,
                         ctx: 'ExecutionContext') -> Any:
        # Evaluate modifier arguments
        evaled_args = [self._eval(a, scope, ctx) for a in mod.args]

        if mod.is_filter:
            # filter( condition )
            if not isinstance(value, list):
                return value
            result = []
            for item in value:
                local = ChainMap({'$': item, 'it': item}, scope)
                if self._eval_condition(mod.condition, local, ctx):
                    result.append(item)
            return result

        if mod.is_map:
            # map( expr )
            if not isinstance(value, list):
                return value
            result = []
            for item in value:
                local = ChainMap({'$': item, 'it': item}, scope)
                mapped = self._eval(mod.args[0], local, ctx)
                result.append(mapped)
            return result

        name = mod.name

        # Date modifiers (must come before type/string to handle date.parse, to.string with fmt)
        res = apply_date_modifier(value, name, evaled_args)
        if res is not _DATE_SENTINEL:
            return res

        # Type conversion
        res = apply_type_modifier(value, name, evaled_args)
        if res is not _TYPE_SENTINEL:
            return res

        # String modifiers
        res = apply_string_modifier(value, name, evaled_args)
        if res is not _STR_SENTINEL:
            return res

        # Array modifiers
        res = apply_array_modifier(value, name, evaled_args,
                                    eval_fn=lambda n, s=scope, c=ctx: self._eval(n, s, c))
        if res is not _ARR_SENTINEL:
            return res

        # Math modifiers
        res = apply_math_modifier(value, name, evaled_args)
        if res is not _MATH_SENTINEL:
            return res

        # JSON modifiers
        res = apply_json_modifier(value, name, evaled_args)
        if res is not _JSON_SENTINEL:
            return res

        # Unknown modifier — pass through
        return value


# ── Helpers ────────────────────────────────────────────────────────────────────

def _deep_set(d: dict, path: list[str], value: Any):
    """Set d[path[0]][path[1]]... = value, creating intermediate dicts."""
    if not path:
        return
    if len(path) == 1:
        d[path[0]] = value
        return
    key = path[0]
    if key not in d or not isinstance(d[key], dict):
        d[key] = {}
    _deep_set(d[key], path[1:], value)


def _to_str(val: Any) -> str:
    if val is None:
        return ''
    if isinstance(val, bool):
        return 'true' if val else 'false'
    if isinstance(val, ISLDatetime):
        return str(val)
    return str(val)


def _compare(left: Any, op: str, right: Any) -> bool:
    try:
        if op == '==':
            # numeric comparison if both can be numbers
            try:
                return float(left) == float(right)
            except (TypeError, ValueError):
                return str(left) == str(right)
        if op == '!=':
            try:
                return float(left) != float(right)
            except (TypeError, ValueError):
                return str(left) != str(right)
        if op == '<':
            return _num(left) < _num(right)
        if op == '<=':
            return _num(left) <= _num(right)
        if op == '>':
            return _num(left) > _num(right)
        if op == '>=':
            return _num(left) >= _num(right)
        if op == 'contains':
            return str(right) in str(left) if left is not None else False
        if op == '!contains':
            return str(right) not in str(left) if left is not None else True
        if op == 'startsWith':
            return str(left).startswith(str(right)) if left is not None else False
        if op == '!startsWith':
            return not str(left).startswith(str(right)) if left is not None else True
        if op == 'endsWith':
            return str(left).endswith(str(right)) if left is not None else False
        if op == '!endsWith':
            return not str(left).endswith(str(right)) if left is not None else True
        if op == 'in':
            if isinstance(right, list):
                return left in right
            return str(left) in str(right)
        if op == '!in':
            if isinstance(right, list):
                return left not in right
            return str(left) not in str(right)
        if op == 'is':
            return left == right
        if op == '!is':
            return left != right
        if op == 'matches':
            pattern = right.get('pattern', '') if isinstance(right, dict) else str(right)
            return bool(re.search(pattern, str(left))) if left is not None else False
        if op == '!matches':
            pattern = right.get('pattern', '') if isinstance(right, dict) else str(right)
            return not bool(re.search(pattern, str(left))) if left is not None else True
    except Exception:
        pass
    return False


def _num(v: Any) -> float:
    if v is None:
        return 0.0
    try:
        return float(v)
    except (TypeError, ValueError):
        return 0.0
