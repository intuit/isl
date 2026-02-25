---
title: Loading Fixtures
parent: Unit Testing
grand_parent: Advanced Topics
nav_order: 5
---

# Loading Test Fixtures

Use `@.Load.From(fileName)` to load JSON, YAML, or CSV files as test data. Paths are resolved **relative to the directory of the current ISL file**.

## Syntax

```isl
$data = @.Load.From("fileName")
```

- `fileName` – Path relative to the current file (e.g. `fixtures/data.json`, `../shared/config.yaml`)

## Supported Formats

| Extension | Description |
|-----------|-------------|
| `.json` | Parsed as JSON |
| `.yaml`, `.yml` | Parsed as YAML, converted to JSON |
| `.csv` | Parsed as CSV; first row = headers, converted to array of objects |

All formats are returned as JSON (objects or arrays) for use in assertions.

## Examples

### JSON

**fixtures/user.json:**
```json
{"name": "Alice", "age": 30, "active": true}
```

**tests/user.isl:**
```isl
@test
fun test_loadJson() {
    $user = @.Load.From("fixtures/user.json")
    @.Assert.equal("Alice", $user.name)
    @.Assert.equal(30, $user.age)
    @.Assert.equal(true, $user.active)
}
```

### YAML

**fixtures/config.yaml:**
```yaml
key: value
nested:
  count: 10
  items: [a, b, c]
```

**tests/config.isl:**
```isl
@test
fun test_loadYaml() {
    $config = @.Load.From("fixtures/config.yaml")
    @.Assert.equal("value", $config.key)
    @.Assert.equal(10, $config.nested.count)
    @.Assert.equal(3, $config.nested.items | length)
}
```

### CSV

**fixtures/data.csv:**
```csv
id,name,score
1,Alice,100
2,Bob,85
```

**tests/data.isl:**
```isl
@test
fun test_loadCsv() {
    $rows = @.Load.From("fixtures/data.csv")
    @.Assert.equal(2, $rows | length)
    @.Assert.equal("Alice", $rows[0].name)
    @.Assert.equal(100, $rows[0].score)
    @.Assert.equal("Bob", $rows[1].name)
}
```

CSV is parsed with the first row as headers. Each subsequent row becomes an object with those headers as keys.

## Path Resolution

Paths are relative to the **directory of the current ISL file**:

| Current file | `fileName` | Resolved path |
|--------------|------------|---------------|
| `tests/sample.isl` | `fixtures/data.json` | `tests/fixtures/data.json` |
| `tests/sample.isl` | `../shared/config.yaml` | `shared/config.yaml` |
| `tests/unit/sample.isl` | `../../fixtures/data.json` | `fixtures/data.json` |

## Availability

`@.Load.From` is available only in the **test context**:

- ✅ When running tests via `isl test`
- ✅ When running via `TransformTestPackage` with `basePath` passed to `TransformTestPackageBuilder`
- ❌ In regular transform execution (e.g. `isl transform`)

If `basePath` is not set (e.g. programmatic use without it), `Load.From` throws a clear error.

## Error Handling

- **File not found**: Throws with the resolved path
- **Unsupported format**: Throws for extensions other than `.json`, `.yaml`, `.yml`, `.csv`
- **Parse error**: Invalid JSON/YAML/CSV throws during parsing
