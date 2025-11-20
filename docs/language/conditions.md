---
title: Conditions
parent: Language Reference
nav_order: 3
---

ISL conditions can be expressed as a statement, an expression that returns a value or as a coalesce operator.

## Conditions
All conditions in ISL evaluate in the same way:

- `if ( $value )` - value _true-ish_. Anything that exists is valid. `null`/Empty string returns false. Existing but empty Array returns false. Boolean `false` return false.
- `if ( !$value )` - value _false-ish_. The exact reverse of the above.
- `==`, `!=`, `<=`, `=>`, `<`, `>`.
- `contains`, `!contains`, `startsWith`, `!startsWith`, `endsWith`, `!endsWith` (operators are case sensitive)
- `matches` for regex matches `if( $var matches /^2\d\d/ )`. The RegEx expressions needs to be enclosed in between slashes `/expression/`
- `and`, `or` and `(` and `)` supported.
- `contains`  for checking if a value exists in an array `if ( ['open', 'closed'] contains $status )`
- `in` which is similar to `contains` but in the other direction `if ( $status in ['open', 'closed'] )`. This is also powerful in a `switch` statement.
- `is` (and `!is` ) tests that a value is of a specific type. Supported types are `number`, `date`, `string`, `node` (object), or `array`
    
## Data Conversions
- The condition comparison will try to bring both operators to the same type.
- Numbers are compared as `BigDecimal`s.
- Arrays are converted to their comma separated string representation except when used on the left of the `contains` condition.

## Simplified Conditions
ISL Conditions do not fail if a property on the evaluation path is missing, thus there's no need to create an `if` and test every property:
```isl
if ( $transaction and $transaction.transaction_info and $transaction.transaction_info.fee_amount and $transaction.transaction_info.fee_amount.value )
```

Can be simplified to:
```isl
if ( $transaction.transaction_info.fee_amount.value )
```
If anything along the way is null, the result will be false.


## Coalesce
ISL supports the coalesce operator `left ?? right`  that returns the first valid value:

E.g.
```
$var1: null;
$var2: "abc":
result: $var1 ?? $var2;
```
Evaluates to: 
```
{ "result": "abc" }
```

Coalesce operators can be also be chained:
E.g.
```
$var1: null;
$var2: "":
result: $var1 ?? $var2 ?? '123';
```
Evaluates to: 
```
{ "result": "123" }
```


## If/Else Statements
ISL support complex `if/else` conditions with support for `and/or`, `(` and `)` for expression groups and `!` (not) operators:

```
if (condition and/or conditions)
    statements
else
    statements
endif
```


### If Expressions
An If statement can be assigned as an expression to a property or variable:
```
prop: if ( true ) "123" else "456" endif;
$var: if ( true ) "123" else "456" endif;
```

If the condition is evaluated to _false-ish_ and there is no `else` 
branch then the property will not be created at all:

```
$result: {
	prop: if ( false ) "123" endif;
}
```
`prop` will not be created at all.

### If Modifiers
`if` statements can also be used in modifiers to condition the running of a specific modifier.
If the `if` evaluates to false, the previous value in the modifier chain is passed down to the next modifier:

In order to keep the visual format clean the `| if ( condition ) modifier` does not currently support an `else` branch.

```
$text: " a b c ";
result1: $text | if ( $text contains "d" ) trim;	// result: " a b c "
result2: $text | if ( $text contains "c" ) trim;	// result: "a b c"
```

If modifiers can also be applied on the value of the previous modifier or value in the chain using the `$` variable which represents the value of the previous modifier in the chain.
```
val: [ 1, 2, 3 ]
	| if ( $ !contains 4 ) push( 4 )	// will add 4
	| if ( $ !contains 4 ) push( 4 )	// won't add a second 4

// > items: [ 1, 2, 3, 4 ]
```

`if` modifiers can be chained:
```
$val: 4;
items: [ 1, 2, 3 ]
	| if ( $val ) push( $val )
	| if ( $unknown ) push( 5 )      // no var -> false
	| if ( ! $someNull ) push( 7 )   // !null -> false
	| if ( $val < 5  ) push( 8 )
	| if ( $val > 5  ) push( 9 )    // false -> false
	| if ( $val == 4 ) push( 10 )
```
Result: `{ "items": [ 1, 2, 3, 4, 7, 8, 10 ] }`


## Switch/Case Statements
Switch case using value conditions or Regular Expressions:
```
switch ( $value ) 
    123 -> result1;
    234 -> result2;
    else -> result3;
endswitch
```

### Case Conditions
The case conditions can be done against a value and can contain a condition operator ( `==`, `!=`, `<=`, `=>`, `<`, `>`, ...)
```
switch ( $result )
    1 -> result1                    // $result == 1
    < 10 -> result2;                // $result was < 10
    < 50 -> result3;                // $result was between 10..50
    > 100 -> result4;               // $result was bigger than 100
    contains "20" -> result5;       // $result contains value 20
    in [1, 2, 4, 8, 16] -> result6; // $result is one of the value in the array
    in $someArray -> result7;       // $result is one of the value in the array
    endsWith "000" -> result8;      // $result ends with "000"
    else -> else result;
endswitch
```

### RegEx Conditions
The case conditions can also be done using a Regular Expression `/regex/`:
```
switch ( $httpStatus ) 
    /^2\d\d/ -> result200;  // 2xx result
    /^4\d\d/ -> result400;  // 4xx result
    else -> result;
endswitch
```
All RegEx evaluations are case insensitive. 
