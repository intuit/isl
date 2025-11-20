//package com.intuit.isl.debug
//
//import com.intuit.isl.parser.tokens.IIslToken
//import java.lang.StringBuilder
//
//// Make our life easy - learn from the big guys
//// https://microsoft.github.io/debug-adapter-protocol/specification#leftwards_arrow_with_hook-stacktrace-request
//class OperationCallstack(private val frames: Array<StackFrame>) {
//    override fun toString(): String {
//        val sb = StringBuilder();
//        frames.forEach {
//            // mind-blowing - Kotlin & Java don't seem to have format specifiers?
//            sb.appendLine("\t> [${it.id}] ${it.name} L:${it.token.position.line} C:${it.token.position.column}-${it.token.position.endColumn}");
//        }
//        return sb.toString();
//    }
//}
//
//// https://microsoft.github.io/debug-adapter-protocol/specification#stackframe
//data class StackFrame(val id: Number, val name: String, val token: IIslToken);