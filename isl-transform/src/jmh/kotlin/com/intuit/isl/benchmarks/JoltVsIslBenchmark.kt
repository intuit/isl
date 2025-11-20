package com.intuit.isl.benchmarks

import com.bazaarvoice.jolt.Chainr
import com.bazaarvoice.jolt.JsonUtils
import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.TransformCompiler
import com.intuit.isl.runtime.ITransformer
import com.intuit.isl.utils.JsonConvert
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.*
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Benchmark comparing JOLT vs ISL Transform performance
 * 
 * This benchmark compares the performance of:
 * - JOLT: A popular JSON-to-JSON transformation library
 * - ISL Simple: Basic field mapping (matching JOLT capabilities)
 * - ISL Complex (Verbose): Full features with many intermediate variables
 * - ISL Complex (Clean): Full features with inline transformations
 * 
 * Both perform similar transformations on a Shopify order JSON.
 * Note: JOLT is more limited and cannot perform all the operations ISL can,
 * so this is a simplified comparison focusing on basic field mapping.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
open class JoltVsIslBenchmark {

    private lateinit var shopifyOrderJson: String
    private lateinit var shopifyOrderObject: Any
    private lateinit var shopifyOrderNode: JsonNode
    private lateinit var islSimpleScript: String
    private lateinit var islComplexVerboseScript: String
    private lateinit var islComplexCleanScript: String
    private lateinit var joltSpec: List<Any>
    private lateinit var islSimpleTransformer: ITransformer
    private lateinit var islComplexVerboseTransformer: ITransformer
    private lateinit var islComplexCleanTransformer: ITransformer
    private lateinit var joltChainr: Chainr

    @Setup(Level.Trial)
    fun setup() {
        val resourcesDir = File("src/jmh/resources")
        
        // Load Shopify order JSON
        shopifyOrderJson = File(resourcesDir, "shopify-order.json").readText()
        shopifyOrderNode = JsonConvert.mapper.readTree(shopifyOrderJson)
        shopifyOrderObject = JsonUtils.jsonToObject(shopifyOrderJson)
        
        // Load and compile simple ISL script (matching JOLT capabilities)
        islSimpleScript = File(resourcesDir, "shopify-transform-simple.isl").readText()
        islSimpleTransformer = TransformCompiler().compileIsl("shopify-simple", islSimpleScript)
        
        // Load and compile complex verbose ISL script (full features with many variables)
        islComplexVerboseScript = File(resourcesDir, "shopify-transform.isl").readText()
        islComplexVerboseTransformer = TransformCompiler().compileIsl("shopify-complex-verbose", islComplexVerboseScript)
        
        // Load and compile complex clean ISL script (full features with inline transformations)
        islComplexCleanScript = File(resourcesDir, "shopify-transform-complex.isl").readText()
        islComplexCleanTransformer = TransformCompiler().compileIsl("shopify-complex-clean", islComplexCleanScript)
        
        // Load and compile JOLT spec
        val joltSpecJson = File(resourcesDir, "shopify-transform.jolt").readText()
        joltSpec = JsonUtils.jsonToList(joltSpecJson)
        joltChainr = Chainr.fromSpec(joltSpec)
    }

    /**
     * Benchmark: JOLT transformation (pre-compiled spec)
     * 
     * Measures JOLT's performance with a pre-compiled transformation spec.
     * This is the most common production scenario for JOLT.
     */
    @Benchmark
    fun joltTransformation(): Any {
        return joltChainr.transform(shopifyOrderObject)
    }

    /**
     * Benchmark: ISL Simple transformation (pre-compiled, matching JOLT capabilities)
     * 
     * Measures ISL's performance with a simple script that matches JOLT's capabilities.
     * Fair comparison: basic field mapping only, no modifiers or complex operations.
     */
    @Benchmark
    fun islSimpleTransformation(): JsonNode? = runBlocking {
        val context = OperationContext()
        context.setVariable("\$input", shopifyOrderNode)
        val result = islSimpleTransformer.runTransformAsync("run", context)
        result.result;
    }

    /**
     * Benchmark: ISL Complex Verbose transformation (pre-compiled, full features)
     * 
     * Measures ISL's performance with a complex script using all features.
     * Uses verbose style with many intermediate variables.
     * Shows ISL's capabilities: functions, modifiers, conditionals, aggregations.
     */
    @Benchmark
    fun islComplexVerboseTransformation(): JsonNode? = runBlocking {
        val context = OperationContext()
        context.setVariable("\$input", shopifyOrderNode)
        val result = islComplexVerboseTransformer.runTransformAsync("run", context)
        result.result;
    }

