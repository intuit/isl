package com.intuit.isl.cmd

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intuit.isl.runtime.FileInfo
import com.intuit.isl.runtime.TransformCompilationException
import com.intuit.isl.test.TransformTestPackageBuilder
import com.intuit.isl.test.annotations.TestResult
import com.intuit.isl.test.annotations.TestResultContext
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.system.exitProcess

/**
 * Command to execute ISL tests.
 * Discovers and runs:
 * - .isl files containing @setup or @test annotations
 * - *.tests.yaml files (YAML-driven unit test suites with setup.islSource, mockSource, mocks, and tests with functionName, input, expected)
 */
@Command(
    name = "test",
    aliases = ["tests"],
    description = ["Execute ISL tests from the specified path or current folder. Runs .isl files with @setup/@test and *.tests.yaml suites. Examples: isl test .  |  isl test tests/  |  isl test calculator.tests.yaml  |  isl test . -f add"]
)
class TestCommand : Runnable {

    @Parameters(
        index = "0",
        arity = "0..1",
        description = ["Path to search for tests: directory, single file, or glob. Examples: . (current dir), tests/, calculator.tests.yaml. Default: current directory"]
    )
    var path: File? = null

    @Option(
        names = ["--glob"],
        description = ["Glob for .isl files when path is a directory (e.g. **/*.isl). YAML suites (*.tests.yaml) use **/*.tests.yaml when not set"]
    )
    var globPattern: String? = null

    @Option(
        names = ["-o", "--output"],
        description = ["Write results to a JSON file for parsing by other tools"]
    )
    var outputFile: File? = null

    @Option(
        names = ["-f", "--function"],
        description = ["Run only the specified test function(s). Can be specified multiple times. Use 'file:function' to target a specific file (e.g. sample.isl:test_customer)"]
    )
    var functions: Array<String> = emptyArray()

    @Option(
        names = ["-v", "--verbose"],
        description = ["Show detailed logs (search, loading, mocks, per-test progress). Without this, only test name, result, and a summary are shown"]
    )
    var verbose: Boolean = false

    @Option(
        names = ["--report"],
        arity = "0..1",
        paramLabel = "FILE",
        description = ["Write a Markdown test report to FILE. FILE is optional: if given, the report is written there (can be used with or without -o/--output). Summary at top, detailed results below. Example: --report test-report.md"]
    )
    var reportFile: File? = null

    private fun logCommandLineParams() {
        val pathStr = path?.let { it.absolutePath } ?: "(default: current directory)"
        val globStr = globPattern ?: "(default: **/*.isl)"
        val outputStr = outputFile?.path ?: "(none)"
        val functionStr = if (functions.isEmpty()) "(all)" else functions.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")
        val reportStr = reportFile?.path ?: "(none)"
        println("[ISL Test] Command line:")
        println("  path     : $pathStr")
        println("  glob     : $globStr")
        println("  output   : $outputStr")
        println("  function : $functionStr")
        println("  verbose  : $verbose")
        println("  report   : $reportStr")
    }

