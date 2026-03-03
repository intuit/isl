package com.intuit.isl.cmd

import com.intuit.isl.runtime.FileInfo
import com.intuit.isl.runtime.TransformCompilationException
import com.intuit.isl.runtime.TransformPackageBuilder
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Shared module resolution for ISL commands (transform, validate, test).
 * Resolves relative imports (e.g. `import Customer from "../customer.isl"`) consistently
 * across all commands that compile ISL scripts.
 */
object IslModuleResolver {

    /**
     * Resolves an import path (e.g. "../customer.isl") relative to the current module's directory.
     * @param basePath Base path (typically script directory or search root)
     * @param fromModule Current module path (e.g. "sample.isl" or "tests/sample.isl")
     * @param dependentModule Import path from the script (e.g. "../customer.isl")
     * @return File contents or null if not found
     */
    fun resolveExternalModule(basePath: Path, fromModule: String, dependentModule: String): String? {
        println("[ISL resolve] basePath=${basePath.toAbsolutePath().normalize()}, fromModule=$fromModule, dependentModule=$dependentModule")
        val fromDir = basePath.resolve(fromModule).parent ?: basePath
        val candidateNames = if (dependentModule.endsWith(".isl", ignoreCase = true)) {
            listOf(dependentModule)
        } else {
            listOf("$dependentModule.isl", "$dependentModule.ISL")
        }
        for (name in candidateNames) {
            val candidatePath = fromDir.resolve(name).normalize()
            val file = candidatePath.toFile()
            if (file.exists() && file.isFile) return file.readText()
        }
        val moduleBaseName = if (dependentModule.endsWith(".isl", ignoreCase = true)) {
            dependentModule.dropLast(4)
        } else {
            dependentModule
        }
        return basePath.toFile().walkTopDown()
            .filter { it.isFile && it.extension.equals("isl", true) }
            .find { it.nameWithoutExtension.equals(moduleBaseName, true) }
            ?.readText()
    }

    /**
     * Creates a findExternalModule for TransformPackageBuilder when compiling a single ISL file.
     * Used by transform and validate commands.
     * @param scriptFile The script file being compiled
     * @param scriptContent Content of the script
     * @return Pair of (FileInfo for the script, BiFunction for findExternalModule)
     */
    fun buildPackageForSingleFile(scriptFile: java.io.File, scriptContent: String): Pair<FileInfo, java.util.function.BiFunction<String, String, String>> {
        println("[ISL load] initial file: ${scriptFile.absolutePath}")
        val basePath = scriptFile.parentFile?.toPath()?.normalize() ?: Paths.get(".").toAbsolutePath().normalize()
        val moduleName = scriptFile.name
        val fileInfo = FileInfo(moduleName, scriptContent)
        val findExternalModule = java.util.function.BiFunction<String, String, String> { fromModule, dependentModule ->
            resolveExternalModule(basePath, fromModule, dependentModule)
                ?: throw TransformCompilationException(
                    "Could not find module '$dependentModule' (imported from $fromModule). Searched relative to ${basePath.resolve(fromModule).parent}"
                )
        }
        return fileInfo to findExternalModule
    }

    /**
     * Compiles a single ISL file with dependent module resolution.
     * Returns the TransformPackage for the compiled module.
     */
    fun compileSingleFile(scriptFile: java.io.File, scriptContent: String): com.intuit.isl.runtime.TransformPackage {
        val (fileInfo, findExternalModule) = buildPackageForSingleFile(scriptFile, scriptContent)
        return TransformPackageBuilder().build(mutableListOf(fileInfo), findExternalModule)
    }
}
