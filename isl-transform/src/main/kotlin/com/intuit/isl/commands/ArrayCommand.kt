package com.intuit.isl.commands

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.IIslToken

class ArrayCommand(token: IIslToken, private val values: ArrayList<IIslCommand>) : BaseCommand(token) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        // return as a JSON Array
        val result = JsonNodeFactory.instance.arrayNode(values.size);

        for (v in values) {
            val realValue = v.executeAsync(executionContext);
            if (realValue.append == true && realValue.value is ArrayNode) {
                val appendArray = realValue.value as ArrayNode;
                appendArray.forEach {
                    result.add(it);
                }
            } else {
                result.add(JsonConvert.handleConvert(realValue.value, this, v));
            }
        }

        return CommandResult(result);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}