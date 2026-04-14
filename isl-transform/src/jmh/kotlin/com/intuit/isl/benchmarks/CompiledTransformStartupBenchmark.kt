package com.intuit.isl.benchmarks

import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.FileInfo
import com.intuit.isl.runtime.ITransformer
import com.intuit.isl.runtime.TransformCompiler
import com.intuit.isl.runtime.TransformPackageBuilder
import com.intuit.isl.utils.JsonConvert
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Compares **cold load + one transform** for the same Shopify ISL scripts:
 *
 * - **Compile from source**: parse + [com.intuit.isl.commands.builder.ExecutionBuilder] + one `runTransformSync`
 * - **Load pre-compiled**: read pre-compiled bytes (built once in trial setup) + one `runTransformSync`
 *
 * Pre-compiled bytes are produced offline in setup (no [TransformPackageBuilder.preCompileToBytes] in the timed path)
 * so this measures shipping `.islc` vs shipping `.isl` and compiling at runtime.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
open class CompiledTransformStartupBenchmark {

    private val moduleSimple = "shopify-simple"
    private val moduleComplexClean = "shopify-complex-clean"

    private lateinit var shopifyOrderNode: JsonNode
    private lateinit var islSimpleScript: String
    private lateinit var islComplexCleanScript: String
    private lateinit var preCompiledSimpleBytes: ByteArray
    private lateinit var preCompiledComplexCleanBytes: ByteArray

    @Setup(Level.Trial)
    fun setup() {
        val resourcesDir = File("src/jmh/resources")
        val shopifyOrderJson = File(resourcesDir, "shopify-order.json").readText()
        shopifyOrderNode = JsonConvert.mapper.readTree(shopifyOrderJson)
        islSimpleScript = File(resourcesDir, "shopify-transform-simple.isl").readText()
        islComplexCleanScript = File(resourcesDir, "shopify-transform-complex.isl").readText()

        preCompiledSimpleBytes = TransformPackageBuilder().preCompileToBytes(
            mutableListOf(FileInfo(moduleSimple, islSimpleScript))
        )
        preCompiledComplexCleanBytes = TransformPackageBuilder().preCompileToBytes(
            mutableListOf(FileInfo(moduleComplexClean, islComplexCleanScript))
        )
    }

    @Benchmark
    fun islSimple_compileFromSourceThenRun(): JsonNode? {
        val transformer = TransformCompiler().compileIsl(moduleSimple, islSimpleScript)
        return runOnce(transformer)
    }

    @Benchmark
    fun islSimple_loadPreCompiledThenRun(): JsonNode? {
        val pkg = TransformPackageBuilder.loadCompiled(preCompiledSimpleBytes)
        val transformer = pkg.getModule(moduleSimple)!!
        return runOnce(transformer)
    }

    @Benchmark
    fun islComplexClean_compileFromSourceThenRun(): JsonNode? {
        val transformer = TransformCompiler().compileIsl(moduleComplexClean, islComplexCleanScript)
        return runOnce(transformer)
    }

    @Benchmark
    fun islComplexClean_loadPreCompiledThenRun(): JsonNode? {
        val pkg = TransformPackageBuilder.loadCompiled(preCompiledComplexCleanBytes)
        val transformer = pkg.getModule(moduleComplexClean)!!
        return runOnce(transformer)
    }

    private fun runOnce(transformer: ITransformer): JsonNode? {
        val context = OperationContext()
        context.setVariable("\$input", shopifyOrderNode)
        return transformer.runTransformSync("run", context)
    }

}
