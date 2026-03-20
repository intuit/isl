package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.TransformCompiler
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertNotSame
import kotlin.test.assertNull

/**
 * Compile-time constant object folding ([com.intuit.isl.commands.ConstantObjectBuildCommand]) must still
 * return a fresh [deepCopy] per execution so callers can mutate without cross-run aliasing.
 */
class ConstantObjectFoldTest {

    @Test
    fun literalRunResult_isIndependentPerTransformInvocation() = runBlocking {
        val script = """
            fun run() {
                result: { a: 1, nested: { b: 2 }, arr: [ 1, 2, 3 ] }
            }
        """.trimIndent()
        val t = TransformCompiler().compileIsl("constant-fold-test", script)
        val ctx = OperationContext()
        val r1 = t.runTransformAsync("run", ctx).result as ObjectNode
        val r2 = t.runTransformAsync("run", ctx).result as ObjectNode
        assertNotSame(r1, r2)
        r1.put("mutated", true)
        assertNull(r2.get("mutated"))
    }
}
