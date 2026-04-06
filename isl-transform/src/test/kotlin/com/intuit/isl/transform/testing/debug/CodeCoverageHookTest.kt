package com.intuit.isl.transform.testing.debug

import com.intuit.isl.common.OperationContext
import com.intuit.isl.debug.CodeCoverageHook
import com.intuit.isl.runtime.TransformCompiler
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class CodeCoverageHookTest {

    @Test
    fun recordsHitsForExecutedStatements() = runBlocking {
        val moduleName = "covtest"
        val script = """
            fun run() {
                a: 1;
                b: 2;
                result: ${'$'}a;
            }
        """.trimIndent()
        val transformer = TransformCompiler().compileIsl(moduleName, script)
        val coverage = CodeCoverageHook()
        transformer.runTransformAsync("run", OperationContext(), coverage)
        val snap = coverage.lineHitsSnapshot()
        val total = snap.values.sumOf { lines -> lines.values.sum() }
        assertTrue(total > 0, "expected some line hits, got $snap")
        assertTrue(
            snap.containsKey(moduleName),
            "expected file key '$moduleName' in ${snap.keys}"
        )

        val byStatement = coverage.statementHitsSnapshot()
        assertTrue(byStatement.isNotEmpty(), "expected statement-level hits")
        assertTrue(byStatement.all { it.statementId > 0 }, "expected build-time statement ids, got $byStatement")
        assertTrue(byStatement.any { it.commandKind == "AssignPropertyCommand" }, "expected property assignments in $byStatement")
    }
}
