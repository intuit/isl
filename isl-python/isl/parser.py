"""Hand-rolled recursive-descent parser for ISL."""
from __future__ import annotations
import re
from typing import Any, Optional
from .ast_nodes import (
    Module, FunctionDecl, AssignProperty, AssignVariable, ReturnStatement,
    IfStatement, InlineIf, SwitchStatement, SwitchCase,
    ForEach, WhileLoop, VariableSelector, SelectorPart,
    ModifierChain, Modifier, FunctionCall, Coalesce, Spread,
    Literal, ArrayLiteral, ObjectLiteral, MathExpr, MathBlock,
    Interpolation, InterpolationText, InterpolationVar,
)

# ──────────────────────────────────────────────────────────────────────────────
# Tokenizer
# ──────────────────────────────────────────────────────────────────────────────

TK_ID       = 'ID'
TK_NUM      = 'NUM'
TK_BOOL     = 'BOOL'
TK_STRING   = 'STRING'
TK_BACKTICK = 'BACKTICK'
TK_OP       = 'OP'
TK_EOF      = 'EOF'

KEYWORDS = {
    'if', 'else', 'endif', 'switch', 'endswitch',
    'foreach', 'endfor', 'parallel',
    'while', 'endwhile',
    'fun', 'modifier', 'return', 'cache',
    'import', 'type', 'as', 'from',
    'in', 'and', 'or', 'filter', 'map',
    'contains', 'startsWith', 'endsWith', 'matches',
    'is',
    'true', 'false', 'null',
}

MULTI_CHAR_OPS = [
    '...', '??', '==', '!=', '<=', '>=', '->', '${', '}}',
    '!contains', '!startsWith', '!endsWith', '!in', '!is', '!matches',
    '[(', ')]', '{{',
]

SINGLE_CHAR_OPS = set('!:=;|,.$@()[]{}><+-*/\\')


class Token:
    __slots__ = ('kind', 'value', 'line')

    def __init__(self, kind: str, value: str, line: int):
        self.kind = kind
        self.value = value
        self.line = line

    def __repr__(self):
        return f'Token({self.kind}, {self.value!r}, line={self.line})'


def tokenize(src: str) -> list[Token]:
    tokens: list[Token] = []
    i = 0
    n = len(src)
    line = 1

    while i < n:
        c = src[i]

        # Skip whitespace
        if c in ' \t\r\n':
            if c == '\n':
                line += 1
            i += 1
            continue

        # Line comments
        if c == '#' or (c == '/' and i + 1 < n and src[i+1] == '/'):
            while i < n and src[i] != '\n':
                i += 1
            continue

        # Quoted strings
        if c in ('"', "'"):
            quote = c
            j = i + 1
            while j < n and src[j] != quote:
                if src[j] == '\\':
                    j += 1
                j += 1
            tok_val = src[i:j+1]
            tokens.append(Token(TK_STRING, tok_val, line))
            i = j + 1
            continue

        # Backtick strings — capture raw content, parse interpolation later
        if c == '`':
            j = i + 1
            depth = 0
            while j < n:
                ch = src[j]
                if ch == '\\':
                    j += 2
                    continue
                if ch == '`' and depth == 0:
                    break
                if ch in ('{', '('):
                    depth += 1
                elif ch in ('}', ')'):
                    if depth > 0:
                        depth -= 1
                j += 1
            raw = src[i+1:j]  # content between backticks
            tokens.append(Token(TK_BACKTICK, raw, line))
            i = j + 1
            continue

        # Identifiers and keywords
        if c.isalpha() or c == '_':
            m = re.match(r'[a-zA-Z_][a-zA-Z0-9_]*', src[i:])
            word = m.group(0)
            kind = TK_BOOL if word in ('true', 'false', 'null') else TK_ID
            tokens.append(Token(kind, word, line))
            i += len(word)
            continue

        # Numbers (only positive; minus is an operator)
        if c.isdigit():
            m = re.match(r'[0-9]+(\.[0-9]+)?', src[i:])
            tok_val = m.group(0)
            tokens.append(Token(TK_NUM, tok_val, line))
            i += len(tok_val)
            continue

        # Multi-char operators (try longest first)
        matched_op = None
        for op in sorted(MULTI_CHAR_OPS, key=len, reverse=True):
            if src[i:i+len(op)] == op:
                matched_op = op
                break
        if matched_op:
            tokens.append(Token(TK_OP, matched_op, line))
            i += len(matched_op)
            continue

        # Single char operators
        if c in SINGLE_CHAR_OPS:
            tokens.append(Token(TK_OP, c, line))
            i += 1
            continue

        # Skip unknown
        i += 1

    tokens.append(Token(TK_EOF, '', line))
    return tokens


# ──────────────────────────────────────────────────────────────────────────────
# Parser
# ──────────────────────────────────────────────────────────────────────────────

