package com.intuit.isl.commands

import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.IIslToken

// TODO: Maybe split this into two classes, one for left conditions only and one for right conditions
class ConditionCommand(
    token: IIslToken,
    private val expression: IEvaluableConditionCommand,
    private val trueResult: IIslCommand,
    private val falseResult: IIslCommand?
) : BaseCommand(token) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        if (expression.evaluateConditionAsync(executionContext)) {
            val result = trueResult.executeAsync(executionContext)
            return CommandResult(result.value, null, true);
        } else {
            if (falseResult == null) {
                // there is no else branch! don't attempt to append the property at all!
                return CommandResult(null, null, false);
            } else {
                val result = falseResult.executeAsync(executionContext)
                return CommandResult(result.value, null, true);
            }
        }
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }


}