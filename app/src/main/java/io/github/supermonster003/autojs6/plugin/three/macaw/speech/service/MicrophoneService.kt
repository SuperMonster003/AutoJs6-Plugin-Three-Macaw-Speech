package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R

/**
 * Microphone foreground service of roadmap appendix D Q5 (a), P3.1: while a microphone session (dictation or wake)
 * runs, the plugin holds a foreground service of type `microphone` with an ongoing notification, so recording goes
 * on after AutoJs6 leaves the foreground or the screen turns off (the capability the host binding passes on ends with
 * the host's own, roadmap D22). It must start while the plugin still has the microphone capability it inherits from
 * the host (the host is visible, or holds the capability itself); [acquire] is called when a session opens for that
 * reason. Android may refuse the start (API 31+ background starts, API 34+ while-in-use checks): the session goes on
 * without it and [Listener.onRefused] tells the sessions, which report a warning. A start Android drops silently
 * (`startForegroundService` answers null, or `onStartCommand` does not come within [ForegroundStartWatch.TIMEOUT_MILLIS])
 * is a refusal too, so the next session starts the service again. It is a service of its own rather
 * than a second type of [ModelOperationService]: its life follows the sessions, not the operation queue, it must not
 * inherit the Android 15 data sync time limit, and its notification says "recording", not a download progress.
 * zh-CN: 路线图附录 D Q5 (a), P3.1 的麦克风前台服务: 麦克风会话 (听写或唤醒) 运行期间, 插件持有 `microphone` 类型的前台服务与常驻通知, 因此 AutoJs6
 * 离开前台或熄屏后录音继续 (宿主绑定传递的能力随宿主自身的能力结束, 路线图 D22). 它必须在插件仍继承宿主麦克风能力时启动 (宿主可见, 或宿主自身持有
 * 能力), 因此会话打开时即调用 [acquire]. Android 可能拒绝启动 (API 31+ 的后台启动, API 34+ 的使用期间检查): 会话照常进行, [Listener.onRefused]
 * 通知各会话报告警告. 被 Android 静默丢弃的启动 (`startForegroundService` 返回 null, 或 [ForegroundStartWatch.TIMEOUT_MILLIS] 内没有
 * `onStartCommand`) 同样按被拒处理, 下一个会话会重新启动服务. 它是独立的服务, 而不是 [ModelOperationService] 的第二种类型: 其生命周期跟随会话
 * 而非操作队列, 不应受 Android 15 数据同步时长限制, 通知内容是 "正在录音" 而非下载进度.
 */
class MicrophoneService : Service() {

