package io.github.supermonster003.autojs6.plugin.three.macaw.speech.tool

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.assistant.MacawVoiceInteractionService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.StrictJson
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentNotifications
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentWake
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentWakeService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.LocalSession
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.MicrophoneService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechPluginBinder
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechRuntime
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.PluginScreens
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.voice.VoiceInputPolicy
import org.autojs.plugin.speech.api.ISpeechSession
import org.autojs.plugin.speech.api.ISpeechSessionCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechResultKeys
import org.autojs.plugin.tool.api.PluginToolCall
import org.autojs.plugin.tool.api.PluginToolErrorCodes
import org.autojs.plugin.tool.api.PluginToolException
import org.autojs.plugin.tool.api.PluginToolResult
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * `speech_listen` (roadmap P7.6.2, D89, D90, D95, D100): one dictation up to the voice activity endpoint, the no-speech
 * timeout or the duration limit. It opens the dictation as the plugin's own entries do (`openLocalSession` with
 * takeover, D90), so the resident wake word and the wake sessions of clients pause and resume by themselves, and walks
 * the routes of [ListenRules.routes] until one records. Without the microphone permission or a recognition model it
 * answers `NOT_READY` and never shows a permission screen. The recognized text is never logged (only its length).
 * zh-CN: `speech_listen` (路线图 P7.6.2, D89, D90, D95, D100): 一次听写, 到语音活动端点, 无语音超时或时长上限为止. 它按插件自身入口的方式打开听写
 * (带接管的 `openLocalSession`, D90), 因此常驻唤醒与客户端的唤醒会话自动暂停并恢复; 按 [ListenRules.routes] 的途径依次尝试直到有一条能录音.
 * 没有麦克风权限或识别模型时返回 `NOT_READY`, 从不弹出授权页. 识别文字从不写日志 (只记长度).
 */
internal class SpeechListenTool(context: Context) {
    private val app = context.applicationContext
    private val runtime = SpeechRuntime.get(app)
    private val binder = SpeechPluginBinder(app)

    fun call(call: PluginToolCall, request: ListenRequest): PluginToolResult {
        val deadline = now() + (call.timeoutMs.takeIf { it > 0 } ?: SpeechToolCatalog.LISTEN_TIMEOUT_MS)
        if (app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw SpeechToolErrors.of(SpeechErrorCodes.PERMISSION_DENIED, "3-Macaw Speech has no microphone permission; allow it on the home screen of the plugin")
        }
        val plan = plan(request)
        val access = access()
        val cancels = Cancels(call)
        val tried = ArrayList<Pair<ListenRoute, String>>()
        for (route in ListenRules.routes(access)) {
            call.throwIfCancelled()
            val outcome = when (route) {
                ListenRoute.DIRECT -> dictate(cancels, plan, ListenRules.foregroundService(route, access), deadline, starter = null)
                ListenRoute.OPEN -> open(cancels, plan, deadline)
                ListenRoute.NOTIFY -> notify(cancels, plan, request.prompt, request.maxMillis, deadline)
            }
            when (outcome) {
                is ListenOutcome.Heard -> {
                    Log.i(TAG, "Listened through the ${route.label} route: ${outcome.endReason}, ${outcome.text.length} chars in ${outcome.durationMillis} ms")
                    val language = VoiceInputPolicy.spokenLanguage(outcome.detectedLanguage, plan.languages)
                    return PluginToolResult(String(StrictJson.write(ListenRules.result(outcome, language, plan.modelId, plan.warnings)), Charsets.UTF_8).trimEnd())
                }
                is ListenOutcome.Fallback -> {
                    Log.w(TAG, "The ${route.label} route has no microphone: ${outcome.reason}")
                    tried += route to outcome.reason
                }
                is ListenOutcome.Failed -> throw SpeechToolErrors.of(outcome.code, outcome.message)
                ListenOutcome.Cancelled -> throw PluginToolException(PluginToolErrorCodes.CANCELLED, "The dictation of speech_listen was cancelled")
            }
        }
        throw SpeechToolErrors.notReady(ListenRules.unavailable(tried, access))
    }

    /** The model for the language (D44, D95), the default otherwise; the recommended model when there is no default. zh-CN: 按语言选模型. */
    private fun plan(request: ListenRequest): ListenPlan {
        val models = runtime.models.list(kind = SpeechContract.KIND_ASR, installed = true)
        val modelId = ListenRules.model(models, request.language) ?: throw ListenRules.noModel()
        val languages = models.firstOrNull { it.id == modelId }?.languages.orEmpty()
        return ListenPlan(modelId, languages, ListenRules.options(request, modelId), listOfNotNull(ListenRules.languageWarning(request.language, modelId, languages)))
    }

    private fun access(): MicrophoneAccess = MicrophoneAccess(
        // A resident wake word that waits for a tap (D76) has no microphone itself. zh-CN: 等待点按的常驻唤醒 (D76) 本身没有麦克风.
        residentRunning = ResidentWakeService.running != null && ResidentWakeService.runningState != ResidentNotifications.State.BLOCKED,
        microphoneForeground = MicrophoneService.state == MicrophoneService.STATE_FOREGROUND,
        screenVisible = PluginScreens.visible,
        assistantActive = MacawVoiceInteractionService.isActive(app),
        overlayGranted = ResidentWake.overlayGranted(app),
        notificationsAllowed = ListenNotification.allowed(app),
        sdk = Build.VERSION.SDK_INT,
    )

