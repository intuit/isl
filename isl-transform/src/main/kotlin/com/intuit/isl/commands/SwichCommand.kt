package com.intuit.isl.commands

import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.IIslToken

class SwitchCaseCommand(token: IIslToken, val value: IIslCommand, val cases: Array<SwitchCaseBranchCommand>) :
    BaseCommand(token) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val leftResult = value.executeAsync(executionContext);

        cases
            .forEach {
                val result = it.executeAsync(leftResult.value, executionContext);
                if (result.first)
                    return result.second!!;
            }

        return CommandResult(null);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }

    class SwitchCaseBranchCommand(
        token: IIslToken,
        val condition: String,
        val right: IIslCommand,
        val result: IIslCommand
    ) :
        BaseCommand(token) {

        override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
            throw NotImplementedError();
        }

        suspend fun executeAsync(left: Any?, context: ExecutionContext): Pair<Boolean, CommandResult?> {
            val rightResult = right.executeAsync(context);
            if (ConditionEvaluator.evaluate(left, condition, rightResult.value))
                return Pair(true, result.executeAsync(context));
            return Pair(false, null);
        }

        override fun <T> visit(visitor: ICommandVisitor<T>): T {
            return visitor.visit(this);
        }
    }
}