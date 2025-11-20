package com.intuit.isl.common

import com.intuit.isl.commands.IIslCommand

data class FunctionExecuteContext(
    val functionName: String,
    val command: IIslCommand,
    val executionContext: ExecutionContext,
    override val parameters: Array<*>
) : IExecuteContext {
    val firstParameter: Any?
        get() = parameters.elementAtOrNull(0)

    val secondParameter: Any?
        get() = parameters.elementAtOrNull(1)

    val thirdParameter: Any?
        get() = parameters.elementAtOrNull(2)

    val fourthParameter: Any?
        get() = parameters.elementAtOrNull(3)

    val fifthParameter: Any?
        get() = parameters.elementAtOrNull(4)

    val sixthParameter: Any?
        get() = parameters.elementAtOrNull(5)
}
