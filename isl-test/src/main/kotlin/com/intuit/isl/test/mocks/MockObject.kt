package com.intuit.isl.test.mocks

import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.utils.JsonConvert

class MockObject {
    private val matchingParamMap = mutableMapOf<MockParamsMatcher, Any?>()
    private val matchingParamMapIndexed = mutableMapOf<MockParamsMatcher, MutableList<Any?>>()
    private val matchingParamCallCount = mutableMapOf<Int, Int>()
    private val matchingParamCaptures = mutableMapOf<Int, MockCaptureContext>()

    private var defaultReturnValue: Any? = null
    private val defaultReturnIndexed = mutableListOf<Any?>()
    private var defaultCallCount = 0
    private val defaultReturnCaptures = MockCaptureContext()

    fun addMock(returnValue: Any?, parameters: Map<Int, JsonNode>, index: Int? = null): Int? {
        return if (parameters.isEmpty()) {
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

    fun tryFindMatch(targetParams: Map<Int, JsonNode>, looseMatch: Boolean = true): Any? {
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
        return defaultReturnValue
    }
}

class MockExhaustedException(message: String) : Exception(message)