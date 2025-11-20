package com.intuit.isl.validation.testing

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intuit.isl.common.AsyncContextAwareExtensionMethod
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.ITransformResult
import com.intuit.isl.runtime.ITransformer
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import java.io.File
import kotlin.test.assertEquals

enum class AssertEqualityType {
    Json,
    Text
}

abstract class BaseTransformTest(val folderPath: String) {
    companion object {
        fun compareJsonResults(
            expectedJson: String,
            result: JsonNode?,
            assertEqualityType: AssertEqualityType = AssertEqualityType.Json
        ) {
            val mapper = ObjectMapper()
            // we need this so 60 does not get written as 6e1 :(
            mapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)

            println("Transformed:\n${mapper.writeValueAsString(result)}")

            val ts = result?.toString()

            when (assertEqualityType) {
                // Json equality does not care about format, spacing and field ordering
                AssertEqualityType.Json -> assertEquals(mapper.readTree(expectedJson), mapper.readTree(ts))
                // Text equality checks that every single character matches
                AssertEqualityType.Text -> assertEquals(expectedJson, ts)
            }
        }

        fun logInfo(context: FunctionExecuteContext): Any? {
            val first = context.firstParameter
            val stringValue = ConvertUtils.tryToString(first)

            val args = context.parameters.drop(1).map { ConvertUtils.tryToString(it) };
            println(stringValue!!.replace("\\n", "\n") + args.joinToString { "," })

            return null
        }

        fun readResource(filename: String): String {
            return File("./src/test/resources/$filename").readText()
        }

        fun readResource(filename: String, folderPath: String? = null): String {
            var baseUrl = "./src/test/resources"
            if (folderPath != null) {
                baseUrl = "$baseUrl/$folderPath"
            }
            return File("$baseUrl/$filename").readText()
        }
    }

    private var objectMapper = ObjectMapper(
        YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE).enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
    )

    init {
        objectMapper.findAndRegisterModules().registerModule(
            KotlinModule.Builder().enable(KotlinFeature.NullIsSameAsDefault).enable(KotlinFeature.NullToEmptyMap)
                .enable(KotlinFeature.NullToEmptyCollection).build()
        )
    }

    fun loadTestSuite(fileName: String): YamlSchemaTestSuite {
        val yamlFile = readResource("$fileName.yaml", "tests/$folderPath")
        return objectMapper.readValue(yamlFile)
    }


    private suspend fun runTransform(
        map: Map<String, Any?>?,
        extensions: Map<String, AsyncContextAwareExtensionMethod>?,
        t: ITransformer
    ): ITransformResult? {
        val context = OperationContext()
        map?.forEach {
            val key = if (it.key.startsWith("$")) it.key else "$${it.key}"
            context.setVariable(key, JsonConvert.convert(it.value))
        }

        // extension functions
        extensions?.forEach {
            context.registerExtensionMethod(it.key, it.value)
        }

        onRegisterExtensions(context)

        var transformResult: ITransformResult? = null
        transformResult = t.runTransformAsync("run", context)
        return transformResult
    }

    open fun onRegisterExtensions(context: OperationContext) {
        context.registerExtensionMethod("Log.Info", BaseTransformTest::logInfo)
    }
}


data class YamlSchemaTestSuite(
    var name: String,
    var tests: List<YamlSchemaTestEntry>
)


data class YamlSchemaTestEntry(
    var name: String,
    var schema: String?,
    var script: String?,
    var value: String?,
    var expected: JsonNode
)
