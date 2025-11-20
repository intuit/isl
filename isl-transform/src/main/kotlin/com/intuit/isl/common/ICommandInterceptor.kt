package com.intuit.isl.common

import com.intuit.isl.commands.IIslCommand

/**
 * Command Interceptor that can be attached on demand to the operation context to trace all executed commands.
 * Note that this is pro-actively called from within each command.
 */
interface ICommandInterceptor{
    fun logInfo(command: IIslCommand, context: ExecutionContext, details: () -> String);
    fun logError(command: IIslCommand, context: ExecutionContext, details: () -> String);

    fun onExecuting(command: IIslCommand, context: ExecutionContext, arguments: Array<*>);
    fun onIssue(command: IIslCommand, context: ExecutionContext, message: String, arguments: Array<*>);
}