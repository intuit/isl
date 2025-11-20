package com.intuit.isl.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.io.CharTypes
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ValueNode
import java.lang.StringBuilder

class ObjectRefNode(val value: IIslReference?) : ValueNode(), IIslIterable {
    override fun equals(other: Any?): Boolean {
        return other == value;
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0;
    }

    override fun serialize(jgen: JsonGenerator, provider: SerializerProvider?) {
        // Can't do this - should we fail?
        jgen.writeString(textVersion())
    }

    override fun asToken(): JsonToken {
        return JsonToken.VALUE_NULL;
    }

    override fun getNodeType(): JsonNodeType {
        return JsonNodeType.NULL;
    }

    fun textVersion(): String{
        return value?.toString()?.replace("\"", "'") ?: "No Ref";
    }

    override fun asText(): String {
        val sb = StringBuilder();
        sb.append("\"");
        CharTypes.appendQuoted(sb, textVersion());
        sb.append("\"");
        return sb.toString();
    }

    override fun getInnerIterator(): Iterable<Any?>? {
        return value as? Iterable<*>;
    }
}


/**
 * Implement this in any class you want to be kept "as is" and not converted to a JSON
 */
interface IIslReference{}

interface IIslIterable{
    fun getInnerIterator(): Iterable<Any?>?;
}
