package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Roadmap P7.6.1 (D95 - D98, D100, D101 E2): the arguments of the four tools as AutoJs6 hands them over (JSON numbers
 * become Integer, Long or Double), their ranges, and the mapping of the plugin's speech errors to tool errors.
 * zh-CN: 路线图 P7.6.1 (D95 - D98, D100, D101 E2): AutoJs6 交来的四个工具的参数 (JSON 数字为 Integer, Long 或 Double), 取值范围, 以及插件
 * 语音错误到工具错误的映射.
 */
class SpeechToolArgumentsTest {

    @Test
    fun `listen reads every argument and converts seconds to milliseconds`() {
        val request = SpeechToolArguments.listen(
            mapOf(
                "language" to " zh-CN ", "silenceMillis" to 300, "noSpeechSeconds" to 30L, "maxSeconds" to 60.0,
                "hotwords" to listOf(" AutoJs6 ", "Macaw"), "prompt" to "  What next?  ",
            ),
        )
        assertEquals(ListenRequest("zh-CN", 300, 30_000, 60_000, listOf("AutoJs6", "Macaw"), "What next?"), request)
        assertEquals(ListenRequest(), SpeechToolArguments.listen(mapOf("language" to null, "hotwords" to null, "prompt" to "   ")))
        assertEquals(ListenRequest(silenceMillis = 5000, noSpeechMillis = 1000, maxMillis = 1000), SpeechToolArguments.listen(mapOf("silenceMillis" to 5000, "noSpeechSeconds" to 1, "maxSeconds" to 1)))
        assertEquals("unknown keys are ignored", ListenRequest(), SpeechToolArguments.listen(mapOf("confidence" to true)))
    }

    @Test
    fun `listen refuses values outside the catalog ranges`() {
        listOf(
            mapOf("silenceMillis" to 299), mapOf("silenceMillis" to 5001), mapOf("silenceMillis" to 1200.5), mapOf("silenceMillis" to "1200"),
            mapOf("noSpeechSeconds" to 0), mapOf("noSpeechSeconds" to 31), mapOf("maxSeconds" to 61), mapOf("maxSeconds" to Double.NaN),
            mapOf("language" to "zh_CN"), mapOf("language" to ""), mapOf("language" to 7), mapOf("language" to "a".repeat(36)),
            mapOf("hotwords" to "AutoJs6"), mapOf("hotwords" to listOf("ok", 3)), mapOf("hotwords" to listOf(" ")),
            mapOf("hotwords" to listOf("x".repeat(129))), mapOf("hotwords" to List(101) { "w$it" }),
            mapOf("prompt" to "x".repeat(201)),
        ).forEach { assertInvalid(it.toString()) { SpeechToolArguments.listen(it) } }
        assertEquals(100, SpeechToolArguments.listen(mapOf("hotwords" to List(100) { "w$it" })).hotwords.size)
        assertEquals("lengths count code points", 200, SpeechToolArguments.listen(mapOf("prompt" to String(Character.toChars(0x1F600)).repeat(200))).prompt!!.codePointCount(0, 400))
    }

    @Test
    fun `speak reads text, language, voice, speed and wait`() {
        assertEquals(SpeakRequest("Hello"), SpeechToolArguments.speak(mapOf("text" to "Hello")))
        assertEquals(
            SpeakRequest("Hello", "en", VoiceChoice("vits-piper-en_US-amy-low", "0"), 1.5f, wait = false),
            SpeechToolArguments.speak(mapOf("text" to "Hello", "language" to "en", "voice" to "vits-piper-en_US-amy-low/0", "speed" to 1.5, "wait" to false)),
        )
        assertEquals("a speaker may be a name", VoiceChoice("sherpa-onnx-vits-zh-ll", "xiaoyu"), SpeechToolArguments.speak(mapOf("text" to "x", "voice" to "sherpa-onnx-vits-zh-ll/xiaoyu")).voice)
        assertEquals("the speaker follows the last slash", VoiceChoice("a/b", "2"), SpeechToolArguments.speak(mapOf("text" to "x", "voice" to "a/b/2")).voice)
        assertEquals(0.5f, SpeechToolArguments.speak(mapOf("text" to "x", "speed" to 0.5)).speed)
        assertEquals(2f, SpeechToolArguments.speak(mapOf("text" to "x", "speed" to 2)).speed)
        assertEquals(4000, SpeechToolArguments.speak(mapOf("text" to "x".repeat(4000))).text.length)
        listOf(
            emptyMap(), mapOf("text" to ""), mapOf("text" to "  \n "), mapOf("text" to "x".repeat(4001)), mapOf("text" to 5),
            mapOf("text" to "x", "voice" to "model"), mapOf("text" to "x", "voice" to "/0"), mapOf("text" to "x", "voice" to "model/"),
            mapOf("text" to "x", "speed" to 0.49), mapOf("text" to "x", "speed" to 2.01), mapOf("text" to "x", "speed" to "fast"),
            mapOf("text" to "x", "wait" to "yes"),
        ).forEach { assertInvalid(it.keys.toString()) { SpeechToolArguments.speak(it) } }
    }

