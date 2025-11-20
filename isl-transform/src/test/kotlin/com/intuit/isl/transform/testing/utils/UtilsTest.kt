package com.intuit.isl.transform.testing.utils

import com.fasterxml.jackson.databind.node.BinaryNode
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.TextNode
import com.intuit.isl.commands.NoopToken
import com.intuit.isl.runtime.TransformCompilationException
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.utils.indexOrDefault
import com.intuit.isl.utils.justOne
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.test.assertEquals

class UtilsTest {
    @Test
    fun convertValue() {
        assertEquals("abc", (JsonConvert.convert("abc") as TextNode).textValue());
        assertEquals(123.toShort(), (JsonConvert.convert(123.toShort()) as NumericNode).shortValue());
        assertEquals(BigInteger.TEN, (JsonConvert.convert(BigInteger.TEN) as NumericNode).bigIntegerValue());
        assertEquals(123.456.toDouble(), (JsonConvert.convert(123.456.toDouble()) as NumericNode).doubleValue());
        assertEquals(123.456.toFloat(), (JsonConvert.convert(123.456.toFloat()) as NumericNode).floatValue());
    }

    @Test
    fun convertUtils_tryToString(){
        assertEquals("abc", ConvertUtils.tryToString("abc"));
        assertEquals("abc", ConvertUtils.tryToString(JsonConvert.convert("abc") as TextNode));
        assertEquals("123", ConvertUtils.tryToString(JsonConvert.convert(123.toShort()) as NumericNode));
        assertEquals(BigInteger.TEN.toString(), ConvertUtils.tryToString((JsonConvert.convert(BigInteger.TEN) as NumericNode)));
        assertEquals(123.456.toString(), ConvertUtils.tryToString(JsonConvert.convert(123.456.toDouble()) as DoubleNode));
        assertEquals(123.456.toString(), ConvertUtils.tryToString(JsonConvert.convert(123.456.toBigDecimal()) as DecimalNode));
        assertEquals("test", ConvertUtils.tryToString(JsonConvert.convert("test".toByteArray()) as BinaryNode));
        assertEquals("test", ConvertUtils.tryToString("test".toByteArray()));

        // Encoding
        assertEquals("��\u0000�\u0000�", ConvertUtils.tryToString("åß".toByteArray(Charsets.UTF_16)));
        assertEquals("åß", ConvertUtils.tryToString("åß", Charsets.UTF_8));
        assertEquals("åß", ConvertUtils.tryToString("åß".toByteArray(Charsets.UTF_16), Charsets.UTF_16));
        assertEquals("åß", ConvertUtils.tryToString("åß".toByteArray(Charsets.UTF_16BE), Charsets.UTF_16BE));
        assertEquals("åß", ConvertUtils.tryToString("åß".toByteArray(Charsets.UTF_16LE), Charsets.UTF_16LE));
        assertEquals("åß", ConvertUtils.tryToString("åß".toByteArray(Charsets.UTF_32), Charsets.UTF_32));
        assertEquals("åß", ConvertUtils.tryToString("åß".toByteArray(Charsets.UTF_32BE), Charsets.UTF_32BE));
        assertEquals("åß", ConvertUtils.tryToString("åß".toByteArray(Charsets.UTF_32LE), Charsets.UTF_32LE));
        assertEquals("åß", ConvertUtils.tryToString("åß".toByteArray(Charsets.ISO_8859_1), Charsets.ISO_8859_1));
        assertEquals("??", ConvertUtils.tryToString("åß".toByteArray(Charsets.US_ASCII), Charsets.US_ASCII));
    }

    @Test
    fun getValue() {
        assertEquals(123, JsonConvert.getValue(JsonNodeFactory.instance.numberNode(123)) as Int);
        assertEquals("abc", JsonConvert.getValue(JsonNodeFactory.instance.textNode("abc")) as String);
        assertEquals(true, JsonConvert.getValue(JsonNodeFactory.instance.booleanNode(true)) as Boolean);
    }

    @Test
    fun length() {
        assertEquals(0, JsonConvert.length(null));
        assertEquals(3, JsonConvert.length("abc"));
        assertEquals(3, JsonConvert.length(arrayListOf(1, 2, 3)));
        assertEquals(3, JsonConvert.length(arrayListOf(1, 2, 3).toTypedArray()));
        assertEquals(3, JsonConvert.length(arrayListOf(1, 2, 3).toList()));

        val ja = JsonNodeFactory.instance.arrayNode();
        ja.add(1);ja.add(1);
        assertEquals(2, JsonConvert.length(ja));

        val m = mapOf<String, String>("a" to "b")
        assertEquals(1, JsonConvert.length(m));
    }

    @Test
    fun indexOrDefault() {
        val ar = arrayOf(1, 2, 3);
        assertEquals(1, ar.indexOrDefault(0));
        assertEquals(3, ar.indexOrDefault(2));
        assertEquals(null, ar.indexOrDefault(-1));
        assertEquals(null, ar.indexOrDefault(3));
    }

    @Test
    fun justOne() {
        assertThrows(TransformCompilationException::class.java) {
            listOf<Int>().justOne(NoopToken(), "");
        }
        assertThrows(TransformCompilationException::class.java) {
            listOf<Int>(1, 2).justOne(NoopToken(), "");
        }
        assertEquals(2, listOf<Int>(2).justOne(NoopToken(), ""));
    }
}