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
 *
 * When [resolvedPaths] is provided, the directory of the **importing** module is taken from
 * where that module was actually loaded (so e.g. coreUtils.isl is resolved relative to
 * lib/isl_common_utils/ when imported from bankingUtils.isl in that folder), not relative to basePath.
 */
object IslModuleResolver {

    /**
     * Resolves an import path relative to the importing module's directory.
     * @param basePath Base path (script directory or search root)
     * @param fromModule Current module name/path as known by the runtime (e.g. "lib/isl_common_utils/bankingUtils.isl")
     * @param dependentModule Import path from the script (e.g. "coreUtils.isl" or "../other.isl")
     * @param resolvedPaths Optional map of module name -> absolute path where that module was loaded.
     *                      When provided, fromDir is taken from the path of fromModule if present,
     *                      and each resolved dependency is recorded so nested imports resolve correctly.
     * @return File contents or null if not found
     */
    fun resolveExternalModule(
        basePath: Path,
        fromModule: String,
        dependentModule: String,
        resolvedPaths: MutableMap<String, Path>? = null
    ): String? {
        val base = basePath.toAbsolutePath().normalize()
        val fromDir = when {
            resolvedPaths != null && resolvedPaths.containsKey(fromModule) -> resolvedPaths[fromModule]!!.parent
            else -> base.resolve(fromModule).parent ?: base
        }
        if (TestRunFlags.shouldShowScriptLogs()) println("[ISL resolve] fromModule=$fromModule, dependentModule=$dependentModule, fromDir=$fromDir")
        val candidateNames = if (dependentModule.endsWith(".isl", ignoreCase = true)) {
            listOf(dependentModule)
        } else {
            listOf("$dependentModule.isl", "$dependentModule.ISL")
        }
        for (name in candidateNames) {
            val candidatePath = fromDir.resolve(name).normalize().toAbsolutePath()
            val file = candidatePath.toFile()
            if (file.exists() && file.isFile) {
                resolvedPaths?.set(dependentModule, candidatePath)
                return file.readText()
            }
        }
        val moduleBaseName = if (dependentModule.endsWith(".isl", ignoreCase = true)) {
            dependentModule.dropLast(4)
        } else {
            dependentModule
        }
        val found = base.toFile().walkTopDown()
            .filter { it.isFile && it.extension.equals("isl", true) }
            .find { it.nameWithoutExtension.equals(moduleBaseName, true) }
        if (found != null && resolvedPaths != null) resolvedPaths[dependentModule] = found.toPath().toAbsolutePath().normalize()
        return found?.readText()
    }

    /**
     * Creates a findExternalModule that records where each module was loaded so nested imports
     * resolve relative to the importing file's actual directory.
     * @param basePath Base path for resolution when a module is not yet in the map
     * @param resolvedPaths Map to fill: module name -> path where it was loaded. Prime with initial module(s) before use.
     * @return BiFunction suitable for TransformPackageBuilder
     */
    fun createModuleFinder(
        basePath: Path,
        resolvedPaths: MutableMap<String, Path>
    ): java.util.function.BiFunction<String, String, String> {
        return java.util.function.BiFunction { fromModule, dependentModule ->
            resolveExternalModule(basePath, fromModule, dependentModule, resolvedPaths)
                ?: throw TransformCompilationException(
                    "Could not find module '$dependentModule' (imported from $fromModule). Searched relative to ${resolvedPaths[fromModule]?.parent ?: basePath.resolve(fromModule).parent}"
                )
        }
    }

    /**
     * Creates a findExternalModule for TransformPackageBuilder when compiling a single ISL file.
     * Uses resolution history so nested imports (e.g. lib/foo.isl importing bar.isl) resolve relative to the importing file.
     */
    fun buildPackageForSingleFile(
        scriptFile: java.io.File,
        scriptContent: String
    ): Pair<FileInfo, java.util.function.BiFunction<String, String, String>> {
        if (TestRunFlags.shouldShowScriptLogs()) println("[ISL load] initial file: ${scriptFile.absolutePath}")
        val basePath = scriptFile.parentFile?.toPath()?.normalize() ?: Paths.get(".").toAbsolutePath().normalize()
        val moduleName = scriptFile.name
        val fileInfo = FileInfo(moduleName, scriptContent)
        val resolvedPaths = mutableMapOf<String, Path>()
        resolvedPaths[moduleName] = scriptFile.toPath().toAbsolutePath().normalize()
        val findExternalModule = createModuleFinder(basePath, resolvedPaths)
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
