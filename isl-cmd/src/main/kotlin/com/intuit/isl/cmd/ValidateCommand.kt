package com.intuit.isl.cmd

import com.intuit.isl.common.OperationContext
import com.intuit.isl.runtime.TransformCompiler
import kotlinx.coroutines.runBlocking
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.File
import kotlin.system.exitProcess

/**
 * Command to validate ISL scripts without executing them
 */
@Command(
    name = "validate",
    description = ["Validate an ISL script without executing it"]
)
class ValidateCommand : Runnable {
    
    @Parameters(
        index = "0",
        description = ["ISL script file to validate"]
    )
    lateinit var scriptFile: File
    
    override fun run() {
        try {
            if (!scriptFile.exists()) {
                System.err.println("Error: Script file not found: ${scriptFile.absolutePath}")
                exitProcess(1)
            }
            
            val scriptContent = scriptFile.readText()
            val compiler = TransformCompiler()
            
            // Try to parse and compile the script
            val transformer = compiler.compileIsl(scriptFile.name, scriptContent)
            
            // Try to execute with empty params to validate
            val context = OperationContext()
            runBlocking {
                transformer.runTransformAsync("run", context)
            }
            
            println("> Script is valid: ${scriptFile.name}")
            
        } catch (e: Exception) {
            System.err.println("✗ Validation failed: ${e.message}")
            if (System.getProperty("debug") == "true") {
                e.printStackTrace()
            }
            exitProcess(1)
        }
    }
}

