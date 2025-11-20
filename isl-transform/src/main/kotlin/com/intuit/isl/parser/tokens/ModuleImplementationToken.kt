package com.intuit.isl.parser.tokens

import com.intuit.isl.commands.builder.IIslTokenVisitor
import com.intuit.isl.parser.CompilationWarning
import com.intuit.isl.types.IslType
import com.intuit.isl.utils.Position
import java.lang.StringBuilder

/**
 * A module is the set of exported functions from a transform file.
 * Most often the Module has a single function "run" which is backward compatible with the existing .xforms
 */
class ModuleImplementationToken(
    val name: String,
    // this is mutable as we can dynamically add more imports
    val imports: List<ImportDeclarationToken>,
    val types: Map<String, IslType>,
    val functions: List<FunctionDeclarationToken>,
    position: Position,
    val warnings: List<CompilationWarning>? = null
)
    : BaseToken(position, null, "module"){

    companion object{
        final val Empty = ModuleImplementationToken("Empty", listOf(), mapOf(), listOf(), Position( "empty", 0, 0 ));
    }

    override fun toString(): String {
        return toPrettyString(0)
    }

    override fun toPrettyString(padding: Int): String {
        val sb = StringBuilder("module $name\n");

        imports.forEach{
            sb.append("$it\n");
        }
        if(imports.isNotEmpty())
            sb.appendLine();

        types.forEach{
            sb.append("type ${it.key} = ${it.value};\n");
        }
        if(types.isNotEmpty())
            sb.appendLine();

        functions.forEach {
            sb.append("${it.toPrettyString(0)}\n\n");
        }
        return sb.toString();
    }

    override fun <T> visit(visitor: IIslTokenVisitor<T>): T {
        //return visitor.visit(this);
        throw NotImplementedError();
    }
}