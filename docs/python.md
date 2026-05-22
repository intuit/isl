## ISL Transform Library — Python Port Analysis

### What ISL Is

ISL (Intuit Scripting Language) is a **purpose-built JSON transformation DSL** — think of it as a declarative, pipeline-oriented language designed specifically to map one JSON shape to another. It sits somewhere between JOLT (declarative JSON spec) and a full scripting language (MVEL/Groovy).

A real ISL program looks like this:

```isl
fun convertCustomer( $cust ) {
    $firstName = $cust.first_name | trim | capitalize;
    $lastName  = $cust.last_name  | trim | upperCase;
    $spent     = $cust.total_spent | to.decimal | precision(2);

    return {
        id:       $cust.id | to.string,
        fullName: `${$firstName} ${$lastName}`,
        email:    $cust.email | lowerCase,
        spent:    $spent
    };
}

fun run( $input ) {
    $customer = @.This.convertCustomer( $input.customer );
    $total    = $input.total_price | to.decimal;
    $discounts = $input.total_discounts | to.decimal;

    orderId:    $input.id | to.string;
    orderNumber: $input.order_number | to.string | padStart(8, "0");
    customer:   $customer;
    finalTotal: {{ $total - $discounts }} | precision(2);
    tags:       $input.tags | split(",") | map( $ | trim | upperCase );
}
```

---

### Architecture Deep Dive

The runtime is a **3-phase compile-then-execute pipeline**:

| Phase | Code | What it does |
|---|---|---|
| **1. Lex + Parse** | `IslLexer.g4` + `IslParser.g4` → ANTLR4 | Text → parse tree |
| **2. Build** | `IslScriptVisitor.kt` + `ExecutionBuilder.kt` | Parse tree → command graph (AST → IR) |
| **3. Execute** | `Transformer.kt` + `*Command.kt` | IR → result via visitor pattern |

Key design decisions in the runtime:
- **All values are `JsonNode`** (Jackson) — a uniform JSON value type threaded through every command
- **Modifier pipeline** — `$val | trim | upperCase | padStart(8, "0")` chains are compiled to `ModifierValueCommand` chains, not nested function calls
- **Virtual threads** (JDK 21) for `parallel foreach` loops
- **Static optimizations**: switch → hash dispatch, self-spread `{...$var}` avoids deep copy, constant folding

---

### Can ISL Be Ported to Python? Yes — Feasibility Assessment

**Short answer: yes, it's very achievable.** Python is actually better suited than Java in some ways because of its dynamic nature. Here's the breakdown:

#### What maps cleanly

| ISL Feature | Python Equivalent |
|---|---|
| Grammar (ANTLR4) | **Lark** or **pyparsing** — both excellent, Lark preferred |
| `JsonNode` value type | Native `dict/list/str/int/float/bool/None` — no wrapper needed |
| Modifier pipeline | `functools.reduce` or simple loop over registered callables |
| Extension methods (`@.Service.Name()`) | Dict of callables keyed by name |
| Variable scoping | `dict` with `ChainMap` for layered scope |
| `foreach` / `map` / `filter` | Python list comprehensions / built-in `map`/`filter` |
| String interpolation (backtick + `${}`) | Custom string interpolation in the visitor |
| Math `{{ expr }}` | `ast.literal_eval` or a small expression evaluator |
| Type coercions (`to.string`, `to.decimal`) | Plain Python casts |
| `if/else` inline conditional | Python ternary |
| `switch` with hash dispatch | `dict.get()` lookup |

#### What requires care

1. **Dual-mode ANTLR lexer** — The backtick string interpolation uses a push/pop mode lexer. Lark supports contextual lexing but you'll need to model this carefully. The `${}`, `{{}}`, and `@.` embedded expressions all switch lexer modes mid-token.

2. **The 3-phase IR** — In Python you can simplify to 2 phases (parse → execute directly via visitor), skipping the intermediate token/command graph layer. This makes implementation simpler but loses the ability to do IR-level optimizations.

3. **`parallel foreach`** — Python's GIL means true parallelism needs `multiprocessing` or `concurrent.futures.ProcessPoolExecutor`. For I/O-bound tasks, `asyncio` is fine. For CPU-bound JSON transformations, this won't match JVM virtual threads.

4. **Crypto modifiers** (`hmac`, `sha`, `rsa`) — Use Python's `hashlib`, `hmac`, `cryptography` packages.

5. **Date modifiers** (`date.parse`, `to.string`) — Use `datetime` + `strptime`/`strftime`. Java's date format patterns (e.g., `yyyy-MM-dd'T'HH:mm:ssXXX`) differ from Python's (`%Y-%m-%dT%H:%M:%S%z`) — this needs a format-string translator.

6. **JsonPath modifier** — `jsonpath-ng` library covers this.

---

### Recommended Python Implementation Strategy

