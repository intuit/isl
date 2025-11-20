package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.utils.ExcludeFromJacocoGeneratedReport
import com.intuit.isl.utils.Position
import java.lang.StringBuilder

open class ForEachToken(val iterator: String, val source: IIslToken, val statements: IIslToken, position: Position) :
    BaseToken(position) {

    @ExcludeFromJacocoGeneratedReport
    override fun toString(): String {
        return toPrettyString(0);
    }

    @ExcludeFromJacocoGeneratedReport
    override fun toPrettyString(padding: Int): String {
        val sb = StringBuilder();
        sb.appendLine("[foreach] \$$iterator in $source {".padStart(padding, ' '));
        sb.appendLine("${statements.toPrettyString(padding + 2)}\n");
        sb.appendLine("}\n".padStart(padding, ' '));
        return sb.toString();
    }

    @ExcludeFromJacocoGeneratedReport
    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}