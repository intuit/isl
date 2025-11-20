package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.node.ValueNode
import com.intuit.isl.commands.VariableWithPathSelectorValueCommand
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.dynamic.CommandBuilder
import com.intuit.isl.dynamic.run
import com.intuit.isl.parser.tokens.ModifierValueToken
import com.intuit.isl.parser.tokens.VariableSelectorValueToken
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import com.jayway.jsonpath.InvalidPathException
import com.jayway.jsonpath.JsonPath
import java.util.*

/**
 * Object manipulation modifier extensions for ISL.
 * 
 * Provides modifiers for object operations:
 * - length, keys, kv (key-value pairs)
 * - sort (for objects, arrays, and strings)
 * - delete (remove properties from objects)
 * - select (JSON path selection)
 * - getProperty, setProperty (case-insensitive property access)
 * - merge (merge expression results)
 * - pick, omit (select or exclude object properties)
 * - rename (rename object keys)
 * - has (check if object has a key)
 * - default (return default value if null or empty)
 */
object ObjectModifierExtensions {
    
    fun registerExtensions(context: IOperationContext) {
        // Object modifiers
        context.registerExtensionMethod("Modifier.length", ObjectModifierExtensions::length)
        context.registerExtensionMethod("Modifier.keys", ObjectModifierExtensions::keys)
        context.registerExtensionMethod("Modifier.kv", ObjectModifierExtensions::kv)
        context.registerExtensionMethod("Modifier.sort", ObjectModifierExtensions::sort)
        context.registerExtensionMethod("Modifier.delete", ObjectModifierExtensions::delete)
        
        // JSON path and property access
        context.registerExtensionMethod("Modifier.select", ObjectModifierExtensions::selectJson)
        context.registerExtensionMethod("Modifier.getProperty", ObjectModifierExtensions::getProperty)
        context.registerExtensionMethod("Modifier.setProperty", ObjectModifierExtensions::setProperty)
        
        // Merge
        context.registerExtensionMethod("Modifier.merge", ObjectModifierExtensions::merge)
        
        // New modifiers
        context.registerExtensionMethod("Modifier.pick", ObjectModifierExtensions::pick)
        context.registerExtensionMethod("Modifier.omit", ObjectModifierExtensions::omit)
        context.registerExtensionMethod("Modifier.rename", ObjectModifierExtensions::rename)
        context.registerExtensionMethod("Modifier.has", ObjectModifierExtensions::has)
        context.registerExtensionMethod("Modifier.default", ObjectModifierExtensions::default)
    }
    
    private fun length(context: FunctionExecuteContext): Any {
        val first = context.firstParameter
        // Performance: Fast-path for common types to avoid JsonConvert overhead
        return when (first) {
            is String -> first.length
            is TextNode -> first.asText().length
            is ArrayNode -> first.size()
            is ObjectNode -> first.size()
            is Array<*> -> first.size
            is Collection<*> -> first.size
            else -> JsonConvert.length(first)
        }
    }
    
    /**
     * Return the list of keys from a map / object - perfect for looping through an object (e.g. lists of custom fields)
     */
    private fun keys(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        // this only works for ObjectNode or Maps really
        return when (first) {
            is ObjectNode -> first.fieldNames().asSequence().toList()
            is Map<*, *> -> first.keys.toList()
            else -> null
        }
    }
    
    /**
     * Return a list of Key/Value from the object
     */
    data class KV(val key: Any, val value: Any)
    private fun kv(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        
        // this only works for ObjectNode or Maps really
        return when (first) {
            is ObjectNode -> first.fields().asSequence().map { KV(it.key, it.value) }.toList()
            is Map<*, *> -> first
            else -> null
        }
    }
    
