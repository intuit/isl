---
title: Strings
parent: Data Types
nav_order: 1
description: "ISL string type with support for interpolation, concatenation, and built-in modifiers for uppercase, lowercase, trim, and more."
excerpt: "ISL string type with support for interpolation, concatenation, and built-in modifiers for uppercase, lowercase, trim, and more."
---

The most basic type of ISL is the `string` type which is the default type for all properties and variables.

## Modifiers
See complete list of [String Modifiers](../language/modifiers.md#string-processing).


## String Interpolation
String interpolation is done using the classical backtick ``` ` text ` ``` and  `$` prefix for the interpolated values.

```
$value: `Hi there $name. Today is ${ @.Date.Now() | to.string("yyyy MM dd") }. `.
```

{% raw %}
- Simple variable format `$var` 
- Deep property selection or expressions `${ $var.property }`
- Function calls inside the interpolated expressions `GST amount of ${ @.This.CalculateGST( $line ) }`. 
	Simple calls can be executed directly `GST amount of @.This.CalculateGST( $line )` if there is no need to apply modifiers. 
- [Math expressions](#new-language-features) inside the interpolated expressions `GST amount of {{ $line.qty & $line.amount }}`
- Escaping using `\`: `\$` can be used to enter `$` in the interpolated string.
{% endraw %}

### Dynamic String Interpolation
Use the *merge* modifier to run string interpolation on a dynamically constructed string. For example,

```
    $who = "world"
    $aModifier = "uppercase"
    $message = "Hello ${$who |" | concat($aModifier) | concat("}!")
    return $message | merge
```
results in `Hello WORLD!`

## String Conversions
Any type can be converted to the string representation using `to.string`

`| to.string` - returns a string representation of the input. Dates are converted to the standard 

## Date to String Conversion
Dates can be converted [using a specified format](./dates.md#format-specifiers):

- Default ISO format: `$date = @.Date.Now() | to.string` will output in the standard ISO format of `yyyy-MM-ddTHH:mm:ss.fffZ` > `2021-12-01T00:57:39.910Z`. 
- Custom format: `$date = @.Date.Now() | to.string(yyMMdd)` > `211201`


