package com.intuit.isl.cmd

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.FileInfo
import com.intuit.isl.runtime.TransformPackageBuilder
import com.intuit.isl.utils.JsonConvert
import kotlinx.coroutines.runBlocking
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
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
        description = ["Output format: json, yaml, pretty-json (default: json)"]
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
    
    override fun run() {
        try {
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
            
            // Add input data to variables if provided
            if (inputData != null) {
                variables["input"] = inputData
            }
            
            // Execute transformation using TransformPackageBuilder so relative imports (e.g. ../customer.isl) resolve correctly
            val basePath = scriptFile.parentFile?.toPath()?.normalize() ?: Paths.get(".").toAbsolutePath().normalize()
            val moduleName = scriptFile.name
            val fileInfos = mutableListOf(FileInfo(moduleName, scriptContent))
            val findExternalModule = java.util.function.BiFunction<String, String, String> { fromModule, dependentModule ->
                resolveExternalModule(basePath, fromModule, dependentModule)
                    ?: throw com.intuit.isl.runtime.TransformCompilationException(
                        "Could not find module '$dependentModule' (imported from $fromModule). Searched relative to ${basePath.resolve(fromModule).parent}"
                    )
            }
            val transformPackage = TransformPackageBuilder().build(fileInfos, findExternalModule)
            val transformer = transformPackage.getModule(moduleName)
                ?: throw IllegalStateException("Compiled module '$moduleName' not found in package")

            // Create operation context with variables and CLI extensions (e.g. Log)
            val context = OperationContext()
            LogExtensions.registerExtensions(context)
            variables.forEach { (key, value) ->
                val varName = if (key.startsWith("$")) key else "$$key"
                val varValue = JsonConvert.convert(value);
                println("Setting variable " + varName + " to " + varValue );
                context.setVariable(varName, varValue)
            }
            
            val result = runBlocking {
                transformer.runTransformAsync(functionName, context)
            }
            
            // Format and output result
            val output = formatOutput(result.result, format, pretty)
            
            if (outputFile != null) {
                outputFile!!.writeText(output)
                println("Output written to: ${outputFile!!.absolutePath}")
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
    
    /**
     * Resolves an import path (e.g. "../customer.isl") relative to the current module's directory.
     * Enables relative imports like `import Customer from "../customer.isl"` when running from the command line.
     */
    private fun resolveExternalModule(basePath: Path, fromModule: String, dependentModule: String): String? {
        val fromDir = basePath.resolve(fromModule).parent ?: basePath
        val candidateNames = if (dependentModule.endsWith(".isl", ignoreCase = true)) {
            listOf(dependentModule)
        } else {
            listOf("$dependentModule.isl", "$dependentModule.ISL")
        }
        for (name in candidateNames) {
            val candidatePath = fromDir.resolve(name).normalize()
            val file = candidatePath.toFile()
            if (file.exists() && file.isFile) return file.readText()
        }
        val moduleBaseName = if (dependentModule.endsWith(".isl", ignoreCase = true)) {
            dependentModule.dropLast(4)
        } else {
            dependentModule
        }
        return basePath.toFile().walkTopDown()
            .filter { it.isFile && it.extension.equals("isl", true) }
            .find { it.nameWithoutExtension.equals(moduleBaseName, true) }
            ?.readText()
    }

    private fun formatOutput(result: Any?, format: String, pretty: Boolean): String {
        val mapper = when (format.lowercase()) {
            "yaml", "yml" -> ObjectMapper(YAMLFactory())
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

