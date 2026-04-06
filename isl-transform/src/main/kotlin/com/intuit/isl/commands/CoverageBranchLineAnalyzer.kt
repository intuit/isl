package com.intuit.isl.commands

import com.intuit.isl.debug.CodeCoverageHook
import com.intuit.isl.debug.SourceCoverageSpan
import com.intuit.isl.debug.StatementCoverageRecord
import com.intuit.isl.parser.tokens.ConditionToken
import com.intuit.isl.runtime.TransformModule
import java.util.IdentityHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * Single-run (or aggregated test) coverage cannot prove both branches of `if ... else` ran.
 * We flag source lines in those constructs for gutter partial styling, and emit precise spans for the
 * branch subtree that had **no** statement hits when the other side did (for editor highlighting).
 */
object CoverageBranchLineAnalyzer {

    /** VS Code 0-based range; [file] matches compiler [Position.file] keys. */
    data class UncoveredBranchSpan(
        val file: String,
        val startLineNumber: Int,
        val startCharacter: Int,
        val endLineNumber: Int,
        val endCharacter: Int
    )

    data class BranchGapAnalysisResult(
        val linesBranchPartial: Map<String, Set<Int>>,
        val uncoveredBranchSpans: List<UncoveredBranchSpan>
    )

    fun branchGapAnalysis(modules: Collection<TransformModule>, hook: CodeCoverageHook): BranchGapAnalysisResult {
        val hits = hook.statementHitsSnapshot().filter { it.hits > 0L }
        val byFile = mutableMapOf<String, MutableSet<Int>>()
        val uncovered = mutableListOf<UncoveredBranchSpan>()
        for (m in modules.distinct()) {
            val seen = IdentityHashMap<IIslCommand, Unit>()
            for (fn in m.functions) {
                CoverageStatementIdAssigner.walkCommandGraph(fn as IIslCommand, seen) { cmd ->
                    if (cmd !is ConditionCommand) return@walkCommandGraph
                    if (cmd.falseBranch == null) return@walkCommandGraph
                    val pos = cmd.token.position
                    val condSpan = SourceCoverageSpan.fromPosition(pos)

                    val trueHit = subtreeTouchesAnyHit(cmd.trueBranch, hits)
                    val falseHit = subtreeTouchesAnyHit(cmd.falseBranch, hits)
                    val condHit =
                        hits.any { sameFile(it.source.file, condSpan.file) && spansOverlap(it.source, condSpan) }

                    if (!condHit && !trueHit && !falseHit) return@walkCommandGraph
                    if (trueHit && falseHit) return@walkCommandGraph
                    addSpan(pos.line, pos.endLine ?: pos.line, pos.file, byFile)
                    (cmd.token as? ConditionToken)?.endifSourceLine?.let { endifLine ->
                        // Same-line `if … else … endif`: keep line in partial (gutter); only drop a dedicated `endif` line.
                        if (endifLine != pos.line) {
                            byFile[pos.file]?.remove(endifLine)
                        }
                    }

                    val missedRoot: IIslCommand? =
                        when {
                            trueHit && !falseHit -> cmd.falseBranch
                            !trueHit && falseHit -> cmd.trueBranch
                            else -> null
                        }
                    if (missedRoot != null) {
                        for (span in collectBaseCommandSpans(missedRoot)) {
                            val vr = span.toVsCodeRange()
                            uncovered.add(
                                UncoveredBranchSpan(
                                    span.file,
                                    vr.startLineNumber,
                                    vr.startCharacter,
                                    vr.endLineNumber,
                                    vr.endCharacter
                                )
                            )
                        }
                    }
                }
            }
        }
        return BranchGapAnalysisResult(byFile.mapValues { it.value.toSet() }, uncovered)
    }

    fun linesWithIncompleteIfElse(modules: Collection<TransformModule>, hook: CodeCoverageHook): Map<String, Set<Int>> =
        branchGapAnalysis(modules, hook).linesBranchPartial

    private fun collectBaseCommandSpans(root: IIslCommand?): List<SourceCoverageSpan> {
        if (root == null) return emptyList()
        val seen = IdentityHashMap<IIslCommand, Unit>()
        val out = mutableListOf<SourceCoverageSpan>()
        CoverageStatementIdAssigner.walkCommandGraph(root, seen) { c ->
            if (c is BaseCommand) {
                out.add(SourceCoverageSpan.fromPosition(c.token.position))
            }
        }
        return out
    }

