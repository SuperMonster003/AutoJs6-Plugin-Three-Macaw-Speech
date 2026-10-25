package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice

import android.app.Activity
import android.speech.RecognizerIntent
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.CommercialUse
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelEntry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelStore
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The rules of the system voice input (roadmap P5.6): the model for `EXTRA_LANGUAGE` (D45), the caller's silence and
 * the session limits (D44), the prompt and the biasing strings (D46), the guidance and the result codes (D47).
 * zh-CN: 系统语音输入的规则 (路线图 P5.6): 按 `EXTRA_LANGUAGE` 选模型 (D45), 调用方的静音时长与会话上限 (D44), 提示与偏置词 (D46), 引导与结果码 (D47).
 */
class VoiceInputPolicyTest {

    private fun asr(id: String, vararg languages: String, isDefault: Boolean = false, recommended: Boolean = false, installed: Boolean = true) = ModelEntry(
        id = id, kind = SpeechContract.KIND_ASR, modelType = "online-transducer", name = id, languages = languages.toList(), sizeBytes = 1000,
        installedBytes = if (installed) 2000 else null, installed = installed, isDefault = isDefault, recommended = recommended, version = 1,
        licenseSpdx = "Apache-2.0", commercialUse = CommercialUse.ALLOWED, noticeKey = null, noticeUrl = null, speakers = null, sampleRate = 16000,
        source = ModelStore.SOURCE_CATALOG,
    )

    private val senseVoice = asr("asr.sense-voice", "zh", "en", "ja", "ko", "yue", isDefault = true, recommended = true)
    private val zhEn = asr("asr.zh-en", "zh", "en", recommended = true)
    private val en = asr("asr.en", "en", recommended = true)
    private val zh = asr("asr.zh", "zh")

    @Test
    fun `the default model is kept when it covers the language`() {
        val models = listOf(zhEn, en, senseVoice)
        assertEquals("asr.sense-voice", VoiceInputPolicy.chooseModel(models, "zh-CN"))
        assertEquals("asr.sense-voice", VoiceInputPolicy.chooseModel(models, "zh-Hans-CN"))
        assertEquals("asr.sense-voice", VoiceInputPolicy.chooseModel(models, "yue-HK"))
        assertEquals("no language: the default model", "asr.sense-voice", VoiceInputPolicy.chooseModel(models, null))
    }

    @Test
    fun `another installed model covers what the default does not, recommended first`() {
        val englishDefault = en.copy(isDefault = true)
        assertEquals("asr.zh-en", VoiceInputPolicy.chooseModel(listOf(zh, zhEn, englishDefault), "zh-TW"))
        assertEquals("by ID among equals", "asr.zh", VoiceInputPolicy.chooseModel(listOf(zhEn.copy(recommended = false), zh, englishDefault), "zh"))
        assertEquals("nothing covers Japanese: the default", "asr.en", VoiceInputPolicy.chooseModel(listOf(zhEn, englishDefault), "ja-JP"))
        assertEquals("models that are not installed do not count", "asr.en", VoiceInputPolicy.chooseModel(listOf(zhEn.copy(installed = false), englishDefault), "zh-CN"))
        assertNull("no default and no cover: the runtime decides", VoiceInputPolicy.chooseModel(listOf(en), "ko"))
        assertEquals("a covering model without a default", "asr.en", VoiceInputPolicy.chooseModel(listOf(zh, en), "en-GB"))
    }

    @Test
    fun `a model of more than five languages answers only what narrower models leave uncovered`() {
        val eight = asr("asr.zipformer-streaming.ar-en-id-ja-ru-th-vi-zh", "ar", "en", "id", "ja", "ru", "th", "vi", "zh")
        val ru = asr("asr.zipformer-streaming.ru", "ru")
        val english = asr("asr.zipformer-streaming.en", "en")
        val sense = senseVoice.copy(isDefault = false, recommended = false)
        val models = listOf(eight, ru, english, sense, asr("asr.nemo-transducer.es", "es", isDefault = true))
        assertEquals("asr.zipformer-streaming.ru", VoiceInputPolicy.chooseModel(models, "ru-RU"))
        assertEquals("narrow models keep their order by ID", "asr.sense-voice", VoiceInputPolicy.chooseModel(models, "en-US"))
        assertEquals("asr.zipformer-streaming.en", VoiceInputPolicy.chooseModel(models - sense, "en-US"))
        assertEquals("Q84: SenseVoice first for Japanese", "asr.sense-voice", VoiceInputPolicy.chooseModel(models, "ja-JP"))
        assertEquals("only the wide model covers Arabic", eight.id, VoiceInputPolicy.chooseModel(models, "ar-SA"))
        assertEquals("recommended still comes first", eight.id, VoiceInputPolicy.chooseModel(models.map { if (it === eight) it.copy(recommended = true) else it }, "ru"))
        assertEquals("the default covers Spanish", "asr.nemo-transducer.es", VoiceInputPolicy.chooseModel(models, "es-MX"))
    }

    @Test
    fun `language tags are checked and underscores accepted`() {
        assertEquals("zh-CN", VoiceInputPolicy.language("zh_CN"))
        assertEquals("en-US", VoiceInputPolicy.language(" en-US "))
        assertNull(VoiceInputPolicy.language("not a tag"))
        assertNull(VoiceInputPolicy.language("x"))
        assertNull(VoiceInputPolicy.language("a".repeat(81)))
        assertEquals("zh", VoiceInputPolicy.primaryLanguage("ZH-Hans-CN"))
    }

