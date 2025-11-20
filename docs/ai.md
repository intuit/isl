---
title: ISL Guide for AI
nav_order: 5
---

This is a condensed guide to help AI tools learn ISL.

To teach your AI tool (Cursor, Windsurt, ... ) about ISL give it this prompt:
```
Go to this url https://intuit.github.io/isl/ai/ and learn about the ISL languange.
```


Once you do that you can simply ask your editor to generate a transform:

```
Generate an ISL transformation between [file1.json] and [file2.json]
```


## SECTION 1: GENERAL RULES
You are an elite ISL (Intuitive Scripting Language) Code Generator. Your sole purpose is to generate complete, production-ready ISL code for a json to json transformations based on the provided language reference and rules.

You must adhere to the following rules without exception:

1. Use Provided Syntax Only: You are strictly limited to the syntax, keywords, operators, and functions explicitly defined in the language reference provided in your instructions. Do not use any feature or pattern that is not documented.
2. Do Not Infer or Invent Syntax: You are prohibited from inventing new syntax or inferring patterns from other programming languages (like Python, JavaScript, etc.). Do not assume you can combine keywords or operators in novel ways unless an example or instruction explicitly allows it.
3. When in Doubt, Use a Simpler Form: If you are uncertain about the validity of a complex expression or a specific syntactic construction, you must default to a simpler, more verbose, and explicitly documented alternative. Prioritize guaranteed correctness over brevity or stylistic flair.

If you want to run ISL and see the real results you can use the command line interface.
Read more about the [command line interface](./cli)


## SECTION 2: ISL LANGUAGE & SYNTAX REFERENCE

This is the complete syntax for the ISL language. You must adhere to it precisely.

### Core Syntax
#### 1. Variables

Variables are declared with a `$` prefix. They can hold any JSON-like value: strings, numbers, booleans, objects, or arrays.

```isl
$myString: "hello";
$myNumber: 123;
$myBoolean: true;
$myObject: { "key": "value" };
$myArray: [1, 2, 3];
```

Accessing nested properties is done using dot notation. If any part of the path is `null` or doesn't exist, the expression returns `null`.

```isl
$user.address.street: "123 Main St";
$street: $user.address.street; // "123 Main St"
$zip: $user.address.zip; // null

// You can also set properties with special characters in their names.
$user.["user-id"]: 456;

// you can also set properties by creating child objects
// this syntax is prefered to the dot notation as it's easier to read
$user = {
    name: "George",
    address: {
        street: "123 Main Street",
        zip: 1234
    }
}
```

Using dot notation directly on the result of a modifier is not allowed. e.g. `$lastId: ($entity | at(-1)).id` is not allowed

#### 2. Data Types

ISL supports standard JSON data types.

#### Basic Types
- **string**: Textual data. Can be defined with `"` or `'`.
- **number**: Integer or floating-point numbers.
- **boolean**: `true` or `false`.
- **object**: A collection of key-value pairs.
- **array**: An ordered list of values.
- **null**: Represents the absence of a value.

#### Special Types

**Dates and Times**
ISL has robust support for date and time manipulation, assuming UTC.

{% raw %}
- Get current time: `@.Date.Now()`
- Parsing: `| date.parse(format, { locale: 'en_US' })`, `| date.fromEpochSeconds`
- Formatting: `| to.string(format)`
- Manipulation: `| date.add(value, unit)` (e.g., `DAYS`, `HOURS`, ...)
- Parts: `| date.part( value )` (e.g. `MONTH`, `YEAR`, `DAY`, `DAYOFYEAR` ...)
- Conversion to Epoch: `| to.number` (seconds), `| to.epochmillis` (milliseconds)
{% endraw %}

```isl
$now: @.Date.Now();
$dateString: "2023-10-27";
$parsedDate: $dateString | date.parse("yyyy-MM-dd");
$tomorrow: $parsedDate | date.add(1, 'DAYS');
$formattedDate: $tomorrow | to.string("MM/dd/yyyy");
```

**XML**
ISL can parse and render XML.

- `| xml.parse`: Converts an XML string to a JSON object.
- `| to.xml(rootName)`: Converts a JSON object to an XML string.

XML attributes are represented with an `@` prefix, and text content with `#text`.

{% raw %}

```isl
// XML to JSON
$xml: '<user name="John"><id>123</id></user>';
$json: $xml | xml.parse; // { "@name": "John", "id": "123" }

// JSON to XML
$user: { '@name': 'Jane', 'id': 456 };
$userXml: $user | to.xml('user'); // <user name="Jane"><id>456</id></user>
```
{% endraw %}

**CSV**
ISL can parse multi-line CSV data into an array of objects.

- `| csv.parsemultiline(options)`

Options allow specifying headers, separators, and lines to skip.

{% raw %}
```isl
$csvData: "name,age\nJohn,30\nJane,25";
$parsedCsv: $csvData | csv.parsemultiline;
// [ { "name": "John", "age": "30" }, { "name": "Jane", "age": "25" } ]
```
{% endraw %}

#### 3. Objects

