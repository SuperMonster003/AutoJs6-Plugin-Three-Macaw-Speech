package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.KeywordEngine
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.runtime.SherpaEngines
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.DictationEvents
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Keyword
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.ListenOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Utterance
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.WakeEvents
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.WakeOptions
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.WakeSession
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import org.autojs.plugin.speech.api.SpeechContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import kotlin.random.Random

/**
 * Keyword spotting on a device with `kws.zipformer.zh-en.2025-12-20` (roadmap D18, P3.4, appendix D Q4): the spotter
 * loads with an empty built-in keyword list and the session keywords spelled by the plugin (the spike of Q4), each
 * upstream keyword recording is spotted, five repetitions of every recording at full and at low level give the hit
 * rate, ten minutes of other speech give the false wakes of two custom keywords, and a wake session with `thenListen`
 * runs one dictation round on the real recognizer after a hit. Results are `MacawDeviceRun` log lines.
 * zh-CN: 在设备上以 `kws.zipformer.zh-en.2025-12-20` 检测关键词 (路线图 D18, P3.4, 附录 D Q4): 检测器以空的内置关键词列表加载, 会话关键词由插件
 * 拼写 (Q4 的 spike); 逐个检测上游关键词录音; 每段录音在原音量与低音量下各重复五次得出命中率; 十分钟其他语音得出两个自定义关键词的误唤醒次数;
 * 带 `thenListen` 的唤醒会话在命中后以真实识别器进行一轮听写. 结果为 `MacawDeviceRun` 日志行.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class KeywordSpottingDeviceTest {

    private val runtime get() = SpeechRuntime.get(SpeechDevice.context)

    private val keywords = listOf("文森特卡索", "周望军", "朱丽楠", "蒋友伯", "女儿", "法国", "见面会", "落实", "LIGHT UP", "LOVELY CHILD").map { Keyword(it) }

    // kws-zh-3 .. kws-zh-6 and en hold the keywords; kws-zh-0 .. kws-zh-2 hold none of them and join the background audio.
    private val recordings = (3..6).map { "kws-zh-$it" } + "en"

    private val raw = (0..6).map { "kws-zh-$it" } + "en"

    @Before
    fun model() {
        assumeTrue("install the models first (SpeechModelSetupDeviceTest, -e macawModels true)", SpeechDevice.installed(SpeechDevice.KWS))
    }

    private fun <T> withSpotter(block: (KeywordEngine, String) -> T): T {
        val model = runtime.resolve(SpeechContract.KIND_KWS, SpeechDevice.KWS)
        val buffer = runtime.keywordBuffer(model, keywords)
        return runtime.acquireKeywords(model).use { lease -> block(lease.engine, buffer) }
    }

    private fun spot(engine: KeywordEngine, buffer: String, samples: FloatArray, count: Int = keywords.size): List<Int> {
        val hits = ArrayList<Int>()
        engine.stream(buffer, count).use { stream ->
            var offset = 0
            while (offset < samples.size) {
                val end = minOf(samples.size, offset + 320)
                stream.accept(samples.copyOfRange(offset, end))
                stream.poll()?.let { hits += it }
                offset = end
            }
            repeat(50) {
                stream.accept(FloatArray(320))
                stream.poll()?.let { hits += it }
            }
        }
        return hits
    }

    @Test
    fun test0RawTextIsNotAKeyword() = withSpotter { engine, _ ->
        // Appendix D Q4 spike: sherpa-onnx 1.13.8 takes keywords as model tokens only; raw text fails to encode, the JNI
        // side answers a null stream and the Kotlin OnlineStream refuses it (IllegalArgumentException).
        listOf("小六小六 @raw", "hey macaw @raw").forEach { text ->
            val outcome = runCatching { engine.spotter.createStream(text) }
            val stream = outcome.getOrNull()
            val refused = outcome.exceptionOrNull() is IllegalArgumentException || stream?.ptr == 0L
            SpeechDevice.log("kws spike rawText=${text.substringBefore(" @")} refused=$refused (${outcome.exceptionOrNull()?.message ?: "pointer ${stream?.ptr}"})")
            if (stream != null && stream.ptr != 0L) stream.release()
            assertTrue(refused)
        }
    }

    @Test
    fun test0RawSpotterResults() = withSpotter { engine, buffer ->
        // The raw KeywordSpotterResult of the upstream recordings, to compare with the plugin's stream (appendix D Q4).
        raw.forEach { name ->
            val samples = SpeechDevice.samples("test-audio/$name.wav")
            val stream = engine.spotter.createStream(buffer)
            val found = ArrayList<String>()
            var offset = 0
            while (offset < samples.size + 16_000) {
                val chunk = if (offset < samples.size) samples.copyOfRange(offset, minOf(samples.size, offset + 3200)) else FloatArray(3200)
                stream.acceptWaveform(chunk, 16_000)
                while (engine.spotter.isReady(stream)) {
                    engine.spotter.decode(stream)
                    val result = engine.spotter.getResult(stream)
                    if (result.keyword.isNotBlank()) {
                        found += "${result.keyword}[${result.tokens.joinToString(" ")}]"
                        engine.spotter.reset(stream)
                    }
                }
                offset += 3200
            }
            stream.release()
            SpeechDevice.log("kws raw file=$name results=${found.joinToString(",")}")
        }
    }

    @Test
    fun test1EveryUpstreamRecordingIsSpotted() = withSpotter { engine, buffer ->
        SpeechDevice.log("kws spike emptyKeywordsFile=accepted buffer=${buffer.replace('/', '|')}")
        val missing = recordings.filter { name ->
            val hits = spot(engine, buffer, SpeechDevice.samples("test-audio/$name.wav"))
            SpeechDevice.log("kws file=$name hits=${hits.joinToString(",") { keywords[it].text }}")
            hits.isEmpty()
        }
        assertTrue("no keyword in $missing", missing.isEmpty())
    }

    @Test
    fun test2HitRateOfFiveRepetitions() = withSpotter { engine, buffer ->
        val silence = FloatArray(24_000)
        listOf(1.0f, 0.25f).forEach { gain ->
            var hits = 0
            var expected = 0
            recordings.forEach { name ->
                val once = SpeechDevice.samples("test-audio/$name.wav").map { it * gain }.toFloatArray()
                val targets = spot(engine, buffer, SpeechDevice.samples("test-audio/$name.wav")).toSet()
                val audio = (0 until 5).fold(FloatArray(0)) { acc, _ -> acc + once + silence }
                val found = spot(engine, buffer, audio)
                targets.forEach { target ->
                    val count = found.count { it == target }
                    hits += minOf(count, 5)
                    expected += 5
                    SpeechDevice.log("kws repeat gain=$gain file=$name keyword=$target hits=$count/5")
                }
                val others = found.count { it !in targets }
                if (others > 0) SpeechDevice.log("kws repeat gain=$gain file=$name otherHits=$others")
            }
            SpeechDevice.log("kws hitRate gain=$gain hits=$hits/$expected")
            if (gain == 1.0f) assertTrue("hit rate $hits/$expected", hits * 10 >= expected * 9)
        }
    }

    @Test
    fun test3FalseWakesInTenMinutesOfOtherSpeech() {
        val model = runtime.resolve(SpeechContract.KIND_KWS, SpeechDevice.KWS)
        val custom = listOf(Keyword("小六小六"), Keyword("hey macaw"))
        val buffer = runtime.keywordBuffer(model, custom)
        val pieces = listOf("mixed-zh-en", "zh", "zh-8k", "kws-zh-0", "kws-zh-1", "kws-zh-2").map { SpeechDevice.samples("test-audio/$it.wav") }
        val random = Random(7)
        val total = 16_000 * 600
        val audio = FloatArray(total)
        var offset = 0
        var index = 0
        while (offset < total) {
            val piece = pieces[index++ % pieces.size]
            val count = minOf(piece.size, total - offset)
            System.arraycopy(piece, 0, audio, offset, count)
            offset += count
            val gap = minOf(16_000, total - offset)
            for (i in 0 until gap) audio[offset + i] = (random.nextFloat() - 0.5f) * 0.01f
            offset += gap
        }
        val started = System.nanoTime()
        val hits = runtime.acquireKeywords(model).use { lease -> spot(lease.engine, buffer, audio, custom.size) }
        val seconds = (System.nanoTime() - started) / 1e9
        SpeechDevice.log("kws falseWakes minutes=10 keywords=${custom.joinToString("|") { it.text }} hits=${hits.size} rtf=${"%.3f".format(seconds / 600)}")
    }

    @Test
    fun test4WakeRoundOnTheRealRecognizer() {
        assumeTrue(SpeechDevice.installed(SpeechDevice.STREAMING))
        val kws = runtime.resolve(SpeechContract.KIND_KWS, SpeechDevice.KWS)
        val asr = runtime.resolve(SpeechContract.KIND_ASR, SpeechDevice.STREAMING)
        val keyword = withSpotter { engine, buffer -> spot(engine, buffer, SpeechDevice.samples("test-audio/kws-zh-6.wav")).first() }
        val options = WakeOptions(listOf(keywords[keyword]), SpeechDevice.KWS, thenListen = ListenOptions())
        val log = ArrayList<String>()
        val wakeEvents = object : WakeEvents {
            override fun ready(sessionId: String, modelId: String) { log += "wake-ready" }
            override fun wake(keyword: String, score: Float, timestampMillis: Long, round: Int?) { log += "wake:$keyword:$round" }
            override fun warning(code: String, message: String) { log += "wake-warning:$code" }
            override fun error(code: String, message: String) { log += "wake-error:$code:$message" }
            override fun end(reason: String) { log += "wake-end:$reason" }
        }
        val listenEvents = object : DictationEvents {
            override fun ready(sessionId: String, modelId: String, round: Int?) { log += "ready:$round" }
            override fun partial(text: String, segmentIndex: Int) = Unit
            override fun final(utterance: Utterance) { log += "final:${utterance.text}" }
            override fun endpoint() { log += "endpoint" }
            override fun voiceActivity(active: Boolean) = Unit
            override fun warning(code: String, message: String) { log += "warning:$code" }
            override fun error(code: String, message: String) { log += "error:$code:$message" }
            override fun end(reason: String, text: String, round: Int?) { log += "end:$reason:$round" }
        }
        val buffer = runtime.keywordBuffer(kws, options.keywords)
        runtime.acquireKeywords(kws).use { kwsLease ->
            runtime.acquireAsr(asr, null, hotwords = false).use { asrLease ->
                val session = WakeSession("device-wake", options, wakeEvents, listenEvents, SherpaEngines.DEFAULT_KEYWORD_THRESHOLD)
                session.start(kws.modelId, kwsLease.engine.stream(buffer, 1), asr.modelId) { sink -> runtime.dictation(asrLease.engine, options.thenListen!!, sink) }
                val audio = SpeechDevice.samples("test-audio/kws-zh-6.wav") + FloatArray(4800) + SpeechDevice.samples("test-audio/mixed-zh-en.wav") + FloatArray(48_000)
                var offset = 0
                while (offset < audio.size) {
                    val end = minOf(audio.size, offset + 320)
                    session.accept(audio.copyOfRange(offset, end))
                    offset = end
                }
                session.finish()
            }
        }
        SpeechDevice.log("kws round log=${log.joinToString("|")}")
        assertEquals("wake-ready", log.first())
        assertTrue(log.any { it.startsWith("wake:") && it.endsWith(":0") })
        assertTrue(log.any { it.startsWith("final:") })
        assertTrue(log.contains("end:stopped:0"))
        assertEquals("wake-end:stopped", log.last())
    }
}
