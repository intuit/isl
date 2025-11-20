package com.intuit.isl.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ValueNode
import java.time.Instant

/**
 * Specialized Date node to keep track of Instant.
 * By default, Jackson converts Instants to strings which makes life hard for us.
 */
class InstantNode(var value: Instant = Instant.now()) : ValueNode() {
    override fun equals(other: Any?): Boolean {
        return when(other){
            is InstantNode -> other.value == this.value;
            else -> false;
        }
    }

    override fun hashCode(): Int {
        return value.hashCode();
    }

    override fun serialize(jgen: JsonGenerator, provider: SerializerProvider?) {
        val stringValue = ConvertUtils.IsoDateTimeFormatter.format(value);
        jgen.writeString(stringValue);
    }

    override fun asToken(): JsonToken {
        return JsonToken.VALUE_STRING;
    }

    override fun getNodeType(): JsonNodeType {
        return JsonNodeType.STRING;
    }

    override fun asText(): String {
        // CT: I have a feeling this is not compatible with the new toPrettyPrint in newer versions of jackson!
        return "\"${ConvertUtils.IsoDateTimeFormatter.format(value)}\"";
    }

    override fun toString(): String {
        return ConvertUtils.IsoDateTimeFormatter.format(value);
    }
}