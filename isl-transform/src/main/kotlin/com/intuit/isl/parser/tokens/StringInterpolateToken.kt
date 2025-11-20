package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.types.IslType
import com.intuit.isl.utils.Position
import java.lang.StringBuilder

class StringInterpolateToken(tokens: ArrayList<IIslToken>, override val position: Position) : ArrayList<IIslToken>(tokens), IIslToken {
    override val type: String = "interpolate";

    override var islType: IslType? = IslType.String;

    override fun toString(): String {
        var sb = StringBuilder();
        this.forEach {
            sb.append("[$it]")
        }
        return sb.toString();
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}