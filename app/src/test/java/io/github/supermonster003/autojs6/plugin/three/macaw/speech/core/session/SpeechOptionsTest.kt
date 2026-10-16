package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Request parsing of contract version 1 (roadmap P3.2 - P3.4): defaults, accepted shapes, the ranges shared with the
 * host's script layer, and the error codes of refusals.
 * zh-CN: 契约版本 1 的请求解析 (路线图 P3.2 - P3.4): 默认值, 接受的形态, 与宿主脚本层一致的取值范围, 以及拒绝时的错误码.
 */
class SpeechOptionsTest {

    @Test
    fun `listen defaults follow SpeechLimits`() {
        val options = SpeechOptions.listen(null)
        assertEquals(ListenOptions(), options)
        assertEquals(SpeechContract.SOURCE_MIC, options.source)
        assertEquals(SpeechLimits.DEFAULT_SILENCE_END_MILLIS, options.silenceEndMillis)
        assertEquals(SpeechLimits.DEFAULT_MAX_DURATION_MILLIS, options.maxDurationMillis)
        assertEquals(SpeechLimits.DEFAULT_TIMEOUT_MILLIS, options.timeoutMillis)
        assertEquals(true, options.vad)
        assertEquals(true, options.punctuation)
    }

    @Test
    fun `listen reads every key and ignores unknown ones`() {
        val options = SpeechOptions.listen(
            source(
                "contractVersion" to 1, "model" to "asr.x", "language" to "zh-CN", "source" to "external", "sampleRate" to 16000,
                "vad" to false, "silenceEndMillis" to 800, "maxDurationMillis" to 60_000L, "timeoutMillis" to 0L,
                "hotwords" to arrayOf("macaw", "AutoJs6"), "punctuation" to false, "displayName" to "a.wav", "futureKey" to 3.5,
            ),
        )
        assertEquals(ListenOptions("asr.x", "zh-CN", SpeechContract.SOURCE_EXTERNAL, false, 800, 60_000, 0, listOf("macaw", "AutoJs6"), false), options)
        assertEquals(true, options.isExternal)
    }

