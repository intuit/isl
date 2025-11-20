package com.intuit.isl.parser

import com.intuit.isl.parser.tokens.*
import com.intuit.isl.types.IslType
import com.intuit.isl.utils.Position
import org.antlr.v4.runtime.Token

object ParserUtils {
    fun getPosition(token: Token): Position {
        return Position(
            "nofileyet",
            token.line,
            token.charPositionInLine,
            token.line,
            token.charPositionInLine + token.text.length
        );
    }

    // TODO: Figure out how we could handle array [] specifiers $val[0].property[1].something - really there are better ways to do such things
    // it's a bit odd to assign to an array though but it seems to be acceptable in the existing code
    fun generateAssignVariable(fullName: String, value: IIslToken, type: IslType?, position: Position): AssignVariableToken{
        val propTokens = fullName.split('.');
        // Deep assignment $var.prop.prop - what we do here we assign object properties for the depth
        // then we merge the final target
        if (propTokens.size == 1) {
            return AssignVariableToken(cleanPropertyName(fullName), null, type, value, position);
        } else {
            // Fake new tokens and Positions
            var childToken = AssignPropertyToken(cleanPropertyName(propTokens.last()), value, null, position) as IIslToken;

            var root: IIslToken = childToken;
            propTokens
                .asReversed()   // from from the bottom
                .subList(1, propTokens.size)  // skip first (that was the child from above)
                .forEach {
                    val childObject = DeclareObjectToken(
                        StatementsToken(listOf(childToken), position),
                        position
                    ) as IIslToken;
                    root = AssignPropertyToken(it, childObject, null, position);
                    childToken = root;
                };

            // we capture the first property as that's the one that needs to be merged
            val assignVariableToken = AssignVariableToken(propTokens.first(), propTokens[1], type, root, position);
            return assignVariableToken;
        }
    }

    /**
     * Cleans up `["` and `"]` from around a property name so `["a-b-c"]` becomes `a-b-c`
     */
    fun cleanPropertyName(name: String): String{
        if(name.startsWith("[\"") && name.endsWith("\"]"))
            return name.substring(2, name.length-2);
        return name;
    }
}