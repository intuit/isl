package com.intuit.isl.test

import com.intuit.isl.common.IOperationContext
import com.intuit.isl.debug.IExecutionHook
import com.intuit.isl.runtime.FileInfo
import com.intuit.isl.runtime.TransformPackageBuilder
import java.nio.file.Path
import java.util.function.BiFunction

class TransformTestPackageBuilder {
    private val transformPackageBuilder = TransformPackageBuilder()

    fun build(
        files: MutableList<FileInfo>,
        findExternalModule: BiFunction<String, String, String>? = null,
        basePath: Path? = null,
        contextCustomizers: List<(IOperationContext) -> Unit> = emptyList(),
        executionHook: IExecutionHook? = null
    ): TransformTestPackage {
        val transformPackage = transformPackageBuilder.build(files, findExternalModule)
        return TransformTestPackage(transformPackage, basePath, contextCustomizers, executionHook)
    }
}