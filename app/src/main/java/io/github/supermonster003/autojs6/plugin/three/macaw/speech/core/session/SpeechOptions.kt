package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import org.autojs.plugin.speech.api.SpeechOptionKeys

/**
 * Read access to one request Bundle (or a map in the JVM tests): [value] answers the raw value, a nested Bundle as an
 * [OptionSource] and a list of Bundles as a list of [OptionSource].
 * zh-CN: 对一个请求 Bundle (JVM 测试中为 map) 的读取: [value] 返回原始值, 嵌套 Bundle 为 [OptionSource], Bundle 列表为 [OptionSource] 列表.
 */
interface OptionSource {
    fun has(key: String): Boolean
    fun value(key: String): Any?
}

/** The [OptionSource] of a plain map. zh-CN: 普通 map 的 [OptionSource]. */
class MapOptionSource(private val map: Map<String, Any?>) : OptionSource {
    override fun has(key: String) = map.containsKey(key)

    override fun value(key: String): Any? = when (val value = map[key]) {
        is Map<*, *> -> MapOptionSource(value.entries.associate { it.key.toString() to it.value })
        is List<*> -> value.map { item -> if (item is Map<*, *>) MapOptionSource(item.entries.associate { it.key.toString() to it.value }) else item }
        else -> value
    }
}

/** Dictation options (`openSession`, `thenListen`; roadmap appendix B.2). zh-CN: 听写选项 (附录 B.2). */
data class ListenOptions(
    val model: String? = null,
    val language: String? = null,
    val source: String = SpeechContract.SOURCE_MIC,
    val vad: Boolean = true,
    val silenceEndMillis: Long = SpeechLimits.DEFAULT_SILENCE_END_MILLIS,
    val maxDurationMillis: Long = SpeechLimits.DEFAULT_MAX_DURATION_MILLIS,
    val timeoutMillis: Long = SpeechLimits.DEFAULT_TIMEOUT_MILLIS,
    val hotwords: List<String> = emptyList(),
    val punctuation: Boolean = true,
) {
    val isExternal: Boolean get() = source == SpeechContract.SOURCE_EXTERNAL
}

/** File recognition options (`recognizeFile`). zh-CN: 文件识别选项. */
data class RecognizeOptions(
    val model: String? = null,
    val language: String? = null,
    val hotwords: List<String> = emptyList(),
    val punctuation: Boolean = true,
    val vad: Boolean = true,
    val displayName: String? = null,
)

/** Voice activity options (`detectVoice`); null means the default of the VAD. zh-CN: 语音活动检测选项; null 取 VAD 默认值. */
data class VadOptions(
    val model: String? = null,
    val threshold: Float? = null,
    val minSpeechMillis: Long? = null,
    val minSilenceMillis: Long? = null,
)

/** One wake keyword as the script wrote it (roadmap D18). zh-CN: 脚本所写的一个唤醒关键词 (路线图 D18). */
data class Keyword(val text: String, val boost: Float? = null, val threshold: Float? = null)

/** Wake session options (`openWakeSession`). zh-CN: 唤醒会话选项. */
data class WakeOptions(
    val keywords: List<Keyword>,
    val model: String? = null,
    val minIntervalMillis: Long = SpeechLimits.DEFAULT_MIN_INTERVAL_MILLIS,
    val thenListen: ListenOptions? = null,
)

/**
 * Synthesis options (`synthesizeToFile`, roadmap D20, appendix B.2): [speaker] stays text (a number or a name, resolved
 * against the model), [speed] 0.5 to 2.0.
 * zh-CN: 合成选项 (`synthesizeToFile`, 路线图 D20, 附录 B.2): [speaker] 保持为文本 (编号或名字, 按模型解析), [speed] 0.5 到 2.0.
 */
data class SynthesizeOptions(
    val text: String,
    val model: String? = null,
    val speaker: String? = null,
    val speed: Float = 1f,
    val format: String = SpeechContract.FORMAT_WAV,
)

