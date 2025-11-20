---
title: Best Practices
parent: Developer Guide
nav_order: 1
---

This guide covers best practices for writing efficient, maintainable ISL transformations and optimizing performance.

## 🚀 Performance Best Practices

### 1. Pre-Compile Scripts Once, Reuse Many Times

**❌ Bad - Compiling Every Time (Slow)**
```java
public JsonNode transform(String inputJson) {
    // DON'T DO THIS - compiling every time is 19x slower!
    String islScript = "{ id: $input.id, name: $input.name }";
    ITransformer transformer = new TransformCompiler().compileIsl("transform", islScript);
    
    OperationContext context = new OperationContext();
    context.setVariable("$input", JsonConvert.convert(inputJson));
    return transformer.runTransformSync("run", context);
}
```

**✅ Good - Compile Once, Reuse (Fast)**
```java
public class ProductTransformer {
    // Compile once during initialization
    private final ITransformer transformer;
    
    public ProductTransformer() {
        String islScript = "{ id: $input.id, name: $input.name }";
        this.transformer = new TransformCompiler().compileIsl("transform", islScript);
    }
    
    public JsonNode transform(String inputJson) {
        // Reuse the pre-compiled transformer
        OperationContext context = new OperationContext();
        context.setVariable("$input", JsonConvert.convert(inputJson));
        return transformer.runTransformSync("run", context);
    }
}
```

**Performance Impact:**
- Compilation: ~0.5-1.0 ms
- Execution (pre-compiled): ~0.03-0.04 ms
- **Pre-compiling is 19x faster!**

---

### 2. Create New OperationContext Per Request

**❌ Bad - Reusing Context (Thread-Unsafe)**
```java
public class BadTransformer {
    private final ITransformer transformer;
    private final OperationContext context = new OperationContext(); // DON'T DO THIS!
    
    public JsonNode transform(String inputJson) {
        // This is NOT thread-safe!
        context.setVariable("$input", JsonConvert.convert(inputJson));
        return transformer.runTransformSync("run", context);
    }
}
```

**✅ Good - New Context Per Request (Thread-Safe)**
```java
public class GoodTransformer {
    private final ITransformer transformer;
    
    public JsonNode transform(String inputJson) {
        // Create new context for each transformation
        OperationContext context = new OperationContext();
        context.setVariable("$input", JsonConvert.convert(inputJson));
        return transformer.runTransformSync("run", context);
    }
}
```

**Why?**
- `OperationContext` is **not thread-safe**
- Creating a new context is cheap (~microseconds)
- Reusing context causes race conditions in concurrent environments

---

### 3. Use Modifiers Instead of Functions for Simple Operations

**❌ Less Efficient - Custom Function**
```isl
fun upperCaseString($text) {
    return $text | upperCase;
}

result: @.This.upperCaseString($input.name)
```

**✅ More Efficient - Direct Modifier**
```isl
result: $input.name | upperCase
```

**Why?**
- Modifiers are more readable
- Modifiers are optimized for chaining
- Function calls have overhead

---

### 4. Avoid Deep Nesting in Conditionals

**❌ Hard to Read and Maintain**
```isl
result: if ($a > 0)
    if ($b > 0)
        if ($c > 0)
            "all positive"
        else
            "c not positive"
        endif
    else
        "b not positive"
    endif
else
    "a not positive"
endif
```

**✅ Better - Early Returns with Functions**
```isl
fun checkValues($a, $b, $c) {
    if ($a <= 0) return "a not positive";
    if ($b <= 0) return "b not positive";
    if ($c <= 0) return "c not positive";
    return "all positive";
}

result: @.This.checkValues($a, $b, $c)
```

**✅ Or Use Boolean Logic**
```isl
result: if ($a > 0 and $b > 0 and $c > 0)
    "all positive"
else
    "not all positive"
endif
```

---

### 5. Use `map` Instead of `foreach` for Array Transformations

**❌ Verbose - Using foreach**
```isl
$result = [];
foreach $item in $input.items
    $result = $result | push({
        id: $item.id,
        name: $item.name | upperCase
    })
endfor
```

**✅ Concise - Using map**
```isl
$result = $input.items | map({
    id: $.id,
    name: $.name | upperCase
})
```

**Why?**
- `map` is more concise and readable
- `map` is optimized for transformations
- `map` creates the array in one operation

