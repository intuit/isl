package com.intuit.isl.transform.testing.debug

import com.intuit.isl.commands.CoverageBranchLineAnalyzer
import com.intuit.isl.common.OperationContext
import com.intuit.isl.debug.CodeCoverageHook
import com.intuit.isl.runtime.TransformCompiler
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class CoverageBranchLineAnalyzerTest {

    /** AssignVariable / ConditionCommand spans cover the whole inline if; else literal must still get uncovered spans. */
    @Test
    fun emitsUncoveredSpansForElseLiteralWhenRhsIsInlineIf() = runBlocking {
        val moduleName = "branchgap"
        val script = """
            fun run() {
                ${'$'}x = if( 1 == 1 ) true else false;
            }
        """.trimIndent()
        val t = TransformCompiler().compileIsl(moduleName, script)
        val coverage = CodeCoverageHook()
        t.runTransformAsync("run", OperationContext(), coverage)
        val gap = CoverageBranchLineAnalyzer.branchGapAnalysis(listOf(t.module), coverage)
        assertTrue(gap.linesBranchPartial.isNotEmpty(), "expected partial branch lines")
        assertTrue(
            gap.uncoveredBranchSpans.isNotEmpty(),
            "expected uncovered branch spans for unevaluated else literal, got ${gap.uncoveredBranchSpans}"
        )
        val f = gap.uncoveredBranchSpans.first()
        assertTrue(f.endCharacter >= f.startCharacter && f.endLineNumber >= f.startLineNumber)
    }
}
