package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The strict JSON reader rejects what Shared-Assets' `distribution.parse_json` rejects (duplicate keys, non-finite
 * numbers, a non-object root, invalid UTF-8) and a little more (byte order mark, lone surrogates), and keeps integers
 * apart from fractions.
 * zh-CN: 严格 JSON 读取器拒绝 Shared-Assets `distribution.parse_json` 拒绝的内容 (重复键, 非有限数, 非对象根, 非法 UTF-8), 另外拒绝 BOM
 * 与孤立代理项, 并区分整数与小数.
 */
class StrictJsonTest {

    private fun parse(text: String) = StrictJson.parseObject(text.toByteArray(Charsets.UTF_8))

    @Test
    fun `objects keep their order and numbers keep integer and fraction apart`() {
        // JSON unicode escapes, assembled so that no tool rewrites them: e-acute and a surrogate pair (U+1F600).
        val value = parse("""{"b": 1, "a": [true, false, null, "x${BS}n${BS}u00e9${BS}ud83d${BS}ude00"], "c": 1.0, "d": -0, "e": 1e3, "f": {}}""")
        assertEquals(listOf("b", "a", "c", "d", "e", "f"), value.keys.toList())
        assertEquals(1L, (value["b"] as JsonNumber).longOrNull())
        assertFalse((value["c"] as JsonNumber).isInteger)
        assertEquals(null, (value["c"] as JsonNumber).longOrNull())
        assertFalse((value["e"] as JsonNumber).isInteger)
        val list = value["a"] as List<*>
        assertEquals(listOf(true, false, null), list.take(3))
        assertEquals(listOf('x'.code, '\n'.code, 0xE9, 0xD83D, 0xDE00), (list[3] as String).map { it.code })
    }

    @Test
    fun `duplicate keys, non-finite numbers and malformed input are refused`() {
        listOf(
            """{"a": 1, "a": 2}""",
            """{"a": 1e400}""",
            """{"a": NaN}""",
            """{"a": Infinity}""",
            """[1, 2]""",
            """{"a": 1,}""",
            """{"a": 01}""",
            """{"a": "unterminated}""",
            """{"a": 1} trailing""",
            """{"a": "${BS}ud800"}""",
            """{"a": "${BS}udc00x"}""",
            """{"a": "${BS}x"}""",
            "{\"a\": \"tab\tinside\"}",
            "",
        ).forEach { text -> assertThrows(text, StrictJsonException::class.java) { parse(text) } }
    }

    @Test
    fun `a byte order mark and invalid UTF-8 are refused`() {
        assertThrows(StrictJsonException::class.java) { StrictJson.parseObject(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte(), '{'.code.toByte(), '}'.code.toByte())) }
        assertThrows(StrictJsonException::class.java) { StrictJson.parseObject(byteArrayOf('{'.code.toByte(), '"'.code.toByte(), 0xC3.toByte(), '"'.code.toByte())) }
    }

    @Test
    fun `nesting is bounded`() {
        val deep = "{\"a\":" + "[".repeat(100) + "]".repeat(100) + "}"
        assertThrows(StrictJsonException::class.java) { parse(deep) }
    }

    @Test
    fun `the writer round trips through the reader`() {
        val value = linkedMapOf("name" to "a\"b\\c\n", "n" to 5L, "list" to listOf(1, true, null), "nested" to mapOf("k" to "v"))
        val parsed = StrictJson.parseObject(StrictJson.write(value))
        assertEquals("a\"b\\c\n", parsed["name"])
        assertEquals(5L, (parsed["n"] as JsonNumber).longOrNull())
        assertTrue(StrictJson.write(value).last() == '\n'.code.toByte())
    }

    private companion object {
        const val BS = "\\"
    }
}
