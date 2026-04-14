package com.intuit.isl.runtime.serialization

/**
 * Thrown by [com.intuit.isl.runtime.TransformPackageBuilder.loadCompiled] when embedded fingerprints do not match [files].
 */
class SourceFingerprintMismatchException(val mismatches: List<ModuleSourceMismatch>) :
    Exception(
        mismatches.joinToString(prefix = "Source fingerprint mismatch: ", separator = "; ") { "${it.moduleName}: ${it.reason}" }
    )