    @Test
    fun `transcribe takes an optional language and model, the file arrives as an input`() {
        assertEquals(TranscribeRequest(), SpeechToolArguments.transcribe(emptyMap()))
        assertEquals(TranscribeRequest("en-US", "sherpa-onnx-whisper-tiny"), SpeechToolArguments.transcribe(mapOf("language" to "en-US", "model" to "sherpa-onnx-whisper-tiny")))
        listOf(mapOf("model" to ""), mapOf("model" to "  "), mapOf("model" to "m".repeat(129)), mapOf("language" to "english!")).forEach {
            assertInvalid(it.toString()) { SpeechToolArguments.transcribe(it) }
        }
    }

    @Test
    fun `synthesize reads wav or m4a and up to 8000 characters`() {
        assertEquals(SynthesizeRequest("Hi"), SpeechToolArguments.synthesize(mapOf("text" to "Hi")))
        assertEquals("wav", SynthesizeRequest("Hi").format)
        assertEquals(
            SynthesizeRequest("Hi", "zh", VoiceChoice("m", "3"), 0.8f, "m4a"),
            SpeechToolArguments.synthesize(mapOf("text" to "Hi", "language" to "zh", "voice" to "m/3", "speed" to 0.8, "format" to "m4a")),
        )
        assertEquals(8000, SpeechToolArguments.synthesize(mapOf("text" to "x".repeat(8000))).text.length)
        listOf(mapOf("text" to "x".repeat(8001)), mapOf("text" to "x", "format" to "mp3"), mapOf("text" to "x", "format" to "WAV"), mapOf("format" to "wav")).forEach {
            assertInvalid(it.keys.toString()) { SpeechToolArguments.synthesize(it) }
        }
    }

    @Test
    fun `messages name the argument but never repeat the text`() {
        val secret = "my bank PIN is 1234 " + "x".repeat(4000)
        val message = try {
            SpeechToolArguments.speak(mapOf("text" to secret))
            fail("too long")
            ""
        } catch (e: PluginToolException) {
            e.message
        }
        assertTrue(message, message.startsWith("text must have 1 to 4000 characters"))
        assertFalse(message.contains("PIN"))
    }

    @Test
    fun `speech errors become the tool errors of D100`() {
        val expected = mapOf(
            SpeechErrorCodes.PERMISSION_DENIED to PluginToolErrorCodes.NOT_READY,
            SpeechErrorCodes.MODEL_MISSING to PluginToolErrorCodes.NOT_READY,
            SpeechErrorCodes.MODEL_LICENSE_NOT_ACCEPTED to PluginToolErrorCodes.NOT_READY,
            SpeechErrorCodes.SESSION_BUSY to PluginToolErrorCodes.BUSY,
            SpeechErrorCodes.INVALID_OPTIONS to PluginToolErrorCodes.INVALID_ARGUMENTS,
            SpeechErrorCodes.LIMIT_EXCEEDED to PluginToolErrorCodes.LIMIT_EXCEEDED,
            SpeechErrorCodes.SESSION_CANCELLED to PluginToolErrorCodes.CANCELLED,
            SpeechErrorCodes.UNSUPPORTED_FORMAT to PluginToolErrorCodes.FAILED,
            SpeechErrorCodes.IO_FAILED to PluginToolErrorCodes.FAILED,
            SpeechErrorCodes.ENGINE_FAILED to PluginToolErrorCodes.FAILED,
            SpeechErrorCodes.SESSION_CLOSED to PluginToolErrorCodes.FAILED,
            SpeechErrorCodes.TIMEOUT to PluginToolErrorCodes.FAILED,
            SpeechErrorCodes.INTERNAL to PluginToolErrorCodes.FAILED,
        )
        expected.forEach { (speech, tool) -> assertEquals(speech, tool, SpeechToolErrors.code(speech)) }
        assertEquals(PluginToolErrorCodes.INPUT_UNREADABLE, SpeechToolErrors.code(SpeechErrorCodes.UNSUPPORTED_FORMAT, input = true))
        assertEquals(PluginToolErrorCodes.INPUT_UNREADABLE, SpeechToolErrors.code(SpeechErrorCodes.IO_FAILED, input = true))
        assertEquals(PluginToolErrorCodes.LIMIT_EXCEEDED, SpeechToolErrors.code(SpeechErrorCodes.LIMIT_EXCEEDED, input = true))

        val error = SpeechToolErrors.of(SpeechException(SpeechErrorCodes.MODEL_MISSING, "No installed asr model speaks fr"))
        assertEquals(PluginToolErrorCodes.NOT_READY, error.code)
        assertEquals("MODEL_MISSING: No installed asr model speaks fr", error.message)
        assertEquals(PluginToolErrorCodes.BUSY, SpeechToolErrors.of(SpeechErrorCodes.SESSION_BUSY, "").code)
        assertEquals("SESSION_BUSY: no detail", SpeechToolErrors.of(SpeechErrorCodes.SESSION_BUSY, "").message)
    }

    private fun assertInvalid(label: String, block: () -> Unit) {
        try {
            block()
            fail("$label should be refused")
        } catch (e: PluginToolException) {
            assertEquals(label, PluginToolErrorCodes.INVALID_ARGUMENTS, e.code)
        }
    }

}
