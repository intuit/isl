package com.intuit.isl.utils

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BinaryNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intuit.isl.IslSecurityKey
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.types.TypedJsonNodeFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.security.Key
import java.security.KeyStore
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder

object JsonConvert {
    val mapper = ObjectMapper();
    private val mergeMapper: ObjectMapper;

    val factory = JsonNodeFactory(true);


    init {
        mapper
            .registerModule(Jdk8Module())
            .registerModule(
                JavaTimeModule()
                    // Some logical default for dates: 2021-11-30T11:19:53.217Z - most common format - using 3 nanoseconds
                    .addSerializer(Instant::class.java, Iso8601tMillisInstantSerializer())
            )
            .registerKotlinModule()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            // make sure our decimals don't come out as 1e6 scientific notation
            .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
            .nodeFactory = JsonNodeFactory(true);


        mergeMapper = mapper.copy();
        mergeMapper.setDefaultMergeable(true);
    }

    class Iso8601tMillisInstantSerializer : InstantSerializer(
        InstantSerializer.INSTANCE,
        false,
        DateTimeFormatterBuilder().appendInstant(3).toFormatter(),
        JsonFormat.Shape.STRING
    ) {

    }

    /**
     * Handle a conversion and generate an error
     */
    internal fun handleConvert(value: Any?, command: IIslCommand, valueSource: IIslCommand): JsonNode {
        try {
            if (value == null)
                return factory.nullNode();
            return convert(value);
        } catch (e: Throwable) {
            throw TransformException(
                "${command.token} Executing ${valueSource.token} Failed converting Result Type=${value!!.javaClass.canonicalName}: ${e.message}",
                command.token.position,
                e
            )
        }
    }

    /**
     * Convert a value to a JsonNode. 
     * Note that this does NOT parse a text/string into a JSON but just wraps the text in a TextNode.
     * If you want to parse and convert a JSON string into a JsonNode you should use
     * `JsonConvert.mapper.readTree( value )
     */
    fun convert(value: Any?): JsonNode {
        when (value) {
            is JsonNode -> return value;
            is Instant -> return InstantNode(value);
            is String -> return factory.textNode(value);
            is Boolean -> return factory.booleanNode(value);

            is Int -> return factory.numberNode(value);
            is Short -> return factory.numberNode(value);
            is Long -> return factory.numberNode(value);
            is BigInteger -> return factory.numberNode(value);
            is BigDecimal -> return factory.numberNode(value);

            is Float -> return factory.numberNode(value);
            is Double -> return factory.numberNode(value);

            null -> return factory.nullNode();

            is IIslReference -> return ObjectRefNode(value);
            is KeyStore -> return ObjectRefNode(IslSecurityKeyStore(value))
            is Key -> return ObjectRefNode(IslSecurityKey(value))
            else ->
                return mapper.valueToTree(value);
        }
    }

    /**
     * Return native value
     */
    fun getValue(node: JsonNode): Any? {
        if (node.isValueNode) {
            when (node) {
                is NumericNode -> return node.numberValue();
                is TextNode -> return node.textValue();
                is BooleanNode -> return node.booleanValue();
                is ObjectRefNode -> return node.value;
            }
        }
        return null;
    }

    fun replaceNullNode(value: Any?): Any? {
        if (value is NullNode || value is MissingNode)
            return null;
        return value;
    }

//    fun merge(original: JsonNode, newNode: JsonNode): JsonNode {
//        val reader = mergeMapper.readerForUpdating(original);
//        val result = reader.readValue<JsonNode>(newNode);
//        return result;
//    }

    // CT: This custom merge was introduced to avoid the ConcurrentModificationException reported when merging objects with arrays
    fun merge(original: JsonNode, newNode: JsonNode): JsonNode {
        if (newNode is ObjectNode) {
            val result = original as? ObjectNode ?: TypedJsonNodeFactory.instance.typedObjectNode();

            newNode.fields().forEach {
                // we don't want to override - we want to merge
                var target = result[it.key];

                // self assign generally $a.list = $a.list | pushItems( $b ) but the pushItems already modified the internal array
                if (target == it.value)
                    return@forEach;

                if (target == null)
                    result.set<JsonNode>(it.key, it.value);
                else if (target is ObjectNode && it.value is ObjectNode) {
                    merge(target, it.value)
                } else if (target is ArrayNode && it.value is ArrayNode) {
                    target.addAll(it.value as ArrayNode);
                } else {
                    result.set<JsonNode>(it.key, it.value);
                }
            }
            return result;
        } else if (original is ArrayNode && newNode is ArrayNode) {
            // we'll append
            (original).addAll(newNode);
            return original;
        } else
            return newNode;
    }

    fun length(value: Any?): Int {
        return when (value) {
            null -> 0;
            is String -> value.length;
            is Array<*> -> value.size;
            is ArrayNode -> value.size();
            is List<*> -> value.size;
            is Map<*, *> -> value.size;
            is TextNode -> value.asText().length;
            is ObjectRefNode -> length(value.value);
            is ByteArray -> value.size;
            is BinaryNode -> value.binaryValue().size;
            else -> 0;
        }
    }
}