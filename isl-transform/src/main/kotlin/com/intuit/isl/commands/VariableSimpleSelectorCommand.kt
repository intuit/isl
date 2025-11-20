package com.intuit.isl.commands

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.parser.tokens.SimplePropertySelectorValueToken
import com.intuit.isl.parser.tokens.SimpleVariableSelectorValueToken
import com.intuit.isl.utils.JsonConvert

/**
 * Select the value from a variable $var or $var[ index ] or $var [ condition ]
 */
class VariableSimpleSelectorCommand(
    token: SimpleVariableSelectorValueToken,
    private val expression: IEvaluableConditionCommand?
) :
    BaseCommand(token) {
    override val token: SimpleVariableSelectorValueToken
        get() = super.token as SimpleVariableSelectorValueToken;

    companion object {
        suspend fun resolvePart(
            variable: JsonNode?,
            executionContext: ExecutionContext,
            indexSelector: Int?,
            conditionSelector: IEvaluableConditionCommand?
        ): CommandResult {
            if (variable != null) {
                // TODO: In theory we could gain a small improvement here by pre-calculating if we have an index or condition and have two classes
                // but it's not worth it
                if (indexSelector != null) {
                    if (variable is ArrayNode) {
                        if (indexSelector >= 0 && indexSelector < variable.size()) {
                            val result = variable[indexSelector];
                            return CommandResult(result);
                        }
                    }
                    // out of bounds is null
                    return CommandResult(null);
                } else
                    if (conditionSelector != null) {
                        if (variable is ArrayNode) {
                            val result = variable.filter {
                                executionContext.operationContext.setVariable("\$", JsonConvert.convert(it));
                                return@filter conditionSelector.evaluateConditionAsync(executionContext);
                            }

                            return CommandResult(result);
                        }
                        // out of bounds is null
                        return CommandResult(null);
                    }
            }

            return CommandResult(variable);
        }
    }


    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val variable = executionContext.operationContext.getVariable(token.name);

        return resolvePart(variable, executionContext, token.indexSelector, expression);
    }


    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}

class VariablePropertySelectorCommand(
    token: SimplePropertySelectorValueToken,
    private val previousCommand: IIslCommand,
    private val expression: IEvaluableConditionCommand?
) :
    BaseCommand(token) {
    override val token: SimplePropertySelectorValueToken
        get() = super.token as SimplePropertySelectorValueToken;

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val variable = previousCommand.executeAsync(executionContext).value;

        if (variable is ObjectNode) {
            // read the child property
            val childProperty = variable[token.name];

            return VariableSimpleSelectorCommand.resolvePart(childProperty, executionContext, token.indexSelector, expression);
        }

        return CommandResult(null);
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}