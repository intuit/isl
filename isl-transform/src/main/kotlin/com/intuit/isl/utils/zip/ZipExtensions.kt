package com.intuit.isl.utils.zip

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BinaryNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.TextNode
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.utils.ObjectRefNode
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.Inflater
import java.util.zip.Deflater

/**
 * Zip utility extension that supports zip and unzip.
 */
object ZipExtensions {

    fun registerExtensions(context: IOperationContext) {
        context.registerExtensionMethod("Zip.Start", ZipExtensions::start)
        context.registerExtensionMethod("Modifier.zip.*", ZipExtensions::handleZip)
        context.registerExtensionMethod("Modifier.unzip", ZipExtensions::unzip)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun start(context: FunctionExecuteContext): Any {
        return ZipObject()
    }

    private fun getZipObject(context: FunctionExecuteContext): ZipObject {
        val obj = context.firstParameter
        return when (obj) {
            is ZipObject -> obj
            is ObjectRefNode -> {
                if (obj.value is ZipObject) obj.value
                else throw TransformException("Not a zip object", context.command.token.position)
            }

            else -> throw TransformException("Not a zip object", context.command.token.position)
        }
    }

    private fun close(context: FunctionExecuteContext): Any {
        val zipObject = getZipObject(context)
        zipObject.close()
        return zipObject.toByteArray()
    }

    private fun inflate(context: FunctionExecuteContext): Any {
        val input = context.firstParameter
        val byteArray = ConvertUtils.getByteArray(input)

        val inflater = Inflater()
        inflater.setInput(byteArray)

        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)

        while (!inflater.finished()) {
            val size = inflater.inflate(buffer)
            outputStream.write(buffer, 0, size)
            if (size == 0)
                break;
        }

        inflater.end()
        return outputStream.toByteArray()
    }

    private fun deflate(context: FunctionExecuteContext): Any {
        val input = context.firstParameter
        val byteArray = ConvertUtils.getByteArray(input)

        val deflater = Deflater()
        deflater.setInput(byteArray)
        deflater.finish()

        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)

        while (!deflater.finished()) {
            val size = deflater.deflate(buffer)
            outputStream.write(buffer, 0, size)
            if (size == 0)
                break;
        }

        deflater.end()
        return outputStream.toByteArray()
    }

    private fun handleZip(context: FunctionExecuteContext): Any {
        val method = context.secondParameter.toString()
        return when (method) {
            "add" -> zip(context)
            "close" -> close(context)
            "inflate" -> inflate(context)
            "deflate" -> deflate(context)
            else -> throw TransformException("Invalid method; method=$method", context.command.token.position)
        }
    }

    /**
     * Adds given file to the ZipObject (ZipOutputStream).
     * Writes to the stream, closes entry, but leaves the stream open.
     * Example usage: @.Zip.Start() | zip.add("foo.txt", "hello world!", "UTF-8") | zip.add("bar.bin", $byteArray) | zip.close
     **/
    private fun zip(context: FunctionExecuteContext): Any {
        val zipObject = getZipObject(context)
        val fileName = ConvertUtils.tryToString(context.thirdParameter)
        if (fileName.isNullOrBlank()) {
            throw TransformException("Blank file name", context.command.token.position)
        }

        val fileContent = JsonConvert.convert(context.fourthParameter)
        val content: ByteArray = when (fileContent) {
            is BinaryNode -> {
                fileContent.binaryValue()
            }

            is TextNode -> {
                val charset = ConvertUtils.tryToString(context.fifthParameter) ?: "UTF-8"
                fileContent.asText().toByteArray(Charset.forName(charset))
            }

            else -> {
                throw TransformException("Invalid content type for zip", context.command.token.position)
            }
        }

        val entry = ZipEntry(fileName)
        entry.size = content.size.toLong()
        zipObject.putNextEntry(entry)
        zipObject.write(content)
        zipObject.closeEntry()
        return zipObject
    }

    /**
     * Takes a zipped byte array and unzips.
     * @return an array of such files [{name: 'foo.txt', content: <bytes>}].
     * Example usage: $unzipped = $zippedBytes | unzip;
     *                $contentString = $unzipped[0].content | to.string(“utf-8”);
     */
    private fun unzip(context: FunctionExecuteContext): ArrayNode {
        val content = when (val firstParam = context.firstParameter) {
            is ByteArray -> firstParam
            is BinaryNode -> firstParam.binaryValue()
            else -> throw TransformException(
                "Invalid content type for unzip " + firstParam!!::class.java.typeName,
                context.command.token.position
            )
        }

        val unzipped = JsonNodeFactory.instance.arrayNode()
        val zis = ZipInputStream(content.inputStream(), Charsets.UTF_8)
        var entry: ZipEntry?
        entry = zis.nextEntry
        while (entry != null) {
            val fileBos = ByteArrayOutputStream()
            zis.copyTo(fileBos)
            val file = JsonNodeFactory.instance.objectNode()
                .put("name", entry.name)
                .put("content", fileBos.toByteArray())
            unzipped.add(file)
            fileBos.close()
            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()
        return unzipped
    }
}
