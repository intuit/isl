package com.intuit.isl.transform.testing.types

import com.intuit.isl.transform.testing.commands.BaseTransformTest
import com.intuit.isl.types.IslObjectType
import com.intuit.isl.types.toJsonSchema
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Suppress("unused")
class JsonSchemaTest : BaseTransformTest() {
    companion object {
        @JvmStatic
        fun functionDeclarations(): Stream<Arguments> {
            return Stream.of(

                Arguments.of(
                    "fun run( \$v: number ) {\n" +
                            "    result: {{ \$v * 12 }}\n" +
                            "}\n",
                    """{ "result": 60 } ""","{\"\$v\":{\"type\":\"number\"}}"
                ),

                // all basic types
                Arguments.of(
                    "fun run( \$s: string, \$n: number, \$i: integer, \$b: boolean, \$o: object, \$a: any ) {\n" +
                            "    result: {{ \$v * 12 }}\n" +
                            "}\n",
                    """{ "result": 60 } ""","{\"\$s\":{\"type\":\"string\"},\"\$n\":{\"type\":\"number\"},\"\$i\":{\"type\":\"integer\"},\"\$b\":{\"type\":\"boolean\"},\"\$o\":{\"type\":\"object\"},\"\$a\":{\"type\":\"object\"}}\n"
                ),

                // extended types
                Arguments.of(
                    "fun run( \$t: text, \$d: date, \$dt: datetime ) {\n" +
                            "    result: {{ \$v * 12 }}\n" +
                            "}\n",
                    """{ "result": 60 } ""","{\"\$t\":{\"type\":\"string\",\"format\":\"multiLine\"},\"\$d\":{\"type\":\"string\",\"format\":\"date\"},\"\$dt\":{\"type\":\"string\",\"format\":\"date-time\"}}"
                ),

                Arguments.of(
                    "fun run( \$c: { FirstName: string, LastName: string } ) {\n" +
                            "    result: {{ \$v * 12 }}\n" +
                            "}\n",
                    """{ "result": 60 } ""","{\"\$c\":{\"properties\":{\"FirstName\":{\"type\":\"string\"},\"LastName\":{\"type\":\"string\"}},\"type\":\"object\"}}"
                ),


                Arguments.of(
                    // main entry point is the run function
                    "fun run( \$method: [\"GET\", \"POST\"] ): string[] {\n" +
                            "    result: {{ \$v * 12 }}\n" +
                            "}\n",
                    """{ "result": 60 } ""","{\"\$method\":{\"type\":\"string\",\"default\":\"GET\",\"enum\":[\"GET\",\"POST\"]}}"
                ),

                Arguments.of(
                    "fun run( \$c: { FirstName: string, LastName: string }[] ): customer[] {\n" +
                    "    result: {{ \$v * 12 }}\n" +
                    "}\n",
                    """{ "result": 60 } ""","{\"\$c\":{\"type\":\"array\",\"items\":{\"properties\":{\"FirstName\":{\"type\":\"string\"},\"LastName\":{\"type\":\"string\"}},\"type\":\"object\"}}}"
                )
            )
        }
    }

    @ParameterizedTest
    @MethodSource(
        "functionDeclarations",
    )
    fun runFixtures(script: String, expectedResult: String, expectedSchema: String) {
        val result = run(script, expectedResult, mapOf("v" to 5)).first;

        val func = result.module.functions.first{ it.name == "run" };

        val signature = func.token.arguments;
        val type = IslObjectType("", signature);

        val schema = type.toJsonSchema().get("properties");

        println("Schema:\n$schema");
        compareJsonResults(expectedSchema, schema);
    }
}