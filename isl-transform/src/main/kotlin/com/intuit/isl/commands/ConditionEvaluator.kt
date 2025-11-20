package com.intuit.isl.commands

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.InstantNode
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

/**
 * Can be used to evaluate conditions. Used by if/then, switch/case, maybe in the future any other conditions
 * This allows a consistent evaluation approach
 */
object ConditionEvaluator {
    const val EXISTS = "exists";

    fun evaluate(leftValue: Any?, condition: String, rightValue: Any?): Boolean {
        return when (condition) {
            "notexists" -> !isValid(leftValue);
            EXISTS -> isValid(leftValue);
            "==" -> equalish(leftValue, rightValue);
            "!=" -> !equalish(leftValue, rightValue);
            "<=" -> equalish(leftValue, rightValue) || smallerThen(leftValue, rightValue);
            "<" -> smallerThen(leftValue, rightValue);
            ">=" -> equalish(leftValue, rightValue) || biggerThen(leftValue, rightValue);
            ">" -> biggerThen(leftValue, rightValue);

            // Match RegEx Expression
            "matches" -> matches(leftValue, rightValue);
            "!matches" -> !matches(leftValue, rightValue);

            "else" -> {
                // an else branch (e.g. in a switch/case)
                return true;
            }

            "contains" -> contains(leftValue, rightValue);
            "!contains" -> !contains(leftValue, rightValue);
            "startswith" -> startsWith(leftValue, rightValue);
            "!startswith" -> notStartsWith(leftValue, rightValue);
            "endswith" -> endsWith(leftValue, rightValue);
            "!endswith" -> notEndsWith(leftValue, rightValue);
            "in" -> contains(rightValue, leftValue)
            "!in" -> !contains(rightValue, leftValue)

            "is" -> isOfType(leftValue, rightValue)
            "!is" -> !isOfType(leftValue, rightValue)

            else -> false;
        }
    }

    fun equalish(left: Any?, right: Any?): Boolean {
        if (left == null && right == null)
            return true;
        if (left == null || right == null)
            return false;

        // Check for date
        val leftDate = ConvertUtils.tryInstant(left);
        val rightDate = ConvertUtils.tryInstant(right);
        if (leftDate != null && rightDate != null) {
            return leftDate.epochSecond == rightDate.epochSecond;
        }


        // CT: There's no reason to check for decimal comparison if at least one of the values is not a numeric value
        // Check for decimal
        if (ConvertUtils.isNumeric(left) || ConvertUtils.isNumeric(right)) {
            val leftNumber = ConvertUtils.tryParseDecimal(left);
            val rightNumber = ConvertUtils.tryParseDecimal(right);
            if (leftNumber != null && rightNumber != null) {
                // TODO: Add some precision evaluation (e.g. 4 decimals)
                return leftNumber.compareTo(rightNumber) == 0;
            }
        }

        // else - string comparison
        val leftValue = ConvertUtils.tryToString(left);
        val rightValue = ConvertUtils.tryToString(right);
        if (leftValue == null && rightValue == null)
            return true;
        if (leftValue != null && rightValue != null)
            if (leftValue.compareTo(rightValue) == 0)
                return true;
        return false;
    }

    private fun smallerThen(left: Any?, right: Any?): Boolean {
        // Check for date
        val leftDate = ConvertUtils.tryInstant(left);
        val rightDate = ConvertUtils.tryInstant(right);
        if (leftDate != null && rightDate != null) {
            return leftDate.epochSecond < rightDate.epochSecond;
        }

        // Check for numbers
        val leftNumber = ConvertUtils.tryParseDecimal(left);
        val rightNumber = ConvertUtils.tryParseDecimal(right);
        if (leftNumber != null && rightNumber != null) {
            // TODO: Add some precision evaluation (e.g. 4 decimals)
            return leftNumber < rightNumber;
        }

        val leftValue = ConvertUtils.tryToString(left);
        val rightValue = ConvertUtils.tryToString(right);
        if (leftValue == null && rightValue == null)
            return true;
        if (leftValue == null || rightValue == null)
            return false;
        return leftValue < rightValue;
    }

