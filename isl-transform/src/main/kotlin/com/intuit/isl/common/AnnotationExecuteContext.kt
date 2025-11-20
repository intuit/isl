package com.intuit.isl.common

import com.intuit.isl.commands.AnnotationCommand
import com.intuit.isl.commands.IIslCommand
import kotlinx.coroutines.runBlocking

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

    suspend fun runNextCommand(): Any? {
        return nextCommand.executeAsync(executionContext).value;
    }

    /**
     * Run next command inline from Java :)
     */
    fun runNextCommandSync(): Any? {
        return runBlocking {
            nextCommand.executeAsync(executionContext).value;
        }
    }
}