package com.intuit.isl.commands

import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.AssignVariableToken
import com.intuit.isl.types.TypedObjectNode
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.types.TypedJsonNodeFactory

/**
 * $var: value
 */
class AssignVariableCommand(token: AssignVariableToken, val value: IIslCommand) : BaseCommand(token) {
    override val token: AssignVariableToken
        get() = super.token as AssignVariableToken;

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val result = value.executeAsync(executionContext);

        val node = TypedObjectNode.tryTypedObject(
            token.islType,
            JsonConvert.handleConvert(result.value, this, value)
        );

        try {
            if (token.topPropertyName != null) {
                val existingVariable = executionContext.operationContext.getTransformVariable(token.name);
                if (existingVariable?.readOnly == true) {
                    throw TransformException(
                        "Could not set property=${token.topPropertyName} in readonly variable=${token.name}.",
                        token.position
                    )
                }

                val variableToModify = existingVariable?.value ?: TypedJsonNodeFactory.instance.typedObjectNode(token.islType);
                val newValue = JsonConvert.merge(variableToModify, node) as ObjectNode;
                executionContext.operationContext.setVariable(token.name, newValue);
            } else {
                // TODO: we should really try to modify whatever variable we already have so we can build complex objects
                // (maybe some append command?)
                // we need to traverse the tree in order to do this properly
                executionContext.operationContext.setVariable(token.name, node);
            }
            return CommandResult(null, null);
        }catch (e: TransformException){
            throw e;
        }catch (e: Exception){
            throw TransformException(e.message + " at ${token.position}", token.position, e);
        }
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}