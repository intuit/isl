package com.intuit.isl.dap

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intuit.isl.cmd.IslModuleResolver
import com.intuit.isl.cmd.LogExtensions
import com.intuit.isl.cmd.YamlUnitTestRunner
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.common.TransformVariable
import com.intuit.isl.runtime.TransformCompiler
import com.intuit.isl.runtime.Transformer
import com.intuit.isl.utils.JsonConvert
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume
import kotlin.system.exitProcess

/**
 * Main DAP adapter that processes DAP requests and orchestrates ISL debugging.
 */
class DapAdapter(private val transport: DapTransport) {
    private val mapper = ObjectMapper().registerKotlinModule()
    private val debugHook = IslDebugHook(this)

    @Volatile
    private var isRunning = true

    @Volatile
    private var transformJob: Job? = null

    private val variableReferences = ConcurrentHashMap<Int, () -> List<DapVariable>>()
    private var variableRefCounter = 0

    @Volatile
    private var completedResult: JsonNode? = null
    @Volatile
    private var completedContinuation: CancellableContinuation<Unit>? = null
    @Volatile
    private var pausedAtReturn = false

    private var scriptPath: String? = null
    private var inputPath: String? = null
    private var functionName: String = "run"
    /** When set with [yamlTestIndex], use [YamlUnitTestRunner] (mocks + YAML input) instead of raw JSON input. */
    private var yamlSuitePath: String? = null
    private var yamlTestIndex: Int? = null

    fun run() {
        while (isRunning) {
            val message = transport.readMessage() ?: break
            handleMessage(message)
        }
    }

    fun sendOutputEvent(text: String, category: String = "console") {
        sendEvent("output", OutputEventBody(output = text, category = category))
    }

    fun notifyStopped(command: IIslCommand, context: ExecutionContext, reason: String) {
        variableReferences.clear()
        variableRefCounter = 0
        sendEvent("stopped", StoppedEventBody(reason = reason, description = command.token.position.toString()))
    }

    private fun handleMessage(message: DapMessage) {
        when (message.type) {
            "request" -> handleRequest(message)
        }
    }

    private fun handleRequest(message: DapMessage) {
        try {
            when (message.command) {
                "initialize" -> handleInitialize(message)
                "launch" -> handleLaunch(message)
                "setBreakpoints" -> handleSetBreakpoints(message)
                "configurationDone" -> handleConfigurationDone(message)
                "threads" -> handleThreads(message)
                "stackTrace" -> handleStackTrace(message)
                "scopes" -> handleScopes(message)
                "variables" -> handleVariables(message)
                "continue" -> handleContinue(message)
                "next" -> handleNext(message)
                "stepIn" -> handleStepIn(message)
                "stepOut" -> handleStepOut(message)
                "pause" -> handlePause(message)
                "evaluate" -> handleEvaluate(message)
                "disconnect" -> handleDisconnect(message)
                "terminate" -> handleTerminate(message)
                else -> sendResponse(message.seq, message.command ?: "", success = false, message = "Unsupported command: ${message.command}")
            }
        } catch (e: Exception) {
            sendResponse(message.seq, message.command ?: "", success = false, message = e.message ?: "Unknown error")
        }
    }

    private fun handleInitialize(message: DapMessage) {
        sendResponse(message.seq, "initialize", InitializeResponseBody())
        sendEvent("initialized")
    }

    /** VS Code may send index as int, double, or string (e.g. from launch.json). */
    private fun parseYamlTestIndexArg(node: JsonNode?): Int? {
        if (node == null || node.isNull || node.isMissingNode) return null
        if (node.isIntegralNumber) return node.asInt()
        if (node.isNumber) {
            val d = node.asDouble()
            if (d == kotlin.math.floor(d) && d >= 0 && d <= Int.MAX_VALUE) return d.toInt()
        }
        if (node.isTextual) return node.asText().trim().toIntOrNull()
        return null
    }

    private fun handleLaunch(message: DapMessage) {
        val args = message.arguments ?: run {
            sendResponse(message.seq, "launch", success = false, message = "Missing launch arguments")
            return
        }

        scriptPath = args["script"]?.asText()
        inputPath = args["input"]?.asText()
        functionName = args["function"]?.asText() ?: "run"
        yamlSuitePath = args["yamlSuite"]?.asText()?.takeIf { it.isNotBlank() }
        yamlTestIndex = parseYamlTestIndexArg(args.get("yamlTestIndex"))

        if (scriptPath.isNullOrBlank()) {
            sendResponse(message.seq, "launch", success = false, message = "Missing 'script' in launch configuration")
            return
        }

        sendResponse(message.seq, "launch")
    }

