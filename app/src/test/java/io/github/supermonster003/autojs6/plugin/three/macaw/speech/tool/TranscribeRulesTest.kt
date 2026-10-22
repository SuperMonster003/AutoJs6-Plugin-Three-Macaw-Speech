package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CommercialUse
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelEntry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelStore
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJson
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileTranscript
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SpeechWarning
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.Utterance
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import org.autojs.plugin.tool.api.PluginToolLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap P7.6.7 (D97, D100, D101 E2): the pure rules of `speech_transcribe`: the model by language or by name, the
 * result of the catalog's output schema, its trimming at the result ceilings and the errors of a stopped transcription.
 * zh-CN: 路线图 P7.6.7 (D97, D100, D101 E2): `speech_transcribe` 的纯规则: 按语言或按名称选择的模型, 符合目录输出 schema 的结果, 在结果上限处的裁剪,
 * 以及被打断的转写的错误.
 */
class TranscribeRulesTest {

    private fun asr(id: String, vararg languages: String, isDefault: Boolean = false, recommended: Boolean = false) = ModelEntry(
        id = id, kind = SpeechContract.KIND_ASR, modelType = "online-transducer", name = id, languages = languages.toList(), sizeBytes = 1000,
        installedBytes = 2000, installed = true, isDefault = isDefault, recommended = recommended, version = 1,
        licenseSpdx = "Apache-2.0", commercialUse = CommercialUse.ALLOWED, noticeKey = null, noticeUrl = null, speakers = null, sampleRate = 16000,
        source = ModelStore.SOURCE_CATALOG,
    )

    private val senseVoice = asr("asr.sense-voice", "zh", "en", "ja", "ko", "yue", isDefault = true, recommended = true)
    private val english = asr("asr.en", "en")
    private val russian = asr("asr.ru", "ru")

    private fun transcript(segments: List<String>, language: String? = "zh", warnings: List<SpeechWarning> = emptyList()) = FileTranscript(
        text = segments.joinToString(""),
        segments = segments.mapIndexed { index, text -> Utterance(text, index, index * 2000L, index * 2000L + 1500, language) },
        language = language,
        durationMillis = segments.size * 2000L,
        modelId = "asr.sense-voice",
        warnings = warnings,
    )

    private fun parse(json: String): Map<String, Any?> = StrictJson.parseObject(json.toByteArray(Charsets.UTF_8))

    @Test
    fun `the model follows the language like speech_listen and a named model goes to the runtime`() {
        val models = listOf(senseVoice, english, russian)
        assertEquals(TranscribePlan("asr.sense-voice"), TranscribeRules.plan(TranscribeRequest(), models))
        assertEquals(TranscribePlan("asr.sense-voice"), TranscribeRules.plan(TranscribeRequest(language = "en-US"), models))
        assertEquals(TranscribePlan("asr.ru"), TranscribeRules.plan(TranscribeRequest(language = "ru"), models))
        assertEquals(
            TranscribePlan(
                "asr.sense-voice",
                listOf(ToolWarning(SpeechErrorCodes.MODEL_MISSING, "No installed recognition model covers the language 'fr'; asr.sense-voice transcribed instead")),
            ),
            TranscribeRules.plan(TranscribeRequest(language = "fr"), models),
        )
        assertEquals("the recommended model without a default", "asr.en", TranscribeRules.plan(TranscribeRequest(), listOf(russian, english.copy(recommended = true))).modelId)
        assertEquals("a named model is not checked here", TranscribePlan("asr.unknown"), TranscribeRules.plan(TranscribeRequest(language = "fr", model = "asr.unknown"), emptyList()))
        val none = assertThrows(PluginToolException::class.java) { TranscribeRules.plan(TranscribeRequest(language = "en"), emptyList()) }
        assertEquals(PluginToolErrorCodes.NOT_READY, none.code)
        assertEquals("MODEL_MISSING: No speech recognition model is installed; download one on the home screen of 3-Macaw Speech", none.message)
    }

