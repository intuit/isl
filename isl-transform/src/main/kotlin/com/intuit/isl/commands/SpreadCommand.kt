package com.intuit.isl.commands

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.parser.tokens.IIslToken

class SpreadCommand(token: IIslToken, val variable: IIslCommand): BaseCommand(token) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val value = variable.executeAsync(executionContext).value;

        if(value is ObjectNode) {
            val clone = value.deepCopy();
            // append - this will do the spread
            return CommandResult(clone, null, true);
        } else  if(value is ArrayNode) {
            val clone = value.deepCopy();
            // append - this will do the spread
            return CommandResult(clone, null, true);
        }
        else
            return CommandResult(null);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}