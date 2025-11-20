package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.utils.Position

class ImportDeclarationToken (val name: String, val sourceName: String, position: Position) : BaseToken(position) {
    override fun toString(): String {
        return "[import] @.$name from $sourceName\n";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}