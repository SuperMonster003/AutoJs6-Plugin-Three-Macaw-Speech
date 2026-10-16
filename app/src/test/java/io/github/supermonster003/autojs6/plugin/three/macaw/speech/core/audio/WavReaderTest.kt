package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * WAV input of roadmap D15 (P3.3): every PCM width and float, channel mixing, extensible headers, extra chunks,
 * truncated data, and the refusals (`UNSUPPORTED_FORMAT`, `LIMIT_EXCEEDED`).
 * zh-CN: 路线图 D15 的 WAV 输入 (P3.3): 各种 PCM 位深与浮点, 声道混合, 扩展头, 额外块, 截断数据, 以及拒绝情形.
 */
class WavReaderTest {

    @Test
    fun `16 bit mono reads as floats`() {
        val reader = reader(wav(format = 1, channels = 1, rate = 16000, bits = 16, data = pcm16(0, 16384, -32768, 32767)))
        assertEquals(WavFormat(false, 16, 1, 16000, 8), reader.format)
        assertArrayEquals(floatArrayOf(0f, 0.5f, -1f, 32767 / 32768f), reader.read(10), 1e-6f)
        assertEquals(0, reader.read(10).size)
    }

    @Test
    fun `8, 24 and 32 bit integers and both float widths decode`() {
        assertArrayEquals(floatArrayOf(-1f, 0f, 0.5f), reader(wav(1, 1, 8000, 8, byteArrayOf(0, 128.toByte(), 192.toByte()))).read(3), 1e-6f)
        val pcm24 = byteArrayOf(0, 0, 0x40, 0, 0, 0x80.toByte())
        assertArrayEquals(floatArrayOf(0.5f, -1f), reader(wav(1, 1, 8000, 24, pcm24)).read(2), 1e-6f)
        val pcm32 = le { putInt(Int.MIN_VALUE).putInt(1 shl 30) }
        assertArrayEquals(floatArrayOf(-1f, 0.5f), reader(wav(1, 1, 8000, 32, pcm32)).read(2), 1e-6f)
        val float32 = le { putFloat(0.25f).putFloat(-2f).putFloat(Float.NaN) }
        assertArrayEquals(floatArrayOf(0.25f, -1f, 0f), reader(wav(3, 1, 8000, 32, float32)).read(3), 1e-6f)
        val float64 = le { putDouble(-0.75).putDouble(0.125) }
        assertArrayEquals(floatArrayOf(-0.75f, 0.125f), reader(wav(3, 1, 8000, 64, float64)).read(2), 1e-6f)
    }

    @Test
    fun `stereo is mixed down and extensible headers name the sample format`() {
        val stereo = reader(wav(1, 2, 44100, 16, pcm16(16384, -16384, 32767, 32767)))
        assertEquals(2, stereo.format.channels)
        assertArrayEquals(floatArrayOf(0f, 32767 / 32768f), stereo.read(4), 1e-6f)
        val extensible = reader(wav(0xFFFE, 1, 48000, 32, le { putFloat(0.5f) }, extensibleSubFormat = 3))
        assertEquals(WavFormat(true, 32, 1, 48000, 4), extensible.format)
        assertArrayEquals(floatArrayOf(0.5f), extensible.read(1), 1e-6f)
    }

    @Test
    fun `extra and odd sized chunks are skipped and a short data chunk keeps its whole frames`() {
        val bytes = wav(1, 1, 16000, 16, pcm16(1000, 2000, 3000), extra = listOf("LIST" to ByteArray(3), "fact" to ByteArray(4)))
        assertArrayEquals(floatArrayOf(1000 / 32768f, 2000 / 32768f, 3000 / 32768f), reader(bytes).read(8), 1e-6f)
        // The data chunk claims 6 bytes but the stream ends after 3: one whole frame is left.
        val truncated = bytes.copyOf(bytes.size - 3)
        val cut = WavReader(ByteArrayInputStream(truncated), truncated.size.toLong())
        assertEquals(2L, cut.format.dataBytes)
        assertArrayEquals(floatArrayOf(1000 / 32768f), cut.read(8), 1e-6f)
        // Without a known length the reader stops at the end of the stream as well.
        assertArrayEquals(floatArrayOf(1000 / 32768f), WavReader(ByteArrayInputStream(truncated)).read(8), 1e-6f)
    }

    @Test
    fun `a streaming placeholder size reads to the end of the stream`() {
        val bytes = wav(1, 1, 16000, 16, pcm16(1, 2, 3, 4), dataSize = 0xFFFFFFFFL)
        val reader = WavReader(ByteArrayInputStream(bytes), bytes.size.toLong())
        assertEquals(8L, reader.format.dataBytes)
        assertEquals(4, reader.read(100).size)
    }

