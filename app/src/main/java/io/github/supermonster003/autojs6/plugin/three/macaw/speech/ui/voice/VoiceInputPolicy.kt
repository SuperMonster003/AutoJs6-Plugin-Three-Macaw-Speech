package io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice

import android.app.Activity
import android.speech.RecognizerIntent
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelEntry
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits

/**
 * What a `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` caller asked for (roadmap P5.6, D44 - D46), already checked by
 * [VoiceInputPolicy.request]: the prompt shown as the title, the language that picks the model, the caller's
 * end-of-speech silence and the biasing strings that become hotwords.
 * zh-CN: `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` 调用方的请求 (路线图 P5.6, D44 - D46), 已经 [VoiceInputPolicy.request] 检查: 作为标题的提示,
 * 用于选模型的语言, 调用方的结束静音时长, 以及作为热词的偏置词.
 */
internal data class VoiceInputRequest(
    val prompt: String? = null,
    val language: String? = null,
    val silenceMillis: Long? = null,
    val biasing: List<String> = emptyList(),
)

/** Why the sheet shows guidance instead of listening (roadmap D47). zh-CN: 弹层显示引导而非收音的原因 (路线图 D47). */
internal enum class VoiceInputProblemKind { MICROPHONE, NO_MODEL, BUSY, FAILED }

/**
 * A problem of the sheet: [code] is the contract error code, [recording] tells whether the microphone was already
 * running (a failure while recording is an audio error, before that the recognizer's).
 * zh-CN: 弹层的问题: [code] 为契约错误码, [recording] 表示麦克风是否已在录音 (录音中的失败属于音频错误, 之前的属于识别器).
 */
internal data class VoiceInputProblem(val kind: VoiceInputProblemKind, val code: String, val recording: Boolean = false)

/** How one voice input ends. zh-CN: 一次语音输入的结局. */
internal sealed class VoiceInputOutcome {
    data class Text(val text: String) : VoiceInputOutcome()
    data object NoMatch : VoiceInputOutcome()
    data object Cancelled : VoiceInputOutcome()
    data class Failed(val problem: VoiceInputProblem) : VoiceInputOutcome()
}

/**
 * The rules of the system voice input (roadmap P5.6, decided with the maintainer on 2026-10-16 as D44 - D47). Pure
 * Kotlin: the `RecognizerIntent` and `Activity` result codes are compile-time constants.
 * zh-CN: 系统语音输入的规则 (路线图 P5.6, 2026-10-16 与维护者确定为 D44 - D47). 纯 Kotlin: `RecognizerIntent` 与 `Activity` 的结果码是编译期常量.
 */
internal object VoiceInputPolicy {
    /** D46: the prompt is cut to this many characters. zh-CN: D46: 提示截断到的字符数. */
    const val MAX_PROMPT_CHARS = 200

    /** D44: the caller's end-of-speech silence is kept within these bounds. zh-CN: D44: 调用方结束静音时长的范围. */
    const val MIN_SILENCE_MILLIS = 300L
    const val MAX_SILENCE_MILLIS = 5000L

    /** D44: no speech for this long ends the input with no match (the session's `timeoutMillis`). zh-CN: D44: 无语音的时长上限. */
    const val NO_SPEECH_MILLIS = 8_000L

    /** D44: one utterance lasts at most this long (the session's `maxDurationMillis`). zh-CN: D44: 单句时长上限. */
    const val MAX_UTTERANCE_MILLIS = 60_000L

    private const val MAX_LANGUAGE_CHARS = 80
    private val LANGUAGE = Regex("[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*")
    private val SPACES = Regex("\\s+")

    fun request(prompt: CharSequence?, language: String?, silenceMillis: Long?, biasing: List<String?>?): VoiceInputRequest =
        VoiceInputRequest(prompt(prompt), language(language), silenceMillis?.takeIf { it > 0 }, biasing(biasing))

    /** D46: trimmed, empty means none, cut to [MAX_PROMPT_CHARS] without splitting a surrogate pair. zh-CN: 去空白, 截断时不拆开代理对. */
    fun prompt(value: CharSequence?): String? {
        val text = value?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (text.length <= MAX_PROMPT_CHARS) return text
        val end = if (Character.isHighSurrogate(text[MAX_PROMPT_CHARS - 1])) MAX_PROMPT_CHARS - 1 else MAX_PROMPT_CHARS
        return text.substring(0, end)
    }

    /** A BCP 47 tag (an underscore as in `zh_CN` is accepted), or null. zh-CN: BCP 47 标签 (接受 `zh_CN` 式的下划线), 否则为 null. */
    fun language(value: String?): String? {
        val tag = value?.trim()?.replace('_', '-')?.takeIf { it.length <= MAX_LANGUAGE_CHARS } ?: return null
        return tag.takeIf { LANGUAGE.matches(it) }
    }

    /** The primary language subtag, lower case: `zh-Hans-CN` -> `zh`. zh-CN: 主语言子标签, 小写. */
    fun primaryLanguage(tag: String?): String? = tag?.substringBefore('-')?.lowercase()?.takeIf { it.isNotEmpty() }

    /**
     * D44: the caller's `EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS` within [MIN_SILENCE_MILLIS] and
     * [MAX_SILENCE_MILLIS], otherwise the endpoint sensitivity of the settings screen.
     * zh-CN: D44: 调用方的 `EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS` (限于两个边界之间), 否则取设置页的端点灵敏度.
     */
    fun silence(caller: Long?, setting: Long): Long = caller?.takeIf { it > 0 }?.coerceIn(MIN_SILENCE_MILLIS, MAX_SILENCE_MILLIS) ?: setting