    override fun run() {
        TestRunFlags.setTestVerbose(verbose)
        logCommandLineParams()
        try {
            val basePath = (path?.absoluteFile ?: File(System.getProperty("user.dir"))).toPath().normalize()
            val searchBase = if (basePath.toFile().isDirectory) basePath else basePath.parent
            if (verbose) {
                when {
                    basePath.toFile().isFile -> println("[ISL Search] Searching: ${basePath.toAbsolutePath()}")
                    else -> {
                        val islGlob = globPattern ?: "**/*.isl"
                        println("[ISL Search] Searching: ${basePath.toAbsolutePath()} (ISL: $islGlob, YAML: **/*.tests.yaml)")
                    }
                }
            }
            val testFiles = discoverTestFiles(basePath)
            val yamlSuites = discoverYamlTestSuites(basePath)
            if (testFiles.isEmpty() && yamlSuites.isEmpty()) {
                System.err.println(red("[ISL Error] No test files found (looking for .isl with @setup/@test, or *.tests.yaml). Try: isl test <dir>  or  isl test path/to/suite.tests.yaml"))
                exitProcess(1)
            }
            val result = TestResultContext()
            val contextCustomizers: List<(com.intuit.isl.common.IOperationContext) -> Unit> = listOf(
                { ctx -> LogExtensions.registerExtensions(ctx) },
                { ctx -> TestExtensions.registerExtensions(ctx) }
            )
            val functionFilter = functions.map { it.trim() }.filter { it.isNotEmpty() }.toSet()

            if (testFiles.isNotEmpty()) {
                if (verbose) println("[ISL Loading] Found ${testFiles.size} ISL test file(s)")
                val fileInfos = testFiles.map { (filePath, content) ->
                    val moduleName = searchBase.relativize(filePath).toString().replace("\\", "/")
                    FileInfo(moduleName, content)
                }.toMutableList()
                val findExternalModule = createModuleResolver(testFiles, searchBase)
                try {
                    @Suppress("UNCHECKED_CAST")
                    val testPackage = TransformTestPackageBuilder().build(
                        fileInfos,
                        findExternalModule as java.util.function.BiFunction<String, String, String>,
                        searchBase,
                        contextCustomizers
                    )
                    if (functionFilter.isEmpty()) {
                        testPackage.runAllTests(result, verbose)
                    } else {
                        testPackage.runFilteredTests(result, { file, func ->
                            functionFilter.any { filter ->
                                when {
                                    filter.contains(":") -> {
                                        val parts = filter.split(":", limit = 2)
                                        val fileMatch = parts[0].equals(file, true) ||
                                            parts[0].equals(file.removeSuffix(".isl"), true)
                                        fileMatch && parts[1].equals(func, true)
                                    }
                                    else -> filter.equals(func, true)
                                }
                            }
                        }, verbose)
                    }
                } catch (e: Exception) {
                    result.testResults.addAll(createErrorResult(e, fileInfos).testResults)
                }
            }

            if (yamlSuites.isNotEmpty()) {
                if (verbose) println("[ISL Loading] Found ${yamlSuites.size} YAML test suite(s)")
                for (yamlPath in yamlSuites) {
                    val suiteBase = if (yamlPath.toFile().isFile) yamlPath.parent else yamlPath
                    YamlUnitTestRunner.runSuite(yamlPath, suiteBase, result, contextCustomizers, functionFilter, verbose)
                }
            }

            if (result.testResults.isEmpty()) {
                System.err.println(red("[ISL Error] No tests ran. Check path and --function filter."))
                exitProcess(1)
            }
            // Always print results to console first; file output is in addition to, not instead of, logs
            reportResults(result, verbose)
            outputFile?.let { writeResultsToJson(result, it) }
            reportFile?.let { writeReportMarkdown(result, it) }
            val failedCount = result.testResults.count { !it.success }
            if (failedCount > 0) {
                exitProcess(1)
            }
        } catch (e: Exception) {
            System.err.println(red("[ISL Error] Error: ${e.message ?: e.toString()}"))
            val root = generateSequence(e as Throwable) { it.cause }.last()
            if (root !== e) {
                System.err.println(red("[ISL Error] Caused by: ${root.javaClass.simpleName}: ${root.message ?: "no message"}"))
            }
            root.stackTrace.take(8).forEach { frame ->
                System.err.println(red("  at $frame"))
            }
            if (System.getProperty("debug") == "true") {
                e.printStackTrace()
            }
            exitProcess(1)
        } finally {
            TestRunFlags.clear()
        }
    }

    private fun discoverTestFiles(basePath: Path): List<Pair<Path, String>> {
        val islFiles = when {
            basePath.toFile().isFile -> {
                if (basePath.toString().endsWith(".isl", true)) listOf(basePath) else emptyList()
            }
            basePath.toFile().isDirectory -> {
                val pattern = globPattern ?: "**/*.isl"
                val matcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")
                Files.walk(basePath)
                    .use { stream ->
                        stream
                            .filter { it.isRegularFile() && it.extension.equals("isl", true) }
                            .filter { path ->
                                val relative = basePath.relativize(path)
                                val normalized = relative.toString().replace("\\", "/")
                                globPattern == null || matcher.matches(FileSystems.getDefault().getPath(normalized))
                            }
                            .toList()
                    }
            }
            else -> emptyList()
        }
        return islFiles
            .mapNotNull { path ->
                val content = path.toFile().readText()
                if (content.contains("@setup") || content.contains("@test")) {
                    path to content
                } else null
            }
    }

    private fun discoverYamlTestSuites(basePath: Path): List<Path> {
        return when {
            basePath.toFile().isFile -> {
                if (basePath.toString().endsWith(".tests.yaml", true) || basePath.toString().endsWith(".tests.yml", true)) {
                    listOf(basePath)
                } else emptyList()
            }
            basePath.toFile().isDirectory -> {
                val pattern = globPattern ?: "**/*.tests.yaml"
                val matcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")
                Files.walk(basePath)
                    .use { stream ->
                        stream
                            .filter { it.isRegularFile() }
                            .filter { path ->
                                val ext = path.extension.lowercase()
                                (ext == "yaml" || ext == "yml") && path.nameWithoutExtension.endsWith(".tests", ignoreCase = true)
                            }
                            .filter { path ->
                                val relative = basePath.relativize(path)
                                val normalized = relative.toString().replace("\\", "/")
                                globPattern == null || matcher.matches(FileSystems.getDefault().getPath(normalized))
                            }
                            .toList()
                    }
            }
            else -> emptyList()
        }
    }

