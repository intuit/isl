---
title: Changelog
nav_order: 100
---

## [Unreleased] - Performance Benchmarks

### Performance Metrics

Added comprehensive JMH (Java Microbenchmark Harness) benchmarking framework to measure ISL Transform performance. Benchmarks test realistic e-commerce scenarios using Shopify order transformations with multiple modifiers.

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

| Benchmark | Score (ms/op) | Description |
|-----------|---------------|-------------|
| **Execution Only** (pre-compiled, pre-parsed JSON) | **0.031 ms** | Pure ISL execution with pre-compiled script and pre-parsed JSON (fastest scenario) |
| **Execution + JSON Parsing** (pre-compiled) | **0.029 ms** | Runtime execution including JSON parsing (most common production scenario) |
| **Modifier Chain Execution** | **0.004 ms** | Simple modifier chain (trim → upperCase → reverse → truncate) |
| **Parsing Only** | **0.344 ms** | ANTLR parser with SLL prediction mode (complex script with functions) - **4.4x faster after optimizations** |
| **Compilation Only** | **0.377 ms** | Parse + build execution graph (includes function compilation) - **3.5x faster after optimizations** |
| **Full Transformation Cycle** | **0.539 ms** | End-to-end: parse + compile + execute - **3.1x faster after optimizations** |

**Key Insights**:
- **Pre-compiled execution is 17-19x faster** than full transformation cycle (0.029-0.031ms vs 0.539ms)
- **JSON parsing overhead is negligible** - runtime parsing is actually slightly faster due to JIT optimizations
- **Recent performance optimizations delivered massive gains**:
  - **Parsing: 4.4x faster** (1.500ms → 0.344ms)
  - **Compilation: 3.5x faster** (1.337ms → 0.377ms)
  - **Full cycle: 3.1x faster** (1.677ms → 0.539ms)
- **Execution overhead is minimal** - only 0.029-0.031ms for complex transformation with 3 functions, 40+ fields, and multiple array operations
- **Modifier chains are highly optimized** - 0.004ms for 4 chained string operations
- **Production recommendation**: Pre-compile ISL scripts once and reuse for optimal performance

**Throughput Estimates** (single-threaded):
- Pre-compiled execution (pure ISL): ~32,260 transformations/second
- Pre-compiled execution + JSON parsing: ~34,480 transformations/second
- Full cycle (parse + compile + execute): ~1,855 transformations/second (**3.1x improvement**)

**Test Environment**:
- JMH Version: 1.37
- JDK: 21.0.7 (OpenJDK 64-Bit Server VM)
- Warmup: 2 iterations, 1s each
- Measurement: 3 iterations, 1s each
- Mode: Average time (avgt)

### Added
- JMH benchmarking framework integration
- `ShopifyOrderTransformBenchmark` - realistic e-commerce transformation benchmark
- `ModifierPerformanceBenchmark` - modifier chain performance testing
- Benchmark test data: Shopify order JSON (based on Shopify Commerce API)
- Automated benchmark execution via `gradlew jmh`
- JSON results output to `build/reports/jmh/results.json`

---

**2.4.20** - First public release

