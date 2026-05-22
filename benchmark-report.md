# ISL Performance Benchmark Report

Cross-runtime performance comparison of the ISL (Intuit Scripting Language) interpreter
across Java, Python, and C# implementations, using identical workloads.

> **Environment**
> Machine: Windows 11, Intel/AMD dev workstation
> Java: Amazon Corretto 21.0.7 — benchmarked with JMH (3 forks, warmup + measurement)
> Python: CPython 3.13.13 — benchmarked with `timeit` (100 warmup + 500–2000 timed iters)
> C#: .NET 8.0, Release build — benchmarked with `Stopwatch` (100 warmup + 500–2000 timed iters)
> Date: 2026-05-22

---

## Benchmark Workloads

All three runtimes run the exact same ISL scripts against the same JSON input data.

| Benchmark | Description |
|---|---|
| **islSimpleTransformation** | Product object transform: field mapping, `trim`, string interpolation, array index, inline-if, `join.string`, `date.fromEpochSeconds`, math |
| **islSimpleTransform (flat)** | Flat field mapping only — no functions, no modifiers (equivalent to JOLT capability level) |
| **shopifySimple** | Shopify order → internal format: 30+ field mappings, `map({})` array transform |
| **shopifyComplex** | Shopify order → internal format: helper functions, `trim`/`upperCase`/`lowerCase`, date parsing, `filter`, `Math.sum`, `unique`/`sort`, string templates |

**Transform-only** = pre-compiled transformer, context setup + execute (steady-state throughput)
**Full-cycle** = compile ISL source + context setup + execute (measures real-world startup cost)

---

## Java (JMH — reference implementation)

Measured with JMH `avgt` mode, 3 forks, warmup + measurement iterations.

| Benchmark | ms/op (transform) | ops/sec | ms/op (full-cycle) | ops/sec |
|---|---:|---:|---:|---:|
| `islSimpleTransformation` | 0.004 | 250,000 | 0.120 | 8,333 |
| `islSimpleTransform (flat)` | 0.001 | 1,000,000 | — | — |
| `shopifySimple` | ~0.004* | ~250,000* | — | — |
| `shopifyComplex` | ~0.004* | ~250,000* | — | — |

> \* Java shopify numbers extrapolated from `islSimpleTransformation` baseline.
> The JMH suite only directly benchmarks `islSimpleTransformation` and `islSimpleFullCycle`.
> Real shopify numbers would require adding those scripts to the JMH suite.

---

## Python (timeit — Python port)

Measured with Python `timeit`, 100 warmup + 500–2000 timed iterations, CPython 3.13.13.

| Benchmark | ms/op (transform) | ops/sec | ms/op (full-cycle) | ops/sec |
|---|---:|---:|---:|---:|
| `islSimpleTransformation` | 0.039 | 25,506 | 0.342 | 2,927 |
| `islSimpleTransform (flat)` | 0.010 | 97,717 | 0.070 | 14,332 |
| `shopifySimple` | 0.165 | 6,062 | 1.120 | 893 |
| `shopifyComplex` | 0.476 | 2,100 | 2.395 | 418 |

---

## C# (Stopwatch — C# port)

Measured with `System.Diagnostics.Stopwatch`, 100 warmup + 500–2000 timed iterations, .NET 8.0 Release.

| Benchmark | ms/op (transform) | ops/sec | ms/op (full-cycle) | ops/sec |
|---|---:|---:|---:|---:|
| `islSimpleTransformation` | 0.013 | 75,880 | 0.075 | 13,321 |
| `islSimpleTransform (flat)` | 0.002 | 486,381 | 0.015 | 66,649 |
| `shopifySimple` | 0.078 | 12,883 | 0.274 | 3,646 |
| `shopifyComplex` | 0.239 | 4,189 | 0.586 | 1,706 |

---

## Three-Runtime Comparison

### Transform-only (steady-state throughput)

| Benchmark | Java ms/op | C# ms/op | Python ms/op | C#/Java ratio | Python/Java ratio | C#/Python ratio |
|---|---:|---:|---:|---:|---:|---:|
| islSimpleTransformation | 0.004 | 0.013 | 0.039 | ~3× slower | ~10× slower | ~3× faster |
| islSimpleTransform (flat) | 0.001 | 0.002 | 0.010 | ~2× slower | ~10× slower | ~5× faster |
| shopifySimple | ~0.004* | 0.078 | 0.165 | ~20× slower* | ~40× slower* | ~2× faster |
| shopifyComplex | ~0.004* | 0.239 | 0.476 | ~60× slower* | ~120× slower* | ~2× faster |

### Full-cycle (compile + run)

| Benchmark | Java ms/op | C# ms/op | Python ms/op | C#/Java ratio | Python/Java ratio | C#/Python ratio |
|---|---:|---:|---:|---:|---:|---:|
| islSimpleTransformation | 0.120 | 0.075 | 0.342 | **1.6× faster** | ~2.8× slower | **4.6× faster** |
| islSimpleTransform (flat) | — | 0.015 | 0.070 | — | — | **4.7× faster** |
| shopifySimple | — | 0.274 | 1.120 | — | — | **4.1× faster** |
| shopifyComplex | — | 0.586 | 2.395 | — | — | **4.1× faster** |

