package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.types.IslType
import com.intuit.isl.utils.Position

/**
 * $var : ... or $var = ...
 * $var : Type = ...
 * $var : { ... }
 */
class AssignVariableToken (
    variableName: String,           // var from $var
    val topPropertyName: String?,   // $var.property =
    islType: IslType?,
    val value: IIslToken,           // right hand side
    position: Position) : BaseToken(position, islType) {

    val name: String = (if(variableName.startsWith("$")) variableName else "$${variableName}").lowercase();
    override fun toString(): String {
        return "[Var] $name : $value";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}

