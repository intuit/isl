package com.intuit.isl.dap

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

/**
 * Reads and writes DAP messages using the base protocol (Content-Length header + JSON body).
 * See: https://microsoft.github.io/debug-adapter-protocol/overview#base-protocol
 */
class DapTransport(
    private val input: InputStream,
    private val output: OutputStream
) {
    val mapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    fun readMessage(): DapMessage? {
        val contentLength = readHeaders() ?: return null
        val body = ByteArray(contentLength)
        var read = 0
        while (read < contentLength) {
            val n = input.read(body, read, contentLength - read)
            if (n < 0) return null
            read += n
        }
        return mapper.readValue(body, DapMessage::class.java)
    }

    @Synchronized
    fun writeMessage(message: DapMessage) {
        val json = mapper.writeValueAsString(message)
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val header = "Content-Length: ${bytes.size}\r\n\r\n"
        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun readHeaders(): Int? {
        val headerBuffer = StringBuilder()
        while (true) {
            val line = readLine() ?: return null
            if (line.isEmpty()) break
            headerBuffer.append(line).append("\n")
        }
        val headers = headerBuffer.toString()
        val match = Regex("Content-Length:\\s*(\\d+)", RegexOption.IGNORE_CASE).find(headers)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun readLine(): String? {
        val sb = StringBuilder()
        while (true) {
            val c = input.read()
            if (c < 0) return if (sb.isEmpty()) null else sb.toString()
            if (c == '\n'.code) {
                // strip trailing \r
                if (sb.isNotEmpty() && sb.last() == '\r') sb.deleteCharAt(sb.length - 1)
                return sb.toString()
            }
            sb.append(c.toChar())
        }
    }
}
