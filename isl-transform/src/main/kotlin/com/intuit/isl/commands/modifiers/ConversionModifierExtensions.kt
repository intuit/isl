package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BinaryNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.TextNode
import com.google.common.net.UrlEscapers
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.InstantNode
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.utils.ObjectRefNode
import com.intuit.isl.utils.Position
import com.intuit.isl.utils.zip.ZipObject
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import javax.mail.internet.InternetAddress

/**
 * Type conversion modifier extensions for ISL.
 * 
 * Provides modifiers for converting between types:
 * - to.boolean, to.number, to.string, to.decimal, to.array, to.object, to.xml
 * - to.hex, to.bytes, to.epochmillis
 * - hex.tobinary
 * - join.* (joining arrays/objects to strings)
 * - email.parse
 */
object ConversionModifierExtensions {
    
    // Performance optimization: Cache DateTimeFormatter instances to avoid repeated pattern compilation
    private val formatterCache = ConcurrentHashMap<String, DateTimeFormatter>()
    
    fun registerExtensions(context: IOperationContext) {
        // Conversion modifiers
        context.registerExtensionMethod("Modifier.to.*", ConversionModifierExtensions::convertTo)
        context.registerExtensionMethod("Modifier.hex.*", ConversionModifierExtensions::convertFromHex)
        context.registerExtensionMethod("Modifier.join.*", ConversionModifierExtensions::joinString)
        context.registerExtensionMethod("Modifier.email.*", ConversionModifierExtensions::email)
    }
    
    private fun convertTo(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val target = ConvertUtils.tryToString(context.secondParameter) ?: ""
        
        return when (target.lowercase()) {
            "boolean" -> ConvertUtils.tryToString(first).toBoolean()
            "number" -> {
                when (first) {
                    is Instant -> first.epochSecond
                    is InstantNode -> first.value.epochSecond
                    else -> ConvertUtils.tryToString(first)?.toBigDecimal()?.toLong()
                }
            }
            "epochmillis" -> {
                when (first) {
                    is Instant -> first.toEpochMilli()
                    is InstantNode -> first.value.toEpochMilli()
                    else -> 0
                }
            }
            "decimal" -> ConvertUtils.tryToString(first)?.toBigDecimal()
            "hex" -> {
                val byteArray = ConvertUtils.getByteArray(first)
                return byteArray.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
            }
            
            "bytes" -> {
                val charset = Charset.forName(ConvertUtils.tryToString(context.thirdParameter) ?: "UTF-8")
                when (first) {
                    is BinaryNode -> first.binaryValue()
                    is ByteArray -> first
                    is ObjectRefNode -> if (first.value is ZipObject) first.value.toByteArray() else ByteArray(0)
                    is TextNode -> first.asText().toByteArray(charset)
                    is String -> first.toByteArray(charset)
                    else -> ByteArray(0)
                }
            }
            
            "string" -> {
                when (first) {
                    is Instant -> formatInstant(first, context)
                    is InstantNode -> formatInstant(first.value, context)
                    else -> ConvertUtils.tryToString(first)
                }
            }
            "array" -> {
                when (first) {
                    is ArrayNode -> first
                    else -> JsonNodeFactory.instance.arrayNode().add(JsonConvert.convert(first))
                }
            }
            
            "object" -> {
                val result = JsonNodeFactory.instance.objectNode()
                // we expect an Array with key and value
                when (first) {
                    is ArrayNode -> {
                        first.forEach {
                            val value = it as? com.fasterxml.jackson.databind.node.ObjectNode?
                            val key = value?.get("key")?.textValue()
                            if (!key.isNullOrBlank())
                                result.set<JsonNode>(key, value.get("value"))
                        }
                    }
                }
                return result
            }
            
            "xml" -> {
                // output to xml - this is tricky - Java/Jackson are not good friends with XML for some reason
                // Unless you work from Pojo directly and you can use Annotations
                // https://github.com/FasterXML/jackson-dataformat-xml
                val root = ConvertUtils.tryToString(context.thirdParameter) ?: "root"
                
                return XmlModifierExtensions.toXml(first, root)
            }
            
            "json" -> {
                // Convert to JSON string
                val node = JsonConvert.convert(first)
                val indent = ConvertUtils.tryParseInt(context.thirdParameter)
                
                return if (indent != null && indent > 0) {
                    // Pretty print with indentation
                    com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(node)
                } else {
                    // Compact JSON
                    com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(node)
                }
            }
            
            "yaml" -> {
                // Convert to YAML string
                val node = JsonConvert.convert(first)
                val yamlMapper = com.fasterxml.jackson.dataformat.yaml.YAMLMapper()
                return yamlMapper.writeValueAsString(node)
            }
            
            "csv" -> {
                // Convert array of objects to CSV string
                val arrayNode = when (first) {
                    is ArrayNode -> first
                    else -> return "Input must be an array for CSV conversion"
                }
                
                if (arrayNode.isEmpty) return ""
                
                // Extract headers from first object
                val firstObj = arrayNode[0] as? com.fasterxml.jackson.databind.node.ObjectNode
                    ?: return "Array must contain objects for CSV conversion"
                
                val headers = firstObj.fieldNames().asSequence().toList()
                val delimiter = ConvertUtils.tryToString(context.thirdParameter) ?: ","
                
                // Build CSV string
                val csv = StringBuilder()
                
                // Add header row
                csv.append(headers.joinToString(delimiter) { escapeCSV(it, delimiter) })
                csv.append("\n")
                
                // Add data rows
                arrayNode.forEach { item ->
                    if (item is com.fasterxml.jackson.databind.node.ObjectNode) {
                        csv.append(headers.joinToString(delimiter) { header ->
                            val value = item.get(header)?.asText() ?: ""
                            escapeCSV(value, delimiter)
                        })
                        csv.append("\n")
                    }
                }
                
                return csv.toString()
            }
            
            else -> throw TransformException(
                "Unsupported conversion target:to.$target",
                context.command.token.position
            )
        }
    }
    