    /** D89 (2): the start screen opened from the background holds the plugin on top while the dictation starts. zh-CN: 从后台打开的启动页. */
    private fun open(cancels: Cancels, plan: ListenPlan, deadline: Long): ListenOutcome {
        val waiter = ListenStarts.arm()
        try {
            try {
                app.startActivity(ListenStartActivity.intent(app))
            } catch (e: RuntimeException) {
                return ListenOutcome.Fallback("Android refused to open the start screen ($e)")
            }
            val shown = cancels.with(waiter::abandon) { waiter.shown.await(ListenRules.OPEN_WAIT_MILLIS, TimeUnit.MILLISECONDS) && waiter.resumed }
            if (cancels.cancelled) return ListenOutcome.Cancelled
            if (!shown) return ListenOutcome.Fallback("the start screen did not open from the background within ${ListenRules.OPEN_WAIT_MILLIS} ms")
            return dictate(cancels, plan, foreground = true, deadline, waiter)
        } finally {
            ListenStarts.release(waiter)
        }
    }

    /** D89 (3): "Tap to speak" opens the start screen. zh-CN: "点此说话" 打开启动页. */
    private fun notify(cancels: Cancels, plan: ListenPlan, prompt: String?, maxMillis: Long, deadline: Long): ListenOutcome {
        val wait = ListenRules.notificationWait(deadline - now(), maxMillis)
        if (wait <= 0) return ListenOutcome.Fallback("too little of the tool's time is left to wait for a tap on the notification")
        val waiter = ListenStarts.arm()
        try {
            ListenNotification.show(app, prompt, wait)
            val tapped = cancels.with(waiter::abandon) { waiter.shown.await(wait, TimeUnit.MILLISECONDS) && waiter.resumed }
            ListenNotification.cancel(app)
            if (cancels.cancelled) return ListenOutcome.Cancelled
            if (!tapped) return ListenOutcome.Fallback("the \"Tap to speak\" notification was not tapped within ${wait / 1000} s")
            return dictate(cancels, plan, foreground = true, deadline, waiter)
        } finally {
            ListenNotification.cancel(app)
            ListenStarts.release(waiter)
        }
    }

    private fun dictate(cancels: Cancels, plan: ListenPlan, foreground: Boolean, deadline: Long, starter: ListenStarts.Waiter?): ListenOutcome {
        val live = LiveDictation()
        val session = try {
            binder.openLocalSession(plan.bundle(), live.callback, LocalSession(takeover = true, foreground = foreground, recording = live::recording))
        } catch (e: RuntimeException) {
            // The binder throws `CODE: detail` as the contract defines. zh-CN: Binder 按契约以 `CODE: detail` 抛出.
            val decoded = SpeechErrorCodes.decode(e.message.orEmpty())
            return ListenOutcome.Failed(decoded?.first ?: SpeechErrorCodes.INTERNAL, decoded?.second ?: e.toString())
        }
        live.attach(session)
        return cancels.with(live::cancel) { live.await(deadline, starter) }
    }

