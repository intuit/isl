package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.commands.VariableWithPathSelectorValueCommand
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.utils.ObjectRefNode
import com.jayway.jsonpath.JsonPath
import java.math.BigDecimal

/**
 * Array manipulation modifier extensions for ISL.
 *
 * Provides modifiers for array operations:
 * - isEmpty, isNotEmpty
 * - at, push, pop, pushItems
 * - reverse, unique, slice
 * - range (Array.range for generating numeric ranges)
 */
object ArrayModifierExtensions {

    fun registerExtensions(context: IOperationContext) {
        // Array modifiers
        context.registerExtensionMethod("Modifier.isEmpty", ArrayModifierExtensions::isArrayEmpty)
        context.registerExtensionMethod("Modifier.isNotEmpty", ArrayModifierExtensions::isArrayNotEmpty)
        context.registerExtensionMethod("Modifier.push", ArrayModifierExtensions::push)
        context.registerExtensionMethod("Modifier.pop", ArrayModifierExtensions::pop)
        context.registerExtensionMethod("Modifier.at", ArrayModifierExtensions::at)
        context.registerExtensionMethod("Modifier.reverse", ArrayModifierExtensions::reverse)
        context.registerExtensionMethod("Modifier.pushItems", ArrayModifierExtensions::pushItems)
        context.registerExtensionMethod("Array.range", ArrayModifierExtensions::range)
        context.registerExtensionMethod("Modifier.unique", ArrayModifierExtensions::unique)
        context.registerExtensionMethod("Array.unique", ArrayModifierExtensions::unique)
        context.registerExtensionMethod("Modifier.slice", ArrayModifierExtensions::slice)
        context.registerExtensionMethod("Array.slice", ArrayModifierExtensions::slice)

        // New modifiers
        context.registerExtensionMethod("Modifier.first", ArrayModifierExtensions::first)
        context.registerExtensionMethod("Modifier.last", ArrayModifierExtensions::last)
        context.registerExtensionMethod("Modifier.take", ArrayModifierExtensions::take)
        context.registerExtensionMethod("Modifier.drop", ArrayModifierExtensions::drop)
        context.registerExtensionMethod("Modifier.indexOf", ArrayModifierExtensions::indexOf)
        context.registerExtensionMethod("Modifier.lastIndexOf", ArrayModifierExtensions::lastIndexOf)
        context.registerExtensionMethod("Modifier.chunk", ArrayModifierExtensions::chunk)
    }

    private fun isArrayEmpty(context: FunctionExecuteContext): Any {
        val first = context.firstParameter

        return when (first) {
            is ArrayNode -> first.size() == 0
            is Array<*> -> first.size == 0
            is List<*> -> first.size == 0
            is Map<*, *> -> first.size == 0
            else -> true
        }
    }

    private fun isArrayNotEmpty(context: FunctionExecuteContext): Any {
        val first = context.firstParameter

        return !when (first) {
            is ArrayNode -> first.size() == 0
            is Array<*> -> first.size == 0
            is List<*> -> first.size == 0
            is Map<*, *> -> first.size == 0
            else -> true
        }
    }

    private fun at(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val second = ConvertUtils.tryParseInt(context.secondParameter) ?: 0

        return when (first) {
            is ArrayNode -> first.elementAtOrNull(second)
            is Array<*> -> first.elementAtOrNull(second)
            is List<*> -> first.elementAtOrNull(second)
            is ObjectRefNode -> (first.value as? List<*>)?.elementAtOrNull(second)
            else -> null
        }
    }

    private fun push(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val second = context.secondParameter

        return when (first) {
            is ArrayNode -> first.add(JsonConvert.convert(second))
            null -> JsonNodeFactory.instance.arrayNode().add(JsonConvert.convert(second))
            else -> first
        }
    }

    private fun pop(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter

        return when (first) {
            is ArrayNode -> {
                return if (first.size() == 0)
                    null
                else
                    first.remove(first.size() - 1)
            }

            else -> null
        }
    }

