package com.intuit.isl.debug

import com.intuit.isl.commands.CommandResult
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.ExecutionContext

/**
 * Hook injected into the ISL runtime for execution observability (debugging, coverage, tracing, etc.).
 * When null on [ExecutionContext], commands incur only a null check at instrumentation sites.
 * When present, the hook is invoked around statement-level execution and function boundaries.
 */
interface IExecutionHook {

    /**
     * When true, [com.intuit.isl.runtime.Transformer] runs [com.intuit.isl.commands.CoverageStatementIdAssigner.assign]
     * once per [com.intuit.isl.runtime.TransformModule] before execution (no cost on compile-only paths).
     */
    val preparesStatementIds: Boolean get() = false

    /**
     * Called before a statement-level command executes.
     * Implementations may suspend the coroutine (e.g. debugger breakpoints / stepping).
     */
    suspend fun onBeforeExecute(command: IIslCommand, context: ExecutionContext)

    /**
     * Called after a statement-level command executes.
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
