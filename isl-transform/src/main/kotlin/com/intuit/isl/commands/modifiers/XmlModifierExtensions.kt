package com.intuit.isl.commands.modifiers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.dataformat.xml.ser.XmlSerializerProvider
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.utils.ConvertUtils
import java.util.*

object XmlModifierExtensions {
    private val mapper: XmlMapper = initMapper();

    private fun initMapper(): XmlMapper {
        return XmlMapper.builder()
            .nameForTextElement("#text")
            .defaultUseWrapper(false)
            .addModule(
                SimpleModule().addDeserializer(
                    JsonNode::class.java,
                    DuplicateToArrayJsonNodeDeserializer()
                )
            )
            .addModule(
                SimpleModule().addSerializer(
                    ObjectNode::class.java,
                    AttributeBasedObjectNodeSerializer()
                )
            )
            .build();
    }

    fun registerDefaultExtensions(context: IOperationContext) {
        context.registerExtensionMethod("Modifier.xml.*", XmlModifierExtensions::xml);
    }

    private fun xml(context: FunctionExecuteContext): Any? {
        val text = ConvertUtils.tryToString(context.firstParameter) ?: "";

        val method = ConvertUtils.tryToString(context.secondParameter) ?: "";

        when (method.lowercase()) {
            "parse" -> {
                if (text.isBlank())
                    return null;

                return try {
                    val result = mapper.readTree(text);
                    result
                } catch (e: Exception) {
//                    context.executionContext.operationContext.interceptor?.logInfo(
//                        context.command,
//                        context.executionContext
//                    ) { "Exception: ${e.localizedMessage}" }
                    null
                }
            }
        }

        return "Unknown modifier: xml.$method";
    }

    // Based on https://stackoverflow.com/questions/39493173/how-to-convert-xml-to-json-using-only-jackson/53296833#53296833
    class DuplicateToArrayJsonNodeDeserializer : JsonNodeDeserializer() {
        override fun _handleDuplicateField(
            p: JsonParser?, ctxt: DeserializationContext?,
            nodeFactory: JsonNodeFactory, fieldName: String?, objectNode: ObjectNode,
            oldValue: JsonNode?, newValue: JsonNode?
        ) {
            val node: ArrayNode
            if (oldValue is ArrayNode) {
                node = oldValue
                node.add(newValue)
            } else {
                node = nodeFactory.arrayNode()
                node.add(oldValue)
                node.add(newValue)
            }
            objectNode.set<JsonNode>(fieldName, node);
        }
    }

    fun toXml(first: Any?, root: String): String {
        return mapper
            .writer()
            .withRootName(root)
            .writeValueAsString(first);
    }

    class AttributeBasedObjectNodeSerializer : JsonSerializer<ObjectNode>() {
        override fun serialize(value: ObjectNode, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeStartObject(this);

            val generator = gen as ToXmlGenerator;
            val xmlSerializer = serializers as XmlSerializerProvider;

            val attributes = LinkedList<Map.Entry<String, JsonNode>>();
            val children = LinkedList<Map.Entry<String, JsonNode>>();
            var textNode: TextNode? = null;
            for (entry in value.fields()) {
                if (entry.key.startsWith("@"))
                    attributes.add(entry);
                else if (entry.key == "#text")
                    textNode = entry.value as TextNode;
                else
                    children.add(entry);
            }

            generator.setNextIsAttribute(true);
            for (entry in attributes) {
                gen.writeFieldName(entry.key.substring(1));
                xmlSerializer.serializeValue(gen, entry.value)
            }
            generator.setNextIsAttribute(false);

            if (textNode != null) {
                gen.setNextIsUnwrapped(true);
                gen.writeRaw(textNode.asText());
                gen.setNextIsUnwrapped(false);
            } else {
                for (entry in children) {
                    gen.writeFieldName(entry.key);
                    xmlSerializer.serializeValue(gen, entry.value)
                }
            }
            gen.writeEndObject();
        }
    }
}