package com.intuit.isl.test.annotations

import com.intuit.isl.utils.Position
import java.lang.Exception

data class TestResult(
    val testFile: String,
    val functionName: String,
    val testName: String,
    val testGroup: String?,
    var success: Boolean,
    var message: String? = null,
    var errorPosition: Position? = null,
    var exception: Exception? = null
)