    private fun sort(context: FunctionExecuteContext): Any? {
        val input = context.firstParameter
        val options = context.secondParameter as ObjectNode?
        
        val sortBy: String? = when (options) {
            is ObjectNode -> {
                val by = options.get("by")
                when (by) {
                    is ValueNode -> by?.textValue()
                    else -> null
                }
            }
            
            else -> null
        }
        
        val sortOrder: String = when (options) {
            is ObjectNode -> {
                val order = options.get("order")
                when (order) {
                    is ValueNode -> if (order?.textValue()?.lowercase() == "desc") "desc" else "asc"
                    else -> "asc"
                }
            }
            
            else -> "asc"
        }
        
        val caseSensitive: Boolean = when (options) {
            is ObjectNode -> {
                val order = options.get("caseSensitive")
                when (order) {
                    is ValueNode -> !(order.textValue()?.lowercase() == "false" || !order.booleanValue())
                    else -> true
                }
            }
            
            else -> true
        }
        
        val caseTransform = when (caseSensitive) {
            true -> { text -> text }
            false -> { text: String? -> text?.lowercase() }
        }
        
        return when (input) {
            is ObjectNode -> {
                // sort the keys
                
                val comparer: Comparator<MutableMap.MutableEntry<String, JsonNode>> = when (sortOrder) {
                    "desc" -> {
                        compareByDescending { caseTransform(it.key) }
                    }
                    
                    else -> {
                        compareBy { caseTransform(it.key) }
                    }
                }
                
                val result = JsonNodeFactory.instance.objectNode()
                input.fields().asSequence().toList().sortedWith(comparer)
                    .forEach {
                        result.set<JsonNode>(it.key, it.value)
                    }
                return result
            }
            
            is ArrayNode -> {
                val firstValue = input.firstOrNull() ?: return input
                
                val valueAccessor = when {
                    // Note that we are not using `isNullOrBlank` here as it's possible to use spaces as a key in JSON
                    sortBy.isNullOrEmpty() -> { node: JsonNode -> node }
                    else -> { node: JsonNode -> node?.get(sortBy) ?: node }
                }
                
                val valueProjection = when (valueAccessor(firstValue)) {
                    is NumericNode -> JsonNode::decimalValue
                    is BooleanNode -> JsonNode::booleanValue
                    else -> { node: JsonNode -> caseTransform(node.textValue()) }
                }
                
                val comparer: Comparator<JsonNode> = when (sortOrder) {
                    "desc" -> {
                        compareByDescending { valueProjection(valueAccessor(it)) }
                    }
                    
                    else -> {
                        compareBy { valueProjection(valueAccessor(it)) }
                    }
                }
                
                val result = JsonNodeFactory.instance.arrayNode(input.size())
                val sorted = input.sortedWith(comparer)
                result.addAll(sorted)
                return result
            }
            
            is TextNode -> {
                if (caseSensitive) {
                    val sorted = input.textValue().toCharArray()
                        .apply { if (sortOrder == "asc") sort() else sortDescending() }
                    return JsonNodeFactory.instance.textNode(String(sorted))
                }
                
                // The case-insensitive sort will result in worse performance due to the conversion from array of char to array of strings
                // This is required to use the String.CASE_INSENSITIVE_ORDER comparator
                val list = input.textValue().toCharArray()
                    .map { it.toString() }
                Collections.sort(list, String.CASE_INSENSITIVE_ORDER)
                return JsonNodeFactory.instance.textNode(list.joinToString(separator = ""))
            }
            
            else -> null
        }
    }
    
    private fun delete(context: FunctionExecuteContext): Any? {
        val param = context.firstParameter
        val name = ConvertUtils.tryToString(context.secondParameter)
        
        // this only works for ObjectNode or Maps really
        return when (param) {
            is ObjectNode -> {
                // sort the keys
                return param.without(name)
            }
            else -> null
        }
    }
    
    /**
     * Convert an argument to a JsonPath. This could have been | select ( "$.path" ) or | select ( $.path )
     */
    private fun evaluateJsonPathFromParameter(context: FunctionExecuteContext): JsonPath? {
        // let's be smart here - and support both `| select ( "$.stuff" )` and `|select ( $.stuff )`
        // there is a danger in the second that is gets executed but we can avoid that a bit
        
        // TODO: This could in theory be optimized as compilation time in the visitor!
        val token = context.command.token as? ModifierValueToken?
        val selectorArgument = token?.arguments?.firstOrNull()
        if (selectorArgument == null)
            return null
        
        val selector =
            if (selectorArgument is VariableSelectorValueToken && selectorArgument.variableName == "$") {
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
        } catch (e: InvalidPathException) {
            throw TransformException(
                "|${context.functionName} Invalid Path '$selector' - ${e.message}",
                context.command.token.position
            )
        }
    }
    
    private fun selectJson(context: FunctionExecuteContext): Any? {
        val first = JsonConvert.convert(context.firstParameter)
        
        val path = evaluateJsonPathFromParameter(context)
        if (path == null)
            return first
        
        val result = path.read<Any?>(first, VariableWithPathSelectorValueCommand.configuration)
        return result
    }
    
