package io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.analysis

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.MapOptionSource
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.OptionSource
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechLimits
import org.autojs.plugin.speech.api.SpeechOptionKeys

/** `diarizeFile` options (roadmap D147); [speakers] 0 counts them, a null [threshold] takes the model's value. zh-CN: `diarizeFile` 的选项. */
data class DiarizeOptions(
    val model: String? = null,
    val speakerModel: String? = null,
    val speakers: Int = 0,
    val threshold: Float? = null,
    val identify: Boolean = false,
    val displayName: String? = null,
)

/** `detectLanguage` options (roadmap D148). zh-CN: `detectLanguage` 的选项. */
data class LanguageOptions(val model: String? = null, val windows: Int = SpeechLimits.DEFAULT_LANGUAGE_WINDOWS, val displayName: String? = null)

/** `tagAudio` options (roadmap D149). zh-CN: `tagAudio` 的选项. */
data class TagOptions(val model: String? = null, val topK: Int = SpeechLimits.DEFAULT_TAG_TOP_K, val timeline: Boolean = false, val displayName: String? = null)

/**
 * Parses and checks the request Bundles of the offline analyses (contract version 2, roadmap D147 - D149) with the
 * ranges of the host's script layer: a wrong shape or range is `INVALID_OPTIONS`, a request of contract version 1 or of
 * an unknown version `PLUGIN_INCOMPATIBLE`; unknown keys are ignored. Pure Kotlin.
 * zh-CN: 以宿主脚本层的取值范围解析并检查离线分析 (契约版本 2, 路线图 D147 - D149) 的请求 Bundle: 形态或范围错误为 `INVALID_OPTIONS`, 契约版本 1
 * 或未知版本的请求为 `PLUGIN_INCOMPATIBLE`; 忽略未知键. 纯 Kotlin.
 */
object AnalysisOptions {

    /** Largest clustering threshold (a cosine distance). zh-CN: 聚类阈值上限 (余弦距离). */
    const val MAX_THRESHOLD = 2f

    fun diarize(source: OptionSource?): DiarizeOptions {
        val given = checked(source)
        val speakers = int(given, SpeechOptionKeys.SPEAKER_COUNT) ?: 0
        if (speakers !in 0..SpeechLimits.MAX_DIARIZATION_SPEAKERS) invalid("options.speakers must be between 0 and ${SpeechLimits.MAX_DIARIZATION_SPEAKERS}, got $speakers")
        val threshold = float(given, SpeechOptionKeys.THRESHOLD)
        if (threshold != null && (threshold < 0f || threshold > MAX_THRESHOLD)) invalid("options.threshold must be between 0 and $MAX_THRESHOLD, got $threshold")
        return DiarizeOptions(
            model = model(given, SpeechOptionKeys.MODEL),
            speakerModel = model(given, SpeechOptionKeys.SPEAKER_MODEL),
            speakers = speakers,
            threshold = threshold,
            identify = boolean(given, SpeechOptionKeys.IDENTIFY) ?: false,
            displayName = string(given, SpeechOptionKeys.DISPLAY_NAME),
        )
    }

    fun language(source: OptionSource?): LanguageOptions {
        val given = checked(source)
        val windows = int(given, SpeechOptionKeys.WINDOWS) ?: SpeechLimits.DEFAULT_LANGUAGE_WINDOWS
        if (windows !in 1..SpeechLimits.MAX_LANGUAGE_WINDOWS) invalid("options.windows must be between 1 and ${SpeechLimits.MAX_LANGUAGE_WINDOWS}, got $windows")
        return LanguageOptions(model(given, SpeechOptionKeys.MODEL), windows, string(given, SpeechOptionKeys.DISPLAY_NAME))
    }

    fun tag(source: OptionSource?): TagOptions {
        val given = checked(source)
        val topK = int(given, SpeechOptionKeys.TOP_K) ?: SpeechLimits.DEFAULT_TAG_TOP_K
        if (topK !in 1..SpeechLimits.MAX_TAG_TOP_K) invalid("options.topK must be between 1 and ${SpeechLimits.MAX_TAG_TOP_K}, got $topK")
        return TagOptions(model(given, SpeechOptionKeys.MODEL), topK, boolean(given, SpeechOptionKeys.TIMELINE) ?: false, string(given, SpeechOptionKeys.DISPLAY_NAME))
    }

    /** Requests without a version are the plugin's own (the screens); a host stamps version 2 on these methods. zh-CN: 不带版本的请求来自插件自身界面. */
    private fun checked(source: OptionSource?): OptionSource {
        val given = source ?: return MapOptionSource(emptyMap())
        if (given.has(SpeechContract.KEY_CONTRACT_VERSION)) {
            val version = (given.value(SpeechContract.KEY_CONTRACT_VERSION) as? Number)?.toInt()
            if (version == null || !SpeechContract.supportsContractVersion(version) || version < SpeechContract.CALL_VERSION_2) {
                throw SpeechException(SpeechErrorCodes.PLUGIN_INCOMPATIBLE, "Unsupported speech contract version $version")
            }
        }
        return given
    }

    private fun model(given: OptionSource, key: String): String? {
        val id = string(given, key) ?: return null
        if (id.isEmpty() || id.length > SpeechLimits.MAX_TOKEN_CHARS) invalid("options.$key is not a valid model id")
        return id
    }

    private fun string(given: OptionSource, key: String): String? = when (val value = given.value(key)) {
        null -> null
        is String -> value
        else -> invalid("options.$key must be a string")
    }

    private fun boolean(given: OptionSource, key: String): Boolean? = when (val value = given.value(key)) {
        null -> null
        is Boolean -> value
        else -> invalid("options.$key must be a boolean")
    }

    private fun int(given: OptionSource, key: String): Int? = when (val value = given.value(key)) {
        null -> null
        is Int, is Short, is Byte -> (value as Number).toInt()
        is Long -> value.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt() ?: invalid("options.$key is out of range")
        else -> invalid("options.$key must be an integer")
    }

    private fun float(given: OptionSource, key: String): Float? = when (val value = given.value(key)) {
        null -> null
        is Float -> value.takeIf { it.isFinite() } ?: invalid("options.$key must be finite")
        is Double -> value.takeIf { it.isFinite() }?.toFloat() ?: invalid("options.$key must be finite")
        is Int, is Long -> (value as Number).toFloat()
        else -> invalid("options.$key must be a number")
    }

    private fun invalid(message: String): Nothing = throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, message)
}
