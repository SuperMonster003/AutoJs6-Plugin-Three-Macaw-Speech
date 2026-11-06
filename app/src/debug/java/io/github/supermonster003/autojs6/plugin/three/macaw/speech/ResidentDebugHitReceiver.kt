package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentWakeService

/**
 * Debug builds only: a hit of the running resident wake word as if the keyword spotter had reported it (roadmap
 * P11.3.5), so that the actions of the wake words (run a script, send the broadcast, the observers) can be checked in the
 * background and on the lock screen without playing a recording. Exported to holders of DUMP only (adb, the system);
 * a broadcast, unlike [ResidentDebugActivity], never makes the plugin visible.
 *
 *     am broadcast -n <package>/.ResidentDebugHitReceiver --es keyword "Hey Macaw"
 *
 * zh-CN: 仅调试构建: 模拟唤醒词检测器报告运行中的常驻唤醒命中 (路线图 P11.3.5), 用于在后台与锁屏下检查唤醒词的动作 (运行脚本, 发广播, 观察者), 无需播放
 * 录音. 只对持有 DUMP 的调用方导出 (adb 与系统); 与 [ResidentDebugActivity] 不同, 广播不会让插件变为可见.
 */
class ResidentDebugHitReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val keyword = intent.getStringExtra(EXTRA_KEYWORD)
        if (keyword.isNullOrEmpty()) {
            Log.w(TAG, "Debug hit: --es keyword <wake word>")
            return
        }
        Handler(Looper.getMainLooper()).post {
            val service = ResidentWakeService.running
            if (service == null) Log.w(TAG, "Debug hit: the resident wake word is not running") else service.injectHit(keyword)
        }
    }

    private companion object {
        const val TAG = "MacawResident"
        const val EXTRA_KEYWORD = "keyword"
    }
}