    // assume the second param is an array - so we push all items into the first array
    private fun pushItems(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val second = context.secondParameter

        val secondArray = when (second) {
            is ArrayNode -> second
            is Array<*> -> {
                val target = JsonNodeFactory.instance.arrayNode(second.size)
                second.forEach {
                    target.add(JsonConvert.convert(it))
                }
                return second
            }

            is List<*> -> {
                val target = JsonNodeFactory.instance.arrayNode(second.size)
                second.forEach {
                    target.add(JsonConvert.convert(it))
                }
                return second
            }

            else -> return first
        }

        return when (first) {
            is ArrayNode -> first.addAll(secondArray)
            else -> secondArray
        }
    }

    private fun slice(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val second = ConvertUtils.tryParseInt(context.secondParameter, 0)!!
        val third = ConvertUtils.tryParseInt(context.thirdParameter, 0)!!

        fun resolveOffset(len: Int, offset: Int): Int {
            var result = if (offset < 0) {
                // count from the end of the array
                len + offset
            } else {
                offset
            }

            if (result < 0)
                return 0

            if (result >= len)
                return len

            return result
        }

        val result = when (first) {
            is ArrayNode -> {
                val from = resolveOffset(first.size(), second)
                val to = resolveOffset(first.size(), third)
                println("From=$from To=$to")
                val output = JsonNodeFactory.instance.arrayNode(to - from + 1)
                for (i in from until to) {
                    val value = first[i]
                    output.add(value)
                }
                return output
            }

            is List<*> -> {
                val from = resolveOffset(first.size, second)
                val to = resolveOffset(first.size, third)
                println("From=$from To=$to")
                val output = JsonNodeFactory.instance.arrayNode(to - from + 1)
                for (i in from until to) {
                    val value = first[i]
                    output.add(JsonConvert.convert(value))
                }
                return output
            }

            else -> null
        }

        return result
    }

    private fun reverse(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter

        val inputs: List<*>? = when (first) {
            is ArrayNode -> first.reversed()
            is List<*> -> first.reversed()
            is Array<*> -> first.reversed()
            else -> null
        }
        return inputs
    }

    private fun range(context: FunctionExecuteContext): Any {
        val first = ConvertUtils.tryParseLong(context.firstParameter, 0)!!
        val second = ConvertUtils.tryParseLong(context.secondParameter, 0)!!
        val increment = ConvertUtils.tryParseLong(context.thirdParameter, 1)!!

        // let's create straight an ArrayNode to avoid conversions down the track
        val result = JsonNodeFactory.instance.arrayNode()
        for (i in first until (first + second * increment) step increment) {
            result.add(i)
        }
        return result
    }

    // this method was very oddly implemented originally as it only works with numbers or strings
    private fun unique(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter

        val path = evaluateJsonPathFromParameter(context)
        if (path != null) {
            // evaluate uniquness based on this path
            val tempSet = mutableSetOf<String?>()
            val resultArray = JsonNodeFactory.instance.arrayNode()
            val iterator = ConvertUtils.getIterator(first)

            iterator?.forEach {
                val id = ConvertUtils.tryToString(
                    path.read<Any?>(it, VariableWithPathSelectorValueCommand.configuration)
                )
                if (tempSet.contains(id))
                    return@forEach // ignore
                resultArray.add(JsonConvert.convert(it))
                tempSet.add(id)
            }

            return resultArray
        }

        val inputs: List<Any>? = when (first) {
            is ArrayNode -> ConvertUtils.tryToList(first)
            is BigDecimal -> context.parameters.map { (BigDecimal(it.toString())) }
            is String -> context.parameters.map { (it.toString()) }
            else -> null
        }
        return inputs?.toSet()?.toList()
    }

    /**
     * Convert an argument to a JsonPath. This could have been | select ( "$.path" ) or | select ( $.path )
     */
    private fun evaluateJsonPathFromParameter(context: FunctionExecuteContext): JsonPath? {
        // let's be smart here - and support both `| select ( "$.stuff" )` and `|select ( $.stuff )`
        // there is a danger in the second that is gets executed but we can avoid that a bit

        // TODO: This could in theory be optimized as compilation time in the visitor!
        val token = context.command.token as? com.intuit.isl.parser.tokens.ModifierValueToken?
        val selectorArgument = token?.arguments?.firstOrNull()
        if (selectorArgument == null)
            return null

        val selector =
            if (selectorArgument is com.intuit.isl.parser.tokens.VariableSelectorValueToken && selectorArgument.variableName == "$") {
                if (selectorArgument.path.isNullOrBlank())
                    selectorArgument.variableName
                else
                    "${selectorArgument.variableName}.${selectorArgument.path}"
            } else
                ConvertUtils.tryToString(context.secondParameter) ?: "\$"

        try {
            val result =
                JsonPath.compile(selector)
            return result
        } catch (e: com.jayway.jsonpath.InvalidPathException) {
            throw com.intuit.isl.runtime.TransformException(
                "|${context.functionName} Invalid Path '$selector' - ${e.message}",
                context.command.token.position
            )
        }
    }

