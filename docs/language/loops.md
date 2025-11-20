---
title: Loops
parent: Language Reference
nav_order: 4
---

ISL provides the support for loops which help in executing a certain sequence of instructions in a continuous and
repeated manner unless a certain condition is reached. They could also be used to iterate over a collection of elements.

## foreach loop

The `foreach` loop can be used for traversing items in a collection. A `foreach` loop can also be used to generate child
properties.

{% raw %}
```isl
$array: [ 1, 2, 3 ];

$result: {
	lines: foreach $i in $array
		{
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

An example of using the `foreach` loop to transform each payload item of a page into an order.

```isl
// assume list of orders is in the $page.orders
// captures $orders as [ { order } ]
$orders: foreach $o in $page.orders
	# convert each payload into an order
	@.This.Transform( $o )
endfor
```

**To access the current item index**, a variable called `$<name>Index` is automatically created within the foreach loop, where `<name>` is the variable name of the iterator

```isl
$orders: foreach $o in $array
	# The index in this instance is $oIndex
	$currentIndex : $oIndex;
endfor
```

### Filtering in For Loops

Filtering in `foreach` loops can easily be achieved using the [`| filter( condition )` modifier](./modifiers.md#filtering) which is more convenient and more efficient
than an individual `if` statement:

```isl
$orders: foreach $o in $page.orders | filter ( $.total > 0 )
	# convert each payload into an order
	@.This.Transform( $o )
endfor
```

### For Loop Readability

**NOTE:** Generally try to avoid the `foreach/push` pattern

```isl
$lines = []
foreach $item in $invoice.lines
	$transformed = @.This.Transform ( $item )
	$lines = $lines | push( $transformed )
endfor
```

This pattern can be replaced with the cleaner:

```isl
$lines = foreach $item in $invoice.lines
	@.This.Transform ( $item )
endfor
```

Or if you need [filtering](./modifiers.md#filtering):

```isl
$lines = foreach $item in $invoice.lines | filter ( $.total > 0 )
	@.This.Transform ( $item )
endfor
```

## Parallel foreach loops

**Available since**: `ISL 2.4.17`

`foreach` loops over any array can be parallelized in processing (if the underlying host supports it).

**NOTE:** Parallel processing is only supported in Kotlin runtime that's using Kotlin coroutines.

Simple usage:

```isl
$result = parallel foreach $o in $page.orders | filter ( $.total > 0 )
	# convert each payload into an order
	$r = @.This.Transform( $o )

	$r	// return can capture the $r result
endfor
```

The loop above will be executed in parallel on up to `6` different workers. The resulting `$r` at the end will be used to capture one result
and add it to the `$result` array. The order of the original input is respected and the `$result` will contain the results in the same order, even if the actual processing was done in parallel.

Parallel `options` can be used to control the number of maximum parallel workers:

```isl
$result = parallel { workers: 20 } foreach $o in $page.orders | filter ( $.total > 0 )
	# convert each payload into an order
	$r = @.This.Transform( $o )

	$r	// return can capture the $r result
endfor
```

### Enabling Parallel Processing

By default parallel processing is disabled as it's dependent on the host.

**Only Kotlin hosts that run code in coroutines can parallelize foreach statements.**

In order to enable parallel processing in ISL, you need to set a value in `Transformer.maxParallelWorkers`:

```kotlin
Transformer.maxParallelWorkers = 10
```

This enables maximum of 10 workers for ISL parallel processing.

The value can be read and logged from ISL code:

```isl
@.Log.Info("Maximum parallel workers={}", $isl.maxParallelWorkers )
```

By default the value is `1` which disables parallel processing.

### Thread safety

Modifying variables that are outside of the scope of the `parallel foreach` is not allowed. You can read, but you can't write.

**CRITICAL:** An attempt to write to a variable outside of the scope of the foreach will generate an exception and terminate the execution of the script.

For example this script will fail:

```isl
$result = []
parallel { worders: 20 } foreach $o in $page.orders | filter ( $.total > 0 )
	# convert each payload into an order
	$r = @.This.Transform( $o )

	$result = $result | push ( $r )		// this will fail and terminate the script
endfor
```

To capture the result you need to use one of the following two approaches:

```isl

// Option 1 - return a variable from inside the parallel foreach
$result = parallel { worders: 20 } foreach $o in $page.orders | filter ( $.total > 0 )
	# convert each payload into an order
	$r = @.This.Transform( $o )

	// return a variable - these will be captured in $result[]
	$r
endfor


// Option 2 - return a new object
$result = parallel { worders: 20 } foreach $o in $page.orders | filter ( $.total > 0 )
	# convert each payload into an order

	// create and return an object - these will be captured in $result[]
	{
		id: $o.id
	}
endfor
```

## while loop

The `while` loop allows the code to be executed repeatedly based on a given boolean condition. In ISL, the while loop
has an option to pass a parameter `maxLoops` which defines the maximum number of iterations of the loop. By default, the
`maxLoops` is set to 50 and can be increased up to 5000. This has been done to avoid any programmer from running into an
infinite loop.

{% raw %}
```isl
$i: 0
while ( $i < 5 )
    $i: {{ $i + 1 }}
endwhile
result: $i
```

Will output:

```isl
{ "result": 5 }
```

Another example (storing the result of a while loop):

```isl
$i: 1;
result: while ( $i < 4 )
{
    first: $i,
    $i: {{ $i + 1 }}
}
endwhile
```

Will output

```isl
result:
[
    { "first": 1 },
    { "first": 2 },
    { "first": 3 }
]
```

Using `maxLoops` option will restrict the number of iterations to the given value:

```isl
$i: 0
while ( $i < 150 , { maxLoops: 100 })
    $i: {{ $i + 1 }}
endwhile
result: $i
```
{% endraw %}

Will output:

```isl
{ "result": 100 }
```
