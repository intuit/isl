package com.intuit.isl.commands

import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.IIslToken


interface IEvaluableConditionCommand {
    suspend fun evaluateConditionAsync(context: ExecutionContext): Boolean;
}

class ConditionExpressionCommand(
    token: IIslToken,
    val left: IEvaluableConditionCommand,
    val condition: String,
    val right: IEvaluableConditionCommand
) : BaseCommand(token), IEvaluableConditionCommand {

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val result = evaluateConditionAsync(executionContext);
        return CommandResult(result);
    }

    override suspend fun evaluateConditionAsync(context: ExecutionContext): Boolean {
        val leftValue = left.evaluateConditionAsync(context);

        if (condition == "or" && leftValue)
            return true;    // short evaluation

        val rightValue = right.evaluateConditionAsync(context);
        if (condition == "or")
            return rightValue;

        return leftValue && rightValue;
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}

/**
 * Just the conditional expression (left <> right). Always returns true or false
 */
class SimpleConditionCommand(token: IIslToken, val left: IIslCommand, val condition: String, val right: IIslCommand?) :
    BaseCommand(token), IEvaluableConditionCommand {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val result = evaluateConditionAsync(executionContext);
        return CommandResult(result);
    }

    override suspend fun evaluateConditionAsync(context: ExecutionContext): Boolean {
        val leftResult = left.executeAsync(context);
        val leftValue = leftResult.value;

        val rightValue = right?.executeAsync(context)?.value;

        return ConditionEvaluator.evaluate(leftValue, condition, rightValue);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}

/**
 * Coalesce command ??
 * We could have solved this with a condition but that tends to evaluate the left part twice
 * Once in the condition and once in the result.
 */
class CoalesceCommand(token: IIslToken, val left: IIslCommand, val right: IIslCommand) : BaseCommand(token) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val leftResult = left.executeAsync(executionContext);
        val leftValue = leftResult.value;

        if (ConditionEvaluator.evaluate(leftValue, ConditionEvaluator.EXISTS, null)) {
            return leftResult;
        } else {
            val rightValue = right.executeAsync(executionContext);
            return rightValue;
        }
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}