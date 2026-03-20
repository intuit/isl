package com.intuit.isl.test

import com.intuit.isl.runtime.FileInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MockLoadTest {

    @Test
    fun mockLoad_yamlFile_registersMocksAndReturnsExpectedValues(@TempDir tempDir: Path) {
        val mocksDir = tempDir.resolve("tests").resolve("mocks")
        Files.createDirectories(mocksDir)
        Files.writeString(
            mocksDir.resolve("api-mocks.yaml"),
            """
            |func:
            |  - name: "Test.Function#1"
            |    return: 5
            |  - name: "Test.Function#2"
            |    return: 3
            |  - name: "Test.Function#3"
            |    return: null
            """.trimMargin()
        )

        val testIsl = tempDir.resolve("tests").resolve("sample.isl")
        Files.createDirectories(testIsl.parent)
        Files.writeString(
            testIsl,
            """
            |@test
            |fun testMockLoad() {
            |   @.Mock.Load("mocks/api-mocks.yaml")
            |   ${'$'}r1 = @.Test.Function()
            |   ${'$'}r2 = @.Test.Function()
            |   ${'$'}r3 = @.Test.Function()
            |   @.Assert.equal(5, ${'$'}r1)
            |   @.Assert.equal(3, ${'$'}r2)
            |   @.Assert.isnull(${'$'}r3)
            |}
            """.trimMargin()
        )

        val fileInfos = mutableListOf(
            FileInfo("tests/sample.isl", testIsl.toFile().readText())
        )
        val testPackage = TransformTestPackageBuilder().build(fileInfos, null, tempDir)
        val result = testPackage.runTest("tests/sample.isl", "testMockLoad")

        assertFalse(result.testResults.isEmpty(), "Expected test results; got: ${result.testResults}")
        val testResult = result.testResults.firstOrNull { it.testName.equals("testMockLoad", ignoreCase = true) }
            ?: result.testResults.first()
        assertTrue(testResult.success, "Test failed: ${testResult.message}")
    }

    @Test
    fun mockLoad_yamlFile_withParams_registersParamBasedMocks(@TempDir tempDir: Path) {
        val mocksDir = tempDir.resolve("tests").resolve("mocks")
        Files.createDirectories(mocksDir)
        Files.writeString(
            mocksDir.resolve("param-mocks.yaml"),
            """
            |func:
            |  - name: "Test.Function"
            |    return: "matched"
            |    params: [4]
            |  - name: "Test.Function"
            |    return: null
            """.trimMargin()
        )

        val testIsl = tempDir.resolve("tests").resolve("sample.isl")
        Files.createDirectories(testIsl.parent)
        Files.writeString(
            testIsl,
            """
            |@test
            |fun testMockLoadParams() {
            |   @.Mock.Load("mocks/param-mocks.yaml")
            |   ${'$'}r1 = @.Test.Function(4)
            |   ${'$'}r2 = @.Test.Function(5)
            |   @.Assert.equal("matched", ${'$'}r1)
            |   @.Assert.notEqual("matched", ${'$'}r2)
            |}
            """.trimMargin()
        )

        val fileInfos = mutableListOf(
            FileInfo("tests/sample.isl", testIsl.toFile().readText())
        )
        val testPackage = TransformTestPackageBuilder().build(fileInfos, null, tempDir)
        val result = testPackage.runTest("tests/sample.isl", "testMockLoadParams")

        assertFalse(result.testResults.isEmpty(), "Expected test results; got: ${result.testResults}")
        val testResult = result.testResults.firstOrNull { it.testName.equals("testMockLoadParams", ignoreCase = true) }
            ?: result.testResults.first()
        assertTrue(testResult.success, "Test failed: ${testResult.message}")
    }

    @Test
    fun mockLoad_jsonFile_registersMocks(@TempDir tempDir: Path) {
        val mocksDir = tempDir.resolve("tests").resolve("mocks")
        Files.createDirectories(mocksDir)
        Files.writeString(
            mocksDir.resolve("mocks.json"),
            """
            |{
            |  "func": [
            |    { "name": "Test.Function", "return": "from-json" }
            |  ]
            |}
            """.trimMargin()
        )

        val testIsl = tempDir.resolve("tests").resolve("sample.isl")
        Files.createDirectories(testIsl.parent)
        Files.writeString(
            testIsl,
            """
            |@test
            |fun testMockLoadJson() {
            |   @.Mock.Load("mocks/mocks.json")
            |   ${'$'}r = @.Test.Function()
            |   @.Assert.equal("from-json", ${'$'}r)
            |}
            """.trimMargin()
        )

        val fileInfos = mutableListOf(
            FileInfo("tests/sample.isl", testIsl.toFile().readText())
        )
        val testPackage = TransformTestPackageBuilder().build(fileInfos, null, tempDir)
        val result = testPackage.runTest("tests/sample.isl", "testMockLoadJson")

        assertFalse(result.testResults.isEmpty(), "Expected test results; got: ${result.testResults}")
        val testResult = result.testResults.firstOrNull { it.testName.equals("testMockLoadJson", ignoreCase = true) }
            ?: result.testResults.first()
        assertTrue(testResult.success, "Test failed: ${testResult.message}")
    }

    @Test
    fun mockLoad_fileNotFound_failsWithClearMessage(@TempDir tempDir: Path) {
        val testIsl = tempDir.resolve("tests").resolve("sample.isl")
        Files.createDirectories(testIsl.parent)
        Files.writeString(
            testIsl,
            """
            |@test
            |fun testMockLoadMissing() {
            |   @.Mock.Load("mocks/nonexistent.yaml")
            |}
            """.trimMargin()
        )

        val fileInfos = mutableListOf(
            FileInfo("tests/sample.isl", testIsl.toFile().readText())
        )
        val testPackage = TransformTestPackageBuilder().build(fileInfos, null, tempDir)
        val result = testPackage.runTest("tests/sample.isl", "testMockLoadMissing")

        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.first()
        assertFalse(testResult.success)
        assertContains(testResult.message ?: "", "File not found")
    }

    @Test
    fun mockLoad_yamlFile_withIslSnippet_compilesAndRunsMockInContext(@TempDir tempDir: Path) {
        val mocksDir = tempDir.resolve("tests").resolve("mocks")
        Files.createDirectories(mocksDir)
        Files.writeString(
            mocksDir.resolve("isl-mocks.yaml"),
            """
            |func:
            |  - name: "Util.Mask"
            |    isl: |
            |         fun mask(${'$'}value) {
            |             return `xxxxxx${'$'}value`;
            |         }
            """.trimMargin()
        )

        val testIsl = tempDir.resolve("tests").resolve("sample.isl")
        Files.createDirectories(testIsl.parent)
        Files.writeString(
            testIsl,
            """
            |@test
            |fun testIslMock() {
            |   @.Mock.Load("mocks/isl-mocks.yaml")
            |   ${'$'}r = @.Util.Mask("tail")
            |   @.Assert.equal("xxxxxxtail", ${'$'}r)
            |}
            """.trimMargin()
        )

        val fileInfos = mutableListOf(
            FileInfo("tests/sample.isl", testIsl.toFile().readText())
        )
        val testPackage = TransformTestPackageBuilder().build(fileInfos, null, tempDir)
        val result = testPackage.runTest("tests/sample.isl", "testIslMock")

        assertFalse(result.testResults.isEmpty(), "Expected test results; got: ${result.testResults}")
        val testResult = result.testResults.firstOrNull { it.testName.equals("testIslMock", ignoreCase = true) }
            ?: result.testResults.first()
        assertTrue(testResult.success, "Test failed: ${testResult.message}")
    }

    @Test
    fun mockLoad_yamlFile_withAnnotationModifier_registersModifierMock_andReturnsFromWrappedFunction(@TempDir tempDir: Path) {
        val mocksDir = tempDir.resolve("tests").resolve("mocks")
        Files.createDirectories(mocksDir)
        Files.writeString(
            mocksDir.resolve("modifier-mocks.yaml"),
            """
            |annotation:
            |  - name: "mask2"
            """.trimMargin()
        )

        val helperIsl = tempDir.resolve("tests").resolve("helper.isl")
        Files.createDirectories(helperIsl.parent)
        Files.writeString(
            helperIsl,
            """
            |@mask2
            |fun maskedValue() {
            |   return "wrapped"
            |}
            """.trimMargin()
        )

        val testIsl = tempDir.resolve("tests").resolve("sample.isl")
        Files.writeString(
            testIsl,
            """
            |import Helper from "tests/helper.isl";
            |@test
            |fun testMask2Modifier() {
            |   @.Mock.Load("mocks/modifier-mocks.yaml")
            |   ${'$'}r = @.Helper.maskedValue()
            |   @.Assert.equal("wrapped", ${'$'}r)
            |}
            """.trimMargin()
        )

        val fileInfos = mutableListOf(
            FileInfo("tests/helper.isl", helperIsl.toFile().readText()),
            FileInfo("tests/sample.isl", testIsl.toFile().readText())
        )
        val testPackage = TransformTestPackageBuilder().build(fileInfos, null, tempDir)
        val result = testPackage.runTest("tests/sample.isl", "testMask2Modifier")

        assertFalse(result.testResults.isEmpty(), "Expected test results; got: ${result.testResults}")
        val testResult = result.testResults.firstOrNull { it.testName.equals("testMask2Modifier", ignoreCase = true) }
            ?: result.testResults.first()
        assertTrue(testResult.success, "Test failed: ${testResult.message}")
    }

    @Test
    fun mockLoad_yamlFile_withAnnotationModifierAndResult_returnsMockResult(@TempDir tempDir: Path) {
        val mocksDir = tempDir.resolve("tests").resolve("mocks")
        Files.createDirectories(mocksDir)
        Files.writeString(
            mocksDir.resolve("modifier-mocks.yaml"),
            """
            |annotation:
            |  - name: "mask2"
            |    result: "masked-value"
            """.trimMargin()
        )

        val helperIsl = tempDir.resolve("tests").resolve("helper.isl")
        Files.createDirectories(helperIsl.parent)
        Files.writeString(
            helperIsl,
            """
            |@mask2
            |fun maskedValue() {
            |   return "wrapped"
            |}
            """.trimMargin()
        )

        val testIsl = tempDir.resolve("tests").resolve("sample.isl")
        Files.writeString(
            testIsl,
            """
            |import Helper from "tests/helper.isl";
            |@test
            |fun testMask2ModifierResult() {
            |   @.Mock.Load("mocks/modifier-mocks.yaml")
            |   ${'$'}r = @.Helper.maskedValue()
            |   @.Assert.equal("masked-value", ${'$'}r)
            |}
            """.trimMargin()
        )

        val fileInfos = mutableListOf(
            FileInfo("tests/helper.isl", helperIsl.toFile().readText()),
            FileInfo("tests/sample.isl", testIsl.toFile().readText())
        )
        val testPackage = TransformTestPackageBuilder().build(fileInfos, null, tempDir)
        val result = testPackage.runTest("tests/sample.isl", "testMask2ModifierResult")

        assertFalse(result.testResults.isEmpty(), "Expected test results; got: ${result.testResults}")
        val testResult = result.testResults.firstOrNull { it.testName.equals("testMask2ModifierResult", ignoreCase = true) }
            ?: result.testResults.first()
        assertTrue(testResult.success, "Test failed: ${testResult.message}")
    }

    @Test
    fun test_withUnmockedModifier_failsWithClearException(@TempDir tempDir: Path) {
        val testIsl = tempDir.resolve("tests").resolve("sample.isl")
        Files.createDirectories(testIsl.parent)
        Files.writeString(
            testIsl,
            """
            |@test
            |fun testUnmockedModifier() {
            |   ${'$'}x = "hello"
            |   ${'$'}r = ${'$'}x | mask
            |   @.Assert.equal("xxxxxxhello", ${'$'}r)
            |}
            """.trimMargin()
        )

        val fileInfos = mutableListOf(
            FileInfo("tests/sample.isl", testIsl.toFile().readText())
        )
        val testPackage = TransformTestPackageBuilder().build(fileInfos, null, tempDir)
        val result = testPackage.runTest("tests/sample.isl", "testUnmockedModifier")

        assertFalse(result.testResults.isEmpty())
        val testResult = result.testResults.firstOrNull { it.testName.equals("testUnmockedModifier", ignoreCase = true) }
            ?: result.testResults.first()
        assertFalse(testResult.success, "Expected test to fail when unmocked modifier | mask is used")
        assertContains(testResult.message ?: "", "Unmocked modifier")
        assertContains(testResult.message ?: "", "| mask")
        assertContains(testResult.message ?: "", "Modifier.mask")
    }
}
