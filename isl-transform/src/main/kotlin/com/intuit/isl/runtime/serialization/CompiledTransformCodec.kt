package com.intuit.isl.runtime.serialization

import com.google.protobuf.ByteString
import com.intuit.isl.commands.FunctionDeclarationCommand
import com.intuit.isl.parser.tokens.FunctionDeclarationToken
import com.intuit.isl.parser.tokens.FunctionType
import com.intuit.isl.parser.tokens.ImportDeclarationToken
import com.intuit.isl.parser.tokens.ModuleImplementationToken
import com.intuit.isl.commands.NoopToken
import com.intuit.isl.serialization.proto.CompiledFunctionPB
import com.intuit.isl.serialization.proto.CompiledModulePB
import com.intuit.isl.serialization.proto.CompiledTransformPackagePB
import com.intuit.isl.serialization.proto.FunctionTypePB
import com.intuit.isl.serialization.proto.ImportDeclPB
import com.intuit.isl.serialization.proto.IslCompiledFileEnvelope
import com.intuit.isl.serialization.proto.SourceModuleFingerprint
import com.intuit.isl.runtime.ITransformer
import com.intuit.isl.runtime.TransformModule
import com.intuit.isl.runtime.TransformPackage
import com.intuit.isl.runtime.Transformer
import com.intuit.isl.types.IslType
import com.intuit.isl.types.JsonProperty
import com.intuit.isl.utils.Position
import java.security.MessageDigest
import java.util.TreeMap

/**
 * Binary **pre-compiled** transform packages (protobuf payload + file envelope). **Module-internal**;
 * use [com.intuit.isl.runtime.TransformPackageBuilder.preCompileToBytes] and
 * [com.intuit.isl.runtime.TransformPackageBuilder.Companion.loadCompiled] from application code.
 */
internal object CompiledTransformCodec {

    internal const val FORMAT_VERSION: Int = 1

    /** ASCII "ISLC" — Intuit Serialized Language Compiled */
    internal const val MAGIC: Int = 0x49534C43

    /** SHA-256 digest length in bytes. */
    internal const val SHA256_BYTES: Int = 32

    /** Writes a versioned `.islc`-style envelope (no source fingerprints when [moduleSourcesUtf8] is null). */
    internal fun preCompileToBytes(pkg: TransformPackage, moduleSourcesUtf8: Map<String, String>? = null): ByteArray {
        val inner = preCompilePackagePayload(pkg)
        val env = IslCompiledFileEnvelope.newBuilder()
            .setMagic(MAGIC)
            .setFormatVersion(FORMAT_VERSION)
            .setIslImplementationVersion(Transformer.version)
            .setPayload(ByteString.copyFrom(inner))
        if (moduleSourcesUtf8 != null) {
            val ciSources = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            moduleSourcesUtf8.forEach { (k, v) -> ciSources[k] = v }
            val pkgModulesCi = pkg.modules.map { it.lowercase() }.toSet()
            val extra = ciSources.keys.map { it.lowercase() }.toSet() - pkgModulesCi
            require(extra.isEmpty()) {
                "moduleSourcesUtf8 has modules not in package: ${extra.joinToString()}"
            }
            val fingerprints = ArrayList<SourceModuleFingerprint>(pkg.modules.size)
            for (moduleName in pkg.modules) {
                val source = ciSources[moduleName]
                    ?: error("moduleSourcesUtf8 missing module '$moduleName'")
                val digest = sha256Utf8(source)
                fingerprints.add(
                    SourceModuleFingerprint.newBuilder()
                        .setModuleName(moduleName)
                        .setSha256(ByteString.copyFrom(digest))
                        .build()
                )
            }
            fingerprints.sortBy { it.moduleName.lowercase() }
            val seen = HashSet<String>(fingerprints.size)
            for (fp in fingerprints) {
                check(seen.add(fp.moduleName.lowercase())) {
                    "duplicate module fingerprint '${fp.moduleName}'"
                }
            }
            env.addAllSourceFingerprints(fingerprints)
        }
        return env.build().toByteArray()
    }

