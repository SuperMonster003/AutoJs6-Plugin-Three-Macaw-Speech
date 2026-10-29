package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.FileTranscriptRecord
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.session.TranscriptExport
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileRun
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileTexts
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.FileTranscription
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.ui.tryout.TryoutActivity

/**
 * Foreground service of the file tab of the tryout screen (roadmap D111, P7.8.2): it keeps the plugin process alive
 * while [FileTranscription] recognizes a picked audio file, so the run goes on after the screen is left (the wake lock
 * of [FileTranscription] keeps it going with the screen off). Type `mediaProcessing` on Android 15+, `dataSync` on
 * Android 10 - 14, none before; on Android 15+ it therefore takes nothing from the data sync time of the model
 * downloads. The ongoing notification shows the state and the recognized time with a cancel action and opens the file
 * tab; a run that ends while no tryout screen is visible leaves a result notification that can be dismissed. No
 * notification names the file or shows recognized text, and the lock screen hides their content. Android may refuse
 * to start the service from the background; the run then goes on in the process without it.
 * zh-CN: 试听试说页文件标签的前台服务 (路线图 D111, P7.8.2): 在 [FileTranscription] 识别所选音频文件期间保持插件进程存活, 因此离开页面后识别继续
 * ([FileTranscription] 的唤醒锁使其在熄屏后也继续). Android 15+ 类型为 `mediaProcessing`, Android 10 - 14 为 `dataSync`, 更早无类型; 因此在
 * Android 15+ 不占用模型下载的数据同步时长. 进行中通知显示状态与已识别时长, 带 "取消" 动作, 点击打开文件标签; 识别结束时若没有可见的试听试说页,
 * 留下一条可清除的结果通知. 通知不写文件名与识别文字, 锁屏上隐藏内容. Android 可能拒绝从后台启动本服务, 这时识别照常在进程内进行.
 */
class FileRecognitionService : Service() {