RELOPS = {'==', '!=', '<=', '>=', '<', '>', 'contains', '!contains',
          'startsWith', '!startsWith', 'endsWith', '!endsWith',
          'in', '!in', 'is', '!is'}


class ParseError(Exception):
    pass


class Parser:
    def __init__(self, tokens: list[Token]):
        self.tokens = tokens
        self.pos = 0

    def peek(self, offset=0) -> Token:
        idx = self.pos + offset
        if idx >= len(self.tokens):
            return self.tokens[-1]
        return self.tokens[idx]

    def consume(self) -> Token:
        t = self.tokens[self.pos]
        self.pos += 1
        return t

    def expect(self, kind: str, value: Optional[str] = None) -> Token:
        t = self.peek()
        if t.kind != kind:
            raise ParseError(f'Line {t.line}: expected {kind!r} but got {t.kind!r} ({t.value!r})')
        if value is not None and t.value != value:
            raise ParseError(f'Line {t.line}: expected {value!r} but got {t.value!r}')
        return self.consume()

    def match(self, kind: str, value: Optional[str] = None) -> bool:
        t = self.peek()
        if t.kind != kind:
            return False
        if value is not None and t.value != value:
            return False
        return True

    def eat(self, kind: str, value: Optional[str] = None) -> Optional[Token]:
        if self.match(kind, value):
            return self.consume()
        return None

    def at_eof(self) -> bool:
        return self.peek().kind == TK_EOF


def parse(src: str) -> Module:
    tokens = tokenize(src)
    p = Parser(tokens)
    return _parse_spec(p)


def _parse_spec(p: Parser) -> Module:
    """spec: (import* type* function+) | statements"""
    # Peek ahead to see if we have function declarations
    # We detect this by looking for 'fun', 'modifier', 'cache' at top level,
    # or @annotation before fun.
    has_functions = _has_function_decls(p)

    if has_functions:
        # skip imports and type declarations
        while p.match(TK_ID, 'import') or p.match(TK_ID, 'type'):
            if p.match(TK_ID, 'import'):
                _parse_import(p)
            else:
                _parse_type_decl(p)
        functions = []
        while not p.at_eof():
            fn = _parse_function_decl(p)
            functions.append(fn)
        return Module(functions=functions)
    else:
        stmts = _parse_statements(p, stop_at=set())
        return Module(statements=stmts)


def _has_function_decls(p: Parser) -> bool:
    """Look ahead to see if the source contains function declarations."""
    for t in p.tokens:
        if t.kind == TK_ID and t.value in ('fun', 'modifier', 'cache'):
            return True
        if t.kind == TK_OP and t.value == '@':
            # could be annotation before fun
            continue
    return False


def _parse_import(p: Parser):
    p.expect(TK_ID, 'import')
    p.expect(TK_ID)  # name
    p.expect(TK_ID, 'from')
    p.expect(TK_STRING)
    p.eat(TK_OP, ';')


def _parse_type_decl(p: Parser):
    p.expect(TK_ID, 'type')
    p.expect(TK_ID)  # name
    if p.match(TK_ID, 'as'):
        p.consume()
        _parse_type_definition(p)
    else:
        p.expect(TK_ID, 'from')
        p.expect(TK_STRING)
    p.eat(TK_OP, ';')


def _parse_type_definition(p: Parser):
    """Skip type definitions — we don't enforce types at runtime."""
    if p.match(TK_OP, '{'):
        depth = 0
        while not p.at_eof():
            t = p.consume()
            if t.value == '{':
                depth += 1
            elif t.value == '}':
                depth -= 1
                if depth == 0:
                    break
    elif p.match(TK_OP, '['):
        p.consume()
        p.eat(TK_OP, ']')
    else:
        # type name
        p.expect(TK_ID)
        while p.match(TK_OP, '.'):
            p.consume()
            p.expect(TK_ID)
        if p.match(TK_OP, '['):
            p.consume()
            p.eat(TK_OP, ']')


def _parse_function_decl(p: Parser) -> FunctionDecl:
    # Skip annotations
    while p.match(TK_OP, '@'):
        _skip_annotation(p)

    is_modifier = False
    cached = False

    if p.match(TK_ID, 'cache'):
        p.consume()
        cached = True
    if p.match(TK_ID, 'modifier'):
        p.consume()
        is_modifier = True

    p.expect(TK_ID, 'fun')
    name = p.expect(TK_ID).value

    # function args
    p.expect(TK_OP, '(')
    params = []
    while not p.match(TK_OP, ')'):
        p.expect(TK_OP, '$')
        param_name = _parse_short_id(p)
        params.append(param_name)
        # optional type annotation
        if p.match(TK_OP, ':'):
            p.consume()
            _parse_type_definition(p)
        if not p.eat(TK_OP, ','):
            break
    p.expect(TK_OP, ')')

    # optional return type
    if p.match(TK_OP, ':'):
        p.consume()
        _parse_type_definition(p)

    p.expect(TK_OP, '{')
    body = _parse_function_statements(p)
    p.expect(TK_OP, '}')

    return FunctionDecl(name=name, params=params, body=body,
                        is_modifier=is_modifier, cached=cached)


