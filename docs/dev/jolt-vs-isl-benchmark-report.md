---
title: Performance Benchmarks
parent: Developer Guide
nav_order: 4
---

## Summary

This report compares the performance of JOLT and ISL for JSON transformations using a real-world Shopify order transformation scenario. 

In production scenarios **ISL demonstrates superior performance compared to JOLT** in pre-compiled execution while offering significantly more powerful features and better code maintainability.

## Transformations

[Source Input Json](.././../isl-transform/src/jmh/resources/shopify-order.json)

- [JOLT Transform](../../isl-transform/src/jmh/resources/shopify-transform.jolt) - Standard JOLT Transformation of the Souce Input Json
- [ISL Simple](../../isl-transform/src/jmh/resources/shopify-transform-simple.isl) - Simple ISL Transformation generating a result similar to the JOLT result
- [ISL Complex](../../isl-transform/src/jmh/resources/shopify-transform-complex.isl) - More complex ISL Transformation on the same input adding functions, modifiers, string interpolations, conditions, variables, math operations, ...
- [ISL Complex Verbose](../../isl-transform/src/jmh/resources/shopify-transform.isl) - A more verbose version of the Complex implementation above using many variables, making the code very verbose (a bit hard to read)


## Benchmark Results

### Pre-Compiled Transformation (Execution Only)

| Implementation | Average Time | vs JOLT | Relative Performance |
|---------------|-------------|---------|---------------------|
| **ISL Simple** | **0.026 ms** | **46% faster** ⚡ | 1.8x faster |
| **ISL Complex** | **0.039 ms** | **19% faster** ✅ | 1.2x faster |
| **JOLT** | **0.048 ms** | baseline | 1.0x |
| ISL Complex (Verbose) | 0.059 ms | 23% slower | 0.8x |

### Full Transformation Cycle (Parse + Compile + Execute)

| Implementation | Average Time | vs JOLT | Relative Performance |
|---------------|-------------|---------|---------------------|
| **JOLT** | **0.114 ms** | baseline | 1.0x |
| **ISL Simple** | **0.127 ms** | 11% slower | 0.9x |
| **ISL Complex** | **0.289 ms** | 154% slower | 0.4x |
| ISL Complex (Verbose) | 0.446 ms | 291% slower | 0.3x |

## Key Findings

### 1. ISL Outperforms JOLT in Production Scenarios

When transformations are pre-compiled (the typical production scenario):
- **ISL Complex is 19% faster than JOLT** (0.039 ms vs 0.048 ms)
- **ISL Simple is > 46% faster than JOLT** (0.026 ms vs 0.048 ms)
- ISL delivers superior performance while providing advanced features unavailable in JOLT

### 2. Full Cycle: JOLT Has Lower Compilation Overhead

For one-time transformations (parse + compile + execute):
- JOLT is faster due to simpler compilation model
- ISL's richer feature set requires more compilation time
- **Production systems should pre-compile transformations** to leverage ISL's execution speed advantage

### 3. Performance Optimization Success

The optimized ISL transformation demonstrates that:
- Strategic caching of computed values reduces redundant operations
- Minimizing expensive string operations improves performance
- Pre-processing collections once and reusing results is highly effective
- ISL can deliver better performance than JOLT without sacrificing functionality

## Feature Comparison

### What ISL Offers Beyond JOLT

The ISL transformations showcases features not available in JOLT:

| Feature | ISL | JOLT |
|---------|-----|------|
| **Custom Functions** | ✅ Reusable helper functions | ❌ No function support |
| **String Manipulation** | ✅ `trim`, `upperCase`, `lowerCase` | ❌ Limited |
| **Math Operations** | ✅ `precision`, `Math.sum`, expressions | ❌ No math support |
| **Conditionals** | ✅ `if/else` logic | ⚠️ Complex syntax |
| **Array Operations** | ✅ `map`, `filter`, `unique`, `sort` | ⚠️ Limited |
| **Type Conversions** | ✅ `to.string`, `to.decimal`, `to.number`, `to.boolean` | ❌ Manual |
| **Date Parsing** | ✅ Full date/time support with formatting | ❌ No date support |
| **String Templates** | ✅ Native string interpolation | ❌ Workarounds needed |
| **Variables** | ✅ Named variables for clarity | ❌ No variables |
| **Object Spread** | ✅ Spread syntax for objects | ❌ Not available |

## Code Comparison

### JOLT Transformation (92 lines)

```json
// JOLT uses a right-hand side approach where the resulting property is
// on the right, opposite to what most programming languanges do
[
  {
    "operation": "shift",
    "spec": {
      "id": "orderId",
      "order_number": "orderNumber",
      "name": "orderName",
      "customer": {
        "id": "customerId",
        "first_name": "customerFirstName",
        "last_name": "customerLastName",
        "email": "customerEmail"
      },
      "line_items": {
        "*": {
          "id": "items[&1].itemId",
          "sku": "items[&1].sku",
          "name": "items[&1].name",
          "quantity": "items[&1].quantity",
          "price": "items[&1].unitPrice"
        }
      }
    }
  }
]
```

**Limitations:**
- No data validation or transformation
- No calculated fields
- No conditional logic
- Limited string manipulation
- No aggregations or filtering

### ISL Simple Transformation (30 lines)

```isl
// ISL uses the familiar left-hand-side assignment
fun run( $input ) {
    orderId: $input.id;
    orderNumber: $input.order_number;
    orderName: $input.name;
    customerId: $input.customer.id;
    customerFirstName: $input.customer.first_name;
    customerLastName: $input.customer.last_name;
    customerEmail: $input.customer.email;
    items: $input.line_items | map({
        itemId: $.id | to.string;
        sku: $.sku;
        name: $.name;
        quantity: $.quantity | to.number;
        unitPrice: $.price | to.decimal
    })
}
```

