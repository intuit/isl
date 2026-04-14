package com.intuit.isl.runtime.serialization

/** Thrown when a command graph cannot be written to or read from the pre-compiled binary format. */
class UnsupportedCommandForPreCompileException(message: String) : RuntimeException(message)
