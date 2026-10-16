package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Resampling of WAV inputs to 16 kHz (roadmap D15, P3.3): sample counts, pitch, anti-aliasing, streaming equal to one
 * shot, and the whole WAV path through [AudioInput.decode].
 * zh-CN: WAV 输入重采样到 16 kHz (路线图 D15, P3.3): 采样数, 音高, 抗混叠, 流式与一次性结果一致, 以及经 [AudioInput.decode] 的完整 WAV 路径.
 */
class ResamplerTest {

    @Test
    fun `equal rates pass through`() {
        val input = FloatArray(100) { it / 100f }
        val resampler = Resampler(16000, 16000)
        assertArrayEquals(input, resampler.process(input), 0f)
        assertEquals(0, resampler.flush().size)
    }

    @Test
    fun `whole inputs yield the rounded up sample count`() {
        listOf(48000 to 48000, 44100 to 44100, 8000 to 8000, 22050 to 22050, 44100 to 1001, 8000 to 7).forEach { (rate, count) ->
            val resampler = Resampler(rate, 16000)
            val produced = resampler.process(FloatArray(count)).size + resampler.flush().size
            assertEquals("$rate Hz x $count", (count.toLong() * 16000 + rate - 1) / rate, produced.toLong())
        }
    }

    @Test
    fun `a tone keeps its pitch and level across rates`() {
        listOf(48000, 44100, 22050, 8000).forEach { rate ->
            val output = resample(tone(rate, 1000.0, rate), rate)
            // Skip the edges where the kernel reads silence.
            val middle = output.copyOfRange(1600, output.size - 1600)
            assertEquals("$rate Hz level", 1 / sqrt(2.0), rms(middle), 0.02)
            assertEquals("$rate Hz pitch", 1000.0, crossings(middle) * 16000.0 / (2 * middle.size), 15.0)
        }
    }

    @Test
    fun `frequencies above the new Nyquist rate are removed`() {
        val output = resample(tone(48000, 12000.0, 48000), 48000)
        val middle = output.copyOfRange(1600, output.size - 1600)
        assertTrue("12 kHz must not alias into the 16 kHz output (rms ${rms(middle)})", rms(middle) < 0.01)
    }

    @Test
    fun `streaming in odd chunks equals one shot`() {
        val input = tone(44100, 440.0, 44100)
        val whole = resample(input, 44100)
        val resampler = Resampler(44100, 16000)
        val pieces = ArrayList<Float>()
        var offset = 0
        var size = 1
        while (offset < input.size) {
            val end = minOf(input.size, offset + size)
            resampler.process(input.copyOfRange(offset, end)).forEach { pieces += it }
            offset = end
            size = size * 3 % 997 + 1
        }
        resampler.flush().forEach { pieces += it }
        assertArrayEquals(whole, pieces.toFloatArray(), 1e-6f)
    }

    @Test
    fun `decode turns an 8 kHz stereo WAV into 16 kHz mono chunks`() {
        val frames = 8000
        val pcm = ByteBuffer.allocate(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
        repeat(frames) { index ->
            val value = (sin(2 * PI * 500 * index / 8000.0) * 16000).toInt().toShort()
            pcm.putShort(value).putShort(value)
        }
        val bytes = WavReaderTest.wav(1, 2, 8000, 16, pcm.array())
        val chunks = ArrayList<FloatArray>()
        val total = AudioInput.decode(WavReader(ByteArrayInputStream(bytes), bytes.size.toLong()), chunkFrames = 3000) { chunks += it }
        assertEquals(16000L, total)
        assertEquals(total, chunks.sumOf { it.size }.toLong())
        assertTrue(chunks.size > 3)
        val joined = chunks.flatMap { it.asIterable() }.toFloatArray()
        assertEquals(16000 / 32768.0 / sqrt(2.0), rms(joined.copyOfRange(1600, 14400)), 0.01)
    }

    @Test
    fun `pcm16 bytes and samples convert to floats`() {
        assertArrayEquals(floatArrayOf(0f, 0.5f, -1f), AudioInput.pcm16(byteArrayOf(0, 0, 0, 0x40, 0, 0x80.toByte())), 0f)
        assertArrayEquals(floatArrayOf(-0.5f, 0.25f), AudioInput.pcm16(shortArrayOf(-16384, 8192, 1), 2), 0f)
        assertEquals(1000L, AudioInput.millis(16000))
        assertEquals(16000L, AudioInput.samples(1000))
    }

    private fun resample(input: FloatArray, rate: Int): FloatArray {
        val resampler = Resampler(rate, 16000)
        return resampler.process(input) + resampler.flush()
    }

    private fun tone(rate: Int, frequency: Double, count: Int) = FloatArray(count) { sin(2 * PI * frequency * it / rate).toFloat() }

    private fun rms(samples: FloatArray) = sqrt(samples.sumOf { it.toDouble() * it } / samples.size)

    private fun crossings(samples: FloatArray): Int = (1 until samples.size).count { (samples[it - 1] < 0) != (samples[it] < 0) && abs(samples[it] - samples[it - 1]) > 1e-6 }
}
