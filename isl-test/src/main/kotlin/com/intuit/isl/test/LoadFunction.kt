package com.intuit.isl.test

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import java.io.File
import java.io.StringReader
import java.nio.file.Path

/**
 * Loads resources from files relative to the current ISL file.
 * Use @.Load.From("fileName") where fileName is relative to the directory of the current file.
 * Supports .json, .yaml, .yml, and .csv - all converted to JSON.
 */
object LoadFunction {
    private const val functionName = "Load"

    fun registerExtensions(context: IOperationContext) {
        context.registerExtensionMethod("$functionName.From") { ctx ->
            from(ctx)
        }
    }

    private fun from(context: FunctionExecuteContext): Any? {
        val fileName = ConvertUtils.tryToString(context.firstParameter)
            ?: throw IllegalArgumentException("@.Load.From requires a file name (string)")

        val testContext = context.executionContext.operationContext as? TestOperationContext
            ?: throw IllegalStateException("@.Load.From is only available in test context")

        val basePath = testContext.basePath
            ?: throw IllegalStateException("@.Load.From requires basePath; run tests via isl test command or pass basePath to TransformTestPackageBuilder")

        val currentFile = testContext.currentFile
            ?: throw IllegalStateException("@.Load.From requires currentFile; run tests via isl test command")

        val resolvedPath = resolvePath(basePath, currentFile, fileName)
        val file = resolvedPath.toFile()

        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $resolvedPath (resolved from $fileName relative to $currentFile)")
        }
        if (!file.isFile) {
            throw IllegalArgumentException("Not a file: $resolvedPath")
        }

        val ext = file.extension.lowercase()
        return when (ext) {
            "json" -> JsonConvert.mapper.readTree(file)
            "yaml", "yml" -> {
                val yamlMapper = com.fasterxml.jackson.databind.ObjectMapper(YAMLFactory())
                yamlMapper.readTree(file)
            }
            "csv" -> parseCsvToJson(file.readText())
            else -> throw IllegalArgumentException(
                "@.Load.From supports .json, .yaml, .yml, .csv; got: $fileName"
            )
        }
    }

    private fun resolvePath(basePath: Path, currentFile: String, fileName: String): Path {
        val currentDir = basePath.resolve(currentFile).parent ?: basePath
        return currentDir.resolve(fileName).normalize()
    }

    private fun parseCsvToJson(text: String): JsonNode {
        val parser = CSVParserBuilder()
            .withSeparator(',')
            .withEscapeChar('\\')
            .withIgnoreQuotations(false)
            .build()
        val reader = CSVReaderBuilder(StringReader(text))
            .withSkipLines(0)
            .withCSVParser(parser)
            .build()

        val result = JsonNodeFactory.instance.arrayNode()
        val firstLine = reader.readNext() ?: return result
        val headers = firstLine

        var line: Array<String?>?
        while (reader.readNext().also { line = it } != null) {
            val item = JsonNodeFactory.instance.objectNode()
            line?.forEachIndexed { i: Int, value: String? ->
                val key = if (i < headers.size) headers[i] ?: "Col$i" else "Col$i"
                item.put(key, value)
            }
            result.add(item)
        }
        return result
    }
}