    private fun handleSetBreakpoints(message: DapMessage) {
        val args = message.arguments ?: return
        val source = args["source"]
        val sourcePath = source?.get("path")?.asText() ?: ""
        val breakpointsNode = args["breakpoints"] as? ArrayNode

        val requestedLines = mutableSetOf<Int>()
        val confirmedBreakpoints = mutableListOf<DapBreakpoint>()

        breakpointsNode?.forEach { bp ->
            val line = bp["line"]?.asInt() ?: return@forEach
            requestedLines.add(line)
            confirmedBreakpoints.add(DapBreakpoint(
                verified = true,
                line = line,
                source = DapSource(path = sourcePath)
            ))
        }

        val normalizedPath = normalizePath(sourcePath)
        debugHook.setBreakpoints(normalizedPath, requestedLines)

        val fileName = Path.of(sourcePath).fileName?.toString()
        if (fileName != null) {
            debugHook.setBreakpoints(fileName, requestedLines)
        }

        sendResponse(message.seq, "setBreakpoints", BreakpointResponseBody(confirmedBreakpoints))
    }

    private fun handleConfigurationDone(message: DapMessage) {
        sendResponse(message.seq, "configurationDone")
        startTransform()
    }

    private fun handleThreads(message: DapMessage) {
        sendResponse(message.seq, "threads", ThreadsResponseBody(
            threads = listOf(DapThread(id = 1, name = "ISL Main"))
        ))
    }

    private fun handleStackTrace(message: DapMessage) {
        val frames = mutableListOf<DapStackFrame>()

        if (pausedAtReturn) {
            val scriptFile = scriptPath?.let { Path.of(it) }
            frames.add(DapStackFrame(
                id = 0,
                name = "$functionName() returned",
                source = DapSource(name = scriptFile?.fileName?.toString(), path = scriptFile?.toAbsolutePath()?.normalize()?.toString()),
                line = 1,
                column = 1
            ))
        } else {
            val currentCmd = debugHook.currentCommand
            if (currentCmd != null) {
                val pos = currentCmd.token.position
                frames.add(DapStackFrame(
                    id = 0,
                    name = currentCmd.token.toString(),
                    source = DapSource(name = Path.of(pos.file).fileName?.toString(), path = resolveSourcePath(pos.file)),
                    line = pos.line,
                    column = pos.column,
                    endLine = pos.endLine,
                    endColumn = pos.endColumn
                ))
            }

            for (frame in debugHook.getCallStack()) {
                val pos = frame.command.token.position
                frames.add(DapStackFrame(
                    id = frame.id,
                    name = frame.name,
                    source = DapSource(name = Path.of(pos.file).fileName?.toString(), path = resolveSourcePath(pos.file)),
                    line = pos.line,
                    column = pos.column,
                    endLine = pos.endLine,
                    endColumn = pos.endColumn
                ))
            }
        }

        sendResponse(message.seq, "stackTrace", StackTraceResponseBody(
            stackFrames = frames,
            totalFrames = frames.size
        ))
    }

    private fun handleScopes(message: DapMessage) {
        val frameId = message.arguments?.get("frameId")?.asInt() ?: 0

        if (pausedAtReturn) {
            val returnRef = allocateVariableRef { buildReturnValueVariables() }
            sendResponse(message.seq, "scopes", ScopesResponseBody(
                scopes = listOf(
                    DapScope(name = "Return Value", variablesReference = returnRef, expensive = false)
                )
            ))
            return
        }

        val ctx = resolveContextForFrame(frameId)

        val globalsRef = allocateVariableRef { buildGlobalVariables(ctx) }
        val localsRef = allocateVariableRef { buildLocalVariables() }

        sendResponse(message.seq, "scopes", ScopesResponseBody(
            scopes = listOf(
                DapScope(name = "Variables", variablesReference = globalsRef, expensive = false),
                DapScope(name = "Last Result", variablesReference = localsRef, expensive = false)
            )
        ))
    }

    private fun resolveContextForFrame(frameId: Int): ExecutionContext? {
        if (frameId == 0) return debugHook.currentContext
        val frame = debugHook.getCallStack().find { it.id == frameId }
        return frame?.context ?: debugHook.currentContext
    }

    private fun handleVariables(message: DapMessage) {
        val ref = message.arguments?.get("variablesReference")?.asInt() ?: 0
        val provider = variableReferences[ref]
        val variables = provider?.invoke() ?: emptyList()
        sendResponse(message.seq, "variables", VariablesResponseBody(variables))
    }

