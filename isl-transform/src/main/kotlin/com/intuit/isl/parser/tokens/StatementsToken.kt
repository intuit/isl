package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.types.IslType
import com.intuit.isl.utils.Position
import java.lang.StringBuilder

class StatementsToken(tokens: List<IIslToken?>, override val position: Position)
    : ArrayList<IIslToken>(tokens.filterNotNull()), IIslToken {

    override val type: String = "statements";

    override var islType: IslType? = null;

    override fun toString(): String {
        return toPrettyString(0);
    }

    override fun toPrettyString(padding: Int): String {
        val sb = StringBuilder("{\n");
        this.forEach {
            sb.append("${it.toPrettyString(padding+2)};\n".padStart(padding, ' '));
        }
        sb.append("}".padStart(padding, ' '));
        return sb.toString();
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}