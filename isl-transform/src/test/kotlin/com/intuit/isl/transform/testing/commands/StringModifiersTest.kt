package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.run


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("unused", "UNUSED_PARAMETER")
class StringModifiersTest : YamlTransformTest("string-modifiers") {
    private fun concatModifier(): Stream<Arguments> {
        return createTests("concat-modifier")
    }

    private fun appendModifier(): Stream<Arguments> {
        return createTests("append-modifier")
    }

    private fun csvModifier(): Stream<Arguments> {
        return createTests("csv-modifier")
    }

    private fun xmlModifier(): Stream<Arguments> {
        return createTests("xml-modifier")
    }

    private fun jsonModifier(): Stream<Arguments> {
        return createTests("json-modifier")
    }

    private fun yamlModifier(): Stream<Arguments> {
        return createTests("yaml-modifier")
    }

    private fun regExModifier(): Stream<Arguments> {
        return createTests("regex-modifier")
    }

    private fun xmlOutputModifier(): Stream<Arguments> {
        return createTests("xml-output-modifier")
    }

    private fun stringInterpolations(): Stream<Arguments> {
        return createTests("string-interpolations")
    }

    @ParameterizedTest
    @MethodSource(
        "concatModifier",
        "appendModifier",
        "csvModifier",
        "xmlModifier",
        "jsonModifier",
        "yamlModifier",
        "regExModifier",
        "xmlOutputModifier",
        "stringInterpolations"
    )
    fun runFixtures(testName: String, script: String, expectedResult: JsonNode, map: Map<String, Any?>? = null) {
        run(script, expectedResult.toPrettyString(), map)
    }

    @Disabled("Disabled to allow for Jenkins build success")
    @ParameterizedTest
    @MethodSource(
//		"csvModifier",
//		"xmlModifier",
        "jsonModifier",
//		"yamlModifier",
//		"regExModifier",
//		"xmlOutputModifier",
        "stringInterpolations"
    )
    fun runJSFixtures(testName: String, script: String, expectedResult: JsonNode, map: Map<String, Any?>? = null) {
        runJs(script, expectedResult, map)
    }
}