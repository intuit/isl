package com.intuit.isl.cmd

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.test.TestOperationContext
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import java.nio.file.Paths

/**
 * Log extension functions for ISL scripts when running transforms or tests from the command line.
 *
 * Usage in ISL:
 *   @.Log.Info("Processing item", $count)
 *   @.Log.Warn("Unexpected value:", $value)
 *   @.Log.Error("Failed:", $error)
 *   @.Log.Debug("Debug info")  // Only outputs when -Ddebug=true
 */
object LogExtensions {
    fun registerExtensions(context: IOperationContext) {
        context.registerSyncExtensionMethod("Log.Debug", LogExtensions::debug)
        context.registerSyncExtensionMethod("Log.Info", LogExtensions::info)
        context.registerSyncExtensionMethod("Log.Warn", LogExtensions::warn)
        context.registerSyncExtensionMethod("Log.Error", LogExtensions::error)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun debug(context: FunctionExecuteContext): Any? {
        if (System.getProperty("debug") != "true") return null
        val message = formatMessage(context.parameters)
        val loc = logLocation(context)
        logToStdout(loc, "DEBUG", message)
        return null
    }

    @Suppress("UNUSED_PARAMETER")
    private fun info(context: FunctionExecuteContext): Any? {
        val message = formatMessage(context.parameters)
        val loc = logLocation(context)
        logToStdout(loc, "INFO", message)
        return null
    }

    @Suppress("UNUSED_PARAMETER")
    private fun warn(context: FunctionExecuteContext): Any? {
        val message = formatMessage(context.parameters)
        val loc = logLocation(context)
        // Use stdout so log order is preserved when plugin runs tests (stdout/stderr are shown separately)
        logToStdout(loc, "WARN", message)
        return null
    }

    @Suppress("UNUSED_PARAMETER")
    private fun error(context: FunctionExecuteContext): Any? {
        val message = formatMessage(context.parameters)
        val loc = logLocation(context)
        // Use stdout so log order is preserved when plugin runs tests (stdout/stderr are shown separately)
        logToStdout(loc, "ERROR", message)
        return null
    }

    private fun logToStdout(loc: String, level: String, message: String) {
        println("$loc ${colorize(level)} $message")
        System.out.flush()
    }

    private fun colorize(level: String): String {
        val ansiColors = System.getenv("NO_COLOR") == null && System.getenv("TERM") != "dumb"
        if (!ansiColors) return level
        val (color, bold) = when (level) {
            "DEBUG" -> "\u001B[90m" to false   // dark gray
            "INFO"  -> "\u001B[36m" to false   // cyan
            "WARN"  -> "\u001B[33m" to true    // yellow, bold
            "ERROR" -> "\u001B[31m" to true    // red, bold
            else    -> return level
        }
        val boldCode = if (bold) "\u001B[1m" else ""
        return "$color$boldCode$level\u001B[0m"
    }

    /**
     * Paths in test context are relative to where the first islSource was loaded from (basePath).
     * We show that relative path (pos.file) instead of resolving to absolute, so log lines
     * match the module path used in the test (e.g. "current/accounttxn.isl:42").
     */
    private fun logLocation(context: FunctionExecuteContext): String {
        val pos = context.command.token.position
        val testContext = context.executionContext.operationContext as? TestOperationContext
        val pathForDisplay = if (testContext?.basePath != null) {
            // In test context: show path relative to basePath (where islSource is resolved from)
            pos.file.replace("\\", "/")
        } else {
            try {
                val resolved = Paths.get(pos.file)
                resolved.toAbsolutePath().normalize().toString()
            } catch (_: Exception) {
                pos.file
            }
        }
        return "$pathForDisplay:${pos.line}"
    }

    private fun formatMessage(parameters: Array<*>): String {
        if (parameters.isEmpty()) return ""
        return parameters.joinToString(" ") { param ->
            when (param) {
                null -> "null"
                is JsonNode -> param.toPrettyString()
                else -> ConvertUtils.tryToString(param) ?: param.toString()
            }
        }
    }

    private fun JsonNode.toPrettyString(): String {
        return try {
            JsonConvert.mapper
                .copy()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(this)
        } catch (_: Exception) {
            this.toString()
        }
    }
}
