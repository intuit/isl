package com.intuit.isl.commands

import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.IIslToken
import java.lang.StringBuilder

class InterpolateCommand(token: IIslToken, private val values: ArrayList<IIslCommand>) : BaseCommand(token) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val sb = StringBuilder();

        for(v in values) {
            val realValue = v.executeAsync(executionContext);

            // value could be a TextNode or another JsonNode!
            when(val value = realValue.value) {
                is TextNode -> sb.append(value.textValue());
                null -> {};
                is NullNode -> {};
                else -> sb.append(realValue.value);
            }
        }

        return CommandResult(sb.toString());
    }

    /**
     * Try to simplify this to have only one command.
    * That allows some commands to return the full object vs being converted to a string.
     */
    fun trySimplify(): IIslCommand{
        if(this.values.size == 1)
            return this.values.first();
        return this;
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}