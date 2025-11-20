package com.intuit.isl.utils.zip

import com.intuit.isl.utils.IIslReference
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipObject: IIslReference {
    private val bos: ByteArrayOutputStream = ByteArrayOutputStream()
    private val zos: ZipOutputStream = ZipOutputStream(bos)

    fun putNextEntry(entry: ZipEntry) {
        zos.putNextEntry(entry)
    }

    fun write(bytes: ByteArray) {
        zos.write(bytes)
    }

    fun closeEntry() {
        zos.closeEntry()
    }

    fun close() {
        zos.close()
    }
    fun toByteArray(): ByteArray {
        return bos.toByteArray()
    }
}
