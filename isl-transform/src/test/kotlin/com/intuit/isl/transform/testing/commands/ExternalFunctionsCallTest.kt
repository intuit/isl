package com.intuit.isl.transform.testing.commands

import com.intuit.isl.common.*
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.utils.indexOrDefault
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * External functions are functions that are not registered but can still be called through delegation.
 */
@Suppress("unused")
class ExternalFunctionsCallTest : BaseTransformTest() {
    companion object {
        @JvmStatic
        fun functionCallFixture(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    //"import R from Random;" +
                    "value: @.Some.Function()",
                    """{ "value": "You called some.function ( null )" }""",
                    null
                )
            )
        }

        fun unknownFunctionHandler(context: FunctionExecuteContext): Any? {
            return "You called ${context.functionName} ( ${context.firstParameter} )";
        }
    }

    @ParameterizedTest
    @MethodSource(
        "functionCallFixture"
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        run(script, expectedResult, map, null);
    }

    override fun onRegisterExtensions(context: OperationContext) {
        context.registerFallbackFunctionHandler(Companion::unknownFunctionHandler);

        super.onRegisterExtensions(context)
    }
}