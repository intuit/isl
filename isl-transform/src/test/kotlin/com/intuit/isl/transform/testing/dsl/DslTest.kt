package com.intuit.isl.transform.testing.dsl


import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.dsl.array
import com.intuit.isl.dsl.node
import com.intuit.isl.transform.testing.commands.BaseTransformTest
import kotlin.test.Test

class DslTest : BaseTransformTest() {
    @Test
    fun createNode() {
        val existing = array {
            add(1)
        }

        val existingList = listOf(JsonNodeFactory.instance.numberNode(1));


        val otherNode = node {
            put("carry", 1)
        }

        val value = node {
            put("test", "value")
            put("string", "value")
            put("bool", true)
            put("int", 123)

            merge(otherNode.node)

            node("child") {
                put("childValue", 123)
            }

            node("j", JsonNodeFactory.instance.numberNode(2))

            array("list") {
                "1"
                node {
                    put("hi", "there")
                }
            }

            array("elist", existingList)

            array("existing", existing.array)
        }

        println(value)

        compareJsonResults(
            """{"test":"value","string":"value","bool":true,"int":123,"carry":1,"child":{"childValue":123},"j":2,"list":[{"hi":"there"}],"elist":[1],"existing":[1]}""",
            value.node)
    }

    @Test
    fun createArray() {
        val value = array {
            add(1)
            add(true)
            add("value")

            node {
                put("childValue", 123)
            }
        }

        println(value)

        compareJsonResults("""[1,true,"value",{"childValue":123}]""",
            value.node)
    }


    @Test
    fun addToArray() {
        val existing = listOf(1,2,3)

        val value = array {
            addAll(existing)

            addAll(emptyList())

            add(true)
        }

        println(value)

        compareJsonResults("""[1,2,3,true]""",
            value.node)
    }
}