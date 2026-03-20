package com.intuit.isl.test.mocks

import com.intuit.isl.common.AsyncContextAwareExtensionMethod
import com.intuit.isl.common.FunctionExecuteContext

/**
 * Represents a mock that runs compiled ISL code instead of returning a static value.
 * When the mock is invoked, the runner is called with the same [FunctionExecuteContext]
 * (parameters from the call), and its return value is used as the mock result.
 */
class IslMockExecutor(
    private val runner: AsyncContextAwareExtensionMethod
) {
    suspend fun run(context: FunctionExecuteContext): Any? {
        return runner(context)
    }
}
