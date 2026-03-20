package com.intuit.isl.test.annotations

import com.intuit.isl.utils.Position
import java.lang.Exception

/** One path-level difference (path, expected value, actual value) for report rendering. */
data class ComparisonDiff(val path: String, val expectedValue: String, val actualValue: String)

data class TestResult(
    val testFile: String,
    val functionName: String,
    val testName: String,
    val testGroup: String?,
    var success: Boolean,
    var message: String? = null,
    var errorPosition: Position? = null,
    var exception: Exception? = null,
    /** When set (e.g. YAML test comparison failure), report can render expected/actual in ```json blocks. */
    var expectedJson: String? = null,
    var actualJson: String? = null,
    /** Per-path differences for markdown report (Expected:/Actual: blocks). */
    var comparisonDiffs: List<ComparisonDiff>? = null
)