package com.intuit.isl.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertContentEquals

class ParseSimpleJsonPathTest {

    @Test
    fun `should parse simple dot notation paths`() {
        // Single property
        assertContentEquals(
            arrayOf("foo"),
            parseSimpleJsonPath("$.foo")
        )

        // Multiple properties
        assertContentEquals(
            arrayOf("foo", "bar"),
            parseSimpleJsonPath("$.foo.bar")
        )

        // Complex nested path
        assertContentEquals(
            arrayOf("current_subtotal_price_set", "shop_money", "amount"),
            parseSimpleJsonPath("$.current_subtotal_price_set.shop_money.amount")
        )

        // With @ instead of $
        assertContentEquals(
            arrayOf("foo", "bar"),
            parseSimpleJsonPath("@.foo.bar")
        )
    }

    @Test
    fun `should parse bracket notation paths`() {
        // Single property
        assertContentEquals(
            arrayOf("foo"),
            parseSimpleJsonPath("\$['foo']")
        )

        // Multiple properties
        assertContentEquals(
            arrayOf("foo", "bar"),
            parseSimpleJsonPath("\$['foo']['bar']")
        )

        // Properties with special characters
        assertContentEquals(
            arrayOf("current_subtotal_price_set", "shop_money", "amount"),
            parseSimpleJsonPath("\$['current_subtotal_price_set']['shop_money']['amount']")
        )
    }

    @Test
    fun `should return null for array accessors`() {
        assertNull(parseSimpleJsonPath("$.foo[0]"))
        assertNull(parseSimpleJsonPath("$.foo[0].bar"))
        assertNull(parseSimpleJsonPath("$.foo.bar[1]"))
        assertNull(parseSimpleJsonPath("$[0]"))
    }

    @Test
    fun `should return null for wildcards`() {
        assertNull(parseSimpleJsonPath("$.foo.*"))
        assertNull(parseSimpleJsonPath("$.*.bar"))
        assertNull(parseSimpleJsonPath("$.*"))
        assertNull(parseSimpleJsonPath("$.foo[*]"))
    }

    @Test
    fun `should return null for recursive descent`() {
        assertNull(parseSimpleJsonPath("$..foo"))
        assertNull(parseSimpleJsonPath("$.foo..bar"))
        assertNull(parseSimpleJsonPath("$.."))
    }

    @Test
    fun `should return null for filter expressions`() {
        assertNull(parseSimpleJsonPath("$.foo[?(@.price > 10)]"))
        assertNull(parseSimpleJsonPath("$[?(@.price > 10)]"))
        assertNull(parseSimpleJsonPath("$.foo[?(@.name == 'test')]"))
    }

    @Test
    fun `should return null for functions`() {
        assertNull(parseSimpleJsonPath("$.foo.length()"))
        assertNull(parseSimpleJsonPath("$.foo.min()"))
        assertNull(parseSimpleJsonPath("$.foo.max()"))
    }

    @Test
    fun `should return null for array slice notation`() {
        assertNull(parseSimpleJsonPath("$.foo[0:5]"))
        assertNull(parseSimpleJsonPath("$.foo[1:10:2]"))
        assertNull(parseSimpleJsonPath("$.foo[-1:]"))
    }

    @Test
    fun `should return null for invalid paths`() {
        assertNull(parseSimpleJsonPath(""))
        assertNull(parseSimpleJsonPath("   "))
        assertNull(parseSimpleJsonPath("foo.bar")) // Missing $
        assertNull(parseSimpleJsonPath("$.")) // Incomplete
        assertNull(parseSimpleJsonPath("$..")) // Recursive descent
    }

    @Test
    fun `should handle edge cases`() {
        // Property names with underscores
        assertContentEquals(
            arrayOf("foo_bar", "baz_qux"),
            parseSimpleJsonPath("$.foo_bar.baz_qux")
        )

        // Property names with numbers
        assertContentEquals(
            arrayOf("foo123", "bar456"),
            parseSimpleJsonPath("$.foo123.bar456")
        )

        // Single character properties
        assertContentEquals(
            arrayOf("a", "b", "c"),
            parseSimpleJsonPath("$.a.b.c")
        )
    }

    @Test
    fun `should return null for mixed notation`() {
        // We reject mixed notation for simplicity
        assertNull(parseSimpleJsonPath("$.foo['bar'].baz"))
        assertNull(parseSimpleJsonPath("\$['foo'].bar['baz']"))
    }

    @Test
    fun `should handle whitespace`() {
        assertContentEquals(
            arrayOf("foo", "bar"),
            parseSimpleJsonPath("  $.foo.bar  ")
        )
    }
}

