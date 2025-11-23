---
title: Math Expressions
parent: Language Reference
nav_order: 7
---

ISL has basic support for most common math operations.

{% raw %}
Math operations have to be expressed as a Math Expression `{{ expression }}`:
```
$total = {{ $item.quantity * $item.amount - $item.discount }}
```

Math expressions can only contain [variables](./variables.md), [function calls or modifiers](./functions.md) and literal numbers.

Supported Expressions:

- `+`, `-`, `*`, `/`
- `(` and `)`

## Examples
- `{{ $i * 10 }}`
- `{{ ( 1 + 2) * 3 / 4 }}`
{% endraw %}

## Math Functions
- `| negate` - negate the value.
- `| absolute` - returns absolute value.
- `| precision( decimals )` - convert a number to a `decimals` precision using `HALF EVEN`. Preference is to use `round.*` modifiers.
- `| round.up( decimals )`, `| round.down( decimals )`, `| round.ceiling( decimals )`, `| round.floor( decimals )` - round in a specific direction using a `decimals` precision.
- `@.Math.mean`, `@.Math.max`, `@.Math.min` - return the expected values based on the input list. 
    
    Supported usage is `max: @.Math.max(2, 5.1, 3, 1.7, 4, 1.2)` OR `$list:[2, 5.1, 3, 1.7, 4, 1.2]; max: @.Math.max($list)`
    
- `@.Math.mod( value )` - modulo
- `@.Math.Sqrt( value )` - square Root
- `@.Math.randInt( from, to )` - generate a random value between from and to
- `| Math.clamp( min, max )` - clamps a number between min and max values. E.g. `15 | Math.clamp(0, 10)` > `10`.
- `| Math.sum( initialValue )` - sums all numbers in an array. E.g. `[1, 2, 3] | Math.sum(0)` > `6`.
- `| Math.log( base )` - calculates logarithm with specified base. E.g. `100 | Math.log(10)` > `2.0`.
- `| Math.log10` - calculates base-10 logarithm. E.g. `1000 | Math.log10` > `3.0`.
- `| Math.ln` - calculates natural logarithm (base e). E.g. `2.718 | Math.ln` > `~1.0`.
- `| Math.pow( exponent )` - raises number to the specified power. E.g. `2 | Math.pow(3)` > `8.0`.

## Best Practices

To get the most from math optimizations:

1. **Use constants when possible:** `{{ 100 * 0.08 }}` is free at runtime
2. **Avoid unnecessary variables:** Direct constants are faster than variable lookups.
    
    **❌ Using unnecessary variables**
    ```isl
    $gst: 0.1;
    $total = {{ $tax * $gst }}
    ```
    
    **✅ Good - Can be optimized**
    ```isl
    $total = {{ $tax * 0.1 }}
    ```

3. **Simplify expressions:** Simpler expressions optimize better
