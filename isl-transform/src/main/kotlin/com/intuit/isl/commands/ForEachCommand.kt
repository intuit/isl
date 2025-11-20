package com.intuit.isl.commands

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.ForEachToken
import com.intuit.isl.utils.ExcludeFromJacocoGeneratedReport
import com.intuit.isl.utils.IIslIterable

open class ForEachCommand(token: ForEachToken, private val source: IIslCommand, val statements: IIslCommand) :
    BaseCommand(token) {
    override val token: ForEachToken
        get() = super.token as ForEachToken;

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val sourceCollection = source.executeAsync(executionContext).value;

        val source = when (sourceCollection) {
            is IIslIterable -> sourceCollection.getInnerIterator();
            is Iterable<Any?> -> sourceCollection

            else -> null;
        };

        val defaultSize = if(sourceCollection is Collection<Any?>) sourceCollection.size else 10;
        val result = JsonNodeFactory.instance.arrayNode(defaultSize);

        source?.forEachIndexed { i, it ->
            // To help debugging we can create internal variables
            //executionContext.operationContext.setVariable("@It-${this.hashCode()}", JsonConvert.convert(it));

            executionContext.operationContext.setVariable(token.iterator, JsonConvert.convert(it));
            executionContext.operationContext.setVariable(token.iterator + "index", JsonConvert.convert(i));

            val itValue = statements.executeAsync(executionContext);

            if(itValue.validResult == false)
                return@forEachIndexed; // ignore

            result.add(JsonConvert.convert(itValue.value));
        }

        return CommandResult(result, null, true);
    }

    @ExcludeFromJacocoGeneratedReport
    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}