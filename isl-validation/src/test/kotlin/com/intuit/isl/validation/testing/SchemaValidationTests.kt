package com.intuit.isl.validation.testing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.dsl.node
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.validation.IslSchema
import com.intuit.isl.validation.IslSchemaProcessingOptions
import com.intuit.isl.validation.IslSchemaProcessor
import com.intuit.isl.validation.SchemaKeywordProcessor
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("unused", "UNUSED_PARAMETER")
class SchemaValidationTests : BaseTransformTest("schema") {
    private fun schemaTests(): Stream<Arguments> {
        return createTests("schema.tests")
    }
    private fun customProcessingTests(): Stream<Arguments> {
        return createTests("customprocessing.tests")
    }

    @ParameterizedTest
    @MethodSource(
        "schemaTests",
        "customProcessingTests"
    )
    fun runFixtures(testName: String, schemaName: String, value: String, expectedResult: JsonNode) {
        return runBlocking {
            val jsonValue = JsonConvert.mapper.readTree(value)

            val tokenize: SchemaKeywordProcessor = { node ->
                // we can either modify the node or return a validation error
                val newNode = JsonNodeFactory.instance.textNode(
                    node.textValue() + "-TOKENIZED"
                )
                println("Tokenize ${node} > ${newNode}")
                Pair(newNode, null)
            };

            val options = IslSchemaProcessingOptions(
                keywordProcessors = mapOf(
                    "x-tokenize" to tokenize
                )
            )

            val schemaProcessor = IslSchemaProcessor(options) { schemaName, schemaUrl ->
                // we'll find the schemas on the disk
                val withoutPrefix = (schemaName ?: schemaUrl ?: "").replace("https://basicschema/schemas/", "");
                val schemaContents = readResource("tests/schema/schemas/$withoutPrefix");
                return@IslSchemaProcessor IslSchema(schemaContents)
            };

         val processOptions = node { put("schemaName", schemaName) }.node
            val result =
                schemaProcessor.processObject(jsonValue, processOptions)

            println("Final Object: $jsonValue")

            compareJsonResults(expectedResult.toString(), result, AssertEqualityType.Json)
        }
    }

    private fun createTests(fileName: String): Stream<Arguments> {
        println("Loading test file: $fileName")
        val testSuite = loadTestSuite(fileName)
        return testSuite.tests.map { t ->
//            val schema = readResource(t.schema!!, "tests/$folderPath");
            val value = if (t.value!!.endsWith(".json"))
                readResource(t.value!!, "tests/$folderPath")
            else
                t.value;
            Arguments.of(t.name, t.schema, value, t.expected)
        }.stream()
    }
}