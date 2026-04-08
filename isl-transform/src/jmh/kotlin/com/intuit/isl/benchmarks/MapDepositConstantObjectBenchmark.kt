package com.intuit.isl.benchmarks

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.ITransformer
import com.intuit.isl.runtime.TransformCompiler
import com.intuit.isl.types.TypedJsonNodeFactory
import com.intuit.isl.utils.ConvertUtils
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * Microbenchmark for the `mapDepositTransactionType`-style pattern: large literal string→string map,
 * then a lookup modifier. Compares **current ISL** (object built via command tree each run) to a
 * simulation of **ConstantJsonCommand + [ObjectNode.deepCopy]** (prototype built once, copy + lookup per call).
 *
 * Run (from repo root):
 * `./gradlew :isl-transform:jmh -PjmhIncludes=MapDepositConstantObjectBenchmark`
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
open class MapDepositConstantObjectBenchmark {

    private lateinit var transformer: ITransformer
    private lateinit var operationContext: OperationContext
    private lateinit var prototype: ObjectNode

    @Setup
    fun setup() {
        transformer = TransformCompiler().compileIsl("mapDepositBench", ISL_SCRIPT)
        operationContext = OperationContext()
        operationContext.registerSyncExtensionMethod("modifier.getmappedvalue", ::getMappedValueStub)

        prototype = TypedJsonNodeFactory.instance.typedObjectNode(null)
        for ((k, v) in MAP_ENTRIES) {
            prototype.put(k, v)
        }
    }

    /** Current engine: compile-time tree, runtime builds the object + pipe + stub modifier each invocation. */
    @Benchmark
    fun islFullPipeline(bh: Blackhole) {
        bh.consume(transformer.runTransformSync("run", operationContext))
    }

    /**
     * Simulates successful constant-folding of the literal object: prototype built once in [setup],
     * each call only [deepCopy]s and runs the same lookup logic as [getMappedValueStub].
     */
    @Benchmark
    fun prebuiltDeepCopyAndLookup(bh: Blackhole) {
        val copy = prototype.deepCopy()
        bh.consume(lookupDepositMap(copy, LOOKUP_KEY))
    }

    /**
     * Lower bound if the map could be shared read-only (not safe with mutable [ObjectNode] today).
     * Included to show how much [deepCopy] costs relative to lookup-only.
     */
    @Benchmark
    fun prebuiltLookupOnlySharedPrototype(bh: Blackhole) {
        bh.consume(lookupDepositMap(prototype, LOOKUP_KEY))
    }

    /**
     * Pure Jackson: build the same map with [ObjectNode.set] in a loop each invocation (no ISL interpreter).
     * Separates "tree command execution" overhead from raw node construction.
     */
    @Benchmark
    fun jacksonManualRebuildAndLookup(bh: Blackhole) {
        val o = TypedJsonNodeFactory.instance.typedObjectNode(null)
        for ((k, v) in MAP_ENTRIES) {
            o.put(k, v)
        }
        bh.consume(lookupDepositMap(o, LOOKUP_KEY))
    }

    private companion object {
        const val LOOKUP_KEY = "DEPOSIT"

        val MAP_ENTRIES: List<Pair<String, String>> = listOf(
            "ADJUSTMENT" to "ADJUSTMENT",
            "ATMDEPOSIT" to "ATMDEPOSIT",
            "ATMWITHDRAWAL" to "ATMWITHDRAWAL",
            "BILLPAYMENT" to "BILLPAYMENT",
            "CHECK" to "CHECK",
            "DEPOSIT" to "DEPOSIT",
            "DIRECTDEPOSIT" to "DIRECTDEPOSIT",
            "FEE" to "FEE",
            "INTEREST" to "INTEREST",
            "OVERDRAFT" to "OVERDRAFT",
            "POSCREDIT" to "POSCREDIT",
            "POSDEBIT" to "POSDEBIT",
            "TRANSFER" to "TRANSFER",
            "WITHDRAWAL" to "WITHDRAWAL",
            "TAX" to "TAX",
            "*" to "OTHER",
        )

        val ISL_SCRIPT = """
            modifier mapDepositTransactionType( ${'$'}value ) {
                ${'$'}map = {
                    "ADJUSTMENT": "ADJUSTMENT",
                    "ATMDEPOSIT": "ATMDEPOSIT",
                    "ATMWITHDRAWAL": "ATMWITHDRAWAL",
                    "BILLPAYMENT": "BILLPAYMENT",
                    "CHECK": "CHECK",
                    "DEPOSIT": "DEPOSIT",
                    "DIRECTDEPOSIT": "DIRECTDEPOSIT",
                    "FEE": "FEE",
                    "INTEREST": "INTEREST",
                    "OVERDRAFT": "OVERDRAFT",
                    "POSCREDIT": "POSCREDIT",
                    "POSDEBIT": "POSDEBIT",
                    "TRANSFER": "TRANSFER",
                    "WITHDRAWAL": "WITHDRAWAL",
                    "TAX": "TAX",
                    "*": "OTHER",
                };
                return ${'$'}map | getMappedValue( 'DepositTransactionType', ${'$'}value );
            }

            fun run() {
                result: "DEPOSIT" | mapDepositTransactionType
            }
        """.trimIndent()
    }
}

private fun lookupDepositMap(map: ObjectNode, key: String): JsonNode? {
    val direct = map.get(key)
    if (direct != null && !direct.isNull) return direct
    return map.get("*")
}

private fun getMappedValueStub(c: FunctionExecuteContext): Any? {
    val map = c.firstParameter as? ObjectNode ?: return null
    val key = ConvertUtils.tryToString(c.parameters.getOrNull(2)) ?: return null
    return lookupDepositMap(map, key)
}
