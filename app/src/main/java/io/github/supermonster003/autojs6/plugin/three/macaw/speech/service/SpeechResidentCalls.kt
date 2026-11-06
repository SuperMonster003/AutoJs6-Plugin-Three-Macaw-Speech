package io.github.supermonster003.autojs6.plugin.three.macaw.speech.service

import android.content.Context
import android.os.Bundle
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.core.SpeechException
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentAction
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentAvailability
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentKeywords
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentObservers
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentPreferences
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentRule
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentRules
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.ResidentWake
import io.github.supermonster003.autojs6.plugin.three.macaw.speech.resident.onMainThread
import org.autojs.plugin.speech.api.ISpeechResidentObserver
import org.autojs.plugin.speech.api.SpeechContract
import org.autojs.plugin.speech.api.SpeechErrorCodes
import org.autojs.plugin.speech.api.SpeechOptionKeys
import org.autojs.plugin.speech.api.SpeechResultKeys

/**
 * The resident wake word calls of contract version 3 (roadmap P11.3.4, D189, D194, D198, D227), on Binder threads: the
 * rules ([get] / [set], checked like the settings screen: [ResidentRules.problem] and the spelling of the wake word
 * model, D18), the switch ([setEnabled]: off at once; on after the checks of D78, from the background, answering
 * `started` false and `restartNeeded` true when Android refuses the foreground service) and the hit observers.
 * zh-CN: 契约版本 3 的常驻唤醒调用 (路线图 P11.3.4, D189, D194, D198, D227), 在 Binder 线程上运行: 规则 ([get] / [set], 与设置页同样检查:
 * [ResidentRules.problem] 与唤醒词模型的拼写, D18), 开关 ([setEnabled]: 关闭立即生效; 开启先做 D78 的检查, 以后台方式启动, Android 拒绝前台服务时应答
 * `started` false 与 `restartNeeded` true) 与命中观察者.
 */
internal class SpeechResidentCalls(context: Context) {

    private val context: Context = context.applicationContext

    /** `{ enabled, rules }`. zh-CN: `{ enabled, rules }`. */
    fun get(): Bundle = config(ResidentPreferences(context).read().rules)

