package com.intuit.isl.commands

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.ForEachToken
import com.intuit.isl.utils.ExcludeFromJacocoGeneratedReport
import com.intuit.isl.utils.IIslIterable

open class ForEachCommand(token: ForEachToken, private val source: IIslCommand, val statements: IIslCommand) :
    BaseCommand(token) {

    internal val foreachSource: IIslCommand get() = source
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
        // Lazily allocated: only created when the first non-null iteration result is encountered.
        // Side-effect-only loops (where every statement returns null) never allocate this array.
        var result: ArrayNode? = null;

        source?.forEachIndexed { i, it ->
            executionContext.operationContext.setVariable(token.iterator, JsonConvert.convert(it));
            executionContext.operationContext.setVariable(token.iterator + "index", JsonConvert.convert(i));

            executionContext.executionHook?.onBeforeExecute(statements, executionContext)
            val itValue = statements.executeAsync(executionContext);
            executionContext.executionHook?.onAfterExecute(statements, executionContext, itValue)

            if(itValue.validResult == false)
                return@forEachIndexed; // ignore

            // Skip null iteration values — avoids collecting NullNodes from side-effect statements
            // like variable assignments ($var: value) that have no meaningful return value.
            val converted = itValue.value ?: return@forEachIndexed;
            if (result == null) result = JsonNodeFactory.instance.arrayNode(defaultSize);
            result!!.add(JsonConvert.convert(converted));
        }

        // cleanup
        executionContext.operationContext.removeVariable(token.iterator);
        executionContext.operationContext.removeVariable(token.iterator + "index");

        return CommandResult(result ?: JsonNodeFactory.instance.arrayNode(), null, true);
    }

    @ExcludeFromJacocoGeneratedReport
    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}