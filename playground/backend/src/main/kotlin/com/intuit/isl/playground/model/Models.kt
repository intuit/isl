package com.intuit.isl.playground.model

data class TransformRequest(
    val isl: String,
    val input: String
)

data class ValidationRequest(
    val isl: String
)

data class TransformResponse(
    val success: Boolean,
    val output: String? = null,
    val error: ErrorDetail? = null
)

data class ValidationResponse(
    val valid: Boolean,
    val errors: List<ErrorDetail> = emptyList()
)

data class ErrorDetail(
    val message: String,
    val line: Int? = null,
    val column: Int? = null,
    val type: String = "ERROR"
)

data class ExampleResponse(
    val name: String,
    val description: String,
    val isl: String,
    val input: String,
    val expectedOutput: String
)

