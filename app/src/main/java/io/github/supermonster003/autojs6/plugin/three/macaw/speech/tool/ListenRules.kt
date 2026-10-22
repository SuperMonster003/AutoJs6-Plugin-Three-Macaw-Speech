package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.HitRoute
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceInputPolicy
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechOptionKeys

/**
 * The ways of `speech_listen` to the microphone (roadmap D89), tried in this order: [DIRECT] while the plugin already
 * has a microphone capability, [OPEN] through a transparent start screen when the plugin may open a screen from the
 * background, [NOTIFY] through a "Tap to speak" notification.
 * zh-CN: `speech_listen` 取得麦克风的途径 (路线图 D89), 依此顺序尝试: 插件已有麦克风能力时 [DIRECT] 直接录, 插件能从后台打开界面时 [OPEN] 经透明启动页,
 * 否则 [NOTIFY] 经 "点此说话" 通知.
 */
internal enum class ListenRoute(val label: String) {
    DIRECT("direct"),
    OPEN("start screen"),
    NOTIFY("notification"),
}

/**
 * What decides the routes: the resident wake word runs its microphone foreground service, the microphone foreground
 * service is in the foreground, a screen of the plugin is visible, 3-Macaw Speech is the digital assistant, it may draw
 * over other apps, its notifications are shown, and the API level.
 * zh-CN: 决定途径的因素: 常驻唤醒的麦克风前台服务在跑, 麦克风前台服务处于前台, 插件界面可见, 3-Macaw Speech 是数字助理, 可在其他应用上层显示, 通知可显示,
 * 以及 API 级别.
 */
internal data class MicrophoneAccess(
    val residentRunning: Boolean = false,
    val microphoneForeground: Boolean = false,
    val screenVisible: Boolean = false,
    val assistantActive: Boolean = false,
    val overlayGranted: Boolean = false,
    val notificationsAllowed: Boolean = false,
    val sdk: Int,
) {
    /**
     * Before Android 11 a foreground service started in the background records as well (the while-in-use rule for the
     * microphone came with API 30), so the plugin records directly there.
     * zh-CN: Android 11 之前在后台启动的前台服务同样可以录音 (麦克风的 while-in-use 规则自 API 30 起), 因此插件在那里直接录.
     */
    val holdsMicrophone: Boolean get() = residentRunning || microphoneForeground || screenVisible || sdk < ListenRules.WHILE_IN_USE_SDK
}

/** A warning of a tool result, `{code, message}` as the catalog declares it. zh-CN: 工具结果中的警告, 即目录声明的 `{code, message}`. */
internal data class ToolWarning(val code: String, val message: String) {
    fun toJson(): Map<String, Any?> = linkedMapOf("code" to code, "message" to message)
}

/**
 * Roadmap D89, D95: the pure rules of `speech_listen`: the routes, whether a route holds the microphone foreground
 * service, the dictation options, the end reason, the time left for the notification and the `NOT_READY` message when
 * every route failed. Pure Kotlin.
 * zh-CN: 路线图 D89, D95: `speech_listen` 的纯规则: 途径, 途径是否持有麦克风前台服务, 听写选项, 结束原因, 留给通知的时间, 以及所有途径都失败时的
 * `NOT_READY` 信息. 纯 Kotlin.
 */
internal object ListenRules {
    /** Android 11, where a foreground service started in the background loses the microphone. zh-CN: Android 11, 后台启动的前台服务自此失去麦克风. */
    const val WHILE_IN_USE_SDK = 30

    /**
     * A refused foreground service or silenced frames within this time after the recording started move on to the next
     * route; later ones end the dictation. The silence check of the microphone needs one second of zero samples.
     * zh-CN: 录音开始后此时间内出现前台服务被拒或帧被静音即换下一条途径; 之后出现则结束听写. 麦克风的静音检测需要一秒的零样本.
     */
    const val CHECK_MILLIS = 3000L

    /** How long the start screen may take to appear. zh-CN: 启动页出现的等待上限. */
    const val OPEN_WAIT_MILLIS = 2000L

