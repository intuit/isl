package com.intuit.isl.test.assertions

import com.intuit.isl.utils.Position

class ComparisonAssertException(
    message: String,
    functionName : String,
    val expectedValue: Any?,
    val actualValue: Any?,
    position: Position? = null,
    cause: Throwable? = null
) : AssertException(message, functionName, position, cause)