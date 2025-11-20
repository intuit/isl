package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.types.IslType
import com.intuit.isl.utils.Position

/**
 * Token representing an object building
 * { property: value }
 */
class DeclareObjectToken(val statements: StatementsToken, position: Position) :
    BaseToken(position, IslType.Object, "object") {
    override fun toString(): String {
        return toPrettyString(0);
    }

    override fun toPrettyString(padding: Int): String {
        return "[Object] ${statements.toPrettyString(padding + 2)}";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}