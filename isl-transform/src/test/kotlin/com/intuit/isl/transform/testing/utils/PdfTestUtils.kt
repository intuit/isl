package com.intuit.isl.transform.testing.utils

import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64

object PdfTestUtils {
    /**
     * Optionally decode Base64 and write bytes to outputPath, returning true if bytes look like a
     * PDF.
     * - If tryBase64Decode is true, attempts Base64 decode first; on failure, writes raw bytes.
     * - Overwrites existing file when overwrite=true.
     */
    fun writePdf(
            outputPath: String,
            data: ByteArray,
            tryBase64Decode: Boolean = true,
            overwrite: Boolean = true
    ): Boolean {
        require(outputPath.isNotBlank()) { "outputPath must not be blank" }

        var bytesToWrite = data
        if (tryBase64Decode) {
            val decoded = tryDecodeBase64(data)
            if (decoded != null) bytesToWrite = decoded
        }

        val looksLikePdf = isPdf(bytesToWrite)

        val path = Paths.get(outputPath)
        val parent = path.parent
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
        }
        if (!overwrite && Files.exists(path)) {
            throw IllegalStateException("File already exists at $outputPath and overwrite=false")
        }
        Files.write(path, bytesToWrite)
        return looksLikePdf
    }

    fun writePdf(
            outputPath: String,
            content: String,
            tryBase64Decode: Boolean = true,
            overwrite: Boolean = true
    ): Boolean {
        return writePdf(outputPath, content.toByteArray(Charsets.UTF_8), tryBase64Decode, overwrite)
    }

    private fun tryDecodeBase64(data: ByteArray): ByteArray? {
        val ascii = String(data, Charsets.US_ASCII).trim().replace("\\s+".toRegex(), "")
        if (ascii.isEmpty()) return null
        return try {
            Base64.getDecoder().decode(ascii)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun isPdf(bytes: ByteArray): Boolean {
        if (bytes.size < 5) return false
        val prefix = "%PDF-".toByteArray(Charsets.US_ASCII)
        for (i in prefix.indices) {
            if (bytes[i] != prefix[i]) return false
        }
        return true
    }
}
