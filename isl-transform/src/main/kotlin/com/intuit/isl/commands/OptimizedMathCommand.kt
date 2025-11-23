package com.intuit.isl.commands

import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.parser.tokens.IIslToken
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.getValueOrDefault
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pre-computed constant value from math expression optimization
 * This is used when all operands in a math expression are constants and can be computed at compile time
 */
class ConstantMathCommand(token: IIslToken, private val constantValue: BigDecimal) : BaseCommand(token) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        return CommandResult(constantValue)
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        // These optimized commands are treated as MathExpressionCommand for visitor purposes
        // Cast to the interface type that the visitor expects
        @Suppress("UNCHECKED_CAST")
        return visitor.visit(this as MathExpressionCommand) as T
    }
}

/**
 * Optimized math expression that chains multiple operations together
 * This reduces the number of executeAsync calls and intermediate CommandResult allocations
 */
class ChainedMathCommand(
    token: IIslToken,
    private val operations: List<MathOperation>
) : BaseCommand(token) {
    
    data class MathOperation(
        val command: IIslCommand?,
        val operator: String,
        val constantValue: BigDecimal?
    )

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        var accumulator: BigDecimal? = null
        
        for (operation in operations) {
            val value = if (operation.constantValue != null) {
                operation.constantValue
            } else {
                val result = operation.command!!.executeAsync(executionContext).value
                ConvertUtils.tryParseDecimal(result)
            }
            
            if (accumulator == null) {
                accumulator = value.getValueOrDefault()
            } else {
                accumulator = applyOperation(accumulator, value.getValueOrDefault(), operation.operator)
            }
        }
        
        return CommandResult(accumulator ?: BigDecimal.ZERO)
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        // This chained command is treated as MathExpressionCommand for visitor purposes
        @Suppress("UNCHECKED_CAST")
        return visitor.visit(this as MathExpressionCommand) as T
    }

    companion object {
        fun applyOperation(left: BigDecimal, right: BigDecimal, operator: String): BigDecimal {
            return when (operator) {
                "+" -> left + right
                "-" -> left - right
                "*" -> left * right
                "/" -> {
                    if (right == BigDecimal.ZERO) {
                        BigDecimal.ZERO  // no division by zero!
                    } else {
                        left.divide(right, 4, RoundingMode.HALF_DOWN)
                    }
                }
                else -> BigDecimal.ZERO
            }
        }
    }
}

/**
 * Optimized math expression for simple binary operations
 * Inline operation dispatch to avoid lambda overhead
 */
class InlinedMathCommand(
    token: IIslToken,
    private val left: IIslCommand,
    private val right: IIslCommand,
    private val operator: String
) : BaseCommand(token) {

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val leftResult = left.executeAsync(executionContext).value
        val rightResult = right.executeAsync(executionContext).value

        val leftValue = ConvertUtils.tryParseDecimal(leftResult).getValueOrDefault()
        val rightValue = ConvertUtils.tryParseDecimal(rightResult).getValueOrDefault()

        // Inline operation dispatch - no lambda overhead
        val result = when (operator) {
            "+" -> leftValue + rightValue
            "-" -> leftValue - rightValue
            "*" -> leftValue * rightValue
            "/" -> {
                if (rightValue == BigDecimal.ZERO) {
                    BigDecimal.ZERO
                } else {
                    leftValue.divide(rightValue, 4, RoundingMode.HALF_DOWN)
                }
            }
            else -> BigDecimal.ZERO
        }

        return CommandResult(result)
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        // This inlined command is treated as MathExpressionCommand for visitor purposes
        @Suppress("UNCHECKED_CAST")
        return visitor.visit(this as MathExpressionCommand) as T
    }
}

