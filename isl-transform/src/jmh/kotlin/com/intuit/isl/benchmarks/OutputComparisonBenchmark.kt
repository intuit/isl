package com.intuit.isl.benchmarks

import com.bazaarvoice.jolt.Chainr
import com.bazaarvoice.jolt.JsonUtils
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.ITransformer
import com.intuit.isl.runtime.TransformCompiler
import com.intuit.isl.utils.JsonConvert
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.*
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * This benchmark is designed to run once and save the outputs of all three transformations
 * for comparison purposes.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
@Fork(1)
open class OutputComparisonBenchmark {

    private lateinit var shopifyOrderJson: String
    private lateinit var shopifyOrderObject: Any // For JOLT
    private lateinit var shopifyOrderNode: JsonNode // For ISL
    private lateinit var islSimpleScript: String
    private lateinit var islComplexScript: String
    private lateinit var joltSpec: List<Any>
    private lateinit var islSimpleTransformer: ITransformer
    private lateinit var islComplexTransformer: ITransformer
    private lateinit var joltChainr: Chainr
    private val mapper = ObjectMapper()

    @Setup(Level.Trial)
    fun setup() {
        val resourcesDir = File("src/jmh/resources")
        
        // Load Shopify order JSON
        shopifyOrderJson = File(resourcesDir, "shopify-order.json").readText()
        shopifyOrderNode = JsonConvert.convert(shopifyOrderJson)
        shopifyOrderObject = JsonUtils.jsonToObject(shopifyOrderJson)

        // Load and compile simple ISL script
        islSimpleScript = File(resourcesDir, "shopify-transform-simple.isl").readText()
        islSimpleTransformer = TransformCompiler().compileIsl("shopify-simple", islSimpleScript)
        
        // Load and compile complex ISL script
        islComplexScript = File(resourcesDir, "shopify-transform.isl").readText()
        islComplexTransformer = TransformCompiler().compileIsl("shopify-complex", islComplexScript)
        
        // Load and compile JOLT spec
        val joltSpecJson = File(resourcesDir, "shopify-transform.jolt").readText()
        joltSpec = JsonUtils.jsonToList(joltSpecJson)
        joltChainr = Chainr.fromSpec(joltSpec)
    }

    @Benchmark
    fun saveAllOutputs() = runBlocking {
        println("=" * 100)
        println("SAVING TRANSFORMATION OUTPUTS FOR COMPARISON")
        println("=" * 100)
        println()

        // 1. JOLT Transformation
        println("1. Running JOLT transformation...")
        val joltOutput = joltChainr.transform(shopifyOrderObject)
        val joltOutputJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(joltOutput)
        File("output-jolt.json").writeText(joltOutputJson)
        println("   ✓ Saved to: output-jolt.json")
        println()

        // 2. ISL Simple Transformation
        println("2. Running ISL Simple transformation...")
        val islSimpleContext = OperationContext()
        islSimpleContext.setVariable("\$input", shopifyOrderNode)
        val islSimpleResult = islSimpleTransformer.runTransformAsync("run", islSimpleContext)
        val islSimpleOutputJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(islSimpleResult.result)
        File("output-isl-simple.json").writeText(islSimpleOutputJson)
        println("   ✓ Saved to: output-isl-simple.json")
        println()

        // 3. ISL Complex Transformation
        println("3. Running ISL Complex transformation...")
        val islComplexContext = OperationContext()
        islComplexContext.setVariable("\$input", shopifyOrderNode)
        val islComplexResult = islComplexTransformer.runTransformAsync("run", islComplexContext)
        val islComplexOutputJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(islComplexResult.result)
        File("output-isl-complex.json").writeText(islComplexOutputJson)
        println("   ✓ Saved to: output-isl-complex.json")
        println()

        // 4. Summary
        println("=" * 100)
        println("SUMMARY")
        println("=" * 100)
        
        val joltFields = (joltOutput as? Map<*, *>)?.keys?.size ?: 0
        val islSimpleNode = islSimpleResult.result as? JsonNode
        val islSimpleFields = islSimpleNode?.fieldNames()?.asSequence()?.count() ?: 0
        val islComplexNode = islComplexResult.result as? JsonNode
        val islComplexFields = islComplexNode?.fieldNames()?.asSequence()?.count() ?: 0
        
        println("JOLT Output:")
        println("  - File: output-jolt.json")
        println("  - Top-level fields: $joltFields")
        println("  - Size: ${joltOutputJson.length} bytes")
        println()
        
        println("ISL Simple Output:")
        println("  - File: output-isl-simple.json")
        println("  - Top-level fields: $islSimpleFields")
        println("  - Size: ${islSimpleOutputJson.length} bytes")
        println()
        
        println("ISL Complex Output:")
        println("  - File: output-isl-complex.json")
        println("  - Top-level fields: $islComplexFields")
        println("  - Size: ${islComplexOutputJson.length} bytes")
        println()
        
        println("=" * 100)
        println("✓ All outputs saved successfully!")
        println("=" * 100)
    }

    private operator fun String.times(count: Int): String = this.repeat(count)
}