    /**
     * Benchmark: ISL Complex Clean transformation (pre-compiled, full features)
     * 
     * Measures ISL's performance with a complex script using all features.
     * Uses clean style with inline transformations and minimal variables.
     * Shows ISL's capabilities: functions, modifiers, conditionals, aggregations.
     */
    @Benchmark
    fun islComplexCleanTransformation(): JsonNode? = runBlocking {
        val context = OperationContext()
        context.setVariable("\$input", shopifyOrderNode)
        val result = islComplexCleanTransformer.runTransformAsync("run", context)
        result.result;
    }

    /**
     * Benchmark: JOLT full cycle (parse spec + transform)
     * 
     * Measures JOLT's performance including spec parsing.
     * This simulates the scenario where specs are not cached.
     */
    @Benchmark
    fun joltFullCycle(): Any {
        val chainr = Chainr.fromSpec(joltSpec)
        return chainr.transform(shopifyOrderObject)
    }

    /**
     * Benchmark: ISL Simple full cycle (parse + compile + transform)
     * 
     * Measures ISL's performance including script parsing and compilation.
     * Uses simple script matching JOLT capabilities.
     */
    @Benchmark
    fun islSimpleFullCycle(): JsonNode? = runBlocking {
        val transformer = TransformCompiler().compileIsl("shopify-simple", islSimpleScript)
        val context = OperationContext()
        context.setVariable("\$input", shopifyOrderNode)
        val result = transformer.runTransformAsync("run", context)
        result.result;
    }

    /**
     * Benchmark: ISL Complex Verbose full cycle (parse + compile + transform)
     * 
     * Measures ISL's performance including script parsing and compilation.
     * Uses complex verbose script with all features and many variables.
     */
    @Benchmark
    fun islComplexVerboseFullCycle(): JsonNode? = runBlocking {
        val transformer = TransformCompiler().compileIsl("shopify-complex-verbose", islComplexVerboseScript)
        val context = OperationContext()
        context.setVariable("\$input", shopifyOrderNode)
        val result = transformer.runTransformAsync("run", context)
        result.result;
    }

    /**
     * Benchmark: ISL Complex Clean full cycle (parse + compile + transform)
     * 
     * Measures ISL's performance including script parsing and compilation.
     * Uses complex clean script with all features and inline transformations.
     */
    @Benchmark
    fun islComplexCleanFullCycle(): JsonNode? = runBlocking {
        val transformer = TransformCompiler().compileIsl("shopify-complex-clean", islComplexCleanScript)
        val context = OperationContext()
        context.setVariable("\$input", shopifyOrderNode)
        val result = transformer.runTransformAsync("run", context)
        result.result;
    }
}

/**
 * Additional benchmark to compare simple field mapping scenarios
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
open class SimpleTransformBenchmark {

    private val simpleJson = """{"firstName":"John","lastName":"Doe","age":30,"email":"john@example.com"}"""
    private lateinit var simpleObject: Any
    private lateinit var simpleNode: JsonNode
    private lateinit var joltSimpleChainr: Chainr
    private lateinit var islSimpleTransformer: ITransformer

    @Setup(Level.Trial)
    fun setup() {
        simpleObject = JsonUtils.jsonToObject(simpleJson)
        simpleNode = JsonConvert.mapper.readTree(simpleJson)
        
        // Simple JOLT spec - just field mapping
        val joltSimpleSpec = """
        [{
            "operation": "shift",
            "spec": {
                "firstName": "name.first",
                "lastName": "name.last",
                "age": "age",
                "email": "contact.email"
            }
        }]
        """.trimIndent()
        joltSimpleChainr = Chainr.fromSpec(JsonUtils.jsonToList(joltSimpleSpec))
        
        // Simple ISL script - equivalent field mapping
        val islSimpleScript = """
        name: {
            first: ${'$'}input.firstName,
            last: ${'$'}input.lastName
        };
        age: ${'$'}input.age;
        contact: {
            email: ${'$'}input.email
        };
        """.trimIndent()
        islSimpleTransformer = TransformCompiler().compileIsl("simple", islSimpleScript)
    }

    @Benchmark
    fun joltSimpleTransform(): Any {
        return joltSimpleChainr.transform(simpleObject)
    }

    @Benchmark
    fun islSimpleTransform(): String = runBlocking {
        val context = OperationContext()
        context.setVariable("\$input", simpleNode)
        val result = islSimpleTransformer.runTransformSync("run", context)
        JsonConvert.mapper.writeValueAsString(result)
    }
}

