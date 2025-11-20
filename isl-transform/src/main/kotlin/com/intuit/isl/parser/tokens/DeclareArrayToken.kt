package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.utils.Position

/**
 * Array definition
 */
class DeclareArrayToken(val values: Array<IIslToken>, position: Position) : BaseToken(position) {
    override fun toString(): String {
        return "[Array] [ ${values.joinToString(",")} ]";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}