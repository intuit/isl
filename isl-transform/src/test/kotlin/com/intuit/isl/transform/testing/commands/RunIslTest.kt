package com.intuit.isl.transform.testing.commands

import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Suppress("unused")
class RunIslTest : BaseTransformTest() {
    companion object {
        @JvmStatic
        fun basicAssignment(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("description: 123;", """{ "description": 123 }""", null), // number
                Arguments.of("description: \"123\"", """{ "description": "123" }""", null),    // string
                Arguments.of("description: true", """{ "description": true }""", null),    // bool

                // Unknown Modifier
                // TODO: Generate an error? Would break future extensibility or dynamically added modifiers
                Arguments.of(
                    "description: 123 | unknown",
                    """{ "description": "Unknown Modifier: unknown" }""", null
                ),
                Arguments.of("description: 123 | negate", """{ "description": -123 }""", null),    // modifier
                Arguments.of("description: 0 | negate", """{ "description": 0 }""", null),    // modifier

                // precision
                Arguments.of("value: 123 | precision", """{ "value": 123.0 }""", null),
                Arguments.of("value: 123.12 | precision", """{ "value": 123.12 }""", null),
                Arguments.of("value: 123.123 | precision", """{ "value": 123.12 }""", null),
                Arguments.of("value: 123.126 | precision", """{ "value": 123.13 }""", null),
                Arguments.of("value: 123.126 | precision(1)", """{ "value": 123.1 }""", null),
                Arguments.of("value: 123.126 | precision(-1)", """{ "value": 123 }""", null),

                // MOD
                Arguments.of("value: @.Math.Mod( 123, 2 )", """{ "value": 1 }""", null),
                Arguments.of("value: @.Math.Mod( 155, 12 )", """{ "value": 11 }""", null),
                Arguments.of("value: @.Math.sqrt( 64 )", """{ "value": 8.0 }""", null),


                Arguments.of(
                    "description.prop1.prop2.prop3: 123;",
                    """{ "description": { "prop1": { "prop2": { "prop3" : 123 } } } }""", null
                ),    // deep hierarchy

                // custom property name
                //Arguments.of("description.[ 1 ]: true", """{ "description": { "x-abc": true } }""", null),
                Arguments.of("description.[\"x-abc\"]: true", """{ "description": { "x-abc": true } }""", null),

                Arguments.of(
                    "description.prop1: 123 | negate;\n" +
                    "description.prop2: \"abc\";",
                    """{ "description": { "prop1": -123, "prop2": "abc" } }""", null
                ),    // deep hierarchy

                Arguments.of(
                    "description._prop1: 123 | negate;\n" +
                            "description.prop2: \"abc\";",
                    """{ "description": { "_prop1": -123, "prop2": "abc" } }""", null
                ),    // deep hierarchy


                Arguments.of("description: \$value", """{ "description": null }""", null),
                Arguments.of(
                    "description: \$value", """{ "description": 123 }""", mapOf(
                        "value" to 123
                    ), null
                ),

                Arguments.of(
                    "description: \$value.property", """{ "description": 123 }""", mapOf(
                        "value" to object {
                            val property = 123
                        })
                ),

                Arguments.of("description: \$value.otherProperty", """{ "description": null }""", mapOf(
                    "value" to object {
                        val property = 123
                    }
                )))
        }

