package com.intuit.isl.transform.testing.commands

import com.intuit.isl.parser.TransformParser
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertContains

class ParseIslTest {
    companion object {
        @JvmStatic
        fun basicStatements(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("description: \$value;", "[Assign] description : [Select] \$value;"),

                Arguments.of(
                    "description: \$value;\n" +
                    "description2: \$value.property.property2;\n" +
                    "someValue: 123;\n" +
                    "amount: \$value | trim | to_decimal | round(4)\n",
                    "[Assign] description : [Select] \$value; [Assign] description2 : [Select] \$value -> property.property2; [Assign] someValue : [Val] `123`; [Assign] amount : [Select] \$value | [M:]trim() | [M:]to_decimal() | [M:]round([Val] `4`);"
                ),

                Arguments.of(
                    "description.property1: \$value;\n" +
                    "description.property2: 123",
                    "[Assign] description : [Object] { [Assign] property1 : [Select] \$value; }; [Assign] description : [Object] { [Assign] property2 : [Val] `123`; };"
                ),

                Arguments.of(
                    "feeAmount: \$tenders | reduce ( {{ \$acc + (\$it.processing_fee_money.amount / 100) }} )",
                    "[Assign] feeAmount: [Select] \$tenders | [M:]reduce(( [Select] \$acc + ( [Select] \$it -> processing_fee_money.amount / [Val] `100` ) ));\n"
                )
            )
        }

        @JvmStatic
        fun variableStatements(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "\$var: 1223;",
                    "[Var] \$var : [Val] `1223`;"
                )
            )
        }

        @JvmStatic
        fun mathStatements(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "description: {{ 123 + 456 }}",
                    "[Assign] description : ( [Val] `123` + [Val] `456` );"
                ),

                Arguments.of(
                    "description: {{ 123 + 456 + 678 - 234 }}",
                    "[Assign] description : ( ( ( [Val] `123` + [Val] `456` ) + [Val] `678` ) - [Val] `234` );"
                ),

                Arguments.of(
                    "description: {{ 6 * (123 + 456) }}",
                    "[Assign] description : ( [Val] `6` * ( [Val] `123` + [Val] `456` ) );"
                ),
                Arguments.of(
                    "description: {{ (123 + 456) * 6 }}",
                    "[Assign] description : ( ( [Val] `123` + [Val] `456` ) * [Val] `6` );"
                )
            )
        }
    }

    @ParameterizedTest
    @MethodSource(
        "variableStatements","basicStatements", "mathStatements"
    )
    fun loadAndValidate(script: String, expectedResult: String) {
        val token = TransformParser().parseTransform("unittest.xform", script);

        println("Loading:\n$script");

        println("Expected:\n$expectedResult");
        println("Result:\n$token");

        // remove \n
        val result = token.toString().trim().replace("\n", "").replace(" ", "");

        assertContains(result, expectedResult.replace("\n", "").replace(" ", ""));
    }
}