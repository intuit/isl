package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.node.BinaryNode
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.ConvertUtils
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Compression modifier extensions for ISL.
 * 
 * Provides modifiers for compression operations:
 * - gzip (compress string to gzipped bytes)
 * - gunzip (decompress gzipped bytes to string)
 * - gunzipToByte (decompress gzipped bytes to byte array)
 */
object CompressionModifierExtensions {
    
    fun registerExtensions(context: IOperationContext) {
        // Compression modifiers
        context.registerExtensionMethod("Modifier.gzip", CompressionModifierExtensions::gzip)
        context.registerExtensionMethod("Modifier.gunzip", CompressionModifierExtensions::gunzip)
        context.registerExtensionMethod("Modifier.gunzipToByte", CompressionModifierExtensions::gunzipToByte)
    }
    
    private fun gunzip(context: FunctionExecuteContext): Any? {
        val content = context.firstParameter
        val charsetString = ConvertUtils.tryToString(context.secondParameter)
        val charset = if (charsetString == null) Charsets.UTF_8 else Charset.forName(charsetString)
        
        // TODO: Unzip String format
        if (content is ByteArray) {
            return GZIPInputStream(content.inputStream()).bufferedReader(charset)
                .use { it.readText() }
        }
        
        if (content is BinaryNode) {
            return GZIPInputStream(content.binaryValue().inputStream()).bufferedReader(charset)
                .use { it.readText() }
        }
        throw TransformException(
            "Invalid content for gunzip " + content!!::class.java.typeName,
            context.command.token.position
        )
    }
    
    // TODO: update gunzip to return Byte array and remove gunzipToByte
    private fun gunzipToByte(context: FunctionExecuteContext): ByteArray? {
        val content = context.firstParameter
        
        // TODO: Unzip String format
        if (content is ByteArray) {
            return GZIPInputStream(content.inputStream()).use { it.readBytes() }
        }
        
        if (content is BinaryNode) {
            return GZIPInputStream(content.binaryValue().inputStream()).use { it.readBytes() }
        }
        throw TransformException(
            "Invalid content for gunzipToByte " + content!!::class.java.typeName,
            context.command.token.position
        )
    }
    
    private fun gzip(context: FunctionExecuteContext): Any? {
        val content = context.firstParameter.toString()
        val charsetString = ConvertUtils.tryToString(context.secondParameter)
        val charset = if (charsetString == null) Charsets.UTF_8 else Charset.forName(charsetString)
        
        val bos = ByteArrayOutputStream(content.length)
        
        GZIPOutputStream(bos).bufferedWriter(charset).use { it.write(content) }
        return bos.toByteArray()
    }
}

