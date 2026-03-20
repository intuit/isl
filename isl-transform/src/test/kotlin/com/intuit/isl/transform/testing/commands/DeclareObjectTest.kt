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
        fun conditionalObjectStatement(): Stream<Arguments> {
            return Stream.of(
                // condition true - both properties are merged
                Arguments.of(
                    "\$result: {\n" +
                            "    always: true,\n" +
                            "    if ( 1 > 0 ) { prop1: \"123\", prop2: \"abcd\" } endif\n" +
                            "}\n" +
                            "result: \$result",
                    """{"result":{"always":true,"prop1":"123","prop2":"abcd"}}""", null
                ),
                // condition false - properties are not added
                Arguments.of(
                    "\$result: {\n" +
                            "    always: true,\n" +
                            "    if ( 1 > 2 ) { prop1: \"123\", prop2: \"abcd\" } endif\n" +
                            "}\n" +
                            "result: \$result",
                    """{"result":{"always":true}}""", null
                ),
                // with else branch - true path
                Arguments.of(
                    "\$isPremium: true;\n" +
                            "\$result: {\n" +
                            "    id: \"u1\",\n" +
                            "    if ( \$isPremium ) { tier: \"premium\", limit: 1000 } else { tier: \"free\", limit: 10 } endif\n" +
                            "}\n" +
                            "result: \$result",
                    """{"result":{"id":"u1","tier":"premium","limit":1000}}""", null
                ),
                // with else branch - false path
                Arguments.of(
                    "\$isPremium: false;\n" +
                            "\$result: {\n" +
                            "    id: \"u1\",\n" +
                            "    if ( \$isPremium ) { tier: \"premium\", limit: 1000 } else { tier: \"free\", limit: 10 } endif\n" +
                            "}\n" +
                            "result: \$result",
                    """{"result":{"id":"u1","tier":"free","limit":10}}""", null
                ),
                // multiple conditional object statements
                Arguments.of(
                    "\$hasDates: true;\n" +
                            "\$isAdmin: false;\n" +
                            "\$result: {\n" +
                            "    id: \"u1\",\n" +
                            "    if ( \$hasDates ) { start: \"2024-01-01\", end: \"2024-12-31\" } endif,\n" +
                            "    if ( \$isAdmin ) { role: \"admin\", permissions: \"all\" } endif\n" +
                            "}\n" +
                            "result: \$result",
                    """{"result":{"id":"u1","start":"2024-01-01","end":"2024-12-31"}}""", null
                ),
                // without endif (optional)
                Arguments.of(
                    "\$result: {\n" +
                            "    always: true,\n" +
                            "    if ( 1 > 0 ) { prop1: \"yes\" }\n" +
                            "}\n" +
                            "result: \$result",
                    """{"result":{"always":true,"prop1":"yes"}}""", null
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

        @JvmStatic
        @Suppress("unused")
        fun selfSpreadOptimization(): Stream<Arguments> {
            return Stream.of(
                // Object: add a single new property via self-spread
                Arguments.of(
                    "\$var: { a: 1, b: 2 };\n" +
                            "\$var = { ...\$var, c: 3 };\n" +
                            "result: \$var",
                    """{"result":{"a":1,"b":2,"c":3}}""", null
                ),
                // Object: override an existing property via self-spread
                Arguments.of(
                    "\$var: { a: 1, b: 2 };\n" +
                            "\$var = { ...\$var, b: 99 };\n" +
                            "result: \$var",
                    """{"result":{"a":1,"b":99}}""", null
                ),
                // Object: self-spread only (no additional properties) — equivalent to a shallow copy
                Arguments.of(
                    "\$var: { a: 1, b: 2 };\n" +
                            "\$var = { ...\$var };\n" +
                            "result: \$var",
                    """{"result":{"a":1,"b":2}}""", null
                ),
                // Object: self-spread with multiple new properties
                Arguments.of(
                    "\$var: { a: 1 };\n" +
                            "\$var = { ...\$var, b: 2, c: 3 };\n" +
                            "result: \$var",
                    """{"result":{"a":1,"b":2,"c":3}}""", null
                ),
                // Object: self-spread applied in a loop — accumulate properties
                Arguments.of(
                    "\$items: [ \"x\", \"y\", \"z\" ];\n" +
                            "\$acc: {};\n" +
                            "foreach \$item in \$items\n" +
                            "    \$acc = { ...\$acc, `\$item`: true }\n" +
                            "endfor\n" +
                            "result: \$acc",
                    """{"result":{"x":true,"y":true,"z":true}}""", null
                ),
                // Object: second spread is a different variable — only first is self-spread optimized
                Arguments.of(
                    "\$base: { a: 1 };\n" +
                            "\$extra: { c: 3 };\n" +
                            "\$base = { ...\$base, ...\$extra, d: 4 };\n" +
                            "result: \$base",
                    """{"result":{"a":1,"c":3,"d":4}}""", null
                ),
                // Object: self-spread when variable does not yet exist — behaves like a fresh object
                Arguments.of(
                    "\$var = { ...\$var, a: 1 };\n" +
                            "result: \$var",
                    """{"result":{"a":1}}""", null
                ),
                // Array: append a single item via self-spread
                Arguments.of(
                    "\$a: [ 1, 2, 3 ];\n" +
                            "\$a = [ ...\$a, 4 ];\n" +
                            "result: \$a",
                    """{"result":[1,2,3,4]}""", null
                ),
                // Array: append multiple items via self-spread
                Arguments.of(
                    "\$a: [ 1, 2 ];\n" +
                            "\$a = [ ...\$a, 3, 4, 5 ];\n" +
                            "result: \$a",
                    """{"result":[1,2,3,4,5]}""", null
                ),
                // Array: concat another array via self-spread
                Arguments.of(
                    "\$a: [ 1, 2 ];\n" +
                            "\$b: [ 3, 4 ];\n" +
                            "\$a = [ ...\$a, ...\$b ];\n" +
                            "result: \$a",
                    """{"result":[1,2,3,4]}""", null
                ),
                // Array: self-spread only — equivalent to a shallow copy
                Arguments.of(
                    "\$a: [ 10, 20 ];\n" +
                            "\$a = [ ...\$a ];\n" +
                            "result: \$a",
                    """{"result":[10,20]}""", null
                ),
                // Array: self-spread applied in a loop — accumulate items
                Arguments.of(
                    "\$nums: [ 1, 2, 3 ];\n" +
                            "\$acc: [];\n" +
                            "foreach \$n in \$nums\n" +
                            "    \$acc = [ ...\$acc, \$n ]\n" +
                            "endfor\n" +
                            "result: \$acc",
                    """{"result":[1,2,3]}""", null
                ),
                // Array: self-spread when variable does not yet exist — behaves like a fresh array
                Arguments.of(
                    "\$a = [ ...\$a, 1, 2 ];\n" +
                            "result: \$a",
                    """{"result":[1,2]}""", null
                ),
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
        "arrayAssignment",
        "conditionalObjectStatement",
        "selfSpreadOptimization"
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