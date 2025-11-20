package com.intuit.isl.benchmarks

import com.bazaarvoice.jolt.Chainr
import com.bazaarvoice.jolt.JsonUtils
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.TransformCompiler
import com.intuit.isl.utils.JsonConvert
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() = runBlocking {
    val mapper = ObjectMapper()
    val resourcesDir = File("src/jmh/resources")
    
    println("=" * 100)
    println("GENERATING TRANSFORMATION OUTPUTS FOR COMPARISON")
    println("=" * 100)
    println()

    // Load Shopify order JSON
    val shopifyOrderJson = File(resourcesDir, "shopify-order.json").readText()
    val shopifyOrderNode = mapper.readTree(shopifyOrderJson)
    val shopifyOrderObject = JsonUtils.jsonToObject(shopifyOrderJson)

    // 1. JOLT Transformation
    println("1. Running JOLT transformation...")
    val joltSpecJson = File(resourcesDir, "shopify-transform.jolt").readText()
    val joltSpec = JsonUtils.jsonToList(joltSpecJson)
    val joltChainr = Chainr.fromSpec(joltSpec)
    val joltOutput = joltChainr.transform(shopifyOrderObject)
    val joltOutputJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(joltOutput)
    File("output-jolt.json").writeText(joltOutputJson)
    println("   ✓ Saved to: output-jolt.json (${joltOutputJson.length} bytes)")
    println()

    // 2. ISL Simple Transformation
    println("2. Running ISL Simple transformation...")
    val islSimpleScript = File(resourcesDir, "shopify-transform-simple.isl").readText()
    val islSimpleTransformer = TransformCompiler().compileIsl("shopify-simple", islSimpleScript)
    val islSimpleContext = OperationContext()
    islSimpleContext.setVariable("\$input", shopifyOrderNode)
    val islSimpleResult = islSimpleTransformer.runTransformAsync("run", islSimpleContext)
    val islSimpleOutputJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(islSimpleResult.result)
    File("output-isl-simple.json").writeText(islSimpleOutputJson)
    println("   ✓ Saved to: output-isl-simple.json (${islSimpleOutputJson.length} bytes)")
    println()

    // 3. ISL Complex Transformation
    println("3. Running ISL Complex transformation...")
    val islComplexScript = File(resourcesDir, "shopify-transform.isl").readText()
    val islComplexTransformer = TransformCompiler().compileIsl("shopify-complex", islComplexScript)
    val islComplexContext = OperationContext()
    islComplexContext.setVariable("\$input", shopifyOrderNode)
    val islComplexResult = islComplexTransformer.runTransformAsync("run", islComplexContext)
    val islComplexOutputJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(islComplexResult.result)
    File("output-isl-complex.json").writeText(islComplexOutputJson)
    println("   ✓ Saved to: output-isl-complex.json (${islComplexOutputJson.length} bytes)")
    println()

    println("=" * 100)
    println("✓ All outputs generated successfully!")
    println("=" * 100)
}

private operator fun String.times(count: Int): String = this.repeat(count)

