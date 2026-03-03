package com.intuit.isl.test.assertions

import com.intuit.isl.runtime.IslException
import com.intuit.isl.utils.Position

open class AssertException(
    message: String,
    val functionName: String,
    override val position: Position? = null,
    cause: Throwable? = null
) : IslException, Exception(message, cause)