package com.intuit.isl.debug

import com.intuit.isl.commands.BaseCommand
import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.ConditionCommand
import com.intuit.isl.commands.ForEachCommand
import com.intuit.isl.commands.HashDispatchSwitchCommand
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.commands.SwitchCaseCommand
import com.intuit.isl.commands.WhileCommand
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.utils.Position
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.LongAdder

/**
 * [IExecutionHook] that records execution hits per compiled statement id and rolled up per source line.
 *
 * - **Statement view**: each instrumented [onBeforeExecute] maps to [BaseCommand.coverageStatementId]
 *   (assigned by [com.intuit.isl.commands.CoverageStatementIdAssigner] once per module when this hook runs).
 *   Hits are keyed by **source file + id** so ids from different modules do not collide.
 * - **Line view**: same hits aggregated by [Position.file] and each source line in the token span
 *   ([Position.line] through [Position.endLine] when set), so multi-line objects map literals show green on every line.
 *
 * For VS Code, use [statementHitsSnapshot] plus [SourceCoverageSpan.toVsCodeRange] to decorate sub-line spans
 * (e.g. individual modifiers on one line).
 */
class CodeCoverageHook : IExecutionHook {

    override val preparesStatementIds: Boolean get() = true

    private val hitsByStatementKey = ConcurrentHashMap<String, LongAdder>()
    private val hitsByFile = ConcurrentHashMap<String, ConcurrentHashMap<Int, LongAdder>>()
    private val metaByStatementKey = ConcurrentHashMap<String, StatementMeta>()

    private data class StatementMeta(val commandKind: String, val span: SourceCoverageSpan, val statementId: Int)

    private val runtimeStatementId = AtomicInteger(-1)

    companion object {
        internal fun normalizeCoverageFile(file: String): String = file.replace('\\', '/')

        internal fun statementKey(file: String, statementId: Int): String =
            normalizeCoverageFile(file) + '\u0000' + statementId
    }

    override fun onBeforeExecute(command: IIslCommand, context: ExecutionContext) {
        val pos = command.token.position
        recordCoverageLines(command, pos)
        val bc = command as? BaseCommand
        val sid = when {
            bc == null -> 0
            bc.coverageStatementId != 0 -> bc.coverageStatementId
            else -> runtimeStatementId.getAndDecrement()
        }
        if (sid == 0) return
        val key = statementKey(pos.file, sid)
        metaByStatementKey.putIfAbsent(
            key,
            StatementMeta(bc?.let { it::class.simpleName } ?: command::class.simpleName ?: "?", SourceCoverageSpan.fromPosition(pos), sid)
        )
        hitsByStatementKey.computeIfAbsent(key) { LongAdder() }.increment()
    }

    override fun onAfterExecute(command: IIslCommand, context: ExecutionContext, result: CommandResult) {}

    override fun onFunctionEnter(command: IIslCommand, context: ExecutionContext) {}

    override fun onFunctionExit(command: IIslCommand, context: ExecutionContext) {}

    /**
     * Legacy line hits: for most commands, every source line in the token span (e.g. `$map = { ... };`).
     * Control-flow roots whose token spans unevaluated branches or loop bodies only record the start line.
     */
    private fun recordCoverageLines(command: IIslCommand, position: Position) {
        when (command) {
            is ConditionCommand,
            is SwitchCaseCommand,
            is HashDispatchSwitchCommand,
            is WhileCommand,
            is ForEachCommand -> recordLineAt(position.file, position.line)
            else -> recordLinesForPosition(position)
        }
    }

    /** One legacy line hit per covered line in the token span. */
    private fun recordLinesForPosition(position: Position) {
        val file = position.file
        val start = position.line
        val end = position.endLine ?: start
        val lo = minOf(start, end)
        val hi = maxOf(start, end)
        for (ln in lo..hi) {
            recordLineAt(file, ln)
        }
    }

    private fun recordLineAt(file: String, line1Based: Int) {
        hitsByFile
            .computeIfAbsent(file) { ConcurrentHashMap() }
            .computeIfAbsent(line1Based) { LongAdder() }
            .increment()
    }

    /**
     * Hit count for a stable statement in [file] (same string as [Position.file] on commands).
     */
    fun hitCount(file: String, statementId: Int): Long =
        hitsByStatementKey[statementKey(file, statementId)]?.sum() ?: 0L

    /**
     * Immutable snapshot: file → (line → hit count). Multiple statements on the same line are summed.
     */
    fun lineHitsSnapshot(): Map<String, Map<Int, Long>> =
        hitsByFile.mapValues { (_, lines) ->
            lines.mapValues { (_, adder) -> adder.sum() }.toSortedMap()
        }

    /**
     * Per-statement hits with source span (for sub-line / modifier highlighting). Sorted by [StatementCoverageRecord.statementId].
     */
    fun statementHitsSnapshot(): List<StatementCoverageRecord> =
        hitsByStatementKey.mapNotNull { (key, adder) ->
            val hits = adder.sum()
            if (hits == 0L) return@mapNotNull null
            val meta = metaByStatementKey[key] ?: return@mapNotNull null
            StatementCoverageRecord(meta.statementId, meta.commandKind, meta.span, hits)
        }.sortedWith(compareBy({ it.source.file }, { it.statementId }))

    /**
     * Statements with hits in [file], ordered by start position (line, column, id).
     */
    fun statementHitsForFile(file: String): List<StatementCoverageRecord> =
        statementHitsSnapshot()
            .filter { normalizeCoverageFile(it.source.file) == normalizeCoverageFile(file) }
            .sortedWith(compareBy({ it.source.startLine }, { it.source.startColumn }, { it.statementId }))

    fun clear() {
        hitsByStatementKey.clear()
        hitsByFile.clear()
        metaByStatementKey.clear()
    }
}