    /** Replaces every rule; answers the stored configuration. zh-CN: 替换全部规则; 应答保存后的配置. */
    fun set(config: Bundle?): Bundle {
        val rules = rules(config)
        ResidentRules.problem(rules)?.let { problem ->
            throw SpeechException(if (problem.limit) SpeechErrorCodes.LIMIT_EXCEEDED else SpeechErrorCodes.INVALID_OPTIONS, problem.message)
        }
        ResidentWake.unspellable(context, rules.map { it.keyword })?.let { word ->
            throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "The wake word model cannot spell \"$word\"")
        }
        val preferences = ResidentPreferences(context)
        val before = preferences.read()
        val after = preferences.update { it.copy(rules = rules) }
        // Other words reopen a running wake session; another action applies at the next hit (D223).
        if (before.reopensFor(after)) onMainThread { ResidentWake.restart(context) }
        Log.i(TAG, "Resident rules set by a script: ${rules.size} wake words")
        return config(after.rules)
    }

    /** `{ enabled, started, restartNeeded }` (D189, D227). zh-CN: `{ enabled, started, restartNeeded }`. */
    fun setEnabled(request: Bundle?): Bundle {
        if (request == null || !request.containsKey(SpeechOptionKeys.ENABLED)) throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "enabled is required")
        if (!request.getBoolean(SpeechOptionKeys.ENABLED)) {
            onMainThread { ResidentWake.turnOff(context) }
            return switch(enabled = false, started = false)
        }
        check(ResidentPreferences(context).read().keywords)
        val started = ResidentWake.turnOnInBackground(context)
        Log.i(TAG, "Resident wake word turned on by a script: started=$started")
        return switch(enabled = true, started = started)
    }

    /**
     * D78 for a script (no dialog can ask): the microphone (`PERMISSION_DENIED`, like every microphone session), then the
     * models, the spelling and the other resident listener apps (D75) as `RESIDENT_UNAVAILABLE`.
     * zh-CN: 脚本的 D78 检查 (无法弹窗询问): 麦克风 (`PERMISSION_DENIED`, 与所有麦克风会话一致), 然后模型, 拼写与其他常驻监听应用 (D75), 均为
     * `RESIDENT_UNAVAILABLE`.
     */
    private fun check(keywords: List<String>) {
        when (ResidentWake.availability(context)) {
            ResidentAvailability.MICROPHONE -> throw SpeechException(SpeechErrorCodes.PERMISSION_DENIED, "3-Macaw Speech has no microphone permission")
            ResidentAvailability.NO_WAKE_MODEL -> unavailable("No wake word model is installed")
            ResidentAvailability.NO_RECOGNITION_MODEL -> unavailable("No recognition model is installed")
            ResidentAvailability.READY -> Unit
        }
        ResidentWake.unspellable(context, keywords)?.let { unavailable("The wake word model cannot spell \"$it\"") }
        ResidentWake.others(context).firstOrNull()?.let { unavailable("The resident listener of ${it.label} is on") }
    }

    private fun unavailable(message: String): Nothing = throw SpeechException(SpeechErrorCodes.RESIDENT_UNAVAILABLE, message)

    fun register(observer: ISpeechResidentObserver?): Bundle {
        if (observer == null) throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "observer is required")
        ResidentObservers.register(observer)
        return SpeechBundles.answer(SpeechContract.CALL_VERSION_3)
    }

    fun unregister(observer: ISpeechResidentObserver?) {
        observer?.let(ResidentObservers::unregister)
    }

    private fun config(rules: List<ResidentRule>): Bundle = SpeechBundles.answer(SpeechContract.CALL_VERSION_3) {
        putBoolean(SpeechResultKeys.ENABLED, ResidentWake.enabled(context))
        putParcelableArrayList(
            SpeechResultKeys.RULES,
            ArrayList(
                rules.map { rule ->
                    Bundle().apply {
                        putString(SpeechResultKeys.KEYWORD, rule.keyword)
                        putString(SpeechResultKeys.ACTION, rule.action.id)
                        rule.script?.let { putString(SpeechResultKeys.SCRIPT, it) }
                        rule.arguments?.let { putString(SpeechResultKeys.ARGUMENTS, it) }
                    }
                },
            ),
        )
    }

    private fun switch(enabled: Boolean, started: Boolean): Bundle = SpeechBundles.answer(SpeechContract.CALL_VERSION_3) {
        putBoolean(SpeechResultKeys.ENABLED, enabled)
        putBoolean(SpeechResultKeys.STARTED, started)
        putBoolean(SpeechResultKeys.RESTART_NEEDED, enabled && !started)
    }

    companion object {
        private const val TAG = "MacawResident"

        /**
         * The rules of a `setResident` Bundle; a missing list, an element that is not a Bundle or an unknown action is
         * `INVALID_OPTIONS` (the host checked them already, D198).
         * zh-CN: `setResident` Bundle 中的规则; 缺少列表, 元素不是 Bundle 或动作未知均为 `INVALID_OPTIONS` (宿主已先检查, D198).
         */
        fun rules(config: Bundle?): List<ResidentRule> {
            @Suppress("DEPRECATION")
            val list = config?.get(SpeechOptionKeys.RULES) as? List<*> ?: throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "rules is required")
            if (list.size > ResidentKeywords.MAX_KEYWORDS) {
                throw SpeechException(SpeechErrorCodes.LIMIT_EXCEEDED, "at most ${ResidentKeywords.MAX_KEYWORDS} wake words, not ${list.size}")
            }
            return list.mapIndexed { i, item ->
                val bundle = item as? Bundle ?: throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "rules[$i] must be a Bundle")
                val action = bundle.getString(SpeechOptionKeys.ACTION)?.let { id ->
                    ResidentAction.of(id) ?: throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "rules[$i].action is unknown: ${id.take(40)}")
                } ?: ResidentAction.OPEN
                ResidentRule(
                    keyword = bundle.getString(SpeechOptionKeys.KEYWORD) ?: throw SpeechException(SpeechErrorCodes.INVALID_OPTIONS, "rules[$i].keyword is required"),
                    action = action,
                    script = bundle.getString(SpeechOptionKeys.SCRIPT),
                    arguments = bundle.getString(SpeechOptionKeys.ARGUMENTS),
                )
            }
        }
    }
}
