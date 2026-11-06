package io.github.supermonster003.autojs6.plugin.three.macaw.speech

import android.app.Activity
import android.os.Bundle
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ListeningDuration
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentKeywords
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentPower
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentWake

/**
 * Debug builds only: switches the resident wake word from adb for the device runs that must not be instrumented (roadmap
 * P7.4: the background reopen of D76 and the screen-off battery measurement of D73), since an instrumented process keeps
 * a foreground state of its own. Exported to holders of DUMP only (adb, the system); a transparent window like the
 * notification trampoline, so the service starts from a visible state as it does for the user.
 *
 *     am start -n <package>/.ResidentDebugActivity --es resident on|off [--es keywords "a|b"] [--ei keywordCount 10] [--es rules <JSON>] [--ez chargingOnly true]
 *         [--es duration 10|30|60|always] [--ez readAloud false] [--ez powerSaving false] [--es power off|gate|vad|step|both|setting]
 *
 * The options are stored before the switch, also with `off`, so that a run can put back what it changed. `power` picks a
 * measured variant of roadmap P8.1.1 for the wake sessions of this process (`setting`: the stored `powerSaving` decides).
 *
 * zh-CN: 仅调试构建: 从 adb 开关常驻唤醒, 供不能用 instrumentation 的设备运行使用 (路线图 P7.4: D76 的后台重开与 D73 的熄屏耗电测量), 因为被
 * instrumentation 的进程自带前台状态. 只对持有 DUMP 的调用方导出 (adb 与系统); 与通知跳板一样是透明窗口, 因此服务与用户操作时一样从可见状态启动.
 * 选项在开关之前保存, `off` 时也保存, 这样运行结束时能恢复它改过的选项. `power` 为本进程的唤醒会话选择路线图 P8.1.1 实测的做法 (`setting`: 由保存的
 * `powerSaving` 决定).
 */
class ResidentDebugActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) apply()
        finish()
    }

    private fun apply() {
        val resident = intent.getStringExtra(EXTRA_RESIDENT)
        if (resident != "on" && resident != "off") {
            Log.w(TAG, "Debug switch: --es resident on|off")
            return
        }
        ResidentPreferences(this).update { settings ->
            var next = settings
            intent.getStringExtra(EXTRA_KEYWORDS)?.let { ResidentKeywords.parse(it.replace('|', Char(10))) }?.let { next = next.withKeywords(it) }
            intent.getStringExtra(EXTRA_RULES)?.let { json ->
                val rules = ResidentRules.decode(json)
                if (rules == null) Log.w(TAG, "Debug switch: --es rules takes a valid JSON array of rules") else next = next.copy(rules = rules)
            }
            if (intent.hasExtra(EXTRA_KEYWORD_COUNT)) {
                next = next.withKeywords((ResidentKeywords.DEFAULT + MORE_KEYWORDS).take(intent.getIntExtra(EXTRA_KEYWORD_COUNT, 2).coerceIn(1, ResidentKeywords.MAX_KEYWORDS)))
            }
            if (intent.hasExtra(EXTRA_CHARGING_ONLY)) next = next.copy(chargingOnly = intent.getBooleanExtra(EXTRA_CHARGING_ONLY, false))
            if (intent.hasExtra(EXTRA_READ_ALOUD)) next = next.copy(readAloud = intent.getBooleanExtra(EXTRA_READ_ALOUD, true))
            intent.getStringExtra(EXTRA_DURATION)?.let { next = next.copy(duration = ListeningDuration.of(it)) }
            if (intent.hasExtra(EXTRA_POWER_SAVING)) next = next.copy(powerSaving = intent.getBooleanExtra(EXTRA_POWER_SAVING, true))
            next
        }
        intent.getStringExtra(EXTRA_POWER)?.let { name ->
            ResidentWake.powerOverride = ResidentPower.variant(name)
            Log.i(TAG, "Debug power: ${ResidentWake.powerOverride ?: "setting"}")
        }
        if (resident == "on") {
            Log.i(TAG, "Debug switch on: started=${ResidentWake.turnOn(this)}")
        } else {
            ResidentWake.turnOff(this)
            Log.i(TAG, "Debug switch off")
        }
    }

    private companion object {
        const val TAG = "MacawResident"
        const val EXTRA_RESIDENT = "resident"
        const val EXTRA_KEYWORDS = "keywords"
        const val EXTRA_CHARGING_ONLY = "chargingOnly"
        const val EXTRA_READ_ALOUD = "readAloud"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_POWER_SAVING = "powerSaving"
        const val EXTRA_POWER = "power"

        /**
         * `--ei keywordCount <n>` (roadmap P11.3.5, D188): the two default words, then words the model spells, n in all.
         * zh-CN: `--ei keywordCount <n>` (路线图 P11.3.5, D188): 两个默认词, 再加模型可拼写的词, 共 n 个.
         */
        const val EXTRA_KEYWORD_COUNT = "keywordCount"

        /** `--es rules <JSON>` (P11.3.5): the stored form of [ResidentRules.encode]. zh-CN: `--es rules <JSON>`: [ResidentRules.encode] 的存储形式. */
        const val EXTRA_RULES = "rules"
        val MORE_KEYWORDS = listOf("文森特卡索", "周望军", "蒋友伯", "女儿", "法国", "见面会", "LIGHT UP", "LOVELY CHILD")
    }
}
