package com.intuit.isl.transform.testing.types

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.transform.testing.commands.YamlTransformTest
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("unused", "UNUSED_PARAMETER")
class TypesDeclarationTest : YamlTransformTest("types") {
    private fun simpleTypes(): Stream<Arguments> {
        return createTests("simple-types")
    }

    private fun typeDeclarations(): Stream<Arguments> {
        return createTests("type-declarations")
    }

    private fun functionTypesDeclarations(): Stream<Arguments> {
        return createTests("function-types")
    }

    @ParameterizedTest
    @MethodSource(
        "simpleTypes",
        "typeDeclarations",
        "functionTypesDeclarations"
    )
    fun runFixtures(testName: String, script: String, expectedResult: JsonNode, map: Map<String, Any?>? = null) {
        run(script, expectedResult.toPrettyString(), map, doWarm = false)
    }

    override fun onRegisterExtensions(context: OperationContext) {
        context.registerExtensionMethod("Api.Call", ::mockApiCall);
        super.onRegisterExtensions(context)
    }

    fun mockApiCall(context: FunctionExecuteContext): Any? {
        val result = JsonNodeFactory.instance.objectNode();
        result.put("success", true);
        return result;
    }
}