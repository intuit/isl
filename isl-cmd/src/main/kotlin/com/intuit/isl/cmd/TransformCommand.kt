package com.intuit.isl.cmd

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intuit.isl.common.OperationContext
import com.intuit.isl.common.TransformVariable
import com.intuit.isl.commands.CoverageStatementIdAssigner
import com.intuit.isl.debug.CodeCoverageHook
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.runtime.TransformModule
import kotlinx.coroutines.runBlocking
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import kotlin.system.exitProcess

/**
 * Command to execute ISL transformation scripts
 */
@Command(
    name = "transform",
    aliases = ["run", "exec"],
    description = ["Execute an ISL transformation script"]
)
class TransformCommand : Runnable {
    
    @Parameters(
        index = "0",
        description = ["ISL script file to execute"]
    )
    lateinit var scriptFile: File
    
    @Option(
        names = ["-i", "--input"],
        description = ["Input data file (JSON or YAML)"]
    )
    var inputFile: File? = null
    
    @Option(
        names = ["-o", "--output"],
        description = ["Output file (defaults to stdout)"]
    )
    var outputFile: File? = null
    
    @Option(
        names = ["-v", "--vars"],
        description = ["Variables file (JSON or YAML)"]
    )
    var varsFile: File? = null
    
    @Option(
        names = ["-p", "--param"],
        description = ["Parameters in key=value format (can be specified multiple times)"]
    )
    var params: Array<String> = emptyArray()
    
    @Option(
        names = ["-f", "--format"],
        description = ["Output format: json, yaml, pretty-json (default: json, or inferred from -o file extension)"]
    )
    var format: String = "json"
    
    @Option(
        names = ["--pretty"],
        description = ["Pretty print output"]
    )
    var pretty: Boolean = false
    
    @Option(
        names = ["--function"],
        description = ["Function name to execute (default: run)"]
    )
    var functionName: String = "run"

    @Option(
        names = ["--coverage-report"],
        description = ["Write code coverage (JSON) for editor extensions after a successful run"]
    )
    var coverageReportFile: File? = null

