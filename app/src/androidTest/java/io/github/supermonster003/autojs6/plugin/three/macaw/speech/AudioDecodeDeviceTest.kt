package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioContainer
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioFiles
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.AudioInput
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.MediaAudioReader
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.MonoAudio
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.audio.OggOpusStream
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Non-WAV decoding on a device without any model (roadmap P7.5.6, D81 - D86), so CI's emulators run it: every encoded
 * recording of `locks/encoded-test-audio.lock` decodes to the length and loudness envelope of its WAV source (FLAC to
 * the same samples), the plugin's own Ogg Opus parser matches the system extractor where both exist, the bundled VAD
 * finds the same speech through the Binder, and damage, a video without audio and other files fail with
 * `UNSUPPORTED_FORMAT` and a reason, as does AC-3 where the device has no decoder for it; an Ogg file the system
 * refuses goes to the plugin's parser (roadmap Q25). Results are `MacawDeviceRun` lines.
 * zh-CN: 不需要任何模型的设备上非 WAV 解码测试 (路线图 P7.5.6, D81 - D86), 因此 CI 的模拟器也运行它: `locks/encoded-test-audio.lock` 的每份编码录音
 * 解出的时长与响度包络都与其 WAV 来源一致 (FLAC 采样完全相同), 插件自带的 Ogg Opus 解析在两者都可用时与系统提取器一致, 随包 VAD 经 Binder 找到相同
 * 的语音, 损坏文件, 没有音轨的视频与其他文件以 `UNSUPPORTED_FORMAT` 失败并写明原因, 设备没有 AC-3 解码器时也是如此; 系统拒绝的 Ogg 文件交给
 * 插件自带解析 (路线图 Q25). 结果为 `MacawDeviceRun` 日志行.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AudioDecodeDeviceTest {

    @After
    fun systemParser() {
        AudioFiles.forceOwnOggOpus = false
    }

    @Test
    fun test0EveryEncodingDecodesLikeItsWav() {
        val references = mapOf("en" to SpeechDevice.samples("test-audio/en.wav"), "zh" to SpeechDevice.samples("test-audio/zh.wav"))
        AUDIO.forEach { name ->
            val reference = references.getValue(name.substringBefore('.').substringBefore('-'))
            val decoded = decode(name)
            val lengthMillis = AudioInput.millis(decoded.samples.size.toLong())
            val difference = lengthMillis - AudioInput.millis(reference.size.toLong())
            val similarity = envelopeSimilarity(reference, decoded.samples)
            SpeechDevice.log("decode file=$name sourceRate=${decoded.rate} ms=$lengthMillis differenceMs=$difference envelope=${"%.3f".format(similarity)} api=${Build.VERSION.SDK_INT}")
            // Encoder priming and frame padding (AMR-NB: whole 20 ms frames at 8 kHz, MP3 / AAC: priming samples).
            assertTrue("$name: $difference ms longer or shorter than the WAV", abs(difference) <= if (name.endsWith(".amr")) 300 else 150)
            assertTrue("$name: envelope similarity $similarity", similarity >= if (name.endsWith(".amr")) 0.80 else 0.90)
        }
        listOf("en", "zh").forEach { source ->
            val flac = decode("$source.flac").samples
            val wav = references.getValue(source)
            assertEquals("$source.flac length", wav.size, flac.size)
            val worst = flac.indices.maxOf { abs(flac[it] - wav[it]) }
            assertTrue("$source.flac differs from the WAV by $worst", worst <= 1f / 32768)
        }
    }

    @Test
    fun test1OwnOggParserMatchesTheSystem() {
        listOf("en.opus", "zh.opus").forEach { name ->
            AudioFiles.forceOwnOggOpus = true
            val own = decode(name)
            AudioFiles.forceOwnOggOpus = false
            assertEquals(48_000, own.rate)
            assertTrue(own.ownParser)
            // The default path: the system extractor, or the plugin's parser where the system refuses the file (roadmap Q25).
            val default = decode(name)
            val correlation = correlation(own.samples, default.samples)
            SpeechDevice.log("ogg own file=$name samples=${own.samples.size} default=${default.samples.size} defaultOwnParser=${default.ownParser} correlation=${"%.5f".format(correlation)} api=${Build.VERSION.SDK_INT}")
            assertTrue("$name: own ${own.samples.size}, default ${default.samples.size}", abs(own.samples.size - default.samples.size) <= AudioInput.samples(20))
            assertTrue("$name: correlation $correlation", correlation >= 0.99)
        }
    }

    @Test
    fun test2VoiceActivityThroughTheBinder() {
        fun segments(file: File): List<Pair<Long, Long>> {
            val answer = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { SpeechDevice.plugin.detectVoice(it, SpeechDevice.request()) }
            assertNull(answer.getString(SpeechContract.KEY_ERROR_MESSAGE), answer.getString(SpeechContract.KEY_ERROR_CODE))
            @Suppress("DEPRECATION")
            return answer.getParcelableArrayList<Bundle>(SpeechResultKeys.SEGMENTS).orEmpty().map { it.getLong(SpeechResultKeys.START_MILLIS) to it.getLong(SpeechResultKeys.END_MILLIS) }
        }
        val references = mapOf("en" to segments(SpeechDevice.asset("test-audio/en.wav")), "zh" to segments(SpeechDevice.asset("test-audio/zh.wav")))
        val results = AUDIO.map { name ->
            val reference = references.getValue(name.substringBefore('.').substringBefore('-'))
            val found = segments(SpeechDevice.asset("encoded-audio/$name"))
            // Lossy codecs move the faint onset, so compare the spoken span (first start to last end) as a whole.
            val overlap = if (found.isEmpty()) 0.0 else spanOverlap(found.first().first to found.last().second, reference.first().first to reference.last().second)
            SpeechDevice.log("vad file=$name segments=${found.size} wav=${reference.size} spanMs=${found.firstOrNull()?.first}..${found.lastOrNull()?.second} wavSpanMs=${reference.first().first}..${reference.last().second} overlap=${"%.3f".format(overlap)}")
            Triple(name, abs(found.size - reference.size), overlap)
        }
        results.forEach { (name, difference, overlap) ->
            assertTrue("$name: $difference segments more or fewer than the WAV", difference <= 1)
            assertTrue("$name: spoken span overlap $overlap", overlap >= 0.85)
        }
    }

    @Test
    fun test3DamageAndFilesWithoutAudio() {
        val folder = File(SpeechDevice.context.cacheDir, "device-test").apply { mkdirs() }
        fun file(name: String, bytes: ByteArray) = File(folder, name).apply { writeBytes(bytes) }
        val opus = SpeechDevice.asset("encoded-audio/en.opus").readBytes()
        val refusals = mapOf(
            SpeechDevice.asset("encoded-audio/video-only.mp4") to "video but no audio track",
            file("empty.bin", ByteArray(0)) to "unknown format",
            file("garbage.bin", "XXXX".toByteArray() + ByteArray(4092) { (it * 37 + 11).toByte() }) to "unknown format",
            file("not-a-wav.mp3", "ID3".toByteArray() + ByteArray(4096)) to "MP3",
            file("junk.ogg", "OggS".toByteArray() + ByteArray(100) { 7 }) to "Ogg",
        )
        refusals.forEach { (input, reason) ->
            val error = assertThrows(input.name, SpeechException::class.java) { open(input) }
            SpeechDevice.log("refused file=${input.name} code=${error.code} message=${error.message}")
            assertEquals(input.name, SpeechErrorCodes.UNSUPPORTED_FORMAT, error.code)
            assertTrue("${input.name}: ${error.message}", error.message!!.contains(reason))
        }
        // The same codes reach the host through the Binder.
        listOf(SpeechDevice.asset("encoded-audio/video-only.mp4"), File(folder, "garbage.bin")).forEach { input ->
            val answer = ParcelFileDescriptor.open(input, ParcelFileDescriptor.MODE_READ_ONLY).use { SpeechDevice.plugin.detectVoice(it, SpeechDevice.request()) }
            assertEquals(input.name, SpeechErrorCodes.UNSUPPORTED_FORMAT, answer.getString(SpeechContract.KEY_ERROR_CODE))
        }
        // A flipped byte in the middle of an Ogg page fails the plugin's checksum.
        val flipped = file("flipped.opus", opus.copyOf().also { it[it.size / 2] = (it[it.size / 2].toInt() xor 0x55).toByte() })
        AudioFiles.forceOwnOggOpus = true
        val checksum = assertThrows(SpeechException::class.java) { decode(flipped) }
        assertEquals(SpeechErrorCodes.UNSUPPORTED_FORMAT, checksum.code)
        assertTrue(checksum.message, checksum.message!!.contains("checksum"))
        // A file cut short yields the audio it holds, as a truncated WAV does.
        val cutOpus = decode(file("cut.opus", opus.copyOf(opus.size * 2 / 5)))
        AudioFiles.forceOwnOggOpus = false
        val mp3 = SpeechDevice.asset("encoded-audio/en.mp3").readBytes()
        val cutMp3 = decode(file("cut.mp3", mp3.copyOf(mp3.size * 2 / 5)))
        // Two fifths of the bytes hold a quarter to a half of the 6625 ms (headers and variable bit rates aside).
        listOf("cut.opus" to cutOpus, "cut.mp3" to cutMp3).forEach { (name, decoded) ->
            val millis = AudioInput.millis(decoded.samples.size.toLong())
            SpeechDevice.log("truncated file=$name ms=$millis")
            assertTrue("$name: $millis ms", millis in 1650..3300)
        }
    }

    @Test
    fun test4MissingDecodersAndTheOggFallback() {
        // AOSP has no AC-3 decoder; some phones ship one. Either it decodes, or the refusal names the MIME type and the API
        // (an older MP4 extractor drops a track it has no decoder for, which leaves no audio track).
        val ac3 = runCatching { decode("en-ac3.mp4") }
        ac3.onSuccess { decoded ->
            SpeechDevice.log("ac3 decoded ms=${AudioInput.millis(decoded.samples.size.toLong())} api=${Build.VERSION.SDK_INT}")
            assertTrue(abs(AudioInput.millis(decoded.samples.size.toLong()) - 6625) <= 150)
        }.onFailure { error ->
            SpeechDevice.log("ac3 refused code=${(error as? SpeechException)?.code} message=${error.message}")
            assertEquals(SpeechErrorCodes.UNSUPPORTED_FORMAT, (error as SpeechException).code)
            val reason = "API ${Build.VERSION.SDK_INT}) has no decoder for audio/ac3"
            assertTrue(error.message, error.message!!.contains(reason) || error.message!!.contains("has no audio track"))
        }
        // Roadmap Q25: an Ogg file goes to the system first and to the plugin's parser when the system refuses it. Every
        // system tried reads plain Ogg Opus (API 25 and 28 included), so a Skeleton stream ahead of the Opus stream (a
        // grouped Ogg file whose first logical stream is not audio) shows which path reads it; both give the same samples.
        val own = run {
            AudioFiles.forceOwnOggOpus = true
            decode("en.opus").also { AudioFiles.forceOwnOggOpus = false }
        }
        val opus = SpeechDevice.asset("encoded-audio/en.opus")
        val system = runCatching {
            ParcelFileDescriptor.open(opus, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                MediaAudioReader.open(descriptor.dup(), opus.length(), AudioContainer.OGG_OPUS).use { drain(it) }
            }
        }
        SpeechDevice.log("system ogg opus api=${Build.VERSION.SDK_INT} ownParser=${system.getOrNull()?.ownParser} correlation=${system.getOrNull()?.let { correlation(own.samples, it.samples) }} message=${system.exceptionOrNull()?.message}")
        assertTrue(system.exceptionOrNull()?.message, system.isSuccess)
        assertTrue(correlation(own.samples, system.getOrThrow().samples) >= 0.99)
        val skeleton = File(SpeechDevice.context.cacheDir, "device-test/skeleton.ogg").apply { writeBytes(skeletonPage() + opus.readBytes()) }
        val grouped = decode(skeleton)
        val correlation = correlation(own.samples, grouped.samples)
        SpeechDevice.log("skeleton ogg opus api=${Build.VERSION.SDK_INT} ownParser=${grouped.ownParser} samples=${grouped.samples.size} correlation=${"%.5f".format(correlation)}")
        assertTrue("correlation $correlation", correlation >= 0.99)
    }

    /** The beginning-of-stream page of an Ogg Skeleton stream (a "fishead" packet). zh-CN: Ogg Skeleton 流的起始页. */
    private fun skeletonPage(): ByteArray {
        val packet = "fishead".toByteArray() + ByteArray(57)
        val page = ByteBuffer.allocate(27 + 1 + packet.size).order(ByteOrder.LITTLE_ENDIAN)
        page.put("OggS".toByteArray()).put(0).put(2).putLong(0).putInt(0x5EED).putInt(0).putInt(0).put(1).put(packet.size.toByte()).put(packet)
        val bytes = page.array()
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(22, OggOpusStream.crc(0, bytes, 0, bytes.size))
        return bytes
    }

    private class Decoded(val rate: Int, val samples: FloatArray, val ownParser: Boolean)

    private fun open(file: File) = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { AudioFiles.open(it).close() }

    private fun decode(name: String): Decoded = decode(SpeechDevice.asset("encoded-audio/$name"))

    private fun decode(file: File): Decoded = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        AudioFiles.open(descriptor).use { drain(it) }
    }

    private fun drain(reader: MonoAudio): Decoded {
        val chunks = ArrayList<FloatArray>()
        AudioInput.decode(reader) { chunks += it }
        val out = FloatArray(chunks.sumOf { it.size })
        var offset = 0
        chunks.forEach {
            System.arraycopy(it, 0, out, offset, it.size)
            offset += it.size
        }
        return Decoded(reader.sampleRate, out, (reader as? MediaAudioReader)?.ownOggParser == true)
    }

    companion object {
        /** The encoded recordings with an audio track. zh-CN: 带音轨的编码录音. */
        val AUDIO = listOf(
            "en.mp3", "en.aac", "en.m4a", "en.ogg", "en.opus", "en.flac", "en.amr", "en-video.mp4", "en-video.webm",
            "zh.mp3", "zh.aac", "zh.m4a", "zh.ogg", "zh.opus", "zh.flac",
        )

        private const val FRAME = 160

        /** Log energy of every 10 ms frame. zh-CN: 每 10 ms 帧的对数能量. */
        private fun envelope(samples: FloatArray): DoubleArray = DoubleArray(samples.size / FRAME) { frame ->
            var sum = 0.0
            for (index in frame * FRAME until (frame + 1) * FRAME) sum += samples[index] * samples[index]
            ln(sum / FRAME + 1e-6)
        }

        /** The best Pearson correlation of the two envelopes within 300 ms of lag. zh-CN: 两个包络在 300 ms 滞后内的最佳皮尔逊相关. */
        fun envelopeSimilarity(a: FloatArray, b: FloatArray): Double {
            val x = envelope(a)
            val y = envelope(b)
            return (-30..30).maxOf { lag ->
                val from = maxOf(0, -lag)
                val to = minOf(x.size, y.size - lag)
                if (to - from < 50) -1.0 else pearson(DoubleArray(to - from) { x[from + it] }, DoubleArray(to - from) { y[from + it + lag] })
            }
        }

        /** Intersection over union of two time spans. zh-CN: 两个时间区间的交并比. */
        fun spanOverlap(a: Pair<Long, Long>, b: Pair<Long, Long>): Double {
            val intersection = minOf(a.second, b.second) - maxOf(a.first, b.first)
            val union = maxOf(a.second, b.second) - minOf(a.first, b.first)
            return if (intersection <= 0 || union <= 0) 0.0 else intersection.toDouble() / union
        }

        /** Sample correlation at lag 0 over the common length. zh-CN: 公共长度上滞后为 0 的采样相关. */
        fun correlation(a: FloatArray, b: FloatArray): Double {
            val count = minOf(a.size, b.size)
            return pearson(DoubleArray(count) { a[it].toDouble() }, DoubleArray(count) { b[it].toDouble() })
        }

        private fun pearson(x: DoubleArray, y: DoubleArray): Double {
            val mx = x.average()
            val my = y.average()
            var sxy = 0.0
            var sxx = 0.0
            var syy = 0.0
            for (index in x.indices) {
                val dx = x[index] - mx
                val dy = y[index] - my
                sxy += dx * dy
                sxx += dx * dx
                syy += dy * dy
            }
            return if (sxx == 0.0 || syy == 0.0) 0.0 else sxy / sqrt(sxx * syy)
        }
    }
}
