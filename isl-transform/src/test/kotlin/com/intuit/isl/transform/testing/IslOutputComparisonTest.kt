package com.intuit.isl.transform.testing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.TransformCompiler
import com.intuit.isl.utils.JsonConvert
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File

class IslOutputComparisonTest {

    private val mapper = ObjectMapper()

    @Test
    fun `compare ISL Simple vs ISL Complex outputs`() = runBlocking {
        // Load input
        val resourcesDir = File("src/jmh/resources")
        val shopifyOrderJson = File(resourcesDir, "shopify-order.json").readText()
        val shopifyOrderNode = JsonConvert.convert(shopifyOrderJson)

        println("=" * 100)
        println("ISL TRANSFORMATION OUTPUT COMPARISON")
        println("=" * 100)
        println()

        // 1. ISL Simple Transformation
        println("1. ISL SIMPLE TRANSFORMATION OUTPUT (matching JOLT capabilities):")
        println("-" * 100)
        val islSimpleScript = File(resourcesDir, "shopify-transform-simple.isl").readText()
        val islSimpleTransformer = TransformCompiler().compileIsl("shopify-simple", islSimpleScript)
        val islSimpleContext = OperationContext()
        islSimpleContext.setVariable("\$input", shopifyOrderNode)
        val islSimpleResult = islSimpleTransformer.runTransformAsync("run", islSimpleContext)
        val islSimpleOutputJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(islSimpleResult.result)
        println(islSimpleOutputJson)
        println()
        println()

        // 2. ISL Complex Transformation
        println("2. ISL COMPLEX TRANSFORMATION OUTPUT (full features):")
        println("-" * 100)
        val islComplexScript = File(resourcesDir, "shopify-transform.isl").readText()
        val islComplexTransformer = TransformCompiler().compileIsl("shopify-complex", islComplexScript)
        val islComplexContext = OperationContext()
        islComplexContext.setVariable("\$input", shopifyOrderNode)
        val islComplexResult = islComplexTransformer.runTransformAsync("run", islComplexContext)
        val islComplexOutputJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(islComplexResult.result)
        println(islComplexOutputJson)
        println()
        println()

        // 3. Field Count Comparison
        println("3. FIELD COUNT COMPARISON:")
        println("-" * 100)
        val islSimpleNode = islSimpleResult.result as? JsonNode
        val islSimpleFields = islSimpleNode?.fieldNames()?.asSequence()?.count() ?: 0
        val islComplexNode = islComplexResult.result as? JsonNode
        val islComplexFields = islComplexNode?.fieldNames()?.asSequence()?.count() ?: 0
        
        println("ISL Simple Output Fields: $islSimpleFields")
        println("ISL Complex Output Fields: $islComplexFields")
        println()

        // 4. Key Comparison
        println("4. TOP-LEVEL KEYS COMPARISON:")
        println("-" * 100)
        val islSimpleKeys = islSimpleNode?.fieldNames()?.asSequence()?.toList()?.sorted() ?: emptyList()
        val islComplexKeys = islComplexNode?.fieldNames()?.asSequence()?.toList()?.sorted() ?: emptyList()
        
        println("ISL Simple Keys (${islSimpleKeys.size}):")
        islSimpleKeys.forEach { println("  - $it") }
        println()
        
        println("ISL Complex Keys (${islComplexKeys.size}):")
        islComplexKeys.forEach { println("  - $it") }
        println()

        // 5. Keys in both
        println("5. KEYS IN BOTH (should match for basic fields):")
        println("-" * 100)
        val commonKeys = islSimpleKeys.filter { it in islComplexKeys }
        commonKeys.forEach { println("  - $it") }
        println()

        // 6. Keys only in ISL Simple
        println("6. KEYS ONLY IN ISL SIMPLE:")
        println("-" * 100)
        val onlyInSimple = islSimpleKeys.filter { it !in islComplexKeys }
        if (onlyInSimple.isEmpty()) {
            println("  (none)")
        } else {
            onlyInSimple.forEach { println("  - $it") }
        }
        println()

        // 7. Keys only in ISL Complex
        println("7. ADDITIONAL KEYS IN ISL COMPLEX:")
        println("-" * 100)
        val onlyInComplex = islComplexKeys.filter { it !in islSimpleKeys }
        if (onlyInComplex.isEmpty()) {
            println("  (none)")
        } else {
            onlyInComplex.forEach { println("  - $it") }
        }
        println()

        // 8. Save outputs to files for manual inspection
        File("output-isl-simple.json").writeText(islSimpleOutputJson)
        File("output-isl-complex.json").writeText(islComplexOutputJson)
        println("8. OUTPUT FILES SAVED:")
        println("-" * 100)
        println("  - output-isl-simple.json")
        println("  - output-isl-complex.json")
        println()

        println("=" * 100)
        println("COMPARISON COMPLETE")
        println("=" * 100)
    }

    private operator fun String.times(count: Int): String = this.repeat(count)
}

