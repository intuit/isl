# GitHub Copilot Instructions for ISL

## Language Overview
ISL (Intuitive Scripting Language) is a declarative language for JSON-to-JSON transformations. It combines clean syntax with powerful data manipulation capabilities.
This applies to files with the .isl extension;

## Key Syntax Elements

### Variables and Paths
```isl
$variable: "value";              // Simple variable
$nested: $object.property.path;  // Dot notation for nested access
$dynamic: { `$key`: $value };    // Dynamic property names
```

### Main Entry Point (Required)
```isl
fun run($input) {
    // All transformations start here
    // Transform $input and return result
}
```

### Functions
```isl
fun calculateTotal($items) {
    $sum: $items | map($.price) | reduce({{ $acc + $it }});
    return $sum;  // Always return a value
}

// Call with @.This prefix
$total: @.This.calculateTotal($input.items);
```

### Modifiers (Pipe Operators)
```isl
// Chain operations with |
$name: $input.firstName | trim | capitalize;
$ids: $input.items | map($.id) | unique | sort;
$filtered: $data | filter($.active) | map($.name);
```

### Math Expressions
```isl
// MUST be wrapped in {{ }}
$total: {{ $price * $quantity }};
$tax: {{ $subtotal * 0.08 }};
$average: {{ $sum / $count }};
```

### String Interpolation
```isl
// Use backticks for interpolation
$message: `Hello ${$user.name}!`;
$summary: `Order ${$order.id}: \${{ $order.total }}`;

// Simple variables (no dots) don't need ${}
$greeting: `Hello $firstName`;

// Nested paths (with dots) need ${}
$full: `Name: ${$user.profile.name}`;
```

## Control Flow

### If/Else
```isl
// Expression form
$status: if ($paid) "completed" else "pending" endif;

// Statement form
if ($amount > 100)
    $discount: 0.1;
else
    $discount: 0.05;
endif
```

### Switch/Case
```isl
$category: switch ($price)
    < 10 -> "budget";
    < 50 -> "standard";
    < 200 -> "premium";
    else -> "luxury";
endswitch
```

### Foreach
```isl
$transformed: foreach $item in $input.items
    {
        id: $item.id,
        total: {{ $item.price * $item.quantity }}
    }
endfor

// With filter
foreach $item in $items | filter($.price > 100)
    // Process only expensive items
endfor
```

## Common Modifiers

**String**: `trim`, `upperCase`, `lowerCase`, `capitalize`, `split`, `replace`, `subString`, `length`

**Array**: `map`, `filter`, `reduce`, `sort`, `reverse`, `unique`, `first`, `last`, `at`, `length`, `push`, `pop`

**Conversion**: `to.string`, `to.number`, `to.decimal`, `to.boolean`, `to.array`, `to.json`

**Date**: `date.parse`, `date.add`, `date.part`, `to.string` (with format)

**Math**: `round`, `absolute`, `precision`, `round.up`, `round.down`

**Object**: `keys`, `kv`, `select`, `delete`, `getProperty`

## Important Rules

1. **No parentheses around modifiers in conditions**:
   ```isl
   // ✅ Correct
   if ($text | length > 5) ... endif
   
   // ❌ Wrong
   if (($text | length) > 5) ... endif
   ```

2. **Return must have value**:
   ```isl
   // ✅ Correct
   return {};
   return $result;
   
   // ❌ Wrong
   return;
   ```

3. **Math must be wrapped**:
   ```isl
   // ✅ Correct
   $result: {{ $a + $b * $c }};
   
   // ❌ Wrong
   $result: $a + $b * $c;
   ```

4. **No dot access after modifiers**:
   ```isl
   // ✅ Correct
   $last: $items | last;
   $id: $last.id;
   
   // ❌ Wrong
   $id: ($items | last).id;
   ```

5. **Foreach without array parentheses**:
   ```isl
   // ✅ Correct
   foreach $item in $array | filter($.active)
   
   // ❌ Wrong
   foreach $item in ($array | filter($.active))
   ```

## Standard Patterns

### Simple Transformation
```isl
fun run($input) {
    userId: $input.id | to.string;
    userName: `${$input.firstName} ${$input.lastName}`;
    createdAt: $input.timestamp | date.parse("yyyy-MM-dd");
}
```

### Complex Transformation with Helpers
```isl
fun run($input) {
    order: @.This.transformOrder($input.order);
    customer: @.This.transformCustomer($input.customer);
    items: $input.items | map(@.This.transformItem($));
}

fun transformOrder($order) {
    return {
        id: $order.id | to.string,
        total: $order.amount | to.decimal | precision(2),
        status: $order.paid | orderStatus
    };
}

modifier orderStatus($paid) {
    return if ($paid) "completed" else "pending" endif;
}
```

### Array Transformations
```isl
// Map array to new structure
items: $input.items | map({
    id: $.id,
    name: $.title | trim | capitalize,
    price: $.amount | to.decimal
})

// Filter and transform
expensiveItems: $input.items 
    | filter($.price > 100)
    | map({ name: $.title, price: $.price })
    | sort

// Aggregate values
$total: $items 
    | map({{ $.price * $.quantity }})
    | reduce({{ $acc + $it }});
```

### Conditional Fields
```isl
{
    id: $input.id,
    name: $input.name,
    // Conditionally include email
    ...if ($input.email) { email: $input.email } else {} endif,
    // Use coalesce for defaults
    phone: $input.phone ?? "N/A"
}
```

## What Copilot Should NOT Suggest

- ❌ JavaScript syntax (`const`, `let`, `var`, `function`, `=>`)
- ❌ Python syntax (`def`, `lambda`, `:`)
- ❌ Method chaining (`.map()`, `.filter()`)
- ❌ Math without `{{ }}` wrapping
- ❌ `return;` without value
- ❌ Accessing properties after modifiers inline
- ❌ Traditional for loops (`for (i=0; i<n; i++)`)
- ❌ Ternary operator (`? :`) - use `if/else/endif`

## Documentation References

- Full Documentation: https://intuit.github.io/isl/
- Critical that you also read this AI Guide: https://intuit.github.io/isl/ai/
- Language Reference: https://intuit.github.io/isl/language/
- Modifiers List: https://intuit.github.io/isl/language/modifiers/
- Examples: https://intuit.github.io/isl/examples/

## Tips for Better Suggestions

1. Always start with `fun run($input)` for new files
2. Suggest breaking complex logic into helper functions
3. Recommend modifiers for simple transformations
4. Use proper spacing around pipes and operators
5. Add helpful comments for complex transformations
6. Suggest appropriate modifiers instead of manual operations
7. Follow the file structure: run function → helper functions → modifiers