    private fun handleContinue(message: DapMessage) {
        sendResponse(message.seq, "continue")
        if (pausedAtReturn) {
            resumeFromReturn()
        } else {
            debugHook.resume(SteppingMode.CONTINUE)
        }
    }

    private fun handleNext(message: DapMessage) {
        sendResponse(message.seq, "next")
        if (pausedAtReturn) { resumeFromReturn(); return }
        debugHook.stepDepth = debugHook.getCurrentDepth()
        debugHook.resume(SteppingMode.STEP_OVER)
    }

    private fun handleStepIn(message: DapMessage) {
        sendResponse(message.seq, "stepIn")
        if (pausedAtReturn) { resumeFromReturn(); return }
        debugHook.resume(SteppingMode.STEP_IN)
    }

    private fun handleStepOut(message: DapMessage) {
        sendResponse(message.seq, "stepOut")
        if (pausedAtReturn) { resumeFromReturn(); return }
        debugHook.stepDepth = debugHook.getCurrentDepth()
        debugHook.resume(SteppingMode.STEP_OUT)
    }

    private fun handlePause(message: DapMessage) {
        debugHook.steppingMode = SteppingMode.PAUSE
        sendResponse(message.seq, "pause")
    }

    private fun handleEvaluate(message: DapMessage) {
        val expression = message.arguments?.get("expression")?.asText() ?: ""
        val evalContext = message.arguments?.get("context")?.asText() ?: "repl"

        val (result, resultType, childRef) = evaluateExpression(expression, evalContext)
        sendResponse(message.seq, "evaluate", EvaluateResponseBody(
            result = result,
            type = resultType,
            variablesReference = childRef
        ))
    }

    private fun handleDisconnect(message: DapMessage) {
        shutdown()
        sendResponse(message.seq, "disconnect")
        forceExit()
    }

    private fun handleTerminate(message: DapMessage) {
        shutdown()
        sendResponse(message.seq, "terminate")
        sendEvent("terminated", TerminatedEventBody())
        forceExit()
    }

    private fun shutdown() {
        debugHook.resume(SteppingMode.CONTINUE)
        resumeFromReturn()
        transformJob?.cancel()
        isRunning = false
    }

    /**
     * Force-exit the JVM after a short delay to allow the last response to flush.
     * This is necessary because the main loop blocks on stdin.read() and
     * setting isRunning=false cannot unblock it.
     */
    private fun forceExit() {
        Thread {
            Thread.sleep(100)
            exitProcess(0)
        }.apply { isDaemon = true }.start()
    }

    private fun resumeFromReturn() {
        val cont = completedContinuation
        completedContinuation = null
        pausedAtReturn = false
        cont?.resume(Unit)
    }

    // --- Transform execution ---