    private data class ListenPlan(val modelId: String, val languages: List<String>, val options: Map<String, Any>, val warnings: List<ToolWarning>) {
        fun bundle(): Bundle = Bundle().apply {
            options.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Long -> putLong(key, value)
                    is List<*> -> putStringArray(key, value.map { it as String }.toTypedArray())
                    else -> throw IllegalStateException("Unexpected dictation option $key")
                }
            }
        }
    }

    /**
     * One dictation: the session callbacks (ASR thread) feed [ListenAttempt]; the tool's worker thread waits for the end,
     * closes the start screen once the microphone foreground service settled, and gives a cancelled session
     * [ListenRules.END_WAIT_MILLIS] to free the microphone.
     * zh-CN: 一次听写: 会话回调 (ASR 线程) 驱动 [ListenAttempt]; 工具的工作线程等待结束, 在麦克风前台服务有结果后关闭启动页, 并给被取消的会话
     * [ListenRules.END_WAIT_MILLIS] 释放麦克风.
     */
    private class LiveDictation {
        private val lock = ReentrantLock()
        private val ended = lock.newCondition()
        private val attempt = ListenAttempt()
        private var session: ISpeechSession? = null
        private var pending = SessionAction.NONE
        private var cancelledAt: Long? = null

        val callback = object : ISpeechSessionCallback.Stub() {
            override fun onReady(event: Bundle?) = recording()

            override fun onPartial(event: Bundle?) = locked { attempt.partial(event?.getString(SpeechResultKeys.TEXT)) }

            override fun onFinal(event: Bundle?) = act(locked { attempt.final(event?.getString(SpeechResultKeys.TEXT), event?.getString(SpeechResultKeys.LANGUAGE)) })

            override fun onEndpoint() = Unit

            override fun onVoiceActivity(event: Bundle?) = Unit

            override fun onWarning(event: Bundle?) =
                act(locked { attempt.warning(event?.getString(SpeechContract.KEY_ERROR_CODE), event?.getString(SpeechContract.KEY_ERROR_MESSAGE), now()) })

            override fun onError(event: Bundle?) = locked { attempt.error(event?.getString(SpeechContract.KEY_ERROR_CODE), event?.getString(SpeechContract.KEY_ERROR_MESSAGE)) }

            override fun onEnd(event: Bundle?) = locked {
                attempt.end(event?.getString(SpeechResultKeys.REASON), event?.getString(SpeechResultKeys.TEXT), now())
                ended.signalAll()
            }
        }

        fun recording() = locked { attempt.recording(now()) }

        fun cancel() = act(locked { attempt.cancel() })

        fun attach(opened: ISpeechSession) = act(locked {
            session = opened
            pending
        })

        fun await(deadline: Long, starter: ListenStarts.Waiter?): ListenOutcome {
            var start = starter
            val opened = now()
            try {
                while (true) {
                    locked {
                        if (attempt.outcome == null) ended.await(WAIT_SLICE_MILLIS, TimeUnit.MILLISECONDS)
                        attempt.outcome
                    }?.let { return it }
                    val now = now()
                    val state = MicrophoneService.state
                    if (start != null && (state == MicrophoneService.STATE_FOREGROUND || state == MicrophoneService.STATE_REFUSED || now - opened >= ListenRules.FOREGROUND_WAIT_MILLIS)) {
                        ListenStarts.release(start)
                        start = null
                    }
                    if (now >= deadline) act(locked { attempt.expire() })
                    // A cancelled session that never reports its end is left behind. zh-CN: 被取消后一直不报告结束的会话就此放弃.
                    val cancelled = locked { cancelledAt }
                    if (cancelled != null && now - cancelled >= ListenRules.END_WAIT_MILLIS) return locked { attempt.end(SpeechContract.REASON_CANCELLED, null, now) }
                }
            } finally {
                start?.let(ListenStarts::release)
            }
        }

        private fun act(action: SessionAction) {
            if (action == SessionAction.NONE) return
            val target = locked {
                if (action == SessionAction.CANCEL && cancelledAt == null) cancelledAt = now()
                // Sent once the session is attached. zh-CN: 会话接上后再发送.
                if (session == null && action > pending) pending = action
                session
            } ?: return
            runCatching { if (action == SessionAction.CANCEL) target.cancel() else target.finish() }
        }

        private inline fun <T> locked(block: () -> T): T = lock.withLock(block)
    }

    /** Hands the cancellation of the tool call to the step that waits. zh-CN: 把工具调用的取消交给正在等待的步骤. */
    private class Cancels(private val call: PluginToolCall) {
        @Volatile
        private var hook: (() -> Unit)? = null

        init {
            call.onCancel { hook?.invoke() }
        }

        val cancelled: Boolean get() = call.isCancelled

        fun <T> with(onCancel: () -> Unit, block: () -> T): T {
            hook = onCancel
            try {
                if (call.isCancelled) onCancel()
                return block()
            } finally {
                hook = null
            }
        }
    }

    private companion object {
        const val TAG = "MacawTools"
        const val WAIT_SLICE_MILLIS = 100L

        fun now(): Long = SystemClock.elapsedRealtime()
    }
}

/**
 * The "Tap to speak" notification of `speech_listen` (D89 (3), D95): its own channel of high importance; the caller's
 * prompt shows when the phone is unlocked, the lock screen shows the plain request. Never recognized text.
 * zh-CN: `speech_listen` 的 "点此说话" 通知 (D89 (3), D95): 独立的高重要性渠道; 调用方的提示只在解锁时显示, 锁屏只显示通用请求. 从不包含识别文字.
 */
internal object ListenNotification {
    private const val CHANNEL = "tool_listen"
    private const val ID = 0x5354
    private const val REQUEST = 0x5355

    private fun manager(context: Context): NotificationManager? = context.getSystemService(NotificationManager::class.java)

    fun allowed(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val channel = manager(context)?.getNotificationChannel(CHANNEL) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun show(context: Context, prompt: String?, waitMillis: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager(context)?.getNotificationChannel(CHANNEL) == null) {
            manager(context)?.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.notification_tool_listen_channel), NotificationManager.IMPORTANCE_HIGH),
            )
        }
        val open = PendingIntent.getActivity(
            context, REQUEST, ListenStartActivity.intent(context), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val request = context.getString(R.string.notification_tool_listen_text)
        fun builder(text: String?) = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_mic_24)
            .setContentTitle(context.getString(R.string.resident_speak_title))
            .setContentText(text ?: request)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text?.let { "$it\n$request" } ?: request))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setTimeoutAfter(waitMillis)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
        val notification = if (prompt == null) {
            builder(null).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).build()
        } else {
            builder(prompt).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(builder(null).build()).build()
        }
        runCatching { manager(context)?.notify(ID, notification) }
    }

    fun cancel(context: Context) {
        runCatching { manager(context)?.cancel(ID) }
    }
}