    private fun createModuleResolver(testFiles: List<Pair<Path, String>>, searchBase: Path): java.util.function.BiFunction<String, String, String?> {
        val fileByModuleName = testFiles.associate { (filePath, content) ->
            val moduleName = searchBase.relativize(filePath).toString().replace("\\", "/").removeSuffix(".isl")
            moduleName to content
        }
        val fileByFullName = testFiles.associate { (filePath, content) ->
            searchBase.relativize(filePath).toString().replace("\\", "/") to content
        }
        val resolvedPaths = mutableMapOf<String, Path>()
        testFiles.forEach { (filePath, _) ->
            val fullName = searchBase.relativize(filePath).toString().replace("\\", "/")
            resolvedPaths[fullName] = filePath.toAbsolutePath().normalize()
        }
        return java.util.function.BiFunction { fromModule: String, dependentModule: String ->
            fileByFullName[dependentModule]
                ?: fileByModuleName[dependentModule]
                ?: IslModuleResolver.resolveExternalModule(searchBase, fromModule, dependentModule, resolvedPaths)
                ?: throw TransformCompilationException(
                    "Could not find module '$dependentModule' (imported from $fromModule). Searched relative to ${resolvedPaths[fromModule]?.parent ?: searchBase.resolve(fromModule).parent}"
                )
        }
    }

    private fun reportResults(result: TestResultContext, verbose: Boolean) {
        val passed = result.testResults.count { it.success }
        val failed = result.testResults.count { !it.success }
        val total = result.testResults.size

        //if (verbose) {
            val byGroup = result.testResults.groupBy { it.testGroup ?: it.testFile }
            byGroup.forEach { (group, tests) ->
                println("[ISL Result]   $group")
                tests.forEach { tr ->
                    val displayName = if (tr.testName != tr.functionName) "${tr.testName} (${tr.functionName})" else tr.testName
                    if (tr.success) {
                        println("[ISL Result]     ${green("[PASS]")} $displayName")
                    } else {
                        println("[ISL Result]     ${red("[FAIL]")} $displayName")
                        tr.message?.let { println("[ISL Result]         ${red(it)}") }
                        tr.errorPosition?.let { pos ->
                            val loc = "${pos.file}:${pos.line}:${pos.column}"
                            println("[ISL Result]         ${red("at $loc")}")
                        }
                        tr.exception?.let { ex ->
                            val root = generateSequence(ex as Throwable) { it.cause }.last()
                            if (root !== ex) {
                                println("[ISL Result]         ${red("Caused by: ${root.javaClass.simpleName}: ${root.message ?: "no message"}")}")
                            }
                            root.stackTrace.take(8).forEach { frame ->
                                println("[ISL Result]           ${red("at $frame")}")
                            }
                        }
                    }
                }
            }
            println("[ISL Result] ---")
            val resultsLine = "Results: $passed passed, $failed failed, $total total"
            println(if (failed > 0) red("[ISL Result] $resultsLine") else "[ISL Result] $resultsLine")
        //} else {
            
            // Nice summary
          //  printSummary(passed, failed, total)
        //}
    }

    private fun printSummary(passed: Int, failed: Int, total: Int) {
        val summaryText = if (failed == 0) {
            "All tests passed ($total total)"
        } else {
            "$failed failed, $passed passed ($total total)"
        }
        val summary = if (failed == 0) {
            "${green("All tests passed")} ($total total)"
        } else {
            "${red("$failed failed")}, $passed passed ($total total)"
        }
        val contentWidth = summaryText.length.coerceAtLeast(28)
        val line = "─".repeat(contentWidth + 4)
        val padding = " ".repeat((contentWidth - summaryText.length).coerceAtLeast(0))
        println()
        println("┌$line┐")
        println("│  $summary$padding  │")
        println("└$line┘")
    }

    /** Use ANSI color only when stdout is a TTY (e.g. terminal). When piped (e.g. from VS Code Test Explorer), output is plain text. */
    private fun useColor(): Boolean = System.console() != null

    private fun green(text: String) = if (useColor()) "\u001B[32m$text\u001B[0m" else text
    private fun red(text: String) = if (useColor()) "\u001B[31m$text\u001B[0m" else text

    private fun createErrorResult(e: Exception, fileInfos: List<FileInfo>): TestResultContext {
        val (message, position) = when (e) {
            is TransformCompilationException -> e.message to e.position
            is com.intuit.isl.runtime.TransformException -> e.message to e.position
            is com.intuit.isl.runtime.IslException -> e.message to e.position
            else -> e.message to null
        }
        val firstFile = fileInfos.firstOrNull()?.name ?: "unknown"
        val errorResult = TestResult(
            testFile = position?.file ?: firstFile,
            functionName = "compilation",
            testName = "compilation",
            testGroup = firstFile,
            success = false,
            message = message ?: e.toString(),
            errorPosition = position
        )
        return TestResultContext(mutableListOf(errorResult))
    }

