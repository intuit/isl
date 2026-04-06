package com.intuit.isl.cmd

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intuit.isl.commands.CoverageBranchLineAnalyzer
import com.intuit.isl.commands.CoverageStatementIdAssigner
import com.intuit.isl.debug.CodeCoverageHook
import com.intuit.isl.debug.StatementCoverageRecord
import com.intuit.isl.runtime.TransformModule
import com.intuit.isl.runtime.Transformer
import java.io.File

/** JSON for editor coverage (transform `--coverage-report`, test `--coverage-report`). Single schema [COVERAGE_REPORT_VERSION]. */
object CoverageReportJson {

    /** Only coverage report format version; bump when breaking the JSON shape. */
    const val COVERAGE_REPORT_VERSION: Int = 1

    /**
     * @param moduleName Primary module label; empty string for aggregated test-run coverage.
     * @param modules When non-empty (e.g. full package from transform), includes every coverable statement (hits may be 0),
     *   [linesBranchPartial], and [statementCatalogComplete] true. When empty (e.g. tests), statements list only records hits > 0.
     */
    fun write(
        out: File,
        moduleName: String,
        hook: CodeCoverageHook,
        modules: List<TransformModule> = emptyList(),
        /** When [modules] is empty (e.g. `isl test`), still analyze if/else branch gaps using these compiled modules. */
        modulesForBranchAnalysis: List<TransformModule> = emptyList()
    ) {
        val mapper = jacksonObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        val catalogComplete = modules.isNotEmpty()
        val distinctModules = dedupeModulesByName(modules.distinct())
        val branchModules: List<TransformModule> =
            if (catalogComplete) distinctModules else dedupeModulesByName(modulesForBranchAnalysis)
        val records: List<StatementCoverageRecord> = if (catalogComplete) {
            distinctModules.flatMap { m ->
                CoverageStatementIdAssigner.listCoverableStatements(m).map { meta ->
                    StatementCoverageRecord(
                        meta.statementId,
                        meta.commandKind,
                        meta.source,
                        hook.hitCount(meta.source.file, meta.statementId)
                    )
                }
            }.sortedWith(
                compareBy({ it.source.file }, { it.source.startLine }, { it.source.startColumn }, { it.statementId })
            )
        } else {
            hook.statementHitsSnapshot()
        }
        val statements = records.map { r ->
            val vr = r.source.toVsCodeRange()
            linkedMapOf(
                "statementId" to r.statementId,
                "commandKind" to r.commandKind,
                "hits" to r.hits,
                "source" to linkedMapOf(
                    "file" to r.source.file,
                    "startLine" to r.source.startLine,
                    "startColumn" to r.source.startColumn,
                    "endLine" to r.source.endLine,
                    "endColumn" to r.source.endColumn
                ),
                "range" to linkedMapOf(
                    "startLineNumber" to vr.startLineNumber,
                    "startCharacter" to vr.startCharacter,
                    "endLineNumber" to vr.endLineNumber,
                    "endCharacter" to vr.endCharacter
                )
            )
        }
        val branchGap =
            if (branchModules.isNotEmpty()) {
                CoverageBranchLineAnalyzer.branchGapAnalysis(branchModules, hook)
            } else {
                null
            }
        val linesBranchPartial: Map<String, List<String>> =
            branchGap?.linesBranchPartial?.mapValues { (_, set) ->
                set.sorted().map { it.toString() }
            } ?: emptyMap()
        val branchUncoveredRanges: List<Map<String, Any?>> =
            branchGap?.uncoveredBranchSpans?.map { s ->
                linkedMapOf(
                    "file" to s.file,
                    "range" to
                        linkedMapOf(
                            "startLineNumber" to s.startLineNumber,
                            "startCharacter" to s.startCharacter,
                            "endLineNumber" to s.endLineNumber,
                            "endCharacter" to s.endCharacter
                        )
                )
            } ?: emptyList()
        val lines: Map<String, Map<String, Long>> =
            if (catalogComplete) {
                emptyMap()
            } else {
                hook.lineHitsSnapshot().mapValues { (_, m) ->
                    m.mapKeys { it.key.toString() }
                }
            }
        val generator =
            linkedMapOf<String, Any?>(
                "islVersion" to Transformer.version,
                "moduleNames" to distinctModules.map { it.name }.sorted().ifEmpty { branchModules.map { it.name }.sorted() },
                "statementCount" to statements.size,
                "catalogComplete" to catalogComplete
            )
        val root = linkedMapOf<String, Any?>(
            "version" to COVERAGE_REPORT_VERSION,
            "moduleName" to moduleName,
            "statementCatalogComplete" to catalogComplete,
            "generator" to generator,
            "lines" to lines,
            "linesBranchPartial" to linesBranchPartial,
            "branchUncoveredRanges" to branchUncoveredRanges,
            "statements" to statements
        )
        out.writeText(mapper.writeValueAsString(root))
    }

    private fun dedupeModulesByName(modules: List<TransformModule>): List<TransformModule> {
        val acc = LinkedHashMap<String, TransformModule>()
        for (m in modules) {
            acc[m.name.lowercase()] = m
        }
        return acc.values.toList()
    }
}