    @Test
    fun `listen refusals carry INVALID_OPTIONS, LIMIT_EXCEEDED or PLUGIN_INCOMPATIBLE`() {
        expect(SpeechErrorCodes.PLUGIN_INCOMPATIBLE) { SpeechOptions.listen(source("contractVersion" to 2)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.listen(source("source" to "line-in")) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.listen(source("sampleRate" to 8000)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.listen(source("silenceEndMillis" to 0)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.listen(source("silenceEndMillis" to 60_001L)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.listen(source("maxDurationMillis" to 0L)) }
        expect(SpeechErrorCodes.LIMIT_EXCEEDED) { SpeechOptions.listen(source("maxDurationMillis" to SpeechLimits.MAX_AUDIO_MILLIS + 1)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.listen(source("timeoutMillis" to -1L)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.listen(source("vad" to "yes")) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.listen(source("model" to "")) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.listen(source("language" to "zh_CN")) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.listen(source("hotwords" to arrayOf(" "))) }
        expect(SpeechErrorCodes.LIMIT_EXCEEDED) { SpeechOptions.listen(source("hotwords" to Array(SpeechLimits.MAX_HOTWORDS + 1) { "w$it" })) }
        expect(SpeechErrorCodes.LIMIT_EXCEEDED) { SpeechOptions.listen(source("hotwords" to arrayOf("x".repeat(SpeechLimits.MAX_TOKEN_CHARS + 1)))) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.listen(source("maxDurationMillis" to 1.5)) }
    }

    @Test
    fun `recognize and vad options`() {
        assertEquals(RecognizeOptions(), SpeechOptions.recognize(null))
        val recognize = SpeechOptions.recognize(source("model" to "asr.y", "language" to "en", "vad" to false, "punctuation" to false, "displayName" to "b.wav"))
        assertEquals(RecognizeOptions("asr.y", "en", emptyList(), false, false, "b.wav"), recognize)
        assertEquals(VadOptions(), SpeechOptions.vad(null))
        assertEquals(VadOptions("vad.silero.v5", 0.4f, 100, 300), SpeechOptions.vad(source("model" to "vad.silero.v5", "vadThreshold" to 0.4f, "minSpeechMillis" to 100L, "minSilenceMillis" to 300L)))
        assertEquals(0.5f, SpeechOptions.vad(source("vadThreshold" to 0.5)).threshold)
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.vad(source("vadThreshold" to 1f)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.vad(source("vadThreshold" to 0f)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.vad(source("minSpeechMillis" to -5L)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.vad(source("vadThreshold" to Float.NaN)) }
    }

    @Test
    fun `wake options with keywords and thenListen`() {
        val options = SpeechOptions.wake(
            source(
                "keywords" to listOf(mapOf("keyword" to " \u5c0f\u516d\u5c0f\u516d "), mapOf("keyword" to "hey macaw", "boost" to 2f, "threshold" to 0.3f)),
                "model" to "kws.z",
                "minIntervalMillis" to 500L,
                "thenListen" to mapOf("silenceEndMillis" to 900L),
            ),
        )
        assertEquals(listOf(Keyword("\u5c0f\u516d\u5c0f\u516d"), Keyword("hey macaw", 2f, 0.3f)), options.keywords)
        assertEquals("kws.z", options.model)
        assertEquals(500L, options.minIntervalMillis)
        assertEquals(900L, options.thenListen?.silenceEndMillis)
        assertNull(SpeechOptions.wake(source("keywords" to listOf(mapOf("keyword" to "a")))).thenListen)
        assertEquals(SpeechLimits.DEFAULT_MIN_INTERVAL_MILLIS, SpeechOptions.wake(source("keywords" to listOf(mapOf("keyword" to "a")))).minIntervalMillis)
    }

    @Test
    fun `wake refusals`() {
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.wake(null) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.wake(source("keywords" to emptyList<Any>())) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.wake(source("keywords" to "hey")) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.wake(source("keywords" to listOf("hey"))) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.wake(source("keywords" to listOf(mapOf("keyword" to " ")))) }
        expect(SpeechErrorCodes.LIMIT_EXCEEDED) { SpeechOptions.wake(source("keywords" to List(SpeechLimits.MAX_KEYWORDS + 1) { mapOf("keyword" to "k$it") })) }
        expect(SpeechErrorCodes.LIMIT_EXCEEDED) { SpeechOptions.wake(source("keywords" to listOf(mapOf("keyword" to "k".repeat(129))))) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.wake(source("keywords" to listOf(mapOf("keyword" to "a", "boost" to 101f)))) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.wake(source("keywords" to listOf(mapOf("keyword" to "a", "threshold" to 1.5f)))) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.wake(source("keywords" to listOf(mapOf("keyword" to "a")), "minIntervalMillis" to 600_001L)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.wake(source("keywords" to listOf(mapOf("keyword" to "a")), "thenListen" to true)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.wake(source("keywords" to listOf(mapOf("keyword" to "a")), "thenListen" to mapOf("source" to "external"))) }
        expect(SpeechErrorCodes.LIMIT_EXCEEDED) { SpeechOptions.wake(source("keywords" to listOf(mapOf("keyword" to "a")), "thenListen" to mapOf("maxDurationMillis" to 1_800_001L))) }
    }

    @Test
    fun `synthesize reads text, model, speaker, speed and format`() {
        assertEquals(SynthesizeOptions("hello"), SpeechOptions.synthesize(source("text" to "hello")))
        val options = SpeechOptions.synthesize(
            source("contractVersion" to 1, "text" to "hi", "model" to "tts.x", "speaker" to " 12 ", "speed" to 1.5, "format" to "WAV", "futureKey" to true),
        )
        assertEquals(SynthesizeOptions("hi", "tts.x", "12", 1.5f, "wav"), options)
        assertEquals("7", SpeechOptions.synthesize(source("text" to "hi", "speaker" to 7)).speaker)
        assertEquals("SSB0005", SpeechOptions.synthesize(source("text" to "hi", "speaker" to "SSB0005")).speaker)
        assertEquals(0.5f, SpeechOptions.synthesize(source("text" to "hi", "speed" to 0.5f)).speed)
        assertEquals(2f, SpeechOptions.synthesize(source("text" to "hi", "speed" to 2)).speed)
        val longest = "x".repeat(SpeechLimits.MAX_SYNTH_TEXT_CHARS)
        assertEquals(longest, SpeechOptions.synthesize(source("text" to longest)).text)
    }

    @Test
    fun `synthesize refusals carry INVALID_OPTIONS, LIMIT_EXCEEDED or PLUGIN_INCOMPATIBLE`() {
        expect(SpeechErrorCodes.PLUGIN_INCOMPATIBLE) { SpeechOptions.synthesize(source("contractVersion" to 2, "text" to "hi")) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.synthesize(null) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.synthesize(source("text" to " \n ")) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.synthesize(source("text" to 3)) }
        expect(SpeechErrorCodes.LIMIT_EXCEEDED) { SpeechOptions.synthesize(source("text" to "x".repeat(SpeechLimits.MAX_SYNTH_TEXT_CHARS + 1))) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.synthesize(source("text" to "hi", "speed" to 0.49)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.synthesize(source("text" to "hi", "speed" to 2.01)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.synthesize(source("text" to "hi", "speed" to Double.NaN)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.synthesize(source("text" to "hi", "speed" to "fast")) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.synthesize(source("text" to "hi", "format" to "mp3")) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.synthesize(source("text" to "hi", "speaker" to "")) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.synthesize(source("text" to "hi", "speaker" to -1)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.synthesize(source("text" to "hi", "speaker" to 1.5)) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.synthesize(source("text" to "hi", "speaker" to "s".repeat(SpeechLimits.MAX_TOKEN_CHARS + 1))) }
        expect(SpeechErrorCodes.INVALID_OPTIONS) { SpeechOptions.synthesize(source("text" to "hi", "model" to "")) }
    }

    private fun source(vararg pairs: Pair<String, Any?>) = MapOptionSource(mapOf(*pairs))

    private fun expect(code: String, block: () -> Unit) {
        val error = assertThrows(SpeechException::class.java) { block() }
        assertEquals(error.message, code, error.code)
    }
}