    /** How long the start screen stays for the microphone foreground service at most. zh-CN: 启动页为麦克风前台服务停留的上限. */
    const val FOREGROUND_WAIT_MILLIS = 3000L

    /** How long a cancelled dictation may take to free the microphone. zh-CN: 被取消的听写释放麦克风的等待上限. */
    const val END_WAIT_MILLIS = 2000L

    /** Time kept for loading the model after the notification was tapped. zh-CN: 点按通知后留给加载模型的时间. */
    const val LOAD_MARGIN_MILLIS = 5000L

    /** A notification that could wait less than this is not shown. zh-CN: 可等待时间短于此值时不发通知. */
    const val MIN_TAP_MILLIS = 5000L

    fun routes(access: MicrophoneAccess): List<ListenRoute> = buildList {
        if (access.holdsMicrophone) add(ListenRoute.DIRECT)
        if (HitRoute.canOpen(access.assistantActive, access.overlayGranted, access.sdk)) add(ListenRoute.OPEN)
        if (access.notificationsAllowed) add(ListenRoute.NOTIFY)
    }

    /**
     * Every route holds the microphone foreground service ("Recording" notification, D93) except a direct dictation
     * inside the foreground service of the resident wake word, which supplies the microphone itself.
     * zh-CN: 每条途径都持有麦克风前台服务 ("正在录音" 通知, D93), 唯有在常驻唤醒的前台服务中直接听写时例外, 由它自己提供麦克风.
     */
    fun foregroundService(route: ListenRoute, access: MicrophoneAccess): Boolean = !(route == ListenRoute.DIRECT && access.residentRunning)

    /**
     * The dictation options of a request (D95): the chosen model, the silence that ends the utterance, the no-speech
     * timeout, the duration limit and the hotwords (the runtime warns when the model takes none). The language only
     * chooses the model (D44); the voice activity detection keeps the protocol default.
     * zh-CN: 请求对应的听写选项 (D95): 选定的模型, 结束语句的静音, 无语音超时, 时长上限与热词 (模型不支持时由运行时给出警告). 语言只用于选模型 (D44);
     * 语音活动检测保持协议默认.
     */
    fun options(request: ListenRequest, modelId: String): Map<String, Any> = buildMap {
        put(SpeechOptionKeys.MODEL, modelId)
        put(SpeechOptionKeys.SILENCE_END_MILLIS, request.silenceMillis)
        put(SpeechOptionKeys.TIMEOUT_MILLIS, request.noSpeechMillis)
        put(SpeechOptionKeys.MAX_DURATION_MILLIS, request.maxMillis)
        if (request.hotwords.isNotEmpty()) put(SpeechOptionKeys.HOTWORDS, request.hotwords)
    }

    /** D95: `max-duration`, `no-speech` when the timeout came before any text, otherwise `endpoint`. zh-CN: D95 的结束原因. */
    fun endReason(reason: String?, heard: Boolean): String = when {
        reason == SpeechContract.REASON_MAX_DURATION -> SpeechToolCatalog.END_MAX_DURATION
        reason == SpeechContract.REASON_TIMEOUT && !heard -> SpeechToolCatalog.END_NO_SPEECH
        else -> SpeechToolCatalog.END_ENDPOINT
    }

    /**
     * D95: how long the notification may wait for a tap: at most a minute, and only as long as the listening that follows
     * still fits into the tool's time; zero skips the notification.
     * zh-CN: D95: 通知等待点按的时长: 至多一分钟, 且须让随后的听写仍落在工具时限内; 为零时跳过通知.
     */
    fun notificationWait(remainingMillis: Long, maxMillis: Long): Long =
        minOf(SpeechToolCatalog.NOTIFICATION_WAIT_SECONDS * 1000L, remainingMillis - maxMillis - LOAD_MARGIN_MILLIS).takeIf { it >= MIN_TAP_MILLIS } ?: 0L

