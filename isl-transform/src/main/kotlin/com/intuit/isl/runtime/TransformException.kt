package com.intuit.isl.runtime

import com.intuit.isl.utils.Position

class TransformException(message: String, override val position: Position?, cause: Throwable?) : IslException, Exception(message, cause) {
    constructor(message: String, position: Position) : this("$message at $position", position, null) {}
}

interface IslException {
    val position: Position?
}