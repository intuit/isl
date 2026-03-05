package com.intuit.isl.cmd

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.utils.JsonConvert

/**
 * YAML-driven unit test suite (e.g. *.tests.yaml).
 * Format:
 * - category: name of test group
 * - setup: islSource, optional mockSource, optional inline mocks (applied after mockSource so they override)
 * - tests: list of test entries with name, functionName, optional input, expected result
 */
data class YamlUnitTestSuite(
    val category: String? = null,
    val setup: YamlTestSetup,
    val tests: List<YamlUnitTestEntry>
)

data class YamlTestSetup(
    val islSource: String,
    /**
     * Mock file(s) to load (same format as @.Mock.Load).
     * - Single string: mockSource: mymocks.yaml
     * - Array: mockSource: [commonMocks.yaml, otherMocks.yaml] — loaded in order, each overrides the previous.
     */
    val mockSource: JsonNode? = null,
    /** Inline mocks in same format as @.Mock.Load (func/annotation arrays). Applied after mockSource; all mocks are additive (params differentiate). Uses Map so Jackson reliably deserializes nested YAML. */
    val mocks: Map<String, Any?>? = null
) {
    /** Converts inline mocks to ObjectNode for applyMocksFromNode. Handles both Map (from YAML) and ensures func/annotation structure. */
    fun mocksAsObject(): ObjectNode? {
        val map = mocks ?: return null
        val node: JsonNode = JsonConvert.mapper.valueToTree(map)
        return if (node.isObject) node as ObjectNode else null
    }

    /** Resolves mockSource to a list of file names: one for a string, many for an array, empty if null. */
    fun mockSourceFiles(): List<String> = when {
        mockSource == null || mockSource.isNull -> emptyList()
        mockSource.isTextual -> listOf(mockSource.asText().trim()).filter { it.isNotEmpty() }
        mockSource.isArray -> mockSource.mapNotNull { if (it.isTextual) it.asText().trim().takeIf { s -> s.isNotEmpty() } else null }
        else -> emptyList()
    }

    /** Last mock source file name (for error messages), or null if none. */
    fun mockSourceDisplayName(): String? = mockSourceFiles().lastOrNull()
}

data class YamlUnitTestEntry(
    val name: String,
    val functionName: String,
    val byPassAnnotations: Boolean? = null,
    /** Single value for single-param functions, or object with param names as keys for multiple params. */
    val input: Any? = null,
    val expected: JsonNode? = null
)
