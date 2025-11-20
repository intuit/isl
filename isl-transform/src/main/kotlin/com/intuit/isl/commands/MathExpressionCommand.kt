package com.intuit.isl.commands

import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.parser.tokens.IIslToken
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.getValueOrDefault
import java.math.BigDecimal
import java.math.RoundingMode

class MathExpressionCommand(token: IIslToken, val left: IIslCommand, val right: IIslCommand, val operator: String) :
    BaseCommand(token) {
    // typealias OperationType = (left: BigDecimal?, right: BigDecimal?) -> BigDecimal; - can't do this :(

    private val operation = buildOperation(this);

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val leftResult = left.executeAsync(executionContext).value;
        val rightResult = right.executeAsync(executionContext).value;

        val leftValue = ConvertUtils.tryParseDecimal(leftResult);
        val rightValue = ConvertUtils.tryParseDecimal(rightResult);

        val result = operation(leftValue, rightValue);

        return CommandResult( result);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }

    companion object {
        private fun buildOperation(command: MathExpressionCommand): (left: BigDecimal?, right: BigDecimal?) -> BigDecimal {
            // let's convert the operator into a Function now so we have to evaluate at runtime
            return when (command.operator) {
                "*" -> { left, right -> left.getValueOrDefault() * right.getValueOrDefault(); }
                "/" -> { left, right ->
                    if (right.getValueOrDefault() == BigDecimal.ZERO) {
                        BigDecimal.ZERO;   // no division by zero!
                    } else {
                        // ah - the magic of Java "a / b" gets rounded to integer - you need to explicitly call .divide
                        // https://stackoverflow.com/questions/10603651/what-causes-non-terminating-decimal-expansion-exception-from-bigdecimal-divide
                        left.getValueOrDefault().divide( right.getValueOrDefault(), 4, RoundingMode.HALF_DOWN );
                    }
                }
                "+" -> { left, right -> left.getValueOrDefault() + right.getValueOrDefault(); }
                "-" -> { left, right -> left.getValueOrDefault() - right.getValueOrDefault(); }
                else -> { _, _ -> BigDecimal.ZERO };
            }
        }
    }
}