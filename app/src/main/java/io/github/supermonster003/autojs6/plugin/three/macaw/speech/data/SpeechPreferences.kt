package io.github.supermonster003.autojs6.plugin.three.macaw.speech.data

import android.content.Context
import android.content.SharedPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileAction
import org.autojs.plugin.speech.api.SpeechLimits

/**
 * The presets of the settings screen (roadmap P5.2, decided with the maintainer on 2026-10-16): three endpoint
 * sensitivities (how long a pause ends an utterance), four idle release times, the tryout speed range. Pure Kotlin.
 * zh-CN: 设置页的预设 (路线图 P5.2, 2026-10-16 与维护者确定): 三档端点灵敏度 (停顿多久算一句结束), 四档空闲释放时间, 试听的语速范围. 纯 Kotlin.
 */
object SpeechPreferencePolicy {
    /** Sensitive, standard (the protocol default), relaxed. zh-CN: 灵敏, 标准 (协议默认值), 宽松. */
    val ENDPOINT_MILLIS = listOf(600L, SpeechLimits.DEFAULT_SILENCE_END_MILLIS, 2000L)
    val IDLE_MINUTES = listOf(1, 5, 15, 30)
    const val DEFAULT_IDLE_MINUTES = 5
    const val MIN_SPEED = 0.5f
    const val MAX_SPEED = 2.0f
    const val SPEED_STEP = 0.1f

    fun endpoint(value: Long): Long = value.takeIf { it in ENDPOINT_MILLIS } ?: SpeechLimits.DEFAULT_SILENCE_END_MILLIS
    fun idleMinutes(value: Int): Int = value.takeIf { it in IDLE_MINUTES } ?: DEFAULT_IDLE_MINUTES

    /** Rounds to one decimal inside the range. zh-CN: 在范围内取一位小数. */
    fun speed(value: Float): Float = (Math.round(value.coerceIn(MIN_SPEED, MAX_SPEED) * 10f) / 10f)

    /** The saved action of the file tab, transcription when unknown (roadmap D160). zh-CN: 文件标签保存的操作, 未知时为转写 (路线图 D160). */
    fun fileAction(name: String?): FileAction = FileAction.entries.firstOrNull { it.name == name } ?: FileAction.TRANSCRIBE
}

/**
 * The plugin's own speech preferences (roadmap P5.2, data layer of roadmap 5.4). By the maintainer's decision of
 * 2026-10-16 the endpoint, VAD, speaker and speed values apply to the tryout screen and the system TTS engine only:
 * scripts keep the protocol defaults. The idle release time and the metered network choice are resource management
 * and apply everywhere. Default models stay in the model store (`setDefaultModel`), not here.
 * zh-CN: 插件自己的语音偏好 (路线图 P5.2, 路线图 5.4 的数据层). 按维护者 2026-10-16 的决定, 端点, VAD, 说话人与语速只作用于试听试说页与系统
 * TTS 引擎: 脚本仍用协议默认值. 空闲释放时间与计量网络选择属于资源管理, 处处生效. 默认模型保存在模型存储 (`setDefaultModel`), 不在此处.
 */
class SpeechPreferences(context: Context) {

    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var endpointMillis: Long
        get() = SpeechPreferencePolicy.endpoint(preferences.getLong(KEY_ENDPOINT, SpeechLimits.DEFAULT_SILENCE_END_MILLIS))
        set(value) = edit { putLong(KEY_ENDPOINT, SpeechPreferencePolicy.endpoint(value)) }

    var vad: Boolean
        get() = preferences.getBoolean(KEY_VAD, true)
        set(value) = edit { putBoolean(KEY_VAD, value) }

    var speed: Float
        get() = SpeechPreferencePolicy.speed(preferences.getFloat(KEY_SPEED, 1.0f))
        set(value) = edit { putFloat(KEY_SPEED, SpeechPreferencePolicy.speed(value)) }

    var idleMinutes: Int
        get() = SpeechPreferencePolicy.idleMinutes(preferences.getInt(KEY_IDLE_MINUTES, SpeechPreferencePolicy.DEFAULT_IDLE_MINUTES))
        set(value) = edit { putInt(KEY_IDLE_MINUTES, SpeechPreferencePolicy.idleMinutes(value)) }

    val idleMillis: Long get() = idleMinutes * 60_000L

    /**
     * The last action of the tryout file tab (roadmap D160); its models and options stay per screen (D114).
     * zh-CN: 试听试说页文件标签上次的操作 (路线图 D160); 其模型与选项仍只在页面存活期间保留 (D114).
     */
    var fileAction: FileAction
        get() = SpeechPreferencePolicy.fileAction(preferences.getString(KEY_FILE_ACTION, null))
        set(value) = edit { putString(KEY_FILE_ACTION, value.name) }

    /** Downloads on a metered network without asking (roadmap appendix D Q7 (a): asked by default). zh-CN: 计量网络下载不再询问. */
    var allowMeteredDownloads: Boolean
        get() = preferences.getBoolean(KEY_METERED, false)
        set(value) = edit { putBoolean(KEY_METERED, value) }

    /** POST_NOTIFICATIONS was asked once at the first download (API 33+). zh-CN: 首次下载时已询问过通知权限. */
    var notificationsAsked: Boolean
        get() = preferences.getBoolean(KEY_NOTIFICATIONS_ASKED, false)
        set(value) = edit { putBoolean(KEY_NOTIFICATIONS_ASKED, value) }

    /** The chosen speaker of a TTS model, or null for the model's default. zh-CN: TTS 模型选定的说话人, null 为模型默认. */
    fun speaker(modelId: String): Int? = preferences.getInt(KEY_SPEAKER_PREFIX + modelId, -1).takeIf { it >= 0 }

    fun setSpeaker(modelId: String, speaker: Int?) = edit {
        if (speaker == null || speaker < 0) remove(KEY_SPEAKER_PREFIX + modelId) else putInt(KEY_SPEAKER_PREFIX + modelId, speaker)
    }

    /** Voice names chosen per language (ISO 639 code -> voice). zh-CN: 按语言选定的语音名 (ISO 639 代码 -> 语音). */
    fun voices(): Map<String, String> = preferences.all.mapNotNull { (key, value) ->
        if (key.startsWith(KEY_VOICE_PREFIX) && value is String) key.removePrefix(KEY_VOICE_PREFIX) to value else null
    }.toMap()

    fun setVoice(language: String, voice: String?) = edit {
        if (voice == null) remove(KEY_VOICE_PREFIX + language) else putString(KEY_VOICE_PREFIX + language, voice)
    }

    private inline fun edit(block: SharedPreferences.Editor.() -> Unit) {
        preferences.edit().apply(block).apply()
    }

    private companion object {
        const val FILE = "speech-preferences"
        const val KEY_ENDPOINT = "endpointMillis"
        const val KEY_VAD = "vad"
        const val KEY_SPEED = "speed"
        const val KEY_IDLE_MINUTES = "idleMinutes"
        const val KEY_METERED = "allowMeteredDownloads"
        const val KEY_NOTIFICATIONS_ASKED = "notificationsAsked"
        const val KEY_FILE_ACTION = "tryoutFileAction"
        const val KEY_SPEAKER_PREFIX = "speaker."
        const val KEY_VOICE_PREFIX = "voice."
    }
}
