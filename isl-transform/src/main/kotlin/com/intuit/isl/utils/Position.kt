package com.intuit.isl.utils

data class Position(
    val file: String,
    val line: Int,
    val column: Int,
    val endLine: Int? = null,
    val endColumn: Int? = null)