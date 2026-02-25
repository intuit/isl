package com.intuit.isl.test.assertions

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.test.TestOperationContext
import com.intuit.isl.commands.ConditionEvaluator
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert

object AssertFunction {
    private const val functionName = "Assert"

    fun registerExtensions(context: TestOperationContext) {
        mapOf<String, (FunctionExecuteContext) -> Any?>(
            AssertFunction::equal.name to AssertFunction::equal,
            AssertFunction::notEqual.name to AssertFunction::notEqual,
            AssertFunction::lessThan.name to AssertFunction::lessThan,
            AssertFunction::lessThanOrEqual.name to AssertFunction::lessThanOrEqual,
            AssertFunction::greaterThan.name to AssertFunction::greaterThan,
            AssertFunction::greaterThanOrEqual.name to AssertFunction::greaterThanOrEqual,
            AssertFunction::matches.name to AssertFunction::matches,
            AssertFunction::notMatches.name to AssertFunction::notMatches,
            AssertFunction::contains.name to AssertFunction::contains,
            AssertFunction::notContains.name to AssertFunction::notContains,
            AssertFunction::startsWith.name to AssertFunction::startsWith,
            AssertFunction::notStartsWith.name to AssertFunction::notStartsWith,
            AssertFunction::endsWith.name to AssertFunction::endsWith,
            AssertFunction::notEndsWith.name to AssertFunction::notEndsWith,
            "in" to AssertFunction::assertIn,
            AssertFunction::notIn.name to AssertFunction::notIn,
            AssertFunction::isType.name to AssertFunction::isType,
            AssertFunction::isNotType.name to AssertFunction::isNotType,
            AssertFunction::notNull.name to AssertFunction::notNull,
            AssertFunction::isNull.name to AssertFunction::isNull
        ).forEach { (t, u) ->
            registerExtensionMethod(context, t, u)
        }
    }

    private fun equal(context: FunctionExecuteContext): Any? {
        return evaluateCondition(context, "==")
    }

    private fun notEqual(context: FunctionExecuteContext): Any? {
        return evaluateCondition(context, "!=")
    }

    private fun lessThan(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, "<")

    private fun lessThanOrEqual(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, "<=")

    private fun greaterThan(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, ">")

    private fun greaterThanOrEqual(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, ">=")

    private fun matches(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, "matches")

    private fun notMatches(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, "!matches")

    private fun contains(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, "contains")

    private fun notContains(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, "!contains")

    private fun startsWith(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, "startswith")

    private fun notStartsWith(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, "!startswith")

    private fun endsWith(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, "endswith")

    private fun notEndsWith(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, "!endswith")

    private fun assertIn(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, "in")

    private fun notIn(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, "!in")

    private fun isType(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, "is")

    private fun isNotType(context: FunctionExecuteContext): Any? =
        evaluateCondition(context, "!is")

    private fun isNull(context: FunctionExecuteContext): Any? {
        val expectedValue = context.firstParameter
        val messageStr = tryGetMessageStr(context.secondParameter)

        val result = ConditionEvaluator.evaluate(expectedValue, "notexists", null)
        val functionName = context.functionName
        if (!result) {
            throw EvaluationAssertException(
                "$functionName failed. Input value is not null. Value: $expectedValue$messageStr",
                functionName,
                expectedValue,
                context.command.token.position
            )
        }

        return null
    }

    private fun notNull(context: FunctionExecuteContext): Any? {
        val expectedValue = context.firstParameter
        val messageStr = tryGetMessageStr(context.secondParameter)

        val result = ConditionEvaluator.evaluate(expectedValue, ConditionEvaluator.EXISTS, null)
        val functionName = context.functionName
        if (!result) {
            throw EvaluationAssertException(
                "$functionName failed. Input value is null. Value: $expectedValue$messageStr",
                functionName,
                expectedValue,
                context.command.token.position
            )
        }

        return null
    }

    private fun evaluateCondition(context: FunctionExecuteContext, condition: String): Nothing? {
        val expectedValue = context.firstParameter
        val actualValue = context.secondParameter
        val messageStr = tryGetMessageStr(context.thirdParameter)

        val result = when (condition) {
            "==" -> deepEqual(expectedValue, actualValue)
            "!=" -> !deepEqual(expectedValue, actualValue)
            else -> ConditionEvaluator.evaluate(expectedValue, condition, actualValue)
        }
        val functionName = context.functionName
        if (!result) {
            throw ComparisonAssertException(
                "$functionName failed. Expected: \n${toReadableString(expectedValue)}\nReceived: \n${
                    toReadableString(
                        actualValue
                    )
                }\n$messageStr",
                functionName,
                expectedValue,
                actualValue,
                context.command.token.position
            )
        }

        return null
    }

    /**
     * Deep equality comparison that identifies objects and compares them
     * ignoring the order of properties. Arrays are compared with order preserved.
     */
    private fun deepEqual(left: Any?, right: Any?): Boolean {
        if (left == null && right == null) return true
        if (left == null || right == null) return false

        return when {
            isJsonObject(left) && isJsonObject(right) ->
                objectsEqualIgnoringPropertyOrder(toJsonNode(left), toJsonNode(right))
            isJsonArray(left) && isJsonArray(right) ->
                arraysEqual(toJsonNode(left) as ArrayNode, toJsonNode(right) as ArrayNode)
            else -> ConditionEvaluator.equalish(left, right)
        }
    }

    private fun isJsonObject(value: Any?): Boolean = when (value) {
        is ObjectNode -> true
        is JsonNode -> value.isObject
        is Map<*, *> -> true
        else -> false
    }

    private fun isJsonArray(value: Any?): Boolean = when (value) {
        is ArrayNode -> true
        is JsonNode -> value.isArray
        else -> false
    }

    private fun toJsonNode(value: Any?): JsonNode = when (value) {
        is JsonNode -> value
        is Map<*, *> -> JsonConvert.convert(value)
        else -> JsonConvert.convert(value)
    }

    private fun objectsEqualIgnoringPropertyOrder(left: JsonNode, right: JsonNode): Boolean {
        if (!left.isObject || !right.isObject) return ConditionEvaluator.equalish(left, right)

        val leftKeys = left.fieldNames().asSequence().toSet()
        val rightKeys = right.fieldNames().asSequence().toSet()
        if (leftKeys != rightKeys) return false

        for (key in leftKeys) {
            if (!deepEqual(left.get(key), right.get(key))) return false
        }
        return true
    }

    private fun arraysEqual(left: ArrayNode, right: ArrayNode): Boolean {
        if (left.size() != right.size()) return false
        for (i in 0 until left.size()) {
            if (!deepEqual(left.get(i), right.get(i))) return false
        }
        return true
    }

    private fun toReadableString(value: Any?): String {
        val valueStr = ConvertUtils.tryToString(value)
        return when {
            valueStr == null -> {
                "<null>"
            }

            valueStr.isEmpty() -> {
                "\"\""
            }

            valueStr.isBlank() -> {
                "\"$valueStr\""
            }

            else -> {
                valueStr
            }
        }
    }

    private fun tryGetMessageStr(msg: Any?): String {
        val message = ConvertUtils.tryToString(msg)
        var messageStr = ""
        if (message != null) {
            messageStr = ". Additional message: $message"
        }
        return messageStr
    }

    private fun registerExtensionMethod(
        context: TestOperationContext, name: String, method: (FunctionExecuteContext) -> Any?
    ) {
        context.registerExtensionMethod("$functionName.${name}") {
            method(it)
        }
    }
}

