package com.intuit.isl.cmd

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intuit.isl.runtime.FileInfo
import com.intuit.isl.runtime.TransformCompilationException
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.runtime.TransformPackage
import com.intuit.isl.runtime.TransformPackageBuilder
import com.intuit.isl.debug.IExecutionHook
import com.intuit.isl.test.TestExitException
import com.intuit.isl.test.TestFailException
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.common.TransformVariable
import com.intuit.isl.test.TestOperationContext
import com.intuit.isl.test.annotations.ComparisonDiff
import com.intuit.isl.test.annotations.TestResult
import com.intuit.isl.test.annotations.TestResultContext
import com.intuit.isl.test.mocks.MockFunction
import com.intuit.isl.utils.JsonConvert
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/** Empty = run all tests; non-empty = run only tests whose functionName (or "suiteFile:functionName") matches. */
typealias FunctionFilter = Set<String>

/** Result of preparing a *.tests.yaml case for the ISL debug adapter (mocks + input applied). */
data class YamlTestDebugPrepared(
    val transformPackage: TransformPackage,
    val moduleName: String,
    val functionName: String,
    /** [TestOperationContext] with YAML suite mocks and input applied. */
    val operationContext: IOperationContext,
    /** Absolute path to the ISL file (for launch config / breakpoints). */
    val scriptPath: String
)

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
     * Build package and [TestOperationContext] (mockSource, inline mocks, YAML input) for one suite entry.
     * Used by the debug adapter so YAML tests debug with the same infrastructure as [runSuite].
     */
    fun prepareYamlTestForDebug(
        yamlPath: Path,
        testIndex: Int,
        contextCustomizers: List<(com.intuit.isl.common.IOperationContext) -> Unit> = emptyList()
    ): YamlTestDebugPrepared {
        val basePath = yamlPath.parent
        val suite = parseSuite(yamlPath.toFile().readText())
        val setup = suite.setup
        val islPath = basePath.resolve(setup.islSource).normalize()
        val islFile = islPath.toFile()
        require(islFile.exists() && islFile.isFile) {
            "ISL file not found: $islPath (islSource: ${setup.islSource})"
        }
        val entries = suite.entries
        require(testIndex in entries.indices) {
            "Test index out of range: $testIndex (suite has ${entries.size} entries)"
        }
        val entry = entries[testIndex]

        val islContent = islFile.readText()
        val effectiveBasePath = islPath.parent
        val moduleName = islPath.fileName.toString()
        val fileInfos = mutableListOf(FileInfo(moduleName, islContent))
        val resolvedPaths = mutableMapOf<String, Path>()
        resolvedPaths[moduleName] = islPath.toAbsolutePath().normalize()
        val findExternalModule = IslModuleResolver.createModuleFinder(effectiveBasePath, resolvedPaths)
        val transformPackage = TransformPackageBuilder().build(fileInfos, findExternalModule)

        val context = buildOperationContextForYamlEntry(
            transformPackage = transformPackage,
            moduleName = moduleName,
            entry = entry,
            setup = setup,
            basePath = effectiveBasePath,
            yamlBasePath = basePath,
            yamlPath = yamlPath,
            contextCustomizers = contextCustomizers,
            verbose = true,
            printMockSummary = true
        )

        return YamlTestDebugPrepared(
            transformPackage = transformPackage,
            moduleName = moduleName,
            functionName = entry.functionName,
            operationContext = context,
            scriptPath = islPath.toAbsolutePath().normalize().toString()
        )
    }

    /**
     * Runs a single *.tests.yaml suite and appends results to [resultContext].
     * [yamlPath] is the path to the .tests.yaml file; [basePath] is the directory containing it (used to resolve islSource, which is relative to the test file). The effective base for the test run is the directory of the resolved ISL file, so paths in logs and @.Load.From are relative to the script under test.
     * When [functionFilter] is non-empty, only test entries whose functionName matches (or "suiteFile:functionName") are run.
     */
    fun runSuite(
        yamlPath: Path,
        basePath: Path,
        resultContext: TestResultContext,
        contextCustomizers: List<(com.intuit.isl.common.IOperationContext) -> Unit>,
        functionFilter: FunctionFilter = emptySet(),
        verbose: Boolean = false,
        executionHook: IExecutionHook? = null,
        onTransformPackageBuilt: ((TransformPackage) -> Unit)? = null
    ) {
        val suite = parseSuite(yamlPath.toFile().readText())
        val setup = suite.setup
        // islSource is relative to the test file (basePath); effective base is the directory of the resolved ISL file
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
        val effectiveBasePath = islPath.parent
        val moduleName = islPath.fileName.toString()
        val fileInfos = mutableListOf(FileInfo(moduleName, islContent))
        val resolvedPaths = mutableMapOf<String, Path>()
        resolvedPaths[moduleName] = islPath.toAbsolutePath().normalize()
        val findExternalModule = IslModuleResolver.createModuleFinder(effectiveBasePath, resolvedPaths)
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

        onTransformPackageBuilt?.invoke(transformPackage)

        val groupName = suite.category ?: yamlPath.nameWithoutExtension
        val suiteFileName = yamlPath.fileName.toString()

        val testsToRun = if (functionFilter.isEmpty()) suite.entries else {
            suite.entries.filter { entry ->
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

        val opts = suite.assertOptions ?: AssertOptions()
        testsToRun.forEachIndexed { index, entry ->
            val testResult = runOneTest(
                transformPackage = transformPackage,
                moduleName = moduleName,
                entry = entry,
                setup = setup,
                basePath = effectiveBasePath,
                yamlBasePath = basePath,
                groupName = groupName,
                yamlPath = yamlPath,
                contextCustomizers = contextCustomizers,
                assertOptions = opts,
                printMockSummary = verbose && (index == 0),
                verbose = verbose,
                executionHook = executionHook
            )
            resultContext.testResults.add(testResult)
        }
    }

    private fun buildOperationContextForYamlEntry(
        transformPackage: TransformPackage,
        moduleName: String,
        entry: YamlUnitTestEntry,
        setup: YamlTestSetup,
        basePath: Path,
        yamlBasePath: Path,
        yamlPath: Path,
        contextCustomizers: List<(com.intuit.isl.common.IOperationContext) -> Unit>,
        verbose: Boolean,
        printMockSummary: Boolean
    ): TestOperationContext {
        val testFileName = yamlBasePath.relativize(yamlPath).toString().replace("\\", "/")
        val context = TestOperationContext.create(
            testResultContext = TestResultContext(),
            currentFile = moduleName,
            basePath = basePath,
            mockFileName = setup.mockSourceDisplayName(),
            testFileName = testFileName,
            verboseLogging = verbose,
            contextCustomizers = contextCustomizers
        )

        // 1. Load mockSource file(s) in order; paths are relative to the .tests.yaml file (yamlBasePath)
        for (mockFileEntry in setup.mockSourceFiles()) {
            val mockPath = yamlBasePath.resolve(mockFileEntry).normalize()
            val mockFile = mockPath.toFile()
            if (mockFile.exists() && mockFile.isFile) {
                val ext = mockFile.extension.lowercase()
                val root = when (ext) {
                    "json" -> JsonConvert.mapper.readTree(mockFile)
                    "yaml", "yml" -> com.fasterxml.jackson.databind.ObjectMapper(YAMLFactory()).readTree(mockFile)
                    else -> throw IllegalArgumentException("Mock file must be .json, .yaml, .yml; got: $mockFileEntry")
                }
                if (root.isObject) MockFunction.applyMocksFromNode(context, root as ObjectNode, mockFileEntry)
            }
        }
        // 2. Apply inline mocks after mockSource (all mocks are additive; params differentiate)
        setup.mocksAsObject()?.let { MockFunction.applyMocksFromNode(context, it, testFileName) }

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
        return context
    }

    private fun runOneTest(
        transformPackage: TransformPackage,
        moduleName: String,
        entry: YamlUnitTestEntry,
        setup: YamlTestSetup,
        basePath: Path,
        yamlBasePath: Path,
        groupName: String,
        yamlPath: Path,
        contextCustomizers: List<(com.intuit.isl.common.IOperationContext) -> Unit>,
        assertOptions: AssertOptions = AssertOptions(),
        printMockSummary: Boolean = false,
        verbose: Boolean = false,
        executionHook: IExecutionHook? = null
    ): TestResult {
        val context = buildOperationContextForYamlEntry(
            transformPackage = transformPackage,
            moduleName = moduleName,
            entry = entry,
            setup = setup,
            basePath = basePath,
            yamlBasePath = yamlBasePath,
            yamlPath = yamlPath,
            contextCustomizers = contextCustomizers,
            verbose = verbose,
            printMockSummary = printMockSummary
        )

        try {
            // 4. Run the function (or capture result from @.Test.Exit / @.Test.Fail)
            if (verbose) println("[ISL Mock] Running ${entry.functionName}")
            val fullName = TransformPackage.toFullFunctionName(moduleName, entry.functionName)
            val result = try {
                transformPackage.runTransformNew(fullName, context, executionHook)
            } catch (e: TestFailException) {
                // Test.Fail — skip comparison, return failure immediately
                return makeFailResult(yamlPath, entry, groupName, e)
            } catch (e: TestExitException) {
                resolveTestExitResult(e)
            } catch (e: TransformException) {
                val testFail = findCause<TestFailException>(e)
                if (testFail != null) return makeFailResult(yamlPath, entry, groupName, testFail)
                val testExit = findCause<TestExitException>(e)
                if (testExit != null) resolveTestExitResult(testExit) else throw e
            }

            // 5. Compare with expected (deep compare; on failure report exact field diffs)
            val expected = entry.expected
            val opts = entry.assertOptions ?: assertOptions
            val ignorePaths = (entry.ignore.orEmpty()).map { normalizeComparePath(it) }.toSet()
            val (success, diffs) = if (expected == null) {
                (result == null) to emptyList<JsonDiff>()
            } else {
                jsonDeepCompare(expected, result, assertOptions = opts, ignorePaths = ignorePaths)
            }
            val message = if (!success && expected != null) {
                buildComparisonFailureMessage(expected, result, diffs, entry.ignore.orEmpty())
            } else null
            val expectedJson = if (!success && expected != null) JsonConvert.mapper.writeValueAsString(expected) else null
            val actualJson = if (!success) (result?.let { JsonConvert.mapper.writeValueAsString(it) } ?: "null") else null
            val comparisonDiffs = if (!success && diffs.isNotEmpty()) diffs.map { ComparisonDiff(it.path, it.expectedValue, it.actualValue) } else null

            return TestResult(
                testFile = yamlPath.toString(),
                functionName = entry.functionName,
                testName = entry.name,
                testGroup = groupName,
                success = success,
                message = message,
                expectedJson = expectedJson,
                actualJson = actualJson,
                comparisonDiffs = comparisonDiffs
            )
        } catch (e: Exception) {
            val (msg, pos) = when (e) {
                is TransformCompilationException -> e.message to e.position
                is TransformException -> e.message to e.position
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
                errorPosition = pos,
                exception = e
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
            val varName = if (paramNames.first().startsWith("$")) paramNames.first() else "$" + paramNames.first()
            context.setVariable(
                varName,
                TransformVariable(JsonConvert.convert(input), readOnly = false, global = true)
            )
            return
        }
        if (input is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val map = input as Map<String, Any?>
            for ((key, value) in map) {
                val varName = if (key.startsWith("$")) key else "$" + key
                context.setVariable(
                    varName,
                    TransformVariable(JsonConvert.convert(value), readOnly = false, global = true)
                )
            }
        }
    }

    /** Path + expected/actual value at a difference; path uses $root.field.[0].key format. */
    private data class JsonDiff(val path: String, val expectedValue: String, val actualValue: String)

    /**
     * Normalizes a user-facing JSON path to the format used during comparison ($.key.[0].field).
     * User may write "providerResponses.items[0].uid" or "providerResponses.error.detail".
     */
    private fun normalizeComparePath(userPath: String): String {
        val t = userPath.trim()
        if (t.isEmpty()) return "$"
        val withRoot = if (t.startsWith("$")) t else "$.$t"
        return withRoot.replace(Regex("(?<!\\.)\\["), ".[")
    }

    /**
     * Deep-compares expected and actual JSON; returns (true, empty) if equal,
     * (false, non-empty list of diffs) with path and values at each difference.
     * [assertOptions] controls lenient matching (null/missing/empty array, extra fields).
     * [ignorePaths] exact paths to treat as equal (e.g. ["$.providerResponses.error.detail"]).
     */
    private fun jsonDeepCompare(
        expected: JsonNode,
        actual: JsonNode?,
        path: String = "$",
        assertOptions: AssertOptions = AssertOptions(),
        ignorePaths: Set<String> = emptySet()
    ): Pair<Boolean, List<JsonDiff>> {
        if (path in ignorePaths) return true to emptyList()
        // actual is null (missing or literal null)
        if (actual == null) {
            if (expected.isNull) return true to emptyList()
            if (assertOptions.nullSameAsEmptyArray && expected.isArray && expected.size() == 0) return true to emptyList()
            return false to listOf(JsonDiff(path, formatJsonValue(expected), "null"))
        }
        // expected is null
        if (expected.isNull) {
            if (actual.isNull) return true to emptyList()
            if (assertOptions.nullSameAsEmptyArray && actual.isArray && actual.size() == 0) return true to emptyList()
            return false to listOf(JsonDiff(path, "null", formatJsonValue(actual)))
        }
        if (expected.isNumber && actual.isNumber) {
            val eq = if (assertOptions.numbersEqualIgnoreFormat) {
                expected.decimalValue().compareTo(actual.decimalValue()) == 0
            } else {
                expected.decimalValue() == actual.decimalValue()
            }
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
                val allKeys = if (assertOptions.ignoreExtraFieldsInActual) {
                    expected.fieldNames().asSequence().toList()
                } else {
                    (expected.fieldNames().asSequence().toSet() + actual.fieldNames().asSequence().toSet()).toList()
                }
                val acc = mutableListOf<JsonDiff>()
                for (k in allKeys) {
                    val expectedChild = expected.get(k)
                    val actualChild = actual.get(k)
                    val subPath = if (path == "$") "$$k" else "$path.$k"
                    when {
                        expectedChild == null && actualChild == null -> {}
                        expectedChild == null -> {
                            // extra in actual (only when not ignoreExtraFieldsInActual)
                            if (assertOptions.missingSameAsEmptyArray && actualChild != null && actualChild.isArray && actualChild.size() == 0) {}
                            else acc.add(JsonDiff(subPath, "missing", formatJsonValue(actualChild!!)))
                        }
                        actualChild == null -> {
                            // missing in actual
                            if (assertOptions.nullSameAsMissing && expectedChild.isNull) {}
                            else if (assertOptions.missingSameAsEmptyArray && expectedChild.isArray && expectedChild.size() == 0) {}
                            else acc.add(JsonDiff(subPath, formatJsonValue(expectedChild), "missing"))
                        }
                        else -> {
                            val (ok, subDiffs) = jsonDeepCompare(expectedChild, actualChild, subPath, assertOptions, ignorePaths)
                            if (!ok) acc.addAll(subDiffs)
                        }
                    }
                }
                if (!assertOptions.ignoreExtraFieldsInActual && expected.size() != actual.size()) {
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
                    val (ok, subDiffs) = jsonDeepCompare(expected.get(i), actual.get(i), indexPath, assertOptions, ignorePaths)
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

    /**
     * Builds a failed [TestResult] for a [TestFailException].
     * Extracts a human-readable message from the result: plain string → used as-is,
     * object with a "message" field → that field's text, anything else → full JSON.
     */
    private fun makeFailResult(
        yamlPath: Path,
        entry: YamlUnitTestEntry,
        groupName: String,
        e: TestFailException
    ): TestResult {
        val msg = when (val r = e.result) {
            null -> "Test.Fail called with no message"
            else -> {
                val node = runCatching { JsonConvert.convert(r) }.getOrNull()
                when {
                    node == null -> r.toString()
                    node.isTextual -> node.textValue()
                    node.isObject && node.has("message") -> node.get("message").asText()
                    else -> JsonConvert.mapper.writeValueAsString(node)
                }
            }
        }
        return TestResult(
            testFile = yamlPath.toString(),
            functionName = entry.functionName,
            testName = entry.name,
            testGroup = groupName,
            success = false,
            message = "[Test.Fail] $msg",
            errorPosition = e.position
        )
    }

    /** Converts a [TestExitException] result to the JSON node used for assertion. */
    private fun resolveTestExitResult(e: TestExitException): JsonNode? {
        val r = e.result
        return when {
            r == null -> null
            r is JsonNode && r.isNull -> null
            else -> JsonConvert.convert(r)
        }
    }

    /** Walks the full [Throwable] cause chain and returns the first instance of [T], or null. */
    private inline fun <reified T : Throwable> findCause(e: Throwable): T? {
        var current: Throwable? = e
        while (current != null) {
            if (current is T) return current
            current = current.cause
        }
        return null
    }

    private fun formatJsonValue(node: JsonNode): String =
        JsonConvert.mapper.writeValueAsString(node)

    private fun buildComparisonFailureMessage(
        expected: JsonNode,
        actual: JsonNode?,
        diffs: List<JsonDiff>,
        ignoredPaths: List<String> = emptyList()
    ): String {
        val fullExpected = JsonConvert.mapper.writeValueAsString(expected)
        val fullActual = actual?.let { JsonConvert.mapper.writeValueAsString(it) } ?: "null"
        val header = "Expected: $fullExpected\nActual: $fullActual"
        if (diffs.isEmpty()) return header
        val diffLines = diffs.joinToString("\n") { d ->
            "Expected: ${d.path} = ${d.expectedValue}\nActual: ${d.path} = ${d.actualValue}\r\n"
        }
        val ignoredSection = if (ignoredPaths.isEmpty()) ""
        else "\n[ISL Assert] Ignored path(s):\n${ignoredPaths.joinToString("\n") { "  $it" }}\n"
        return "[ISL Assert] Result Differences:\n$header$ignoredSection\n[ISL Assert] Difference(s):\n$diffLines\n"
    }
}
