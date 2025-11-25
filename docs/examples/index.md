---
title: Transformation Examples
parent: Examples
nav_order: 1
description: "Common ISL transformation patterns for JSON data manipulation including field mapping, array transformations, nested objects, and more."
excerpt: "Common ISL transformation patterns for JSON data manipulation including field mapping, array transformations, nested objects, and more."
---

This guide demonstrates common JSON transformation patterns using ISL. Each example shows how to handle typical data transformation scenarios.

## Table of Contents
- [Simple Field Mapping](#simple-field-mapping)
- [Field Renaming & Restructuring](#field-renaming--restructuring)
- [Array Transformations](#array-transformations)
- [Nested Object Flattening](#nested-object-flattening)
- [Object Nesting](#object-nesting)
- [Conditional Transformations](#conditional-transformations)
- [Array to Object Conversion](#array-to-object-conversion)
- [Object to Key/Value Array](#object-to-keyvalue-array-conversion)
- [Filtering and Mapping](#filtering-and-mapping)
- [Merging Multiple Sources](#merging-multiple-sources)
- [Default Values](#default-values)

---

## Simple Field Mapping

**Use Case:** Copy and rename fields from input to output

**Documentation:** [Variables](/isl/language/variables/), [Objects](/isl/language/objects/)

**Input:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "age": 30
}
```

**ISL Transformation:**
```isl
{
  first_name: $input.firstName,
  last_name: $input.lastName,
  age: $input.age
}
```

**Output:**
```json
{
  "first_name": "John",
  "last_name": "Doe",
  "age": 30
}
```

---

## Field Renaming & Restructuring

**Use Case:** Rename multiple fields and reorganize structure

**Documentation:** [Objects](/isl/language/objects/), [Variables](/isl/language/variables/)

**Input:**
```json
{
  "user_id": "12345",
  "user_name": "johndoe",
  "user_email": "john@example.com",
  "account_status": "active"
}
```

**ISL Transformation:**
```isl
{
  id: $input.user_id,
  profile: {
    username: $input.user_name,
    email: $input.user_email
  },
  status: $input.account_status
}
```

**Output:**
```json
{
  "id": "12345",
  "profile": {
    "username": "johndoe",
    "email": "john@example.com"
  },
  "status": "active"
}
```

---

## Array Transformations

**Use Case:** Transform each item in an array

**Documentation:** [Loops](/isl/language/loops/), [Built-in Modifiers](/isl/language/modifiers/)

**Input:**
```json
{
  "items": [
    {"name": "Product A", "price": 10},
    {"name": "Product B", "price": 20}
  ]
}
```

**ISL Transformation:**
```isl
{
  products: foreach $item in $input.items
    {
      title: $item.name,
      cost: $item.price,
      currency: "USD"
    }
  endfor
}
```
or using `|map ( )`
```isl
{
  products: $input.items | map(
    {   // $ contains the current item
      title: $.name,
      cost: $.price,
      currency: "USD"
    })
}
```


**Output:**
```json
{
  "products": [
    {"title": "Product A", "cost": 10, "currency": "USD"},
    {"title": "Product B", "cost": 20, "currency": "USD"}
  ]
}
```

---

## Nested Object Flattening

**Use Case:** Flatten nested structure into flat object

**Documentation:** [Objects](/isl/language/objects/), [Variables](/isl/language/variables/)

**Input:**
```json
{
  "user": {
    "profile": {
      "name": "John Doe",
      "email": "john@example.com"
    },
    "address": {
      "city": "New York",
      "country": "USA"
    }
  }
}
```

**ISL Transformation:**
```isl
{
  user_name: $input.user.profile.name,
  user_email: $input.user.profile.email,
  user_city: $input.user.address.city,
  user_country: $input.user.address.country
}
```

**Output:**
```json
{
  "user_name": "John Doe",
  "user_email": "john@example.com",
  "user_city": "New York",
  "user_country": "USA"
}
```

---

## Object Nesting

**Use Case:** Create nested structure from flat data

**Documentation:** [Objects](/isl/language/objects/), [Variables](/isl/language/variables/)

**Input:**
```json
{
  "product_name": "Laptop",
  "product_price": 999,
  "product_category": "Electronics",
  "seller_name": "TechStore",
  "seller_rating": 4.5
}
```

**ISL Transformation:**
```isl
{
  product: {
    name: $input.product_name,
    price: $input.product_price,
    category: $input.product_category
  },
  seller: {
    name: $input.seller_name,
    rating: $input.seller_rating
  }
}
```

**Output:**
```json
{
  "product": {
    "name": "Laptop",
    "price": 999,
    "category": "Electronics"
  },
  "seller": {
    "name": "TechStore",
    "rating": 4.5
  }
}
```

---

## Conditional Transformations

**Use Case:** Include fields based on conditions

**Documentation:** [Conditions](/isl/language/conditions/), [Objects](/isl/language/objects/)

**Input:**
```json
{
  "user": "john",
  "age": 25,
  "premium": true,
  "discount": 10
}
```

**ISL Transformation:**
```isl
{
  username: $input.user,
  age: $input.age,
  membership: if ($input.premium) "premium" else "standard" endif,
  discount_percent: if ($input.premium) $input.discount endif
}
```

**Output:**
```json
{
  "username": "john",
  "age": 25,
  "membership": "premium",
  "discount_percent": 10
}
```

---

## Array to Object Conversion

**Use Case:** Convert array of key-value pairs to object

**Documentation:** [Functions](/isl/language/functions/), [Built-in Modifiers](/isl/language/modifiers/), [Loops](/isl/language/loops/)

**Input:**
```json
{
  "attributes": [
    {"key": "color", "value": "red"},
    {"key": "size", "value": "large"},
    {"key": "brand", "value": "Nike"}
  ]
}
```

**ISL Transformation:**
```isl
fun run($input) {
  $result: $input.attributes | to.object;  // convert any [{key/value}] to object
  
  // alternatively use the foreach - not as efficient
  // foreach $attr in $input.attributes
  //  $result = {
  //      ...$result,
  //      `${ $attr.key }`: $attr.value
  //  }
  // endfor
  
  return $result;
}
```

**Output:**
```json
{
  "color": "red",
  "size": "large",
  "brand": "Nike"
}
```

---

## Object to Key/Value Array Conversion

**Use Case:** Convert object to a Key/Value array

**Documentation:** [Functions](/isl/language/functions/), [Built-in Modifiers](/isl/language/modifiers/)

**Input:**
```json
{
  "username": "john",
  "age": 25,
  "membership": "premium",
  "discount_percent": 10
}
```

**ISL Transformation:**
```isl
fun run($input) {
  $array: $input | kv // kv operator
  
  return $array;
}
```

**Output:**
```json
[
	{
		"key": "username",
		"value": "john"
	},
	{
		"key": "age",
		"value": 25
	},
	{
		"key": "membership",
		"value": "premium"
	},
	{
		"key": "discount_percent",
		"value": 10
	}
]
```

---

## Filtering and Mapping

**Use Case:** Filter array and transform matching items

**Documentation:** [Loops](/isl/language/loops/), [Built-in Modifiers](/isl/language/modifiers/), [Conditions](/isl/language/conditions/)

**Input:**
```json
{
  "orders": [
    {"id": 1, "status": "completed", "amount": 100},
    {"id": 2, "status": "pending", "amount": 50},
    {"id": 3, "status": "completed", "amount": 200}
  ]
}
```

**ISL Transformation:**
```isl
{
  completed_orders: foreach $order in $input.orders | filter( $.status == "completed" )
    {
      order_id: $order.id,
      total: $order.amount
    }
  endfor
}
```

**Output:**
```json
{
  "completed_orders": [
    {"order_id": 1, "total": 100},
    {"order_id": 3, "total": 200}
  ]
}
```

---

## Merging Multiple Sources

**Use Case:** Combine data from multiple input sources

**Documentation:** [Objects](/isl/language/objects/), [Variables](/isl/language/variables/)

**Input:**
```json
{
  "user": {
    "id": "123",
    "name": "John"
  },
  "preferences": {
    "theme": "dark",
    "language": "en"
  }
}
```

**ISL Transformation:**
```isl
{
  user_id: $input.user.id,
  user_name: $input.user.name,
  settings: {
    ...$input.preferences,
    notifications: true
  }
}
```

**Output:**
```json
{
  "user_id": "123",
  "user_name": "John",
  "settings": {
    "theme": "dark",
    "language": "en",
    "notifications": true
  }
}
```

---

## Default Values

**Use Case:** Provide default values for missing fields

**Documentation:** [Conditions](/isl/language/conditions/) (Coalesce Operator)

**Input:**
```json
{
  "name": "Product",
  "price": 50
}
```

**ISL Transformation:**
```isl
{
  name: $input.name,
  price: $input.price,
  quantity: $input.quantity ?? 1,
  available: $input.available ?? true,
  category: $input.category ?? "uncategorized"
}
```

**Output:**
```json
{
  "name": "Product",
  "price": 50,
  "quantity": 1,
  "available": true,
  "category": "uncategorized"
}
```

---

## Complex Example: E-commerce Order

**Use Case:** Real-world transformation of an order from external API format to internal format

**Documentation:** [Loops](/isl/language/loops/), [Math Expressions](/isl/language/math/), [Dates & Times](/isl/types/dates/), [Built-in Modifiers](/isl/language/modifiers/)

**Input:**
```json
{
  "order_number": "ORD-12345",
  "customer_info": {
    "customer_id": "CUST-999",
    "full_name": "Jane Smith",
    "contact_email": "jane@example.com"
  },
  "line_items": [
    {
      "sku": "PROD-A",
      "item_name": "Widget",
      "unit_price": 25.00,
      "qty": 2
    },
    {
      "sku": "PROD-B",
      "item_name": "Gadget",
      "unit_price": 50.00,
      "qty": 1
    }
  ],
  "order_status": "confirmed",
  "created_timestamp": 1645004735
}
```

**ISL Transformation:**
{% raw %}
```isl
{
  id: $input.order_number,
  customer: {
    id: $input.customer_info.customer_id,
    name: $input.customer_info.full_name,
    email: $input.customer_info.contact_email
  },
  items: foreach $item in $input.line_items
    {
      product_sku: $item.sku,
      name: $item.item_name,
      price: $item.unit_price,
      quantity: $item.qty,
      subtotal: {{ $item.unit_price * $item.qty }}
    }
  endfor,
  status: $input.order_status,
  total: $input.line_items | map({{ $.unit_price * $.qty }}) | reduce({{ $acc + $it }}),
  created_at: $input.created_timestamp | date.fromEpochSeconds | to.string("yyyy-MM-dd HH:mm:ss")
}
```
{% endraw %}

**Output:**
```json
{
  "id": "ORD-12345",
  "customer": {
    "id": "CUST-999",
    "name": "Jane Smith",
    "email": "jane@example.com"
  },
  "items": [
    {
      "product_sku": "PROD-A",
      "name": "Widget",
      "price": 25.0,
      "quantity": 2,
      "subtotal": 50.0
    },
    {
      "product_sku": "PROD-B",
      "name": "Gadget",
      "price": 50.0,
      "quantity": 1,
      "subtotal": 50.0
    }
  ],
  "status": "confirmed",
  "total": 100.0,
  "created_at": "2022-02-16 09:45:35"
}
```

---

## Date Processing

**Documentation:** [Dates & Times](/isl/types/dates/), [Built-in Modifiers](/isl/language/modifiers/)

### Parsing Dates with Multiple Formats

**Use Case:** Parse dates that may come in different formats

**Input:**
```json
{
  "orders": [
    {"id": 1, "created": "2024-01-15"},
    {"id": 2, "created": "15/01/2024"},
    {"id": 3, "created": "01-15-2024"}
  ]
}
```

**ISL Transformation:**
```isl
{
  orders: foreach $order in $input.orders
    {
      id: $order.id,
      created_date: $order.created | date.parse(["yyyy-MM-dd", "dd/MM/yyyy", "MM-dd-yyyy"])
    }
  endfor
}
```

**Output:**
```json
{
  "orders": [
    {"id": 1, "created_date": "2024-01-15T00:00:00.000Z"},
    {"id": 2, "created_date": "2024-01-15T00:00:00.000Z"},
    {"id": 3, "created_date": "2024-01-15T00:00:00.000Z"}
  ]
}
```

### Date Arithmetic

**Use Case:** Add or subtract time from dates

**Documentation:** [Dates & Times](/isl/types/dates/)

**Input:**
```json
{
  "subscription": {
    "start_date": "2024-01-01",
    "plan": "monthly"
  }
}
```

**ISL Transformation:**
```isl
{
  subscription: {
    start_date: $input.subscription.start_date | date.parse("yyyy-MM-dd"),
    end_date: $input.subscription.start_date 
      | date.parse("yyyy-MM-dd") 
      | date.add(30, "DAYS")
      | to.string("yyyy-MM-dd"),
    renewal_date: $input.subscription.start_date 
      | date.parse("yyyy-MM-dd") 
      | date.add(1, "MONTHS")
      | to.string("yyyy-MM-dd"),
    trial_expired: $input.subscription.start_date 
      | date.parse("yyyy-MM-dd") 
      | date.add(-7, "DAYS")
      | to.string("yyyy-MM-dd")
  }
}
```

**Output:**
```json
{
  "subscription": {
    "start_date": "2024-01-01T00:00:00.000Z",
    "end_date": "2024-01-31",
    "renewal_date": "2024-02-01",
    "trial_expired": "2023-12-25"
  }
}
```

### Extracting Date Parts

**Use Case:** Extract specific parts from a date (year, month, day, etc.)

**Documentation:** [Dates & Times](/isl/types/dates/)

**Input:**
```json
{
  "event": {
    "name": "Conference 2024",
    "timestamp": "2024-11-15T14:30:00Z"
  }
}
```

**ISL Transformation:**
```isl
{
  event: {
    name: $input.event.name,
    full_date: $input.event.timestamp | date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'"),
    year: $input.event.timestamp | date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'") | date.part("year"),
    month: $input.event.timestamp | date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'") | date.part("month"),
    day: $input.event.timestamp | date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'") | date.part("day"),
    hour: $input.event.timestamp | date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'") | date.part("hour"),
    day_of_week: $input.event.timestamp | date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'") | date.part("dayOfWeek"),
    formatted_display: $input.event.timestamp 
      | date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'")
      | to.string("MMMM dd, yyyy 'at' HH:mm")
  }
}
```

**Output:**
```json
{
  "event": {
    "name": "Conference 2024",
    "full_date": "2024-11-15T14:30:00.000Z",
    "year": 2024,
    "month": 11,
    "day": 15,
    "hour": 14,
    "day_of_week": 5,
    "formatted_display": "November 15, 2024 at 14:30"
  }
}
```

### Converting Between Date Formats

**Use Case:** Convert dates from one format to another

**Documentation:** [Dates & Times](/isl/types/dates/), [Built-in Modifiers](/isl/language/modifiers/)

**Input:**
```json
{
  "reports": [
    {"id": 1, "generated": "2024-01-15 10:30:00"},
    {"id": 2, "generated": "2024-02-20 15:45:00"}
  ]
}
```

**ISL Transformation:**
```isl
{
  reports: foreach $report in $input.reports
    {
      id: $report.id,
      generated_iso: $report.generated 
        | date.parse("yyyy-MM-dd HH:mm:ss")
        | to.string("yyyy-MM-dd'T'HH:mm:ss'Z'"),
      generated_readable: $report.generated 
        | date.parse("yyyy-MM-dd HH:mm:ss")
        | to.string("MMM dd, yyyy 'at' h:mm a"),
      generated_short: $report.generated 
        | date.parse("yyyy-MM-dd HH:mm:ss")
        | to.string("MM/dd/yy"),
      unix_timestamp: $report.generated 
        | date.parse("yyyy-MM-dd HH:mm:ss")
        | to.number
    }
  endfor
}
```

**Output:**
```json
{
  "reports": [
    {
      "id": 1,
      "generated_iso": "2024-01-15T10:30:00Z",
      "generated_readable": "Jan 15, 2024 at 10:30 AM",
      "generated_short": "01/15/24",
      "unix_timestamp": 1705315800
    },
    {
      "id": 2,
      "generated_iso": "2024-02-20T15:45:00Z",
      "generated_readable": "Feb 20, 2024 at 3:45 PM",
      "generated_short": "02/20/24",
      "unix_timestamp": 1708444700
    }
  ]
}
```

## Tips for Complex Transformations

1. **Break down complex transformations** - Use [functions](/isl/language/functions/) for reusable logic
2. **Use modifiers** - Chain operations with `|` for cleaner code - see [Built-in Modifiers](/isl/language/modifiers/)
3. **Leverage spread operator** - Use `...` to merge [objects](/isl/language/objects/) efficiently
4. **Test incrementally** - Build transformations step by step
5. **Use variables** - Store intermediate results for clarity - see [Variables](/isl/language/variables/)

## Related Documentation

- [Variables](../language/variables.md)
- [Objects](../language/objects.md)
- [Loops](../language/loops.md)
- [Built-in Modifiers](../language/modifiers.md)
- [Conditions](../language/conditions.md)

