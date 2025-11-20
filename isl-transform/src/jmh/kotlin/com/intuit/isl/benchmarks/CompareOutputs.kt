package com.intuit.isl.benchmarks

import com.bazaarvoice.jolt.Chainr
import com.bazaarvoice.jolt.JsonUtils
import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.TransformCompiler
import com.intuit.isl.utils.JsonConvert
import kotlinx.coroutines.runBlocking
import org.mvel2.MVEL
import java.io.File

/**
 * Compare the actual outputs of JOLT, ISL, and MVEL transformations
 */
fun main() {
    val resourcesDir = File("src/jmh/resources")
    
    // Load Shopify order JSON
    val shopifyOrderJson = File(resourcesDir, "shopify-order.json").readText()
    val shopifyOrderNode = JsonConvert.mapper.readTree(shopifyOrderJson)
    val shopifyOrderObject = JsonUtils.jsonToObject(shopifyOrderJson)
    val shopifyOrderMap = JsonConvert.mapper.readValue(shopifyOrderJson, Map::class.java) as Map<*, *>
    
    println("=" .repeat(100))
    println("JSON TRANSFORMATION OUTPUT COMPARISON")
    println("=" .repeat(100))
    println()
    
    // 1. JOLT Transformation
    println("1. JOLT TRANSFORMATION")
    println("-" .repeat(100))
    val joltSpecJson = File(resourcesDir, "shopify-transform.jolt").readText()
    val joltSpec = JsonUtils.jsonToList(joltSpecJson)
    val joltChainr = Chainr.fromSpec(joltSpec)
    val joltOutput = joltChainr.transform(shopifyOrderObject)
    val joltOutputJson = JsonConvert.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(joltOutput)
    println(joltOutputJson)
    println()
    
    // 2. ISL Simple Transformation
    println("2. ISL SIMPLE TRANSFORMATION")
    println("-" .repeat(100))
    val islSimpleScript = File(resourcesDir, "shopify-transform-simple.isl").readText()
    val islSimpleTransformer = TransformCompiler().compileIsl("shopify-simple", islSimpleScript)
    val islSimpleOutput = runBlocking {
        val context = OperationContext()
        context.setVariable("\$input", shopifyOrderNode)
        val result = islSimpleTransformer.runTransformAsync("run", context)
        result.result
    }
    val islSimpleOutputJson = JsonConvert.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(islSimpleOutput)
    println(islSimpleOutputJson)
    println()
    
    // 3. MVEL Transformation
    println("3. MVEL TRANSFORMATION")
    println("-" .repeat(100))
    val mvelScript = File(resourcesDir, "shopify-transform.mvel").readText()
    val mvelCompiled = MVEL.compileExpression(mvelScript)
    val vars = hashMapOf<String, Any?>("input" to shopifyOrderMap)
    val mvelOutput = MVEL.executeExpression(mvelCompiled, vars)
    val mvelOutputJson = JsonConvert.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mvelOutput)
    println(mvelOutputJson)
    println()
    
    // Summary
    println("=" .repeat(100))
    println("SUMMARY")
    println("=" .repeat(100))
    println("JOLT Output Size: ${joltOutputJson.length} characters")
    println("ISL Simple Output Size: ${islSimpleOutputJson.length} characters")
    println("MVEL Output Size: ${mvelOutputJson.length} characters")
    println()
    
    // Save to files
    File("jolt-output.json").writeText(joltOutputJson)
    File("isl-simple-output.json").writeText(islSimpleOutputJson)
    File("mvel-output.json").writeText(mvelOutputJson)
    
    println("✓ Outputs saved to:")
    println("  - jolt-output.json")
    println("  - isl-simple-output.json")
    println("  - mvel-output.json")
    println()
}

