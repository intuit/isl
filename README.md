# ISL

<div align="center">
  <img src="./docs/img/isl_small.png" alt="ISL">
</div>

ISL is a low-code, interpreted scripting language designed primarily for JSON-to-JSON transformations, utilizing a WYSIWYG (What You See Is What You Get) approach to make scripting visual and intuitive. 

Operating within a JVM-based runtime container, ISL provides a ready-to-deploy execution environment that allows both developers and non-developers to easily write, test, and integrate their custom code into virtually any service.


⏩ **[Get started now with this Java or Kotlin Hello World](docs/quickstart.md)**

⏩ **[Checkout the Overview](./overview.md) for examples and the basics of the ISL**

✅ ISL has a permissive [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)


## Overview

Originally ISL was designed as a **JSON-to-JSON transformation library** as an [alternative to JOLT](./docs/dev/jolt-vs-isl-benchmark-report.md) and other Java based JSON-to-JSON transformations but since then ISL has evolved into a fully fledged scripting languange while still providing a simple powerful JSON-to-JSON transformation capabilities.

The ISL supports an intuitive simplified syntax with features that make data transformations easy with minimal lines of code.
In addition, the language supports easy extensibility allowing it to be used as a multi-purpose service extensibility language.

The ISL can be embedded in any JVM based project to provide runtime based extensibility through a fast and lightweight runtime.

**If it looks like a JSON it's a valid ISL :)**

![ISL Transformation](./docs/img/simple_transform.png)

Checkout the [Overview](./docs/overview.md) for examples and the basics of the ISL.



## Major Features

