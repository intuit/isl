package com.intuit.isl.common

import com.intuit.isl.debug.IExecutionHook
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

data class ExecutionContext(
    val operationContext: IOperationContext,
    val localContext: Any?,
    val executionHook: IExecutionHook? = null,
    val coroutineContext: CoroutineContext = EmptyCoroutineContext
)