package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.common.OperationContext
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.run

@Suppress("unused")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeclareObjectTest : YamlTransformTest("transform") {
    companion object {
        @JvmStatic
        fun basicAssignment(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "result.prop1 = 123\n" +
                    "result.prop2 = \"abc\"",
                    """{"result":{"prop1":123, "prop2": "abc"}}""",
                    null
                ),



                Arguments.of("\$var: { description: 123 }; result: \$var", """{"result":{"description":123}}""", null),
                Arguments.of(
                    "\$var: { description: 123, \"custom-field\": true }; result: \$var",
                    """{"result":{"description":123, "custom-field": true}}""",
                    null
                ),
                // extra "," at the end to be json friendly
                Arguments.of(
                    "\$var: { description: 123, \"custom-field\": true, }; result: \$var",
                    """{"result":{"description":123, "custom-field": true}}""",
                    null
                ),
                Arguments.of(
                    "\$array: [ 1, 2, 3, 4 ];\n" +
                            "\$var: {\n" +
                            "   description: 123,\n" +
                            "   \"custom-field\": {{ 1 + 2 + 3 }},\n" +
                            "};\n" +
                            "result: \$var", """{"result":{"description":123, "custom-field": 6}}""", null
                ),
                Arguments.of(
                    "\$array: [ 1, 2, 3, 4 ];\n" +
                            "\$var: {\n" +
                            "   description: 123,\n" +  // inline if
                            "   \"custom-field\": if( 1 > 2 ) true else false,\n" +
                            "};\n" +
                            "result: \$var", """{"result":{"description":123, "custom-field": false}}""", null
                ),
                Arguments.of(
                    "\$array: [ 1, 2, 3, 4 ];\n" +
                            "\$var: {\n" +
                            "   description: 123,\n" +  // inline if - no output property
                            "   \"custom-field\": if( 1 > 2 ) true,\n" +
                            "};\n" +
                            "result: \$var", """{"result":{"description":123}}""", null
                ),

                Arguments.of(
                    "\$t: 3;\n" +
                            "\$var: {\n" +
                            "   description: 123,\n" +  // inline if - no output property
                            "   \"switched\": switch( \$t ) \n" +
                            "       1 -> \"a\";\n" +
                            "       2 -> \"b\";\n" +
                            "       3 -> \"c\";\n" +
                            "    endswitch\n" +
                            "};\n" +
                            "result: \$var", """{"result":{"description":123, "switched": "c"}}""", null
                ),

                // embedded forloop
                Arguments.of(
                    "\$array: [ 1, 2, 3, 4 ];\n" +
                            "\$var: {\n" +
                            "   description: 123,\n" +
                            "   \"custom-field\": true,\n" +
                            "    \"the-lines\": foreach \$i in \$array {\n" +
                            "        id: \$i, \n" +
                            "        id2: {{ \$i * 10 }}\n" +
                            "    }\n" +
                            "    endfor\n" +
                            "};\n" +
                            "result: \$var",
                    """{"result":{"description":123, "custom-field": true, "the-lines":[{"id":1,"id2":10},{"id":2,"id2":20},{"id":3,"id2":30},{"id":4,"id2":40}]}}""",
                    null
                ),
            )
        }

        @JvmStatic
        fun dynamicAssignment(): Stream<Arguments> {
            // dynamic build of property names
            return Stream.of(
                Arguments.of(
                    "\$name: 'prop-name';" +
                            "\$var: { " +
                            "`\$name`: 123 " +
                            "}; result: \$var",
                    """{"result":{"prop-name":123}}""", null
                ),

                // Dynamic Properties won't build deep objects to prop.name creates { "prop.name": value } and not { prop: { name: value } }
                Arguments.of(
                    "\$name: 'prop.name';" +
                            "\$var: { `\$name`: 123, \"custom-field\": true }; result: \$var",
                    """{"result":{"prop.name": 123, "custom-field": true}}""",
                    null
                ),
                // extra "," at the end to be json friendly
                Arguments.of(
                    "\$var: { description: 123, \"custom-field\": true, }; result: \$var",
                    """{"result":{"description":123, "custom-field": true}}""",
                    null
                ),
                // create a var with dynamic property names
                Arguments.of(
                    "\$array: [ 1, 2, 3, 4 ];\n" +
                            "foreach \$n in \$array\n" +
                            "\$var: { ...\$var," +
                            "`name-\$n`: 123,\n" +
                            "}\n" +
                            "endfor\n" +
                            "result: \$var",
                    """{"result":{"name-1":123,"name-2":123,"name-3":123,"name-4":123}}""",
                    null
                )
            )
        }

        @JvmStatic
        fun spreadAssignment(): Stream<Arguments> {
            return Stream.of(
                // spread
                Arguments.of(
                    "\$o: { a: 1, b: 2 }\n" +
                            "result: { ...\$o }",
                    """{ "result": { "a": 1, "b": 2} }""", null
                ),
                Arguments.of(
                    "\$o: { a: 1 }\n" +
                            "result: { ...\$o, b:2 }",
                    """{ "result": { "a": 1, "b": 2} }""", null
                ),
                Arguments.of(
                    "\$a: { a: 1 };\n" +
                            "\$b: { b: 1, c: 2 };\n" +
                            "result: { ...\$a, ...\$b, d:3 }",
                    """{ "result": {"a":1,"b":1,"c":2,"d":3} }""", null
                ),

                // order
                Arguments.of(
                    "\$a: { a: 1 };\n" +
                            "\$b: { a: 2 };\n" +
                            "result: { ...\$a, ...\$b }",
                    """{ "result": {"a":2} }""", null
                ),

                // order
                Arguments.of(
                    "\$a: { a: 1, c: 3 };\n" +
                            "\$b: { a: 2, b: 2 };\n" +
                            "result: { ...\$a, ...\$b } | delete ( 'b' )",
                    """{ "result": {"a":2, "c": 3} }""", null
                ),

                // function
                Arguments.of(
                    "fun run() {\n" +
                            "   \$o: { a: 1, b: 2 }\n" +
                            "   return { ...@.This.GetResult(), ...\$o }\n" +
                            "}\n" +
                            "fun getResult() {\n" +
                            "   return { child: true }\n" +
                            "}\n",
                    """{"child":true,"a":1,"b":2}""", null
                ),
            )
        }

        @JvmStatic
        fun arrayAssignment(): Stream<Arguments> {
            return Stream.of(
                // direct variable & property assignments
                Arguments.of(
                    "\$a: [ 1, 2, 3]\n" +
                            "result: \$a",
                    """{ "result": [ 1, 2, 3] }""", null
                ),
                Arguments.of(
                    "\$a: [ 1, 2, 3];\n" +
                            "\$a1: \$a;\n" +
                            "result: \$a1",
                    """{ "result": [ 1, 2, 3] }""", null
                ),
                Arguments.of(
                    "\$a: [ 1, 2, 3];\n" +
                            "\$a1: [ 1, 2 ];\n" +
                            "\$a1: \$a;\n" +    // this is assigning
                            "result: \$a1",
                    """{ "result": [ 1, 2, 3] }""", null
                ),


                // validate ConcurrentModificationException
                // this is a normal assign & append due to pushITems
                Arguments.of(
                    "\$c: { list: [ 1, 2, 3] };\n" +
                            "\$c1: [ 4, 5 ];\n" +
                            "\$c.list = \$c.list | pushItems( \$c1 )\n" +
                            "result: \$c",
                    """{ "result": { "list": [ 1, 2, 3, 4, 5] } }""", null
                ),


                // append behaviour of arrays when used as child properties
                Arguments.of(
                    "\$a: { list: [ 1, 2, 3] }\n" +
                            "\$a1: [ 4, 5 ];\n" +
                            "\$a.list: \$a1;\n" +    // this is doing an append
                            "result: \$a",
                    """{ "result": { "list": [ 1, 2, 3, 4, 5] } }""", null
                ),
                Arguments.of(
                    "\$a: { b: { c: [ 1, 2, 3] } }\n" +
                            "\$a1: [ 4, 5 ];\n" +
                            "\$a.b.c: \$a1;\n" +    // this is doing an append
                            "result: \$a",
                    """{ "result": { "b": { "c": [ 1, 2, 3, 4, 5] } } }""", null
                ),
                Arguments.of(
                    "\$a: { list: [ 1, 2, 3] }\n" +
                            "\$a1: [ 4, 5 ];\n" +
                            "\$a.list: null;\n" +    // this is resetting the array
                            "\$a.list: \$a1;\n" +    // this is doing an assign because the prev value was null
                            "result: \$a",
                    """{ "result": { "list": [ 4, 5 ] } }""", null
                )
            )
        }
    }


    private fun readonlyVarsFixture(): Stream<Arguments> {
        return createTests("readonly-vars-fixture")
    }

    @ParameterizedTest
    @MethodSource(
        "basicAssignment",
        "dynamicAssignment",
        "spreadAssignment",
        "arrayAssignment"
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        run(script, expectedResult, map);
    }

    @ParameterizedTest
    @MethodSource(
        "readonlyVarsFixture"
    )
    fun runFixtures(testName: String, script: String, expectedResult: JsonNode, map: Map<String, Any?>? = null) {
        run(script, expectedResult.toPrettyString(), map);
    }

    override fun onRegisterExtensions(context: OperationContext) {
        context.registerExtensionMethod("Headers.Get", ExpressionRunTest::configGet);
    }
}