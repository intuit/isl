package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.commands.BaseCommand
import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.removeVariableCanonical
import com.intuit.isl.common.setVariableCanonical
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.MapModifierValueToken
import com.intuit.isl.utils.JsonConvert

class MapModifierValueCommand(
    token: MapModifierValueToken,
    private val previousValue: IIslCommand,
    private val argument: IIslCommand
) : BaseCommand(token) {

    internal val mapPreviousValue: IIslCommand get() = previousValue
    internal val mapArgument: IIslCommand get() = argument
    override fun execute(executionContext: ExecutionContext): CommandResult {
        val hook = executionContext.executionHook
        hook?.onBeforeExecute(this, executionContext)
        val sourceCollection = previousValue.execute(executionContext).value

        val source = when (sourceCollection) {
            is Iterable<Any?> -> sourceCollection
            else -> null
        }

        val defaultSize = if (sourceCollection is Collection<Any?>) sourceCollection.size else 10
        val array = JsonNodeFactory.instance.arrayNode(defaultSize)

        source?.forEach { it ->
            executionContext.operationContext.setVariableCanonical("\$", JsonConvert.convert(it))
            array.add(JsonConvert.convert(argument.execute(executionContext).value))
        }
        executionContext.operationContext.removeVariableCanonical("\$")

        val result = CommandResult(array)
        hook?.onAfterExecute(this, executionContext, result)
        return result
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}