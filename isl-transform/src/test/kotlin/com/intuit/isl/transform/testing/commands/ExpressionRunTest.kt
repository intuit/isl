package com.intuit.isl.transform.testing.commands

import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.dynamic.CommandBuilder
import com.intuit.isl.dynamic.run
import com.intuit.isl.utils.JsonConvert
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals

@Suppress("unused")
class ExpressionRunTest : BaseTransformTest() {
    companion object {
        @JvmStatic
        fun expressionTest(): Stream<Arguments> {
            return Stream.of(
                // just text
                Arguments.of(
                    "", """""",
                    emptyMap<String, Any>()
                ),

                Arguments.of(
                    "hi there", """hi there""",
                    emptyMap<String, Any>()
                ),

                Arguments.of(
                    "this is my text", """this is my text""",
                    emptyMap<String, Any>()
                ),

                Arguments.of(
                    "1234", """1234""",
                    emptyMap<String, Any>()
                ),

                Arguments.of(
                    "1234 + 5678", """1234 + 5678""",
                    emptyMap<String, Any>()
                ),

                Arguments.of(
                    "5000", """5000""",
                    emptyMap<String, Any>()
                ),

                // Direct Variable access
                Arguments.of(
                    "\$v", """123""",
                    mapOf("v" to 123)
                ),


                Arguments.of(
                    "\$v.a.b", """123""",
                    mapOf<String, Any>("v" to mapOf<String, Any>("a" to mapOf<String, Any>("b" to 123)))
                ),

                Arguments.of(
                    "Hi \$v", """Hi 123""",
                    mapOf("v" to 123)
                ),
                // escaped
                Arguments.of(
                    "Hi \\$123", """Hi $123""",
                    mapOf("v" to 123)
                ),

                // This should NOT be parsed as a variable
                Arguments.of(
                    "\${ \$v | lowerCase }", """brucewayne""",
                    mapOf("v" to "BruceWayne")
                ),


                Arguments.of(
                    "{{ 1 + 2 }}", """3""",
                    mapOf("v" to 123)
                ),
                Arguments.of(
                    "Your Total is {{ 1 + 2 }} dollars", """Your Total is 3 dollars""",
                    mapOf("v" to 123)
                ),
                Arguments.of(
                    "Your $ Total is {{ 1 + 2 }}\\$ dollars", """Your $ Total is 3$ dollars""",
                    mapOf("v" to 123)
                ),

                Arguments.of(
                    "Your $ @ Total is {{ 1 + 2 }}\\$ dollars", """Your $ @ Total is 3$ dollars""",
                    mapOf("v" to 123)
                ),
                Arguments.of(
                    "Your $ Total is {{ 1 + 2 }}\\$ dollars from @.Config.Get( \$v ) text @",
                    """Your ${'$'} Total is 3${'$'} dollars from VAL:123 text @""",
                    mapOf("v" to 123)
                ),
                Arguments.of(
                    "Dear \$user,\nTotal is {{ 1 + 2 }}.\nIf you think this is wrong please <a href=\"@.Config.GetUrl( 'support' )\">contact our support team</a>.",
                    """Dear George,
Total is 3.
If you think this is wrong please <a href="https://support.intuit.com">contact our support team</a>.""",
                    mapOf("user" to "George")
                ),
                Arguments.of(
                    "Hello, \${\$msg | merge}", "Hello, My name is STEVE",
                    mapOf(
                        "msg" to "My name is \${\$name | upperCase}",
                        "name" to "Steve"
                    )
                ),
                Arguments.of(
                    "Hello, \${\$date | merge}", "Hello, @.Date.Now()",
                    mapOf (
                        "date" to "@.Date.Now()"
                    )
                ),
                // Complex JSON Expressions
//                Arguments.of(
//                    "\$user.name",
//                    """George""",
//                    mapOf("user" to JsonNodeFactory.instance.objectNode().put("name", "George"))
//                )
            )
        }

        fun configGet(context: FunctionExecuteContext): Any? {
            return "VAL:${context.firstParameter}";
        }

        fun configGetUrl(context: FunctionExecuteContext): Any? {
            return "https://${context.firstParameter}.intuit.com";
        }
    }

    @ParameterizedTest
    @MethodSource(
        "expressionTest"
    )
    fun runFixtures(expression: String, expectedResult: String, map: Map<String, Any?>? = null) {
        return runBlocking {
            val context = OperationContext();
            onRegisterExtensions(context);
            map?.forEach {
                val key = if (it.key.startsWith("$")) it.key else "$${it.key}";
                context.setVariable(key, JsonConvert.convert(it.value));
            }

            val command = CommandBuilder().expression(expression);
            val result = context.run(command);

            println("Expression:");
            println(expression);
            println();
            println("Result");
            println(result.toString());
            assertEquals(expectedResult, result.toString());
        }
    }

    override fun onRegisterExtensions(context: OperationContext) {
        context.registerExtensionMethod("Config.Get", ExpressionRunTest::configGet);
        context.registerExtensionMethod("Config.GetUrl", ExpressionRunTest::configGetUrl);

        super.onRegisterExtensions(context)
    }
}