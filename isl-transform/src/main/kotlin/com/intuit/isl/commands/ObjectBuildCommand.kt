package com.intuit.isl.commands

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ValueNode
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.IIslToken
import com.intuit.isl.types.TypedJsonNodeFactory

class ObjectBuildCommand(token: IIslToken, var commands: MutableList<IIslCommand>) : BaseCommand(token) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        // run the list of statements - collect the results into a JsonNode
        val result = TypedJsonNodeFactory.instance.typedObjectNode(token.islType);

        // To help debugging we can create fake variables
//        val tempVariableName = "@Object-${this.hashCode()}";
//        executionContext.operationContext.setVariable(tempVariableName, result);

        for (c in commands) {
            val commandResult = c.executeAsync(executionContext);

            if (!commandResult.propertyName.isNullOrEmpty() && commandResult.append != false) {
                // TBD: There are way too many ways to do this.
                val realValue = JsonConvert.handleConvert(commandResult.value, this, c);

                // We need to be careful here! We could get duplicate(ish) data due to the funny format for properties
                // prop1.prop2.prop3: ...
                // prop1.prop2.prop4: ...
                // we'll get two objects back, so we have to update the prop1 and prop2 by merging, we can't just set them
                val existingNode = result.get(commandResult.propertyName);
                if (existingNode != null && existingNode !is ValueNode) {
                    JsonConvert.merge(existingNode, realValue) as ObjectNode;
                } else {
                    result.set(commandResult.propertyName, realValue);
                }
            } else if (commandResult.append == true) {
                // we need to append whatever we received in the value into our object (e.g. we got a child from if/else)
                (commandResult.value as? ObjectNode)?.fields()?.forEach {
                    result.set<JsonNode>(it.key, it.value);
                }
            }
        }

        //executionContext.operationContext.removeVariable(tempVariableName);
        return CommandResult(result);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}