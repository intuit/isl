package com.intuit.isl.benchmarks

import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.VariableWithPathSelectorValueCommand
import com.intuit.isl.utils.JsonConvert
import com.jayway.jsonpath.JsonPath
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OperationsPerInvocation
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * Measures the cost of [JsonPath.compile] on every modifier-style invocation vs reusing a pre-compiled path.
 *
 * Before the engine change, `| unique($.id)` / `| select($.path)` compiled the path on each modifier call.
 * Static paths are now compiled once in [com.intuit.isl.commands.builder.ExecutionBuilder] and stored on
 * [com.intuit.isl.commands.modifiers.HardwiredModifierValueCommand.precompiledModifierJsonPath].
 *
 * This benchmark models **one modifier invocation** (e.g. inside a `foreach` body), not one element inside `unique`.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
open class JsonPathModifierBenchmark {

    private lateinit var doc: ObjectNode
    private val pathStr = "$.nested.id"
    private lateinit var precompiled: JsonPath
    private val cfg = VariableWithPathSelectorValueCommand.configuration

    @Setup
    fun setup() {
        doc = JsonConvert.mapper.createObjectNode()
        doc.putObject("nested").put("id", 42)
        precompiled = JsonPath.compile(pathStr)
    }

    /**
     * Legacy behavior: compile + read on every simulated modifier call.
     * [OperationsPerInvocation] makes JMH report average time **per compile+read** (not per batch).
     */
    @Benchmark
    @OperationsPerInvocation(BATCH)
    fun compileEachInvocation(bh: Blackhole) {
        repeat(BATCH) {
            val p = JsonPath.compile(pathStr)
            bh.consume(p.read<Any?>(doc, cfg))
        }
    }

    /**
     * Optimized: one compile in [setup], read only per simulated modifier call.
     */
    @Benchmark
    @OperationsPerInvocation(BATCH)
    fun precompiledReadOnly(bh: Blackhole) {
        repeat(BATCH) {
            bh.consume(precompiled.read<Any?>(doc, cfg))
        }
    }

    private companion object {
        const val BATCH = 500
    }
}
