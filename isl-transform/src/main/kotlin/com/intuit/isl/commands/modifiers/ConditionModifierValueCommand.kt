package com.intuit.isl.commands.modifiers

import com.intuit.isl.commands.BaseCommand
import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.IEvaluableConditionCommand
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.ConditionModifierValueToken
import com.intuit.isl.utils.JsonConvert

class ConditionModifierValueCommand(
    token: ConditionModifierValueToken,
    val value: IIslCommand,
    val expression: IEvaluableConditionCommand,
    val trueModifier: IIslCommand
) : BaseCommand(token) {
    override fun execute(executionContext: ExecutionContext): CommandResult {
        val hook = executionContext.executionHook
        hook?.onBeforeExecute(this, executionContext)
        val sourceValue = value.execute(executionContext)

        val oldValue = executionContext.operationContext.getVariable("\$mval")
        val result = try {
            val converted = JsonConvert.convert(sourceValue.value)
            executionContext.operationContext.setVariable("\$mval", converted)
            executionContext.operationContext.setVariable("\$", converted)
            if (expression.evaluateCondition(executionContext)) {
                trueModifier.execute(executionContext)
            } else {
                sourceValue
            }
        } finally {
            executionContext.operationContext.removeVariable("\$")
            if (oldValue == null) {
                executionContext.operationContext.removeVariable("\$mval")
            } else {
                executionContext.operationContext.setVariable("\$mval", oldValue)
            }
        }
        hook?.onAfterExecute(this, executionContext, result)
        return result
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}