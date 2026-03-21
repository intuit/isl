package com.intuit.isl.dap

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Debug Adapter Protocol message types.
 * See: https://microsoft.github.io/debug-adapter-protocol/specification
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DapMessage(
    val seq: Int,
    val type: String,
    val command: String? = null,
    val arguments: JsonNode? = null,
    @JsonProperty("request_seq") val requestSeq: Int? = null,
    val success: Boolean? = null,
    val message: String? = null,
    val body: JsonNode? = null,
    val event: String? = null
)

object DapMessageFactory {
    private val mapper = ObjectMapper()
    private var seqCounter = 1

    @Synchronized
    fun nextSeq(): Int = seqCounter++

    fun response(requestSeq: Int, command: String, body: Any? = null, success: Boolean = true, message: String? = null): DapMessage {
        return DapMessage(
            seq = nextSeq(),
            type = "response",
            command = command,
            requestSeq = requestSeq,
            success = success,
            message = message,
            body = if (body != null) mapper.valueToTree(body) else null
        )
    }

    fun event(eventName: String, body: Any? = null): DapMessage {
        return DapMessage(
            seq = nextSeq(),
            type = "event",
            event = eventName,
            body = if (body != null) mapper.valueToTree(body) else null
        )
    }
}

// --- Response / Event body types ---

@JsonInclude(JsonInclude.Include.NON_NULL)
data class InitializeResponseBody(
    val supportsConfigurationDoneRequest: Boolean = true,
    val supportsFunctionBreakpoints: Boolean = false,
    val supportsEvaluateForHovers: Boolean = true,
    val supportsStepBack: Boolean = false,
    val supportsSetVariable: Boolean = false,
    val supportsRestartFrame: Boolean = false,
    val supportsStepInTargetsRequest: Boolean = false,
    val supportsCompletionsRequest: Boolean = false,
    val supportsTerminateRequest: Boolean = true
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class StoppedEventBody(
    val reason: String,
    val threadId: Int = 1,
    val allThreadsStopped: Boolean = true,
    val description: String? = null,
    val text: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ThreadsResponseBody(
    val threads: List<DapThread>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DapThread(
    val id: Int,
    val name: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class StackTraceResponseBody(
    val stackFrames: List<DapStackFrame>,
    val totalFrames: Int? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DapStackFrame(
    val id: Int,
    val name: String,
    val source: DapSource? = null,
    val line: Int,
    val column: Int,
    val endLine: Int? = null,
    val endColumn: Int? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DapSource(
    val name: String? = null,
    val path: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ScopesResponseBody(
    val scopes: List<DapScope>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DapScope(
    val name: String,
    val variablesReference: Int,
    val expensive: Boolean = false
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VariablesResponseBody(
    val variables: List<DapVariable>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DapVariable(
    val name: String,
    val value: String,
    val type: String? = null,
    val variablesReference: Int = 0
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EvaluateResponseBody(
    val result: String,
    val type: String? = null,
    val variablesReference: Int = 0
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BreakpointResponseBody(
    val breakpoints: List<DapBreakpoint>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DapBreakpoint(
    val id: Int? = null,
    val verified: Boolean,
    val line: Int? = null,
    val source: DapSource? = null,
    val message: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OutputEventBody(
    val category: String = "console",
    val output: String
)

data class TerminatedEventBody(
    val restart: Boolean = false
)

data class ExitedEventBody(
    val exitCode: Int
)
