---
title: Mocking
parent: Unit Testing
grand_parent: Advanced Topics
nav_order: 4
---

## Introduction

The following functions allow for the mocking of behaviour for functions within ISL. This allows us to create unit tests that don't have an external dependency (e.g. `@.Call.Api`)

All mocks will override any existing functionality of the functions mocks.

These are accessible under the `@.Mock` namespace.

## Features

### Basic mocking

We can create a basic mock by specifying the function name, followed by the return value for the function.

```kotlin
@.Mock.Func("function.name", "returnValue")
```

#### Example

```kotlin
@test
fun assert_mock() {
 @.Mock.Func("test.value", 20)
 $var1 : @.Test.Value()
 @.Assert.Equal(20, $var1); // This should be true.
}
```

### Mocking with Parameters

We can specify particular mock return values for specific parameters provided to the mock function.

```kotlin
@.Mock.Func("function.name", "returnValue", "ParamMatch1", "ParamMatch2", ....)
```

#### Example

```kotlin
@test
fun assert_mock() {
 @.Mock.Func("test.value", 20, 4)
 $var1 : @.Test.Value(4)
 $var2 : @.Test.Value(5)
 @.Assert.Equal(20, $var1); // This should be true.
 @.Assert.NotEqual(20, $var2); // This should be true.
}
```

#### Parameter matching behaviour

The parameter matching utilises a loose matching scheme for JSON objects, meaning that it'll only look for the parameters specified during the mocking process. Any additional properties in the input params won't be considered.

```kotlin
@test
fun assert_mock() {
 $paramMatcher = {
    var1: "3",
    var2: "4",
 }
 @.Mock.Func("test.value", 20, $paramMatcher)

 $var1Input = {
    // Same as original parameter
    var1: "3",
    var2: "4",
    // Extra parameters won't be considered
    // in matching process
    var3: "5"
 }

 $var1 : @.Test.Value($var1Input)

 $var2Input = {
    // Different to original parameter
    var1: "5", 
    var2: "1"
 }

 $var2 : @.Test.Value($var2Input)

 $var3Input = {
    // Different to original parameter
    var1: "3", 
    // var2 is missing, so is not a match
    var3: "4",
 }

 $var3 : @.Test.Value($var3Input)

 @.Assert.Equal(20, $var1); // This should be true.
 @.Assert.NotEqual(20, $var2); // This should be true.
 @.Assert.NotEqual(20, $var3); // This should be true.
}
```

### Capturing parameter inputs

We can also obtain all of the parameters passed into the function during the test. The parameters are stored in chronological order.

#### Example

```kotlin
$value = "hello"
$differentValue = "there"

// The mock instance will represent the capture instance that matches the input parameter
$mockInstance = @.Mock.Func('Event.Publish', { ... }, $value) 

// Run the mocked method
@.Event.Publish( $value )
@.Event.Publish( $differentValue )

// validate what was published
// Return latest capture

// we can also get all captures from instance id
$allPublishedToInstance = @.Mock.GetFuncCaptures('Event.Publish', $mockInstance)

// $allPublishedToInstance example
// [
//     { "0": "hello" }
// ]

// we can also get all captures regardless of params
$allPublished = @.Mock.GetFuncCaptures('Event.Publish')

// $allPublished example
// [
//     { "0": "hello" }
//     { "0": "there" }
// ]
```

## Functions

### Func

#### Description

Creates a mock of an ISL function.

#### Syntax

`@.Mock.Func($funcToMock, $returnValue, ...$paramsToMatch)`

- `$funcToMock`: ISL function to mock.
- `$returnValue`: Return value of mock.
- `$paramsToMatch`:  Optional error message to show if assertion fails.
- `Returns`: Unique id of the mock.

#### Example

```kotlin
@test
fun assert_equals() {
 $var1 : 20
 @.Mock.Func("test.value", 20)
 $var1 : @.Test.Value()
 @.Assert.Equal(20, $var1); // This should be true.
}
```

### Annotation

#### Description

Creates a mock of an ISL annotation.

##### Note

Currently has no functionality, but can be used to:

- disable existing annotations which have external dependencies (e.g. `@cache`)
- capture the input parameters

#### Syntax

`@.Mock.Annotation($funcToMock, $returnValue, ...$paramsToMatch)`

- `$funcToMock`: ISL annotation to mock.
- `$returnValue`: Return value of mock.
- `$paramsToMatch`:  Optional error message to show if assertion fails.
- `Returns`: Unique id of the mock.

#### Example

```kotlin
@test
fun assert_equals() {
 $var1 : 20
 @.Mock.Annotation("hello", 20)
 $var1 : @.This.testFunction()
}

// Function using the mocked annotation
@hello
fun testFunction() {
    ...
}
```

