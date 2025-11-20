package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.utils.Position

/**
 * Token representing a spread "...$var"
 */
class SpreadToken(val variable: IIslToken, position: Position) : BaseToken(position) {
    override fun toString(): String {
        return toPrettyString(0);
    }

    override fun toPrettyString(padding: Int): String {
        return "...${variable.toPrettyString(padding+2)}";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}