package com.intuit.isl.cmd

import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.File
import kotlin.system.exitProcess

/**
 * Command to validate ISL scripts without executing them.
 * Compiles the script and lists loaded files and detected functions.
 * Uses the same module resolution as transform and test commands (supports relative imports).
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
            val transformPackage = IslModuleResolver.compileSingleFile(scriptFile, scriptContent)

            println("> Script is valid: ${scriptFile.name}")
            println("  Files loaded:")
            for (moduleName in transformPackage.modules) {
                println("    $moduleName")
            }
            println("  Functions by module:")
            for (moduleName in transformPackage.modules) {
                val transformer = transformPackage.getModule(moduleName) ?: continue
                val functionNames = transformer.module.functions.map { it.name }.sorted()
                println("    $moduleName: ${functionNames.joinToString(", ").ifEmpty { "(none)" }}")
            }

        } catch (e: Exception) {
            System.err.println("✗ Validation failed: ${e.message}")
            if (System.getProperty("debug") == "true") {
                e.printStackTrace()
            }
            exitProcess(1)
        }
    }
}

