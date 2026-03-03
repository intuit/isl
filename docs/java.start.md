---
title: Java/Kotlin Integration
nav_order: 4
description: "Learn how to embed ISL in your Java/Kotlin project. Add ISL transformation capabilities to your JVM application with simple Maven or Gradle integration."
excerpt: "Learn how to embed ISL in your Java/Kotlin project. Add ISL transformation capabilities to your JVM application with simple Maven or Gradle integration."
---

This guide shows you how to embed ISL in your Java/Kotlin project.

## Prerequisites

- Java 21 or higher
- Maven or Gradle build tool

## Maven Dependency

```xml
<dependency>
    <groupId>com.intuit.isl</groupId>
    <artifactId>isl-transform</artifactId>
    <version>[Version]</version>
</dependency>
```

## Gradle Dependency

```kotlin
dependencies {
    implementation("com.intuit.isl:isl-transform:1.1.0-SNAPSHOT")
}
```

## Hello World Example

### Java Example

```java
import com.intuit.isl.common.OperationContext;
import com.intuit.isl.runtime.TransformCompiler;
import com.intuit.isl.runtime.Transformer;
import com.intuit.isl.utils.JsonConvert;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IslExample {
    public static void main(String[] args) throws Exception {
        // 1. Define your ISL script
        String islScript = """
            fun run($name) {
                greeting: "Hello, $name!",
                timestamp: @.Date.Now()
            }
            """;
        
        // 2. Compile the script
        // Important: In real life scenarios you want to only compile once then cache the transformer and reuse it as many times as possible
        TransformCompiler compiler = new TransformCompiler();
        Transformer transformer = compiler.compileIsl("hello", islScript);

    
        // 3. Create operation context and set variables
        // Important: Do not cache the OperationContext between operations. It is not thread safe. 
        // Every thread and every request should have its own OperationContext
        OperationContext context = new OperationContext();
        context.setVariable("$name", JsonConvert.convert("World"));
        
        
        // 4. Execute the transformation
        // Important: This is thead safe as long as the context is unique per thread.
        JsonNode result = transformer.runTransformSync("run", context);
        
        // 5. Print the result
        ObjectMapper mapper = new ObjectMapper();
        String jsonOutput = mapper.writerWithDefaultPrettyPrinter()
                                   .writeValueAsString(result);
        System.out.println(jsonOutput);
    }
}
```

**Output:**
```json
{
  "greeting": "Hello, World!",
  "timestamp": "2024-11-16T10:30:00.000Z"
}
```

### Kotlin Example

```kotlin
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.TransformCompiler
import com.intuit.isl.utils.JsonConvert
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 1. Define your ISL script
    val islScript = """
        fun run(${"$"}name) {
            greeting: "Hello, ${"$"}name!",
            timestamp: @.Date.Now()
        }
    """.trimIndent()
    
    // 2. Compile the script
    val compiler = TransformCompiler()
    val transformer = compiler.compileIsl("hello", islScript)
    
    // 3. Create operation context and set variables
    val context = OperationContext()
    context.setVariable("${"$"}name", JsonConvert.convert("World"))
    
    // 4. Execute the transformation
    val result = transformer.runTransformAsync("run", context)
    
    // 5. Print the result
    val mapper = jacksonObjectMapper()
    val jsonOutput = mapper.writerWithDefaultPrettyPrinter()
                          .writeValueAsString(result.result)
    println(jsonOutput)
}
```

## Adding Custom Functions

You can extend ISL by registering custom functions:

### Java

```java
OperationContext context = new OperationContext();

// Register a custom function
context.registerExtensionMethod("MyService.CalculateTax", (params) -> {
    Double amount = (Double) params[0];
    Double taxRate = (Double) params[1];
    return amount * taxRate;
});

// Use in ISL script:
// taxAmount: @.MyService.CalculateTax($amount, 0.08)
```

### Kotlin

```kotlin
val context = OperationContext()

// Register a custom function
context.registerExtensionMethod("MyService.CalculateTax") { params ->
    val amount = params[0] as Double
    val taxRate = params[1] as Double
    amount * taxRate
}

// Use in ISL script:
// taxAmount: @.MyService.CalculateTax($amount, 0.08)
```

## Adding Custom Modifiers

Register custom modifiers for fluent data transformations:

```kotlin
context.registerExtensionMethod("Modifier.taxAmount") { params ->
    val price = params[0] as Double
    val taxRate = params.getOrNull(1) as? Double ?: 0.08
    price * taxRate
}

// Use in ISL script:
// tax: $price | taxAmount(0.08)
```

## Working with JSON Data

### Loading JSON Input

```kotlin
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

val mapper = jacksonObjectMapper()
val jsonString = """{"name": "Alice", "age": 30}"""
val inputData: Map<String, Any> = mapper.readValue(jsonString)

val context = OperationContext()
context.setVariable("${"$"}input", JsonConvert.convert(inputData))
```

### Complex Transformation Example

{% raw %}
```kotlin
val islScript = """
    fun run(${"$"}orders) {
        totalOrders: ${"$"}orders | length,
        activeOrders: foreach ${"$"}order in ${"$"}orders | filter(${"$"}order.status == "active") {
            id: ${"$"}order.id,
            customer: ${"$"}order.customer,
            total: {{ ${"$"}order.quantity * ${"$"}order.price }}
        }
        endfor,
        grandTotal: ${"$"}orders | reduce({{ ${"$"}acc + (${"$"}it.quantity * ${"$"}it.price) }})
    }
""".trimIndent()

val compiler = TransformCompiler()
val transformer = compiler.compileIsl("orders", islScript)

val context = OperationContext()
context.setVariable("${"$"}orders", JsonConvert.convert(ordersData))

val result = transformer.runTransformSync("run", context)
```

{% endraw %}

## Async Execution

ISL supports async/coroutine execution for better performance:

```kotlin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

runBlocking {
    val results = (1..10).map { i ->
        async {
            val context = OperationContext()
            context.setVariable("${"$"}id", JsonConvert.convert(i))
            transformer.runTransformAsync("run", context)
        }
    }.awaitAll()
    
    results.forEach { println(it.result) }
}
```

## Error Handling

```kotlin
try {
    val transformer = compiler.compileIsl("script", islScript)
    val result = transformer.runTransformSync("run", context)
    println(result)
} catch (e: com.intuit.isl.runtime.TransformCompilationException) {
    println("Compilation error: ${e.message}")
    println("Position: ${e.position}")
} catch (e: com.intuit.isl.runtime.TransformException) {
    println("Runtime error: ${e.message}")
}
```

## Next Steps

- Explore the [Language Documentation](./index.md)
- Learn about [Modifiers](./language/modifiers.md)
- See [Examples](./examples/index.md)
- Check out [Custom Functions](./language/functions.md)

## Resources

- [GitHub Repository](https://github.com/intuit/isl)
- [Issue Tracker](https://github.com/intuit/isl/issues)

