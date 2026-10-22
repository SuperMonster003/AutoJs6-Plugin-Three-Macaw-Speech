package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decoder output to mono (roadmap D85, P7.5.1): the five PCM encodings with 1, 2 and 6 channels, partial frames,
 * buffer offsets; and the limits and messages of [DecodeRules] (D83).
 * zh-CN: 解码器输出转单声道 (路线图 D85, P7.5.1): 五种 PCM 编码与 1 / 2 / 6 声道, 不完整的帧, 缓冲偏移; 以及 [DecodeRules] 的上限与信息 (D83).
 */
class PcmFramesTest {

    /** One frame per row of [values] (full scale -1..1), every channel of a frame equal to the row's value plus its index. */
    private fun encode(encoding: Int, channels: Int, values: FloatArray): ByteBuffer {
        val width = PcmEncoding.bytes(encoding)!!
        val buffer = ByteBuffer.allocate(values.size * channels * width).order(ByteOrder.LITTLE_ENDIAN)
        values.forEach { value ->
            repeat(channels) { channel ->
                val sample = if (channel % 2 == 0) value else -value
                when (encoding) {
                    PcmEncoding.PCM_8BIT -> buffer.put(((sample * 128).toInt() + 128).coerceIn(0, 255).toByte())
                    PcmEncoding.PCM_16BIT -> buffer.putShort((sample * 32768).toInt().coerceIn(-32768, 32767).toShort())
                    PcmEncoding.PCM_24BIT_PACKED -> {
                        val v = (sample * 8388608).toInt().coerceIn(-8388608, 8388607)
                        buffer.put(v.toByte()).put((v shr 8).toByte()).put((v shr 16).toByte())
                    }
                    PcmEncoding.PCM_32BIT -> buffer.putInt((sample * 2147483648.0).toLong().coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt())
                    else -> buffer.putFloat(sample)
                }
            }
        }
        buffer.flip()
        return buffer
    }

    @Test
    fun `every encoding and channel count mixes to mono`() {
        val values = floatArrayOf(0f, 0.5f, -0.5f, -1f, 0.25f)
        val encodings = listOf(PcmEncoding.PCM_8BIT, PcmEncoding.PCM_16BIT, PcmEncoding.PCM_24BIT_PACKED, PcmEncoding.PCM_32BIT, PcmEncoding.PCM_FLOAT)
        encodings.forEach { encoding ->
            val tolerance = if (encoding == PcmEncoding.PCM_8BIT) 1f / 128 else 1e-4f
            assertArrayEquals("encoding $encoding mono", values, PcmFrames.mono(encode(encoding, 1, values), encoding, 1), tolerance)
            // Even channels carry the value, odd ones its negation: two channels cancel out, six as well.
            assertArrayEquals("encoding $encoding stereo", FloatArray(values.size), PcmFrames.mono(encode(encoding, 2, values), encoding, 2), tolerance)
            assertArrayEquals("encoding $encoding 5.1", FloatArray(values.size), PcmFrames.mono(encode(encoding, 6, values), encoding, 6), tolerance)
        }
        // Equal channels average to the value itself.
        val equal = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putShort(16384).putShort(16384).putShort(16384).putShort(-16384).putShort(-16384).putShort(-16384)
        equal.flip()
        assertArrayEquals(floatArrayOf(0.5f, -0.5f), PcmFrames.mono(equal, PcmEncoding.PCM_16BIT, 3), 1e-6f)
    }

