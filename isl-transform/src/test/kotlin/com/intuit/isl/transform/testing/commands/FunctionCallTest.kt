package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.common.*
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.utils.indexOrDefault
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("unused")
class FunctionCallTest : YamlTransformTest("functions") {
    companion object {
        suspend fun methodNull(context: FunctionExecuteContext): Any? {
            return null
        }

        suspend fun method0(context: FunctionExecuteContext): Any? {
            return "method0";
        }

        suspend fun method1(context: FunctionExecuteContext): Any? {
            return "method1 ${context.firstParameter ?: "<was null>"}";
        }

        suspend fun processBatch(context: FunctionExecuteContext): Any? {
            return "processBatch ${context.firstParameter}";
        }

        fun paramMethod0(context: FunctionExecuteContext): Any? {
            return "paramMethod0";
        }

        fun paramMethod1(context: FunctionExecuteContext): Any? {
            val tos = ConvertUtils.tryToString(context.firstParameter);
            return "paramMethod1 $tos";
        }

        fun paramMethod2(context: FunctionExecuteContext): Any? {
            return "paramMethod2 ${context.firstParameter} ${context.parameters.get(1)}";
        }

        fun save(context: FunctionExecuteContext): Any? {
            return "save ${context.firstParameter} ${context.parameters.get(1)}";
        }

        @JvmStatic
        fun functionCallFixture(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "value: @.unknown.unknown()",
                    """"Could not Execute '@.unknown.unknown'. Error='Unknown Function: unknown.unknown' at Position(file=test, line=1, column=7, endLine=1, endColumn=26)"""",
                    null
                ),
                Arguments.of("value: @.test.methodnull()", """{ "value": null }""", null),

                Arguments.of("value: @.test.method0()", """{ "value": "method0" }""", null),
                Arguments.of("value: @.test.method1( 123 )", """{ "value": "method1 123" }""", null),

                Arguments.of("value: @.test.method1( null )", """{ "value": "method1 <was null>" }""", null),

                Arguments.of(
                    "value: {\n" +
                            "  \"start_date\": @.test.method1( \"abc\" )\n" +
                            "};", """{ "value": { "start_date": "method1 abc" } }""", null
                ),


                Arguments.of(
                    "value: @.test.method1( { a : 123; b: \"abc\" } )",
                    """{"value":"method1 {\"a\":123,\"b\":\"abc\"}"}""",
                    null
                ),
                Arguments.of(
                    "value: @.test.method1({ \n" +
                            "a : 123;\n" +
                            "b: \"abc\" \n" +
                            "} )", """{"value":"method1 {\"a\":123,\"b\":\"abc\"}"}""", null
                ),


                Arguments.of("value: @.test.paramMethod0()", """{ "value": "paramMethod0" }""", null),


                Arguments.of("value: @.test.paramMethod1( 123 )", """{ "value": "paramMethod1 123" }""", null),
                Arguments.of("value: @.test.paramMethod1( \"abc\" )", """{ "value": "paramMethod1 abc" }""", null),
                Arguments.of(
                    "value: @.test.paramMethod1( \$var )",
                    """{ "value": "paramMethod1 abc" }""",
                    mapOf("var" to "abc")
                ),

                Arguments.of("value: @.test.paramMethod2( 123, 456 )", """{ "value": "paramMethod2 123 456" }""", null),

                Arguments.of(
                    "\$var: @.test.paramMethod2( 123, 456 ); value: \$var",
                    """{ "value": "paramMethod2 123 456" }""",
                    null
                ),

                Arguments.of("@.test.paramMethod2( 123, 456 );", """{}""", null),
                Arguments.of("@.Log.Info( \"text\", \$var );", """{}""", mapOf("var" to "123")),
                Arguments.of("@.Log.Info( \"text {} \", \$var );", """{}""", mapOf("var" to "123")),
                Arguments.of("@.Log.Info(\"text {}\",\$var)", """{}""", mapOf("var" to "123")),
                Arguments.of("@.Log.Info(\"text {}\",\$var);", """{}""", mapOf("var" to "123")),

                Arguments.of("@.Test.Name.Save(\"text {}\",\$var);", """{}""", mapOf("var" to "123")),

                //Arguments.of("value: @.Acquire.ProcessBatch( [123] )", """{ "value": "paramMethod1 123" }""", null),

                // TODO: Add support for Dates & Times - Arik - help me :)
                //Arguments.of("today: @.Date.Today( );", """{ "value": "paramMethod2 123 456" }""", null),
            )
        }


        suspend fun testStatementFunction(
            context: FunctionExecuteContext,
            statementsExtensionMethod: StatementExecution
        ): Any? {
            // let's call the statements few times
            var result = 1;

            val varName = "\$" + context.parameters.indexOrDefault(0).toString();
            val f = ConvertUtils.tryParseLong(context.parameters.indexOrDefault(1)) ?: 0;
            val t = ConvertUtils.tryParseLong(context.parameters.indexOrDefault(2)) ?: 0;

            context.executionContext.operationContext.setVariable(varName, JsonConvert.convert(result));
            for (i in f..t) {
                result = ConvertUtils.tryParseInt(statementsExtensionMethod(context.executionContext).value) ?: 0;
                context.executionContext.operationContext.setVariable(varName, JsonConvert.convert(result));
            }

            return result;
        }

        @JvmStatic
        fun functionStatementCallFixture(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "value: @.Statement.Math( \"ttt\", 1, 1 ) { return {{ \$ttt * 2 }} } ",
                    """{ "value": 2 }""",
                    null
                ),
                Arguments.of(
                    "value: @.Statement.Math( \"ttt\", 1, 5 ) { return {{ \$ttt * 2 }} } ",
                    """{ "value": 32 }""",
                    null
                ),
                Arguments.of(
                    "value: @.Statement.Math( \"ttt\", 3, null ) { return {{ \$ttt * 2 }} } ",
                    """{ "value": 1 }""",   // we start from 1
                    null
                ),
            )
        }

        @JvmStatic
        fun forLoopFunctionStatementCallFixture(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "\$a: [ 1, 2, 3 ];\n" +
                            "foreach \$i in \$a\n" +
                            "   \$r: @.test.paramMethod1( \$i )\n" +
                            "   \$resultArray: \$resultArray | push ( \$r )\n" +
                            "endfor\n" +
                            "value: \$resultArray;",
                    """{"value":["paramMethod1 1","paramMethod1 2","paramMethod1 3"]}""",
                    null
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource(
        "functionCallFixture",
        "functionStatementCallFixture",
        "forLoopFunctionStatementCallFixture"
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        val extensions = mapOf<String, AsyncContextAwareExtensionMethod>(
            "test.methodNull" to Companion::methodNull,
            "test.method0" to Companion::method0,
            "test.method1" to Companion::method1,
        )

        try {
            run(script, expectedResult, map, extensions);
        } catch (e: TransformException) {
            assertEquals(expectedResult, e.message);
        }
    }

    override fun onRegisterExtensions(context: OperationContext) {
        context.registerExtensionMethod("test.paramMethod0", Companion::paramMethod0);
        context.registerExtensionMethod("test.paramMethod1", Companion::paramMethod1);
        context.registerExtensionMethod("test.paramMethod2", Companion::paramMethod2);

        context.registerExtensionMethod("test.name.save", Companion::save);

        context.registerExtensionMethod("Acquire.ProcessBatch") { context ->
            println("Acquire ${context.parameters}");
        };

        context.registerStatementMethod("Statement.Math", Companion::testStatementFunction);

        super.onRegisterExtensions(context)
    }


    private fun recursiveCallFixture(): Stream<Arguments> {
        return createTests("recursive")
    }
    private fun globalVarsFixture(): Stream<Arguments> {
        return createTests("globalvars")
    }

    @ParameterizedTest
    @MethodSource(
        //"recursiveCallFixture",
        "globalVarsFixture"
    )
    fun runFixtures(testName: String, script: String, expectedResult: JsonNode, map: Map<String, Any?>? = null) {
        run(script, expectedResult.toPrettyString(), map, mapOf(), doWarm = false);
    }
}