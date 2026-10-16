package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.SpeechDevice.SENSE_VOICE
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.SpeechDevice.STREAMING
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max

/**
 * Recognition with the real models on a device (roadmap P3.2, P3.3, P3.5): model load time and memory, file
 * recognition of the test recordings through `ISpeechPlugin.recognizeFile` (real time factor, character error rate
 * against the LibriSpeech transcript), the first partial of an external PCM session fed in real time, 30 second and 10
 * minute files, voice activity and the refusals. Needs the models of [SpeechModelSetupDeviceTest]; skipped otherwise.
 * Results are `MacawDeviceRun` log lines.
 * zh-CN: 在设备上以真实模型识别 (路线图 P3.2, P3.3, P3.5): 模型加载时间与内存, 经 `ISpeechPlugin.recognizeFile` 识别测试录音 (实时率, 相对
 * LibriSpeech 转写的字错率), 实时送入的外部 PCM 会话的首个 partial, 30 秒与 10 分钟文件, 语音活动检测与各种拒绝情形. 需要
 * [SpeechModelSetupDeviceTest] 安装的模型, 否则跳过. 结果为 `MacawDeviceRun` 日志行.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SpeechRecognitionDeviceTest {

    private val runtime get() = SpeechRuntime.get(SpeechDevice.context)

    @Before
    fun models() {
        assumeTrue("install the models first (SpeechModelSetupDeviceTest, -e macawModels true)", SpeechDevice.installed(STREAMING) && SpeechDevice.installed(SENSE_VOICE))
    }

    @Test
    fun test0StreamingPipelineKeepsWhatTheRawStreamHears() {
        val samples = SpeechDevice.samples("test-audio/en.wav")
        val model = runtime.resolve(SpeechContract.KIND_ASR, STREAMING)
        runtime.acquireAsr(model, null, hotwords = false).use { lease ->
            val engine = lease.engine as io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.OnlineEngine
            val raw = engine.stream(emptyList()).use { stream ->
                stream.accept(samples)
                stream.accept(FloatArray(8000))
                stream.decode()
                stream.text()
            }
            val offsets = listOf(0, 3584, 7168, 10752).associateWith { skip ->
                engine.stream(emptyList()).use { stream ->
                    var offset = skip
                    while (offset < samples.size) {
                        val end = minOf(samples.size, offset + 5120)
                        stream.accept(samples.copyOfRange(offset, end))
                        stream.decode()
                        offset = end
                    }
                    stream.accept(FloatArray(8000))
                    stream.decode()
                    stream.text()
                }
            }
            SpeechDevice.log("streaming offsets " + offsets.entries.joinToString(" | ") { (skip, text) -> "${skip / 16}ms:${"%.3f".format(SpeechDevice.cer(SpeechDevice.EN_REFERENCE, text))}:${text.take(40)}" })
            val texts = listOf(true, false).associateWith { gate ->
                val finals = ArrayList<String>()
                val trace = ArrayList<String>()
                val sink = object : io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.DictationSink {
                    override fun partial(text: String, segmentIndex: Int) {
                        trace += "p$segmentIndex:${text.length}"
                    }
                    override fun final(utterance: io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Utterance) {
                        finals += utterance.text
                        trace += "f${utterance.segmentIndex}:${utterance.startMillis}-${utterance.endMillis}:${utterance.text}"
                    }
                    override fun endpoint() = Unit
                    override fun voiceActivity(active: Boolean) {
                        trace += "v$active"
                    }
                }
                val options = io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.ListenOptions(vad = gate, silenceEndMillis = 600)
                runtime.dictation(engine, options, sink).use { dictation ->
                    var offset = 0
                    while (offset < samples.size) {
                        val end = minOf(samples.size, offset + 8000)
                        dictation.accept(samples.copyOfRange(offset, end))
                        offset = end
                    }
                    dictation.flush(emitFinal = true)
                }
                SpeechDevice.log("streaming trace gate=$gate ${trace.joinToString(" ")}")
                finals.joinToString(" ")
            }
            val rawCer = SpeechDevice.cer(SpeechDevice.EN_REFERENCE, raw)
            val gatedCer = SpeechDevice.cer(SpeechDevice.EN_REFERENCE, texts.getValue(true))
            val ungatedCer = SpeechDevice.cer(SpeechDevice.EN_REFERENCE, texts.getValue(false))
            SpeechDevice.log("streaming cer raw=${"%.3f".format(rawCer)} gated=${"%.3f".format(gatedCer)} ungated=${"%.3f".format(ungatedCer)} text=raw[$raw] gated[${texts[true]}] ungated[${texts[false]}]")
            assertTrue("the gate loses words: raw $rawCer, gated $gatedCer", gatedCer <= rawCer + 0.05)
            assertTrue("the pipeline loses words: raw $rawCer, ungated $ungatedCer", ungatedCer <= rawCer + 0.05)
        }
    }

    @Test
    fun test1LoadTimesAndMemory() {
        listOf(STREAMING, SENSE_VOICE, SpeechDevice.KWS).filter(SpeechDevice::installed).forEach { id ->
            runtime.engines.evict(id)
            Runtime.getRuntime().gc()
            val (nativeBefore, pssBefore) = SpeechDevice.memory()
            val kind = if (id == SpeechDevice.KWS) SpeechContract.KIND_KWS else SpeechContract.KIND_ASR
            val model = runtime.resolve(kind, id)
            val started = SystemClock.elapsedRealtimeNanos()
            val lease = if (id == SpeechDevice.KWS) runtime.acquireKeywords(model) else runtime.acquireAsr(model, null, hotwords = false)
            val millis = (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000
            val (nativeAfter, pssAfter) = SpeechDevice.memory()
            SpeechDevice.log("load model=$id ms=$millis nativeDeltaMiB=${nativeAfter - nativeBefore} pssMiB=$pssAfter pssDeltaMiB=${pssAfter - pssBefore}")
            lease.close()
            runtime.engines.evict(id)
        }
    }

    @Test
    fun test2RecognizeFilesThroughTheBinder() {
        val files = listOf("en", "zh", "mixed-zh-en", "zh-8k")
        listOf(STREAMING, SENSE_VOICE).forEach { id ->
            files.forEach { name ->
                val file = SpeechDevice.asset("test-audio/$name.wav")
                val started = SystemClock.elapsedRealtimeNanos()
                val answer = SpeechDevice.recognize(file, SpeechDevice.request {
                    putString(SpeechOptionKeys.MODEL, id)
                    putString(SpeechOptionKeys.DISPLAY_NAME, file.name)
                })
                val millis = (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000
                assertNull(answer.getString(SpeechContract.KEY_ERROR_MESSAGE), answer.getString(SpeechContract.KEY_ERROR_CODE))
                val text = answer.getString(SpeechResultKeys.TEXT).orEmpty()
                val duration = answer.getLong(SpeechResultKeys.DURATION_MILLIS)
                @Suppress("DEPRECATION")
                val segments = answer.getParcelableArrayList<Bundle>(SpeechResultKeys.SEGMENTS).orEmpty()
                assertTrue("$id $name: empty text", text.isNotBlank())
                assertTrue("$id $name: no segments", segments.isNotEmpty())
                assertEquals(id, answer.getString(SpeechResultKeys.MODEL_ID))
                segments.forEach { segment -> assertTrue(segment.getLong(SpeechResultKeys.START_MILLIS) <= segment.getLong(SpeechResultKeys.END_MILLIS)) }
                val cer = if (name == "en") SpeechDevice.cer(SpeechDevice.EN_REFERENCE, text) else null
                // Coarse regression bound: both models stay below 0.05 on this sample (roadmap P3.5 baseline).
                if (cer != null) assertTrue("$id: CER $cer on the LibriSpeech sample", cer < 0.15)
                SpeechDevice.log(
                    "file model=$id file=$name durationMs=$duration wallMs=$millis rtf=${"%.3f".format(millis.toDouble() / max(duration, 1))} " +
                        "segments=${segments.size} language=${answer.getString(SpeechResultKeys.LANGUAGE)} cer=${cer?.let { "%.3f".format(it) }} text=$text",
                )
            }
        }
    }

    @Test
    fun test3FirstPartialOfARealTimeExternalSession() {
        val samples = SpeechDevice.samples("test-audio/mixed-zh-en.wav")
        val onset = samples.indexOfFirst { abs(it) > 0.05f }.coerceAtLeast(0)
        listOf(STREAMING, SENSE_VOICE).forEach { id ->
            val events = SpeechDevice.Events()
            val opened = SystemClock.elapsedRealtimeNanos()
            val session = SpeechDevice.plugin.openSession(SpeechDevice.request {
                putString(SpeechOptionKeys.MODEL, id)
                putString(SpeechOptionKeys.SOURCE, SpeechContract.SOURCE_EXTERNAL)
                putLong(SpeechOptionKeys.TIMEOUT_MILLIS, 0)
            }, events)
            assertTrue("$id: no ready", events.ready.await(120, TimeUnit.SECONDS))
            val readyMillis = (SystemClock.elapsedRealtimeNanos() - opened) / 1_000_000
            val started = SystemClock.elapsedRealtimeNanos()
            var offset = 0
            while (offset < samples.size) {
                val end = minOf(samples.size, offset + 320)
                session.feed(SpeechDevice.pcm16(samples, offset, end))
                offset = end
                val due = started + offset * 1_000_000_000L / 16_000
                val wait = (due - SystemClock.elapsedRealtimeNanos()) / 1_000_000
                if (wait > 0) SystemClock.sleep(wait)
            }
            session.finish()
            assertTrue("$id: no end", events.awaitEnd(60))
            val onsetAt = started + onset * 1_000_000_000L / 16_000
            val firstPartial = synchronized(events.log) { events.log.firstOrNull { it.second == "partial" }?.first }
            assertNotNull("$id: no partial", firstPartial)
            val types = events.types()
            assertEquals("ready", types.first())
            assertEquals("end", types.last())
            assertEquals(1, types.count { it == "end" })
            assertTrue(types.indexOf("final") < types.lastIndexOf("endpoint"))
            assertEquals(SpeechContract.REASON_STOPPED, events.end()?.getString(SpeechResultKeys.REASON))
            assertFalse(session.isActive)
            SpeechDevice.log(
                "latency model=$id readyMs=$readyMillis firstPartialMs=${(firstPartial!! - onsetAt) / 1_000_000} finals=${events.finals().size} " +
                    "partials=${types.count { it == "partial" }} text=${events.end()?.getString(SpeechResultKeys.TEXT)}",
            )
        }
    }

    @Test
    fun test4LongFiles() {
        val pieces = listOf("mixed-zh-en", "zh", "en", "zh-8k").map { SpeechDevice.samples("test-audio/$it.wav") }
        val gap = FloatArray(8000)
        listOf(30_000L to "30s", 600_000L to "10min").forEach { (millis, label) ->
            val total = (millis * 16).toInt()
            val audio = FloatArray(total)
            var offset = 0
            var index = 0
            while (offset < total) {
                val piece = pieces[index++ % pieces.size] + gap
                val count = minOf(piece.size, total - offset)
                System.arraycopy(piece, 0, audio, offset, count)
                offset += count
            }
            val file = File(SpeechDevice.context.cacheDir, "device-test/long-$label.wav")
            SpeechDevice.writeWav(file, audio)
            listOf(STREAMING, SENSE_VOICE).forEach { id ->
                val peak = PeakPss()
                val started = SystemClock.elapsedRealtimeNanos()
                val answer = peak.during { SpeechDevice.recognize(file, SpeechDevice.request { putString(SpeechOptionKeys.MODEL, id) }) }
                val wall = (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000
                assertNull(answer.getString(SpeechContract.KEY_ERROR_MESSAGE), answer.getString(SpeechContract.KEY_ERROR_CODE))
                val duration = answer.getLong(SpeechResultKeys.DURATION_MILLIS)
                assertTrue("$id $label duration $duration", abs(duration - millis) <= 50)
                @Suppress("DEPRECATION")
                val segments = answer.getParcelableArrayList<Bundle>(SpeechResultKeys.SEGMENTS).orEmpty()
                assertTrue(segments.size > millis / 30_000)
                SpeechDevice.log("long model=$id length=$label wallMs=$wall rtf=${"%.3f".format(wall.toDouble() / duration)} segments=${segments.size} peakPssMiB=${peak.peak}")
            }
            file.delete()
        }
    }

    @Test
    fun test5VoiceActivityAndRefusals() {
        val vadFile = SpeechDevice.asset(VadReference.ASSET)
        val voice = ParcelFileDescriptor.open(vadFile, ParcelFileDescriptor.MODE_READ_ONLY).use { SpeechDevice.plugin.detectVoice(it, SpeechDevice.request()) }
        assertNull(voice.getString(SpeechContract.KEY_ERROR_CODE))
        @Suppress("DEPRECATION")
        val segments = voice.getParcelableArrayList<Bundle>(SpeechResultKeys.SEGMENTS).orEmpty()
        assertTrue("${segments.size} segments", segments.size in 10..20)
        assertTrue(segments.first().getLong(SpeechResultKeys.START_MILLIS) < 100)
        assertEquals(60_000.0, voice.getLong(SpeechResultKeys.DURATION_MILLIS).toDouble(), 100.0)

        val mp3 = File(SpeechDevice.context.cacheDir, "device-test/not-a-wav.mp3").apply { writeBytes("ID3".toByteArray() + ByteArray(4096)) }
        assertEquals(SpeechErrorCodes.UNSUPPORTED_FORMAT, SpeechDevice.recognize(mp3, SpeechDevice.request()).getString(SpeechContract.KEY_ERROR_CODE))
        // 31 minutes of 8-bit mono at 4 kHz: refused from the header, before any model loads.
        val long = File(SpeechDevice.context.cacheDir, "device-test/too-long.wav")
        writeLongHeader(long, 4000 * 60 * 31)
        assertEquals(SpeechErrorCodes.LIMIT_EXCEEDED, SpeechDevice.recognize(long, SpeechDevice.request()).getString(SpeechContract.KEY_ERROR_CODE))
        long.delete()

        val zh = SpeechDevice.asset("test-audio/zh.wav")
        assertEquals(SpeechErrorCodes.INVALID_OPTIONS, SpeechDevice.recognize(zh, SpeechDevice.request { putString(SpeechOptionKeys.MODEL, SpeechDevice.KWS) }).getString(SpeechContract.KEY_ERROR_CODE))
        assertEquals(SpeechErrorCodes.MODEL_MISSING, SpeechDevice.recognize(zh, SpeechDevice.request { putString(SpeechOptionKeys.MODEL, "asr.no.such.model") }).getString(SpeechContract.KEY_ERROR_CODE))

        val hotwords = arrayOf("AutoJs6", "macaw")
        val offline = SpeechDevice.recognize(zh, SpeechDevice.request {
            putString(SpeechOptionKeys.MODEL, SENSE_VOICE)
            putStringArray(SpeechOptionKeys.HOTWORDS, hotwords)
        })
        @Suppress("DEPRECATION")
        val warnings = offline.getParcelableArrayList<Bundle>(SpeechResultKeys.WARNINGS).orEmpty()
        assertEquals(listOf(SpeechErrorCodes.INVALID_OPTIONS), warnings.map { it.getString(SpeechContract.KEY_ERROR_CODE) })
        val started = SystemClock.elapsedRealtimeNanos()
        val streaming = SpeechDevice.recognize(zh, SpeechDevice.request {
            putString(SpeechOptionKeys.MODEL, STREAMING)
            putStringArray(SpeechOptionKeys.HOTWORDS, hotwords)
        })
        @Suppress("DEPRECATION")
        assertTrue(streaming.getParcelableArrayList<Bundle>(SpeechResultKeys.WARNINGS).orEmpty().isEmpty())
        assertTrue(streaming.getString(SpeechResultKeys.TEXT).orEmpty().isNotBlank())
        SpeechDevice.log("refusals ok vadSegments=${segments.size} hotwordsMs=${(SystemClock.elapsedRealtimeNanos() - started) / 1_000_000}")
    }

    private fun writeLongHeader(file: File, frames: Int) {
        file.parentFile?.mkdirs()
        file.outputStream().use { out ->
            val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray()).putInt(36 + frames).put("WAVE".toByteArray()).put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
                .putInt(4000).putInt(4000).putShort(1).putShort(8).put("data".toByteArray()).putInt(frames)
            out.write(header.array())
            val block = ByteArray(64 * 1024) { 0x80.toByte() }
            var left = frames
            while (left > 0) {
                val count = minOf(left, block.size)
                out.write(block, 0, count)
                left -= count
            }
        }
    }

    /** Samples the total PSS every 200 ms while [during] runs. zh-CN: 在 [during] 运行期间每 200 ms 采样一次总 PSS. */
    private class PeakPss {
        @Volatile
        var peak = 0L

        fun <T> during(block: () -> T): T {
            val running = java.util.concurrent.atomic.AtomicBoolean(true)
            val sampler = Thread {
                while (running.get()) {
                    peak = max(peak, SpeechDevice.memory().second)
                    SystemClock.sleep(200)
                }
            }.apply { start() }
            try {
                return block()
            } finally {
                running.set(false)
                sampler.join()
                peak = max(peak, SpeechDevice.memory().second)
            }
        }
    }
}
