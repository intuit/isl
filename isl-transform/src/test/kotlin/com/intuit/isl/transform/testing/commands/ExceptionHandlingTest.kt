package com.intuit.isl.transform.testing.commands

import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.ConvertUtils
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.run
import kotlin.test.assertEquals

class ExceptionHandlingTest : BaseTransformTest() {

    class CustomException(message: String) : Exception(message);

    companion object {
        @JvmStatic
        fun basicExceptions(): Stream<Arguments> {
            return Stream.of(
                // direct error
                Arguments.of(
                    "fun run(){" +
                            "@.Error.Raise('error');" +
                            "}" +
                            "" +
                            "",
                    """"Could not Execute '@.error.raise'. Error='CustomException: error at com.intuit.isl.transform.testing.commands.ExceptionHandlingTest${'$'}Companion.errorRaise(ExceptionHandlingTest.kt:86)' at test:1:10."""",
                    null
                ),
                // modifier one level down
                Arguments.of(
                    "fun run(){\n" +
                            "   return {\n" +
                            "       data: 123 | test,\n" +
                            "   }\n" +
                            "}\n\n" +
                            "modifier test() {" +
                            "@.Error.Raise('error');" +
                            "}" +
                            "",
                    """"Could not Execute '@.test' at test:3:17.\nCould not Execute '@.error.raise'. Error='CustomException: error at com.intuit.isl.transform.testing.commands.ExceptionHandlingTest${'$'}Companion.errorRaise(ExceptionHandlingTest.kt:86)' at test:7:17."""",
                    null
                ),

                // modifier two levels down
                Arguments.of(
                    "fun run(){\n" +
                            "   return {\n" +
                            "       data: @.This.DoWork( 123 ),\n" +
                            "   }\n" +
                            "}\n\n" +
                            "fun dowork( \$input ) {\n" +
                            "   return { data: \$input | test };\n" +
                            "}\n" +
                            "modifier test() {\n" +
                            "   @.Error.Raise('error');\n" +
                            "}\n" +
                            "",
                    """"Could not Execute '@.this.dowork' at test:3:13.\nCould not Execute '@.test' at test:8:25.\nCould not Execute '@.error.raise'. Error='CustomException: error at com.intuit.isl.transform.testing.commands.ExceptionHandlingTest${'$'}Companion.errorRaise(ExceptionHandlingTest.kt:86)' at test:11:3."""",
                    null
                ),

                // functions two levels down
                Arguments.of(
                    "fun run(){\n" +
                            "   return {\n" +
                            "       data: @.This.DoWork( 123 ),\n" +
                            "   }\n" +
                            "}\n\n" +
                            "fun dowork( \$input ) {\n" +
                            "   return { data: @.This.Test( \$input ) };\n" +
                            "}\n" +
                            "fun test() {\n" +
                            "   @.Error.Raise('error');\n" +
                            "}\n" +
                            "",
                    """"Could not Execute '@.this.dowork' at test:3:13.\nCould not Execute '@.this.test' at test:8:18.\nCould not Execute '@.error.raise'. Error='CustomException: error at com.intuit.isl.transform.testing.commands.ExceptionHandlingTest${'$'}Companion.errorRaise(ExceptionHandlingTest.kt:86)' at test:11:3."""",
                    null
                ),
            )
        }

        fun errorRaise(context: FunctionExecuteContext): Any? {
            throw CustomException(ConvertUtils.tryToString(context.firstParameter)!!);
        }
    }

    @ParameterizedTest
    @MethodSource(
        "basicExceptions"
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        run(script, expectedResult, map);
    }

    override fun onRegisterExtensions(context: OperationContext) {
        context.registerExtensionMethod("Error.Raise", ::errorRaise);
    }
}