---
title: JSON DSL (Kotlin)
nav_order: 10
description: "Use ISL's JSON DSL for Kotlin to easily create JSON objects and arrays with a clean, type-safe syntax instead of verbose factory methods."
excerpt: "Use ISL's JSON DSL for Kotlin to easily create JSON objects and arrays with a clean, type-safe syntax instead of verbose factory methods."
---

When using ISL from Kotlin you can benefit from Kotlin's custom DSL capabilities and use the JSON DSL to easily create JSON Objects and Arrays without using the `JsonNodeFactory.instance.objectNode()` and other 
factory specific functions.

## Example
```kotlin
import com.intuit.isl.dsl.*

// create a json node
val value = node {
    put ("first name", "John")
    put ("last name", "Smith")
    put ("age", 23 )

    array ("addresses")[
        node ("work"){
            put("line1", "123 Intuit Way")
        }
    ]
}

val jsonNode = value.node;  // access the internal JsonNode that was generated

// create an array with two items
val items = array [
        node ("work"){
            put("line1", "123 Intuit Way")
        }

        node ("home"){
            put("line1", "123 Intuit Way")
        }
    ]

val jsonNode = value.node;  // access the internal JsonNode that was generated

```

## Usage
- `array[ ... ]` - start a new json array `[ ... ]`
- `node{ ... }` - start a json object node `{ ... }`
    - `put(name, value)` - put a property with a value
    - `node(name) { ... }` - add a property as a new object with `name` as the property key
    - `merge( node )` - merge the properties of another object into the current object
- `array( name )[ ... ]` - add a property as a new array with name as the property key
    - `add( value )` - add a value to the array
    - `addAll( list )` - add a full list to the array
    - `node { ... }` - add a new object to the array