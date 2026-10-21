package io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.assistant.MacawVoiceInteractionService
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.service.SpeechPluginBinder
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.AssistantEntry
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.SheetVisibility
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.TriggerSource
import org.autojs.plugin.speech.api.ISpeechSession
import org.autojs.plugin.speech.api.ISpeechWakeCallback
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechResultKeys
import java.lang.ref.WeakReference

/**
 * The resident wake word (roadmap P7.4, D69 - D80): a foreground service of type `microphone` that keeps a wake session of
 * the plugin itself as the background holder of the session table (D76), so every other microphone session silences it
 * until the microphone is free again (D72). A hit opens the assistant sheet like the other trigger entries (D60, D79), or
 * posts "Tap to speak" where the plugin may not open a screen from the background (D71); the screen turns on (D73).
 *
 * It is also the component of the 3-Stone AI D31 convention (D75): exported behind the plugin permission for the query
 * of the other apps only (they never start it), disabled by the Manifest until the user turns the switch on. It starts
 * only from a visible state, so Android lets it record; it stays in the foreground while the session yields or pauses,
 * which keeps that capability for the next recording (D76).
 *
 * The listening time and "only while charging" (D67, D77): at the deadline the switch turns off and the notification
 * goes; unplugged, the wake session ends (no recording, no microphone indicator) and the service waits for the power.
 * zh-CN: 常驻唤醒 (路线图 P7.4, D69 - D80): `microphone` 类型的前台服务, 以会话表的后台持有者 (D76) 持有插件自己的唤醒会话, 因此其他任何麦克风会话都
 * 让它静默, 直到麦克风再次空闲 (D72). 命中后与其他触发式入口一样打开助理弹层 (D60, D79), 插件无法从后台打开界面时发 "点此说话" (D71); 同时点亮屏幕
 * (D73). 它也是 3-Stone AI D31 约定的组件 (D75): 受插件权限保护导出, 只供其他应用查询 (对方从不启动它), Manifest 默认禁用, 用户打开开关时才启用.
 * 只从可见状态启动, Android 因此允许录音; 会话让出或暂停期间仍保持前台, 下次录音时仍有该能力 (D76). 监听时长与 "只在充电时监听" (D67, D77): 到点关闭
 * 开关并撤下通知; 拔电时结束唤醒会话 (不录音, 无麦克风指示灯), 服务等待接电.
 */
class ResidentWakeService : Service() {

    private val main = Handler(Looper.getMainLooper())
    private lateinit var preferences: ResidentPreferences
    private val binder by lazy { SpeechPluginBinder(applicationContext) }
    private var session: ISpeechSession? = null
    private var started = false

    /** Bumped for every wake session, pause and stop, so that late events of an earlier session are dropped. zh-CN: 每次开会话, 暂停与停止都递增. */
    private var generation = 0
    private var keyword = ResidentKeywords.DEFAULT.first()
    private var deadline: Long? = null
    private var state = ResidentNotifications.State.LOADING
    private var sessionError: String? = null
    private var stopping = false

    /** Plugged into power; follows the power broadcasts while the service runs (D77). zh-CN: 接着电源; 服务运行期间跟随电源广播 (D77). */
    private var plugged = true
    private var watchingPower = false

    /** The last command came from a visible state; a power change acts from the background. zh-CN: 最近的命令来自可见状态; 电源变化在后台发生. */
    private var visible = false

    /** When the current wake session was opened from the background, else null (D76). zh-CN: 当前唤醒会话从后台打开的时刻, 否则为 null (D76). */
    private var openedInBackground: Long? = null

    private val expiry = Runnable { apply() }
    private val power = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            plugged = intent.action == Intent.ACTION_POWER_CONNECTED
            visible = false
            Log.i(TAG, "Power ${if (plugged) "connected" else "disconnected"}")
            apply()
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = ResidentPreferences(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TURN_OFF) {
            // The notification's "Turn off" (a plain start): the switch goes off as well.
            ResidentWake.setEnabled(this, false)
            ResidentNotifications.cancelReminders(this)
            stop(null)
            return START_NOT_STICKY
        }
        // startForegroundService needs startForeground first, even when the service stops right away.
        if (!promote()) return START_NOT_STICKY
        if (stopping) {
            // A start that reached an instance on its way down.
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        current = WeakReference(this)
        when (intent?.action) {
            ACTION_RESTART -> {
                visible = true
                if (started) apply(reopen = true) else start()
            }
            else -> {
                visible = true
                start()
            }
        }
        // A restart by the system would come from the background, where the microphone stays silent (D74).
        return START_NOT_STICKY
    }

    private fun promote(): Boolean = try {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        ServiceCompat.startForeground(this, ResidentNotifications.LISTENING_ID, ResidentNotifications.listening(this, state, keyword, deadline), type)
        true
    } catch (e: Exception) {
        // SecurityException (no RECORD_AUDIO, or not started from a visible state on API 34+) or a refused start (API 31+).
        Log.w(TAG, "The resident wake word service was refused: $e")
        if (!stopping) ResidentNotifications.stopped(this, ResidentStop.MICROPHONE, null)
        stopping = true
        stopSelf()
        false
    }