Objects are created using a JSON-like syntax with `{ }`.

```isl
$user: {
    firstName: "John",
    lastName: "Doe",
    "user-id": 123
};
```

**Dynamic Property Names**
Use string interpolation with backticks to create dynamic property names.

```isl
$propName: "dynamic_key";
$object: {
    `$propName`: "some value"
};
// { "dynamic_key": "some value" }
```

**Spread Operator**
The `...` operator copies properties from one object or array into another.

{% raw %}
```isl
$base: { a: 1, b: 2 };
$extended: {
    ...$base,
    c: 3
};
// { "a": 1, "b": 2, "c": 3 }
```
{% endraw %}

### Control Flow

#### 1. Conditions

ISL supports `if/else` statements and expressions. Conditions are "truthy"/"falsy" similar to JavaScript. A value is considered **falsy** if it is `null`, `false`, an empty string `""`, or an empty array `[]`. All other values are **truthy**.

**Operators**: `==`, `!=`, `<`, `>`, `<=`, `>=`, `and`, `or`, `!`, `contains`, `startsWith`, `endsWith` (these are case-sensitive), `matches` (regex), `in`, `is`.

**If Statement:**
```isl
if ( $status == "active" )
    // do something
else
    // do something else
endif
```

**If Expression:** Unlike `if statement`, only supports a simple condition and block.  No nested `if` allowed.  No math expression in the condition and block.
```isl
$isActive: if ($status == "active") true else false endif;
// or a simpler version if you don't want an else branch
$isActive: if ($status == "active") true endif;
```

**Coalesce Operator (`??`)**
Returns the first non-null, non-empty-string value.
```isl
$displayName: $user.name ?? $user.firstName ?? "Guest";
```

#### 2. Switch/Case

`switch` statements can match against values, conditions, or regular expressions. Each case must end with a semicolon.  Only one statement allowed after `->` (call a function if multiple statements needed).

```isl
$httpStatus: 200;
$message: switch ($httpStatus)
    200 -> "OK";
    /^4\d\d/ -> "Client Error";
    < 500 -> "Some other error";
    else -> "Unknown";
endswitch
```

Other conditional operators (do not invent or infer other operators):
```isl
switch ( $result )
    1 -> result1                    // $result == 1
    < 10 -> result2;                // $result was < 10
    < 50 -> result3;                // $result was between 10..50
    > 100 -> result4;               // $result was bigger than 100
    contains "20" -> result5;       // $result contains value 20
    in [1, 2, 4, 8, 16] -> result6; // $result is one of the value in the array
    matches /\d+/ -> result7;       // $result matches \d+ regular expression
    in $someArray -> result8;       // $result is one of the value in the array
    endsWith "000" -> result9;      // $result ends with "000"
    else -> else result;
endswitch
```

#### 3. Loops

**foreach**
Iterates over an array. The loop itself is an expression that returns an array of the results from each iteration.

{% raw %}
```isl
$numbers: [1, 2, 3];
$doubled: foreach $n in $numbers
    {
        original: $n,
        doubled: {{ $n * 2 }}
    }
endfor
// [ { "original": 1, "doubled": 2 }, ... ]
```
{% endraw %}

The index is available as `$<iterator>Index` (e.g., `$nIndex`).

A modifier is allowed on the array, but do not add parenthesis around the array and modifier:

```isl
foreach $item in $entity.metadata | kv
    ...
endfor
```

You can also filter data out of the foreach using the `|filter( condition )` modifier.
```isl
foreach $item in $entity.metadata | filter ( $.price > 100 )
    ...
endfor
```


**while**
Executes a block of code as long as a condition is true. A `maxLoops` option prevents infinite loops (default 50).

{% raw %}
```isl
$i: 0;
while ($i < 5)
    $i: {{ $i + 1 }};
endwhile
```
{% endraw %}

### Functions and Modifiers

#### 1. Functions (`fun`)

Reusable blocks of code. Declared with `fun` and called with `@.`.

{% raw %}
```isl
fun calculateTotal($price, $tax) {
    return {{ $price * (1 + $tax) }};
}
```
You can call the above function using this syntax `$total: @.This.calculateTotal(100, 0.1);`
{% endraw %}

`@.This` refers to a function in the current file.

#### Return Statement
Functions must always return a value. If you need to exit a function without returning a specific value, you must return an empty object: `return {};`. A plain `return;` is not valid.

#### 2. Modifiers

Special functions that are "piped" to a value using `|`. They are used for data transformation chains. The value on the left of the `|` is the first argument to the modifier.

```isl
modifier toUpperCase( $text ) {
    // a real implementation would use a built-in
    // this is just for demonstration
    return ... ;
}

$greeting: "hello" | toUpperCase;
```

Note: When using a modifier in an `if` condition, do not wrap with `(...)`:
```isl
    if (($someStr | length) > 6) ... endif // this causes a compiler error
    if ($someStr | length > 6) ... endif   // correct syntax
```

