package com.intuit.isl.common

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.ContinuationInterceptor

/**
 * Bridge utility for calling suspend functions from non-suspend code.
 * 
 * This is used at the boundaries where the internal ISL engine (non-suspend, running on virtual threads)
 * needs to call out to host-provided extensions (suspend functions).
 * 
 * On a virtual thread, runBlocking is cheap - it blocks the virtual thread (not an OS thread)
 * and creates a minimal coroutine scope to run the suspend lambda.
 */
internal object SuspendBridge {
    /**
     * Call a suspend function from non-suspend code, preserving the coroutine context.
     * 
     * Filters out the ContinuationInterceptor (dispatcher) from the context to ensure
     * the suspend function runs on the current thread, not on a different thread from the dispatcher.
     * This preserves thread affinity while still respecting cancellation and other context elements.
     * 
     * @param capturedContext The coroutine context from the original entry point (preserves cancellation, logging context, etc.)
     * @param block The suspend function to call
     * @return The result of the suspend function
     */
    fun <T> callSuspend(capturedContext: CoroutineContext = EmptyCoroutineContext, block: suspend () -> T): T {
        // Filter out the dispatcher to run on current thread
        // Keep Job for cancellation and other context elements
        val contextWithoutDispatcher = capturedContext.minusKey(ContinuationInterceptor)
        
        // On a virtual thread, this blocks the VT (cheap — no OS thread wasted)
        // and creates a minimal coroutine scope to run the suspend lambda on the current thread
        return runBlocking(contextWithoutDispatcher) { block() }
    }
}
