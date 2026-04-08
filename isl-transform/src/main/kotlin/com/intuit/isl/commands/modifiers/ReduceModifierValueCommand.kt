package com.intuit.isl.commands.modifiers

import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.getVariableCanonical
import com.intuit.isl.common.resetVariableCanonical
import com.intuit.isl.common.setVariableCanonical
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
    override fun execute(executionContext: ExecutionContext): CommandResult {
        val hook = executionContext.executionHook
        hook?.onBeforeExecute(this, executionContext)
        val sourceCollection = value.execute(executionContext).value

        val source = when (sourceCollection) {
            is Iterable<Any?> -> sourceCollection
            else -> null
        }

        val oldAcc = executionContext.operationContext.getVariableCanonical("\$acc")
        val oldIt = executionContext.operationContext.getVariableCanonical("\$it")
        var acc = "" as Any?
        source?.forEach { it ->
            executionContext.operationContext.setVariableCanonical("\$it", JsonConvert.convert(it))
            executionContext.operationContext.setVariableCanonical("\$acc", JsonConvert.convert(acc))
            acc = argument.execute(executionContext).value
        }
        executionContext.operationContext.resetVariableCanonical("\$it", oldIt)
        executionContext.operationContext.resetVariableCanonical("\$acc", oldAcc)

        val result = CommandResult(acc)
        hook?.onAfterExecute(this, executionContext, result)
        return result
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}