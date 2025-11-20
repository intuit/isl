package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.utils.Position
import java.lang.StringBuilder

class FunctionCallToken(
    val name: String,
    val arguments: List<IIslToken>,
    val statements: IIslToken?,
    position: Position
) : BaseToken(position, null, "call") {
    override fun toString(): String {
        val sb = StringBuilder("@.$name( ");
        if (this.arguments.isNotEmpty()) {
            this.arguments.forEachIndexed { index, it ->
                sb.append(it);

                if (index < this.arguments.size - 1)
                    sb.append(", ")
            }
        }
        sb.append(" )");

        if (statements != null) {
            sb.append("{\n");
            sb.append("  $statements\n");
            sb.append("}\n");
        }

        return sb.toString();
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}