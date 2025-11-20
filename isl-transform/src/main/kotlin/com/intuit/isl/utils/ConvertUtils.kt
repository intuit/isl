package com.intuit.isl.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.Charset
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

class ConvertUtils {
    companion object {
        const val IsoDateTimeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"; // S - fraction of a second
        const val IsoDateTimeFormatOptional = "yyyy-MM-dd['T'HH:mm:ss[.SSS]['Z']]"; // S - fraction of a second

        // We picked this from the old DCS so we assure compatibility
        const val ISO8601DefaultParseFormat =
            "[yyyy-MM-dd['T'HH[:mm[:ss[.SSSSSSSSS][.SSSSSS][.SSS][.SS][.S]]][XXX][X]]]" +
                    "[yyyyMMdd['T'HH[mm[ss[.SSSSSSSSS][.SSSSSS][.SSS][.SS][.S]]][XXX][X]]]";

        val IsoDateTimeFormatter = DateTimeFormatter.ofPattern(IsoDateTimeFormat).withZone(ZoneOffset.UTC);

        private val BIG_DECIMAL_PATTERN: Pattern = Pattern.compile(
            "[-+]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][-+]?\\d+)?"
        )

        fun isStringDecimal(value: String): Boolean {
            return BIG_DECIMAL_PATTERN.matcher(value).matches();
        }

        fun canValueBeDecimal(value: Any?): Boolean {
            try {
                // Performance: Order type checks by frequency - most common types first
                return when (value) {
                    null -> false
                    is BigDecimal -> true
                    is NumericNode -> true
                    is Double -> true
                    is Int -> true
                    is Long -> true
                    is BigInteger -> true
                    is Number -> true

                    JsonNodeFactory.instance.nullNode() -> false

                    is String -> {
                        if (isStringDecimal(value))
                            true
                        else
                            false
                    }  // Direct, no intermediate conversion
                    is TextNode -> {
                        val textValue = value.textValue();
                        if (isStringDecimal(textValue))
                            true
                        else
                            false
                    }  // Direct access, avoid tryToString
                    is JsonNode -> {
                        val textValue = value.asText();
                        if (isStringDecimal(textValue))
                            true
                        else
                            false
                    }

                    else -> false
                }
            } catch (ex: NumberFormatException) {
                return false
            }
        }

        fun tryParseDecimal(value: Any?): BigDecimal? {
            try {
                // Performance: Order type checks by frequency - most common types first
                return when (value) {
                    null -> BigDecimal.ZERO
                    is BigDecimal -> value  // Fast path - no conversion
                    is NumericNode -> value.decimalValue()  // Fast path
                    is Double -> BigDecimal.valueOf(value)  // Faster than string conversion
                    is Int -> BigDecimal.valueOf(value.toLong())
                    is Long -> BigDecimal.valueOf(value)
                    is BigInteger -> BigDecimal(value)
                    is Number -> BigDecimal(value.toDouble())

                    JsonNodeFactory.instance.nullNode() -> null

                    is String -> {
                        if (isStringDecimal(value))
                            BigDecimal(value)
                        else
                            null;
                    }  // Direct, no intermediate conversion
                    is TextNode -> {
                        val textValue = value.textValue();
                        if (isStringDecimal(textValue))
                            BigDecimal(textValue)
                        else
                            null;
                    }  // Direct access, avoid tryToString
                    is JsonNode -> {
                        val textValue = value.asText();
                        if (isStringDecimal(textValue))
                            BigDecimal(textValue)
                        else
                            null;
                    }

                    else -> null
                }
            } catch (ex: NumberFormatException) {
                return null
            }
        }

        fun isNumeric(value: Any?): Boolean {
            return when (value) {
                is NumericNode -> true;

                is BigDecimal -> true;
                is BigInteger -> true;
                is Number -> true;

                else -> false;
            }
        }

        private fun BigDecimal_fromString(text: String?): BigDecimal? {
            if (text.isNullOrEmpty())
                return null;
            return BigDecimal(text);
        }

        fun getIterator(collection: Any?): Iterable<Any?>? {
            return when (collection) {
                is IIslIterable -> collection.getInnerIterator();
                is Iterable<Any?> -> collection
                else -> null;
            };
        }

        fun tryToList(values: ArrayNode?): List<Any>? {
            if (values == null || values.size() < 1) {
                return listOf();
            }

            val useNumeric = canValueBeDecimal(values.get(0));

            if (useNumeric) {
                return tryToNumbersList(values);
            } else {
                return tryToStringList(values);
            }
        }

        fun tryInstant(value: Any?): Instant? {
            return when (value) {
                is Instant -> value;
                is InstantNode -> value.value;
                else -> null;
            }
        }

        fun tryToNumbersList(values: ArrayNode?): List<BigDecimal>? {
            if (values == null) {
                return listOf();
            }
            val result: List<BigDecimal>? = values.map { it -> tryParseDecimal(it) ?: BigDecimal.ZERO };
            return result?.toList();
        }

        fun tryToStringList(values: ArrayNode?): List<String>? {
            if (values == null) {
                return listOf();
            }
            val result: List<String>? = values.map { it.asText() };
            return result?.toList();
        }

        fun tryParseLong(value: Any?, default: Long? = null): Long? {
            try {
                // Performance: Order type checks by frequency
                return when (value) {
                    null -> default
                    is Long -> value  // Fast path
                    is NumericNode -> value.longValue()  // Fast path
                    is Int -> value.toLong()
                    is BigDecimal -> value.longValueExact()
                    is String -> value.toLongOrNull() ?: default
                    is TextNode -> value.textValue().toLongOrNull() ?: default
                    is BigInteger -> value.longValueExact()
                    is Number -> value.toLong()
                    is JsonNode -> value.asText().toLongOrNull() ?: default
                    JsonNodeFactory.instance.nullNode() -> default
                    else -> default
                }
            } catch (ex: NumberFormatException) {
                return default
            }
        }

        fun tryParseInt(value: Any?, default: Int? = null): Int? {
            try {
                // Performance: Order type checks by frequency
                return when (value) {
                    null -> default
                    is Int -> value  // Fast path
                    is NumericNode -> value.intValue()  // Fast path
                    is Long -> value.toInt()
                    is BigDecimal -> value.intValueExact()
                    is String -> value.toIntOrNull() ?: default
                    is TextNode -> value.textValue().toIntOrNull() ?: default
                    is BigInteger -> value.intValueExact()
                    is Number -> value.toInt()
                    is JsonNode -> value.asText().toIntOrNull() ?: default
                    JsonNodeFactory.instance.nullNode() -> default
                    else -> default
                }
            } catch (ex: NumberFormatException) {
                return default
            }
        }

        // we need this to make sure TextNode values don't end up as ""text""
        fun tryToString(value: Any?): String? {
            return tryToString(value, Charsets.UTF_8);
        }

        fun tryToString(value: Any?, encoding: Charset?): String? {
            var enc = encoding ?: Charsets.UTF_8;

            // Performance: Order type checks by frequency - String and TextNode are most common
            return when (value) {
                null -> null
                is String -> value  // Fast path - already a string!
                is TextNode -> value.textValue()  // Very common
                is NumericNode -> value.asText()  // Common
                is BigDecimal -> value.toPlainString()
                is InstantNode -> value.toString()
                is DecimalNode -> value.decimalValue().toPlainString()
                is DoubleNode -> value.decimalValue().toPlainString()
                is BinaryNode -> String(value.binaryValue(), enc)
                is ByteArray -> String(value, enc)
                is BooleanNode -> value.asText();
                is ValueNode -> value.textValue();
                else -> value.toString();
            }
        }

        fun getByteArray(p: Any?): ByteArray {
            return when (p) {
                is ByteArray -> p;
                is BinaryNode -> p.binaryValue();
                // TODO: Should we look into handling from base64?
                null -> byteArrayOf();
                else -> (tryToString(p) ?: "").toByteArray(Charsets.UTF_8)
            };
        }


        fun extractFromNode(value: Any?): Any? {
            return when (value) {
                null -> null;
                JsonNodeFactory.instance.nullNode() -> return null;
                //JsonNodeFactory.instance.missingNode() -> return null;
                is NumericNode -> return value.decimalValue();
                is BooleanNode -> return value.booleanValue();
                is TextNode -> value.textValue();
                is JsonNode -> value;
                is ArrayNode -> value.fields().asSequence().toList();
                is ObjectRefNode -> value.value;
                else -> value;
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun <T : IIslReference> getReference(value: Any?): T? {
            if (value is ObjectRefNode)
                return value.value as? T?;
            return null;
        }
    }
}