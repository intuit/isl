@file:Suppress("UNUSED_PARAMETER")

package com.intuit.isl.utils

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.runtime.TransformException
import org.apache.commons.lang3.LocaleUtils
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object DateExtensions {
    fun registerExtensions(context: IOperationContext) {
        context.registerExtensionMethod("Date.Now", DateExtensions::now);
        context.registerExtensionMethod("Modifier.date.*", DateExtensions::date);
    }

    fun now(context: FunctionExecuteContext?): Any {
        val now = Instant.now();
        return now;
    }

    private val localeCache = ConcurrentHashMap<String, Locale>()

    private fun date(context: FunctionExecuteContext): Any? {
        val value = context.firstParameter;
        val method = ConvertUtils.tryToString(context.secondParameter) ?: "";

        when (method.lowercase()) {
            "add" -> {
                val date = getDate(value) ?: return null;

                val valueToAdd = ConvertUtils.tryParseLong(context.thirdParameter) ?: 0;
                val type = ConvertUtils.tryToString(context.fourthParameter) ?: "Seconds";
                val chronoUnit = ChronoUnit.valueOf(type);
                if (chronoUnit > ChronoUnit.DAYS) {
                    // we need to convert to date as Instant does not allow add on bigger units
                    val local = LocalDateTime.ofEpochSecond(date.epochSecond, 0, ZoneOffset.UTC);
                    val newValue = local.plus(valueToAdd, chronoUnit)
                    // let's go back to Instant
                    return Instant.ofEpochSecond(newValue.toEpochSecond(ZoneOffset.UTC));
                } else {
                    return date.plus(valueToAdd, chronoUnit);
                }
            }

            "parse" -> {
                if (value == null)
                    return null;

                // don't bother parsing
                if (value is InstantNode || value is Instant)
                    return value;

                val stringValue = ConvertUtils.tryToString(value);

                val options = context.fourthParameter as ObjectNode?

                val localeTag = options?.get("locale")?.textValue() ?: "en_US"

                val locale = localeCache.getOrPut(localeTag) {
                    LocaleUtils.toLocale(localeTag)
                };

                var formatToParse = context.thirdParameter;

                // careful here as sometimes we get TextNodes back which are iterable
                if (formatToParse is TextNode)
                    formatToParse = formatToParse.textValue();

                if (formatToParse is Iterable<Any?>) {
                    // we have multiple formats?
                    for (format in formatToParse) {
                        val parsedValue =
                            tryParseDatetime(ConvertUtils.tryToString(format), stringValue, locale, context);
                        if (parsedValue != null)
                            return parsedValue;
                    }
                    return null;
                } else {
                    return tryParseDatetime(formatToParse, stringValue, locale, context)
                }
            }

            "part" -> {
                val date = getDate(value) ?: return null;
                val local = LocalDateTime.ofEpochSecond(date.epochSecond, 0, ZoneOffset.UTC);

                val part = ConvertUtils.tryToString(context.thirdParameter)?.lowercase() ?: "";
                return when (part) {
                    "year" -> local.year;
                    "month" -> local.monthValue;
                    "day" -> local.dayOfMonth;
                    "dayOfWeek" -> local.dayOfWeek;
                    "dayOfYear" -> local.dayOfYear;
                    "hour" -> local.hour;
                    "minute" -> local.minute;
                    "second" -> local.second;
                    else -> ""
                }
            }

            "fromepochmillis" -> {
                val longValue = ConvertUtils.tryParseLong(value) ?: 0;
                return Instant.ofEpochMilli(longValue)
            }

            "fromepochseconds" -> {
                val longValue = ConvertUtils.tryParseLong(value) ?: 0;
                return Instant.ofEpochSecond(longValue);
            }

            "totimezone" -> {
                val date = getDate(value) ?: return null;
                val timeZone = ConvertUtils.tryToString(context.thirdParameter) ?: "UTC";

                try {
                    val zoneId = ZoneId.of(timeZone);
                    // Convert to ZonedDateTime with the specified timezone
                    val zonedDateTime = date.atZone(zoneId);
                    // Return as Instant (which is timezone-independent)
                    return zonedDateTime.toInstant();
                } catch (e: Exception) {
                    throw TransformException("Invalid timezone: $timeZone", context.command.token.position);
                }
            }

            "toutc" -> {
                val date = getDate(value) ?: return null;
                // Instant is already in UTC, but we return it for consistency
                return date;
            }

            "tolocal" -> {
                val date = getDate(value) ?: return null;
                // Convert to local timezone (system default)
                val localZoneId = ZoneId.systemDefault();
                val zonedDateTime = date.atZone(localZoneId);
                return zonedDateTime.toInstant();
            }

            else -> throw TransformException("Unsupported | date.$method operation.", context.command.token.position);
        }
    }

    private fun tryParseDatetime(
        format: Any?,
        stringValue: String?,
        locale: Locale,
        context: FunctionExecuteContext
    ): Instant? {
        val useFormat = if (format == "ISO_8601") ConvertUtils.ISO8601DefaultParseFormat else format;

        val formatToParse = ConvertUtils.tryToString(useFormat) ?: ConvertUtils.IsoDateTimeFormatOptional;

        try {
            val usesAmPmFormat = formatToParse.contains("h")
            val defaultHourField = if (usesAmPmFormat) ChronoField.CLOCK_HOUR_OF_AMPM else ChronoField.HOUR_OF_DAY

            val builder = DateTimeFormatterBuilder()
                .appendPattern(formatToParse)
                .parseDefaulting(defaultHourField, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                .parseLenient()
                .toFormatter()
                .withZone(ZoneId.of("UTC"))
                .withLocale(locale)

            return builder.parse(stringValue, Instant::from)
        } catch (e: DateTimeParseException) {
            return null;
        }
    }

    fun getDate(value: Any?): Instant? {
        return when (value) {
            null -> null;
            is Instant -> value;
            is InstantNode -> value.value;
            else -> null;
        }
    }
}
