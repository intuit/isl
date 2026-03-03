package com.intuit.isl.test.mocks

import com.fasterxml.jackson.databind.JsonNode

class MockCaptureContext {
    val captures = mutableListOf<Map<Int, JsonNode>>()
}