    /** SHA-256 of [text] encoded as UTF-8. */
    internal fun sha256Utf8(text: String): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(text.toByteArray(Charsets.UTF_8))
        return md.digest()
    }

    /** Parses only the file envelope (no command graph decode). */
    internal fun parseEnvelope(bytes: ByteArray): IslCompiledFileEnvelope {
        val env = IslCompiledFileEnvelope.parseFrom(bytes)
        require(env.magic == MAGIC) { "bad magic, expected $MAGIC got ${env.magic}" }
        require(env.formatVersion == FORMAT_VERSION) {
            "unsupported formatVersion ${env.formatVersion}, expected $FORMAT_VERSION"
        }
        return env
    }

    /**
     * Stored fingerprints from an artifact (empty if none were written).
     * Keys use the **module names as stored** in the file (same casing as at pre-compile).
     */
    internal fun readStoredSourceFingerprints(bytes: ByteArray): Map<String, ByteArray> {
        val env = parseEnvelope(bytes)
        if (env.sourceFingerprintsCount == 0) {
            return emptyMap()
        }
        val out = TreeMap<String, ByteArray>(String.CASE_INSENSITIVE_ORDER)
        for (fp in env.sourceFingerprintsList) {
            val prev = out.put(fp.moduleName, fp.sha256.toByteArray())
            require(prev == null) { "duplicate fingerprint for module '${fp.moduleName}'" }
        }
        return out
    }

    /**
     * Compares current UTF-8 sources to fingerprints embedded in the artifact.
     *
     * - [SourceArtifactMatchResult.NoFingerprintsStored] — artifact was built without [moduleSourcesUtf8]; nothing to check.
     * - [SourceArtifactMatchResult.Match] — every stored fingerprint matches the given source.
     * - [SourceArtifactMatchResult.Mismatch] — missing module, wrong hash, or extra constraints failed.
     */
    internal fun verifySourcesMatchArtifact(bytes: ByteArray, moduleSourcesUtf8: Map<String, String>): SourceArtifactMatchResult {
        val env = parseEnvelope(bytes)
        if (env.sourceFingerprintsCount == 0) {
            return SourceArtifactMatchResult.NoFingerprintsStored
        }
        val ciSources = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
        moduleSourcesUtf8.forEach { (k, v) -> ciSources[k] = v }
        val mismatches = ArrayList<ModuleSourceMismatch>()
        for (fp in env.sourceFingerprintsList) {
            val name = fp.moduleName
            val expected = fp.sha256.toByteArray()
            if (expected.size != SHA256_BYTES) {
                mismatches.add(ModuleSourceMismatch(name, "stored digest length ${expected.size}, expected $SHA256_BYTES"))
                continue
            }
            val source = ciSources[name]
            if (source == null) {
                mismatches.add(ModuleSourceMismatch(name, "no source provided for module"))
                continue
            }
            val actual = sha256Utf8(source)
            if (!actual.contentEquals(expected)) {
                mismatches.add(ModuleSourceMismatch(name, "SHA-256 differs from stored fingerprint"))
            }
        }
        return if (mismatches.isEmpty()) SourceArtifactMatchResult.Match else SourceArtifactMatchResult.Mismatch(mismatches)
    }

    /** Protobuf package payload only (no file envelope). */
    internal fun preCompilePackagePayload(pkg: TransformPackage): ByteArray {
        val pkgB = CompiledTransformPackagePB.newBuilder()
        for (name in pkg.modules) {
            val t = pkg.getModule(name) ?: continue
            val mod = t.module
            val modB = CompiledModulePB.newBuilder().setName(name)
            for (imp in mod.imports) {
                modB.addImports(
                    ImportDeclPB.newBuilder()
                        .setAlias(imp.name)
                        .setSourceModule(imp.sourceName)
                )
            }
            for (fn in mod.functions) {
                val fdc = fn as FunctionDeclarationCommand
                val enc = ProtoCommandGraphEncoder(name)
                val graph = enc.encodeRoot(fdc.statements)
                modB.addFunctions(
                    CompiledFunctionPB.newBuilder()
                        .setFuncType(
                            if (fdc.token.functionType == FunctionType.Function) FunctionTypePB.FUNCTION
                            else FunctionTypePB.MODIFIER
                        )
                        .setName(fdc.name)
                        .addAllParamNames(fdc.token.arguments.map { it.name })
                        .setBody(graph)
                )
            }
            pkgB.addModules(modB)
        }
        return pkgB.build().toByteArray()
    }

    /** Reads an envelope produced by [preCompileToBytes] and returns a linked [TransformPackage]. */
    internal fun loadPreCompiledFromBytes(bytes: ByteArray): TransformPackage {
        val env = parseEnvelope(bytes)
        return loadPreCompiledPackagePayload(env.payload.toByteArray())
    }

    /**
     * Loads a pre-compiled package after verifying embedded source fingerprints against [moduleSourcesUtf8].
     *
     * @throws SourceFingerprintMismatchException if fingerprints exist and do not all match.
     */
    internal fun loadPreCompiledFromBytes(bytes: ByteArray, moduleSourcesUtf8: Map<String, String>): TransformPackage {
        when (val v = verifySourcesMatchArtifact(bytes, moduleSourcesUtf8)) {
            SourceArtifactMatchResult.NoFingerprintsStored,
            SourceArtifactMatchResult.Match -> Unit
            is SourceArtifactMatchResult.Mismatch -> throw SourceFingerprintMismatchException(v.modules)
        }
        val env = parseEnvelope(bytes)
        return loadPreCompiledPackagePayload(env.payload.toByteArray())
    }

    /** Loads a package from a raw protobuf payload (e.g. inner bytes of the envelope). */
    internal fun loadPreCompiledPackagePayload(payload: ByteArray): TransformPackage {
        val pkgPb = CompiledTransformPackagePB.parseFrom(payload)
        val tree = TreeMap<String, ITransformer>(String.CASE_INSENSITIVE_ORDER)
        for (modPb in pkgPb.modulesList) {
            val p = Position(modPb.name, 0, 0)
            val imports = modPb.importsList.map {
                ImportDeclarationToken(it.alias, it.sourceModule, p)
            }
            val funcTokens = modPb.functionsList.map { synthFunctionToken(modPb.name, it) }
            val moduleToken = ModuleImplementationToken(
                modPb.name,
                imports,
                emptyMap(),
                funcTokens,
                p
            )
            val builtFunctions = mutableListOf<com.intuit.isl.commands.IFunctionDeclarationCommand>()
            for (i in modPb.functionsList.indices) {
                val fnPb = modPb.functionsList[i]
                val fTok = funcTokens[i]
                val decoder = ProtoCommandGraphDecoder(DecodeContext(modPb.name, fTok))
                val body = decoder.decodeRoot(fnPb.body)
                builtFunctions.add(FunctionDeclarationCommand(fTok, body))
            }
            val tm = TransformModule(modPb.name, builtFunctions, moduleToken)
            tree[modPb.name] = Transformer(tm)
        }
        val result = TransformPackage(tree)
        TransformPackageLinker.link(result)
        return result
    }

    private fun synthFunctionToken(moduleName: String, fn: CompiledFunctionPB): FunctionDeclarationToken {
        val p = Position(moduleName, 0, 0)
        val params = fn.paramNamesList.map { JsonProperty(it, IslType.Any) }
        val ft = when (fn.funcType) {
            FunctionTypePB.MODIFIER -> FunctionType.Modifier
            else -> FunctionType.Function
        }
        return FunctionDeclarationToken(ft, fn.name, listOf(), params, NoopToken(), IslType.Any, p)
    }
}
