package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool.M4aWriter
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool.SynthesizeRules
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.sin

/**
 * The `m4a` output of `speech_synthesize` without any model (roadmap P7.6.7, D98, D101 E3), so CI's emulators run it: a
 * tone at every sample rate of the catalog's voices goes through [M4aWriter] in uneven pieces, and `MediaExtractor`
 * reads back one mono AAC track at that rate whose duration is within 150 ms of the samples written, or longer by at
 * most the encoder's priming and the padding of the last frame (see [maxExcessMillis]); a writer past its byte ceiling
 * fails with `LIMIT_EXCEEDED`. Results are `MacawDeviceRun` lines.
 * zh-CN: 不需要任何模型的 `speech_synthesize` `m4a` 输出测试 (路线图 P7.6.7, D98, D101 E3), 因此 CI 的模拟器也运行它: 目录中各语音采样率的音调以不
 * 均匀的分块经 [M4aWriter] 写出, `MediaExtractor` 读回一条该采样率的单声道 AAC 音轨, 时长与写入的采样相差不超过 150 ms, 或者至多长出编码器预滚与
 * 末帧补齐 (见 [maxExcessMillis]); 超过字节上限的写入器以 `LIMIT_EXCEEDED` 失败. 结果为 `MacawDeviceRun` 日志行.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class M4aWriterDeviceTest {

    @Test
    fun test0EveryVoiceRateReadsBack() {
        RATES.forEach { rate ->
            val file = File.createTempFile("m4a-$rate-", ".m4a", SpeechDevice.context.cacheDir)
            try {
                val samples = tone(rate, SECONDS)
                val writer = M4aWriter(file, rate)
                val size = writer.use {
                    var offset = 0
                    var piece = 0
                    while (offset < samples.size) {
                        val count = minOf(PIECES[piece++ % PIECES.size], samples.size - offset)
                        it.write(samples.copyOfRange(offset, offset + count))
                        offset += count
                    }
                    it.finish()
                }
                assertEquals(file.length(), size)
                val writtenMillis = samples.size * 1000L / rate
                assertEquals(writtenMillis, writer.durationMillis)
                val track = readBack(file)
                val difference = track.durationMillis - writtenMillis
                SpeechDevice.log(
                    "m4a rate=$rate bitRate=${SynthesizeRules.aac(rate).bitRate} bytes=$size ms=$writtenMillis extractorMs=${track.durationMillis} " +
                        "lastSampleMs=${track.lastSampleMillis} differenceMs=$difference api=${Build.VERSION.SDK_INT}",
                )
                assertEquals(MediaFormat.MIMETYPE_AUDIO_AAC, track.mime)
                assertEquals(rate, track.sampleRate)
                assertEquals(1, track.channels)
                assertTrue("rate $rate: ${track.durationMillis} ms read back for $writtenMillis ms written", difference in -MAX_DIFFERENCE_MILLIS..maxExcessMillis(rate))
                assertTrue("rate $rate: ${track.frames} AAC frames", track.frames >= samples.size / AAC_FRAME_SAMPLES)
                assertTrue("rate $rate: $size bytes within the bit rate", size * 8 * 1000 / writtenMillis <= SynthesizeRules.aac(rate).bitRate * 1.2 + 16_000)
            } finally {
                file.delete()
            }
        }
    }

    @Test
    fun test1TheCeilingStopsTheWriter() {
        val rate = 22050
        val file = File.createTempFile("m4a-limit-", ".m4a", SpeechDevice.context.cacheDir)
        try {
            val error = M4aWriter(file, rate, maxBytes = LIMIT_BYTES).use { writer ->
                assertThrows(SpeechException::class.java) {
                    repeat(30) { writer.write(tone(rate, 1)) }
                    writer.finish()
                }
            }
            SpeechDevice.log("m4a limit rate=$rate maxBytes=$LIMIT_BYTES code=${error.code} api=${Build.VERSION.SDK_INT}")
            assertEquals(SpeechErrorCodes.LIMIT_EXCEEDED, error.code)
            assertEquals("The synthesized audio exceeds $LIMIT_BYTES bytes; synthesize less text at a time", error.message)
        } finally {
            file.delete()
        }
    }

    private class Track(val mime: String?, val sampleRate: Int, val channels: Int, val durationMillis: Long, val frames: Int, val lastSampleMillis: Long)

    private fun readBack(file: File): Track {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.path)
            assertEquals(1, extractor.trackCount)
            val format = extractor.getTrackFormat(0)
            extractor.selectTrack(0)
            val buffer = ByteBuffer.allocate(64 * 1024)
            var frames = 0
            var last = 0L
            while (extractor.readSampleData(buffer, 0) >= 0) {
                frames++
                last = extractor.sampleTime
                extractor.advance()
            }
            return Track(
                mime = format.getString(MediaFormat.KEY_MIME),
                sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                durationMillis = format.getLong(MediaFormat.KEY_DURATION) / 1000,
                frames = frames,
                lastSampleMillis = last / 1000,
            )
        } finally {
            extractor.release()
        }
    }

    private fun tone(rate: Int, seconds: Int): FloatArray = FloatArray(rate * seconds) { (0.3 * sin(2 * PI * 440 * it / rate)).toFloat() }

    /**
     * How much longer than the samples written a track may read back: on API 37 the track also counts the encoder's
     * priming ([ENCODER_DELAY_SAMPLES]) and the padding of its last frame (26 frames from 0 ms, 3328 ms read back for
     * 3000 ms at 8 kHz on the API 37.1 emulator); earlier releases stay within 150 ms either way.
     * zh-CN: 读回的音轨最多可比写入的采样长出多少: API 37 上音轨还计入编码器的预滚 ([ENCODER_DELAY_SAMPLES]) 与末帧补齐 (API 37.1 模拟器上 8 kHz
     * 写入 3000 ms, 读回 26 帧, 自 0 ms 起, 共 3328 ms); 更早的版本两个方向都在 150 ms 以内.
     */
    private fun maxExcessMillis(rate: Int): Long = maxOf(MAX_DIFFERENCE_MILLIS, (ENCODER_DELAY_SAMPLES + AAC_FRAME_SAMPLES) * 1000L / rate)

    private companion object {
        /** The sample rates of the catalog's text-to-speech models. zh-CN: 目录中 TTS 模型的采样率. */
        val RATES = listOf(8000, 16000, 22050, 24000, 44100)
        val PIECES = listOf(4410, 1000, 333, 8192, 1)
        const val SECONDS = 3
        const val AAC_FRAME_SAMPLES = 1024
        const val ENCODER_DELAY_SAMPLES = 2048
        const val MAX_DIFFERENCE_MILLIS = 150L
        const val LIMIT_BYTES = 20_000L
    }
}
