package com.intuit.isl.debug

import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.ExecutionContext

/**
 * Hook injected into the ISL runtime to support debugging.
 * When null on [ExecutionContext], commands execute with zero overhead (a single null check).
 * When present, the hook is called before each statement-level command, allowing the debugger
 * to suspend execution (breakpoints, stepping) without any decorator wrappers.
 */
interface IDebugHook {
    /**
     * Called before a statement-level command executes.
     * Implementations may suspend the coroutine to pause execution (e.g. at a breakpoint).
     */
    suspend fun onBeforeExecute(command: IIslCommand, context: ExecutionContext)

    /**
     * Called after a statement-level command executes.
     * Useful for capturing result values in the Variables panel.
     */
    suspend fun onAfterExecute(command: IIslCommand, context: ExecutionContext, result: CommandResult)

    /**
     * Called when entering a function body (push call frame).
     */
    fun onFunctionEnter(command: IIslCommand, context: ExecutionContext)

    /**
     * Called when leaving a function body (pop call frame).
     */
    fun onFunctionExit(command: IIslCommand, context: ExecutionContext)
}