    private var lastUpdate = 0L
    private var shownPhase: FileRun.Phase? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            FileTranscription.get(this).cancel()
            // Started for the action alone (no run holds the service): nothing to keep. zh-CN: 仅为动作而启动 (没有识别占用服务): 无需保留.
            if (running !== this && !starting) stopSelf(startId)
            return START_NOT_STICKY
        }
        starting = false
        val run = latest
        try {
            // startForeground must follow startForegroundService in every case, even when the run just ended.
            // Not ServiceCompat: androidx.core 1.15 masks the type with the types of Android 14 and drops mediaProcessing,
            // so Android 15 refuses the start ("type none").
            // zh-CN: 不用 ServiceCompat: androidx.core 1.15 用 Android 14 的类型掩码, 去掉了 mediaProcessing, Android 15 因此拒绝启动 ("type none").
            val notification = ongoing(run ?: FileRun(FileRun.Phase.PREPARING))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(RUNNING_ID, notification, foregroundType())
            else startForeground(RUNNING_ID, notification)
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException (API 31+): the run goes on without the service.
            Log.w(TAG, "Foreground service refused; the file recognition goes on without it: $e")
            stopSelf()
            return START_NOT_STICKY
        }
        running = this
        shownPhase = run?.phase
        lastUpdate = SystemClock.elapsedRealtime()
        if (run == null) stop()
        return START_NOT_STICKY
    }

    /** The time limit of Android 15 counts as a cancel (roadmap D111). zh-CN: Android 15 的时间上限按取消处理 (路线图 D111). */
    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.w(TAG, "Foreground service time limit reached; the file recognition is cancelled")
        FileTranscription.get(this).cancel()
        stop()
    }

    override fun onDestroy() {
        if (running === this) running = null
        super.onDestroy()
    }

    private fun update(run: FileRun) {
        val now = SystemClock.elapsedRealtime()
        if (run.phase == shownPhase && now - lastUpdate < UPDATE_INTERVAL_MILLIS) return
        lastUpdate = now
        shownPhase = run.phase
        getSystemService(NotificationManager::class.java)?.notify(RUNNING_ID, ongoing(run))
    }

    private fun stop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        if (running === this) running = null
    }

    private fun ongoing(run: FileRun): Notification {
        createChannel(this)
        val cancelling = run.phase == FileRun.Phase.CANCELLING
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_audio_file_24)
            .setContentTitle(getString(if (cancelling) R.string.tryout_transcribe_cancelling else if (run.action.transcribes) R.string.notification_file_running else R.string.notification_file_analyzing))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openTab(this))
        val total = run.declaredMillis
        builder.setContentText(FileTexts.progress(this, run))
        if (run.phase != FileRun.Phase.PREPARING && total != null && total > 0) {
            builder.setProgress(PROGRESS_MAX, (run.recognizedMillis.coerceIn(0, total) * PROGRESS_MAX / total).toInt(), false)
        } else {
            builder.setProgress(0, 0, true)
        }
        if (!cancelling) builder.addAction(0, getString(R.string.ui_dialog_cancel), cancel(this))
        return builder.build()
    }

    private fun foregroundType(): Int = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        else -> 0
    }

    companion object {
        private const val TAG = "MacawFileService"
        private const val CHANNEL_ID = "file_recognition"
        /** Notification ids of the ongoing and the result notification (device tests). zh-CN: 进行中通知与结果通知的 id (设备测试用). */
        @VisibleForTesting
        internal const val RUNNING_ID = 0x4D46

        @VisibleForTesting
        internal const val RESULT_ID = 0x4D52
        private const val ACTION_CANCEL = "io.github.supermonster003.autojs6.plugin.three.macaw.speech.action.CANCEL_FILE_RECOGNITION"
        private const val REQUEST_OPEN = 0x4D46
        private const val REQUEST_CANCEL = 0x4D47
        private const val PROGRESS_MAX = 1000
        private const val UPDATE_INTERVAL_MILLIS = 500L

        // Touched on the main thread only.
        private var latest: FileRun? = null
        private var running: FileRecognitionService? = null
        private var starting = false

        /** The foreground service type in effect, or null while the service does not run (device tests). zh-CN: 当前的前台服务类型, 服务未运行时为 null (设备测试用). */
        @VisibleForTesting
        internal val runningType: Int?
            get() = running?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) it.foregroundServiceType else 0 }

        /** A run starts (main thread). zh-CN: 识别开始 (主线程). */
        internal fun start(context: Context, run: FileRun) {
            latest = run
            clearResult(context)
            val service = running
            when {
                service != null -> service.update(run)
                !starting -> {
                    starting = true
                    try {
                        ContextCompat.startForegroundService(context, Intent(context, FileRecognitionService::class.java))
                    } catch (e: Exception) {
                        // ForegroundServiceStartNotAllowedException (API 31+) or IllegalStateException (API 26+) from the background.
                        starting = false
                        Log.w(TAG, "Foreground service not started; the file recognition goes on without it: $e")
                    }
                }
            }
        }

        /** Progress (main thread). zh-CN: 进度 (主线程). */
        internal fun update(context: Context, run: FileRun) {
            latest = run
            running?.update(run)
        }

        /** A run ended (main thread): the ongoing notification goes, a result notification follows unless [showResult] is false. zh-CN: 识别结束 (主线程). */
        internal fun finish(context: Context, record: FileTranscriptRecord, showResult: Boolean) {
            latest = null
            running?.stop()
            if (showResult) runCatching { context.getSystemService(NotificationManager::class.java)?.notify(RESULT_ID, result(context, record)) }
        }

        /** The result is on the screen now. zh-CN: 结果已显示在页面上. */
        internal fun clearResult(context: Context) {
            context.getSystemService(NotificationManager::class.java)?.cancel(RESULT_ID)
        }

        private fun result(context: Context, record: FileTranscriptRecord): Notification {
            createChannel(context)
            val recognized = TranscriptExport.clock(record.recognizedMillis)
            val failed = context.getString(record.failure?.let { FileTexts.problem(it.problem) } ?: R.string.tryout_transcribe_error_engine)
            // D153: the analyses have their own titles and no recognized time. zh-CN: D153: 分析有自己的标题, 没有已识别时长.
            val (title, text) = if (record.action.transcribes) {
                when (record.state) {
                    FileTranscriptRecord.State.COMPLETED -> R.string.notification_file_done to context.getString(R.string.notification_file_done_text, recognized)
                    FileTranscriptRecord.State.CANCELLED -> R.string.notification_file_cancelled to context.getString(R.string.tryout_transcribe_cancelled, recognized)
                    FileTranscriptRecord.State.FAILED -> R.string.notification_file_failed to failed
                }
            } else {
                when (record.state) {
                    FileTranscriptRecord.State.COMPLETED -> R.string.notification_analysis_done to context.getString(R.string.notification_analysis_done_text)
                    FileTranscriptRecord.State.CANCELLED -> R.string.notification_analysis_cancelled to null
                    FileTranscriptRecord.State.FAILED -> R.string.notification_analysis_failed to failed
                }
            }
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_audio_file_24)
                .setContentTitle(context.getString(title))
                .setContentText(text)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setSilent(true)
                .setAutoCancel(true)
                .setContentIntent(openTab(context))
                .build()
        }

        private fun openTab(context: Context): PendingIntent = PendingIntent.getActivity(
            context, REQUEST_OPEN, TryoutActivity.fileIntent(context), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        private fun cancel(context: Context): PendingIntent = PendingIntent.getService(
            context, REQUEST_CANCEL, Intent(context, FileRecognitionService::class.java).setAction(ACTION_CANCEL), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        private fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_file_recognition), NotificationManager.IMPORTANCE_LOW).apply {
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                },
            )
        }
    }
}
