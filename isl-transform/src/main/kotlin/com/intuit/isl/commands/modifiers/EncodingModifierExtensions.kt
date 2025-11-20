package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.Position
import com.google.common.net.UrlEscapers
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Encoding and decoding modifier extensions for ISL.
 * 
 * Provides modifiers for encoding/decoding data:
 * - encode.base64, encode.base64url, encode.path, encode.query
 * - decode.base64, decode.base64url, decode.query
 */
object EncodingModifierExtensions {
    
    fun registerExtensions(context: IOperationContext) {
        // Encoding/Decoding modifiers
        context.registerExtensionMethod("Modifier.encode.*", EncodingModifierExtensions::encode)
        context.registerExtensionMethod("Modifier.decode.*", EncodingModifierExtensions::decode)
    }
    
    private fun encode(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val format = ConvertUtils.tryToString(context.secondParameter)
        val options = context.thirdParameter as ObjectNode?
        
        return internalEncode(first, format, options, context.command.token.position)
    }
    
    internal fun internalEncode(first: Any?, format: String?, options: ObjectNode?, position: Position): String? {
        val value = ConvertUtils.tryToString(first) ?: ""
        val withoutPadding = options?.get("withoutPadding")?.booleanValue() ?: false
        
        return when (format) {
            "string" -> value
            "base64" -> {
                val encoder = if (withoutPadding) Base64.getEncoder().withoutPadding() else Base64.getEncoder()
                encoder.encodeToString(ConvertUtils.getByteArray(first))
            }
            "base64url" -> {
                val encoder = if (withoutPadding) Base64.getUrlEncoder().withoutPadding() else Base64.getUrlEncoder()
                encoder.encodeToString(ConvertUtils.getByteArray(first))
            }
            "path" -> UrlEscapers.urlFragmentEscaper().escape(value)
            "query" -> URLEncoder.encode(value, StandardCharsets.UTF_8.name())
            else -> throw TransformException("Unsupported encoding method:$format", position)
        }
    }
    
    private fun decode(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter
        val value = ConvertUtils.tryToString(first) ?: ""
        val format = ConvertUtils.tryToString(context.secondParameter)
        
        return try {
            // additional methods can be added here
            when (format) {
                "string" -> value
                "base64" -> Base64.getDecoder().decode(value)
                "base64url" -> Base64.getUrlDecoder().decode(value)
                // Java or Guava don't have a reverse for converting from a % encoded string to normal string
                //"path" -> UrlEscapers.urlFragmentEscaper().(value);
                "query" -> URLDecoder.decode(value, StandardCharsets.UTF_8.name())
                else -> throw TransformException("Unsupported decoding method:$format", context.command.token.position)
            }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

