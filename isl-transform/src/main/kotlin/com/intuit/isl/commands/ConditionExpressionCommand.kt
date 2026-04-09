package com.intuit.isl.commands

import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.IIslToken


interface IEvaluableConditionCommand {
    fun evaluateCondition(context: ExecutionContext): Boolean;
}

class ConditionExpressionCommand(
    token: IIslToken,
    val left: IEvaluableConditionCommand,
    val condition: String,
    val right: IEvaluableConditionCommand
) : BaseCommand(token), IEvaluableConditionCommand {

    override fun execute(executionContext: ExecutionContext): CommandResult {
        val result = evaluateCondition(executionContext);
        return CommandResult(result);
    }

    override fun evaluateCondition(context: ExecutionContext): Boolean {
        val leftValue = left.evaluateCondition(context);

        if (condition == "or" && leftValue)
            return true;    // short evaluation

        val rightValue = right.evaluateCondition(context);
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
    override fun execute(executionContext: ExecutionContext): CommandResult {
        val result = evaluateCondition(executionContext);
        return CommandResult(result);
    }

    override fun evaluateCondition(context: ExecutionContext): Boolean {
        val leftResult = left.execute(context);
        val leftValue = leftResult.value;

        val rightValue = right?.execute(context)?.value;

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
    override fun execute(executionContext: ExecutionContext): CommandResult {
        val leftResult = left.execute(executionContext);
        val leftValue = leftResult.value;

        if (ConditionEvaluator.evaluate(leftValue, ConditionEvaluator.EXISTS, null)) {
            return leftResult;
        } else {
            val rightValue = right.execute(executionContext);
            return rightValue;
        }
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}