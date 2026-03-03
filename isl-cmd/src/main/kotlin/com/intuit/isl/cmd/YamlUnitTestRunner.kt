package com.intuit.isl.cmd

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intuit.isl.runtime.FileInfo
import com.intuit.isl.runtime.TransformCompilationException
import com.intuit.isl.runtime.TransformPackage
import com.intuit.isl.runtime.TransformPackageBuilder
import com.intuit.isl.test.TestOperationContext
import com.intuit.isl.test.annotations.TestResult
import com.intuit.isl.test.annotations.TestResultContext
import com.intuit.isl.test.mocks.MockFunction
import com.intuit.isl.utils.JsonConvert
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

object YamlUnitTestRunner {

    private val yamlMapper = com.fasterxml.jackson.databind.ObjectMapper(YAMLFactory()).apply {
        registerModule(
            kotlinModule {
                enable(KotlinFeature.NullIsSameAsDefault)
                enable(KotlinFeature.NullToEmptyMap)
                enable(KotlinFeature.NullToEmptyCollection)
            }
        )
    }

    fun parseSuite(yamlContent: String): YamlUnitTestSuite = yamlMapper.readValue(yamlContent)

    /**
     * Runs a single *.tests.yaml suite and appends results to [resultContext].
     * [yamlPath] is the path to the .tests.yaml file; [basePath] is the directory containing it (used for resolving islSource and mockSource).
     */
    fun runSuite(
        yamlPath: Path,
        basePath: Path,
        resultContext: TestResultContext,
        contextCustomizers: List<(com.intuit.isl.common.IOperationContext) -> Unit>
    ) {
        val suite = parseSuite(yamlPath.toFile().readText())
        val setup = suite.setup
        val islPath = basePath.resolve(setup.islSource).normalize()
        val islFile = islPath.toFile()
        if (!islFile.exists() || !islFile.isFile) {
            resultContext.testResults.add(
                TestResult(
                    testFile = yamlPath.toString(),
                    functionName = "setup",
                    testName = suite.category ?: yamlPath.nameWithoutExtension,
                    testGroup = suite.category ?: yamlPath.nameWithoutExtension,
                    success = false,
                    message = "ISL file not found: $islPath (islSource: ${setup.islSource})"
                )
            )
            return
        }
        val islContent = islFile.readText()
        val moduleName = basePath.relativize(islPath).toString().replace("\\", "/")
        val fileInfos = mutableListOf(FileInfo(moduleName, islContent))
        val findExternalModule = java.util.function.BiFunction<String, String, String> { from, dep ->
            IslModuleResolver.resolveExternalModule(basePath, from, dep)
                ?: throw TransformCompilationException(
                    "Could not find module '$dep' (imported from $from). Searched relative to $basePath"
                )
        }
        val transformPackage: TransformPackage = try {
            TransformPackageBuilder().build(fileInfos, findExternalModule)
        } catch (e: Exception) {
            resultContext.testResults.add(
                TestResult(
                    testFile = moduleName,
                    functionName = "compilation",
                    testName = suite.category ?: yamlPath.nameWithoutExtension,
                    testGroup = suite.category ?: yamlPath.nameWithoutExtension,
                    success = false,
                    message = (e as? TransformCompilationException)?.message ?: e.toString(),
                    errorPosition = (e as? TransformCompilationException)?.position
                )
            )
            return
        }

        val groupName = suite.category ?: yamlPath.nameWithoutExtension

        for (entry in suite.tests) {
            val testResult = runOneTest(
                transformPackage = transformPackage,
                moduleName = moduleName,
                entry = entry,
                setup = setup,
                basePath = basePath,
                groupName = groupName,
                yamlPath = yamlPath,
                contextCustomizers = contextCustomizers
            )
            resultContext.testResults.add(testResult)
        }
    }

