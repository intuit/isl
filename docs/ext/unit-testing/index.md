---
title: Unit Testing
parent: Advanced Topics
nav_order: 3
has_children: true
---

# ISL Unit Testing

The ISL unit testing framework lets you write and run tests entirely in ISL. Tests live alongside your transformation code, use the same syntax, and can verify behavior without learning a separate testing language.

## Quick Start

1. Create an `.isl` file with `@test`-annotated functions:

```isl
@test
fun test_simpleAssertion() {
    $value: 42;
    @.Assert.equal(42, $value);
}
```

2. Run tests from the command line:

```bash
isl test
# or specify a path
isl test tests/
isl test tests/sample.isl
```

3. Or run tests programmatically from Kotlin/Java (see [Test Setup](setup.md)).

## What You Can Test

- **Transformations** – Call your ISL functions and assert on the output
- **Conditions** – Verify branching logic, edge cases
- **Modifiers** – Test `| trim`, `| map`, `| filter`, etc.
- **External integrations** – Use [mocking](mocking.md) to replace `@.Call.Api` and similar

## File Format and Structure

Tests are written in standard `.isl` files. A test file typically contains:

- One optional `@setup` function (runs before each test)
- One or more `@test` functions (each is a test case)

```isl
@setup
fun setup() {
    $x: 1;  // Shared setup runs before each test
}

@test
fun test_basic() {
    @.Assert.equal(1, 1);
}

@test("Custom display name")
fun test_withName() {
    @.Assert.equal(2, 2);
}

@test({ name: "Grouped test", group: "math" })
fun test_grouped() {
    @.Assert.equal(3, 3);
}
```

File discovery:

- **CLI**: `isl test` finds all `.isl` files (by default `**/*.isl`) containing `@setup` or `@test`
- **API**: You pass the list of files to `TransformTestPackageBuilder`

## Attributes (Annotations)

| Attribute | Description |
|-----------|-------------|
| `@test` | Marks a function as a test. Runs as a test case. |
| `@test("Name")` | Same, with a custom display name |
| `@test(name, group)` | Custom name and group for organization |
| `@test({ name: "x", group: "y" })` | Object form for name and group |
| `@setup` | Marks a function to run before each test in the file (at most one per file) |

See [Test Annotations](annotations.md) for details.

## Assertions

Use `@.Assert` to verify values:

| Assertion | Description |
|-----------|-------------|
| `@.Assert.equal(expected, actual, message?)` | Deep equality (objects, arrays, primitives) |
| `@.Assert.notEqual(expected, actual, message?)` | Values must differ |
| `@.Assert.notNull(value, message?)` | Value must not be null |
| `@.Assert.isNull(value, message?)` | Value must be null |
| `@.Assert.contains(expected, actual)` | actual contains expected |
| `@.Assert.matches(pattern, value)` | value matches regex |
| `@.Assert.startsWith(prefix, value)` | value starts with prefix |
| ... | See [Assertions](assertions.md) for the full list |

## Loading Test Fixtures

Use `@.Load.From(fileName)` to load JSON, YAML, or CSV files relative to the current ISL file:

```isl
@test
fun test_withFixture() {
    $data = @.Load.From("fixtures/input.json")
    @.Assert.equal("expected", $data.name)

    $config = @.Load.From("config.yaml")
    @.Assert.equal(10, $config.count)

    $rows = @.Load.From("fixtures/data.csv")
    @.Assert.equal(2, $rows | length)
}
```

Supported formats: `.json`, `.yaml`, `.yml`, `.csv` (all converted to JSON). See [Loading Fixtures](loading.md).

## How to Run Tests

### Command Line (Recommended)

```bash
isl test [path] [options]
```

- `path`: Directory, file, or glob (default: current directory)
- `--glob PATTERN`: Filter files (e.g. `**/*.isl`)
- `-o, --output FILE`: Write results to JSON

### Programmatic (Kotlin/Java)

See [Test Setup](setup.md) for adding the `isl-test` dependency and running tests from code.

## Mocking

Mock external functions (e.g. `@.Call.Api`) so tests don't hit real services:

```isl
@test
fun test_withMock() {
    @.Mock.Func("Call.Api", { status: 200, body: "ok" })
    $result = @.Call.Api("https://example.com")
    @.Assert.equal(200, $result.status)
}
```

See [Mocking](mocking.md) for parameter matching, indexed (sequential) returns, loading mocks from files, captures, and annotation mocks.

## Test Output

- **CLI**: Prints pass/fail per test, with failure messages and locations
- **JSON**: Use `-o results.json` for machine-readable output
- **Exit code**: 1 if any test failed, 0 if all passed

## Next Steps

- [Test Setup](setup.md) – CLI usage, dependencies, programmatic API
- [Test Annotations](annotations.md) – `@test` and `@setup` in detail
- [Assertions](assertions.md) – Full assertion reference
- [Loading Fixtures](loading.md) – `@.Load.From` for JSON/YAML/CSV
- [Mocking](mocking.md) – Mock functions and annotations
