package com.intuit.isl.commands.modifiers

import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.resetVariable
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.commands.BaseCommand
import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.parser.tokens.ModifierValueToken
import com.intuit.isl.utils.JsonConvert

class ReduceModifierValueCommand(
    token: ModifierValueToken,
    private val value: IIslCommand,
    private val argument: IIslCommand
) : BaseCommand(token) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val sourceCollection = value.executeAsync(executionContext).value;

        val source = when (sourceCollection) {
            is Iterable<Any?> -> sourceCollection
            else -> null;
        };

        // save and restore a potential old variables
        val oldAcc = executionContext.operationContext.getVariable("\$acc");
        val oldIt = executionContext.operationContext.getVariable("\$it");
        var acc = "" as Any?;
        source?.forEach { it ->
            executionContext.operationContext.setVariable("\$it", JsonConvert.convert(it));
            executionContext.operationContext.setVariable("\$acc", JsonConvert.convert(acc));
            acc = argument.executeAsync(executionContext).value;
        };
        executionContext.operationContext.resetVariable("\$it", oldIt);
        executionContext.operationContext.resetVariable("\$acc", oldAcc)

        return CommandResult(acc);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}