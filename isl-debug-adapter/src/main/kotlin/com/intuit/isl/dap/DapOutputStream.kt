package com.intuit.isl.dap

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

/**
 * A PrintStream that captures all writes and forwards them as DAP output events.
 * Used to redirect System.out (println, Log.Info, etc.) into the Debug Console.
 */
class DapOutputStream(
    private val adapter: DapAdapter,
    private val category: String = "console"
) : PrintStream(DapEventStream(adapter, category), true, StandardCharsets.UTF_8)

private class DapEventStream(
    private val adapter: DapAdapter,
    private val category: String
) : ByteArrayOutputStream() {

    override fun flush() {
        val text = toString(StandardCharsets.UTF_8)
        if (text.isNotEmpty()) {
            adapter.sendOutputEvent(text, category)
            reset()
        }
        super.flush()
    }

    override fun write(b: Int) {
        super.write(b)
        if (b == '\n'.code) flush()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        super.write(b, off, len)
        val written = String(b, off, len, StandardCharsets.UTF_8)
        if (written.contains('\n')) flush()
    }
}
