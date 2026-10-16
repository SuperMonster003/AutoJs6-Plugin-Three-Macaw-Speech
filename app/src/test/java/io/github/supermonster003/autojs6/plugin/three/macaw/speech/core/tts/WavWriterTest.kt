package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.WavReader
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The WAV output of `synthesizeToFile` (roadmap D20): canonical header with patched sizes, PCM16 rounding and clamping,
 * the 100 MiB ceiling, and a file the plugin's own WAV reader accepts.
 * zh-CN: `synthesizeToFile` 的 WAV 输出 (路线图 D20): 回填大小的标准文件头, PCM16 的取整与截断, 100 MiB 上限, 以及插件自身 WAV 读取器可接受的文件.
 */
class WavWriterTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun write(file: File, sampleRate: Int, maxBytes: Long = Long.MAX_VALUE, block: (WavWriter) -> Unit): Long =
        RandomAccessFile(file, "rw").use { raf -> WavWriter(raf.channel, sampleRate, maxBytes).let { writer -> block(writer); writer.finish() } }

    @Test
    fun `the header carries the model rate and the patched sizes`() {
        val file = folder.newFile("out.wav")
        file.writeBytes(ByteArray(500) { 7 })
        val size = write(file, 22050) { writer ->
            writer.write(floatArrayOf(0f, 0.5f, -0.5f))
            writer.write(FloatArray(10) { 1f }, 2, 4)
            assertEquals(14L, writer.dataBytes)
        }
        val bytes = file.readBytes()
        assertEquals(58L, size)
        assertEquals("the stale bytes are truncated", 58, bytes.size)
        val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("RIFF", String(bytes, 0, 4))
        assertEquals(50, header.getInt(4))
        assertEquals("WAVEfmt ", String(bytes, 8, 8))
        assertEquals(16, header.getInt(16))
        assertEquals(1, header.getShort(20).toInt())
        assertEquals(1, header.getShort(22).toInt())
        assertEquals(22050, header.getInt(24))
        assertEquals(44100, header.getInt(28))
        assertEquals(2, header.getShort(32).toInt())
        assertEquals(16, header.getShort(34).toInt())
        assertEquals("data", String(bytes, 36, 4))
        assertEquals(14, header.getInt(40))
        assertEquals(listOf(0, 16384, -16383, 32767, 32767, 32767, 32767), (0 until 7).map { header.getShort(44 + it * 2).toInt() })
    }

    @Test
    fun `pcm16 rounds and clamps`() {
        assertEquals(32767, Pcm16.sample(2f).toInt())
        assertEquals(-32767, Pcm16.sample(-3f).toInt())
        assertEquals(0, Pcm16.sample(0.00001f).toInt())
        assertEquals(1, Pcm16.sample(0.00002f).toInt())
        val target = ByteArray(6)
        assertEquals(4, Pcm16.encode(floatArrayOf(9f, 1f, -1f), 1, 2, target, 2))
        assertArrayEquals(byteArrayOf(0, 0, 0xff.toByte(), 0x7f, 1, 0x80.toByte()), target)
    }

    @Test
    fun `the plugin's wav reader reads the result back`() {
        val file = folder.newFile("tone.wav")
        val samples = FloatArray(8000) { (kotlin.math.sin(it / 10.0) * 0.5).toFloat() }
        write(file, 8000) { it.write(samples) }
        WavReader(file.inputStream(), file.length()).use { reader ->
            assertEquals(8000, reader.format.sampleRate)
            assertEquals(1000L, reader.format.durationMillis)
            assertEquals(16, reader.format.bitsPerSample)
            assertEquals(1, reader.format.channels)
        }
    }

    @Test
    fun `audio past the ceiling fails with LIMIT_EXCEEDED before it is written`() {
        val file = folder.newFile("big.wav")
        val error = assertThrows(SpeechException::class.java) {
            write(file, 16000, maxBytes = 44 + 100) { writer ->
                writer.write(FloatArray(50))
                assertEquals(1000L * 50 / 16000, writer.durationMillis)
                writer.write(FloatArray(1))
            }
        }
        assertEquals(SpeechErrorCodes.LIMIT_EXCEEDED, error.code)
        assertEquals(144L, file.length())
    }
}