def _skip_annotation(p: Parser):
    p.expect(TK_OP, '@')
    p.expect(TK_ID)
    if p.match(TK_OP, '('):
        depth = 1
        p.consume()
        while not p.at_eof() and depth > 0:
            t = p.consume()
            if t.value == '(':
                depth += 1
            elif t.value == ')':
                depth -= 1


def _parse_function_statements(p: Parser) -> list:
    stmts = []
    while not p.match(TK_OP, '}') and not p.at_eof():
        stmt = _parse_one_statement(p, inside_function=True)
        if stmt is not None:
            stmts.append(stmt)
    return stmts


def _parse_statements(p: Parser, stop_at: set) -> list:
    """Parse a list of statements, stopping when we see a keyword in stop_at."""
    stmts = []
    while not p.at_eof():
        t = p.peek()
        # Stop conditions
        if t.kind == TK_OP and t.value == '}':
            break
        if t.kind == TK_ID and t.value in stop_at:
            break
        stmt = _parse_one_statement(p, inside_function=False)
        if stmt is not None:
            stmts.append(stmt)
    return stmts


def _parse_one_statement(p: Parser, inside_function: bool) -> Any:
    t = p.peek()

    # return
    if t.kind == TK_ID and t.value == 'return':
        p.consume()
        val = _parse_assignment_value(p)
        p.eat(TK_OP, ';')
        return ReturnStatement(val)

    # if statement (block form)
    if t.kind == TK_ID and t.value == 'if':
        return _parse_if_statement(p)

    # switch
    if t.kind == TK_ID and t.value == 'switch':
        return _parse_switch(p)

    # foreach
    if t.kind == TK_ID and t.value in ('foreach', 'parallel'):
        return _parse_foreach(p)

    # while
    if t.kind == TK_ID and t.value == 'while':
        return _parse_while(p)

    # function call (@.Service.Method(...))
    if t.kind == TK_OP and t.value == '@':
        fc = _parse_function_call(p)
        p.eat(TK_OP, ';')
        return fc

    # variable assignment: $var = / $var: ...
    if t.kind == TK_OP and t.value == '$':
        return _parse_assign_var(p)

    # property assignment: name: value or name.sub: value
    if t.kind == TK_ID or t.kind == TK_BACKTICK:
        return _parse_assign_prop(p)

    # skip unknown tokens
    p.consume()
    return None


def _parse_if_statement(p: Parser) -> IfStatement:
    p.expect(TK_ID, 'if')
    cond = _parse_condition(p)
    true_body = _parse_statements(p, stop_at={'else', 'endif'})
    false_body = []
    if p.eat(TK_ID, 'else'):
        false_body = _parse_statements(p, stop_at={'endif'})
    p.eat(TK_ID, 'endif')
    return IfStatement(condition=cond, true_body=true_body, false_body=false_body)


def _parse_inline_if(p: Parser) -> InlineIf:
    """inline if: if ( cond ) value [else value] [endif]"""
    p.expect(TK_ID, 'if')
    cond = _parse_condition(p)
    true_val = _parse_assignment_value_item(p)
    false_val = None
    if p.eat(TK_ID, 'else'):
        false_val = _parse_assignment_value_item(p)
    p.eat(TK_ID, 'endif')
    return InlineIf(condition=cond, true_val=true_val, false_val=false_val)


def _parse_condition(p: Parser) -> Any:
    p.expect(TK_OP, '(')
    expr = _parse_condition_expr(p)
    p.expect(TK_OP, ')')
    return expr


def _parse_condition_expr(p: Parser) -> Any:
    left = _parse_simple_condition(p)
    while p.match(TK_ID, 'and') or p.match(TK_ID, 'or'):
        op = p.consume().value
        right = _parse_simple_condition(p)
        left = {'op': op, 'left': left, 'right': right}
    return left


