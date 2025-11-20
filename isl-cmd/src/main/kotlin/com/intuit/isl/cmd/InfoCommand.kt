package com.intuit.isl.cmd

import com.intuit.isl.runtime.Transformer
import picocli.CommandLine.Command

/**
 * Command to display ISL version and system information
 */
@Command(
    name = "info",
    description = ["Show ISL version and system information"]
)
class InfoCommand : Runnable {
    override fun run() {
        println("ISL Command Line Interface")
        println("ISL Version: ${Transformer.version}")
        println()
        println("System Information:")
        println("  Java Version: ${System.getProperty("java.version")}")
        println("  Java Vendor: ${System.getProperty("java.vendor")}")
        println("  OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
        println("  Architecture: ${System.getProperty("os.arch")}")
        println()
        println("For help, run: isl --help")
    }
}

