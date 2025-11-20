package com.intuit.isl.dsl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import com.intuit.isl.utils.JsonConvert

// CT: Sometimes we just want a nice clean way of creating Json Objects
// Because JsonNodeFactory.instance.objectNode() is just a mouth-full - or a keyboard-full

// https://kotlinlang.org/docs/type-safe-builders.html
// https://www.baeldung.com/kotlin/dsl

@DslMarker
annotation class JsonMarker

@JsonMarker
abstract class BaseElement(open val node: JsonNode) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseElement

        return node == other.node
    }

    override fun hashCode(): Int {
        return node.hashCode()
    }

    override fun toString(): String = JsonConvert.mapper.writeValueAsString(node)
}

open class DslObjectNode(override val node: ObjectNode = JsonNodeFactory.instance.objectNode()) : BaseElement(node) {

    protected val innerNode
        get() = node

//  This doesn't work because Java's generics are stuck in 1998 - I miss you C# :(((((
//    fun put(pair: Pair<String, String?>): DslObjectNode{
//        node.put(pair.first, pair.second)
//        return this;
//    }
//    fun put(pair: Pair<String, Boolean>): DslObjectNode{
//        node.put(pair.first, pair.second)
//        return this;
//    }
    fun put(name: String, value: String?): DslObjectNode {
        node.put(name, value)
        return this;
    }

    fun put(name: String, value: Boolean): DslObjectNode {
        node.put(name, value)
        return this;
    }

    //    fun put(name: String, value: Int?): DslObjectNode {
//        node.put(name, value)
//        return this;
//    }
    fun put(name: String, value: Long?): DslObjectNode {
        node.put(name, value)
        return this;
    }
//    fun put(name: String, value: Int): DslObjectNode {
//        node.put(name, value)
//        return this;
//    }

    fun node(key: String, value: DslObjectNode.() -> Unit): DslObjectNode {
        val realNode = DslObjectNode().apply(value).node;
        node.set<ObjectNode>(key, realNode)
        return this;
    }

    fun node(key: String, value: JsonNode): DslObjectNode {
        node.set<ObjectNode>(key, value)
        return this;
    }
    fun merge(value: ObjectNode): DslObjectNode {
        node.setAll<ObjectNode>(value)
        return this;
    }
    fun array(key: String, value: DslArrayNode.() -> Unit): DslObjectNode {
        val realArray = DslArrayNode().apply(value).node;
        node.set<ArrayNode>(key, realArray)
        return this;
    }
    fun array(key: String, array: ArrayNode): DslObjectNode {
        node.set<ArrayNode>(key, array)
        return this;
    }
    fun array(key: String, array: List<JsonNode>): DslObjectNode {
        val arrayNode = JsonNodeFactory.instance.arrayNode(array.size)
        arrayNode.addAll(array)
        node.set<ArrayNode>(key, arrayNode)
        return this;
    }
}

class DslArrayNode(val array: ArrayNode = JsonNodeFactory.instance.arrayNode()) : BaseElement(array) {
    fun add(value: String): DslArrayNode {
        array.add(value)
        return this;
    }

    fun add(value: Boolean): DslArrayNode {
        array.add(value)
        return this;
    }

    fun add(value: Int): DslArrayNode {
        array.add(value)
        return this;
    }

    fun addAll(list: List<Any?>?): DslArrayNode {
        if (list.isNullOrEmpty())
            return this;
        list.forEach {
            when (it) {
                is DslObjectNode -> array.add(it.node)
                // I feel we should give an error here
                // I don't see a value where we'd ever add an array to one array
                is DslArrayNode -> array.add(it.array)
                is JsonNode -> array.add(it)
                else -> array.add(JsonConvert.convert(it))
            }
        }
        return this;
    }

    fun node(value: DslObjectNode.() -> Unit): DslArrayNode {
        val realNode = DslObjectNode().apply(value).node;
        array.add(realNode)
        return this;
    }
}

// we can't use object :(
fun node(init: DslObjectNode.() -> Unit): DslObjectNode {
    return DslObjectNode().apply(init);
}

fun array(init: DslArrayNode.() -> Unit): DslArrayNode {
    return DslArrayNode().apply(init);
}

