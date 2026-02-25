package com.intuit.isl.test

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intuit.isl.test.TransformTestPackage
import com.intuit.isl.runtime.FileInfo
import com.intuit.isl.runtime.TransformPackage
import com.intuit.isl.runtime.TransformPackageBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransformTestPackageTest {
    companion object {
        @JvmStatic
        fun testFunctions(): Stream<Arguments> {
            return Stream.of(
                // assert equal = true tests
                Arguments.of("@.Assert.equal(1, 1)", true),
                Arguments.of("@.Assert.equal(\"hello\", \"hello\")", true),
                Arguments.of("@.Assert.equal(null, null)", true),
                Arguments.of("@.Assert.equal(true, true)", true),
                Arguments.of(
                    """
                    |${"$"}var1 = 1;
                    |${"$"}var2 = 1;
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "number" : 1 };
                    |${"$"}var2 = { "number" : 1 };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : "valid" };
                    |${"$"}var2 = { "words" : "valid" };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : true };
                    |${"$"}var2 = { "words" : true };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : null };
                    |${"$"}var2 = { "words" : null };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : [1, 2, 3] };
                    |${"$"}var2 = { "words" : [1, 2, 3] };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : { "valid" : "yes" } };
                    |${"$"}var2 = { "words" : { "valid" : "yes" } };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : { "valid" : "yes", "words" : { "valid" : "yes" } } };
                    |${"$"}var2 = { "words" : { "valid" : "yes", "words" : { "valid" : "yes" } } };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                // Objects with different property order should be equal
                Arguments.of(
                    """
                    |${"$"}var1 = { "a" : 1, "b" : 2, "c" : "three" };
                    |${"$"}var2 = { "c" : "three", "a" : 1, "b" : 2 };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "outer" : { "x" : 1, "y" : 2 } };
                    |${"$"}var2 = { "outer" : { "y" : 2, "x" : 1 } };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),

                // assert equal = false tests
                Arguments.of("@.Assert.equal(1, 2)", false),
                Arguments.of("@.Assert.equal(\"hello\", \"there\")", false),
                Arguments.of("@.Assert.equal(null, 1)", false),
                Arguments.of("@.Assert.equal(true, false)", false),
                Arguments.of(
                    """
                    |${"$"}var1 = 1;
                    |${"$"}var2 = 2;
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "number" : 1 };
                    |${"$"}var2 = { "number" : 2 };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : "valid" };
                    |${"$"}var2 = { "words" : "not" };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : true };
                    |${"$"}var2 = { "words" : false };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : null };
                    |${"$"}var2 = { "words" : 1 };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : [1, 2, 3] };
                    |${"$"}var2 = { "words" : [1, 2, 4] };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : { "valid" : "yes" } };
                    |${"$"}var2 = { "words" : { "valid" : "no" } };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : { "valid" : "yes", "words" : { "valid" : "yes" } } };
                    |${"$"}var2 = { "words" : { "valid" : "yes", "words" : { "valid" : "no" } } };
                    |@.Assert.equal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),

                // assert notequal = false tests
                Arguments.of("@.Assert.notequal(1, 1)", false),
                Arguments.of("@.Assert.notequal(\"hello\", \"hello\")", false),
                Arguments.of("@.Assert.notequal(null, null)", false),
                Arguments.of("@.Assert.notequal(true, true)", false),
                Arguments.of(
                    """
                    |${"$"}var1 = 1;
                    |${"$"}var2 = 1;
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "number" : 1 };
                    |${"$"}var2 = { "number" : 1 };
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : "valid" };
                    |${"$"}var2 = { "words" : "valid" };
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : true };
                    |${"$"}var2 = { "words" : true };
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : null };
                    |${"$"}var2 = { "words" : null };
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : [1, 2, 3] };
                    |${"$"}var2 = { "words" : [1, 2, 3] };
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : { "valid" : "yes" } };
                    |${"$"}var2 = { "words" : { "valid" : "yes" } };
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : { "valid" : "yes", "words" : { "valid" : "yes" } } };
                    |${"$"}var2 = { "words" : { "valid" : "yes", "words" : { "valid" : "yes" } } };
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), false
                ),

                // assert notequal = true tests
                Arguments.of("@.Assert.notequal(1, 2)", true),
                Arguments.of("@.Assert.notequal(\"hello\", \"there\")", true),
                Arguments.of("@.Assert.notequal(null, 1)", true),
                Arguments.of("@.Assert.notequal(true, false)", true),
                Arguments.of(
                    """
                    |${"$"}var1 = 1;
                    |${"$"}var2 = 2;
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "number" : 1 };
                    |${"$"}var2 = { "number" : 2 };
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : "valid" };
                    |${"$"}var2 = { "words" : "not" };
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : true };
                    |${"$"}var2 = { "words" : false };
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : null };
                    |${"$"}var2 = { "words" : 1 };
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : [1, 2, 3] };
                    |${"$"}var2 = { "words" : [1, 2, 4] };
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : { "valid" : "yes" } };
                    |${"$"}var2 = { "words" : { "valid" : "no" } };
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),
                Arguments.of(
                    """
                    |${"$"}var1 = { "words" : { "valid" : "yes", "words" : { "valid" : "yes" } } };
                    |${"$"}var2 = { "words" : { "valid" : "yes", "words" : { "valid" : "no" } } };
                    |@.Assert.notequal(${"$"}var1, ${"$"}var2)
                """.trimMargin(), true
                ),

                // assert isnull tests
                Arguments.of("@.Assert.isnull(null)", true),
                Arguments.of(
                    """
                    |${"$"}var1 = null
                    |@.Assert.isnull(${"$"}var1)
                """.trimMargin(), true
                ),

                Arguments.of("@.Assert.isnull(1)", false),
                Arguments.of("@.Assert.isnull(\"hello\")", false),
                Arguments.of("@.Assert.isnull(true)", false),
                Arguments.of("@.Assert.isnull({ \"number\" : 1 })", false),
                Arguments.of(
                    """
                    |${"$"}var1 = { "number" : 1 }
                    |@.Assert.isnull(${"$"}var1)
                """.trimMargin(), false
                ),

                // assert notnull tests
                Arguments.of("@.Assert.notnull(null)", false),
                Arguments.of(
                    """
                    |${"$"}var1 = null
                    |@.Assert.notnull(${"$"}var1)
                """.trimMargin(), false
                ),

                Arguments.of("@.Assert.notnull(1)", true),
                Arguments.of("@.Assert.notnull(\"hello\")", true),
                Arguments.of("@.Assert.notnull(true)", true),
                Arguments.of("@.Assert.notnull({ \"number\" : 1 })", true),
                Arguments.of(
                    """
                    |${"$"}var1 = { "number" : 1 }
                    |@.Assert.notnull(${"$"}var1)
                """.trimMargin(), true
                ),
            )
        }

        @JvmStatic
        fun testMockFunctions(): Stream<Arguments> {
            val mapper = ObjectMapper()
            return Stream.of(
                // mocks with default value
                Arguments.of("Test.Function", "hello", true, null, null),
                Arguments.of("Test.Function", "hello", true, null, listOf(1)),
                Arguments.of("Test.Function", "hello", true, null, listOf("hello")),
                Arguments.of("Test.Function", "hello", true, null, listOf(mapper.readTree("{ \"result\" : 1 }"))),

                // mocks that return the value when params exactly match
                Arguments.of("Test.Function", "hello", true, listOf("there"), listOf("there")),
                Arguments.of("Test.Function", "hello", true, listOf(1), listOf(1)),
                Arguments.of("Test.Function", "hello", true, listOf(true), listOf(true)),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    true,
                    listOf(mapper.readTree("{ \"result\" : 1 }")),
                    listOf(mapper.readTree("{ \"result\" : 1 }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    true,
                    listOf(mapper.readTree("{ \"result\" : true }")),
                    listOf(mapper.readTree("{ \"result\" : true }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    true,
                    listOf(mapper.readTree("{ \"result\" : null }")),
                    listOf(mapper.readTree("{ \"result\" : null }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    true,
                    listOf(mapper.readTree("{ \"result\" : [1, 2, 3] }")),
                    listOf(mapper.readTree("{ \"result\" : [1, 2, 3] }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    true,
                    listOf(mapper.readTree("{ \"result\" : { \"valid\" : \"yes\" } }")),
                    listOf(mapper.readTree("{ \"result\" : { \"valid\" : \"yes\" } }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    true,
                    listOf(mapper.readTree("{ \"result\" : { \"valid\" : \"yes\", \"result\" : { \"valid\" : \"yes\" } } }")),
                    listOf(mapper.readTree("{ \"result\" : { \"valid\" : \"yes\", \"result\" : { \"valid\" : \"yes\" } } }"))
                ),

                // mocks that return the value when params loosely match
                Arguments.of(
                    "Test.Function",
                    "hello",
                    true,
                    listOf(mapper.readTree("{ \"result\" : \"yes\" }")),
                    listOf(mapper.readTree("{ \"result\" : \"yes\", \"other\" : \"no\" }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    true,
                    listOf(mapper.readTree("{ \"result\" : \"yes\" }")),
                    listOf(mapper.readTree("{ \"result\" : \"yes\", \"other\" : 1 }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    true,
                    listOf(mapper.readTree("{ \"result\" : \"yes\" }")),
                    listOf(mapper.readTree("{ \"result\" : \"yes\", \"other\" : true }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    true,
                    listOf(mapper.readTree("{ \"result\" : \"yes\" }")),
                    listOf(mapper.readTree("{ \"result\" : \"yes\", \"other\" : [1, 2, 3] }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    true,
                    listOf(mapper.readTree("{ \"result\" : [1, 2] }")),
                    listOf(mapper.readTree("{ \"result\" : [1, 2, 4] }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    true,
                    listOf(mapper.readTree("{ \"result\" : [{ \"valid\" : \"yes\" }, { \"valid\" : \"no\" }] }")),
                    listOf(mapper.readTree("{ \"result\" : [{ \"valid\" : \"yes\" }, { \"valid\" : \"no\", \"invalid\" : \"yes\" }, 4] }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    true,
                    listOf(mapper.readTree("{ \"result\" : { \"valid\" : \"yes\" } }")),
                    listOf(mapper.readTree("{ \"result\" : { \"valid\" : \"yes\", \"not\" : \"there\" } }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    true,
                    listOf(mapper.readTree("{ \"result\" : { \"valid\" : \"yes\" } }")),
                    listOf(mapper.readTree("{ \"result\" : { \"valid\" : \"yes\", \"not\" : [1, 2, 3] } }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    true,
                    listOf(mapper.readTree("{ \"result\" : { \"valid\" : \"yes\", \"result\" : { \"valid\" : \"yes\" } } }")),
                    listOf(mapper.readTree("{ \"result\" : { \"valid\" : \"yes\", \"result\" : { \"valid\" : \"yes\", \"result\" : { \"valid\" : \"yes\" } } } }"))
                ),

                // mocks that don't return the value when params don't exactly match
                Arguments.of("Test.Function", "hello", false, listOf(1), listOf(null)),
                Arguments.of("Test.Function", "hello", false, listOf("there"), listOf("not there")),
                Arguments.of("Test.Function", "hello", false, listOf(true), listOf(false)),
                Arguments.of("Test.Function", "hello", false, listOf(1), listOf(2)),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    false,
                    listOf(mapper.readTree("{ \"result\" : 1 }")),
                    listOf(mapper.readTree("{ \"result\" : 2 }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    false,
                    listOf(mapper.readTree("{ \"result\" : true }")),
                    listOf(mapper.readTree("{ \"result\" : false }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    false,
                    listOf(mapper.readTree("{ \"result\" : null }")),
                    listOf(mapper.readTree("{ \"result\" : 1 }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    false,
                    listOf(mapper.readTree("{ \"result\" : [1, 2, 3] }")),
                    listOf(mapper.readTree("{ \"result\" : [1, 2, 4] }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    false,
                    listOf(mapper.readTree("{ \"result\" : { \"valid\" : \"yes\" } }")),
                    listOf(mapper.readTree("{ \"result\" : { \"valid\" : \"no\" } }"))
                ),
                Arguments.of(
                    "Test.Function",
                    "hello",
                    false,
                    listOf(mapper.readTree("{ \"result\" : { \"valid\" : \"yes\" } }")),
                    listOf(mapper.readTree("{ \"result\" : { \"notValid\" : \"yes\" } }"))
                ),
            )
        }

        @JvmStatic
        fun testCaptureFunctions(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "0", "[{ \"0\" : 0 }]", 1
                ), Arguments.of(
                    "\"hello\"", "[{ \"0\" : \"hello\" }]", 1
                ), Arguments.of(
                    "0, 1, 2", "[{ \"0\" : 0, \"1\" : 1, \"2\" : 2 }]", 1
                ), Arguments.of(
                    "0, \"1\", \"hello\"", "[{ \"0\" : 0, \"1\" : \"1\", \"2\" : \"hello\" }]", 1
                ), Arguments.of(
                    "0", "[{ \"0\" : 0 }, { \"0\" : 0 }, { \"0\" : 0 }]", 3
                ), Arguments.of(
                    "\"hello\"", "[{ \"0\" : \"hello\" }, { \"0\" : \"hello\" }, { \"0\" : \"hello\" }]", 3
                ), Arguments.of(
                    "0, 1, 2",
                    "[{ \"0\" : 0, \"1\" : 1, \"2\" : 2 }, { \"0\" : 0, \"1\" : 1, \"2\" : 2 }, { \"0\" : 0, \"1\" : 1, \"2\" : 2 }]",
                    3
                ), Arguments.of(
                    "0, \"1\", \"hello\"",
                    "[{ \"0\" : 0, \"1\" : \"1\", \"2\" : \"hello\" }, { \"0\" : 0, \"1\" : \"1\", \"2\" : \"hello\" }, { \"0\" : 0, \"1\" : \"1\", \"2\" : \"hello\" }]",
                    3
                )
            )
        }
    }

    private val testFileFunction = "test"
    private val testFileName = "testFile.isl"

    private val transformPackageBuilder = TransformPackageBuilder()
    private lateinit var transformPackage: TransformPackage

    private fun setup(testFile: String) {
        val fileInfo = mutableListOf(FileInfo(testFileName, testFile))
        transformPackage = transformPackageBuilder.build(fileInfo)
    }

    private fun createParamsString(params: List<Any?>?, firstParam: Boolean): String {
        return (if (params == null) {
            ""
        } else ((if (firstParam) {
            ""
        } else ", ") + params.joinToString(", ", transform = ::createParamString)))
    }

    private fun createParamString(param: Any?): String {
        return when (param) {
            (param == null) -> "null"
            is Int -> param.toString()
            is JsonNode -> param.toPrettyString()
            is Boolean -> param.toString()
            else -> "\"$param\""
        }
    }


    @ParameterizedTest
    @MethodSource("testFunctions")
    fun inputTransformPackageWithTest_RunTest_AssertTestResultExists(func: String) {
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   $func
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val testResult = testPackage.runTest(testFileName, "test")
        assertFalse(testResult.testResults.isEmpty())
        assertTrue {
            testResult.testResults.any {
                it.testName == testFileFunction
            }
        }
    }

    @ParameterizedTest
    @MethodSource("testFunctions")
    fun inputTransformPackageWithTest_RunTest_AssertTestResultEqualsExpected(func: String, expectResult: Boolean) {
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   $func
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val testResult = testPackage.runTest(testFileName, "test")
        assertFalse(testResult.testResults.isEmpty())
        assertTrue {
            testResult.testResults.all {
                it.success == expectResult
            }
        }
    }

    @Test
    fun inputTransformPackageWithTest_RunFailedTest_InputErrorMessageInError() {
        val errorMsg = "This was a failure"
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   @.Assert.Equal(1, 2, "$errorMsg")
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val result = testPackage.runTest(testFileName, "test")
        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first {
            it.testName == testFileFunction
        }
        assertContains(testResult.message ?: "", errorMsg)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "Test.Function",
        "test.function",
        "Test.function",
        "test.Function",
        "Test.FUNCTION",
        "test.FUNCTION",
        "Test.function1",
        "test.function1",
        "Test.FUNCTION1",
        "test.FUNCTION1",
        "Test.function_1",
        "test.function_1",
        "Test.FUNCTION_1",
        "test.FUNCTION_1",
        "Test.Function#1",
        "test.function#2"
    ])
    fun inputTransformPackageWithMock_MockFunction_VerifyFunctionNamePasses(funcName : String) {
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   @.Mock.func("$funcName")
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val result = testPackage.runTest(testFileName, "test")
        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first {
            it.testName == testFileFunction
        }
        assertTrue(testResult.success, testResult.message)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "Test1.Function",
        "test1.function",
        "Test1.function",
        "test1.Function",
        "Test_1.function",
        "test_1.function",
        "Test_1.function",
        "test_1.Function",
        "Test_1.h@!!0",
        "test.hello.there",
        "Test.h@llo",
        "test_hell0"
    ])
    fun inputTransformPackageWithMock_MockFunction_VerifyIncorrectFunctionNameFails(funcName : String) {
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   @.Mock.func("$funcName")
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val result = testPackage.runTest(testFileName, "test")
        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first {
            it.testName == testFileFunction
        }
        assertFalse(testResult.success)
    }

    @ParameterizedTest
    @MethodSource("testMockFunctions")
    fun inputTransformPackageWithTestAndMock_RunTestWithMock_AssertMockReturnsInputValue(
        functionName: String,
        expectedReturnValue: Any?,
        expectResult: Boolean,
        matchingParams: List<Any?>? = null,
        inputParams: List<Any?>? = null
    ) {
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   @.Mock.func("$functionName", ${createParamString(expectedReturnValue)}${
            createParamsString(
                matchingParams, false
            )
        })
        |   ${"$"}value = @.$functionName(${createParamsString(inputParams, true)})
        |   @.Assert.equal(${createParamString(expectedReturnValue)}, ${"$"}value)
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val result = testPackage.runTest(testFileName, "test")
        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first {
            it.testName == testFileFunction
        }
        assertEquals(expectResult, testResult.success, testResult.message)
    }

    @ParameterizedTest
    @MethodSource("testCaptureFunctions")
    fun inputTransformPackageWithTestAndMock_RunMock_VerifyParamCaptureIsReturned(
        inputParams: String, expectedCaptures: String, numberOfTimesToCallFunc: Int
    ) {
        val functionName = "Test.Function"
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   ${"$"}instanceId = @.Mock.func("$functionName")
        |   ${"@.$functionName($inputParams)".repeat(numberOfTimesToCallFunc)}
        |   ${"$"}value = @.Mock.GetFuncCaptures("$functionName", ${"$"}instanceId)
        |   @.Assert.equal($expectedCaptures, ${"$"}value)
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val result = testPackage.runTest(testFileName, "test")
        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first {
            it.testName == testFileFunction
        }
        assertTrue(testResult.success, testResult.message)
    }

    @Test
    fun runFuncMockWithDifferentParams_GetCapturesWithoutInstanceId_VerifyCorrectParamCaptureIsReturned() {
        val functionName = "Test.Function"
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   ${"$"}instanceId = @.Mock.func("$functionName", null, 1)
        |   @.$functionName(0)
        |   @.$functionName(1)
        |   ${"$"}value = @.Mock.GetFuncCaptures("$functionName", ${"$"}instanceId)
        |   @.Assert.equal([{ "0": 1 }], ${"$"}value)
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val result = testPackage.runTest(testFileName, "test")
        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first {
            it.testName == testFileFunction
        }
        assertTrue(testResult.success, testResult.message)
    }

    @Test
    fun runFuncMockWithDifferentParams_GetCapturesWithoutInstanceId_VerifyAllCapturesAreReturned() {
        val functionName = "Test.Function"
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   ${"$"}instanceId = @.Mock.func("$functionName", null, 1)
        |   @.$functionName(0)
        |   @.$functionName(1)
        |   ${"$"}value = @.Mock.GetFuncCaptures("$functionName")
        |   @.Assert.equal([{ "0": 1 }, {"0": 0 }], ${"$"}value)
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val result = testPackage.runTest(testFileName, "test")
        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first {
            it.testName == testFileFunction
        }
        assertTrue(testResult.success, testResult.message)
    }

    @Test
    fun runAnnotationMockWithDifferentParams_GetCapturesWithInstanceId_VerifyCorrectParamCaptureIsReturned() {
        val annotationName = "hello"
        val functionName = "greetings"
        val functionCallName = "This.$functionName"
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   ${"$"}instanceId = @.Mock.annotation("$annotationName", null, 1)
        |   @.${functionCallName}A(1)
        |   @.${functionCallName}B(1)
        |   ${"$"}value = @.Mock.GetAnnotationCaptures("$annotationName", ${"$"}instanceId)
        |   @.Assert.equal([{ "0": 1 }], ${"$"}value)
        |}
        |
        |@hello(0)
        |fun ${functionName}A(${"$"}input) {
        |   return "Hello"
        |}
        |
        |@hello(1)
        |fun ${functionName}B(${"$"}input) {
        |   return "Hello"
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val result = testPackage.runTest(testFileName, "test")
        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first {
            it.testName == testFileFunction
        }
        assertTrue(testResult.success, testResult.message)
    }

    @Test
    fun runAnnotationMockWithDifferentParams_GetCapturesWithoutInstanceId_VerifyAllCapturesAreReturned() {
        val annotationName = "hello"
        val functionName = "greetings"
        val functionCallName = "This.$functionName"
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   ${"$"}instanceId = @.Mock.annotation("$annotationName", null, 1)
        |   ${"$"}result1 = @.${functionCallName}1(1)
        |   ${"$"}result2 = @.${functionCallName}2(1)
        |   ${"$"}value = @.Mock.GetAnnotationCaptures("$annotationName")
        |   @.Assert.equal([{ "0": 1 }, {"0": 0 }], ${"$"}value)
        |}
        |
        |@$annotationName(0)
        |fun ${functionName}1(${"$"}input) {
        |   return "Hello"
        |}
        |
        |@$annotationName(1)
        |fun ${functionName}2(${"$"}input) {
        |   return "Hello"
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val result = testPackage.runTest(testFileName, "test")
        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first {
            it.testName == testFileFunction
        }
        assertTrue(testResult.success, testResult.message)
    }

    @Test
    fun runStatementFuncMockWithDifferentParams_GetCapturesWithInstanceId_VerifyCorrectParamCaptureIsReturned() {
        val functionName = "Test.Function"
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   ${"$"}instanceId = @.Mock.statementFunc("$functionName", null, 1)
        |   @.$functionName(0) {
        |     ${"$"}value = 1
        |   }
        |   @.$functionName(1) {
        |     ${"$"}value = 1
        |   }
        |   ${"$"}value = @.Mock.GetStatementFuncCaptures("$functionName", ${"$"}instanceId)
        |   @.Assert.equal([{ "0": 1 }], ${"$"}value)
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val result = testPackage.runTest(testFileName, "test")
        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first {
            it.testName == testFileFunction
        }
        assertTrue(testResult.success, testResult.message)
    }

    @Test
    fun runStatementFuncMockWithDifferentParams_GetCapturesWithoutInstanceId_VerifyAllCapturesAreReturned() {
        val functionName = "Test.Function"
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   ${"$"}instanceId = @.Mock.statementFunc("$functionName", null, 1)
        |   @.$functionName(0) {
        |     ${"$"}value = 1
        |   }
        |   @.$functionName(1) {
        |     ${"$"}value = 1
        |   }
        |   ${"$"}value = @.Mock.GetStatementFuncCaptures("$functionName")
        |   @.Assert.equal([{ "0": 1 }, {"0": 0 }], ${"$"}value)
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val result = testPackage.runTest(testFileName, "test")
        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first {
            it.testName == testFileFunction
        }
        assertTrue(testResult.success, testResult.message)
    }

    @Test
    fun indexedFuncMock_ReturnsDifferentValuesPerCall_Succeeds() {
        val functionName = "Test.Function"
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   @.Mock.func("$functionName#1", 5)
        |   @.Mock.func("$functionName#2", 3)
        |   @.Mock.func("$functionName#3", null)
        |   ${"$"}r1 = @.$functionName()
        |   ${"$"}r2 = @.$functionName()
        |   ${"$"}r3 = @.$functionName()
        |   @.Assert.equal(5, ${"$"}r1)
        |   @.Assert.equal(3, ${"$"}r2)
        |   @.Assert.isnull(${"$"}r3)
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val result = testPackage.runTest(testFileName, "test")
        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first { it.testName == testFileFunction }
        assertTrue(testResult.success, testResult.message)
    }

    @Test
    fun indexedFuncMock_Exhausted_FailsWithClearMessage() {
        val functionName = "Test.Function"
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   @.Mock.func("$functionName#1", 5)
        |   @.Mock.func("$functionName#2", 3)
        |   ${"$"}r1 = @.$functionName()
        |   ${"$"}r2 = @.$functionName()
        |   ${"$"}r3 = @.$functionName()
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val result = testPackage.runTest(testFileName, "test")
        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first { it.testName == testFileFunction }
        assertFalse(testResult.success)
        assertContains(testResult.message ?: "", "Mock exhausted")
    }

    @Test
    fun indexedAnnotationMock_AllowsMultipleCallsWhenDefined_Succeeds() {
        val annotationName = "seq"
        val functionName = "greetings"
        val functionCallName = "This.$functionName"
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   @.Mock.annotation("$annotationName#1", null)
        |   @.Mock.annotation("$annotationName#2", null)
        |   ${"$"}r1 = @.${functionCallName}A()
        |   ${"$"}r2 = @.${functionCallName}B()
        |   @.Assert.equal("Hello", ${"$"}r1)
        |   @.Assert.equal("Hello", ${"$"}r2)
        |}
        |
        |@$annotationName
        |fun ${functionName}A() {
        |   return "Hello"
        |}
        |
        |@$annotationName
        |fun ${functionName}B() {
        |   return "Hello"
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val result = testPackage.runTest(testFileName, "test")
        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first { it.testName == testFileFunction }
        assertTrue(testResult.success, testResult.message)
    }

    @Test
    fun indexedAnnotationMock_Exhausted_FailsWithClearMessage() {
        val annotationName = "seq"
        val functionName = "greetings"
        val functionCallName = "This.$functionName"
        val testFile = """
        |@test
        |fun ${testFileFunction}() {
        |   @.Mock.annotation("$annotationName#1", "first")
        |   ${"$"}r1 = @.${functionCallName}A()
        |   ${"$"}r2 = @.${functionCallName}B()
        |}
        |
        |@$annotationName
        |fun ${functionName}A() {
        |   return "Hello"
        |}
        |
        |@$annotationName
        |fun ${functionName}B() {
        |   return "Hello"
        |}
        """.trimMargin()
        setup(testFile)
        val testPackage = TransformTestPackage(transformPackage)
        val result = testPackage.runTest(testFileName, "test")
        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first { it.testName == testFileFunction }
        assertFalse(testResult.success)
        assertContains(testResult.message ?: "", "Mock exhausted")
    }
}