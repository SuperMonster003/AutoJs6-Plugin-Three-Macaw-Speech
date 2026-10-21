package io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.R
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.TriggerActivity
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger.TriggerSource
import java.util.Date

/**
 * The notifications of the resident wake word (roadmap D71, D74, D75, D76, D77): the ongoing one of the foreground
 * service with "Turn off", "Tap to speak" after a hit that cannot open the sheet, "tap to resume" after a reboot or an
 * update, and the stop reasons. They never carry recognized text.
 * zh-CN: 常驻唤醒的通知 (路线图 D71, D74, D75, D76, D77): 前台服务的常驻通知 (带 "关闭"), 命中后无法打开弹层时的 "点此说话", 重启或更新后的
 * "点此恢复", 以及停止原因. 通知中从不出现识别文字.
 */
internal object ResidentNotifications {
    const val LISTENING_ID = 0x5257
    const val SPEAK_ID = 0x5253
    const val STOPPED_ID = 0x5254
    const val RESUME_ID = 0x5255
    private const val LISTENING_CHANNEL = "resident_wake"
    private const val SPEAK_CHANNEL = "resident_wake_speak"
    private const val ALERT_CHANNEL = "resident_wake_alerts"

    /** D71: "Tap to speak" goes away after a minute. zh-CN: D71: "点此说话" 一分钟后撤销. */
    private const val SPEAK_TIMEOUT_MILLIS = 60_000L

    private const val REQUEST_SETTINGS = 0x5260
    private const val REQUEST_OFF = 0x5261
    private const val REQUEST_CONTINUE = 0x5262
    private const val REQUEST_SPEAK = 0x5263
    private const val REQUEST_RESUME = 0x5264

    /** [PAUSED]: unplugged with "only while charging"; [BLOCKED]: waits for a tap (D76). zh-CN: [PAUSED]: 未充电暂停; [BLOCKED]: 等待点按 (D76). */
    enum class State { LOADING, WAITING, PAUSED, BLOCKED }

    private fun manager(context: Context): NotificationManager? = context.getSystemService(NotificationManager::class.java)

    private fun channel(context: Context, id: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = manager(context) ?: return
        if (manager.getNotificationChannel(id) != null) return
        val (name, importance) = when (id) {
            LISTENING_CHANNEL -> R.string.settings_resident_group to NotificationManager.IMPORTANCE_LOW
            SPEAK_CHANNEL -> R.string.notification_resident_speak_channel to NotificationManager.IMPORTANCE_HIGH
            else -> R.string.notification_resident_alert_channel to NotificationManager.IMPORTANCE_DEFAULT
        }
        manager.createNotificationChannel(NotificationChannel(id, context.getString(name), importance))
    }

