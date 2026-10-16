package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/**
 * A JSON number kept as its source text, so integers and fractions stay distinguishable like in the Python validator
 * of Shared-Assets (`type(value) is int` rejects `1.0` and booleans).
 * zh-CN: 保留源文本的 JSON 数字, 使整数与小数可以区分, 与 Shared-Assets 的 Python 校验器一致 (`type(value) is int` 拒绝 `1.0` 与布尔值).
 */
class JsonNumber(val text: String) {

    /** Whether the literal has neither a fraction nor an exponent. zh-CN: 字面量既无小数也无指数部分. */
    val isInteger: Boolean = text.none { it == '.' || it == 'e' || it == 'E' }

    /** The integer value, or null for fractions and values beyond [Long]. zh-CN: 整数值; 小数或超出 [Long] 时为 null. */
    fun longOrNull(): Long? = if (isInteger) text.toLongOrNull() else null

    fun toDouble(): Double = text.toDouble()

    override fun equals(other: Any?): Boolean = other is JsonNumber && other.text == text

    override fun hashCode(): Int = text.hashCode()

    override fun toString(): String = text
}

/** Malformed or unsafe JSON. zh-CN: 格式错误或不安全的 JSON. */
class StrictJsonException(message: String) : Exception(message)

/**
 * Strict RFC 8259 reader and a small writer for the catalog, signature envelope and store files: UTF-8 without BOM,
 * an object root, no duplicate keys, finite numbers only, no lone surrogates and bounded nesting. Objects become
 * [LinkedHashMap] in source order, arrays [List], numbers [JsonNumber]. Pure Kotlin for JVM tests.
 * zh-CN: 目录, 签名信封与存储文件使用的严格 RFC 8259 读取器及小型写入器: 无 BOM 的 UTF-8, 根为对象, 不允许重复键, 只允许有限数字,
 * 不允许孤立代理项, 嵌套深度有上限. 对象解析为保持源顺序的 [LinkedHashMap], 数组为 [List], 数字为 [JsonNumber]. 纯 Kotlin, 供 JVM 测试.
 */
object StrictJson {

    private const val MAX_DEPTH = 64

    @Throws(StrictJsonException::class)
    fun parseObject(bytes: ByteArray): Map<String, Any?> {
        val text = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (e: CharacterCodingException) {
            throw StrictJsonException("JSON is not valid UTF-8")
        }
        if (text.isNotEmpty() && text[0].code == 0xFEFF) throw StrictJsonException("JSON must not start with a byte order mark")
        val reader = Reader(text)
        reader.skipWhitespace()
        val value = reader.readValue(0)
        reader.skipWhitespace()
        if (!reader.atEnd()) throw StrictJsonException("Trailing data after the JSON value at ${reader.position}")
        @Suppress("UNCHECKED_CAST")
        return value as? Map<String, Any?> ?: throw StrictJsonException("JSON root must be an object")
    }

    /** Compact UTF-8 JSON with keys in map order. zh-CN: 键按映射顺序输出的紧凑 UTF-8 JSON. */
    fun write(value: Any?): ByteArray = StringBuilder().also { appendValue(it, value) }.append('\n').toString().toByteArray(Charsets.UTF_8)

    private fun appendValue(out: StringBuilder, value: Any?) {
        when (value) {
            null -> out.append("null")
            is Boolean -> out.append(value)
            is Int, is Long, is Short, is Byte, is BigInteger -> out.append(value.toString())
            is JsonNumber -> out.append(value.text)
            is Double -> {
                require(value.isFinite()) { "Non-finite numbers are not JSON" }
                out.append(value.toString())
            }
            is String -> appendString(out, value)
            is Map<*, *> -> {
                out.append('{')
                var first = true
                value.forEach { (key, item) ->
                    if (!first) out.append(',')
                    first = false
                    appendString(out, key as String)
                    out.append(':')
                    appendValue(out, item)
                }
                out.append('}')
            }
            is Iterable<*> -> {
                out.append('[')
                var first = true
                value.forEach { item ->
                    if (!first) out.append(',')
                    first = false
                    appendValue(out, item)
                }
                out.append(']')
            }
            else -> throw IllegalArgumentException("Unsupported JSON value: ${value::class.java.name}")
        }
    }

    private fun appendString(out: StringBuilder, value: String) {
        out.append('"')
        for (char in value) {
            when {
                char == '"' -> out.append("\\\"")
                char == '\\' -> out.append("\\\\")
                char == '\n' -> out.append("\\n")
                char == '\r' -> out.append("\\r")
                char == '\t' -> out.append("\\t")
                char.code < 0x20 -> out.append("\\u").append(String.format("%04x", char.code))
                else -> out.append(char)
            }
        }
        out.append('"')
    }

