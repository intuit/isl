---
title: Quick Start
nav_order: 2
description: "Get up and running with ISL in just 5 minutes! Learn how to add ISL to your project and transform JSON data with simple, intuitive syntax."
excerpt: "Get up and running with ISL in just 5 minutes! Learn how to add ISL to your project and transform JSON data with simple, intuitive syntax."
---

Get up and running with ISL Transform in just 5 minutes!

## What is ISL?

ISL is a JSON-to-JSON transformation language that makes data transformations simple and intuitive. If it looks like JSON, it's JSON!

## Step 1: Add ISL to Your Project

### Maven
```xml
<dependency>
    <groupId>com.intuit.isl</groupId>
    <artifactId>isl-transform</artifactId>
    <version>[version]</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.intuit.isl:isl-transform:[version]")
}
```

**Requirements:** Java 21 or higher

## Step 2: Write Your First Transformation

Let's transform a product JSON into a simpler format.

**Input JSON:**
```json
{
  "title": "IPod Nano - 8GB",
  "id": 632910392,
  "vendor": "Apple",
  "status": "active",
  "tags": "Emotive, Flash Memory, MP3, Music"
}
```

**ISL Transformation:**
```isl
{
    id: $input.id,
    name: $input.title,
    description: `${$input.title} by ${$input.vendor}`,
    isActive: $input.status == 'active',
    tags: $input.tags | split(',') | map( $ | trim )
}
```

**Output JSON:**
```json
{
  "id": 632910392,
  "name": "IPod Nano - 8GB",
  "description": "IPod Nano - 8GB by Apple",
  "isActive": true,
  "tags": ["Emotive", "Flash Memory", "MP3", "Music"]
}
```

## Step 3: Run It in Java/Kotlin

### Java Example
```java
import com.intuit.isl.common.OperationContext;
import com.intuit.isl.runtime.TransformCompiler;
import com.intuit.isl.runtime.ITransformer;
import com.intuit.isl.utils.JsonConvert;
import com.fasterxml.jackson.databind.JsonNode;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        // 1. Define your ISL script
        String islScript = """
            {
                id: $input.id,
                name: $input.title,
                description: `${ $input.title } by ${ $input.vendor }`,
                isActive: $input.status == 'active',
                tags: $input.tags | split(',') | map( $ | trim )
            }
            """;
        
        // 2. Compile the script (do this once, reuse many times!)
        TransformCompiler compiler = new TransformCompiler();
        // Make sure you cache this transformer if you plan to reuse it
        ITransformer transformer = compiler.compileIsl("product-transform", islScript);
        
        // 3. Prepare your input data
        String inputJson = """
            {
              "title": "IPod Nano - 8GB",
              "id": 632910392,
              "vendor": "Apple",
              "status": "active",
              "tags": "Emotive, Flash Memory, MP3, Music"
            }
            """;
        
        // 4. Create context and set variables
        // Make sure you don't cache this context. This is unique per execution
        OperationContext context = new OperationContext();
        context.setVariable("$input", JsonConvert.convert(inputJson));
        
        // 5. Execute the transformation
        JsonNode result = transformer.runTransformSync("run", context);
        
        // 6. Print the result
        System.out.println(JsonConvert.mapper.writerWithDefaultPrettyPrinter()
                                           .writeValueAsString(result));
    }
}
```

### Kotlin Example
```kotlin
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.TransformCompiler
import com.intuit.isl.utils.JsonConvert

fun main() {
    // 1. Define your ISL script
    val islScript = """
        {
            id: ${'$'}input.id,
            name: ${'$'}input.title,
            description: `${'$'}{${'$'}input.title} by ${'$'}{${'$'}input.vendor}`,
            isActive: ${'$'}input.status == 'active',
            tags: ${'$'}input.tags | split(',') | map( ${'$'} | trim )
        }
    """.trimIndent()
    
    // 2. Compile the script (do this once, reuse many times!)
    val compiler = TransformCompiler()
    val transformer = compiler.compileIsl("product-transform", islScript)
    
    // 3. Prepare your input data
    val inputJson = """
        {
          "title": "IPod Nano - 8GB",
          "id": 632910392,
          "vendor": "Apple",
          "status": "active",
          "tags": "Emotive, Flash Memory, MP3, Music"
        }
    """.trimIndent()
    
    // 4. Create context and set variables
    val context = OperationContext()
    context.setVariable("\$input", JsonConvert.convert(inputJson))
    
    // 5. Execute the transformation
    val result = transformer.runTransformSync("run", context)
    
    // 6. Print the result
    println(JsonConvert.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result))
}
```

## 🎯 Key Concepts

- **Variables** start with `$`: `$input`, `$name`, `$total`
- **Modifiers** use pipes `|`: `$text | trim | upperCase`
- **String interpolation** uses backticks: `` `Hello ${$name}!` ``
- **Arrays** use `[...]`: `[1, 2, 3]`
- **Objects** use `{...}`: `{ name: $value }`
- **Functions** use `@.`: `@.Date.Now()`

## ⚡ Performance Tips

- **Pre-compile once, reuse many times**: Compilation is expensive (~0.5ms), execution is fast (~0.03ms)
- **Don't cache OperationContext**: Create a new context for each transformation
- **Pre-compiled transformations are 19x faster** than compiling every time!

## 📚 Next Steps

Now that you've got the basics, explore more features:

1. **[Language Overview](./overview.md)** - Learn ISL syntax and structure
2. **[Modifiers Reference](./language/modifiers.md)** - 100+ built-in modifiers for strings, arrays, objects, dates, and more
3. **[Functions](./language/functions.md)** - Create reusable functions and custom modifiers
4. **[Conditionals](./language/conditions.md)** - If/else, switch statements, and regex matching
5. **[Math Operations](./language/math.md)** - Math expressions and functions
6. **[Best Practices](./dev/best-practices.md)** - Performance optimization and patterns
7. **[Examples](./examples/index.md)** - Real-world transformation examples

## 🚀 What Can ISL Do?

- ✅ Transform JSON to JSON with minimal code
- ✅ Parse and format dates with timezone support
- ✅ Process arrays with map, filter, reduce
- ✅ Handle XML and CSV conversions
- ✅ Perform cryptographic operations (SHA, HMAC, Base64)
- ✅ Execute complex conditionals and loops
- ✅ Call custom Java/Kotlin functions
- ✅ **29,000+ transformations per second** (single-threaded, pre-compiled)

## 💡 Common Use Cases

- **API Response Mapping**: Transform external API responses to your internal format
- **Data Enrichment**: Add calculated fields, format data, apply business rules
- **ETL Pipelines**: Extract, transform, and load data between systems
- **Configuration-Driven Transformations**: Let users define transformations without code
- **Microservice Integration**: Normalize data between different service contracts

## 🆘 Need Help?

- **Documentation**: Check the [full documentation](./index.md)
- **Examples**: Browse [example transformations](./examples/index.md)
- **Support**: See [support options](./support.md)

---

**Ready to dive deeper?** Start with the [Language Overview](./overview.md) or explore the [Modifiers Reference](./language/modifiers.md)!