    /**
     * Escape CSV field value
     */
    private fun escapeCSV(value: String, delimiter: String): String {
        val needsQuoting = value.contains(delimiter) || value.contains("\"") || value.contains("\n")
        
        return if (needsQuoting) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
    
    private fun formatInstant(value: Instant, context: FunctionExecuteContext): String {
        val format = ConvertUtils.tryToString(context.thirdParameter)
        val formatter =
            if (format == null)
                ConvertUtils.IsoDateTimeFormatter
            else
                getOrCreateFormatter(format)
        return formatter.format(value)
    }
    
    /**
     * Get or create a cached DateTimeFormatter for the given pattern.
     * Performance optimization: Avoids repeated pattern compilation.
     */
    private fun getOrCreateFormatter(pattern: String): DateTimeFormatter {
        return formatterCache.getOrPut(pattern) {
            DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.UTC)
        }
    }
    
    private fun convertFromHex(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val stringValue = ConvertUtils.tryToString(first)
        
        val target = ConvertUtils.tryToString(context.secondParameter)
        
        when (target) {
            "tobinary" -> {
                val convertValue = stringValue ?: ""
                val result = ByteArray(convertValue.length / 2)
                for (i in 0 until convertValue.length step 2) {
                    val v =
                        ((Character.digit(convertValue[i], 16) shl 4) + Character.digit(convertValue[i + 1], 16))
                    result[i / 2] = v.toByte()
                }
                
                return result
            }
            
            else -> throw TransformException(
                "Unsupported conversion target:hex.$target",
                context.command.token.position
            )
        }
    }
    
    private fun joinString(context: FunctionExecuteContext): Any {
        val first = context.firstParameter
        
        val encoding = ConvertUtils.tryToString(context.secondParameter)
        val itemDelimiter = ConvertUtils.tryToString(context.thirdParameter) ?: ","
        val fieldDelimiter = ConvertUtils.tryToString(context.fourthParameter) ?: "="
        
        return when (first) {
            is com.fasterxml.jackson.databind.node.ObjectNode -> {
                val result = first.fields()
                    .asSequence()
                    .joinToString(separator = itemDelimiter) {
                        internalEncode(
                            it.key,
                            encoding,
                            null,
                            context.command.token.position
                        ) + fieldDelimiter + internalEncode(
                            ConvertUtils.tryToString(it.value),
                            encoding,
                            null,
                            context.command.token.position
                        )
                    }
                return result
            }
            
            is ArrayNode -> {
                val result = first.joinToString(separator = itemDelimiter) {
                    internalEncode(
                        ConvertUtils.tryToString(it),
                        encoding,
                        null,
                        context.command.token.position
                    ).toString()
                }
                return result
            }
            
            is List<*> -> {
                val result = first.joinToString(separator = itemDelimiter) {
                    internalEncode(
                        ConvertUtils.tryToString(it),
                        encoding,
                        null,
                        context.command.token.position
                    ).toString()
                }
                return result
            }
            
            null -> ""
            else -> throw throw TransformException(
                "Unsupported conversion for=${first.javaClass.name} target=join.$encoding",
                context.command.token.position
            )
        }
    }
    
    private fun internalEncode(first: Any?, format: String?, options: com.fasterxml.jackson.databind.node.ObjectNode?, position: Position): String? {
        val value = ConvertUtils.tryToString(first) ?: ""
        
        return when (format) {
            "string" -> value
            "path" -> UrlEscapers.urlFragmentEscaper().escape(value)
            "query" -> URLEncoder.encode(value, StandardCharsets.UTF_8.name())
            else -> value
        }
    }
    
    private fun email(context: FunctionExecuteContext): Any? {
        val target = ConvertUtils.tryToString(context.secondParameter) ?: ""
        return when (target.lowercase()) {
            "parse" -> parseEmail(context)
            else -> throw TransformException(
                "Unsupported conversion target:to.$target",
                context.command.token.position
            )
        }
    }
    
    private fun parseEmail(functionContext: FunctionExecuteContext): Any? {
        val input = ConvertUtils.tryToString(functionContext.firstParameter)
        val jsonNodes: ArrayNode = JsonNodeFactory.instance.arrayNode()
        try {
            for (address in InternetAddress.parse(input)) {
                val result = JsonNodeFactory.instance.objectNode()
                result.put("name", address.getPersonal())
                result.put("emailAddress", address.getAddress())
                jsonNodes.add(result)
            }
        } catch (e: Exception) {
            throw TransformException(
                "Could not extract name and emailAddress from InternetAddress: $e",
                functionContext.command.token.position
            )
        }
        return jsonNodes
    }
}