    private fun runOneTest(
        transformPackage: TransformPackage,
        moduleName: String,
        entry: YamlUnitTestEntry,
        setup: YamlTestSetup,
        basePath: Path,
        groupName: String,
        yamlPath: Path,
        contextCustomizers: List<(com.intuit.isl.common.IOperationContext) -> Unit>
    ): TestResult {
        val context = TestOperationContext.create(
            testResultContext = TestResultContext(),
            currentFile = moduleName,
            basePath = basePath,
            mockFileName = setup.mockSourceDisplayName(),
            contextCustomizers = contextCustomizers
        )

        try {
            // 1. Load mockSource file(s) in order (each can override the previous)
            for (mockFileName in setup.mockSourceFiles()) {
                val mockPath = basePath.resolve(mockFileName).normalize()
                val mockFile = mockPath.toFile()
                if (mockFile.exists() && mockFile.isFile) {
                    val ext = mockFile.extension.lowercase()
                    val root = when (ext) {
                        "json" -> JsonConvert.mapper.readTree(mockFile)
                        "yaml", "yml" -> com.fasterxml.jackson.databind.ObjectMapper(YAMLFactory()).readTree(mockFile)
                        else -> throw IllegalArgumentException("Mock file must be .json, .yaml, .yml; got: $mockFileName")
                    }
                    if (root.isObject) MockFunction.applyMocksFromNode(context, root as ObjectNode)
                }
            }
            // 2. Apply inline mocks (override mockSource)
            setup.mocksAsObject()?.let { MockFunction.applyMocksFromNode(context, it) }

            // 3. Set input variables (param names from function, or from input map keys)
            val paramNames = getFunctionParamNames(transformPackage, moduleName, entry.functionName)
            setInputVariables(context, entry.input, paramNames)

            // 4. Run the function
            val fullName = TransformPackage.toFullFunctionName(moduleName, entry.functionName)
            val result = transformPackage.runTransformNew(fullName, context)

            // 5. Compare with expected
            val expected = entry.expected
            val success = if (expected == null) result == null else jsonEquals(expected, result)
            val message = if (!success && expected != null) {
                "Expected: ${JsonConvert.mapper.writeValueAsString(expected)}\nActual: ${JsonConvert.mapper.writeValueAsString(result)}"
            } else null

            return TestResult(
                testFile = yamlPath.toString(),
                functionName = entry.functionName,
                testName = entry.name,
                testGroup = groupName,
                success = success,
                message = message
            )
        } catch (e: Exception) {
            val (msg, pos) = when (e) {
                is TransformCompilationException -> e.message to e.position
                is com.intuit.isl.runtime.TransformException -> e.message to e.position
                is com.intuit.isl.runtime.IslException -> e.message to e.position
                else -> e.message to null
            }
            return TestResult(
                testFile = yamlPath.toString(),
                functionName = entry.functionName,
                testName = entry.name,
                testGroup = groupName,
                success = false,
                message = msg ?: e.toString(),
                errorPosition = pos
            )
        }
    }

    private fun getFunctionParamNames(pkg: TransformPackage, moduleName: String, functionName: String): List<String> {
        val module = pkg.getModule(moduleName)?.module ?: return emptyList()
        val func = module.getFunction(functionName) ?: return emptyList()
        return func.token.arguments.map { it.name }
    }

    private fun setInputVariables(context: TestOperationContext, input: Any?, paramNames: List<String>) {
        if (input == null) return
        if (paramNames.size == 1) {
            val varName = if (paramNames.first().startsWith("$")) paramNames.first() else "$${paramNames.first()}"
            context.setVariable(varName, JsonConvert.convert(input))
            return
        }
        if (input is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val map = input as Map<String, Any?>
            for ((key, value) in map) {
                val varName = if (key.startsWith("$")) key else "$$key"
                context.setVariable(varName, JsonConvert.convert(value))
            }
        }
    }

    private fun jsonEquals(expected: JsonNode, actual: JsonNode?): Boolean {
        if (actual == null) return expected.isNull
        if (expected.isNumber && actual.isNumber) return expected.decimalValue() == actual.decimalValue()
        if (expected.isNull && actual.isNull) return true
        if (expected.nodeType != actual.nodeType) return false
        when {
            expected.isObject -> {
                if (!actual.isObject || expected.size() != actual.size()) return false
                return expected.fields().asSequence().all { (k, v) -> jsonEquals(v, actual.get(k)) }
            }
            expected.isArray -> {
                if (!actual.isArray || expected.size() != actual.size()) return false
                return (0 until expected.size()).all { i -> jsonEquals(expected.get(i), actual.get(i)) }
            }
            else -> return expected.equals(actual)
        }
    }
}
