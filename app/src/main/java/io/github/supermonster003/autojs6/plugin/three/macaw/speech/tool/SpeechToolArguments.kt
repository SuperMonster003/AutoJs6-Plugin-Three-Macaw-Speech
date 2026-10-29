package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool.SpeechToolCatalog as Catalog
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException

/** `speech_listen` (roadmap D95); the durations are milliseconds. zh-CN: `speech_listen` (路线图 D95); 时长均为毫秒. */
internal data class ListenRequest(
    val language: String? = null,
    val silenceMillis: Long = Catalog.DEFAULT_SILENCE_MILLIS.toLong(),
    val noSpeechMillis: Long = Catalog.DEFAULT_NO_SPEECH_SECONDS * 1000L,
    val maxMillis: Long = Catalog.DEFAULT_MAX_SECONDS * 1000L,
    val hotwords: List<String> = emptyList(),
    val prompt: String? = null,
)

/** A voice written as `<modelId>/<speaker>` (D96); [speaker] is a number or a name. zh-CN: 写作 `<modelId>/<speaker>` 的语音. */
internal data class VoiceChoice(val modelId: String, val speaker: String) {
    override fun toString() = "$modelId/$speaker"
}

/** `speech_speak` (D96). zh-CN: `speech_speak` (D96). */
internal data class SpeakRequest(
    val text: String,
    val language: String? = null,
    val voice: VoiceChoice? = null,
    val speed: Float = Catalog.DEFAULT_SPEED.toFloat(),
    val wait: Boolean = true,
)

/** `speech_transcribe` (D97); the file arrives as an input descriptor. zh-CN: `speech_transcribe` (D97); 文件以输入描述符送达. */
internal data class TranscribeRequest(val language: String? = null, val model: String? = null)

/** `speech_synthesize` (D98). zh-CN: `speech_synthesize` (D98). */
internal data class SynthesizeRequest(
    val text: String,
    val language: String? = null,
    val voice: VoiceChoice? = null,
    val speed: Float = Catalog.DEFAULT_SPEED.toFloat(),
    val format: String = Catalog.FORMAT_WAV,
)

/** `speech_speaker_verify` (roadmap P8.3.5); [threshold] null for the model's value. zh-CN: [threshold] 为 null 时取模型的值. */
internal data class SpeakerVerifyRequest(val name: String, val threshold: Double? = null)

/** `speech_speaker_identify` (roadmap P8.3.5). zh-CN: `speech_speaker_identify` (路线图 P8.3.5). */
internal data class SpeakerIdentifyRequest(val threshold: Double? = null, val topK: Int = Catalog.DEFAULT_TOP_K)

/**
 * Reads the arguments of the tools (roadmap D95 - D98, P8.3.5) with the ranges of [SpeechToolCatalog]. AutoJs6 checks them
 * against the input schema and fills in the defaults before the call; the provider checks again for clients that bind
 * the service directly, so a wrong shape or range is `INVALID_ARGUMENTS` either way. Absent values, JSON null among
 * them, take the defaults; unknown keys are ignored, as in [io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.SpeechOptions].
 * The messages name the argument, never its text. Pure Kotlin: the service turns the JSON object into a map first.
 * zh-CN: 按 [SpeechToolCatalog] 的取值范围读取各工具的参数 (路线图 D95 - D98, P8.3.5). AutoJs6 在调用前已按输入 schema 检查并补上默认值;
 * 提供方为直接绑定服务的客户端再查一遍, 因此形态或范围错误都是 `INVALID_ARGUMENTS`. 缺省值 (含 JSON null) 取默认值; 未知键被忽略. 信息只写
 * 参数名, 不写其文字. 纯 Kotlin: 服务先把 JSON 对象转成 Map.
 */
internal object SpeechToolArguments {

    private val LANGUAGE = Regex("[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*")

