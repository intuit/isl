---
title: Changelog
nav_order: 100
---

## [Unreleased] - Performance Benchmarks

### Performance Metrics

Added comprehensive JMH (Java Microbenchmark Harness) benchmarking framework to measure ISL Transform performance compared to JOLT, MVEL, and Python (GraalVM). Benchmarks test realistic e-commerce scenarios using Shopify order transformations with multiple modifiers.

#### Benchmark Results (JDK 21.0.7, OpenJDK 64-Bit Server VM)

**Test Scenario**: Shopify order JSON (4.5KB) transformation with complex ISL script featuring:
- **3 helper functions**: `convertAddress`, `convertCustomer`, `processLineItem`
- **Function calls**: Cross-function calls using `@.This.functionName()`
- **Array operations**: `map`, `filter`, `unique`, `sort`, `length`, `first`
- **Math operations**: `Math.sum`, `Math.clamp`, `precision`
- **String modifiers**: `trim`, `capitalize`, `upperCase`, `lowerCase`, `titleCase`, `snakeCase`, `padStart`, `truncate`, `default`
- **Type conversions**: `to.string`, `to.number`, `to.decimal`, `to.boolean`
- **Conditionals**: `if/else` expressions
- **Date operations**: `date.parse`, `to.string` with format
- **Complex transformations**: 40+ field mappings with nested object construction

#### Pre-Compiled Performance (Production Scenario)

| Implementation | Score (ms/op) | vs ISL Simple | Memory/op | Description |
|---------------|---------------|---------------|-----------|-------------|
| **MVEL** 🥇 | **0.003 ms** | 1.3x faster | ~12 KB | Fastest execution, but 12,727x compilation penalty |
| **ISL Simple** 🥈 | **0.004 ms** | **baseline** | ~15 KB | **Best overall value** - speed + features + maintainability |
| **ISL Complex (Clean)** 🥉 | **0.020 ms** | 5x slower | ~35 KB | Full features with inline transformations |
| **JOLT** | **0.034 ms** | 8.5x slower | ~28 KB | Industry standard, limited features |
| **Python (GraalVM)** | **0.074 ms** | 18.5x slower | - | 17x slower than ISL, impractical for JSON |

**Note:** ISL Complex Verbose excluded from comparisons (intentionally inefficient coding style for demonstration).

#### Full Cycle Performance (Parse + Compile + Execute)

| Implementation | Score (ms/op) | vs ISL Simple | Memory/op | Compilation Penalty |
|---------------|---------------|---------------|-----------|---------------------|
| **JOLT** 🥇 | **0.070 ms** | 2.1x faster | ~32 KB | 2.1x (minimal) |
| **ISL Simple** 🥈 | **0.149 ms** | **baseline** | ~65 KB | 36x |
| **ISL Complex (Clean)** | **0.366 ms** | 2.5x slower | ~180 KB | 17x |
| **MVEL** ⚠️ | **35.185 ms** | 236x slower | ~450 KB | **12,727x** (catastrophic) |
| **Python (GraalVM)** ❌ | **240.277 ms** | **1,612x slower** | **~3.2 MB** | **3,247x** (catastrophic) |

**Note:** ISL Complex Verbose excluded from comparisons (intentionally inefficient coding style).

**Key Insights**:
- **ISL Simple is the best overall choice**: 8.4x faster than JOLT, 17x faster than Python, with rich features and low memory usage
- **Pre-compilation is critical**: ISL Simple is 36x faster pre-compiled, MVEL is 12,727x faster, Python is 3,247x faster
- **Python (GraalVM) is impractical for JSON transformations**:
  - 240 ms initialization overhead (context creation)
  - 17x slower execution even when cached
  - 213x more memory usage (3.2 MB vs 15 KB for ISL)
  - Need to process ~3,000 requests to break even vs ISL
- **MVEL has catastrophic compilation overhead**: 35.185 ms compilation vs 0.003 ms execution (12,727x penalty)
- **Memory efficiency**: ISL Simple uses only ~15 KB per operation vs Python's ~3.2 MB (213x difference)
- **Production recommendation**: Use ISL for JSON transformations, reserve Python for ML/data science workloads

**Throughput Estimates** (single-threaded, pre-compiled):
- ISL Simple: ~250,000 transformations/second (~15 KB memory per op)
- ISL Complex (Clean): ~50,000 transformations/second (~35 KB memory per op)
- JOLT: ~29,400 transformations/second (~28 KB memory per op)
- Python (GraalVM): ~13,500 transformations/second (~8 KB memory per op, but requires context caching)

**Test Environment**:
- JMH Version: 1.37
- JDK: 21.0.7 (OpenJDK 64-Bit Server VM)
- Warmup: 2 iterations, 1s each
- Measurement: 3 iterations, 1s each
- Mode: Average time (avgt)

### Added
- JMH benchmarking framework integration with memory profiling
- `JsonTransformBenchmark` - comprehensive comparison of JOLT, ISL, MVEL, and Python (GraalVM)
- `SimpleTransformBenchmark` - minimal overhead testing
- `OutputComparisonBenchmark` - validation of transformation correctness
- Python (GraalVM) polyglot integration for performance comparison
- Memory allocation tracking per operation
- Benchmark test data: Shopify order JSON (based on Shopify Commerce API)
- Automated benchmark execution via `gradlew :isl-transform:jmh`
- JSON results output to `build/reports/jmh/results.json`
- Comprehensive documentation in `docs/dev/benchmark-report.md`

---

**2.4.20** - First public release

