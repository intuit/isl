package com.intuit.isl.validation.testing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.intuit.isl.common.OperationContext
import com.intuit.isl.dsl.node
import com.intuit.isl.runtime.TransformCompiler
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.validation.IslSchema
import com.intuit.isl.validation.IslValidationException
import com.intuit.isl.validation.ValidateTypeModifier
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.FileNotFoundException
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("unused", "UNUSED_PARAMETER")
class IslTypeValidationTests : BaseTransformTest("schema") {
    private fun scriptWithModifier(): Stream<Arguments> {
        return createTests("isl.type.tests")
    }

    @Suppress("UNCHECKED_CAST")
    @ParameterizedTest
    @MethodSource(
        "scriptWithModifier"
    )
    fun runFixtures(testName: String, script: String, expectedResult: JsonNode) {
        runBlocking {
            val context = OperationContext();
            ValidateTypeModifier().registerExtensions(context, true) { schemaName, schemaUrl ->
                try {
                    val schemaContents = readResource("tests/schema/schemas/$schemaName.yaml");
                    return@registerExtensions IslSchema(schemaContents);
                } catch (e: FileNotFoundException) {
                    return@registerExtensions null;
                }
            }

            try {
                val t = TransformCompiler().compileIsl("test", script)
                val result = t.runTransformAsync("run", context)
                // this is running default validations when using | validate.schema
                compareJsonResults(expectedResult.toString(), result.result, AssertEqualityType.Json)
            } catch (e: TransformException) {
                // we might have others but we don't bother in these unit tests
                val validation = e.cause as IslValidationException;
                val result = JsonConvert.convert(node {
                    put("message", validation.message)
                    array("issues", JsonConvert.convert(validation.issues) as ArrayNode)
                }.node);
                compareJsonResults(expectedResult.toString(), result, AssertEqualityType.Json)
            }
        }
    }

    private fun createTests(fileName: String): Stream<Arguments> {
        println("Loading test file: $fileName")
        val testSuite = loadTestSuite(fileName)
        return testSuite.tests.map { t ->
            Arguments.of(fileName + ":" + t.name, t.script, t.expected)
        }.stream()
    }
}