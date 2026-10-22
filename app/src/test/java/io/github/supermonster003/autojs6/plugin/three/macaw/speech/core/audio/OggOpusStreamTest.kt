package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The plugin's own Ogg Opus reader of roadmap D81 (P7.5.3): the ffmpeg recordings of `locks/encoded-test-audio.lock`
 * (checksums, header, packet durations), packets across pages, multiplexed streams, damage and truncation, the
 * identification header rules, the codec specific data and the packet durations of RFC 6716.
 * zh-CN: 路线图 D81 插件自带的 Ogg Opus 读取器 (P7.5.3): `locks/encoded-test-audio.lock` 中 ffmpeg 生成的录音 (校验和, 头, 包时长), 跨页的包,
 * 多路复用的流, 损坏与截断, 识别头规则, 编解码器专用数据, 以及 RFC 6716 的包时长.
 */
class OggOpusStreamTest {

    @Test
    fun `the ffmpeg recordings parse with every checksum`() {
        listOf("en.opus" to (1 to 6.625), "zh.opus" to (2 to 5.1)).forEach { (name, expected) ->
            val (channels, seconds) = expected
            OggOpusStream(File(AudioContainersTest.ENCODED, name).inputStream()).use { stream ->
                assertEquals(name, channels, stream.head.channels)
                assertEquals(name, 0, stream.head.mappingFamily)
                assertEquals(name, 312, stream.head.preSkip)
                var packets = 0
                var last = -1L
                while (true) {
                    val packet = stream.next() ?: break
                    assertTrue(packet.timeMicros > last)
                    last = packet.timeMicros
                    packets++
                }
                val decoded = (stream.millis - 312 * 1000L / 48000) / 1000.0
                // The last packet is padded up to its 20 ms frame.
                assertTrue("$name: $decoded s", decoded >= seconds && decoded <= seconds + 0.025)
                assertTrue("$name: $packets packets", packets > 100)
            }
        }
    }

    @Test
    fun `packets join across pages and the tags are skipped`() {
        val long = ByteArray(600) { if (it == 0) 0xF8.toByte() else (it % 251).toByte() }
        val bytes = Ogg(7).apply {
            page(BOS, head())
            page(0, tags())
            page(0, packet(10), long.copyOfRange(0, 510), complete = false)
            continued(long.copyOfRange(510, 600), packet(20))
            page(EOS, packet(30))
        }.bytes()
        OggOpusStream(ByteArrayInputStream(bytes)).use { stream ->
            assertEquals(1, stream.head.channels)
            assertArrayEquals(packet(10), stream.next()!!.data)
            assertArrayEquals(long, stream.next()!!.data)
            assertArrayEquals(packet(20), stream.next()!!.data)
            val third = stream.next()!!
            assertArrayEquals(packet(30), third.data)
            // Every test packet is one 20 ms CELT frame (config 31, one frame).
            assertEquals(60_000L, third.timeMicros)
            assertNull(stream.next())
            assertEquals(80L, stream.millis)
        }
    }

    @Test
    fun `a 255 byte packet ends with a zero lacing value`() {
        val exact = ByteArray(255) { 0x7C }
        val bytes = Ogg(1).apply {
            page(BOS, head())
            page(0, tags(), exact, packet(1))
        }.bytes()
        OggOpusStream(ByteArrayInputStream(bytes)).use { stream ->
            assertArrayEquals(exact, stream.next()!!.data)
            assertArrayEquals(packet(1), stream.next()!!.data)
            assertNull(stream.next())
        }
    }

    @Test
    fun `other logical streams are skipped`() {
        val vorbis = Ogg(1)
        val opus = Ogg(2)
        val bytes = ByteArrayOutputStream().apply {
            write(vorbis.apply { page(BOS, byteArrayOf(1) + "vorbis".toByteArray() + ByteArray(23)) }.bytes())
            write(opus.apply { page(BOS, head(channels = 2)) }.bytes())
            write(vorbis.apply { page(0, ByteArray(40)) }.bytes())
            write(opus.apply { page(0, tags()) }.bytes())
            write(vorbis.apply { page(0, ByteArray(80)) }.bytes())
            write(opus.apply { page(EOS, packet(5)) }.bytes())
            write(vorbis.apply { page(EOS, ByteArray(9)) }.bytes())
        }.toByteArray()
        OggOpusStream(ByteArrayInputStream(bytes)).use { stream ->
            assertEquals(2, stream.head.channels)
            assertArrayEquals(packet(5), stream.next()!!.data)
            assertNull(stream.next())
        }
    }

    @Test
    fun `a missing tags packet is audio and a truncated file ends quietly`() {
        val ogg = Ogg(3).apply {
            page(BOS, head())
            page(0, packet(1), packet(2))
            page(0, packet(3))
        }
        val bytes = ogg.bytes()
        OggOpusStream(ByteArrayInputStream(bytes.copyOf(bytes.size - 4))).use { stream ->
            assertArrayEquals(packet(1), stream.next()!!.data)
            assertArrayEquals(packet(2), stream.next()!!.data)
            assertNull(stream.next())
        }
    }

