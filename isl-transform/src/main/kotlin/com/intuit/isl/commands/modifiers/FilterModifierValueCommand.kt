package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.commands.BaseCommand
import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.IEvaluableConditionCommand
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.getVariableCanonical
import com.intuit.isl.common.removeVariableCanonical
import com.intuit.isl.common.setVariableCanonical
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.FilterModifierValueToken
import com.intuit.isl.utils.JsonConvert

class FilterModifierValueCommand(
    token: FilterModifierValueToken,
    private val value: IIslCommand,
    private val expression: IEvaluableConditionCommand
) : BaseCommand(token) {

    internal val filterSource: IIslCommand get() = value
    internal val filterExpression: IEvaluableConditionCommand get() = expression
    override fun execute(executionContext: ExecutionContext): CommandResult {
        val hook = executionContext.executionHook
        hook?.onBeforeExecute(this, executionContext)
        val sourceCollection = value.execute(executionContext).value

        val source = when (sourceCollection) {
            is Iterable<Any?> -> sourceCollection
            else -> null
        }

        val oldIt = executionContext.operationContext.getVariableCanonical("\$fit")

        val defaultSize = if (sourceCollection is Collection<Any?>) sourceCollection.size else 10
        val resultArray = JsonNodeFactory.instance.arrayNode(defaultSize)

        source?.forEach { it ->
            executionContext.operationContext.setVariableCanonical("\$fit", JsonConvert.convert(it))
            executionContext.operationContext.setVariableCanonical("\$", JsonConvert.convert(it))
            if (expression.evaluateCondition(executionContext)) {
                resultArray.add(JsonConvert.convert(it))
            }
        }

        executionContext.operationContext.removeVariableCanonical("\$")
        if (oldIt == null) {
            executionContext.operationContext.removeVariableCanonical("\$fit")
        } else {
            executionContext.operationContext.setVariableCanonical("\$fit", oldIt)
        }

        val result = CommandResult(resultArray)
        hook?.onAfterExecute(this, executionContext, result)
        return result
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}