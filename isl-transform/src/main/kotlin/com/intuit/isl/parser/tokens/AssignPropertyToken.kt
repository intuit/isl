package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.types.IslType
import com.intuit.isl.utils.Position

/**
 * value: ....
 */
class AssignPropertyToken(val name: String, val value: IIslToken, islType: IslType?, position: Position) :
    BaseToken(position, islType, "assignprop") {
    override fun toString(): String {
        return toPrettyString(0);
    }

    override fun toPrettyString(padding: Int): String {
        return "[Assign] $name: ${value.toPrettyString(padding + 2)}".padStart(padding);
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}

class AssignDynamicPropertyToken(val name: StringInterpolateToken, val value: IIslToken, islType: IslType?, position: Position) :
    BaseToken(position, islType, "assignprop") {
    override fun toString(): String {
        return toPrettyString(0);
    }

    override fun toPrettyString(padding: Int): String {
        return "[Assign] $name: ${value.toPrettyString(padding + 2)}".padStart(padding);
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}