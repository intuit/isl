package com.intuit.isl.transform.testing.commands

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Let's play with declaring reusable functions inline. Reusable functions can be called via
 * @.This.Name ( parameters ) and they always return a value
 */
@Suppress("unused")
class FunctionDeclarationTest : BaseTransformTest() {
    companion object {
        @JvmStatic
        fun functionDeclarations(): Stream<Arguments> {
            val obj1 =  "{\n" +
                    "        name: \"test\",\n" +
                    "        value: 1\n" +
                    "    }\n";

            val obj2 =  "{\n" +
                    "        value: 1,\n" +
                    "        name: \"test\"\n" +
                    "    }\n";

            return Stream.of(

                Arguments.of(
                    // main entry point is the run function
                    "fun run( \$v ){\n" +
                            "    result: {{ \$v * 12 }}\n" +
                            "}\n",
                    """{ "result": 60 } """,
                    mapOf("v" to 5)
                ),

                Arguments.of(
                    "fun demo( \$tax ) {\n" +
                            "   \$val: {{ \$tax * 5 }};\n" +   // return the tax calculation
                            "   return \$val;\n" +
                            "}\n" +
                            // main entry point is the run function
                            "fun run(){\n" +
                            "    result: @.This.Demo( 12 )\n" +
                            "}\n",
                    """{ "result": 60 } """,
                    null
                ),

                Arguments.of(
                    "fun child( \$tax ) {\n" +
                            "   \$val: {{ \$tax * 5 }};\n" +   // return the tax calculation
                            "   return \$val;\n" +
                            "}\n" +
                            // main entry point is the run function
                            "fun run( \$v1, \$v2 ){\n" +
                            "   return {\n" +
                            "       result: @.This.child( \$v1 ),\n" +
                            "       v2result: \$v2,\n" +
                            "}\n" +
                            "   }\n",
                    """{ "result": 25, "v2result": 6 } """,
                    mapOf("v1" to 5, "v2" to 6)
                ),

                // let's make sure we don't leak external variables when calling methods
                Arguments.of(
                    "fun child( \$tax ) {\n" +
                            "   return {\n" +
                            "       tax: \$tax,\n" +
                            "       v1: \$v1,\n" +  // these should be null as they are not coming across
                            "       v2: \$v2,\n" +
                            "   };\n" +
                            "}\n" +
                            // main entry point is the run function
                            "fun run( \$v1, \$v2 ){\n" +
                            "    result: @.This.child( \$v1 );\n" +
                            "}\n",
                    """{"result":{"tax":5,"v1":null,"v2":null}}""",
                    mapOf("v1" to 5, "v2" to 6)
                ),

                // how deep can we go - not we have no protection ATM not even against recursion!
                Arguments.of(
                    "fun child1( \$p1 ) {\n" +
                            "    \$v: @.This.Child2( 2 );\n" +
                            "    return `child1:\$p1 \$v`;\n" +
                            "}\n" +

                            "fun child2( \$p2 ) {\n" +
                            "    return `child2:\$p2`;\n" +
                            "}\n" +

                            // main entry point is the run function
                            "fun run( ){\n" +
                            "    result: @.This.child1( 1 );\n" +
                            "}\n",
                    """{"result":"child1:1 child2:2"}""",
                    mapOf("v1" to 5, "v2" to 6)
                ),
            )
        }


        @JvmStatic
        fun modifierDeclarations(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "modifier name( \$name ) {\n" +
                            "   return `Hi \$name`;\n" +
                            "}\n" +
                            "fun run( \$name ){\n" +
                            "    result: \$name | name;\n" +
                            "}\n",
                    """{ "result": "Hi " } """,
                    null    // null arguments
                ),
                Arguments.of(
                    "modifier name( \$name ) {\n" +
                            "   return `Hi \$name`;\n" +
                            "}\n" +
                            "fun run( \$name ){\n" +
                            "    result: \$name | name;\n" +
                            "}\n",
                    """{ "result": "Hi " } """,
                    mapOf("name" to null)    // null arguments
                ),
                Arguments.of(
                    "modifier name( \$name ) {\n" +
                            "   return `Hi \$name`;\n" +
                            "}\n" +
                            "fun run( \$name ){\n" +
                            "    result: \$name | name;\n" +
                            "}\n",
                    """{ "result": "Hi George" } """,
                    mapOf("name" to "George")
                ),

                Arguments.of(
                    "modifier gst( \$tax ) {\n" +   // create a GST modifier
                            "   \$val: {{ \$tax * 0.1 }};\n" +   // return the GST
                            "   return \$val;\n" +
                            "}\n" +
                            // main entry point is the run function
                            "fun run( \$value ){\n" +
                            "    result: \$value | gst;\n" +   // call the GST modifier
                            "}\n",
                    """{ "result": 9.0 } """,
                    mapOf("value" to 90)
                ),

                Arguments.of(
                    "modifier gst( \$tax, \$percent ) {\n" +   // create a GST modifier with extra parameters
                            "   \$val: {{ \$tax * \$percent / 100 }};\n" +   // return the GST
                            "   return \$val;\n" +
                            "}\n" +
                            // main entry point is the run function
                            "fun run( \$value ){\n" +
                            "    result: \$value | gst( 12.5 );\n" +   // call the GST to calculate 12.5%
                            "}\n",
                    """{ "result": 11.25 } """,
                    mapOf("value" to 90)
                ),

                Arguments.of(
                    "modifier gst( \$tax, \$percent ) {\n" +   // create a GST modifier with extra parameters
                            "   return null;\n" +
                            "}\n" +
                            // main entry point is the run function
                            "fun run( \$value ){\n" +
                            "    result: \$value | gst( 12.5 );\n" +   // call the GST to calculate 12.5%
                            "}\n",
                    """{ "result": null } """,
                    mapOf("value" to 90)
                ),

                // TBD: Make unit tests failing on errors
//                Arguments.of(
//                    "modifier name( \$name ) {\n" +
//                            "   return `Hi \$name`;\n" +
//                            "}\n" +
//                    "fun name( \$name ){\n" +
//                    "    result: \$name | name;\n" +
//                    "}\n",
//                    """{ "result": "Hi George" } """,
//                    mapOf("name" to "George")
//                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource(
        "functionDeclarations",
        "modifierDeclarations"
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        run(script, expectedResult, map);
    }
}