package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.FileInfo
import com.intuit.isl.runtime.TransformPackageBuilder
import com.intuit.isl.utils.ConvertUtils
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals

class ModulesTest {

    companion object {
        @JvmStatic
        fun modulesCallFixture(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    // note the silly $ for string interpolation ${'$'} -- https://kotlinlang.org/docs/basic-types.html#string-templates
                    mapOf(
                        "main.isl" to
                                """
                            import Common from 'second.isl';
                            fun test(){
                                result: @.Common.DoStuff( 123 );
                            }
                            """.trimIndent(),
                        "second.isl" to """
                        fun DoStuff( ${'$'}value ){
                            return {{ ${'$'}value * 10 }};
                        }
                        """.trimIndent()
                    ),

                    "main.test",

                    """{ "result": 1230 }""",
                ),

                // Check Imported Modifier
                Arguments.of(
                    mapOf(
                        "main.isl" to
                                """
                            import Common from 'second.isl';
                            fun test(){
                                resultF: @.Common.DoStuff( 123 );
                                resultM: 1253 | Common.DoStuff;
                            }
                            """.trimIndent(),
                        "second.isl" to """
                        modifier DoStuff( ${'$'}value ){
                            return {{ ${'$'}value * 10 }};
                        }
                        """.trimIndent()
                    ),

                    "main.test",

                    """{ "resultF": 1230, "resultM": 12530 }""",
                ),

                // make sure all @.This still work (as we do play with the context in the crossModuleExecution
                Arguments.of(
                    // note the silly $ for string interpolation ${'$'} -- https://kotlinlang.org/docs/basic-types.html#string-templates
                    mapOf(
                        "main.isl" to
                                """
                            import S from 'second.isl';
                            import T from 'third.isl';
                            fun test(){
                                @.Log.Debug("main:test");
                                result1: @.This.Test2( 123 );
                            }
                            fun test2( ${'$'}val ){
                                @.Log.Debug("main:test2");
                                return {
                                    result2: @.S.DoStuff( ${'$'}val ),
                                    result3: @.T.DoThird( ${'$'}val ),
                                    pow: 12 | T.calculate
                                }
                            }
                            """.trimIndent(),

                        "second.isl" to """
                        fun DoStuff( ${'$'}value ){
                            @.Log.Debug("second:dostuff");
                            return @.This.DoStuff2( ${'$'}value );
                        }
                        fun DoStuff2( ${'$'}value ){
                            @.Log.Debug("second:dostuff2");
                            return {{ ${'$'}value * 10 }};
                        }
                        """.trimIndent(),

                        "third.isl" to """
                        fun DoThird( ${'$'}value ){
                            @.Log.Debug("third:dostuff");
                            return {
                                text: @.External.Call( "value" ),
                                value: @.This.DoMoreThird( ${'$'}value )
                            }
                        }
                        fun DoMoreThird( ${'$'}value ){
                            @.Log.Debug("second:dostuff2");
                            return {{ ${'$'}value * 20 }};
                        }
                        modifier calculate( ${'$'}value ){
                            return {{ ${'$'}value * ${'$'}value }};
                        }
                        """.trimIndent()
                    ),

                    "main.test",

                    """{"result1":{"result2":1230,"result3":{"text":"External:value","value":2460}, "pow": 144}}""",
                )
            )
        }
    }


    @ParameterizedTest
    @MethodSource(
        "modulesCallFixture"
    )
    fun runFixtures(
        files: Map<String, String>,
        functionToRun: String,
        expectedResult: String
    ) {
        run(files, functionToRun, expectedResult);
    }

    fun run(
        files: Map<String, String>,
        functionToRun: String,
        expectedResult: String,
        variables: Map<String, Any?>? = null
    ) {
        val list = files.map {
            println("module ${it.key}\n${it.value}\n");
            FileInfo(it.key, it.value)
        }.toList();
        val p = TransformPackageBuilder().build(list.toMutableList());

        runBlocking {
            val context = OperationContext();
            onRegisterExtensions(context);

            val transformResult = p.runTransformAsync("main.isl:test", context);

            val mapper = ObjectMapper();
            // we need this so 60 does not get written as 6e1 :(
            mapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

            println("Transformed:\n${mapper.writeValueAsString(transformResult.result)}");

            assertEquals(mapper.readTree(expectedResult), mapper.readTree(transformResult.result?.toString()));
        }
    }

    fun onRegisterExtensions(context: IOperationContext) {
        context.registerExtensionMethod("Log.Debug", this::logDebug);
        context.registerExtensionMethod("External.Call", this::externalCall);
    }

    fun logDebug(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter;
        val stringValue = ConvertUtils.tryToString(first);

        println(stringValue!!.replace("\\n", "\n"));

        return null;
    }

    fun externalCall(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter;
        val stringValue = ConvertUtils.tryToString(first);

        return "External:" + stringValue;
    }
}