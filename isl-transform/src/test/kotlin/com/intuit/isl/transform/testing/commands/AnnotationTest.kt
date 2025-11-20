package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.common.AnnotationExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.utils.JsonConvert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream

/**
 * Let's play with declaring reusable functions inline. Reusable functions can be called via
 * @.This.Name ( parameters ) and they always return a value
 */
@Suppress("unused")
class AnnotationTest : BaseTransformTest() {
    companion object {
        @JvmStatic
        fun functionDeclarations(): Stream<Arguments> {
            val obj1 = "{\n" +
                    "        name: \"test\",\n" +
                    "        value: 1\n" +
                    "    }\n";

            val obj2 = "{\n" +
                    "        value: 1,\n" +
                    "        name: \"test\"\n" +
                    "    }\n";

            return Stream.of(
                // Test cache fun with single param
                Arguments.of(
                    "cache fun child( \$tax ) {\n" +
                    "   \$val: {{ \$tax * 10 }};\n" +   // return the tax calculation
                    "   return \$val;\n" +
                    "}\n" +
                    // main entry point is the run function
                    "fun run( \$v1, \$v2 ){\n" +
                    "   \$result: @.This.child( \$v1 );\n" +
                    "   \$cachedResult: @.This.child( \$v2 );\n" +
                    "   return { result: \$result, cachedResult: \$cachedResult };\n" +
                    "}\n",
                    """{ "result": 10, "cachedResult" : 10 }""",
                    mapOf("v1" to 1, "v2" to 1)
                ),

                // Test with multiple params
                Arguments.of(
                    "cache fun child( \$tax1, \$tax2 ) {\n" +
                    "   \$val: {{ \$tax1 + \$tax2 }};\n" +   // return the tax calculation
                    "   return \$val;\n" +
                    "}\n" +
                    // main entry point is the run function
                    "fun run( \$v1, \$v2, \$v3, \$v4 ){\n" +
                    "   \$result: @.This.child( \$v1, \$v2 );\n" +
                    "   \$cachedResult: @.This.child( \$v3, \$v4 );\n" +
                    "   return { result: \$result, cachedResult: \$cachedResult };\n" +
                    "}\n",
                    """{ "result": 3, "cachedResult" : 3 }""",
                    mapOf("v1" to 1, "v2" to 2, "v3" to 1, "v4" to 2)
                ),

                // Test with object param
                Arguments.of(
                    "cache fun child( \$obj ) {\n" +
                    "   return \$obj;\n" +
                    "}\n" +
                    // main entry point is the run function
                    "fun run( ){\n" +
                    "   \$result: @.This.child( $obj1 );\n" +
                    "   \$cachedResult: @.This.child( $obj2 );\n" +
                    "   return { result: \$result, cachedResult: \$cachedResult };\n" +
                    "}\n",
                    """{ "result": {"name":"test","value":1}, "cachedResult": {"name":"test","value":1} }""", null
                ),
                // Test with simple param & object param
                Arguments.of(
                    "cache fun child( \$var, \$obj ) {\n" +
                    "   return {{ \$var + \$obj.value }};\n" +
                    "}\n" +
                    // main entry point is the run function`
                    "fun run( ){\n" +
                    "   \$result: @.This.child( 1, $obj1 );\n" +
                    "   \$cachedResult: @.This.child( 1, $obj2 );\n" +
                    "   return { result: \$result, cachedResult: \$cachedResult };\n" +
                    "}\n",
                    """{ "result": 2, "cachedResult": 2 }""", null
                ),

                // Test with single param function
                Arguments.of(
                    "@Cache\n" +
                    "fun child( \$tax ) {\n" +
                    "   \$val: {{ \$tax * 10 }};\n" +   // return the tax calculation
                    "   return \$val;\n" +
                    "}\n" +
                    // main entry point is the run function
                    "fun run( \$v1, \$v2 ){\n" +
                    "   \$result: @.This.child( \$v1 );\n" +
                    "   \$cachedResult: @.This.child( \$v2 );\n" +
                    "   return { result: \$result, cachedResult: \$cachedResult };\n" +
                    "}\n",
                    """{ "result": 10, "cachedResult" : 10 }""",
                    mapOf("v1" to 1, "v2" to 1)
                ),

                // Test with multiple params function
                Arguments.of(
                    "@Cache\n" +
                    "fun child( \$tax1, \$tax2 ) {\n" +
                    "   \$val: {{ \$tax1 + \$tax2 }};\n" +   // return the tax calculation
                    "   return \$val;\n" +
                    "}\n" +
                    // main entry point is the run function
                    "fun run( \$v1, \$v2, \$v3, \$v4 ){\n" +
                    "   \$result: @.This.child( \$v1, \$v2 );\n" +
                    "   \$cachedResult: @.This.child( \$v3, \$v4 );\n" +
                    "   return { result: \$result, cachedResult: \$cachedResult };\n" +
                    "}\n",
                    """{ "result": 3, "cachedResult" : 3 }""",
                    mapOf("v1" to 1, "v2" to 2, "v3" to 1, "v4" to 2)
                ),

                // Test with object param
                Arguments.of(
                    "@Cache\n" +
                    "fun child( \$obj ) {\n" +
                    "   return \$obj;\n" +
                    "}\n" +
                    // main entry point is the run function
                    "fun run( ){\n" +
                    "   \$result: @.This.child( $obj1 );\n" +
                    "   \$cachedResult: @.This.child( $obj2 );\n" +
                    "   return { result: \$result, cachedResult: \$cachedResult };\n" +
                    "}\n",
                    """{ "result": {"name":"test","value":1}, "cachedResult": {"name":"test","value":1} }""", null
                ),
                // Test with simple param & object param
                Arguments.of(
                    "@Cache\n" +
                    "fun child( \$var, \$obj ) {\n" +
                    "   return {{ \$var + \$obj.value }};\n" +
                    "}\n" +
                    // main entry point is the run function
                    "fun run( ){\n" +
                    "   \$result: @.This.child( 1, $obj1 );\n" +
                    "   \$cachedResult: @.This.child( 1, $obj2 );\n" +
                    "   return { result: \$result, cachedResult: \$cachedResult };\n" +
                    "}\n",
                    """{ "result": 2, "cachedResult": 2 }""", null
                ),

                Arguments.of(
//                   annotation with selected param
                    "@Cache({ key: \$obj.name })\n" +
                    "fun child( \$obj ) {\n" +
                    "   return \$obj;\n" +
                    "}\n" +
                    // main entry point is the run function
                    "fun run( ){\n" +
                    "   \$result: @.This.child( $obj1 );\n" +
                    "   \$cachedResult: @.This.child( $obj2 );\n" +
                    "   return { result: \$result, cachedResult: \$cachedResult };\n" +
                    "}\n",
                    """{ "result": {"name":"test","value":1}, "cachedResult": {"name":"test","value":1} }""", null
                ),

                // Test with default cache annotation
                Arguments.of(
                    "@defaultCache\n" +
                    "fun child( \$tax ) {\n" +
                    "   \$val: {{ \$tax * 10 }};\n" +   // return the tax calculation
                    "   return \$val;\n" +
                    "}\n" +
                    // main entry point is the run function
                    "fun run( \$v1, \$v2 ){\n" +
                    "   \$result: @.This.child( \$v1 );\n" +
                    "   \$cachedResult: @.This.child( \$v2 );\n" +
                    "   return { result: \$result, cachedResult: \$cachedResult };\n" +
                    "}\n",
                    """{ "result": 10, "cachedResult" : 10 }""",
                    mapOf("v1" to 1, "v2" to 1)
                ),

                Arguments.of(
                    "@defaultCache\n" +
                    "fun child( \$tax1, \$tax2 ) {\n" +
                    "   \$val: {{ \$tax1 + \$tax2 }};\n" +   // return the tax calculation
                    "   return \$val;\n" +
                    "}\n" +
                    // main entry point is the run function
                    "fun run( \$v1, \$v2, \$v3, \$v4 ){\n" +
                    "   \$result: @.This.child( \$v1, \$v2 );\n" +
                    "   \$cachedResult: @.This.child( \$v3, \$v4 );\n" +
                    "   return { result: \$result, cachedResult: \$cachedResult };\n" +
                    "}\n",
                    """{ "result": 3, "cachedResult" : 3 }""",
                    mapOf("v1" to 1, "v2" to 2, "v3" to 1, "v4" to 2)
                ),

                Arguments.of(
//                   annotation with selected param
                    "@defaultCache({ key: \$obj.name })\n" +
                    "fun child( \$obj ) {\n" +
                    "   return \$obj;\n" +
                    "}\n" +
                    // main entry point is the run function
                    "fun run( ){\n" +
                    "   \$result: @.This.child( $obj1 );\n" +
                    "   \$cachedResult: @.This.child( $obj2 );\n" +
                    "   return { result: \$result, cachedResult: \$cachedResult };\n" +
                    "}\n",
                    """{ "result": {"name":"test","value":1}, "cachedResult": {"name":"test","value":1} }""", null
                ),

                Arguments.of(
//                   multiple annotations
                    "@FeatureFlag({ flag: \$flag})\n" +
                    "@Cache({ key: \$obj.name })\n" +
                    "fun child( \$obj, \$flag ) {\n" +
                    "   return \$obj;\n" +
                    "}\n" +
                    // main entry point is the run function
                    "fun run( ){\n" +
                    "   \$result: @.This.child( $obj1, true );\n" +
                    "   \$cachedResult: @.This.child( $obj2, true );\n" +
                    "   return { result: \$result, cachedResult: \$cachedResult };\n" +
                    "}\n",
                    """{ "result": {"name":"test","value":1}, "cachedResult": {"name":"test","value":1} }""", null
                ),
                Arguments.of(
//                   multiple annotations
                    "@FeatureFlag({ flag: \$flag})\n" +
                    "@Cache({ key: \$obj.name })\n" +
                    "fun child( \$obj, \$flag ) {\n" +
                    "   return \$obj;\n" +
                    "}\n" +
                    // main entry point is the run function
                    "fun run( ){\n" +
                    "   \$result: @.This.child( $obj1, false );\n" +
                    "   \$cachedResult: @.This.child( $obj2, false );\n" +
                    "   return { result: \$result, cachedResult: \$cachedResult };\n" +
                    "}\n",
                    """{"result":null,"cachedResult":null}""", null
                ),

                // Test for capturing the FunctionReturnException
                Arguments.of(
                    "@Cache\n" +
                    "fun child( \$v1 ) {\n" +
                    "   foreach \$it in \$v1  return \$it endfor\n" +
                    "}\n" +
                    // main entry point is the run function
                    "fun run( \$v1 ){\n" +
                    "   \$result: @.This.child( \$v1 );\n" +
                    "   \$cachedResult: @.This.child( \$v1 );\n" +
                    "   return { result: \$result, cachedResult: \$cachedResult };\n" +
                    "}\n",
                    """{ "result": 1, "cachedResult" : 1 }""",
                    mapOf("v1" to arrayOf(1, 2, 3))
                )
            )
        }

        // Apply MissingObject as a default value for cache map to include null value
        private val MissingObject = Any();

        var cache: ConcurrentHashMap<JsonNode, Any?> = ConcurrentHashMap<JsonNode, Any?>();
        suspend fun cacheAnnotation(context: AnnotationExecuteContext): Any? {

            val param = if (context.parameters.isNotEmpty()) context.parameters
            else context.functionParameters;

            val cacheKey = JsonNodeFactory.instance.objectNode()
                .put(
                    "fn",
                    context.command.token.position.file + ":@" + context.annotationName + "->" + context.functionName
                )
                .set<JsonNode>("param", JsonConvert.mapper.valueToTree(param))

            val cachedResult = cache.getOrDefault(cacheKey, MissingObject)
            if (cachedResult == MissingObject) {
                // We need to invoke the next command
                val result = context.runNextCommand()
                cache[cacheKey] = result
                return result
            }
            return cachedResult;
        }

        suspend fun featureFlag(context: AnnotationExecuteContext): Any? {
            val param = context.firstParameter as ObjectNode
            val flag = param["flag"].toString().toBoolean()
            if (flag) {
                return context.runNextCommand()
            }
            return null
        }
    }

    @ParameterizedTest
    @MethodSource(
        "functionDeclarations"
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        run(script, expectedResult, map);
    }

    override fun onRegisterExtensions(context: OperationContext) {
        context.registerAnnotation("Cache", Companion::cacheAnnotation);
        context.registerAnnotation("FeatureFlag", Companion::featureFlag);
    }
}