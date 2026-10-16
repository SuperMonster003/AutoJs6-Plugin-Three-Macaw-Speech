package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * The plugin's bzip2 decoder against streams written by libbzip2 (Python's bz2 module, `.python/generate_model_fixtures.py`):
 * several blocks at level 1, runs past the 4 + 255 limit of the first run-length stage, a 1 MiB run, every byte value,
 * concatenated streams and an empty stream; damaged streams fail with an IOException instead of returning wrong bytes.
 * zh-CN: 插件的 bzip2 解码器对照 libbzip2 (Python bz2 模块) 写出的数据流: level 1 的多个块, 超过第一级游程 4 + 255 上限的游程, 1 MiB
 * 的游程, 所有字节值, 拼接的多个流与空流; 损坏的流以 IOException 失败, 不会返回错误的字节.
 */
class Bzip2InputStreamTest {

    private val vectors = StrictJson.parseObject(Fixtures.bytes("bz2-vectors.json"))

    @Test
    fun `every libbzip2 vector decodes to its recorded size and SHA-256`() {
        assertEquals(6, vectors.size)
        vectors.forEach { (name, value) ->
            val expected = value as Map<*, *>
            val (size, sha) = decode(Fixtures.bytes("bz2/$name"))
            assertEquals(name, (expected["size"] as JsonNumber).longOrNull(), size)
            assertEquals(name, expected["sha256"], sha)
        }
    }

    @Test
    fun `single byte reads and bulk reads agree`() {
        val bytes = Fixtures.bytes("bz2/long-runs.bz2")
        val single = Bzip2InputStream(ByteArrayInputStream(bytes)).use { input ->
            generateSequence { input.read().takeIf { it >= 0 } }.map { it.toByte() }.toList().toByteArray()
        }
        val bulk = Bzip2InputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
        assertEquals(bulk.toList(), single.toList())
        assertEquals((vectors.getValue("long-runs.bz2") as Map<*, *>)["sha256"], sha256(bulk))
    }

    @Test
    fun `a truncated stream fails`() {
        val bytes = Fixtures.bytes("bz2/random-all-bytes.bz2")
        listOf(10, bytes.size / 2, bytes.size - 3).forEach { cut ->
            assertThrows("cut at $cut", IOException::class.java) { decode(bytes.copyOf(cut)) }
        }
    }

    @Test
    fun `flipped bits fail a CRC or the structure checks`() {
        val bytes = Fixtures.bytes("bz2/text-level1-multiblock.bz2")
        listOf(40, 1000, bytes.size / 2, bytes.size - 20).forEach { at ->
            val damaged = bytes.copyOf().also { it[at] = (it[at].toInt() xor 0x10).toByte() }
            assertThrows("flip at $at", IOException::class.java) { decode(damaged) }
        }
    }

    @Test
    fun `data that is not bzip2 is refused and trailing garbage after a stream is ignored`() {
        assertThrows(IOException::class.java) { decode("not bzip2 at all".toByteArray()) }
        assertThrows(IOException::class.java) { decode(ByteArray(0)) }
        val stream = Fixtures.bytes("bz2/long-runs.bz2")
        val (size, sha) = decode(stream + "garbage".toByteArray())
        val expected = vectors.getValue("long-runs.bz2") as Map<*, *>
        assertEquals((expected["size"] as JsonNumber).longOrNull(), size)
        assertEquals(expected["sha256"], sha)
    }

    private fun decode(bytes: ByteArray): Pair<Long, String> {
        val digest = MessageDigest.getInstance("SHA-256")
        var total = 0L
        Bzip2InputStream(ByteArrayInputStream(bytes)).use { input ->
            val buffer = ByteArray(7919)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
                total += read
            }
        }
        return total to digest.digest().toHex()
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
}