    private fun startTransform() {
        val path = scriptPath ?: return
        val scriptFile = File(path)
        if (!scriptFile.exists()) {
            sendEvent("output", OutputEventBody(output = "Script file not found: $path\n", category = "stderr"))
            sendEvent("terminated", TerminatedEventBody())
            return
        }

        val script = scriptFile.readText()

        val inputJson = if (!inputPath.isNullOrBlank()) {
            val inputFile = File(inputPath!!)
            if (inputFile.exists()) {
                try {
                    mapper.readTree(inputFile.readText())
                } catch (e: Exception) {
                    sendEvent("output", OutputEventBody(output = "Failed to parse input JSON: ${e.message}\n", category = "stderr"))
                    mapper.createObjectNode()
                }
            } else {
                mapper.createObjectNode()
            }
        } else {
            mapper.createObjectNode()
        }

        transformJob = CoroutineScope(Dispatchers.Default).launch {
            var exitCode = 0
            try {
                val yamlPath = yamlSuitePath
                val yamlIdx = yamlTestIndex
                val useYamlTest = yamlPath != null && yamlIdx != null

                if (useYamlTest) {
                    sendEvent("output", OutputEventBody(output = "Preparing YAML test debug: $yamlPath (index=$yamlIdx)\n"))
                    val prepared = try {
                        YamlUnitTestRunner.prepareYamlTestForDebug(Path.of(yamlPath), yamlIdx)
                    } catch (e: Exception) {
                        sendEvent("output", OutputEventBody(output = "YAML test preparation failed: ${e.message}\n", category = "stderr"))
                        exitCode = 1
                        return@launch
                    }
                    LogExtensions.registerExtensions(prepared.operationContext)
                    val transformer = prepared.transformPackage.getModule(prepared.moduleName)
                    if (transformer == null) {
                        sendEvent("output", OutputEventBody(output = "Module '${prepared.moduleName}' not found after compilation. Available: ${prepared.transformPackage.modules}\n", category = "stderr"))
                        exitCode = 1
                        return@launch
                    }
                    sendEvent("output", OutputEventBody(output = "Starting ISL Debugging - ${prepared.functionName}() (YAML test: mocks + input applied)\n"))
                    val result = transformer.runTransformAsync(prepared.functionName, prepared.operationContext, debugHook)
                    pauseWithReturnValue(result.result)
                } else {
                    sendEvent("output", OutputEventBody(output = "Compiling ISL script: $path\n"))

                    val pkg = try {
                        IslModuleResolver.compileSingleFile(scriptFile, script)
                    } catch (e: Exception) {
                        sendEvent("output", OutputEventBody(output = "Compilation error: ${e.message}\n", category = "stderr"))
                        exitCode = 1
                        return@launch
                    }

                    val moduleName = scriptFile.name
                    val transformer = pkg.getModule(moduleName)
                    if (transformer == null) {
                        sendEvent("output", OutputEventBody(output = "Module '$moduleName' not found after compilation. Available: ${pkg.modules}\n", category = "stderr"))
                        exitCode = 1
                        return@launch
                    }

                    val operationContext = OperationContext()
                    LogExtensions.registerExtensions(operationContext)
                    inputJson.fields().forEach { (key, value) ->
                        val varName = if (key.startsWith("$")) key else "$" + key
                        val node = JsonConvert.convert(value)
                        operationContext.setVariable(varName, TransformVariable(node, readOnly = false, global = true))
                    }

                    sendEvent("output", OutputEventBody(output = "Starting ISL Debugging - looking for fun $functionName() ...\n"))

                    val result = transformer.runTransformAsync(functionName, operationContext, debugHook)
                    pauseWithReturnValue(result.result)
                }
            } catch (e: CancellationException) {
                sendEvent("output", OutputEventBody(output = "Transform cancelled.\n"))
            } catch (e: Exception) {
                sendEvent("output", OutputEventBody(output = "Transform error: ${e.message}\n", category = "stderr"))
                exitCode = 1
            } finally {
                sendEvent("terminated", TerminatedEventBody())
                sendEvent("exited", ExitedEventBody(exitCode = exitCode))
                // Give VSCode a few seconds to send disconnect; force-exit if it doesn't
                Thread {
                    Thread.sleep(5000)
                    exitProcess(exitCode)
                }.apply { isDaemon = true }.start()
            }
        }
    }

    /**
     * Suspend the transform coroutine so the user can inspect the return value.
     * The debugger pauses as if it hit a breakpoint; the Variables panel shows
     * a "Return Value" scope.  Pressing Continue resumes → terminates normally.
     */
    private suspend fun pauseWithReturnValue(result: JsonNode?) {
        val resultStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result)
        sendEvent("output", OutputEventBody(output = "\n── $functionName() returned ──\n$resultStr\n"))

        completedResult = result
        pausedAtReturn = true
        variableReferences.clear()
        variableRefCounter = 0

