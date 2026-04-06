package com.intuit.isl.debug

import com.intuit.isl.utils.Position

/**
 * Source span for a command (ISL / parser coordinates: **1-based** lines, **0-based** columns from ANTLR).
 * Use [toVsCodeRange] when building a VS Code `Range` for decorations.
 */
data class SourceCoverageSpan(
    val file: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int?,
    val endColumn: Int?
) {
    companion object {
        fun fromPosition(p: Position): SourceCoverageSpan =
            SourceCoverageSpan(p.file, p.line, p.column, p.endLine, p.endColumn)
    }

    /**
     * VS Code `Range`: 0-based line and character indices (inclusive start, exclusive end for empty use start).
     */
    fun toVsCodeRange(): VsCodeRange {
        val el = endLine ?: startLine
        val sc = startColumn
        val ec = endColumn ?: (sc + 1)
        return VsCodeRange(
            startLineNumber = startLine - 1,
            startCharacter = sc,
            endLineNumber = el - 1,
            endCharacter = ec
        )
    }
}

/**
 * Serializable-friendly range for editor extensions (e.g. TypeScript).
 */
data class VsCodeRange(
    val startLineNumber: Int,
    val startCharacter: Int,
    val endLineNumber: Int,
    val endCharacter: Int
)

/**
 * Per-command coverage: stable [statementId] matches [com.intuit.isl.commands.BaseCommand.coverageStatementId].
 */
data class StatementCoverageRecord(
    val statementId: Int,
    val commandKind: String,
    val source: SourceCoverageSpan,
    val hits: Long
)

/** Static coverable statement (after [com.intuit.isl.commands.CoverageStatementIdAssigner.assign]); merge with hook hits for full reports. */
data class CoverableStatementMeta(
    val statementId: Int,
    val commandKind: String,
    val source: SourceCoverageSpan
)
