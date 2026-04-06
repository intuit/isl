package com.intuit.isl.dap

/**
 * ISL Debug Adapter entry point.
 * Communicates with VSCode/Cursor via the Debug Adapter Protocol over stdin/stdout.
 *
 * Process termination is handled by [DapAdapter]: it calls exitProcess()
 * on disconnect, terminate, or via a watchdog after the transform completes.
 */
fun main() {
    // DAP uses stdin/stdout for JSON-RPC; redirect println away from it immediately
    val originalOut = System.out
    System.setOut(System.err)

    val transport = DapTransport(System.`in`, originalOut)
    val adapter = DapAdapter(transport)

    // Now redirect System.out so that println/Log.Info/etc. appear in the Debug Console
    System.setOut(DapOutputStream(adapter, "console"))

    adapter.run()
}