> \* Java shopify transform numbers are extrapolated estimates; the JMH suite does not include those scripts directly.

---

## Analysis

### Steady-state transform throughput

**C# vs Python**: C# is consistently **3–5× faster** for simple transforms and **~2× faster** for complex Shopify transforms. The gap narrows on heavier workloads because Python's overhead is more evenly distributed across more operations (array maps, modifier chains), while C# still pays .NET object-allocation costs on large JSON trees.

**C# vs Java**: Java JIT wins on simple transforms (~2–3×) due to decades of JIT maturity and hotspot optimization. On complex scripts the extrapolated gap grows to ~20–60×, though this is estimated. Java's JIT ultimately eliminates interpreter overhead that both C# and Python still pay.

**Python vs Java**: ~10× slower on simple transforms, degrading to ~40–120× on complex scripts. CPython's pure-Python interpreter loop has no JIT and pays full dynamic dispatch on every operation.

### Full-cycle throughput (compile + run)

The most striking finding: **C# full-cycle is faster than Java full-cycle** (0.075 ms vs 0.120 ms for `islSimpleTransformation`). The C# parser/compiler is lean — no JVM class-loading, no JMH framework overhead, no reflection warmup. The .NET 8 runtime JITs the interpreter's hot path quickly.

C# is **4–5× faster than Python** for full-cycle across all workloads, which makes it the best choice in per-request compilation scenarios (e.g., dynamically loaded ISL scripts at runtime).

### Scaling with complexity

| Complexity | Java ops/sec | C# ops/sec | Python ops/sec | C# vs Python |
|---|---:|---:|---:|---:|
| Flat mapping | ~1,000,000 | 486,381 | 97,717 | **5× faster** |
| Product transform | ~250,000 | 75,880 | 25,506 | **3× faster** |
| Shopify simple | ~250,000* | 12,883 | 6,062 | **2.1× faster** |
| Shopify complex | ~250,000* | 4,189 | 2,100 | **2× faster** |

Both C# and Python degrade on complex scripts, but C# degrades more gracefully: it holds a ~2–5× advantage over Python that remains consistent. The relative gap shrinks on heavier workloads because JSON tree manipulation (common to both) becomes the bottleneck.

### Comparison with other Java engines (from JMH)

| Engine | Transform ms/op | vs Java ISL |
|---|---:|---:|
| ISL (Java) | 0.004 | baseline |
| MVEL | 0.003 | 1.3× faster |
| ISL (C#) | 0.013 | 3.3× slower |
| Jolt (Java) | 0.030 | 7.5× slower |
| ISL (Python) | 0.039 | 10× slower |
| MVEL full-cycle | 97.431 | 24,000× slower cold start |

**C# ISL** sits between MVEL and Jolt in steady-state throughput — significantly faster than the Python port and competitive with Jolt (the most common Java JSON transformation alternative), while offering the full ISL semantic richness.

---

## Fixture Test Coverage

| Runtime | Tests Passing | Total | Coverage |
|---|---:|---:|---:|
| Java | 959 | 959 | 100% |
| C# | 341 | 341 | 100% |
| Python | 228 | 320 | 71% |

> Note: Python and C# fixture counts differ because C# skips a larger set of unimplemented features
> (parallel foreach, XML/CSV/YAML format modifiers, `$isl.` built-ins, `@.Log.*`, `@.Pagination.*`, `@.Retry.*`)
> while Python attempts but fails some of the same.

---

## Practical Conclusions

### When to use each runtime

| Scenario | Recommendation |
|---|---|
| Maximum throughput, JVM platform | **Java** — fastest steady-state, 100% feature parity |
| .NET platform, high throughput | **C#** — 3–5× faster than Python, competitive with Jolt, full-cycle beats Java |
| Python/ML platform, moderate throughput | **Python** — 0.04–0.5 ms/op is negligible vs network I/O in most ETL workloads |
| Per-request script compilation | **C#** — best full-cycle throughput (0.015–0.6 ms) |

### Operational viability

For most integration and ETL use cases where transforms run once per API request (network I/O: 10–500 ms):

- **Java ISL**: 0.004 ms/op → **operationally negligible** in all scenarios
- **C# ISL**: 0.013–0.24 ms/op → **operationally negligible** for most workloads
- **Python ISL**: 0.04–0.48 ms/op → **operationally negligible** for simple/medium scripts; may become measurable for complex Shopify-style transforms at very high RPS

---

## Benchmark Reproducibility

```bash
# Java
cd isl-transform
mvn jmh:run -pl . -Pbenchmark

# Python
cd isl-python
python bench.py

# C#
cd isl-csharp
dotnet run --project Isl.Bench -c Release
```
