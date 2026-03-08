package com.intuit.isl.cmd

/**
 * Thread-local flags for the current test run. Set by [TestCommand] so that
 * [LogExtensions], [IslModuleResolver], and [YamlUnitTestRunner] can reduce
 * output when -verbose is not passed.
 */
object TestRunFlags {
    private val verbose = ThreadLocal<Boolean?>()

    /** Call at start of test run: pass true for -verbose, false for quiet. */
    fun setTestVerbose(verbose: Boolean) {
        this.verbose.set(verbose)
    }

    /** True when test run is in verbose mode. */
    fun isVerbose(): Boolean = verbose.get() == true

    /** Show script logs (@.Log.Info etc.) only when not in a quiet test run. */
    fun shouldShowScriptLogs(): Boolean = verbose.get() != false

    /** Call when test run finishes (e.g. in finally). */
    fun clear() {
        verbose.remove()
    }
}
