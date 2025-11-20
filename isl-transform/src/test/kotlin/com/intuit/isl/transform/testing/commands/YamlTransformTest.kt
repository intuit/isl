package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intuit.isl.transform.testing.datacontract.YamlTestSuite
import org.junit.jupiter.params.provider.Arguments
import org.junit.platform.commons.JUnitException
import java.io.File
import java.util.stream.Stream

abstract class YamlTransformTest(
    private val folderPath: String
) : BaseTransformTest() {
    companion object {

        fun createFile(filename: String, folderPath: String? = null): File {
            var baseUrl = "./src/test/resources"
            if (folderPath != null) {
                baseUrl = "$baseUrl/$folderPath"
            }
            return File("$baseUrl/$filename")
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

    /**
     * Method used for porting kotlin tests into yaml.
     */
    @Suppress("unused")
    protected fun printTestSuiteToYaml(fileName: String, testSuite: YamlTestSuite) {
        objectMapper.writeValue(createFile("$fileName.yaml", "tests/$folderPath"), testSuite)
    }

    private fun loadTestSuite(fileName: String): YamlTestSuite {
        val yamlFile = readResource("$fileName.yaml", "tests/$folderPath")
        return objectMapper.readValue(yamlFile)
    }

    protected fun createTests(fileName: String): Stream<Arguments> {
        println("Loading test file: $fileName")
        val testSuite = loadTestSuite(fileName)
        return testSuite.tests.map { t ->
            var scriptValue = t.script
            if (scriptValue == null) {
                if (testSuite.scripts == null) {
                    throw JUnitException("A script reference is needed, but no scripts declared.")
                }
                scriptValue = testSuite.scripts?.first { s ->
                    s.name == t.scriptReference
                }?.value

                if (scriptValue == null) {
                    throw JUnitException("A script reference is needed, but no scripts found.")
                }
            }
            Arguments.of(t.name, scriptValue, t.expected, t.inputs)
        }.stream()
    }
}