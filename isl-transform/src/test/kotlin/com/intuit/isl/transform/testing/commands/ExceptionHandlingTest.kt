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
                    """"Could not Execute '@.error.raise'. Error='CustomException: error' at Position(file=test, line=1, column=10, endLine=1, endColumn=32)."""",
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
                    """"Could not Execute '@.test' at Position(file=test, line=3, column=17, endLine=3, endColumn=23).\nCould not Execute '@.error.raise'. Error='CustomException: error' at Position(file=test, line=7, column=17, endLine=7, endColumn=39)."""",
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
                    """"Could not Execute '@.this.dowork' at Position(file=test, line=3, column=13, endLine=3, endColumn=33).\nCould not Execute '@.test' at Position(file=test, line=8, column=25, endLine=8, endColumn=31).\nCould not Execute '@.error.raise'. Error='CustomException: error' at Position(file=test, line=11, column=3, endLine=11, endColumn=25)."""",
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
                    """"Could not Execute '@.this.dowork' at Position(file=test, line=3, column=13, endLine=3, endColumn=33).\nCould not Execute '@.this.test' at Position(file=test, line=8, column=18, endLine=8, endColumn=39).\nCould not Execute '@.error.raise'. Error='CustomException: error' at Position(file=test, line=11, column=3, endLine=11, endColumn=25)."""",
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