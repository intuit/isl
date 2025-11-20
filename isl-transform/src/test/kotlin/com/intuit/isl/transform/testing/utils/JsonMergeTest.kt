package com.intuit.isl.transform.testing.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.intuit.isl.utils.JsonConvert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

import kotlin.test.assertEquals

class JsonMergeTest {
    companion object {
        @JvmStatic
        fun mergeJson(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("""{ "abc": 1 }""", """{ "def": 2 }""", """{ "abc": 1, "def": 2 }"""),
                Arguments.of(
                    """{ "a": { "b": { "c": 1 } } } }""", """{ "a": { "bb": { "d": 2 } } }""",
                    """{ "a": { "b": { "c": 1 }, "bb": { "d": 2 } } }"""
                ),
                Arguments.of(
                    """{ "a": { "b": { "c": 1 } } } }""", """{ "a": { "b": { "d": 2 } } }""",
                    """{ "a": { "b": { "c": 1, "d": 2 } } }"""
                )
            );
        }
    }

    @ParameterizedTest
    @MethodSource("mergeJson")
    fun test(source: String, merged: String, expectedResult: String){
        println("\n\n$source $merged");

        val s = ObjectMapper().readTree(source);
        val m = ObjectMapper().readTree(merged);

        val result = JsonConvert.merge(s, m);
        println("Result:\n$result");

        val mapper = ObjectMapper();
        assertEquals(mapper.readTree(expectedResult), mapper.readTree(result.toString()));
    }
}