    private fun getProperty(context: FunctionExecuteContext): Any? {
        val first = JsonConvert.convert(context.firstParameter) as? ObjectNode
        val selector = ConvertUtils.tryToString(context.secondParameter)
        
        if (first != null && selector != null) {
            // this is case-insensitive
            first.fields()
                .forEach {
                    if (selector.equals(it.key, true))
                        return it.value
                }
        }
        return null
    }
    
    private fun setProperty(context: FunctionExecuteContext): Any? {
        val first = JsonConvert.convert(context.firstParameter) as? ObjectNode ?: JsonNodeFactory.instance.objectNode()
        val selector = ConvertUtils.tryToString(context.secondParameter)
        val value = JsonConvert.convert(context.thirdParameter)
        
        if (first != null && selector != null) {
            // this is case-insensitive
            first.set<JsonNode>(selector, value)
        }
        return first
    }
    
    private suspend fun merge(context: FunctionExecuteContext): Any? {
        val expression = ConvertUtils.tryToString(context.firstParameter)
        if (expression?.contains("@.") == true) {
            return expression
        }
        val command = expression?.let { CommandBuilder().expression(it) }
        val result = command?.let { context.executionContext.operationContext.run(it) }
        return result
    }
    
    /**
     * Pick only specified keys from object
     */
    private fun pick(context: FunctionExecuteContext): Any? {
        val obj = JsonConvert.convert(context.firstParameter) as? ObjectNode ?: return null
        
        // Collect all keys to pick from parameters (starting from second parameter)
        val keysToPick = mutableSetOf<String>()
        for (i in 1 until context.parameters.size) {
            ConvertUtils.tryToString(context.parameters[i])?.let { keysToPick.add(it) }
        }
        
        val result = JsonNodeFactory.instance.objectNode()
        obj.fields().forEach { (key, value) ->
            if (keysToPick.contains(key)) {
                result.set<JsonNode>(key, value)
            }
        }
        
        return result
    }
    
    /**
     * Omit specified keys from object
     */
    private fun omit(context: FunctionExecuteContext): Any? {
        val obj = JsonConvert.convert(context.firstParameter) as? ObjectNode ?: return null
        
        // Collect all keys to omit from parameters (starting from second parameter)
        val keysToOmit = mutableSetOf<String>()
        for (i in 1 until context.parameters.size) {
            ConvertUtils.tryToString(context.parameters[i])?.let { keysToOmit.add(it) }
        }
        
        val result = JsonNodeFactory.instance.objectNode()
        obj.fields().forEach { (key, value) ->
            if (!keysToOmit.contains(key)) {
                result.set<JsonNode>(key, value)
            }
        }
        
        return result
    }
    
    /**
     * Rename a key in object
     */
    private fun rename(context: FunctionExecuteContext): Any? {
        val obj = JsonConvert.convert(context.firstParameter) as? ObjectNode ?: return null
        val oldKey = ConvertUtils.tryToString(context.secondParameter) ?: return obj
        val newKey = ConvertUtils.tryToString(context.thirdParameter) ?: return obj
        
        if (!obj.has(oldKey)) return obj
        
        val result = JsonNodeFactory.instance.objectNode()
        obj.fields().forEach { (key, value) ->
            if (key == oldKey) {
                result.set<JsonNode>(newKey, value)
            } else {
                result.set<JsonNode>(key, value)
            }
        }
        
        return result
    }
    
    /**
     * Check if object has a specific key
     */
    private fun has(context: FunctionExecuteContext): Any {
        val obj = JsonConvert.convert(context.firstParameter) as? ObjectNode
        val key = ConvertUtils.tryToString(context.secondParameter) ?: return false
        
        return obj?.has(key) ?: false
    }
    
    /**
     * Return a default value if the input is null or empty
     * Usage: $value | default("N/A")
     * 
     * Returns the default value if:
     * - Input is null
     * - Input is an empty string
     * - Input is an empty array
     * - Input is an empty object
     * - Input is a NullNode
     * 
     * Otherwise returns the original input value.
     */
    private fun default(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val defaultValue = context.secondParameter
        
        // Check if the value is null or empty
        val isEmpty = when (first) {
            null -> true
            is String -> first.isEmpty()
            is TextNode -> first.asText().isEmpty()
            is ArrayNode -> first.isEmpty
            is ObjectNode -> first.isEmpty
            is JsonNode -> first.isNull || (first.isTextual && first.asText().isEmpty())
            else -> false
        }
        
        return if (isEmpty) defaultValue else first
    }
}

