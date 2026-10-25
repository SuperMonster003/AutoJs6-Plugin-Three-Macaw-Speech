package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.tts

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Speakers and system engine voices (roadmap D19, appendix D Q6): speaker numbers and names, the single speaker
 * warning, unique voice names per language, the `TextToSpeech` language codes and the default voice of a language.
 * zh-CN: 说话人与系统引擎语音 (路线图 D19, 附录 D Q6): 说话人编号与名字, 单说话人警告, 每种语言唯一的语音名, `TextToSpeech` 语言代码与语言的默认语音.
 */
class TtsVoicesTest {

    private val melo = TtsModelVoices("tts.vits.melo.zh-en", listOf("zh", "en"), 1, 44100, isDefault = false)
    private val aishell = TtsModelVoices("tts.vits.icefall.zh-aishell3", listOf("zh"), 3, 8000, isDefault = true, speakerNames = listOf("SSB0005", "SSB0009", "SSB0011"))
    private val piper = TtsModelVoices("tts.vits.piper.en-us-lessac-medium", listOf("en"), 1, 22050, isDefault = false)
    private val catalog = listOf("zh", "en", "ja")

    @Test
    fun `speakers resolve by number or name and single speaker models warn`() {
        val many = TtsSpeakers(174, listOf("SSB0005", "SSB0009"))
        assertEquals(TtsSpeakers.Choice(0, null), many.resolve("m", null))
        assertEquals(TtsSpeakers.Choice(173, null), many.resolve("m", "173"))
        assertEquals(TtsSpeakers.Choice(1, null), many.resolve("m", "SSB0009"))
        assertEquals(SpeechErrorCodes.INVALID_OPTIONS, assertThrows(SpeechException::class.java) { many.resolve("m", "174") }.code)
        assertEquals(SpeechErrorCodes.INVALID_OPTIONS, assertThrows(SpeechException::class.java) { many.resolve("m", "nobody") }.code)
        assertEquals(SpeechErrorCodes.INVALID_OPTIONS, assertThrows(SpeechException::class.java) { TtsSpeakers(103).resolve("m", "zf_001") }.code)
        val single = TtsSpeakers(1)
        assertEquals(TtsSpeakers.Choice(0, null), single.resolve("m", "0"))
        val ignored = single.resolve("m", "5")
        assertEquals(0, ignored.id)
        assertEquals(SpeechErrorCodes.INVALID_OPTIONS, ignored.warning?.code)
        assertTrue(ignored.warning!!.message.contains("single speaker"))
        assertEquals(0, single.resolve("m", "anyone").id)
        assertEquals(listOf("a", "b"), TtsSpeakers.parseNames("a\r\n\n  b  \n"))
    }

    @Test
    fun `every speaker has one uniquely named voice per language`() {
        val table = TtsVoiceTable(listOf(aishell, melo, piper), catalog)
        assertEquals(3 + 2 + 1, table.voices.size)
        assertEquals(table.voices.size, table.voices.map { it.name }.toSet().size)
        val zh = table.voice("tts.vits.melo.zh-en/0")!!
        assertEquals("zh-CN", zh.languageTag)
        assertEquals("zho", zh.iso3Language)
        assertEquals("CHN", zh.iso3Country)
        assertTrue(zh.primary)
        val en = table.voice("tts.vits.melo.zh-en/0/en-US")!!
        assertEquals("eng", en.iso3Language)
        assertEquals("USA", en.iso3Country)
        assertEquals(TtsVoiceTable.QUALITY_HIGH, en.quality)
        assertEquals(TtsVoiceTable.QUALITY_LOW, table.voice("tts.vits.icefall.zh-aishell3/2")!!.quality)
        assertEquals(TtsVoiceTable.QUALITY_HIGH, table.voice("tts.vits.piper.en-us-lessac-medium/0")!!.quality)
        assertEquals("a speaker name reaches the numbered voice", 1, table.voice("tts.vits.icefall.zh-aishell3/SSB0009")!!.speakerId)
        assertNull(table.voice("tts.vits.icefall.zh-aishell3/3"))
        assertNull(table.voice("tts.missing/0"))
        assertNull(table.voice("nothing"))
    }