    @Test
    fun `the caller's silence is bounded and the settings decide otherwise`() {
        assertEquals(1200L, VoiceInputPolicy.silence(null, 1200L))
        assertEquals(600L, VoiceInputPolicy.silence(0L, 600L))
        assertEquals(300L, VoiceInputPolicy.silence(50L, 2000L))
        assertEquals(5000L, VoiceInputPolicy.silence(30_000L, 2000L))
        assertEquals(1500L, VoiceInputPolicy.silence(1500L, 600L))
        assertEquals(8_000L, VoiceInputPolicy.NO_SPEECH_MILLIS)
        assertEquals(60_000L, VoiceInputPolicy.MAX_UTTERANCE_MILLIS)
        assertNull("a negative caller value is ignored", VoiceInputPolicy.request(null, null, -5L, null).silenceMillis)
    }

    @Test
    fun `the prompt is trimmed and cut without splitting a surrogate pair`() {
        assertNull(VoiceInputPolicy.prompt("   "))
        assertEquals("Say a goal", VoiceInputPolicy.prompt("  Say a goal \n"))
        assertEquals(200, VoiceInputPolicy.prompt("a".repeat(500))!!.length)
        val emoji = "a".repeat(199) + String(Character.toChars(0x1F600)) + "tail"
        assertEquals(199, VoiceInputPolicy.prompt(emoji)!!.length)
    }

    @Test
    fun `biasing strings become clean hotwords within the contract limits`() {
        assertEquals(listOf("AutoJs6", "3-Macaw Speech", "a b"), VoiceInputPolicy.biasing(listOf(" AutoJs6 ", null, "", "3-Macaw   Speech", "AutoJs6", "a/b")))
        assertEquals(emptyList<String>(), VoiceInputPolicy.biasing(listOf("x".repeat(SpeechLimits.MAX_TOKEN_CHARS + 1))))
        assertEquals(SpeechLimits.MAX_HOTWORDS, VoiceInputPolicy.biasing((0 until 500).map { "word$it" }).size)
        assertEquals(emptyList<String>(), VoiceInputPolicy.biasing(null))
    }

    @Test
    fun `an ended session answers its text, the last partial or no match`() {
        assertEquals(VoiceInputOutcome.Text("hello"), VoiceInputPolicy.onEnd(SpeechContract.REASON_STOPPED, " hello ", "hel"))
        assertEquals(VoiceInputOutcome.Text("hel"), VoiceInputPolicy.onEnd(SpeechContract.REASON_MAX_DURATION, "", "hel"))
        assertEquals(VoiceInputOutcome.NoMatch, VoiceInputPolicy.onEnd(SpeechContract.REASON_TIMEOUT, "", ""))
        assertEquals(VoiceInputOutcome.NoMatch, VoiceInputPolicy.onEnd(SpeechContract.REASON_STOPPED, null, null))
        assertEquals(VoiceInputOutcome.Cancelled, VoiceInputPolicy.onEnd(SpeechContract.REASON_CANCELLED, "hello", "hello"))
    }

    @Test
    fun `problems and outcomes map to the RecognizerIntent result codes`() {
        assertEquals(VoiceInputProblemKind.MICROPHONE, VoiceInputPolicy.problem(SpeechErrorCodes.PERMISSION_DENIED, false).kind)
        assertEquals(VoiceInputProblemKind.NO_MODEL, VoiceInputPolicy.problem(SpeechErrorCodes.MODEL_MISSING, false).kind)
        assertEquals(VoiceInputProblemKind.BUSY, VoiceInputPolicy.problem(SpeechErrorCodes.SESSION_BUSY, false).kind)
        assertEquals(VoiceInputProblemKind.FAILED, VoiceInputPolicy.problem(SpeechErrorCodes.ENGINE_FAILED, false).kind)
        assertEquals(SpeechErrorCodes.INTERNAL, VoiceInputPolicy.problem(null, false).code)

        fun code(error: String, recording: Boolean = false) = VoiceInputPolicy.resultCode(VoiceInputOutcome.Failed(VoiceInputPolicy.problem(error, recording)))
        assertEquals(Activity.RESULT_OK, VoiceInputPolicy.resultCode(VoiceInputOutcome.Text("hi")))
        assertEquals(Activity.RESULT_CANCELED, VoiceInputPolicy.resultCode(VoiceInputOutcome.Cancelled))
        assertEquals(RecognizerIntent.RESULT_NO_MATCH, VoiceInputPolicy.resultCode(VoiceInputOutcome.NoMatch))
        assertEquals(RecognizerIntent.RESULT_AUDIO_ERROR, code(SpeechErrorCodes.PERMISSION_DENIED))
        assertEquals(RecognizerIntent.RESULT_AUDIO_ERROR, code(SpeechErrorCodes.SESSION_BUSY))
        assertEquals(RecognizerIntent.RESULT_SERVER_ERROR, code(SpeechErrorCodes.MODEL_MISSING))
        assertEquals("loading failed before the microphone ran", RecognizerIntent.RESULT_SERVER_ERROR, code(SpeechErrorCodes.ENGINE_FAILED))
        assertEquals("recording failed", RecognizerIntent.RESULT_AUDIO_ERROR, code(SpeechErrorCodes.ENGINE_FAILED, recording = true))
        assertEquals(RecognizerIntent.RESULT_SERVER_ERROR, code(SpeechErrorCodes.INTERNAL))
    }
}
