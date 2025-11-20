---
title: Assertions
parent: Unit Testing
grand_parent: Advanced Topics
nav_order: 2
---

In order to verify values within the unit tests, we can utilise the Assertion framework.

These are accessible under the `@.Assert` namespace.

## Equal

### Description

Verifies whether or not values are equal.

### Syntax

`@.Assert.Equal($expectedValue, $actualValue, $msg)`

- `$expectedValue`: Expected value.
- `$actualValue`: Actual value to verify against.
- `$msg`:  Optional error message to show if assertion fails.

### Example

```kotlin
@test
fun assert_equals() {
 $var1 : 20
 @.Assert.Equal(40, $var1, "Values don't match.");
}
```

## NotEqual

### Description

Verifies whether or not values are not equal.

### Syntax

`@.Assert.NotEqual($expectedValue, $actualValue, $msg)`

- `$expectedValue`: Expected value.
- `$actualValue`: Actual value to verify against.
- `$msg`: Optional error message to show if assertion fails.

### Example

```kotlin
@test
fun assert_equals() {
 $var1 : 20
 @.Assert.NotEqual(40, $var1, "Values match.");
}
```

## NotNull

### Description

Verifies whether or not value is not null.

### Syntax

`@.Assert.NotNull($value, $msg)`

- `$value`: Expected value.
- `$msg`: Optional error message to show if assertion fails.

### Example

```kotlin
@test
fun assert_equals() {
 $var1 : null
 @.Assert.NotNull($var1, "Value is null");
}
```

## IsNull

### Description

Verifies whether or not value is null.

### Syntax

`@.Assert.IsNull($value, $msg)`

- `$value`: Expected value.
- `$msg`: Optional error message to show if assertion fails.

### Example

```kotlin
@test
fun assert_equals() {
 $var1 : null
 @.Assert.NotNull($var1, "Value not null");
}
```
