---
title: Functions & Modifiers
parent: Language Reference
nav_order: 5
description: "ISL functions and modifiers for extending functionality. Learn how to create custom functions, modifiers, and block statement extensions."
excerpt: "ISL functions and modifiers for extending functionality. Learn how to create custom functions, modifiers, and block statement extensions."
---

ISL supports two types of functions: `fun` as standard functions and `modifier` as modifiers.

- `fun`ctions look and behave like any any normal function and are callable using `@.[source].[functionName]( parameters )` 
  where `[source]` is `this` if calling a function in the local file or the [name of the import](#imports) if calling a function from an imported file.
  ```isl
  // declare the function
  fun transformLineItems( $lines ){
    // transform
  }

  // call then function
  $lines = @.This.transformLineItems( $order.line_items );
  ```
- `modifier`s are special function that are called using the `| [name](parameters)` syntax. The value on the
  left of the modifiers is considered the first parameter of the modifier and all the other parameters come after
  {% raw %}
  ```isl
  modifier addTax( $amount ){
    // math uses {{ }} 
    return {{ $amount * 1.1 }};
  }
  
  {
    // call the modifier. $orderTotal is the $amount in the modifier
    total: $orderTotal | addTax
  }
  ```

  The above example could also use a modifier:
  ```isl
  // declare the modifier
  modifier transformLineItems( $lines ){
    // transform
  }

  // call then the modifier
  $lines = $order.line_items | transformLineItems;
  ```
  {% endraw %}
  
  - Modifier are preferred if the function does a specific action on the first input parameter and always returns one value that could be used as a result or used for another modifier
  - Modifiers are generally easier to read when they have no or few parameters and can be easily chained one after another making code very readable
  - Modifiers don't require brackets `( )` if they are called without parameters
  - Modifiers can also be called using a function syntax to avoid ambiguities 
  `total: $orderTotal | @.This.addTax`

**Note:** ISL has a preference for Camel Case function and modifier names vs snake*case `*`names:`getAmount()`not`get_amount`.



## Functions

Functions can be declared by using the prefix `fun`. One or multiple positional parameters can be specified in the declaration:

```isl
fun name( $param1, $param2 ) { .... }
```

An `.isl` file can have one or multiple functions.

Functions in the same file can be called via `@.This.FunctionName( parameters )`.

As of ISL 2.3.0, recursive ISL calls are allowed.

## Modifiers

Modifiers are special functions than can be _piped_ `|` one after another.

`modifier name( $param1, $param2 ) { .... }` - declare a modifier.

You can use your own modifiers inline inside any expression. First parameter is the left-hand-value in front of the modifier.

{% raw %}
```
// math.isl file
modifier gstAmount( $price ){
    {{ $price * 0.1 }};  // 10%
}
/// ...
gst: $totalPrice | gstAmount;
```
{% endraw %}

### Wildcard Modifiers
**Note:** Wildcard modifier can only be registered from Java/Kotlin and not from other ISL files.

In some scenarios we don't want to register a separate modifier for each situation we want to handle but want to register _wildcard_ modifiers
that can capture a set of modifiers in one allowing a cleaner code and simpler modifier handling:

You can register a wildcard modifier by adding the suffix `.*` e.g. `Modifier.encode.*`. This modifier will then capture
any request for `encode.value` and provide the `value` part as the second parameter.
`$value | encode.base64` will call your method as `encode( $value, 'base64')`

```kotlin
// register wildcard modifier `encode:` by using the prefix "Modifier.encode:*" - note the `*`
context.registerExtensionMethod("Modifier.encode.*", MyClass::encode);
```

You now have access to the modifier using the `| encode.value` format:

```
value: "my text" | encode.base64;
value: "my text" | encode.url;
value: "my text" | encode.urlQueryParam;
value: "my text" | encode.hex;
```

**Note:**: Make sure the verification of the modifier parameter is case-insensitive.

## Annotations

ISL Supports annotations `@name(params)` in front of functions.
Annotations behave like a [Chain of Responsibility](https://en.wikipedia.org/wiki/Chain-of-responsibility_pattern) intercepting the call to a function,
being able to act on the parameters or the result of a function.

```isl
@cache( { key: $id })   // cache using just the $id as the key
fun readProduct( $id, $name ){

}
```

Annotations have to be installed in ISL through the use of Java/Kotlin handlers by the host.

### ISL Annotations [WIP]

Annotations can also be created through ISL code directly:

```isl
// Run a function only if a feature flag is turned on
// $target is the target function
annotation feature( $target, $featureName ){
   // add prefix so make the flag name shorter
   $enabled = // read the flag - maybe from some registered extension like @.MyFlag.Get("featureName")
   if ( $enabled )
      return @target() // call original function
   endif
}

// feature enabled function
@feature ( 'orders.enabled' )
fun retrieveOrders( ... ){

}
```

## Imports

ISL Files can import other ISL Files using the `import Name from file` syntax:

Import all functions from the other file.

```
import Name from "file.isl";
```

You can then call them via `@.Name.FunctionName( $param1, $param2 ... )`.

The default `run` function can be called via `@.Name.Run( ... )`. Importing a module automatically imports the declared modifiers:

```
import Math from 'math.isl';

// Modifiers need to be prefixed with the name of the import
gst: $price | Math.gst;
```

## Block/Statement Functions

The ISL language can also be extended by providing new custom block statements (like the `foreach`) without actually modifying the language itself.

For example you can create your own `@.Do.Magic( iterator, params ) { block of code }`,
and execute the specific block of code as many times as you want and capture the result.

A good example would be a custom pagination or custom loops:

```isl
 // Page is the name of the iterator variable > $Page will be the iterator
 // Provide a list of custom named parameters
 // Run the { } block
 @.Pagination.Page( "Page", { startIndex: 0, pageSize: 100  } ){
     $page = @.Api.Call( {
         Url: `/api/orders`,
         Query: {
             page: $Page.Current,
             size: $Page.pageSize
         }
     } );

     // Return to the @.Pagination any result you want
     return $page;
 }

```

To register statement block extensions simply use the `context.registerStatementMethod`:

Read more about [pagination extensions in ISL](../advanced/pagination.md).

Registration:

```kotlin
companion object{
  fun myPagination(context: ExecutionContext, statements: StatementExecution, params: Array<Any?>): Any?{

    // do your magic

    // call back into the runtime to execute the child block of statements
    val runStatements = statements(context).value;  // run the complete { } block inside

    // exit when ready
  }
}

// register custom statement function.
context.registerStatementMethod("Pagination.Page", MyClass::myPagination);
```
