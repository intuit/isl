package com.intuit.isl.test

import com.intuit.isl.utils.Position

/**
 * Exception thrown by @.Test.Exit(...) to signal an early test exit with a result.
 * The YAML test runner catches this and uses [result] as the test result for comparison with expected.
 */
open class TestExitException(val result: Any?) : RuntimeException(
    "Test.Exit" + (result?.let { ": $it" } ?: "")
)

/**
 * Exception thrown by @.Test.Fail(...) to signal an immediate test failure with a message.
 * Unlike [TestExitException], the runner does NOT compare [result] against expected — it reports
 * the failure message directly and marks the test as failed.
 *
 * [position] is the source position of the call site (e.g. the mock call that triggered the failure)
 * and is attached to the [TestResult] so the plugin can highlight the correct location.
 */
class TestFailException(result: Any?, val position: Position? = null) : TestExitException(result) {
    override val message: String
        get() = "Test.Fail" + (result?.let { ": $it" } ?: "")
}