def _parse_simple_condition(p: Parser) -> Any:
    # Check for negation
    negated = False
    if p.match(TK_OP, '!'):
        # Could be negation or !contains etc. — check next token
        nxt = p.peek(1)
        if nxt.kind == TK_OP and nxt.value == '(':
            p.consume()  # eat !
            negated = True
        # Otherwise the ! is part of a relop token like '!='

    if p.match(TK_OP, '('):
        p.consume()
        inner = _parse_condition_expr(p)
        p.expect(TK_OP, ')')
        if negated:
            return {'op': 'not', 'expr': inner}
        return inner

    # Could be: !expr  or  left relop right  or  just left
    if negated:
        left = _parse_rhs_val(p)
        return {'op': 'not', 'expr': left}

    left = _parse_rhs_val(p)

    # Check for relop
    t = p.peek()
    relop_val = None
    if t.kind == TK_OP and t.value in RELOPS:
        relop_val = p.consume().value
    elif t.kind == TK_ID and t.value in RELOPS:
        relop_val = p.consume().value
    elif t.kind == TK_OP and t.value == '!':
        # could be !contains, !in etc. — already tokenized as multi-char?
        # If not, try
        nxt = p.peek(1)
        combined = '!' + nxt.value
        if combined in RELOPS:
            p.consume()
            p.consume()
            relop_val = combined

    if relop_val:
        right = _parse_rhs_val(p)
        return {'op': relop_val, 'left': left, 'right': right}

    # regex relop
    if t.kind == TK_ID and t.value in ('matches', '!matches'):
        relop_val = p.consume().value
        regex = _parse_regex(p)
        return {'op': relop_val, 'left': left, 'right': regex}

    return {'op': 'truthy', 'expr': left}


def _parse_rhs_val(p: Parser) -> Any:
    """right-hand-side value (may have modifiers)."""
    val = _parse_right_side_value(p)
    mods = _parse_modifiers(p)
    if mods:
        return ModifierChain(value=val, modifiers=mods)
    return val


def _parse_switch(p: Parser) -> SwitchStatement:
    p.expect(TK_ID, 'switch')
    p.expect(TK_OP, '(')
    subject = _parse_rhs_val(p)
    p.expect(TK_OP, ')')

    cases = []
    else_case = None
    while not p.match(TK_ID, 'endswitch') and not p.at_eof():
        if p.match(TK_ID, 'else'):
            p.consume()
            p.expect(TK_OP, '->')
            body = _parse_switch_body(p)
            p.eat(TK_OP, ';')
            else_case = body
        else:
            # optional relop
            op = None
            t = p.peek()
            if ((t.kind == TK_OP or t.kind == TK_ID) and
                    t.value in RELOPS):
                op = p.consume().value
            # match value
            mv = _parse_switch_match_val(p)
            p.expect(TK_OP, '->')
            body = _parse_switch_body(p)
            p.eat(TK_OP, ';')
            cases.append(SwitchCase(op=op, match_val=mv, body=body))
    p.eat(TK_ID, 'endswitch')
    return SwitchStatement(subject=subject, cases=cases, else_case=else_case)


def _parse_switch_match_val(p: Parser) -> Any:
    if p.match(TK_OP, '['):
        return _parse_array(p)
    return _parse_right_side_value(p)


def _parse_switch_body(p: Parser) -> Any:
    if p.match(TK_OP, '@'):
        return _parse_function_call(p)
    if p.match(TK_OP, '{'):
        return _parse_declare_object(p)
    # Could be statements or just a value
    val = _parse_rhs_val(p)
    return val


def _parse_foreach(p: Parser) -> ForEach:
    parallel = False
    if p.match(TK_ID, 'parallel'):
        parallel = True
        p.consume()
        # optional options object
        if p.match(TK_OP, '{') or p.match(TK_OP, '$'):
            pass  # skip options for now

    p.expect(TK_ID, 'foreach')
    p.expect(TK_OP, '$')
    iter_var = _parse_short_id(p)
    p.expect(TK_ID, 'in')
    iterable = _parse_rhs_val(p)

    body = []
    body_obj = None

    # Determine if body is statements or an inline object/var
    if p.match(TK_OP, '{'):
        # This is either a trailing object literal (the shape of each result)
        # or statements. Peek inside: if it looks like property: value pattern,
        # it's a declare_object. We'll parse it as declare_object (shape definition).
        body_obj = _parse_declare_object(p)
    elif not p.match(TK_ID, 'endfor') and not p.at_eof():
        # Parse statements until we hit endfor or a trailing {
        body = _parse_statements(p, stop_at={'endfor'})
        # After statements, optionally a trailing declare_object
        if p.match(TK_OP, '{'):
            body_obj = _parse_declare_object(p)
        elif p.match(TK_OP, '$'):
            body_obj = _parse_var_selector(p)

    p.eat(TK_ID, 'endfor')
    return ForEach(iterator_var=iter_var, iterable=iterable,
                   body=body, body_object=body_obj, parallel=parallel)


