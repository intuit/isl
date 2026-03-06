package com.intuit.isl.cmd

import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext

/**
 * Exception thrown by @.Test.Exit(...) to signal an early test exit with a result.
 * The YAML test runner catches this and uses [result] as the test result for comparison with expected.
 */
class TestExitException(val result: Any?) : RuntimeException("Test.Exit with result")

/**
 * Test extension functions for ISL scripts when running tests from the command line.
 *
 * Usage in ISL:
 *   @.Test.Exit()           // exit with null result
 *   @.Test.Exit($value)     // exit with $value as the test result
 *
 * When the runner catches this, the result is compared with the test's expected value.
 */
object TestExtensions {
    fun registerExtensions(context: IOperationContext) {
        context.registerExtensionMethod("Test.Exit", TestExtensions::exit)
    }

    private fun exit(context: FunctionExecuteContext): Nothing {
        val result = context.firstParameter
        throw TestExitException(result)
    }
}