    /** A warning when no installed model covers the requested language. zh-CN: 已装模型都不覆盖请求的语言时给出警告. */
    fun languageWarning(language: String?, modelId: String, modelLanguages: List<String>): ToolWarning? {
        val primary = VoiceInputPolicy.primaryLanguage(language) ?: return null
        if (modelLanguages.any { VoiceInputPolicy.primaryLanguage(it) == primary }) return null
        return ToolWarning(SpeechErrorCodes.MODEL_MISSING, "No installed recognition model covers the language '$language'; $modelId listened instead")
    }

    /**
     * D89 (4): the `NOT_READY` message when no route reached the microphone: the reason of every route tried and what the
     * user can do. It never holds recognized text.
     * zh-CN: D89 (4): 没有途径取得麦克风时的 `NOT_READY` 信息: 每条已试途径的原因与用户可做的事. 不含识别文字.
     */
    fun unavailable(tried: List<Pair<ListenRoute, String>>, access: MicrophoneAccess): String {
        val reasons = if (tried.isEmpty()) "no route to the microphone is open" else tried.joinToString("; ") { (route, reason) -> "${route.label}: $reason" }
        val remedies = buildList {
            add("turn on the resident wake word")
            if (!access.assistantActive) add("make 3-Macaw Speech the default digital assistant")
            if (!access.overlayGranted) add("allow it to display over other apps")
            if (!access.notificationsAllowed) add("allow its notifications")
        }
        val advice = remedies.dropLast(1).joinToString(", ").let { if (it.isEmpty()) remedies.last() else "$it or ${remedies.last()}" }
        return "${SpeechErrorCodes.PERMISSION_DENIED}: 3-Macaw Speech cannot use the microphone in the background ($reasons). To fix this, ${advice}"
    }

    /**
     * The structured result of D95; [warnings] of the model choice come before those of the dictation.
     * zh-CN: D95 的结构化结果; 选模型的 [warnings] 排在听写的警告之前.
     */
    fun result(heard: ListenOutcome.Heard, language: String?, modelId: String, warnings: List<ToolWarning>): Map<String, Any?> = buildMap {
        put("text", heard.text)
        if (language != null) put("language", language)
        put("modelId", modelId)
        put("durationMillis", heard.durationMillis)
        put("endReason", heard.endReason)
        put("warnings", (warnings + heard.warnings).distinct().map { it.toJson() })
    }

}

/** How one attempt on a route ended. zh-CN: 一条途径上的一次尝试如何结束. */
internal sealed interface ListenOutcome {
    /** The dictation ran; [text] is empty when nothing was said. zh-CN: 听写已完成; 没有说话时 [text] 为空. */
    data class Heard(
        val text: String,
        val detectedLanguage: String?,
        val endReason: String,
        val durationMillis: Long,
        val warnings: List<ToolWarning> = emptyList(),
    ) : ListenOutcome

    /** This route has no microphone; the next one is tried. zh-CN: 本途径没有麦克风, 改试下一条. */
    data class Fallback(val reason: String) : ListenOutcome

    /** A speech error ends the call ([code] is a speech code, D100). zh-CN: 语音错误结束调用 ([code] 为语音错误码, D100). */
    data class Failed(val code: String, val message: String) : ListenOutcome

    data object Cancelled : ListenOutcome
}

/** What the tool does to the session after an event. zh-CN: 事件之后工具对会话的操作. */
internal enum class SessionAction { NONE, FINISH, CANCEL }

/**
 * The state machine of one dictation of `speech_listen` (D89, D95, D100), fed with the session callbacks: the first
 * final with text finishes the dictation (the text of the end event joins every final); a refused foreground service or
 * silenced frames before the recording runs [ListenRules.CHECK_MILLIS] cancel it and fall back to the next route, later
 * ones finish it and fail it with `PERMISSION_DENIED` unless text was heard; an error fails it. Other warnings go into the
 * result. Not thread-safe: the caller serializes the events. Pure Kotlin.
 * zh-CN: `speech_listen` 一次听写的状态机 (D89, D95, D100), 由会话回调驱动: 第一个有文字的 final 结束听写 (结束事件的文字合并了所有 final);
 * 录音满 [ListenRules.CHECK_MILLIS] 之前出现前台服务被拒或帧被静音则取消并改试下一条途径, 之后出现则结束听写, 没听到文字时以 `PERMISSION_DENIED`
 * 失败; 错误使其失败. 其他警告放进结果. 非线程安全: 调用方串行化事件. 纯 Kotlin.
 */
