package com.intuit.isl.transform.testing.runtime

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.FileInfo
import com.intuit.isl.runtime.TransformProjectBuilder
import com.intuit.isl.runtime.TransformProjectManifest
import com.intuit.isl.runtime.Transformer
import kotlinx.coroutines.future.await
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.CompletableFuture
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TransformProjectTest {
    private val script = """
        |fun run( ${"$"}myVar ) {
        |    ${"$"}t = {
        |    prop1: 123
        |}
        |    return ${"$"}t;
        |}
        """.trimMargin()
    private val builder: TransformProjectBuilder = TransformProjectBuilder()
    private val scriptName = "test.isl"
    private val scripts: List<FileInfo> = listOf(FileInfo(scriptName, script))
    private val projectVersion = "1.1.0"

    @Test
    fun inputIslFiles_CreateProject_ProjectFileFound() {
        ByteArrayOutputStream().use { t ->
            builder.writeProject(scripts, projectVersion, t)
            val projectFile = t.toByteArray()
            assertNotNull(projectFile)
        }
    }

    @Test
    fun inputIslFiles_CreateProject_ISLFileInProject() {
        ByteArrayOutputStream().use { t ->
            builder.writeProject(scripts, projectVersion, t)
            val projectFile = t.toByteArray()
            ByteArrayInputStream(projectFile).use {
                ZipInputStream(it).use { t ->
                    generateSequence { t.nextEntry }.forEach { entry ->
                        if (entry.name == "test.isl") {
                            val contents = t.readBytes().toString(Charsets.UTF_8)
                            assertEquals(script, contents)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun inputIslFiles_CreateProject_ManifestFileExists() {
        ByteArrayOutputStream().use { t ->
            builder.writeProject(scripts, projectVersion, t)
            val projectFile = t.toByteArray()
            ByteArrayInputStream(projectFile).use {
                ZipInputStream(it).use { t ->
                    generateSequence { t.nextEntry }.forEach { entry ->
                        if (entry.name == "manifest.json") {
                            val contents = t.readBytes().toString(Charsets.UTF_8)
                            assertNotNull(contents)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun inputIslFiles_CreateProject_ManifestFileContentsCorrect() {
        ByteArrayOutputStream().use { t ->
            builder.writeProject(scripts, projectVersion, t)
            val projectFile = t.toByteArray()
            ByteArrayInputStream(projectFile).use {
                ZipInputStream(it).use { t ->
                    generateSequence { t.nextEntry }.forEach { entry ->
                        if (entry.name == "manifest.json") {
                            val mapper = jacksonObjectMapper()
                            val contents: TransformProjectManifest = mapper.readValue(t.readBytes())
                            assertEquals(Transformer.version, contents.islVersion)
                            assertEquals(projectVersion, contents.projectVersion)
                            assertContains(contents.files, "test.isl")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun inputIslProject_ParseProject_TransformPackageCreated() {
        ByteArrayOutputStream().use { t ->
            builder.writeProject(scripts, projectVersion, t)
            val projectFile = t.toByteArray()
            ByteArrayInputStream(projectFile).use {
                val transformPackage = builder.readProject(it)
                assertNotNull(transformPackage)
            }
        }
    }

    @Test
    fun inputIslProject_ParseProject_TransformPackageContainsFunction() {
        ByteArrayOutputStream().use { t ->
            builder.writeProject(scripts, projectVersion, t)
            val projectFile = t.toByteArray()
            ByteArrayInputStream(projectFile).use {
                val transformPackage = builder.readProject(it)
                assertNotNull(transformPackage)
                assertContains(transformPackage.modules, scriptName)
            }
        }
    }

    @Test
    fun inputIslProject_ParseProject_TransformPackageFunctionRunsCorrectly() {
        ByteArrayOutputStream().use { t ->
            builder.writeProject(scripts, projectVersion, t)
            val projectFile = t.toByteArray()
            ByteArrayInputStream(projectFile).use {
                val transformPackage = builder.readProject(it)
                assertNotNull(transformPackage)
                var operationContext = OperationContext()
                val result = transformPackage.runTransform("${scriptName}:run", operationContext)
                assertNotNull(result)
                if (result is JsonNode) {
                    assertEquals(123, result["prop1"].asInt())
                }
            }
        }
    }
}