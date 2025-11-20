package com.intuit.isl.types

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.utils.JsonConvert

// TODO: We want to be able to serialize / deserialize this directly out of a basic Json Schema.
// For now we can serialize to JsonSchema via .toJsonSchema()

@Suppress("unused")
abstract class IslType(
    val type: Type,
    val description: String? = null,
    val title: String? = null,
    val attributes: Map<String, String>? = null
) {
    companion object {
        val Any: IslType = JsonBasicType(Type.ANY);
        val Object: IslType = IslObjectType("");
        val String = JsonBasicType(Type.STRING);
    }

    open val schemaName: String? = null;
    open val schemaUrl: String? = null;

    /**
     * @param value - ISL Type
     * @param schemaType - JSON Schema Equivalent Type
     * @param format - JSON Schema Equivalent Formatting
     */
    enum class Type(val value: String, val schemaType: String? = null, val format: String? = null) {
        STRING("string"),
        NUMBER("number"),
        INTEGER("integer"),
        BOOLEAN("boolean"),
        ARRAY("array"),
        OBJECT("object"),
        BINARY("binary"),

        // Custom JSON Formats
        TEXT("text", "string", "multiLine"),
        DATE("date", "string", "date"),
        DATETIME("datetime", "string", "date-time"),

        // We won't apply any validations to the Any
        ANY("any"),
        NULL("null");

        companion object {
            fun fromText(v: String): Type? {
                return values().firstOrNull { it.value == v };
            }
        }

        val schemaValue
            get() =
                if (this == ANY) OBJECT.value
                else this.schemaType ?: this.value;
    }

    override fun toString(): String {
        return type.value;
    }
}


class JsonBasicType(type: Type, val values: List<Any?>? = null) : IslType(type) {
    override fun toString(): String {
        if (values.isNullOrEmpty())
            return super.toString()
        return "{ type: ${type.value}, values: $values }";
    }
}

class JsonArrayType(val itemType: IslType) : IslType(Type.ARRAY) {
    override fun toString(): String {
        return "$itemType[]";
    }
}

class IslObjectType(
    val name: String,   // Schema Name
    val properties: List<JsonProperty>? = null,
) : IslType(Type.OBJECT) {
    override val schemaName: String?
        get() = name;

    override fun toString(): String {
        return name.ifBlank { Type.OBJECT.value };
    }
}

/**
 * Reference to a URL providing a schema
 */
class JsonReferenceType(
    val url: String,
) : IslType(Type.OBJECT) {
    override val schemaUrl: String?
        get() = url;

    override fun toString(): String {
        return "{ \$ref: $url }";
    }
}

data class JsonProperty(val name: String, val value: IslType) {
    override fun toString(): String {
        return "$name: $value";
    }
}


fun IslType.toJsonSchema(): ObjectNode {
    // how many ways can we skin a visitor of a type?
    val result = JsonNodeFactory.instance.objectNode();

    result
        // TBD: Put this back - but for now it's crashing the DataFlow
        //.put("\$schema", "http://json-schema.org/draft-04/schema#")
        .put("type", "object");

    fun internalToJsonSchema(t: IslType, node: ObjectNode) {
        if (t.title != null)
            node.put("title", t.title);
        if (t.description != null)
            node.put("description", t.description)

        when {
            t is IslObjectType -> {
                val p = JsonNodeFactory.instance.objectNode();
                node.set<JsonNode>("properties", p);
                node.put("type", "object");
                t.properties?.forEach {
                    val child = JsonNodeFactory.instance.objectNode();
                    p.set<JsonNode>(it.name, child);
                    internalToJsonSchema(it.value, child);
                }
            }

            t is JsonArrayType -> {
                node.put("type", t.type.schemaValue);
                val items = JsonNodeFactory.instance.objectNode();
                node.set<JsonNode>("items", items);
                internalToJsonSchema(t.itemType, items);
            }

            t is JsonBasicType -> {
                node.put("type", t.type.schemaValue);
                if (t.type.format != null)
                    node.put("format", t.type.format);
                if (!t.values.isNullOrEmpty()) {
                    val enum = JsonNodeFactory.instance.arrayNode();
                    t.values.forEach { enum.add(JsonConvert.convert(it)) };
                    node.set<JsonNode>("default", enum.get(0));
                    node.set<JsonNode>("enum", enum);
                }
            }
        }

        if (!t.attributes.isNullOrEmpty()) {
            t.attributes.forEach {
                node.put(it.key, it.value);
            }
        }
    }

    internalToJsonSchema(this, result);

    return result;
}