---
title: Test Annotations
parent: Unit Testing
grand_parent: Advanced Topics
nav_order: 3
---

## Test

To create a unit test, we need to utilise the `@test` annotation to denote that the function
is a unit test.

```kotlin
@test
fun test_addNumbers() {
 ...
}
```

In the test function, we can write regular ISL code, along with additional functions specific for testing capabilities.

## Setup

We can also use the `@setup` annotation to denote actions we wish to repeat for each unit tests (e.g. setup of something specific)

```kotlin
@setup
fun setup() {
 // Perform actions required before executing test
}


@test
fun test_addNumbers() {
 ...
}
```
