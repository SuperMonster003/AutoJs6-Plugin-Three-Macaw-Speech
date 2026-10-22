package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJson
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsModelVoices
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts.TtsVoiceTable
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P7.6.3 (D96, D100, D101 E2): the pure rules of `speech_speak`: the language guessed from the text, the voice and
 * engine plan on the installed voices, the messages of the engine problems and errors, and the shape of the result.
 * zh-CN: 路线图 P7.6.3 (D96, D100, D101 E2): `speech_speak` 的纯规则: 从文字推断的语言, 按已装语音制定的语音与引擎选择, 引擎问题与错误的信息, 以及
 * 结果的形态.
 */
class SpeakRulesTest {

    private val melo = TtsModelVoices("tts.vits.melo.zh-en", listOf("zh", "en"), 1, 44100, isDefault = false)
    private val aishell = TtsModelVoices("tts.vits.icefall.zh-aishell3", listOf("zh"), 3, 8000, isDefault = true, speakerNames = listOf("SSB0005", "SSB0009", "SSB0011"))
    private val piper = TtsModelVoices("tts.vits.piper.en-us-lessac-medium", listOf("en"), 1, 22050, isDefault = false)
    private val table = TtsVoiceTable(listOf(melo, aishell, piper))

    @Test
    fun `the language is guessed from the script of the text`() {
        assertEquals("zh", SpeakRules.guessLanguage("打开设置"))
        assertEquals("ja", SpeakRules.guessLanguage("日本語のテキスト"))
        assertEquals("ja", SpeakRules.guessLanguage("カタカナ"))
        assertEquals("ko", SpeakRules.guessLanguage("안녕하세요"))
        assertEquals("ru", SpeakRules.guessLanguage("Привет"))
        assertEquals("ar", SpeakRules.guessLanguage("مرحبا"))
        assertEquals("en", SpeakRules.guessLanguage("Hello, AutoJs6"))
        assertEquals("en", SpeakRules.guessLanguage("42 ..."))
    }

    @Test
    fun `without a voice the language decides and only an installed model brings the plugin's engine`() {
        val chinese = SpeakRules.plan(SpeakRequest("打开设置"), table)
        assertEquals(SpeakPlan("zh", table.voice("tts.vits.icefall.zh-aishell3/0")), chinese)
        assertEquals("the default model of the language", "tts.vits.icefall.zh-aishell3/0", chinese.voice!!.name)
        assertEquals("tts.vits.melo.zh-en/0/en-US", SpeakRules.plan(SpeakRequest("hello", language = "en-GB"), TtsVoiceTable(listOf(melo))).voice!!.name)
        assertEquals("tts.vits.piper.en-us-lessac-medium/0", SpeakRules.plan(SpeakRequest("hello"), table).voice!!.name)
        val french = SpeakRules.plan(SpeakRequest("bonjour", language = "fr"), table)
        assertEquals(SpeakPlan("fr", null), french)
        assertNull("no model, no voice", SpeakRules.plan(SpeakRequest("hello"), TtsVoiceTable(emptyList())).voice)
    }

    @Test
    fun `a named voice reads in its language or in the requested one of the same speaker`() {
        val byName = SpeakRules.plan(SpeakRequest("你好", voice = VoiceChoice("tts.vits.icefall.zh-aishell3", "SSB0009")), table)
        assertEquals(SpeakPlan("zh-CN", table.voice("tts.vits.icefall.zh-aishell3/1"), named = true), byName)
        val english = SpeakRules.plan(SpeakRequest("hello", language = "en", voice = VoiceChoice("tts.vits.melo.zh-en", "0")), table)
        assertEquals("the same speaker in English", "tts.vits.melo.zh-en/0/en-US", english.voice!!.name)
        assertTrue(english.named)
        assertEquals("en", english.language)
        assertEquals(
            "a variant name is accepted as well",
            "tts.vits.melo.zh-en/0/en-US",
            SpeakRules.plan(SpeakRequest("hello", voice = VoiceChoice("tts.vits.melo.zh-en/0", "en-US")), table).voice!!.name,
        )
    }