    @Test
    fun `damage is UNSUPPORTED_FORMAT`() {
        val good = Ogg(4).apply {
            page(BOS, head())
            page(0, tags())
            page(0, packet(1))
        }.bytes()
        val flipped = good.copyOf().also { it[it.size - 3] = (it[it.size - 3].toInt() xor 0x40).toByte() }
        val checksum = assertThrows(SpeechException::class.java) { OggOpusStream(ByteArrayInputStream(flipped)).use { while (it.next() != null) Unit } }
        assertEquals(SpeechErrorCodes.UNSUPPORTED_FORMAT, checksum.code)
        assertTrue(checksum.message, checksum.message!!.contains("checksum"))
        val lost = good.copyOf(good.size + 30).also { "Oggs".toByteArray().copyInto(it, good.size) }
        val capture = assertThrows(SpeechException::class.java) { OggOpusStream(ByteArrayInputStream(lost)).use { while (it.next() != null) Unit } }
        assertTrue(capture.message, capture.message!!.contains("capture pattern"))
        val noOpus = Ogg(5).apply { page(BOS, byteArrayOf(1) + "vorbis".toByteArray()) }.bytes() + Ogg(5).apply { page(0, packet(1)) }.bytes()
        assertEquals(SpeechErrorCodes.UNSUPPORTED_FORMAT, assertThrows(SpeechException::class.java) { OggOpusStream(ByteArrayInputStream(noOpus)) }.code)
    }

    @Test
    fun `identification header rules and codec specific data`() {
        val head = OpusHead.parse(head(channels = 2, preSkip = 312))
        assertEquals(2, head.channels)
        assertEquals(312, head.preSkip)
        assertEquals(16000L, head.inputRate)
        assertEquals(6_500_000L, ByteBuffer.wrap(head.codecDelay()).order(ByteOrder.LITTLE_ENDIAN).long)
        assertEquals(80_000_000L, ByteBuffer.wrap(head.seekPreRoll()).order(ByteOrder.LITTLE_ENDIAN).long)
        assertArrayEquals(head(channels = 2, preSkip = 312), head.raw)
        val surround = head(channels = 6, family = 1) + byteArrayOf(4, 2, 0, 4, 1, 2, 3, 5)
        assertEquals(6, OpusHead.parse(surround).channels)
        listOf(
            head(channels = 3),
            head(channels = 6, family = 1),
            head(family = 255),
            head(version = 0x10),
            "OpusTags".toByteArray() + ByteArray(11),
            head().copyOf(12),
        ).forEach { bytes ->
            assertEquals(SpeechErrorCodes.UNSUPPORTED_FORMAT, assertThrows(SpeechException::class.java) { OpusHead.parse(bytes) }.code)
        }
    }

    @Test
    fun `packet durations follow the table of contents`() {
        assertEquals(0, OpusPackets.samples(ByteArray(0)))
        // SILK 10 / 20 / 40 / 60 ms, hybrid 10 / 20 ms, CELT 2.5 / 5 / 10 / 20 ms, one frame each.
        assertEquals(listOf(480, 960, 1920, 2880), (0..3).map { OpusPackets.samples(byteArrayOf((it shl 3).toByte())) })
        assertEquals(listOf(480, 960), (12..13).map { OpusPackets.samples(byteArrayOf((it shl 3).toByte())) })
        assertEquals(listOf(120, 240, 480, 960), (28..31).map { OpusPackets.samples(byteArrayOf((it shl 3).toByte())) })
        // Two frames (codes 1 and 2), an arbitrary count (code 3), and the 120 ms ceiling.
        assertEquals(1920, OpusPackets.samples(byteArrayOf((31 shl 3 or 1).toByte())))
        assertEquals(1920, OpusPackets.samples(byteArrayOf((31 shl 3 or 2).toByte(), 0)))
        assertEquals(4800, OpusPackets.samples(byteArrayOf((31 shl 3 or 3).toByte(), 5)))
        assertEquals(0, OpusPackets.samples(byteArrayOf((31 shl 3 or 3).toByte())))
        assertEquals(0, OpusPackets.samples(byteArrayOf((3 shl 3 or 3).toByte(), 3)))
    }

    /** Builds one logical stream page by page. zh-CN: 逐页构造一个逻辑流. */
    private class Ogg(private val serial: Int) {
        private val out = ByteArrayOutputStream()
        private var sequence = 0

        fun page(type: Int, vararg packets: ByteArray, complete: Boolean = true) = write(type, packets.toList(), complete)

        fun continued(vararg packets: ByteArray) = write(CONTINUED, packets.toList(), true)

        fun bytes(): ByteArray = out.toByteArray().also { out.reset() }

        private fun write(type: Int, packets: List<ByteArray>, complete: Boolean) {
            val lacing = ByteArrayOutputStream()
            packets.forEachIndexed { index, packet ->
                repeat(packet.size / 255) { lacing.write(255) }
                if (complete || index < packets.size - 1) lacing.write(packet.size % 255) else require(packet.size % 255 == 0)
            }
            val header = ByteBuffer.allocate(27).order(ByteOrder.LITTLE_ENDIAN)
            header.put("OggS".toByteArray()).put(0).put(type.toByte()).putLong(0).putInt(serial).putInt(sequence++).putInt(0).put(lacing.size().toByte())
            val body = packets.fold(ByteArray(0)) { acc, packet -> acc + packet }
            val page = header.array() + lacing.toByteArray() + body
            ByteBuffer.wrap(page).order(ByteOrder.LITTLE_ENDIAN).putInt(22, OggOpusStream.crc(0, page, 0, page.size))
            out.write(page)
        }
    }

    private companion object {
        const val CONTINUED = 1
        const val BOS = 2
        const val EOS = 4

        fun head(channels: Int = 1, preSkip: Int = 312, family: Int = 0, version: Int = 1): ByteArray =
            ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN).put("OpusHead".toByteArray()).put(version.toByte()).put(channels.toByte())
                .putShort(preSkip.toShort()).putInt(16000).putShort(0).put(family.toByte()).array()

        fun tags(): ByteArray = "OpusTags".toByteArray() + ByteArray(8)

        /** A 20 ms CELT packet (config 31, code 0) tagged with [id]. zh-CN: 带 [id] 标记的 20 ms CELT 包. */
        fun packet(id: Int): ByteArray = byteArrayOf((31 shl 3).toByte(), id.toByte(), 0x55)
    }
}
