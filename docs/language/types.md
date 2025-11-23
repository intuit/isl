---
title: Type System
parent: Language Reference
nav_order: 8
---

ISL has support for type declaration for variables and parameters, but just like TypeScript, ISL does not (currently) attempt to enforce those types.

The types are mostly declarative to help with interfacing, editing, auto-complete and introspection of the method interfaces.

ISL Types are closely mapped to the [JSON Schema types](https://json-schema.org/understanding-json-schema/reference/type.html) with few shortcuts
to help keep the code cleaner.

## Default Types

- `$v: string` - any string. JSON Schema `{ "type": "string" }`
- `$v: number` - any valid number with or without decimals. JSON Schema `{ "type": "number" }`
- `$v: integer` - any valid integer with no decimals. JSON Schema `{ "type": "integer" }`
- `$v: boolean` - a boolean `true` or `false` value. JSON Schema `{ "type": "boolean" }`
- `$v: object` - any object (can have child schema defined). JSON Schema `{ "type": "object" }`
- `$v: []` - an array with or without a type. JSON Schema `{ "type": "array" }`
- `$v: any` - anything. No validations will be applied. **By default everything is `any`.** JSON Schema `{ "type": "object" }`

## Extended Types

The following types don't directly exist in the JSON Schema but they can be mapped to it.

- `$v: text` - a multi-line string. JSON Schema `{ "type": "string", "format": "multiLine" }`
- `$v: date` - a string containing a date in [ISO8601 format](https://www.iso.org/iso-8601-date-and-time-format.html) e.g. `2018-11-13`
  JSON Schema `{ "type": "string", "format": "date" }`
- `$v: datetime` - a string containing a date & time in [ISO8601 format](https://www.iso.org/iso-8601-date-and-time-format.html) e.g. `2018-11-13T20:20:39+00:00.`
  JSON Schema `{ "type": "string", "format": "date-time" }`
- `$v: [ "GET", "POST" ]` - a string with a value from an enum list.
  JSON Schema `{ "type": "string", "default": "GET", "enum": [ "GET", "POST" ] }`

## Object Type Definition

An object type can have a defined schema:
`$v: { FirstName: string, LastName: string, Age: Number }`

Schema:

```
{
	"FirstName":{"type":"string"},
	"LastName":{"type":"string"},
	"Age": {"type":"number"}
}
```

## Array Type Definition

Array definitions can be combined with other types to obtain types array:

- `$a: []` -> Schema: `{"type":"array"}`
- `$a: string[]` -> Schema:
  ```
  {
    "type":"array",
    "items":{
      "type":"string"
    }
  }
  ```
- `$a: { FirstName: string, LastName: string }[]` -> Schema:
  ```
  {
    "type":"array",
    "items":{
  	"type":"object",
  	"properties":{
        "FirstName":{"type":"string"},
        "LastName":{"type":"string"}
  	}
    }
  }
  ```

## Declarations

Types can be declared in function signatures or function return types.
Types are not currently available when declaring a variable inside a function.

### Type Declarations

ISL support `type` declarations that can be reused across the current file:

```
type customer as { FirstName: String };

// Function that receives one number and returns one `customer`
fun download(id: number): customer
{
	...
}
```

### Schema Type Imports

An ISL type can also be imported from a declared schema.
Note that the ISL will not attempt to download the declared schema and validate against it but the tooling around it (e.g. an IDE) can do that.

```
type customer from "https://...";

fun download(id: number): customer
{
	...
}
```

### Known Named Types

Since 2.4.0 ISL supports Named Type Declaration in which a specific entity can be declared as having a type that the host understands.

```
$account: mycompany.account = {
	id: {
		externalId: "1234"
	},
	accountNumber: "123",
	accountType: "DEPOSIT"
}
```

[Read more about Known Named Types](./namedtypes.md)