/**
 * Parses and checks the request Bundles of contract version 1 (`docs/dev/speech-plugin-protocol-v1.md`) with the same
 * ranges as the host's script layer, so a request that slipped past the host still fails with the documented code:
 * a wrong shape or range is `INVALID_OPTIONS`, a contract ceiling `LIMIT_EXCEEDED`, a request of another contract
 * version `PLUGIN_INCOMPATIBLE`. Unknown keys are ignored (a newer host may add optional keys). Pure Kotlin.
 * zh-CN: 以与宿主脚本层相同的取值范围解析并检查契约版本 1 的请求 Bundle, 越过宿主的请求同样得到文档规定的错误码: 形态或范围错误为
 * `INVALID_OPTIONS`, 契约上限为 `LIMIT_EXCEEDED`, 其他契约版本的请求为 `PLUGIN_INCOMPATIBLE`. 未知键被忽略 (较新的宿主可能追加可选键). 纯 Kotlin.
 */
object SpeechOptions {

    /** `silenceEndMillis` upper bound, as in the host. zh-CN: `silenceEndMillis` 上限, 与宿主一致. */
    const val MAX_SILENCE_END_MILLIS = 60_000L

    /** `minIntervalMillis` upper bound, as in the host. zh-CN: `minIntervalMillis` 上限, 与宿主一致. */
    const val MAX_MIN_INTERVAL_MILLIS = 600_000L

    private val LANGUAGE = Regex("[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*")

    fun checkContract(source: OptionSource?) {
        if (source == null || !source.has(SpeechContract.KEY_CONTRACT_VERSION)) return
        val version = (source.value(SpeechContract.KEY_CONTRACT_VERSION) as? Number)?.toInt()
        if (version == null || !SpeechContract.supportsContractVersion(version)) {
            throw SpeechException(SpeechErrorCodes.PLUGIN_INCOMPATIBLE, "Unsupported speech contract version $version")
        }
    }

    fun listen(source: OptionSource?, label: String = "options"): ListenOptions {
        checkContract(source)
        val given = source ?: MapOptionSource(emptyMap())
        val origin = string(given, SpeechOptionKeys.SOURCE, label) ?: SpeechContract.SOURCE_MIC
        if (origin !in SpeechContract.SOURCES) invalid("$label.source must be one of ${SpeechContract.SOURCES}, got $origin")
        val sampleRate = long(given, SpeechOptionKeys.SAMPLE_RATE, label) ?: SpeechLimits.EXTERNAL_SAMPLE_RATE.toLong()
        if (sampleRate != SpeechLimits.EXTERNAL_SAMPLE_RATE.toLong()) {
            invalid("$label.sampleRate must be ${SpeechLimits.EXTERNAL_SAMPLE_RATE} (16 kHz mono PCM16), got $sampleRate")
        }
        val silenceEnd = long(given, SpeechOptionKeys.SILENCE_END_MILLIS, label) ?: SpeechLimits.DEFAULT_SILENCE_END_MILLIS
        if (silenceEnd !in 1..MAX_SILENCE_END_MILLIS) invalid("$label.silenceEndMillis must be between 1 and $MAX_SILENCE_END_MILLIS, got $silenceEnd")
        val maxDuration = long(given, SpeechOptionKeys.MAX_DURATION_MILLIS, label) ?: SpeechLimits.DEFAULT_MAX_DURATION_MILLIS
        if (maxDuration < 1) invalid("$label.maxDurationMillis must be positive, got $maxDuration")
        if (maxDuration > SpeechLimits.MAX_AUDIO_MILLIS) limit("$label.maxDurationMillis is at most ${SpeechLimits.MAX_AUDIO_MILLIS}, got $maxDuration")
        val timeout = long(given, SpeechOptionKeys.TIMEOUT_MILLIS, label) ?: SpeechLimits.DEFAULT_TIMEOUT_MILLIS
        if (timeout !in 0..SpeechLimits.MAX_AUDIO_MILLIS) invalid("$label.timeoutMillis must be between 0 and ${SpeechLimits.MAX_AUDIO_MILLIS}, got $timeout")
        return ListenOptions(
            model = model(given, label),
            language = language(given, label),
            source = origin,
            vad = boolean(given, SpeechOptionKeys.VAD, label) ?: true,
            silenceEndMillis = silenceEnd,
            maxDurationMillis = maxDuration,
            timeoutMillis = timeout,
            hotwords = hotwords(given, label),
            punctuation = boolean(given, SpeechOptionKeys.PUNCTUATION, label) ?: true,
        )
    }

    fun recognize(source: OptionSource?): RecognizeOptions {
        checkContract(source)
        val given = source ?: MapOptionSource(emptyMap())
        return RecognizeOptions(
            model = model(given, "options"),
            language = language(given, "options"),
            hotwords = hotwords(given, "options"),
            punctuation = boolean(given, SpeechOptionKeys.PUNCTUATION, "options") ?: true,
            vad = boolean(given, SpeechOptionKeys.VAD, "options") ?: true,
            displayName = string(given, SpeechOptionKeys.DISPLAY_NAME, "options"),
        )
    }

