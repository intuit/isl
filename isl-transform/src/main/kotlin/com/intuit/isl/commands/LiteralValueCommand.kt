package com.intuit.isl.commands

import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.LiteralValueToken

class LiteralValueCommand(token: LiteralValueToken) : BaseCommand(token) {

    override val token: LiteralValueToken
        get() = super.token as LiteralValueToken;

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        return CommandResult(token.value);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}