package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.containsAll
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import java.io.StringReader
import java.util.*

/**
 * String manipulation modifier extensions for ISL.
 * 
 * Provides modifiers for string operations:
 * - Trimming (trim, trimStart, trimEnd)
 * - Substring operations (left, right, substring, substringUpto, substringAfter)
 * - Case conversion (lowerCase, upperCase)
 * - String manipulation (replace, remove, concat, append, split)
 * - CSV parsing
 * - UUID sanitization
 */
object StringModifierExtensions {
    
    fun registerExtensions(context: IOperationContext) {
        // String modifiers
        context.registerExtensionMethod("Modifier.trim", StringModifierExtensions::trim)
        context.registerExtensionMethod("Modifier.trimStart", StringModifierExtensions::trimStart)
        context.registerExtensionMethod("Modifier.trimEnd", StringModifierExtensions::trimEnd)
        context.registerExtensionMethod("Modifier.cap", StringModifierExtensions::left)
        context.registerExtensionMethod("Modifier.left", StringModifierExtensions::left)
        context.registerExtensionMethod("Modifier.right", StringModifierExtensions::right)
        context.registerExtensionMethod("Modifier.substring", StringModifierExtensions::subString)
        context.registerExtensionMethod("Modifier.sanitizeTid", StringModifierExtensions::sanitizeTid)
        context.registerExtensionMethod("Modifier.lowerCase", StringModifierExtensions::lowerCase)
        context.registerExtensionMethod("Modifier.upperCase", StringModifierExtensions::upperCase)
        context.registerExtensionMethod("Modifier.substringUpto", StringModifierExtensions::substringUpto)
        context.registerExtensionMethod("Modifier.substringAfter", StringModifierExtensions::substringAfter)
        context.registerExtensionMethod("Modifier.replace", StringModifierExtensions::replace)
        context.registerExtensionMethod("Modifier.remove", StringModifierExtensions::remove)
        context.registerExtensionMethod("Modifier.concat", StringModifierExtensions::concat)
        context.registerExtensionMethod("Modifier.append", StringModifierExtensions::append)
        context.registerExtensionMethod("Modifier.split", StringModifierExtensions::split)
        context.registerExtensionMethod("Modifier.csv.*", StringModifierExtensions::csv)
        
        // New modifiers
        context.registerExtensionMethod("Modifier.padStart", StringModifierExtensions::padStart)
        context.registerExtensionMethod("Modifier.padEnd", StringModifierExtensions::padEnd)
        context.registerExtensionMethod("Modifier.reverse", StringModifierExtensions::reverse)
        context.registerExtensionMethod("Modifier.capitalize", StringModifierExtensions::capitalize)
        context.registerExtensionMethod("Modifier.titleCase", StringModifierExtensions::titleCase)
        context.registerExtensionMethod("Modifier.camelCase", StringModifierExtensions::camelCase)
        context.registerExtensionMethod("Modifier.snakeCase", StringModifierExtensions::snakeCase)
        context.registerExtensionMethod("Modifier.truncate", StringModifierExtensions::truncate)
        context.registerExtensionMethod("Modifier.html.*", StringModifierExtensions::html)
    }
    
    private fun trim(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        // Performance: Fast-path for String to avoid conversion
        val stringValue = when (first) {
            is String -> first
            is TextNode -> first.asText()
            else -> ConvertUtils.tryToString(first)
        }
        
        if (context.parameters.size > 1) {
            // we have a list of trim chars
            val chars = (context.secondParameter as? String ?: "").toCharArray()
            return stringValue?.trim(*chars)
        } else {
            return stringValue?.trim()
        }
    }
    
    private fun trimStart(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        // Performance: Fast-path for String to avoid conversion
        val stringValue = when (first) {
            is String -> first
            is TextNode -> first.asText()
            else -> ConvertUtils.tryToString(first)
        }
        
        if (context.parameters.size > 1) {
            // we have a list of trim chars
            val chars = (context.secondParameter as? String ?: "").toCharArray()
            return stringValue?.trimStart(*chars)
        } else {
            return stringValue?.trimStart()
        }
    }
    