    fun listen(arguments: Map<String, Any?>): ListenRequest {
        val hotwords: List<*> = when (val value = arguments["hotwords"]) {
            null -> emptyList<Any?>()
            is List<*> -> value
            else -> invalid("hotwords must be an array of strings")
        }
        if (hotwords.size > Catalog.MAX_HOTWORDS) invalid("hotwords holds at most ${Catalog.MAX_HOTWORDS} words, got ${hotwords.size}")
        return ListenRequest(
            language = language(arguments),
            silenceMillis = integer(arguments, "silenceMillis", Catalog.MIN_SILENCE_MILLIS, Catalog.MAX_SILENCE_MILLIS) ?: Catalog.DEFAULT_SILENCE_MILLIS.toLong(),
            noSpeechMillis = 1000L * (integer(arguments, "noSpeechSeconds", Catalog.MIN_NO_SPEECH_SECONDS, Catalog.MAX_NO_SPEECH_SECONDS) ?: Catalog.DEFAULT_NO_SPEECH_SECONDS.toLong()),
            maxMillis = 1000L * (integer(arguments, "maxSeconds", Catalog.MIN_MAX_SECONDS, Catalog.MAX_MAX_SECONDS) ?: Catalog.DEFAULT_MAX_SECONDS.toLong()),
            hotwords = hotwords.mapIndexed { index, word ->
                val text = (word as? String)?.trim() ?: invalid("hotwords[$index] must be a string")
                if (text.isEmpty()) invalid("hotwords[$index] is empty")
                if (text.codePointCount(0, text.length) > Catalog.MAX_HOTWORD_CHARS) invalid("hotwords[$index] has more than ${Catalog.MAX_HOTWORD_CHARS} characters")
                text
            },
            prompt = string(arguments, "prompt", 0, Catalog.MAX_PROMPT_CHARS)?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    fun speak(arguments: Map<String, Any?>): SpeakRequest = SpeakRequest(
        text = text(arguments, Catalog.MAX_SPEAK_CHARS),
        language = language(arguments),
        voice = voice(arguments),
        speed = speed(arguments),
        wait = when (val value = arguments["wait"]) {
            null -> true
            is Boolean -> value
            else -> invalid("wait must be true or false")
        },
    )

    fun transcribe(arguments: Map<String, Any?>): TranscribeRequest = TranscribeRequest(
        language = language(arguments),
        model = string(arguments, "model", 1, Catalog.MAX_MODEL_CHARS)?.trim()?.also { if (it.isEmpty()) invalid("model must not be blank") },
    )

    fun synthesize(arguments: Map<String, Any?>): SynthesizeRequest = SynthesizeRequest(
        text = text(arguments, Catalog.MAX_SYNTHESIZE_CHARS),
        language = language(arguments),
        voice = voice(arguments),
        speed = speed(arguments),
        format = string(arguments, "format", 1, 8)?.let { format ->
            format.takeIf { it in Catalog.FORMATS } ?: invalid("format must be one of ${Catalog.FORMATS.joinToString()}, got $format")
        } ?: Catalog.FORMAT_WAV,
    )

    fun speakerVerify(arguments: Map<String, Any?>): SpeakerVerifyRequest {
        val name = string(arguments, "name", 1, Catalog.MAX_SPEAKER_NAME_CHARS)?.trim() ?: invalid("name is required")
        if (name.isEmpty()) invalid("name must not be blank")
        if (name.any { Character.isISOControl(it) }) invalid("name must not contain control characters or line breaks")
        return SpeakerVerifyRequest(name, threshold(arguments))
    }

    fun speakerIdentify(arguments: Map<String, Any?>): SpeakerIdentifyRequest = SpeakerIdentifyRequest(
        threshold = threshold(arguments),
        topK = integer(arguments, "topK", 1, Catalog.MAX_TOP_K)?.toInt() ?: Catalog.DEFAULT_TOP_K,
    )

    private fun threshold(arguments: Map<String, Any?>): Double? {
        val value = arguments["threshold"] ?: return null
        val threshold = (value as? Number)?.toDouble()?.takeIf { it.isFinite() } ?: invalid("threshold must be a number")
        if (threshold < 0.0 || threshold > 1.0) invalid("threshold must be between 0 and 1, got $threshold")
        return threshold
    }

    private fun text(arguments: Map<String, Any?>, max: Int): String {
        val text = string(arguments, "text", 1, max) ?: invalid("text is required")
        if (text.isBlank()) invalid("text must not be blank")
        return text
    }

    private fun language(arguments: Map<String, Any?>): String? {
        val tag = string(arguments, "language", 1, Catalog.MAX_LANGUAGE_CHARS)?.trim() ?: return null
        if (!LANGUAGE.matches(tag)) invalid("language is not a BCP 47 language tag: $tag")
        return tag
    }

    private fun voice(arguments: Map<String, Any?>): VoiceChoice? {
        val voice = string(arguments, "voice", 3, Catalog.MAX_VOICE_CHARS)?.trim() ?: return null
        val modelId = voice.substringBeforeLast('/', "").trim()
        val speaker = voice.substringAfterLast('/').trim()
        if (modelId.isEmpty() || speaker.isEmpty() || modelId.length > Catalog.MAX_MODEL_CHARS || speaker.length > Catalog.MAX_MODEL_CHARS) {
            invalid("voice must be written as <modelId>/<speaker>, got $voice")
        }
        return VoiceChoice(modelId, speaker)
    }

    private fun speed(arguments: Map<String, Any?>): Float {
        val value = arguments["speed"] ?: return Catalog.DEFAULT_SPEED.toFloat()
        val speed = (value as? Number)?.toDouble()?.takeIf { it.isFinite() } ?: invalid("speed must be a number")
        if (speed < Catalog.MIN_SPEED || speed > Catalog.MAX_SPEED) invalid("speed must be between ${Catalog.MIN_SPEED} and ${Catalog.MAX_SPEED}, got $speed")
        return speed.toFloat()
    }

    private fun string(arguments: Map<String, Any?>, key: String, min: Int, max: Int): String? {
        val value = arguments[key] ?: return null
        val text = value as? String ?: invalid("$key must be a string")
        val length = text.codePointCount(0, text.length)
        if (length < min || length > max) invalid("$key must have $min to $max characters, got $length")
        return text
    }

    private fun integer(arguments: Map<String, Any?>, key: String, min: Int, max: Int): Long? {
        val value = arguments[key] ?: return null
        val number = (value as? Number)?.toDouble() ?: invalid("$key must be an integer")
        if (number != Math.rint(number) || !number.isFinite()) invalid("$key must be an integer, got $number")
        if (number < min || number > max) invalid("$key must be between $min and $max, got ${number.toLong()}")
        return number.toLong()
    }

    private fun invalid(message: String): Nothing = throw PluginToolException(PluginToolErrorCodes.INVALID_ARGUMENTS, message)

}
