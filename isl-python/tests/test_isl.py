"""ISL Python port — integration tests."""
from __future__ import annotations
import json
import os
import sys
from pathlib import Path

import pytest

# Add parent directory to path so we can import isl
sys.path.insert(0, str(Path(__file__).parent.parent))

from isl import compile_isl, ExecutionContext

REPO_ROOT = Path(__file__).parent.parent.parent  # isl-python/.. = repo root


# ──────────────────────────────────────────────────────────────────────────────
# Test 1: hello.isl
# ──────────────────────────────────────────────────────────────────────────────

def test_hello_isl():
    """Run hello.isl with {name: 'World'} and assert message contains 'Hello, World!'"""
    hello_path = REPO_ROOT / "isl-cmd" / "examples" / "hello.isl"
    source = hello_path.read_text(encoding="utf-8")

    transformer = compile_isl("hello", source)
    ctx = ExecutionContext()
    ctx.set_variable("$input", {"name": "World"})

    result = transformer.run("run", ctx)

    assert result is not None, "Result should not be None"
    # result should be a dict with 'result' key containing 'message'
    if isinstance(result, dict):
        if "result" in result:
            inner = result["result"]
            if isinstance(inner, dict):
                msg = inner.get("message", "")
            else:
                msg = str(inner)
        elif "message" in result:
            msg = result.get("message", "")
        else:
            msg = str(result)
    else:
        msg = str(result)

    assert "Hello" in msg and "World" in msg, (
        f"Expected 'Hello, World!' in message, got: {msg!r}"
    )


# ──────────────────────────────────────────────────────────────────────────────
# Test 2: simple-transform.isl
# ──────────────────────────────────────────────────────────────────────────────

def test_simple_transform():
    """Run simple-transform.isl with simple-order.json and check key fields."""
    isl_path = REPO_ROOT / "isl-transform" / "src" / "jmh" / "resources" / "simple-transform.isl"
    json_path = REPO_ROOT / "isl-transform" / "src" / "jmh" / "resources" / "simple-order.json"

    source = isl_path.read_text(encoding="utf-8")
    input_data = json.loads(json_path.read_text(encoding="utf-8"))

    transformer = compile_isl("simple-transform", source)
    ctx = ExecutionContext()
    ctx.set_variable("$input", input_data)

    result = transformer.run("run", ctx)

    assert isinstance(result, dict), f"Expected dict result, got: {type(result)}"

    assert "orderId" in result, f"Missing 'orderId' in result: {result}"
    assert "orderName" in result, f"Missing 'orderName' in result: {result}"
    assert "amount" in result, f"Missing 'amount' in result: {result}"
    assert "customerName" in result, f"Missing 'customerName' in result: {result}"

    assert result["orderId"] == 12345, f"Expected orderId=12345, got: {result['orderId']}"
    assert result["orderName"] == "Order #1001", f"Unexpected orderName: {result['orderName']}"
    assert result["amount"] == 99.99, f"Unexpected amount: {result['amount']}"
    assert result["customerName"] == "John", f"Unexpected customerName: {result['customerName']}"


# ──────────────────────────────────────────────────────────────────────────────
# Test 3: shopify-transform-complex.isl
# ──────────────────────────────────────────────────────────────────────────────

def test_shopify_complex():
    """Run shopify-transform-complex.isl with shopify-order.json."""
    isl_path = REPO_ROOT / "isl-transform" / "src" / "jmh" / "resources" / "shopify-transform-complex.isl"
    json_path = REPO_ROOT / "isl-transform" / "src" / "jmh" / "resources" / "shopify-order.json"

    source = isl_path.read_text(encoding="utf-8")
    input_data = json.loads(json_path.read_text(encoding="utf-8"))

    transformer = compile_isl("shopify-complex", source)
    ctx = ExecutionContext()
    ctx.set_variable("$input", input_data)

    result = transformer.run("run", ctx)

    assert isinstance(result, dict), f"Expected dict result, got: {type(result)}"

    # orderId must be present and be the order id as string
    assert "orderId" in result, f"Missing 'orderId' in result keys: {list(result.keys())}"
    assert str(result["orderId"]) == "4479203598557", (
        f"Expected orderId '4479203598557', got: {result['orderId']!r}"
    )

    # customer must be a dict with fullName
    assert "customer" in result, f"Missing 'customer' in result"
    customer = result["customer"]
    assert isinstance(customer, dict), f"customer should be dict, got: {type(customer)}"
    assert "fullName" in customer, f"Missing 'fullName' in customer: {customer}"
    assert "John" in customer["fullName"], f"Expected 'John' in fullName: {customer['fullName']}"

    # items must be a list with 3 items
    assert "items" in result, f"Missing 'items' in result"
    items = result["items"]
    assert isinstance(items, list), f"items should be a list, got: {type(items)}"
    assert len(items) == 3, f"Expected 3 items, got: {len(items)}"

    # finalTotal must be present (total - discounts = 409.94 - 0 = 409.94)
    assert "finalTotal" in result, f"Missing 'finalTotal' in result"
    final_total = result["finalTotal"]
    assert final_total is not None, "finalTotal should not be None"
    try:
        ft = float(final_total)
        assert abs(ft - 409.94) < 0.01, f"Expected finalTotal ~409.94, got: {ft}"
    except (TypeError, ValueError):
        pytest.fail(f"finalTotal should be numeric, got: {final_total!r}")


# ──────────────────────────────────────────────────────────────────────────────
# Additional unit tests
# ──────────────────────────────────────────────────────────────────────────────

