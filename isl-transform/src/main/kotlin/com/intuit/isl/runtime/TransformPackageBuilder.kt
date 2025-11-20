package com.intuit.isl.runtime

import java.util.*
import java.util.function.BiFunction

/**
 * A builder for TransformPackages. Please note that Packages (like everything else in this runtime)
 * is immutable and can only be built at once. This is due to the way the cross module dependencies
 * are calculated.
 */
class TransformPackageBuilder {
    fun build(
        files: MutableList<FileInfo>,
        findExternalModule: BiFunction<String, String, String>? = null
    ): TransformPackage {
        try {
            // list of loaded modules
            val modules = TreeMap<String, ITransformer>(String.CASE_INSENSITIVE_ORDER);
            val compilationStack = TreeSet(String.CASE_INSENSITIVE_ORDER);

            // the toList here is important because we'll modify the files in case we import external files
            files.toList().forEach {
                compileModule(it, modules, files, compilationStack, findExternalModule);
            }

            return TransformPackage(modules);
        } catch (e: Exception) {
            println("Failed compiling $e");
            throw e;
        }
    }

    private fun compileModule(
        it: FileInfo,
        modules: TreeMap<String, ITransformer>,
        files: MutableList<FileInfo>,
        // used to avoid circular dependencies A > B > C > A
        compilationStack: TreeSet<String>,
        findExternalModule: BiFunction<String, String, String>?
    ): ITransformer {
        val compiledModule = modules[it.name];
        if (compiledModule != null)
            return compiledModule;

        if (compilationStack.contains(it.name))
            throw TransformCompilationException("Circular Dependency Between Modules: ${compilationStack.joinToString(" > ")} > ${it.name}.");

        compilationStack.add(it.name);

        val compiled = TransformCompiler().compileIsl(it.name, it.contents) moduleFinder@{ dependentModule ->
            val internalFound =
                findDependentModule(modules, dependentModule, files, compilationStack, findExternalModule);
            if (internalFound != null)
                return@moduleFinder internalFound;

            // we need to calculate the relative file path for the dependent Module
            val externalFile =
                findExternalModule?.apply(it.name, dependentModule)
                    ?: throw TransformCompilationException("Could not find referenced module $dependentModule from ${it.name}");

            files.add(FileInfo(dependentModule, externalFile));

            val compiled = findDependentModule(modules, dependentModule, files, compilationStack, findExternalModule);
            return@moduleFinder compiled;
        }

        modules[it.name] = compiled;
        return compiled;
    }

    private fun findDependentModule(
        modules: TreeMap<String, ITransformer>,
        dependentModule: String,
        files: MutableList<FileInfo>,
        compilationStack: TreeSet<String>,
        findExternalModule: BiFunction<String, String, String>?
    ): ITransformer? {
        val compiledModule = modules[dependentModule];
        if (compiledModule != null)
            return compiledModule;

        val nonCompiledModule = files.find { f -> f.name.equals(dependentModule, true) };
        if (nonCompiledModule != null) {
            // try to compile this
            val newlyCompiledModule =
                compileModule(nonCompiledModule, modules, files, compilationStack, findExternalModule);
            return newlyCompiledModule;
        }

        return null
    }
}

data class FileInfo(val name: String, val contents: String);