    @Test
    fun `partial frames, offsets and odd floats`() {
        val buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(0).putShort(1000).putShort(2000).putShort(3000).putShort(4000)
        buffer.position(2).limit(10)
        // Two complete stereo frames from offset 2; the position ends at the limit.
        assertArrayEquals(floatArrayOf(1500 / 32768f, 3500 / 32768f), PcmFrames.mono(buffer, PcmEncoding.PCM_16BIT, 2), 1e-6f)
        assertEquals(10, buffer.position())
        val partial = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN).putShort(8192).putShort(8192).put(1)
        partial.flip()
        assertArrayEquals(floatArrayOf(0.25f, 0.25f), PcmFrames.mono(partial, PcmEncoding.PCM_16BIT, 1), 1e-6f)
        val floats = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putFloat(Float.NaN).putFloat(3f).putFloat(-7f)
        floats.flip()
        assertArrayEquals(floatArrayOf(0f, 1f, -1f), PcmFrames.mono(floats, PcmEncoding.PCM_FLOAT, 1), 0f)
    }

    @Test
    fun `unknown encodings and channel counts are refused`() {
        assertNull(PcmEncoding.bytes(1))
        assertNull(PcmEncoding.bytes(13))
        assertThrows(IllegalArgumentException::class.java) { PcmFrames.mono(ByteBuffer.allocate(4), 13, 1) }
        assertThrows(IllegalArgumentException::class.java) { PcmFrames.mono(ByteBuffer.allocate(4), PcmEncoding.PCM_16BIT, 9) }
        assertThrows(IllegalArgumentException::class.java) { PcmFrames.mono(ByteBuffer.allocate(4), PcmEncoding.PCM_16BIT, 0) }
    }

    @Test
    fun `track durations allow one second of padding`() {
        DecodeRules.requireTrackDuration(null)
        DecodeRules.requireTrackDuration(-1)
        DecodeRules.requireTrackDuration(SpeechLimits.MAX_AUDIO_MILLIS * 1000)
        DecodeRules.requireTrackDuration((SpeechLimits.MAX_AUDIO_MILLIS + DecodeRules.PADDING_MILLIS) * 1000)
        val long = assertThrows(SpeechException::class.java) { DecodeRules.requireTrackDuration((SpeechLimits.MAX_AUDIO_MILLIS + DecodeRules.PADDING_MILLIS + 1) * 1000) }
        assertEquals(SpeechErrorCodes.LIMIT_EXCEEDED, long.code)
    }

    @Test
    fun `the duration guard stops a file that understates its length`() {
        val guard = DecodeRules.DurationGuard(8000)
        val limit = (SpeechLimits.MAX_AUDIO_MILLIS + DecodeRules.PADDING_MILLIS) * 8
        repeat((limit / 8000).toInt()) { guard.add(8000) }
        guard.add((limit % 8000).toInt())
        assertEquals(SpeechLimits.MAX_AUDIO_MILLIS + DecodeRules.PADDING_MILLIS, guard.millis)
        val error = assertThrows(SpeechException::class.java) { guard.add(8) }
        assertEquals(SpeechErrorCodes.LIMIT_EXCEEDED, error.code)
    }

    @Test
    fun `refusals are UNSUPPORTED_FORMAT and say why`() {
        val errors = mapOf(
            DecodeRules.unreadable(AudioContainer.UNKNOWN) to "unknown format",
            DecodeRules.unreadable(AudioContainer.MP3) to "MP3",
            DecodeRules.noAudioTrack(AudioContainer.MP4, hasVideo = true) to "video but no audio track",
            DecodeRules.noAudioTrack(AudioContainer.MATROSKA, hasVideo = false) to "no audio track",
            DecodeRules.noDecoder("audio/opus", "9", 28) to "Android 9, API 28) has no decoder for audio/opus",
            DecodeRules.encrypted("audio/mp4a-latm") to "encrypted",
            DecodeRules.unreadablePcm(13, 2) to "encoding 13, 2 channels",
            DecodeRules.rateChanged(22050, 44100) to "from 22050 Hz to 44100 Hz",
            DecodeRules.damaged(1500, IllegalStateException("bad frame")) to "failed at 1500 ms (bad frame)",
            DecodeRules.empty(AudioContainer.FLAC) to "FLAC",
        )
        errors.forEach { (error, text) ->
            assertEquals(SpeechErrorCodes.UNSUPPORTED_FORMAT, error.code)
            assertTrue("${error.message} lacks $text", error.message!!.contains(text))
        }
    }
}