    private fun trimEnd(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        // Performance: Fast-path for String to avoid conversion
        val stringValue = when (first) {
            is String -> first
            is TextNode -> first.asText()
            else -> ConvertUtils.tryToString(first)
        }
        
        if (context.parameters.size > 1) {
            // we have a list of trim chars
            val chars = (context.secondParameter as? String ?: "").toCharArray()
            return stringValue?.trimEnd(*chars)
        } else {
            return stringValue?.trimEnd()
        }
    }
    
    private fun left(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val stringValue = ConvertUtils.tryToString(first)
        if (stringValue.isNullOrEmpty())
            return stringValue
        
        val to = Math.min(stringValue.length, ConvertUtils.tryParseInt(context.secondParameter?.toString(), 0) ?: 0)
        
        return stringValue.substring(0, to)
    }
    
    private fun right(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val stringValue = ConvertUtils.tryToString(first)
        if (stringValue.isNullOrEmpty())
            return stringValue
        
        val to = Math.min(stringValue.length, ConvertUtils.tryParseInt(context.secondParameter?.toString(), 0) ?: 0)
        
        return stringValue.substring(stringValue.length - to, stringValue.length)
    }
    
    private fun subString(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val stringValue = ConvertUtils.tryToString(first)
        
        if (stringValue.isNullOrEmpty())
            return stringValue
        
        val from = Math.min(
            stringValue.length,
            Math.max(0, ConvertUtils.tryParseInt(context.secondParameter?.toString(), 0) ?: 0)
        )
        
        val to = Math.min(
            stringValue.length,
            Math.max(from, ConvertUtils.tryParseInt(context.thirdParameter?.toString()) ?: 0)
        )
        
        return stringValue.substring(from, to)
    }
    
    private fun substringUpto(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val second = if (context.parameters.size > 1) context.secondParameter else ""
        
        val stringValue = ConvertUtils.tryToString(first)
        val delim = ConvertUtils.tryToString(second)
        return stringValue?.substringBefore(delim.toString())
    }
    
    private fun substringAfter(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val second = if (context.parameters.size > 1) context.secondParameter else ""
        
        val stringValue = ConvertUtils.tryToString(first)
        val delim = ConvertUtils.tryToString(second)
        return stringValue?.substringAfter(delim.toString())
    }
    
    private fun replace(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val second = if (context.parameters.size > 1) context.secondParameter else ""
        val third = if (context.parameters.size > 2) context.thirdParameter else ""
        
        val stringValue = ConvertUtils.tryToString(first)
        val replaceWhat = ConvertUtils.tryToString(second)
        val replaceWith = ConvertUtils.tryToString(third)
        return stringValue?.replace(replaceWhat.toString(), replaceWith.toString())
    }
    
    private fun remove(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val second = if (context.parameters.size > 1) context.secondParameter else ""
        
        val stringValue = ConvertUtils.tryToString(first)
        val removePart = ConvertUtils.tryToString(second)
        return stringValue?.replace(removePart.toString(), "")
    }
    
    private fun concat(context: FunctionExecuteContext): Any {
        val first = context.firstParameter
        val second = context.secondParameter
        val delim = ConvertUtils.tryToString(
            if (context.parameters.size > 2) context.thirdParameter else ""
        ).orEmpty()
        
        val stringValue = ConvertUtils.tryToString(first).orEmpty()
        val concatWith = ConvertUtils.tryToString(second).orEmpty()
        return stringValue.plus(delim).plus(concatWith)
    }
    
    /**
     * Append any number of strings to an existing string
     * simpler than | concat( value, delimiter )
     */
    private fun append(context: FunctionExecuteContext): Any {
        val first = ConvertUtils.tryToString(context.firstParameter)
        val sb = StringBuilder(first)
        for (p in context.parameters.drop(1)) {
            val value = ConvertUtils.tryToString(p)
            if (value != null)
                sb.append(value)
        }
        return sb.toString()
    }
    
    private fun split(context: FunctionExecuteContext): Any {
        val first = ConvertUtils.tryToString(context.firstParameter) ?: ""
        val second = ConvertUtils.tryToString(context.secondParameter) ?: ","
        
        val items = first.split(second)
        
        return items
    }
    
