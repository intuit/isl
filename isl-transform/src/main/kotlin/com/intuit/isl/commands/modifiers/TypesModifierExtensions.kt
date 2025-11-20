package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.node.*
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.types.IslType
import com.intuit.isl.types.TypedObjectNode
import com.intuit.isl.utils.InstantNode

@Suppress("MoveVariableDeclarationIntoWhen")
object TypesModifierExtensions {
    fun registerDefaultExtensions(context: IOperationContext) {
        context.registerExtensionMethod("Modifier.typeof", TypesModifierExtensions::typeofInfo);
    }

    private fun typeofInfo(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter;
        println("Checking type of ${first}")
        when (first) {
            is TypedObjectNode -> return first.type?.toString() ?: IslType.Type.OBJECT.value;
            is POJONode -> return IslType.Type.OBJECT.value;
            is ObjectNode -> return IslType.Type.OBJECT.value;

            is ArrayNode -> return IslType.Type.ARRAY.value;

            is TextNode -> return IslType.Type.STRING.value;
            is String -> return IslType.Type.STRING.value;

            is BooleanNode -> return IslType.Type.BOOLEAN.value;
            is Boolean -> return IslType.Type.BOOLEAN.value;

            is BigIntegerNode -> return IslType.Type.INTEGER.value;
            is IntNode -> return IslType.Type.INTEGER.value;
            is ShortNode -> return IslType.Type.INTEGER.value;
            is LongNode -> return IslType.Type.INTEGER.value;

            is DecimalNode -> return IslType.Type.NUMBER.value;
            is DoubleNode -> return IslType.Type.NUMBER.value;
            is FloatNode -> return IslType.Type.NUMBER.value;

            is NumericNode -> return IslType.Type.NUMBER.value;

            is BinaryNode -> return IslType.Type.BINARY.value;

            is InstantNode -> return IslType.Type.DATETIME.value;


            is MissingNode -> return IslType.Type.NULL.value;
            is NullNode -> return IslType.Type.NULL.value;
            null -> return IslType.Type.NULL.value;

            else -> return IslType.Type.ANY.value
        }
    }
}