    /**
     * Get first element of array
     */
    private fun first(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter

        return when (first) {
            is ArrayNode -> if (first.size() > 0) first[0] else null
            is Array<*> -> first.firstOrNull()
            is List<*> -> first.firstOrNull()
            else -> null
        }
    }

    /**
     * Get last element of array
     */
    private fun last(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter

        return when (first) {
            is ArrayNode -> if (first.size() > 0) first[first.size() - 1] else null
            is Array<*> -> first.lastOrNull()
            is List<*> -> first.lastOrNull()
            else -> null
        }
    }

    /**
     * Take first n elements from array
     */
    private fun take(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val n = ConvertUtils.tryParseInt(context.secondParameter) ?: 1

        return when (first) {
            is ArrayNode -> {
                val result = JsonNodeFactory.instance.arrayNode()
                val count = minOf(n, first.size())
                for (i in 0 until count) {
                    result.add(first[i])
                }
                result
            }

            is Array<*> -> first.take(n)
            is List<*> -> first.take(n)
            else -> null
        }
    }

    /**
     * Drop first n elements from array
     */
    private fun drop(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val n = ConvertUtils.tryParseInt(context.secondParameter) ?: 1

        return when (first) {
            is ArrayNode -> {
                val result = JsonNodeFactory.instance.arrayNode()
                val start = minOf(n, first.size())
                for (i in start until first.size()) {
                    result.add(first[i])
                }
                result
            }

            is Array<*> -> first.drop(n)
            is List<*> -> first.drop(n)
            else -> null
        }
    }

    /**
     * Find index of element in array
     */
    private fun indexOf(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val searchElement = context.secondParameter
        val searchValue = JsonConvert.convert(searchElement)

        return when (first) {
            is ArrayNode -> {
                for (i in 0 until first.size()) {
                    if (first[i] == searchValue) {
                        return i
                    }
                }
                -1
            }

            is Array<*> -> first.indexOf(searchElement)
            is List<*> -> first.indexOf(searchElement)
            else -> -1
        }
    }

    /**
     * Find last index of element in array
     */
    private fun lastIndexOf(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val searchElement = context.secondParameter
        val searchValue = JsonConvert.convert(searchElement)

        return when (first) {
            is ArrayNode -> {
                for (i in first.size() - 1 downTo 0) {
                    if (first[i] == searchValue) {
                        return i
                    }
                }
                -1
            }

            is Array<*> -> first.lastIndexOf(searchElement)
            is List<*> -> first.lastIndexOf(searchElement)
            else -> -1
        }
    }

    /**
     * Split array into chunks of specified size
     */
    private fun chunk(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val size = ConvertUtils.tryParseInt(context.secondParameter) ?: return first

        if (size <= 0) return first

        return when (first) {
            is ArrayNode -> {
                val result = JsonNodeFactory.instance.arrayNode()
                var chunk = JsonNodeFactory.instance.arrayNode()

                for (i in 0 until first.size()) {
                    chunk.add(first[i])

                    if (chunk.size() == size || i == first.size() - 1) {
                        result.add(chunk)
                        chunk = JsonNodeFactory.instance.arrayNode()
                    }
                }

                result
            }

            is List<*> -> {
                val result = JsonNodeFactory.instance.arrayNode()
                first.chunked(size).forEach { sublist ->
                    val chunk = JsonNodeFactory.instance.arrayNode()
                    sublist.forEach { chunk.add(JsonConvert.convert(it)) }
                    result.add(chunk)
                }
                result
            }

            else -> first
        }
    }
}

