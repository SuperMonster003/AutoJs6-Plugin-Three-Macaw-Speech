package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentNotifications
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentResume
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentWake

/**
 * Brings the floating microphone back after a reboot or an update while its switch is on (roadmap D62), and reminds of
 * the resident wake word whose switch is still on (D74): Android 14+ forbids starting its microphone foreground service
 * from these broadcasts, so on every version a notification resumes it with a tap; a switch whose listening time ran
 * out meanwhile turns off without a reminder (D77). Nothing records here.
 * zh-CN: 开关打开时, 重启或更新后恢复悬浮麦克风 (路线图 D62), 并提醒开关仍开着的常驻唤醒 (D74): Android 14+ 不允许从这些广播启动其麦克风前台服务,
 * 因此各版本都以通知让用户点按恢复; 监听时长在此期间已到的开关直接关闭, 不提醒 (D77). 此处不录音.
 */
class TriggerResumeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (FloatingMicService.start(context)) Log.i(TAG, "Floating microphone resumed after ${intent.action}")
                when (ResidentResume.of(ResidentWake.enabled(context), ResidentPreferences(context).read(), System.currentTimeMillis())) {
                    ResidentResume.TURN_OFF -> {
                        ResidentWake.turnOff(context)
                        Log.i(TAG, "Resident wake word turned off after ${intent.action}: the listening time is over")
                    }
                    ResidentResume.RESUME -> {
                        ResidentNotifications.resume(context)
                        Log.i(TAG, "Resident wake word reminder after ${intent.action}")
                    }
                    ResidentResume.NOTHING -> Unit
                }
            }
        }
    }

    private companion object {
        const val TAG = "MacawTrigger"
    }
}
