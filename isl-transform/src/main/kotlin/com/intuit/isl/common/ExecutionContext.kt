package com.intuit.isl.common

import com.intuit.isl.debug.IExecutionHook

data class ExecutionContext(
    val operationContext: IOperationContext,
    val localContext: Any?,
    val executionHook: IExecutionHook? = null
)