- JSON Compatible [Object Building](./docs/language/objects.md) `item: { field: true, "my-property": 12 }`.
- Comprehensive [Conditions, If/Else & Switch statements](./docs/language/conditions.md) including [RegEx Switch Case](./docs/language/conditions.md#regex)
- Script [Imports](./docs/language/functions.md#imports).
- [Math Expressions](./docs/language/math.md) `$total: {{ $amount * $quantity * 1.2 }}`.
- Functional Expressions for:
  - [Filters](./docs/language/modifiers.md#filtering): `$positive: [ -1, 0, 1 ] | filter( $ > 0 )`.
  - [Reduce](./docs/language/modifiers.md#reduce) `$total: [ 1, 2, 3, 4 ] | reduce( {{ $acc + $it }} )`.
  - [Map](./docs/language/modifiers.md#map) `item: [ 1, 2, 3, 4 ] | map( { id : $ } )`.
- Easily Extensible with
  - [functions](./docs/language/functions.md): `@.Service.Function( ... )`.
  - [modifiers](./docs/language/functions.md#modifiers): `| calculate_tax( ... )`.
  - [wildcard modifiers](./docs/language/functions.md#modifiers): `| encode.base64( ... )`.
  - [block statement extensions](./docs/language/functions.md#blockstatement-functions) similar [to pagination support](./docs/advanced/pagination.md) `@.Pagination.Page() { ... block ... }`.
  - [annotations](./docs/language/functions.md#annotations) that work as interceptors `@cache() fun getCurrentUser(){ ... }`
- Pagination Strategies for [Page](./docs/advanced/pagination.md#page) and [Cursor](./docs/advanced/pagination.md#cursor).
- Utilities for dealing with [Time & Dates](./docs/types/dates.md#timedate-processing), [Signatures & Hashing](./docs/advanced/crypto.md#cryptography).
- Support for [parsing XML](./docs/types/xml.md#xml-processing) and [outputting XML](./docs/types/xml.md#xml-output),
  [parsing CSV](./docs/types/csv.md#csv-processing).
- Support for advanced String Interpolation `Hi there $name. Today is ${ @.Date.Now() | to.string("yyyy MM dd") }. `.
- Support for [`find`, `match` and `replace` using Regular Expressions](./docs/language/modifiers.md#regex-processing).

## Documentation

See the [`./docs/`](./docs/) folder for complete ISL documentation.

## Extensibility Points

As a generic runtime, the runtime is designed to be heavily extensible while maintaining the same
simple grammar. Modules, Functions, ISL Modifiers, Custom Functions and Custom Modifiers are a few of
the available existing extension points:

### Custom Functions

Most common extensibility is to provide new services/functions to be callable from inside the script.

**Note:** At the moment the expectation is that all registered services as **async coroutines**
using the `suspend` keyword.

Registration (Kotlin):

```kotlin
companion object{
  fun myExtension(context: FunctionExecuteContext): Any?{
    // do stuff
    val value = context.firstParamer;
    ...
    return "result";
  }
}

// register custom extension function.
// Name has to be in format `Service.Name` and will be callable from code as `@.Service.Name( ... )`
context.registerExtensionMethod("MyService.MyMethod", MyClass::myExtension);

// You can register closure callbacks so you can use any existing state you have
context.registerExtensionMethod("MyService.MyMethod", {
  // it: params of Array<Any?> all the parameters that were passed
  myLocalService.DoStuff ( ... )
});
```

You can now call those functions from the script:

```isl
value: @.MyService.MyMethod( parameters );
```

### Custom Modifiers

In some situations custom modifiers are more appropriate as they make code more succinct and fluent.
Register an extension method with prefix `Modifier`. This will added it to the list of modifiers.

```kotlin
// register custom extension modifier by using the prefix "Modifier."
context.registerExtensionMethod("Modifier.taxAmount", MyClass::calculateTaxAmount);

// Or you can register just the lambda
context.registerExtensionMethod("Modifier.taxAmount", {
  // it: params of Array<Any?> all the parameters that were passed
  return calculatedAmount;
});
```

You now have access to the modifier using the `| modifier` format:

```
value: $price | taxAmount;
```

**Note:** You can override system modifiers if you really want by overriding by name e.g.
registering a modifier with `Modifier.Trim` will override the default `|trim` modifier.

**Note:** Parameters to modifiers are supported. The first parameter you receive in the list of parameters
is always the left-hand-side of the modifier, then all the other passed in parameters.

## Command Line
ISL-CMD is the command line version of ISL that can validate or transform scripts from the command line.

[Read complete documentation](./isl-cmd/README.md).


## How to use the ISL in your own project:

Embedding the ISL in your own Java project to add scripting features is straight forward:
[Java Hello World](docs/java.start.md)

## ISL vs JOLT

In most common scenarios on a 1:1 comparison ISL is about 46% faster than JOLT.

Read the [Detailed benchmark of ISL vs JOLT](./docs/dev/jolt-vs-isl-benchmark-report.md).

| Feature | ISL | JOLT |
|---------|-----|------|
| **Custom Functions** | ✅ Reusable helper functions | ❌ No function support |
| **String Manipulation** | ✅ `trim`, `upperCase`, `lowerCase`, ... | ❌ Limited |
| **Math Operations** | ✅ `precision`, `Math.sum`, expressions, ... | ❌ No math support |
| **Conditionals** | ✅ `if/else`, `when/case`, `if`-expressions logic | ⚠️ Complex syntax |
| **Array Operations** | ✅ `map`, `filter`, `unique`, `sort`, ... | ⚠️ Limited |
| **Type Conversions** | ✅ `to.string`, `to.decimal`, `to.number`, `to.boolean`, ... | ❌ Manual |
| **Content Type Conversions** | ✅ `json`, `csv`, `xsl`, `base64`, ... | ❌ Manual |
| **Crypto Support** | ✅ `hmac`, `sha`, `rsa`,  ... | ❌ Manual |
| **Date Parsing** | ✅ Full date/time support with formatting | ❌ No date support |
| **String Templates** | ✅ Native interpolation | ❌ Workarounds needed |
| **Variables** | ✅ Named variables for clarity | ❌ No variables |
| **Object Spread** | ✅ Spread syntax for objects | ❌ Not available |

And many more features.


## Release Strategy

This repo uses semantic versioning for releases:

- `Impact: Major` > `x.0.0`
- `Impact: Minor` > `0.x.0`
- `Impact: Patch` > `0.0.1`

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Contributing

Eager to contribute to this service? Check out our [Contribution Guidelines](./CONTRIBUTING.md)!

Thank you very much to our contributors:
@corneliutusnea, @arikgdev, @francoisbeaussier, Paulo Miguel Magalhaes, @wilsonchendevelopment, @AaronTheSoftWearEngineer, @andrewPapad and many many others.