    fun vad(source: OptionSource?): VadOptions {
        checkContract(source)
        val given = source ?: MapOptionSource(emptyMap())
        val threshold = float(given, SpeechOptionKeys.VAD_THRESHOLD, "options")
        if (threshold != null && !(threshold > 0f && threshold < 1f)) invalid("options.vadThreshold must be within (0, 1), got $threshold")
        val minSpeech = long(given, SpeechOptionKeys.MIN_SPEECH_MILLIS, "options")
        if (minSpeech != null && minSpeech !in 0..SpeechLimits.MAX_AUDIO_MILLIS) invalid("options.minSpeechMillis is out of range: $minSpeech")
        val minSilence = long(given, SpeechOptionKeys.MIN_SILENCE_MILLIS, "options")
        if (minSilence != null && minSilence !in 0..SpeechLimits.MAX_AUDIO_MILLIS) invalid("options.minSilenceMillis is out of range: $minSilence")
        return VadOptions(model(given, "options"), threshold, minSpeech, minSilence)
    }

    fun wake(source: OptionSource?): WakeOptions {
        checkContract(source)
        val given = source ?: MapOptionSource(emptyMap())
        val raw = given.value(SpeechOptionKeys.KEYWORDS) ?: invalid("options.keywords is required")
        val list = raw as? List<*> ?: invalid("options.keywords must be a list of keyword Bundles")
        if (list.isEmpty()) invalid("options.keywords must hold at least one keyword")
        if (list.size > SpeechLimits.MAX_KEYWORDS) limit("options.keywords holds at most ${SpeechLimits.MAX_KEYWORDS} keywords, got ${list.size}")
        val keywords = list.mapIndexed { index, item ->
            val label = "options.keywords[$index]"
            val keyword = item as? OptionSource ?: invalid("$label must be a Bundle")
            val text = string(keyword, SpeechOptionKeys.KEYWORD, label)?.trim() ?: invalid("$label.keyword is required")
            if (text.isEmpty()) invalid("$label.keyword is empty")
            if (text.length > SpeechLimits.MAX_TOKEN_CHARS) limit("$label.keyword has more than ${SpeechLimits.MAX_TOKEN_CHARS} characters")
            val boost = float(keyword, SpeechOptionKeys.BOOST, label)
            if (boost != null && boost !in -100f..100f) invalid("$label.boost must be between -100 and 100, got $boost")
            val threshold = float(keyword, SpeechOptionKeys.THRESHOLD, label)
            if (threshold != null && threshold !in 0f..1f) invalid("$label.threshold must be between 0 and 1, got $threshold")
            Keyword(text, boost, threshold)
        }
        val interval = long(given, SpeechOptionKeys.MIN_INTERVAL_MILLIS, "options") ?: SpeechLimits.DEFAULT_MIN_INTERVAL_MILLIS
        if (interval !in 0..MAX_MIN_INTERVAL_MILLIS) invalid("options.minIntervalMillis must be between 0 and $MAX_MIN_INTERVAL_MILLIS, got $interval")
        val thenListen = when (val value = given.value(SpeechOptionKeys.THEN_LISTEN)) {
            null -> null
            is OptionSource -> listen(value, "options.thenListen").also {
                if (it.isExternal) invalid("options.thenListen listens to the microphone; source \"external\" is not available")
            }
            else -> invalid("options.thenListen must be a Bundle")
        }
        return WakeOptions(keywords, model(given, "options"), interval, thenListen)
    }

