package com.intuit.isl.utils

data class Position(
    val file: String,
    val line: Int,
    val column: Int,
    val endLine: Int? = null,
    val endColumn: Int? = null) {

    /**
     * Format as file:line or file:line:column so editors (e.g. VSCode) can parse and open the location.
     */
    override fun toString(): String = "$file:$line:$column"
}