def _parse_while(p: Parser) -> WhileLoop:
    p.expect(TK_ID, 'while')
    p.expect(TK_OP, '(')
    cond = _parse_condition_expr(p)
    max_loops = 50
    if p.match(TK_OP, ','):
        p.consume()
        # options object — extract maxLoops if present
        # simple: parse as declare_object and extract
        obj = _parse_declare_object(p)
        # We'll just use default for now
    p.expect(TK_OP, ')')
    body = _parse_statements(p, stop_at={'endwhile'})
    p.eat(TK_ID, 'endwhile')
    return WhileLoop(condition=cond, body=body, max_loops=max_loops)


def _parse_function_call(p: Parser) -> FunctionCall:
    p.expect(TK_OP, '@')
    p.expect(TK_OP, '.')
    service = _parse_short_id(p)
    name = None
    if p.match(TK_OP, '.'):
        p.consume()
        name = _parse_multi_ident(p)
    args = _parse_arguments(p)
    body = []
    if p.match(TK_OP, '{'):
        p.consume()
        body = _parse_function_statements(p)
        p.expect(TK_OP, '}')
    return FunctionCall(service=service, name=name, args=args, body=body)


def _parse_arguments(p: Parser) -> list:
    p.expect(TK_OP, '(')
    args = []
    while not p.match(TK_OP, ')') and not p.at_eof():
        arg = _parse_arg_value(p)
        args.append(arg)
        if not p.eat(TK_OP, ','):
            break
    p.expect(TK_OP, ')')
    return args


def _parse_arg_value(p: Parser) -> Any:
    val = _parse_arg_item(p)
    while p.match(TK_OP, '??'):
        p.consume()
        right = _parse_arg_item(p)
        val = Coalesce(left=val, right=right)
    return val


def _parse_arg_item(p: Parser) -> Any:
    val = _parse_right_side_value(p)
    mods = _parse_modifiers(p)
    if mods:
        return ModifierChain(value=val, modifiers=mods)
    return val


def _parse_assign_var(p: Parser) -> Any:
    """$var = value  or  $var: value"""
    p.expect(TK_OP, '$')
    path = _parse_assign_selector(p)

    # '=' or ':'
    if p.match(TK_OP, '='):
        p.consume()
    elif p.match(TK_OP, ':'):
        p.consume()
        # optional type annotation before '='
        if p.peek().kind == TK_ID and p.peek(1).value in ('=', ':'):
            _parse_type_name_decl(p)
            p.eat(TK_OP, '=')
    else:
        raise ParseError(f'Expected = or : after variable, got {p.peek()}')

    val = _parse_assignment_value(p)
    p.eat(TK_OP, ';')

    if len(path) == 1:
        return AssignVariable(name=path[0], value=val)
    # $var.sub: value → treated as nested assignment into variable
    return AssignVariable(name=path[0], value=_build_nested(path[1:], val))


def _build_nested(path: list[str], val: Any) -> ObjectLiteral:
    if not path:
        return val
    return ObjectLiteral(entries=[AssignProperty(path=[path[0]], value=_build_nested(path[1:], val))])


def _parse_assign_prop(p: Parser) -> Any:
    """name: value  or  name.sub: value  or  `interp`: value"""
    if p.match(TK_BACKTICK):
        key_interp = _parse_interpolation(p)
        if p.match(TK_OP, ':'):
            p.consume()
        elif p.match(TK_OP, '='):
            p.consume()
        val = _parse_assignment_value(p)
        p.eat(TK_OP, ';')
        return AssignProperty(path=[], value=val, dynamic_key=key_interp)

    path = _parse_assign_selector(p)

    if p.match(TK_OP, ':'):
        p.consume()
        if p.peek().kind == TK_ID and _peek_is_type_annotation(p):
            _parse_type_name_decl(p)
            p.eat(TK_OP, '=')
    elif p.match(TK_OP, '='):
        p.consume()
    else:
        # bare identifier — just a property with same name as value? Skip.
        p.eat(TK_OP, ';')
        return AssignProperty(path=path, value=Literal(None))

    val = _parse_assignment_value(p)
    p.eat(TK_OP, ';')
    return AssignProperty(path=path, value=val)


def _peek_is_type_annotation(p: Parser) -> bool:
    """Check if current position looks like 'TypeName =' (type annotation)."""
    # Save pos, try to parse type name, then check for '='
    saved = p.pos
    try:
        _parse_type_name_decl(p)
        result = p.match(TK_OP, '=')
    except Exception:
        result = False
    p.pos = saved
    return result


def _parse_assign_selector(p: Parser) -> list[str]:
    """Returns list of path segments."""
    path = [_parse_short_id(p)]
    # optional index
    if p.match(TK_OP, '['):
        p.consume()
        p.expect(TK_NUM)
        p.expect(TK_OP, ']')
    while p.match(TK_OP, '.'):
        p.consume()
        if p.match(TK_OP, '['):
            p.consume()
            seg = p.expect(TK_STRING).value.strip('"\'')
            p.expect(TK_OP, ']')
            path.append(seg)
        else:
            path.append(_parse_short_id(p))
        if p.match(TK_OP, '['):
            p.consume()
            p.expect(TK_NUM)
            p.expect(TK_OP, ']')
    return path


