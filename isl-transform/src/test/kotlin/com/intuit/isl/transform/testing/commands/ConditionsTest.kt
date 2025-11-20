package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.TransformCompilationException
import com.intuit.isl.utils.ConvertUtils
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.run

@Suppress("unused", "UNUSED_PARAMETER")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConditionsTest : YamlTransformTest("conditions") {
    companion object {
        fun configGet(context: FunctionExecuteContext): Any? {
            val name = ConvertUtils.tryToString(context.firstParameter);
            if (name.isNullOrBlank())
                return null;
            return "$name Value";
        }

        fun callApi(context: FunctionExecuteContext): Any? {
            val result = """ { "statusCode": 200, "body": { "product": { "id": 1, "value": null } } }""";
            val resultObject = ObjectMapper().readTree(result);
            return resultObject;
        }
    }

    private fun inlineConditionsFixture(): Stream<Arguments> {
        return createTests("inline-conditions-fixture")
    }

    private fun modifierConditionsFixture(): Stream<Arguments> {
        return createTests("modifier-conditions-fixture")
    }

    private fun conditionsV2SimpleFixture(): Stream<Arguments> {
        return createTests("conditions-v2-simple-fixture")
    }

    private fun numericConditions(): Stream<Arguments> {
        return createTests("numeric-conditions")
    }

    private fun dateConditions(): Stream<Arguments> {
        return createTests("date-conditions")
    }

    private fun nullConditions(): Stream<Arguments> {
        return createTests("null-conditions")
    }

    private fun stringConditions(): Stream<Arguments> {
        return createTests("string-conditions")
    }

    private fun regexConditions(): Stream<Arguments> {
        return createTests("regex-conditions")
    }

    private fun arrayConditions(): Stream<Arguments> {
        return createTests("array-conditions")
    }

    private fun booleanConditions(): Stream<Arguments> {
        return createTests("boolean-conditions")
    }

    private fun switchCaseFixture(): Stream<Arguments> {
        return createTests("switch-case-fixture")
    }

    private fun deepObjectFixtures(): Stream<Arguments> {
        return createTests("deep-object-fixtures")
    }

    private fun coalesceFixture() : Stream<Arguments> {
        return createTests("coalesce-fixture")
    }

    @ParameterizedTest
    @MethodSource(
        "conditionsV2SimpleFixture",
        "inlineConditionsFixture",
        "modifierConditionsFixture",
        "numericConditions",
        "dateConditions",
        "nullConditions",
        "stringConditions",
        "regexConditions",
        "booleanConditions",
        "arrayConditions",
        "switchCaseFixture",
        // Test for deep null
        "deepObjectFixtures",
        "coalesceFixture"
    )
    fun runFixtures(testName: String, script: String, expectedResult: JsonNode, map: Map<String, Any?>? = null) {
        run(script, expectedResult.toPrettyString(), map);
    }

    @Disabled
    @ParameterizedTest
    @MethodSource(
        "conditionsV2SimpleFixture",
        "inlineConditionsFixture",
        "modifierConditionsFixture",
        "numericConditions",
//        "dateConditions",
        "stringConditions",
        "booleanConditions",
        "arrayConditions",
//        "switchCaseFixture",
//        // Test for deep null
        "deepObjectFixtures",
        "coalesceFixture"
    )
    fun runJSFixtures(testName: String, script: String, expectedResult: JsonNode, map: Map<String, Any?>? = null) {
        try {
            runJs(script, expectedResult, map)
        }catch (e: TransformCompilationException){
            if(e.message!!.contains("is not supported by"))
                return;
            else throw e;
        }
    }



    override fun onRegisterExtensions(context: OperationContext) {
        context.registerExtensionMethod("Call.Api", ::callApi);
        context.registerExtensionMethod("Config.Get", ::configGet);
    }
}