---

### 6. Pre-Parse JSON When Possible

**❌ Parsing JSON Every Time**
```java
public JsonNode transform(String inputJson) {
    OperationContext context = new OperationContext();
    context.setVariable("$input", JsonConvert.convert(inputJson)); // Parsing happens here
    return transformer.runTransformSync("run", context);
}
```

**✅ Pass Pre-Parsed JsonNode**
```java
public JsonNode transform(JsonNode inputNode) {
    OperationContext context = new OperationContext();
    context.setVariable("$input", inputNode); // No parsing needed
    return transformer.runTransformSync("run", context);
}
```

**Performance Impact:**
- JSON parsing adds minimal overhead (~0.01ms for 4KB)
- But if you already have a `JsonNode`, don't convert to string and back

---

## 📝 Code Organization Best Practices

### 1. Use Functions to Break Down Complex Transformations

**❌ Monolithic Transformation**
```isl
{
    customerId: $input.customer.id | to.string,
    customerName: `${$input.customer.firstName} ${$input.customer.lastName}` | trim,
    customerEmail: $input.customer.email | trim | lowerCase,
    shippingStreet: $input.shipping.address1 | trim,
    shippingCity: $input.shipping.city | trim | titleCase,
    shippingState: $input.shipping.state | trim | upperCase,
    billingStreet: $input.billing.address1 | trim,
    billingCity: $input.billing.city | trim | titleCase,
    billingState: $input.billing.state | trim | upperCase
}
```

**✅ Organized with Functions**
```isl
fun formatCustomer($customer) {
    return {
        id: $customer.id | to.string,
        name: `${$customer.firstName} ${$customer.lastName}` | trim,
        email: $customer.email | trim | lowerCase
    };
}

fun formatAddress($address) {
    return {
        street: $address.address1 | trim,
        city: $address.city | trim | titleCase,
        state: $address.state | trim | upperCase
    };
}

{
    customer: @.This.formatCustomer($input.customer),
    shippingAddress: @.This.formatAddress($input.shipping),
    billingAddress: @.This.formatAddress($input.billing)
}
```

---

### 2. Use Descriptive Variable Names

**❌ Unclear**
```isl
$a = $input.items | filter($.price > 100);
$b = $a | map($.price);
$c = $b | Math.sum(0);
```

**✅ Clear**
```isl
$expensiveItems = $input.items | filter($.price > 100);
$expensivePrices = $expensiveItems | map($.price);
$totalExpensivePrice = $expensivePrices | Math.sum(0);
```

---

### 3. Use String Interpolation for Complex Strings

**❌ Hard to Read**
```isl
$fullName = $input.firstName | concat(" ") | concat($input.middleName) | concat(" ") | concat($input.lastName);
```

**✅ Readable**
```isl
$fullName = `${ $input.firstName | trim } ${$input.middleName} ${ $input.lastName | trim }`;
```

**Notes:**
- Remember the extra `$` in the string interpolation to be able to access the variable properties.
- You can use modifiers inside the interpolation `${ $var | modifier }`

---

## 🛡️ Error Handling Best Practices

### 1. Use `default` Modifier or Coalesce `??`

**❌ Risky - Can Produce Null**
```isl
{
    name: $input.customer.name,
    email: $input.customer.email
}
```

**✅ Safe - With Defaults**
```isl
{
    name: $input.customer.name | default("Unknown"),    // default
    name: $input.customer.name ?? "Unknown",            // coalesce
    email: $input.customer.email | default("no-email@example.com")
}
```

**Notes:**
- `|default` and `??` have similar functions, just different readability.
- Both can be chained 
    - `name: $input.customer.name | default ( $input.customer.last ) | default ( $input.customer.first ) | default( "Unknown" )` vs
    - `name: $input.customer.name ?? $input.customer.last ?? $input.customer.first ?? "Unknown"`

---

### 2. Use multi date formats for parsing dates

**❌ Single Format**
```isl
$date = $input.dateString | date.parse("yyyy-MM-dd") ;
```

**✅ Safer - Multiple Formats**
```isl
$date = $input.dateString | date.parse(["yyyy-MM-dd", "MM/dd/yyyy", "dd-MM-yyyy"]);
```

---

## 🎯 Common Patterns

