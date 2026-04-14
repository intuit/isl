package com.intuit.isl.runtime.serialization

/**
 * Outcome of [com.intuit.isl.runtime.TransformPackageBuilder.verifyCompiledAgainstSources].
 */
sealed class SourceArtifactMatchResult {
    /** Artifact has no embedded source fingerprints (legacy or produced without source recording). */
    data object NoFingerprintsStored : SourceArtifactMatchResult()

    /** Every stored fingerprint matched the supplied UTF-8 source. */
    data object Match : SourceArtifactMatchResult()

    /** At least one module failed the hash check or was missing from [moduleSourcesUtf8]. */
    data class Mismatch(val modules: List<ModuleSourceMismatch>) : SourceArtifactMatchResult()
}

data class ModuleSourceMismatch(val moduleName: String, val reason: String)
