package com.intuit.isl.cmd

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.utils.JsonConvert
import java.io.IOException

/**
 * Options for comparing expected vs actual in YAML test assertions.
 * All default to false (strict comparison).
 *
 * In YAML, assertOptions can be written as:
 * - Object: `assertOptions: { nullSameAsMissing: true, ... }`
 * - Comma-separated list: `assertOptions: nullSameAsMissing, nullSameAsEmptyArray, ...`
 * - Array: `assertOptions: [nullSameAsMissing, nullSameAsEmptyArray, ...]`
 */
@JsonDeserialize(using = AssertOptionsDeserializer::class)
data class AssertOptions(
    /** Treat null and missing (absent key) as equal. */
    val nullSameAsMissing: Boolean = false,
    /** Treat null and empty array [] as equal. */
    val nullSameAsEmptyArray: Boolean = false,
    /** Treat missing (absent key) and empty array [] as equal. */
    val missingSameAsEmptyArray: Boolean = false,
    /** Only compare keys present in expected; ignore extra keys in actual. */
    val ignoreExtraFieldsInActual: Boolean = false,
    /** Compare numbers by numeric value only (e.g. 1234.0 equals 1234 equals 1234.00). */
    val numbersEqualIgnoreFormat: Boolean = false
) {
    companion object {
        private val OPTION_NAMES = setOf(
            "nullSameAsMissing",
            "nullSameAsEmptyArray",
            "missingSameAsEmptyArray",
            "ignoreExtraFieldsInActual",
            "numbersEqualIgnoreFormat"
        )

        fun fromNames(names: List<String>): AssertOptions {
            val normalized = names.map { it.trim() }.filter { it.isNotEmpty() }.filter { it in OPTION_NAMES }.toSet()
            return AssertOptions(
                nullSameAsMissing = "nullSameAsMissing" in normalized,
                nullSameAsEmptyArray = "nullSameAsEmptyArray" in normalized,
                missingSameAsEmptyArray = "missingSameAsEmptyArray" in normalized,
                ignoreExtraFieldsInActual = "ignoreExtraFieldsInActual" in normalized,
                numbersEqualIgnoreFormat = "numbersEqualIgnoreFormat" in normalized
            )
        }
    }
}

/**
 * Deserializes assertOptions from either an object (boolean keys) or a string/array of option names.
 */
class AssertOptionsDeserializer : JsonDeserializer<AssertOptions>() {
    @Throws(IOException::class)
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): AssertOptions {
        val node: JsonNode = p.codec.readTree(p) ?: return AssertOptions()
        return when {
            node.isObject -> deserializeObject(node)
            node.isTextual -> AssertOptions.fromNames(node.asText().split(','))
            node.isArray -> AssertOptions.fromNames(node.map { if (it.isTextual) it.asText() else "" })
            else -> AssertOptions()
        }
    }

    private fun deserializeObject(node: JsonNode): AssertOptions {
        return AssertOptions(
            nullSameAsMissing = node.path("nullSameAsMissing").asBoolean(false),
            nullSameAsEmptyArray = node.path("nullSameAsEmptyArray").asBoolean(false),
            missingSameAsEmptyArray = node.path("missingSameAsEmptyArray").asBoolean(false),
            ignoreExtraFieldsInActual = node.path("ignoreExtraFieldsInActual").asBoolean(false),
            numbersEqualIgnoreFormat = node.path("numbersEqualIgnoreFormat").asBoolean(false)
        )
    }
}

/**
 * YAML-driven unit test suite (e.g. *.tests.yaml).
 * Format:
 * - category: name of test group
 * - setup: islSource, optional mockSource, optional inline mocks (applied after mockSource so they override)
 * - assertOptions: optional assertion comparison options
 * - tests or islTests: list of test entries with name, functionName, optional input, expected result
 */
data class YamlUnitTestSuite(
    val category: String? = null,
    val setup: YamlTestSetup,
    val assertOptions: AssertOptions? = null,
    @com.fasterxml.jackson.annotation.JsonProperty("tests") val tests: List<YamlUnitTestEntry>? = null,
    @com.fasterxml.jackson.annotation.JsonProperty("islTests") val islTests: List<YamlUnitTestEntry>? = null
) {
    /** Test entries from either "tests" or "islTests" YAML key (for backward compatibility). */
    val entries: List<YamlUnitTestEntry>
        get() = (islTests ?: tests).orEmpty()
}

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
    /**
     * JSON paths to ignore when comparing expected vs actual (exact path match).
     * Paths use dot notation; array indices as [0], [1], etc. Examples:
     * - providerResponses.error.detail
     * - providerResponses.items[0].uid
     */
    val ignore: List<String>? = null,
    val expected: JsonNode? = null,
    /** Override suite assertOptions for this test only. Same formats as suite assertOptions (object, comma-separated, or array). */
    val assertOptions: AssertOptions? = null
)
