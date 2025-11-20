package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.utils.Position
import java.lang.StringBuilder

class SwitchCaseToken(val value: IIslToken, val cases: Array<SwitchCaseBranchToken>, position: Position): BaseToken(position) {
    override fun toString(): String {
        val sb = StringBuilder("switch ( $value ){\n");

        cases.forEach {
            sb.appendLine( it );
        }

        sb.appendLine("endswitch");
        return sb.toString();
    }
    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}

class SwitchCaseBranchToken(val value: IIslToken, val condition: String, val statements: IIslToken, position: Position): BaseToken(position){
    override fun toString(): String {
        return "$condition $value: $statements";
    }
    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}