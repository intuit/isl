package com.intuit.isl.cmd

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert

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
        context.registerExtensionMethod("Log.Debug", LogExtensions::debug)
        context.registerExtensionMethod("Log.Info", LogExtensions::info)
        context.registerExtensionMethod("Log.Warn", LogExtensions::warn)
        context.registerExtensionMethod("Log.Error", LogExtensions::error)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun debug(context: FunctionExecuteContext): Any? {
        if (System.getProperty("debug") != "true") return null
        val message = formatMessage(context.parameters)
        println("[DEBUG] $message")
        return null
    }

    @Suppress("UNUSED_PARAMETER")
    private fun info(context: FunctionExecuteContext): Any? {
        val message = formatMessage(context.parameters)
        println("[INFO] $message")
        return null
    }

    @Suppress("UNUSED_PARAMETER")
    private fun warn(context: FunctionExecuteContext): Any? {
        val message = formatMessage(context.parameters)
        System.err.println("[WARN] $message")
        return null
    }

    @Suppress("UNUSED_PARAMETER")
    private fun error(context: FunctionExecuteContext): Any? {
        val message = formatMessage(context.parameters)
        System.err.println("[ERROR] $message")
        return null
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
