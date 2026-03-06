---
title: YAML-Driven Test Suites
parent: Unit Testing
grand_parent: Advanced Topics
nav_order: 2
---

# YAML-Driven Test Suites

You can define unit tests in **\*.tests.yaml** (or \*.tests.yml) files without writing ISL test code. A YAML suite specifies the ISL module to test, optional mocks, and a list of test cases with `functionName`, `input`, and `expected` result. The runner invokes each function and compares the result to `expected` using configurable comparison options.

## When to Use YAML Suites

- **Data-heavy tests** – Many input/expected pairs without custom logic
- **Non-ISL authors** – QA or product can add tests by editing YAML
- **Shared mocks** – Reuse `mockSource` and inline `mocks` across many tests
- **CI / tooling** – Generate or parse `*.tests.yaml` from other systems

You can mix YAML suites with [annotation-based tests](annotations.md) in the same project; `isl test .` runs both.

## File and Discovery

- **Naming**: `*.tests.yaml` or `*.tests.yml` (e.g. `calculator.tests.yaml`).
- **Discovery**: When you run `isl test <dir>`, the CLI finds all such files under the path (default glob: `**/*.tests.yaml`).
- **Single file**: `isl test path/to/suite.tests.yaml` runs only that suite.

All paths inside the YAML file (`islSource`, `mockSource`) are **relative to the directory containing the `.tests.yaml` file**.

## Suite Structure

```yaml
category: my-group-name          # optional; used as test group in output
setup:
  islSource: mymodule.isl       # ISL file to test (required)
  mockSource: optional.yaml     # optional; see Mocks below
  mocks:                        # optional inline mocks; see Mocks below
    func: [ ... ]
assertOptions:                  # optional; see Assert options below
  nullSameAsMissing: true
tests:                          # or islTests (same meaning)
  - name: test display name
    functionName: myFunction
    input: 42                   # or object for multiple params
    expected: { result: 42 }
```

- **category** – Label for the suite in results (e.g. `[ISL Result] my-group-name`). If omitted, the suite file name (without extension) is used.
- **setup** – Required. Contains `islSource` and optionally `mockSource` and `mocks`.
- **assertOptions** – Optional. Controls how `expected` is compared to the function result. Can be set at suite level and overridden per test.
- **tests** / **islTests** – List of test entries. Either key is accepted.

## Setup

### islSource (required)

The ISL file to load and run. Path is relative to the directory of the `.tests.yaml` file.

```yaml
setup:
  islSource: calculator.isl
```

### mockSource (optional)

Mock definitions loaded from file(s), in the same format as `@.Mock.Load`. Paths are relative to the suite directory.

- **Single file**: `mockSource: ../mocks/sample-mocks.yaml`
- **Multiple files** (loaded in order; later overrides earlier):  
  `mockSource: [common.yaml, overrides.yaml]`

Supported extensions: `.json`, `.yaml`, `.yml`.

### mocks (optional, inline)

Inline mocks applied **after** `mockSource`, so they override or add to file-based mocks. Same structure as in `@.Mock.Load`: `func` and/or `annotation` arrays.

```yaml
setup:
  islSource: service.isl
  mockSource: ../mocks/sample-mocks.yaml
  mocks:
    func:
      - name: "Api.Call"
        return: { status: 200, body: "overridden" }
```

All mocks are additive; parameter lists differentiate overloads.

## Test Entries

Each entry under `tests` (or `islTests`) has:

| Field | Required | Description |
|-------|----------|-------------|
| **name** | Yes | Display name in results |
| **functionName** | Yes | ISL function to call |
| **input** | No | Input to the function. Single value for single-param; object with param names as keys for multiple params |
| **expected** | No | Expected return value (JSON). Omitted or `null` means expect `null` |
| **byPassAnnotations** | No | If `true`, bypass annotation processing (optional) |
| **assertOptions** | No | Override suite `assertOptions` for this test only. Same formats as suite (object, comma-separated list, or array of option names) |

### Input format

- **Single-parameter function**: `input` can be a scalar or object (passed as that one argument).
- **Multi-parameter function**: `input` must be an object; keys are parameter names (with or without `$`). Values are passed as the corresponding arguments.

