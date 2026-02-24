# ISL Language Quick Reference

This guide helps AI assistants and developers understand ISL (Intuitive Scripting Language) syntax and best practices.

## Table of Contents
1. [Quick Start](#quick-start)
2. [Core Syntax](#core-syntax)
3. [Built-in Modifiers](#built-in-modifiers)
4. [Control Flow](#control-flow)
5. [Common Patterns](#common-patterns)
6. [Important Rules](#important-rules)

## Quick Start

Every ISL transformation starts with a `run` function:

```isl
fun run($input) {
    // Transform $input here
    userId: $input.id | to.string;
    userName: `${$input.firstName} ${$input.lastName}`;
    createdDate: $input.timestamp | date.parse("yyyy-MM-dd");
}
```

## Core Syntax

### Variables
Variables start with `$` and can hold any JSON value:
```isl
$name: "John";
$age: 30;
$active: true;
$address: { street: "123 Main St", city: "Boston" };
$items: [1, 2, 3];
```

Access nested properties with dot notation:
```isl
$street: $user.address.street;
$firstItem: $array[0];
```

### Functions
Declare reusable logic with `fun`:
```isl
fun calculateTax($amount, $rate) {
    return {{ $amount * $rate }};
}

// Call with @.This prefix
$tax: @.This.calculateTax($subtotal, 0.08);
```

Functions **must always return a value**. Use `return {};` for empty returns.

### Modifiers
Transform data by piping values through modifiers:
```isl
$cleanName: $input.name | trim | capitalize;
$ids: $items | map($.id) | unique;
$adults: $users | filter($.age >= 18);
```

Create custom modifiers:
```isl
modifier formatCurrency($amount) {
    return `\$${$amount | to.decimal | precision(2)}`;
}

$price: 99.9975 | formatCurrency;  // "$100.00"
```

### Math Expressions
All math operations must be wrapped in `{{ }}`:
```isl
$total: {{ $price * $quantity }};
$average: {{ ($sum / $count) }};
$discount: {{ $amount * 0.1 }};
```

### String Interpolation
Use backticks for string interpolation:
```isl
// Simple variable (no dots)
$greeting: `Hello $firstName`;

// Nested paths (has dots) - needs ${}
$message: `User ${$user.name} logged in`;

// Math expressions
$summary: `Total: \${{ $price * $qty }}`;  // Escape $ for literal
```

### Objects
Create objects with JSON-like syntax:
```isl
$user: {
    id: $input.userId,
    name: `${$input.first} ${$input.last}`,
    active: true
};
```

Use spread operator to merge objects:
```isl
$extended: {
    ...$baseUser,
    role: "admin",
    permissions: ["read", "write"]
};
```

Dynamic property names:
```isl
$key: "dynamicProp";
$obj: { `$key`: "value" };  // { "dynamicProp": "value" }
```

## Built-in Modifiers

### String Modifiers
```isl
$text | trim                      // Remove whitespace
$text | trim("*")                 // Remove specific characters
$text | upperCase                 // UPPERCASE
$text | lowerCase                 // lowercase
$text | capitalize                // Capitalize first letter
$text | titleCase                 // Title Case Each Word
$text | replace("old", "new")     // Replace text
$text | split(",")                // Split into array
$text | subString(0, 10)          // Extract substring
$text | length                    // Get length
$text | padStart(10, "0")         // Pad left
$text | truncate(50, "...")       // Truncate with suffix
$text | default("fallback")       // Default if empty/null
```

### Array Modifiers
```isl
$array | map(expression)          // Transform each element
$array | filter(condition)        // Select matching elements
$array | reduce(expression)       // Aggregate to single value
$array | sort                     // Sort ascending
$array | reverse                  // Reverse order
$array | unique                   // Remove duplicates
$array | first                    // Get first element
$array | last                     // Get last element
$array | at(2)                    // Get element at index
$array | length                   // Get array length
$array | isEmpty                  // Check if empty
$array | push(item)               // Add item to end
$array | pop                      // Remove last item
$array | join(", ")               // Join to string
```

### Conversion Modifiers
```isl
$value | to.string               // Convert to string
$value | to.number               // Convert to integer
$value | to.decimal              // Convert to decimal
$value | to.boolean              // Convert to boolean
$value | to.array                // Convert to array
$value | to.json                 // Serialize to JSON
$value | to.xml("root")          // Convert to XML
```

### Math Modifiers
```isl
$num | absolute                  // Absolute value
$num | negate                    // Negative value
$num | round                     // Round to nearest
$num | precision(2)              // Format decimal places
$num | round.up(2)               // Round up
$num | round.down(2)             // Round down
```

### Date/Time Modifiers
```isl
@.Date.Now()                                          // Current date/time
$str | date.parse("yyyy-MM-dd")                       // Parse date string
$date | date.add(1, "DAYS")                           // Add time unit
$date | date.add(-2, "HOURS")                         // Subtract
$date | date.part("YEAR")                             // Extract part
$date | to.string("MM/dd/yyyy HH:mm:ss")              // Format date
$date | to.number                                     // Unix epoch (seconds)
$date | to.epochmillis                                // Epoch milliseconds
$epochSecs | date.fromEpochSeconds                    // Parse epoch
```

### Object Modifiers
```isl
$obj | keys                      // Get property names
$obj | kv                        // Key-value pairs array
$obj | select("path.to.prop")    // Extract nested value
$obj | delete("propName")        // Remove property
$obj | getProperty("Name")       // Case-insensitive get
```

### Data Format Modifiers
```isl
$str | json.parse                // Parse JSON string
$str | xml.parse                 // Parse XML string
$str | yaml.parse                // Parse YAML string
$str | csv.parsemultiline        // Parse CSV data
$bytes | encode.base64           // Base64 encode
$str | decode.base64             // Base64 decode
$str | to.hex                    // Convert to hex
$hex | from.hex                  // Convert from hex
```

### Cryptographic Modifiers
```isl
$data | crypto.sha256 | to.hex   // SHA-256 hash
$data | crypto.sha512 | to.hex   // SHA-512 hash
$data | crypto.md5 | to.hex      // MD5 hash
$msg | crypto.hmacsha256($key) | encode.base64  // HMAC-SHA256
```

## Control Flow

### If/Else
```isl
// Expression form (simple condition only)
$status: if ($paid) "success" else "pending" endif;
$discount: if ($amount > 100) 0.1 endif;  // else is optional

// Statement form (supports complex logic)
if ($user.role == "admin" and $user.active)
    $permissions: ["read", "write", "delete"];
else
    $permissions: ["read"];
endif
```

### Switch/Case
```isl
$message: switch ($statusCode)
    200 -> "OK";
    404 -> "Not Found";
    /^5\d\d/ -> "Server Error";        // Regex match
    < 300 -> "Success";                // Comparison
    contains "Error" -> "Failed";      // String contains
    in [401, 403] -> "Unauthorized";   // Array membership
    else -> "Unknown";
endswitch
```

### Foreach Loop
```isl
// Basic foreach
$doubled: foreach $n in $numbers
    {{ $n * 2 }}
endfor

// Transform objects
$users: foreach $user in $input.users
    {
        id: $user.id,
        name: `${$user.first} ${$user.last}`,
        index: $userIndex  // Automatic index variable
    }
endfor

// With filter
foreach $item in $items | filter($.price > 100)
    // Process expensive items only
endfor
```

### While Loop
```isl
$i: 0;
$sum: 0;
while ($i < 10)
    $sum: {{ $sum + $i }};
    $i: {{ $i + 1 }};
endwhile
```

### Coalesce Operator
Return first non-null, non-empty value:
```isl
$name: $user.preferredName ?? $user.firstName ?? "Guest";
$email: $contact.email ?? $contact.alternateEmail ?? "";
```

## Common Patterns

### Transform Array of Objects
```isl
items: $input.lineItems | map({
    productId: $.product.id | to.string,
    name: $.product.name | trim | capitalize,
    quantity: $.qty | to.number,
    price: $.unitPrice | to.decimal | precision(2),
    total: {{ $.qty * $.unitPrice }} | precision(2)
})
```

### Filter and Aggregate
```isl
// Get expensive items total
$expensiveTotal: $items 
    | filter($.price > 100)
    | map($.price)
    | reduce({{ $acc + $it }});

// Count active users
$activeCount: $users | filter($.active) | length;
```

### Conditional Object Properties
```isl
{
    id: $input.id,
    name: $input.name,
    // Include email only if present
    ...if ($input.email) { email: $input.email } else {} endif,
    // Include address with default
    address: $input.address ?? "N/A"
}
```

### Complex Nested Transformation
```isl
fun run($input) {
    orderId: $input.order.id | to.string;
    customer: @.This.transformCustomer($input.customer);
    items: $input.order.items | map(@.This.transformItem($));
    summary: @.This.calculateSummary($input.order.items);
}

fun transformCustomer($cust) {
    return {
        id: $cust.id | to.string,
        fullName: `${$cust.firstName} ${$cust.lastName}`,
        email: $cust.email | lowerCase | trim
    };
}

fun transformItem($item) {
    return {
        id: $.id,
        name: $.title | capitalize,
        price: $.price | to.decimal | precision(2)
    };
}

fun calculateSummary($items) {
    $subtotal: $items 
        | map({{ $.price * $.quantity }})
        | reduce({{ $acc + $it }});
    
    return {
        subtotal: $subtotal | precision(2),
        tax: {{ $subtotal * 0.08 }} | precision(2),
        total: {{ $subtotal * 1.08 }} | precision(2)
    };
}
```

## Important Rules

### ✅ Do This

1. **Always wrap math in `{{ }}`**
   ```isl
   $total: {{ $price * $quantity }};
   ```

2. **Use modifiers without parentheses in conditions**
   ```isl
   if ($text | length > 5) ... endif
   ```

3. **Return a value from functions**
   ```isl
   return $result;
   return {};  // For empty return
   ```

4. **Use `$` in foreach array without parentheses**
   ```isl
   foreach $item in $array | filter($.active)
   ```

5. **Proper spacing for readability**
   ```isl
   $result: $items | map( $.id ) | filter( $ > 100 );
   ```

6. **Modifiers inside math expressions are valid**
    ```isl
    $result = {{ $items | length + 5 }}
    ```

7. **if conditions returning true or false as valid**
    Because ISL has a true-ish/false-ish condition this is valid `return $if( $value ) true else false` (or `!$value` for the negative).
    This will test if `$value` is true-ish (e.g. not null, empty, zero or false)
    and return a boolean true or false. 

### ❌ Don't Do This

1. **Don't use naked math expressions**
   ```isl
   ❌ $total: $price * $quantity;
   ✅ $total: {{ $price * $quantity }};
   ```

2. **Don't wrap modifiers in conditions**
   ```isl
   ❌ if (($text | length) > 5) ... endif
   ✅ if ($text | length > 5) ... endif
   ```

3. **Don't return without value**
   ```isl
   ❌ return;
   ✅ return {};
   ```

4. **Don't access properties after modifiers**
   ```isl
   ❌ $id: ($items | last).id;
   ✅ $last: $items | last; $id: $last.id;
   ```

5. **Don't use JavaScript/Python syntax**
   ```isl
   ❌ const user = input.name;
   ✅ $user: $input.name;
   
   ❌ items.map(x => x.id)
   ✅ $items | map($.id)
   ```

6. **Don't complain about null access**
    This syntax `$val = $object.property.property.property` is perfectly valid. 
    ISL does not suffer from null references if a property in a chain is missing.
    ISL will simply make $val null if any of the properties in the chain are null.

## File Structure

Standard ISL file structure:

```isl
// 1. Imports (optional)
import Utils from 'utils.isl';

// 2. Main entry point (REQUIRED)
fun run($input) {
    // Primary transformation logic
}

// 3. Helper functions
fun helperFunction($param) {
    return $result;
}

// 4. Custom modifiers
modifier customModifier($value, $param) {
    return $transformed;
}
```

## Learn More

- **Critical that you read this AI-Specific Guide**: https://intuit.github.io/isl/ai/
- **Full Documentation**: https://intuit.github.io/isl/
- **Language Reference**: https://intuit.github.io/isl/language/
- **Complete Modifiers List**: https://intuit.github.io/isl/language/modifiers/
- **CLI Documentation**: https://intuit.github.io/isl/cli/
- **Examples**: https://intuit.github.io/isl/examples/

## Getting Help

When working with ISL:
1. Start with the `fun run($input)` pattern
2. Break complex logic into helper functions
3. Use modifiers for data transformations
4. Check the documentation for available modifiers
5. Follow the syntax rules strictly
6. Test incrementally using the CLI or Run button