### Pattern 1: Conditional Field Inclusion

```isl
{
    id: $input.id,
    name: $input.name,

    // Only include email if it exists
    // if email is null the field will not be generated at all
    email: if ($input.email != null) $input.email endif,

}
```

### Pattern 2: Array Filtering and Transformation

```isl
// Get active products with price > 100, sorted by price
$premiumProducts = $input.products
    | filter($.status == 'active' and $.price > 100)
    | sort({ by: "price", order: "desc" })
    | map({
        id: $.id,
        name: $.name | titleCase,
        price: $.price | precision(2)
    });
```

### Pattern 3: Aggregations

{% raw %}
```isl
$items = $input.orderItems;

{
    totalItems: $items | length,
    totalQuantity: $items | map($.quantity) | Math.sum(0),
    totalPrice: $items | map({{ $.quantity * $.price }}) | Math.sum(0) | precision(2),
    averagePrice: $items | map($.price) | Math.mean | precision(2)
}
```
{% endraw %}

### Pattern 4: Nested Object Flattening

```isl
// Flatten nested customer address
{
    customerId: $input.customer.id,
    customerName: $input.customer.name,
    street: $input.customer.address.street,
    city: $input.customer.address.city,
    state: $input.customer.address.state,
    zip: $input.customer.address.zip
}
```

### Pattern 5: Dynamic Property Names

```isl
$propName = "dynamicField";

{
    staticField: "value",
    `$propName`: "dynamic value"  // Creates "dynamicField": "dynamic value"
}
```

### Pattern 6: Build Nested Objects Instead of Multiple Top-Level Properties

**❌ Bad - Multiple Top-Level Properties with Dotted Names**
```isl
$customer.id: $input.customerId,
$customer.name: $input.customerName,
$customer.email: $input.email,
$customer.address.line1: $input.addressLine1,
$customer.address.line2: $input.addressLine2,
$customer.address.city: $input.city,
$customer.address.state: $input.state,
$customer.address.zip: $input.zip
```

**✅ Good - Properly Nested Objects**

```isl
$customer = {   // build object shapes
        id: $input.customerId,
        name: $input.customerName,
        email: $input.customerEmail,
        address: {
            line1: $input.addressLine1,
            line2: $input.addressLine2,
            city: $input.city,
            state: $input.state,
            zip: $input.zip
        }
    }
```

**Why Nested is Better:**
- **Easier to read**: No duplicates of `$customer.address.`
- **Better Performance**: ISL Is optimized for the object build format not for individual property setting
- **Better JSON structure**: Follows standard JSON conventions
- **Type-safe**: IDEs and tools can understand the structure
- **More maintainable**: Clear hierarchy and relationships
- **Easier to extend**: Add new nested properties without name collisions

---

## 📊 Performance Benchmarks

Based on real-world testing with a 4.5KB Shopify order transformation:

| Scenario | Time (ms) | Throughput (ops/sec) |
|----------|-----------|----------------------|
| **Pre-compiled Execution** | 0.034 | ~29,400 |
| **Parsing Only** | 0.484 | ~2,066 |
| **Compilation Only** | 0.904 | ~1,106 |
| **Full Cycle (parse + compile + execute)** | 0.717 | ~1,395 |

**Key Takeaways:**
- Pre-compilation provides **19x performance improvement**
- Execution is extremely fast (~34 microseconds)
- Parsing and compilation are one-time costs
- For high-throughput scenarios, always pre-compile

---

## 📚 Additional Resources

- **[Language Reference](../language/)** - Complete ISL syntax guide
- **[Modifiers Reference](../language/modifiers.md)** - All available modifiers
- **[Examples](../examples/)** - Real-world transformation examples

---

## ✅ Checklist for Production-Ready ISL

- [ ] Scripts are pre-compiled and cached
- [ ] New `OperationContext` created per request
- [ ] Null safety (if needed) with `default` modifier or `??` coalesce
- [ ] Functions used to break down complex logic
- [ ] Descriptive variable names
- [ ] Error handling for edge cases
- [ ] Performance tested under expected load
- [ ] No deep nesting (max 2-3 levels)
- [ ] Modifiers preferred over custom functions for simple operations

---

**Need more help?** Check out the [Quick Start Guide](../quickstart.md) or browse [example transformations](../examples/)!

