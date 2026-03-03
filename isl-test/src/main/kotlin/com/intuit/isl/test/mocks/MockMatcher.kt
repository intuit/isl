package com.intuit.isl.test.mocks

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.ConditionEvaluator
import com.intuit.isl.utils.JsonConvert

class MockMatcher(private val field: JsonNode) {
    private val parameterMap = hashMapOf<String, MockMatcher>()

    init {
        when (field) {
            is ObjectNode -> {
                field.fields().forEach {
                    parameterMap[it.key] = MockMatcher(it.value)
                }
            }

            is ArrayNode -> {
                field.forEachIndexed { i, it ->
                    parameterMap[i.toString()] = MockMatcher(it)
                }
            }
        }
    }

    fun match(targetNode: JsonNode, looseMatch: Boolean = true): Boolean {
        // Keep track of params that have been matched
        val paramsToMatch = parameterMap.keys.toMutableSet()
        val noParams = paramsToMatch.isEmpty()
        var matchedField = false

        when (targetNode) {
            is ObjectNode -> {
                targetNode.fields().forEach {
                    val key = it.key
                    val value = it.value
                    val matcher = parameterMap[key]
                    paramsToMatch.remove(key)
                    if (matcher != null) {
                        // Check if elements match
                        if (!matcher.match(value, looseMatch)) {
                            return false
                        }
                    }
                    // If there's no matcher, then it's considered a match
                    // only if it's a loose match
                    else if (!looseMatch) {
                        return false
                    }
                }
            }

            is ArrayNode -> {
                targetNode.forEachIndexed { i, it ->
                    val key = i.toString()
                    val matcher = parameterMap[key]
                    if (matcher != null) {
                        paramsToMatch.remove(key)
                        // Check if elements match
                        if (!matcher.match(it, looseMatch)) {
                            return false
                        }
                    }
                    // If there's no matcher, then it's considered a match
                    // only if it's a loose match
                    else if (!looseMatch) {
                        return false
                    }
                }
            }

            else -> {
                matchedField = true
                val srcValue = JsonConvert.getValue(field)
                val targetValue = JsonConvert.getValue(targetNode)
                // Do simple check for other elements
                if (!ConditionEvaluator.equalish(srcValue, targetValue)) {
                    return false
                }
            }
        }
        // If there's no params left to match, then it's a match
        // If the field is matched and there's no params in the first place, then it's a match
        return if (noParams && paramsToMatch.isEmpty()) {
            matchedField
        } else {
            paramsToMatch.isEmpty()
        }
    }
}