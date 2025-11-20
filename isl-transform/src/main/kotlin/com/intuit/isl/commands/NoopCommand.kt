package com.intuit.isl.commands

import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.parser.tokens.BaseToken
import com.intuit.isl.utils.Position

class NoopCommand(token: NoopToken): BaseCommand(token) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        return CommandResult(null);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}
class NoopToken: BaseToken(Position("",0,0,0,0)){
    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}