package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.app.Activity
import android.os.Bundle
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ListeningDuration
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentKeywords
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentWake

/**
 * Debug builds only: switches the resident wake word from adb for the device runs that must not be instrumented (roadmap
 * P7.4: the background reopen of D76 and the screen-off battery measurement of D73), since an instrumented process keeps
 * a foreground state of its own. Exported to holders of DUMP only (adb, the system); a transparent window like the
 * notification trampoline, so the service starts from a visible state as it does for the user.
 *
 *     am start -n <package>/.ResidentDebugActivity --es resident on [--es keywords "a|b"] [--ez chargingOnly true]
 *         [--es duration 10|30|60|always] [--ez readAloud false]
 *     am start -n <package>/.ResidentDebugActivity --es resident off
 *
 * zh-CN: 仅调试构建: 从 adb 开关常驻唤醒, 供不能用 instrumentation 的设备运行使用 (路线图 P7.4: D76 的后台重开与 D73 的熄屏耗电测量), 因为被
 * instrumentation 的进程自带前台状态. 只对持有 DUMP 的调用方导出 (adb 与系统); 与通知跳板一样是透明窗口, 因此服务与用户操作时一样从可见状态启动.
 */
class ResidentDebugActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) apply()
        finish()
    }

    private fun apply() {
        when (intent.getStringExtra(EXTRA_RESIDENT)) {
            "on" -> {
                ResidentPreferences(this).update { settings ->
                    var next = settings
                    intent.getStringExtra(EXTRA_KEYWORDS)?.let { ResidentKeywords.parse(it.replace('|', Char(10))) }?.let { next = next.copy(keywords = it) }
                    if (intent.hasExtra(EXTRA_CHARGING_ONLY)) next = next.copy(chargingOnly = intent.getBooleanExtra(EXTRA_CHARGING_ONLY, false))
                    if (intent.hasExtra(EXTRA_READ_ALOUD)) next = next.copy(readAloud = intent.getBooleanExtra(EXTRA_READ_ALOUD, true))
                    intent.getStringExtra(EXTRA_DURATION)?.let { next = next.copy(duration = ListeningDuration.of(it)) }
                    next
                }
                Log.i(TAG, "Debug switch on: started=${ResidentWake.turnOn(this)}")
            }
            "off" -> {
                ResidentWake.turnOff(this)
                Log.i(TAG, "Debug switch off")
            }
            else -> Log.w(TAG, "Debug switch: --es resident on|off")
        }
    }

    private companion object {
        const val TAG = "MacawResident"
        const val EXTRA_RESIDENT = "resident"
        const val EXTRA_KEYWORDS = "keywords"
        const val EXTRA_CHARGING_ONLY = "chargingOnly"
        const val EXTRA_READ_ALOUD = "readAloud"
        const val EXTRA_DURATION = "duration"
    }
}