def _parse_type_name_decl(p: Parser):
    _parse_short_id(p)
    while p.match(TK_OP, '.'):
        p.consume()
        _parse_short_id(p)
    if p.match(TK_OP, '['):
        p.consume()
        p.eat(TK_OP, ']')


def _parse_assignment_value(p: Parser) -> Any:
    val = _parse_assignment_value_item(p)
    while p.match(TK_OP, '??'):
        p.consume()
        right = _parse_assignment_value_item(p)
        val = Coalesce(left=val, right=right)
    return val


def _parse_assignment_value_item(p: Parser) -> Any:
    val = _parse_primary(p)
    mods = _parse_modifiers(p)
    if mods:
        return ModifierChain(value=val, modifiers=mods)
    return val


def _parse_primary(p: Parser) -> Any:
    t = p.peek()

    # Math block {{ ... }}
    if t.kind == TK_OP and t.value == '{{':
        return _parse_math_block(p)

    # Object literal
    if t.kind == TK_OP and t.value == '{':
        return _parse_declare_object(p)

    # Inline if
    if t.kind == TK_ID and t.value == 'if':
        return _parse_inline_if(p)

    # foreach inline
    if t.kind == TK_ID and t.value in ('foreach', 'parallel'):
        return _parse_foreach(p)

    # while inline
    if t.kind == TK_ID and t.value == 'while':
        return _parse_while(p)

    # Interpolated string
    if t.kind == TK_BACKTICK:
        return _parse_interpolation(p)

    # Function call
    if t.kind == TK_OP and t.value == '@':
        return _parse_function_call(p)

    # Switch inline
    if t.kind == TK_ID and t.value == 'switch':
        return _parse_switch(p)

    # Array
    if t.kind == TK_OP and t.value == '[':
        return _parse_array(p)

    # Variable selector
    if t.kind == TK_OP and t.value == '$':
        return _parse_var_selector(p)

    # Literal
    if t.kind in (TK_BOOL, TK_NUM, TK_STRING):
        return _parse_literal(p)

    # Bare identifier in certain contexts (e.g. hello.isl has 'dasdsa')
    if t.kind == TK_ID:
        # Could be a bare identifier used as a string-like property name
        p.consume()
        return Literal(t.value)

    raise ParseError(f'Line {t.line}: unexpected token {t!r} in expression')


def _parse_right_side_value(p: Parser) -> Any:
    """Like _parse_primary but used in rhs context."""
    return _parse_primary(p)


_VAR_STOP_KEYWORDS = frozenset({
    'if', 'else', 'endif', 'switch', 'endswitch',
    'foreach', 'endfor', 'while', 'endwhile',
    'fun', 'modifier', 'return', 'cache',
    'import', 'type', 'and', 'or',
    'true', 'false', 'null', 'parallel',
})


def _parse_var_selector(p: Parser) -> VariableSelector:
    p.expect(TK_OP, '$')
    parts = []

    # First part: optional identifier + optional index/condition
    t = p.peek()
    name = None
    if t.kind == TK_ID and t.value not in _VAR_STOP_KEYWORDS:
        name = p.consume().value

    sp = SelectorPart(name=name)
    sp = _try_parse_index_or_cond(p, sp)
    parts.append(sp)

    # Additional path parts
    while p.match(TK_OP, '.'):
        p.consume()
        sp = SelectorPart(name=None)
        if p.match(TK_OP, '['):
            p.consume()
            # text key
            key = p.expect(TK_STRING).value.strip('"\'')
            p.expect(TK_OP, ']')
            sp.text_key = key
        else:
            t2 = p.peek()
            if t2.kind == TK_ID and t2.value not in _VAR_STOP_KEYWORDS:
                sp.name = p.consume().value
        sp = _try_parse_index_or_cond(p, sp)
        parts.append(sp)

    return VariableSelector(parts=parts)


def _try_parse_index_or_cond(p: Parser, sp: SelectorPart) -> SelectorPart:
    if p.match(TK_OP, '['):
        nxt = p.peek(1)
        if nxt.kind == TK_OP and nxt.value == '(':
            # condition: [(condition)]
            p.consume()  # [
            p.consume()  # (
            cond = _parse_condition_expr(p)
            p.expect(TK_OP, ')')
            p.expect(TK_OP, ']')
            sp.condition = cond
        else:
            p.consume()  # [
            idx = p.expect(TK_NUM).value
            p.expect(TK_OP, ']')
            sp.index = int(float(idx))
    elif p.match(TK_OP, '[('):
        p.consume()  # [(
        cond = _parse_condition_expr(p)
        p.expect(TK_OP, ')]')
        sp.condition = cond
    return sp