    private fun writeResultsToJson(result: TestResultContext, file: File) {
        val passed = result.testResults.count { it.success }
        val failed = result.testResults.count { !it.success }
        val results = result.testResults.map { tr ->
            mapOf(
                "testFile" to tr.testFile,
                "functionName" to tr.functionName,
                "testName" to tr.testName,
                "testGroup" to tr.testGroup,
                "success" to tr.success,
                "message" to tr.message,
                "errorPosition" to (tr.errorPosition?.let { pos ->
                    mapOf(
                        "file" to pos.file,
                        "line" to pos.line,
                        "column" to pos.column
                    )
                })
            )
        }
        val output = mapOf(
            "passed" to passed,
            "failed" to failed,
            "total" to result.testResults.size,
            "success" to (failed == 0),
            "results" to results
        )
        val mapper = jacksonObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        file.writeText(mapper.writeValueAsString(output))
        println("[ISL Output] Results written to: ${file.absolutePath}")
    }

    private fun writeReportMarkdown(result: TestResultContext, reportFile: File) {
        val passed = result.testResults.count { it.success }
        val failed = result.testResults.count { !it.success }
        val total = result.testResults.size
        val success = failed == 0

        val md = buildString {
            // Title and summary at top
            appendLine("# ISL Test Report")
            appendLine()
            appendLine("**Generated:** ${java.time.Instant.now().atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)}")
            appendLine()
            appendLine("## Summary")
            appendLine()
            appendLine("| | Count |")
            appendLine("|---|------|")
            appendLine("| **Total** | $total |")
            appendLine("| **Passed** | $passed |")
            appendLine("| **Failed** | $failed |")
            appendLine("| **Status** | ${if (success) "✅ All passed" else "❌ $failed failed"} |")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## Detailed Results")
            appendLine()

            val byGroup = result.testResults.groupBy { it.testGroup ?: it.testFile }
            for ((group, tests) in byGroup) {
                appendLine("### $group")
                appendLine()
                for (tr in tests) {
                    val fileLabel = "`${tr.testFile.replace("`", "\\`")}`"
                    val displayName = if (tr.testName != tr.functionName) "${tr.testName} (`${tr.functionName}`)" else tr.testName
                    val line = "$fileLabel — $displayName"
                    if (tr.success) {
                        appendLine("- ✅ **$line**")
                    } else {
                        appendLine("- ❌ **$line**")
                        val hasExpectedActual = tr.expectedJson != null && tr.actualJson != null
                        tr.message?.let { msg ->
                            appendLine("  - *${escapeMarkdownInline(msg.lines().first().trim())}*")
                            if (hasExpectedActual) {
                                appendLine()
                                appendLine("  **Expected:**")
                                appendLine("  ```json")
                                prettyJson(tr.expectedJson!!).lines().forEach { appendLine("  $it") }
                                appendLine("  ```")
                                appendLine()
                                appendLine("  **Actual:**")
                                appendLine("  ```json")
                                prettyJson(tr.actualJson!!).lines().forEach { appendLine("  $it") }
                                appendLine("  ```")
                                val diffs = tr.comparisonDiffs
                                if (!diffs.isNullOrEmpty()) {
                                    appendLine()
                                    appendLine("  **Differences:**")
                                    for (d in diffs) {
                                        appendLine()
                                        appendLine("  **Expected:**")
                                        appendLine("  ```")
                                        appendLine("  ${d.path} = ${d.expectedValue}")
                                        appendLine("  ```")
                                        appendLine("  **Actual:**")
                                        appendLine("  ```")
                                        appendLine("  ${d.path} = ${d.actualValue}")
                                        appendLine("  ```")
                                    }
                                }
                            } else if (msg.lines().size > 1) {
                                appendLine("  ```")
                                msg.lines().take(20).forEach { appendLine(it) }
                                if (msg.lines().size > 20) appendLine("  ...")
                                appendLine("  ```")
                            }
                        }
                        tr.errorPosition?.let { pos ->
                            appendLine("  - `${pos.file}:${pos.line}:${pos.column}`")
                        }
                    }
                }
                appendLine()
            }
        }
        reportFile.writeText(md)
        println("[ISL Output] Report written to: ${reportFile.absolutePath}")
    }

    private fun escapeMarkdownInline(s: String): String =
        s.replace("\\", "\\\\").replace("`", "\\`").replace("*", "\\*").replace("_", "\\_")

    private fun prettyJson(json: String): String {
        return try {
            val tree = jacksonObjectMapper().readTree(json)
            jacksonObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(tree)
        } catch (_: Exception) {
            json
        }
    }
}