    @Test
    fun `the result has the catalog's keys in order with the warnings of the plan and the runtime`() {
        val runtimeWarning = SpeechWarning(SpeechErrorCodes.INVALID_OPTIONS, "asr.sense-voice does not know the language 'fr'; it detects the language itself")
        val planWarning = ToolWarning(SpeechErrorCodes.MODEL_MISSING, "fallback")
        val json = TranscribeRules.result(transcript(listOf("你好.", "再见."), warnings = listOf(runtimeWarning)), listOf(planWarning, planWarning))
        assertEquals(
            """{"text":"你好.再见.","segments":[{"startMillis":0,"endMillis":1500,"text":"你好."},{"startMillis":2000,"endMillis":3500,"text":"再见."}],""" +
                """"language":"zh","modelId":"asr.sense-voice","durationMillis":4000,"warnings":[{"code":"MODEL_MISSING","message":"fallback"},""" +
                """{"code":"INVALID_OPTIONS","message":"asr.sense-voice does not know the language 'fr'; it detects the language itself"}]}""",
            json,
        )
        val unknown = parse(TranscribeRules.result(transcript(emptyList(), language = null), emptyList()))
        assertEquals("no language, no key", listOf("text", "segments", "modelId", "durationMillis", "warnings"), unknown.keys.toList())
        assertEquals("", unknown["text"])
    }

    @Test
    fun `a result past the ceilings keeps the segment times without text, then fails`() {
        val segments = List(40) { "segment number $it of the long recording." }
        val full = TranscribeRules.result(transcript(segments), emptyList())
        val trimmedSize = full.length - segments.sumOf { ""","text":"$it"""".length }
        val trimmed = parse(TranscribeRules.result(transcript(segments), emptyList(), maxChars = full.length - 1))
        assertEquals(segments.joinToString(""), trimmed["text"])
        val kept = trimmed["segments"] as List<*>
        assertEquals(40, kept.size)
        assertEquals(listOf("startMillis", "endMillis"), (kept[0] as Map<*, *>).keys.toList())
        assertEquals(listOf(mapOf("code" to SpeechErrorCodes.LIMIT_EXCEEDED, "message" to TranscribeRules.TRIMMED)), trimmed["warnings"])
        val tooLarge = assertThrows(PluginToolException::class.java) { TranscribeRules.result(transcript(segments), emptyList(), maxChars = trimmedSize) }
        assertEquals(PluginToolErrorCodes.LIMIT_EXCEEDED, tooLarge.code)
        assertTrue(tooLarge.message.startsWith("The transcript of ${segments.sumOf { it.length }} characters is too large for a tool result"))
    }

    @Test
    fun `UTF-8 bytes and UTF-16 units are both ceilings`() {
        val chinese = List(10) { "这是一段很长的中文转写内容." }
        val full = TranscribeRules.result(transcript(chinese), emptyList())
        val bytes = full.toByteArray(Charsets.UTF_8).size
        assertTrue("Chinese text takes more UTF-8 bytes than UTF-16 units", bytes > full.length)
        assertEquals(full, TranscribeRules.result(transcript(chinese), emptyList(), maxChars = full.length, maxBytes = bytes))
        val byBytes = TranscribeRules.result(transcript(chinese), emptyList(), maxChars = full.length, maxBytes = bytes - 1)
        assertFalse(((parse(byBytes)["segments"] as List<*>)[0] as Map<*, *>).containsKey("text"))
        assertTrue("for ASCII text the Binder budget is the tighter ceiling", TranscribeRules.MAX_RESULT_CHARS * 2 < PluginToolLimits.MAX_RESULT_BYTES)
    }

    @Test
    fun `stopped transcriptions and a missing file have their codes`() {
        assertEquals(PluginToolErrorCodes.CANCELLED, TranscribeRules.cancelled().code)
        val expired = TranscribeRules.expired()
        assertEquals(PluginToolErrorCodes.FAILED, expired.code)
        assertEquals("TIMEOUT: speech_transcribe ran out of time before the end of the audio; transcribe a shorter part of the audio", expired.message)
        val missing = TranscribeRules.missingFile()
        assertEquals(PluginToolErrorCodes.INVALID_ARGUMENTS, missing.code)
        assertEquals("file is required", missing.message)
    }

}
