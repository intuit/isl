package com.intuit.isl.common

import com.intuit.isl.commands.AnnotationCommand
import com.intuit.isl.commands.IIslCommand

data class AnnotationExecuteContext(
    val command: AnnotationCommand,
    val executionContext: ExecutionContext,
    override val parameters: Array<*>
) : IExecuteContext {
    val annotationName: String
        get() = command.token.annotationName;

    val nextCommand: IIslCommand
        get() = command.nextCommand;

    val firstParameter: Any?
        get() = parameters.elementAtOrNull(0);

    val functionParameters: Array<*>
        get() = executionContext.operationContext.variables.map { it.value.value }
            .toTypedArray();

    val functionName: String
        get() = command.function.functionName;

    /**
     * Run next command - now sync since all commands are sync
     */
    fun runNextCommand(): Any? {
        return nextCommand.execute(executionContext).value;
    }

    /**
     * Run next command inline from Java - same as runNextCommand now
     * @deprecated Use runNextCommand() instead - both are sync now
     */
    @Deprecated("Use runNextCommand() instead - both are sync now", ReplaceWith("runNextCommand()"))
    fun runNextCommandSync(): Any? {
        return nextCommand.execute(executionContext).value;
    }
}