### StatementFunc

#### Description

Creates a mock of an ISL statement function.

##### Note

Currently only has ability to capture the input parameters. Will execute the functions within the statement functions regardless of whether or not mock is declared.

#### Syntax

`@.Mock.StatementFunc($funcToMock, $returnValue, ...$paramsToMatch)`

- `$funcToMock`: ISL statement function to mock.
- `$returnValue`: Return value of mock.
- `$paramsToMatch`:  Optional error message to show if assertion fails.
- `Returns`: Unique id of the mock.

#### Example

```kotlin
@test
fun test_function() {
 @.Mock.StatementFunc("test.value")
 $var1 : @.Test.Value() {
    ...
 }
}
```

### GetFuncCaptures

#### Description

Gets the parameters captured by the function mock.

#### Syntax

`@.Mock.GetFuncCaptures($funcToMock, $mockId)`

- `$funcToMock`: Mocked ISL function.
- `$mockId`: Optional id of mock instance. Used to filter capture results.
- `Returns`: Array of captured parameters, ordered chronologically.

#### Example

```kotlin
@test
fun test_function() {
 $value = "hello"
 $differentValue = "there"

 // The mock instance will represent the capture instance that matches the input parameter
 $mockInstance = @.Mock.Func('Event.Publish', { ... }, $value) 
 
 // Run the mocked method
 @.Event.Publish( $value )
 @.Event.Publish( $differentValue )
 
 // validate what was published
 // Return latest capture
 
 // we can also get all captures from instance id
 $allPublishedToInstance = @.Mock.GetFuncCaptures('Event.Publish', $mockInstance)

 // $allPublishedToInstance example
 // [
 //     { "0": "hello" }
 // ]
 
 // we can also get all captures regardless of params
 $allPublished = @.Mock.GetFuncCaptures('Event.Publish')

 // $allPublished example
 // [
 //     { "0": "hello" }
 //     { "0": "there" }
 // ]
}
```

### GetAnnotationCaptures

#### Description

Gets the parameters captured by the annotation mock.

#### Syntax

`@.Mock.GetAnnotationCaptures($funcToMock, $mockId)`

- `$funcToMock`: Mocked ISL annotation.
- `$mockId`: Optional id of mock instance. Used to filter capture results.
- `Returns`: Array of captured parameters, ordered chronologically.

#### Example

```kotlin
@test
fun test_function() {
 $value = 1
 $differentValue = 2

 // The mock instance will represent the capture instance that matches the mock
 $mockInstance = @.Mock.Annotation("hello", $value)
 
 // Run the mocked method
 @.Test.Value($value)
 @.Test.Value($differentValue)
 
 // validate what was published
 // Return latest capture
 
 // we can also get all captures from instance id
 $allPublishedToInstance = @.Mock.GetAnnotationCaptures("hello", $mockInstance)

 // $allPublishedToInstance example
 // [
 //     { "0": 1 }
 // ]
 
 
 // we can also get all captures regardless of params
 $allPublished = @.Mock.GetAnnotationCaptures("hello")

 // $allPublished example
 // [
 //     { "0": 1 },
 //     { "0": 2 },
 // ]

}

// Function using the mocked annotation
@hello(1)
fun testFunction() {
    ...
}
```

### GetStatementFuncCaptures

#### Description

Gets the parameters captured by the statement function mock.

#### Syntax

`@.Mock.GetStatementFuncCaptures($funcToMock, $mockId)`

- `$funcToMock`: Mocked ISL statement function.
- `$mockId`: Optional id of mock instance. Used to filter capture results.
- `Returns`: Array of captured parameters, ordered chronologically.

#### Example

```kotlin
@test
fun test_function() {
 $value = "hello"
 $differentValue = "there"

 // The mock instance will represent the capture instance that matches the input parameter
 $mockInstance = @.Mock.StatementFunc('Event.Publish', { ... }, $value) 
 
 // Run the mocked method
 @.Event.Publish( $value ) {
    ...
 }
 // Run the mocked method
 @.Event.Publish( $differentValue ) {
    ...
 }
 
 // validate what was published
 // Return latest capture
 
 // we can also get all captures from instance id
 $allPublishedToInstance = @.Mock.GetStatementFuncCaptures('Event.Publish', $mockInstance)

 // $allPublishedToInstance example
 // [
 //     { "0": "hello" }
 // ]
 
 // we can also get all captures regardless of params
 $allPublished = @.Mock.GetStatementFuncCaptures('Event.Publish')

 // $allPublished example
 // [
 //     { "0": "hello" },
 //     { "0": "there" },
 // ]
}
```