def _parse_short_id(p: Parser) -> str:
    t = p.peek()
    if t.kind == TK_ID:
        return p.consume().value
    raise ParseError(f'Line {t.line}: expected identifier, got {t!r}')


def _parse_multi_ident(p: Parser) -> str:
    name = _parse_short_id(p)
    if p.match(TK_OP, '.'):
        p.consume()
        name += '.' + _parse_short_id(p)
    return name


def _parse_literal(p: Parser) -> Literal:
    t = p.peek()
    if t.kind == TK_BOOL:
        p.consume()
        if t.value == 'true':
            return Literal(True)
        elif t.value == 'false':
            return Literal(False)
        else:
            return Literal(None)
    if t.kind == TK_NUM:
        p.consume()
        v = t.value
        if '.' in v:
            return Literal(float(v))
        return Literal(int(v))
    if t.kind == TK_STRING:
        p.consume()
        return Literal(t.value[1:-1])  # strip quotes
    raise ParseError(f'Expected literal at line {t.line}')


def _parse_array(p: Parser) -> ArrayLiteral:
    p.expect(TK_OP, '[')
    elements = []
    while not p.match(TK_OP, ']') and not p.at_eof():
        if p.match(TK_OP, '...'):
            p.consume()
            src = _parse_var_selector(p) if p.match(TK_OP, '$') else _parse_function_call(p)
            elements.append(Spread(source=src))
        else:
            elements.append(_parse_arg_value(p))
        if not p.eat(TK_OP, ','):
            break
    p.expect(TK_OP, ']')
    return ArrayLiteral(elements=elements)


def _parse_declare_object(p: Parser) -> ObjectLiteral:
    p.expect(TK_OP, '{')
    entries = []
    while not p.match(TK_OP, '}') and not p.at_eof():
        entry = _parse_declare_object_stmt(p)
        if entry is not None:
            entries.append(entry)
        # eat comma/semicolon separator; if none, we need a closing brace
        p.eat(TK_OP, ',')
        p.eat(TK_OP, ';')
    p.expect(TK_OP, '}')
    return ObjectLiteral(entries=entries)


def _parse_declare_object_stmt(p: Parser) -> Any:
    t = p.peek()

    # spread
    if t.kind == TK_OP and t.value == '...':
        p.consume()
        if p.match(TK_OP, '$'):
            src = _parse_var_selector(p)
        else:
            src = _parse_function_call(p)
        return Spread(source=src)

    # quoted key
    if t.kind == TK_STRING:
        key = p.consume().value[1:-1]
        p.expect(TK_OP, ':')
        if _peek_is_type_annotation(p):
            _parse_type_name_decl(p)
            p.eat(TK_OP, '=')
        val = _parse_assignment_value(p)
        return AssignProperty(path=[key], value=val)

    # $var assignment
    if t.kind == TK_OP and t.value == '$':
        return _parse_assign_var(p)

    # interpolated key
    if t.kind == TK_BACKTICK:
        return _parse_assign_prop(p)

    # regular identifier key
    if t.kind == TK_ID:
        return _parse_assign_prop(p)

    return None


def _parse_math_block(p: Parser) -> MathBlock:
    p.expect(TK_OP, '{{')
    expr = _parse_math_expr(p)
    p.expect(TK_OP, '}}')
    return MathBlock(expr=expr)


def _parse_math_expr(p: Parser) -> Any:
    left = _parse_math_term(p)
    while p.match(TK_OP, '+') or p.match(TK_OP, '-'):
        op = p.consume().value
        right = _parse_math_term(p)
        left = MathExpr(op=op, left=left, right=right)
    return left


def _parse_math_term(p: Parser) -> Any:
    left = _parse_math_unary(p)
    while p.match(TK_OP, '*') or p.match(TK_OP, '/'):
        op = p.consume().value
        right = _parse_math_unary(p)
        left = MathExpr(op=op, left=left, right=right)
    return left


def _parse_math_unary(p: Parser) -> Any:
    if p.match(TK_OP, '('):
        p.consume()
        expr = _parse_math_expr(p)
        p.expect(TK_OP, ')')
        return expr
    t = p.peek()
    if t.kind == TK_NUM:
        return _parse_literal(p)
    if t.kind == TK_OP and t.value == '$':
        return _parse_var_selector(p)
    if t.kind == TK_OP and t.value == '@':
        return _parse_function_call(p)
    if t.kind == TK_OP and t.value == '-':
        p.consume()
        inner = _parse_math_unary(p)
        return MathExpr(op='*', left=Literal(-1), right=inner)
    # Try literal (e.g. BOOL used in math)
    if t.kind in (TK_BOOL, TK_STRING):
        return _parse_literal(p)
    # Fallback: consume and return 0
    return Literal(0)


