# ISL CLI Examples

Sample scripts and tests for the ISL command-line interface.

## Contents

| File | Description |
|------|-------------|
| `hello.isl` | Simple greeting transform |
| `transform.isl` | Data transformation with filter/reduce |
| `tests/calculator.isl` | Pure functions for unit testing (add, double, greet, echo) |
| `tests/service.isl` | Functions that use mocks (Api.Call, Config.GetLimit, Data.GetItems) |
| `tests/calculator.tests.yaml` | YAML-driven unit tests for `calculator.isl` |
| `tests/service.tests.yaml` | YAML-driven unit tests for `service.isl` (uses `../mocks/sample-mocks.yaml`) |
| `tests/sample.isl` | Annotation-based tests (`@setup`, `@test`) with `@.Mock.Load` |
| `mocks/sample-mocks.yaml` | Mock definitions (same format as `@.Mock.Load`) |

## Running examples

From the **examples** directory (e.g. after `cd isl-cmd/examples`):

```bash
# Transform
isl transform hello.isl -i hello-input.json --pretty
isl transform transform.isl -i data.json --pretty

# Validate
isl validate transform.isl

# Run all tests (YAML suites + annotation-based .isl tests)
isl test .
```

To run only a specific YAML suite:

```bash
isl test tests/calculator.tests.yaml
isl test tests/service.tests.yaml
```

## YAML test format

`*.tests.yaml` suites use: `category`, `setup.islSource`, optional `setup.mockSource` (single path or array) and `setup.mocks` (inline), optional suite-level `assertOptions`, and `tests` (or `islTests`) with `name`, `functionName`, `input`, `expected`, and optional per-test `assertOptions`. See [../README.md](../README.md#test-command) for a short reference and [../docs/ext/unit-testing/yaml-tests.md](../../docs/ext/unit-testing/yaml-tests.md) for the full format and assertOptions reference.
