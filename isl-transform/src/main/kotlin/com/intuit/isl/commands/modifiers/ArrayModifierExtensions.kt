package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.VariableWithPathSelectorValueCommand
import com.intuit.isl.parser.tokens.IIslToken
import com.intuit.isl.parser.tokens.VariableSelectorValueToken
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.utils.ObjectRefNode
import com.jayway.jsonpath.InvalidPathException
import com.jayway.jsonpath.JsonPath
import com.intuit.isl.runtime.TransformException
import java.math.BigDecimal
import java.util.LinkedHashMap

/**
 * Array manipulation modifier extensions for ISL.
 *
 * Provides modifiers for array operations:
 * - isEmpty, isNotEmpty
 * - at, push, pop, pushItems
 * - reverse, unique, slice
 * - range (Array.range for generating numeric ranges)
 * - group.by (group array items by field name or JSON path; options: as, keyAs, valuesAs, nullKeyAs, emptyKeyAs)
 */
object ArrayModifierExtensions {

    fun registerExtensions(context: IOperationContext) {
        // Array modifiers
        context.registerSyncExtensionMethod("Modifier.isEmpty", ArrayModifierExtensions::isArrayEmpty)
        context.registerSyncExtensionMethod("Modifier.isNotEmpty", ArrayModifierExtensions::isArrayNotEmpty)
        context.registerSyncExtensionMethod("Modifier.push", ArrayModifierExtensions::push)
        context.registerSyncExtensionMethod("Modifier.pop", ArrayModifierExtensions::pop)
        context.registerSyncExtensionMethod("Modifier.at", ArrayModifierExtensions::at)
        context.registerSyncExtensionMethod("Modifier.reverse", ArrayModifierExtensions::reverse)
        context.registerSyncExtensionMethod("Modifier.pushItems", ArrayModifierExtensions::pushItems)
        context.registerSyncExtensionMethod("Array.range", ArrayModifierExtensions::range)
        context.registerSyncExtensionMethod("Modifier.unique", ArrayModifierExtensions::unique)
        context.registerSyncExtensionMethod("Array.unique", ArrayModifierExtensions::unique)
        context.registerSyncExtensionMethod("Modifier.slice", ArrayModifierExtensions::slice)
        context.registerSyncExtensionMethod("Array.slice", ArrayModifierExtensions::slice)

        // New modifiers
        context.registerSyncExtensionMethod("Modifier.first", ArrayModifierExtensions::first)
        context.registerSyncExtensionMethod("Modifier.last", ArrayModifierExtensions::last)
        context.registerSyncExtensionMethod("Modifier.take", ArrayModifierExtensions::take)
        context.registerSyncExtensionMethod("Modifier.drop", ArrayModifierExtensions::drop)
        context.registerSyncExtensionMethod("Modifier.indexOf", ArrayModifierExtensions::indexOf)
        context.registerSyncExtensionMethod("Modifier.lastIndexOf", ArrayModifierExtensions::lastIndexOf)
        context.registerSyncExtensionMethod("Modifier.chunk", ArrayModifierExtensions::chunk)
        context.registerSyncExtensionMethod("Modifier.group.*", ArrayModifierExtensions::group)
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
                second.forEach { target.add(JsonConvert.convert(it)) }
                target
            }
            is List<*> -> {
                val target = JsonNodeFactory.instance.arrayNode(second.size)
                second.forEach { target.add(JsonConvert.convert(it)) }
                target
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

        val path = JsonPathModifierSupport.evaluateJsonPathFromParameter(context)
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
     * Wildcard entry for `| group.by(...)`.
     * [FunctionExecuteContext.secondParameter] is the segment after `group.` (e.g. `"by"`).
     */
    private fun group(context: FunctionExecuteContext): Any? {
        val sub = ConvertUtils.tryToString(context.secondParameter)?.lowercase()
        return when (sub) {
            "by" -> groupByKey(context)
            else -> throw TransformException(
                "|group.$sub is not supported",
                context.command.token.position
            )
        }
    }

    /**
     * Group items from the piped collection by a key:
     * - Field name: `| group.by( "status" )`
     * - JSON path: `| group.by( $.address.city )` or `| group.by( "$.address.city" )`
     *
     * Optional second argument: `{ as: "array", keyAs: "key", valuesAs: "items",
     * nullKeyAs: "...", emptyKeyAs: "..." }`.
     */
    private fun groupByKey(context: FunctionExecuteContext): Any? {
        val collection = context.firstParameter
        val token = context.command.token as? com.intuit.isl.parser.tokens.ModifierValueToken
            ?: throw TransformException("|group.by internal error", context.command.token.position)
        if (token.arguments.isEmpty()) {
            throw TransformException(
                "|group.by requires a key (field name or JSON path such as \"$.field\")",
                token.position
            )
        }

        val path = resolveGroupByJsonPath(context, token.arguments.first())
        val options = context.fourthParameter as? ObjectNode
        val outputAsArray =
            ConvertUtils.tryToString(options?.get("as"))?.lowercase() == "array"
        val keyAs = ConvertUtils.tryToString(options?.get("keyAs")) ?: "key"
        val valuesAs = ConvertUtils.tryToString(options?.get("valuesAs")) ?: "items"
        val nullKeyAs = optionStringOrNull(options, "nullKeyAs")
        val emptyKeyAs = optionStringOrNull(options, "emptyKeyAs")

        val keyExtractor: (Any?) -> String
        val compiledPath = path
        if (compiledPath != null) {
            keyExtractor = { item: Any? ->
                groupingKeyString(
                    compiledPath.read(item, VariableWithPathSelectorValueCommand.configuration),
                    nullKeyAs,
                    emptyKeyAs
                )
            }
        } else {
            val fieldName = ConvertUtils.tryToString(context.thirdParameter)
                ?: throw TransformException(
                    "|group.by requires a non-empty field name when not using a JSON path",
                    token.position
                )
            keyExtractor = { item: Any? ->
                when (item) {
                    is ObjectNode -> groupingKeyString(item.get(fieldName), nullKeyAs, emptyKeyAs)
                    else -> groupingKeyString(null, nullKeyAs, emptyKeyAs)
                }
            }
        }

        val grouped = LinkedHashMap<String, ArrayNode>()
        val source = ConvertUtils.getIterator(collection)
        source?.forEach { item ->
            val key = keyExtractor(item)
            grouped.getOrPut(key) { JsonNodeFactory.instance.arrayNode() }
                .add(JsonConvert.convert(item))
        }

        return if (outputAsArray) {
            val result = JsonNodeFactory.instance.arrayNode(grouped.size)
            grouped.forEach { (key, items) ->
                val row = JsonNodeFactory.instance.objectNode()
                row.put(keyAs, key)
                row.set<JsonNode>(valuesAs, items)
                result.add(row)
            }
            result
        } else {
            val result = JsonNodeFactory.instance.objectNode()
            grouped.forEach { (key, items) -> result.set<JsonNode>(key, items) }
            result
        }
    }

    /**
     * Turn a resolved key value into a string safe for JSON object keys.
     * [nullKeyAs] replaces null / un-stringifiable keys (default `"null"`).
     * [emptyKeyAs] replaces `""` (default remains `""`).
     */
    private fun groupingKeyString(value: Any?, nullKeyAs: String?, emptyKeyAs: String?): String {
        val s = ConvertUtils.tryToString(value)
        if (s == null) return nullKeyAs ?: "null"
        if (s.isEmpty()) return emptyKeyAs ?: ""
        return s
    }

    /** Read optional string option; missing key or JSON null → null (use defaults in groupingKeyString). */
    private fun optionStringOrNull(options: ObjectNode?, name: String): String? {
        if (options == null || !options.has(name)) return null
        val node = options.get(name)
        if (node == null || node.isNull) return null
        return ConvertUtils.tryToString(node)
    }

    /**
     * For `group.by`, the first user argument lives at [FunctionExecuteContext.thirdParameter]
     * (after the piped value and the wildcard segment `"by"`).
     */
    private fun resolveGroupByJsonPath(context: FunctionExecuteContext, selectorArgument: IIslToken): JsonPath? {
        if (selectorArgument is VariableSelectorValueToken && selectorArgument.variableName == "$") {
            val selector = if (selectorArgument.path.isNullOrBlank()) {
                "$"
            } else {
                "${selectorArgument.variableName}.${selectorArgument.path}"
            }
            return compileGroupByPath(selector, context)
        }
        val literal = ConvertUtils.tryToString(context.thirdParameter)
        if (!literal.isNullOrBlank() && literal.startsWith("$")) {
            return compileGroupByPath(literal, context)
        }
        return null
    }

    private fun compileGroupByPath(selector: String, context: FunctionExecuteContext): JsonPath {
        try {
            return JsonPath.compile(selector)
        } catch (e: InvalidPathException) {
            throw TransformException(
                "|group.by invalid JSON path '$selector' - ${e.message}",
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

