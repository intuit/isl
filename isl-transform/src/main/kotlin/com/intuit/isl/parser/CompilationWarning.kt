package com.intuit.isl.parser

import com.intuit.isl.parser.tokens.IIslToken

data class CompilationWarning(val token: IIslToken, val message: String) {
}