package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.utils.Position

class LiteralValueToken(val value: Any?, position: Position) : BaseToken(position, null, "literal") {
    override fun toString(): String {
        return "[Val] `$value`";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}