def test_simple_assignment():
    """Test basic variable assignment and property setting."""
    source = """
fun run($input) {
    name: $input.name | upperCase;
    count: $input.count | to.number;
}
"""
    transformer = compile_isl("test", source)
    ctx = ExecutionContext()
    ctx.set_variable("$input", {"name": "hello", "count": "42"})
    result = transformer.run("run", ctx)
    assert result["name"] == "HELLO"
    assert result["count"] == 42


def test_interpolation():
    """Test backtick string interpolation."""
    source = """
fun run($input) {
    greeting: `Hello, ${$input.name}!`;
}
"""
    transformer = compile_isl("test", source)
    ctx = ExecutionContext()
    ctx.set_variable("$input", {"name": "World"})
    result = transformer.run("run", ctx)
    assert result["greeting"] == "Hello, World!"


def test_math_block():
    """Test {{ math }} blocks."""
    source = """
fun run($input) {
    total: {{ $input.price * $input.qty }};
}
"""
    transformer = compile_isl("test", source)
    ctx = ExecutionContext()
    ctx.set_variable("$input", {"price": 10.0, "qty": 3})
    result = transformer.run("run", ctx)
    assert abs(float(result["total"]) - 30.0) < 0.001


def test_inline_if():
    """Test inline if/else expressions."""
    source = """
fun run($input) {
    status: if( $input.active == true ) "active" else "inactive";
}
"""
    transformer = compile_isl("test", source)
    ctx = ExecutionContext()
    ctx.set_variable("$input", {"active": True})
    result = transformer.run("run", ctx)
    assert result["status"] == "active"

    ctx2 = ExecutionContext()
    ctx2.set_variable("$input", {"active": False})
    result2 = transformer.run("run", ctx2)
    assert result2["status"] == "inactive"


def test_map_modifier():
    """Test | map( ... ) modifier."""
    source = """
fun run($input) {
    names: $input.items | map( $.name | upperCase );
}
"""
    transformer = compile_isl("test", source)
    ctx = ExecutionContext()
    ctx.set_variable("$input", {"items": [{"name": "alice"}, {"name": "bob"}]})
    result = transformer.run("run", ctx)
    assert result["names"] == ["ALICE", "BOB"]


def test_filter_modifier():
    """Test | filter( condition ) modifier."""
    source = """
fun run($input) {
    big: $input.nums | filter( $ >= 10 );
}
"""
    transformer = compile_isl("test", source)
    ctx = ExecutionContext()
    ctx.set_variable("$input", {"nums": [5, 10, 15, 3, 20]})
    result = transformer.run("run", ctx)
    assert result["big"] == [10, 15, 20]


def test_flat_statements():
    """Test flat (no function) ISL file."""
    source = """
orderId: $input.id;
orderName: $input.name;
"""
    transformer = compile_isl("test", source)
    ctx = ExecutionContext()
    ctx.set_variable("$input", {"id": 99, "name": "Test Order"})
    result = transformer.run("run", ctx)
    assert result["orderId"] == 99
    assert result["orderName"] == "Test Order"


def test_function_call_this():
    """Test @.This.FunctionName() calls."""
    source = """
fun double($n) {
    return {{ $n * 2 }};
}

fun run($input) {
    result: @.This.double( $input.value );
}
"""
    transformer = compile_isl("test", source)
    ctx = ExecutionContext()
    ctx.set_variable("$input", {"value": 7})
    result = transformer.run("run", ctx)
    assert float(result["result"]) == 14.0


def test_coalesce():
    """Test ?? coalesce operator."""
    source = """
fun run($input) {
    name: $input.name ?? "Unknown";
}
"""
    transformer = compile_isl("test", source)
    ctx = ExecutionContext()
    ctx.set_variable("$input", {"name": None})
    result = transformer.run("run", ctx)
    assert result["name"] == "Unknown"

    ctx2 = ExecutionContext()
    ctx2.set_variable("$input", {"name": "Alice"})
    result2 = transformer.run("run", ctx2)
    assert result2["name"] == "Alice"


def test_date_parse_format():
    """Test date.parse and to.string date formatting."""
    source = """
fun run($input) {
    formatted: $input.date | date.parse("yyyy-MM-dd'T'HH:mm:ssXXX") | to.string("yyyy-MM-dd");
}
"""
    transformer = compile_isl("test", source)
    ctx = ExecutionContext()
    ctx.set_variable("$input", {"date": "2023-10-15T14:32:45-04:00"})
    result = transformer.run("run", ctx)
    assert result["formatted"] == "2023-10-15", f"Got: {result['formatted']}"


def test_nested_object():
    """Test nested object literal construction."""
    source = """
fun run($input) {
    address: {
        street: $input.addr.street,
        city: $input.addr.city | titleCase
    };
}
"""
    transformer = compile_isl("test", source)
    ctx = ExecutionContext()
    ctx.set_variable("$input", {"addr": {"street": "123 Main St", "city": "boston"}})
    result = transformer.run("run", ctx)
    assert result["address"]["street"] == "123 Main St"
    assert result["address"]["city"] == "Boston"


def test_spread_operator():
    """Test ... spread in object literals."""
    source = """
fun run($input) {
    return {
        ...$input.base,
        extra: "value"
    };
}
"""
    transformer = compile_isl("test", source)
    ctx = ExecutionContext()
    ctx.set_variable("$input", {"base": {"a": 1, "b": 2}})
    result = transformer.run("run", ctx)
    assert result["a"] == 1
    assert result["b"] == 2
    assert result["extra"] == "value"
