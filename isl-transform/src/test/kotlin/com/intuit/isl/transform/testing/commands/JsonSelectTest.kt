package com.intuit.isl.transform.testing.commands

import com.intuit.isl.common.OperationContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.run

@Suppress("unused")
class JsonSelectTest : BaseTransformTest() {
    companion object {
        @JvmStatic
        fun arraySelect(): Stream<Arguments> {
            val testArray =  "[ {\n" +
                    "        id: \"1\",\n" +
                    "        first: \"steve\",\n" +
                    "        last: \"mendoza\"\n" +
                    "    }, {\n" +
                    "        id: \"2\",\n" +
                    "        first: \"steven\",\n" +
                    "        last: \"mendoza\"\n" +
                    "    } ]\n";


            return Stream.of(
                Arguments.of(
                    "\$in: [ 1, 2, 3 ];\n" +
                            "result: \$in[0]", """{ "result": 1 }""", null
                ), // number

                Arguments.of(
                    "\$in: [ 1, 2, 3 ];\n" +
                            "result: \$in[2]", """{ "result": 3 }""", null
                ), // number

                // Conditional Json Path
                Arguments.of(
                    "\$in: [ 1, 2, 3 ];\n" +
                            "result: \$in | select ( '$.[?(@ > 1)]' )", """{ "result": [2, 3] }""", null
                ), // number


                Arguments.of(
                    "\$in: [{ a: 1}, {a:2}, {a: 3}]; " +
                            "result: \$in[( $.a > 1 )]",
                    """{"result":[{"a":2},{"a":3}]}""", null
                ), // number

                Arguments.of(
                    "\$in: { test: [{ a: 1}, {a:2}, {a: 3}] };\n" +
                            "result: \$in.test[( $.a > 1 )]",
                    """{"result":[{"a":2},{"a":3}]}""", null
                ), // number

                Arguments.of(
                    "\$in: { test: [{ a: 1}, {a:2}, {a: 3}] };\n" +
                            "result: \$in.test[( $.a > 1 )] | select ( '$[*].a' )",
                    """{"result":[ 2, 3]}""", null
                ),

                Arguments.of(
                    "\$in: { test: [{ a: 1 }, { a: 2 }, { a: 3 }] };\n" +
                    "result: \$in.test[( $.a > 1 )] | at(0) | select ( '$.a' )",
                    """{"result": 2}""", null
                ),


                // https://intuit-teams.slack.com/archives/C02B7U753SM/p1647907010718459
                Arguments.of(
                    "\$people: $testArray;\n" +
                    "result: \$people[( $.id == '1' )]",
                    """{"result": [{"id":"1","first":"steve","last":"mendoza"}]}""", null
                ),

                Arguments.of(
                    "\$people: $testArray;\n" +
                    "\$index: 1;\n" +
                    "result: \$people[( $.id == \$index )]",
                    """{"result": [{"id":"1","first":"steve","last":"mendoza"}]}""", null
                ),

                Arguments.of(
                    "\$people: $testArray;\n" +
                    "\$index: 2;\n" +
                    "result: \$people[( $.id == \$index )]",
                    """{"result": [{"id":"2","first":"steven","last":"mendoza"}]}""", null
                ),

                Arguments.of(
                    "\$people: $testArray;\n" +
                            "\$index: 0;\n" +
                            "result: \$people[( $.id < \$index and $.first endsWith 'n' )]",
                    """{"result": []}""", null
                ),

                Arguments.of(
                    "\$people: $testArray;\n" +
                            "\$index: 0;\n" +
                            "result: \$people[( $.id > \$index and $.first endsWith 'n' )]",
                    """{"result": [{"id":"2","first":"steven","last":"mendoza"}]}""", null
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource(
        "arraySelect"
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        run(script, expectedResult, map);
    }

    override fun onRegisterExtensions(context: OperationContext) {
    }
}