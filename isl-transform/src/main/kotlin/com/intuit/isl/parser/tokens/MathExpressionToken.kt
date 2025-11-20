package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.utils.Position

class MathExpressionToken(val left: IIslToken, val right: IIslToken, val operation: String, position: Position)
    : BaseToken(position) {

    override fun toString(): String {
        return "( $left $operation $right )";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}
