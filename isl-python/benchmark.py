"""
ISL Python Port — Performance Benchmark
Mirrors the Java JMH and C# benchmark suites for apples-to-apples comparison.
Measures transform-only (pre-compiled) and full-cycle (compile + run).
"""
from __future__ import annotations

import json
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from isl import compile_isl, ExecutionContext

# ── Paths ────────────────────────────────────────────────────────────────────

REPO_ROOT = Path(__file__).parent.parent
JMH_RES = REPO_ROOT / "isl-transform" / "src" / "jmh" / "resources"

# ── Input data ───────────────────────────────────────────────────────────────

shopify_input = json.loads((JMH_RES / "shopify-order.json").read_text(encoding="utf-8"))
simple_input  = json.loads((JMH_RES / "simple-order.json").read_text(encoding="utf-8"))

shopify_simple_isl  = (JMH_RES / "shopify-transform-simple.isl").read_text(encoding="utf-8")
shopify_complex_isl = (JMH_RES / "shopify-transform-complex.isl").read_text(encoding="utf-8")
simple_transform_isl = (JMH_RES / "simple-transform.isl").read_text(encoding="utf-8")

PRODUCT_ISL = """
fun run( $input ){
   return {
        id: $input.id,
        name: $input.title | trim,
        short_description: `${ $input.title } by ${ $input.vendor }`,
        is_active: if ( $input.status == 'active' ) true else false,
        option_name: $input.options.name,
        total: {{ $input.amount * 3 / $input.amount }},
    };
}
"""

product_input = {
    "title": "IPod Nano - 8GB",
    "body_html": "It's the small iPod with a big idea: Video.",
    "id": 632910392,
    "amount": 1235.678,
    "images": [{"id": 850703190, "src": "http://example.com/burton.jpg"}],
    "options": {"name": "Color", "values": ["Pink", "Red", "Green", "Black"]},
    "status": "active",
    "tags": "Emotive, Flash Memory, MP3, Music",
    "updated_at": 1645004735,
    "vendor": "Apple"
}

# ── Benchmark definitions ────────────────────────────────────────────────────

BENCHMARKS = [
    {
        "name": "islSimpleTransformation",
        "desc": "Product transform (trim, interpolation, inline-if, math)",
        "script": PRODUCT_ISL,
        "input": product_input,
        "fn": "run",
        "iters": 2000,
    },
    {
        "name": "islSimpleTransform (flat)",
        "desc": "Flat field mapping, no modifiers (simple-transform)",
        "script": simple_transform_isl,
        "input": simple_input,
        "fn": None,
        "iters": 2000,
    },
    {
        "name": "shopifySimple",
        "desc": "Shopify → internal (field mapping + array map)",
        "script": shopify_simple_isl,
        "input": shopify_input,
        "fn": "run",
        "iters": 1000,
    },
    {
        "name": "shopifyComplex",
        "desc": "Shopify → internal (functions, modifiers, date, math)",
        "script": shopify_complex_isl,
        "input": shopify_input,
        "fn": "run",
        "iters": 500,
    },
]

# ── Timing helpers ────────────────────────────────────────────────────────────

def make_ctx(input_data: dict) -> ExecutionContext:
    ctx = ExecutionContext()
    ctx.set_variable("$input", input_data)
    return ctx


def run_transform(transformer, fn, input_data, iters: int, warmup: int = 100):
    """Pre-compiled: warmup then time."""
    for _ in range(warmup):
        transformer.run(fn, make_ctx(input_data))

    t0 = time.perf_counter()
    for _ in range(iters):
        transformer.run(fn, make_ctx(input_data))
    elapsed = time.perf_counter() - t0

    ms_per_op = elapsed * 1000 / iters
    ops_per_sec = iters / elapsed
    return ms_per_op, ops_per_sec


def run_full_cycle(name, script, fn, input_data, iters: int):
    """Full-cycle: compile + run each iteration."""
    t0 = time.perf_counter()
    for _ in range(iters):
        t = compile_isl(name, script)
        t.run(fn, make_ctx(input_data))
    elapsed = time.perf_counter() - t0

    ms_per_op = elapsed * 1000 / iters
    ops_per_sec = iters / elapsed
    return ms_per_op, ops_per_sec


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    import platform
    print("=" * 72)
    print("ISL Python Port — Performance Benchmark")
    print(f"Python {sys.version.split()[0]}  |  {platform.platform()}")
    print("=" * 72)
    print()

    results = []

    for b in BENCHMARKS:
        name   = b["name"]
        script = b["script"]
        fn     = b["fn"]
        inp    = b["input"]
        iters  = b["iters"]
        fc_iters = max(iters // 5, 50)

        print(f"Compiling: {name} ...", end="", flush=True)
        try:
            transformer = compile_isl(name, script)
            print(" OK")
        except Exception as ex:
            print(f" ERROR: {ex}")
            continue

        # Smoke test
        try:
            result = transformer.run(fn, make_ctx(inp))
            if result is None:
                print(f"  WARNING: {name} returned None")
        except Exception as ex:
            print(f"  RUNTIME ERROR: {ex}")
            continue

        print(f"  transform ({iters} iters) ...", end="", flush=True)
        try:
            ms_op, ops_s = run_transform(transformer, fn, inp, iters)
            print(f" {ms_op:.4f} ms/op  ({ops_s:,.0f} ops/sec)")
        except Exception as ex:
            print(f" ERROR: {ex}")
            ms_op, ops_s = float("nan"), 0.0

        print(f"  full-cycle ({fc_iters} iters) ...", end="", flush=True)
        try:
            fc_ms, fc_ops = run_full_cycle(name, script, fn, inp, fc_iters)
            print(f" {fc_ms:.4f} ms/op  ({fc_ops:,.0f} ops/sec)")
        except Exception as ex:
            print(f" ERROR: {ex}")
            fc_ms, fc_ops = float("nan"), 0.0

        results.append((name, ms_op, ops_s, fc_ms, fc_ops))

    # ── Summary table ──────────────────────────────────────────────────────────
    print()
    print("=" * 72)
    print(f"{'Benchmark':<30} {'ms/op':>10} {'ops/sec':>12} {'fc ms/op':>10} {'fc ops/s':>12}")
    print("-" * 72)
    for name, ms, ops, fc_ms, fc_ops in results:
        print(f"{name:<30} {ms:>10.4f} {ops:>12,.0f} {fc_ms:>10.3f} {fc_ops:>12,.0f}")
    print("=" * 72)
    print()
    print("Notes:")
    print("  ms/op    = transform-only (pre-compiled; context setup + run)")
    print("  fc ms/op = full-cycle (compile ISL + context setup + run)")


if __name__ == "__main__":
    main()
