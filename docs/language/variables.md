---
title: Variables
parent: Language Reference
nav_order: 1
description: "Learn how to declare and use variables in ISL. Variables can store values, have custom properties, and be used throughout your transformations."
excerpt: "Learn how to declare and use variables in ISL. Variables can store values, have custom properties, and be used throughout your transformations."
---

## Variable declaration
- `$var: 123` - simple Variable declaration
- `$var.property: 123` - variable with custom property declaration
- `$var.["my-name"]: 123` - variable with a custom property name `my-name`.

Multiple variable declarations with different properties are additive 
however the language preference is to use an object declaration:
```isl
$var.prop1 = 123;
$var.prop2.child1 = 1;
$var.prop2.child2 = 2;
$var.prop2.child3 = 3;
```
Will Output:
```json
{
	"prop1": 123,
	"prop2": {
		"child1": 1,
		"child2": 2,
		"child3": 3
	}
}
```

However the following declaration is **equivalent and the preferred formatting** for readability and performance reasons:
```isl
$var = {
	prop1: 123;
	prop2: {
		child1 = 1;
		child2 = 2;
		child3 = 3;
	}
}
```
Will Output:
```json
{
	"prop1": 123,
	"prop2": {
		"child1": 1,
		"child2": 2,
		"child3": 3
	}
}
```


## Variable Usage & Selection
Variables always behave like full JSON elements (values, objects or arrays).
A variable or a selector will always return null if it can't figure out the full path.

- `$var` - value of the `$var` variable.
- `$var.prop1` - value of the `prop1` property inside the `$var` variable
- `$var[0]` - element `0` in the `$var` array
- `$var.array[0]` - element `0` in the `array` property of the `$var` variable

## JSON Path Variable Selection
Full [JSON Path selection](https://github.com/json-path/JsonPath#path-examples) is possible 
by using [the `| select ( path )` modifier](./modifiers.md#object-processing).

This can be used to select custom indexes from inside an array.
- `$result = $value | select( '$.items[ $index ]' )`

## Array Index selection
Arrays element can be selected from inside an array [using the `| at ( value )` modifier](./modifiers.md#array-processing):

- `$array | at( 2 )` - returns the 3-rd element (arrays are 0 indexed)
- `$array | at( $index )` - returns the `$index` element

Currently `$array[ $index ]` format is not supported.

## Conditional Variable Selection
Variable arrays support conditional selection similar to [the `| filter ( condition )` modifier](./modifiers.md#filtering).

- `$array[( condition )]` - select all items that [respect a condition](./conditions.md).
- `$array[( $.name startsWith 'John' and $.id > 2 )]` - select all elements where the `name` 
	starts with `John` and have an `id > 2`
