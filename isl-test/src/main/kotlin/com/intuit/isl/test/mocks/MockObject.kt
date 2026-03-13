package com.intuit.isl.test.mocks

import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.utils.Position

class MockObject {
    private val matchingParamMap = mutableMapOf<MockParamsMatcher, Any?>()
    private val matchingParamMapIndexed = mutableMapOf<MockParamsMatcher, MutableList<Any?>>()
    private val matchingParamCallCount = mutableMapOf<Int, Int>()
    private val matchingParamCaptures = mutableMapOf<Int, MockCaptureContext>()

    private var defaultReturnValue: Any? = null
    private val defaultReturnIndexed = mutableListOf<Any?>()
    private var defaultCallCount = 0
    private val defaultReturnCaptures = MockCaptureContext()
    /** True when a default (no-params) mock was ever added; used to distinguish "no match" from "matched and returned null". */
    private var hasDefaultReturn = false

    /** Clears all mocks and captures so this MockObject can be reused (e.g. for local overrides). */
    fun clear() {
        matchingParamMap.clear()
        matchingParamMapIndexed.clear()
        matchingParamCallCount.clear()
        matchingParamCaptures.clear()
        defaultReturnValue = null
        defaultReturnIndexed.clear()
        defaultCallCount = 0
        defaultReturnCaptures.captures.clear()
        hasDefaultReturn = false
    }

    fun addMock(returnValue: Any?, parameters: Map<Int, JsonNode>, index: Int? = null, functionName: String? = null): Int? {
        return if (parameters.isEmpty()) {
            hasDefaultReturn = true
            if (index != null) {
                ensureIndexCapacity(defaultReturnIndexed, index)
                defaultReturnIndexed[index - 1] = returnValue
            } else {
                defaultReturnValue = returnValue
            }
            null
        } else {
            val mockMatcher = MockParamsMatcher(parameters)
            if (index != null) {
                val list = matchingParamMapIndexed.getOrPut(mockMatcher) { mutableListOf() }
                ensureIndexCapacity(list, index)
                list[index - 1] = returnValue
            } else {
                if (matchingParamMap.containsKey(mockMatcher)) {
                    val fn = if (functionName != null) " for function @.$functionName" else ""
                    println("[ISL Mock] Overriding existing mock$fn for params: ${parameters.values}")
                }
                matchingParamMap[mockMatcher] = returnValue
            }
            mockMatcher.hashCode()
        }
    }

    private fun ensureIndexCapacity(list: MutableList<Any?>, index: Int) {
        while (list.size < index) {
            list.add(null)
        }
    }

    fun getCaptures(instanceId: Int?): Any? {
        val captures = mutableListOf<Map<Int, JsonNode>>()
        if (instanceId == null) {
            captures.addAll(matchingParamCaptures.values.flatMap { it.captures })
            captures.addAll(defaultReturnCaptures.captures)
        } else {
            matchingParamCaptures[instanceId]?.let {
                captures.addAll(it.captures)
            }
        }

        return if (captures.isEmpty()) {
            null
        } else {
            JsonConvert.convert(captures)
        }
    }

    /**
     * Finds a matching mock for the given parameters.
     * @param functionName Optional; used in the error message when no match is found.
     * @param position Optional; attached to the thrown exception when no match is found.
     * @param mockFileName Optional; used in the error message when testFileName is null (which mock file to add the entry to).
     * @param testFileName Optional; when set, error message suggests adding to the test file (in setup.mocks or in the test) instead of mock files.
     * @throws TransformException when no mock matches and no default mock is defined (with function name, params, and YAML snippet in the message).
     */
    fun tryFindMatch(
        targetParams: Map<Int, JsonNode>,
        looseMatch: Boolean = true,
        functionName: String? = null,
        position: Position? = null,
        mockFileName: String? = null,
        testFileName: String? = null
    ): Any? {
        matchingParamMap.forEach { (matcher, returnValue) ->
            if (matcher.match(targetParams, looseMatch)) {
                val captureContext = matchingParamCaptures.getOrPut(matcher.hashCode()) { MockCaptureContext() }
                captureContext.captures.add(targetParams)
                return returnValue
            }
        }

        matchingParamMapIndexed.forEach { (matcher, returnList) ->
            if (matcher.match(targetParams, looseMatch)) {
                val captureContext = matchingParamCaptures.getOrPut(matcher.hashCode()) { MockCaptureContext() }
                captureContext.captures.add(targetParams)
                val callCount = matchingParamCallCount.getOrPut(matcher.hashCode()) { 0 }
                if (callCount >= returnList.size) {
                    throw MockExhaustedException(
                        "Mock exhausted: expected at most ${returnList.size} call(s), but got call #${callCount + 1}"
                    )
                }
                val result = returnList[callCount]
                matchingParamCallCount[matcher.hashCode()] = callCount + 1
                return result
            }
        }

        // Default (no param match): check indexed first, then standard
        defaultReturnCaptures.captures.add(targetParams)
        if (defaultReturnIndexed.isNotEmpty()) {
            if (defaultCallCount >= defaultReturnIndexed.size) {
                throw MockExhaustedException(
                    "Mock exhausted: expected at most ${defaultReturnIndexed.size} call(s), but got call #${defaultCallCount + 1}"
                )
            }
            val result = defaultReturnIndexed[defaultCallCount]
            defaultCallCount++
            return result
        }
        if (!hasDefaultReturn) {
            val paramsJson = targetParams.toSortedMap().values.let { JsonConvert.mapper.writeValueAsString(it) }
            val addToHint = when {
                testFileName != null -> "test file [$testFileName] (in setup.mocks or in the test)"
                else -> "[${mockFileName ?: "your-mocks.yaml"}]"
            }
            val place = position?.let { pos ->
                "file=${pos.file}, line=${pos.line}, column=${pos.column}" +
                    (pos.endLine?.let { ", endLine=$it" } ?: "") +
                    (pos.endColumn?.let { ", endColumn=$it" } ?: "")
            }
            val yamlSnippet = buildString {
                appendLine("- name: \"${functionName ?: "<function>"}\"")
                if (targetParams.isNotEmpty()) {
                    appendLine("  params: $paramsJson")
                }
                appendLine("  result: <replace with expected return value>")
            }
            val message = buildString {
                appendLine(if (functionName != null) "No mock matched for function @.$functionName. The test must only call with parameters that are mocked." else "No mock matched. The test must only call with parameters that are mocked.")
                if (functionName != null) appendLine("Function: @.$functionName")
                if (place != null) appendLine("Called from: $place")
                appendLine("Parameters: $paramsJson")
                appendLine("")
                appendLine("To mock this function add this to your $addToHint then rerun the tests:")
                appendLine("")
                appendLine("func:")
                appendLine(yamlSnippet)
                appendLine("")
            }
            throw TransformException(message.trimEnd(), position, null)
        }
        return defaultReturnValue
    }
}

class MockExhaustedException(message: String) : Exception(message)