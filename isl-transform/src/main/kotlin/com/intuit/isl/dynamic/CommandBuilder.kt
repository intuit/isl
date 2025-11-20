package com.intuit.isl.dynamic

import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.commands.builder.ExecutionBuilder
import com.intuit.isl.parser.IslScriptVisitor
import com.intuit.isl.parser.tokens.ModuleImplementationToken

class CommandBuilder {
//    fun condition(left: IIslToken, operator: String, rigth: IIslToken): ConditionToken {
//        return ConditionToken(ConditionExpressionToken(left, operator , rigth, Position.Dynamic))
//    }

    /**
     * Compile a simplified expression (String Interpolation Only).
     * This result can be cached against the @param expressionValue
     */
    fun expression(expressionValue: String): IIslCommand {
        // TODO: Some optimization so we don't always fully execute the expression
        // We can check if it contains any of our special chars and drop out if not.

        val visitor = IslScriptVisitor("expression.isl", expressionValue, System.out);
        val expressionToken = visitor.parseExpression();

        val command = ExecutionBuilder("expression.isl", ModuleImplementationToken.Empty, null, null)
            .buildExpression(expressionToken);

        return command;
    }
}