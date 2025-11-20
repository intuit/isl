package com.intuit.isl.common

data class ExecutionContext(
    val operationContext: IOperationContext,
    val localContext: Any?
)