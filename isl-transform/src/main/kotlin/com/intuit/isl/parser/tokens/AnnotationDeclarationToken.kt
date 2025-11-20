package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.utils.Position
import java.lang.StringBuilder

class AnnotationDeclarationToken(
    val annotationName: String,
    val arguments: List<IIslToken>,
    position: Position
) : BaseToken(position) {
    override fun toString(): String {
        return toPrettyString(0);
    }

    override fun toPrettyString(padding: Int): String {
        val sb = StringBuilder("@$annotationName");

        if( this.arguments.isNotEmpty() ){
            sb.append("( ");
            this.arguments.forEachIndexed { index, it ->
                sb.append(it);

                if (index < this.arguments.size - 1)
                    sb.append(", ")
            }
            sb.append(" )");
        }

        return sb.toString();
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}