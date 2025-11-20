package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.utils.Position

class ConditionToken(
    val expression: IIslToken,
    val trueResult: IIslToken,
    val falseResult: IIslToken?,
    position: Position
) : BaseToken(position, null, "if") {
    override fun toString(): String {
        return "[if] $expression\n" +
                "{\n" +
                "  $trueResult\n" +
                "}\n" +
                if (falseResult != null) "else\n" +
                        "{\n" +
                        "  $falseResult\n" +
                        "}\n"
                else "";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}

// Condition is and, or
class ConditionExpressionToken(val left: IIslToken, val condition: String, val right: IIslToken?, position: Position) :
    BaseToken(position, null, "if") {
    override fun toString(): String {
        if (right == null)
            return "$condition $left";
        return "( $left $condition $right )";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}

// condition is exists, not exists, ==, !=, ....
class SimpleConditionToken(val left: IIslToken, val condition: String, val right: IIslToken?, position: Position) :
    BaseToken(position, null, "if") {
    override fun toString(): String {
        if (right == null)
            return "$condition $left";
        return "( $left $condition $right )";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}

// We can solve the Coalesce with a simple condition but that will evaluate the left twice once for the condition and
// and once for the result
class CoalesceToken(val left: IIslToken, val right: IIslToken, position: Position) :
    BaseToken(position, null, "coalesce") {
    override fun toString(): String {
        return "( $left ?? $right )";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}