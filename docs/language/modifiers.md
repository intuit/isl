---
title: Built-in Modifiers
parent: Language Reference
nav_order: 6
---

[Modifiers are always used](./functions.md#modifiers) after a statement using the `|` (pipe) separator. They should be in general used for simple conversions or simple data processing.

The application of a modifiers can be conditioned using an `if` statement: `| if (condition) modifier(params)`. See detailed [If Modifiers documentation](conditions.md#if-modifiers).

If you need more complex operations then please prefer a `function` to a modifier.

## String Processing
1. `| trim` - trim standard white spaces from both ends of a string. Alternative version of `| trimStart` and `| trimEnd` exist.
1. `| trim( "list of chars to trim" )`  - trim all chars in the list. Alternative version of `| trimEnd ( "list of chars to trim" )` and `| trimEnd ( "list of chars to trim" )` exist.
1. `| subString( startIndex, endIndex )` - substring from `startIndex` to `endIndex`.
1. `| subStringAfter( 'after this string' )` - substring from first occurrence of `after this string` until end of the string.
1. `| subStringUpto( 'up to this string' )` - substring from start of the string until the last occurrence of `up to string`.
1. `| sanitizeTid` - ensures that the tid is in proper UUID format and if not, generates a valid UUID value for tid.
1. `| concat( param )` or `| contact( param, delim )` - concatenate the string and allows for an optional delimiter. Will generate a warning and recommend using [string interpolation](../types/strings.md#string-interpolation).
1. `| append( param1, param2, ... )` - append one or multiple values to a string. This is a much more efficient version of `| concat` that allows you to concatenate multiple values to one string but it does not have a delimiter supported. Use this instead of chaining multiple `| concat( ) | concat( ) | ..` together.
1. `| lowerCase` - return lowercase representation of the input string.
1. `| upperCase` - return uppercase representation of the input string.
1. `| capitalize` - capitalizes the first letter of the string. E.g. `"hello" | capitalize` > `"Hello"`.
1. `| titleCase` - converts string to title case (first letter of each word capitalized). E.g. `"hello world" | titleCase` > `"Hello World"`.
1. `| camelCase` - converts string to camelCase. E.g. `"hello world" | camelCase` > `"helloWorld"`.
1. `| snakeCase` - converts string to snake_case. E.g. `"hello world" | snakeCase` > `"hello_world"`.
1. `| reverse` - reverses the characters in the string. E.g. `"hello" | reverse` > `"olleh"`.
1. `| padStart( length, padString )` - pads the string from the start to reach the specified length. E.g. `"5" | padStart(3, "0")` > `"005"`.
1. `| padEnd( length, padString )` - pads the string from the end to reach the specified length. E.g. `"5" | padEnd(3, "0")` > `"500"`.
1. `| truncate( maxLength )` - truncates the string to the specified maximum length. E.g. `"Hello World" | truncate(5)` > `"Hello"`.
1. `| html.escape` - escapes HTML special characters. E.g. `"<div>" | html.escape` > `"&lt;div&gt;"`.
1. `| html.unescape` - unescapes HTML entities. E.g. `"&lt;div&gt;" | html.unescape` > `"<div>"`.
1. `| remove('removal')` - return modified string where `removal` is removed from the input string.
1. `| replace( 'replaceWhat', 'replaceWith' )` - returns modified string where the `replaceWhat` part is replaced with `replaceWith` part in the input string.
1. `| cap ( length )` or `| left( length )` - returns modified string up to the specified position `length`, which is equivalent of a `substring(0, length)` of the input string. E.g. `"George | left(3)" > "Geo"`.
1. `| right ( length )` - returns modified string up to the specified position `length` backwards from the end. E.g. `"George | right(3)" > "rge"`.
1. `| split ( delimiter )` - splits a string based on a specified delimiter (default is `,`).
1. `| length` - returns the length of the string.
1. `| sort` or `| sort( {order: "asc/desc", caseSensitive: true/false} )` - sort the characters of the string. The defaults are `order: "asc"` and `caseSensitive: true`.
1. `| merge` to run [string interpolation](../types/strings.md#dynamic-string-interpolation) on a dynamically constructed string.

## RegEx Processing
- `| regex.find( pattern, options )` - returns an array of all matching patterns in the string. 
- `| regex.matches( pattern, options )` - checks if the pattern exists in the string and returns a boolean value.
- `| regex.replace( pattern, replacementText, options )` - replaces the matched pattern with replacementText and returns the modified string.
- `| regex.replaceFirst( pattern, replacementText, options )` - replaces the matched pattern with replacementText once and returns then modified string.

**Available Options:**
 - `{ multiLine: true/false }` match not only at the beginning and the end of the string, but start/end of each line.
 - `{ ignoreCase: true/false }` for case-insensitive matching.
 - `{ comments: true/false }` when regex contains comments that have to be ignored during the execution stage.

Expressions:
```
// using find
result1: "a b c a b c" | regex.find( "(\s.?\s)" )
// using find with 'ignoreCase' option
result2: "test CAseInSensiTive strings" | regex.find( "caseinsensitive", { ignoreCase: true })
// using find with 'multiLine' option
result3: "1234 is a number\n1432 is also a number" | regex.find( "^([0-9]+).*", { multiLine: true })
// using matches with comments option
result4: "Hello world" | regex.find( "world #some comment", { comments: true })
// using multiple options
result5: "This is a string\nthis is also a string" | regex.find( "this", { multiLine: true, ignoreCase: true })
// using replace
result6: "The quick Blue fox." | regex.replace( "blue", "brown", { ignoreCase: true })
// using named groups in find
result7: "a b  c d b e b c" | regex.find( "(?<first>\sb\s)(?<second>\sc\s)*" ) 
// using replaceFirst
result8: "aa aaaaab aab aaaab" | regex.replaceFirst( "a+b" )
```

Evaluates to:
```
{
	"result1": [ " b ", " a " ],
	"result2": [ "CAseInSensiTive" ],
	"result3": [ "1234", "1424" ],
	"result4": "world",
	"result5": [ "This", "this" ],
	"result6": "The quick brown fox.",
	"result7": [{ "first": " b ", "second": " c "}, { "first": " b "}, {"first": " b "}]
	"result8": "aa  aab aaaab"
}
```

## Json Processing
`| json.parse` - parses a text and converts it to a JSON payload.

```
$t = "{\"x\":\"1\",\"y\":\"2\"}";
result = $t | json.parse;

```
Evaluates to:
```json
{
	"x": "1",
	"y": "2"
}
```

`| json.parse` - returns `null` when parsing an invalid json text.

```isl
$t: "abc";
$result: $t | json.parse;
```
Evaluates to:
```json
null
```

## Yaml Processing
`| yaml.parse` - parses a Yaml text and converts it to a JSON payload.
```
$t = "info: 
		title: test
		version: 1.0.2";
result = $t | yaml.parse;
```
Evaluates to:
```json
{
	"info": {
		"title": "test",
		"version": "1.0.2"
	}
}
```

`| yaml.parse` - returns `null` when parsing an invalid Yaml text.

```isl
$t: "a: b:";
$result: $t | yaml.parse;
```
Evaluates to:
```json
null
```

## Object Processing
- `| select ( path )` - select the `path` from a JSON object. The path can be any valid path and can be interpolated allowing the usage of dynamically generated paths `$result = $value | select( ``$.items[ $index ]`` )`.

- `| keys` - returns an array with all the `keys` from an object. Can be used to introspect an object.

	```
	$value = { a: 1, b: 2 };

	$k = $value | keys;	// [ "a", "b" ]

	foreach $k in $value | keys 
		$v = $value | getProperty( $k ) 
	endfor
	```

- `| kv` - returns an array with all the keys and values from an object. Each element will have `.key` and `.value`. Can be used to introspect an object:
	```
   	$value = { a: 1, b: 2 };

	$k = $value | kv;	// [ { "key": "a", "value": "1" }, { "key": "b", "value": "2" } ]

	foreach $kv in $value | kv
		key: $k.key
		val: $k.value
	endfor
   	```
	The reverse of this call is `| to.object` which will convert an array of `[ { key: key, value: value } ]` to an object.

- `| sort` or `| sort(order: "asc/desc", caseSensitive: true/false)` - sorts the keys of the object. The defaults are `order: "asc"` and `caseSensitive: true`.
	```
   	$value = { b: 2, c: 3, a: 1 };
	$sorted = $value | sort;	// { a: 1, b: 2, c:3 }
	```

- `| join.string( keySeparator, valueSeparator )`, `| join.path( keySeparator, valueSeparator )`, `join.query( keySeparator, valueSeparator )` - joins all the properties of an object into a single string using the 
	specified `keySeparator` (default of `,`) between the keys and the `valueSeparator` (default of `=`) between the property and the value.
	The specified encoding (e.g. `path`) is also applied as per the [available encodings](#conversions).
```
$value = { a: "1 2", b: "3 4" };

$csv = $value | join.string;	// "a=1 2,b=3 4"
$query = $value | join.query("&","=");		// "a=1+2&b=3+4"
$path = $value | join.path("/","_");		// "a_1%202/b_3%204"
```

- `| delete ( propertyName )` - deletes a specific property from an object. This is a modification in-place
	so it should always be applied to the same object:
```
$value = { a: "1 2", b: "3 4" };
$value = $value | delete ( "b" );	// { a: "1 2" }
```

- `| getProperty( 'property name' )` - get a custom property from the object. This is a **case insensitive** search for the property name.
- `| setProperty( 'property name', $value )` - set a custom property into an object. This is a **case insensitive** set for the property name.
- `$name` - Use string interpolation to generate dynamic property names.
```
$propName = "dynamic";
$value = {
	name: $name,
	`$value`: 123
}
```
will generate:
```json
{
	"name": "name",
	"dynamic": 123 
}
```

- `| pick( 'key1', 'key2', ... )` - creates a new object with only the specified keys from the original object.
```
$value = { a: 1, b: 2, c: 3 };
$result = $value | pick('a', 'c');  // { a: 1, c: 3 }
```

- `| omit( 'key1', 'key2', ... )` - creates a new object excluding the specified keys from the original object.
```
$value = { a: 1, b: 2, c: 3 };
$result = $value | omit('b');  // { a: 1, c: 3 }
```

- `| rename( 'oldKey', 'newKey' )` - renames a key in an object.
```
$value = { a: 1, b: 2 };
$result = $value | rename('a', 'x');  // { x: 1, b: 2 }
```

- `| has( 'key' )` - checks if an object has a specific key, returns `true` or `false`.
```
$value = { a: 1, b: 2 };
$hasA = $value | has('a');  // true
$hasC = $value | has('c');  // false
```

- `| default( defaultValue )` - returns the default value if the input is `null` or empty (empty string, empty array, or empty object).
```
$name = null | default("Unknown");  // "Unknown"
$text = "" | default("N/A");  // "N/A"
$value = "Hello" | default("N/A");  // "Hello"
$items = [] | default(["none"]);  // ["none"]
```


## Array Processing
- `| length` - returns the length of the array.
- `| isEmpty` - returns true if the array is empty.
- `| isNotEmpty` - returns true if the array is not empty.
- `| at( index )` - returns the value at a specific index. E.g. `| at (2)` returns 3-rd item. Index starts at `0`.
- `$array[ index ]` - returns the value at the specific index.
- `@.Array.Range( Start, Count, [Increment]])` - generate an array of `Count` Integers starting from `Start` using the `Increment`. `@.Array.Range( 0, 5 )` generates `[0, 1, 2, 3, 4]`.
- `@.Array.Unique( <list of numbers OR strings> )` - remove all duplicate values from an array and returns a set of unique values.
- `| sort` or `| sort( {order: "asc/desc", caseSensitive: true/false} )` - sorts an array of values (either as text or numbers, depending on the type of the first item in the array). The defaults are `order: "asc"` and `caseSensitive: true`. Note: `caseSensitive` only works on text.
	```
   	$value = [ "b", "c", "a" ];
	$sorted = $value | sort;	// [ "a", "b", "c" ]
	```
	```
   	$value = [ 99, 1, 30 ];
	$sorted = $value | sort;	// [ 1, 30, 99 ]
	```

- `| sort( {by: "propertyname", order: "asc/desc", caseSensitive: true/false} )` - sorts an array of objects by a specific property.
	```
   	$value = [ { b: 8, a: "anna" }, { b: 5, a: "zoe" }, { b: 2, a: "max" } ];
	$sorted = $value | sort( { by: "a", order: "desc"});	// [ { "b": 5, "a": "zoe"}, { "b": 2, "a": "max" }, { "b": 8, "a": "anna" } ]
	```	

- `| join.string( separator )`, `| join.path( separator )`, `join.query( separator )` - joins all the values of an array into a single string using the 
	specified `separator` (default of `,`).
	The specified encoding (e.g. `path`) is also applied as per the [available encodings](#conversions).
	```
   	$value = [ "a b", "c d" ];

	$csv = $value | join.string;	// "a b,c d"
	$query = $value | join.query("&");		// "a+b&c+d"
	$path = $value | join.path("/");	// "a%20b/c%20d"
	```

10. `| push ( value )` - pushes a value at the end of an array.
11. `| pushItems ( array )` - pushes an array at the end of an array (concatenating the two arrays).
12. `| pop` - removes the last value of an array and return it. The array is mutated. You can combine `push` and `pop` to make an array work as a stack.
13. `| reverse` - reverses the order of the items in the array.
14. `| first` - returns the first element of the array. E.g. `[1, 2, 3] | first` > `1`.
15. `| last` - returns the last element of the array. E.g. `[1, 2, 3] | last` > `3`.
16. `| take( n )` - returns the first `n` elements of the array. E.g. `[1, 2, 3, 4, 5] | take(3)` > `[1, 2, 3]`.
17. `| drop( n )` - returns the array without the first `n` elements. E.g. `[1, 2, 3, 4, 5] | drop(2)` > `[3, 4, 5]`.
18. `| indexOf( value )` - returns the index of the first occurrence of the value in the array, or `-1` if not found. E.g. `[1, 2, 3, 2] | indexOf(2)` > `1`.
19. `| lastIndexOf( value )` - returns the index of the last occurrence of the value in the array, or `-1` if not found. E.g. `[1, 2, 3, 2] | lastIndexOf(2)` > `3`.
20. `| chunk( size )` - splits the array into chunks of the specified size. E.g. `[1, 2, 3, 4, 5] | chunk(2)` > `[[1, 2], [3, 4], [5]]`.

### Mapping
The `| map ( statement )` modifier can be used to map data in an array from one shape to another. 
The `$` variable will contain the current element.

Create an object:
```
items: [ 1, 2, 3, 4] | map( { id: $ } );
```
Evaluates to: 
```
"items": [ { "id": 1 }, { "id": 2 },{ "id" : 3 }, { "id": 4 } ]
```

Create simpler arrays from objects:
```
items: [ { amount : 3.1 }, { amount : 4.5 }, { amount: 3 }, { }, { amount: "abc" }, { amount: true } ] | map ( $.amount )
```
Evaluates to: 
```
"items": [ 3.1, 4.5, 3, null, "abc", true ]
```


### Filtering
The `| filter ( condition )` modifier can be used to quickly filter items out of an array. This can be used on an array or as part of a `foreach` statement.
The condition is done against the custom `$fit` filter iterator.

E.g. 
```
items: [ 1, 2, 3, 4] | filter( $fit < 3 );
```
Evaluates to: 
```
"items": [ 1, 2 ]
```

Combine wth the `foreach`. Note that the `filter` is still applied on the `$fit` iterator not on the `foreach` iterator of `$i`.

```
$items: [ 1, 2, 3, 4 ] | filter( $fit < 3 );
result: foreach $i in $items | filter ( $fit < 3 ) 
	{
		value: $i
	}
endfor
```
Evaluates to:
```
"items": [ {
		"value": 1
	}, {
		"value": 2
	}]
```

### Reduce
{% raw %}
The `| reduce ( {{ math expression }} )` modifier can be used to reduce an array to a single value by applying a math expression to the complete array. This can be used to quickly calculate totals or other values across a range of items.

The modifier will receive two variables `$acc` - the accumulator and `$it` the current iterator value.
The value returned will be captured as the new `$acc` to be used on the next iteration. 

E.g.
```
totalWithGst: [ 1, 2, 3, 4 ] | reduce(  {{ $acc + $it * 1.10 }} );
```
Evaluates to:
```json
{ 
	"totalWithGst": 11
}
```

E.g.
```
$lines: [ 
	{ amount : 3.1 }, 
	{ amount : 4.5 }, 
	{ amount: 3 }, 
	{ }, 
	{ amount: "abc" }, 
	{ amount: true } 
];
total: $lines | reduce( {{ $acc + $it.amount * 1.23 }} )
```
Evaluates to:
```json
{ 
	"total": 13.038
}
```
{% endraw %}


## Conversions
1. `| date.parse( format, [{ locale: 'en_AU' }] )` or `| date.parse( [ format1, format2 ], [{ locale: 'en_AU' }] )` - parses a value into a date. The default locale is `en_US` if not specified. It might be necessary to specific a locale if the input string contains language or country specific elements e.g. AM (US) vs am (AU) or Monday (English) vs Lundi (French). See [detailed dates documentation](../types/dates.md).
2. `| to.boolean` - returns boolean representation of the specified input.
3. `| to.number` - returns numeric representation as int/long of the specified input.
4. `| to.decimal` - returns numeric representation as decimal of the specified input.
5. `| to.string` - returns a string representation of the input. Dates are converted to the standard.
   1. `$date = @.Date.Now() | to.string` will output in the standard ISO format of `yyyy-MM-ddTHH:mm:ss.fffZ` > `2021-12-01T00:57:39.910Z`.
   2. You can also specify a custom formatting: `$date = @.Date.Now() | to.string(yyMMdd)` will output `211201`.
6. `| to.hex` - converts the input to a byte array then to a (lowercase) hex representation.
7. `| to.array` - converts the input to an array. If they input is already an array then it is left untouched. `r: 1 | to.array` -> `r: [ 1 ]`.
   Can be used to guarantee that a specific property of an object is an array.
8. `| to.object` - converts an array of `[{ key: k, value: value }]` elements to an object. This is the reverse of the `| kv` modifier.
9. `| to.json` - converts the input to a JSON string representation. E.g. `{ a: 1, b: 2 } | to.json` > `"{\"a\":1,\"b\":2}"`.
10. `| to.yaml` - converts the input to a YAML string representation.
11. `| to.csv` - converts an array of objects to CSV format. E.g. `[{ a: 1, b: 2 }, { a: 3, b: 4 }] | to.csv` > `"a,b\n1,2\n3,4"`.
12.  `| hex.tobinary` - converts a hex string to a binary array.
13.  `| encode.base64` - accepts a string and returns an encoded string based on the specified encoding method, in this example being `base64`.
14. `| encode.string` - just converts to string.
15. `| encode.path` - percentage encoding for paths `"a b" | encode.path` > `a%20b`.
16. `| encode.query` - query based encoding `"a b" | encode.query` > `a+b`.
17. `| decode.base64` - accepts a base64 encoded string and returns a decoded `byte[]` based on the specified decoding method, in this example being `base64`based on the encoding method in this case `base64`. To get the string value of the `byte[]` call `| to.string`.
18. `| decode.query` - decode a query string `"a+b" | decode.query` > `a b`.


## Other Utilities
- `| sanitize_tid` - ensures the specified intuit_tid is in UUID format, if not, returns a valid UUID.
- `@.Run.Sleep( timeInMs )` - sleep a number of ms. When ISL is running in a Java host using native Java threads this will sleep the thread.
- `@.UUID.New()` - generate a new UUID.
- `| retry.when( condition, { retryCount: 3, backOff: false|true, delay: timeinMs, delayFrom: time, delayTo: time  })` - Automatically retry the operation if the condition is true. You have access to the `$` variable as the result of the previous operation. 
	- `condition` - is an ISL condition that will be tested to confirm the retry is required. E.g. `|retry.when( $.status in [500, 502, 503] )`
	- `retryCount: 3` - number of retries to be attempted
	- `backOff: false|true` - whenever to make each retry backOff by 2x the delay
	- `delay: timeinMs` - time to delay the operation. If not specified a random time between 50ms and 500ms will be used.
	- `delayFrom: 50 and delayTo: 500` - a window of delay time. Retry operation will select a random time within that window to retry.
		- Simplest usage is `| retry.when ( $.status in [500, 502, 503] )`
		- To test a group of values you can use [regex match](./conditions.md#regex-conditions) `| retry.when ( $.status matches '4\d\d|5\d\d' )` - match for any `4xx` or `5xx` code.


## Unsupported XForm Modifiers
- `| get('type')` - get a custom attribute. Used  for attributes that can't be read through the normal `$value.name` e.g. `_void` due to parser limitation or attributes based on name like in the `| keys`.
- `| get_key_from_value` - does not seem used anywhere.
- `| unique( [selector] )` - similar to `@.Array.Unique( )` [see above](#array-processing). This modifier allows a field selector that will be used to calculate the uniquness of the objects. 
	- `$items | unique ( $.id )` - return a unique list of objects, using the `$.id` as the unique selector.

- `| mask:accountNumber` or `| mask:creditCardNumber` - apply standard masking.
- `| (custom enum mapping)` - will generate a warning to replace with `| enum:nameofenum` to be explicit about enum value mappings.
- `| normalize_txn_amt_sign( txType )` - normalize amounts based on txType.
    ```
    if ( ($value < 0 AND txType == 'CREDIT') OR ($value > 0 AND txType == 'DEBIT') )
      return - $value;
    else
      return $value;
    endif
    ```

- `| extract_account_number_from_iban` 
- `| open_banking_tokenize` 
- `| open_banking_balance_filter` 
- `| normalized_account_type_map` 
- `| account_category_map` 
- `| open_banking_tokenize` 

## Compression & Decompression
### Zip
1. `@.Zip.Start()` - Initializes a zip archive that will hold the files. This object needs to be piped into any of the following commands.
2. `| zip.add("foo.txt", "string file content")` - Adds a file to be compressed. The file will be named "foo.txt" whose content will be the text "string file content". Uses UTF-8 as the default encoding. Returns the zip object.
3. `| zip.add("foo.txt", "string file content", "Latin-8")` - Same as above except with a different encoding.
4. `| zip.add("bar.bin", $byteArray)` - Adds a binary file to be compressed. Returns the zip object.
5. `| zip.close` - Closes the zip stream and outputs the compressed zip archive.
6. `| gzip()` - Compresses the input using gzip compression, you can also specify the charset. ie. `| gzip("UTF-8")`. The default charset is UTF-8.

Example usage:
```
$z = @.Zip.Start();
$archive: $z | zip.add("foo.txt", "hello world!", "UTF-8") | zip.add("bar.bin", $byteArray) | zip.close
```

### Unzip
1. `| unzip` - Takes a byte array (zipped) and unzips into an array of objects with `name` and `content`. ie. `[{name: 'foo.txt', content: 'foo bar'}]`.
2. `| gunzip` - Accepts a byte array or a binary node and unzips it using gzip decompression. You can also specify the charset. ie. `| gunzip("UTF-8")`. The default charset is UTF-8.
3. `| gunzipTobytes` - Decompresses the input using gzip decompression and returns the decompressed bytes, it accepts both byte array and binary node.

Example usage:
```
$unzipped = $zippedBytes | unzip;
$contentString = $unzipped[0].content | to.string(“utf-8”);
```
