package com.intuit.isl.commands

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.getVariableCanonical
import com.intuit.isl.common.setVariableCanonical
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

    internal val indexCondition: IEvaluableConditionCommand? get() = expression

    private val variableKey = token.name.lowercase()

    override val token: SimpleVariableSelectorValueToken
        get() = super.token as SimpleVariableSelectorValueToken;

    companion object {
        fun resolvePart(
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
                    return CommandResult.NULL
                } else
                    if (conditionSelector != null) {
                        if (variable is ArrayNode) {
                            val result = variable.filter {
                                executionContext.operationContext.setVariableCanonical("\$", JsonConvert.convert(it));
                                return@filter conditionSelector.evaluateCondition(executionContext);
                            }

                            return CommandResult(result);
                        }
                        // out of bounds is null
                        return CommandResult.NULL
                    }
            }

            return CommandResult(variable);
        }
    }


    override fun execute(executionContext: ExecutionContext): CommandResult {
        val variable = executionContext.operationContext.getVariableCanonical(variableKey);

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

    internal val propertyPrevious: IIslCommand get() = previousCommand
    internal val propertyIndexCondition: IEvaluableConditionCommand? get() = expression

    override val token: SimplePropertySelectorValueToken
        get() = super.token as SimplePropertySelectorValueToken;

    override fun execute(executionContext: ExecutionContext): CommandResult {
        val variable = previousCommand.execute(executionContext).value;

        if (variable is ObjectNode) {
            // read the child property
            val childProperty = variable[token.name];

            return VariableSimpleSelectorCommand.resolvePart(childProperty, executionContext, token.indexSelector, expression);
        }

        return CommandResult.NULL
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this);
    }
}