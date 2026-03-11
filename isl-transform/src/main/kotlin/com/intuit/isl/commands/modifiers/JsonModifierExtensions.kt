package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.JsonNode
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

                return try {
                    val firstChar = text.trim().firstOrNull() ?: return null;
                    // Only parse if content looks like JSON (starts with { or [)
                    if (firstChar != '{' && firstChar != '[') return null;

                    // Lenient reader only for json.parse: trailing commas, comments, leading zeros.
                    // Other options: ALLOW_UNQUOTED_FIELD_NAMES, ALLOW_SINGLE_QUOTES,
                    //   ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS, ALLOW_UNESCAPED_CONTROL_CHARS
                    val reader = JsonConvert.mapper.reader()
                        .with(JsonReadFeature.ALLOW_TRAILING_COMMA)
                        .with(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                        .with(JsonReadFeature.ALLOW_YAML_COMMENTS)
                        .with(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
                    reader.readValue(text, JsonNode::class.java)
                } catch (e: Exception) {
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