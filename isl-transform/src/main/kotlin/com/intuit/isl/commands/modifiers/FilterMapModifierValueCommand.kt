package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.commands.BaseCommand
import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.IEvaluableConditionCommand
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.MapModifierValueToken
import com.intuit.isl.utils.JsonConvert

/**
 * Single-pass fusion of [FilterModifierValueCommand] followed by [MapModifierValueCommand],
 * built when the token graph is `... | filter(...) | map(...)`.
 */
class FilterMapModifierValueCommand(
    token: MapModifierValueToken,
    private val value: IIslCommand,
    private val filterExpression: IEvaluableConditionCommand,
    private val mapArgument: IIslCommand
) : BaseCommand(token) {

    internal val filterMapSource: IIslCommand get() = value
    internal val filterMapPredicate: IEvaluableConditionCommand get() = filterExpression
    internal val filterMapMapArgument: IIslCommand get() = mapArgument
    override fun execute(executionContext: ExecutionContext): CommandResult {
        val hook = executionContext.executionHook
        hook?.onBeforeExecute(this, executionContext)
        val sourceCollection = value.execute(executionContext).value

        val source = when (sourceCollection) {
            is Iterable<Any?> -> sourceCollection
            else -> null
        }

        val oldIt = executionContext.operationContext.getVariable("\$fit")
        val defaultSize = if (sourceCollection is Collection<Any?>) sourceCollection.size else 10
        val resultArray = JsonNodeFactory.instance.arrayNode(defaultSize)

        source?.forEach { it ->
            executionContext.operationContext.setVariable("\$fit", JsonConvert.convert(it))
            executionContext.operationContext.setVariable("\$", JsonConvert.convert(it))
            if (filterExpression.evaluateCondition(executionContext)) {
                resultArray.add(JsonConvert.convert(mapArgument.execute(executionContext).value))
            }
        }

        executionContext.operationContext.removeVariable("\$")
        if (oldIt == null) {
            executionContext.operationContext.removeVariable("\$fit")
        } else {
            executionContext.operationContext.setVariable("\$fit", oldIt)
        }

        val result = CommandResult(resultArray)
        hook?.onAfterExecute(this, executionContext, result)
        return result
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this)
    }
}
