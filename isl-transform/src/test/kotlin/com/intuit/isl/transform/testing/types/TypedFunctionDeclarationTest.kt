package com.intuit.isl.transform.testing.types

import com.intuit.isl.transform.testing.commands.BaseTransformTest
import com.intuit.isl.types.IslObjectType
import com.intuit.isl.types.toJsonSchema
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Let's play with declaring reusable functions inline. Reusable functions can be called via
 * @.This.Name ( parameters ) and they always return a value
 */
@Suppress("unused")
class TypedFunctionDeclarationTest : BaseTransformTest() {
    companion object {
        @JvmStatic
        fun functionDeclarations(): Stream<Arguments> {
            return Stream.of(

                Arguments.of(
                    // main entry point is the run function
                    "fun run( \$v: number, \$a: [], \$b: string[], \$c: { FirstName: string, LastName: string } ): string[] {\n" +
                            "    result: {{ \$v * 12 }}\n" +
                            "}\n",
                    """{ "result": 60 } """,
                    mapOf("v" to 5)
                ),

                Arguments.of(
                    // main entry point is the run function
                    "type customer as { FirstName: string };\n" +
                    "" +
                    "fun run( \$c: customer, \$a: customer[] ): customer[] {\n" +
                    "    result: {{ \$v * 12 }}\n" +
                    "}\n",
                    """{ "result": 60 } """,
                    mapOf("v" to 5)
                ),

                Arguments.of(
                    "type Request as { invoiceId: String };\n" +
                    "\n" +
                    "fun run( \$input: Request ){ \n" +
                    "    @.Log.Info(`Received \$input`);\n" +
                    "    return { result: {{ \$v * 12 }} };\n" +
                    "}",
                    """{ "result": 60 } """,
                    mapOf("v" to 5)
                ),

                Arguments.of(
                    // main entry point is the run function
                    // the metadata registry does not yet allow us to refer to a schema directly - but that's ok as we don't validate it anyway
                    "type customer from \"https://\";\n" +
                    "" +
                    "fun run( \$c: customer, \$a: customer[] ): customer[] {\n" +
                    "    result: {{ \$v * 12 }}\n" +
                    "}\n",
                    """{ "result": 60 } """,
                    mapOf("v" to 5)
                ),

//                Arguments.of(
//                        // direct from a schema
//                        "type customer as {\n" +
//                                "    \"event_type\": {\n" +
//                                "      \"description\": \"CREATE or UPDATE\",\n" +
//                                "      \"type\": \"string\",\n" +
//                                "      \"intuitDataClassification\": \"PUBLIC\"\n" +
//                                "    },\n" +
//                                "    \"realm_id\": {\n" +
//                                "      \"description\": \"Realm ID\",\n" +
//                                "      \"intuitDataClassification\": \"PUBLIC\",\n" +
//                                "      \"type\": \"string\"\n" +
//                                "    }\n" +
//                                "" +
//                                "fun run( \$c: customer, \$a: customer[] ): customer[] {\n" +
//                                "    result: {{ \$v * 12 }}\n" +
//                                "}\n",
//                """{ "result": 60 } """,
//                mapOf("v" to 5)
//            )
            )
        }
    }

    @ParameterizedTest
    @MethodSource(
        "functionDeclarations",
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        val result = run(script, expectedResult, map).first;

        val func = result.module.functions.first{ it.name == "run" };

        val signature = func.token.arguments;
        val type = IslObjectType("", signature);

        val schema = type.toJsonSchema();

        println("Schema:\n$schema");
    }
}