    private fun biggerThen(left: Any?, right: Any?): Boolean {
        // Check for date
        val leftDate = ConvertUtils.tryInstant(left);
        val rightDate = ConvertUtils.tryInstant(right);
        if (leftDate != null && rightDate != null) {
            return leftDate.epochSecond > rightDate.epochSecond;
        }

        val leftNumber = ConvertUtils.tryParseDecimal(left);
        val rightNumber = ConvertUtils.tryParseDecimal(right);
        if (leftNumber != null && rightNumber != null) {
            // TODO: Add some precision evaluation (e.g. 4 decimals)
            return leftNumber > rightNumber;
        }

        val leftValue = ConvertUtils.tryToString(left);
        val rightValue = ConvertUtils.tryToString(right);
        if (leftValue == null && rightValue == null)
            return true;
        if (leftValue == null || rightValue == null)
            return false;
        return leftValue > rightValue;
    }


    private fun contains(left: Any?, right: Any?): Boolean {
        // if left value is an array then check the right value
        if (left is Iterable<*> && left !is TextNode) {
            // execute a contains in array
            left.forEach {
                if (equalish(it, right))
                    return true;
            }
            return false;
        } else {
            val leftValue = ConvertUtils.tryToString(left);
            val rightValue = ConvertUtils.tryToString(right);

            if (leftValue == null && rightValue == null)
                return false;
            if (leftValue == null || rightValue == null)
                return false;

            if (leftValue.contains(rightValue, true))
                return true;
            return false;
        }
    }

    private fun startsWith(left: Any?, right: Any?): Boolean {
        val leftValue = ConvertUtils.tryToString(left);
        val rightValue = ConvertUtils.tryToString(right);

        if (leftValue == null || rightValue == null)
            return false;

        if (leftValue.startsWith(rightValue, true))
            return true;
        return false;
    }

    private fun notStartsWith(left: Any?, right: Any?): Boolean {
        val leftValue = ConvertUtils.tryToString(left);
        val rightValue = ConvertUtils.tryToString(right);

        if (leftValue == null || rightValue == null)
            return false;

        if (leftValue.startsWith(rightValue, true))
            return false;
        return true;
    }

    private fun endsWith(left: Any?, right: Any?): Boolean {
        val leftValue = ConvertUtils.tryToString(left);
        val rightValue = ConvertUtils.tryToString(right);

        if (leftValue == null || rightValue == null)
            return false;

        if (leftValue.endsWith(rightValue, true))
            return true;
        return false;
    }

    private fun notEndsWith(left: Any?, right: Any?): Boolean {
        val leftValue = ConvertUtils.tryToString(left);
        val rightValue = ConvertUtils.tryToString(right);

        if (leftValue == null || rightValue == null)
            return false;

        if (leftValue.endsWith(rightValue, true))
            return false;
        return true;
    }


    fun isValid(result: Any?): Boolean {
        return when (result) {
            null -> false;
            JsonNodeFactory.instance.nullNode() -> false;
            is TextNode ->
                return !result.textValue().isNullOrEmpty();
            is String ->
                return !result.isNullOrEmpty();
            is ArrayNode ->
                return result.size() != 0;
            is BooleanNode ->
                return result.booleanValue();
            is Boolean ->
                return result;
            else -> true;
        }
    }

    private fun matches(leftValue: Any?, rightValue: Any?): Boolean {
        // TODO: Maybe handle the standard "/evaluation options" like IgnoreCase, MultiLine,
        val regEx = Regex(ConvertUtils.tryToString(rightValue) ?: "", RegexOption.IGNORE_CASE);
        val left = ConvertUtils.tryToString(leftValue);
        if (left == null)
            return false;
        // putting a regEx.match(left) here won't work because your regex might not be an exact match
        // e.g. /^v/ does not match "value" but /^v/ finds "value"
        return regEx.find(left) != null;
    }

    private fun isOfType(left: Any?, right: Any?): Boolean {
        val right = ConvertUtils.tryToString(right)?.lowercase() ?: "";

        when (right) {
            "number" -> if (left is NumericNode
                || left is BigDecimal || left is BigInteger
                || left is Double || left is Int || left is Long || left is Number
            )
                return true;
            "date" -> if (left is Instant || left is InstantNode)
                return true;
            "string" -> if (left is String || left is TextNode)
                return true;
            "node" -> if (left is ObjectNode)
                return true;
            "array" -> if (left is ArrayNode || left is Iterable<*>)
                return true;
        }
        return false;
    }
}