    /** The settings screen at the resident group. zh-CN: 定位到常驻唤醒组的设置页. */
    private fun settings(context: Context): PendingIntent = PendingIntent.getActivity(
        context, REQUEST_SETTINGS,
        Intent().setClassName(context, context.packageName + RESIDENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** The transparent trampoline that starts the service while visible (D74, D76). zh-CN: 在可见状态下启动服务的透明跳板 (D74, D76). */
    private fun trampoline(context: Context, request: Int): PendingIntent = PendingIntent.getActivity(
        context, request, ResidentStartActivity.intent(context), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /**
     * The ongoing notification of the foreground service, with "Turn off"; [deadline] is when the listening time ends.
     * zh-CN: 前台服务的常驻通知, 带 "关闭"; [deadline] 为监听时长的截止时刻.
     */
    fun listening(context: Context, state: State, keyword: String, deadline: Long?): Notification {
        channel(context, LISTENING_CHANNEL)
        val text = when (state) {
            State.LOADING -> context.getString(R.string.resident_loading)
            State.WAITING -> context.getString(R.string.resident_waiting, keyword)
            State.PAUSED -> context.getString(R.string.resident_paused)
            State.BLOCKED -> context.getString(R.string.resident_blocked)
        }
        val off = PendingIntent.getService(
            context, REQUEST_OFF, Intent(context, ResidentWakeService::class.java).setAction(ResidentWakeService.ACTION_TURN_OFF),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, LISTENING_CHANNEL)
            .setSmallIcon(R.drawable.ic_mic_24)
            .setContentTitle(context.getString(R.string.settings_resident_group))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(if (state == State.BLOCKED) trampoline(context, REQUEST_CONTINUE) else settings(context))
            .addAction(0, context.getString(R.string.resident_turn_off), off)
            .apply { deadline?.let { setSubText(until(context, it)) } }
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun updateListening(context: Context, state: State, keyword: String, deadline: Long?) {
        runCatching { manager(context)?.notify(LISTENING_ID, listening(context, state, keyword, deadline)) }
    }

    /** "Turns off at 10:42" in the device's time format (D77). zh-CN: 按设备时间格式的 "10:42 自动关闭" (D77). */
    fun until(context: Context, deadline: Long): String =
        context.getString(R.string.resident_until, DateFormat.getTimeFormat(context).format(Date(deadline)))

    /**
     * D71: a hit while the plugin may not open a screen from the background; a tap opens the sheet (over the lock screen
     * when the phone was locked at the hit).
     * zh-CN: D71: 命中时插件无法从后台打开界面; 点按即打开弹层 (命中时手机锁着则显示在锁屏之上).
     */
    fun speak(context: Context) {
        channel(context, SPEAK_CHANNEL)
        val locked = context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
        val open = PendingIntent.getActivity(
            context, REQUEST_SPEAK, TriggerActivity.intent(context, TriggerSource.WAKE, overKeyguard = locked),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, SPEAK_CHANNEL)
            .setSmallIcon(R.drawable.ic_mic_24)
            .setContentTitle(context.getString(R.string.resident_speak_title))
            .setContentText(context.getString(R.string.resident_speak_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.resident_speak_text)))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setTimeoutAfter(SPEAK_TIMEOUT_MILLIS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        runCatching { manager(context)?.notify(SPEAK_ID, notification) }
    }

    /**
     * The service stopped by itself; the switch stays on, except for [ResidentStop.CONFLICT] (D75).
     * zh-CN: 服务自行停止; 开关保持开启, [ResidentStop.CONFLICT] 除外 (D75).
     */
    fun stopped(context: Context, reason: ResidentStop, other: String?) {
        channel(context, ALERT_CHANNEL)
        val text = when (reason) {
            ResidentStop.CONFLICT -> context.getString(R.string.resident_stopped_conflict, other.orEmpty())
            ResidentStop.MICROPHONE -> context.getString(R.string.resident_stopped_microphone)
            ResidentStop.MODEL -> context.getString(R.string.resident_stopped_model)
            ResidentStop.KEYWORDS -> context.getString(R.string.resident_stopped_keywords)
            ResidentStop.ENDED -> context.getString(R.string.resident_stopped_ended)
        } + " " + context.getString(R.string.resident_stopped_retry)
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_mic_24)
            .setContentTitle(context.getString(R.string.resident_stopped_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(settings(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        runCatching { manager(context)?.notify(STOPPED_ID, notification) }
    }

    /** D74: after a reboot or an update a tap resumes the switch that is still on. zh-CN: D74: 重启或更新后, 点按即恢复仍开着的开关. */
    fun resume(context: Context) {
        channel(context, ALERT_CHANNEL)
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_mic_24)
            .setContentTitle(context.getString(R.string.resident_resume_title))
            .setContentText(context.getString(R.string.resident_resume_text))
            .setContentIntent(trampoline(context, REQUEST_RESUME))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        runCatching { manager(context)?.notify(RESUME_ID, notification) }
    }

    /** The service runs again or the switch went off: the reminders are stale. zh-CN: 服务重新运行或开关已关: 提醒作废. */
    fun cancelReminders(context: Context) {
        runCatching {
            manager(context)?.cancel(STOPPED_ID)
            manager(context)?.cancel(RESUME_ID)
            manager(context)?.cancel(SPEAK_ID)
        }
    }

    /** The settings alias that opens at the resident group (D75). zh-CN: 定位到常驻唤醒组的设置别名 (D75). */
    const val RESIDENT_SETTINGS = ".ui.settings.ResidentSettingsAlias"
}
