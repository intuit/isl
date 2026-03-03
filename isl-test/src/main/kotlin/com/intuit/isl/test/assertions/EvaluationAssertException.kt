package com.intuit.isl.test.assertions

import com.intuit.isl.utils.Position

class EvaluationAssertException(
    message: String,
    functionName : String,
    val inputValue: Any?,
    position: Position? = null,
    cause: Throwable? = null
) : AssertException(message, functionName, position, cause)