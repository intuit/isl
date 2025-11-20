package com.intuit.isl.parser.tokens

import com.fasterxml.jackson.annotation.JsonProperty
import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.types.IslType
import com.intuit.isl.utils.Position

interface IIslToken {
    /**
     * Physical position of the token in a file
     */
    val position: Position;

    /**
     * Token type (used internally for linting)
     */
    val type: String;

    /**
     * ISL Declared type (as pre-detected or defined in the ISL Code by the developer)
     */
    var islType: IslType?;

    fun <T> visit(visitor: IIslTokenVisitor<T>): T;

    fun toPrettyString(padding: Int): String{
        return toString();
    }
}

// No love for this sort of function in K/J :(
//abstract class BaseXToken : IXToken{
//    override fun <T> visit(visitor: IXTokenVisitor<T>): T {
//        return visitor.visit( (dynamic) this);
//    }
//}