def _parse_modifiers(p: Parser) -> list[Modifier]:
    mods = []
    while p.match(TK_OP, '|'):
        p.consume()
        mod = _parse_one_modifier(p)
        mods.append(mod)
    return mods


def _parse_one_modifier(p: Parser) -> Modifier:
    t = p.peek()

    # filter modifier: | filter ( condition )
    if t.kind == TK_ID and t.value == 'filter':
        p.consume()
        cond = _parse_condition(p)
        return Modifier(name='filter', condition=cond, is_filter=True)

    # map modifier: | map ( arg )
    if t.kind == TK_ID and t.value == 'map':
        p.consume()
        p.expect(TK_OP, '(')
        arg = _parse_arg_value(p)
        p.expect(TK_OP, ')')
        return Modifier(name='map', args=[arg], is_map=True)

    # if modifier: | if ( cond ) modname args?
    if t.kind == TK_ID and t.value == 'if':
        p.consume()
        cond = _parse_condition(p)
        mod_name = _parse_multi_ident(p)
        args = []
        if p.match(TK_OP, '('):
            args = _parse_arguments(p)
        return Modifier(name=mod_name, condition=cond, args=args, is_cond=True)

    # Generic modifier with optional args
    name = _parse_multi_ident(p)
    args = []
    if p.match(TK_OP, '('):
        args = _parse_arguments(p)
    return Modifier(name=name, args=args)


def _parse_interpolation(p: Parser) -> Interpolation:
    raw = p.expect(TK_BACKTICK).value
    parts = _parse_interp_content(raw)
    return Interpolation(parts=parts)


def _parse_interp_content(raw: str) -> list:
    """Parse the content of a backtick string into parts."""
    parts = []
    i = 0
    n = len(raw)
    buf = ''

    while i < n:
        # ${expr}
        if raw[i:i+2] == '${':
            if buf:
                parts.append(InterpolationText(text=buf))
                buf = ''
            i += 2
            depth = 1
            j = i
            while j < n and depth > 0:
                if raw[j] == '{':
                    depth += 1
                elif raw[j] == '}':
                    depth -= 1
                j += 1
            inner = raw[i:j-1]
            parts.append(_parse_interp_expr(inner))
            i = j
            continue

        # {{math}}
        if raw[i:i+2] == '{{':
            if buf:
                parts.append(InterpolationText(text=buf))
                buf = ''
            i += 2
            depth = 1
            j = i
            while j < n:
                if raw[j:j+2] == '}}':
                    break
                j += 1
            inner = raw[i:j]
            parts.append(_parse_interp_math(inner))
            i = j + 2
            continue

        # @. function call
        if raw[i:i+2] == '@.':
            if buf:
                parts.append(InterpolationText(text=buf))
                buf = ''
            i += 2
            j = i
            depth = 0
            while j < n:
                c = raw[j]
                if c == '(':
                    depth += 1
                elif c == ')':
                    if depth == 0:
                        j += 1
                        break
                    depth -= 1
                j += 1
            inner = '@.' + raw[i:j]
            parts.append(_parse_interp_func(inner))
            i = j
            continue

        # $varname
        m = re.match(r'\$([A-Za-z_][A-Za-z0-9_]*)', raw[i:])
        if m:
            if buf:
                parts.append(InterpolationText(text=buf))
                buf = ''
            parts.append(InterpolationVar(name=m.group(1)))
            i += len(m.group(0))
            continue

        buf += raw[i]
        i += 1

    if buf:
        parts.append(InterpolationText(text=buf))

    return parts


def _parse_interp_expr(inner: str) -> Any:
    """Parse the content of ${...}"""
    try:
        tokens = tokenize(inner)
        p = Parser(tokens)
        val = _parse_right_side_value(p)
        mods = _parse_modifiers(p)
        if mods:
            return ModifierChain(value=val, modifiers=mods)
        return val
    except Exception:
        return InterpolationText(text='${' + inner + '}')


def _parse_interp_math(inner: str) -> MathBlock:
    try:
        tokens = tokenize(inner)
        p = Parser(tokens)
        expr = _parse_math_expr(p)
        return MathBlock(expr=expr)
    except Exception:
        return InterpolationText(text='{{' + inner + '}}')


def _parse_interp_func(inner: str) -> Any:
    try:
        tokens = tokenize(inner)
        p = Parser(tokens)
        return _parse_function_call(p)
    except Exception:
        return InterpolationText(text=inner)


def _parse_regex(p: Parser) -> dict:
    p.expect(TK_OP, '/')
    pattern = ''
    while not p.match(TK_OP, '/') and not p.at_eof():
        t = p.consume()
        pattern += t.value
    p.expect(TK_OP, '/')
    return {'type': 'regex', 'pattern': pattern}
