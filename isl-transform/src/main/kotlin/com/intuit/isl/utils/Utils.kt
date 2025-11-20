package com.intuit.isl.utils

import com.intuit.isl.parser.tokens.IIslToken
import com.intuit.isl.runtime.TransformCompilationException
import java.lang.annotation.ElementType
import java.lang.annotation.RetentionPolicy
import java.math.BigDecimal


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class ExcludeFromJacocoGeneratedReport;


internal object Const{
    const val FallbackMethodName = "*";
}

internal fun String.removeQuotes(): String {
    return this.substring(1, this.length - 1);
}
internal fun String.fixString(): String {
    return this.removeQuotes()
        // yes, we can make this a regex
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t");
}

internal fun <T> Array<T>.indexOrDefault(index: Int): T? {
    return if (index >= 0 && index < this.size)
        this[index]
    else
        null
}

internal fun <T> Array<T?>.containsAll(items: Array<T>): Boolean {
    for (item in items) {
        if (!this.contains(item)) {
            return false
        }
    }
    return true
}

internal fun BigDecimal?.getValueOrDefault(default: BigDecimal = BigDecimal.ZERO): BigDecimal {
    return this ?: default;
}

internal fun <E> List<E>.justOne(token: IIslToken, error: String): E {
    // we try to correct some grammar here - to make sure we only have a maximum of one value in some statements
    if (this.isEmpty())
        throw TransformCompilationException(
            "Statement ${token.type} has ${this.size} '${error}'. One statement should exist.",
            token.position
        );
    if (this.size == 1)
        return this[0];
    throw TransformCompilationException(
        "Statement ${token.type} has ${this.size} '${error}'. Only one such statement should exist.",
        token.position
    );
}

/**
 * Parses a simple JSONPath expression and returns an array of property names.
 * 
 * Returns null if the path contains:
 * - Array accessors (e.g., [0], [*])
 * - Wildcards (e.g., *, ..)
 * - Filters (e.g., [?(@.price > 10)])
 * - Functions (e.g., length(), min())
 * - Any other complex JSONPath features
 * 
 * Valid examples:
 * - "$.foo" -> ["foo"]
 * - "$.foo.bar" -> ["foo", "bar"]
 * - "$.current_subtotal_price_set.shop_money.amount" -> ["current_subtotal_price_set", "shop_money", "amount"]
 * - "$['foo']['bar']" -> ["foo", "bar"]
 * 
 * Invalid examples (returns null):
 * - "$.foo[0]" (array accessor)
 * - "$.foo.*" (wildcard)
 * - "$.foo..bar" (recursive descent)
 * - "$.foo[?(@.price > 10)]" (filter)
 * 
 * @param jsonPath The JSONPath string to parse
 * @return Array of property names if path is simple, null otherwise
 */
internal fun parseSimpleJsonPath(jsonPath: String): Array<String>? {
    if (jsonPath.isBlank()) return null
    
    val trimmed = jsonPath.trim()
    
    // Must start with $ or @
    if (!trimmed.startsWith("$") && !trimmed.startsWith("@")) {
        return null
    }
    
    // Check for complex features that make it non-simple
    if (trimmed.contains("[") && !trimmed.matches(Regex("^[$@](\\['[^']+'])+$"))) {
        // Contains [ but not in the bracket notation format ['property']
        return null
    }
    
    // Check for wildcards
    if (trimmed.contains("*")) return null
    
    // Check for recursive descent
    if (trimmed.contains("..")) return null
    
    // Check for filter expressions (but allow @ at the start)
    if (trimmed.contains("?(")) return null
    if (trimmed.contains("@.") && !trimmed.startsWith("@.")) return null
    
    // Check for functions
    if (trimmed.contains("()")) return null
    
    // Check for array slice notation
    if (trimmed.contains(":")) return null
    
    // Parse the path
    return try {
        when {
            // Bracket notation: $['foo']['bar'] or @['foo']['bar']
            trimmed.matches(Regex("^[$@](\\['[^']+'])+$")) -> {
                val pattern = Regex("\\['([^']+)']")
                pattern.findAll(trimmed)
                    .map { it.groupValues[1] }
                    .toList()
                    .toTypedArray()
            }
            
            // Dot notation: $.foo.bar or @.foo.bar
            trimmed.matches(Regex("^[$@](\\.[a-zA-Z_][a-zA-Z0-9_]*)+$")) -> {
                trimmed.substring(1) // Remove $ or @
                    .split(".")
                    .filter { it.isNotEmpty() }
                    .toTypedArray()
            }
            
            // Mixed notation: $.foo['bar'].baz
            // For simplicity, we'll reject mixed notation as it's more complex to parse reliably
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}