    /**
     * `synthesizeToFile`: `text` (1 to `MAX_SYNTH_TEXT_CHARS` UTF-16 units, not blank), `model`, `speaker` (String or
     * integer, at most `MAX_TOKEN_CHARS`), `speed` (0.5 to 2.0) and `format` (`wav`).
     * zh-CN: `synthesizeToFile`: `text` (1 到 `MAX_SYNTH_TEXT_CHARS` 个 UTF-16 单元, 非空白), `model`, `speaker` (字符串或整数, 至多
     * `MAX_TOKEN_CHARS`), `speed` (0.5 到 2.0) 与 `format` (`wav`).
     */
    fun synthesize(source: OptionSource?): SynthesizeOptions {
        checkContract(source)
        val given = source ?: MapOptionSource(emptyMap())
        val text = string(given, SpeechOptionKeys.TEXT, "request") ?: invalid("request.text is required")
        if (text.isBlank()) invalid("request.text must not be blank")
        if (text.length > SpeechLimits.MAX_SYNTH_TEXT_CHARS) limit("request.text has ${text.length} characters; one synthesis takes at most ${SpeechLimits.MAX_SYNTH_TEXT_CHARS}")
        val speaker = when (val value = given.value(SpeechOptionKeys.SPEAKER)) {
            null -> null
            is String -> value.trim().also { if (it.isEmpty() || it.length > SpeechLimits.MAX_TOKEN_CHARS) invalid("request.speaker is not a speaker id or name") }
            is Int, is Long, is Short, is Byte -> (value as Number).toLong().also { if (it < 0) invalid("request.speaker must not be negative, got $it") }.toString()
            else -> invalid("request.speaker must be a speaker id or name")
        }
        val speed = float(given, SpeechOptionKeys.SPEED, "request") ?: 1f
        if (speed < SpeechLimits.MIN_SPEED || speed > SpeechLimits.MAX_SPEED) invalid("request.speed must be between ${SpeechLimits.MIN_SPEED} and ${SpeechLimits.MAX_SPEED}, got $speed")
        val format = string(given, SpeechOptionKeys.FORMAT, "request")?.trim()?.lowercase() ?: SpeechContract.FORMAT_WAV
        if (format !in SpeechContract.FORMATS) invalid("request.format must be one of ${SpeechContract.FORMATS}, got $format")
        return SynthesizeOptions(text, model(given, "request"), speaker, speed, format)
    }

    private fun model(given: OptionSource, label: String): String? {
        val id = string(given, SpeechOptionKeys.MODEL, label) ?: return null
        if (id.isEmpty() || id.length > SpeechLimits.MAX_TOKEN_CHARS) invalid("$label.model is not a valid model id")
        return id
    }

    private fun language(given: OptionSource, label: String): String? {
        val tag = string(given, SpeechOptionKeys.LANGUAGE, label) ?: return null
        if (tag.length > SpeechLimits.MAX_TOKEN_CHARS || !LANGUAGE.matches(tag)) invalid("$label.language is not a language tag: $tag")
        return tag
    }

    private fun hotwords(given: OptionSource, label: String): List<String> {
        val value = given.value(SpeechOptionKeys.HOTWORDS) ?: return emptyList()
        val words = when (value) {
            is Array<*> -> value.toList()
            is List<*> -> value
            else -> invalid("$label.hotwords must be a string array")
        }
        if (words.size > SpeechLimits.MAX_HOTWORDS) limit("$label.hotwords holds at most ${SpeechLimits.MAX_HOTWORDS} words, got ${words.size}")
        return words.mapIndexed { index, word ->
            val text = (word as? String)?.trim() ?: invalid("$label.hotwords[$index] must be a string")
            if (text.isEmpty()) invalid("$label.hotwords[$index] is empty")
            if (text.length > SpeechLimits.MAX_TOKEN_CHARS) limit("$label.hotwords[$index] has more than ${SpeechLimits.MAX_TOKEN_CHARS} characters")
            text
        }
    }

    private fun string(given: OptionSource, key: String, label: String): String? = when (val value = given.value(key)) {
        null -> null
        is String -> value
        else -> invalid("$label.$key must be a string")
    }

    private fun boolean(given: OptionSource, key: String, label: String): Boolean? = when (val value = given.value(key)) {
        null -> null
        is Boolean -> value
        else -> invalid("$label.$key must be a boolean")
    }

    private fun long(given: OptionSource, key: String, label: String): Long? = when (val value = given.value(key)) {
        null -> null
        is Int, is Long, is Short, is Byte -> (value as Number).toLong()
        else -> invalid("$label.$key must be an integer")
    }

    private fun float(given: OptionSource, key: String, label: String): Float? = when (val value = given.value(key)) {
        null -> null
        is Float -> value.takeIf { it.isFinite() } ?: invalid("$label.$key must be finite")
        is Double -> value.takeIf { it.isFinite() }?.toFloat() ?: invalid("$label.$key must be finite")
        is Int, is Long -> (value as Number).toFloat()
        else -> invalid("$label.$key must be a number")
    }

    private fun invalid(message: String): Nothing = throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, message)

    private fun limit(message: String): Nothing = throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, message)
}
