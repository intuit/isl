package com.intuit.isl.parser.tokens

import com.fasterxml.jackson.databind.ser.Serializers
import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.runtime.TransformCompilationException
import com.intuit.isl.utils.Position


/**
 * Value Selector $variable.property[array | (condition)]
 */
class VariableSelectorValueToken(name: String, position: Position) : BaseToken(position, null, "selector") {
    // make sure variable name always starts with $
    val variableName: String;   // = "\$" + name.substringBefore(".", name).trimStart('$');
    val path: String?;  // = if (name.contains('.')) name.substringAfter(".") else null;

    init {
        val shortName = name.trimStart('$');
        val firstSeparator = shortName.indexOfFirst { it == '.' || it == '[' };  // first separator starts at . or [
        if (firstSeparator >= 0) {
            variableName = "\$" + shortName.substring(0, firstSeparator);
            if (shortName[firstSeparator] == '.')
                path = shortName.substring(firstSeparator + 1);    // exclude the `.`
            else
                path = shortName.substring(firstSeparator);      // include the `[`
        } else {
            variableName = "\$" + shortName;
            path = null;
        }
    }

    override fun toString(): String {
        if (path == null)
            return "[Select] $variableName";
        else
            return "[Select] $variableName -> $path";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}

// first part $var or $var[index] or $var[condition]
class SimpleVariableSelectorValueToken(
    val name: String,
    val indexSelector: Int? = null,
    val conditionSelector: IIslToken? = null,
    position: Position
) : BaseToken(position, null, "selector") {

    override fun toString(): String {
        if (indexSelector != null)
            return "\$$name[$indexSelector]";
        else if (conditionSelector != null)
            return "\$$name[($conditionSelector)]";
        return "\$$name";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}

// second part $var.prop or $var.prop[index] or $var.prop[condition] or $var.["name"]
class SimplePropertySelectorValueToken(
    val name: String,
    val previousToken: IIslToken,
    val indexSelector: Int? = null,
    val conditionSelector: IIslToken? = null,
    position: Position
) : BaseToken(position, null, "selector") {

    override fun toString(): String {
        if (indexSelector != null)
            return "$previousToken.$name[$indexSelector]";
        else if (conditionSelector != null)
            return "$previousToken.$name[($conditionSelector)]";
        return "$previousToken.$name";
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        return visitor.visit(this);
    }
}