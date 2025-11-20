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
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val sourceValue = value.executeAsync(executionContext);

        val oldValue = executionContext.operationContext.getVariable("\$mval");
        try {
            val value = JsonConvert.convert(sourceValue.value);
            executionContext.operationContext.setVariable("\$mval", value);
            executionContext.operationContext.setVariable("\$", value);
            if (expression.evaluateConditionAsync(executionContext)) {
                return trueModifier.executeAsync(executionContext);
            } else {
                return sourceValue;
            }
        } finally {
            executionContext.operationContext.removeVariable("\$");
            if (oldValue == null)
                executionContext.operationContext.removeVariable("\$mval");
            else
                executionContext.operationContext.setVariable("\$mval", oldValue);
        }
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}