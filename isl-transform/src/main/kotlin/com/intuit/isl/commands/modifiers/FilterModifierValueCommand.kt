package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.commands.BaseCommand
import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.IEvaluableConditionCommand
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.FilterModifierValueToken
import com.intuit.isl.utils.JsonConvert

class FilterModifierValueCommand(
    token: FilterModifierValueToken,
    private val value: IIslCommand,
    private val expression: IEvaluableConditionCommand
) : BaseCommand(token) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val sourceCollection = value.executeAsync(executionContext).value;

        val source = when (sourceCollection) {
            is Iterable<Any?> -> sourceCollection
            else -> null;
        };

        // save and restore a potential old $it variable
        val oldIt = executionContext.operationContext.getVariable("\$fit");

        val defaultSize = if(sourceCollection is Collection<Any?>) sourceCollection.size else 10;
        val resultArray = JsonNodeFactory.instance.arrayNode(defaultSize);

        source?.forEach { it ->
            executionContext.operationContext.setVariable("\$fit", JsonConvert.convert(it));
            // use the new $ format only
            executionContext.operationContext.setVariable("\$", JsonConvert.convert(it));
            if( expression.evaluateConditionAsync(executionContext) ){
                resultArray.add( JsonConvert.convert(it) );
            }
        };

        executionContext.operationContext.removeVariable("\$");
        if (oldIt == null)
            executionContext.operationContext.removeVariable("\$fit");
        else
            executionContext.operationContext.setVariable("\$fit", oldIt);

        return CommandResult(resultArray);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}