**Performance:** 0.026 ms (46% faster than JOLT)

### ISL Complex Transformation (130 lines)

{% raw %}
```isl
// Helper function: Convert address
fun convertAddress( $addr ) {
    $street = $addr.address1 | trim;
    $city = $addr.city;
    $state = $addr.province_code | trim | upperCase;
    $zip = $addr.zip | trim;
    
    return {
        street: $street,
        city: $city,
        state: $state,
        zipCode: $zip,
        country: $addr.country_code | upperCase,
        formatted: `${$street}, ${$city}, ${$state} ${$zip}`
    };
}

// Helper function: Convert customer
fun convertCustomer( $cust ) {
    $firstName = $cust.first_name | trim;
    $lastName = $cust.last_name | trim;
    
    return {
        id: $cust.id | to.string,
        fullName: `${$firstName} ${$lastName}`,
        firstName: $firstName,
        lastName: $lastName,
        email: $cust.email | lowerCase,
        phone: $cust.phone,
        totalOrders: $cust.orders_count | to.number,
        lifetimeValue: $cust.total_spent | to.decimal | precision(2),
        address: @.This.convertAddress( $cust.default_address )
    };
}

// Main entry point with advanced features
fun run( $input ) {
    // Pre-compute reused values (optimization)
    $customer = @.This.convertCustomer( $input.customer );
    $shippingAddr = @.This.convertAddress( $input.shipping_address );
    $items = $input.line_items;
    $processedItems = $items | map( @.This.processLineItem( $ ) );
    
    // Financial calculations with precision
    $total = $input.total_price | to.decimal;
    $discounts = $input.total_discounts | to.decimal;
    $finalTotal = {{ $total - $discounts }} | precision(2);
    
    // Status flags with conditional logic
    $fulfillmentStatus = $input.fulfillment_status | upperCase;
    $isPaid = if( $input.financial_status | lowerCase == "paid" ) true else false;
    $isFulfilled = if( $fulfillmentStatus == "FULFILLED" ) true else false;
    
    return {
        orderId: $input.id | to.string,
        customer: $customer,
        shipping: {
            ...$shippingAddr,
            status: if( $isFulfilled ) "DELIVERED" else "PENDING",
            speed: if( $input.total_shipping_price_set.shop_money.amount | to.decimal >= 20 ) "EXPRESS" else "STANDARD"
        },
        items: $processedItems,
        premiumItemCount: $items | filter( $.price | to.decimal >= 100 ) | length,
        vendors: $items | map( $.vendor ) | unique | sort,
        finalTotal: $finalTotal,
        isPaid: $isPaid,
        processedAt: $input.processed_at | date.parse("yyyy-MM-dd'T'HH:mm:ssXXX") | to.string("yyyy-MM-dd HH:mm:ss")
    }
}
```
{% endraw %}

**Performance:** 0.039 ms (19% faster than JOLT)

**Advanced Features:**
- ✅ Custom reusable functions
- ✅ Variable caching for performance
- ✅ String manipulation and formatting
- ✅ Math operations with precision control
- ✅ Conditional logic (if/else)
- ✅ Array operations (map, filter, unique, sort)
- ✅ Date parsing and formatting
- ✅ Type conversions
- ✅ Object spread syntax
- ✅ Aggregations and calculations

## Optimization Techniques

The ISL Complex version achieves superior performance through:

1. **Value Caching** - Compute once, reuse multiple times
   ```isl
   $customer = @.This.convertCustomer( $input.customer );
   // Reuse $customer.fullName, $customer.email, etc.
   ```

2. **Pre-processing Collections** - Transform arrays once
   ```isl
   $processedItems = $items | map( @.This.processLineItem( $ ) );
   ```

3. **Strategic String Operations** - Minimize expensive operations
   ```isl
   $firstName = $cust.first_name | trim;  // Trim once
   fullName: `${$firstName} ${$lastName}` // Reuse trimmed value
   ```

4. **Efficient Conditionals** - Pre-compute boolean flags
   ```isl
   $isPaid = if( $input.financial_status == "paid" ) true else false;
   ```

## Production Recommendations

### Use ISL When:
- ✅ **Performance is critical** - ISL is faster than JOLT in production (pre-compiled)
- ✅ You need advanced transformations (math, dates, conditionals, aggregations)
- ✅ Code maintainability and readability are priorities
- ✅ You want to reuse transformation logic via functions
- ✅ You need to validate, filter, or aggregate data during transformation
- ✅ Complex business logic is required

### Use JOLT When:
- ✅ You only need simple field mapping with no logic
- ✅ Compilation overhead is a concern (one-time transformations)
- ✅ You have existing JOLT transformations to maintain
- ✅ Team has no capacity to learn ISL syntax

### Best Practice: Pre-Compile in Production

For both JOLT and ISL, **always pre-compile transformations** in production:
- Eliminates parsing/compilation overhead
- **Enables ISL to outperform JOLT** (19-46% faster execution)
- Reduces memory allocation per transformation
- Improves overall system throughput

## Test Environment

- **JVM**: OpenJDK 21.0.7, 64-Bit Server VM
- **Framework**: JMH 1.37 (Java Microbenchmark Harness)
- **Warmup**: 5 iterations, 1 second each
- **Measurement**: 20 iterations, 1 second each
- **Mode**: Average time per operation
- **Input**: Real-world Shopify order JSON (complex nested structure)
- **Confidence Interval**: 99.9%
