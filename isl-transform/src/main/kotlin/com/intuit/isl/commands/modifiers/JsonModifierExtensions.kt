package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import java.lang.Exception

@Suppress("MoveVariableDeclarationIntoWhen")
object JsonModifierExtensions {
    fun registerDefaultExtensions(context: IOperationContext) {
        context.registerExtensionMethod("Modifier.json.*", JsonModifierExtensions::json);
        context.registerExtensionMethod("Modifier.yaml.*", JsonModifierExtensions::yaml);
    }

    private fun json(context: FunctionExecuteContext): Any? {
        val text = ConvertUtils.tryToString(context.firstParameter) ?: "";

        val method = ConvertUtils.tryToString(context.secondParameter) ?: "";

        when (method.lowercase()) {
            "parse" -> {
                if (text.isBlank())
                    return null;

                return try{
                    // cheap test to avoid unnecessary exceptions
                    val firstChar = text[0];

                    // we only parse if first char is whitespace { or [
                    // anything else is not json anyway
                    if(firstChar.isWhitespace() || firstChar == '{' || firstChar == '['  ) {
                        return JsonConvert.mapper.readTree(text)
                    } else {
                        return null;
                    }
                }catch (e: Exception) {
//                    context.executionContext.operationContext.interceptor?.logInfo(
//                        context.command,
//                        context.executionContext
//                    ) { "Exception: ${e.localizedMessage}" }
                    null
                }
            }
        }

        return "Unknown modifier: json.$method";
    }

    private fun yaml(context: FunctionExecuteContext): Any? {
        val text = ConvertUtils.tryToString(context.firstParameter) ?: "";

        val method = ConvertUtils.tryToString(context.secondParameter) ?: "";

        when (method.lowercase()) {
            "parse" -> {
                if (text.isBlank())
                    return null;

                return try {
                    val mapper = ObjectMapper(YAMLFactory())
                    val result = mapper.readTree(text)
                    result
                }catch (e: Exception){
//                    context.executionContext.operationContext.interceptor?.logInfo(
//                        context.command,
//                        context.executionContext
//                    ) { "Exception: ${e.localizedMessage}" }
                    null
                }
            }
        }

        return "Unknown modifier: yaml.$method";
    }
}