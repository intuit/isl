package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.utils.JsonConvert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Suppress("unused")
class ForLoopTest : BaseTransformTest() {
    companion object {
        @JvmStatic
        fun forLoopFixture(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("foreach \$it in \$val assign: \$it endfor", """{}""", null),

                Arguments.of(
                    "foreach \$it in \$val\n" +
                    "   \$total = {{ \$total + \$t.amount_money.amount / 100 }}\n" +
                    "   \$fees = {{ \$fees  + \$t.processing_fee_money.amount / 100 }}\n" +
                    "endfor",
                    """{}""", null),

                Arguments.of(
                    "   \$total = {{ \$total * \$t.amount_money.amount / 100 }}\n" +
                    "   \$fees = {{ \$fees  + \$t.processing_fee_money.amount / 100 }}\n",
                    """{}""", null),

                // NOTE: this will be empty result because the foreach does not generate a property!
                Arguments.of(
                    "foreach \$it in \$arrr someProperty: \$it endfor", """{}""",
                    mapOf("arrr" to arrayOf(1, 2, 3))
                ),

                // this generates an array property!
                Arguments.of(
                    "result: foreach \$it in \$arrr\n" +
                    "  { someProperty: \$it }\n" +
                    "endfor",
                    """{  "result" : [ {"someProperty" : 1  }, { "someProperty" : 2  }, { "someProperty" : 3 } ]}""",
                    mapOf("arrr" to arrayOf(1, 2, 3))
                ),

                // generate a complex object
                Arguments.of(
                    "result: foreach \$it in \$arrr { tax_id: \$it; tax_amount: {{ \$it * 3 }} } endfor",
                    """{ "result" : [ 
                        { "tax_id" : 1, "tax_amount" : 3 }
                        , { "tax_id" : 2, "tax_amount" : 6  }
                        , { "tax_id" : 3, "tax_amount" : 9  } ]}""",
                    mapOf("arrr" to arrayOf(1, 2, 3))
                ),

                // do some math in the foreach
                Arguments.of(
                    "\$start: 10;\n" +
                            "foreach \$it in \$arrr \$start: {{ \$start + \$it }} endfor\n" +
                            "result: \$start;", // 10+1+2+3 > 16
                    """{  "result" : 16 }""",
                    mapOf("arrr" to arrayOf(1, 2, 3))
                ),

                Arguments.of(
                    "\$start: 10;\n" +
                    "\$r = foreach \$it in \$arrr\n" +
                    "  {\n" +
                    "      \$start: {{ \$start + \$it }},\n" +
                    "      item: \$start\n" +
                    "  }\n" +
                    "endfor\n" +
                    "result: \$r;", // 10+1+2+3 > 16
                    """{"result":[{"item":11},{"item":13},{"item":16}]}""",
                    mapOf("arrr" to arrayOf(1, 2, 3))
                ),

                // have an object generated - this should be captured automatically
                Arguments.of(
                    "\$r: foreach \$it in \$arrr @.Method.GenerateObject( \$it ) endfor\n" +
                    "result: \$r;",
                    """{ "result": [{"value":1},{"value":2},{"value":3}] }""",
                    mapOf("arrr" to arrayOf(1, 2, 3))
                )
            )
        }


        fun generateObject(context:FunctionExecuteContext): Any? {
            val o = JsonNodeFactory.instance.objectNode();
            o.set<JsonNode>("value", JsonConvert.convert(context.firstParameter));
            return o;
        }
    }

    @ParameterizedTest
    @MethodSource(
        "forLoopFixture"
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        run(script, expectedResult, map);
    }

    override fun onRegisterExtensions(context: OperationContext) {
        context.registerJavaExtension("Method.GenerateObject", Companion::generateObject);
    }
}