        @JvmStatic
        fun assignObjectFixture(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("description: {  }", """{ "description": {} }""", null),
                Arguments.of("description: { property : 123 }", """{ "description": { "property": 123 } }""", null),

                // ; and , as object separators
                Arguments.of(
                    "description: { property1: 123; property2: 234, someVal: \"text\", someNull: null }",
                    """{"description":{"property1":123,"property2":234,"someVal":"text","someNull":null }}""",
                    null
                ),
                // TODO: Fix Me
                //Arguments.of("description: { property1: 123, property2: 234 }", """{ "description": { "property1": 123, "property2": 234 } }""", null),


                Arguments.of(
                    "description: { d1 : { d2 : { d3 : 123 } } }",
                    """{ "description": { "d1": { "d2": { "d3" : 123 } } } }""",
                    null
                ),

                Arguments.of(
                    "description: { property: \$value }", """{ "description": { "property": 123 } }""", mapOf(
                        "value" to 123
                    )
                )
            );
        }

        @JvmStatic
        fun variableAssignment(): Stream<Arguments> {
            val request = mapOf(
                "body" to mapOf(
                    "value" to 1
                ),
                "headers" to mapOf(
                    "content-type" to "application/json"
                ),
                "items" to arrayOf(
                    mapOf(
                        "property1" to "value1",
                        "property2" to "value2"
                    )
                )
            );

            val requestJson = JsonConvert.convert(request);

            return Stream.of(
                Arguments.of("\$description: 123; result: \$description", """{ "result": 123 }""", null), // number
                Arguments.of(
                    "\$description.prop1: 123; result: \$description",
                    """{ "result": { "prop1" : 123 } }""",
                    null
                ),

                Arguments.of(
                    "result: \$request",
                    """{"result":{"body":{"value":1},"headers":{"content-type":"application/json"},"items":[{"property1":"value1","property2":"value2"}]}}""",
                    mapOf("\$request" to requestJson)
                ),

                // modify the contents of a variable!
                Arguments.of(
                    "\$request.headers.abc: 123; result: \$request",
                    """{"result":{"body":{"value":1},"headers":{"content-type":"application/json","abc":123},"items":[{"property1":"value1","property2":"value2"}]}}""",
                    mapOf("\$request" to request)
                ),

                Arguments.of(
                    "result: \$request.items[0]",
                    """{"result":{"property1":"value1","property2":"value2"}}""",
                    mapOf("\$request" to requestJson)
                ),

                Arguments.of(
                    "result: \$request.items[0].property1",
                    """{"result": "value1"}""",
                    mapOf("\$request" to requestJson)
                ),

                Arguments.of(
                    "\$description.prop1: 123;\n" +
                            "\$description.prop2: 234;" +
                            " result: \$description",
                    """{ "result": { "prop1" : 123, "prop2": 234 } }""", null
                ),
                Arguments.of(
                    "\$description.prop1: 123;\n" +
                            "\$description.prop2.prop3.prop4: 234;" +
                            "\$description.prop2.prop4.prop5: 345;" +
                            " result: \$description",
                    """{"result":{"prop1":123,"prop2":{"prop3":{"prop4":234},"prop4":{"prop5":345}}}}""", null
                ),
            )
        }

        @JvmStatic
        fun arrayFixture(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("r: []", """{ "r":[] }""", null),
                Arguments.of("r: [ 1, \"abc\", true ]", """{ "r":[ 1, "abc", true] }""", null),
                Arguments.of("r: [ 1, { prop: 123 }, true ]", """{ "r":[ 1, { "prop": 123 }, true] }""", null),

                Arguments.of(
                    "r: [ 1 | to.string, \"abc\" | upperCase, true ]",
                    """{ "r":[ "1", "ABC", true] }""",
                    null
                ),

                // spread
                Arguments.of("\$a: [ 1, 2 ];\n" +
                        "r: [ ...\$a, 3, 4 ]",
                    """{ "r":[ 1, 2, 3, 4 ] }""", null),


                // TODO: Add support for array indexing $array [ $index ]
//                Arguments.of("\$items: [ 1, 2, 3 ];\n" +
//                        "\$index = 0;\n" +
//                        "r: \$items[\$index]",
//                    """{ "r": 1 }""", null),

            )
        }

        @JvmStatic
        fun interpolateFixture(): Stream<Arguments> {
            val d = "\$";
            return Stream.of(
                Arguments.of("r: ``", """{ "r": "" }""", null),
                Arguments.of("r: `abc`", """{ "r": "abc" }""", null),
                Arguments.of("r: `abc \$var`", """{ "r": "abc " }""", null),
                Arguments.of("r: `abc \$var`", """{ "r": "abc 123" }""", mapOf("var" to 123)),

                Arguments.of("r: `abc \${ \$var | negate }`;", """{ "r": "abc -123" }""", mapOf("var" to 123)),
                Arguments.of("r: `abc \${ \$var }`", """{ "r": "abc 123" }""", mapOf("var" to 123)),

                // escaped $
                Arguments.of("r: `escaped abc \\${d}123`", """{ "r": "escaped abc $123" }""", mapOf("var" to 123)),
                Arguments.of("r: `escaped abc \\${d}var`", """{ "r": "escaped abc ${d}var" }""", mapOf("var" to 123)),

                Arguments.of("r: `escaped dollar \\$123`", """{ "r": "escaped dollar $123" }""", mapOf("var" to 123)),
                Arguments.of("r: `escaped var \\$\$var`", """{ "r": "escaped var $123456" }""", mapOf("var" to 123456)),


                Arguments.of(
                    "r: `abc \$var \$v2 final`",
                    """{ "r": "abc 123 def final" }""",
                    mapOf("var" to 123, "v2" to "def")
                ),
                Arguments.of(
                    "\$var: { id: \"1234\" };\n" +
                            "r: `abc \${\$var.id}`", """{ "r": "abc 1234" }""", mapOf("var" to 123)
                ),
                Arguments.of(
                    "    \$v = 10;\n" +
                            "    \$v2 = { n: 10 };\n" +
                            "    result: `hello there number \$v - number \${\$v2.n}`;",
                    """{"result":"hello there number 10 - number 10"}""",
                    null
                ),
                Arguments.of(
                    "    \$name = \"George\";\n" +
                            "    result: `hello \${\$name}ABC`;", """{"result":"hello GeorgeABC"}""", null
                ),
                Arguments.of(
                    "    \$name = \"'George'\";\n" +
                            "    result: `hello \${ @.String.trimAp( \$name )}ABC`;",
                    """{"result":"hello GeorgeABC"}""",
                    null
                ),
                Arguments.of(
                    "    \$name = \"'George'\";\n" +
                            "    result: `hello \${ {{ 2 * 4 }} }ABC`;", """{"result":"hello 8ABC"}""", null
                ),

                // Inline Function Calls
                Arguments.of(
                    "r: `abc \${ @.String.trim(\"  abc  \") }`;",
                    """{ "r": "abc abc" }""",
                    mapOf("var" to 123)
                ),
            )
        }

        @JvmStatic
        fun mathStatements(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "description: {{ 123.5 + 456.1235 }}", """{ "description" : 579.6235 }""", null
                ),
                Arguments.of(
                    "description: {{ 1 + 2 + 3 - 4}}", """{ "description" : 2 }""", null
                ),
                // test order of operations
                Arguments.of(
                    "description: {{ 6 * (2 + 5) }}", """{ "description" : 42 }""", null
                ),
                Arguments.of(
                    "description: {{ (123 + 456) * 6 }}", """{ "description" : 3474 }""", null
                ),
                Arguments.of(
                    "description: {{ (123 + 456) / 100 }}", """{ "description" : 5.79 }""", null
                ),
                Arguments.of(
                    "description: {{ 123 + 456 / 100 }}", """{ "description" : 127.56 }""", null
                ),
                Arguments.of(
                    "\$a: 123;\n" +
                            "\$b: 45600;\n" +
                            "result: {{ \$a + \$b / 100 }}", """{ "result" : 579.0 }""", null
                ),
                Arguments.of(
                    "\$items: [ { amount: 12300 }, { amount: 34614 } ];\n" +
                            "\$total: 0;\n" +
                            "foreach \$i in \$items\n" +
                            "   \$total = {{ \$total + \$i.amount / 100 }}\n" +
                            "endfor\n" +
                            "result: \$total",
                    """{ "result" : 469.14 }""", null
                ),
                Arguments.of(
                    "\$v: { total_amount: { value: 123 } };\n" +
                            "result: {\n" +
                            "    tax: {{ \$v.total_amount.value / 100 }},\n" +
                            "    amount: {{ \$v.total_amount.value * 10 }}\n" +
                            "}\n", """{"result":{"tax":1.23,"amount":1230 }}""", null
                )
            )
        }