    @Test
    fun `language codes follow the installed and the catalog models`() {
        val table = TtsVoiceTable(listOf(melo), catalog)
        assertEquals(TtsVoiceTable.LANG_COUNTRY_AVAILABLE, table.availability("zho", "CHN"))
        assertEquals(TtsVoiceTable.LANG_COUNTRY_AVAILABLE, table.availability("zh", "CN"))
        assertEquals(TtsVoiceTable.LANG_AVAILABLE, table.availability("zho", "TWN"))
        assertEquals(TtsVoiceTable.LANG_AVAILABLE, table.availability("eng", ""))
        assertEquals(TtsVoiceTable.LANG_AVAILABLE, table.availability("eng", null))
        assertEquals(TtsVoiceTable.LANG_COUNTRY_AVAILABLE, table.availability("cmn", "CHN"))
        assertEquals(TtsVoiceTable.LANG_MISSING_DATA, table.availability("jpn", "JPN"))
        assertEquals(TtsVoiceTable.LANG_NOT_SUPPORTED, table.availability("deu", "DEU"))
        assertEquals(TtsVoiceTable.LANG_NOT_SUPPORTED, table.availability("", ""))
        assertEquals(TtsVoiceTable.LANG_NOT_SUPPORTED, table.availability(null, null))
        val empty = TtsVoiceTable(emptyList(), catalog)
        assertTrue(empty.isEmpty())
        assertEquals("no model installed: LANG_MISSING_DATA", TtsVoiceTable.LANG_MISSING_DATA, empty.availability("zho", "CHN"))
        assertNull(empty.defaultVoice("zho", "CHN"))
        assertNull(empty.defaultVoice(null, null))
    }

    @Test
    fun `the default voice prefers the default model, then the first language, then the country`() {
        val table = TtsVoiceTable(listOf(melo, aishell, piper), catalog)
        assertEquals("tts.vits.icefall.zh-aishell3/0", table.defaultVoice("zho", "CHN")!!.name)
        assertEquals("tts.vits.icefall.zh-aishell3/0", table.defaultVoice(null, null)!!.name)
        assertEquals("tts.vits.icefall.zh-aishell3/0", table.defaultVoice("", "")!!.name)
        assertEquals("piper speaks English first", "tts.vits.piper.en-us-lessac-medium/0", table.defaultVoice("eng", "USA")!!.name)
        val noPiper = TtsVoiceTable(listOf(melo, aishell), catalog)
        assertEquals("tts.vits.melo.zh-en/0/en-US", noPiper.defaultVoice("eng", "GBR")!!.name)
        assertNull(noPiper.defaultVoice("jpn", "JPN"))
        val noDefault = TtsVoiceTable(listOf(melo, aishell.copy(isDefault = false)), catalog)
        assertEquals("installed order breaks the tie", "tts.vits.melo.zh-en/0", noDefault.defaultVoice("zho", "CHN")!!.name)
    }

    @Test
    fun `the voice data check lists installed locales with their common regions`() {
        val table = TtsVoiceTable(listOf(melo), catalog)
        val available = table.availableLocales()
        assertEquals(listOf("zho-CHN", "eng-USA"), available.take(2))
        assertTrue(available.containsAll(listOf("zho-TWN", "zho-HKG", "eng-GBR", "eng-AUS")))
        assertEquals(available.size, available.toSet().size)
        assertEquals(listOf("jpn-JPN"), table.missingLocales())
        assertEquals(listOf("eng-USA", "jpn-JPN", "zho-CHN"), TtsVoiceTable(emptyList(), catalog).missingLocales())
    }

    @Test
    fun `kokoro speakers 0 to 2 speak english first and speaker 3 is the default`() {
        assertEquals(3, TtsSpeakerHints.defaultSpeaker("tts.kokoro.multi-lang.v1_1", 103))
        assertEquals(0, TtsSpeakerHints.defaultSpeaker("tts.vits.melo.zh-en", 1))
        assertEquals(0, TtsSpeakerHints.defaultSpeaker("tts.kokoro.multi-lang.v1_1", 2))
        assertEquals(emptyList<List<String>>(), TtsSpeakerHints.speakerLanguages("tts.vits.melo.zh-en", 1))
        val languages = TtsSpeakerHints.speakerLanguages("tts.kokoro.multi-lang.v1_1", 103)
        assertEquals(listOf("en", "en", "en", "zh"), languages.take(4).map { it.first() })
        assertEquals(103, languages.size)
        assertEquals(emptySet<Int>(), TtsSpeakerHints.unavailable("tts.kokoro.multi-lang.v1_1", 103))
        assertEquals("", TtsSpeakerHints.engineLanguage("tts.kokoro.multi-lang.v1_1", 50))
        val kokoro = TtsModelVoices("tts.kokoro.multi-lang.v1_1", listOf("zh", "en"), 103, 24000, isDefault = true, speakerLanguages = languages, defaultSpeaker = 3)
        val table = TtsVoiceTable(listOf(kokoro), catalog)
        assertEquals(206, table.voices.size)
        assertEquals("en-US", table.voice("tts.kokoro.multi-lang.v1_1/0")!!.languageTag)
        assertEquals("zh-CN", table.voice("tts.kokoro.multi-lang.v1_1/0/zh-CN")!!.languageTag)
        assertEquals("zh-CN", table.voice("tts.kokoro.multi-lang.v1_1/3")!!.languageTag)
        assertEquals("en-US", table.voice("tts.kokoro.multi-lang.v1_1/3/en-US")!!.languageTag)
        assertEquals("tts.kokoro.multi-lang.v1_1/3", table.defaultVoice("zho", "CHN")!!.name)
        assertEquals("tts.kokoro.multi-lang.v1_1/3", table.defaultVoice(null, null)!!.name)
        assertEquals("an English speaker reads English", "tts.kokoro.multi-lang.v1_1/0", table.defaultVoice("eng", "USA")!!.name)
        assertEquals(TtsSpeakers.Choice(3, null), TtsSpeakers(103, defaultId = 3).resolve("k", null))
        assertEquals(TtsSpeakers.Choice(0, null), TtsSpeakers(103, defaultId = 3).resolve("k", "0"))
    }

