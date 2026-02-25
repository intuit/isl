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
import kotlin.system.exitProcess

/**
 * Command to execute ISL tests.
 * Discovers .isl files containing @setup or @test annotations and runs them.
 */
@Command(
    name = "test",
    aliases = ["tests"],
    description = ["Execute ISL tests from the specified path or current folder"]
)
class TestCommand : Runnable {

    @Parameters(
        index = "0",
        arity = "0..1",
        description = ["Path to search for tests: directory, file, or glob pattern (e.g. tests/**/*.isl). Default: current directory"]
    )
    var path: File? = null

    @Option(
        names = ["--glob"],
        description = ["Glob pattern to filter files (e.g. **/*.isl). Used when path is a directory"]
    )
    var globPattern: String? = null

    @Option(
        names = ["-o", "--output"],
        description = ["Write results to a JSON file for parsing by other tools"]
    )
    var outputFile: File? = null

    override fun run() {
        try {
            val basePath = (path?.absoluteFile ?: File(System.getProperty("user.dir"))).toPath().normalize()
            val searchBase = if (basePath.toFile().isDirectory) basePath else basePath.parent
            when {
                basePath.toFile().isFile -> println("Searching: ${basePath.toAbsolutePath()}")
                else -> {
                    val pattern = globPattern ?: "**/*.isl"
                    println("Searching: ${basePath.toAbsolutePath()} (glob: $pattern)")
                }
            }
            val testFiles = discoverTestFiles(basePath)
            if (testFiles.isEmpty()) {
                System.err.println(red("No test files found (looking for .isl files with @setup or @test)"))
                exitProcess(1)
            }
            println("Found ${testFiles.size} test file(s)")
            val fileInfos = testFiles.map { (filePath, content) ->
                val moduleName = searchBase.relativize(filePath).toString().replace("\\", "/")
                FileInfo(moduleName, content)
            }.toMutableList()
            val findExternalModule = createModuleResolver(testFiles, searchBase)
            val result = try {
                @Suppress("UNCHECKED_CAST")
                val testPackage = TransformTestPackageBuilder().build(
                    fileInfos,
                    findExternalModule as java.util.function.BiFunction<String, String, String>,
                    searchBase,
                    listOf { LogExtensions.registerExtensions(it) }
                )
                testPackage.runAllTests()
            } catch (e: Exception) {
                createErrorResult(e, fileInfos)
            }
            reportResults(result)
            outputFile?.let { writeResultsToJson(result, it) }
            val failedCount = result.testResults.count { !it.success }
            if (failedCount > 0) {
                exitProcess(1)
            }
        } catch (e: Exception) {
            System.err.println(red("Error: ${e.message}"))
            if (System.getProperty("debug") == "true") {
                e.printStackTrace()
            }
            exitProcess(1)
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

    private fun createModuleResolver(testFiles: List<Pair<Path, String>>, searchBase: Path): java.util.function.BiFunction<String, String, String?> {
        val fileByModuleName = testFiles.associate { (filePath, content) ->
            val moduleName = searchBase.relativize(filePath).toString().replace("\\", "/").removeSuffix(".isl")
            moduleName to content
        }
        val fileByFullName = testFiles.associate { (filePath, content) ->
            searchBase.relativize(filePath).toString().replace("\\", "/") to content
        }
        return java.util.function.BiFunction { fromModule: String, dependentModule: String ->
            fileByFullName[dependentModule]
                ?: fileByModuleName[dependentModule]
                ?: resolveExternalModule(searchBase, fromModule, dependentModule)
        }
    }

    private fun resolveExternalModule(searchBase: Path, fromModule: String, dependentModule: String): String? {
        val fromDir = searchBase.resolve(fromModule).parent ?: searchBase
        val candidateNames = if (dependentModule.endsWith(".isl", ignoreCase = true)) {
            listOf(dependentModule)
        } else {
            listOf("$dependentModule.isl", "$dependentModule.ISL")
        }
        val searchedPaths = mutableListOf<Path>()
        for (name in candidateNames) {
            val candidatePath = fromDir.resolve(name)
            searchedPaths.add(candidatePath.toAbsolutePath())
            val file = candidatePath.toFile()
            if (file.exists()) return file.readText()
        }
        val moduleBaseName = if (dependentModule.endsWith(".isl", ignoreCase = true)) {
            dependentModule.dropLast(4)
        } else {
            dependentModule
        }
        val found = searchBase.toFile().walkTopDown()
            .filter { it.isFile && it.extension.equals("isl", true) }
            .find { it.nameWithoutExtension.equals(moduleBaseName, true) }
        if (found != null) return found.readText()
        searchedPaths.forEach { System.err.println("Module not found. Searched: $it") }
        return null
    }

    private fun reportResults(result: TestResultContext) {
        val passed = result.testResults.count { it.success }
        val failed = result.testResults.count { !it.success }
        val byGroup = result.testResults.groupBy { it.testGroup ?: it.testFile }
        byGroup.forEach { (group, tests) ->
            println("  $group")
            tests.forEach { tr ->
                val displayName = if (tr.testName != tr.functionName) "${tr.testName} (${tr.functionName})" else tr.testName
                if (tr.success) {
                    println("    ${green("[PASS]")} $displayName")
                } else {
                    println("    ${red("[FAIL]")} $displayName")
                    tr.message?.let { println("        ${red(it)}") }
                    tr.errorPosition?.let { pos ->
                        val loc = "${pos.file}:${pos.line}:${pos.column}"
                        println("        ${red("at $loc")}")
                    }
                }
            }
        }
        println("---")
        val resultsLine = "Results: $passed passed, $failed failed, ${result.testResults.size} total"
        println(if (failed > 0) red(resultsLine) else resultsLine)
    }

    private fun green(text: String) = "\u001B[32m$text\u001B[0m"
    private fun red(text: String) = "\u001B[31m$text\u001B[0m"

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
        println("Results written to: ${file.absolutePath}")
    }
}
