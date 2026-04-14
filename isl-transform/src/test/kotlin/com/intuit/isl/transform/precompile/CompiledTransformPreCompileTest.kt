package com.intuit.isl.transform.precompile

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.FileInfo
import com.intuit.isl.runtime.ITransformer
import com.intuit.isl.runtime.TransformCompiler
import com.intuit.isl.runtime.TransformPackage
import com.intuit.isl.runtime.TransformPackageBuilder
import com.intuit.isl.runtime.serialization.CompiledTransformCodec
import com.intuit.isl.runtime.serialization.SourceArtifactMatchResult
import com.intuit.isl.runtime.serialization.SourceFingerprintMismatchException
import com.intuit.isl.utils.JsonConvert
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.util.TreeMap
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CompiledTransformPreCompileTest {

    @Test
    fun roundTrip_simpleImportAndHardwired_matchesCompileFromSource() {
        val files = mapOf(
            "main.isl" to """
                import Lib from 'lib.isl';
                fun test() {
                    result: @.Lib.id( 123 );
                }
            """.trimIndent(),
            "lib.isl" to """
                fun id( ${'$'}x ) {
                    return ${'$'}x;
                }
            """.trimIndent()
        )
        val compiled = TransformPackageBuilder().build(files.toMutableFileInfos())
        val bytes = TransformPackageBuilder().preCompileToBytes(files.toMutableFileInfos())
        val restored = TransformPackageBuilder.loadCompiled(bytes)

        val mapper = ObjectMapper().apply {
            enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
        }
        val expected = runTransformResultTree(mapper, compiled, "main.isl:test")
        val actual = runTransformResultTree(mapper, restored, "main.isl:test")
        assertEquals(expected, actual)
    }

    /**
     * End-to-end parity on the same **complex** Shopify fixture used in JMH
     * ([shopify-transform-complex.isl] + [shopify-order.json]): pre-compile → load → run
     * must match compile → run.
     */
    @Test
    fun roundTrip_jmhShopifyComplex_matchesCompileFromSource() {
        val script = jmhResourceFile("shopify-transform-complex.isl").readText()
        val orderJson = jmhResourceFile("shopify-order.json").readText()
        val inputNode = JsonConvert.mapper.readTree(orderJson)
        val moduleName = "shopify-complex-clean"
        val fileList = mutableListOf(FileInfo(moduleName, script))
        val compiled = TransformPackageBuilder().build(fileList.map { FileInfo(it.name, it.contents) }.toMutableList())
        val bytes = TransformPackageBuilder().preCompileToBytes(mutableListOf(FileInfo(moduleName, script)))
        val restored = TransformPackageBuilder.loadCompiled(bytes)

        val mapper = ObjectMapper().apply {
            enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
        }
        val fullName = "$moduleName:run"
        val expected = runTransformResultTree(mapper, compiled, fullName, inputNode)
        val actual = runTransformResultTree(mapper, restored, fullName, inputNode)
        assertEquals(expected, actual)
    }

    @Test
    fun sourceFingerprints_matchWhenUnchanged() {
        val main = """
            fun run() { return 1; }
        """.trimIndent()
        val lib = "fun id(\$x) { return \$x; }"
        val files = mapOf(
            "main.isl" to "import Lib from 'lib.isl';\n$main",
            "lib.isl" to lib
        )
        val list = files.toMutableFileInfos()
        val bytes = TransformPackageBuilder().preCompileToBytes(list.map { FileInfo(it.name, it.contents) }.toMutableList())
        assertIs<SourceArtifactMatchResult.Match>(TransformPackageBuilder.verifyCompiledAgainstSources(bytes, list.map { FileInfo(it.name, it.contents) }.toMutableList()))
    }

    @Test
    fun sourceFingerprints_mismatchWhenSourceEdited() {
        val main = "fun run() { return 1; }"
        val lib = "fun id(\$x) { return \$x; }"
        val files = mapOf(
            "main.isl" to "import Lib from 'lib.isl';\n$main",
            "lib.isl" to lib
        )
        val list = files.toMutableFileInfos()
        val bytes = TransformPackageBuilder().preCompileToBytes(list.map { FileInfo(it.name, it.contents) }.toMutableList())

        val edited = mutableListOf(
            FileInfo("main.isl", files.getValue("main.isl")),
            FileInfo("lib.isl", lib + "\n")
        )
        val bad = TransformPackageBuilder.verifyCompiledAgainstSources(bytes, edited)
        val mismatch = assertIs<SourceArtifactMatchResult.Mismatch>(bad)
        assertTrue(mismatch.modules.any { it.moduleName.equals("lib.isl", ignoreCase = true) })
    }

    @Test
    fun sourceFingerprints_absentWhenNotPassedAtPreCompile() {
        val compiled = singleModulePackage("m", "fun run() { return 1; }")
        val bytes = CompiledTransformCodec.preCompileToBytes(compiled, null)
        assertEquals(
            SourceArtifactMatchResult.NoFingerprintsStored,
            TransformPackageBuilder.verifyCompiledAgainstSources(bytes, mutableListOf(FileInfo("m", "x")))
        )
    }

    @Test
    fun loadWithSourceCheck_throwsOnMismatch() {
        val script = "fun run() { return 1; }"
        val bytes = TransformPackageBuilder().preCompileToBytes(mutableListOf(FileInfo("mod", script)))
        assertThrows<SourceFingerprintMismatchException> {
            TransformPackageBuilder.loadCompiled(bytes, mutableListOf(FileInfo("mod", script + " ")))
        }
    }

    @Test
    fun preCompile_rejectsDuplicateModuleNamesInFileList() {
        assertThrows<IllegalArgumentException> {
            TransformPackageBuilder().preCompileToBytes(
                mutableListOf(
                    FileInfo("Mod", "fun run() { return 1; }"),
                    FileInfo("mod", "fun run() { return 2; }")
                )
            )
        }
    }

    private fun Map<String, String>.toMutableFileInfos() =
        map { FileInfo(it.key, it.value) }.toMutableList()

    private fun singleModulePackage(moduleName: String, script: String): TransformPackage {
        val compiled = TransformCompiler().compileIsl(moduleName, script)
        val modules = TreeMap<String, ITransformer>(String.CASE_INSENSITIVE_ORDER)
        modules[moduleName] = compiled
        return TransformPackage(modules)
    }

    private fun jmhResourceFile(name: String): File {
        val fromModule = File("src/jmh/resources/$name")
        if (fromModule.isFile) return fromModule
        val fromRepo = File("isl-transform/src/jmh/resources/$name")
        if (fromRepo.isFile) return fromRepo
        error("Cannot find JMH resource '$name' (tried $fromModule and $fromRepo)")
    }

    private fun runTransformResultTree(
        mapper: ObjectMapper,
        pkg: com.intuit.isl.runtime.TransformPackage,
        fullName: String,
        input: JsonNode? = null
    ) = runBlocking {
        val ctx = OperationContext()
        ctx.registerSyncExtensionMethod("Log.Debug") { _: FunctionExecuteContext -> null }
        if (input != null) {
            ctx.setVariable("\$input", input)
        }
        val r = pkg.runTransformAsync(fullName, ctx)
        mapper.readTree(mapper.writeValueAsString(r.result))
    }
}
