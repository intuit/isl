package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.commands.modifiers.IConditionalCommand
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.run

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("UNUSED_PARAMETER")
class ConditionalModifiersTest : YamlTransformTest("conditional-modifiers") {

    private fun retryModifiersFixture(): Stream<Arguments> {
        return createTests("retry-modifiers")
    }

    private fun inlineConditionsFixture(): Stream<Arguments> {
        return createTests("generic-modifiers")
    }

    private fun modifierConditionsFixture(): Stream<Arguments> {
        return createTests("generic-conditional-modifiers")
    }

    @ParameterizedTest(name = "{index}. {0}")
    @MethodSource(
        "retryModifiersFixture",
        "inlineConditionsFixture",
        "modifierConditionsFixture"
    )
    fun runFixtures(testName: String, script: String, expectedResult: JsonNode, map: Map<String, Any?>? = null) {
        run(script, expectedResult.toPrettyString(), map)
    }

    override fun onRegisterExtensions(context: OperationContext) {

        context.registerExtensionMethod("modifier.simple", ::simpleModifier)
        context.registerExtensionMethod("modifier.wild.*", ::wildModifier)
        context.registerConditionalExtensionMethod("modifier.test", ::testModifier)
        context.registerConditionalExtensionMethod("modifier.do.*", ::doWhenModifier)

        super.onRegisterExtensions(context)
    }

    private fun simpleModifier(context: FunctionExecuteContext): Any {
        return "|simple( ${context.firstParameter}, ${context.secondParameter}, ${context.thirdParameter} )"
    }
    private fun wildModifier(context: FunctionExecuteContext): Any {
        return "|wild.${context.secondParameter}( ${context.firstParameter}, ${context.secondParameter}, ${context.thirdParameter}, ${context.fourthParameter}, ${context.fifthParameter} )"
    }

    private fun testModifier(command: IConditionalCommand, context: ExecutionContext): Any {
        return "|test( ${command.expression}, ${command.arguments} )"
    }
    private fun doWhenModifier(command: IConditionalCommand, context: ExecutionContext): Any {
        return "|do.when( ${command.expression}, ${command.arguments} )"
    }
}

