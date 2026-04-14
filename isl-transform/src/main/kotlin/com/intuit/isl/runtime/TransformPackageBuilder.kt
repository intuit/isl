package com.intuit.isl.runtime

import com.intuit.isl.runtime.serialization.CompiledTransformCodec
import com.intuit.isl.runtime.serialization.SourceArtifactMatchResult
import java.util.*
import java.util.function.BiFunction

/**
 * A builder for TransformPackages. Please note that Packages (like everything else in this runtime)
 * is immutable and can only be built at once. This is due to the way the cross module dependencies
 * are calculated.
 *
 * **Pre-compiled `.islc` packages:** use [preCompileToBytes] (same inputs as [build]) to emit bytes with
 * embedded source fingerprints, and [Companion.loadCompiled] to load + link a [TransformPackage] at runtime.
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

    /**
     * Compiles the same [files] graph as [build], then writes a versioned **`.islc`** blob (protobuf envelope +
     * optional command graph) including **SHA-256 fingerprints** of each module’s source text from [files]
     * (after any external modules were resolved the same way as [build]).
     */
    fun preCompileToBytes(
        files: MutableList<FileInfo>,
        findExternalModule: BiFunction<String, String, String>? = null
    ): ByteArray {
        requireUniqueModuleNames(files)
        val working = files.map { FileInfo(it.name, it.contents) }.toMutableList()
        val pkg = build(working, findExternalModule)
        val sources = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
        working.forEach { sources[it.name] = it.contents }
        for (m in pkg.modules) {
            check(sources.containsKey(m)) {
                "internal error: compiled module '$m' has no corresponding FileInfo source after build"
            }
        }
        return CompiledTransformCodec.preCompileToBytes(pkg, sources)
    }

    private fun requireUniqueModuleNames(files: List<FileInfo>) {
        val seen = HashSet<String>()
        for (f in files) {
            val key = f.name.lowercase()
            require(seen.add(key)) { "duplicate module name in files: ${f.name}" }
        }
    }

    companion object {
        /**
         * Decodes a **`.islc`** envelope, links hardwired calls, and returns a ready [TransformPackage].
         */
        fun loadCompiled(bytes: ByteArray): TransformPackage =
            CompiledTransformCodec.loadPreCompiledFromBytes(bytes)

        /**
         * Like [loadCompiled] but verifies embedded source **SHA-256** fingerprints against [files] first.
         *
         * @throws com.intuit.isl.runtime.serialization.SourceFingerprintMismatchException if fingerprints exist and do not match.
         */
        fun loadCompiled(bytes: ByteArray, files: MutableList<FileInfo>): TransformPackage =
            CompiledTransformCodec.loadPreCompiledFromBytes(bytes, sourcesFromFiles(files))

        /**
         * Compares [files] to embedded fingerprints without decoding the full command graph (cheap).
         */
        fun verifyCompiledAgainstSources(bytes: ByteArray, files: MutableList<FileInfo>): SourceArtifactMatchResult =
            CompiledTransformCodec.verifySourcesMatchArtifact(bytes, sourcesFromFiles(files))

        private fun sourcesFromFiles(files: Iterable<FileInfo>): Map<String, String> {
            val m = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            for (f in files) {
                require(m.put(f.name, f.contents) == null) {
                    "duplicate module name in files: ${f.name}"
                }
            }
            return m
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