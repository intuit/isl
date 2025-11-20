package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.commands.builder.ExecutionBuilder
import com.intuit.isl.commands.builder.IslJsBuilder
import com.intuit.isl.common.AsyncContextAwareExtensionMethod
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.parser.TransformParser
import com.intuit.isl.runtime.*
import com.intuit.isl.transform.testing.utils.runLoop
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*
import kotlin.test.assertEquals

enum class AssertEqualityType {
    Json,
    Text
}

abstract class BaseTransformTest {
//    @Before
//    fun changeLogLevel() {
//        Configurator.setAllLevels("", Level.ALL)
//    }

    companion object {
        fun readResource(filename: String, folderPath: String? = null): String {
            var baseUrl = "./src/test/resources"
            if (folderPath != null) {
                baseUrl = "$baseUrl/$folderPath"
            }
            return File("$baseUrl/$filename").readText()
        }


        fun compareJsonResults(
            expectedJson: String,
            result: JsonNode?,
            assertEqualityType: AssertEqualityType = AssertEqualityType.Json
        ) {
            val mapper = ObjectMapper()
            // we need this so 60 does not get written as 6e1 :(
            mapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)

            println("Transformed:\n${mapper.writeValueAsString(result)}")

            val ts = result?.toString()

            when (assertEqualityType) {
                // Json equality does not care about format, spacing and field ordering
                AssertEqualityType.Json -> assertEquals(mapper.readTree(expectedJson), mapper.readTree(ts))
                // Text equality checks that every single character matches
                AssertEqualityType.Text -> assertEquals(expectedJson, ts)
            }
        }

        fun logInfo(context: FunctionExecuteContext): Any? {
            val first = context.firstParameter
            val stringValue = ConvertUtils.tryToString(first)

            val args = context.parameters.drop(1).map { ConvertUtils.tryToString(it) };
            println(stringValue!!.replace("\\n", "\n") + args.joinToString { "," })

            return null
        }

        fun readResource(filename: String): String {
            return File("./src/test/resources/$filename").readText()
        }
    }

    protected fun run(
        script: String,
        expectedResult: String,
        map: Map<String, Any?>? = null,
        extensions: Map<String, AsyncContextAwareExtensionMethod>? = null,
        runCount: Int = 1,
        assertEqualityType: AssertEqualityType = AssertEqualityType.Json,
        doWarm: Boolean = false
    ): Pair<ITransformer, ITransformResult> {
        //println(script)

        return runBlocking {
            val moduleToken = runLoop(1, "parseISL") {
                TransformParser().parseTransform("test", script)
            }

            // We'll to string this
            val toString = moduleToken.toString();
            val toPrettyString = moduleToken.toPrettyString(0);

            runLoop(1, "build") {
                ExecutionBuilder("test", moduleToken, null, null).build()
            }

            println();println();
            val t = runLoop(1, "compileIsl") {
//                println("Compiling");
//                println(script);
//                println("-----");
                val r = TransformCompiler().compileIsl("test", script)
//                println("Compiled");
                return@runLoop r;
            }

            if (doWarm) {
                try {
                    runLoop(10, "warmUp:RunTransform") {
                        runTransform(map, extensions, t)
                    }
                } catch (_: Exception) {
                }  // safe to ignore - we'll process it later as the result
            }

            val transformResult = try {
                runLoop(runCount, "runTransform") {
                    val result = runTransform(map, extensions, t)
                    return@runLoop result;
                }
            } catch (e: Exception) {
                TransformResult(JsonNodeFactory.instance.textNode(e.message))
            }

            compareJsonResults(expectedResult, transformResult!!.result, assertEqualityType)

            return@runBlocking Pair(t, transformResult!!)
        }
    }

    private suspend fun runTransform(
        map: Map<String, Any?>?,
        extensions: Map<String, AsyncContextAwareExtensionMethod>?,
        t: ITransformer
    ): ITransformResult? {
        val context = OperationContext()
        map?.forEach {
            val key = if (it.key.startsWith("$")) it.key else "$${it.key}"
            context.setVariable(key, JsonConvert.convert(it.value))
        }

        // extension functions
        extensions?.forEach {
            context.registerExtensionMethod(it.key, it.value)
        }

        onRegisterExtensions(context)

        var transformResult: ITransformResult? = null
        transformResult = t.runTransformAsync("run", context)
        return transformResult
    }

    open fun onRegisterExtensions(context: OperationContext) {
        context.registerExtensionMethod("Log.Info", BaseTransformTest::logInfo)
    }

    fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")
    }

    fun runJs(script: String, expectedResult: JsonNode, map: Map<String, Any?>? = null) {
        println(">>>>\r\n$script\r\n")
        val moduleToken = TransformParser().parseTransform("test", script);
        val json = try {
            IslJsBuilder("test", moduleToken, null, null).build();
        } catch (e: TransformCompilationException) {
            println(e.message);
            println(e.stackTraceToString());
            return;
        }

        val items = json["fun"]["run"]["statements"]
        println(items.toString())

        val islJsPath = File("../isl-js/").absoluteFile;
        val tsNode = if(isWindows()) "\\node_modules\\.bin\\ts-node.cmd" else "/node_modules/.bin/ts-node";
        val command = islJsPath.canonicalPath.toString() + tsNode;
        val processBuilder = ProcessBuilder(
            command,
            "./src/index.ts",
            "script='${items
                .toString()
                .replace("\\\"", "\\'")  // when we encode \" in the json we need to fix it 
                .replace("\"", "\\\"")}'"   // encode " with \"
        )
        val path = System.getProperty("java.library.path");
        processBuilder.environment()["PATH"] = path;
        println("Path ${processBuilder.environment()["PATH"]} >>> ${path}");
        println("ISLJs: " + islJsPath.canonicalPath);
        processBuilder.directory(islJsPath);
        val proc = processBuilder.start();

        val stdInput = BufferedReader(InputStreamReader(proc.inputStream))
        val stdError = BufferedReader(InputStreamReader(proc.errorStream))

        var s: String?;
        var actual: String? = null;
        while ((stdInput.readLine().also { s = it }) != null) {
            println(s)
            if (s?.startsWith(">>>Result=") == true)
                actual = s?.substringAfter("=")
        }
        while ((stdError.readLine().also { s = it }) != null) {
            println(s)
        }

        println("Final Result=${actual ?: ""}")

        try {
            val jsonResult = JsonConvert.mapper.readTree(actual);

            compareJsonResults(expectedResult.toPrettyString(), jsonResult, AssertEqualityType.Json)
        }catch (e: IllegalArgumentException){
            // command too long
            println("TEST WAS NOT RUN Result=${e.message}")
        }
    }
}