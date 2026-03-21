package com.intuit.isl.common

import com.intuit.isl.debug.IDebugHook

data class ExecutionContext(
    val operationContext: IOperationContext,
    val localContext: Any?,
    val debugHook: IDebugHook? = null
)