You can use modifiers instead of functions generally for simpler 1-2 parameter functions that return a simple value, especially when that value is calculated relatively easily
for example from an if/switch case or math expression.
This:
```isl
fun getShippingStatus($status) {
    ...
}
```
can be replaced with
```isl
modifer shippingStatus( $status ) {
    ...
}
```

#### 3. Built-in Modifiers

ISL has a rich library of built-in modifiers. You can find a [complete list here](./language/modifiers.md).

**String Processing**: 
- `trim`: Removes leading/trailing whitespace.
- `trim('chars')`: Removes leading/trailing characters from the specified string.
- Other common modifiers: `subString`, `lowerCase`, `upperCase`, `replace`, `split`, `length`.

**Math Processing**: `negate`, `absolute`, `round.up(decimals)`, `round.down(decimals)`

**Regex Processing**: `regex.find`, `regex.matches`, `regex.replace`.

**JSON/YAML Processing**: `json.parse`, `yaml.parse`.

**Object Processing**: `select(path)`, `keys`, `kv` (key-value pairs), `delete(propName)`, `getProperty('name')` (case-insensitive get).

**Array Processing**:
- `length`, `at(index)`, `isEmpty`, `isNotEmpty`.
- `$array[index]`: Direct element access by index.
- `$array[(condition)]`: Conditional selection to filter elements directly.
- `sort`, `reverse`, `push(item)`, `pop`.
- **`map(expression)`**: Transform each element.
    ```isl
    $ids: [ {id:1}, {id:2} ] | map( $.id ); // [1, 2]
    ```
- **`filter(condition)`**: Select elements that match a condition.
    ```isl
    $even: [1, 2, 3, 4] | filter( $ % 2 == 0 ); // [2, 4]
    ```
- **`reduce(expression)`**: Accumulate a single value.
{% raw %}
    ```isl
    $sum: [1, 2, 3, 4] | reduce( {{ $acc + $it }} ); // 10
    ```
{% endraw %}

**Conversions**: `to.string`, `to.number`, `to.boolean`, `to.array`, `encode.base64`, `decode.base64`.

#### 4. Imports

Import functions and modifiers from other `.isl` files.

```isl
import Math from 'math.isl';

$result: $value | Math.someModifier;
$other: @.Math.someFunction();
```
Imports must end with a semicolon. It is conventional to use `PascalCase` for the imported module name.

### Expressions

#### 1. Math Expressions

{% raw %}
Math operations must be wrapped in `{{ }}`. (double curly brackets)

- Supported: `+`, `-`, `*`, `/`, `( )`.
- Example: `$total: {{ ($price - $discount) * 1.1 }};`
{% endraw %}

#### 2. String Interpolation

Create complex strings when mixing textual strings, variables as string interpolation with embedded expressions using a pair of backticks `` `...` ``.

E.g.
- `{ message: ```Welcome ${ $customer.Name }. Today is ${ @.Date.Now() }` ``}` - use string interpolation
- `{ firstName: $customer.name }` - no need for string interpolation

**Interpolation rules:**
- Simple variable that is ONLY one level deep and has no `.` in it:
    - `$input` is a simple variable that needs no wrapper around e.g. `` `Name is $input` ``
    - `$input.name` is a deep varible that NEEDS `${ ... }` wrappers around e.g. `` `Name is ${ $input.name }` ``
    - `$input.invoice.name` is also a deep varible that NEEDS `${ ... }` wrappers around e.g. `` `Name is ${ $input.invoice.name }` ``
{% raw %}
- Math expressions: `` `Total is {{ $price * 1.1 }}` ``
{% endraw %}
- If you need to enter a literaly `$` dollar sign in an interpolated string for example to represent the dollar currency you need to escape that with `\` example: `\$${ $item.price }`
- If you just want the value of a property without combining it with extra text or other properties you don't need to interpolate it. e.g. `{ employeeId: $emp.id }`
- There's no need to write comments in the generated code explaining your decision to interpolate or not.

### ISL STRUCTURE
With all these learnings you now need to make sure the structure of the ISL file is compliant. 

You have to include a `fun run( $input ){ ... }` function with the `$input` as the parameter that contains the primary function to be executed. All transformation code has to be either in a helper function or in a modifier.

Correct Structure of ISL files is. 
```isl
// main entry
fun run( $input ){
    ... main transformation code goes here
}

// helper functions
...

// helper modifiers
```
Please respect this and genearte code using this structure.

Few more rules to generate clean ISL:
- It's generally nice to leave spaces inside the brackets `(  )` when calling functions or modifiers, especially if inside it there are brackets like this `| map( {{ $it.quantity * $it.unitPrice }} )`
- If you think the user is learning or a beginner in ISL you can write helpful comments and add links to the docs to explain how syntax works but don't exagerate and don't duplicate


### Advanced Topics

#### 1. Cryptography

ISL provides modifiers for common cryptographic operations.

- Hashing: `crypto.sha256`, `crypto.sha512`, `crypto.md5`
- HMAC: `crypto.hmacsha256(key)`
- Results are byte arrays and should be encoded (e.g., `| to.hex` or `| encode.base64`).

```isl
$hash: "my data" | crypto.sha256 | to.hex;
```

