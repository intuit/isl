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

/** Empty = run all tests; non-empty = run only tests whose functionName (or "suiteFile:functionName") matches. */
typealias FunctionFilter = Set<String>

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
     * When [functionFilter] is non-empty, only test entries whose functionName matches (or "suiteFile:functionName") are run.
     */
    fun runSuite(
        yamlPath: Path,
        basePath: Path,
        resultContext: TestResultContext,
        contextCustomizers: List<(com.intuit.isl.common.IOperationContext) -> Unit>,
        functionFilter: FunctionFilter = emptySet()
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
        val resolvedPaths = mutableMapOf<String, Path>()
        resolvedPaths[moduleName] = islPath.toAbsolutePath().normalize()
        val findExternalModule = IslModuleResolver.createModuleFinder(basePath, resolvedPaths)
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
        val suiteFileName = yamlPath.fileName.toString()

        val testsToRun = if (functionFilter.isEmpty()) suite.tests else {
            suite.tests.filter { entry ->
                functionFilter.any { filter ->
                    when {
                        filter.contains(":") -> {
                            val parts = filter.split(":", limit = 2)
                            val fileMatch = parts[0].equals(suiteFileName, true) ||
                                parts[0].equals(yamlPath.nameWithoutExtension, true)
                            fileMatch && parts[1].equals(entry.functionName, true)
                        }
                        else -> filter.equals(entry.functionName, true)
                    }
                }
            }
        }

        testsToRun.forEachIndexed { index, entry ->
            val testResult = runOneTest(
                transformPackage = transformPackage,
                moduleName = moduleName,
                entry = entry,
                setup = setup,
                basePath = basePath,
                groupName = groupName,
                yamlPath = yamlPath,
                contextCustomizers = contextCustomizers,
                printMockSummary = (index == 0)
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
        contextCustomizers: List<(com.intuit.isl.common.IOperationContext) -> Unit>,
        printMockSummary: Boolean = false
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
            // 2. Apply inline mocks after mockSource (all mocks are additive; params differentiate)
            setup.mocksAsObject()?.let { MockFunction.applyMocksFromNode(context, it) }

            if (printMockSummary) {
                val names = (context.mockExtensions.mockExtensions.keys +
                    context.mockExtensions.mockAnnotations.keys +
                    context.mockExtensions.mockStatementExtensions.keys).sorted()
                val n = names.size
                if (n > 0) println("[ISL Mock] Mocked M $n function(s): ${names.joinToString(", ")}")
            }

            // 3. Set input variables (param names from function, or from input map keys)
            val paramNames = getFunctionParamNames(transformPackage, moduleName, entry.functionName)
            setInputVariables(context, entry.input, paramNames)

            // 4. Run the function
            println("[ISL Mock] Running ${entry.functionName}")
            val fullName = TransformPackage.toFullFunctionName(moduleName, entry.functionName)
            val result = transformPackage.runTransformNew(fullName, context)

            // 5. Compare with expected (deep compare; on failure report exact field diffs)
            val expected = entry.expected
            val (success, diffs) = if (expected == null) {
                (result == null) to emptyList<JsonDiff>()
            } else {
                jsonDeepCompare(expected, result)
            }
            val message = if (!success && expected != null) {
                buildComparisonFailureMessage(expected, result, diffs)
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

    /** Path + expected/actual value at a difference; path uses $root.field.[0].key format. */
    private data class JsonDiff(val path: String, val expectedValue: String, val actualValue: String)

    /**
     * Deep-compares expected and actual JSON; returns (true, empty) if equal,
     * (false, non-empty list of diffs) with path and values at each difference.
     */
    private fun jsonDeepCompare(expected: JsonNode, actual: JsonNode?, path: String = "$"): Pair<Boolean, List<JsonDiff>> {
        if (actual == null) {
            return if (expected.isNull) true to emptyList()
            else false to listOf(JsonDiff(path, formatJsonValue(expected), "null"))
        }
        if (expected.isNull) {
            return if (actual.isNull) true to emptyList()
            else false to listOf(JsonDiff(path, "null", formatJsonValue(actual)))
        }
        if (expected.isNumber && actual.isNumber) {
            val eq = expected.decimalValue() == actual.decimalValue()
            return if (eq) true to emptyList()
            else false to listOf(JsonDiff(path, formatJsonValue(expected), formatJsonValue(actual)))
        }
        if (expected.nodeType != actual.nodeType) {
            return false to listOf(JsonDiff(path, formatJsonValue(expected), formatJsonValue(actual)))
        }
        when {
            expected.isObject -> {
                if (!actual.isObject) {
                    return false to listOf(JsonDiff(path, formatJsonValue(expected), formatJsonValue(actual)))
                }
                val allKeys = (expected.fieldNames().asSequence().toSet() + actual.fieldNames().asSequence().toSet()).toList()
                val acc = mutableListOf<JsonDiff>()
                for (k in allKeys) {
                    val expectedChild = expected.get(k)
                    val actualChild = actual.get(k)
                    val subPath = if (path == "$") "$$k" else "$path.$k"
                    when {
                        expectedChild == null && actualChild == null -> {}
                        expectedChild == null -> acc.add(JsonDiff(subPath, "missing", formatJsonValue(actualChild)))
                        actualChild == null -> acc.add(JsonDiff(subPath, formatJsonValue(expectedChild), "missing"))
                        else -> {
                            val (ok, subDiffs) = jsonDeepCompare(expectedChild, actualChild, subPath)
                            if (!ok) acc.addAll(subDiffs)
                        }
                    }
                }
                if (expected.size() != actual.size()) {
                    acc.add(JsonDiff(path, "object size ${expected.size()}", "object size ${actual.size()}"))
                }
                return (acc.isEmpty()) to acc
            }
            expected.isArray -> {
                if (!actual.isArray) {
                    return false to listOf(JsonDiff(path, formatJsonValue(expected), formatJsonValue(actual)))
                }
                val acc = mutableListOf<JsonDiff>()
                val size = minOf(expected.size(), actual.size())
                for (i in 0 until size) {
                    val indexPath = if (path == "$") "$[$i]" else "$path.[$i]"
                    val (ok, subDiffs) = jsonDeepCompare(expected.get(i), actual.get(i), indexPath)
                    if (!ok) acc.addAll(subDiffs)
                }
                if (expected.size() != actual.size()) {
                    acc.add(JsonDiff(path, "array size ${expected.size()}", "array size ${actual.size()}"))
                }
                return (acc.isEmpty()) to acc
            }
            else -> {
                val eq = expected.equals(actual)
                return if (eq) true to emptyList()
                else false to listOf(JsonDiff(path, formatJsonValue(expected), formatJsonValue(actual)))
            }
        }
    }

    private fun formatJsonValue(node: JsonNode): String =
        JsonConvert.mapper.writeValueAsString(node)

    private fun buildComparisonFailureMessage(expected: JsonNode, actual: JsonNode?, diffs: List<JsonDiff>): String {
        val fullExpected = JsonConvert.mapper.writeValueAsString(expected)
        val fullActual = actual?.let { JsonConvert.mapper.writeValueAsString(it) } ?: "null"
        val header = "Expected: $fullExpected\nActual: $fullActual"
        if (diffs.isEmpty()) return header
        val diffLines = diffs.joinToString("\n") { d ->
            "Expected: ${d.path} = ${d.expectedValue}\nActual: ${d.path} = ${d.actualValue}\r\n"
        }
        return "[ISL Assert] Result Differences:\n$header\n\n[ISL Assert] Difference(s):\n$diffLines\n"
    }
}
