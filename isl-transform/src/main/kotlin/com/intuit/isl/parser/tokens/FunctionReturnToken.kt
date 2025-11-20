package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.utils.Position

/**
 * fun doMagic(){
 *    return { magic };
 * }
 */
class FunctionReturnToken (val value: IIslToken, position: Position) : BaseToken(position) {
    override fun toString(): String {
        return "[Return] $value";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}