internal class ListenAttempt(private val checkMillis: Long = ListenRules.CHECK_MILLIS) {
    private var recordingAt: Long? = null
    private var decided: ListenOutcome? = null
    private var heard: String? = null
    private var detected: String? = null
    private var partial: String? = null
    private var lateDenial: String? = null
    private val collected = ArrayList<ToolWarning>()

    /** Set by the end event. zh-CN: 由结束事件设置. */
    var outcome: ListenOutcome? = null
        private set

    val warnings: List<ToolWarning> get() = collected.distinct()

    val recording: Boolean get() = recordingAt != null

    /** The microphone records (at `ready`, or earlier for a dictation without the foreground service). zh-CN: 麦克风开始录音. */
    fun recording(now: Long) {
        if (recordingAt == null) recordingAt = now
    }

    fun warning(code: String?, message: String?, now: Long): SessionAction {
        val known = code ?: SpeechErrorCodes.INTERNAL
        val detail = message?.takeIf { it.isNotBlank() } ?: known
        if (known != SpeechErrorCodes.PERMISSION_DENIED) {
            collected += ToolWarning(known, detail)
            return SessionAction.NONE
        }
        if (decided != null || outcome != null) return SessionAction.NONE
        val started = recordingAt
        if (started == null || now - started < checkMillis) {
            decided = ListenOutcome.Fallback(detail)
            return SessionAction.CANCEL
        }
        collected += ToolWarning(known, detail)
        if (lateDenial != null) return SessionAction.NONE
        lateDenial = detail
        return SessionAction.FINISH
    }

    fun partial(text: String?) {
        partial = text
    }

    fun final(text: String?, language: String?): SessionAction {
        val said = text?.trim().orEmpty()
        if (said.isEmpty() || heard != null || decided != null) return SessionAction.NONE
        heard = said
        detected = language
        return SessionAction.FINISH
    }

    fun error(code: String?, message: String?) {
        if (decided != null || outcome != null) return
        decided = ListenOutcome.Failed(code ?: SpeechErrorCodes.INTERNAL, message?.takeIf { it.isNotBlank() } ?: "The dictation failed")
    }

    /** The tool call was cancelled. zh-CN: 工具调用已取消. */
    fun cancel(): SessionAction = decide(ListenOutcome.Cancelled)

    /** The tool's time ran out before the dictation ended. zh-CN: 听写结束前工具时限已到. */
    fun expire(): SessionAction = decide(ListenOutcome.Failed(SpeechErrorCodes.TIMEOUT, "speech_listen ran out of time before the dictation ended"))

    private fun decide(early: ListenOutcome): SessionAction {
        if (decided != null || outcome != null) return SessionAction.NONE
        decided = early
        return SessionAction.CANCEL
    }

    fun end(reason: String?, text: String?, now: Long): ListenOutcome = outcome ?: settle(reason, text, now).also { outcome = it }

    private fun settle(reason: String?, text: String?, now: Long): ListenOutcome {
        decided?.let { return it }
        if (reason == SpeechContract.REASON_CANCELLED) return ListenOutcome.Cancelled
        if (reason == SpeechContract.REASON_ERROR) return ListenOutcome.Failed(SpeechErrorCodes.INTERNAL, "The dictation ended with an error")
        val said = text?.trim()?.takeIf { it.isNotEmpty() } ?: heard ?: partial?.trim()?.takeIf { it.isNotEmpty() } ?: ""
        lateDenial?.let { if (said.isEmpty()) return ListenOutcome.Failed(SpeechErrorCodes.PERMISSION_DENIED, it) }
        val duration = recordingAt?.let { (now - it).coerceAtLeast(0) } ?: 0L
        return ListenOutcome.Heard(said, detected, ListenRules.endReason(reason, said.isNotEmpty()), duration, warnings)
    }
}
