package com.intuit.isl.test.mocks

import com.fasterxml.jackson.databind.JsonNode

class MockParamsMatcher(private val rawParams: Map<Int, JsonNode>) {
    private val matchingParamMap = mutableMapOf<Int, MockMatcher>()

    init {
        rawParams.forEach { (i, it) ->
            matchingParamMap[i] = MockMatcher(it)
        }
    }

    fun match(targetParams: Map<Int, JsonNode>, looseMatch: Boolean = true): Boolean {
        targetParams.forEach { (i, it) ->
            val matcher = matchingParamMap[i]
            if (matcher != null) {
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
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MockParamsMatcher) return false
        return rawParams == other.rawParams
    }

    override fun hashCode(): Int = rawParams.hashCode()
}