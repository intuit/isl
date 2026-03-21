package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.commands.BaseCommand
import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.MapModifierValueToken
import com.intuit.isl.utils.JsonConvert

class MapModifierValueCommand(
    token: MapModifierValueToken,
    private val previousValue: IIslCommand,
    private val argument: IIslCommand
) : BaseCommand(token) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val hook = executionContext.debugHook
        hook?.onBeforeExecute(this, executionContext)
        val sourceCollection = previousValue.executeAsync(executionContext).value

        val source = when (sourceCollection) {
            is Iterable<Any?> -> sourceCollection
            else -> null
        }

        val defaultSize = if (sourceCollection is Collection<Any?>) sourceCollection.size else 10
        val array = JsonNodeFactory.instance.arrayNode(defaultSize)

        source?.forEach { it ->
            executionContext.operationContext.setVariable("\$", JsonConvert.convert(it))
            array.add(JsonConvert.convert(argument.executeAsync(executionContext).value))
        }
        executionContext.operationContext.removeVariable("\$")

        val result = CommandResult(array)
        hook?.onAfterExecute(this, executionContext, result)
        return result
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}