    private fun start() {
        if (stopping) return
        // Running already: a visible state applies the rules again, which also reopens a session that waited for it.
        if (started) {
            apply()
            return
        }
        if (!ResidentWake.enabled(this)) return stop(null)
        if (preferences.read().expired(System.currentTimeMillis())) return expire()
        // Another resident listener is on (D75): two enabled components would break the convention, so this switch goes off.
        ResidentWake.others(this).firstOrNull()?.let { rival ->
            ResidentWake.setEnabled(this, false)
            return stop(ResidentStop.CONFLICT, rival.label)
        }
        ResidentNotifications.cancelReminders(this)
        watchPower()
        started = true
        Log.i(TAG, "Resident wake word started")
        apply()
    }

    /** Power broadcasts are implicit: only a registered receiver hears them. zh-CN: 电源广播是隐式广播: 只有动态注册的接收器能收到. */
    private fun watchPower() {
        if (watchingPower) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        watchingPower = runCatching { ContextCompat.registerReceiver(this, power, filter, ContextCompat.RECEIVER_NOT_EXPORTED) }.isSuccess
        plugged = ResidentWake.plugged(this)
    }

    /**
     * Applies the listening time and the power rule (D77): at the deadline the switch turns off, unplugged the wake
     * session ends, otherwise a session opens unless one runs. [reopen] reopens a running session with new keywords.
     * A session that the background could not give a microphone waits for a visible state (D76).
     * zh-CN: 应用监听时长与电源规则 (D77): 到点关闭开关, 拔电结束唤醒会话, 否则没有会话时打开一个. [reopen] 以新的唤醒词重开运行中的会话. 后台拿不到
     * 麦克风的会话等待可见状态 (D76).
     */
    private fun apply(reopen: Boolean = false) {
        if (stopping || !started) return
        val settings = preferences.read()
        val now = System.currentTimeMillis()
        deadline = settings.deadline
        main.removeCallbacks(expiry)
        // Wall clock: a timer that slept with the phone fires late; the next power change or visible state checks again.
        settings.deadline?.let { main.postDelayed(expiry, (it - now).coerceAtLeast(0)) }
        when (ResidentGate.of(settings, now, plugged)) {
            ResidentGate.EXPIRED -> expire()
            ResidentGate.PAUSED -> pause()
            ResidentGate.LISTEN -> if (reopen || (session == null && (state != ResidentNotifications.State.BLOCKED || visible))) open(settings) else notifyState()
        }
    }

    /** Unplugged with "only while charging": no wake session; the service stays for the power (D77). zh-CN: 未充电: 不开唤醒会话; 服务等待接电. */
    private fun pause() {
        if (state == ResidentNotifications.State.PAUSED && session == null) return
        Log.i(TAG, "Resident wake word paused: not charging")
        dropSession()
        state = ResidentNotifications.State.PAUSED
        notifyState()
    }

    /** A wake session opened from the background got no microphone: wait for a tap (D76). zh-CN: 后台打开的唤醒会话没有麦克风: 等待点按 (D76). */
    private fun block() {
        Log.i(TAG, "Wake session without a microphone in the background; waiting for a tap")
        dropSession()
        state = ResidentNotifications.State.BLOCKED
        notifyState()
    }

    private fun dropSession() {
        generation++
        runCatching { session?.cancel() }
        session = null
    }

    /** The listening time is over (D77): the switch turns off and the notification goes; nothing reminds. zh-CN: 监听时长已到 (D77). */
    private fun expire() {
        Log.i(TAG, "Resident wake word turned off: the listening time is over")
        ResidentWake.setEnabled(this, false)
        ResidentNotifications.cancelReminders(this)
        stop(null)
    }

    /** Opens (or reopens, after a settings change) the wake session with the stored keywords. zh-CN: 以保存的唤醒词打开 (或重开) 唤醒会话. */
    private fun open(settings: ResidentSettings) {
        dropSession()
        val gen = generation
        openedInBackground = if (visible) null else SystemClock.elapsedRealtime()
        keyword = settings.keywords.first()
        sessionError = null
        state = ResidentNotifications.State.LOADING
        notifyState()
        session = try {
            binder.openResidentWakeSession(ResidentWake.options(settings), WakeEvents(gen))
        } catch (e: RuntimeException) {
            // The binder throws `CODE: detail` as the contract defines.
            val code = SpeechErrorCodes.decode(e.message.orEmpty())?.first
            Log.w(TAG, "Wake session refused: ${code ?: e.javaClass.simpleName}")
            stop(ResidentStop.of(code))
            null
        }
        if (session != null) Log.i(TAG, "Wake session opened (${settings.keywords.size} wake words, ${if (visible) "visible" else "background"})")
    }

