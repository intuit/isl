package com.intuit.isl.types

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode

class TypedObjectNode : ObjectNode {
    var type: IslType?;

    companion object {
        fun <T : JsonNode> tryTypedObject(islType: IslType?, value: T): JsonNode {
            if (islType == null)
                return value;
            if (value is TypedObjectNode) {
                value.type = islType;
                return value;
            }
            if (value is ObjectNode) {
                // somehow we got back an object but this has a declaration
                // happens when we get results back via ApiCalls for example
                val newResult = TypedJsonNodeFactory.instance.typedObjectNode(islType, value)
                return newResult;
            }
            return value;
        }
    }

    constructor(nc: JsonNodeFactory, type: IslType? = null)
            : super(nc) {
        this.type = type;
    }

    constructor(nc: JsonNodeFactory, type: IslType? = null, other: ObjectNode)
            : super(nc) {
        this.type = type;
        val it = other.fields()

        while (it.hasNext()) {
            val entry = it.next();
            _children.put(entry.key, entry.value)
        }
    }

    override fun deepCopy(): ObjectNode {
        val other = TypedObjectNode(this._nodeFactory, this.type);
        other.setAll<JsonNode>(this)
        return other;
    }

//    constructor(
//        nc: JsonNodeFactory,
//        type: IslType?,
//        kids: Map<String, JsonNode>
//    ) : super(nc, kids) {
//        this.type = type;
//    }
}

class TypedJsonNodeFactory : JsonNodeFactory(true) {
    companion object {
        val instance = TypedJsonNodeFactory();
    }

    override fun objectNode(): ObjectNode {
        return TypedObjectNode(this);
    }

    fun typedObjectNode(type: IslType? = null): ObjectNode {
        return TypedObjectNode(this, type);
    }

    fun typedObjectNode(type: IslType, other: ObjectNode): ObjectNode {
        return TypedObjectNode(this, type, other);
    }
}