    fun interface Listener {
        fun onRefused(reason: String)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startWatch.arrived()
        try {
            // startForeground must follow startForegroundService, even when the last session already ended.
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), foregroundType())
            running = this
            state = if (users > 0) STATE_FOREGROUND else STATE_IDLE
        } catch (e: Exception) {
            // SecurityException or ForegroundServiceStartNotAllowedException (API 31+, while-in-use rules of API 34+).
            Log.w(TAG, "Microphone foreground service refused: $e")
            refused(e.toString())
            stopSelf()
            return START_NOT_STICKY
        } finally {
            finishStarter()
        }
        if (users == 0) stop()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (running === this) running = null
        if (state == STATE_FOREGROUND) state = STATE_IDLE
        super.onDestroy()
    }

    private fun stop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        if (running === this) running = null
        state = STATE_IDLE
    }

    private fun notification(): Notification {
        createChannel(this)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.notification_microphone_title))
            .setContentText(getString(R.string.notification_microphone_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun foregroundType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0

    companion object {
        private const val TAG = "MacawMicrophoneService"
        private const val CHANNEL_ID = "microphone_sessions"
        private const val NOTIFICATION_ID = 0x4D49

        const val STATE_IDLE = "idle"
        const val STATE_STARTING = "starting"
        const val STATE_FOREGROUND = "foreground"
        const val STATE_REFUSED = "refused"

        private val main = Handler(Looper.getMainLooper())

        /** How long the start activity may take to appear before the service is started without it. zh-CN: 启动 Activity 出现的等待上限. */
        private const val STARTER_TIMEOUT_MILLIS = 1500L

        /** The start activity closes after this at the latest. zh-CN: 启动 Activity 最迟在此后关闭. */
        private const val STARTER_LIFETIME_MILLIS = 3000L

        // Touched on the main thread only.
        private var users = 0
        private var running: MicrophoneService? = null
        private val startWatch = ForegroundStartWatch({ delayMillis, action -> main.postDelayed(action, delayMillis) })
        private var starter: MicrophoneStartActivity? = null
        private var starterSeen = false
        private val listeners = LinkedHashSet<Listener>()

        /** Last known state, for diagnostics and device tests. zh-CN: 最近的状态, 供诊断与设备测试. */
        @Volatile
        var state: String = STATE_IDLE
            private set

        /**
         * A microphone session opened: start (or keep) the foreground service. [listener] hears a refusal.
         * zh-CN: 一个麦克风会话已打开: 启动 (或保持) 前台服务. [listener] 接收拒绝通知.
         */
        fun acquire(context: Context, listener: Listener) {
            val application = context.applicationContext
            // A refusal from startForegroundService itself must reach the caller now: the host is still waiting.
            val synchronous = onMain {
                users++
                listeners += listener
                // A start under way answers this listener too. zh-CN: 进行中的启动同样答复此监听器.
                if (running != null || !startWatch.begin()) return@onMain null
                state = STATE_STARTING
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && launchStarter(application)) return@onMain null
                startService(application)
            }
            synchronous?.let { listener.onRefused(it) }
        }

        /**
         * API 34+: a foreground service started while the plugin is only bound by the visible AutoJs6 (`PROC_STATE_BTOP`)
         * gets no microphone access, and `startForeground` with type microphone throws. A transparent activity of the
         * plugin makes it the top app for a moment and starts the service from there; AutoJs6 binds the plugin with
         * `BIND_ALLOW_ACTIVITY_STARTS` (API 34+), which allows this background activity start while AutoJs6 is visible.
         * When the activity does not come up (AutoJs6 is not visible either), the service is started without it and
         * Android refuses it as before.
         * zh-CN: API 34+: 插件仅被可见的 AutoJs6 绑定 (`PROC_STATE_BTOP`) 时启动的前台服务没有麦克风权限, 以 microphone 类型调用
         * `startForeground` 会抛出异常. 插件的透明 Activity 让插件短暂成为顶层应用并从那里启动服务; AutoJs6 以 `BIND_ALLOW_ACTIVITY_STARTS`
         * (API 34+) 绑定插件, 因此 AutoJs6 可见时允许这次后台启动 Activity. Activity 没有出现时 (AutoJs6 也不可见), 不经它直接启动服务,
         * Android 照旧拒绝.
         */
        private fun launchStarter(context: Context): Boolean {
            starterSeen = false
            val attempt = startWatch.attempt
            return try {
                context.startActivity(
                    Intent(context, MicrophoneStartActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                    ),
                )
                main.postDelayed({
                    if (startWatch.isCurrent(attempt) && !starterSeen) {
                        Log.w(TAG, "The microphone start activity did not appear; starting the service without it")
                        startService(context)?.let(::refused)
                    }
                }, STARTER_TIMEOUT_MILLIS)
                true
            } catch (e: Exception) {
                Log.w(TAG, "The microphone start activity was refused: $e")
                false
            }
        }

        /** Called by [MicrophoneStartActivity] once it is created (main thread). zh-CN: 由 [MicrophoneStartActivity] 在创建后调用 (主线程). */
        internal fun onStarterCreated(activity: MicrophoneStartActivity) {
            starterSeen = true
            starter = activity
            main.postDelayed(::finishStarter, STARTER_LIFETIME_MILLIS)
            if (!startWatch.starting || running != null) {
                finishStarter()
                return
            }
            startService(activity)?.let(::refused)
        }

        private fun finishStarter() {
            val activity = starter ?: return
            starter = null
            if (!activity.isFinishing) activity.finish()
        }

        /**
         * Starts the service; answers the reason of a refusal. A start that Android accepts but that never reaches
         * `onStartCommand` is refused later, through [refused], by [ForegroundStartWatch].
         * zh-CN: 启动服务; 被拒时返回原因. Android 接受却始终没有到达 `onStartCommand` 的启动, 由 [ForegroundStartWatch] 稍后经 [refused] 拒绝.
         */
        private fun startService(context: Context): String? {
            val component = try {
                ForegroundStarts.request(context, Intent(context, MicrophoneService::class.java))
            } catch (e: Exception) {
                // ForegroundServiceStartNotAllowedException (API 31+) or SecurityException.
                startWatch.abandon()
                finishStarter()
                Log.w(TAG, "Microphone foreground service not started: $e")
                state = STATE_REFUSED
                return e.toString()
            }
            val reason = startWatch.requested(component != null) { late ->
                Log.w(TAG, "Microphone foreground service not started: $late")
                finishStarter()
                refused(late)
            } ?: return null
            finishStarter()
            Log.w(TAG, "Microphone foreground service not started: $reason")
            state = STATE_REFUSED
            return reason
        }

        /** A microphone session ended. zh-CN: 一个麦克风会话已结束. */
        fun release(listener: Listener) {
            main.post {
                if (!listeners.remove(listener)) return@post
                users = maxOf(0, users - 1)
                if (users == 0) running?.stop()
            }
        }

        private fun refused(reason: String) {
            state = STATE_REFUSED
            listeners.toList().forEach { runCatching { it.onRefused(reason) } }
        }

        private fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_microphone), NotificationManager.IMPORTANCE_LOW),
            )
        }

        /** Runs [block] on the main thread and waits for it (at most two seconds). zh-CN: 在主线程运行 [block] 并等待 (至多两秒). */
        private fun <T> onMain(block: () -> T): T? {
            if (Looper.myLooper() == Looper.getMainLooper()) return block()
            val latch = java.util.concurrent.CountDownLatch(1)
            var result: T? = null
            main.post {
                try {
                    result = block()
                } finally {
                    latch.countDown()
                }
            }
            latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
            return result
        }
    }
}
