package com.intuit.isl.cmd

import com.intuit.isl.runtime.Transformer
import picocli.CommandLine
import picocli.CommandLine.Command
import kotlin.system.exitProcess

/**
 * ISL Command Line Interface
 * 
 * Allows running ISL transformation scripts from the command line.
 */
@Command(
    name = "isl",
    mixinStandardHelpOptions = true,
    version = ["ISL Command Line \${COMMAND-NAME} - ISL Version: \${Transformer.version}"],
    description = ["Transform JSON/YAML data using ISL scripts"],
    subcommands = [
        TransformCommand::class,
        ValidateCommand::class,
        InfoCommand::class,
        TestCommand::class
    ]
)
class IslCommandLine : Runnable {   
    override fun run() {
        CommandLine.usage(this, System.out)
    }
}

fun main(args: Array<String>) {
    val exitCode = CommandLine(IslCommandLine()).execute(*args)
    exitProcess(exitCode)
}

