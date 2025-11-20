//package com.intuit.isl.parser.tokens
//
//import com.intuit.isl.commands.builder.IIslTokenVisitor
//import com.intuit.isl.utils.Position
//import java.lang.StringBuilder
//
//class WithLockToken(val statements: IIslToken, position: Position) :
//    BaseToken(position) {
//    override fun toString(): String {
//        return toPrettyString(0);
//    }
//
//    override fun toPrettyString(padding: Int): String {
//        val sb = StringBuilder();
//        sb.appendLine("[lock] {".padStart(padding, ' '));
//        sb.appendLine("${statements.toPrettyString(padding + 2)}\n");
//        sb.appendLine("}\n".padStart(padding, ' '));
//        return sb.toString();
//    }
//
//    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
//        return visitor.visit(this);
//    }
//}