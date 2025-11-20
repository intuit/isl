package com.intuit.isl.commands

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ValueNode
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.VariableSelectorValueToken
import com.intuit.isl.utils.IIslReference
import com.intuit.isl.utils.InstantNode
import com.intuit.isl.utils.JsonConvert
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import java.time.Instant

open class VariableSelectorValueCommand(token: VariableSelectorValueToken) : BaseCommand(token) {
    val variableName = token.variableName

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val variable = executionContext.operationContext.getVariable(variableName)

        return CommandResult(variable)
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this)
    }
}

/**
 * Select the value from a variable ($var.property1.property2....)
 */
class VariableWithPathSelectorValueCommand(token: VariableSelectorValueToken) : VariableSelectorValueCommand(token) {
    // Pre-Compile
    private val path: JsonPath = JsonPath.compile(token.path)

    companion object {
        val configuration: Configuration = initConfiguration()

        private fun initConfiguration(): Configuration {
            return Configuration
                .ConfigurationBuilder()
                .jsonProvider(InstantJacksonJsonNodeJsonProvider(JsonConvert.mapper))
                .mappingProvider(JacksonMappingProvider(JsonConvert.mapper))
                .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build()
        }
    }

    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        val variable = executionContext.operationContext.getVariable(variableName)

        if (variable != null) {
            val result = path.read<Any?>(variable, configuration)
            return CommandResult(result)
        }

        return CommandResult(variable)
    }

    // We need a custom JacksonNodeProvider to avoid the Json Paths selection to convert our InstantNodes into TextNodes
    // during the selection process
    private class InstantJacksonJsonNodeJsonProvider(objectMapper: ObjectMapper?) :
        JacksonJsonNodeJsonProvider(objectMapper) {

        override fun setArrayIndex(array: Any?, index: Int, newValue: Any?) {
            if (newValue is Instant) // wrap in InstantNode
                super.setArrayIndex(array, index, InstantNode(newValue))
            else
                super.setArrayIndex(array, index, newValue)
        }

        override fun unwrap(o: Any?): Any? {
            if (o is InstantNode)
                return o.value
            if (o is IIslReference && o is ValueNode)
                return o
            return super.unwrap(o)
        }
    }
}

/**
 * Fast access to the list of properties. $.value.value
 */
class FastVariableWithPathSelectorValueCommand(token: VariableSelectorValueToken, val pathParts: Array<String>) :
    VariableSelectorValueCommand(token) {
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        var variable = executionContext.operationContext.getVariable(variableName)

        for (pathPart in pathParts) {
            variable = variable?.get(pathPart);
        }
        return CommandResult(variable)
    }
}

/**
 * Fast access ONE property generally for $.value
 */
class FastSingleVariableWithPathSelectorValueCommand(token: VariableSelectorValueToken) :
    VariableSelectorValueCommand(token) {
    private val path = token.path;
    override suspend fun executeAsync(executionContext: ExecutionContext): CommandResult {
        var variable = executionContext.operationContext.getVariable(variableName)

        variable = variable?.get(path);

        return CommandResult(variable)
    }
}