    @Test
    fun `a voice that cannot speak the requested language is ignored with a warning`() {
        val plan = SpeakRules.plan(SpeakRequest("你好", language = "zh", voice = VoiceChoice("tts.vits.piper.en-us-lessac-medium", "0")), table)
        assertEquals("tts.vits.icefall.zh-aishell3/0", plan.voice!!.name)
        assertFalse(plan.named)
        assertEquals(
            listOf(ToolWarning(SpeechErrorCodes.INVALID_OPTIONS, "The voice tts.vits.piper.en-us-lessac-medium/0 does not speak 'zh' and was ignored")),
            plan.warnings,
        )
        val unknown = assertThrows(PluginToolException::class.java) { SpeakRules.plan(SpeakRequest("hi", voice = VoiceChoice("tts.vits.piper.en-us-lessac-medium", "7")), table) }
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, unknown.code)
        assertEquals(
            "3-Macaw Speech has no installed voice tts.vits.piper.en-us-lessac-medium/7; a voice is <modelId>/<speaker> of an installed text-to-speech model",
            unknown.message,
        )
    }

    @Test
    fun `messages name every engine and its problem`() {
        assertEquals("its voice data for the language is not installed", SpeakRules.languageProblem(TtsVoiceTable.LANG_MISSING_DATA))
        assertEquals("it does not speak the language", SpeakRules.languageProblem(TtsVoiceTable.LANG_NOT_SUPPORTED))
        assertEquals("3-Macaw Speech's engine", SpeakRules.engineLabel(true, "io.github.x"))
        assertEquals("the system default engine com.example.tts", SpeakRules.engineLabel(false, "com.example.tts"))
        assertEquals("the system default engine", SpeakRules.engineLabel(false, ""))
        assertEquals(
            "No text-to-speech engine can read 'fr' (3-Macaw Speech's engine: no installed voice speaks the language; the system default engine " +
                "com.example.tts: its voice data for the language is not installed). To fix this, download a text-to-speech model of 3-Macaw Speech " +
                "that speaks the language, or install the voice data of the language for the system engine",
            SpeakRules.unavailable(
                "fr",
                listOf("3-Macaw Speech's engine" to "no installed voice speaks the language", "the system default engine com.example.tts" to "its voice data for the language is not installed"),
            ),
        )
        assertEquals(
            ToolWarning(SpeechErrorCodes.ENGINE_FAILED, "3-Macaw Speech's engine could not read (it did not start (-1)); the system default engine read instead and the voice was ignored"),
            SpeakRules.fallbackWarning("it did not start (-1)", named = true),
        )
        assertEquals(
            "3-Macaw Speech's engine could not read (it refused the text); the system default engine read instead",
            SpeakRules.fallbackWarning("it refused the text", named = false).message,
        )
    }

    @Test
    fun `engine errors map to tool codes`() {
        val expected = mapOf(
            SpeakRules.ERROR_NOT_INSTALLED_YET to (PluginToolErrorCodes.NOT_READY to "The text-to-speech engine com.example.tts has not installed the voice data yet (ERROR_NOT_INSTALLED_YET)"),
            SpeakRules.ERROR_NETWORK to (PluginToolErrorCodes.FAILED to "The text-to-speech engine com.example.tts needs the network and could not reach it (ERROR_NETWORK)"),
            SpeakRules.ERROR_NETWORK_TIMEOUT to (PluginToolErrorCodes.FAILED to "The text-to-speech engine com.example.tts needs the network and could not reach it (ERROR_NETWORK_TIMEOUT)"),
            SpeakRules.ERROR_INVALID_REQUEST to (PluginToolErrorCodes.FAILED to "The text-to-speech engine com.example.tts refused the request (ERROR_INVALID_REQUEST)"),
            SpeakRules.ERROR_OUTPUT to (PluginToolErrorCodes.FAILED to "The text-to-speech engine com.example.tts could not play the audio (ERROR_OUTPUT)"),
            SpeakRules.ERROR_SYNTHESIS to (PluginToolErrorCodes.FAILED to "The text-to-speech engine com.example.tts could not synthesize the text (ERROR_SYNTHESIS)"),
            SpeakRules.ERROR_SERVICE to (PluginToolErrorCodes.FAILED to "The text-to-speech engine com.example.tts failed (ERROR_SERVICE)"),
            SpeakRules.ERROR to (PluginToolErrorCodes.FAILED to "The text-to-speech engine com.example.tts failed (ERROR)"),
            -42 to (PluginToolErrorCodes.FAILED to "The text-to-speech engine com.example.tts failed (error -42)"),
        )
        expected.forEach { (code, pair) ->
            val error = SpeakRules.engineError(code, "com.example.tts")
            assertEquals(pair, error.code to error.message)
        }
        assertEquals("The text-to-speech engine of the system failed (ERROR)", SpeakRules.engineError(SpeakRules.ERROR, "").message)
        assertEquals(PluginToolErrorCodes.BUSY, SpeakRules.busy().code)
        assertEquals(PluginToolErrorCodes.CANCELLED, SpeakRules.cancelled().code)
        val expired = SpeakRules.expired()
        assertEquals(PluginToolErrorCodes.FAILED, expired.code)
        assertEquals("TIMEOUT: speech_speak ran out of time before reading ended; reading stopped", expired.message)
    }

    @Test
    fun `the result has the catalog's keys in order`() {
        val warning = ToolWarning(SpeechErrorCodes.ENGINE_FAILED, "fallback")
        val own = SpeakReport("io.github.own", "zh", "tts.vits.melo.zh-en/0", completed = true, durationMillis = 2500, warnings = listOf(warning))
        val result = SpeakRules.result(own, "io.github.own")
        assertEquals(listOf("engine", "enginePackage", "language", "voice", "completed", "durationMillis", "warnings"), result.keys.toList())
        assertEquals(
            """{"engine":"plugin","enginePackage":"io.github.own","language":"zh","voice":"tts.vits.melo.zh-en/0","completed":true,"durationMillis":2500,""" +
                """"warnings":[{"code":"ENGINE_FAILED","message":"fallback"}]}""",
            String(StrictJson.write(result), Charsets.UTF_8).trimEnd(),
        )
        val started = SpeakRules.result(SpeakReport("com.example.tts", "fr", null, completed = false, durationMillis = null, warnings = emptyList()), "io.github.own")
        assertEquals("no voice of the system engine, no time before the end", listOf("engine", "enginePackage", "language", "completed", "warnings"), started.keys.toList())
        assertEquals(SpeechToolCatalog.ENGINE_SYSTEM, started["engine"])
        assertEquals(false, started["completed"])
    }

}