//        @JvmStatic
//        fun errorStatements(): Stream<Arguments> {
//            return Stream.of(
//                Arguments.of(
//                    "description: @.Exception.bang()", """{ "description" : 579.6235 }""", null
//                ),
//            )
//        }

        fun trimWithApostrophes(context: FunctionExecuteContext): Any? {
            val first = ConvertUtils.tryToString(context.firstParameter);
            val stringValue = first?.toString();
            return stringValue?.trim('\'');
        }

        fun trim(context: FunctionExecuteContext): Any? {
            val first = ConvertUtils.tryToString(context.firstParameter);
            return first?.trim();
        }

        fun kafkaPublish(context: FunctionExecuteContext): Any? {
            val first = context.firstParameter;
            val stringValue = first?.toString();
            return "Published to Kafka: $stringValue";
        }

        fun exceptionBang(context: FunctionExecuteContext): Any? {
            throw IllegalArgumentException("some random stuff");
        }
    }

    @ParameterizedTest
    @MethodSource(
        "basicAssignment",
        "variableAssignment",
        "assignObjectFixture",
        "arrayFixture",
        "interpolateFixture",
        "mathStatements",
        //"errorStatements"
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        run(script, expectedResult, map);
    }

    override fun onRegisterExtensions(context: OperationContext) {
        context.registerExtensionMethod("String.trim", Companion::trim);
        context.registerExtensionMethod("String.trimAp", Companion::trimWithApostrophes);
        context.registerExtensionMethod("Modifier.calculate_tax_amount", Companion::trimWithApostrophes);
        context.registerExtensionMethod("Kafka.Publish", Companion::trimWithApostrophes);

        context.registerExtensionMethod("Exception.bang", Companion::exceptionBang);
    }
}