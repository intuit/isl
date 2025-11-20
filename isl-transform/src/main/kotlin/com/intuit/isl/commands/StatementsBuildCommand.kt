package com.intuit.isl.commands

import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.IIslToken
import com.intuit.isl.utils.JsonConvert

/**
 * Similar to an object build command but there is no Object {} output but we run and capture last valid result
 * Similar to Kotlin pretty much
 */
class StatementsBuildCommand(token: IIslToken, val commands: List<IIslCommand>) : BaseCommand(token) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        // run the list of statements - collect the results
        var commandResult: CommandResult? = null;

//        val tempVariableName = "@Return-${this.hashCode()}";
//        executionContext.operationContext.setVariable(tempVariableName, JsonConvert.convert(commandResult?.value))

        for (c in commands) {
            val cr = c.executeAsync(executionContext);
            // we need to ignore property assignment as they we don't want them captured
            if (cr.propertyName.isNullOrEmpty() && cr.value != null)
                commandResult = cr;
            //executionContext.operationContext.setVariable(tempVariableName, JsonConvert.convert(commandResult?.value))
        }

        return commandResult ?: CommandResult(null, validResult = false);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}