    /**
     * Assignment and inline-if roots record a span over the whole expression. Those hits overlap branch literals
     * (`true` / `false`) and would incorrectly mark the unevaluated branch as "hit". Ignore those kinds here.
     * For [ConditionCommand], ignore a hit only when its span **strictly contains** the candidate span so nested
     * `if` inside a branch still matches its own ConditionCommand hit (same bounds, not a strict superset).
     */
    private fun hitProvesSubtreeExecuted(rec: StatementCoverageRecord, candidate: SourceCoverageSpan): Boolean {
        if (!sameFile(rec.source.file, candidate.file) || !spansOverlap(rec.source, candidate)) return false
        return when (rec.commandKind) {
            "AssignPropertyCommand", "AssignVariableCommand", "AssignDynamicPropertyCommand" -> false
            "ConditionCommand" -> !spanStrictlyContains(rec.source, candidate)
            else -> true
        }
    }

    /** True if [outer]'s span fully covers [inner] and is strictly larger (proper superset). */
    private fun spanStrictlyContains(outer: SourceCoverageSpan, inner: SourceCoverageSpan): Boolean {
        if (!sameFile(outer.file, inner.file)) return false
        val oLo = outer.startLine
        val oHi = outer.endLine ?: outer.startLine
        val iLo = inner.startLine
        val iHi = inner.endLine ?: inner.startLine
        if (iLo < oLo || iHi > oHi) return false
        if (iLo > oLo || iHi < oHi) return true
        for (line in iLo..iHi) {
            val o = halfOpenColumnsOnLine(outer, line) ?: return false
            val i = halfOpenColumnsOnLine(inner, line) ?: return false
            if (i.first < o.first || i.second > o.second) return false
        }
        for (line in iLo..iHi) {
            val o = halfOpenColumnsOnLine(outer, line) ?: return false
            val i = halfOpenColumnsOnLine(inner, line) ?: return false
            if (i.first > o.first || i.second < o.second) return true
        }
        return false
    }

    private fun subtreeTouchesAnyHit(root: IIslCommand?, hits: List<StatementCoverageRecord>): Boolean {
        if (root == null) return false
        val seen = IdentityHashMap<IIslCommand, Unit>()
        var found = false
        CoverageStatementIdAssigner.walkCommandGraph(root, seen) { c ->
            if (found) return@walkCommandGraph
            if (c is BaseCommand) {
                val span = SourceCoverageSpan.fromPosition(c.token.position)
                if (hits.any { hitProvesSubtreeExecuted(it, span) }) {
                    found = true
                }
            }
        }
        return found
    }

    private fun normalizeFile(file: String): String = file.replace('\\', '/')

    private fun sameFile(a: String, b: String): Boolean =
        normalizeFile(a).equals(normalizeFile(b), ignoreCase = true)

    /**
     * True if two spans intersect (1-based lines, 0-based columns; end column/line optional, default single-char).
     */
    private fun spansOverlap(a: SourceCoverageSpan, b: SourceCoverageSpan): Boolean {
        if (!sameFile(a.file, b.file)) return false
        val aLo = a.startLine
        val aHi = a.endLine ?: a.startLine
        val bLo = b.startLine
        val bHi = b.endLine ?: b.startLine
        if (aLo > bHi || bLo > aHi) return false
        val lineMin = max(aLo, bLo)
        val lineMax = min(aHi, bHi)
        if (lineMin < lineMax) return true
        val ac = halfOpenColumnsOnLine(a, lineMin) ?: return false
        val bc = halfOpenColumnsOnLine(b, lineMin) ?: return false
        return ac.first < bc.second && bc.first < ac.second
    }

    private fun halfOpenColumnsOnLine(s: SourceCoverageSpan, line: Int): Pair<Int, Int>? {
        val sl = s.startLine
        val el = s.endLine ?: s.startLine
        if (line < sl || line > el) return null
        val sc = if (line == sl) s.startColumn else 0
        val ec = if (line == el) (s.endColumn ?: (s.startColumn + 1)) else 1_000_000_000
        val end = max(ec, sc + 1)
        return sc to end
    }

    private fun addSpan(startLine: Int, endLine: Int, file: String, acc: MutableMap<String, MutableSet<Int>>) {
        val lo = minOf(startLine, endLine)
        val hi = maxOf(startLine, endLine)
        val set = acc.computeIfAbsent(file) { mutableSetOf() }
        for (ln in lo..hi) {
            set.add(ln)
        }
    }
}
