package com.intuit.isl.runtime

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class TransformProjectBuilder {

    private val manifestFileName = "manifest.json"

    private val islVersion = Transformer.version

    private val objectMapper = jacksonObjectMapper()

    fun writeProject(
        files: List<FileInfo>,
        projectVersion: String,
        stream: ByteArrayOutputStream
    ): TransformProject {
        var setIslVersion = islVersion
        var manifest = TransformProjectManifest(files.map { f -> f.name }.toSet(), setIslVersion, projectVersion)
        ZipOutputStream(stream).use { t ->
            for (file in files) {
                val zipFile = ZipEntry(file.name)
                t.putNextEntry(zipFile)
                t.write(file.contents.toByteArray())
                t.closeEntry()
            }
            val manifestFile = ZipEntry(manifestFileName)
            t.putNextEntry(manifestFile)
            t.write(objectMapper.writeValueAsBytes(manifest))
            t.closeEntry()
        }

        return TransformProject(manifest)
    }

    fun readProject(projectStream: InputStream): TransformPackage? {
        val transformPackageBuilder = TransformPackageBuilder()
        val fileInfos: ArrayList<FileInfo> = ArrayList()
        // Set default values for manifest

        var manifest : TransformProjectManifest? = null

        ZipInputStream(projectStream).use { zipStream ->
            generateSequence { zipStream.nextEntry }.forEach { entry ->
                if (entry.name.contains(".isl")) {
                    val contents = zipStream.readBytes().toString(Charsets.UTF_8)
                    fileInfos.add(FileInfo(entry.name, contents))
                }

                if (entry.name == manifestFileName) {
                    val contents = zipStream.readBytes().toString(Charsets.UTF_8)
                    manifest = objectMapper.readValue<TransformProjectManifest>(contents)
                }
            }
        }

        if (fileInfos.size == 0) {
            return null
        }

        var finalList : List<FileInfo> = fileInfos
        if (manifest != null) {
            finalList = fileInfos.filter { f -> manifest!!.files.contains(f.name) }
        }

        return transformPackageBuilder.build(finalList.toMutableList())
    }
}

