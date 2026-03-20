package com.intuit.isl.commands.modifiers

import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.parser.tokens.ModifierValueToken
import com.intuit.isl.parser.tokens.VariableSelectorValueToken
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.ConvertUtils
import com.jayway.jsonpath.InvalidPathException
import com.jayway.jsonpath.JsonPath

/**
 * Shared JSON Path handling for object/array modifiers that take a path argument
 * (e.g. `| select($.field)`, `| unique($.id)`).
 *
 * [HardwiredModifierValueCommand] may carry [HardwiredModifierValueCommand.precompiledModifierJsonPath],
 * compiled once at build time when the path is syntactically static.
 */
object JsonPathModifierSupport {

    fun evaluateJsonPathFromParameter(context: FunctionExecuteContext): JsonPath? {
        (context.command as? HardwiredModifierValueCommand)?.precompiledModifierJsonPath?.let { return it }

        val token = context.command.token as? ModifierValueToken ?: return null
        val selectorArgument = token.arguments.firstOrNull() ?: return null

        val selector =
            if (selectorArgument is VariableSelectorValueToken && selectorArgument.variableName == "$") {
                if (selectorArgument.path.isNullOrBlank())
                    selectorArgument.variableName
                else
                    "${selectorArgument.variableName}.${selectorArgument.path}"
            } else
                ConvertUtils.tryToString(context.secondParameter) ?: "\$"

        try {
            return JsonPath.compile(selector)
        } catch (e: InvalidPathException) {
            throw TransformException(
                "|${context.functionName} Invalid Path '$selector' - ${e.message}",
                context.command.token.position
            )
        }
    }
}
