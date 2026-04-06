package com.intuit.isl.commands.modifiers

import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.resetVariable
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.commands.BaseCommand
import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.parser.tokens.ModifierValueToken
import com.intuit.isl.utils.JsonConvert

class ReduceModifierValueCommand(
    token: ModifierValueToken,
    private val value: IIslCommand,
    private val argument: IIslCommand
) : BaseCommand(token) {

    internal val reduceSource: IIslCommand get() = value
    internal val reduceArgument: IIslCommand get() = argument
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val hook = executionContext.executionHook
        hook?.onBeforeExecute(this, executionContext)
        val sourceCollection = value.executeAsync(executionContext).value

        val source = when (sourceCollection) {
            is Iterable<Any?> -> sourceCollection
            else -> null
        }

        val oldAcc = executionContext.operationContext.getVariable("\$acc")
        val oldIt = executionContext.operationContext.getVariable("\$it")
        var acc = "" as Any?
        source?.forEach { it ->
            executionContext.operationContext.setVariable("\$it", JsonConvert.convert(it))
            executionContext.operationContext.setVariable("\$acc", JsonConvert.convert(acc))
            acc = argument.executeAsync(executionContext).value
        }
        executionContext.operationContext.resetVariable("\$it", oldIt)
        executionContext.operationContext.resetVariable("\$acc", oldAcc)

        val result = CommandResult(acc)
        hook?.onAfterExecute(this, executionContext, result)
        return result
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}