    private class Reader(private val text: String) {

        var position = 0
            private set

        fun atEnd(): Boolean = position >= text.length

        fun skipWhitespace() {
            while (position < text.length) {
                when (text[position]) {
                    ' ', '\t', '\n', '\r' -> position++
                    else -> return
                }
            }
        }

        fun readValue(depth: Int): Any? {
            if (depth > MAX_DEPTH) fail("JSON nesting is too deep")
            if (atEnd()) fail("Unexpected end of JSON")
            return when (val char = text[position]) {
                '{' -> readObject(depth)
                '[' -> readArray(depth)
                '"' -> readString()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> if (char == '-' || char in '0'..'9') readNumber() else fail("Unexpected character")
            }
        }

        private fun readObject(depth: Int): Map<String, Any?> {
            position++
            val result = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                position++
                return result
            }
            while (true) {
                skipWhitespace()
                if (peek() != '"') fail("Object keys must be strings")
                val key = readString()
                if (result.containsKey(key)) fail("Duplicate JSON key: $key")
                skipWhitespace()
                expect(':')
                skipWhitespace()
                result[key] = readValue(depth + 1)
                skipWhitespace()
                when (next()) {
                    ',' -> continue
                    '}' -> return result
                    else -> fail("Expected ',' or '}'")
                }
            }
        }

        private fun readArray(depth: Int): List<Any?> {
            position++
            val result = ArrayList<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                position++
                return result
            }
            while (true) {
                skipWhitespace()
                result += readValue(depth + 1)
                skipWhitespace()
                when (next()) {
                    ',' -> continue
                    ']' -> return result
                    else -> fail("Expected ',' or ']'")
                }
            }
        }

        private fun readString(): String {
            expect('"')
            val out = StringBuilder()
            while (true) {
                val char = next()
                when {
                    char == '"' -> break
                    char == '\\' -> when (val escape = next()) {
                        '"' -> out.append('"')
                        '\\' -> out.append('\\')
                        '/' -> out.append('/')
                        'b' -> out.append('\b')
                        'f' -> out.append(Char(0x0C))
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'u' -> out.append(readHexChar())
                        else -> fail("Invalid escape \\$escape")
                    }
                    char.code < 0x20 -> fail("Control characters must be escaped in JSON strings")
                    else -> out.append(char)
                }
            }
            return checkSurrogates(out.toString())
        }

        private fun readHexChar(): Char {
            if (position + 4 > text.length) fail("Truncated unicode escape")
            val value = text.substring(position, position + 4).toIntOrNull(16) ?: fail("Invalid unicode escape")
            position += 4
            return Char(value)
        }

        private fun checkSurrogates(value: String): String {
            var index = 0
            while (index < value.length) {
                val char = value[index]
                if (char.isHighSurrogate()) {
                    if (index + 1 >= value.length || !value[index + 1].isLowSurrogate()) fail("Lone surrogate in JSON string")
                    index += 2
                    continue
                }
                if (char.isLowSurrogate()) fail("Lone surrogate in JSON string")
                index++
            }
            return value
        }

        private fun readNumber(): JsonNumber {
            val start = position
            if (peek() == '-') position++
            when {
                peek() == '0' -> position++
                peek() in '1'..'9' -> while (peek() in '0'..'9') position++
                else -> fail("Invalid number")
            }
            if (peek() == '.') {
                position++
                if (peek() !in '0'..'9') fail("Invalid fraction")
                while (peek() in '0'..'9') position++
            }
            if (peek() == 'e' || peek() == 'E') {
                position++
                if (peek() == '+' || peek() == '-') position++
                if (peek() !in '0'..'9') fail("Invalid exponent")
                while (peek() in '0'..'9') position++
            }
            val number = JsonNumber(text.substring(start, position))
            if (!number.isInteger && !number.toDouble().isFinite()) fail("Non-finite JSON number")
            return number
        }

        private fun literal(word: String, value: Any?): Any? {
            if (!text.startsWith(word, position)) fail("Invalid literal")
            position += word.length
            return value
        }

        private fun peek(): Char = if (position < text.length) text[position] else Char(0)

        private fun next(): Char {
            if (position >= text.length) fail("Unexpected end of JSON")
            return text[position++]
        }

        private fun expect(char: Char) {
            if (next() != char) fail("Expected '$char'")
        }

        private fun fail(message: String): Nothing = throw StrictJsonException("$message at offset $position")
    }
}