    /**
     * D46: `EXTRA_BIASING_STRINGS` as hotwords: whitespace collapsed, `/` (the hotword separator of sherpa-onnx) made a
     * space, empty and over-long entries dropped, duplicates removed, at most [SpeechLimits.MAX_HOTWORDS].
     * zh-CN: D46: 把 `EXTRA_BIASING_STRINGS` 作为热词: 合并空白, `/` (sherpa-onnx 的热词分隔符) 改为空格, 丢弃空的与过长的条目, 去重, 至多
     * [SpeechLimits.MAX_HOTWORDS] 条.
     */
    fun biasing(values: List<String?>?): List<String> = values.orEmpty().asSequence()
        .mapNotNull { value -> value?.replace('/', ' ')?.replace(SPACES, " ")?.trim()?.takeIf { it.isNotEmpty() && it.length <= SpeechLimits.MAX_TOKEN_CHARS } }
        .distinct()
        .take(SpeechLimits.MAX_HOTWORDS)
        .toList()

    /**
     * D45: the recognition model for [language]: the default model when it covers the language, otherwise the
     * installed model that covers it (recommended first, then by ID), otherwise the default model; null leaves the
     * choice to the runtime's default. [models] are the installed recognition models.
     * zh-CN: D45: 为 [language] 选识别模型: 默认模型覆盖该语言就用它, 否则取覆盖它的已装模型 (推荐优先, 再按 ID), 否则仍用默认模型; null 表示交给运行时的
     * 默认模型. [models] 为已安装的识别模型.
     */
    fun chooseModel(models: List<ModelEntry>, language: String?): String? {
        val recognition = models.filter { it.kind == SpeechContract.KIND_ASR && it.installed }
        val default = recognition.firstOrNull { it.isDefault }
        val primary = primaryLanguage(language) ?: return default?.id
        if (default != null && covers(default, primary)) return default.id
        return recognition.filter { covers(it, primary) }
            .sortedWith(compareByDescending<ModelEntry> { it.recommended }.thenBy { it.id })
            .firstOrNull()?.id ?: default?.id
    }

    private fun covers(model: ModelEntry, primary: String): Boolean = model.languages.any { primaryLanguage(it) == primary }

    /** D47: the guidance for a contract error code. zh-CN: D47: 契约错误码对应的引导. */
    fun problem(code: String?, recording: Boolean): VoiceInputProblem {
        val known = code ?: SpeechErrorCodes.INTERNAL
        val kind = when (known) {
            SpeechErrorCodes.PERMISSION_DENIED -> VoiceInputProblemKind.MICROPHONE
            SpeechErrorCodes.MODEL_MISSING -> VoiceInputProblemKind.NO_MODEL
            SpeechErrorCodes.SESSION_BUSY -> VoiceInputProblemKind.BUSY
            else -> VoiceInputProblemKind.FAILED
        }
        return VoiceInputProblem(kind, known, recording)
    }

    /**
     * D44: the outcome when the session ended without a usable final: its text (or the last partial) when there is
     * one, otherwise no match; a cancelled session is a cancellation.
     * zh-CN: D44: 会话结束而没有可用 final 时的结局: 有文字 (或最后的 partial) 即返回它, 否则为无匹配; 被取消的会话即取消.
     */
    fun onEnd(reason: String?, text: String?, partial: String?): VoiceInputOutcome {
        if (reason == SpeechContract.REASON_CANCELLED) return VoiceInputOutcome.Cancelled
        val heard = text?.trim()?.takeIf { it.isNotEmpty() } ?: partial?.trim()?.takeIf { it.isNotEmpty() }
        return heard?.let { VoiceInputOutcome.Text(it) } ?: VoiceInputOutcome.NoMatch
    }

    /**
     * D47: the activity result: the text is `RESULT_OK`, a cancellation `RESULT_CANCELED`, no speech `RESULT_NO_MATCH`;
     * the microphone permission, a busy microphone and a failure while recording are audio errors, a missing model and
     * every failure before the microphone ran are the recognizer's (`RESULT_SERVER_ERROR`).
     * zh-CN: D47: Activity 结果: 有文字为 `RESULT_OK`, 取消为 `RESULT_CANCELED`, 无语音为 `RESULT_NO_MATCH`; 麦克风权限, 麦克风被占用与录音中的失败为
     * 音频错误, 缺模型以及麦克风运行之前的失败都属于识别器 (`RESULT_SERVER_ERROR`).
     */
    fun resultCode(outcome: VoiceInputOutcome): Int = when (outcome) {
        is VoiceInputOutcome.Text -> Activity.RESULT_OK
        VoiceInputOutcome.Cancelled -> Activity.RESULT_CANCELED
        VoiceInputOutcome.NoMatch -> RecognizerIntent.RESULT_NO_MATCH
        is VoiceInputOutcome.Failed -> when (outcome.problem.kind) {
            VoiceInputProblemKind.MICROPHONE, VoiceInputProblemKind.BUSY -> RecognizerIntent.RESULT_AUDIO_ERROR
            VoiceInputProblemKind.NO_MODEL -> RecognizerIntent.RESULT_SERVER_ERROR
            VoiceInputProblemKind.FAILED -> if (outcome.problem.recording) RecognizerIntent.RESULT_AUDIO_ERROR else RecognizerIntent.RESULT_SERVER_ERROR
        }
    }
}
