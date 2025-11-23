---
title: ISL
nav_order: 1
description: "ISL is a low-code interpreted scripting language for easy JSON-to-JSON transformations. Simple, intuitive syntax with powerful features for data acquisition and transformation."
permalink: /
excerpt: "ISL is a low-code interpreted scripting language for easy JSON-to-JSON transformations. Simple, intuitive syntax with powerful features for data acquisition and transformation."
---

<div align="center">
  <img src="./img/isl_small.png" alt="ISL">
</div>

ISL is a low-code [interpreted scripting language](<https://en.wikipedia.org/wiki/Interpreter_(computing)>) and runtime container designed to provide developers and non-developers an easy way to write, test, and deploy user developed code inside any service.

The ISL supports an intuitive simplified syntax with features that make data acquisition and data transformations easy with minimal lines of code. In addition, the language supports easy extensibility allowing it to be used as a multi-purpose service extensibility language.

The ISL can be embedded in any JVM based project to provide runtime based extensibility through a fast and lightweight runtime.

## Overview

ISL is a [WYSIWYG](https://en.wikipedia.org/wiki/WYSIWYG) scripting language designed for data transformations of JSON to JSON objects.

⌨️ **If it looks like a JSON it's a valid ISL :)**

⏩ Checkout the [Overview](./overview.md) for examples and the basics of the ISL.

⏩ Checkout the [QuickStart Guide](quickstart.md) for how to use ISL in 2 minutes.

‼️ Checkout the [Best Practices](best-practices.md) to understand how to run ISL efficiently.

## Example

In the most simple form the ISL is a JSON transformation language:

Given Input JSON:

```json
{
    "title": "IPod Nano - 8GB",
    "body_html": "It's the small iPod with a big idea: Video.",
    "id": 632910392,
    "images": [
        {
            "id": 850703190,
            "src": "http://example.com/burton.jpg"
        }
      ],
    "options": {
        "name": "Color",
        "values": ["Pink", "Red", "Green", "Black"]
    },
    "status": "active",
    "tags": "Emotive, Flash Memory, MP3, Music",
    "updated_at": 1645004735,
    "vendor": "Apple"
}
```

And Transformation:
```isl
fun transform( $input ){
    return {
      // Simple JSON Path Selectors
      id: $input.id,
      // piped modifiers using `|`
      name: $input.title | trim,
      // easy string building using interpolation ` ... `
      short_description: `${ $input.title } by ${ $input.vendor }`,
      // child object building
      primary_image: {
          id: $input.images[0].id,
          url: $input.images[0].src
      },
      // conditional properties
      is_active: if( $input.status == "active" ) true else false,
      option_name: $input.options.name,
      // array to csv
      option_values: $input.options.values | join(','),
      // date processing
      updated: $input.updated_at | date.fromEpochSeconds | to.string("yyyy-MM-dd HH:mm")
    }
}
```

Will output:

```json
{
    "id": 632910392,
    "name": "IPod Nano - 8GB",
    "short_description": "IPod Nano - 8GB by Apple",
    "primary_image": {
        "id": 850703190,
        "url": "http://example.com/burton.jpg"
    },
    "is_active": true,
    "option_name": "Color",
    "option_values": "Pink,Red,Green,Black",
    "updated": "2022-02-47 09:45"
}
```

## Major Features

{% raw %}
- JSON Compatible [Object Building](./language/objects.md) `item: { field: true, "my-property": 12 }`.
- Comprehensive [Conditions, If/Else & Switch statements](./language/conditions.md) including [RegEx Switch Case](./language/conditions.md#regex)
- Script [Imports](./language/functions.md#imports).
- [Math Expressions](./language/math.md) `$total: {{ $amount * $quantity * 1.2 }}`.
- Functional Expressions for:
  - [Filters](./language/modifiers.md#filtering): `$positive: [ -1, 0, 1 ] | filter( $ > 0 )`.
  - [Reduce](./language/modifiers.md#reduce) `$total: [ 1, 2, 3, 4 ] | reduce( {{ $acc + $it }} )`.
{% endraw %}
  - [Map](./language/modifiers.md#map) `item: [ 1, 2, 3, 4 ] | map( { id : $ } )`.
- Easily Extensible (from Java/Kotlin or ISL) with
  - [functions](./language/functions.md): `@.Service.Function( ... )`.
  - [modifiers](./language/functions.md#modifiers): `| calculate_tax( ... )`.
  - [wildcard modifiers](./language/functions.md#modifiers): `| encode.base64( ... )`.
  - [block statement extensions](./language/functions.md#blockstatement-functions) similar [to pagination support](./advanced/pagination.md) `@.Pagination.Page() { ... block ... }`.
- Pagination Strategies for [Page](./advanced/pagination.md#page), [Cursor](./advanced/pagination.md#cursor) or [Date Ranges](./advanced/pagination.md#date).
- Utilities for dealing with [Time & Dates](./types/dates.md#timedate-processing), [Signatures & Hashing](./advanced/crypto.md#cryptography).
- Support for [parsing XML](./types/xml.md#xml-processing) and [outputting XML](./types/xml.md#xml-output), [parsing CSV](./types/csv.md#csv-processing) or yaml.
- Support for advanced String Interpolation `Hi there $name. Today is ${ @.Date.Now() | to.string("yyyy MM dd") }. `.
- Support for [`find`, `match` and `replace` using Regular Expressions](./language/modifiers.md#regex-processing).

## Learning And Support

See the documentation in this repository for comprehensive guides and examples.

## JSON DSL
For Kotlin users ISL also bring a new [JSON based DSL](./dsl.md) to simplify the creation of JSON nodes from Kotlin:

```kotlin
import com.intuit.isl.dsl.*

val value = node {
    put ("first name", "John")
    put ("last name", "Smith")
    put ("age", 23 )

    array ("addresses")[
        node ("work"){
            put("line1", "123 Intuit Way")
        }
    ]
}


val jsonNode = value.node;  // access the internal JsonNode that was generated
```


## Latest Version

Check the releases page for the latest version.

## Change Log

Checkout out the [changelog](./changelog.md) for the changes.
