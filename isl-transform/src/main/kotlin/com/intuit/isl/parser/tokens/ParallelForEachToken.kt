package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.utils.Position
import java.lang.StringBuilder

class ParallelForEachToken(iterator: String, val options: IIslToken?, source: IIslToken, statements: IIslToken, position: Position) :
    ForEachToken(iterator, source, statements, position) {
    override fun toString(): String {
        return toPrettyString(0);
    }

    override fun toPrettyString(padding: Int): String {
        val sb = StringBuilder();
        sb.appendLine("[parallel.foreach] \$$iterator in $source {".padStart(padding, ' '));
        sb.appendLine("${statements.toPrettyString(padding + 2)}\n");
        sb.appendLine("}\n".padStart(padding, ' '));
        return sb.toString();
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}