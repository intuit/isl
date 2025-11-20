package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.utils.ConvertUtils
import java.util.stream.Stream
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@Suppress("unused")
class ModifiersTest : BaseTransformTest() {
    companion object {
        @JvmStatic
        fun arrayModifiers(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "range: @.Array.Range( 0, 5 )",
                    """{ "range": [0, 1, 2, 3, 4] }""",
                    null
                ),
                Arguments.of(
                    "range: @.Array.Range( 10, 3 )",
                    """{ "range": [ 10, 11, 12 ] }""",
                    null
                ),
                Arguments.of(
                    "range: @.Array.Range( 10, 5, 2 )",
                    """{ "range": [ 10, 12, 14, 16, 18 ] }""",
                    null
                ),

                // slice
                Arguments.of(
                    "range: [ 9, 8, 7, 6 ] | slice(0, 2)",
                    """{ "range": [ 9, 8 ] }""",
                    null
                ),
                Arguments.of(
                    "range: [ 9, 8, 7, 6 ] | slice(1, 2)",
                    """{ "range": [ 8 ] }""",
                    null
                ),
                Arguments.of(
                    "range: [ 9, 8, 7, 6 ] | slice(2, 2)",
                    """{ "range": [ ] }""",
                    null
                ),
                Arguments.of(
                    "range: [ 9, 8, 7, 6 ] | slice(0, 4)",
                    """{ "range": [9, 8, 7, 6] }""",
                    null
                ),
                Arguments.of(
                    "range: [ 9, 8, 7, 6 ] | slice(0, 5)",
                    """{ "range": [9, 8, 7, 6] }""",
                    null
                ),
                // slice - negative offsets count from the end of the array
                Arguments.of(
                    "range: [ 9, 8, 7, 6 ] | slice(-3, -1)",
                    """{ "range": [ 8, 7 ] }""",
                    null
                ),
                Arguments.of(
                    "range: [ 9, 8, 7, 6 ] | slice(-30, -10)",
                    """{ "range": [] }""",
                    null
                ),
                // slice - Also handle List as returned by some utilities and not just JsonArray
                Arguments.of(
                    "range: [ 8, 8, 7, 6 ] | unique | slice(0, 2)",
                    """{ "range": [ 8, 7 ] }""",
                    null
                ),
                // slice - Handles other data types
                Arguments.of(
                    "range: [ \"9\", \"8\", \"7\", \"6\" ] | slice(0, 2)",
                    """{ "range": [ "9", "8" ] }""",
                    null
                ),
                Arguments.of(
                    "range: [ { \"a\": true }, { \"b\": true },{ \"c\": false } ] | slice(0, 2)",
                    """{ "range": [ { "a": true }, { "b": true } ] }""",
                    null
                ),

                // filters
                Arguments.of(
                    "range: [ 1, 2, 3, 4 ] | filter ( \$fit > 2 )",
                    """{ "range": [ 3, 4 ] }""",
                    null
                ),
                Arguments.of(
                    "range: [ 1, 2, 3, 4 ] | filter ( \$fit < 2 )",
                    """{ "range": [ 1 ] }""",
                    null
                ),

                // gzip/gunzip - these tests give different results in different versions of
                // Java
                // Arguments.of("result: [ 1, 2, 3, 4 ] | gzip", """{ "result":
                // "H4sIAAAAAAAA/4s21DHSMdYxiQUA2JnOZwkAAAA=" }""", null),
                // Arguments.of("result: [ 1, 2, 3, 4 ] | gzip(\"UTF-16\")", """{ "result":
                // "H4sIAAAAAAAA//v3nyGawZBBh8EIiI2B2IQhFgBX2poPFAAAAA==" }""", null),

                Arguments.of(
                    "result: [ 1, 2, 3, 4 ] | gzip | gunzip",
                    """{ "result": "[1,2,3,4]" }""",
                    null
                ),
                Arguments.of(
                    "result: [ 1, 2, 3, 4 ] | gzip(\"UTF-16\") | gunzip(\"UTF-16\")",
                    """{ "result": "[1,2,3,4]" }""",
                    null
                ),
                Arguments.of(
                    "result: \"1f8b08001609e7640003732b4acc3bbc3c3fb31800771e7afa09000000\" | hex.tobinary | gunzip",
                    """{ "result": "François" }""",
                    null
                ),
                //                Arguments.of("result:
                // \"1f8b08001609e7640003732b4acc3bbc3c3fb31800771e7afa09000000\" | hex.tobinary
                // | gunzipToByte | to.string()", """{ "result": "François" }""", null),
                //                Arguments.of("result:
                // \"1f8b0800a369016502fffbff2f9ac1904187c108888d81d88421960100206bc0d614000000\" | hex.tobinary | gunzipToByte | to.string(\"UTF-16\")", """{ "result": "[1,2,3,4]" }""", null),

                Arguments.of(
                    "\$a: { \"body\" : [ 1, 2, 3, 4 ] };\n" +
                            "\$o: {\n" +
                            "    items: foreach \$i in \$a.body {\n" +
                            "        amount: {{ \$i * 10 }}" +
                            "    }\n" +
                            "    endfor\n" +
                            "};\n" +
                            "range: [ 1, 2, 3, 4 ] | filter ( \$fit < 2 );\n" +
                            "items: foreach \$i in \$o.items | filter( \$fit.amount > 20 ) \$i\nendfor;",
                    """{"range":[ 1 ], "items":[{"amount":30},{"amount":40}]} }""",
                    null
                ),
                Arguments.of(
                    "\$a: { \"body\" : [ 1, 2, 3, 4 ] };\n" +
                            "\$o: {\n" +
                            "    items: foreach \$i in \$a.body {\n" +
                            "        amount: {{ \$i * 10 }}" +
                            "    }\n" +
                            "    endfor\n" +
                            "};\n" + // use the new $
                            "range: [ 1, 2, 3, 4 ] | filter ( \$ < 2 );\n" +
                            "items: foreach \$i in \$o.items | filter( \$fit.amount > 20 ) \$i\nendfor;",
                    """{"range":[ 1 ], "items":[{"amount":30},{"amount":40}]} }""",
                    null
                ),
                Arguments.of(
                    "\$a: { a: 1, b: null, c: 2  };\n" +
                            "result: \$a | kv | filter( $.value );",
                    """{"result":[{"key":"a","value":1},{"key":"c","value":2}]}""",
                    null
                ),
                Arguments.of(
                    "\$a: { a: 1, b: null, c: 2  };\n" +
                            "result: \$a | kv | filter( $.value ) | to.object;",
                    """{"result":{"a":1,"c":2}}""",
                    null
                ),

                // reduce
                Arguments.of(
                    "total: [ 1, 2, 3, 4 ] |reduce( {{ \$acc + \$it }} )",
                    """{ "total": 10 }""",
                    null
                ),
                Arguments.of(
                    "totalWithGst: [ 1, 2, 3, 4 ] |reduce( {{ \$acc + \$it * 1.10 }} )",
                    """{ "totalWithGst": 11.0 }""",
                    null
                ),
                Arguments.of(
                    "total: [ { amount : 3.1 }, { amount : 4.5 }, { amount: 3 }, { }, { amount: \"abc\" }, { amount: true } ] |reduce( {{ \$acc + \$it.amount * 1.23 }} )",
                    """{ "total": 13.038 }""",
                    null
                ),

                // Map
                // reduce
                Arguments.of(
                    "total: [ 1, 2, 3, 4 ] | map( { id: $ } )",
                    """{"total":[{"id":1},{"id":2},{"id":3},{"id":4}]}""",
                    null
                ),
                Arguments.of(
                    "total: [ 1, 2, 3, 4 ] | filter ( $ > 2 ) | map( { id: $ } )",
                    """{"total":[{"id":3},{"id":4}]}""",
                    null
                ),
                Arguments.of(
                    "total: [ { amount : 3.1 }, { amount : 4.5 }, { amount: 3 }, { }, { amount: \"abc\" }, { amount: true } ] | map ( $ )",
                    """{"total":[{"amount":3.1},{"amount":4.5},{"amount":3},{},{"amount":"abc"},{"amount":true}]}""",
                    null
                ),
                Arguments.of(
                    "total: [ { amount : 3.1 }, { amount : 4.5 }, { amount: 3 }, { }, { amount: \"abc\" }, { amount: true } ] | map ( $.amount )",
                    """{"total":[3.1,4.5,3,null,"abc",true]}""",
                    null
                ),

                // Let's try something more complicated
                Arguments.of(
                    "\$text: 'Accept: application/json\nContent-Type: application/xml\nMissing:';\n" +
                            "headers: \$text | split( '\n' ) | map( { \n" +
                            "   key: $ | split(':') | at (0) | trim,  \n" +
                            "   value: \$ | split(':') | at (1) | trim,  \n" +
                            "}) | to.object;\n",
                    """{"headers":{"Accept":"application/json","Content-Type":"application/xml","Missing":""}}""",
                    null
                ),
                Arguments.of(
                    "\$text: '';\n" +
                            "headers: \$text | split( '\n' ) | map( { \n" +
                            "   key: $ | split(':') | at (0) | trim,  \n" +
                            "   value: \$ | split(':') | at (1) | trim,  \n" +
                            "}) | to.object;\n",
                    """{"headers":{ }}""",
                    null
                ),

                // unique
                Arguments.of(
                    "unique: @.Array.unique( 2, 3, 3, 4, 5, 6, 2, 3 )",
                    """{ "unique": [ 2, 3, 4, 5, 6 ] }""",
                    null
                ),
                Arguments.of(
                    "\$v:[2, 3, 3, 4, 5, 6, 2, 3]; result: @.Array.unique(\$v)",
                    """{ "result": [ 2, 3, 4, 5, 6 ] }"""",
                    null
                ),
                Arguments.of(
                    "unique: @.Array.unique( \"abc\", \"xyz\", \"abc\", \"xyz\", \"hello\", \"foo\", \"hello\", \"abc\" )",
                    """{ "unique": [ "abc", "xyz", "hello", "foo" ] }""",
                    null
                ),
                Arguments.of(
                    "\$v:[\"abc\", \"xyz\", \"abc\", \"xyz\", \"hello\", \"bar\", \"hello\", \"abc\"]; result: @.Array.unique(\$v)",
                    """{ "result": [ "abc", "xyz", "hello", "bar" ] }"""",
                    null
                ),

                // push & pushItems
                Arguments.of(
                    "items: [] | push( 1 ) | push ( 2 ) | push ( \"abc\" );",
                    """{"items":[1,2,"abc"]}""",
                    null
                ),
                Arguments.of(
                    "\$i: [ 1, 2, 3 ];\n" + "items: \$i | push( 4 ) | push ( 5 );",
                    """{"items":[1,2,3,4,5]}""",
                    null
                ),
                Arguments.of(
                    "\$a: [ 1, 2, 3 ];\n" +
                            "\$b: [ 4, 5, 6 ];\n" +
                            "items: \$a | pushItems( \$b );",
                    """{"items":[1,2,3,4,5,6]}""",
                    null
                ),
                Arguments.of( // no $a
                    "\$b: [ 4, 5, 6 ];\n" + "items: \$a | pushItems( \$b );",
                    """{"items":[4,5,6]}""",
                    null
                ),
                Arguments.of( // no $b
                    "\$a: [ 1, 2, 3 ];\n" + "items: \$a | pushItems( \$b );",
                    """{"items":[1,2,3]}""",
                    null
                ),

                // pop - return last element
                Arguments.of(
                    "\$items: [1, 2, 3]; \$last: \$items | pop; output: [\$items, \$last]",
                    """{"output":[[1,2], 3]}""",
                    null
                ),
                // pop - empty array, returns null
                Arguments.of(
                    "\$items: []; \$last: \$items | pop; output: [\$items, \$last]",
                    """{"output":[[], null]}""",
                    null
                ),
                // pop - Not an array, returns null
                Arguments.of(
                    "\$items: \"12345\"; \$last: \$items | pop; output: [\$items, \$last]",
                    """{"output":["12345", null]}""",
                    null
                ),
                // push and pop
                Arguments.of(
                    "\$items: [] | push(1) | push(2); \$last: \$items | pop; output: [\$items, \$last]",
                    """{"output":[[1], 2]}""",
                    null
                ),
                Arguments.of(
                    "\$a: [ 1, 2, 3 ];\n" +
                            "empty: \$a | isEmpty;\n" +
                            "notEmpty: \$a | isNotEmpty;\n",
                    """{ "empty": false, "notEmpty": true }""",
                    null
                ),
                Arguments.of(
                    "\$a: [ ];\n" +
                            "empty: \$a | isEmpty;\n" +
                            "notEmpty: \$a | isNotEmpty;\n",
                    """{ "empty": true, "notEmpty": false }""",
                    null
                ),
                Arguments.of( // no $a
                    "empty: \$a | isEmpty;\n" + "notEmpty: \$a | isNotEmpty;\n",
                    """{ "empty": true, "notEmpty": false }""",
                    null
                ),

                // AT
                Arguments.of(
                    "\$items: [ 1, 2, 3, 4 ]; r : \$items | at( 0 )",
                    """{ "r": 1 }""",
                    null
                ),
                Arguments.of(
                    "\$items: [ 1, 2, 3, 4 ]; r : \$items | at( 2 )",
                    """{ "r": 3 }""",
                    null
                ),
                Arguments.of(
                    "\$items: [ 1, 2, 3, 4 ]; r : \$items | at( 4 )",
                    """{ "r": null }""",
                    null
                ),
                Arguments.of(
                    "\$items: [ 1, 2, 3, 4 ]; r : \$items | at( -2 )",
                    """{ "r": null }""",
                    null
                ),

                // Array Access
                Arguments.of("\$items: [ 1, 2, 3, 4 ]; r : \$items[0]", """{ "r": 1 }""", null),
                Arguments.of("\$items: [ 1, 2, 3, 4 ]; r : \$items[3]", """{ "r": 4 }""", null),
                Arguments.of(
                    "\$items: [ { amount : 3.1 }, { amount : 4.5 }, { amount: 3 }, { }, { amount: \"abc\" }, { amount: true } ];\n" +
                            "r : \$items[1]",
                    """{ "r": { "amount" : 4.5 } }""",
                    null
                ),
                Arguments.of(
                    "\$items: [ { amount : 3.1 }, { amount : 4.5 }, { amount: 3 }, { }, { amount: \"abc\" }, { amount: true } ];\n" +
                            "r : \$items[1].amount",
                    """{ "r": 4.5 }""",
                    null
                ),
                Arguments.of(
                    "\$items: [ 1 ];\n" + "r : \$items | to.array",
                    """{ "r": [ 1 ] }""",
                    null
                ),
                Arguments.of(
                    "\$items: null;\n" + "r : \$items | to.array",
                    """{ "r": [ null ] }""",
                    null
                ),
                Arguments.of(
                    "\$items: 1;\n" + "r : \$items | to.array",
                    """{ "r": [ 1 ] }""",
                    null
                ),
                Arguments.of(
                    "\$items: { a: 4 };\n" + "r : \$items | to.array",
                    """{ "r": [ {"a":4} ] }""",
                    null
                ),
            )
        }

        @JvmStatic
        fun simpleModifiers(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "\$v: \"abcd\"; result: \$v|mask",
                    """{ "result": "abcd" }""",
                    null
                ),
                Arguments.of(
                    "\$uuid: \"bd46955c-7c67-4c87-82c5-b96b8b4ca04b\"; result: \$uuid|sanitizeTid",
                    """{ "result": "bd46955c-7c67-4c87-82c5-b96b8b4ca04b" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"abcDEF\"; result: \$in | trim",
                    """{ "result": "abcDEF" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \" abcDEF \"; result: \$in | trim",
                    """{ "result": "abcDEF" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \" abcDEF \"; result: \$in | trimStart",
                    """{ "result": "abcDEF " }""",
                    null
                ),
                Arguments.of(
                    "\$in: \" abcDEF \"; result: \$in | trimEnd",
                    """{ "result": " abcDEF" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"abcDEF\"; result: \$in | lowerCase",
                    """{ "result": "abcdef" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"abcDEF\"; result: \$in | upperCase",
                    """{ "result": "ABCDEF" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"NOT TRUE\"; result: \$in | to.boolean",
                    """{ "result": false }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"my input string-up to-this value\"; result: \$in | substring()",
                    """{ "result": "" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"my input string-up to-this value\"; result: \$in | substring(-5)",
                    """{ "result": "" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"my input string-up to-this value\"; result: \$in | substring(-5, 0)",
                    """{ "result": "" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"my input string-up to-this value\"; result: \$in | substring(0, 0)",
                    """{ "result": "" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"my input string-up to-this value\"; result: \$in | substring(0, 5)",
                    """{ "result": "my in" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"my input string-up to-this value\"; result: \$in | substring(999, 5)",
                    """{ "result": "" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"my input string-up to-this value\"; result: \$in | substring(999, 9999)",
                    """{ "result": "" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"my input string-up to-this value\"; result: \$in | substringUpto(\"up to\")",
                    """{ "result": "my input string-" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"my input string-after-this value\"; result: \$in | substringAfter(\"after\")",
                    """{ "result": "-this value" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"my input string to be replaced\"; result: \$in | replace(\"string to be replaced\", \"replacement\")",
                    """{ "result": "my input replacement" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"need to remove this part of the text\"; result: \$in | remove(\"this part of the \")",
                    """{ "result": "need to remove text" }""",
                    null
                ),
                Arguments.of("\$in: 1; result: \$in | cap(2)", """{ "result": "1" }""", null),
                Arguments.of("\$in: 1; result: \$in | left(2)", """{ "result": "1" }""", null),
                Arguments.of(
                    "\$in: 123; result: \$in | cap(2)",
                    """{ "result": "12" }""",
                    null
                ),
                Arguments.of(
                    "\$in: 123; result: \$in | left(2)",
                    """{ "result": "12" }""",
                    null
                ),
                Arguments.of(
                    "\$in: null; result: \$in | cap(2)",
                    """{ "result": null }""",
                    null
                ),
                Arguments.of(
                    "\$in: null; result: \$in | left(2)",
                    """{ "result": null }""",
                    null
                ),
                Arguments.of("\$in: \"\"; result: \$in | cap(2)", """{ "result": "" }""", null),
                Arguments.of(
                    "\$in: \"need to cap at this part of the text\"; result: \$in | cap(15)",
                    """{ "result": "need to cap at " }""",
                    null
                ),

                // right
                Arguments.of("\$in: 1; result: \$in | right(2)", """{ "result": "1" }""", null),
                Arguments.of(
                    "\$in: 123; result: \$in | right(2)",
                    """{ "result": "23" }""",
                    null
                ),
                Arguments.of(
                    "\$in: null; result: \$in | right(2)",
                    """{ "result": null }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"\"; result: \$in | right(2)",
                    """{ "result": "" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"need to cap at this part of the text\"; result: \$in | right(15)",
                    """{ "result": "art of the text" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"first string\"; result: \$in | concat(\"second string\", \"--\")",
                    """{ "result": "first string--second string" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"first string\"; result: \$in | concat(\"-ending\")",
                    """{ "result": "first string-ending" }""",
                    null
                ),

                // split
                Arguments.of("result: null | split", """{ "result": [""] }""", null),
                Arguments.of("result: \"abc\" | split", """{ "result": ["abc"] }""", null),
                Arguments.of(
                    "result: \"abc,def\" | split",
                    """{ "result": ["abc","def"] }""",
                    null
                ),
                Arguments.of(
                    "result: \"a|b|c|\" | split(\"|\")",
                    """{ "result": ["a", "b", "c", ""] }""",
                    null
                ),

                // Keys
                Arguments.of( // url encoded
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in | keys",
                    """{ "result": ["b", "c","a"] } """,
                    null
                ),
                Arguments.of( // url encoded
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in | keys | join.string()",
                    """{"result":"b,c,a"}""",
                    null
                ),
                Arguments.of( // url encoded
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in | keys | join.string( \"\n\" )",
                    """{"result":"b\nc\na"}""",
                    null
                ),

                // KV
                Arguments.of( // url encoded
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in | kv",
                    """{"result":[{"key":"b","value":1},{"key":"c","value":2},{"key":"a","value":3}]}""",
                    null
                ),

                // |delete
                Arguments.of( // url encoded
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in | delete ( \"b\" )",
                    """{"result": { "c":2, "a": 3 } }""",
                    null
                ),
                Arguments.of( // url encoded
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in | delete ( \"a\" )",
                    """{"result": { "b":1, "c": 2 } }""",
                    null
                ),
                Arguments.of("@.Run.Sleep( 100 )", """{ }""", null),
                Arguments.of(
                    "\$u: @.UUID.New();\n" + "result: if (\$u) true else false",
                    """{ "result": true }""",
                    null
                ),
            )
        }

        @JvmStatic
        fun propertyManagement(): Stream<Arguments> {
            return Stream.of(
                Arguments.of( // url encoded
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in | getProperty( \"c\" )",
                    """{"result": 2}""",
                    null
                ),
                Arguments.of( // url encoded
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in | getProperty( \"C\" )",
                    """{"result": 2}""",
                    null
                ),
                Arguments.of( // url encoded
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in | getProperty( \"abce\" )",
                    """{"result": null }""",
                    null
                ),
                Arguments.of(
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in | getProperty( \$v )",
                    """{"result": null }""",
                    mapOf("v" to JsonNodeFactory.instance.objectNode().put("Field", "b"))
                ),
                Arguments.of(
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in | getProperty( \$v.Field )",
                    """{"result": 1 }""",
                    mapOf("v" to JsonNodeFactory.instance.objectNode().put("Field", "b"))
                ),
                Arguments.of(
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in.b | getProperty( \$v.Field )",
                    """{"result": null }""",
                    mapOf("v" to JsonNodeFactory.instance.objectNode().put("Field", "b"))
                ),
                Arguments.of(
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in | setProperty( 'new', \$v );",
                    """{"result":{"b":1,"c":2,"a":3,"new":{"Field":"b"}}}""",
                    mapOf("v" to JsonNodeFactory.instance.objectNode().put("Field", "b"))
                ),
                Arguments.of(
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in | setProperty( 'new', \$v.Field )",
                    """{"result":{"b":1,"c":2,"a":3,"new":"b"}}""",
                    mapOf("v" to JsonNodeFactory.instance.objectNode().put("Field", "b"))
                ),
                Arguments.of(
                    "\$in: { b: 1, c:2, a: 3 }; result: null | setProperty( 'new', \$v.Field )",
                    """{"result":{"new":"b"}}""",
                    mapOf("v" to JsonNodeFactory.instance.objectNode().put("Field", "b"))
                ),

                //                Arguments.of(
                //                    "\$in: { b: 1, c:2, a: 3 }; result: \$in.b | setProperty(
                // 'new', \$v.Field )",
                //                    """{"result":{ "b": { "new": "b" }, "c":2, "a":3 }}""",
                //                    mapOf("v" to
                // JsonNodeFactory.instance.objectNode().put("Field", "b"))
                //                ),
                //
                //                Arguments.of(
                //                    "\$in: { b: 1, c:2, a: 3 }; result: \$in.x | setProperty(
                // 'new', \$v.Field )",
                //                    """{"result":{"b":1,"c":2,"a":3,"new":"b"}}""",
                //                    mapOf("v" to
                // JsonNodeFactory.instance.objectNode().put("Field", "b"))
                //                ),
                //
                //                Arguments.of(
                //                    "\$in: { b: 1, c:2, a: 3 }; result: \$in.x | setProperty(
                // 'new', \$v.Field )",
                //                    """{"result":{"b":1,"c":2,"a":3,"new":"b"}}""",
                //                    mapOf("v" to
                // JsonNodeFactory.instance.objectNode().put("Field", "b"))
                //                ),
            )
        }

        @JvmStatic
        fun conversionModifiers(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "\$in: \"abc\"; result: \$in|encode.base64",
                    """{ "result": "YWJj" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"YWJj\"; result: \$in|decode.base64|to.string",
                    """{ "result": "abc" }""",
                    null
                ),

                // query string encoding (space becomes + encoding)
                Arguments.of(
                    "\$in: \"abc\"; result: \$in|encode.query",
                    """{ "result": "abc" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"a b % c\"; result: \$in|encode.query",
                    """{ "result": "a+b+%25+c" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"abc\"; result: \$in|decode.query",
                    """{ "result": "abc" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"a+b+%25+c\"; result: \$in|decode.query",
                    """{ "result": "a b % c" }""",
                    null
                ),

                // url path encoding (space becomes %20 encoding)
                Arguments.of(
                    "\$in: \"a b % c\"; result: \$in|encode.path",
                    """{ "result": "a%20b%20%25%20c" }""",
                    null
                ),
                // decode.path is not supported as I can't find a Java implementation

                Arguments.of(
                    "\$in: \"Truncation Testing ,  ट्रंकेशन परीक्षण , トランケーションテスト, 截断测试 , ਕੱਟਣ ਦੀ ਜਾਂਚ\\r\\n\"; result: \$in|encode.base64url",
                    """{ "result": "VHJ1bmNhdGlvbiBUZXN0aW5nICwgIOCkn-CljeCksOCkguCkleClh-CktuCkqCDgpKrgpLDgpYDgpJXgpY3gpLfgpKMgLCDjg4jjg6njg7PjgrHjg7zjgrfjg6fjg7Pjg4bjgrnjg4gsIOaIquaWrea1i-ivlSAsIOColeCpseCon-CooyDgqKbgqYAg4Kic4Ki-4KiC4KiaDQo=" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"VHJ1bmNhdGlvbiBUZXN0aW5nICwgIOCkn-CljeCksOCkguCkleClh-CktuCkqCDgpKrgpLDgpYDgpJXgpY3gpLfgpKMgLCDjg4jjg6njg7PjgrHjg7zjgrfjg6fjg7Pjg4bjgrnjg4gsIOaIquaWrea1i-ivlSAsIOColeCpseCon-CooyDgqKbgqYAg4Kic4Ki-4KiC4KiaDQo=\"; result: \$in|decode.base64url|to.string",
                    """{ "result": "Truncation Testing ,  ट्रंकेशन परीक्षण , トランケーションテスト, 截断测试 , ਕੱਟਣ ਦੀ ਜਾਂਚ\r\n" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \'<div dir=\"ltr\">Truncation Testing ,  ट्रंकेशन परीक्षण , トランケーションテスト, 截断测试 , ਕੱਟਣ ਦੀ ਜਾਂਚ</div>\'; result: \$in|encode.base64url",
                    """{ "result": "PGRpdiBkaXI9Imx0ciI-VHJ1bmNhdGlvbiBUZXN0aW5nICwgIOCkn-CljeCksOCkguCkleClh-CktuCkqCDgpKrgpLDgpYDgpJXgpY3gpLfgpKMgLCDjg4jjg6njg7PjgrHjg7zjgrfjg6fjg7Pjg4bjgrnjg4gsIOaIquaWrea1i-ivlSAsIOColeCpseCon-CooyDgqKbgqYAg4Kic4Ki-4KiC4KiaPC9kaXY-" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"PGRpdiBkaXI9Imx0ciI-VHJ1bmNhdGlvbiBUZXN0aW5nICwgIOCkn-CljeCksOCkguCkleClh-CktuCkqCDgpKrgpLDgpYDgpJXgpY3gpLfgpKMgLCDjg4jjg6njg7PjgrHjg7zjgrfjg6fjg7Pjg4bjgrnjg4gsIOaIquaWrea1i-ivlSAsIOColeCpseCon-CooyDgqKbgqYAg4Kic4Ki-4KiC4KiaPC9kaXY-\"; result: \$in|decode.base64url|to.string",
                    """{ "result": "<div dir=\"ltr\">Truncation Testing ,  ट्रंकेशन परीक्षण , トランケーションテスト, 截断测试 , ਕੱਟਣ ਦੀ ਜਾਂਚ</div>" }""",
                    null
                ),
                // other conversions
                Arguments.of(
                    "\$in: \"TRUE\"; result: \$in | to.boolean",
                    """{ "result": true }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"12345.56\"; result: \$in | to.number",
                    """{ "result": 12345 }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"123451234512345.56\"; result: \$in | to.number",
                    """{ "result": 123451234512345 }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"12345.56\"; result: \$in | to.decimal",
                    """{ "result": 12345.56 }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"123451234512345.56\"; result: \$in | to.decimal",
                    """{ "result": 123451234512345.56 }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"123\"; result: \$in | to.string",
                    """{ "result": "123" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"123.456\"; result: \$in | to.string",
                    """{ "result": "123.456" }""",
                    null
                ),
                //                Arguments.of(
                //                    "\$in: \"4161726f6e\" | hex.tobinary; result: \$in |
                // to.string(\"UTF-8\")",
                //                    """{ "result": "Aaron" }""",
                //                    null
                //                ),
                //                Arguments.of(
                //                    "\$in: \"e6bd98\" | hex.tobinary; result: \$in |
                // to.string",
                //                    """{ "result": "潘" }""",
                //                    null
                //                ),
                //                Arguments.of(
                //                    "\$in: \"fffe586f\" | hex.tobinary; result: \$in |
                // to.string(\"UTF-16\")",
                //                    """{ "result": "潘" }""",
                //                    null
                //                ),
                //                Arguments.of(
                //                    "\$in: \"586f\" | hex.tobinary; result: \$in |
                // to.string(\"UTF-16LE\")",
                //                    """{ "result": "潘" }""",
                //                    null
                //                ),
                //                Arguments.of(
                //                    "\$in: \"4161726f6e\" | hex.tobinary; result: \$in |
                // to.string(\"US-ASCII\")",
                //                    """{ "result": "Aaron" }""",
                //                    null
                //                ),
                //                Arguments.of(
                //                    "\$in: \"4161726f6e\" | hex.tobinary; result: \$in |
                // to.string(\"ISO-8859-1\")",
                //                    """{ "result": "Aaron" }""",
                //                    null
                //                ),
                Arguments.of(
                    "\$in: null; result: \$in | join.string",
                    """{ "result": "" }""",
                    null
                ),

                Arguments.of(
                    "\$in: \"Truncation Testing ,  ट्रंकेशन परीक्षण , トランケーションテスト, 截断测试 , ਕੱਟਣ ਦੀ ਜਾਂਚ\\r\\n\"; result: \$in|encode.base64url",
                    """{ "result": "VHJ1bmNhdGlvbiBUZXN0aW5nICwgIOCkn-CljeCksOCkguCkleClh-CktuCkqCDgpKrgpLDgpYDgpJXgpY3gpLfgpKMgLCDjg4jjg6njg7PjgrHjg7zjgrfjg6fjg7Pjg4bjgrnjg4gsIOaIquaWrea1i-ivlSAsIOColeCpseCon-CooyDgqKbgqYAg4Kic4Ki-4KiC4KiaDQo=" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"VHJ1bmNhdGlvbiBUZXN0aW5nICwgIOCkn-CljeCksOCkguCkleClh-CktuCkqCDgpKrgpLDgpYDgpJXgpY3gpLfgpKMgLCDjg4jjg6njg7PjgrHjg7zjgrfjg6fjg7Pjg4bjgrnjg4gsIOaIquaWrea1i-ivlSAsIOColeCpseCon-CooyDgqKbgqYAg4Kic4Ki-4KiC4KiaDQo=\"; result: \$in|decode.base64url|to.string",
                    """{ "result": "Truncation Testing ,  ट्रंकेशन परीक्षण , トランケーションテスト, 截断测试 , ਕੱਟਣ ਦੀ ਜਾਂਚ\r\n" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \'<div dir=\"ltr\">Truncation Testing ,  ट्रंकेशन परीक्षण , トランケーションテスト, 截断测试 , ਕੱਟਣ ਦੀ ਜਾਂਚ</div>\'; result: \$in|encode.base64url",
                    """{ "result": "PGRpdiBkaXI9Imx0ciI-VHJ1bmNhdGlvbiBUZXN0aW5nICwgIOCkn-CljeCksOCkguCkleClh-CktuCkqCDgpKrgpLDgpYDgpJXgpY3gpLfgpKMgLCDjg4jjg6njg7PjgrHjg7zjgrfjg6fjg7Pjg4bjgrnjg4gsIOaIquaWrea1i-ivlSAsIOColeCpseCon-CooyDgqKbgqYAg4Kic4Ki-4KiC4KiaPC9kaXY-" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"PGRpdiBkaXI9Imx0ciI-VHJ1bmNhdGlvbiBUZXN0aW5nICwgIOCkn-CljeCksOCkguCkleClh-CktuCkqCDgpKrgpLDgpYDgpJXgpY3gpLfgpKMgLCDjg4jjg6njg7PjgrHjg7zjgrfjg6fjg7Pjg4bjgrnjg4gsIOaIquaWrea1i-ivlSAsIOColeCpseCon-CooyDgqKbgqYAg4Kic4Ki-4KiC4KiaPC9kaXY-\"; result: \$in|decode.base64url|to.string",
                    """{ "result": "<div dir=\"ltr\">Truncation Testing ,  ट्रंकेशन परीक्षण , トランケーションテスト, 截断测试 , ਕੱਟਣ ਦੀ ਜਾਂਚ</div>" }""",
                    null
                ),
                // other conversions
                Arguments.of("\$in: \"TRUE\"; result: \$in | to.boolean", """{ "result": true }""", null),
                Arguments.of("\$in: \"12345.56\"; result: \$in | to.number", """{ "result": 12345 }""", null),
                Arguments.of(
                    "\$in: \"123451234512345.56\"; result: \$in | to.number",
                    """{ "result": 123451234512345 }""",
                    null
                ),
                Arguments.of("\$in: \"12345.56\"; result: \$in | to.decimal", """{ "result": 12345.56 }""", null),
                Arguments.of(
                    "\$in: \"123451234512345.56\"; result: \$in | to.decimal",
                    """{ "result": 123451234512345.56 }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"123\"; result: \$in | to.string",
                    """{ "result": "123" }""",
                    null
                ),
                Arguments.of(
                    "\$in: \"123.456\"; result: \$in | to.string",
                    """{ "result": "123.456" }""",
                    null
                ),
//                Arguments.of(
//                    "\$in: \"4161726f6e\" | hex.tobinary; result: \$in | to.string(\"UTF-8\")",
//                    """{ "result": "Aaron" }""",
//                    null
//                ),
//                Arguments.of(
//                    "\$in: \"e6bd98\" | hex.tobinary; result: \$in | to.string",
//                    """{ "result": "潘" }""",
//                    null
//                ),
//                Arguments.of(
//                    "\$in: \"fffe586f\" | hex.tobinary; result: \$in | to.string(\"UTF-16\")",
//                    """{ "result": "潘" }""",
//                    null
//                ),
//                Arguments.of(
//                    "\$in: \"586f\" | hex.tobinary; result: \$in | to.string(\"UTF-16LE\")",
//                    """{ "result": "潘" }""",
//                    null
//                ),
//                Arguments.of(
//                    "\$in: \"4161726f6e\" | hex.tobinary; result: \$in | to.string(\"US-ASCII\")",
//                    """{ "result": "Aaron" }""",
//                    null
//                ),
//                Arguments.of(
//                    "\$in: \"4161726f6e\" | hex.tobinary; result: \$in | to.string(\"ISO-8859-1\")",
//                    """{ "result": "Aaron" }""",
//                    null
//                ),
                Arguments.of(
                    "\$in: null; result: \$in | join.string",
                    """{ "result": "" }""",
                    null
                ),

                // test BinaryArray > unzip > get file content > to.string
                Arguments.of(
                    "\$z = @.Zip.Start(); " +
                            "\$zipped = \$z | zip.add(\"foo.txt\", \"Hello World!123\", \"utf-8\") | zip.add(\"bar.txt\", \"some content\") | zip.close;" +
                            "\$unzipped = \$zipped | unzip;" +
                            "\$foo = \$unzipped[0].content | to.string;" +
                            "\$bar = \$unzipped[1].content | to.string;" +
                            "result: [\$foo, \$bar]",
                    """{"result":["Hello World!123","some content"]}""",
                    null
                ),
                // test BinaryNode > to.bytes > unzip > get file content > to.string
                Arguments.of(
                    "\$z = @.Zip.Start(); " +
                            "\$zipped = \$z | zip.add(\"foo.txt\", \"Hello World!123\", \"utf-8\") | zip.add(\"bar.txt\", \"some content\") | zip.close;" +
                            "\$unzipped = \$zipped | to.bytes | unzip;" +
                            "\$foo = \$unzipped[0].content | to.string;" +
                            "\$bar = \$unzipped[1].content | to.string;" +
                            "result: [\$foo, \$bar]",
                    """{"result":["Hello World!123","some content"]}""",
                    null
                ),
                // test zip binary file
                Arguments.of(
                    "\$z = @.Zip.Start(); \$text = \"hello world\"; \$b = \$text | to.bytes(\"utf-8\");" +
                            "\$zipped = \$z | zip.add(\"foo.bin\", \$b) | zip.add(\"bar.txt\", \"some content\") | zip.close;" +
                            "\$unzipped = \$zipped | to.bytes | unzip;" +
                            "\$foo = \$unzipped[0].content;" +
//                            "\$foo = \$b;" +
                            "\$bar = \$unzipped[1].content | to.string;" +
                            "result: [\$foo, \$bar]",
                    """{"result":["aGVsbG8gd29ybGQ=","some content"]}""",
                    null
                ),
                // string > zip.add should return error
                Arguments.of(
                    "\$obj = \"hello\"; result: \$obj | zip.add(\"foo.txt\", \"bar\")",
                    """"Could not Execute '@.zip.add' at Position(file=test, line=1, column=29, endLine=1, endColumn=56).\nNot a zip object at Position(file=test, line=1, column=29, endLine=1, endColumn=56)"""",
                    null
                ),
                // not ZipObject > zip.add should return error
                Arguments.of(
                    "\$obj = {}; result: \$obj | zip.add(\"foo.txt\", \"bar\")",
                    """"Could not Execute '@.zip.add' at Position(file=test, line=1, column=24, endLine=1, endColumn=51).\nNot a zip object at Position(file=test, line=1, column=24, endLine=1, endColumn=51)"""",
                    null
                ),
                // test invalid zip modifier
                Arguments.of(
                    "\$z = @.Zip.Start(); " +
                            "result: \$z | zip.plus(\"foo.txt\", \"Hello World!123\", \"utf-8\")",
                    """"Could not Execute '@.zip.plus' at Position(file=test, line=1, column=31, endLine=1, endColumn=80).\nInvalid method; method=plus at Position(file=test, line=1, column=31, endLine=1, endColumn=80)"""",
                    null
                ),
                // test zip invalid filename
                Arguments.of(
                    "\$z = @.Zip.Start(); " +
                            "result: \$z | zip.add(\"\", \"Hello World!123\", \"utf-8\")",
                    """"Could not Execute '@.zip.add' at Position(file=test, line=1, column=31, endLine=1, endColumn=72).\nBlank file name at Position(file=test, line=1, column=31, endLine=1, endColumn=72)"""",
                    null
                ),
                // test unzip should fail if input is not bytearray
                Arguments.of(
                    "\$s = \"hello world\"; result: \$s | unzip",
                    """"Could not Execute '@.unzip' at Position(file=test, line=1, column=31, endLine=1, endColumn=38).\nInvalid content type for unzip com.fasterxml.jackson.databind.node.TextNode at Position(file=test, line=1, column=31, endLine=1, endColumn=38)"""",
                    null
                ),
                // ObjectRefNode > to.bytes should return empty byte array
                Arguments.of(
                    "\$obj = {}; result: \$obj | to.bytes",
                    """{"result":""}""",
                    null
                ),
                // null > to.bytes should return empty byte array
                Arguments.of(
                    "\$obj = null; result: \$obj | to.bytes",
                    """{"result":""}""",
                    null
                ),
                Arguments.of(
                    "\$in: { b: \"a b\", c:2, a: 3 }; result: \$in | join.string(\"\n\")",
                    """{ "result": "b=a b\nc=2\na=3"} """,
                    null
                ),
                Arguments.of(   // url encoded
                    "\$in: { b: \"a b\", c:2, a: 3 }; result: \$in | join.query(\"\n\")",
                    """{ "result": "b=a+b\nc=2\na=3"} """,
                    null
                ),
                Arguments.of(   // url encoded
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in | sort | join.query(\"&\")",
                    """{ "result": "a=3&b=1&c=2"} """,
                    null
                ),
                Arguments.of(   // url encoded
                    "\$in: \"emilydoe@gmail.com , something@example.com , John M <johnM@example.com>, Doe Emily <emilydoe2@gmail.com>\"; result: \$in | email.parse",
                    """{"result":[{"name":null,"emailAddress":"emilydoe@gmail.com"},{"name":null,"emailAddress":"something@example.com"},{"name":"John M","emailAddress":"johnM@example.com"},{"name":"Doe Emily","emailAddress":"emilydoe2@gmail.com"}]}""",
                    null
                ),

                Arguments.of(   // decode.base64 twice test with generic JSON (no compression)
                    "\$in = \"ZXlKeVpYTjFiSFFpT25zaVpHRjBZU0k2ZXlKdVlXMWxJam9pUVd4cFkyVWlMQ0poWjJVaU9qTXdmWDE5\"; " +
                            "result: \$in  | decode.base64 | decode.base64 | to.string",
                    """{"result":"{\"result\":{\"data\":{\"name\":\"Alice\",\"age\":30}}}"}""",
                    null
                ),

                Arguments.of(   // decode.base64 twice test with generic JSON (duplicate for coverage - no compression)
                    "\$in = \"ZXlKeVpYTjFiSFFpT25zaVpHRjBZU0k2ZXlKdVlXMWxJam9pUVd4cFkyVWlMQ0poWjJVaU9qTXdmWDE5\"; " +
                            "result: \$in  | decode.base64 | decode.base64 | to.string",
                    """{"result":"{\"result\":{\"data\":{\"name\":\"Alice\",\"age\":30}}}"}""",
                    null
                ),
                Arguments.of(
                    "\$in = null; " +
                            "result: \$in  | decode.base64 | decode.base64 | zip.inflate | to.string",
                    """{"result":""}""",
                    null
                ),
                Arguments.of(
                    "\$in = \"abc\"; " +
                            "result: \$in | zip.deflate | to.hex",
                    """{"result":"789c4b4c4a0600024d0127"}""",
                    null
                ),
                Arguments.of(
                    "\$in = \"789c4b4c4a0600024d0127\"; " +
                            "result: \$in | hex.tobinary | zip.inflate | to.string",
                    """{"result":"abc"}""",
                    null
                ),
                // test BinaryArray > unzip > get file content > to.string
                Arguments.of(
                    "\$z = @.Zip.Start(); " +
                            "\$zipped = \$z | zip.add(\"foo.txt\", \"Hello World!123\", \"utf-8\") | zip.add(\"bar.txt\", \"some content\") | zip.close;" +
                            "\$unzipped = \$zipped | unzip;" +
                            "\$foo = \$unzipped[0].content | to.string;" +
                            "\$bar = \$unzipped[1].content | to.string;" +
                            "result: [\$foo, \$bar]",
                    """{"result":["Hello World!123","some content"]}""",
                    null
                ),
                // test BinaryNode > to.bytes > unzip > get file content > to.string
                Arguments.of(
                    "\$z = @.Zip.Start(); " +
                            "\$zipped = \$z | zip.add(\"foo.txt\", \"Hello World!123\", \"utf-8\") | zip.add(\"bar.txt\", \"some content\") | zip.close;" +
                            "\$unzipped = \$zipped | to.bytes | unzip;" +
                            "\$foo = \$unzipped[0].content | to.string;" +
                            "\$bar = \$unzipped[1].content | to.string;" +
                            "result: [\$foo, \$bar]",
                    """{"result":["Hello World!123","some content"]}""",
                    null
                ),
                // test zip binary file
                Arguments.of(
                    "\$z = @.Zip.Start(); \$text = \"hello world\"; \$b = \$text | to.bytes(\"utf-8\");" +
                            "\$zipped = \$z | zip.add(\"foo.bin\", \$b) | zip.add(\"bar.txt\", \"some content\") | zip.close;" +
                            "\$unzipped = \$zipped | to.bytes | unzip;" +
                            "\$foo = \$unzipped[0].content;" +
                            //                            "\$foo = \$b;" +
                            "\$bar = \$unzipped[1].content | to.string;" +
                            "result: [\$foo, \$bar]",
                    """{"result":["aGVsbG8gd29ybGQ=","some content"]}""",
                    null
                ),
                // string > zip.add should return error
                Arguments.of(
                    "\$obj = \"hello\"; result: \$obj | zip.add(\"foo.txt\", \"bar\")",
                    """"Could not Execute '@.zip.add' at Position(file=test, line=1, column=29, endLine=1, endColumn=56).\nNot a zip object at Position(file=test, line=1, column=29, endLine=1, endColumn=56)"""",
                    null
                ),
                // not ZipObject > zip.add should return error
                Arguments.of(
                    "\$obj = {}; result: \$obj | zip.add(\"foo.txt\", \"bar\")",
                    """"Could not Execute '@.zip.add' at Position(file=test, line=1, column=24, endLine=1, endColumn=51).\nNot a zip object at Position(file=test, line=1, column=24, endLine=1, endColumn=51)"""",
                    null
                ),
                // test invalid zip modifier
                Arguments.of(
                    "\$z = @.Zip.Start(); " +
                            "result: \$z | zip.plus(\"foo.txt\", \"Hello World!123\", \"utf-8\")",
                    """"Could not Execute '@.zip.plus' at Position(file=test, line=1, column=31, endLine=1, endColumn=80).\nInvalid method; method=plus at Position(file=test, line=1, column=31, endLine=1, endColumn=80)"""",
                    null
                ),
                // test zip invalid filename
                Arguments.of(
                    "\$z = @.Zip.Start(); " +
                            "result: \$z | zip.add(\"\", \"Hello World!123\", \"utf-8\")",
                    """"Could not Execute '@.zip.add' at Position(file=test, line=1, column=31, endLine=1, endColumn=72).\nBlank file name at Position(file=test, line=1, column=31, endLine=1, endColumn=72)"""",
                    null
                ),
                // test unzip should fail if input is not bytearray
                Arguments.of(
                    "\$s = \"hello world\"; result: \$s | unzip",
                    """"Could not Execute '@.unzip' at Position(file=test, line=1, column=31, endLine=1, endColumn=38).\nInvalid content type for unzip com.fasterxml.jackson.databind.node.TextNode at Position(file=test, line=1, column=31, endLine=1, endColumn=38)"""",
                    null
                ),
                // ObjectRefNode > to.bytes should return empty byte array
                Arguments.of("\$obj = {}; result: \$obj | to.bytes", """{"result":""}""", null),
                // null > to.bytes should return empty byte array
                Arguments.of(
                    "\$obj = null; result: \$obj | to.bytes",
                    """{"result":""}""",
                    null
                ),
                Arguments.of(
                    "\$in: { b: \"a b\", c:2, a: 3 }; result: \$in | join.string(\"\n\")",
                    """{ "result": "b=a b\nc=2\na=3"} """,
                    null
                ),
                Arguments.of( // url encoded
                    "\$in: { b: \"a b\", c:2, a: 3 }; result: \$in | join.query(\"\n\")",
                    """{ "result": "b=a+b\nc=2\na=3"} """,
                    null
                ),
                Arguments.of( // url encoded
                    "\$in: { b: 1, c:2, a: 3 }; result: \$in | sort | join.query(\"&\")",
                    """{ "result": "a=3&b=1&c=2"} """,
                    null
                ),
                Arguments.of( // url encoded
                    "\$in: \"emilydoe@gmail.com , something@example.com , John M <johnM@example.com>, Doe Emily <emilydoe2@gmail.com>\"; result: \$in | email.parse",
                    """{"result":[{"name":null,"emailAddress":"emilydoe@gmail.com"},{"name":null,"emailAddress":"something@example.com"},{"name":"John M","emailAddress":"johnM@example.com"},{"name":"Doe Emily","emailAddress":"emilydoe2@gmail.com"}]}""",
                    null
                ),
                Arguments.of(   // decode.base64 twice test with generic JSON (no compression)
                    "\$in = \"ZXlKeVpYTjFiSFFpT25zaVpHRjBZU0k2ZXlKdVlXMWxJam9pUVd4cFkyVWlMQ0poWjJVaU9qTXdmWDE5\"; " +
                            "result: \$in  | decode.base64 | decode.base64 | to.string",
                    """{"result":"{\"result\":{\"data\":{\"name\":\"Alice\",\"age\":30}}}"}""",
                    null
                ),
                Arguments.of(
                    "\$in = \"abc\"; " + "result: \$in | zip.deflate | to.hex",
                    """{"result":"789c4b4c4a0600024d0127"}""",
                    null
                ),
                Arguments.of(
                    "\$in = \"789c4b4c4a0600024d0127\"; " +
                            "result: \$in | hex.tobinary | zip.inflate | to.string",
                    """{"result":"abc"}""",
                    null
                )
            )
        }

        @JvmStatic
        fun mathModifiers(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "\$v: \"100.89193456\"; result: \$v|negate",
                    """{ "result": -100.89193456 }""",
                    null
                ),
                Arguments.of(
                    "\$v: \"-100.89193456\"; result: \$v|absolute",
                    """{ "result": 100.89193456 }""",
                    null
                ),
                Arguments.of(
                    "\$v: \"100.89193456\"; result: \$v|round.up(3)",
                    """{ "result": 100.892 }""",
                    null
                ),
                Arguments.of(
                    "\$v: \"100.89123456\"; result: \$v|round.down(3)",
                    """{ "result": 100.891 }""",
                    null
                ),
                Arguments.of(
                    "\$v: \"100.89193456\"; result: \$v|round.ceiling(3)",
                    """{ "result": 100.892 }""",
                    null
                ),
                Arguments.of(
                    "\$v: \"100.89123456\"; result: \$v|round.floor(3)",
                    """{ "result": 100.891 }""",
                    null
                ),
                Arguments.of(
                    "min: @.Math.min(2, 5.1, 3, 1.7, 4, 1.2)",
                    """ { "min": 1.2 } """",
                    null
                ),
                Arguments.of(
                    "\$v:[2, 5.1, 3, 1.7, 4, 1.2]; min: @.Math.min(\$v)",
                    """{ "min": 1.2 }"""",
                    null
                ),
                Arguments.of("\$v:3.2; min: @.Math.min(\$v, 2.4)", """{ "min": 2.4 }"""", null),
                Arguments.of(
                    "\$v:3.2; \$w:4.8; min: @.Math.min(\$v, \$w, 2.4)",
                    """{ "min": 2.4 }"""",
                    null
                ),
                Arguments.of(
                    "\$v:3.2; \$w:1.0; min: @.Math.min(4, \$v, 2.4, \$w)",
                    """{ "min": 1.0 }"""",
                    null
                ),
                Arguments.of(
                    "\$v:[2, 5.1, 3, 1.7, 4, 1.2]; len: \$v | length",
                    """{ "len": 6 }"""",
                    null
                ),
                Arguments.of("\$v:\"abc\"; len: \$v | length", """{ "len": 3 }"""", null),
                Arguments.of(
                    "max: @.Math.max(2, 5.1, 3, 1.7, 4, 1.2)",
                    """ { "max": 5.1 } """",
                    null
                ),
                Arguments.of(
                    "\$v:[2, 5.1, 3, 1.7, 4, 1.2]; max: @.Math.max(\$v)",
                    """{ "max": 5.1 }"""",
                    null
                ),
                Arguments.of("\$v:2.4; max: @.Math.max(\$v, 3.2)", """{ "max": 3.2 }"""", null),
                Arguments.of(
                    "\$v:2.4; \$w:4.8 max: @.Math.max(\$v, \$w, 3.2)",
                    """{ "max": 4.8 }"""",
                    null
                ),
                Arguments.of(
                    "\$v:3.2; max: @.Math.max(4, 2.4, 1.6, \$v)",
                    """{ "max": 4 }"""",
                    null
                ),
                Arguments.of(
                    "mean: @.Math.mean(2, 5.1, 3, 1.7, 4, 1.2)",
                    """ { "mean": 2.83 } """",
                    null
                ),
                Arguments.of(
                    "\$v:[2, 5.1, 3, 1.7, 4, 1.2]; mean: @.Math.mean(\$v)",
                    """{ "mean": 2.83 }"""",
                    null
                ),
                Arguments.of(
                    "\$v:3.2; mean: @.Math.mean(\$v, 2.4)",
                    """{ "mean": 2.8 }"""",
                    null
                ),
                Arguments.of(
                    "\$v:3.2; \$w:4.6; mean: @.Math.mean(\$v, \$w, 2.4)",
                    """{ "mean": 3.4 }"""",
                    null
                ),
                Arguments.of(
                    "\$v:3.2; mean: @.Math.mean(1.6, \$v, 2.4)",
                    """{ "mean": 2.4 }"""",
                    null
                ),
                // tests to validate errors
                Arguments.of(
                    "min: @.Math.min(1.2, \"a\", 2.4)",
                    """"Could not Execute '@.math.min'. Error='NumberFormatException: Character a is neither a decimal digit number, decimal point, nor \"e\" notation exponential mark.' at Position(file=test, line=1, column=5, endLine=1, endColumn=30)."""",
                    null
                ),
                Arguments.of(
                    "\$v:\"a\"; max: @.Math.max(1.2, 3.6, \$v)",
                    """"Could not Execute '@.math.max'. Error='NumberFormatException: Character \" is neither a decimal digit number, decimal point, nor \"e\" notation exponential mark.' at Position(file=test, line=1, column=13, endLine=1, endColumn=37)."""",
                    null
                ),
                Arguments.of(
                    "\$v:3.6; mean: @.Math.mean(\"a\", \$v)",
                    """"Could not Execute '@.math.mean'. Error='NumberFormatException: Character n is neither a decimal digit number, decimal point, nor \"e\" notation exponential mark.' at Position(file=test, line=1, column=14, endLine=1, endColumn=34)."""",
                    null
                ),
            )
        }

        @JvmStatic
        fun selectModifier(): Stream<Arguments> {
            return Stream.of(
                // string
                Arguments.of(
                    "\$range = @.Array.Range( 0, 5 );\n" +
                            "value: \$range | select ( \"$[0]\" )\n",
                    """{ "value": 0 }""",
                    null
                ),
                Arguments.of(
                    "\$range = @.Array.Range( 0, 5 );\n" +
                            "value: \$range | select ( \"$[3]\" )\n",
                    """{ "value": 3 }""",
                    null
                ),

                // mix variables into string interpolation
                Arguments.of(
                    "\$range = { items: @.Array.Range( 10, 30, 3 ) };\n" +
                            "\$index = 1;\n" +
                            "value: \$range | select ( `$.items[ \$index ]` )\n",
                    """{ "value": 13 }""",
                    null
                ),

                // string
                Arguments.of(
                    "\$o = { a : { b: 123 } };\n" + "value: \$o | select ( \"$.a.b\" )\n",
                    """{ "value": 123 }""",
                    null
                ),
                // argument format without ""
                Arguments.of(
                    "\$o = { a : { b: 123 } };\n" + "value: \$o | select ( $.a.b )\n",
                    """{ "value": 123 }""",
                    null
                ),
                // variable no path
                Arguments.of(
                    "\$o = { a : { b: 123 } };\n" + "value: \$o | select ( \$v )\n",
                    """{ "value": 123 }""",
                    mapOf("v" to "$.a.b")
                ),
                Arguments.of(
                    "\$o = { a : { b: 123 } };\n" + "value: \$o | select ( \$v.path )\n",
                    """{ "value": 123 }""",
                    mapOf("v" to JsonNodeFactory.instance.objectNode().put("path", "$.a.b"))
                ),
            )
        }

        @JvmStatic
        fun sortModifier_Strict(): Stream<Arguments> {
            return Stream.of(
                // Sort empty object - does not crash
                Arguments.of("\$o = {  };\n" + "value: \$o | sort\n", """{"value":{}}""", null),
                // Sort object - use the keys - default is asc
                Arguments.of(
                    "\$o = { b: 1, c:2, a: 3 };\n" + "value: \$o | sort\n",
                    """{"value":{"a":3,"b":1,"c":2}}""",
                    null
                ),
                // Sort object - use the keys - asc
                Arguments.of(
                    "\$o = { b: 1, c:2, a: 3 };\n" +
                            "value: \$o | sort( {order: \"asc\"} )\n",
                    """{"value":{"a":3,"b":1,"c":2}}""",
                    null
                ),
                // Sort object - use the keys - desc
                Arguments.of(
                    "\$o = { b: 1, c:2, a: 3 };\n" +
                            "value: \$o | sort( {order: \"desc\"} )\n",
                    """{"value":{"c":2,"b":1,"a":3}}""",
                    null
                ),
                // Sort object - use the keys - desc, case-insensitive
                Arguments.of(
                    "\$o = { b: 1, c:2, a: 3 };\n" +
                            "value: \$o | sort( {order: \"DESC\"} )\n",
                    """{"value":{"c":2,"b":1,"a":3}}""",
                    null
                ),
                // Sort object - use the keys - desc, case-sensitive
                Arguments.of(
                    "\$o = { B: 1, c:2, a: 3 };\n" +
                            "value: \$o | sort( {order: \"DESC\", caseSensitive: true} )\n",
                    """{"value":{"c":2,"a":3,"B":1}}""",
                    null
                )
            )
        }

        @JvmStatic
        fun sortModifier(): Stream<Arguments> {
            return Stream.of(
                // Sort array of numbers - default is asc
                Arguments.of(
                    "\$o = [ 1, 9, 3, 99, 15 ];\n" + "value: \$o | sort\n",
                    """{ "value": [1, 3, 9, 15, 99] }""",
                    null
                ),
                // Sort array of numbers - asc
                Arguments.of(
                    "\$o = [ 1, 9, 3, 99, 15 ];\n" +
                            "value: \$o | sort( {order: \"asc\"} )\n",
                    """{ "value": [1, 3, 9, 15, 99] }""",
                    null
                ),
                // Sort array of numbers - desc
                Arguments.of(
                    "\$o = [ 1, 9, 3, 99, 15 ];\n" +
                            "value: \$o | sort( {order: \"desc\"} )\n",
                    """{ "value": [99, 15, 9, 3, 1] }""",
                    null
                ),
                // Sort array of numbers - desc, case-insensitive
                Arguments.of(
                    "\$o = [ 1, 9, 3, 99, 15 ];\n" +
                            "value: \$o | sort( {order: \"DESC\"} )\n",
                    """{ "value": [99, 15, 9, 3, 1] }""",
                    null
                ),
                // Sort array of boolean - default is asc
                Arguments.of(
                    "\$o = [ false, true, true, false, true ];\n" + "value: \$o | sort\n",
                    """{ "value": [false, false, true, true, true] }""",
                    null
                ),
                // Sort array of boolean - desc
                Arguments.of(
                    "\$o = [ false, true, true, false, true ];\n" +
                            "value: \$o | sort( {order: \"desc\" } )\n",
                    """{ "value": [true, true, true, false, false] }""",
                    null
                ),
                // Sort array of strings
                Arguments.of(
                    "\$o = [ \"d\", \"a\", \"c\", \"b\" ];\n" + "value: \$o | sort\n",
                    """{ "value": ["a", "b", "c", "d"] }""",
                    null
                ),
                // Sort array of strings representing numbers - perform a text comparison (not
                // numeric)
                Arguments.of(
                    "\$o = [ \"1\", \"9\", \"3\", \"99\", \"15\" ];\n" +
                            "value: \$o | sort\n",
                    """{ "value": ["1", "15", "3", "9", "99"] }""",
                    null
                ),
                // Sort array of strings and numbers and the first item is a string - undefined
                // behaviour
                // The numbers will be order first but in a random order followed by all the
                // string sorted
                Arguments.of(
                    "\$o = [ \"1\", 9, \"99\", \"3\", 1 ];\n" + "value: \$o | sort\n",
                    """{ "value": [9,  1, "1", "3", "99"] }""",
                    null
                ),
                // Sort array of strings and numbers and the first item is a number, undefined
                // behaviour
                // The string will be ordered first but in a random order followed by the
                // numbers in a random order
                Arguments.of(
                    "\$o = [9, \"3\", \"1\", 5, \"99\", 1, \"2\" ];\n" +
                            "value: \$o | sort\n",
                    """{ "value": ["3", "1", "99", "2", 1, 5, 9] }""",
                    null
                ),
                // Sort can be chained (i.e. return type is valid)
                Arguments.of(
                    "\$o = [ \"d\", \"a\", \"c\", \"b\" ];\n" +
                            "value: \$o | sort | sort( {order: \'desc\'} ) \n",
                    """{ "value": ["d", "c", "b", "a"] }""",
                    null
                ),
                // Sort 'by' does not crash when sorting an object
                Arguments.of(
                    "\$o = { b: 1, c:2, a: 3 };\n" + "value: \$o | sort( { by: \"a\" } )\n",
                    """{ "value": { "a": 3, "b": 1, "c": 2 } }""",
                    null
                ),
                // Sort array of object using 'by'
                Arguments.of(
                    "\$o = [{ b: 8, c: 3, a: \"anna\" }, { b: 5, c: 5, a: \"zoe\" }, { b: 2, c: 9, a: \"max\" }];\n" +
                            "value: \$o | sort( { by: \"a\" } )\n",
                    """{ "value": [{ "b": 8, "c": 3, "a": "anna" }, { "b": 2, "c": 9, "a": "max" }, { "b": 5, "c": 5, "a": "zoe" }] }""",
                    null
                ),
                // Sort array of object using 'by' - desc
                Arguments.of(
                    "\$o = [{ b: 8, c: 3, a: \"anna\" }, { b: 5, c: 5, a: \"zoe\" }, { b: 2, c: 9, a: \"max\" }];\n" +
                            "value: \$o | sort( { by: \"a\", order: \"desc\" } )\n",
                    """{ "value": [{ "b": 5, "c": 5, "a": "zoe" }, { "b": 2, "c": 9, "a": "max" }, { "b": 8, "c": 3, "a": "anna" }] }""",
                    null
                ),
                // Sort array of object using 'by', case-sensitive (default)
                Arguments.of(
                    "\$o = [{ a: \"max1\" }, { a: \"MAX2\" }, { a: \"max3\" }];\n" +
                            "value: \$o | sort( { by: \"a\", caseSensitive: true } )\n",
                    """{ "value": [{ "a": "MAX2" }, { "a": "max1" }, { "a": "max3" }] }""",
                    null
                ),
                // Sort array of object using 'by', not case-sensitive
                Arguments.of(
                    "\$o = [{ a: \"max1\" }, { a: \"MAX2\" }, { a: \"max3\" }];\n" +
                            "value: \$o | sort( { by: \"a\", caseSensitive: false } )\n",
                    """{ "value": [{ "a": "max1" }, { "a": "MAX2" }, { "a": "max3" }] }""",
                    null
                ),
                // Sort array of object using 'by' - some object may not have such a key, gets
                // sorted as `null`
                Arguments.of(
                    "\$o = [{ b: 8, c: 3, a: \"anna\" }, { b: 5, c: 5}, { b: 2, c: 9, a: \"max\" }];\n" +
                            "value: \$o | sort( { by: \"a\" } )\n",
                    """{ "value": [{ "b": 5, "c": 5 }, { "b": 8, "c": 3, "a": "anna" }, { "b": 2, "c": 9, "a": "max" }] }""",
                    null
                ),
                // Sort array of object using 'by' - detects booleans
                Arguments.of(
                    "\$o = [{ a: true }, { a: false }, { a: true }];\n" +
                            "value: \$o | sort( { by: \"a\" } )\n",
                    """{ "value": [{ "a": false }, { "a": true }, { "a": true }] }""",
                    null
                ),
                // Sort array of object using 'by' - detects numbers
                Arguments.of(
                    "\$o = [{ a: 99 }, { a: 5 }, { a: 15 }];\n" +
                            "value: \$o | sort( { by: \"a\" } )\n",
                    """{ "value": [{ "a": 5 }, { "a": 15 }, { "a": 99 }] }""",
                    null
                ),
                // Sort a string character by character - case-sensitive
                Arguments.of(
                    "\$o = \"hello I am a string\";\n" + "value: \$o | sort\n",
                    """{ "value": "    Iaaeghillmnorst" }""",
                    null
                ),
                // Sort a string character by character - case-insensitive
                Arguments.of(
                    "\$o = \"hello I am a string\";\n" +
                            "value: \$o | sort( { caseSensitive: false } )\n",
                    """{ "value": "    aaeghIillmnorst" }""",
                    null
                ),
                // Sort a string character by character - Empty string
                Arguments.of(
                    "\$o = \"\";\n" + "value: \$o | sort\n",
                    """{ "value": "" }""",
                    null
                ),
                // Sort a string character by character - desc
                Arguments.of(
                    "\$o = \"hello I am a string\";\n" +
                            "value: \$o | sort( {order:\"desc\"} )\n",
                    """{ "value": "tsronmllihgeaaI    " }""",
                    null
                ),
                // Passing unsupported data (e.g. a number) to |sort return null
                Arguments.of(
                    "\$o = 12345;\n" + "value: \$o | sort\n",
                    """{ "value": null }""",
                    null
                )
            )
        }
        
        @JvmStatic
        fun newMathModifiers(): Stream<Arguments> {
            return Stream.of(
                // Math.clamp
                Arguments.of("result: 5 | Math.clamp(0, 10)", """{"result":5}""", null),
                Arguments.of("result: -5 | Math.clamp(0, 10)", """{"result":0}""", null),
                Arguments.of("result: 15 | Math.clamp(0, 10)", """{"result":10}""", null),
                
                // Math.sum
                Arguments.of("result: @.Math.sum(1, 2, 3, 4, 5)", """{"result":15}""", null),
                Arguments.of("result: [10, 20, 30] | Math.sum", """{"result":60}""", null),
                
                // Math.pow
                Arguments.of("result: 2 | Math.pow(3)", """{"result":8.0}""", null),
                Arguments.of("result: 5 | Math.pow(2)", """{"result":25.0}""", null),
                
                // Math.ln (natural logarithm)
                Arguments.of("result: 2.718281828459045 | Math.ln", """{"result":1.0}""", null),
                
                // Math.log10
                Arguments.of("result: 100 | Math.log10", """{"result":2.0}""", null),
                Arguments.of("result: 1000 | Math.log10", """{"result":3.0}""", null),
                
                // Math.log (with base)
                Arguments.of("result: 8 | Math.log(2)", """{"result":3.0}""", null),
                Arguments.of("result: 27 | Math.log(3)", """{"result":3.0}""", null)
            )
        }
        
        @JvmStatic
        fun newStringModifiers(): Stream<Arguments> {
            return Stream.of(
                // padStart
                Arguments.of("result: 'hello' | padStart(10)", """{"result":"     hello"}""", null),
                Arguments.of("result: 'hello' | padStart(10, '0')", """{"result":"00000hello"}""", null),
                
                // padEnd
                Arguments.of("result: 'hello' | padEnd(10)", """{"result":"hello     "}""", null),
                Arguments.of("result: 'hello' | padEnd(10, '*')", """{"result":"hello*****"}""", null),
                
                // reverse
                Arguments.of("result: 'hello' | reverse", """{"result":"olleh"}""", null),
                
                // capitalize
                Arguments.of("result: 'hello world' | capitalize", """{"result":"Hello world"}""", null),
                
                // titleCase
                Arguments.of("result: 'hello world' | titleCase", """{"result":"Hello World"}""", null),
                
                // camelCase
                Arguments.of("result: 'hello world' | camelCase", """{"result":"helloWorld"}""", null),
                Arguments.of("result: 'hello-world-test' | camelCase", """{"result":"helloWorldTest"}""", null),
                Arguments.of("result: 'hello_world_test' | camelCase", """{"result":"helloWorldTest"}""", null),
                
                // snakeCase
                Arguments.of("result: 'helloWorld' | snakeCase", """{"result":"hello_world"}""", null),
                Arguments.of("result: 'hello world' | snakeCase", """{"result":"hello_world"}""", null),
                Arguments.of("result: 'hello-world' | snakeCase", """{"result":"hello_world"}""", null),
                
                // truncate
                Arguments.of("result: 'hello world this is long' | truncate(10)", """{"result":"hello w..."}""", null),
                Arguments.of("result: 'hello' | truncate(10)", """{"result":"hello"}""", null),
                Arguments.of("result: 'hello world' | truncate(8, '…')", """{"result":"hello w…"}""", null),
                
                // html.escape and html.unescape
                Arguments.of("result: '<div>Hello & \"World\"</div>' | html.escape", """{"result":"&lt;div&gt;Hello &amp; &quot;World&quot;&lt;/div&gt;"}""", null),
                Arguments.of("result: '&lt;div&gt;Hello&lt;/div&gt;' | html.unescape", """{"result":"<div>Hello</div>"}""", null)
            )
        }
        
        @JvmStatic
        fun newArrayModifiers(): Stream<Arguments> {
            return Stream.of(
                // first
                Arguments.of("result: [1, 2, 3, 4, 5] | first", """{"result":1}""", null),
                Arguments.of("result: [] | first", """{"result":null}""", null),
                
                // last
                Arguments.of("result: [1, 2, 3, 4, 5] | last", """{"result":5}""", null),
                Arguments.of("result: [] | last", """{"result":null}""", null),
                
                // take
                Arguments.of("result: [1, 2, 3, 4, 5] | take(3)", """{"result":[1,2,3]}""", null),
                Arguments.of("result: [1, 2, 3] | take(10)", """{"result":[1,2,3]}""", null),
                Arguments.of("result: [1, 2, 3, 4, 5] | take(0)", """{"result":[]}""", null),
                
                // drop
                Arguments.of("result: [1, 2, 3, 4, 5] | drop(2)", """{"result":[3,4,5]}""", null),
                Arguments.of("result: [1, 2, 3] | drop(10)", """{"result":[]}""", null),
                Arguments.of("result: [1, 2, 3, 4, 5] | drop(0)", """{"result":[1,2,3,4,5]}""", null),
                
                // indexOf
                Arguments.of("result: [1, 2, 3, 2, 1] | indexOf(2)", """{"result":1}""", null),
                Arguments.of("result: [1, 2, 3] | indexOf(5)", """{"result":-1}""", null),
                
                // lastIndexOf
                Arguments.of("result: [1, 2, 3, 2, 1] | lastIndexOf(2)", """{"result":3}""", null),
                Arguments.of("result: [1, 2, 3] | lastIndexOf(5)", """{"result":-1}""", null),
                
                // chunk
                Arguments.of("result: [1, 2, 3, 4, 5, 6] | chunk(2)", """{"result":[[1,2],[3,4],[5,6]]}""", null),
                Arguments.of("result: [1, 2, 3, 4, 5] | chunk(2)", """{"result":[[1,2],[3,4],[5]]}""", null),
                Arguments.of("result: [1, 2, 3] | chunk(10)", """{"result":[[1,2,3]]}""", null)
            )
        }
        
        @JvmStatic
        fun newObjectModifiers(): Stream<Arguments> {
            return Stream.of(
                // pick
                Arguments.of("result: { a: 1, b: 2, c: 3 } | pick('a', 'c')", """{"result":{"a":1,"c":3}}""", null),
                Arguments.of("result: { a: 1, b: 2 } | pick('a')", """{"result":{"a":1}}""", null),
                
                // omit
                Arguments.of("result: { a: 1, b: 2, c: 3 } | omit('b')", """{"result":{"a":1,"c":3}}""", null),
                Arguments.of("result: { a: 1, b: 2, c: 3 } | omit('a', 'c')", """{"result":{"b":2}}""", null),
                
                // rename
                Arguments.of("result: { a: 1, b: 2 } | rename('a', 'x')", """{"result":{"x":1,"b":2}}""", null),
                Arguments.of("result: { a: 1, b: 2 } | rename('c', 'x')", """{"result":{"a":1,"b":2}}""", null),
                
                // has
                Arguments.of("result: { a: 1, b: 2 } | has('a')", """{"result":true}""", null),
                Arguments.of("result: { a: 1, b: 2 } | has('c')", """{"result":false}""", null),
                
                // default
                Arguments.of("result: null | default('N/A')", """{"result":"N/A"}""", null),
                Arguments.of("result: '' | default('Empty')", """{"result":"Empty"}""", null),
                Arguments.of("result: 'Hello' | default('N/A')", """{"result":"Hello"}""", null),
                Arguments.of("result: [] | default(['default'])", """{"result":["default"]}""", null),
                Arguments.of("result: {} | default({ default: true })", """{"result":{"default":true}}""", null),
                Arguments.of("result: 0 | default(999)", """{"result":0}""", null),
                Arguments.of("result: false | default(true)", """{"result":false}""", null)
            )
        }
        
        @JvmStatic
        fun newConversionModifiers(): Stream<Arguments> {
            return Stream.of(
                // to.json
                Arguments.of("result: { a: 1, b: 2 } | to.json", """{"result":"{\"a\":1,\"b\":2}"}""", null),
                Arguments.of("result: [1, 2, 3] | to.json", """{"result":"[1,2,3]"}""", null),
                
                // to.yaml
                Arguments.of("result: { a: 1, b: 2 } | to.yaml", """{"result":"---\na: 1\nb: 2\n"}""", null),
                
                // to.csv
                Arguments.of("result: [{name: 'John', age: 30}, {name: 'Jane', age: 25}] | to.csv", 
                    """{"result":"name,age\nJohn,30\nJane,25\n"}""", null),
                Arguments.of("result: [] | to.csv", """{"result":""}""", null)
            )
        }
    }

    @ParameterizedTest
    @MethodSource(
        "simpleModifiers",
        "arrayModifiers",
        "conversionModifiers",
        "mathModifiers",
        "selectModifier",
        "sortModifier",
        "propertyManagement",
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        run(script, expectedResult, map)
    }
    
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("newMathModifiers")
    fun testNewMathModifiers(script: String, expected: String, error: String?) = run(script, expected)
    
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("newStringModifiers")
    fun testNewStringModifiers(script: String, expected: String, error: String?) = run(script, expected)
    
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("newArrayModifiers")
    fun testNewArrayModifiers(script: String, expected: String, error: String?) = run(script, expected)
    
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("newObjectModifiers")
    fun testNewObjectModifiers(script: String, expected: String, error: String?) = run(script, expected)
    
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("newConversionModifiers")
    fun testNewConversionModifiers(script: String, expected: String, error: String?) = run(script, expected)

    @ParameterizedTest
    @MethodSource(
        "sortModifier_Strict",
    )
    fun runFixturesStrict(script: String, expectedResult: String, map: Map<String, Any?>? = null) {

        run(script, expectedResult, map, assertEqualityType = AssertEqualityType.Text)
    }

    override fun onRegisterExtensions(context: OperationContext) {
        context.registerExtensionMethod("Modifier.mask", this::mask)

        super.onRegisterExtensions(context)
    }

    private fun mask(context: FunctionExecuteContext): Any? {
        return ConvertUtils.tryToString(context.firstParameter)
    }
}
