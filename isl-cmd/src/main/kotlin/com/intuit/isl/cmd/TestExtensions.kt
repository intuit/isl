package com.intuit.isl.cmd

import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.test.TestExitException
import com.intuit.isl.test.TestFailException

/**
 * Test extension functions for ISL scripts when running tests from the command line.
 *
 * Usage in ISL:
 *   @.Test.Exit()                        // exit with null result (compared against expected)
 *   @.Test.Exit($value)                  // exit with $value as the test result
 *   @.Test.Fail("reason")               // immediately fail the test with a message
 *   @.Test.Fail({ message: "reason" })  // immediately fail the test (message field extracted)
 *
 * Test.Exit result is compared with the test's expected value.
 * Test.Fail skips comparison and marks the test as failed with the given message.
 */
object TestExtensions {
    fun registerExtensions(context: IOperationContext) {
        context.registerSyncExtensionMethod("Test.Exit", TestExtensions::exit)
        context.registerSyncExtensionMethod("Test.Fail", TestExtensions::fail)
    }

    private fun exit(context: FunctionExecuteContext): Nothing {
        throw TestExitException(context.firstParameter)
    }

    private fun fail(context: FunctionExecuteContext): Nothing {
        throw TestFailException(context.firstParameter)
    }
}