        suspendCancellableCoroutine { continuation ->
            completedContinuation = continuation
            sendEvent("stopped", StoppedEventBody(
                reason = "step",
                description = "$functionName() returned",
                text = "$functionName() returned"
            ))
        }
    }

    // --- Variable inspection ---

    private fun buildGlobalVariables(ctx: ExecutionContext? = null): List<DapVariable> {
        @Suppress("NAME_SHADOWING")
        val ctx = ctx ?: debugHook.currentContext ?: return emptyList()
        val variables = mutableListOf<DapVariable>()

        ctx.operationContext.variables.forEach { (name, transformVar) ->
            val value = transformVar.value
            val displayName = if (name.startsWith("\$")) name else "\$$name"
            val (displayValue, childRef) = formatJsonValue(name, value)
            variables.add(DapVariable(
                name = displayName,
                value = displayValue,
                type = getJsonType(value),
                variablesReference = childRef
            ))
        }

        return variables.sortedBy { it.name }
    }

    private fun buildLocalVariables(): List<DapVariable> {
        val result = debugHook.lastResult ?: return emptyList()
        val variables = mutableListOf<DapVariable>()

        val value = result.value?.let { JsonConvert.convert(it) }
        if (value != null) {
            val (displayValue, childRef) = formatJsonValue("result", value)
            variables.add(DapVariable(
                name = "result",
                value = displayValue,
                type = getJsonType(value),
                variablesReference = childRef
            ))
        }

        if (result.propertyName != null) {
            variables.add(DapVariable(name = "propertyName", value = result.propertyName ?: "", type = "string"))
        }

        return variables
    }

    private fun buildReturnValueVariables(): List<DapVariable> {
        val result = completedResult ?: return listOf(
            DapVariable(name = "$functionName()", value = "null", type = "null")
        )
        val (displayValue, childRef) = formatJsonValue(functionName, result)
        return listOf(DapVariable(
            name = "$functionName()",
            value = displayValue,
            type = getJsonType(result),
            variablesReference = childRef
        ))
    }

    private fun formatJsonValue(name: String, value: JsonNode?): Pair<String, Int> {
        if (value == null) return Pair("null", 0)

        return when {
            value is ObjectNode -> {
                val childRef = allocateVariableRef {
                    value.fields().asSequence().map { (k, v) ->
                        val (childDisplay, childChildRef) = formatJsonValue(k, v)
                        DapVariable(name = k, value = childDisplay, type = getJsonType(v), variablesReference = childChildRef)
                    }.toList()
                }
                Pair("{${value.size()} properties}", childRef)
            }
            value is ArrayNode -> {
                val childRef = allocateVariableRef {
                    value.mapIndexed { i, v ->
                        val (childDisplay, childChildRef) = formatJsonValue("[$i]", v)
                        DapVariable(name = "[$i]", value = childDisplay, type = getJsonType(v), variablesReference = childChildRef)
                    }
                }
                Pair("[${value.size()} items]", childRef)
            }
            else -> Pair(value.asText(), 0)
        }
    }

    private fun getJsonType(value: JsonNode?): String {
        return when {
            value == null -> "null"
            value.isTextual -> "string"
            value.isNumber -> "number"
            value.isBoolean -> "boolean"
            value.isObject -> "object"
            value.isArray -> "array"
            value.isNull -> "null"
            else -> "unknown"
        }
    }

    // --- Expression evaluation ---

    data class EvalResult(val display: String, val type: String, val variablesReference: Int)

    private fun evaluateExpression(expression: String, evalContext: String): EvalResult {
        val ctx = debugHook.currentContext ?: return EvalResult("<no execution context>", "string", 0)

        // Direct variable lookup: $varName or varName (for hover context)
        val varLookup = when {
            expression.startsWith("\$") -> expression
            evalContext == "hover" && !expression.contains(" ") -> "\$$expression"
            else -> null
        }

        if (varLookup != null) {
            val value = ctx.operationContext.getVariable(varLookup)
            if (value != null) {
                val (displayValue, childRef) = formatJsonValue(varLookup, value)
                return EvalResult(
                    if (childRef > 0) displayValue else mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value),
                    getJsonType(value),
                    childRef
                )
            }
            if (evalContext == "hover") {
                return EvalResult("<undefined>", "string", 0)
            }
        }

        // Full ISL expression evaluation via compilation
        return try {
            val compiler = TransformCompiler()
            val evalScript = "fun run()\n    return $expression\nendfun"
            val transformer = compiler.compileIsl("eval", evalScript) as Transformer

            var result: JsonNode? = null
            runBlocking {
                val evalResult = transformer.runTransformAsync("run", ctx.operationContext)
                result = evalResult.result
            }

            val jsonResult = result
            if (jsonResult != null) {
                val (displayValue, childRef) = formatJsonValue("result", jsonResult)
                EvalResult(
                    if (childRef > 0) displayValue else mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResult),
                    getJsonType(jsonResult),
                    childRef
                )
            } else {
                EvalResult("null", "null", 0)
            }
        } catch (e: Exception) {
            EvalResult("<error: ${e.message}>", "string", 0)
        }
    }

    // --- Helpers ---

    private fun resolveSourcePath(file: String): String {
        val f = File(file)
        if (f.isAbsolute) return file
        val scriptDir = scriptPath?.let { File(it).parent } ?: ""
        val resolved = File(scriptDir, file)
        return if (resolved.exists()) resolved.absolutePath else file
    }

    private fun normalizePath(path: String): String {
        return path.replace("\\", "/")
    }

    private fun sendResponse(requestSeq: Int, command: String, body: Any? = null, success: Boolean = true, message: String? = null) {
        transport.writeMessage(DapMessageFactory.response(requestSeq, command, body, success, message))
    }

    private fun sendEvent(eventName: String, body: Any? = null) {
        transport.writeMessage(DapMessageFactory.event(eventName, body))
    }

    @Synchronized
    private fun allocateVariableRef(provider: () -> List<DapVariable>): Int {
        val ref = ++variableRefCounter
        variableReferences[ref] = provider
        return ref
    }
}
