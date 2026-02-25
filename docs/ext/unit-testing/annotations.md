---
title: Test Annotations
parent: Unit Testing
grand_parent: Advanced Topics
nav_order: 3
---

# Test Annotations

## @test

Marks a function as a unit test. The function runs as a test case when tests are executed.

### Basic form

```isl
@test
fun test_addNumbers() {
    $sum: 1 + 2;
    @.Assert.equal(3, $sum);
}
```

When no parameters are given, the function name is used as the test name.

### Custom display name

```isl
@test("Addition of positive numbers")
fun test_addNumbers() {
    @.Assert.equal(3, 1 + 2);
}
```

### Name and group

```isl
@test("Check total", "math")
fun test_total() {
    @.Assert.equal(10, 5 + 5);
}
```

### Object form

```isl
@test({ name: "Grouped test", group: "math" })
fun test_grouped() {
    $value: 30;
    @.Assert.equal(30, $value);
}
```

Use the object form when you need both a custom name and group.

### Parameter summary

| Form | Example | Result |
|------|---------|--------|
| No params | `@test` | Test name = function name |
| String | `@test("My test")` | Test name = "My test" |
| Two strings | `@test("Name", "group")` | Custom name and group |
| Object | `@test({ name: "x", group: "y" })` | Custom name and group |

## @setup

Marks a function to run **before each test** in the same file. Use it for shared initialization.

- At most **one** `@setup` function per file
- Runs before every `@test` function in that file
- Does not run as a test itself

```isl
@setup
fun setup() {
    $sharedState: { count: 0 };
    // This runs before each test
}

@test
fun test_first() {
    // setup() already ran
    @.Assert.equal(1, 1);
}

@test
fun test_second() {
    // setup() runs again before this test
    @.Assert.equal(2, 2);
}
```

## File structure

A typical test file:

```isl
// Optional: imports
import Helper from "../helper.isl";

@setup
fun setup() {
    // Runs before each test
}

@test
fun test_basic() {
    // Test code
}

@test("Custom name")
fun test_withName() {
    // Test code
}

@test({ name: "Edge case", group: "validation" })
fun test_edgeCase() {
    // Test code
}
```