    /**
     * A hit (D79): ignored while the sheet is visible (its read-aloud text may contain a wake word), else the sheet opens
     * and the screen turns on (D73), or "Tap to speak" where no screen may open from the background (D71).
     * zh-CN: 命中 (D79): 弹层可见时忽略 (其朗读的文字可能含唤醒词), 否则打开弹层并点亮屏幕 (D73); 无法从后台打开界面时发 "点此说话" (D71).
     */
    private fun onHit() {
        val route = HitRoute.of(SheetVisibility.visible, MacawVoiceInteractionService.isActive(this), ResidentWake.overlayGranted(this), Build.VERSION.SDK_INT)
        Log.i(TAG, "Wake word hit: ${route.name.lowercase()}")
        hits++
        when (route) {
            HitRoute.IGNORE -> Unit
            HitRoute.OPEN -> runCatching { AssistantEntry.open(this, TriggerSource.WAKE) }.onFailure {
                Log.w(TAG, "The sheet could not open: $it")
                ResidentNotifications.speak(this)
            }
            HitRoute.NOTIFY -> ResidentNotifications.speak(this)
        }
    }

    private fun notifyState() {
        if (!stopping) ResidentNotifications.updateListening(this, state, keyword, deadline)
    }

    private fun stop(reason: ResidentStop?, other: String? = null) {
        if (stopping) return
        stopping = true
        generation++
        main.removeCallbacks(expiry)
        if (watchingPower) {
            runCatching { unregisterReceiver(power) }
            watchingPower = false
        }
        reason?.let { Log.i(TAG, "Resident wake word stopped: $it") }
        runCatching { session?.cancel() }
        session = null
        if (reason != null) ResidentNotifications.stopped(this, reason, other)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stop(null)
        if (current?.get() === this) current = null
        super.onDestroy()
    }

    /** The wake events arrive on the ASR thread (in process) and are posted to the main thread. zh-CN: 唤醒事件在 ASR 线程上到达, 投递到主线程. */
    private inner class WakeEvents(private val gen: Int) : ISpeechWakeCallback.Stub() {
        override fun onReady(info: Bundle?) = post {
            state = ResidentNotifications.State.WAITING
            notifyState()
        }

        override fun onWake(hit: Bundle?) = post { onHit() }

        override fun onWarning(warning: Bundle?) {
            val code = warning?.getString(SpeechContract.KEY_ERROR_CODE)
            Log.i(TAG, "Wake warning: $code")
            post { if (ResidentBlock.blocked(code, openedInBackground, SystemClock.elapsedRealtime())) block() }
        }

        override fun onError(error: Bundle?) {
            val code = error?.getString(SpeechContract.KEY_ERROR_CODE)
            post { sessionError = code }
        }

        override fun onEnd(end: Bundle?) {
            val reason = end?.getString(SpeechResultKeys.REASON)
            post {
                Log.i(TAG, "Wake session ended: $reason")
                stop(ResidentStop.of(sessionError))
            }
        }

        private fun post(block: () -> Unit) {
            main.post { if (gen == generation && !stopping) block() }
        }
    }

    companion object {
        private const val TAG = "MacawResident"
        const val ACTION_TURN_OFF = "io.github.supermonster003.autojs6.plugin.three.macaw.speech.action.RESIDENT_TURN_OFF"
        const val ACTION_RESTART = "io.github.supermonster003.autojs6.plugin.three.macaw.speech.action.RESIDENT_RESTART"

        /** The listening time or the power rule changed; handled like a visible start. zh-CN: 监听时长或电源规则已改; 按可见启动处理. */
        const val ACTION_REFRESH = "io.github.supermonster003.autojs6.plugin.three.macaw.speech.action.RESIDENT_REFRESH"

        private var current: WeakReference<ResidentWakeService>? = null

        /** The running service (main thread). zh-CN: 运行中的服务 (主线程). */
        internal val running: ResidentWakeService? get() = current?.get()?.takeIf { !it.stopping }

        /** Hits since the process started, for device tests. zh-CN: 进程启动以来的命中次数, 供设备测试使用. */
        @Volatile
        internal var hits = 0
            private set

        /** The state of the running service, for device tests. zh-CN: 运行中服务的状态, 供设备测试使用. */
        internal val runningState: ResidentNotifications.State? get() = running?.state
    }
}

/**
 * The transparent trampoline of "tap to resume" (D74) and "Tap to continue listening" (D76): it starts the service while
 * the plugin is visible for a moment, then finishes. Internal.
 * zh-CN: "点此恢复" (D74) 与 "点此继续监听" (D76) 的透明跳板: 在插件短暂可见时启动服务后即结束. 不导出.
 */
class ResidentStartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) ResidentWake.resume(this)
        finish()
    }

    companion object {
        internal fun intent(context: Context): Intent = Intent(context, ResidentStartActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }
}