    // CSV
    private fun csv(context: FunctionExecuteContext): Any? {
        val text = ConvertUtils.tryToString(context.firstParameter) ?: ""
        
        val method = ConvertUtils.tryToString(context.secondParameter) ?: ""
        
        val options = context.thirdParameter as ObjectNode?
        
        val skipLines = ConvertUtils.tryParseInt(options?.get("skipLines")) ?: 0
        val separator = ConvertUtils.tryToString(options?.get("separator"))?.firstOrNull() ?: ','
        val escapeChar = ConvertUtils.tryToString(options?.get("escapeChar"))?.firstOrNull() ?: '\\'
        val quoteChar = ConvertUtils.tryToString(options?.get("quoteChar"))?.firstOrNull()
        
        val reader = getCsvReader(text, separator, escapeChar, quoteChar, skipLines)
        
        return when (method.lowercase()) {
            "findrow" -> {
                val seek: Array<String?> =
                    (options?.get("seek") as? ArrayNode?)?.map { it?.textValue() }?.toTypedArray() ?: return null
                val maxRows = ConvertUtils.tryParseInt(options.get("maxRows")) ?: Integer.MAX_VALUE
                
                var line: Array<String?>?
                var i = 0
                
                while (reader.readNext().also { line = it } != null) {
                    if (line!!.containsAll(seek)) {
                        break
                    }
                    i++
                    if (i >= maxRows) {
                        line = null
                        break
                    }
                }
                if (line == null) null else i
            }
            
            "parsemultiline" -> {
                var line: Array<String?>?
                
                var headers = (options?.get("headers") as? ArrayNode?)?.map { it?.textValue() }?.toTypedArray()
                
                // do we need to parse a header?
                if (headers == null) {
                    // assume first line contains the headers
                    if (reader.readNext().also { line = it } != null) {
                        headers = line
                    }
                }
                if (headers == null) {
                    headers = arrayOf()
                }
                
                val result = JsonNodeFactory.instance.arrayNode(10)
                
                while (reader.readNext().also { line = it } != null) {
                    val item = JsonNodeFactory.instance.objectNode()
                    line?.forEachIndexed { i, it ->
                        if (i < headers.size) {
                            item.put(headers[i], it)
                        } else {
                            // make up columns named Col0, Col1, ...
                            item.put("Col${i + 1}", it)
                        }
                    }
                    result.add(item)
                }
                result
            }
            
            else -> "Unknown method: csv.$method"
        }
    }
    
    private fun getCsvReader(
        text: String,
        separator: Char,
        escapeChar: Char,
        quoteChar: Char?,
        skipLines: Int
    ): CSVReader {
        var csvParseBuilder = CSVParserBuilder()
            .withSeparator(separator)
            .withEscapeChar(escapeChar)
        
        csvParseBuilder = if (quoteChar == null) {
            // If a quote character is not specified we will ignore quotations.
            csvParseBuilder.withIgnoreQuotations(true)
        } else {
            csvParseBuilder.withQuoteChar(quoteChar)
        }
        
        val parser = csvParseBuilder.build()
        
        return CSVReaderBuilder(StringReader(text))
            .withSkipLines(skipLines)
            .withCSVParser(parser)
            .build()
    }
    
    private fun sanitizeTid(context: FunctionExecuteContext): Any {
        val first = context.firstParameter
        
        val stringValue = ConvertUtils.tryToString(first)
        
        // standard UUID
        if (stringValue?.length == 36) {
            try {
                val checkedUUID = UUID.fromString(stringValue)
                return checkedUUID
            } catch (e: IllegalArgumentException) {
                // nothing to do at this point
            }
        }
        return UUID.randomUUID().toString()
    }
    
    private fun lowerCase(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        // Performance: Fast-path for String to avoid conversion
        val stringValue = when (first) {
            is String -> first
            is TextNode -> first.asText()
            else -> ConvertUtils.tryToString(first)
        }
        return stringValue?.lowercase()
    }
    
    private fun upperCase(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        // Performance: Fast-path for String to avoid conversion
        val stringValue = when (first) {
            is String -> first
            is TextNode -> first.asText()
            else -> ConvertUtils.tryToString(first)
        }
        return stringValue?.uppercase()
    }
    
