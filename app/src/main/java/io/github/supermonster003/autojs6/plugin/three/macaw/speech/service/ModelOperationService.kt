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
import android.os.SystemClock
import android.text.format.Formatter
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.model.ModelManager

/**
 * Data sync foreground service (roadmap P2.3) that keeps the plugin process alive with a progress notification while
 * model operations (downloads, imports, catalog refreshes) are queued or running; it stops itself when the queue is
 * empty. Android may refuse to start it while the plugin is in the background (API 31+): the operations then continue
 * without it, and an interrupted download resumes from its partial file on the next request. The notification is
 * silent and needs `POST_NOTIFICATIONS` on API 33+ to be visible; the home screen asks for it once, at the first
 * download (roadmap P5.1), and script calls never ask.
 * zh-CN: 数据同步前台服务 (路线图 P2.3), 在模型操作 (下载, 导入, 目录刷新) 排队或运行期间以进度通知保持插件进程存活, 队列清空后自行停止.
 * 插件在后台时 Android 可能拒绝启动它 (API 31+): 操作照常继续, 被中断的下载会在下次请求时从部分文件续传. 通知为静默通知, 在 API 33+ 需要
 * `POST_NOTIFICATIONS` 才可见; 首页在第一次下载时申请一次 (路线图 P5.1), 脚本调用从不申请.
 */
class ModelOperationService : Service() {

    private var lastUpdate = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        starting = false
        // startForeground must follow startForegroundService in every case, even when the queue just emptied.
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(latest), foregroundType())
        running = this
        if (latest.pending == 0) stop()
        return START_NOT_STICKY
    }

    /** Android 15 limits data sync services to 6 hours a day. zh-CN: Android 15 限制数据同步服务每天 6 小时. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.w(TAG, "Data sync time limit reached; operations continue without the foreground service")
        stop()
    }

    override fun onDestroy() {
        if (running === this) running = null
        super.onDestroy()
    }

    private fun update(activity: ModelManager.Activity) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastUpdate < UPDATE_INTERVAL_MILLIS && activity.done < activity.total) return
        lastUpdate = now
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification(activity))
    }

    private fun stop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        if (running === this) running = null
    }

    private fun notification(activity: ModelManager.Activity): Notification {
        createChannel(this)
        val title = activity.title ?: getString(R.string.notification_channel_model_operations)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (activity.total > 0) {
            val percent = (activity.done.coerceIn(0, activity.total) * 100 / activity.total).toInt()
            builder.setProgress(100, percent, false)
                .setContentText("${Formatter.formatShortFileSize(this, activity.done)} / ${Formatter.formatShortFileSize(this, activity.total)}")
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    private fun foregroundType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0

    companion object {
        private const val TAG = "MacawModelService"
        private const val CHANNEL_ID = "model_operations"
        private const val NOTIFICATION_ID = 0x4D41
        private const val UPDATE_INTERVAL_MILLIS = 500L

        private val main = Handler(Looper.getMainLooper())

        // Touched on the main thread only.
        private var latest = ModelManager.Activity(0, null, 0, 0)
        private var running: ModelOperationService? = null
        private var starting = false

        /** Called by [ModelManager] from any thread. zh-CN: 由 [ModelManager] 从任意线程调用. */
        fun onActivity(context: Context, activity: ModelManager.Activity) {
            main.post {
                latest = activity
                val service = running
                when {
                    activity.pending == 0 -> service?.stop()
                    service != null -> service.update(activity)
                    !starting -> start(context)
                }
            }
        }

        private fun start(context: Context) {
            starting = true
            try {
                ContextCompat.startForegroundService(context, Intent(context, ModelOperationService::class.java))
            } catch (e: Exception) {
                // ForegroundServiceStartNotAllowedException (API 31+) or IllegalStateException (API 26+) from the background.
                starting = false
                Log.w(TAG, "Foreground service not started; operations continue without it: $e")
            }
        }

        private fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_model_operations), NotificationManager.IMPORTANCE_LOW),
            )
        }
    }
}
