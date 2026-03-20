package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("unused")
class ArrayModifiersTest : YamlTransformTest("array-modifiers") {

    private fun groupBy(): Stream<Arguments> = createTests("group-by")

    @ParameterizedTest
    @MethodSource("groupBy")
    fun runFixtures(
        testName: String,
        script: String,
        expectedResult: JsonNode,
        map: Map<String, Any?>? = null
    ) {
        run(script, expectedResult.toPrettyString(), map)
    }
}
