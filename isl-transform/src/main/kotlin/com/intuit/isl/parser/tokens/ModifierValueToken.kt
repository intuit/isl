package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.utils.Position
import java.lang.StringBuilder

// TBD: We might need some parameters
// $value | bla1| bla2 creates a reverse tree
// Modifier( bla2, Modifier ( bla1, Selector ($value) ) )
open class ModifierValueToken(
    val name: String,
    val previousToken: IIslToken,
    val arguments: List<IIslToken>,
    position: Position
) : BaseToken(position, null, "call") {
    override fun toString(): String {
        val sb = StringBuilder();
        if (this.arguments.isNotEmpty()) {
            this.arguments.forEach {
                sb.append(it);
                sb.append(", ")
            }
        }
        return "$previousToken | [M:]$name(${sb.removeSuffix(", ")})";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}

// Special modifier `| filter ( condition )`
class FilterModifierValueToken(previousToken: IIslToken, val condition: IIslToken, position: Position) :
    ModifierValueToken("filter", previousToken, arrayListOf(), position) {
    override fun toString(): String {
        return "$previousToken | $name( $condition )";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}

// Special modifier `| map ( operation )`
class MapModifierValueToken(previousToken: IIslToken, val argument: IIslToken, position: Position) :
    ModifierValueToken("map", previousToken, arrayListOf(), position) {
    override fun toString(): String {
        return "$previousToken | $name( $argument )";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}

// Special modifier `| if ( condition ) result`
class ConditionModifierValueToken(
    previousToken: IIslToken,
    val condition: IIslToken,
    val trueModifier: IIslToken,
    position: Position
) : ModifierValueToken("if", previousToken, arrayListOf(), position) {
    override fun toString(): String {
        return "$previousToken | $name( $condition ) $trueModifier";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}

/**
 * Any modifier that has the format | name ( condition, arguments ) like first, last, all, where, retry
 * WARNING WARNING: GenericConditionModifier CAN pickup also standard modifiers due to overlap of the grammar
 * | dostuff ( $value ) can be both a condition modifier and a standard modifier
 * if we detect that we'll create the token with both expression and argument and let the command decide later
 * The `firstArgument` is the potential first parameter that could be a condition
 */
class GenericConditionalModifierValueToken(
    name: String,
    previousToken: IIslToken,

    val condition: IIslToken,
    val firstArgument: IIslToken?,

    arguments: List<IIslToken>,
    position: Position
) : ModifierValueToken(name, previousToken, arguments, position) {
    override fun toString(): String {
        if (firstArgument != null) {
            // we'll assume all are arguments
            if (arguments.isEmpty())
                return "$previousToken | [M:]$name( $firstArgument )";
            return "$previousToken | [M:]$name( $firstArgument, $arguments )";
        }
        if (arguments.isEmpty())
            return "$previousToken | [CM:]$name( $condition )";
        return "$previousToken | [CM:]$name( $condition, $arguments )";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}
