package com.intuit.isl.commands

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.getVariableCanonical
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.IIslToken

class ArrayCommand(
    token: IIslToken,
    private val values: ArrayList<IIslCommand>,
    val seedVariableName: String? = null
) : BaseCommand(token) {
    internal val elementCommands: List<IIslCommand> get() = values
    override fun execute(executionContext: ExecutionContext): CommandResult {
        // When seeded, load the existing variable value directly and mutate it in place,
        // avoiding both the deepCopy and the shallow-copy that a spread would otherwise require.
        val result = if (seedVariableName != null)
            executionContext.operationContext.getVariableCanonical(seedVariableName) as? ArrayNode
                ?: JsonNodeFactory.instance.arrayNode(values.size)
        else
            JsonNodeFactory.instance.arrayNode(values.size);

        for (v in values) {
            val realValue = v.execute(executionContext);
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