    @Test
    fun `inputs that are not supported WAV files are refused`() {
        listOf(
            ByteArray(0),
            "ID3\u0004 not a wav file at all".toByteArray(),
            wav(1, 1, 16000, 16, pcm16(1)).also { it[8] = 'X'.code.toByte() },
            wav(7, 1, 8000, 8, ByteArray(4)),
            wav(1, 1, 16000, 12, ByteArray(4)),
            wav(1, 9, 16000, 16, ByteArray(36)),
            wav(1, 1, 1000, 16, ByteArray(4)),
            wav(1, 2, 16000, 16, ByteArray(8), blockAlign = 2),
            dataBeforeFmt(),
            wav(1, 1, 16000, 16, pcm16(1)).copyOf(30),
        ).forEachIndexed { index, bytes ->
            val error = assertThrows("case $index", SpeechException::class.java) { WavReader(ByteArrayInputStream(bytes), bytes.size.toLong()) }
            assertEquals("case $index", SpeechErrorCodes.UNSUPPORTED_FORMAT, error.code)
        }
    }

    @Test
    fun `files beyond 200 MiB or 30 minutes exceed the limit`() {
        val big = assertThrows(SpeechException::class.java) { WavReader(ByteArrayInputStream(ByteArray(0)), SpeechLimits.MAX_AUDIO_FILE_BYTES + 1) }
        assertEquals(SpeechErrorCodes.LIMIT_EXCEEDED, big.code)
        // 31 minutes of 8-bit mono at 4 kHz: 7.4 MB of declared data, only the header present.
        val frames = 4000L * 60 * 31
        val header = wav(1, 1, 4000, 8, ByteArray(0), dataSize = frames)
        val long = assertThrows(SpeechException::class.java) { WavReader(ByteArrayInputStream(header), header.size + frames) }
        assertEquals(SpeechErrorCodes.LIMIT_EXCEEDED, long.code)
        val ok = WavReader(ByteArrayInputStream(header), header.size + 4000L * 60 * 29)
        assertTrue(ok.format.durationMillis < SpeechLimits.MAX_AUDIO_MILLIS)
    }

    private fun reader(bytes: ByteArray) = WavReader(ByteArrayInputStream(bytes), bytes.size.toLong())

    private fun le(block: ByteBuffer.() -> Unit): ByteArray {
        val buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
        buffer.block()
        return buffer.array().copyOf(buffer.position())
    }

    private fun pcm16(vararg values: Int): ByteArray = le { values.forEach { putShort(it.toShort()) } }

    private fun dataBeforeFmt(): ByteArray {
        val out = ByteArrayOutputStream()
        out.write("RIFF".toByteArray())
        out.write(le { putInt(28) })
        out.write("WAVE".toByteArray())
        out.write("data".toByteArray())
        out.write(le { putInt(2) })
        out.write(ByteArray(2))
        return out.toByteArray()
    }

    companion object {
        /** A RIFF / WAVE file; [extra] chunks come before `data`. zh-CN: 一个 RIFF / WAVE 文件; [extra] 块位于 `data` 之前. */
        fun wav(
            format: Int,
            channels: Int,
            rate: Int,
            bits: Int,
            data: ByteArray,
            extra: List<Pair<String, ByteArray>> = emptyList(),
            dataSize: Long = data.size.toLong(),
            extensibleSubFormat: Int? = null,
            blockAlign: Int = channels * bits / 8,
        ): ByteArray {
            val body = ByteArrayOutputStream()
            val fmtSize = if (format == 0xFFFE) 40 else 16
            val fmt = ByteBuffer.allocate(8 + fmtSize).order(ByteOrder.LITTLE_ENDIAN)
            fmt.put("fmt ".toByteArray()).putInt(fmtSize).putShort(format.toShort()).putShort(channels.toShort()).putInt(rate)
                .putInt(rate * blockAlign).putShort(blockAlign.toShort()).putShort(bits.toShort())
            if (format == 0xFFFE) {
                fmt.putShort(22).putShort(bits.toShort()).putInt(0).putShort((extensibleSubFormat ?: 1).toShort())
                fmt.put(byteArrayOf(0, 0, 0, 0, 0x10, 0, 0x80.toByte(), 0, 0, 0xAA.toByte(), 0, 0x38, 0x9B.toByte(), 0x71))
            }
            body.write(fmt.array())
            extra.forEach { (id, bytes) ->
                body.write(id.toByteArray())
                body.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(bytes.size).array())
                body.write(bytes)
                if (bytes.size % 2 == 1) body.write(0)
            }
            body.write("data".toByteArray())
            body.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dataSize.toInt()).array())
            body.write(data)
            val out = ByteArrayOutputStream()
            out.write("RIFF".toByteArray())
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(4 + body.size()).array())
            out.write("WAVE".toByteArray())
            out.write(body.toByteArray())
            return out.toByteArray()
        }
    }
}
