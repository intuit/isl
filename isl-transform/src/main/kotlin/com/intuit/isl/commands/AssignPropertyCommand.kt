package com.intuit.isl.commands

import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.AssignDynamicPropertyToken
import com.intuit.isl.parser.tokens.AssignPropertyToken
import com.intuit.isl.types.TypedObjectNode
import com.intuit.isl.utils.ConvertUtils

/**
 * something: value
 * something: $variable
 * ....
 */
class AssignPropertyCommand(token: AssignPropertyToken, val value: IIslCommand) : BaseCommand(token) {
    override val token: AssignPropertyToken
        get() = super.token as AssignPropertyToken;

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        // description: 123
        val result = value.executeAsync(executionContext);   // 123

        if (token.islType != null) {
            val objectValue = result.value as? ObjectNode?;
            if(objectValue is ObjectNode) {
                val typedNode = TypedObjectNode.tryTypedObject(token.islType, objectValue);
                return CommandResult(typedNode, token.name, result.append); // description -> "123"
            }
        }

        return CommandResult(result.value, token.name, result.append); // description -> "123"
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}

class AssignDynamicPropertyCommand(token: AssignDynamicPropertyToken, val name: IIslCommand, val value: IIslCommand) :
    BaseCommand(token) {
    override val token: AssignDynamicPropertyToken
        get() = super.token as AssignDynamicPropertyToken;

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        // `$dynamicname`: 123
        val name = name.executeAsync(executionContext);
        val result = value.executeAsync(executionContext);   // 123
        return CommandResult(result.value, ConvertUtils.tryToString(name.value), result.append); // description -> "123"
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}