```yaml
# Single param
- name: double a number
  functionName: double
  input: 7
  expected: 14

# Multiple params
- name: add two numbers
  functionName: add
  input:
    a: 2
    b: 3
  expected: 5
```

## Assert Options (assertOptions)

Assert options control how the actual function result is compared to `expected`. By default, comparison is strict (exact match). You can relax it at the **suite** level and optionally **per test**.

### Where to set assertOptions

- **Suite level**: under the root key `assertOptions`. Applies to all tests in the suite unless a test overrides.
- **Per test**: under a test entry as `assertOptions`. Overrides the suite options for that test only.

### Formats

You can write `assertOptions` in three ways:

**1. Object (explicit booleans):**

```yaml
assertOptions:
  nullSameAsMissing: true
  ignoreExtraFieldsInActual: true
```

**2. Comma-separated list of option names:**

```yaml
assertOptions: nullSameAsMissing, nullSameAsEmptyArray, missingSameAsEmptyArray, ignoreExtraFieldsInActual, numbersEqualIgnoreFormat
```

**3. Array of option names:**

```yaml
assertOptions:
  - nullSameAsMissing
  - nullSameAsEmptyArray
  - missingSameAsEmptyArray
  - ignoreExtraFieldsInActual
  - numbersEqualIgnoreFormat
```

### Option reference

All options default to `false` (strict comparison). Supported options:

| Option | Default | Description |
|--------|---------|-------------|
| **nullSameAsMissing** | `false` | Treat `null` and missing (absent key) as equal |
| **nullSameAsEmptyArray** | `false` | Treat `null` and empty array `[]` as equal |
| **missingSameAsEmptyArray** | `false` | Treat missing (absent key) and empty array `[]` as equal |
| **ignoreExtraFieldsInActual** | `false` | Only compare keys present in `expected`; ignore extra keys in actual |
| **numbersEqualIgnoreFormat** | `false` | Compare numbers by numeric value only (e.g. `1234`, `1234.0`, `1234.00` are equal) |

### Example: suite and per-test override

```yaml
category: api
setup:
  islSource: api.isl
assertOptions:
  ignoreExtraFieldsInActual: true
  numbersEqualIgnoreFormat: true
tests:
  - name: strict comparison for this test
    functionName: getExact
    input: 1
    expected: { id: 1, name: "x" }
    assertOptions: {}   # or omit; use only suite options

  - name: allow extra fields and null as missing
    functionName: getPartial
    input: 2
    expected: { id: 2 }
    assertOptions:
      nullSameAsMissing: true
      ignoreExtraFieldsInActual: true
```

## Running YAML Suites

### Command line

```bash
# Run all tests (YAML suites + .isl tests under current dir)
isl test .

# Run a single YAML suite
isl test path/to/calculator.tests.yaml

# Run only tests whose function name matches
isl test . -f add -f double

# Run a specific test in a specific suite (suiteFile:functionName)
isl test . -f calculator.tests.yaml:add
```

The `-f` / `--function` filter applies to both annotation-based tests and YAML suites. For YAML, you can use `functionName` or `suiteFile:functionName` (e.g. `calculator.tests.yaml:add`).

### Output

- Pass/fail per test with the entry’s `name` (and `functionName` in brackets when different).
- On failure, a comparison message shows expected vs actual and, when available, path-level differences (e.g. `$.field.[0].key`).
- Use `-o results.json` for machine-readable results.

## Full Example

**calculator.isl** (snippet):

```isl
fun add($a, $b) { $a + $b }
fun double($x) { $x * 2 }
```

**calculator.tests.yaml**:

```yaml
category: calculator
setup:
  islSource: calculator.isl
tests:
  - name: add two numbers
    functionName: add
    input: { a: 2, b: 3 }
    expected: 5
  - name: double a number
    functionName: double
    input: 7
    expected: 14
```

Run: `isl test calculator.tests.yaml` or `isl test .`

## See also

- [Test Setup](setup.md) – CLI discovery, `-f`, `-o`
- [Mocking](mocking.md) – Mock format for `mockSource` and `mocks`
- [Test Annotations](annotations.md) – `@test` / `@setup` in .isl files
