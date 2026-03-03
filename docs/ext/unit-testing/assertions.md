---
title: Assertions
parent: Unit Testing
grand_parent: Advanced Topics
nav_order: 2
---

# Assertions

Assertions verify values in unit tests. They are available under the `@.Assert` namespace. All assertion methods accept an optional third parameter `message` for custom failure output.

## Equality

### equal

Verifies that two values are equal (deep equality for objects and arrays; property order is ignored for objects).

**Syntax:** `@.Assert.equal(expected, actual, message?)`

```isl
@test
fun test_equal() {
    $var1: 20;
    @.Assert.equal(20, $var1);

    $obj1: { a: 1, b: 2 };
    $obj2: { b: 2, a: 1 };
    @.Assert.equal($obj1, $obj2);  // Objects equal despite property order
}
```

### notEqual

Verifies that two values are not equal.

**Syntax:** `@.Assert.notEqual(expected, actual, message?)`

```isl
@test
fun test_notEqual() {
    $var1: 20;
    @.Assert.notEqual(40, $var1, "Values should differ");
}
```

## Null Checks

### notNull

Verifies that a value is not null.

**Syntax:** `@.Assert.notNull(value, message?)`

```isl
@test
fun test_notNull() {
    $var1: 42;
    @.Assert.notNull($var1, "Value should not be null");
}
```

### isNull

Verifies that a value is null.

**Syntax:** `@.Assert.isNull(value, message?)`

```isl
@test
fun test_isNull() {
    $var1: null;
    @.Assert.isNull($var1, "Value should be null");
}
```

## Comparisons

| Assertion | Description |
|-----------|-------------|
| `@.Assert.lessThan(a, b)` | a < b |
| `@.Assert.lessThanOrEqual(a, b)` | a <= b |
| `@.Assert.greaterThan(a, b)` | a > b |
| `@.Assert.greaterThanOrEqual(a, b)` | a >= b |

## String and Pattern

| Assertion | Description |
|-----------|-------------|
| `@.Assert.matches(pattern, value)` | value matches regex pattern |
| `@.Assert.notMatches(pattern, value)` | value does not match pattern |
| `@.Assert.contains(expected, actual)` | actual contains expected |
| `@.Assert.notContains(expected, actual)` | actual does not contain expected |
| `@.Assert.startsWith(prefix, value)` | value starts with prefix |
| `@.Assert.notStartsWith(prefix, value)` | value does not start with prefix |
| `@.Assert.endsWith(suffix, value)` | value ends with suffix |
| `@.Assert.notEndsWith(suffix, value)` | value does not end with suffix |

## Membership and Type

| Assertion | Description |
|-----------|-------------|
| `@.Assert.in(value, collection)` | value is in collection |
| `@.Assert.notIn(value, collection)` | value is not in collection |
| `@.Assert.isType(value, type)` | value is of type (e.g. `number`, `string`, `array`, `node`, `date`) |
| `@.Assert.isNotType(value, type)` | value is not of type |

## Custom Failure Message

All assertions accept an optional third parameter for a custom message:

```isl
@test
fun test_withMessage() {
    $var1: 1;
    $var2: 2;
    @.Assert.equal($var1, $var2, "Expected 1 to equal 2 - values mismatch");
}
```

When the assertion fails, the custom message is included in the output.
