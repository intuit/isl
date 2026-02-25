package com.intuit.isl.test

import com.intuit.isl.runtime.FileInfo
import com.intuit.isl.test.annotations.TestResultContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoadFunctionTest {

    @Test
    fun loadFrom_jsonFile_returnsParsedJson(@TempDir tempDir: Path) {
        val fixturesDir = tempDir.resolve("tests").resolve("fixtures")
        Files.createDirectories(fixturesDir)
        Files.writeString(fixturesDir.resolve("data.json"), """{"name": "test", "value": 42}""")

        val testIsl = tempDir.resolve("tests").resolve("sample.isl")
        Files.createDirectories(testIsl.parent)
        Files.writeString(
            testIsl,
            """
            |@test
            |fun testLoadJson() {
            |   ${'$'}data = @.Load.From("fixtures/data.json")
            |   @.Assert.equal("test", ${'$'}data.name)
            |   @.Assert.equal(42, ${'$'}data.value)
            |}
            """.trimMargin()
        )

        val fileInfos = mutableListOf(
            FileInfo("tests/sample.isl", testIsl.toFile().readText())
        )
        val testPackage = TransformTestPackageBuilder().build(fileInfos, null, tempDir)
        val result = testPackage.runTest("tests/sample.isl", "testLoadJson")

        assertFalse(result.testResults.isEmpty(), "Expected test results; got: ${result.testResults}")
        val testResult = result.testResults.firstOrNull { it.testName.equals("testLoadJson", ignoreCase = true) }
            ?: result.testResults.first()
        assertTrue(testResult.success, "Test failed: ${testResult.message}")
    }

    @Test
    fun loadFrom_yamlFile_returnsParsedJson(@TempDir tempDir: Path) {
        val fixturesDir = tempDir.resolve("tests").resolve("fixtures")
        Files.createDirectories(fixturesDir)
        Files.writeString(
            fixturesDir.resolve("config.yaml"),
            """
            |key: value
            |nested:
            |  count: 10
            """.trimMargin()
        )

        val testIsl = tempDir.resolve("tests").resolve("sample.isl")
        Files.createDirectories(testIsl.parent)
        Files.writeString(
            testIsl,
            """
            |@test
            |fun testLoadYaml() {
            |   ${'$'}data = @.Load.From("fixtures/config.yaml")
            |   @.Assert.equal("value", ${'$'}data.key)
            |   @.Assert.equal(10, ${'$'}data.nested.count)
            |}
            """.trimMargin()
        )

        val fileInfos = mutableListOf(
            FileInfo("tests/sample.isl", testIsl.toFile().readText())
        )
        val testPackage = TransformTestPackageBuilder().build(fileInfos, null, tempDir)
        val result = testPackage.runTest("tests/sample.isl", "testLoadYaml")

        assertFalse(result.testResults.isEmpty(), "Expected test results; got: ${result.testResults}")
        val testResult = result.testResults.firstOrNull { it.testName.equals("testLoadYaml", ignoreCase = true) }
            ?: result.testResults.first()
        assertTrue(testResult.success, "Test failed: ${testResult.message}")
    }

    @Test
    fun loadFrom_csvFile_returnsArrayOfObjects(@TempDir tempDir: Path) {
        val fixturesDir = tempDir.resolve("tests").resolve("fixtures")
        Files.createDirectories(fixturesDir)
        Files.writeString(
            fixturesDir.resolve("data.csv"),
            """
            |id,name,score
            |1,Alice,100
            |2,Bob,85
            """.trimMargin()
        )

        val testIsl = tempDir.resolve("tests").resolve("sample.isl")
        Files.createDirectories(testIsl.parent)
        Files.writeString(
            testIsl,
            """
            |@test
            |fun testLoadCsv() {
            |   ${'$'}data = @.Load.From("fixtures/data.csv")
            |   @.Assert.equal(2, ${'$'}data | length)
            |   @.Assert.equal("Alice", ${'$'}data[0].name)
            |   @.Assert.equal("Bob", ${'$'}data[1].name)
            |   @.Assert.equal(100, ${'$'}data[0].score)
            |}
            """.trimMargin()
        )

        val fileInfos = mutableListOf(
            FileInfo("tests/sample.isl", testIsl.toFile().readText())
        )
        val testPackage = TransformTestPackageBuilder().build(fileInfos, null, tempDir)
        val result = testPackage.runTest("tests/sample.isl", "testLoadCsv")

        assertFalse(result.testResults.isEmpty(), "Expected test results; got: ${result.testResults}")
        val testResult = result.testResults.firstOrNull { it.testName.equals("testLoadCsv", ignoreCase = true) }
            ?: result.testResults.first()
        assertTrue(testResult.success, "Test failed: ${testResult.message}")
    }
}

