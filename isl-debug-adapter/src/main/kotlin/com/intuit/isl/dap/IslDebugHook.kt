package com.intuit.isl.dap

import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.FunctionDeclarationCommand
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.debug.IDebugHook
import com.intuit.isl.parser.tokens.FunctionDeclarationToken
import com.intuit.isl.utils.Position
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

enum class SteppingMode {
    CONTINUE,
    STEP_IN,
    STEP_OVER,
    STEP_OUT,
    PAUSE
}

data class BreakpointLocation(val file: String, val line: Int)

/**
 * Stack frame tracked during ISL execution.
 */
data class CallFrame(
    val id: Int,
    val name: String,
    val command: IIslCommand,
    val context: ExecutionContext
)

/**
 * ISL debug hook that suspends execution at breakpoints / stepping boundaries
 * and notifies VS Code via DAP.
 */
class IslDebugHook(
    private val adapter: DapAdapter
) : IDebugHook {

    @Volatile
    var steppingMode: SteppingMode = SteppingMode.STEP_IN

    @Volatile
    var stepDepth: Int = 0

    val breakpoints = ConcurrentHashMap<String, MutableSet<Int>>()

    private val callStack = ArrayDeque<CallFrame>()
    private var frameIdCounter = 0
    @Volatile
    var currentCommand: IIslCommand? = null
    @Volatile
    var currentContext: ExecutionContext? = null
    @Volatile
    var lastResult: CommandResult? = null

    var pausedContinuation: CancellableContinuation<SteppingMode>? = null

    fun setBreakpoints(file: String, lines: Set<Int>) {
        if (lines.isEmpty()) {
            breakpoints.remove(file)
        } else {
            breakpoints[file] = lines.toMutableSet()
        }
    }

    private fun normalizePathKey(path: String): String = path.replace('\\', '/')

    private fun baseNameKey(path: String): String =
        try {
            Path.of(path).fileName?.toString() ?: path
        } catch (_: Exception) {
            path
        }

    /**
     * Lines registered for this source: exact path, normalized slashes, and basename (DAP registers both).
     */
    private fun breakpointLinesForSource(sourceFile: String): Set<Int> {
        val keys = listOf(sourceFile, normalizePathKey(sourceFile), baseNameKey(sourceFile)).distinct()
        val lines = mutableSetOf<Int>()
        for (k in keys) {
            breakpoints[k]?.let { lines.addAll(it) }
        }
        if (lines.isEmpty()) {
            val base = baseNameKey(sourceFile)
            for ((k, v) in breakpoints) {
                if (baseNameKey(k).equals(base, ignoreCase = true)) {
                    lines.addAll(v)
                }
            }
        }
        return lines
    }

    /**
     * True if any IDE breakpoint line lies in [position.line .. position.endLine] (inclusive).
     * Gutter breakpoints on continuation lines of a multi-line statement still match.
     */
    fun breakpointMatchesPosition(position: Position): Boolean {
        val bpLines = breakpointLinesForSource(position.file)
        if (bpLines.isEmpty()) return false
        val start = position.line
        val end = (position.endLine ?: position.line).coerceAtLeast(start)
        return bpLines.any { it in start..end }
    }

    fun getCallStack(): List<CallFrame> = callStack.toList().reversed()

    fun getCurrentDepth(): Int = callStack.size

    override suspend fun onBeforeExecute(command: IIslCommand, context: ExecutionContext) {
        currentCommand = command
        currentContext = context
        val pos = command.token.position

        val shouldStop = when (steppingMode) {
            SteppingMode.CONTINUE -> breakpointMatchesPosition(pos)
            SteppingMode.STEP_IN -> true
            SteppingMode.STEP_OVER -> getCurrentDepth() <= stepDepth
            SteppingMode.STEP_OUT -> getCurrentDepth() < stepDepth
            SteppingMode.PAUSE -> true
        }

        if (!shouldStop) return

        val reason = when (steppingMode) {
            SteppingMode.CONTINUE -> "breakpoint"
            SteppingMode.STEP_IN, SteppingMode.STEP_OVER, SteppingMode.STEP_OUT -> "step"
            SteppingMode.PAUSE -> "entry"
        }

        stepDepth = getCurrentDepth()

        steppingMode = suspendCancellableCoroutine { continuation ->
            pausedContinuation = continuation
            adapter.notifyStopped(command, context, reason)
        }
    }

    override suspend fun onAfterExecute(command: IIslCommand, context: ExecutionContext, result: CommandResult) {
        lastResult = result
    }

    override fun onFunctionEnter(command: IIslCommand, context: ExecutionContext) {
        val token = command.token
        val name = if (token is FunctionDeclarationToken) "${token.functionType.name.lowercase()} ${token.functionName}()" else token.toString()
        val frame = CallFrame(
            id = ++frameIdCounter,
            name = name,
            command = command,
            context = context
        )
        callStack.addLast(frame)
    }

    override fun onFunctionExit(command: IIslCommand, context: ExecutionContext) {
        if (callStack.isNotEmpty()) {
            callStack.removeLast()
        }
    }

    fun resume(mode: SteppingMode) {
        val cont = pausedContinuation
        pausedContinuation = null
        cont?.resume(mode)
    }
}
