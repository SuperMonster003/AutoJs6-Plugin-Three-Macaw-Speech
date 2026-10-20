package io.github.supermonster003.autojs6.plugin.three.macaw.speech.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Brings the floating microphone back after a reboot or an update while its switch is on (roadmap D62). Nothing else
 * starts here, and nothing records.
 * zh-CN: 开关打开时, 重启或更新后恢复悬浮麦克风 (路线图 D62). 此处不启动其他任何内容, 也不录音.
 */
class TriggerResumeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (FloatingMicService.start(context)) Log.i(TAG, "Floating microphone resumed after ${intent.action}")
            }
        }
    }

    private companion object {
        const val TAG = "MacawTrigger"
    }
}
