package com.intuit.isl.transform.precompile

import com.intuit.isl.runtime.FileInfo
import com.intuit.isl.runtime.TransformPackageBuilder
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue

/**
 * Compares wall-clock time to obtain a runnable [com.intuit.isl.runtime.TransformPackage]:
 * - **compile**: parse + ExecutionBuilder (via [TransformPackageBuilder])
 * - **load pre-compiled**: read pre-compiled protobuf + hardwired linking (no parse/ExecutionBuilder)
 *
 * Pre-compiled bytes are built once up front so load timing excludes [TransformPackageBuilder.preCompileToBytes].
 */
class CompiledTransformPreCompileLoadTimingTest {

    private val fixtureFiles = listOf(
        FileInfo(
            "main.isl",
            """
            import Lib from 'lib.isl';
            fun run( ${'$'}input ) {
                return @.Lib.echo( ${'$'}input );
            }
            """.trimIndent()
        ),
        FileInfo(
            "lib.isl",
            """
            fun echo( ${'$'}x ) {
                return ${'$'}x;
            }
            """.trimIndent()
        )
    )

    @Test
    fun reportCompilationVsPreCompiledLoadMillis() {
        val warmup = 5
        val iterations = 30

        val preCompiledBytes = TransformPackageBuilder().preCompileToBytes(fixtureFiles.toMutableList())

        repeat(warmup) {
            TransformPackageBuilder().build(fixtureFiles.map { FileInfo(it.name, it.contents) }.toMutableList())
            TransformPackageBuilder.loadCompiled(preCompiledBytes)
        }

        val compileMs = measureTimeMillis {
            repeat(iterations) {
                TransformPackageBuilder().build(fixtureFiles.map { FileInfo(it.name, it.contents) }.toMutableList())
            }
        }

        val loadPreCompiledMs = measureTimeMillis {
            repeat(iterations) {
                TransformPackageBuilder.loadCompiled(preCompiledBytes)
            }
        }

        val compileAvg = compileMs.toDouble() / iterations
        val loadPreCompiledAvg = loadPreCompiledMs.toDouble() / iterations

        println(
            "[CompiledTransformPreCompileLoadTimingTest] iterations=$iterations " +
                "compile avg=${"%.3f".format(compileAvg)} ms " +
                "loadPreCompiled avg=${"%.3f".format(loadPreCompiledAvg)} ms " +
                "(load uses pre-built bytes; no parse/ExecutionBuilder)"
        )

        assertTrue(
            loadPreCompiledAvg <= compileAvg * 2.0,
            "load pre-compiled avg $loadPreCompiledAvg ms unexpectedly slower than 2x compile avg $compileAvg ms"
        )
    }
}
