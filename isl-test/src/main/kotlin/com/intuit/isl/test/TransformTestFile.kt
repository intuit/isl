package com.intuit.isl.test

data class TransformTestFile(
    val fileName: String,
    val testFunctions: Set<String> = setOf(),
    val setupFile: String? = null
)