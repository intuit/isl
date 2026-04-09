package com.intuit.isl.commands.modifiers

import com.intuit.isl.commands.BaseCommand
import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.IEvaluableConditionCommand
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.getVariableCanonical
import com.intuit.isl.common.removeVariableCanonical
import com.intuit.isl.common.setVariableCanonical
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

        val oldValue = executionContext.operationContext.getVariableCanonical("\$mval")
        val result = try {
            val converted = JsonConvert.convert(sourceValue.value)
            executionContext.operationContext.setVariableCanonical("\$mval", converted)
            executionContext.operationContext.setVariableCanonical("\$", converted)
            if (expression.evaluateCondition(executionContext)) {
                trueModifier.execute(executionContext)
            } else {
                sourceValue
            }
        } finally {
            executionContext.operationContext.removeVariableCanonical("\$")
            if (oldValue == null) {
                executionContext.operationContext.removeVariableCanonical("\$mval")
            } else {
                executionContext.operationContext.setVariableCanonical("\$mval", oldValue)
            }
        }
        hook?.onAfterExecute(this, executionContext, result)
        return result
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}