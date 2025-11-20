package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.types.JsonProperty
import com.intuit.isl.types.IslType
import com.intuit.isl.utils.Position

enum class FunctionType {
    Function,
    Modifier,
}

class FunctionDeclarationToken(
    val functionType: FunctionType,
    val functionName: String,
    val annotations: List<AnnotationDeclarationToken>,
    val arguments: List<JsonProperty>,
    val statements: IIslToken,
    islType: IslType,
    position: Position
) : BaseToken(position, islType, "func") {
    override fun toString(): String {
        return toPrettyString(0);
    }

    override fun toPrettyString(padding: Int): String {
        val sb = StringBuilder();
        annotations.forEach { sb.appendLine(it.toString()) };
        sb.appendLine(
            "[$functionType] $functionName ($arguments) : $islType {\n" +
                    "${statements.toPrettyString(padding + 2)}\n" +
                    "}\n"
        );
        return sb.toString();
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}