    @Test
    fun `kokoro v1_0 speakers speak their own language with the sherpa-onnx lang it needs`() {
        val id = TtsSpeakerHints.KOKORO_V1_0
        val languages = TtsSpeakerHints.speakerLanguages(id, 54)
        assertEquals(54, languages.size)
        assertEquals(listOf("en"), languages[3])
        assertEquals(listOf("en-GB"), languages[20])
        assertEquals(listOf("es"), languages[28])
        assertEquals(listOf("es"), languages[53])
        assertEquals(listOf("pt-BR"), languages[42])
        assertEquals(listOf("zh", "en"), languages[45])
        assertEquals(setOf(37, 38, 39, 40, 41), TtsSpeakerHints.unavailable(id, 54))
        assertEquals(mapOf(0 to "", 20 to "", 28 to "es", 30 to "fr", 31 to "hi", 35 to "it", 42 to "pt-br", 45 to "", 53 to "es"),
                     listOf(0, 20, 28, 30, 31, 35, 42, 45, 53).associateWith { TtsSpeakerHints.engineLanguage(id, it) })
        assertEquals("af_heart", TtsSpeakerHints.names(id)[3])
        assertEquals("em_santa", TtsSpeakerHints.names(id)[53])
        assertEquals(3, TtsSpeakerHints.defaultSpeaker(id, 54))

        val kokoro = TtsModelVoices(id, listOf("en", "zh", "es", "fr", "hi", "it", "pt"), 54, 24000, isDefault = true, speakerLanguages = languages, defaultSpeaker = 3)
        val table = TtsVoiceTable(listOf(kokoro), catalog)
        // 20 American and 8 British English speakers, 3 Spanish, 1 French, 4 Hindi, 2 Italian, 3 Portuguese and 8 Chinese
        // speakers who also read English; no Japanese voice.
        assertEquals(28 + 3 + 1 + 4 + 2 + 3 + 8 * 2, table.voices.size)
        assertTrue(table.voices.none { it.speakerId in 37..41 })
        assertEquals("$id/3", table.defaultVoice(null, null)!!.name)
        assertEquals("$id/28", table.defaultVoice("spa", "ESP")!!.name)
        assertEquals("$id/20", table.defaultVoice("eng", "GBR")!!.name)
        assertEquals("hi-IN", table.voice("$id/31")!!.languageTag)
        assertEquals("$id/45/en-US", table.voice("$id/45/en-US")!!.name)
        assertTrue(table.voices.none { it.iso3Language == "jpn" })

        val speakers = TtsSpeakers(54, TtsSpeakerHints.names(id), 3, TtsSpeakerHints.unavailable(id, 54))
        assertEquals(TtsSpeakers.Choice(28, null), speakers.resolve(id, "ef_dora"))
        assertEquals(SpeechErrorCodes.INVALID_OPTIONS, assertThrows(SpeechException::class.java) { speakers.resolve(id, "37") }.code)
        assertEquals(SpeechErrorCodes.INVALID_OPTIONS, assertThrows(SpeechException::class.java) { speakers.resolve(id, "jm_kumo") }.code)
    }

    @Test
    fun `speed maps the android rate`() {
        assertEquals(1f, TtsSpeed.fromAndroidRate(100))
        assertEquals(2.5f, TtsSpeed.fromAndroidRate(250))
        assertEquals(TtsSpeed.MIN_ENGINE, TtsSpeed.fromAndroidRate(10))
        assertEquals(TtsSpeed.MAX_ENGINE, TtsSpeed.fromAndroidRate(600))
        assertEquals(1f, TtsSpeed.fromAndroidRate(0))
    }
}