    /**
     * Pad string at the start to reach target length
     */
    private fun padStart(context: FunctionExecuteContext): Any? {
        val stringValue = ConvertUtils.tryToString(context.firstParameter) ?: ""
        val length = ConvertUtils.tryParseInt(context.secondParameter) ?: return stringValue
        val padChar = ConvertUtils.tryToString(context.thirdParameter)?.firstOrNull() ?: ' '
        
        return stringValue.padStart(length, padChar)
    }
    
    /**
     * Pad string at the end to reach target length
     */
    private fun padEnd(context: FunctionExecuteContext): Any? {
        val stringValue = ConvertUtils.tryToString(context.firstParameter) ?: ""
        val length = ConvertUtils.tryParseInt(context.secondParameter) ?: return stringValue
        val padChar = ConvertUtils.tryToString(context.thirdParameter)?.firstOrNull() ?: ' '
        
        return stringValue.padEnd(length, padChar)
    }
    
    /**
     * Reverse a string
     */
    private fun reverse(context: FunctionExecuteContext): Any? {
        val stringValue = ConvertUtils.tryToString(context.firstParameter)
        return stringValue?.reversed()
    }
    
    /**
     * Capitalize first letter of string
     */
    private fun capitalize(context: FunctionExecuteContext): Any? {
        val stringValue = ConvertUtils.tryToString(context.firstParameter)
        if (stringValue.isNullOrEmpty()) return stringValue
        
        return stringValue[0].uppercase() + stringValue.substring(1)
    }
    
    /**
     * Convert string to title case (capitalize each word)
     */
    private fun titleCase(context: FunctionExecuteContext): Any? {
        val stringValue = ConvertUtils.tryToString(context.firstParameter)
        if (stringValue.isNullOrEmpty()) return stringValue
        
        return stringValue.split(" ").joinToString(" ") { word ->
            if (word.isEmpty()) word
            else word[0].uppercase() + word.substring(1).lowercase()
        }
    }
    
    /**
     * Convert string to camelCase
     */
    private fun camelCase(context: FunctionExecuteContext): Any? {
        val stringValue = ConvertUtils.tryToString(context.firstParameter)
        if (stringValue.isNullOrEmpty()) return stringValue
        
        // Split on spaces, hyphens, underscores
        val words = stringValue.split(Regex("[\\s_-]+"))
        if (words.isEmpty()) return stringValue
        
        return words.mapIndexed { index, word ->
            if (word.isEmpty()) ""
            else if (index == 0) word.lowercase()
            else word[0].uppercase() + word.substring(1).lowercase()
        }.joinToString("")
    }
    
    /**
     * Convert string to snake_case
     */
    private fun snakeCase(context: FunctionExecuteContext): Any? {
        val stringValue = ConvertUtils.tryToString(context.firstParameter)
        if (stringValue.isNullOrEmpty()) return stringValue
        
        // Handle camelCase and PascalCase
        var result = stringValue.replace(Regex("([a-z])([A-Z])"), "$1_$2")
        // Replace spaces and hyphens with underscores
        result = result.replace(Regex("[\\s-]+"), "_")
        
        return result.lowercase()
    }
    
    /**
     * Truncate string to specified length with optional suffix
     */
    private fun truncate(context: FunctionExecuteContext): Any? {
        val stringValue = ConvertUtils.tryToString(context.firstParameter) ?: ""
        val maxLength = ConvertUtils.tryParseInt(context.secondParameter) ?: return stringValue
        val suffix = ConvertUtils.tryToString(context.thirdParameter) ?: "..."
        
        if (stringValue.length <= maxLength) return stringValue
        
        val truncateAt = maxLength - suffix.length
        if (truncateAt <= 0) return suffix.take(maxLength)
        
        return stringValue.substring(0, truncateAt) + suffix
    }
    
    /**
     * HTML encoding/decoding
     */
    private fun html(context: FunctionExecuteContext): Any? {
        val value = ConvertUtils.tryToString(context.firstParameter) ?: ""
        val method = ConvertUtils.tryToString(context.secondParameter) ?: ""
        
        return when (method.lowercase()) {
            "escape" -> {
                value
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;")
            }
            "unescape" -> {
                value
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace("&#x27;", "'")
                    .replace("&#x2F;", "/")
                    .replace("&#47;", "/")
            }
            else -> "Unknown method: html.$method"
        }
    }
}

