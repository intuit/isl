---
title: Objects
parent: Language Reference
nav_order: 2
---

ISL can be used to model all types of objects, in alignment with the JSON format.

Just use the `,` (comma) as the separator and use `" "` (quotes) for property names that have special characters:

## Simple Objects
```isl 
$result: {
	property: true,
	"long-field": "yes"
}
```
Will output:
```json
{
	"property": true,
	"long-field": "yes"
}
```

## Dynamic Property Names
[String Interpolation](../types/strings.md#string-interpolation) can be used to dynamically generate property names on objects for example from a variable name:

```isl
$propName: 'my-custom-property';

$result: {
	property: true,
	`$propName`: "yes"
}
```
Will output:
```json
{
	"property": true,
	"my-custom-property": "yes"
}
```

## Spread Operator
The `...` (spread) operator can be used to deep-copy a complete object or an array into another object/array.

```isl
$input: { value1: true, value2: false }

$result: { 
	...$input,	// copy $input inline
	value3: "yey"
}
```
Will output:
```json
{ 
	"value1": true,
	"value2": false,
	"value3": "yey"
}
```

Multiple objects can be spread in-place:
```isl
$input1: { value1: true, value2: false }
$input2: { value4: true, value5: false }

$result: { 
	...$input1,	
	value3: "yey",
	...$input2
}
```
Will output:
```json
{ 
	// from input1
	"value1": true,
	"value2": false,
	// inline
	"value3": "yey",
	// from input2
	"value4": true,
	"value5": false
}
```


Arrays will be copied in-place:
```isl
$input: [ 1, 2 ]

$result: [ 
	...$input,	// copy $input inline
	3
]
```
Will output:
```json
[ 1, 2, 3]
```


## Inline Ifs
You can also create conditional properties using [inline `if` statements](conditions.md#if-expressions) or [coalesce `??` conditions](conditions.md#coalesce)
```isl 
$result: {
	useStartDate: if ( 2 > 1 ) true else false,
	useEndDate: if ( 1 > 2 ) true else false,
	// there is no else - so no property is generated at all
	useBothDates: if ( 1 > 2 ) true,
	// use result.data or a default value via coalesce
	value: $result.data ?? 'defaultValue',
}
```
Will output:
```json
{
	"useStartDate": true,
	"useEndDate": false,
	// No `useBothDates` property is generated as the false result has no value
	"value": "defaultValue"
}
```

## Switch Case
A [switch case statement](conditions.md) can be embedded inside an object to generate a conditional property
```isl 
$val: 3;

$result: {
	start: switch( $val )
		1 -> "a";
		2 -> "b";
		3 -> "c";
	endswitch
}
```
Will output:
```json
{
	"start": "c"
}
```
Note that the [switch case statement](conditions.md) also supports conditions ( `==`, `!=`, `<=`, `=>`, `<`, `>`, `contains`, `in`, ... ) or Regular Expressions.


## Arrays & For Loops
A `foreach` loop can also be used to generate child properties.
{% raw %}
```isl 
$array: [ 1, 2, 3 ];

$result: {
	lines: foreach $i in $array {
		id: $i,
		total: {{ $i * 10 }}	// math expression
	}
	endfor
}
```
{% endraw %}

Will output:
```json
{
	"lines": [
		{
			"id": 1,
			"total": 10
		},
		{
			"id": 2,
			"total": 20
		},
		{
			"id": 3,
			"total": 30
		}
	]
}
```

Arrays can also be filtered using [`| filter( condition)`](./modifiers.md#filtering),
or [conditional selection](variables.md#conditional-variable-selection).