    override fun run() {
        try {
            // Infer output format from output file extension when not explicitly set
            if (outputFile != null && format == "json") {
                when (outputFile!!.extension.lowercase()) {
                    "yaml", "yml" -> format = "yaml"
                    "json" -> { /* keep json */ }
                    "txt" -> format = "txt"
                    else -> { /* keep json */ }
                }
            }
            
            // Validate script file
            if (!scriptFile.exists()) {
                System.err.println("Error: Script file not found: ${scriptFile.absolutePath}")
                exitProcess(1)
            }
            
            val scriptContent = scriptFile.readText()
            
            // Load input data
            val inputData = when {
                inputFile != null -> {
                    if (!inputFile!!.exists()) {
                        System.err.println("Error: Input file not found: ${inputFile!!.absolutePath}")
                        exitProcess(1)
                    }
                    parseFile(inputFile!!)
                }
                else -> null
            }
            
            // Load variables
            val variables = mutableMapOf<String, Any?>()
            
            // From file
            if (varsFile != null) {
                if (!varsFile!!.exists()) {
                    System.err.println("Error: Variables file not found: ${varsFile!!.absolutePath}")
                    exitProcess(1)
                }
                val varsMap = parseFile(varsFile!!) as? Map<*, *>
                varsMap?.forEach { (k, v) ->
                    variables[k.toString()] = v
                }
            }
            
            // From command line params
            params.forEach { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) {
                    variables[parts[0]] = parseValue(parts[1])
                } else {
                    System.err.println("Warning: Invalid parameter format: $param (expected key=value)")
                }
            }
            
            // Add input data to variables if provided:
            // - JSON/YAML root object → bind each top-level key as $key (same as debug adapter / YAML tests)
            // - array or scalar → legacy single variable $input
            when (inputData) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val map = inputData as Map<*, *>
                    map.forEach { (k, v) ->
                        variables[k.toString()] = v
                    }
                }
                else -> {
                    if (inputData != null) {
                        variables["input"] = inputData
                    }
                }
            }

            // When -i was used, add inputFileName into $context (merge; do not replace user JSON)
            if (inputFile != null) {
                val fileName = inputFile!!.name
                val ctx = variables["context"]
                variables["context"] = when (ctx) {
                    null -> mapOf("inputFileName" to fileName)
                    is MutableMap<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        val m = ctx as MutableMap<String, Any?>
                        m["inputFileName"] = fileName
                        m
                    }
                    is Map<*, *> -> {
                        val m = LinkedHashMap<String, Any?>()
                        ctx.forEach { (k, v) -> m[k.toString()] = v }
                        m["inputFileName"] = fileName
                        m
                    }
                    else -> mapOf("inputFileName" to fileName)
                }
            }
            
            // Execute transformation using shared module resolution (supports relative imports like ../customer.isl)
            val transformPackage = IslModuleResolver.compileSingleFile(scriptFile, scriptContent)
            val transformer = transformPackage.getModule(scriptFile.name)
                ?: throw IllegalStateException("Compiled module '${scriptFile.name}' not found in package")
            val coverageModules: List<TransformModule> =
                transformPackage.modules.mapNotNull { transformPackage.getModule(it)?.module }

            // Create operation context with variables and CLI extensions (e.g. Log)
            val context = OperationContext()
            LogExtensions.registerExtensions(context)
            variables.forEach { (key, value) ->
                val varName = if (key.startsWith("$")) key else "$" + key
                val varValue = JsonConvert.convert(value)
                val valuePreview = varValue.toString().let { if (it.length > 10) it.take(10) + "..." else it }
                println("Setting variable $varName to $valuePreview")
                context.setVariable(varName, TransformVariable(varValue, readOnly = false, global = true))
            }
            
            val coverageHook = if (coverageReportFile != null) CodeCoverageHook() else null
            if (coverageHook != null) {
                for (m in coverageModules) {
                    CoverageStatementIdAssigner.assign(m)
                }
            }
            val result = runBlocking {
                transformer.runTransformAsync(functionName, context, coverageHook)
            }

            if (coverageReportFile != null && coverageHook != null) {
                try {
                    coverageReportFile!!.parentFile?.mkdirs()
                    CoverageReportJson.write(coverageReportFile!!, scriptFile.name, coverageHook, coverageModules)
                } catch (e: Exception) {
                    System.err.println("Warning: Failed to write coverage report: ${e.message}")
                }
            }

            // Format and output result
            val output = formatOutput(result.result, format, pretty)
            
            if (outputFile != null) {
                outputFile!!.writeText(output)
                // When -o/--output is set, result goes only to the file (no console output)
            } else {
                println(output)
            }
            
        } catch (e: Exception) {
            System.err.println("Error: ${e.message}")
            if (System.getProperty("debug") == "true") {
                e.printStackTrace()
            }
            exitProcess(1)
        }
    }
    
    private fun parseFile(file: File): Any? {
        val content = file.readText()
        return when {
            file.extension.lowercase() in listOf("yaml", "yml") -> {
                val mapper = ObjectMapper(YAMLFactory())
                mapper.readValue<Any>(content)
            }
            else -> {
                val mapper = jacksonObjectMapper()
                mapper.readValue<Any>(content)
            }
        }
    }
    
    private fun parseValue(value: String): Any? {
        return when {
            value == "null" -> null
            value == "true" -> true
            value == "false" -> false
            value.toIntOrNull() != null -> value.toInt()
            value.toDoubleOrNull() != null -> value.toDouble()
            value.startsWith("{") || value.startsWith("[") -> {
                try {
                    jacksonObjectMapper().readTree(value)
                } catch (e: Exception) {
                    System.err.println("Failed to process='$value' as JSON: ${e.message}. Value will be interpreted as a string.")
                    value
                }
            }
            else -> value
        }
    }
    
    private fun formatOutput(result: Any?, format: String, pretty: Boolean): String {
        val mapper = when (format.lowercase()) {
            "yaml", "yml" -> ObjectMapper(YAMLFactory())
            "txt" -> return ConvertUtils.tryToString( result ) ?: ""
            else -> jacksonObjectMapper()
        }
        
        if (pretty || format == "pretty-json") {
            mapper.enable(SerializationFeature.INDENT_OUTPUT)
        }
        
        return when (result) {
            is JsonNode -> mapper.writeValueAsString(result)
            is String -> result
            null -> ""
            else -> mapper.writeValueAsString(result)
        }
    }
}