```
isl-python/
├── isl/
│   ├── grammar/          # Lark grammar file (ISL.lark)
│   ├── parser.py         # Lark → AST nodes
│   ├── ast_nodes.py      # Dataclasses mirroring token types
│   ├── interpreter.py    # Tree visitor → execute
│   ├── context.py        # ExecutionContext + variable scoping
│   ├── modifiers/        # All built-in modifiers as callables
│   │   ├── string.py     # trim, upperCase, lowerCase, padStart...
│   │   ├── array.py      # map, filter, reduce, sort, unique...
│   │   ├── math.py       # precision, Math.sum, Math.clamp...
│   │   ├── type.py       # to.string, to.number, to.decimal...
│   │   └── date.py       # date.parse, date.format
│   └── compiler.py       # Public API: compile_isl() -> transformer
```

The public API can mirror the Java one almost exactly:

```python
from isl import compile_isl

transformer = compile_isl("my-script", isl_source_code)

ctx = ExecutionContext()
ctx.set_variable("$input", json_data)
ctx.register_extension("MyService.Call", my_callable)

result = transformer.run("run", ctx)
```

---

### Performance: Java vs Python — What to Expect

The benchmarks already in the repo compare ISL/Java against **GraalVM Python** (running Python inside the JVM). Here's what the benchmark structure reveals about where performance lands, and what a native CPython port would look like:

#### Existing benchmark comparison (GraalVM Python vs ISL/Java)

The benchmark code (`JsonTransformBenchmark.kt`) is explicit that GraalVM Python was so slow they had to reduce iterations dramatically:

```kotlin
// Python full cycle:
@Warmup(iterations = 0)   // ← no warmup needed, it's that slow
@Measurement(iterations = 1, time = 1)  // minimal measurement
fun pythonFullCycle(): Any? { ... }

// Comment in code: "WARNING: This is extremely expensive and not recommended for production."
// "catastrophic initialization overhead"
```

#### Projected performance tiers for the Python ISL port

| Scenario | ISL/Java | Native Python ISL port | Ratio |
|---|---|---|---|
| **Parse + compile + run** (cold) | ~1–3ms | ~15–50ms | 10–20× slower |
| **Pre-compiled, single run** (warm) | ~0.1–0.5ms | ~2–10ms | 10–20× slower |
| **Throughput, batch transforms** | Very high | Moderate | GIL is the bottleneck |
| **Memory footprint** | JVM overhead (~256MB) | Lean (~30–80MB) | Python wins |
| **Startup time** | JVM cold start (~1–3s) | Instant | Python wins |

#### Why Python will be slower

1. **Interpreter overhead** — Python executes ~10–50× slower than JVM bytecode for compute-heavy loops (modifier chains, map/filter/reduce on arrays)
2. **No JIT** — The JVM JIT-compiles hot paths to native code after warmup; CPython never does this
3. **GIL** — Parallel `foreach` can't use true thread parallelism in CPython
4. **Dict lookups** — Everything in Python flows through hash maps; the JVM can inline virtual dispatch

#### Where Python won't matter

- **Low-volume transforms** (< 1000/sec): Python is fast enough
- **I/O-bound pipelines**: If transforms happen between HTTP calls, Python overhead disappears
- **Startup-sensitive scenarios**: Python starts instantly; the JVM doesn't
- **Memory-constrained environments**: Python's footprint is much smaller

#### If you need Python-speed ISL closer to Java

Two options to close the gap:
1. **PyPy** — 3–10× faster than CPython for interpreter-heavy code, likely brings the gap to 2–5× of Java
2. **Cython or compiled modifiers** — Compile the hot modifier functions (array ops, math) to native extensions
3. **GraalVM Python** — Run the ISL Python interpreter inside GraalVM's JIT; the benchmarks show this path exists but has catastrophic cold-start (the existing Python benchmark in the repo *is* GraalVM Python)

---

### Bottom Line

**Yes, ISL can be ported to Python to run the exact same `.isl` files.** The grammar is clean ANTLR4, the semantics are well-defined, and Python's dynamic nature simplifies several JVM-specific patterns (no need for `JsonNode` wrappers, no token/command graph split needed).

The core effort is:
1. Rewrite the grammar for **Lark** (~1 week)
2. Implement the **interpreter/visitor** (~2 weeks)
3. Port all **built-in modifiers** (~1 week, mostly mechanical)
4. Wire up the **extension registration API** (~2 days)

**Performance trade-off:** Expect ~10–20× slower than JVM for compute-heavy transforms. For typical integration workloads (< 500 transforms/sec), that's irrelevant. For high-throughput batch processing, Java ISL remains the right choice. PyPy narrows the gap to 2–5×.

The Java date format strings (`yyyy-MM-dd'T'HH:mm:ssXXX`) are the trickiest portability issue — you'll need a format-string translator layer to convert them to Python's `strptime`/`strftime` syntax.